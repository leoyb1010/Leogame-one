package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

/**
 * Layered flash, pressure rays and smoke for committed explosion events.
 */
public final class BukovExplosionFx extends Group {

	public static final float DURATION_SECONDS = 0.42f;

	private static final int RAY_COUNT = 12;
	private static final int SMOKE_COUNT = 6;

	private final BlastRay[] rays = new BlastRay[RAY_COUNT];
	private final SmokeBlock[] smoke = new SmokeBlock[SMOKE_COUNT];
	private final int hotColor;
	private final int flameColor;
	private final LightBlock core;
	private float centerX;
	private float centerY;
	private float strength;
	private float age;

	public BukovExplosionFx() {
		this(BukovUiTokens.loadDefault());
	}

	BukovExplosionFx(BukovUiTokens tokens) {
		if (tokens == null) {
			throw new IllegalArgumentException("tokens are required");
		}
		int solidColor = color(tokens, "combat.fx.solid");
		hotColor = color(tokens, "combat.fx.explosion.hot");
		flameColor = color(tokens, "combat.fx.explosion.flame");
		int smokeColor = color(tokens, "combat.fx.explosion.smoke");
		core = new LightBlock(solidColor, hotColor);
		add(core);
		for (int index = 0; index < rays.length; index++) {
			rays[index] = new BlastRay(solidColor);
			add(rays[index]);
		}
		for (int index = 0; index < smoke.length; index++) {
			smoke[index] = new SmokeBlock(solidColor, smokeColor);
			add(smoke[index]);
		}
		retire();
	}

	boolean reset(
			float explosionX,
			float explosionY,
			boolean hostile,
			float intensity) {
		if (!finite(explosionX) || !finite(explosionY)
				|| !finite(intensity)) {
			retire();
			return false;
		}
		centerX = explosionX;
		centerY = explosionY;
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
		float flash = flashAlphaAt(progress);
		float coreSize = (3f + strength * 5f)
				* (0.55f + progress * 0.9f);
		core.configure(centerX, centerY, coreSize, flash);
		for (int index = 0; index < rays.length; index++) {
			float uneven = 0.76f + (index % 4) * 0.09f;
			float length = (4f + strength * 7f)
					* uneven * (0.18f + progress * 0.82f);
			rays[index].configure(
					centerX,
					centerY,
					length,
					0.7f + strength * 0.35f,
					index * (360f / RAY_COUNT),
					index % 3 == 0 ? hotColor : flameColor,
					flash);
		}
		float smokeAlpha = smokeAlphaAt(progress);
		for (int index = 0; index < smoke.length; index++) {
			float angle = (float)Math.toRadians(
					index * (360f / SMOKE_COUNT) + 15f);
			float distance = progress * (2.5f + strength * 3.5f)
					* (0.78f + (index % 3) * 0.12f);
			float size = (2f + strength * 1.8f)
					* (0.65f + progress * 0.7f);
			smoke[index].configure(
					centerX + (float)Math.cos(angle) * distance,
					centerY + (float)Math.sin(angle) * distance
							- progress * 2f,
					size,
					smokeAlpha);
		}
	}

	static float flashAlphaAt(float progress) {
		if (!finite(progress) || progress < 0f || progress >= 1f) {
			return 0f;
		}
		return clamp((1f - progress) * (1f - progress), 0f, 1f);
	}

	static float smokeAlphaAt(float progress) {
		if (!finite(progress) || progress < 0f || progress >= 1f) {
			return 0f;
		}
		float rise = clamp(progress / 0.18f, 0f, 1f);
		return rise * clamp((1f - progress) / 0.72f, 0f, 1f) * 0.58f;
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

	private static final class LightBlock extends ColorBlock {

		private final int hotColor;

		private LightBlock(int solidColor, int hotColor) {
			super(1f, 1f, solidColor);
			this.hotColor = hotColor;
		}

		private void configure(
				float centerX, float centerY, float size, float alpha) {
			size(size, size);
			x = centerX - size * 0.5f;
			y = centerY - size * 0.5f;
			color(hotColor);
			this.alpha(alpha);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static final class BlastRay extends ColorBlock {

		private BlastRay(int solidColor) {
			super(1f, 1f, solidColor);
			origin.set(0f, 0.5f);
		}

		private void configure(
				float centerX,
				float centerY,
				float length,
				float thickness,
				float angle,
				int color,
				float alpha) {
			size(length, thickness);
			x = centerX;
			y = centerY - thickness * 0.5f;
			this.angle = angle;
			color(color);
			this.alpha(alpha);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}

	private static final class SmokeBlock extends ColorBlock {

		private final int smokeColor;

		private SmokeBlock(int solidColor, int smokeColor) {
			super(1f, 1f, solidColor);
			this.smokeColor = smokeColor;
		}

		private void configure(
				float centerX, float centerY, float size, float alpha) {
			size(size, size);
			x = centerX - size * 0.5f;
			y = centerY - size * 0.5f;
			color(smokeColor);
			this.alpha(alpha);
		}
	}

	private static int color(BukovUiTokens tokens, String name) {
		return tokens.colorWithAlpha(name, 255);
	}
}
