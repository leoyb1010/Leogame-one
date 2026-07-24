package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCombatHudRuntimeTest {

	@Test
	public void hitDirectionCoversEightCompassSectors() {
		assertEquals(BukovRaidHudState.Direction.N,
				BukovRealtimeWorld.direction(0f, -1f));
		assertEquals(BukovRaidHudState.Direction.NE,
				BukovRealtimeWorld.direction(1f, -1f));
		assertEquals(BukovRaidHudState.Direction.E,
				BukovRealtimeWorld.direction(1f, 0f));
		assertEquals(BukovRaidHudState.Direction.SE,
				BukovRealtimeWorld.direction(1f, 1f));
		assertEquals(BukovRaidHudState.Direction.S,
				BukovRealtimeWorld.direction(0f, 1f));
		assertEquals(BukovRaidHudState.Direction.SW,
				BukovRealtimeWorld.direction(-1f, 1f));
		assertEquals(BukovRaidHudState.Direction.W,
				BukovRealtimeWorld.direction(-1f, 0f));
		assertEquals(BukovRaidHudState.Direction.NW,
				BukovRealtimeWorld.direction(-1f, -1f));
	}

	@Test
	public void damageNumberModesActuallyGatePresentation() {
		assertFalse(BukovRealtimeWorld.shouldShowDamageNumber(0, 99, 100));
		assertFalse(BukovRealtimeWorld.shouldShowDamageNumber(1, 7, 100));
		assertTrue(BukovRealtimeWorld.shouldShowDamageNumber(1, 15, 100));
		assertTrue(BukovRealtimeWorld.shouldShowDamageNumber(2, 1, 100));
	}

	@Test
	public void shotgunFireUsesItsAuthoredNearRecoilFeedback() {
		FirearmDefinition shotgun = new FirearmDefinition();
		shotgun.weaponClass = FirearmClass.SHOTGUN;
		shotgun.pellets = 8;
		assertEquals(
				CombatFeedbackType.SHOTGUN_NEAR,
				BukovRealtimeWorld.playerShotFeedback(shotgun));

		FirearmDefinition rifle = new FirearmDefinition();
		rifle.weaponClass = FirearmClass.ASSAULT_RIFLE;
		rifle.pellets = 1;
		assertEquals(
				CombatFeedbackType.RIFLE_SHOT,
				BukovRealtimeWorld.playerShotFeedback(rifle));
	}
}
