package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Hooded teal scout: narrow silhouette and bright cyan optic. */
public final class BukovAlleyScoutSprite extends BukovEnemySprite {
	public BukovAlleyScoutSprite() {
		super(Assets.Sprites.BUKOV_ALLEY_SCOUT, 0xFF315D5A,
				SpecialAction.RELOAD);
	}
}
