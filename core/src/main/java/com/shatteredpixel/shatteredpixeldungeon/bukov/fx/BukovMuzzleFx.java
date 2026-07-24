package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
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
	private static final BukovUiTokens DEFAULT_TOKENS =
			BukovUiTokens.loadDefault();
	static final int FRIENDLY_COLOR = color(
			DEFAULT_TOKENS, "combat.fx.muzzle.friendly");

	private final float duration = DURATION_SECONDS;
	private final int friendlyColor;
	private final int hostileColor;
	private final FlashRay center;
	private final FlashRay lower;
	private final FlashRay upper;
	private float age;

	public BukovMuzzleFx() {
		this(DEFAULT_TOKENS);
	}

	BukovMuzzleFx(BukovUiTokens tokens) {
		if (tokens == null) {
			throw new IllegalArgumentException("tokens are required");
		}
		friendlyColor = color(tokens, "combat.fx.muzzle.friendly");
		hostileColor = color(tokens, "combat.fx.tracer.hostile");
		int solidColor = color(tokens, "combat.fx.solid");
		center = new FlashRay(solidColor);
		add(center);
		lower = new FlashRay(solidColor);
		add(lower);
		upper = new FlashRay(solidColor);
		add(upper);
		retire();
	}

	public BukovMuzzleFx(PointF muzzle, PointF direction, boolean hostile, float intensity) {
		this();
		reset(
				muzzle == null ? Float.NaN : muzzle.x,
				muzzle == null ? Float.NaN : muzzle.y,
				direction == null ? Float.NaN : direction.x,
				direction == null ? Float.NaN : direction.y,
				hostile,
				intensity);
	}

	boolean reset(
			float muzzleX,
			float muzzleY,
			float directionX,
			float directionY,
			boolean hostile,
			float intensity) {
		float directionLength = (float) Math.sqrt(
				directionX * directionX + directionY * directionY);
		if (directionLength <= 0.01f
				|| Float.isNaN(directionLength)
				|| Float.isInfinite(directionLength)
				|| Float.isNaN(muzzleX) || Float.isInfinite(muzzleX)
				|| Float.isNaN(muzzleY) || Float.isInfinite(muzzleY)) {
			retire();
			return false;
		}
		float angle = (float) Math.toDegrees(Math.atan2(directionY, directionX));
		float strength = Math.max(0.45f, Math.min(1.6f, intensity));
		int color = hostile ? hostileColor : friendlyColor;
		center.configure(muzzleX, muzzleY, 4.2f * strength, 1.4f, angle, color);
		lower.configure(muzzleX, muzzleY, 2.7f * strength, 0.8f, angle - 24f, color);
		upper.configure(muzzleX, muzzleY, 2.7f * strength, 0.8f, angle + 24f, color);
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

	private static final class FlashRay extends ColorBlock {

		private FlashRay(int solidColor) {
			super(1f, 1f, solidColor);
			this.origin.set(0f, 0.5f);
		}

		private void configure(
				float originX,
				float originY,
				float length,
				float thickness,
				float angle,
				int color) {
			size(length, thickness);
			color(color);
			alpha(1f);
			x = originX;
			y = originY - 0.5f;
			this.angle = angle;
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static int color(BukovUiTokens tokens, String name) {
		return tokens.colorWithAlpha(name, 255);
	}
}
