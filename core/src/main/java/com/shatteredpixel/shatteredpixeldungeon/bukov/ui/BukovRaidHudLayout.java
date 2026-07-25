package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import java.util.regex.Pattern;

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

	private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");
	public static final float WIDE_THRESHOLD = 220f;
	public static final float RELOAD_RING_SIZE = 24f;
	public static final float DESKTOP_PAUSE_WIDTH = 48f;
	public static final float DESKTOP_PAUSE_HEIGHT = 18f;
	public static final float DESKTOP_PAUSE_RIGHT_MARGIN = 4f;
	public static final float DESKTOP_HUD_PAUSE_GAP = 4f;
	public static final float HUD_SIDE_INSET = 4f;
	private static final float WIDE_HEIGHT = 46f;
	private static final float COMPACT_HEIGHT = 90f;
	private static final float MOBILE_FEEDBACK_SIDE_MARGIN = 6f;
	private static final float MOBILE_FEEDBACK_HEIGHT = 13f;
	private static final float TUTORIAL_HINT_HEIGHT = 28f;
	private static final int CAPTION_FONT_PX = 7;
	private static final int BODY_FONT_PX = 8;

	/**
	 * Conservative renderer-free footprint used by both production fitting and
	 * layout gates. CJK, Latin, digits and punctuation intentionally have
	 * different advances; UI scale adds a small safety allowance rather than
	 * pretending that every code point occupies one fixed cell.
	 */
	public static final class TextFootprint {
		public final float width;
		public final float height;
		public final int lines;

		private TextFootprint(float width, float height, int lines) {
			this.width = width;
			this.height = height;
			this.lines = lines;
		}

		public boolean fits(
				float availableWidth,
				float availableHeight,
				int maximumLines) {
			return width <= availableWidth
					&& height <= availableHeight
					&& lines <= maximumLines;
		}
	}

	/**
	 * Allocation-free cache hit for one rendered HUD row. A row owns one cache
	 * and invalidates only when its source copy, measured width, scale or
	 * typography contract changes.
	 */
	public static final class FitCache {
		private static final int OBJECTIVE = 0;
		private static final int CAPTION = 1;
		private static final int BODY = 2;
		private static final int PRIMARY = 3;

		private String source;
		private int widthBits;
		private int scaleLevel = Integer.MIN_VALUE;
		private int contract = Integer.MIN_VALUE;
		private String fitted;
		private int recomputations;

		public String objective(
				String value, float availableWidth, int uiScaleLevel) {
			return fit(value, availableWidth, uiScaleLevel, OBJECTIVE);
		}

		public String captionLine(
				String value, float availableWidth, int uiScaleLevel) {
			return fit(value, availableWidth, uiScaleLevel, CAPTION);
		}

		public String bodyLine(
				String value, float availableWidth, int uiScaleLevel) {
			return fit(value, availableWidth, uiScaleLevel, BODY);
		}

		public String primaryLine(
				String value, float availableWidth, int uiScaleLevel) {
			return fit(value, availableWidth, uiScaleLevel, PRIMARY);
		}

		public int recomputations() {
			return recomputations;
		}

		private String fit(
				String value,
				float availableWidth,
				int uiScaleLevel,
				int requestedContract) {
			int requestedWidthBits =
					Float.floatToIntBits(Math.max(1f, availableWidth));
			int requestedScale = clampScaleLevel(uiScaleLevel);
			if (fitted != null
					&& same(source, value)
					&& widthBits == requestedWidthBits
					&& scaleLevel == requestedScale
					&& contract == requestedContract) {
				return fitted;
			}
			source = value;
			widthBits = requestedWidthBits;
			scaleLevel = requestedScale;
			contract = requestedContract;
			float width = Float.intBitsToFloat(requestedWidthBits);
			switch (requestedContract) {
				case OBJECTIVE:
					fitted = compactObjective(value, width, requestedScale);
					break;
				case BODY:
					fitted = compactBodyLine(value, width, requestedScale);
					break;
				case PRIMARY:
					fitted = compactPrimaryLine(value, width, requestedScale);
					break;
				case CAPTION:
				default:
					fitted = compactLine(value, width, requestedScale);
					break;
			}
			recomputations++;
			return fitted;
		}

		private static boolean same(String first, String second) {
			return first == second
					|| first != null && first.equals(second);
		}
	}

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

	/**
	 * Shared desktop top-bar contract. Keeping the pause origin and HUD width
	 * in one renderer-free model prevents the 48px button from silently
	 * overlapping a HUD which still reserves the former 32px button.
	 */
	public static float desktopPauseX(
			float viewportWidth, float safeRight) {
		return viewportWidth
				- Math.max(0f, safeRight)
				- DESKTOP_PAUSE_RIGHT_MARGIN
				- DESKTOP_PAUSE_WIDTH;
	}

	public static float desktopHudWidth(
			float viewportWidth,
			float safeLeft,
			float safeRight) {
		float hudLeft = Math.max(0f, safeLeft) + HUD_SIDE_INSET;
		float hudRight = desktopPauseX(viewportWidth, safeRight)
				- DESKTOP_HUD_PAUSE_GAP;
		return Math.max(1f, hudRight - hudLeft);
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
	 * Keeps the single high-priority mobile interaction prompt below the HUD.
	 * Portrait centres it between split navigation buttons; landscape keeps it
	 * in the left rail and narrows it enough to clear stacked navigation.
	 * Directional sound and damage remain represented by their world-space arcs
	 * instead of additional text slabs.
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
		boolean portrait = viewportHeight > viewportWidth;
		float width = Math.min(
				96f,
				Math.max(1f,
						viewportWidth
								* (portrait ? 0.34f : 0.32f)));
		float x = portrait
				? Math.max(0f, (viewportWidth - width) * 0.5f)
				: Math.min(
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
		return fitText(
				value,
				availableWidth,
				BODY_FONT_PX,
				scaleLevel,
				2,
				Math.min(
						64,
						32 - clampScaleLevel(scaleLevel) * 2));
	}

	/** Keeps the status/weapon priority rows to one line on narrow screens. */
	public static String compactLine(
			String value, float availableWidth, int scaleLevel) {
		return fitText(
				value,
				availableWidth,
				CAPTION_FONT_PX,
				scaleLevel,
				1,
				32);
	}

	/** One-line fitting for body typography used by objective/action copy. */
	public static String compactBodyLine(
			String value, float availableWidth, int scaleLevel) {
		return fitText(
				value,
				availableWidth,
				BODY_FONT_PX,
				scaleLevel,
				1,
				32);
	}

	/** One-line fitting for the larger HP/ammunition typography. */
	public static String compactPrimaryLine(
			String value, float availableWidth, int scaleLevel) {
		return fitText(
				value,
				availableWidth,
				BODY_FONT_PX,
				scaleLevel,
				1,
				Math.min(
						24,
						Math.max(
								5,
								(int)Math.floor(
										Math.max(1f, availableWidth)
												/ (BODY_FONT_PX * 0.68f)))));
	}

	public static TextFootprint objectiveFootprint(
			String value, float availableWidth, int scaleLevel) {
		return textFootprint(
				value,
				availableWidth,
				BODY_FONT_PX,
				scaleLevel);
	}

	public static TextFootprint captionFootprint(
			String value, float availableWidth, int scaleLevel) {
		return textFootprint(
				value,
				availableWidth,
				CAPTION_FONT_PX,
				scaleLevel);
	}

	public static TextFootprint primaryFootprint(
			String value, float availableWidth, int scaleLevel) {
		return textFootprint(
				value,
				availableWidth,
				BODY_FONT_PX,
				scaleLevel);
	}

	public static float bodyLineHeight(int scaleLevel) {
		return lineHeight(BODY_FONT_PX, scaleLevel);
	}

	public static float captionLineHeight(int scaleLevel) {
		return lineHeight(CAPTION_FONT_PX, scaleLevel);
	}

	private static String fitText(
			String value,
			float availableWidth,
			int fontPx,
			int scaleLevel,
			int maximumLines,
			int hardCodePointLimit) {
		String normalized = normalize(value);
		float width = Math.max(1f, availableWidth);
		int codePoints = normalized.codePointCount(0, normalized.length());
		if (codePoints <= hardCodePointLimit
				&& fitsText(
						normalized,
						width,
						fontPx,
						scaleLevel,
						codePoints,
						false,
						maximumLines)) {
			return normalized;
		}

		int retained = Math.min(
				Math.max(0, hardCodePointLimit - 1),
				codePoints);
		while (retained > 0) {
			if (fitsText(
					normalized,
					width,
					fontPx,
					scaleLevel,
					retained,
					true,
					maximumLines)) {
				return prefixByCodePoints(normalized, retained) + "…";
			}
			retained--;
		}
		return fitsText(
				"…",
				width,
				fontPx,
				scaleLevel,
				1,
				false,
				maximumLines) ? "…" : "";
	}

	private static boolean fitsText(
			String value,
			float availableWidth,
			int fontPx,
			int scaleLevel,
			int codePointLimit,
			boolean appendEllipsis,
			int maximumLines) {
		float currentLine = 0f;
		int lines = 1;
		int consumed = 0;
		for (int offset = 0;
				offset < value.length() && consumed < codePointLimit;) {
			int codePoint = value.codePointAt(offset);
			float advance = glyphAdvance(
					codePoint,
					fontPx,
					scaleLevel);
			if (advance > availableWidth) return false;
			if (currentLine > 0f
					&& currentLine + advance > availableWidth) {
				lines++;
				currentLine = advance;
			} else {
				currentLine += advance;
			}
			if (lines > maximumLines) return false;
			offset += Character.charCount(codePoint);
			consumed++;
		}
		if (!appendEllipsis) return true;
		float ellipsis = glyphAdvance('…', fontPx, scaleLevel);
		if (ellipsis > availableWidth) return false;
		if (currentLine > 0f
				&& currentLine + ellipsis > availableWidth) {
			lines++;
		}
		return lines <= maximumLines;
	}

	private static TextFootprint textFootprint(
			String value,
			float availableWidth,
			int fontPx,
			int scaleLevel) {
		String normalized = normalize(value);
		if (normalized.isEmpty()) {
			return new TextFootprint(0f, 0f, 0);
		}
		float maximumWidth = Math.max(1f, availableWidth);
		float widestLine = 0f;
		float currentLine = 0f;
		int lines = 1;
		for (int offset = 0; offset < normalized.length();) {
			int codePoint = normalized.codePointAt(offset);
			float advance = glyphAdvance(
					codePoint,
					fontPx,
					scaleLevel);
			if (currentLine > 0f
					&& currentLine + advance > maximumWidth) {
				widestLine = Math.max(widestLine, currentLine);
				currentLine = advance;
				lines++;
			} else {
				currentLine += advance;
			}
			offset += Character.charCount(codePoint);
		}
		widestLine = Math.max(widestLine, currentLine);
		return new TextFootprint(
				widestLine,
				lines * lineHeight(fontPx, scaleLevel),
				lines);
	}

	private static float glyphAdvance(
			int codePoint, int fontPx, int scaleLevel) {
		float em;
		if (isCjk(codePoint) || codePoint > 0xFFFF) {
			em = 1f;
		} else if (Character.isWhitespace(codePoint)) {
			em = 0.35f;
		} else if (Character.isUpperCase(codePoint)) {
			em = 0.68f;
		} else if (Character.isLowerCase(codePoint)) {
			em = 0.56f;
		} else if (Character.isDigit(codePoint)) {
			em = 0.58f;
		} else {
			em = 0.42f;
		}
		float scaleSafety =
				1f + clampScaleLevel(scaleLevel) * 0.04f;
		return fontPx * em * scaleSafety;
	}

	private static float lineHeight(int fontPx, int scaleLevel) {
		float scaleSafety =
				1f + clampScaleLevel(scaleLevel) * 0.04f;
		return (float)Math.ceil(fontPx * scaleSafety);
	}

	private static boolean isCjk(int codePoint) {
		// RoboVM's Java runtime does not expose Character.UnicodeScript.of.
		// These stable Unicode blocks cover Han, kana, and Hangul HUD copy.
		return codePoint >= 0x3400 && codePoint <= 0x4DBF
				|| codePoint >= 0x4E00 && codePoint <= 0x9FFF
				|| codePoint >= 0xF900 && codePoint <= 0xFAFF
				|| codePoint >= 0x20000 && codePoint <= 0x2FA1F
				|| codePoint >= 0x3040 && codePoint <= 0x30FF
				|| codePoint >= 0x31F0 && codePoint <= 0x31FF
				|| codePoint >= 0xFF66 && codePoint <= 0xFF9D
				|| codePoint >= 0x1100 && codePoint <= 0x11FF
				|| codePoint >= 0x3130 && codePoint <= 0x318F
				|| codePoint >= 0xA960 && codePoint <= 0xA97F
				|| codePoint >= 0xAC00 && codePoint <= 0xD7FF;
	}

	private static String prefixByCodePoints(
			String value, int codePointCount) {
		int end = value.offsetByCodePoints(
				0,
				Math.min(
						codePointCount,
						value.codePointCount(0, value.length())));
		return value.substring(0, end);
	}

	private static String normalize(String value) {
		if (value == null) return "";
		// String.replaceAll compiles its pattern on every call, and every HUD
		// row normalizes once per frame.
		return WHITESPACE_RUN.matcher(value.trim()).replaceAll(" ");
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
