package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Pure layout model for the raid touch overlay.
 *
 * All values use PixelScene logical pixels. The caller supplies the safe-area
 * insets already converted to the same coordinate system.
 */
public final class BukovTouchLayout {

	public static final class Rect {
		public final float x;
		public final float y;
		public final float width;
		public final float height;

		Rect(float x, float y, float width, float height) {
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

		public float centerX() {
			return x + width * 0.5f;
		}

		public float centerY() {
			return y + height * 0.5f;
		}

		public boolean contains(Rect other) {
			return other.x >= x && other.y >= y
					&& other.right() <= right() && other.bottom() <= bottom();
		}

		public boolean overlaps(Rect other) {
			return x < other.right() && right() > other.x
					&& y < other.bottom() && bottom() > other.y;
		}
	}

	public final boolean landscape;
	public final Rect safeBounds;
	public final Rect movement;
	public final Rect aimFire;
	public final Rect interact;
	public final Rect reload;
	public final Rect medical;
	public final Rect drop;
	public final Rect backpack;
	public final Rect pause;

	private BukovTouchLayout(
			boolean landscape,
			Rect safeBounds,
			Rect movement,
			Rect aimFire,
			Rect interact,
			Rect reload,
			Rect medical,
			Rect drop,
			Rect backpack,
			Rect pause) {
		this.landscape = landscape;
		this.safeBounds = safeBounds;
		this.movement = movement;
		this.aimFire = aimFire;
		this.interact = interact;
		this.reload = reload;
		this.medical = medical;
		this.drop = drop;
		this.backpack = backpack;
		this.pause = pause;
	}

	public static BukovTouchLayout calculate(
			float width,
			float height,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom) {
		return calculate(
				width,
				height,
				safeLeft,
				safeTop,
				safeRight,
				safeBottom,
				safeTop
		);
	}

	/**
	 * @param hudBottom bottom edge of the raid HUD, expressed as a local
	 *                  y-coordinate in the same logical-pixel surface. The two
	 *                  navigation buttons are placed below this edge.
	 */
	public static BukovTouchLayout calculate(
			float width,
			float height,
			float safeLeft,
			float safeTop,
			float safeRight,
			float safeBottom,
			float hudBottom) {
		if (width <= 0f || height <= 0f) {
			throw new IllegalArgumentException("touch surface must have positive dimensions");
		}
		safeLeft = clamp(safeLeft, 0f, width * 0.25f);
		safeRight = clamp(safeRight, 0f, width * 0.25f);
		safeTop = clamp(safeTop, 0f, height * 0.25f);
		safeBottom = clamp(safeBottom, 0f, height * 0.25f);

		Rect safe = new Rect(
				safeLeft,
				safeTop,
				Math.max(1f, width - safeLeft - safeRight),
				Math.max(1f, height - safeTop - safeBottom)
		);
		boolean landscape = safe.width >= safe.height;
		float shortest = Math.min(safe.width, safe.height);
		float stick = clamp(shortest * (landscape ? 0.39f : 0.29f), 46f, 72f);
		float margin = clamp(shortest * 0.045f, 4f, 9f);

		// If a very small logical viewport is used, preserve a center lane.
		stick = Math.min(stick, (safe.width - margin * 3f) * 0.5f);
		stick = Math.max(36f, stick);

		/*
		 * TouchAction expands every hit target to at least 22 logical pixels.
		 * Keep the authored rectangle at that same floor so the expanded target
		 * can never silently overlap a neighbouring control.
		 */
		float estimatedAction = clamp(stick * 0.38f, 22f, 28f);
		float estimatedGap = clamp(estimatedAction * 0.18f, 3f, 5f);
		float pauseWidth = clamp(safe.width * 0.15f, 34f, 52f);
		float pauseHeight = clamp(shortest * 0.10f, 22f, 24f);
		float navigationY = clamp(
				Math.max(safe.y, hudBottom),
				safe.y,
				Math.max(safe.y, safe.bottom() - pauseHeight)
		);

		/*
		 * Portrait uses three horizontal zones: side navigation, a centred
		 * two-column action rail, and the two edge sticks. The maximum stick
		 * size must satisfy both the space below navigation and the width left
		 * beside the action rail. This remains valid when the scaled HUD leaves
		 * only a short lower control band.
		 */
		if (!landscape) {
			float actionGridWidth =
					estimatedAction * 2f + estimatedGap;
			float availableBesideActions =
					(safe.width - actionGridWidth) * 0.5f
							- margin
							- estimatedGap;
			float availableBelowNavigation =
					safe.bottom()
							- margin
							- navigationY
							- pauseHeight
							- estimatedGap;
			stick = Math.min(
					stick,
					Math.max(
							24f,
							Math.min(
									availableBesideActions,
									availableBelowNavigation)));
		}

		Rect movement = new Rect(
				safe.x + margin,
				safe.bottom() - margin - stick,
				stick,
				stick
		);
		Rect aim = new Rect(
				safe.right() - margin - stick,
				safe.bottom() - margin - stick,
				stick,
				stick
		);

		float action = clamp(stick * 0.38f, 22f, 28f);
		float gap = clamp(action * 0.18f, 3f, 5f);
		Rect interact;
		Rect reload;
		Rect medical;
		Rect drop;
		if (landscape) {
			/*
			 * A compact landscape cannot afford an action row above the right
			 * stick: that row intersects even the base-height raid HUD. Keep a
			 * two-by-two action grid in the center lane, aligned with the two
			 * sticks and entirely below the reserved HUD edge.
			 */
			float rightColumnX = aim.x - gap - action;
			float leftColumnX = rightColumnX - gap - action;
			float topRowY = Math.max(
					Math.max(safe.y + margin, hudBottom + gap),
					aim.y);
			float bottomRowY = aim.bottom() - action;
			if (topRowY + action + gap > bottomRowY) {
				topRowY = Math.max(
						safe.y + margin,
						bottomRowY - gap - action);
			}
			drop = new Rect(
					leftColumnX, topRowY, action, action);
			interact = new Rect(
					rightColumnX, topRowY, action, action);
			reload = new Rect(
					leftColumnX, bottomRowY, action, action);
			medical = new Rect(
					rightColumnX, bottomRowY, action, action);
		} else {
			/*
			 * A 135-pixel portrait viewport cannot fit two 46-pixel sticks and
			 * two 22-pixel actions in one horizontal row. Put the action grid
			 * in its own vertical rail between navigation and the two sticks.
			 */
			float gridWidth = action * 2f + gap;
			float laneLeft = safe.centerX() - gridWidth * 0.5f;
			float rightColumnX = laneLeft + action + gap;
			float bottomRowY =
					safe.bottom() - margin - action;
			float topRowY = bottomRowY - gap - action;
			drop = new Rect(
					laneLeft, topRowY, action, action);
			interact = new Rect(
					rightColumnX, topRowY, action, action);
			reload = new Rect(
					laneLeft, bottomRowY, action, action);
			medical = new Rect(
					rightColumnX, bottomRowY, action, action);
		}

		Rect pause;
		Rect backpack;
		if (landscape
				&& navigationY + pauseHeight + gap > movement.y) {
			/*
			 * On a 240x135 iPhone surface the navigation row and right stick
			 * share the same vertical band. Stack navigation in the free rail
			 * immediately left of the action grid instead of placing it over
			 * the aim stick.
			 */
			float navigationX = drop.x - gap - pauseWidth;
			backpack = new Rect(
					navigationX,
					navigationY,
					pauseWidth,
					pauseHeight);
			pause = new Rect(
					navigationX,
					navigationY + pauseHeight + gap,
					pauseWidth,
					pauseHeight);
		} else if (!landscape) {
			/*
			 * Split portrait navigation across the two upper corners. The
			 * interaction feedback owns the middle of this row, while the
			 * bottom-anchored action rail remains clear of both buttons.
			 */
			backpack = new Rect(
					safe.x,
					navigationY,
					pauseWidth,
					pauseHeight
			);
			pause = new Rect(
					safe.right() - pauseWidth,
					navigationY,
					pauseWidth,
					pauseHeight
			);
		} else {
			pause = new Rect(
					safe.right() - pauseWidth,
					navigationY,
					pauseWidth,
					pauseHeight
			);
			backpack = new Rect(
					pause.x - gap - pauseWidth,
					navigationY,
					pauseWidth,
					pauseHeight
			);
		}

		return new BukovTouchLayout(
				landscape,
				safe,
				movement,
				aim,
				interact,
				reload,
				medical,
				drop,
				backpack,
				pause
		);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
