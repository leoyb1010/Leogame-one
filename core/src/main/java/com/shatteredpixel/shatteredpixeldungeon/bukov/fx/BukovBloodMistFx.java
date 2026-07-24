package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

/**
 * Short directional blood spray for a confirmed character hit.
 *
 * All droplets are owned by the slot for its entire lifetime. Resetting a
 * saturated slot only rewinds this presentation and never touches combat.
 */
public final class BukovBloodMistFx extends Group {

	public static final float DURATION_SECONDS = 0.28f;
	static final int BLOOD_DARK = 0xFF5A1517;
	static final int BLOOD_BRIGHT = 0xFFB53B32;

	private static final float[] FORWARD =
			{0.35f, 0.62f, 0.90f, 0.48f, 0.78f, 1.05f, 0.58f};
	private static final float[] SIDE =
			{-0.46f, -0.22f, -0.08f, 0.15f, 0.34f, 0.47f, 0.02f};

	private final Droplet[] droplets = new Droplet[FORWARD.length];
	private float originX;
	private float originY;
	private float directionX;
	private float directionY;
	private float perpendicularX;
	private float perpendicularY;
	private float strength;
	private float age;

	public BukovBloodMistFx() {
		for (int index = 0; index < droplets.length; index++) {
			droplets[index] = new Droplet(index % 3 == 0);
			add(droplets[index]);
		}
		retire();
	}

	boolean reset(
			float impactX,
			float impactY,
			float incomingX,
			float incomingY,
			boolean hostile,
			float intensity) {
		float length = (float)Math.sqrt(
				incomingX * incomingX + incomingY * incomingY);
		if (!finite(impactX) || !finite(impactY)
				|| !finite(length) || length <= 0.01f) {
			retire();
			return false;
		}
		originX = impactX;
		originY = impactY;
		directionX = incomingX / length;
		directionY = incomingY / length;
		perpendicularX = -directionY;
		perpendicularY = directionX;
		strength = clamp(intensity, 0.45f, 1.6f);
		age = 0f;
		place(0f);
		revive();
		active = true;
		visible = true;
		return true;
	}

	@Override
	public void update() {
		super.update();
		age += Math.max(0f, Game.elapsed);
		float progress = clamp(age / DURATION_SECONDS, 0f, 1f);
		place(progress);
		if (age >= DURATION_SECONDS) {
			retire();
		}
	}

	private void place(float progress) {
		float travel = (1f - (1f - progress) * (1f - progress))
				* (6f + strength * 4f);
		float alpha = mistAlphaAt(progress);
		for (int index = 0; index < droplets.length; index++) {
			float forward = travel * FORWARD[index];
			float sideways = travel * SIDE[index];
			float fall = progress * progress * (1.5f + index * 0.18f);
			droplets[index].place(
					originX + directionX * forward
							+ perpendicularX * sideways,
					originY + directionY * forward
							+ perpendicularY * sideways + fall,
					alpha);
		}
	}

	static float mistAlphaAt(float progress) {
		if (!finite(progress) || progress < 0f || progress >= 1f) {
			return 0f;
		}
		if (progress < 0.18f) {
			return progress / 0.18f;
		}
		float fade = (1f - progress) / 0.82f;
		return clamp(fade * fade, 0f, 1f);
	}

	private void retire() {
		alive = false;
		exists = false;
		active = false;
		visible = false;
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	private static final class Droplet extends ColorBlock {

		private Droplet(boolean bright) {
			super(bright ? 2f : 1.5f, bright ? 1.5f : 1f, 0xFFFFFFFF);
			color((bright ? BLOOD_BRIGHT : BLOOD_DARK) & 0xFFFFFF);
			origin.set(width() * 0.5f, height() * 0.5f);
		}

		private void place(float centerX, float centerY, float alpha) {
			x = centerX - width() * 0.5f;
			y = centerY - height() * 0.5f;
			this.alpha(alpha);
		}
	}
}
