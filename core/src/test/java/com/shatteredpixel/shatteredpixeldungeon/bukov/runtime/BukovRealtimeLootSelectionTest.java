package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.watabou.utils.SparseArray;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRealtimeLootSelectionTest {

	private static final int WIDTH = 5;
	private static final int LENGTH = 25;
	private static final int HERO = 12;

	@Test
	public void currentCellWinsBeforeVisibleNeighbour() {
		boolean[] visible = visible();
		SparseArray<Heap> heaps = new SparseArray<>();
		heaps.put(HERO + 1, heap(HERO + 1));
		heaps.put(HERO, heap(HERO));

		assertEquals(
				HERO,
				BukovRealtimeWorld.selectVisibleLootHeap(
						HERO,
						WIDTH,
						LENGTH,
						visible,
						heaps,
						-1));
	}

	@Test
	public void invisibleLockedAndExtractionHeapsAreIgnored() {
		boolean[] visible = visible();
		SparseArray<Heap> heaps = new SparseArray<>();
		int invisible = HERO - WIDTH;
		int locked = HERO - 1;
		int extraction = HERO + 1;
		int valid = HERO + WIDTH + 1;
		visible[invisible] = false;
		Heap lockedHeap = heap(locked);
		lockedHeap.type = Heap.Type.LOCKED_CHEST;
		heaps.put(invisible, heap(invisible));
		heaps.put(locked, lockedHeap);
		heaps.put(extraction, heap(extraction));
		heaps.put(valid, heap(valid));

		assertEquals(
				valid,
				BukovRealtimeWorld.selectVisibleLootHeap(
						HERO,
						WIDTH,
						LENGTH,
						visible,
						heaps,
						extraction));
	}

	@Test
	public void outOfRangeInputsAndNoHeapReturnMinusOne() {
		boolean[] visible = visible();
		SparseArray<Heap> heaps = new SparseArray<>();

		assertEquals(
				-1,
				BukovRealtimeWorld.selectVisibleLootHeap(
						-1,
						WIDTH,
						LENGTH,
						visible,
						heaps,
						-1));
		assertEquals(
				-1,
				BukovRealtimeWorld.selectVisibleLootHeap(
						HERO,
						WIDTH,
						LENGTH,
						visible,
						heaps,
						-1));
	}

	@Test
	public void automaticSelectionSkipsValuablesAndFindsAmmunition() {
		boolean[] visible = visible();
		SparseArray<Heap> heaps = new SparseArray<>();
		heaps.put(HERO, heap(HERO, valuable()));
		heaps.put(HERO + 1, heap(HERO + 1, ammunition()));

		assertEquals(
				HERO + 1,
				BukovRealtimeWorld.selectVisibleAutoPickupHeap(
						HERO,
						WIDTH,
						LENGTH,
						visible,
						heaps,
						-1));
	}

	@Test
	public void autoPickupPolicyIsLimitedToAmmoAndLightObjectives() {
		assertTrue(BukovAutoPickupPolicy.shouldPickup(ammunition()));
		assertTrue(BukovAutoPickupPolicy.shouldPickup(
				new BukovMissionArchive()));
		assertTrue(BukovAutoPickupPolicy.shouldPickup(
				new BukovLootItem().configure(
						"key:maintenance",
						"maintenance key",
						BukovLootItem.Category.TOOL,
						0.10f,
						40)));
		assertFalse(BukovAutoPickupPolicy.shouldPickup(valuable()));
		assertFalse(BukovAutoPickupPolicy.shouldPickup(
				new BukovLootItem().configure(
						"key:heavy",
						"heavy key",
						BukovLootItem.Category.TOOL,
						0.50f,
						40)));
	}

	private static boolean[] visible() {
		boolean[] result = new boolean[LENGTH];
		Arrays.fill(result, true);
		return result;
	}

	private static Heap heap(int cell) {
		return heap(cell, new Item());
	}

	private static Heap heap(int cell, Item item) {
		Heap heap = new Heap();
		heap.pos = cell;
		heap.items.add(item);
		return heap;
	}

	private static AmmoStack ammunition() {
		return new AmmoStack().configure(
				"ammo_9x19",
				12,
				0.02f,
				2);
	}

	private static BukovLootItem valuable() {
		return new BukovLootItem().configure(
				"encrypted_drive",
				"encrypted drive",
				BukovLootItem.Category.HIGH_VALUE,
				0.40f,
				2400);
	}
}
