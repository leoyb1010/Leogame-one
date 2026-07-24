package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Alarm drone: low hovering chassis, twin rotors, cyan sensor eye. */
public final class BukovDroneSprite extends BukovEnemySprite {
	public BukovDroneSprite() {
		super(Assets.Sprites.BUKOV_DRONE,
				"combat.enemy.blood.drone",
				SpecialAction.SCAN);
	}
}
