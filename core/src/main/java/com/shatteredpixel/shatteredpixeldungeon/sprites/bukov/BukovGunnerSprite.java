package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Mobile ranged scavenger: olive rig, cyan optic, long gun silhouette. */
public final class BukovGunnerSprite extends BukovEnemySprite {
	public BukovGunnerSprite() {
		super(Assets.Sprites.BUKOV_GUNNER, 0xFF6E2522,
				SpecialAction.RELOAD);
	}
}
