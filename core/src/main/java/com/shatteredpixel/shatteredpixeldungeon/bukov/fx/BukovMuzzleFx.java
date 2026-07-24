package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

/**
 * Compact directional muzzle flash for Bukov firearms.
 */
public final class BukovMuzzleFx extends Group {

	public static final float DURATION_SECONDS = 0.12f;
	static final int FRIENDLY_COLOR = 0xFFFFF1A6;

	private final float duration;
	private float age;

	public BukovMuzzleFx(PointF muzzle, PointF direction, boolean hostile, float intensity) {
		duration = DURATION_SECONDS;
		if (muzzle == null || direction == null) {
			visible = false;
			kill();
			return;
		}
		float directionLength = (float) Math.sqrt(
				direction.x * direction.x + direction.y * direction.y);
		if (directionLength <= 0.01f
				|| Float.isNaN(directionLength)
				|| Float.isInfinite(directionLength)) {
			visible = false;
			kill();
			return;
		}
		float angle = (float) Math.toDegrees(Math.atan2(direction.y, direction.x));
		float strength = Math.max(0.45f, Math.min(1.6f, intensity));
		int color = hostile ? BukovTracerFx.HOSTILE_COLOR : FRIENDLY_COLOR;
		add(new FlashRay(muzzle, 4.2f * strength, 1.4f, angle, color));
		add(new FlashRay(muzzle, 2.7f * strength, 0.8f, angle - 24f, color));
		add(new FlashRay(muzzle, 2.7f * strength, 0.8f, angle + 24f, color));
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

	private static final class FlashRay extends ColorBlock {

		private FlashRay(PointF origin, float length, float thickness, float angle, int color) {
			super(length, thickness, color);
			this.origin.set(0f, 0.5f);
			x = origin.x;
			y = origin.y - 0.5f;
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
