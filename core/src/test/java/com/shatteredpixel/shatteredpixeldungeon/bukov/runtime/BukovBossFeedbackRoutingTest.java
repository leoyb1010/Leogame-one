package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BukovBossFeedbackRoutingTest {

	@Test
	public void phaseThresholdUsesTheDedicatedBreakEnvelope() {
		assertEquals(
				CombatFeedbackType.BOSS_PHASE_BREAK,
				BukovRealtimeWorld.bossHitFeedback(
						WhiteLineBossStateMachine.Result.PHASE_CHANGED));
		assertNull(BukovRealtimeWorld.bossHitFeedback(
				WhiteLineBossStateMachine.Result.DAMAGED));
	}

	@Test
	public void finalVulnerableBossKillUsesWeakpointFeedback() {
		assertEquals(
				CombatFeedbackType.WEAKPOINT_KILL,
				BukovRealtimeWorld.bossDeathFeedback(
						WhiteLineBossStateMachine.Result.DEFEATED));
		assertEquals(
				CombatFeedbackType.KILL,
				BukovRealtimeWorld.bossDeathFeedback(
						WhiteLineBossStateMachine.Result.DAMAGED));
	}

	@Test
	public void bossPulseKindsMapWithoutChangingCombatMath() {
		assertEquals(
				CombatFeedbackType.BOSS_SLAM,
				BukovRealtimeWorld.bossPulseFeedback(
						WhiteLineBossStateMachine.Phase.DECOY_SEARCH));
		assertEquals(
				CombatFeedbackType.BOSS_OVERLOAD,
				BukovRealtimeWorld.bossPulseFeedback(
						WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD));
		assertNull(BukovRealtimeWorld.bossPulseFeedback(
				WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD));
	}
}
