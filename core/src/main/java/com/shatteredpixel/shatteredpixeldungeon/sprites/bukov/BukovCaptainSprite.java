package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Elite commander: black-red command armor and red optic. */
public final class BukovCaptainSprite extends BukovEnemySprite {
	public BukovCaptainSprite() {
		super(Assets.Sprites.BUKOV_CAPTAIN,
				"combat.enemy.blood.captain",
				SpecialAction.RELOAD);
	}
}
