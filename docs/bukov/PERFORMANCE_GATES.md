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
Release acceptance still requires a separate captured run of the packaged game
for GPU frame time, frame pacing, draw calls, texture memory, and thermal load.
