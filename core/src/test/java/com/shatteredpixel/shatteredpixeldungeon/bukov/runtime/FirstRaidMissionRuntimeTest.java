package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
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
}
