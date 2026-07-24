package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovMaintenanceCachePersistenceTest {

	@Test
	public void keyConsumptionContainerUnlockAndResumeAreOneCheckpoint()
			throws IOException {
		InMemoryBukovSaveService saves =
				new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				44117L,
				"maintenance-side-route",
				20f,
				Collections.singletonList(ExtractionState.basic()),
				Arrays.asList(
						new BukovContainerDefinition(
								BukovFirstRaidLootTables
										.MAINTENANCE_CACHE_CONTAINER_ID,
								40,
								BukovFirstRaidLootTables
										.MAINTENANCE_CACHE,
								3,
								3.2f,
								true),
						new BukovContainerDefinition(
								"critical-high-value",
								60,
								FirstRaidMission
										.HIGH_VALUE_LOOT_TABLE_ID,
								3,
								3f,
								true),
						new BukovContainerDefinition(
								FirstRaidMission
										.ARCHIVE_CONTAINER_ID,
								20,
								FirstRaidMission
										.ARCHIVE_LOOT_TABLE_ID,
								1,
								1.4f,
								false)));
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				raid.pickup(new RaidItem(
						"maintenance-keys",
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID,
						2,
						0.03f,
						460,
						true,
						false,
						1f)));

		assertEquals(
				BukovKeyDoorState.UnlockResult.UNLOCKED,
				raid.session().keyDoors().unlock(
						BukovFirstRaidLootTables
								.MAINTENANCE_CACHE_DOOR_ID,
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID,
						raid.loot()));
		assertTrue(raid.unlockContainer(
				BukovFirstRaidLootTables
						.MAINTENANCE_CACHE_CONTAINER_ID));
		assertEquals(1,
				raid.loot().item("maintenance-keys").quantity());
		assertEquals(
				BukovSearchableContainer.State.LOCKED,
				raid.container("critical-high-value").state);
		assertFalse(raid.eventCompleted(FirstRaidMission.EVENT_ID));
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertTrue(resumed.session().keyDoors().unlocked(
				BukovFirstRaidLootTables
						.MAINTENANCE_CACHE_DOOR_ID));
		assertEquals(
				BukovSearchableContainer.State.UNSEARCHED,
				resumed.container(
						BukovFirstRaidLootTables
								.MAINTENANCE_CACHE_CONTAINER_ID)
						.state);
		assertEquals(
				BukovKeyDoorState.UnlockResult.ALREADY_UNLOCKED,
				resumed.session().keyDoors().unlock(
						BukovFirstRaidLootTables
								.MAINTENANCE_CACHE_DOOR_ID,
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID,
						resumed.loot()));
		assertEquals(1,
				resumed.loot().item("maintenance-keys").quantity());
	}
}
