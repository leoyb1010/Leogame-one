package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubViewModel;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Behavioral acceptance gate for the complete lifecycle shared by every
 * selectable Bukov mode. This deliberately drives public application APIs
 * instead of inferring support from enum values or source-code strings.
 */
@RunWith(Parameterized.class)
public class BukovFiveModeLifecycleTest {

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> modes() {
		return Arrays.asList(new Object[][]{
				{BukovRaidMode.EXPEDITION},
				{BukovRaidMode.QUICK_SWEEP},
				{BukovRaidMode.SCAVENGER},
				{BukovRaidMode.BOSS_CONTRACT},
				{BukovRaidMode.TRAINING_GROUND}
		});
	}

	private final BukovRaidMode mode;

	public BukovFiveModeLifecycleTest(BukovRaidMode mode) {
		this.mode = mode;
	}

	@Test
	public void selectionLoadoutEntryObjectiveExtractionAndSettlement()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.selectRaidMode(mode);
		BukovHubViewModel deployment = hub.viewModel();
		assertEquals(mode, deployment.raidMode);
		assertTrue(deployment.canDeploy);
		assertNull(deployment.deploymentBlockReason);
		if (mode.usesPlayerLoadout()) {
			assertTrue(deployment.selectedCount >= 2);
		} else {
			assertEquals(0, deployment.selectedCount);
		}
		hub.confirmDeployment();
		assertEquals(mode, saves.loadProfile().selectedRaidMode());

		String raidId = "five-mode-" + mode.name().toLowerCase();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				441199L + mode.ordinal(),
				raidId,
				BukovHubController.FIRST_RAID_WEIGHT_LIMIT,
				Arrays.asList(
						ExtractionState.basic(),
						ExtractionState.conditional()),
				firstRaidContainers());

		assertEquals(mode, raid.session().raidMode());
		assertNotNull(saves.loadRaidCheckpoint());
		assertEntryKitPolicy(raid, saves.loadProfile());

		raid.pickup(foundItem(raidId + "-found"));
		if (mode.trainingGround()) {
			assertFalse(raid.firstRaidMissionActive());
			assertEquals(FirstRaidMission.Stage.EXTRACT,
					raid.firstRaidStage());
			assertTrue(raid.beginExtraction("E01"));
			raid.tick(5f, ExtractionState.Interaction.ACTIVE);
		} else {
			completeFirstRaidObjectiveAndConditionalExtraction(raid, mode);
		}

		RaidResult result = raid.settleSuccess();
		BukovProfile settled = saves.loadProfile();
		SettlementReceipt receipt = settled.settlement(raidId);

		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertFalse(result.replayed());
		assertTrue(settled.isSettled(raidId));
		assertNull(saves.loadRaidCheckpoint());
		assertNotNull(receipt);
		assertEquals(RaidOutcome.SUCCESS, receipt.outcome());
		assertTrue(receipt.debriefAvailable());
		assertEquals(
				!mode.trainingGround()
						&& mode != BukovRaidMode.BOSS_CONTRACT,
				receipt.missionCompleted());
		assertSettlementPolicy(raidId, result, settled);
	}

	private void assertEntryKitPolicy(
			BukovRaidCoordinator raid,
			BukovProfile deployed) {
		assertEquals(0, deployed.loadout().distinctItemCount());
		if (mode == BukovRaidMode.SCAVENGER) {
			assertEquals(4, raid.loot().distinctItemCount());
			assertTrue(hasDefinition(
					raid, BukovScavengerKit.FIREARM_DEFINITION));
			assertTrue(hasDefinition(
					raid, BukovScavengerKit.AMMO_DEFINITION));
			assertTrue(hasDefinition(
					raid, BukovScavengerKit.MEDICAL_DEFINITION));
			assertTrue(hasDefinition(
					raid, BukovScavengerKit.BACKPACK_DEFINITION));
		} else if (mode.trainingGround()) {
			assertEquals(2, raid.loot().distinctItemCount());
			assertTrue(hasDefinition(
					raid,
					BukovRaidCoordinator.TRAINING_FIREARM_DEFINITION));
			assertTrue(hasDefinition(
					raid,
					BukovRaidCoordinator.TRAINING_AMMO_DEFINITION));
			assertEquals(0, deployed.raidsStarted());
		} else {
			assertTrue(raid.loot().distinctItemCount() >= 2);
			assertTrue(hasDefinitionPrefix(raid, "firearm:"));
			assertTrue(hasDefinitionPrefix(raid, "ammo:"));
		}
	}

	private static void completeFirstRaidObjectiveAndConditionalExtraction(
			BukovRaidCoordinator raid,
			BukovRaidMode mode) throws IOException {
		assertTrue(raid.firstRaidMissionActive());
		assertEquals(
				FirstRaidMission.Stage.RECOVER_ARCHIVE,
				raid.firstRaidStage());

		// Even a powered conditional point must remain blocked by the mission.
		raid.setExtractionCondition(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID, true);
		assertFalse(raid.beginExtraction(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID));

		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));
		assertEquals(
				FirstRaidMission.Stage.SECURE_HIGH_VALUE_CACHE,
				raid.firstRaidStage());
		assertTrue(raid.beginContainerSearch("L01"));
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				raid.updateContainerSearch(
						"L01",
						5f,
						true,
						false,
						false,
						highValueTable()));
		assertEquals(
				FirstRaidMission.Stage.EXTRACT,
				raid.firstRaidStage());
		if (mode == BukovRaidMode.BOSS_CONTRACT) {
			assertFalse(raid.bossContractCompleted());
		}
		assertTrue(raid.beginExtraction(
				FirstRaidMission.CONDITIONAL_EXTRACTION_ID));
		raid.tick(8f, ExtractionState.Interaction.ACTIVE);
	}

	private void assertSettlementPolicy(
			String foundUid,
			RaidResult result,
			BukovProfile settled) {
		String itemUid = foundUid + "-found";
		if (mode.trainingGround()) {
			assertEquals(0L, result.transferredQuantity());
			assertFalse(settled.stash().contains(itemUid));
			assertEquals(0, settled.raidsStarted());
			assertEquals(0, settled.statistics().successfulRaids());
			for (RaidItem item : settled.stash().items()) {
				assertFalse(item.itemUid().startsWith("training:"));
			}
			return;
		}

		assertTrue(result.transferredUids().contains(itemUid));
		assertTrue(settled.stash().contains(itemUid));
		assertEquals(
				Math.round(1_000d * mode.lootValueMultiplier),
				settled.stash().item(itemUid).unitValue());
		assertEquals(1, settled.raidsStarted());
		assertEquals(1, settled.statistics().successfulRaids());
		if (mode == BukovRaidMode.SCAVENGER) {
			for (RaidItem item : settled.stash().items()) {
				assertFalse(BukovScavengerKit.issuedItem(item));
			}
		}
	}

	private static Collection<BukovContainerDefinition>
			firstRaidContainers() {
		return Arrays.asList(
				new BukovContainerDefinition(
						FirstRaidMission.ARCHIVE_CONTAINER_ID,
						31,
						FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
						1,
						1.4f,
						false),
				new BukovContainerDefinition(
						"L01",
						87,
						FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
						2,
						2f,
						false));
	}

	private static BukovLootTable highValueTable() {
		return new BukovLootTable(
				FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
				Collections.singletonList(
						new BukovLootTable.Entry(
								"five-mode-cache",
								1,
								1,
								1,
								TestItem::new)));
	}

	private static RaidItem foundItem(String uid) {
		return new RaidItem(
				uid,
				"loot:five_mode_evidence",
				1,
				0.1f,
				1_000,
				true,
				false,
				1f);
	}

	private static boolean hasDefinition(
			BukovRaidCoordinator raid,
			String definitionId) {
		for (RaidItem item : raid.loot().items()) {
			if (definitionId.equals(item.definitionId())) return true;
		}
		return false;
	}

	private static boolean hasDefinitionPrefix(
			BukovRaidCoordinator raid,
			String prefix) {
		for (RaidItem item : raid.loot().items()) {
			if (item.definitionId().startsWith(prefix)) return true;
		}
		return false;
	}

	public static class TestItem extends Item {
	}
}
