package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

/**
 * Restrained, long-lived geometry mark. The finite pool bounds both scene
 * nodes and simultaneous decals; saturation replaces the oldest mark.
 */
public final class BukovBulletMarkFx extends Group {

	public static final float DURATION_SECONDS = 12f;
	static final float FADE_START_SECONDS = 9f;

	private final ColorBlock edge;
	private final ColorBlock hole;
	private final int edgeColor;
	private final int holeColor;
	private float age;

	public BukovBulletMarkFx() {
		this(BukovUiTokens.loadDefault());
	}

	BukovBulletMarkFx(BukovUiTokens tokens) {
		if (tokens == null) {
			throw new IllegalArgumentException("tokens are required");
		}
		int solidColor = color(tokens, "combat.fx.solid");
		edgeColor = color(tokens, "combat.fx.bulletMark.edge");
		holeColor = color(tokens, "combat.fx.bulletMark.hole");
		edge = new ColorBlock(1f, 1f, solidColor);
		hole = new ColorBlock(1f, 1f, solidColor);
		add(edge);
		add(hole);
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
		float strength = clamp(intensity, 0.45f, 1.6f);
		float angle = (float)Math.toDegrees(
				Math.atan2(incomingY, incomingX)) + 90f;
		configure(
				edge,
				impactX,
				impactY,
				2.8f + strength,
				1.5f + strength * 0.35f,
				angle,
				edgeColor);
		configure(
				hole,
				impactX,
				impactY,
				1.45f + strength * 0.55f,
				0.8f + strength * 0.22f,
				angle,
				holeColor);
		age = 0f;
		revive();
		active = true;
		visible = true;
		return true;
	}

	@Override
	public void update() {
		super.update();
		age += Math.max(0f, Game.elapsed);
		float alpha = alphaAt(age);
		edge.alpha(alpha * 0.65f);
		hole.alpha(alpha);
		if (age >= DURATION_SECONDS) {
			retire();
		}
	}

	static float alphaAt(float ageSeconds) {
		if (!finite(ageSeconds) || ageSeconds < 0f
				|| ageSeconds >= DURATION_SECONDS) {
			return 0f;
		}
		if (ageSeconds <= FADE_START_SECONDS) {
			return 1f;
		}
		return clamp(
				(DURATION_SECONDS - ageSeconds)
						/ (DURATION_SECONDS - FADE_START_SECONDS),
				0f,
				1f);
	}

	private static void configure(
			ColorBlock block,
			float centerX,
			float centerY,
			float width,
			float height,
			float angle,
			int color) {
		block.size(width, height);
		block.origin.set(width * 0.5f, height * 0.5f);
		block.x = centerX - width * 0.5f;
		block.y = centerY - height * 0.5f;
		block.angle = angle;
		block.color(color);
		block.alpha(1f);
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

	private static int color(BukovUiTokens tokens, String name) {
		return tokens.colorWithAlpha(name, 255);
	}
}
