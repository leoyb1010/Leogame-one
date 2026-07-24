package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;

/** Purple signal walker with an unmistakable tall broadcast antenna. */
public final class BukovSignalOperatorSprite extends BukovEnemySprite {
	public BukovSignalOperatorSprite() {
		super(Assets.Sprites.BUKOV_SIGNAL_OPERATOR, 0xFF665275,
				SpecialAction.SCAN);
	}
}
