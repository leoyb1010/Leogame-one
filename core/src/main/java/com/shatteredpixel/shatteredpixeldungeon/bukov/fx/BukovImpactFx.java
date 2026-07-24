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

	private final float duration;
	private float age;

	public BukovImpactFx(PointF impact, boolean hostile, float intensity) {
		duration = DURATION_SECONDS;
		if (impact == null
				|| Float.isNaN(impact.x) || Float.isInfinite(impact.x)
				|| Float.isNaN(impact.y) || Float.isInfinite(impact.y)) {
			visible = false;
			kill();
			return;
		}
		float strength = Math.max(0.45f, Math.min(1.6f, intensity));
		int color = hostile ? BukovTracerFx.HOSTILE_COLOR : 0xFFF0B2;
		float rayLength = 2.2f + strength * 1.7f;
		for (int index = 0; index < 4; index++) {
			add(new SparkRay(impact, rayLength, 0.75f, index * 90f + 45f, color));
		}
		add(new SparkRay(impact, 1.6f + strength, 1.6f + strength, 0f, color));
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
			killAndErase();
		}
	}

	private static final class SparkRay extends ColorBlock {

		private SparkRay(PointF impact,
						 float length,
						 float thickness,
						 float angle,
						 int color) {
			super(length, thickness, color);
			origin.set(0f, 0.5f);
			x = impact.x;
			y = impact.y - 0.5f;
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
