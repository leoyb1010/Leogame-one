package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The four economic contracts share one realtime runtime and vary only the
 * pressure, economic exposure, and boss policy. Training ground is deliberately
 * outside that economy: it is a short, repeatable controls-and-ballistics
 * slice with disposable supplies.
 */
public enum BukovRaidMode {

	EXPEDITION(
			BukovMessages.get("bukov.economy.mode.name_expedition"),
			BukovMessages.get("bukov.economy.mode.summary_expedition"),
			12f, 20f, 12f, 8, 3, true, 360f, 1f,
			31, 26, 34, 0.55f, 0.30f, 8),
	QUICK_SWEEP(
			BukovMessages.get("bukov.economy.mode.name_quick_sweep"),
			BukovMessages.get("bukov.economy.mode.summary_quick_sweep"),
			6f, 10f, 16f, 5, 2, false, Float.MAX_VALUE, 0.72f,
			22, 18, 24, 0.38f, 0.42f, 4),
	SCAVENGER(
			BukovMessages.get("bukov.economy.mode.name_scavenger"),
			BukovMessages.get("bukov.economy.mode.summary_scavenger"),
			8f, 14f, 15f, 6, 2, false, Float.MAX_VALUE, 0.58f,
			26, 22, 29, 0.48f, 0.35f, 6),
	BOSS_CONTRACT(
			BukovMessages.get("bukov.economy.mode.name_boss_contract"),
			BukovMessages.get("bukov.economy.mode.summary_boss_contract"),
			8f, 15f, 9f, 8, 3, true, 30f, 1.25f,
			34, 28, 37, 0.72f, 0.22f, 10),
	TRAINING_GROUND(
			BukovMessages.get("bukov.economy.mode.name_training_ground"),
			BukovMessages.get("bukov.economy.mode.summary_training_ground"),
			3f, 5f, 20f, 4, 2, false, Float.MAX_VALUE, 1f,
			18, 16, 20, 0.22f, 0.55f, 3);

	public final String displayName;
	public final String summary;
	public final float targetMinutesMinimum;
	public final float targetMinutesMaximum;
	public final float spawnIntervalSeconds;
	public final int maximumActiveEnemies;
	public final int initialEnemyCount;
	public final boolean bossEnabled;
	public final float bossEarliestSeconds;
	public final float lootValueMultiplier;
	public final int standardRoomBudget;
	public final int minimumContentRooms;
	public final int maximumContentRooms;
	public final float loopShapeIntensity;
	public final float extraConnectionChance;
	public final int routeDetourAllowance;

	BukovRaidMode(
			String displayName,
			String summary,
			float targetMinutesMinimum,
			float targetMinutesMaximum,
			float spawnIntervalSeconds,
			int maximumActiveEnemies,
			int initialEnemyCount,
			boolean bossEnabled,
			float bossEarliestSeconds,
			float lootValueMultiplier,
			int standardRoomBudget,
			int minimumContentRooms,
			int maximumContentRooms,
			float loopShapeIntensity,
			float extraConnectionChance,
			int routeDetourAllowance) {
		this.displayName = displayName;
		this.summary = summary;
		this.targetMinutesMinimum = targetMinutesMinimum;
		this.targetMinutesMaximum = targetMinutesMaximum;
		this.spawnIntervalSeconds = spawnIntervalSeconds;
		this.maximumActiveEnemies = maximumActiveEnemies;
		this.initialEnemyCount = initialEnemyCount;
		this.bossEnabled = bossEnabled;
		this.bossEarliestSeconds = bossEarliestSeconds;
		this.lootValueMultiplier = lootValueMultiplier;
		this.standardRoomBudget = standardRoomBudget;
		this.minimumContentRooms = minimumContentRooms;
		this.maximumContentRooms = maximumContentRooms;
		this.loopShapeIntensity = loopShapeIntensity;
		this.extraConnectionChance = extraConnectionChance;
		this.routeDetourAllowance = routeDetourAllowance;
	}

	public boolean acceptsContentRoomCount(int count) {
		return count >= minimumContentRooms && count <= maximumContentRooms;
	}

	public float[] pathTunnelChances() {
		if (this == TRAINING_GROUND) return new float[]{5f, 1f, 0f};
		if (this == QUICK_SWEEP) return new float[]{4f, 1f, 0f};
		if (this == SCAVENGER) return new float[]{3f, 2f, 0f};
		if (this == BOSS_CONTRACT) return new float[]{1f, 2f, 2f};
		return new float[]{2f, 2f, 1f};
	}

	public float[] branchTunnelChances() {
		if (this == TRAINING_GROUND) return new float[]{4f, 1f, 0f};
		if (this == QUICK_SWEEP) return new float[]{3f, 1f, 0f};
		if (this == BOSS_CONTRACT) return new float[]{1f, 2f, 1f};
		return new float[]{1f, 1f, 0f};
	}

	public boolean usesPlayerLoadout() {
		return this != SCAVENGER && this != TRAINING_GROUND;
	}

	/** Training attempts never enter the four-mode economy ledger. */
	public boolean countsTowardEconomyStatistics() {
		return this != TRAINING_GROUND;
	}

	public boolean trainingGround() {
		return this == TRAINING_GROUND;
	}

	public boolean protectsHighestValueDeploymentOnDeath() {
		return this == QUICK_SWEEP;
	}

	public float targetMinimumSeconds() {
		return targetMinutesMinimum * 60f;
	}

	public float targetMaximumSeconds() {
		return targetMinutesMaximum * 60f;
	}

	public boolean convergenceStarted(float elapsedSeconds) {
		return elapsedSeconds >= targetMinimumSeconds();
	}

	public boolean overtime(float elapsedSeconds) {
		return elapsedSeconds >= targetMaximumSeconds();
	}

	public float pressureMultiplier(float elapsedSeconds) {
		if (elapsedSeconds <= targetMinimumSeconds()) return 1f;
		if (elapsedSeconds >= targetMaximumSeconds()) return 1.75f;
		float window = targetMaximumSeconds() - targetMinimumSeconds();
		float progress = (elapsedSeconds - targetMinimumSeconds()) / window;
		return 1f + progress * 0.5f;
	}

	public float spawnIntervalAt(float elapsedSeconds) {
		return spawnIntervalSeconds / pressureMultiplier(elapsedSeconds);
	}

	public int maximumActiveEnemiesAt(float elapsedSeconds) {
		return maximumActiveEnemies
				+ (overtime(elapsedSeconds) ? 2
						: convergenceStarted(elapsedSeconds) ? 1 : 0);
	}

	/**
	 * Applies mode economy once, at successful extraction. Physical UID,
	 * quantity, weight and durability are conserved; only found-in-raid unit
	 * value changes before item and receipt enter the stash atomically.
	 */
	public RaidItem settleExtractedItem(RaidItem item) {
		if (item == null) throw new IllegalArgumentException("item is required");
		if (!item.foundInRaid()) return item.copy();
		long adjusted = Math.round(item.unitValue() * (double)lootValueMultiplier);
		if (adjusted > Integer.MAX_VALUE) {
			throw new IllegalStateException("Adjusted loot value overflow");
		}
		return item.withUnitValue((int)Math.max(0L, adjusted));
	}

	/**
	 * Deterministic container projection shared by new raids and resume.
	 * Formal raid mission containers are never rebalanced. Training deliberately
	 * removes the Q01 archive and high-value objective cache.
	 */
	public List<BukovContainerDefinition> configureContainers(
			Collection<BukovContainerDefinition> source,
			long raidSeed) {
		if (source == null) {
			throw new IllegalArgumentException("containers are required");
		}
		List<BukovContainerDefinition> mission = new ArrayList<>();
		List<BukovContainerDefinition> regular = new ArrayList<>();
		for (BukovContainerDefinition definition : source) {
			if (definition == null) {
				throw new IllegalArgumentException(
						"container definition is required");
			}
			boolean archiveMission =
					FirstRaidMission.ARCHIVE_LOOT_TABLE_ID.equals(
							definition.lootTableId);
			boolean highValueMission =
					FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							definition.lootTableId);
			if (trainingGround() && (archiveMission || highValueMission)) {
				continue;
			}
			if (archiveMission) {
				mission.add(definition);
			} else {
				regular.add(definition);
			}
		}
		Collections.sort(regular, new Comparator<BukovContainerDefinition>() {
			@Override
			public int compare(
					BukovContainerDefinition first,
					BukovContainerDefinition second) {
				long firstRank = rank(raidSeed, first.containerId);
				long secondRank = rank(raidSeed, second.containerId);
				int byRank = Long.compare(firstRank, secondRank);
				return byRank != 0 ? byRank
						: first.containerId.compareTo(second.containerId);
			}
		});
		if ((this == QUICK_SWEEP || this == TRAINING_GROUND)
				&& regular.size() > 2) {
			List<BukovContainerDefinition> compact =
					new ArrayList<>(regular.subList(0, 2));
			// Quick Sweep still follows the authored first-raid route, so its
			// compact selection must retain the high-value objective cache.
			if (this == QUICK_SWEEP
					&& !containsLootTable(
							compact,
							FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID)) {
				BukovContainerDefinition critical =
						firstWithLootTable(
								regular,
								FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID);
				if (critical != null) {
					compact.set(compact.size() - 1, critical);
				}
			}
			regular = compact;
		}
		List<BukovContainerDefinition> result = new ArrayList<>();
		for (BukovContainerDefinition definition : regular) {
			int rolls = definition.rolls;
			float search = definition.searchSeconds;
			if (this == QUICK_SWEEP || this == TRAINING_GROUND) {
				rolls = Math.max(1, rolls - 1);
				search *= 0.75f;
			} else if (this == SCAVENGER) {
				rolls = Math.min(1, rolls);
				search *= 0.85f;
			} else if (this == BOSS_CONTRACT) {
				rolls += 1;
				search *= 1.15f;
			}
			result.add(new BukovContainerDefinition(
					definition.containerId,
					definition.cell,
					definition.lootTableId,
					rolls,
					clampSearch(search),
					definition.locked
							|| !trainingGround()
									&& FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
											definition.lootTableId)));
		}
		result.addAll(mission);
		return Collections.unmodifiableList(result);
	}

	private static boolean containsLootTable(
			List<BukovContainerDefinition> definitions,
			String lootTableId) {
		return firstWithLootTable(definitions, lootTableId) != null;
	}

	private static BukovContainerDefinition firstWithLootTable(
			List<BukovContainerDefinition> definitions,
			String lootTableId) {
		for (BukovContainerDefinition definition : definitions) {
			if (lootTableId.equals(definition.lootTableId)) {
				return definition;
			}
		}
		return null;
	}

	private static float clampSearch(float value) {
		return Math.max(
				BukovSearchableContainer.MINIMUM_SEARCH_SECONDS,
				Math.min(BukovSearchableContainer.MAXIMUM_SEARCH_SECONDS, value));
	}

	private static long rank(long seed, String id) {
		long value = seed ^ id.hashCode();
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	public BukovRaidMode next() {
		BukovRaidMode[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static BukovRaidMode safeValueOf(String value) {
		if (value == null || value.trim().isEmpty()) {
			return EXPEDITION;
		}
		try {
			return valueOf(value);
		} catch (IllegalArgumentException ignored) {
			return EXPEDITION;
		}
	}
}
