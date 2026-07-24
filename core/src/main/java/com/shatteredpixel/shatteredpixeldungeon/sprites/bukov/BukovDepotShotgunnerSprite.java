package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Depot shotgunner: broad orange shoulder blocks and amber face shield. */
public final class BukovDepotShotgunnerSprite extends BukovEnemySprite {
	public BukovDepotShotgunnerSprite() {
		super(Assets.Sprites.BUKOV_DEPOT_SHOTGUNNER,
				"combat.enemy.blood.depotShotgunner",
				SpecialAction.RELOAD);
	}
}
