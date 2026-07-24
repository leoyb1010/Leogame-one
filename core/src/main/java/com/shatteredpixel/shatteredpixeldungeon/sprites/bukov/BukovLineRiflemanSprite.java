package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Disciplined blue-line rifleman with a cold cyan sight. */
public final class BukovLineRiflemanSprite extends BukovEnemySprite {
	public BukovLineRiflemanSprite() {
		super(Assets.Sprites.BUKOV_LINE_RIFLEMAN,
				"combat.enemy.blood.lineRifleman",
				SpecialAction.RELOAD);
	}
}
