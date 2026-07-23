package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The four single-player contracts share one realtime runtime and vary only
 * the pressure, economic exposure, and boss policy.
 */
public enum BukovRaidMode {

	EXPEDITION(
			"远征行动", "12-20分钟 · 完整风险 · 中大型随机区",
			12f, 20f, 12f, 8, 3, true, 360f, 1f,
			31, 26, 34, 0.55f, 0.30f, 8),
	QUICK_SWEEP(
			"快扫行动", "6-10分钟 · 保护最高价值带入物 · 紧凑补给",
			6f, 10f, 16f, 5, 2, false, Float.MAX_VALUE, 0.72f,
			22, 18, 24, 0.38f, 0.42f, 4),
	SCAVENGER(
			"布衣行动", "8-14分钟 · 不带自有装备 · 低收益恢复",
			8f, 14f, 15f, 6, 2, false, Float.MAX_VALUE, 0.58f,
			26, 22, 29, 0.48f, 0.35f, 6),
	BOSS_CONTRACT(
			"Boss合同", "8-15分钟 · 完整风险 · 白线高权重",
			8f, 15f, 9f, 8, 3, true, 30f, 1.25f,
			34, 28, 37, 0.72f, 0.22f, 10);

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
		if (this == QUICK_SWEEP) return new float[]{4f, 1f, 0f};
		if (this == SCAVENGER) return new float[]{3f, 2f, 0f};
		if (this == BOSS_CONTRACT) return new float[]{1f, 2f, 2f};
		return new float[]{2f, 2f, 1f};
	}

	public float[] branchTunnelChances() {
		if (this == QUICK_SWEEP) return new float[]{3f, 1f, 0f};
		if (this == BOSS_CONTRACT) return new float[]{1f, 2f, 1f};
		return new float[]{1f, 1f, 0f};
	}

	public boolean usesPlayerLoadout() {
		return this != SCAVENGER;
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
	 * Mission containers are never removed or rebalanced.
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
			if ("mission_archive".equals(definition.lootTableId)) {
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
		if (this == QUICK_SWEEP && regular.size() > 2) {
			regular = new ArrayList<>(regular.subList(0, 2));
		}
		List<BukovContainerDefinition> result = new ArrayList<>();
		for (BukovContainerDefinition definition : regular) {
			int rolls = definition.rolls;
			float search = definition.searchSeconds;
			if (this == QUICK_SWEEP) {
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
					definition.locked));
		}
		result.addAll(mission);
		return Collections.unmodifiableList(result);
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
