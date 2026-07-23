package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Realtime container search state. Interruption resets progress, while rolled
 * contents are persisted and can never be generated twice.
 */
public final class BukovSearchableContainer implements Bundlable {

	public enum State {
		UNSEARCHED,
		SEARCHING,
		SEARCHED,
		INTERRUPTED,
		LOCKED
	}

	public enum UpdateResult {
		UNCHANGED,
		PROGRESSED,
		INTERRUPTED,
		COMPLETED
	}

	public static final float MINIMUM_SEARCH_SECONDS = 0.6f;
	public static final float MAXIMUM_SEARCH_SECONDS = 5f;
	public static final int CURRENT_VERSION = 1;

	private static final String VERSION = "version";
	private static final String CONTAINER_ID = "container_id";
	private static final String CELL = "cell";
	private static final String LOOT_TABLE_ID = "loot_table_id";
	private static final String RAID_SEED = "raid_seed";
	private static final String ROLLS = "rolls";
	private static final String SEARCH_SECONDS = "search_seconds";
	private static final String STATE = "state";
	private static final String PROGRESS = "progress";
	private static final String CONTENT_GENERATED = "content_generated";
	private static final String CONTENTS_RELEASED = "contents_released";
	private static final String CONTENTS = "contents";

	private int version = CURRENT_VERSION;
	private String containerId;
	private int cell = -1;
	private String lootTableId;
	private long raidSeed;
	private int rolls;
	private float searchSeconds;
	private State state;
	private float progressSeconds;
	private boolean contentGenerated;
	private boolean contentsReleased;
	private final List<Item> contents = new ArrayList<>();

	public BukovSearchableContainer() {
		// Required by Bundle reflection.
	}

	public BukovSearchableContainer(
			String containerId,
			String lootTableId,
			long raidSeed,
			int rolls,
			float searchSeconds,
			boolean locked) {
		this(
				containerId,
				-1,
				lootTableId,
				raidSeed,
				rolls,
				searchSeconds,
				locked);
	}

	public BukovSearchableContainer(
			String containerId,
			int cell,
			String lootTableId,
			long raidSeed,
			int rolls,
			float searchSeconds,
			boolean locked) {
		this.containerId = requireId(containerId, "containerId");
		if (cell < -1) {
			throw new IllegalArgumentException("cell must be -1 or non-negative");
		}
		this.cell = cell;
		this.lootTableId = requireId(lootTableId, "lootTableId");
		if (rolls < 0) {
			throw new IllegalArgumentException("rolls must be non-negative");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(searchSeconds)
				|| searchSeconds < MINIMUM_SEARCH_SECONDS
				|| searchSeconds > MAXIMUM_SEARCH_SECONDS) {
			throw new IllegalArgumentException(
					"searchSeconds must be between 0.6 and 5");
		}
		this.raidSeed = raidSeed;
		this.rolls = rolls;
		this.searchSeconds = searchSeconds;
		state = locked ? State.LOCKED : State.UNSEARCHED;
	}

	public String containerId() {
		return containerId;
	}

	public State state() {
		return state;
	}

	public int cell() {
		return cell;
	}

	public String lootTableId() {
		return lootTableId;
	}

	public float searchSeconds() {
		return searchSeconds;
	}

	public int rolls() {
		return rolls;
	}

	public float progressSeconds() {
		return progressSeconds;
	}

	public float progressFraction() {
		return progressSeconds / searchSeconds;
	}

	public boolean contentsReleased() {
		return contentsReleased;
	}

	public List<Item> contents() {
		return Collections.unmodifiableList(new ArrayList<>(contents));
	}

	public boolean unlock() {
		if (state != State.LOCKED) {
			return false;
		}
		state = State.UNSEARCHED;
		return true;
	}

	public boolean begin() {
		if (state != State.UNSEARCHED && state != State.INTERRUPTED) {
			return false;
		}
		state = State.SEARCHING;
		progressSeconds = 0f;
		return true;
	}

	public UpdateResult update(
			float deltaSeconds,
			boolean insideRange,
			boolean moving,
			boolean damaged,
			BukovLootTable lootTable) {
		return update(
				deltaSeconds,
				insideRange,
				moving,
				damaged,
				false,
				lootTable);
	}

	public UpdateResult update(
			float deltaSeconds,
			boolean insideRange,
			boolean moving,
			boolean damaged,
			boolean reloading,
			BukovLootTable lootTable) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (state != State.SEARCHING) {
			return UpdateResult.UNCHANGED;
		}
		if (!insideRange || moving || damaged || reloading) {
			state = State.INTERRUPTED;
			progressSeconds = 0f;
			return UpdateResult.INTERRUPTED;
		}
		if (lootTable == null || !lootTableId.equals(lootTable.tableId())) {
			throw new IllegalArgumentException("matching lootTable is required");
		}

		progressSeconds = Math.min(searchSeconds, progressSeconds + deltaSeconds);
		if (progressSeconds < searchSeconds) {
			return UpdateResult.PROGRESSED;
		}
		generateContents(lootTable);
		state = State.SEARCHED;
		return UpdateResult.COMPLETED;
	}

	/**
	 * Transfers the exact generated host Item instances to a regular Heap.
	 * BukovHeapLootAdapter can then consume that heap without a second item model.
	 */
	public int releaseTo(Heap heap) {
		if (heap == null) {
			throw new IllegalArgumentException("heap is required");
		}
		if (state != State.SEARCHED || !contentGenerated || contentsReleased) {
			return 0;
		}
		int released = contents.size();
		for (Item item : contents) {
			heap.items.addLast(item);
		}
		contents.clear();
		contentsReleased = true;
		return released;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(VERSION, version);
		bundle.put(CONTAINER_ID, containerId);
		bundle.put(CELL, cell);
		bundle.put(LOOT_TABLE_ID, lootTableId);
		bundle.put(RAID_SEED, raidSeed);
		bundle.put(ROLLS, rolls);
		bundle.put(SEARCH_SECONDS, searchSeconds);
		bundle.put(STATE, state);
		bundle.put(PROGRESS, progressSeconds);
		bundle.put(CONTENT_GENERATED, contentGenerated);
		bundle.put(CONTENTS_RELEASED, contentsReleased);
		bundle.put(CONTENTS, contents);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		int restoredVersion = bundle.getInt(VERSION);
		if (restoredVersion <= 0 || restoredVersion > CURRENT_VERSION) {
			throw new IllegalStateException(
					"Unsupported Bukov container version: " + restoredVersion);
		}
		BukovSearchableContainer restored = new BukovSearchableContainer(
				bundle.getString(CONTAINER_ID),
				bundle.contains(CELL) ? bundle.getInt(CELL) : -1,
				bundle.getString(LOOT_TABLE_ID),
				bundle.getLong(RAID_SEED),
				bundle.getInt(ROLLS),
				bundle.getFloat(SEARCH_SECONDS),
				bundle.getEnum(STATE, State.class) == State.LOCKED);
		restored.version = restoredVersion;
		restored.state = bundle.getEnum(STATE, State.class);
		restored.progressSeconds = bundle.getFloat(PROGRESS);
		restored.contentGenerated = bundle.getBoolean(CONTENT_GENERATED);
		restored.contentsReleased = bundle.getBoolean(CONTENTS_RELEASED);
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				restored.progressSeconds)
				|| restored.progressSeconds < 0f
				|| restored.progressSeconds > restored.searchSeconds) {
			throw new IllegalStateException("Invalid container search progress");
		}
		Collection<Bundlable> storedContents = bundle.getCollection(CONTENTS);
		for (Bundlable stored : storedContents) {
			if (!(stored instanceof Item)) {
				throw new IllegalStateException("Unexpected container content");
			}
			restored.contents.add((Item) stored);
		}
		if (restored.contentsReleased && !restored.contents.isEmpty()) {
			throw new IllegalStateException("Released container still owns items");
		}
		if (!restored.contentGenerated && !restored.contents.isEmpty()) {
			throw new IllegalStateException("Ungenerated container owns items");
		}
		if (restored.state == State.SEARCHED && !restored.contentGenerated) {
			throw new IllegalStateException("Searched container lacks generated contents");
		}

		version = restored.version;
		containerId = restored.containerId;
		cell = restored.cell;
		lootTableId = restored.lootTableId;
		raidSeed = restored.raidSeed;
		rolls = restored.rolls;
		searchSeconds = restored.searchSeconds;
		state = restored.state;
		progressSeconds = restored.progressSeconds;
		contentGenerated = restored.contentGenerated;
		contentsReleased = restored.contentsReleased;
		contents.clear();
		contents.addAll(restored.contents);
	}

	private void generateContents(BukovLootTable lootTable) {
		if (contentGenerated) {
			return;
		}
		contents.addAll(lootTable.roll(raidSeed, containerId, rolls));
		contentGenerated = true;
	}

	private static String requireId(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}
}
