package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;

/**
 * Deterministic grid LOS used by realtime enemy perception and contact attacks.
 */
public final class GridLineOfSight {

	public static boolean visible(float fromX,
								  float fromY,
								  float toX,
								  float toY,
								  float maximumDistance,
								  CollisionMap map) {
		if (map == null) {
			throw new IllegalArgumentException("map is required");
		}
		if (maximumDistance < 0f || Float.isNaN(maximumDistance)) {
			throw new IllegalArgumentException(
					"maximumDistance must not be negative"
			);
		}

		float deltaX = toX - fromX;
		float deltaY = toY - fromY;
		if (deltaX * deltaX + deltaY * deltaY
				> maximumDistance * maximumDistance) {
			return false;
		}

		int x = floor(fromX);
		int y = floor(fromY);
		int endX = floor(toX);
		int endY = floor(toY);
		int startX = x;
		int startY = y;
		int width = Math.abs(endX - x);
		int stepX = x < endX ? 1 : -1;
		int height = -Math.abs(endY - y);
		int stepY = y < endY ? 1 : -1;
		int error = width + height;

		while (true) {
			if ((x != startX || y != startY) && map.blocksLine(x, y)) {
				return false;
			}
			if (x == endX && y == endY) {
				return true;
			}
			int doubledError = error * 2;
			if (doubledError >= height) {
				error += height;
				x += stepX;
			}
			if (doubledError <= width) {
				error += width;
				y += stepY;
			}
		}
	}

	private static int floor(float value) {
		return (int)Math.floor(value);
	}

	private GridLineOfSight() {
	}
}
