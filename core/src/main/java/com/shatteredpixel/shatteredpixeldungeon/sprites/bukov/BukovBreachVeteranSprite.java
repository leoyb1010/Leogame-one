package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Breach veteran: red armored shoulders and an amber visor. */
public final class BukovBreachVeteranSprite extends BukovEnemySprite {
	public BukovBreachVeteranSprite() {
		super(Assets.Sprites.BUKOV_BREACH_VETERAN, 0xFF6F302A,
				SpecialAction.RELOAD);
	}
}
