package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Repairs realtime hero coordinates restored from checkpoints made against an
 * older version of the raid terrain.
 */
final class RealtimeHeroBodyRecovery {

	private RealtimeHeroBodyRecovery() {
	}

	/**
	 * Returns the authoritative hero cell after repair.
	 *
	 * A matching, passable body keeps its sub-tile position. A stale, blocked,
	 * non-finite, out-of-bounds, or cell-mismatched body is snapped to the
	 * nearest passable cell and has all interpolation state synchronized.
	 */
	static int repair(
			RealtimeBody body,
			int heroCell,
			CollisionMap map) {
		if (body == null || map == null) {
			throw new IllegalArgumentException("body and map are required");
		}
		if (map.width() <= 0 || map.height() <= 0) {
			throw new IllegalArgumentException("collision map must not be empty");
		}

		boolean finiteBody = finite(body.x) && finite(body.y);
		int bodyX = finiteBody ? (int)Math.floor(body.x) : -1;
		int bodyY = finiteBody ? (int)Math.floor(body.y) : -1;
		boolean bodyInBounds = inBounds(bodyX, bodyY, map);
		int bodyCell = bodyInBounds ? cell(bodyX, bodyY, map.width()) : -1;
		if (bodyCell == heroCell && !map.blocked(bodyX, bodyY)) {
			return bodyCell;
		}

		int anchorX;
		int anchorY;
		if (bodyInBounds) {
			anchorX = bodyX;
			anchorY = bodyY;
		} else if (validCell(heroCell, map)) {
			anchorX = heroCell % map.width();
			anchorY = heroCell / map.width();
		} else {
			anchorX = map.width() / 2;
			anchorY = map.height() / 2;
		}

		int recoveredCell = nearestPassableCell(anchorX, anchorY, map);
		if (recoveredCell < 0) {
			throw new IllegalStateException(
					"Realtime hero checkpoint has no passable recovery cell");
		}
		snap(body, recoveredCell, map.width());
		return recoveredCell;
	}

	private static int nearestPassableCell(
			int anchorX,
			int anchorY,
			CollisionMap map) {
		int maximumRadius = map.width() + map.height();
		for (int radius = 0; radius <= maximumRadius; radius++) {
			for (int offsetY = -radius; offsetY <= radius; offsetY++) {
				int offsetX = radius - Math.abs(offsetY);
				int left = candidate(
						anchorX - offsetX,
						anchorY + offsetY,
						map);
				if (left >= 0) return left;
				if (offsetX != 0) {
					int right = candidate(
							anchorX + offsetX,
							anchorY + offsetY,
							map);
					if (right >= 0) return right;
				}
			}
		}
		return -1;
	}

	private static int candidate(int x, int y, CollisionMap map) {
		return inBounds(x, y, map) && !map.blocked(x, y)
				? cell(x, y, map.width())
				: -1;
	}

	private static void snap(RealtimeBody body, int cell, int width) {
		body.x = cell % width + 0.5f;
		body.y = cell / width + 0.5f;
		body.previousX = body.x;
		body.previousY = body.y;
		body.velocityX = 0f;
		body.velocityY = 0f;
	}

	private static boolean validCell(int cell, CollisionMap map) {
		return cell >= 0 && cell < map.width() * map.height();
	}

	private static boolean inBounds(int x, int y, CollisionMap map) {
		return x >= 0 && y >= 0 && x < map.width() && y < map.height();
	}

	private static int cell(int x, int y, int width) {
		return x + y * width;
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}
}
