package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/** Immutable level-authored definition used when a new raid is created. */
public final class BukovContainerDefinition {

	public final String containerId;
	public final int cell;
	public final String lootTableId;
	public final int rolls;
	public final float searchSeconds;
	public final boolean locked;

	public BukovContainerDefinition(
			String containerId,
			int cell,
			String lootTableId,
			int rolls,
			float searchSeconds,
			boolean locked) {
		if (containerId == null || containerId.trim().isEmpty()) {
			throw new IllegalArgumentException("containerId is required");
		}
		if (cell < 0) {
			throw new IllegalArgumentException("cell must be non-negative");
		}
		if (lootTableId == null || lootTableId.trim().isEmpty()) {
			throw new IllegalArgumentException("lootTableId is required");
		}
		if (rolls < 0) {
			throw new IllegalArgumentException("rolls must be non-negative");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(searchSeconds)
				|| searchSeconds < BukovSearchableContainer.MINIMUM_SEARCH_SECONDS
				|| searchSeconds > BukovSearchableContainer.MAXIMUM_SEARCH_SECONDS) {
			throw new IllegalArgumentException(
					"searchSeconds must be between 0.6 and 5");
		}
		this.containerId = containerId;
		this.cell = cell;
		this.lootTableId = lootTableId;
		this.rolls = rolls;
		this.searchSeconds = searchSeconds;
		this.locked = locked;
	}

	BukovSearchableContainer create(long raidSeed) {
		return new BukovSearchableContainer(
				containerId,
				cell,
				lootTableId,
				raidSeed,
				rolls,
				searchSeconds,
				locked);
	}
}
