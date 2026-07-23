package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovRaidContainerPersistenceTest {

	@Test
	public void newRaidDefinitionsRoundTripCellStateAndContents() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = start(saves);
		assertEquals(2, raid.containers().size());
		assertEquals(41, raid.container("medical").cell);
		assertEquals("medical-table", raid.container("medical").lootTableId);

		assertTrue(raid.beginContainerSearch("medical"));
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				raid.updateContainerSearch(
						"medical",
						1.2f,
						true,
						false,
						false,
						table()));
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		BukovRaidCoordinator.ContainerSnapshot medical =
				resumed.container("medical");
		assertNotNull(medical);
		assertEquals(41, medical.cell);
		assertEquals(BukovSearchableContainer.State.SEARCHED, medical.state);
		assertEquals(2, medical.contentCount);
		assertFalse(medical.contentsReleased);
		assertEquals(BukovSearchableContainer.State.LOCKED,
				resumed.container("locked").state);
	}

	@Test
	public void releasedContentsCannotBeReleasedAgainAfterResume() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = start(saves);
		raid.beginContainerSearch("medical");
		raid.updateContainerSearch(
				"medical",
				1.2f,
				true,
				false,
				false,
				table());
		Heap firstHeap = new Heap();
		firstHeap.pos = 41;
		assertEquals(2, raid.releaseContainerContents("medical", firstHeap));
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		Heap secondHeap = new Heap();
		secondHeap.pos = 41;
		assertEquals(0, resumed.releaseContainerContents("medical", secondHeap));
		assertTrue(secondHeap.items.isEmpty());
		assertTrue(resumed.container("medical").contentsReleased);
		assertEquals(0, resumed.container("medical").contentCount);
	}

	@Test
	public void v2CheckpointMigratesToCurrentWithEmptyMissionState() {
		RaidSession session = RaidSession.create(8L, "legacy-v2");
		Bundle legacy = new Bundle();
		legacy.put("checkpoint_version", 2);
		legacy.put("session", session);
		legacy.put("loot", new LootTransaction("legacy-v2", 10f));
		legacy.put(
				"extractions",
				Collections.singletonList(ExtractionState.basic()));
		legacy.put("active_extraction", "");
		legacy.put("host_items", Collections.<Bundlable>emptyList());
		legacy.put("next_item_sequence", 0L);

		BukovRaidCheckpoint restored = new BukovRaidCheckpoint();
		restored.restoreFromBundle(legacy);

		assertEquals(BukovRaidCheckpoint.CURRENT_VERSION, restored.version());
		assertTrue(restored.containers().isEmpty());
		assertFalse(restored.eventCompleted(
				com.shatteredpixel.shatteredpixeldungeon.bukov.mission
						.FirstRaidMission.EVENT_ID));
	}

	@Test
	public void existingStartApiRemainsContainerFree() throws IOException {
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				new InMemoryBukovSaveService(),
				1L,
				"legacy-start",
				10f,
				Collections.singletonList(ExtractionState.basic()));

		assertTrue(raid.containers().isEmpty());
	}

	@Test
	public void oldCheckpointReceivesMissingWorldDefinitionsOnce()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				17L,
				"world-definition-migration",
				10f,
				Collections.singletonList(ExtractionState.basic()));
		BukovContainerDefinition container = new BukovContainerDefinition(
				"L01",
				61,
				"low",
				2,
				2.5f,
				false);

		assertTrue(raid.ensureWorldDefinitions(
				Arrays.asList(
						ExtractionState.basic(),
						ExtractionState.conditional(),
						ExtractionState.temporary(480f)),
				Collections.singletonList(container)));
		assertFalse(raid.ensureWorldDefinitions(
				Arrays.asList(
						ExtractionState.basic(),
						ExtractionState.conditional(),
						ExtractionState.temporary(480f)),
				Collections.singletonList(container)));

		BukovRaidCoordinator restored =
				BukovRaidCoordinator.resume(saves);
		assertEquals(3, restored.extractions().size());
		assertFalse(restored.extraction("E02").conditionMet());
		assertEquals(61, restored.container("L01").cell);
		assertEquals(2.5f, restored.container("L01").searchSeconds, 0f);
	}

	private static BukovRaidCoordinator start(BukovSaveService saves)
			throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				99L,
				"container-roundtrip",
				25f,
				Collections.singletonList(ExtractionState.basic()),
				Arrays.asList(
						new BukovContainerDefinition(
								"medical",
								41,
								"medical-table",
								2,
								1.2f,
								false),
						new BukovContainerDefinition(
								"locked",
								57,
								"medical-table",
								1,
								2f,
								true)));
	}

	private static BukovLootTable table() {
		return new BukovLootTable(
				"medical-table",
				Collections.singletonList(
						new BukovLootTable.Entry(
								"bandage",
								1,
								1,
								1,
								TestItem::new)));
	}

	public static class TestItem extends Item {
	}
}
