#!/usr/bin/env python3
"""Gate real Escape from Bukov render-callback frame-pacing logs.

This consumes JSON emitted by BukovFrameTelemetry from a running packaged app.
It measures CPU-side callback delivery (including presentation wait and host
scheduling). It is deliberately not, and must never be reported as, a hardware
GPU counter or a Metal/Instruments replacement.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SCHEMA = "bukov-render-frame-v4"
METRIC_KIND = "cpu-render-callback-frame-pacing"
MEASUREMENT = "Gdx.graphics.getDeltaTime"
SCOPE = "render-callback-frame-pacing"
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
BUILD_ID_RE = re.compile(r"^[A-Za-z0-9._:+-]{3,160}$")
RESOLUTION_RE = re.compile(r"^([1-9][0-9]*)x([1-9][0-9]*)$")
SUPPORTED_PLATFORMS = frozenset({"macOS", "iOS"})


class GateInputError(ValueError):
    pass


@dataclass(frozen=True)
class Thresholds:
    minimum_duration_seconds: float
    max_p95_ms: float
    max_p99_ms: float
    max_over_budget_ratio: float
    max_over_33_3_ratio: float
    high_refresh_min_hz: int
    high_refresh_max_p95_ms: float
    minimum_delivered_fps_ratio: float
    maximum_report_gap_seconds: float


@dataclass(frozen=True)
class RunEvidence:
    path: Path
    sha256: str
    source_commit: str
    build_id: str
    platform: str
    report_count: int
    session_frames: int
    session_seconds: float
    p50_ms: float
    p95_ms: float
    p99_ms: float
    maximum_frame_ms: float
    frame_budget_ms: float
    frames_over_budget: int
    frames_over_33_3_ms: int
    resolution_px: str
    observed_resolutions_px: tuple[str, ...]
    target_refresh_hz: int
    observed_refresh_targets_hz: tuple[int, ...]
    final_sequence: int
    maximum_report_gap_seconds: float

    @property
    def over_budget_ratio(self) -> float:
        return self.frames_over_budget / self.session_frames

    @property
    def over_33_3_ratio(self) -> float:
        return self.frames_over_33_3_ms / self.session_frames

    @property
    def delivered_fps(self) -> float:
        return self.session_frames / self.session_seconds


def _number(record: dict[str, Any], key: str) -> float:
    value = record.get(key)
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise GateInputError(f"{key} must be numeric")
    result = float(value)
    if not math.isfinite(result):
        raise GateInputError(f"{key} must be finite")
    return result


def _integer(record: dict[str, Any], key: str) -> int:
    value = record.get(key)
    if isinstance(value, bool) or not isinstance(value, int):
        raise GateInputError(f"{key} must be an integer")
    return value


def _telemetry_record(line: str) -> dict[str, Any] | None:
    start = line.find("{")
    if start < 0:
        return None
    try:
        record, _ = json.JSONDecoder().raw_decode(line[start:])
    except json.JSONDecodeError:
        return None
    if not isinstance(record, dict) or record.get("schema") != SCHEMA:
        return None
    return record


def _validate_record(record: dict[str, Any]) -> None:
    if record.get("metricKind") != METRIC_KIND:
        raise GateInputError("telemetry metricKind is not CPU render-callback pacing")
    if record.get("measurement") != MEASUREMENT:
        raise GateInputError("telemetry measurement is not raw Gdx render delta")
    if record.get("scope") != SCOPE:
        raise GateInputError("telemetry scope is not render-callback frame pacing")
    if record.get("hardwareGpuCounter") is not False:
        raise GateInputError("telemetry must explicitly state hardwareGpuCounter=false")
    source_commit = record.get("sourceCommit")
    build_id = record.get("buildId")
    platform = record.get("platform")
    if not isinstance(source_commit, str) or COMMIT_RE.fullmatch(source_commit) is None:
        raise GateInputError("sourceCommit must be a full lowercase SHA-1")
    if not isinstance(build_id, str) or BUILD_ID_RE.fullmatch(build_id) is None:
        raise GateInputError("buildId must be a stable non-empty build token")
    if platform not in SUPPORTED_PLATFORMS:
        raise GateInputError("platform must be macOS or iOS")
    sequence = _integer(record, "sequence")
    if sequence <= 0:
        raise GateInputError("sequence must be positive")
    if record.get("activeGameplay") is not True:
        raise GateInputError("telemetry must describe active gameplay")
    if record.get("paused") is not False:
        raise GateInputError("paused telemetry cannot be used as evidence")
    if record.get("suspended") is not False:
        raise GateInputError("suspended telemetry cannot be used as evidence")
    if _integer(record, "sessionDiscontinuities") != 0:
        raise GateInputError("discontinuous telemetry cannot be used as evidence")

    session_frames = _integer(record, "sessionFrames")
    session_seconds = _number(record, "sessionSeconds")
    active_seconds = _number(record, "activeGameplaySeconds")
    p50 = _number(record, "sessionP50Ms")
    p95 = _number(record, "sessionP95Ms")
    p99 = _number(record, "sessionP99Ms")
    maximum = _number(record, "sessionMaximumFrameMs")
    frame_budget = _number(record, "frameBudgetMs")
    over_budget = _integer(record, "sessionFramesOverBudget")
    over_33 = _integer(record, "sessionFramesOver33_3Ms")
    refresh = _integer(record, "targetRefreshHz")
    resolution = record.get("resolutionPx")

    if session_frames <= 0 or session_seconds <= 0:
        raise GateInputError("session frame count and duration must be positive")
    if active_seconds <= 0 or abs(active_seconds - session_seconds) > 0.005:
        raise GateInputError(
            "sessionSeconds must equal positive activeGameplaySeconds"
        )
    if not (0 <= over_33 <= over_budget <= session_frames):
        raise GateInputError("slow-frame counters are inconsistent")
    if not (0 <= p50 <= p95 <= p99 <= maximum):
        raise GateInputError("session percentile order is inconsistent")
    if refresh <= 0:
        raise GateInputError("targetRefreshHz must be known and positive")
    expected_budget = 1_000.0 / refresh * 1.10
    if abs(frame_budget - expected_budget) > 0.01:
        raise GateInputError(
            "frameBudgetMs must equal the refresh period plus 10% "
            "host-scheduling tolerance"
        )
    if not isinstance(resolution, str) or RESOLUTION_RE.fullmatch(resolution) is None:
        raise GateInputError("resolutionPx must be WIDTHxHEIGHT with positive integers")


def load_run(path: Path) -> RunEvidence:
    if not path.is_file():
        raise GateInputError(f"render log is missing: {path}")

    source_commit: str | None = None
    platform: str | None = None
    reports: list[dict[str, Any]] = []
    resolutions: set[str] = set()
    refresh_targets: set[int] = set()
    previous_frames = 0
    previous_seconds = 0.0
    previous_sequence = 0
    maximum_report_gap_seconds = 0.0
    record_source_commit: str | None = None
    record_build_id: str | None = None
    record_platform: str | None = None

    with path.open(encoding="utf-8", errors="replace") as handle:
        for line_number, line in enumerate(handle, start=1):
            stripped = line.strip()
            if stripped.startswith("source_commit="):
                observed_commit = stripped.split("=", 1)[1]
                if source_commit is not None and observed_commit != source_commit:
                    raise GateInputError(
                        f"{path}:{line_number}: conflicting source_commit metadata"
                    )
                source_commit = observed_commit
            elif stripped.startswith("platform="):
                observed_platform = stripped.split("=", 1)[1]
                if platform is not None and observed_platform != platform:
                    raise GateInputError(
                        f"{path}:{line_number}: conflicting platform metadata"
                    )
                platform = observed_platform

            record = _telemetry_record(line)
            if record is None:
                continue
            try:
                _validate_record(record)
            except GateInputError as error:
                raise GateInputError(f"{path}:{line_number}: {error}") from error

            current_frames = _integer(record, "sessionFrames")
            current_seconds = _number(record, "activeGameplaySeconds")
            current_sequence = _integer(record, "sequence")
            if current_sequence != previous_sequence + 1:
                raise GateInputError(
                    f"{path}:{line_number}: sequence must start at 1 and "
                    "increase by exactly one"
                )
            if current_frames <= previous_frames or current_seconds <= previous_seconds:
                raise GateInputError(
                    f"{path}:{line_number}: cumulative session telemetry reset "
                    "or moved backwards; use one uninterrupted live gameplay "
                    "scene per log"
                )
            previous_frames = current_frames
            maximum_report_gap_seconds = max(
                maximum_report_gap_seconds,
                current_seconds - previous_seconds,
            )
            previous_seconds = current_seconds
            previous_sequence = current_sequence
            observed_source_commit = str(record["sourceCommit"])
            observed_build_id = str(record["buildId"])
            observed_platform = str(record["platform"])
            if (
                record_source_commit is not None
                and observed_source_commit != record_source_commit
            ):
                raise GateInputError(
                    f"{path}:{line_number}: sourceCommit changed during run"
                )
            if record_build_id is not None and observed_build_id != record_build_id:
                raise GateInputError(
                    f"{path}:{line_number}: buildId changed during run"
                )
            if record_platform is not None and observed_platform != record_platform:
                raise GateInputError(
                    f"{path}:{line_number}: platform changed during run"
                )
            record_source_commit = observed_source_commit
            record_build_id = observed_build_id
            record_platform = observed_platform
            resolutions.add(str(record["resolutionPx"]))
            refresh_targets.add(_integer(record, "targetRefreshHz"))
            reports.append(record)

    if not reports:
        raise GateInputError(f"no {SCHEMA} records found in {path}")
    if source_commit is not None and COMMIT_RE.fullmatch(source_commit) is None:
        raise GateInputError(f"invalid source_commit metadata in {path}")
    if platform is not None and not platform:
        raise GateInputError(f"empty platform metadata in {path}")

    assert record_source_commit is not None
    assert record_build_id is not None
    assert record_platform is not None
    if source_commit is not None and source_commit != record_source_commit:
        raise GateInputError(
            f"external source_commit metadata conflicts with telemetry in {path}"
        )
    if platform is not None and platform != record_platform:
        raise GateInputError(
            f"external platform metadata conflicts with telemetry in {path}"
        )
    if len(resolutions) != 1:
        raise GateInputError(
            f"resolution changed during run in {path}; capture one stable viewport"
        )
    if len(refresh_targets) != 1:
        raise GateInputError(
            f"target refresh changed during run in {path}; capture one stable mode"
        )

    final = reports[-1]
    return RunEvidence(
        path=path.resolve(),
        sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        source_commit=record_source_commit,
        build_id=record_build_id,
        platform=record_platform,
        report_count=len(reports),
        session_frames=_integer(final, "sessionFrames"),
        session_seconds=_number(final, "activeGameplaySeconds"),
        p50_ms=_number(final, "sessionP50Ms"),
        p95_ms=_number(final, "sessionP95Ms"),
        p99_ms=_number(final, "sessionP99Ms"),
        maximum_frame_ms=_number(final, "sessionMaximumFrameMs"),
        frame_budget_ms=_number(final, "frameBudgetMs"),
        frames_over_budget=_integer(final, "sessionFramesOverBudget"),
        frames_over_33_3_ms=_integer(final, "sessionFramesOver33_3Ms"),
        resolution_px=str(final["resolutionPx"]),
        observed_resolutions_px=tuple(sorted(resolutions)),
        target_refresh_hz=_integer(final, "targetRefreshHz"),
        observed_refresh_targets_hz=tuple(sorted(refresh_targets)),
        final_sequence=_integer(final, "sequence"),
        maximum_report_gap_seconds=maximum_report_gap_seconds,
    )


def evaluate_run(run: RunEvidence, thresholds: Thresholds) -> list[str]:
    failures: list[str] = []
    p95_limit = thresholds.max_p95_ms
    if max(run.observed_refresh_targets_hz) >= thresholds.high_refresh_min_hz:
        p95_limit = thresholds.high_refresh_max_p95_ms

    if run.session_seconds < thresholds.minimum_duration_seconds:
        failures.append(
            f"duration {run.session_seconds:.3f}s "
            f"< {thresholds.minimum_duration_seconds:.3f}s"
        )
    minimum_reports = math.ceil(
        thresholds.minimum_duration_seconds
        / thresholds.maximum_report_gap_seconds
    )
    if run.report_count < minimum_reports:
        failures.append(
            f"report count {run.report_count} < {minimum_reports} required "
            "for a continuous capture"
        )
    if run.final_sequence != run.report_count:
        failures.append(
            f"final sequence {run.final_sequence} != report count "
            f"{run.report_count}"
        )
    if (
        run.maximum_report_gap_seconds
        > thresholds.maximum_report_gap_seconds + 0.005
    ):
        failures.append(
            f"report gap {run.maximum_report_gap_seconds:.3f}s "
            f"> {thresholds.maximum_report_gap_seconds:.3f}s"
        )
    minimum_fps = (
        run.target_refresh_hz
        * thresholds.minimum_delivered_fps_ratio
    )
    if run.delivered_fps < minimum_fps:
        failures.append(
            f"delivered FPS {run.delivered_fps:.3f} "
            f"< {minimum_fps:.3f} "
            f"({thresholds.minimum_delivered_fps_ratio:.0%} of target)"
        )
    if run.p95_ms > p95_limit:
        failures.append(f"P95 {run.p95_ms:.3f}ms > {p95_limit:.3f}ms")
    if run.p99_ms > thresholds.max_p99_ms:
        failures.append(
            f"P99 {run.p99_ms:.3f}ms > {thresholds.max_p99_ms:.3f}ms"
        )
    if run.over_budget_ratio > thresholds.max_over_budget_ratio:
        failures.append(
            f">refresh-budget ratio {run.over_budget_ratio:.6f} "
            f"> {thresholds.max_over_budget_ratio:.6f}"
        )
    if run.over_33_3_ratio > thresholds.max_over_33_3_ratio:
        failures.append(
            f">33.3ms ratio {run.over_33_3_ratio:.6f} "
            f"> {thresholds.max_over_33_3_ratio:.6f}"
        )
    return failures


def build_summary(
    runs: list[RunEvidence],
    thresholds: Thresholds,
    expected_source_commit: str | None,
    required_platforms: list[str],
) -> tuple[dict[str, Any], list[str]]:
    failures: list[str] = []
    observed_platforms = {run.platform for run in runs if run.platform}
    for platform in required_platforms:
        if platform not in observed_platforms:
            failures.append(f"required platform missing: {platform}")

    run_payloads: list[dict[str, Any]] = []
    for run in runs:
        run_failures = evaluate_run(run, thresholds)
        if expected_source_commit is not None:
            if run.source_commit != expected_source_commit:
                run_failures.append(
                    "source_commit does not match expected sealed commit"
                )
        failures.extend(f"{run.path}: {failure}" for failure in run_failures)
        run_payloads.append(
            {
                "path": str(run.path),
                "sha256": run.sha256,
                "sourceCommit": run.source_commit,
                "buildId": run.build_id,
                "platform": run.platform,
                "reportCount": run.report_count,
                "finalSequence": run.final_sequence,
                "sessionFrames": run.session_frames,
                "activeGameplaySeconds": round(run.session_seconds, 3),
                "deliveredFps": round(run.delivered_fps, 3),
                "maximumReportGapSeconds": round(
                    run.maximum_report_gap_seconds, 3
                ),
                "p50Ms": round(run.p50_ms, 3),
                "p95Ms": round(run.p95_ms, 3),
                "p99Ms": round(run.p99_ms, 3),
                "maximumFrameMs": round(run.maximum_frame_ms, 3),
                "frameBudgetMs": round(run.frame_budget_ms, 3),
                "framesOverBudget": run.frames_over_budget,
                "framesOver33_3Ms": run.frames_over_33_3_ms,
                "framesOverBudgetRatio": round(run.over_budget_ratio, 8),
                "framesOver33_3Ratio": round(run.over_33_3_ratio, 8),
                "resolutionPx": run.resolution_px,
                "observedResolutionsPx": list(run.observed_resolutions_px),
                "targetRefreshHz": run.target_refresh_hz,
                "observedRefreshTargetsHz": list(
                    run.observed_refresh_targets_hz
                ),
                "status": "passed" if not run_failures else "failed",
                "failures": run_failures,
            }
        )

    total_frames = sum(run.session_frames for run in runs)
    total_seconds = sum(run.session_seconds for run in runs)
    total_over_budget = sum(run.frames_over_budget for run in runs)
    total_over_33 = sum(run.frames_over_33_3_ms for run in runs)
    aggregate = {
        "aggregation": (
            "exact counts plus worst cumulative nearest-rank percentile "
            "per run at 0.1ms histogram resolution; "
            "not a reconstructed cross-run percentile"
        ),
        "totalFrames": total_frames,
        "totalSeconds": round(total_seconds, 3),
        "worstRunP50Ms": round(max(run.p50_ms for run in runs), 3),
        "worstRunP95Ms": round(max(run.p95_ms for run in runs), 3),
        "worstRunP99Ms": round(max(run.p99_ms for run in runs), 3),
        "maximumFrameMs": round(max(run.maximum_frame_ms for run in runs), 3),
        "framesOverBudget": total_over_budget,
        "framesOver33_3Ms": total_over_33,
        "framesOverBudgetRatio": round(total_over_budget / total_frames, 8),
        "framesOver33_3Ratio": round(total_over_33 / total_frames, 8),
        "resolutionsPx": sorted(
            {value for run in runs for value in run.observed_resolutions_px}
        ),
        "targetRefreshHz": sorted(
            {
                value
                for run in runs
                for value in run.observed_refresh_targets_hz
            }
        ),
    }
    summary = {
        "schemaVersion": 1,
        "gate": "bukov_render_frame_gate",
        "status": "passed" if not failures else "failed",
        "metricKind": METRIC_KIND,
        "measurement": MEASUREMENT,
        "scope": SCOPE,
        "hardwareGpuCounter": False,
        "gpuEvidenceLimitation": (
            "CPU render-callback frame pacing only. This does not provide "
            "hardware GPU counters, draw-call timing, Metal utilization, or "
            "thermal telemetry; use Instruments/Metal separately."
        ),
        "expectedSourceCommit": expected_source_commit,
        "requiredPlatforms": required_platforms,
        "thresholds": {
            "minimumDurationSecondsPerRun": thresholds.minimum_duration_seconds,
            "maxP95Ms": thresholds.max_p95_ms,
            "maxP99Ms": thresholds.max_p99_ms,
            "maxFramesOverRefreshBudgetRatio":
                thresholds.max_over_budget_ratio,
            "maxFramesOver33_3Ratio": thresholds.max_over_33_3_ratio,
            "highRefreshMinHz": thresholds.high_refresh_min_hz,
            "highRefreshMaxP95Ms": thresholds.high_refresh_max_p95_ms,
            "minimumDeliveredFpsRatio":
                thresholds.minimum_delivered_fps_ratio,
            "maximumReportGapSeconds":
                thresholds.maximum_report_gap_seconds,
        },
        "aggregate": aggregate,
        "runs": run_payloads,
        "failures": failures,
    }
    return summary, failures


def _positive(value: str) -> float:
    parsed = float(value)
    if not math.isfinite(parsed) or parsed <= 0:
        raise argparse.ArgumentTypeError("must be finite and positive")
    return parsed


def _ratio(value: str) -> float:
    parsed = float(value)
    if not math.isfinite(parsed) or parsed < 0 or parsed > 1:
        raise argparse.ArgumentTypeError("must be between zero and one")
    return parsed


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Gate real CPU render-callback frame-pacing JSON logs. "
            "This is not a hardware GPU counter."
        )
    )
    parser.add_argument(
        "--input",
        action="append",
        required=True,
        type=Path,
        help="Captured app log; repeat for macOS/iOS or multiple runs.",
    )
    parser.add_argument("--output", type=Path)
    parser.add_argument("--expected-source-commit")
    parser.add_argument("--require-platform", action="append", default=[])
    parser.add_argument(
        "--minimum-duration-seconds", type=_positive, default=1800.0
    )
    parser.add_argument("--max-p95-ms", type=_positive, default=18.4)
    parser.add_argument("--max-p99-ms", type=_positive, default=33.3)
    parser.add_argument(
        "--max-over-budget-ratio", type=_ratio, default=0.05
    )
    parser.add_argument(
        "--max-over-33-3-ratio", type=_ratio, default=0.01
    )
    parser.add_argument("--high-refresh-min-hz", type=int, default=120)
    parser.add_argument(
        "--high-refresh-max-p95-ms", type=_positive, default=10.0
    )
    parser.add_argument(
        "--minimum-delivered-fps-ratio", type=_ratio, default=0.80
    )
    parser.add_argument(
        "--maximum-report-gap-seconds", type=_positive, default=15.0
    )
    args = parser.parse_args(argv)
    if (
        args.expected_source_commit is not None
        and COMMIT_RE.fullmatch(args.expected_source_commit) is None
    ):
        parser.error("--expected-source-commit must be a full lowercase SHA-1")
    if args.high_refresh_min_hz <= 0:
        parser.error("--high-refresh-min-hz must be positive")
    if args.minimum_delivered_fps_ratio <= 0:
        parser.error("--minimum-delivered-fps-ratio must be greater than zero")
    if len(set(args.require_platform)) != len(args.require_platform):
        parser.error("--require-platform values must be unique")
    return args


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    thresholds = Thresholds(
        minimum_duration_seconds=args.minimum_duration_seconds,
        max_p95_ms=args.max_p95_ms,
        max_p99_ms=args.max_p99_ms,
        max_over_budget_ratio=args.max_over_budget_ratio,
        max_over_33_3_ratio=args.max_over_33_3_ratio,
        high_refresh_min_hz=args.high_refresh_min_hz,
        high_refresh_max_p95_ms=args.high_refresh_max_p95_ms,
        minimum_delivered_fps_ratio=args.minimum_delivered_fps_ratio,
        maximum_report_gap_seconds=args.maximum_report_gap_seconds,
    )
    try:
        runs = [load_run(path) for path in args.input]
        summary, failures = build_summary(
            runs,
            thresholds,
            args.expected_source_commit,
            args.require_platform,
        )
        rendered = json.dumps(summary, ensure_ascii=False, indent=2) + "\n"
        if args.output is not None:
            if args.output.exists():
                raise GateInputError(
                    f"refusing to overwrite output: {args.output}"
                )
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(rendered, encoding="utf-8")
        sys.stdout.write(rendered)
        return 1 if failures else 0
    except (GateInputError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
