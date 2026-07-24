package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Data-only definition for an abstract Bukov raid theme.
 *
 * Themes deliberately do not own a second map generator. They tune content
 * composition and reuse the proven room, route and extraction contracts.
 */
public final class ThemeDefinition {

	private static final String[] REQUIRED_LOOT_TABLES = {
			"low", "medical", "industrial", "high_value", "boss"
	};
	private static final String[] REQUIRED_ENEMIES = {
			"scavenger_gunner",
			"melee_rusher",
			"iron_clasp_guard",
			"sensor_doll",
			"iron_clasp_captain",
			"alley_scout",
			"depot_shotgunner",
			"line_rifleman",
			"fog_stalker",
			"signal_operator",
			"iron_clasp_marksman",
			"breach_veteran",
			"boss_white_line"
	};

	public final String id;
	public final String name;
	public final int primaryColor;
	public final int secondaryColor;
	public final float riskMultiplier;
	/** Procedural floor grammar consumed by the semantic visual layer. */
	public final String floorPattern;
	/** Lower values create denser decorated equipment walls. */
	public final int wallDecoModulo;
	/** Number of safe two-cell cover clusters attempted per authored room. */
	public final int coverClusters;
	/** Fixed-step runtime tradeoff; never owns topology or direct damage. */
	public final ThemeEnvironmentRules environmentRules;
	private final Map<BukovRaidLayout.Zone, Float> roomWeights;
	private final Map<String, Float> lootWeights;
	private final Map<String, Float> enemyWeights;
	private final List<String> coverCombination;

	ThemeDefinition(
			String id,
			String name,
			int primaryColor,
			int secondaryColor,
			float riskMultiplier,
			String floorPattern,
			int wallDecoModulo,
			int coverClusters,
			ThemeEnvironmentRules environmentRules,
			Map<BukovRaidLayout.Zone, Float> roomWeights,
			Map<String, Float> lootWeights,
			Map<String, Float> enemyWeights,
			List<String> coverCombination) {
		this.id = id;
		this.name = name;
		this.primaryColor = primaryColor;
		this.secondaryColor = secondaryColor;
		this.riskMultiplier = riskMultiplier;
		this.floorPattern = floorPattern;
		this.wallDecoModulo = wallDecoModulo;
		this.coverClusters = coverClusters;
		this.environmentRules = environmentRules;
		this.roomWeights = immutableCopy(roomWeights);
		this.lootWeights = immutableCopy(lootWeights);
		this.enemyWeights = immutableCopy(enemyWeights);
		this.coverCombination = Collections.unmodifiableList(
				new ArrayList<>(coverCombination));
		validate();
	}

	public float roomWeight(BukovRaidLayout.Zone zone) {
		Float value = roomWeights.get(zone);
		return value == null ? 1f : value;
	}

	public float lootWeight(String tableId) {
		Float value = lootWeights.get(tableId);
		return value == null ? 1f : value;
	}

	public float enemyWeight(String enemyId) {
		Float value = enemyWeights.get(enemyId);
		return value == null ? 1f : value;
	}

	public Map<BukovRaidLayout.Zone, Float> roomWeights() {
		return roomWeights;
	}

	public Map<String, Float> lootWeights() {
		return lootWeights;
	}

	public Map<String, Float> enemyWeights() {
		return enemyWeights;
	}

	public List<String> coverCombination() {
		return coverCombination;
	}

	/** Tunes the same mode's reinforcement cadence without a second loop. */
	public float pressureAdjustedSeconds(float baselineSeconds) {
		if (!finite(baselineSeconds) || baselineSeconds <= 0f) {
			throw new IllegalArgumentException(
					"baselineSeconds must be finite and positive");
		}
		return baselineSeconds / riskMultiplier
				* environmentRules.reinforcementIntervalMultiplier;
	}

	/** Converts an authored base spawn weight into this theme's live weight. */
	public int adjustedEnemyWeight(String enemyId, int baseWeight) {
		if (baseWeight <= 0) return 0;
		return Math.max(1, Math.round(baseWeight * enemyWeight(enemyId)));
	}

	/**
	 * Deterministically chooses a live container table from the five authored
	 * pools. The container id prevents three anchors from sharing one roll.
	 */
	public String selectLootTable(long seed, String stableContainerId) {
		if (!text(stableContainerId)) {
			throw new IllegalArgumentException("stableContainerId is required");
		}
		int total = 0;
		for (String tableId : REQUIRED_LOOT_TABLES) {
			total += scaledWeight(lootWeight(tableId));
		}
		long key = mix(seed ^ stableContainerId.hashCode() ^ id.hashCode());
		int roll = (int)com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(key, total);
		for (String tableId : REQUIRED_LOOT_TABLES) {
			int weight = scaledWeight(lootWeight(tableId));
			if (roll < weight) return tableId;
			roll -= weight;
		}
		throw new IllegalStateException("theme loot selection drift: " + id);
	}

	public void applyLootWeights(BukovRaidLayout layout) {
		if (layout == null) throw new IllegalArgumentException("layout is required");
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			anchor.lootTableId = selectLootTable(
					layout.seed, anchor.id + ":" + anchor.roomId);
		}
	}

	/**
	 * Reorders the existing zone multiset inside each exclusive risk corridor.
	 * Counts, route topology, extraction positions and route-average threat stay
	 * unchanged; only the room-by-room semantic composition changes.
	 */
	public void applyRoomWeights(BukovRaidLayout layout) {
		if (layout == null) throw new IllegalArgumentException("layout is required");
		Map<String, Integer> routeMembership = new HashMap<>();
		for (BukovRaidLayout.Route route : layout.routes) {
			for (String roomId : route.roomIds) {
				Integer count = routeMembership.get(roomId);
				routeMembership.put(roomId, count == null ? 1 : count + 1);
			}
		}
		for (BukovRaidLayout.Route route : layout.routes) {
			if (route.risk == BukovRaidLayout.RouteRisk.HIGH_RISK) continue;
			List<BukovRaidLayout.Mark> marks = new ArrayList<>();
			List<BukovRaidLayout.Zone> remaining = new ArrayList<>();
			for (String roomId : route.roomIds) {
				BukovRaidLayout.Mark mark = layout.mark(roomId);
				if (mark == null
						|| routeMembership.get(roomId) == null
						|| routeMembership.get(roomId) != 1
						|| !roomWeights.containsKey(mark.zone)) {
					continue;
				}
				marks.add(mark);
				remaining.add(mark.zone);
			}
			Random random = new Random(mix(
					layout.seed
							^ ((long)id.hashCode() << 32)
							^ route.risk.ordinal()));
			for (BukovRaidLayout.Mark mark : marks) {
				float total = 0f;
				for (BukovRaidLayout.Zone zone : remaining) {
					total += roomWeight(zone);
				}
				float roll = random.nextFloat() * total;
				int selected = remaining.size() - 1;
				for (int index = 0; index < remaining.size(); index++) {
					roll -= roomWeight(remaining.get(index));
					if (roll < 0f) {
						selected = index;
						break;
					}
				}
				mark.zone = remaining.remove(selected);
			}
		}
	}

	private void validate() {
		require(text(id), "theme id is required");
		require(text(name), "theme name is required: " + id);
		require(finite(riskMultiplier)
				&& riskMultiplier >= 0.65f
				&& riskMultiplier <= 1.50f,
				"theme riskMultiplier out of range: " + id);
		require("FOG_PATCHES".equals(floorPattern)
						|| "RUST_STRIPES".equals(floorPattern)
						|| "FLOOD_CHANNELS".equals(floorPattern)
						|| "YARD_BLOCKS".equals(floorPattern)
						|| "COLD_GRID".equals(floorPattern)
						|| "LAB_CIRCUIT".equals(floorPattern),
				"unknown theme floorPattern: " + id);
		require(wallDecoModulo >= 2 && wallDecoModulo <= 17,
				"theme wallDecoModulo out of range: " + id);
		require(coverClusters >= 1 && coverClusters <= 3,
				"theme coverClusters out of range: " + id);
		require(environmentRules != null,
				"theme environmentRules are required: " + id);

		require(roomWeights.size() == 5
				&& roomWeights.containsKey(BukovRaidLayout.Zone.LOW_LOOT)
				&& roomWeights.containsKey(BukovRaidLayout.Zone.COMBAT)
				&& roomWeights.containsKey(BukovRaidLayout.Zone.MEDICAL)
				&& roomWeights.containsKey(BukovRaidLayout.Zone.HAZARD)
				&& roomWeights.containsKey(BukovRaidLayout.Zone.HIGH_VALUE),
				"theme requires five room weights: " + id);
		for (Map.Entry<BukovRaidLayout.Zone, Float> entry : roomWeights.entrySet()) {
			require(positive(entry.getValue()),
					"invalid room weight " + entry.getKey() + ": " + id);
		}

		for (String tableId : REQUIRED_LOOT_TABLES) {
			require(positive(lootWeights.get(tableId)),
					"missing loot weight " + tableId + ": " + id);
		}
		require(lootWeights.size() == REQUIRED_LOOT_TABLES.length,
				"theme has unknown loot weights: " + id);

		for (String enemyId : REQUIRED_ENEMIES) {
			require(positive(enemyWeights.get(enemyId)),
					"missing enemy weight " + enemyId + ": " + id);
		}
		require(enemyWeights.size() == REQUIRED_ENEMIES.length,
				"theme has unknown enemy weights: " + id);

		require(coverCombination.size() == 3,
				"theme needs three cover choices: " + id);
		for (String kind : coverCombination) {
			require("CONCRETE_COVER".equals(kind)
					|| "SANDBAG_COVER".equals(kind),
					"theme cover must reuse original industrial art: " + id);
		}
	}

	private static boolean positive(Float value) {
		return value != null && finite(value) && value > 0f;
	}

	private static int scaledWeight(float value) {
		return Math.max(1, Math.round(value * 1000f));
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static boolean finite(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value);
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalArgumentException(message);
	}

	private static <K> Map<K, Float> immutableCopy(Map<K, Float> values) {
		if (values == null) throw new IllegalArgumentException("weights are required");
		return Collections.unmodifiableMap(new LinkedHashMap<>(values));
	}
}
