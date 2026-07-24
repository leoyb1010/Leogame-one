package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

/**
 * Small programmatic pixel-line icons for the raid touch overlay.
 *
 * <p>The controls must remain readable without a system symbol font or a
 * colour-only cue. Every glyph is assembled from solid pixel strokes; pressed
 * state moves the complete drawing down one pixel, while disabled state adds a
 * visible diagonal strike.</p>
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
		PAUSE
	}

	private static final float GRID = 16f;
	private static final Stroke[] DISABLED_STRIKE = {
			stroke(2, 2, 2, 2),
			stroke(4, 4, 2, 2),
			stroke(6, 6, 2, 2),
			stroke(8, 8, 2, 2),
			stroke(10, 10, 2, 2),
			stroke(12, 12, 2, 2)
	};
	private static final Stroke[][] BLUEPRINTS = {
			// MOVEMENT: a four-direction pad.
			{
					stroke(7, 1, 2, 10),
					stroke(5, 3, 2, 2),
					stroke(9, 3, 2, 2),
					stroke(7, 5, 2, 10),
					stroke(5, 11, 2, 2),
					stroke(9, 11, 2, 2),
					stroke(1, 7, 10, 2),
					stroke(3, 5, 2, 2),
					stroke(3, 9, 2, 2),
					stroke(5, 7, 10, 2),
					stroke(11, 5, 2, 2),
					stroke(11, 9, 2, 2)
			},
			// AIM_FIRE: crosshair, centre point, and four broken corners.
			{
					stroke(7, 1, 2, 4),
					stroke(7, 11, 2, 4),
					stroke(1, 7, 4, 2),
					stroke(11, 7, 4, 2),
					stroke(7, 7, 2, 2),
					stroke(4, 4, 3, 1),
					stroke(4, 5, 1, 2),
					stroke(9, 4, 3, 1),
					stroke(11, 5, 1, 2),
					stroke(4, 11, 3, 1),
					stroke(4, 9, 1, 2),
					stroke(9, 11, 3, 1),
					stroke(11, 9, 1, 2)
			},
			// INTERACT: a tap target with three outward response marks.
			{
					stroke(6, 6, 4, 4),
					stroke(7, 2, 2, 3),
					stroke(2, 7, 3, 2),
					stroke(11, 7, 3, 2),
					stroke(4, 4, 2, 1),
					stroke(3, 3, 1, 1),
					stroke(10, 4, 2, 1),
					stroke(12, 3, 1, 1),
					stroke(7, 11, 2, 3)
			},
			// RELOAD: a broken circular arrow around a magazine.
			{
					stroke(4, 2, 7, 2),
					stroke(2, 4, 2, 6),
					stroke(4, 10, 3, 2),
					stroke(10, 4, 2, 3),
					stroke(9, 2, 4, 2),
					stroke(11, 1, 2, 5),
					stroke(12, 4, 2, 2),
					stroke(7, 7, 3, 6),
					stroke(8, 8, 1, 3),
					stroke(7, 12, 3, 2)
			},
			// MEDICAL: unmistakable high-contrast first-aid cross.
			{
					stroke(6, 2, 4, 12),
					stroke(2, 6, 12, 4),
					stroke(4, 4, 2, 2),
					stroke(10, 4, 2, 2),
					stroke(4, 10, 2, 2),
					stroke(10, 10, 2, 2)
			},
			// DROP: downward arrow entering an open container.
			{
					stroke(7, 1, 2, 7),
					stroke(4, 6, 3, 2),
					stroke(9, 6, 3, 2),
					stroke(6, 8, 4, 2),
					stroke(2, 10, 2, 4),
					stroke(12, 10, 2, 4),
					stroke(2, 13, 12, 2),
					stroke(4, 10, 2, 1),
					stroke(10, 10, 2, 1)
			},
			// BACKPACK: lid, body, pocket, and two shoulder straps.
			{
					stroke(5, 1, 6, 2),
					stroke(3, 3, 10, 2),
					stroke(2, 5, 2, 9),
					stroke(12, 5, 2, 9),
					stroke(3, 13, 10, 2),
					stroke(5, 7, 6, 1),
					stroke(5, 10, 6, 3),
					stroke(6, 11, 4, 1),
					stroke(1, 6, 1, 5),
					stroke(14, 6, 1, 5)
			},
			// PAUSE: two bars in a persistent outer bracket.
			{
					stroke(4, 3, 3, 10),
					stroke(9, 3, 3, 10),
					stroke(2, 1, 12, 1),
					stroke(2, 14, 12, 1),
					stroke(1, 2, 1, 3),
					stroke(14, 2, 1, 3)
			}
	};

	private final Stroke[] blueprint;
	private final ColorBlock[] strokes;
	private final ColorBlock[] disabledStrike;
	private final int restingColor;
	private final int pressedColor;
	private final int disabledColor;
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
		this.blueprint = blueprint(glyph);
		this.restingColor = restingColor;
		this.pressedColor = pressedColor;
		this.disabledColor = disabledColor;
		this.strokes = new ColorBlock[blueprint.length];
		this.disabledStrike = new ColorBlock[DISABLED_STRIKE.length];
		buildChildren();
	}

	@Override
	protected void createChildren() {
		// Built after constructor arguments are assigned.
	}

	private void buildChildren() {
		for (int index = 0; index < blueprint.length; index++) {
			strokes[index] = new ColorBlock(
					1f, 1f, withFullAlpha(restingColor));
			add(strokes[index]);
		}
		for (int index = 0; index < DISABLED_STRIKE.length; index++) {
			disabledStrike[index] = new ColorBlock(
					1f, 1f, withFullAlpha(disabledColor));
			disabledStrike[index].visible = false;
			add(disabledStrike[index]);
		}
	}

	public void visualState(boolean pressed, boolean disabled) {
		this.pressed = pressed && !disabled;
		this.disabled = disabled;
		int color = disabled
				? disabledColor
				: this.pressed ? pressedColor : restingColor;
		for (ColorBlock block : strokes) {
			block.hardlight(color);
			block.alpha(disabled ? 0.64f : 1f);
		}
		for (ColorBlock block : disabledStrike) {
			block.hardlight(disabledColor);
			block.alpha(1f);
			block.visible = disabled;
		}
		layout();
	}

	@Override
	protected void layout() {
		if (strokes == null || width <= 0f || height <= 0f) {
			return;
		}
		float side = Math.max(1f, Math.min(width, height));
		float scale = side / GRID;
		float left = x + (width - side) * 0.5f;
		float top = y + (height - side) * 0.5f + (pressed ? 1f : 0f);
		layoutStrokes(strokes, blueprint, left, top, scale);
		layoutStrokes(disabledStrike, DISABLED_STRIKE, left, top, scale);
	}

	private static void layoutStrokes(
			ColorBlock[] blocks,
			Stroke[] blueprint,
			float left,
			float top,
			float scale) {
		for (int index = 0; index < blocks.length; index++) {
			Stroke stroke = blueprint[index];
			ColorBlock block = blocks[index];
			block.x = pixel(left + stroke.x * scale);
			block.y = pixel(top + stroke.y * scale);
			block.size(
					Math.max(0.75f, pixel(stroke.width * scale)),
					Math.max(0.75f, pixel(stroke.height * scale)));
		}
	}

	static int strokeCount(Glyph glyph) {
		return blueprint(glyph).length;
	}

	static String fingerprint(Glyph glyph) {
		StringBuilder result = new StringBuilder();
		for (Stroke stroke : blueprint(glyph)) {
			result.append(stroke.x).append(',')
					.append(stroke.y).append(',')
					.append(stroke.width).append(',')
					.append(stroke.height).append(';');
		}
		return result.toString();
	}

	static int disabledStrikeCount() {
		return DISABLED_STRIKE.length;
	}

	private static Stroke[] blueprint(Glyph glyph) {
		return BLUEPRINTS[glyph.ordinal()];
	}

	private static float pixel(float value) {
		return Math.round(value * 2f) * 0.5f;
	}

	static int withFullAlpha(int color) {
		return (255 << 24) | (color & 16_777_215);
	}

	private static Stroke stroke(
			float x,
			float y,
			float width,
			float height) {
		return new Stroke(x, y, width, height);
	}

	private static final class Stroke {
		final float x;
		final float y;
		final float width;
		final float height;

		Stroke(float x, float y, float width, float height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}
}
