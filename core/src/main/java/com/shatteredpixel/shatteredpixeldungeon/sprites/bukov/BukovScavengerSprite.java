package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Close-range scavenger: tan jacket, improvised pack, compact weapon. */
public final class BukovScavengerSprite extends BukovEnemySprite {
	public BukovScavengerSprite() {
		super(Assets.Sprites.BUKOV_SCAVENGER, 0xFF6E2522,
				SpecialAction.RUSH);
	}
}
