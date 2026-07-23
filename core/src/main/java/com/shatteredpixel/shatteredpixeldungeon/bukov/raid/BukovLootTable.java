package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Deterministic weighted host-item table. It uses a local fixed algorithm and
 * never consumes the game's global RNG, so a container is stable across resume.
 */
public final class BukovLootTable {

	public interface ItemFactory {
		Item create();
	}

	public static final class Entry {
		private final String entryId;
		private final int weight;
		private final int minimumQuantity;
		private final int maximumQuantity;
		private final ItemFactory factory;

		public Entry(
				String entryId,
				int weight,
				int minimumQuantity,
				int maximumQuantity,
				ItemFactory factory) {
			if (entryId == null || entryId.trim().isEmpty()) {
				throw new IllegalArgumentException("entryId is required");
			}
			if (weight <= 0) {
				throw new IllegalArgumentException("weight must be positive");
			}
			if (minimumQuantity <= 0 || maximumQuantity < minimumQuantity) {
				throw new IllegalArgumentException("invalid quantity range");
			}
			if (factory == null) {
				throw new IllegalArgumentException("factory is required");
			}
			this.entryId = entryId;
			this.weight = weight;
			this.minimumQuantity = minimumQuantity;
			this.maximumQuantity = maximumQuantity;
			this.factory = factory;
		}

		public String entryId() {
			return entryId;
		}

		public int weight() {
			return weight;
		}

		public int minimumQuantity() {
			return minimumQuantity;
		}

		public int maximumQuantity() {
			return maximumQuantity;
		}

		public Item createForValidation() {
			return factory.create();
		}
	}

	private final String tableId;
	private final List<Entry> entries;
	private final int totalWeight;

	public BukovLootTable(String tableId, List<Entry> entries) {
		if (tableId == null || tableId.trim().isEmpty()) {
			throw new IllegalArgumentException("tableId is required");
		}
		if (entries == null || entries.isEmpty()) {
			throw new IllegalArgumentException("at least one loot entry is required");
		}
		int weight = 0;
		for (Entry entry : entries) {
			if (entry == null) {
				throw new IllegalArgumentException("loot entry is required");
			}
			weight = com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.addExact(weight, entry.weight);
		}
		this.tableId = tableId;
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
		totalWeight = weight;
	}

	public String tableId() {
		return tableId;
	}

	public List<Entry> entries() {
		return entries;
	}

	public int totalWeight() {
		return totalWeight;
	}

	public List<Item> roll(long raidSeed, String containerId, int rolls) {
		if (containerId == null || containerId.trim().isEmpty()) {
			throw new IllegalArgumentException("containerId is required");
		}
		if (rolls < 0) {
			throw new IllegalArgumentException("rolls must be non-negative");
		}
		StableRandom random = new StableRandom(
				mix(raidSeed ^ hash(tableId) ^ Long.rotateLeft(hash(containerId), 17)));
		List<Item> result = new ArrayList<>(rolls);
		Set<Item> instances = Collections.newSetFromMap(new IdentityHashMap<>());
		for (int roll = 0; roll < rolls; roll++) {
			Entry entry = select(random.nextInt(totalWeight));
			Item item = entry.factory.create();
			if (item == null || !instances.add(item)) {
				throw new IllegalStateException(
						"Loot factories must return a new non-null Item instance");
			}
			int quantity = entry.minimumQuantity
					+ random.nextInt(
							entry.maximumQuantity - entry.minimumQuantity + 1);
			item.quantity(quantity);
			result.add(item);
		}
		return result;
	}

	private Entry select(int ticket) {
		int cursor = ticket;
		for (Entry entry : entries) {
			if (cursor < entry.weight) {
				return entry;
			}
			cursor -= entry.weight;
		}
		throw new IllegalStateException("loot weight selection overflow");
	}

	private static long hash(String value) {
		long hash = 0xcbf29ce484222325L;
		for (int i = 0; i < value.length(); i++) {
			hash ^= value.charAt(i);
			hash *= 0x100000001b3L;
		}
		return hash;
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xbf58476d1ce4e5b9L;
		value ^= value >>> 27;
		value *= 0x94d049bb133111ebL;
		return value ^ (value >>> 31);
	}

	private static final class StableRandom {
		private long state;

		private StableRandom(long seed) {
			state = seed == 0L ? 0x9e3779b97f4a7c15L : seed;
		}

		private int nextInt(int bound) {
			if (bound <= 0) {
				throw new IllegalArgumentException("bound must be positive");
			}
			state ^= state << 13;
			state ^= state >>> 7;
			state ^= state << 17;
			return (int)com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
					.remainderUnsigned(state, bound);
		}
	}
}
