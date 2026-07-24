package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovScavengerKitFlowTest {

	@Test
	public void newScavengerRaidGetsOneCompleteDisposableKit()
			throws IOException {
		BukovSaveService saves = scavengerProfile();
		BukovRaidCoordinator raid = start(saves, "scav-new");

		assertEquals(BukovRaidMode.SCAVENGER, raid.session().raidMode());
		assertEquals(4, raid.loot().distinctItemCount());
		assertEquals(
				BukovScavengerKit.weightCapacityKg(),
				raid.loot().maxWeight(),
				0f);
		assertIssued(
				raid,
				BukovScavengerKit.FIREARM_DEFINITION,
				1);
		assertIssued(
				raid,
				BukovScavengerKit.AMMO_DEFINITION,
				BukovScavengerKit.AMMO_QUANTITY);
		assertIssued(
				raid,
				BukovScavengerKit.MEDICAL_DEFINITION,
				BukovScavengerKit.MEDICAL_QUANTITY);
		assertIssued(
				raid,
				BukovScavengerKit.BACKPACK_DEFINITION,
				1);
		assertEquals(
				BukovScavengerKit.BACKPACK_DEFINITION,
				BukovGearRules.resolve(raid.loot().items())
						.backpack.definitionId());

		BukovProfile deployed = saves.loadProfile();
		assertEquals(3, deployed.stash().distinctItemCount());
		assertEquals(0, deployed.loadout().distinctItemCount());
		for (RaidItem item : deployed.stash().items()) {
			assertFalse(BukovScavengerKit.issuedItem(item));
		}
	}

	@Test
	public void checkpointResumeReusesKitWithoutIssuingAgain()
			throws IOException {
		BukovSaveService saves = scavengerProfile();
		BukovRaidCoordinator started = start(saves, "scav-resume");
		String fingerprint = started.loot().fingerprint();
		started.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);

		assertNotNull(resumed);
		assertEquals(4, resumed.loot().distinctItemCount());
		assertEquals(fingerprint, resumed.loot().fingerprint());
		assertEquals(
				1,
				countIssued(
						resumed,
						BukovScavengerKit.FIREARM_DEFINITION));
		assertEquals(
				1,
				countIssued(
						resumed,
						BukovScavengerKit.AMMO_DEFINITION));
	}

	@Test
	public void extractionReturnsOnlyFoundInRaidLoot()
			throws IOException {
		BukovSaveService saves = scavengerProfile();
		BukovRaidCoordinator raid = start(saves, "scav-success");
		raid.pickup(foundItem("scav-found-success", 2, 700));
		completeExtraction(raid);

		RaidResult result = raid.settleSuccess();
		BukovProfile settled = saves.loadProfile();

		assertEquals(2L, result.transferredQuantity());
		assertEquals(812L, result.transferredValue());
		assertEquals(1, result.transferredUids().size());
		assertTrue(settled.stash().contains("scav-found-success"));
		assertFalse(
				settled.stash().item("scav-found-success").foundInRaid());
		assertNoIssuedItems(settled);
		assertNull(saves.loadRaidCheckpoint());
	}

	@Test
	public void deathRemovesIssuedKitAndFoundLoot()
			throws IOException {
		BukovSaveService saves = scavengerProfile();
		BukovRaidCoordinator raid = start(saves, "scav-death");
		raid.pickup(foundItem("scav-found-death", 2, 700));

		RaidResult result = raid.settleDeath();
		BukovProfile settled = saves.loadProfile();

		assertEquals(0L, result.transferredQuantity());
		assertEquals(2L, result.lostQuantity());
		assertEquals(1, result.lostUids().size());
		assertFalse(settled.stash().contains("scav-found-death"));
		assertNoIssuedItems(settled);
		assertNull(saves.loadRaidCheckpoint());
	}

	@Test
	public void repeatedSettlementReplaysWithoutMintingIssuedGear() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("scav-idempotent", 40f);
		BukovScavengerKit.grant(loot, "scav-idempotent");
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				loot.pickup(foundItem("idempotent-found", 1, 500)));
		RaidSettlement settlement = new RaidSettlement();

		RaidResult first = settlement.settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				60f,
				0,
				false,
				BukovRaidMode.SCAVENGER);
		RaidResult replay = settlement.settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				60f,
				0,
				false,
				BukovRaidMode.SCAVENGER);

		assertFalse(first.replayed());
		assertTrue(replay.replayed());
		assertEquals(1, profile.stash().distinctItemCount());
		assertEquals(290L, profile.stash().totalValue());
		assertNoIssuedItems(profile);
	}

	private static BukovSaveService scavengerProfile() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = saves.loadProfile();
		assertTrue(BukovStarterProvisioning.ensure(profile));
		profile.selectRaidMode(BukovRaidMode.SCAVENGER);
		saves.saveProfile(profile);
		return saves;
	}

	private static BukovRaidCoordinator start(
			BukovSaveService saves,
			String raidId) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				123L,
				raidId,
				40f,
				Collections.singletonList(ExtractionState.basic()));
	}

	private static void completeExtraction(BukovRaidCoordinator raid) {
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
	}

	private static RaidItem foundItem(
			String uid,
			int quantity,
			int unitValue) {
		return new RaidItem(
				uid,
				"loot:test",
				quantity,
				0.10f,
				unitValue,
				true,
				false,
				1f);
	}

	private static void assertIssued(
			BukovRaidCoordinator raid,
			String definitionId,
			int quantity) {
		RaidItem item = findByDefinition(raid, definitionId);
		assertEquals(quantity, item.quantity());
		assertEquals(0, item.unitValue());
		assertFalse(item.foundInRaid());
		assertFalse(item.insured());
		assertTrue(BukovScavengerKit.issuedItem(item));
	}

	private static RaidItem findByDefinition(
			BukovRaidCoordinator raid,
			String definitionId) {
		for (RaidItem item : raid.loot().items()) {
			if (definitionId.equals(item.definitionId())) {
				return item;
			}
		}
		throw new AssertionError("Missing raid item: " + definitionId);
	}

	private static int countIssued(
			BukovRaidCoordinator raid,
			String definitionId) {
		int count = 0;
		for (RaidItem item : raid.loot().items()) {
			if (definitionId.equals(item.definitionId())
					&& BukovScavengerKit.issuedItem(item)) {
				count++;
			}
		}
		return count;
	}

	private static void assertNoIssuedItems(BukovProfile profile) {
		for (RaidItem item : profile.stash().items()) {
			assertFalse(BukovScavengerKit.issuedItem(item));
		}
	}
}
