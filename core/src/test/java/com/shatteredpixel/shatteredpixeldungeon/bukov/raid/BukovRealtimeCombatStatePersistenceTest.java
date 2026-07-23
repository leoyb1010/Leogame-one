package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatIntent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeMedicalSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeStatusState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovRealtimeCombatStatePersistenceTest {

	@Test
	public void coordinatorRoundTripKeepsPlayerAndEnemyRuntimeExact()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				884422L,
				"runtime-state",
				30f,
				Collections.singletonList(ExtractionState.basic()));
		raid.loot().pickup(new RaidItem(
				"bandage-uid",
				"bandage",
				2,
				0.1f,
				100,
				false,
				false,
				1f));

		RealtimeStatusState status = new RealtimeStatusState(100f, 62f);
		status.addBleeding(0.8f);
		status.setFractured(true);
		status.addPain(0.4f);
		status.addConcussion(7f);
		RealtimeMedicalSystem medical =
				RealtimeMedicalSystem.fromLedger(raid.loot(), status);
		assertEquals(RealtimeMedicalSystem.BeginResult.STARTED,
				medical.beginUse("bandage-uid"));
		assertEquals(RealtimeMedicalSystem.StepResult.IN_PROGRESS,
				medical.fixedStep(0.5f, false, false, false));
		medical.writeBack(raid.loot());

		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(77);
		brain.recordSound(8f, 3f);
		brain.perceptionDue(0.2f);
		brain.decide(0.1f, 1f, 1f, 20f, 20f, 1f);
		EnemyRangedCombatController ranged = ranged(5, 10, 77);
		EnemyRangedCombatIntent intent = new EnemyRangedCombatIntent();
		ranged.step(0.3f, true, 2f, 0f, intent);

		raid.updateRealtimeState(
				status,
				medical.snapshot(),
				Collections.singletonList(
						new BukovRaidCheckpoint.EnemyRuntimeState(
								77,
								"scavenger_gunner",
								brain.snapshot(),
								ranged.snapshot())));
		raid.saveCheckpoint();

		BukovRaidCoordinator restored =
				BukovRaidCoordinator.resume(saves);
		assertEquals(status.health(),
				restored.realtimeStatus().health(), 0f);
		assertEquals(status.bleedingPerSecond(),
				restored.realtimeStatus().bleedingPerSecond(), 0f);
		assertTrue(restored.realtimeStatus().fractured());
		assertEquals(0.5f,
				restored.medicalRuntime().activeElapsed(), 0f);
		assertEquals("bandage-uid",
				restored.medicalRuntime().activeItemUid());
		assertEquals(2,
				restored.loot().item("bandage-uid").quantity());

		BukovRaidCheckpoint.EnemyRuntimeState enemy =
				restored.enemyRuntime(77);
		assertEquals("scavenger_gunner", enemy.definitionId());
		RealtimeEnemyBrain restoredBrain = new RealtimeEnemyBrain(77);
		restoredBrain.restoreSnapshot(enemy.brain());
		assertEquals(RealtimeEnemyBrain.State.INVESTIGATE,
				restoredBrain.state());
		assertTrue(restoredBrain.investigatingSound());
		assertEquals(brain.lastSeenAge(),
				restoredBrain.lastSeenAge(), 0f);
		assertEquals(ranged.magazineAmmo(),
				enemy.rangedCombat().magazineAmmo());
		assertEquals(ranged.reserveAmmo(),
				enemy.rangedCombat().reserveAmmo());
		assertEquals(ranged.shotSequence(),
				enemy.rangedCombat().shotSequence());

		RealtimeMedicalSystem resumedMedical =
				RealtimeMedicalSystem.fromLedger(
						restored.loot(), restored.realtimeStatus());
		resumedMedical.restoreSnapshot(restored.medicalRuntime());
		assertEquals(2, resumedMedical.quantity("bandage-uid"));
		assertEquals(medical.useProgress(),
				resumedMedical.useProgress(), 0f);
	}

	@Test
	public void versionFiveMigratesToSafeEmptyRuntimeDefaults() {
		Bundle legacy = new Bundle();
		legacy.put("checkpoint_version", 5);
		legacy.put("session", RaidSession.create(5L, "legacy-v5"));
		legacy.put("loot", new LootTransaction("legacy-v5", 10f));
		legacy.put("extractions",
				Collections.singletonList(ExtractionState.basic()));
		legacy.put("active_extraction", "");
		legacy.put("host_items", Collections.<Bundlable>emptyList());
		legacy.put("next_item_sequence", 0L);
		legacy.put("containers", Collections.<Bundlable>emptyList());
		legacy.put("completed_events", new String[0]);
		legacy.put("deployment_definitions", new String[0]);

		BukovRaidCheckpoint restored = new BukovRaidCheckpoint();
		restored.restoreFromBundle(legacy);

		assertEquals(BukovRaidCheckpoint.CURRENT_VERSION,
				restored.version());
		assertNull(restored.playerStatus());
		assertNull(restored.medicalRuntime());
		assertTrue(restored.enemyRuntimeStates().isEmpty());
	}

	private static EnemyRangedCombatController ranged(
			int magazine,
			int reserve,
			int seed) {
		return new EnemyRangedCombatController(
				new EnemyRangedCombatController.Config(
						5,
						150f,
						1.6f,
						6f,
						0.3f,
						4,
						7),
				magazine,
				reserve,
				seed);
	}
}
