package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovSearchableContainer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FirstRaidMissionRuntimeTest {

	@Test
	public void archiveTableNeverOmitsObjectiveItem() {
		for (long seed = 0; seed < 256; seed++) {
			List<Item> rolled = BukovFirstRaidLootTables
					.require(FirstRaidMission.ARCHIVE_LOOT_TABLE_ID)
					.roll(seed, FirstRaidMission.ARCHIVE_CONTAINER_ID, 1);
			assertEquals(1, rolled.size());
			assertTrue(rolled.get(0) instanceof BukovMissionArchive);
		}
	}

	@Test
	public void archiveCannotBeDroppedAfterPickup() throws IOException {
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				new InMemoryBukovSaveService(),
				44L,
				"mission-runtime",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(raid);
		Heap source = new Heap();
		source.pos = 17;
		source.items.add(new BukovMissionArchive());

		assertEquals(
				com.shatteredpixel.shatteredpixeldungeon.bukov.raid
						.LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(source));
		assertEquals(
				BukovHeapLootAdapter.DropResult.PROTECTED_ITEM,
				adapter.drop(raid.loot().items().get(0).itemUid(), new Heap()));
		assertEquals(1, raid.loot().distinctItemCount());
	}

	@Test
	public void searchedArchiveBecomesCarriedMissionStateAndSurvivesResume()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				45L,
				"mission-production-chain",
				40f,
				Collections.singletonList(ExtractionState.basic()),
				Collections.singletonList(new BukovContainerDefinition(
						FirstRaidMission.ARCHIVE_CONTAINER_ID,
						17,
						FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
						1,
						1.4f,
						false)));

		assertTrue(raid.beginContainerSearch(
				FirstRaidMission.ARCHIVE_CONTAINER_ID));
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				raid.updateContainerSearch(
						FirstRaidMission.ARCHIVE_CONTAINER_ID,
						1.4f,
						true,
						false,
						false,
						BukovFirstRaidLootTables.require(
								FirstRaidMission.ARCHIVE_LOOT_TABLE_ID)));

		Heap released = new Heap();
		released.pos = 17;
		assertEquals(
				1,
				raid.releaseContainerContents(
						FirstRaidMission.ARCHIVE_CONTAINER_ID,
						released));
		assertTrue(released.peek() instanceof BukovMissionArchive);
		assertEquals(
				com.shatteredpixel.shatteredpixeldungeon.bukov.raid
						.LootTransaction.PickupResult.ADDED,
				new BukovHeapLootAdapter(raid).pickupTop(released));
		assertTrue(raid.loot().containsDefinition(
				FirstRaidMission.ARCHIVE_DEFINITION_ID));
		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));

		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertTrue(resumed.eventCompleted(FirstRaidMission.EVENT_ID));
		assertTrue(resumed.loot().containsDefinition(
				FirstRaidMission.ARCHIVE_DEFINITION_ID));
	}

	@Test
	public void objectiveInteractionsNameTheArchiveAndHighValueSteps() {
		Heap archiveHeap = new Heap();
		archiveHeap.items.add(new BukovMissionArchive());

		assertEquals(
				"搜索维修间档案柜",
				BukovRealtimeWorld.containerSearchLabel(
						FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
						false));
		assertEquals(
				"正在搜索维修间档案柜",
				BukovRealtimeWorld.containerSearchLabel(
						FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
						true));
		assertEquals(
				"搜查高价值仓",
				BukovRealtimeWorld.containerSearchLabel(
						FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID,
						false));
		assertEquals(
				"拾取维修通道档案",
				BukovRealtimeWorld.heapPickupLabel(archiveHeap));
		assertEquals(
				"拾取物资",
				BukovRealtimeWorld.heapPickupLabel(new Heap()));
	}

	@Test
	public void gateHintTargetsTheNearestCellOfAWidePassage() {
		int width = 12;
		int length = 120;
		int[] gateCells = {40, 41, 42};

		assertEquals(
				42,
				BukovRealtimeWorld.nearestMissionGateCell(
						43, width, length, gateCells));
		assertEquals(
				40,
				BukovRealtimeWorld.nearestMissionGateCell(
						39, width, length, gateCells));
		assertEquals(
				-1,
				BukovRealtimeWorld.nearestMissionGateCell(
						39, width, length, new int[0]));
	}
}
