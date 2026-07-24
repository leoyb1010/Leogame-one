package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Iron Clasp suppressor: broad steel armor and amber face shield. */
public final class BukovArmoredSprite extends BukovEnemySprite {
	public BukovArmoredSprite() {
		super(Assets.Sprites.BUKOV_ARMORED, 0xFF672825,
				SpecialAction.RELOAD);
	}
}
