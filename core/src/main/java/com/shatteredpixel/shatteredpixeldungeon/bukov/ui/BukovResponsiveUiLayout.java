package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Coordinates the mobile HUD overlays with the real touch-control geometry.
 *
 * <p>Touch controls are always reserved first. Interaction feedback, offscreen
 * navigation and tutorial copy are then placed in that order. An overlay that
 * cannot fit at full size is explicitly compacted or hidden; it is never
 * clamped onto a higher-priority surface.</p>
 */
public final class BukovResponsiveUiLayout {

	private static final float CLEARANCE = 1f;
	private static final float HUD_GAP = 2f;
	private static final float FEEDBACK_HEIGHT = 13f;
	private static final float COMPACT_FEEDBACK_SIZE = 12f;
	private static final float NAVIGATION_WIDTH = 20f;
	private static final float NAVIGATION_HEIGHT = 18f;
	private static final float COMPACT_NAVIGATION_SIZE = 12f;
	private static final float TUTORIAL_HEIGHT = 28f;
	private static final float COMPACT_TUTORIAL_HEIGHT = 14f;

	public enum Presentation {
		FULL,
		COMPACT,
		HIDDEN
	}

	public static final class Overlay {
		public final Presentation presentation;
		public final float x;
		public final float y;
		public final float width;
		public final float height;

		private Overlay(
				Presentation presentation,
				float x,
				float y,
				float width,
				float height) {
			this.presentation = presentation;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public boolean visible() {
			return presentation != Presentation.HIDDEN;
		}

		public float right() {
			return x + width;
		}

		public float bottom() {
			return y + height;
		}

		public boolean overlaps(Overlay other) {
			return visible()
					&& other != null
					&& other.visible()
					&& x < other.right()
					&& right() > other.x
					&& y < other.bottom()
					&& bottom() > other.y;
		}
	}

	public final BukovTouchLayout touch;
	public final Overlay interaction;
	public final Overlay navigation;
	public final Overlay tutorial;

	/**
	 * Production cache for the per-frame HUD layout path. A normal refresh
	 * returns the same immutable result without entering the allocating packer.
	 */
	public static final class Cache {
		private BukovResponsiveUiLayout value;
		private float viewportWidth;
		private float viewportHeight;
		private float safeLeft;
		private float safeTop;
		private float safeRight;
		private float safeBottom;
		private float hudBottom;
		private int scaleLevel;
		private boolean interactionVisible;
		private BukovRaidHudState.Direction navigationDirection;
		private boolean tutorialVisible;
		private int recomputations;

		public BukovResponsiveUiLayout layout(
				float viewportWidth,
				float viewportHeight,
				float safeLeft,
				float safeTop,
				float safeRight,
				float safeBottom,
				float hudBottom,
				int scaleLevel,
				boolean interactionVisible,
				BukovRaidHudState.Direction navigationDirection,
				boolean tutorialVisible) {
			if (value == null
					|| changed(this.viewportWidth, viewportWidth)
					|| changed(this.viewportHeight, viewportHeight)
					|| changed(this.safeLeft, safeLeft)
					|| changed(this.safeTop, safeTop)
					|| changed(this.safeRight, safeRight)
					|| changed(this.safeBottom, safeBottom)
					|| changed(this.hudBottom, hudBottom)
					|| this.scaleLevel != scaleLevel
					|| this.interactionVisible != interactionVisible
					|| this.navigationDirection != navigationDirection
					|| this.tutorialVisible != tutorialVisible) {
				this.viewportWidth = viewportWidth;
				this.viewportHeight = viewportHeight;
				this.safeLeft = safeLeft;
				this.safeTop = safeTop;
				this.safeRight = safeRight;
				this.safeBottom = safeBottom;
				this.hudBottom = hudBottom;
				this.scaleLevel = scaleLevel;
				this.interactionVisible = interactionVisible;
				this.navigationDirection = navigationDirection;
				this.tutorialVisible = tutorialVisible;
				value = calculateMobile(
						viewportWidth,
						viewportHeight,
						safeLeft,
						safeTop,
						safeRight,
						safeBottom,
						hudBottom,
						scaleLevel,
						interactionVisible,
						navigationDirection,
						tutorialVisible);
				recomputations++;
			}
			return value;
		}

		public int recomputations() {
			return recomputations;
		}

		private static boolean changed(float first, float second) {
			return Float.floatToIntBits(first)
					!= Float.floatToIntBits(second);
		}
	}

	private BukovResponsiveUiLayout(
			BukovTouchLayout touch,
			Overlay interaction,
			Overlay navigation,
			Overlay tutorial) {
		this.touch = touch;
		this.interaction = interaction;
		this.navigation = navigation;
		this.tutorial = tutorial;
	}

	public static BukovResponsiveUiLayout calculateMobile(
			float viewportWidth,
			float viewportHeight,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom,
			float hudBottom,
			int scaleLevel,
			boolean interactionVisible,
			BukovRaidHudState.Direction navigationDirection,
			boolean tutorialVisible) {
		if (viewportWidth <= 0f || viewportHeight <= 0f) {
			throw new IllegalArgumentException(
					"viewport dimensions must be positive");
		}
		BukovTouchLayout touch = BukovTouchLayout.calculate(
				viewportWidth,
				viewportHeight,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				hudBottom + HUD_GAP);
		Overlay[] reserved = touchReservations(touch);
		int reservedCount = reserved.length;

		float feedbackWidth = Math.min(
				96f,
				Math.max(
						1f,
						viewportWidth
								* (touch.landscape ? 0.32f : 0.34f)));
		float feedbackX = touch.landscape
				? touch.safeBounds.x
				: touch.safeBounds.centerX() - feedbackWidth * 0.5f;
		Overlay interaction = interactionVisible
				? place(
						touch.safeBounds,
						hudBottom,
						feedbackWidth,
						FEEDBACK_HEIGHT,
						COMPACT_FEEDBACK_SIZE,
						COMPACT_FEEDBACK_SIZE,
						feedbackX,
						hudBottom + HUD_GAP,
						reserved,
						reservedCount)
				: hidden(touch.safeBounds.x, hudBottom);
		if (interaction.visible()) {
			reserved = append(reserved, interaction);
			reservedCount++;
		}

		Overlay navigation = navigationDirection == null
				? hidden(touch.safeBounds.x, hudBottom)
				: placeNavigation(
						touch.safeBounds,
						hudBottom,
						navigationDirection,
						reserved,
						reservedCount);
		if (navigation.visible()) {
			reserved = append(reserved, navigation);
			reservedCount++;
		}

		float tutorialWidth = Math.min(
				190f * BukovRaidHudLayout.scaleMultiplier(scaleLevel),
				Math.max(1f, touch.safeBounds.width - 8f));
		float compactTutorialWidth = Math.min(
				112f,
				Math.max(1f, touch.safeBounds.width - 8f));
		Overlay tutorial = tutorialVisible
				? place(
						touch.safeBounds,
						hudBottom,
						tutorialWidth,
						TUTORIAL_HEIGHT,
						compactTutorialWidth,
						COMPACT_TUTORIAL_HEIGHT,
						touch.safeBounds.centerX()
								- tutorialWidth * 0.5f,
						hudBottom + 30f,
						reserved,
						reservedCount)
				: hidden(touch.safeBounds.x, hudBottom);

		/*
		 * Navigation outranks tutorial. If even its compact form cannot fit,
		 * do not let the lower-priority tutorial consume the remaining sliver.
		 */
		if (navigationDirection != null
				&& !navigation.visible()
				&& tutorial.visible()) {
			tutorial = hidden(tutorial.x, tutorial.y);
		}
		return new BukovResponsiveUiLayout(
				touch,
				interaction,
				navigation,
				tutorial);
	}

	private static Overlay placeNavigation(
			BukovTouchLayout.Rect bounds,
			float hudBottom,
			BukovRaidHudState.Direction direction,
			Overlay[] reserved,
			int reservedCount) {
		float fullX = directionalX(
				bounds,
				NAVIGATION_WIDTH,
				direction);
		float fullY = directionalY(
				bounds,
				hudBottom,
				NAVIGATION_HEIGHT,
				direction);
		return place(
				bounds,
				hudBottom,
				NAVIGATION_WIDTH,
				NAVIGATION_HEIGHT,
				COMPACT_NAVIGATION_SIZE,
				COMPACT_NAVIGATION_SIZE,
				fullX,
				fullY,
				reserved,
				reservedCount);
	}

	private static float directionalX(
			BukovTouchLayout.Rect bounds,
			float width,
			BukovRaidHudState.Direction direction) {
		switch (direction) {
			case NE:
			case E:
			case SE:
				return bounds.right() - width;
			case N:
			case S:
				return bounds.centerX() - width * 0.5f;
			case NW:
			case W:
			case SW:
			default:
				return bounds.x;
		}
	}

	private static float directionalY(
			BukovTouchLayout.Rect bounds,
			float hudBottom,
			float height,
			BukovRaidHudState.Direction direction) {
		float top = Math.max(bounds.y, hudBottom + HUD_GAP);
		switch (direction) {
			case SW:
			case S:
			case SE:
				return bounds.bottom() - height;
			case W:
			case E:
				return (top + bounds.bottom() - height) * 0.5f;
			case NW:
			case N:
			case NE:
			default:
				return top;
		}
	}

	private static Overlay place(
			BukovTouchLayout.Rect bounds,
			float hudBottom,
			float fullWidth,
			float fullHeight,
			float compactWidth,
			float compactHeight,
			float desiredFullX,
			float desiredFullY,
			Overlay[] reserved,
			int reservedCount) {
		Overlay full = findPlacement(
				Presentation.FULL,
				bounds,
				hudBottom,
				fullWidth,
				fullHeight,
				desiredFullX,
				desiredFullY,
				reserved,
				reservedCount);
		if (full.visible()) return full;

		float compactX =
				desiredFullX + (fullWidth - compactWidth) * 0.5f;
		float compactY =
				desiredFullY + (fullHeight - compactHeight) * 0.5f;
		return findPlacement(
				Presentation.COMPACT,
				bounds,
				hudBottom,
				compactWidth,
				compactHeight,
				compactX,
				compactY,
				reserved,
				reservedCount);
	}

	private static Overlay findPlacement(
			Presentation presentation,
			BukovTouchLayout.Rect bounds,
			float hudBottom,
			float width,
			float height,
			float desiredX,
			float desiredY,
			Overlay[] reserved,
			int reservedCount) {
		float minX = bounds.x;
		float maxX = bounds.right() - width;
		float minY = Math.max(bounds.y, hudBottom + HUD_GAP);
		float maxY = bounds.bottom() - height;
		if (width <= 0f
				|| height <= 0f
				|| maxX < minX
				|| maxY < minY) {
			return hidden(minX, minY);
		}

		float[] xCandidates = new float[3 + reservedCount * 2];
		float[] yCandidates = new float[3 + reservedCount * 2];
		int xCount = baseCandidates(
				xCandidates,
				minX,
				maxX,
				clamp(desiredX, minX, maxX));
		int yCount = baseCandidates(
				yCandidates,
				minY,
				maxY,
				clamp(desiredY, minY, maxY));
		for (int index = 0; index < reservedCount; index++) {
			Overlay obstacle = reserved[index];
			xCandidates[xCount++] = clamp(
					obstacle.x - CLEARANCE - width,
					minX,
					maxX);
			xCandidates[xCount++] = clamp(
					obstacle.right() + CLEARANCE,
					minX,
					maxX);
			yCandidates[yCount++] = clamp(
					obstacle.y - CLEARANCE - height,
					minY,
					maxY);
			yCandidates[yCount++] = clamp(
					obstacle.bottom() + CLEARANCE,
					minY,
					maxY);
		}

		Overlay best = null;
		float bestScore = Float.MAX_VALUE;
		for (int xIndex = 0; xIndex < xCount; xIndex++) {
			for (int yIndex = 0; yIndex < yCount; yIndex++) {
				Overlay candidate = new Overlay(
						presentation,
						xCandidates[xIndex],
						yCandidates[yIndex],
						width,
						height);
				if (!clear(candidate, reserved, reservedCount)) {
					continue;
				}
				float score =
						Math.abs(candidate.x - desiredX)
								+ Math.abs(candidate.y - desiredY);
				if (score < bestScore) {
					best = candidate;
					bestScore = score;
				}
			}
		}
		return best == null ? hidden(minX, minY) : best;
	}

	private static int baseCandidates(
			float[] candidates,
			float minimum,
			float maximum,
			float desired) {
		candidates[0] = desired;
		candidates[1] = minimum;
		candidates[2] = maximum;
		return 3;
	}

	private static boolean clear(
			Overlay candidate,
			Overlay[] reserved,
			int reservedCount) {
		for (int index = 0; index < reservedCount; index++) {
			Overlay obstacle = reserved[index];
			if (candidate.x < obstacle.right() + CLEARANCE
					&& candidate.right() + CLEARANCE > obstacle.x
					&& candidate.y < obstacle.bottom() + CLEARANCE
					&& candidate.bottom() + CLEARANCE > obstacle.y) {
				return false;
			}
		}
		return true;
	}

	private static Overlay[] touchReservations(BukovTouchLayout touch) {
		return new Overlay[] {
				reserved(touch.movement),
				reserved(touch.aimFire),
				reserved(touch.interact),
				reserved(touch.reload),
				reserved(touch.medical),
				reserved(touch.drop),
				reserved(touch.backpack),
				reserved(touch.pause)
		};
	}

	private static Overlay reserved(BukovTouchLayout.Rect rect) {
		return new Overlay(
				Presentation.FULL,
				rect.x,
				rect.y,
				rect.width,
				rect.height);
	}

	private static Overlay[] append(
			Overlay[] values, Overlay value) {
		Overlay[] result = new Overlay[values.length + 1];
		System.arraycopy(values, 0, result, 0, values.length);
		result[values.length] = value;
		return result;
	}

	private static Overlay hidden(float x, float y) {
		return new Overlay(Presentation.HIDDEN, x, y, 0f, 0f);
	}

	private static float clamp(
			float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private BukovResponsiveUiLayout() {
		throw new AssertionError();
	}
}
