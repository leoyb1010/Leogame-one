package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.TextureFilm;

/**
 * Shared animation contract for original Bukov raid combatants.
 *
 * The shared code is intentionally limited to frame timing. Each archetype
 * owns an independent sheet and remains visually distinguishable at 1x.
 */
public abstract class BukovEnemySprite extends MobSprite {

	/**
	 * The authored sheets are intentionally dark industrial pixel art. At the
	 * wide realtime camera scale their original 11x14 opaque silhouettes blend
	 * into the floor even while the host sprite is correctly inside hero FOV.
	 * Keep the source art, but present it as a readable combat contact.
	 */
	public static final float CONTACT_SCALE = 1.20f;
	public static final float CONTACT_LIGHTNESS = 0.64f;
	public static final int CONTACT_COLOR = 0xFFFF6847;

	private final int bloodColor;
	private final ColorBlock contactUnderline =
			new ColorBlock(12f, 1f, CONTACT_COLOR);
	private final ColorBlock contactBracketLeft =
			new ColorBlock(1f, 4f, CONTACT_COLOR);
	private final ColorBlock contactBracketRight =
			new ColorBlock(1f, 4f, CONTACT_COLOR);

	protected BukovEnemySprite(String asset, int bloodColor) {
		super();
		this.bloodColor = bloodColor;

		texture(asset);
		texture.filter(SmartTexture.NEAREST, SmartTexture.NEAREST);
		TextureFilm frames = new TextureFilm(texture, 16, 18);

		idle = new Animation(3, true);
		idle.frames(frames, 0, 0, 1, 0);

		run = new Animation(12, true);
		run.frames(frames, 4, 5, 6, 7);

		attack = new Animation(12, false);
		attack.frames(frames, 2, 3, 0);

		die = new Animation(10, false);
		die.frames(frames, 8, 9, 10);

		scale.set(CONTACT_SCALE, CONTACT_SCALE);
		resetColor();
		play(idle);
	}

	@Override
	public void resetColor() {
		super.resetColor();
		// Adds a neutral lift rather than a faction tint, so every dedicated
		// archetype palette remains distinguishable. Super preserves the 0.4
		// alpha used by a real invisibility state; normal contacts stay opaque.
		lightness(CONTACT_LIGHTNESS);
	}

	@Override
	public void draw() {
		super.draw();
		drawContactMarker(contactUnderline,
				x + (width() - 12f) * 0.5f,
				y + height() - 1.5f);
		float bracketY = y + Math.max(3f, height() * 0.30f);
		drawContactMarker(contactBracketLeft, x - 1.5f, bracketY);
		drawContactMarker(contactBracketRight, x + width() + 0.5f, bracketY);
	}

	private void drawContactMarker(ColorBlock marker, float markerX, float markerY) {
		marker.camera = camera();
		marker.x = markerX;
		marker.y = markerY;
		marker.alpha(alpha());
		marker.draw();
	}

	@Override
	public void destroy() {
		contactUnderline.destroy();
		contactBracketLeft.destroy();
		contactBracketRight.destroy();
		super.destroy();
	}

	@Override
	public int blood() {
		return bloodColor;
	}
}
