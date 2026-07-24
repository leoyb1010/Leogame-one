package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authored first-raid loot pools. Normal loot is data-driven so adding content
 * does not require one host Item subclass per definition.
 */
public final class BukovFirstRaidLootTables {

	public static final String LOW = "low";
	public static final String MEDICAL = "medical";
	public static final String INDUSTRIAL = "industrial";
	public static final String HIGH_VALUE = "high_value";
	public static final String MAINTENANCE_CACHE = "maintenance_cache";
	public static final String BOSS = "boss";
	public static final String MISSION_ARCHIVE =
			FirstRaidMission.ARCHIVE_LOOT_TABLE_ID;
	public static final String MAINTENANCE_KEY_DEFINITION_ID =
			"key:maintenance";
	public static final String MAINTENANCE_CACHE_CONTAINER_ID =
			"side:maintenance_cache";
	public static final String MAINTENANCE_CACHE_DOOR_ID =
			"lock:maintenance_cache";
	private static final int MAINTENANCE_KEY_DROP_PERCENT = 8;

	private static final Map<String, BukovLootTable> TABLES = createTables();

	private BukovFirstRaidLootTables() {
	}

	public static BukovLootTable require(String tableId) {
		BukovLootTable table = TABLES.get(tableId);
		if (table == null) {
			throw new IllegalArgumentException("Unknown first-raid loot table: " + tableId);
		}
		return table;
	}

	public static Map<String, BukovLootTable> all() {
		return TABLES;
	}

	public static Item createByEconomicDefinitionId(String definitionId) {
		if (definitionId == null || definitionId.trim().isEmpty()) {
			throw new IllegalArgumentException("definitionId is required");
		}
		for (BukovLootTable table : TABLES.values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item item = entry.createForValidation();
				if (item instanceof BukovEconomicItem
						&& definitionId.equals(
								((BukovEconomicItem) item).bukovDefinitionId())) {
					return item;
				}
			}
		}
		return null;
	}

	/**
	 * Stable per-enemy rare key roll. It never consumes the global RNG, so
	 * checkpoint resume cannot reroll a key into or out of existence.
	 */
	public static boolean maintenanceKeyDrops(
			long raidSeed,
			int stableSourceId) {
		long mixed = raidSeed
				^ stableSourceId * 0x9E3779B97F4A7C15L
				^ MAINTENANCE_KEY_DEFINITION_ID.hashCode();
		mixed ^= mixed >>> 33;
		mixed *= 0xff51afd7ed558ccdl;
		mixed ^= mixed >>> 33;
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(mixed, 100L)
				< MAINTENANCE_KEY_DROP_PERCENT;
	}

	private static Map<String, BukovLootTable> createTables() {
		Map<String, BukovLootTable> tables = new LinkedHashMap<>();

		register(tables, table(LOW,
				loot("canned_food", 20, 1, 2,
						BukovLootItem.Category.LOOT, 0.45f, 120),
				loot("water_filter", 14, 1, 1,
						BukovLootItem.Category.LOOT, 0.35f, 180),
				loot("duct_tape", 18, 1, 2,
						BukovLootItem.Category.TOOL, 0.25f, 140),
				loot("bolts", 18, 1, 3,
						BukovLootItem.Category.LOOT, 0.30f, 90),
				loot("scrap_metal", 16, 1, 2,
						BukovLootItem.Category.LOOT, 0.80f, 160),
				loot("cloth_roll", 16, 1, 2,
						BukovLootItem.Category.LOOT, 0.40f, 130),
				loot("battery", 12, 1, 2,
						BukovLootItem.Category.LOOT, 0.20f, 210),
				loot("ceramic_shard", 10, 1, 2,
						BukovLootItem.Category.LOOT, 0.30f, 170),
				loot("rubber_hose", 12, 1, 2,
						BukovLootItem.Category.LOOT, 0.55f, 190),
				loot("sealed_coffee", 8, 1, 1,
						BukovLootItem.Category.LOOT, 0.25f, 260),
				loot("lighter", 14, 1, 1,
						BukovLootItem.Category.TOOL, 0.08f, 100),
				loot(MAINTENANCE_KEY_DEFINITION_ID, 1, 1, 1,
						BukovLootItem.Category.TOOL, 0.03f, 460),
				firearm("needle_9", 2, 0.90f, 850),
				firearm("sentinel_9", 2, 1.05f, 1450),
				firearm("sparrow_9", 2, 0.78f, 1750),
				ammo("ammo_9_training", 14, 10, 24, 0.012f, 5),
				ammo("ammo_9_standard", 12, 8, 20, 0.012f, 12),
				ammo("ammo_9_subsonic", 6, 6, 14, 0.013f, 22)));

		register(tables, table(MEDICAL,
				loot("bandage", 20, 1, 3,
						BukovLootItem.Category.MEDICAL, 0.12f, 180),
				loot("painkiller", 15, 1, 2,
						BukovLootItem.Category.MEDICAL, 0.08f, 240),
				loot("first_aid", 8, 1, 1,
						BukovLootItem.Category.MEDICAL, 0.75f, 680),
				loot("tourniquet", 12, 1, 2,
						BukovLootItem.Category.MEDICAL, 0.15f, 320),
				loot("antiseptic", 13, 1, 2,
						BukovLootItem.Category.MEDICAL, 0.35f, 260),
				loot("splint", 11, 1, 2,
						BukovLootItem.Category.MEDICAL, 0.45f, 300),
				loot("stim", 4, 1, 1,
						BukovLootItem.Category.MEDICAL, 0.10f, 850)));

		register(tables, table(INDUSTRIAL,
				loot("copper_wire", 18, 1, 3,
						BukovLootItem.Category.LOOT, 0.35f, 220),
				loot("electric_motor", 8, 1, 1,
						BukovLootItem.Category.LOOT, 1.20f, 520),
				loot("bearing", 13, 1, 2,
						BukovLootItem.Category.LOOT, 0.55f, 340),
				loot("circuit_board", 8, 1, 2,
						BukovLootItem.Category.HIGH_VALUE, 0.18f, 620),
				loot("fuel_can", 7, 1, 1,
						BukovLootItem.Category.LOOT, 1.80f, 480),
				loot("tool_set", 6, 1, 1,
						BukovLootItem.Category.TOOL, 1.40f, 760),
				loot("welding_rod", 14, 1, 3,
						BukovLootItem.Category.TOOL, 0.60f, 280),
				loot("pressure_gauge", 9, 1, 1,
						BukovLootItem.Category.LOOT, 0.65f, 410),
				loot("relay_module", 7, 1, 2,
						BukovLootItem.Category.LOOT, 0.25f, 560),
				loot("copper_coil", 10, 1, 2,
						BukovLootItem.Category.LOOT, 0.80f, 390),
				loot("machine_oil", 9, 1, 1,
						BukovLootItem.Category.LOOT, 0.90f, 360),
				firearm("shuttle_9", 3, 2.20f, 2100),
				firearm("hive_9", 2, 2.55f, 3200),
				firearm("whisper_9", 2, 2.35f, 3900),
				firearm("bolt_12", 2, 3.10f, 4800),
				firearm("breaker_12", 1, 3.55f, 5900),
				firearm("river_556", 1, 2.85f, 4700),
				ammo("ammo_556_standard", 8, 5, 16, 0.013f, 18),
				ammo("ammo_556_armor_piercing", 4, 4, 10, 0.013f, 42),
				ammo("ammo_12g_buckshot", 5, 3, 8, 0.045f, 28)));

		register(tables, table(HIGH_VALUE,
				loot("gold_watch", 9, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.12f, 1800),
				loot("encrypted_drive", 7, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.20f, 2400),
				loot("antique_coin", 10, 1, 2,
						BukovLootItem.Category.HIGH_VALUE, 0.05f, 1600),
				loot("camera_lens", 11, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.35f, 1350),
				loot("military_chip", 4, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.08f, 3200),
				loot("radio_crystal", 6, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.06f, 2100),
				loot("optical_sensor", 5, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.18f, 2700),
				firearm("ward_556", 3, 3.00f, 4200),
				firearm("mountain_762", 2, 3.60f, 6100),
				firearm("longstreet_762", 1, 4.00f, 7600),
				firearm("jackal_9", 2, 2.70f, 4400),
				firearm("foundry_762", 1, 4.15f, 7200),
				firearm("carbine_556", 2, 2.65f, 5600),
				firearm("watchtower_556", 1, 3.70f, 8900),
				firearm("frontier_762", 1, 4.60f, 11200),
				ammo("ammo_762_standard", 7, 4, 12, 0.024f, 24),
				ammo("ammo_762_expanding", 4, 3, 9, 0.025f, 48)));

		register(tables, table(MAINTENANCE_CACHE,
				loot("maintenance_optic", 10, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.22f, 2100),
				loot("maintenance_servo", 9, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.65f, 2450),
				loot("maintenance_controller", 7, 1, 1,
						BukovLootItem.Category.HIGH_VALUE, 0.28f, 2900),
				loot("maintenance_bearing_case", 8, 1, 2,
						BukovLootItem.Category.HIGH_VALUE, 0.50f, 1750)));

		register(tables, table(BOSS,
				loot("officer_badge", 12, 1, 1,
						BukovLootItem.Category.BOSS, 0.10f, 2800),
				loot("command_key", 10, 1, 1,
						BukovLootItem.Category.BOSS, 0.06f, 4200),
				loot("prototype_core", 5, 1, 1,
						BukovLootItem.Category.BOSS, 1.50f, 6800),
				loot("classified_docs", 8, 1, 1,
						BukovLootItem.Category.BOSS, 0.25f, 5200),
				loot("titanium_case", 4, 1, 1,
						BukovLootItem.Category.BOSS, 2.20f, 7600),
				firearm("rainstorm_12", 1, 4.80f, 9800)));

		// One entry, one roll: the objective archive cannot be omitted by a
		// weighted random result and never shares a normal economy table.
		register(tables, table(MISSION_ARCHIVE,
				new BukovLootTable.Entry(
						FirstRaidMission.ARCHIVE_DEFINITION_ID,
						1,
						1,
						1,
						BukovMissionArchive::new)));

		validate(tables);
		return Collections.unmodifiableMap(tables);
	}

	private static BukovLootTable table(
			String tableId,
			BukovLootTable.Entry... entries) {
		return new BukovLootTable(tableId, Arrays.asList(entries));
	}

	private static BukovLootTable.Entry loot(
			String definitionId,
			int weight,
			int minimumQuantity,
			int maximumQuantity,
			BukovLootItem.Category category,
			float unitWeight,
			int unitValue) {
		return new BukovLootTable.Entry(
				definitionId,
				weight,
				minimumQuantity,
				maximumQuantity,
				() -> new BukovLootItem().configure(
						definitionId,
						definitionId,
						category,
						unitWeight,
						unitValue));
	}

	private static BukovLootTable.Entry ammo(
			String ammoId,
			int weight,
			int minimumQuantity,
			int maximumQuantity,
			float unitWeight,
			int unitValue) {
		return new BukovLootTable.Entry(
				ammoId,
				weight,
				minimumQuantity,
				maximumQuantity,
				() -> new AmmoStack().configure(
						ammoId,
						1,
						unitWeight,
						unitValue));
	}

	private static BukovLootTable.Entry firearm(
			String firearmId,
			int weight,
			float unitWeight,
			int unitValue) {
		String economicId = "firearm:" + firearmId;
		return new BukovLootTable.Entry(
				economicId,
				weight,
				1,
				1,
				() -> new BukovLootItem().configure(
						economicId,
						economicId,
						BukovLootItem.Category.HIGH_VALUE,
						unitWeight,
						unitValue));
	}

	private static void register(
			Map<String, BukovLootTable> tables,
			BukovLootTable table) {
		if (tables.put(table.tableId(), table) != null) {
			throw new IllegalStateException("Duplicate first-raid table: " + table.tableId());
		}
	}

	private static void validate(Map<String, BukovLootTable> tables) {
		Set<String> entryIds = new LinkedHashSet<>();
		Set<String> definitionIds = new LinkedHashSet<>();
		for (BukovLootTable table : tables.values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				if (!entryIds.add(entry.entryId())) {
					throw new IllegalStateException("Duplicate first-raid entry: " + entry.entryId());
				}
				Item first = entry.createForValidation();
				Item second = entry.createForValidation();
				if (first == null || second == null || first == second) {
					throw new IllegalStateException(
							"Loot factory must create fresh instances: " + entry.entryId());
				}
				if (!(first instanceof BukovEconomicItem)) {
					throw new IllegalStateException(
							"Loot is missing economic metadata: " + entry.entryId());
				}
				BukovEconomicItem economic = (BukovEconomicItem) first;
				if (economic.bukovDefinitionId() == null
						|| economic.bukovDefinitionId().trim().isEmpty()
						|| !definitionIds.add(economic.bukovDefinitionId())) {
					throw new IllegalStateException(
							"Invalid or duplicate loot definition: " + entry.entryId());
				}
				if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(economic.bukovUnitWeight())
						|| economic.bukovUnitWeight() < 0f
						|| economic.bukovUnitValue() < 0) {
					throw new IllegalStateException(
							"Invalid loot economy: " + entry.entryId());
				}
			}
		}
		if (entryIds.size() < 30) {
			throw new IllegalStateException("First raid requires at least 30 loot entries");
		}
	}
}
