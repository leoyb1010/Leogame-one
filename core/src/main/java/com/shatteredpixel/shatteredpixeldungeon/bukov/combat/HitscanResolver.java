package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeBody;

public final class HitscanResolver {

	public interface TargetQuery {
		Iterable<RealtimeBody> candidates(
				float minX,
				float minY,
				float maxX,
				float maxY
		);
	}

	public static final class Hit {
		public RealtimeBody body;
		public float distance;
		public float x;
		public float y;

		public void clear(float maximumDistance) {
			body = null;
			distance = maximumDistance;
			x = 0f;
			y = 0f;
		}
	}

	public static void cast(float originX,
							float originY,
							float directionX,
							float directionY,
							float maximumDistance,
							CollisionMap map,
							TargetQuery targets,
							RealtimeBody ignored,
							Hit out) {
		if (map == null || targets == null || out == null) {
			throw new IllegalArgumentException("map, targets, and out are required");
		}
		if (maximumDistance < 0f) {
			throw new IllegalArgumentException("maximumDistance must not be negative");
		}

		float length = (float)Math.sqrt(
				directionX * directionX + directionY * directionY
		);
		if (length <= 0.00001f) {
			out.clear(0f);
			return;
		}

		float dx = directionX / length;
		float dy = directionY / length;
		float terrainDistance = terrainDistance(
				originX,
				originY,
				dx,
				dy,
				maximumDistance,
				map
		);
		out.clear(terrainDistance);

		float endX = originX + dx * terrainDistance;
		float endY = originY + dy * terrainDistance;
		float minX = Math.min(originX, endX) - 1f;
		float minY = Math.min(originY, endY) - 1f;
		float maxX = Math.max(originX, endX) + 1f;
		float maxY = Math.max(originY, endY) + 1f;

		Iterable<RealtimeBody> candidates = targets.candidates(
				minX,
				minY,
				maxX,
				maxY
		);
		if (candidates != null) {
			for (RealtimeBody body : candidates) {
				if (body == null || body == ignored || !body.active) {
					continue;
				}
				float distance = rayCircle(
						originX,
						originY,
						dx,
						dy,
						body.x,
						body.y,
						body.radius
				);
				if (distance >= 0f && distance < out.distance) {
					out.body = body;
					out.distance = distance;
				}
			}
		}

		out.x = originX + dx * out.distance;
		out.y = originY + dy * out.distance;
	}

	static float terrainDistance(float originX,
								 float originY,
								 float directionX,
								 float directionY,
								 float maximum,
								 CollisionMap map) {
		int cellX = floor(originX);
		int cellY = floor(originY);
		int stepX = directionX < 0f ? -1 : 1;
		int stepY = directionY < 0f ? -1 : 1;
		float nextX = directionX < 0f ? cellX : cellX + 1f;
		float nextY = directionY < 0f ? cellY : cellY + 1f;
		float maxX = directionX == 0f
				? Float.POSITIVE_INFINITY
				: (nextX - originX) / directionX;
		float maxY = directionY == 0f
				? Float.POSITIVE_INFINITY
				: (nextY - originY) / directionY;
		float deltaX = directionX == 0f
				? Float.POSITIVE_INFINITY
				: Math.abs(1f / directionX);
		float deltaY = directionY == 0f
				? Float.POSITIVE_INFINITY
				: Math.abs(1f / directionY);
		float distance = 0f;

		while (distance <= maximum) {
			if (map.blocked(cellX, cellY)) {
				return Math.max(0f, distance);
			}
			if (maxX < maxY) {
				cellX += stepX;
				distance = maxX;
				maxX += deltaX;
			} else {
				cellY += stepY;
				distance = maxY;
				maxY += deltaY;
			}
		}
		return maximum;
	}

	static float rayCircle(float originX,
						   float originY,
						   float directionX,
						   float directionY,
						   float centerX,
						   float centerY,
						   float radius) {
		float toX = centerX - originX;
		float toY = centerY - originY;
		float projection = toX * directionX + toY * directionY;
		if (projection < 0f) {
			return -1f;
		}

		float closestX = originX + projection * directionX;
		float closestY = originY + projection * directionY;
		float distanceX = closestX - centerX;
		float distanceY = closestY - centerY;
		float distanceSquared = distanceX * distanceX + distanceY * distanceY;
		float radiusSquared = radius * radius;
		if (distanceSquared > radiusSquared) {
			return -1f;
		}

		return Math.max(
				0f,
				projection - (float)Math.sqrt(radiusSquared - distanceSquared)
		);
	}

	private static int floor(float value) {
		return (int)Math.floor(value);
	}

	private HitscanResolver() {
	}
}
