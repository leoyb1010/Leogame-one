package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovSearchableContainerTest {

	@Test
	public void searchProgressCompletesInsideConfiguredWindow() {
		BukovSearchableContainer container = container(false, 1.2f);
		assertTrue(container.begin());

		assertEquals(
				BukovSearchableContainer.UpdateResult.PROGRESSED,
				container.update(0.7f, true, false, false, table()));
		assertEquals(0.7f, container.progressSeconds(), 0.0001f);
		assertEquals(
				BukovSearchableContainer.UpdateResult.COMPLETED,
				container.update(0.5f, true, false, false, table()));
		assertEquals(BukovSearchableContainer.State.SEARCHED, container.state());
		assertEquals(3, container.contents().size());
	}

	@Test
	public void leavingMovingAndDamageInterruptAndResetProgress() {
		for (boolean[] input : new boolean[][]{
				{false, false, false},
				{true, true, false},
				{true, false, true}
		}) {
			BukovSearchableContainer container = container(false, 1f);
			assertTrue(container.begin());
			container.update(0.5f, true, false, false, table());

			assertEquals(
					BukovSearchableContainer.UpdateResult.INTERRUPTED,
					container.update(0.1f, input[0], input[1], input[2], table()));
			assertEquals(BukovSearchableContainer.State.INTERRUPTED, container.state());
			assertEquals(0f, container.progressSeconds(), 0f);
			assertTrue(container.begin());
		}
	}

	@Test
	public void reloadInterruptsAndResetsProgress() {
		BukovSearchableContainer container = container(false, 1f);
		assertTrue(container.begin());
		container.update(0.5f, true, false, false, table());

		assertEquals(
				BukovSearchableContainer.UpdateResult.INTERRUPTED,
				container.update(
						0.1f,
						true,
						false,
						false,
						true,
						table()));
		assertEquals(BukovSearchableContainer.State.INTERRUPTED, container.state());
		assertEquals(0f, container.progressSeconds(), 0f);
	}

	@Test
	public void lockedContainerRequiresExplicitUnlock() {
		BukovSearchableContainer container = container(true, 0.6f);

		assertFalse(container.begin());
		assertTrue(container.unlock());
		assertTrue(container.begin());
		assertFalse(container.unlock());
	}

	@Test
	public void deterministicTableAndBundleRestoreNeverRerollContents() {
		BukovLootTable table = table();
		BukovSearchableContainer first = container(false, 0.6f);
		BukovSearchableContainer second = container(false, 0.6f);
		first.begin();
		second.begin();
		first.update(0.6f, true, false, false, table);
		second.update(0.6f, true, false, false, table);
		String expected = signature(first.contents());

		assertEquals(expected, signature(second.contents()));

		Bundle bundle = new Bundle();
		bundle.put("container", first);
		BukovSearchableContainer restored =
				(BukovSearchableContainer) bundle.get("container");
		assertNotNull(restored);
		assertEquals(BukovSearchableContainer.State.SEARCHED, restored.state());
		assertEquals(expected, signature(restored.contents()));
		assertEquals(
				BukovSearchableContainer.UpdateResult.UNCHANGED,
				restored.update(10f, true, false, false, table));
		assertEquals(expected, signature(restored.contents()));
	}

	@Test
	public void releasedHeapFeedsExistingAdapterWithSameHostInstance()
			throws IOException {
		BukovSearchableContainer container = container(false, 0.6f);
		container.begin();
		container.update(0.6f, true, false, false, table());
		Item first = container.contents().get(0);
		Heap heap = new Heap();
		heap.pos = 9;

		assertEquals(3, container.releaseTo(heap));
		assertEquals(0, container.releaseTo(heap));
		assertSame(first, heap.peek());

		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				new InMemoryBukovSaveService(),
				44L,
				"container-adapter",
				100f,
				Collections.singletonList(ExtractionState.basic()));
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(
				raid,
				new BukovHeapLootAdapter.ItemEconomy() {
					@Override
					public float unitWeight(Item item) {
						return 0.25f;
					}

					@Override
					public int unitValue(Item item) {
						return 10;
					}
				});

		assertEquals(
				LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(heap));
		assertSame(first, adapter.carriedHostItem(first.bukovItemUid()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void searchDurationBelowMinimumIsRejected() {
		container(false, 0.59f);
	}

	private static BukovSearchableContainer container(
			boolean locked,
			float seconds) {
		return new BukovSearchableContainer(
				"medical-room-crate",
				"first-level",
				123456L,
				3,
				seconds,
				locked);
	}

	private static BukovLootTable table() {
		return new BukovLootTable(
				"first-level",
				Arrays.asList(
						new BukovLootTable.Entry(
								"bandage",
								3,
								1,
								2,
								BandageItem::new),
						new BukovLootTable.Entry(
								"ammo",
								2,
								2,
								4,
								AmmoItem::new)));
	}

	private static String signature(List<Item> items) {
		StringBuilder result = new StringBuilder();
		for (Item item : items) {
			result.append(item.getClass().getSimpleName())
					.append(':')
					.append(item.quantity())
					.append(';');
		}
		return result.toString();
	}

	public static class BandageItem extends Item {
	}

	public static class AmmoItem extends Item {
	}
}
