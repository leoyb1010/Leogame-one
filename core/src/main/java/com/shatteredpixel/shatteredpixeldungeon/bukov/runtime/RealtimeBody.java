package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

/**
 * Continuous, tile-space body used only by Bukov realtime simulation.
 */
public class RealtimeBody implements Bundlable {

	private static final String X = "x";
	private static final String Y = "y";
	private static final String VX = "vx";
	private static final String VY = "vy";
	private static final String RADIUS = "radius";
	private static final String ACTIVE = "active";

	public float x;
	public float y;
	public float previousX;
	public float previousY;
	public float velocityX;
	public float velocityY;
	public float radius = 0.28f;
	public boolean active = true;

	public RealtimeBody() {
	}

	public RealtimeBody(int cell, int mapWidth, float radius) {
		if (mapWidth <= 0) {
			throw new IllegalArgumentException("mapWidth must be > 0");
		}
		if (radius <= 0f) {
			throw new IllegalArgumentException("radius must be > 0");
		}
		x = cell % mapWidth + 0.5f;
		y = cell / mapWidth + 0.5f;
		previousX = x;
		previousY = y;
		this.radius = radius;
	}

	public void beginStep() {
		previousX = x;
		previousY = y;
	}

	public int cell(int mapWidth) {
		return (int)Math.floor(x) + (int)Math.floor(y) * mapWidth;
	}

	public PointF interpolated(float alpha) {
		float clamped = Math.max(0f, Math.min(1f, alpha));
		return new PointF(
				previousX + (x - previousX) * clamped,
				previousY + (y - previousY) * clamped
		);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(X, x);
		bundle.put(Y, y);
		bundle.put(VX, velocityX);
		bundle.put(VY, velocityY);
		bundle.put(RADIUS, radius);
		bundle.put(ACTIVE, active);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		x = bundle.getFloat(X);
		y = bundle.getFloat(Y);
		previousX = x;
		previousY = y;
		velocityX = bundle.getFloat(VX);
		velocityY = bundle.getFloat(VY);
		radius = bundle.contains(RADIUS) ? bundle.getFloat(RADIUS) : 0.28f;
		active = !bundle.contains(ACTIVE) || bundle.getBoolean(ACTIVE);
	}
}
