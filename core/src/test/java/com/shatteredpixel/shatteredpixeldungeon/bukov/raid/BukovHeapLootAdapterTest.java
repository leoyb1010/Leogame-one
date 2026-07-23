package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovHeapLootAdapterTest {

	private static final BukovHeapLootAdapter.ItemEconomy TEST_ECONOMY =
			new BukovHeapLootAdapter.ItemEconomy() {
				@Override
				public float unitWeight(Item item) {
					return 0.5f;
				}

				@Override
				public int unitValue(Item item) {
					return 25;
				}
			};

	@After
	public void resetHostState() {
		Dungeon.hero = null;
		Dungeon.quickslot.reset();
	}

	@Test
	public void pickupMovesExactHostInstanceIntoCheckpoint() throws IOException {
		BukovRaidCoordinator raid = raid(new InMemoryBukovSaveService(), 10f);
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(raid, TEST_ECONOMY);
		TestItem item = new TestItem().quantityOf(3);
		Heap heap = heap(12, item);

		assertEquals(
				LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(heap));

		String uid = item.bukovItemUid();
		assertNotNull(uid);
		assertTrue(heap.items.isEmpty());
		assertSame(item, adapter.carriedHostItem(uid));
		assertEquals(3L, raid.loot().totalQuantity());
		assertEquals(1.5f, raid.loot().totalWeight(), 0.0001f);
		assertEquals(75L, raid.loot().totalValue());
	}

	@Test
	public void checkpointRestorePreservesUidAndHostItemThenAllowsDropRepick()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = raid(saves, 10f);
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(raid, TEST_ECONOMY);
		TestItem original = new TestItem().quantityOf(2);
		adapter.pickupTop(heap(7, original));
		String uid = original.bukovItemUid();
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		BukovHeapLootAdapter resumedAdapter =
				new BukovHeapLootAdapter(resumed, TEST_ECONOMY);
		Item restoredHost = resumedAdapter.carriedHostItem(uid);
		assertNotNull(restoredHost);
		assertEquals(uid, restoredHost.bukovItemUid());
		assertEquals(2, restoredHost.quantity());

		Heap target = new Heap();
		target.pos = 31;
		assertEquals(
				BukovHeapLootAdapter.DropResult.DROPPED,
				resumedAdapter.drop(uid, target));
		assertSame(restoredHost, target.peek());
		assertFalse(resumed.loot().contains(uid));

		assertEquals(
				LootTransaction.PickupResult.ADDED,
				resumedAdapter.pickupTop(target));
		assertEquals(uid, target.items.isEmpty()
				? resumedAdapter.carriedHostItem(uid).bukovItemUid()
				: null);
	}

	@Test
	public void overweightPickupLeavesHeapAndHostInstanceUntouched()
			throws IOException {
		BukovRaidCoordinator raid = raid(new InMemoryBukovSaveService(), 0.25f);
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(raid, TEST_ECONOMY);
		TestItem item = new TestItem();
		Heap heap = heap(4, item);

		assertEquals(
				LootTransaction.PickupResult.OVERWEIGHT,
				adapter.pickupTop(heap));

		assertSame(item, heap.peek());
		assertEquals(0, raid.loot().distinctItemCount());
		assertEquals(null, adapter.carriedHostItem(item.bukovItemUid()));
	}

	@Test
	public void copiedHostItemsDoNotDuplicateBukovUid() {
		TestItem item = new TestItem().quantityOf(3);
		item.assignBukovItemUid("raid:heap:1");

		Item split = item.split(1);
		Item duplicate = item.duplicate();

		assertNotNull(split);
		assertNotNull(duplicate);
		assertEquals(null, split.bukovItemUid());
		assertEquals(null, duplicate.bukovItemUid());
		assertEquals("raid:heap:1", item.bukovItemUid());
	}

	@Test
	public void pickedAmmunitionCanReloadAndWritesConsumptionBack()
			throws IOException {
		BukovRaidCoordinator raid = raid(
				new InMemoryBukovSaveService(),
				10f);
		BukovHeapLootAdapter adapter =
				new BukovHeapLootAdapter(raid, TEST_ECONOMY);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		AmmoStack ammunition = new AmmoStack().configure(
				"ammo_9_standard",
				10,
				0.012f,
				12);

		assertEquals(
				LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(heap(14, ammunition), hero));
		assertTrue(hero.belongings.backpack.items.contains(ammunition));
		assertEquals(4, ammunition.takeUpTo(4));

		adapter.syncRuntimeState(hero);

		String uid = ammunition.bukovItemUid();
		assertEquals(6, raid.loot().item(uid).quantity());
		Heap dropped = new Heap();
		dropped.pos = 16;
		assertEquals(
				BukovHeapLootAdapter.DropResult.DROPPED,
				adapter.drop(uid, dropped, hero));
		assertFalse(hero.belongings.backpack.items.contains(ammunition));
		assertSame(ammunition, dropped.peek());
	}

	@Test
	public void authoredFirearmPickupBecomesEquipableAndSurvivesDropRepick()
			throws IOException {
		BukovRaidCoordinator raid = raid(
				new InMemoryBukovSaveService(),
				10f);
		BukovHeapLootAdapter adapter = new BukovHeapLootAdapter(raid);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Item authored =
				BukovFirstRaidLootTables.createByEconomicDefinitionId(
						"firearm:needle_9");
		assertNotNull(authored);
		Heap source = heap(19, authored);

		assertEquals(
				LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(source, hero));
		String uid = authored.bukovItemUid();
		Item runtime = adapter.carriedHostItem(uid);
		assertTrue(runtime instanceof Firearm);
		assertSame(runtime, hero.belongings.weapon);
		assertEquals("firearm:needle_9",
				raid.loot().item(uid).definitionId());

		Heap dropped = new Heap();
		dropped.pos = 20;
		assertEquals(
				BukovHeapLootAdapter.DropResult.DROPPED,
				adapter.drop(uid, dropped, hero));
		assertSame(runtime, dropped.peek());
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				adapter.pickupTop(dropped, hero));
		assertSame(runtime, adapter.carriedHostItem(uid));
		assertEquals("firearm:needle_9",
				raid.loot().item(uid).definitionId());
	}

	@Test
	public void dropGuardKeepsInUseMedicalInLedgerAndBackpack()
			throws IOException {
		BukovRaidCoordinator raid = raid(
				new InMemoryBukovSaveService(),
				10f);
		BukovHeapLootAdapter adapter =
				new BukovHeapLootAdapter(raid, TEST_ECONOMY);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		Item medical =
				BukovFirstRaidLootTables.createByEconomicDefinitionId(
						"bandage");
		adapter.pickupTop(heap(24, medical), hero);
		String uid = medical.bukovItemUid();
		adapter.dropGuard(uid::equals);
		Heap target = new Heap();
		target.pos = 25;

		assertEquals(
				BukovHeapLootAdapter.DropResult.IN_USE_ITEM,
				adapter.drop(uid, target, hero));
		assertTrue(raid.loot().contains(uid));
		assertSame(adapter.carriedHostItem(uid), medical);
		assertTrue(hero.belongings.backpack.items.contains(medical));
		assertTrue(target.items.isEmpty());
	}

	private static BukovRaidCoordinator raid(
			BukovSaveService saves,
			float maxWeight) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				3L,
				"loot-adapter",
				maxWeight,
				Collections.singletonList(ExtractionState.basic()));
	}

	private static Heap heap(int pos, Item item) {
		Heap heap = new Heap();
		heap.pos = pos;
		heap.items.add(item);
		return heap;
	}

	public static class TestItem extends Item {
		public TestItem quantityOf(int quantity) {
			quantity(quantity);
			return this;
		}

		@Override
		public int value() {
			return 25;
		}
	}
}
