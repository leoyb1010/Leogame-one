package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.watabou.noosa.Game;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementTimingTest {

	@Test
	public void movementVisualRemainsFastWithoutChangingGameTimeScale() {
		assertEquals(0.075f, CharSprite.DEFAULT_MOVE_INTERVAL, 0.0001f);
		assertEquals(1f, Game.timeScale, 0.0001f);
	}
}
