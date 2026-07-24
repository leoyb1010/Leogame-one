package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Renderer-independent geometry and copy fitting for the raid HUD.
 *
 * <p>The portrait HUD used to place extraction and mission copy on the same
 * baseline while allowing both blocks to wrap without a vertical limit. That
 * made the text overlap the backpack/pause touch targets on narrow iPhones.
 * This model gives every priority group an exclusive band and keeps the
 * renderer-specific component responsible only for applying the result.</p>
 */
public final class BukovRaidHudLayout {

	public static final float WIDE_THRESHOLD = 220f;
	public static final float RELOAD_RING_SIZE = 24f;
	private static final float WIDE_HEIGHT = 46f;
	private static final float COMPACT_HEIGHT = 90f;
	private static final float MOBILE_FEEDBACK_SIDE_MARGIN = 6f;
	private static final float MOBILE_FEEDBACK_HEIGHT = 13f;
	private static final float TUTORIAL_HINT_HEIGHT = 28f;

	public static final class Rect {
		public final float x;
		public final float y;
		public final float width;
		public final float height;

		private Rect(float x, float y, float width, float height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public float right() {
			return x + width;
		}

		public float bottom() {
			return y + height;
		}

		public boolean overlaps(Rect other) {
			return x < other.right() && right() > other.x
					&& y < other.bottom() && bottom() > other.y;
		}
	}

	public final boolean compact;
	public final float height;
	public final Rect vitals;
	public final Rect firepower;
	public final Rect vitalPrimary;
	public final Rect vitalSecondary;
	public final Rect firepowerPrimary;
	public final Rect firepowerSecondary;
	public final Rect condition;
	public final Rect medicalHint;
	public final Rect clock;
	public final Rect extraction;
	public final Rect objective;

	private BukovRaidHudLayout(
			boolean compact,
			float height,
			Rect vitals,
			Rect firepower,
			Rect vitalPrimary,
			Rect vitalSecondary,
			Rect firepowerPrimary,
			Rect firepowerSecondary,
			Rect condition,
			Rect medicalHint,
			Rect clock,
			Rect extraction,
			Rect objective) {
		this.compact = compact;
		this.height = height;
		this.vitals = vitals;
		this.firepower = firepower;
		this.vitalPrimary = vitalPrimary;
		this.vitalSecondary = vitalSecondary;
		this.firepowerPrimary = firepowerPrimary;
		this.firepowerSecondary = firepowerSecondary;
		this.condition = condition;
		this.medicalHint = medicalHint;
		this.clock = clock;
		this.extraction = extraction;
		this.objective = objective;
	}

	public static BukovRaidHudLayout calculate(
			float availableWidth, int scaleLevel) {
		return calculate(
				availableWidth,
				availableWidth < WIDE_THRESHOLD
						? availableWidth * 2f : availableWidth,
				scaleLevel);
	}

	public static BukovRaidHudLayout calculate(
			float availableWidth,
			float viewportHeight,
			int scaleLevel) {
		if (availableWidth <= 0f) {
			throw new IllegalArgumentException(
					"availableWidth must be positive");
		}
		if (viewportHeight <= 0f) {
			throw new IllegalArgumentException(
					"viewportHeight must be positive");
		}
		float scale = scaleMultiplier(scaleLevel);
		boolean compact = isCompact(availableWidth, viewportHeight);
		// The wide renderer has fixed row geometry. Growing only its background
		// consumed most of a compact iPhone landscape without enlarging any
		// content, and pushed the touch controls underneath the HUD.
		float height = compact ? COMPACT_HEIGHT * scale : WIDE_HEIGHT;
		float padding = 4f * scale;
		float contentWidth = Math.max(1f, availableWidth - padding * 2f);
		float halfGap = 4f * scale;
		float columnWidth = Math.max(
				1f, (contentWidth - halfGap) * 0.5f);
		float rightX = padding + columnWidth + halfGap;

		if (compact) {
			return new BukovRaidHudLayout(
					true,
					height,
					rect(padding, 2f * scale, columnWidth, 26f * scale),
					rect(rightX, 2f * scale, columnWidth, 26f * scale),
					rect(padding, 2f * scale, columnWidth, 11f * scale),
					rect(padding, 18f * scale, columnWidth, 9f * scale),
					rect(rightX, 2f * scale, columnWidth, 11f * scale),
					rect(rightX, 17f * scale, columnWidth, 10f * scale),
					rect(padding, 30f * scale, contentWidth, 9f * scale),
					rect(padding + contentWidth, 30f * scale, 0f, 9f * scale),
					rect(padding, 41f * scale, contentWidth, 9f * scale),
					rect(padding, 52f * scale, contentWidth, 9f * scale),
					rect(padding, 63f * scale, contentWidth, 24f * scale)
			);
		}

		float leftWidth = Math.min(88f * scale, availableWidth * 0.28f);
		float rightWidth = Math.min(78f * scale, availableWidth * 0.24f);
		float centerX = leftWidth + padding;
		float centerWidth = Math.max(
				1f,
				availableWidth - rightWidth - padding - centerX);
		return new BukovRaidHudLayout(
				false,
				height,
				rect(padding, 2f * scale,
						Math.max(1f, leftWidth - padding), 33f * scale),
				rect(availableWidth - rightWidth, 2f * scale,
						Math.max(1f, rightWidth - padding), 33f * scale),
				rect(padding, 2f * scale,
						Math.max(1f, leftWidth - padding), 12f * scale),
				rect(padding, 19f * scale,
						Math.max(1f, leftWidth - padding), 9f * scale),
				rect(availableWidth - rightWidth, 2f * scale,
						Math.max(1f, rightWidth - padding), 12f * scale),
				rect(availableWidth - rightWidth, 16f * scale,
						Math.max(1f, rightWidth - padding), 9f * scale),
				rect(padding, 27f * scale,
						Math.max(1f, leftWidth - padding), 8f * scale),
				rect(padding, 36f * scale,
						Math.max(1f, leftWidth - padding), 8f * scale),
				rect(availableWidth - rightWidth, 24f * scale,
						Math.max(1f, rightWidth - padding), 10f * scale),
				rect(centerX, 13f * scale, centerWidth, 9f * scale),
				rect(centerX, 2f * scale, centerWidth, 9f * scale)
		);
	}

	public static float preferredHeight(
			float availableWidth, int scaleLevel) {
		return calculate(availableWidth, scaleLevel).height;
	}

	public static float preferredHeight(
			float availableWidth,
			float viewportHeight,
			int scaleLevel) {
		return calculate(
				availableWidth,
				viewportHeight,
				scaleLevel).height;
	}

	/** v2 firepower ring: fixed 24x24 and contained by the compact band. */
	public static Rect compactReloadRing(
			float availableWidth, int scaleLevel) {
		return compactReloadRing(
				availableWidth,
				availableWidth * 2f,
				scaleLevel);
	}

	public static Rect compactReloadRing(
			float availableWidth,
			float viewportHeight,
			int scaleLevel) {
		BukovRaidHudLayout layout =
				calculate(
						availableWidth,
						viewportHeight,
						scaleLevel);
		Rect firepower = layout.firepower;
		float x = firepower.right() - RELOAD_RING_SIZE;
		float y = firepower.y
				+ Math.max(0f,
						(firepower.height - RELOAD_RING_SIZE) * 0.5f);
		return rect(
				x,
				y,
				RELOAD_RING_SIZE,
				RELOAD_RING_SIZE);
	}

	/**
	 * Keeps the single high-priority mobile interaction prompt in the empty
	 * left rail below the HUD. Backpack/pause own the right side of this row;
	 * directional sound and damage remain represented by their world-space
	 * arcs instead of additional text slabs.
	 */
	public static Rect mobileFeedback(
			float viewportWidth,
			float viewportHeight,
			float hudLeft,
			float hudBottom) {
		if (viewportWidth <= 0f || viewportHeight <= 0f) {
			throw new IllegalArgumentException(
					"viewport dimensions must be positive");
		}
		float width = Math.min(
				96f,
				Math.max(1f,
						viewportWidth * 0.34f));
		float x = Math.min(
				Math.max(
						MOBILE_FEEDBACK_SIDE_MARGIN,
						Math.max(0f, hudLeft)),
				Math.max(0f, viewportWidth - width));
		float y = Math.max(
				0f,
				Math.min(
						hudBottom + 4f,
						Math.max(
								0f,
								viewportHeight
										- MOBILE_FEEDBACK_HEIGHT
										- 4f)));
		return rect(
				x,
				y,
				Math.min(width, viewportWidth - x),
				MOBILE_FEEDBACK_HEIGHT);
	}

	/**
	 * The portrait tutorial occupies a dedicated row below backpack/pause and
	 * the interaction rail. It must never share a baseline with either one.
	 */
	public static Rect portraitTutorialHint(
			float viewportWidth,
			float viewportHeight,
			float hudBottom,
			int scaleLevel) {
		if (viewportWidth <= 0f || viewportHeight <= 0f) {
			throw new IllegalArgumentException(
					"viewport dimensions must be positive");
		}
		float width = Math.min(
				190f * scaleMultiplier(scaleLevel),
				Math.max(1f, viewportWidth - 16f));
		float x = Math.max(0f, (viewportWidth - width) * 0.5f);
		// Backpack/pause can be 24px tall and begin two pixels below the HUD.
		// Reserve that complete row plus a four-pixel breathing gap.
		float y = clamp(
				hudBottom + 30f,
				hudBottom,
				Math.max(
						hudBottom,
						viewportHeight - TUTORIAL_HINT_HEIGHT - 4f));
		return rect(x, y, width, TUTORIAL_HINT_HEIGHT);
	}

	public static float scaleMultiplier(int scaleLevel) {
		return 1f + clampScaleLevel(scaleLevel) * 0.25f;
	}

	public static boolean isCompact(
			float availableWidth, float viewportHeight) {
		return availableWidth < WIDE_THRESHOLD
				|| viewportHeight > availableWidth * 1.15f;
	}

	/**
	 * Fits dynamic mission copy to at most two narrow-screen lines. The full
	 * text remains available in the task/interaction surfaces; this is only the
	 * always-on combat summary.
	 */
	public static String compactObjective(
			String value, float availableWidth, int scaleLevel) {
		String normalized = normalize(value);
		float glyphWidth = 6.4f + clampScaleLevel(scaleLevel) * 0.8f;
		int perLine = Math.max(
				10, (int)Math.floor(Math.max(1f, availableWidth) / glyphWidth));
		return ellipsize(normalized, Math.min(64, perLine * 2));
	}

	/** Keeps the status/weapon priority rows to one line on narrow screens. */
	public static String compactLine(
			String value, float availableWidth, int scaleLevel) {
		String normalized = normalize(value);
		float glyphWidth = 6.2f + clampScaleLevel(scaleLevel) * 0.9f;
		int limit = Math.max(
				6, (int)Math.floor(Math.max(1f, availableWidth) / glyphWidth));
		return ellipsize(normalized, Math.min(32, limit));
	}

	/** One-line fitting for the larger HP/ammunition typography. */
	public static String compactPrimaryLine(
			String value, float availableWidth, int scaleLevel) {
		String normalized = normalize(value);
		float glyphWidth = 5.5f + clampScaleLevel(scaleLevel) * 0.5f;
		int limit = Math.max(
				5, (int)Math.floor(Math.max(1f, availableWidth) / glyphWidth));
		return ellipsize(normalized, Math.min(24, limit));
	}

	private static String normalize(String value) {
		if (value == null) return "";
		return value.trim().replaceAll("\\s+", " ");
	}

	private static String ellipsize(String value, int maximumCodePoints) {
		int count = value.codePointCount(0, value.length());
		if (count <= maximumCodePoints) return value;
		int keep = Math.max(1, maximumCodePoints - 1);
		int end = value.offsetByCodePoints(0, keep);
		return value.substring(0, end).trim() + "…";
	}

	private static Rect rect(
			float x, float y, float width, float height) {
		return new Rect(x, y, width, height);
	}

	private static float clamp(
			float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static int clampScaleLevel(int scaleLevel) {
		return Math.max(0, Math.min(2, scaleLevel));
	}

	private BukovRaidHudLayout() {
		throw new AssertionError();
	}
}
