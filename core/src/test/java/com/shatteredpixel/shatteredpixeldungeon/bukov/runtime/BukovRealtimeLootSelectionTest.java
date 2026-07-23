package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.SparseArray;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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

	private static boolean[] visible() {
		boolean[] result = new boolean[LENGTH];
		Arrays.fill(result, true);
		return result;
	}

	private static Heap heap(int cell) {
		Heap heap = new Heap();
		heap.pos = cell;
		heap.items.add(new Item());
		return heap;
	}
}
