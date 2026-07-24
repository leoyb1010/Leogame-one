#!/usr/bin/env python3

import json
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

import bukov_render_frame_gate as gate


COMMIT = "0123456789abcdef0123456789abcdef01234567"


def record(
    *,
    frames: int,
    seconds: float,
    p50: float = 8.0,
    p95: float = 9.0,
    p99: float = 15.0,
    maximum: float = 40.0,
    over_budget: int = 10,
    over_33: int = 1,
    resolution: str = "1920x1080",
    refresh: int = 120,
) -> dict:
    return {
        "schema": gate.SCHEMA,
        "metricKind": gate.METRIC_KIND,
        "measurement": gate.MEASUREMENT,
        "hardwareGpuCounter": False,
        "scope": gate.SCOPE,
        "sessionFrames": frames,
        "sessionSeconds": seconds,
        "sessionP50Ms": p50,
        "sessionP95Ms": p95,
        "sessionP99Ms": p99,
        "frameBudgetMs": 1000.0 / refresh * 1.10,
        "sessionFramesOverBudget": over_budget,
        "sessionFramesOver33_3Ms": over_33,
        "sessionMaximumFrameMs": maximum,
        "resolutionPx": resolution,
        "targetRefreshHz": refresh,
    }


class RenderFrameGateTest(unittest.TestCase):

    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary_directory.name)

    def tearDown(self) -> None:
        self.temporary_directory.cleanup()

    def write_log(
        self,
        name: str,
        platform: str,
        records: list[dict],
        commit: str = COMMIT,
    ) -> Path:
        path = self.root / name
        lines = [
            f"source_commit={commit}",
            f"platform={platform}",
            "unrelated host log line",
        ]
        lines.extend(
            "2026-07-24 INFO BukovFrameTelemetry "
            + json.dumps(item, separators=(",", ":"))
            for item in records
        )
        path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return path

    def thresholds(self) -> gate.Thresholds:
        return gate.Thresholds(
            minimum_duration_seconds=10.0,
            max_p95_ms=18.4,
            max_p99_ms=33.3,
            max_over_budget_ratio=0.05,
            max_over_33_3_ratio=0.01,
            high_refresh_min_hz=120,
            high_refresh_max_p95_ms=10.0,
        )

    def test_prefixed_logs_produce_exact_cumulative_dual_platform_summary(
        self,
    ) -> None:
        mac = self.write_log(
            "mac.log",
            "macOS",
            [
                record(frames=600, seconds=5.0, over_budget=4, over_33=0),
                record(frames=1200, seconds=10.0, over_budget=8, over_33=1),
            ],
        )
        ios = self.write_log(
            "ios.log",
            "iOS",
            [
                record(
                    frames=600,
                    seconds=10.0,
                    p95=14.0,
                    over_budget=12,
                    over_33=2,
                    resolution="1179x2556",
                    refresh=60,
                )
            ],
        )

        summary, failures = gate.build_summary(
            [gate.load_run(mac), gate.load_run(ios)],
            self.thresholds(),
            COMMIT,
            ["macOS", "iOS"],
        )

        self.assertEqual([], failures)
        self.assertEqual("passed", summary["status"])
        self.assertFalse(summary["hardwareGpuCounter"])
        self.assertEqual(
            "cpu-render-callback-frame-pacing",
            summary["metricKind"],
        )
        self.assertEqual(1800, summary["aggregate"]["totalFrames"])
        self.assertEqual(20.0, summary["aggregate"]["totalSeconds"])
        self.assertEqual(20, summary["aggregate"]["framesOverBudget"])
        self.assertEqual(3, summary["aggregate"]["framesOver33_3Ms"])
        self.assertIn(
            "not a reconstructed cross-run percentile",
            summary["aggregate"]["aggregation"],
        )

    def test_high_refresh_run_uses_stricter_p95_limit(self) -> None:
        log = self.write_log(
            "slow-120hz.log",
            "macOS",
            [
                record(
                    frames=1200,
                    seconds=10.0,
                    p95=10.1,
                    p99=20.0,
                    over_budget=0,
                    over_33=0,
                )
            ],
        )

        failures = gate.evaluate_run(
            gate.load_run(log), self.thresholds()
        )

        self.assertIn("P95 10.100ms > 10.000ms", failures)

    def test_normal_sixty_hertz_rounding_is_within_refresh_budget(self) -> None:
        log = self.write_log(
            "steady-60hz.log",
            "iOS",
            [
                record(
                    frames=600,
                    seconds=10.0,
                    p50=16.7,
                    p95=17.0,
                    p99=17.2,
                    maximum=18.0,
                    over_budget=0,
                    over_33=0,
                    resolution="1179x2556",
                    refresh=60,
                )
            ],
        )

        self.assertEqual(
            [],
            gate.evaluate_run(gate.load_run(log), self.thresholds()),
        )

    def test_rejects_gpu_counter_claim_and_cumulative_reset(self) -> None:
        gpu_claim = record(frames=600, seconds=10.0)
        gpu_claim["hardwareGpuCounter"] = True
        gpu_log = self.write_log("gpu.log", "macOS", [gpu_claim])
        with self.assertRaisesRegex(
            gate.GateInputError, "hardwareGpuCounter=false"
        ):
            gate.load_run(gpu_log)

        reset_log = self.write_log(
            "reset.log",
            "macOS",
            [
                record(frames=600, seconds=10.0),
                record(
                    frames=10,
                    seconds=0.1,
                    over_budget=0,
                    over_33=0,
                ),
            ],
        )
        with self.assertRaisesRegex(
            gate.GateInputError, "uninterrupted live gameplay"
        ):
            gate.load_run(reset_log)

    def test_cli_fails_thresholds_and_writes_auditable_summary(self) -> None:
        log = self.write_log(
            "threshold.log",
            "iOS",
            [
                record(
                    frames=100,
                    seconds=10.0,
                    p95=20.0,
                    p99=40.0,
                    maximum=80.0,
                    over_budget=10,
                    over_33=3,
                    resolution="1179x2556",
                    refresh=60,
                )
            ],
        )
        output = self.root / "summary.json"

        with redirect_stdout(StringIO()):
            exit_code = gate.main(
                [
                    "--input",
                    str(log),
                    "--output",
                    str(output),
                    "--expected-source-commit",
                    COMMIT,
                    "--require-platform",
                    "iOS",
                    "--minimum-duration-seconds",
                    "10",
                ]
            )

        self.assertEqual(1, exit_code)
        payload = json.loads(output.read_text(encoding="utf-8"))
        self.assertEqual("failed", payload["status"])
        self.assertTrue(payload["failures"])
        self.assertIn("hardware GPU counters", payload["gpuEvidenceLimitation"])


if __name__ == "__main__":
    unittest.main()
