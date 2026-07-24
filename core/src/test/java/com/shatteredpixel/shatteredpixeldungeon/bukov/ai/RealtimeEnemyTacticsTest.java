package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealtimeEnemyTacticsTest {

	@Test
	public void liveContentTagsSelectDifferentCombatProfiles() {
		assertEquals(
				RealtimeEnemyTactics.Profile.SUPPRESSOR,
				RealtimeEnemyTactics.profileFor(definition(
						EnemyRole.ARMORED_SUPPRESSOR,
						"USE_COVER",
						"SHORT_SUPPRESSION")));
		assertEquals(
				RealtimeEnemyTactics.Profile.FLANKER,
				RealtimeEnemyTactics.profileFor(definition(
						EnemyRole.ELITE_COMMANDER,
						"ORDER_FLANK")));
		assertEquals(
				RealtimeEnemyTactics.Profile.RUSHER,
				RealtimeEnemyTactics.profileFor(definition(
						EnemyRole.MELEE_RUSHER,
						"SHORT_DASH")));
		assertEquals(
				RealtimeEnemyTactics.Profile.RETREATING_SKIRMISHER,
				RealtimeEnemyTactics.profileFor(definition(
						EnemyRole.RANGED_SKIRMISHER,
						"RETREAT_FROM_STRONG_TARGET")));
	}

	@Test
	public void suppressorAnchorsOnlyWithSightAndInsideWeaponBand() {
		RealtimeEnemyTactics tactics = tactics(
				RealtimeEnemyTactics.Profile.SUPPRESSOR, 0);
		RealtimeEnemyTactics.Intent intent =
				new RealtimeEnemyTactics.Intent();

		step(tactics, false, 4f, 8f, 1f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.FOLLOW_BRAIN,
				intent.maneuver());
		assertEquals(1f, intent.desiredX(), 0f);

		step(tactics, true, 4f, 8f, 1f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.ANCHOR_AND_SUPPRESS,
				intent.maneuver());
		assertEquals(0f, intent.speedMultiplier(), 0f);

		step(tactics, true, 10f, 8f, 1f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.FOLLOW_BRAIN,
				intent.maneuver());
	}

	@Test
	public void flankersCrossTheSightLineThenRespectCooldown() {
		RealtimeEnemyTactics tactics = tactics(
				RealtimeEnemyTactics.Profile.FLANKER, 0);
		RealtimeEnemyTactics.Intent intent =
				new RealtimeEnemyTactics.Intent();

		step(tactics, true, 5f, 7f, 0f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.FLANK_LEFT,
				intent.maneuver());
		assertTrue(Math.abs(intent.desiredY()) > 0.9f);
		assertTrue(Math.abs(intent.desiredX()) < 0.3f);

		float elapsed = 0f;
		while (elapsed < RealtimeEnemyTactics.FLANK_SECONDS + 0.2f) {
			step(tactics, true, 5f, 7f, 0f, 0f, intent);
			elapsed += RealtimeEnemyTactics.DECISION_PERIOD_SECONDS;
		}
		assertEquals(
				RealtimeEnemyTactics.Maneuver.ANCHOR_AND_SUPPRESS,
				intent.maneuver());
		assertTrue(tactics.maneuverCooldown() > 0f);
	}

	@Test
	public void rusherDashIsFastBoundedAndCannotChain() {
		RealtimeEnemyTactics tactics = tactics(
				RealtimeEnemyTactics.Profile.RUSHER, 0);
		RealtimeEnemyTactics.Intent intent =
				new RealtimeEnemyTactics.Intent();

		step(tactics, true, 5f, 1.1f, 1f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.DASH,
				intent.maneuver());
		assertEquals(
				RealtimeEnemyTactics.MAXIMUM_SPEED_MULTIPLIER,
				intent.speedMultiplier(),
				0f);
		assertEquals(1f, intent.desiredX(), 0.0001f);

		float elapsed = 0f;
		while (elapsed < RealtimeEnemyTactics.DASH_SECONDS + 0.2f) {
			step(tactics, true, 5f, 1.1f, 1f, 0f, intent);
			elapsed += RealtimeEnemyTactics.DECISION_PERIOD_SECONDS;
		}
		assertEquals(
				RealtimeEnemyTactics.Maneuver.FOLLOW_BRAIN,
				intent.maneuver());
		assertTrue(tactics.maneuverCooldown() > 0f);
		assertEquals(1f, intent.speedMultiplier(), 0f);
	}

	@Test
	public void skirmisherRetreatsFromCloseContactAndHoldsAtRange() {
		RealtimeEnemyTactics tactics = tactics(
				RealtimeEnemyTactics.Profile.RETREATING_SKIRMISHER, 0);
		RealtimeEnemyTactics.Intent intent =
				new RealtimeEnemyTactics.Intent();

		step(tactics, true, 2f, 6f, 1f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.RETREAT,
				intent.maneuver());
		assertEquals(-1f, intent.desiredX(), 0.0001f);

		RealtimeEnemyTactics fresh = tactics(
				RealtimeEnemyTactics.Profile.RETREATING_SKIRMISHER, 0);
		step(fresh, true, 5f, 6f, 0f, 0f, intent);
		assertEquals(
				RealtimeEnemyTactics.Maneuver.ANCHOR_AND_SUPPRESS,
				intent.maneuver());
	}

	@Test
	public void tacticalChoicesAreCappedAtEightHertzWithoutStoppingMotion() {
		RealtimeEnemyTactics tactics = tactics(
				RealtimeEnemyTactics.Profile.FLANKER, 0);
		RealtimeEnemyTactics.Intent intent =
				new RealtimeEnemyTactics.Intent();

		for (int i = 0; i < 100; i++) {
			tactics.step(
					0.01f,
					true,
					0f,
					0f,
					5f,
					0f,
					7f,
					1f,
					0f,
					intent);
			assertTrue(intent.speedMultiplier()
					<= RealtimeEnemyTactics.MAXIMUM_SPEED_MULTIPLIER);
		}

		assertTrue(tactics.decisionSequence() >= 8);
		assertTrue(tactics.decisionSequence() <= 9);
	}

	private static RealtimeEnemyTactics tactics(
			RealtimeEnemyTactics.Profile profile,
			int stableKey) {
		return new RealtimeEnemyTactics(profile, stableKey);
	}

	private static void step(
			RealtimeEnemyTactics tactics,
			boolean sight,
			float distance,
			float range,
			float brainX,
			float brainY,
			RealtimeEnemyTactics.Intent out) {
		tactics.step(
				RealtimeEnemyTactics.DECISION_PERIOD_SECONDS,
				sight,
				0f,
				0f,
				distance,
				0f,
				range,
				brainX,
				brainY,
				out);
	}

	private static EnemyArchetypeDefinition definition(
			EnemyRole role,
			String... abilities) {
		EnemyArchetypeDefinition definition =
				new EnemyArchetypeDefinition();
		definition.role = role;
		definition.abilities = abilities;
		return definition;
	}
}
