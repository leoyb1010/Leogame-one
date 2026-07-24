package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Iron Clasp marksman: pale helmet, long optic and radio antenna. */
public final class BukovIronClaspMarksmanSprite extends BukovEnemySprite {
	public BukovIronClaspMarksmanSprite() {
		super(Assets.Sprites.BUKOV_IRON_CLASP_MARKSMAN, 0xFF56636B,
				SpecialAction.RELOAD);
	}
}
