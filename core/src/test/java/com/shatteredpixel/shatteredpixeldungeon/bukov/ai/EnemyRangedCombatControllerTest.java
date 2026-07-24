package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.watabou.utils.Bundle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnemyRangedCombatControllerTest {

	@Test
	public void lineOfSightRangeAndAimGateTheFirstShot() {
		EnemyRangedCombatController controller = controller(3, 6, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		controller.step(0.1f, false, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.SEEK_TARGET, intent.action());

		controller.step(0.1f, true, 9f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.CLOSE_DISTANCE, intent.action());

		controller.step(0.1f, true, 3f, 4f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.AIM, intent.action());
		assertFalse(intent.hasDamageEvent());

		controller.step(0.1f, true, 3f, 4f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.FIRE, intent.action());
		assertTrue(intent.hasDamageEvent());
		assertEquals(0, intent.shotSequence());
		assertEquals(0.6f, intent.directionX(), 0.0001f);
		assertEquals(0.8f, intent.directionY(), 0.0001f);
		assertEquals(2, controller.magazineAmmo());
	}

	@Test
	public void rpmAndAimCooldownBothGateFollowUpShots() {
		EnemyRangedCombatController controller = controller(3, 6, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		controller.step(0.2f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.FIRE, intent.action());

		controller.step(0.2f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.HOLD_FIRE, intent.action());

		controller.step(0.3f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.FIRE, intent.action());
		assertEquals(1, intent.shotSequence());
	}

	@Test
	public void emptyMagazineReloadsOnlyRoundsAvailableInReserve() {
		EnemyRangedCombatController controller = controller(0, 2, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		controller.step(0f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.RELOAD, intent.action());
		assertTrue(intent.reloadStarted());
		assertEquals(0.5f, controller.reloadRemaining(), 0.0001f);

		controller.step(0.25f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.RELOAD, intent.action());
		assertFalse(intent.reloadCompleted());

		controller.step(0.25f, true, 2f, 0f, intent);
		assertTrue(intent.reloadCompleted());
		assertEquals(2, controller.magazineAmmo());
		assertEquals(0, controller.reserveAmmo());
	}

	@Test
	public void losingSightResetsUninterruptedAimRequirement() {
		EnemyRangedCombatController controller = controller(3, 6, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		controller.step(0.1f, true, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.AIM, intent.action());
		controller.step(0.01f, false, 2f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.SEEK_TARGET, intent.action());
		controller.step(0.1f, true, 2f, 0f, intent);

		assertEquals(EnemyRangedCombatIntent.Action.AIM, intent.action());
		assertFalse(intent.hasDamageEvent());
	}

	@Test
	public void openingWarningSurvivesSnapshotAndDelaysFirstDamage() {
		EnemyRangedCombatController original =
				onboardingController(5, 15, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		original.step(1f, true, 6f, 0f, intent);
		assertEquals(EnemyRangedCombatIntent.Action.AIM, intent.action());
		assertFalse(intent.hasDamageEvent());
		assertEquals(
				1.25f,
				original.openingWarningRemaining(),
				0.0001f);

		EnemyRangedCombatController restored =
				onboardingController(5, 15, 17);
		restored.restoreSnapshot(original.snapshot());
		assertEquals(
				original.openingWarningRemaining(),
				restored.openingWarningRemaining(),
				0f);

		float elapsed = 1f;
		float firstDamageAt = -1f;
		for (int i = 0; i < 3000 && firstDamageAt < 0f; i++) {
			restored.step(1f / 120f, true, 6f, 0f, intent);
			elapsed += 1f / 120f;
			if (intent.hasDamageEvent()) {
				firstDamageAt = elapsed;
			}
		}
		assertTrue(firstDamageAt >= 3f);
		assertTrue(firstDamageAt < 3.05f);
	}

	@Test
	public void openingWarningDoesNotDrainWithoutLineOfSight() {
		EnemyRangedCombatController controller =
				onboardingController(5, 15, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();

		controller.step(2f, false, 6f, 0f, intent);

		assertEquals(
				2.25f,
				controller.openingWarningRemaining(),
				0f);
		assertFalse(intent.hasDamageEvent());
	}

	@Test
	public void equalSeedsProduceEqualBoundedDamageEvents() {
		EnemyRangedCombatController first = controller(3, 6, 913);
		EnemyRangedCombatController second = controller(3, 6, 913);
		EnemyRangedCombatIntent firstIntent = new EnemyRangedCombatIntent();
		EnemyRangedCombatIntent secondIntent = new EnemyRangedCombatIntent();

		first.step(0.2f, true, 1f, 0f, firstIntent);
		second.step(0.2f, true, 1f, 0f, secondIntent);

		assertTrue(firstIntent.hasDamageEvent());
		assertEquals(firstIntent.damage(), secondIntent.damage());
		assertTrue(firstIntent.damage() >= 4);
		assertTrue(firstIntent.damage() <= 7);
	}

	@Test
	public void magazineSequenceAndShotCooldownResumeWithoutDrift() {
		EnemyRangedCombatController original = controller(3, 6, 913);
		EnemyRangedCombatIntent originalIntent =
				new EnemyRangedCombatIntent();
		original.step(0.2f, true, 1f, 0f, originalIntent);
		assertTrue(originalIntent.hasDamageEvent());

		Bundle bundle = new Bundle();
		bundle.put("ranged", original.snapshot());
		EnemyRangedCombatController.Snapshot snapshot =
				(EnemyRangedCombatController.Snapshot)bundle.get("ranged");
		EnemyRangedCombatController restored = controller(3, 6, 913);
		restored.restoreSnapshot(snapshot);

		assertEquals(original.magazineAmmo(), restored.magazineAmmo());
		assertEquals(original.reserveAmmo(), restored.reserveAmmo());
		assertEquals(original.shotSequence(), restored.shotSequence());
		assertEquals(original.shotCooldown(), restored.shotCooldown(), 0f);
		assertEquals(original.aimRemaining(), restored.aimRemaining(), 0f);
		assertEquals(original.targetLocked(), restored.targetLocked());

		EnemyRangedCombatIntent restoredIntent =
				new EnemyRangedCombatIntent();
		original.step(0.5f, true, 1f, 0f, originalIntent);
		restored.step(0.5f, true, 1f, 0f, restoredIntent);
		assertEquals(originalIntent.action(), restoredIntent.action());
		assertEquals(originalIntent.damage(), restoredIntent.damage());
		assertEquals(originalIntent.shotSequence(),
				restoredIntent.shotSequence());
		assertEquals(original.magazineAmmo(), restored.magazineAmmo());
	}

	@Test
	public void inFlightReloadResumesAtExactRemainingTime() {
		EnemyRangedCombatController original = controller(0, 2, 17);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();
		original.step(0f, true, 2f, 0f, intent);
		original.step(0.2f, true, 2f, 0f, intent);

		EnemyRangedCombatController restored = controller(0, 2, 17);
		restored.restoreSnapshot(original.snapshot());
		assertEquals(0.3f, restored.reloadRemaining(), 0.0001f);

		EnemyRangedCombatIntent restoredIntent =
				new EnemyRangedCombatIntent();
		original.step(0.29f, true, 2f, 0f, intent);
		restored.step(0.29f, true, 2f, 0f, restoredIntent);
		assertFalse(intent.reloadCompleted());
		assertFalse(restoredIntent.reloadCompleted());
		original.step(0.02f, true, 2f, 0f, intent);
		restored.step(0.02f, true, 2f, 0f, restoredIntent);
		assertTrue(intent.reloadCompleted());
		assertTrue(restoredIntent.reloadCompleted());
		assertEquals(original.magazineAmmo(), restored.magazineAmmo());
		assertEquals(original.reserveAmmo(), restored.reserveAmmo());
	}

	private static EnemyRangedCombatController controller(int magazine,
														 int reserve,
														 int seed) {
		return new EnemyRangedCombatController(
				new EnemyRangedCombatController.Config(
						3,
						120f,
						0.5f,
						6f,
						0.2f,
						4,
						7
				),
				magazine,
				reserve,
				seed
		);
	}

	private static EnemyRangedCombatController onboardingController(
			int magazine,
			int reserve,
			int seed) {
		return new EnemyRangedCombatController(
				new EnemyRangedCombatController.Config(
						5,
						150f,
						1.6f,
						6.5f,
						0.75f,
						2,
						3,
						2.25f),
				magazine,
				reserve,
				seed);
	}
}
