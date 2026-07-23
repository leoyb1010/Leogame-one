package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.watabou.utils.Bundle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WhiteLineBossStateMachineTest {

	@Test
	public void threePhasesRequireDifferentReadableObjectives() {
		WhiteLineBossStateMachine boss =
				new WhiteLineBossStateMachine(300, 991177L);

		assertEquals(
				WhiteLineBossStateMachine.Result.ENGAGED,
				boss.engage()
		);
		assertEquals(
				WhiteLineBossStateMachine.Objective.FLANK_UMBRELLA,
				boss.objective()
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.DAMAGE_BLOCKED,
				boss.applyDamage(500)
		);
		assertEquals(300, boss.health());

		assertEquals(
				WhiteLineBossStateMachine.Result.MECHANISM_REJECTED,
				boss.flankUmbrella(1f, 0f, 1f, 0f)
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.MECHANISM_REJECTED,
				boss.flankUmbrella(Float.NaN, 0f, -1f, 0f)
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.flankUmbrella(1f, 0f, 0f, 1f)
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.PHASE_CHANGED,
				boss.applyDamage(500)
		);
		assertEquals(
				WhiteLineBossStateMachine.Phase.DECOY_SEARCH,
				boss.phase()
		);
		assertEquals(
				WhiteLineBossStateMachine.Objective.IDENTIFY_TRUE_BODY,
				boss.objective()
		);

		int wrongBody = (boss.trueBodyIndex() + 1) % boss.bodyCount();
		assertEquals(
				WhiteLineBossStateMachine.Result.MECHANISM_REJECTED,
				boss.identifyTrueBody(wrongBody)
		);
		assertFalse(boss.synchronizedTrace(wrongBody));
		assertTrue(boss.synchronizedTrace(boss.trueBodyIndex()));
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.identifyTrueBody(boss.trueBodyIndex())
		);
		boss.applyDamage(500);
		assertEquals(
				WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD,
				boss.phase()
		);
		assertEquals(
				WhiteLineBossStateMachine.Objective.DISABLE_FOG_LAMPS,
				boss.objective()
		);

		assertEquals(
				WhiteLineBossStateMachine.Result.MECHANISM_REJECTED,
				boss.disableFogLamp("boss_body")
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				boss.disableFogLamp(
						WhiteLineBossStateMachine.DEFAULT_FOG_LAMP_ANCHOR)
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.DEFEATED,
				boss.applyDamage(500)
		);
		assertEquals(WhiteLineBossStateMachine.Phase.DEFEATED, boss.phase());
	}

	@Test
	public void encounterCanBeBypassedWithoutBossKill() {
		WhiteLineBossStateMachine boss =
				new WhiteLineBossStateMachine(240);
		boss.engage();
		boss.flankUmbrella(1f, 0f, -1f, 0f);
		boss.applyDamage(100);

		assertEquals(
				WhiteLineBossStateMachine.Result.NO_CHANGE,
				boss.bypass(false)
		);
		assertEquals(
				WhiteLineBossStateMachine.Result.BYPASSED,
				boss.bypass(true)
		);
		assertEquals(WhiteLineBossStateMachine.Phase.BYPASSED, boss.phase());
		assertTrue(boss.health() > 0);
		assertFalse(boss.active());
	}

	@Test
	public void longFightRecommendsRetreatInsteadOfForcingVictory() {
		WhiteLineBossStateMachine boss =
				new WhiteLineBossStateMachine(240);
		boss.engage();
		boss.update(119f);
		assertFalse(boss.retreatRecommended());
		boss.update(1f);
		assertTrue(boss.retreatRecommended());
	}

	@Test
	public void seedSelectedBodyAndProgressSurviveBundleRestore() {
		WhiteLineBossStateMachine original =
				new WhiteLineBossStateMachine(300, 0xCAFEF00DL);
		original.engage();
		original.flankUmbrella(0f, 1f, -1f, 0f);
		original.applyDamage(500);
		int trueBody = original.trueBodyIndex();
		original.update(37.5f);

		Bundle bundle = new Bundle();
		bundle.put("boss", original);
		WhiteLineBossStateMachine restored =
				(WhiteLineBossStateMachine)bundle.get("boss");

		assertEquals(original.phase(), restored.phase());
		assertEquals(original.objective(), restored.objective());
		assertEquals(original.health(), restored.health());
		assertEquals(original.encounterSeconds(),
				restored.encounterSeconds(), 0f);
		assertEquals(trueBody, restored.trueBodyIndex());
		assertTrue(restored.synchronizedTrace(trueBody));
		assertEquals(
				WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED,
				restored.identifyTrueBody(trueBody)
		);
	}

	@Test
	public void sameEncounterKeyAlwaysSelectsSameAuthenticTrace() {
		for (long seed = 0; seed < 40; seed++) {
			WhiteLineBossStateMachine first =
					new WhiteLineBossStateMachine(240, seed);
			WhiteLineBossStateMachine second =
					new WhiteLineBossStateMachine(240, seed);
			assertEquals(first.trueBodyIndex(), second.trueBodyIndex());
		}
	}
}
