package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Swept, axis-separated collision against the host game's tile flags.
 */
public final class GridCollision {

	private final CollisionMap map;

	public GridCollision(CollisionMap map) {
		if (map == null) {
			throw new IllegalArgumentException("map is required");
		}
		this.map = map;
	}

	public void move(RealtimeBody body, float deltaX, float deltaY) {
		if (body == null || !body.active) {
			return;
		}
		float maxDelta = Math.max(Math.abs(deltaX), Math.abs(deltaY));
		float segmentLength = Math.max(0.05f, body.radius * 0.5f);
		int segments = Math.max(1, (int)Math.ceil(maxDelta / segmentLength));
		float stepX = deltaX / segments;
		float stepY = deltaY / segments;
		for (int i = 0; i < segments; i++) {
			float candidateX = body.x + stepX;
			if (!overlapsWall(candidateX, body.y, body.radius)) {
				body.x = candidateX;
			} else {
				body.velocityX = 0f;
			}
			float candidateY = body.y + stepY;
			if (!overlapsWall(body.x, candidateY, body.radius)) {
				body.y = candidateY;
			} else {
				body.velocityY = 0f;
			}
		}
	}

	public boolean overlapsWall(float centerX, float centerY, float radius) {
		int minX = (int)Math.floor(centerX - radius);
		int maxX = (int)Math.floor(centerX + radius);
		int minY = (int)Math.floor(centerY - radius);
		int maxY = (int)Math.floor(centerY + radius);
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				if (map.blocked(x, y)) {
					return true;
				}
			}
		}
		return false;
	}
}
