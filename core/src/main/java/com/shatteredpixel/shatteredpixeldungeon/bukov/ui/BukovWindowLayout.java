package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.RectF;

/** Shared safe-area fitting for Bukov modal windows. */
final class BukovWindowLayout {

	private static final int EDGE_GAP = 4;

	private BukovWindowLayout() {
	}

	static int safeWidth(int desired) {
		RectF insets = logicalSafeInsets();
		return fit(
				PixelScene.uiCamera.width,
				insets.left,
				insets.right,
				scaledDesired(desired));
	}

	static int safeHeight(int desired) {
		RectF insets = logicalSafeInsets();
		return fit(
				PixelScene.uiCamera.height,
				insets.top,
				insets.bottom,
				scaledDesired(desired));
	}

	static int scaledDesired(int desired) {
		return BukovUiScale.pixels(
				desired,
				SPDSettings.bukovUiScale());
	}

	static int fit(
			int viewport,
			float leadingInset,
			float trailingInset,
			int desired) {
		int available = (int)Math.floor(
				viewport
						- Math.max(0f, leadingInset)
						- Math.max(0f, trailingInset)
						- EDGE_GAP * 2f);
		return Math.max(1, Math.min(desired, available));
	}

	static boolean fits(
			int viewport,
			float leadingInset,
			float trailingInset,
			int windowSize) {
		return windowSize + Math.max(0f, leadingInset)
				+ Math.max(0f, trailingInset)
				+ EDGE_GAP * 2f <= viewport + 0.0001f;
	}

	private static RectF logicalSafeInsets() {
		if (Game.platform == null || PixelScene.uiCamera == null) {
			return new RectF();
		}
		RectF insets =
				Game.platform.getSafeInsets(PlatformSupport.INSET_BLK);
		if (insets == null) {
			return new RectF();
		}
		float zoom = PixelScene.uiCamera.zoom <= 0f
				? 1f : PixelScene.uiCamera.zoom;
		return new RectF(
				insets.left / zoom,
				insets.top / zoom,
				insets.right / zoom,
				insets.bottom / zoom);
	}
}
