# Bukov ballistic visual effects

`BukovTracerFx`, `BukovMuzzleFx`, and `BukovImpactFx` are dedicated Noosa
effects for the Bukov realtime combat path. They do not use `MissileSprite`,
arrows, magic missiles, or midpoint particles.

## Coordinate contract

All constructor coordinates are world-space pixels:

- muzzle: shooter sprite center or its directional muzzle offset;
- impact: resolved hitscan collision point, not the target cell origin;
- direction: normalized or non-normalized muzzle direction.

For cell-based fallbacks, use `DungeonTilemap.tileCenterToWorld(cell)`.

## Event wiring

Drain each `CombatFxEventPool` event exactly once and add the matching effect to
the scene's world foreground group:

```java
switch (event.type()) {
	case MUZZLE_FLASH:
		foreground.add(new BukovMuzzleFx(
				new PointF(event.fromX(), event.fromY()),
				new PointF(event.toX() - event.fromX(), event.toY() - event.fromY()),
				event.hostile(),
				event.intensity()));
		break;
	case TRACER:
		foreground.add(new BukovTracerFx(
				new PointF(event.fromX(), event.fromY()),
				new PointF(event.toX(), event.toY()),
				event.hostile(),
				event.intensity()));
		break;
	case IMPACT:
		foreground.add(new BukovImpactFx(
				new PointF(event.toX(), event.toY()),
				event.hostile(),
				event.intensity()));
		break;
}
```

A zero-length or invalid tracer kills itself without drawing. The tracer
persists for 0.08 seconds, muzzle flash for 0.06 seconds, and impact sparks for
0.10 seconds. Friendly and hostile fire use different color palettes.
