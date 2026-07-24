package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

/**
 * Atlas-backed semantic icons for the raid touch overlay.
 *
 * <p>The eight glyphs are project-original 16x16 pixel art generated into the
 * shared Bukov UI atlas. One image replaces the previous per-stroke draw-node
 * tree, while pressed and disabled states remain shape-distinct.</p>
 */
public final class BukovTouchIcon extends Component {

	public enum Glyph {
		MOVEMENT,
		AIM_FIRE,
		INTERACT,
		RELOAD,
		MEDICAL,
		DROP,
		BACKPACK,
		PAUSE,
		MODE,
		VENDOR,
		FILTER,
		SORT,
		SEARCH,
		RECOMMEND,
		DEPLOY,
		BACK
	}

	private final int restingColor;
	private final int pressedColor;
	private final int disabledColor;
	private Image glyphImage;
	private Image disabledStrike;
	private boolean pressed;
	private boolean disabled;

	public BukovTouchIcon(
			Glyph glyph,
			int restingColor,
			int pressedColor,
			int disabledColor) {
		if (glyph == null) {
			throw new IllegalArgumentException("glyph is required");
		}
		this.restingColor = restingColor;
		this.pressedColor = pressedColor;
		this.disabledColor = disabledColor;
		glyphImage = BukovUiAssets.touchGlyph(
				BukovUiAssets.TouchGlyph.valueOf(glyph.name()),
				withFullAlpha(restingColor));
		add(glyphImage);
		disabledStrike = BukovUiAssets.touchDisabledStrike(
				withFullAlpha(disabledColor));
		disabledStrike.visible = false;
		add(disabledStrike);
		visualState(false, false);
	}

	@Override
	protected void createChildren() {
		// Children are built after constructor colors are assigned.
	}

	public void visualState(boolean pressed, boolean disabled) {
		this.pressed = pressed && !disabled;
		this.disabled = disabled;
		int color = disabled
				? disabledColor
				: this.pressed ? pressedColor : restingColor;
		glyphImage.hardlight(color);
		glyphImage.alpha(disabled ? 0.64f : 1f);
		disabledStrike.hardlight(disabledColor);
		disabledStrike.alpha(1f);
		disabledStrike.visible = disabled;
		layout();
	}

	@Override
	protected void layout() {
		if (glyphImage == null || width <= 0f || height <= 0f) {
			return;
		}
		float side = Math.max(1f, Math.min(width, height));
		float scale = PixelScene.align(
				side / BukovUiAssets.TILE_SIZE);
		float renderedSide = BukovUiAssets.TILE_SIZE * scale;
		float left = PixelScene.align(
				x + (width - renderedSide) * 0.5f);
		float top = PixelScene.align(
				y + (height - renderedSide) * 0.5f
						+ (pressed ? 1f : 0f));
		layoutImage(glyphImage, left, top, scale);
		layoutImage(disabledStrike, left, top, scale);
	}

	private static void layoutImage(
			Image image, float left, float top, float scale) {
		image.scale.set(scale);
		image.x = left;
		image.y = top;
	}

	static int atlasColumn(Glyph glyph) {
		if (glyph == null) {
			throw new IllegalArgumentException("glyph is required");
		}
		return 8 + glyph.ordinal();
	}

	static int withFullAlpha(int color) {
		return (255 << 24) | (color & 16_777_215);
	}
}
