# Bukov performance gates

The performance checks intentionally have two levels:

1. `scripts/bukov_performance_smoke.sh` runs the original allocation-light
   controller/projectile inner loop as a micro benchmark.
2. `scripts/bukov_performance_e2e.sh` runs the sustained integrated CPU path:
   a 64×64 tile collision map, 30 realtime enemy brains, staggered LOS,
   movement collision, hitscan, 200 swept projectiles, queued damage,
   `RealtimeDamage`, `CombatFxEventPool`, and combat-feedback resolution.

Both benchmarks execute a requested number of simulated 60 Hz frames as fast
as the host allows. They are fast CPU regressions, not wall-clock soak tests.
Both reports include average, P95, and P99 frame time. The end-to-end report
also records current-thread allocated bytes as an allocation proxy plus JVM
garbage-collection count and time. Its default gates are P95 below 4.5 ms for
two 120 Hz fixed steps and no more than 4096 allocated bytes per 60 Hz frame.
The limits can be overridden with
`bukov.performance.e2e.maxP95Ms` and
`bukov.performance.e2e.maxAllocatedBytesPerFrame`.

These are CPU simulation gates. They do **not** create a real `GameScene`,
submit sprite batches, compile shaders, play audio, or render through a GPU.
Release acceptance also requires captured packaged-game evidence. The
automated render-callback gate below covers real delivered frame pacing, while
GPU timing, draw calls, texture memory, Metal utilization, and thermal load
remain separate Instruments/Metal evidence.

## Packaged-app render-callback gate

`BukovFrameTelemetry` emits one `bukov-render-frame-v4` JSON record every
reporting window. The v4 record keeps cumulative nearest-rank session
P50/P95/P99 values at 0.1 ms histogram resolution and exact cumulative
refresh-budget miss counts, so a 30-minute run can be gated without trying to
reconstruct percentiles from 10-second summaries. Each record also includes
the render resolution, target refresh rate, and the per-device frame budget.
Each record embeds the build source commit, build ID, platform, monotonic
sequence, active-gameplay duration, pause/suspend state, and discontinuity
count. Pausing, suspending, receiving a resume-sized delta, or changing the
viewport/refresh target resets the evidence session.

The budget is the target refresh period plus 10% host-scheduling tolerance;
this prevents a normal 60 Hz `16.67 ms` callback from being mislabeled as a
drop merely because its measured delivery rounds to `16.7–17.0 ms`.

This metric is explicitly
`cpu-render-callback-frame-pacing`, sampled from the raw
`Gdx.graphics.getDeltaTime()` received by the real render callback. It includes
CPU work, presentation wait, and host scheduling visible between callbacks.
It is **not** a hardware GPU counter and must not be presented as GPU frame
time.

Capture one uninterrupted packaged-app live gameplay scene per file. The app
records identity in every telemetry object; optional external metadata may be
prepended for operator readability and must agree with the embedded identity:

```text
source_commit=<full 40-character lowercase Git SHA>
platform=macOS
```

Use `platform=iOS` for the iOS capture. Do not concatenate app restarts into
one file: cumulative counters resetting or moving backwards fail the gate.
Ordinary platform log prefixes before each telemetry JSON object are accepted.

Run both platform captures through:

```sh
package_source_commit=a91461c5ba57aedb76f07acd741b749100425fb2
python3 scripts/bukov_render_frame_gate.py \
  --input /absolute/path/macOS-render.log \
  --input /absolute/path/iOS-render.log \
  --expected-source-commit "$package_source_commit" \
  --require-platform macOS \
  --require-platform iOS \
  --output /absolute/new/path/render-frame-summary.json
```

Use the source commit embedded in the package and every telemetry record. Do
not substitute a later development `HEAD` after more commits have landed:
the gate must identify the binary that actually produced the logs.

The default per-run thresholds are:

- uninterrupted duration at least 1,800 seconds;
- P95 at most 18.4 ms at 60 Hz, tightened to 10.0 ms when the recorded target is
  120 Hz or higher;
- P99 at most 33.3 ms;
- no more than 5% of frames above the recorded refresh budget;
- no more than 1% of frames above 33.3 ms;
- delivered frame rate at least 80% of the recorded refresh target;
- consecutive report sequence, no report gap above 15 seconds, and enough
  reports to prove the full duration;
- active gameplay only: no paused, suspended, discontinuous, resized, or
  refresh-target-changing evidence;
- known positive resolution and refresh target;
- exact match to the sealed source commit when one is supplied.

The JSON summary reports exact counts and the worst cumulative histogram
percentile among the individual runs. It deliberately does not label that
value as a reconstructed cross-run percentile.

## Accepted Alpha 31 packaged-app evidence

The installed `2.0.0-alpha31-e2e-ci` candidate, sealed release source commit
`a91461c5ba57aedb76f07acd741b749100425fb2`, passed the two-platform
render-callback gate on 2026-07-25. The immutable summary is:

`/Users/leoyuan/Documents/日常/output/evidence/a91461c5b-alpha31-performance/render-frame-summary.json`

| Platform | Active gameplay | Frames | Delivered FPS | P50 | P95 | P99 | Refresh-budget misses | >33.3 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| macOS | 1850.970 s | 216158 | 116.781 | 8.9 ms | 12.5 ms | 16.8 ms | 4 | 1 |
| iOS Simulator | 2072.118 s | 124310 | 59.992 | 16.7 ms | 17.6 ms | 18.1 ms | 366 | 3 |

Both runs reported continuous active gameplay, monotonic sequences, no
pause/suspend/session discontinuity and one stable resolution/refresh target.
The aggregate contains 340468 frames over 3923.088 seconds. Its worst per-run
P95 was 17.6 ms, below the 18.4 ms 60 Hz threshold. This acceptance remains
bound to the sealed release source `a91461c5`; a later documentation-only
commit does not change the package identity.

The adjacent `thermal-process-snapshot.txt` reported no system thermal or
performance warning at capture time. It is a process snapshot, not a substitute
for Instruments/Metal hardware GPU, temperature or throttling evidence.

The same sealed source also completed the 33-step serial automated final gate:

`/Users/leoyuan/Documents/日常/output/evidence/a91461c5b-alpha31-final-gate/summary.json`

All 33 steps passed, including render-callback pacing, full core/desktop/iOS
tests, 10,000 seeds, 100 save iterations, 25 mode/theme/Boss combinations,
Apple builds, packaged legal files, provenance and source-integrity
verification. This automated result still does not replace physical iPhone,
controller, hardware GPU/Instruments or full manual player-route evidence.

The final serial gate requires both captures:

```sh
scripts/bukov_final_gate.sh --apply \
  --render-frame-log /absolute/path/macOS-render.log \
  --render-frame-log /absolute/path/iOS-render.log
```

The render log check runs before the expensive Gradle suites, so missing,
stale, reset, or threshold-failing evidence stops the final gate early.
