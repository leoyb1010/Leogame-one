# Bukov performance gates

The performance checks intentionally have two levels:

1. `scripts/bukov_performance_smoke.sh` runs the original allocation-light
   controller/projectile inner loop as a micro benchmark.
2. `scripts/bukov_performance_e2e.sh` runs the sustained integrated CPU path:
   a 64×64 tile collision map, 30 realtime enemy brains, staggered LOS,
   movement collision, hitscan, 200 swept projectiles, queued damage,
   `RealtimeDamage`, `CombatFxEventPool`, and combat-feedback resolution.

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

`BukovFrameTelemetry` emits one `bukov-render-frame-v3` JSON record every
reporting window. The v3 record keeps cumulative nearest-rank session
P50/P95/P99 values at 0.1 ms histogram resolution and exact cumulative
refresh-budget miss counts, so a 30-minute run can be gated without trying to
reconstruct percentiles from 10-second summaries. Each record also includes
the render resolution, target refresh rate, and the per-device frame budget.
The budget is the target refresh period plus 10% host-scheduling tolerance;
this prevents a normal 60 Hz `16.67 ms` callback from being mislabeled as a
drop merely because its measured delivery rounds to `16.7–17.0 ms`.

This metric is explicitly
`cpu-render-callback-frame-pacing`, sampled from the raw
`Gdx.graphics.getDeltaTime()` received by the real render callback. It includes
CPU work, presentation wait, and host scheduling visible between callbacks.
It is **not** a hardware GPU counter and must not be presented as GPU frame
time.

Capture one uninterrupted packaged-app live gameplay scene per file. Prepend
these two metadata lines to every capture:

```text
source_commit=<full 40-character lowercase Git SHA>
platform=macOS
```

Use `platform=iOS` for the iOS capture. Do not concatenate app restarts into
one file: cumulative counters resetting or moving backwards fail the gate.
Ordinary platform log prefixes before each telemetry JSON object are accepted.

Run both platform captures through:

```sh
python3 scripts/bukov_render_frame_gate.py \
  --input /absolute/path/macOS-render.log \
  --input /absolute/path/iOS-render.log \
  --expected-source-commit "$(git rev-parse HEAD)" \
  --require-platform macOS \
  --require-platform iOS \
  --output /absolute/new/path/render-frame-summary.json
```

The default per-run thresholds are:

- uninterrupted duration at least 1,800 seconds;
- P95 at most 18.4 ms, tightened to 10.0 ms when the recorded target is
  120 Hz or higher;
- P99 at most 33.3 ms;
- no more than 5% of frames above the recorded refresh budget;
- no more than 1% of frames above 33.3 ms;
- known positive resolution and refresh target;
- exact match to the sealed source commit when one is supplied.

The JSON summary reports exact counts and the worst cumulative histogram
percentile among the individual runs. It deliberately does not label that
value as a reconstructed cross-run percentile.

The final serial gate requires both captures:

```sh
scripts/bukov_final_gate.sh --apply \
  --render-frame-log /absolute/path/macOS-render.log \
  --render-frame-log /absolute/path/iOS-render.log
```

The render log check runs before the expensive Gradle suites, so missing,
stale, reset, or threshold-failing evidence stops the final gate early.
