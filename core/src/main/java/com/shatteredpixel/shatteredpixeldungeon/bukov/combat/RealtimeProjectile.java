package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;

public final class RealtimeProjectile {

	public interface Listener {
		boolean hitTarget(RealtimeProjectile projectile,
						  float fromX,
						  float fromY,
						  float toX,
						  float toY);

		void hitTerrain(RealtimeProjectile projectile, float x, float y);
	}

	public float x;
	public float y;
	public float velocityX;
	public float velocityY;
	public float radius;
	public float remainingLife;
	public boolean active;

	public void launch(float x,
					   float y,
					   float velocityX,
					   float velocityY,
					   float radius,
					   float lifeSeconds) {
		if (radius < 0f || lifeSeconds <= 0f) {
			throw new IllegalArgumentException(
					"radius must not be negative and lifeSeconds must be positive"
			);
		}
		this.x = x;
		this.y = y;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.radius = radius;
		remainingLife = lifeSeconds;
		active = true;
	}

	public void update(float dt, CollisionMap map, Listener listener) {
		if (!active) {
			return;
		}
		if (dt < 0f) {
			throw new IllegalArgumentException("dt must not be negative");
		}
		if (map == null || listener == null) {
			throw new IllegalArgumentException("map and listener are required");
		}

		float speed = (float)Math.sqrt(
				velocityX * velocityX + velocityY * velocityY
		);
		int steps = Math.max(1, (int)Math.ceil(speed * dt / 0.35f));
		float stepDt = dt / steps;

		for (int i = 0; i < steps && active; i++) {
			float nextX = x + velocityX * stepDt;
			float nextY = y + velocityY * stepDt;
			if (map.blocksLine(
					(int)Math.floor(nextX),
					(int)Math.floor(nextY))) {
				active = false;
				listener.hitTerrain(this, nextX, nextY);
			} else if (listener.hitTarget(this, x, y, nextX, nextY)) {
				active = false;
			} else {
				x = nextX;
				y = nextY;
			}
		}

		remainingLife -= dt;
		if (remainingLife <= 0f) {
			active = false;
		}
	}
}
