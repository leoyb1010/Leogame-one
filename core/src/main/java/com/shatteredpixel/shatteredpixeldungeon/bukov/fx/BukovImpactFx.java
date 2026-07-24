package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * Deterministic hit spark placed at the tracer endpoint.
 */
public final class BukovImpactFx extends Group {

	public static final float DURATION_SECONDS = 0.14f;
	static final int FRIENDLY_COLOR = 0xFFFFF0B2;

	private final float duration = DURATION_SECONDS;
	private final SparkRay[] rays = new SparkRay[5];
	private float age;

	public BukovImpactFx() {
		for (int index = 0; index < rays.length; index++) {
			rays[index] = new SparkRay();
			add(rays[index]);
		}
		retire();
	}

	public BukovImpactFx(PointF impact, boolean hostile, float intensity) {
		this();
		reset(
				impact == null ? Float.NaN : impact.x,
				impact == null ? Float.NaN : impact.y,
				hostile,
				intensity);
	}

	boolean reset(
			float impactX,
			float impactY,
			boolean hostile,
			float intensity) {
		if (Float.isNaN(impactX) || Float.isInfinite(impactX)
				|| Float.isNaN(impactY) || Float.isInfinite(impactY)) {
			retire();
			return false;
		}
		float strength = Math.max(0.45f, Math.min(1.6f, intensity));
		int color = hostile ? BukovTracerFx.HOSTILE_COLOR : FRIENDLY_COLOR;
		float rayLength = 2.2f + strength * 1.7f;
		for (int index = 0; index < 4; index++) {
			rays[index].configure(
					impactX,
					impactY,
					rayLength,
					0.75f,
					index * 90f + 45f,
					color);
		}
		rays[4].configure(
				impactX,
				impactY,
				1.6f + strength,
				1.6f + strength,
				0f,
				color);
		age = 0f;
		revive();
		active = true;
		visible = true;
		return true;
	}

	@Override
	public void update() {
		super.update();
		age += Game.elapsed;
		float alpha = BukovTracerFx.alphaAt(age, duration);
		for (int index = 0; index < length; index++) {
			com.watabou.noosa.Gizmo child = members.get(index);
			if (child instanceof ColorBlock) {
				((ColorBlock) child).alpha(alpha);
			}
		}
		if (BukovTracerFx.expiredAt(age, duration)) {
			retire();
		}
	}

	private void retire() {
		alive = false;
		exists = false;
		active = false;
		visible = false;
	}

	private static final class SparkRay extends ColorBlock {

		private SparkRay() {
			super(1f, 1f, 0xFFFFFFFF);
			origin.set(0f, 0.5f);
		}

		private void configure(
				float impactX,
				float impactY,
				float length,
				float thickness,
				float angle,
				int color) {
			size(length, thickness);
			color(color & 0xFFFFFF);
			alpha(1f);
			x = impactX;
			y = impactY - 0.5f;
			this.angle = angle;
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
