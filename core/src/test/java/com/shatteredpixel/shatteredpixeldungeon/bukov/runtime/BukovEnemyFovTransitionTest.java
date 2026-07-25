package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovEnemyFovTransitionTest {

	@Test
	public void stationaryPlayerFovShowsEnteringEnemyAndHidesLeavingEnemy() {
		boolean[] stationaryPlayerFov = new boolean[25];
		int outsideBefore = 11;
		int visibleCell = 12;
		int outsideAfter = 13;
		stationaryPlayerFov[visibleCell] = true;

		BukovHostMob enemy = new BukovHostMob();
		enemy.pos = outsideBefore;
		enemy.sprite = new CharSprite();
		enemy.sprite.visible = false;

		BukovRealtimeWorld.syncMovedEnemyVisibility(
				enemy,
				visibleCell,
				stationaryPlayerFov);

		assertEquals(visibleCell, enemy.pos);
		assertTrue(enemy.sprite.visible);

		BukovRealtimeWorld.syncMovedEnemyVisibility(
				enemy,
				outsideAfter,
				stationaryPlayerFov);

		assertEquals(outsideAfter, enemy.pos);
		assertFalse(enemy.sprite.visible);
	}
}
