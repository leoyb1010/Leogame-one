package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovActiveRaidRecoveryTest {

	@Test
	public void legacyEmptyRaidReceivesOneDurableRuntimeKit()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		saveLegacyCheckpoint(saves, "legacy-empty");

		BukovRaidCoordinator recovered =
				BukovRaidCoordinator.resume(saves);

		assertTrue(recovered.emergencyLoadoutRecovered());
		assertEquals(2, recovered.loot().distinctItemCount());
		assertTrue(recovered.loot().containsDefinition(
				BukovActiveRaidRecovery.WEAPON_DEFINITION));
		assertTrue(recovered.loot().containsDefinition(
				BukovActiveRaidRecovery.AMMO_DEFINITION));
		assertEquals(
				BukovActiveRaidRecovery.AMMUNITION,
				quantity(
						recovered.loot(),
						BukovActiveRaidRecovery.AMMO_DEFINITION));

		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(recovered);
		assertNotNull(runtime.primaryWeapon());
		assertEquals("needle_9", runtime.primaryWeapon().definitionId());
		assertEquals(12, runtime.primaryWeapon().magazineAmmo());
		assertEquals(24, runtime.reserveAmmo().get(0).quantity());

		BukovRaidCoordinator replay =
				BukovRaidCoordinator.resume(saves);
		assertFalse(replay.emergencyLoadoutRecovered());
		assertEquals(2, replay.loot().distinctItemCount());
	}

	@Test
	public void currentAndArmedLegacyRaidsNeverReceiveExtraItems()
			throws IOException {
		BukovSaveService currentSaves = new InMemoryBukovSaveService();
		BukovRaidCoordinator.start(
				currentSaves,
				7L,
				"current-empty",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		BukovRaidCoordinator current =
				BukovRaidCoordinator.resume(currentSaves);
		assertFalse(current.emergencyLoadoutRecovered());
		assertEquals(0, current.loot().distinctItemCount());

		BukovSaveService armedSaves = new InMemoryBukovSaveService();
		saveLegacyCheckpoint(
				armedSaves,
				"legacy-armed",
				new RaidItem(
						"owned-gun",
						"firearm:needle_9",
						1,
						0.9f,
						850,
						false,
						false,
						1f));
		BukovRaidCoordinator armed =
				BukovRaidCoordinator.resume(armedSaves);
		assertFalse(armed.emergencyLoadoutRecovered());
		assertEquals(1, armed.loot().distinctItemCount());

		armed.drop("owned-gun");
		armed.saveCheckpoint();
		BukovRaidCoordinator afterPlayerDrop =
				BukovRaidCoordinator.resume(armedSaves);
		assertFalse(afterPlayerDrop.emergencyLoadoutRecovered());
		assertEquals(0, afterPlayerDrop.loot().distinctItemCount());
	}

	@Test
	public void emergencyKitNeverEntersSuccessOrDeathEconomy()
			throws IOException {
		BukovSaveService successSaves = new InMemoryBukovSaveService();
		saveLegacyCheckpoint(successSaves, "legacy-success");
		LootTransaction successLoot =
				BukovRaidCoordinator.resume(successSaves).loot();
		BukovProfile successProfile = new BukovProfile();

		RaidResult success = new RaidSettlement().settle(
				successProfile,
				successLoot,
				RaidOutcome.SUCCESS);

		assertEquals(0L, success.transferredQuantity());
		assertEquals(0L, success.transferredValue());
		assertEquals(0, successProfile.stash().distinctItemCount());

		BukovSaveService deathSaves = new InMemoryBukovSaveService();
		saveLegacyCheckpoint(deathSaves, "legacy-death");
		LootTransaction deathLoot =
				BukovRaidCoordinator.resume(deathSaves).loot();
		BukovProfile deathProfile = new BukovProfile();

		RaidResult death = new RaidSettlement().settle(
				deathProfile,
				deathLoot,
				RaidOutcome.DEATH);

		assertEquals(0L, death.lostQuantity());
		assertEquals(0L, death.lostValue());
		assertEquals(0, deathProfile.stash().distinctItemCount());
	}

	private static void saveLegacyCheckpoint(
			BukovSaveService saves,
			String raidId,
			RaidItem... items) throws IOException {
		LootTransaction loot = new LootTransaction(raidId, 40f);
		for (RaidItem item : items) {
			assertEquals(
					LootTransaction.PickupResult.ADDED,
					loot.pickup(item));
		}
		saves.saveProfile(new BukovProfile());
		saves.saveRaidCheckpoint(new BukovRaidCheckpoint(
				RaidSession.create(19L, raidId),
				loot,
				Collections.singletonList(ExtractionState.basic())));
	}

	private static int quantity(
			LootTransaction loot,
			String definitionId) {
		for (RaidItem item : loot.items()) {
			if (definitionId.equals(item.definitionId())) {
				return item.quantity();
			}
		}
		return 0;
	}

	private static BukovRuntimeLoadoutAdapter adapter()
			throws IOException {
		FirearmRegistry firearms = new FirearmRegistry();
		firearms.loadJson(read("firearms.json"));
		AmmoRegistry ammunition = new AmmoRegistry();
		ammunition.loadJson(read("ammunition.json"));
		return new BukovRuntimeLoadoutAdapter(firearms, ammunition);
	}

	private static String read(String fileName) throws IOException {
		java.nio.file.Path path = Paths.get(
				"src/main/assets/bukov/content/" + fileName);
		if (!Files.exists(path)) {
			path = Paths.get(
					"core/src/main/assets/bukov/content/" + fileName);
		}
		return new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8);
	}
}
