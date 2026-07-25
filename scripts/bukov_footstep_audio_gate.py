#!/usr/bin/env python3
"""Static acoustic guard for the three original Bukov footstep families."""

from __future__ import annotations

import math
import pathlib
import struct
import sys
import wave


SURFACES = ("hard", "water", "metal")
VARIANTS_PER_SURFACE = 2
MINIMUM_FAMILY_SEPARATION = 2.25


def features(path: pathlib.Path) -> tuple[float, float, float]:
    with wave.open(str(path), "rb") as source:
        if (
            source.getnchannels() != 1
            or source.getsampwidth() != 2
            or source.getframerate() != 48_000
            or source.getcomptype() != "NONE"
        ):
            raise ValueError(f"{path.name}: expected mono 48 kHz PCM16 WAV")
        frame_count = source.getnframes()
        samples = struct.unpack(
            f"<{frame_count}h", source.readframes(frame_count)
        )

    if frame_count < 2:
        raise ValueError(f"{path.name}: empty or truncated waveform")
    energy = sum(sample * sample for sample in samples)
    if energy <= 0:
        raise ValueError(f"{path.name}: silent waveform")

    duration = frame_count / 48_000
    zero_crossing_rate = sum(
        (left < 0) != (right < 0)
        for left, right in zip(samples, samples[1:])
    ) / (frame_count - 1)
    # First-difference energy is a dependency-free high-frequency proxy. It
    # distinguishes the damped water transient, hard impact and metal ring.
    roughness = sum(
        (right - left) ** 2
        for left, right in zip(samples, samples[1:])
    ) / energy
    return duration, zero_crossing_rate, roughness


def mean(values: list[tuple[float, float, float]]) -> tuple[float, float, float]:
    return tuple(
        sum(row[index] for row in values) / len(values)
        for index in range(3)
    )


def separation(
    left: tuple[float, float, float],
    right: tuple[float, float, float],
) -> float:
    # Normalizers represent a readily observable difference for each feature,
    # not a fitted model. Requiring combined distance keeps the gate tolerant
    # of future remasters while rejecting nearly identical audio families.
    scales = (0.04, 0.015, 0.10)
    return math.sqrt(
        sum(
            ((left[index] - right[index]) / scales[index]) ** 2
            for index in range(3)
        )
    )


def main() -> int:
    repo_root = pathlib.Path(__file__).resolve().parents[1]
    sound_dir = repo_root / "core/src/main/assets/sounds/bukov"
    family_features: dict[str, tuple[float, float, float]] = {}

    for surface in SURFACES:
        paths = sorted(sound_dir.glob(f"footstep_{surface}_*.wav"))
        if len(paths) != VARIANTS_PER_SURFACE:
            raise ValueError(
                f"{surface}: expected {VARIANTS_PER_SURFACE} variants, "
                f"found {len(paths)}"
            )
        family_features[surface] = mean(
            [features(path) for path in paths]
        )

    for left_index, left in enumerate(SURFACES):
        for right in SURFACES[left_index + 1 :]:
            distance = separation(
                family_features[left], family_features[right]
            )
            if distance < MINIMUM_FAMILY_SEPARATION:
                raise ValueError(
                    f"{left}/{right}: acoustic separation {distance:.2f} "
                    f"is below {MINIMUM_FAMILY_SEPARATION:.2f}"
                )

    summary = ", ".join(
        f"{surface}=duration:{values[0]:.3f}s/"
        f"zcr:{values[1]:.3f}/roughness:{values[2]:.3f}"
        for surface, values in family_features.items()
    )
    print(
        "Bukov footstep audio gate: PASS "
        f"(3 surfaces, 6 original variants; {summary})"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, wave.Error) as error:
        print(f"Bukov footstep audio gate: FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
