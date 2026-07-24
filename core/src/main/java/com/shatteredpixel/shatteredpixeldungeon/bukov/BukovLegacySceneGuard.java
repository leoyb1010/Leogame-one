package com.shatteredpixel.shatteredpixeldungeon.bukov;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.BukovHubScene;

/**
 * Product boundary for inherited campaign scenes that are still compiled as
 * engine dependencies but must never render in Escape from Bukov.
 */
public final class BukovLegacySceneGuard {

	private BukovLegacySceneGuard() {
	}

	public static boolean redirectToHub() {
		BukovMode.enter();
		GamesInProgress.curSlot = BukovMode.SAVE_SLOT;
		ShatteredPixelDungeon.switchNoFade(BukovHubScene.class);
		return true;
	}
}
