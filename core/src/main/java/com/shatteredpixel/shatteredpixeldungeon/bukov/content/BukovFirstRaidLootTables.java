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
	public static final String BOSS = "boss";
	public static final String MISSION_ARCHIVE =
			FirstRaidMission.ARCHIVE_LOOT_TABLE_ID;

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

	private static Map<String, BukovLootTable> createTables() {
		Map<String, BukovLootTable> tables = new LinkedHashMap<>();

		register(tables, table(LOW,
				loot("canned_food", 20, 1, 2, "罐头食品",
						BukovLootItem.Category.LOOT, 0.45f, 120),
				loot("water_filter", 14, 1, 1, "净水滤芯",
						BukovLootItem.Category.LOOT, 0.35f, 180),
				loot("duct_tape", 18, 1, 2, "工业胶带",
						BukovLootItem.Category.TOOL, 0.25f, 140),
				loot("bolts", 18, 1, 3, "螺栓包",
						BukovLootItem.Category.LOOT, 0.30f, 90),
				loot("scrap_metal", 16, 1, 2, "废金属",
						BukovLootItem.Category.LOOT, 0.80f, 160),
				loot("cloth_roll", 16, 1, 2, "布料卷",
						BukovLootItem.Category.LOOT, 0.40f, 130),
				loot("battery", 12, 1, 2, "电池组",
						BukovLootItem.Category.LOOT, 0.20f, 210),
				loot("ceramic_shard", 10, 1, 2, "工业陶瓷片",
						BukovLootItem.Category.LOOT, 0.30f, 170),
				loot("rubber_hose", 12, 1, 2, "耐压胶管",
						BukovLootItem.Category.LOOT, 0.55f, 190),
				loot("sealed_coffee", 8, 1, 1, "密封咖啡",
						BukovLootItem.Category.LOOT, 0.25f, 260),
				loot("lighter", 14, 1, 1, "打火机",
						BukovLootItem.Category.TOOL, 0.08f, 100),
				firearm("needle_9", 2, "针蜂-9", 0.90f, 850),
				firearm("sentinel_9", 2, "哨兵-9", 1.05f, 1450),
				firearm("sparrow_9", 2, "雀翎-9", 0.78f, 1750),
				ammo("ammo_9_training", 14, 10, 24, 0.012f, 5),
				ammo("ammo_9_standard", 12, 8, 20, 0.012f, 12),
				ammo("ammo_9_subsonic", 6, 6, 14, 0.013f, 22)));

		register(tables, table(MEDICAL,
				loot("bandage", 20, 1, 3, "绷带",
						BukovLootItem.Category.MEDICAL, 0.12f, 180),
				loot("painkiller", 15, 1, 2, "止痛药",
						BukovLootItem.Category.MEDICAL, 0.08f, 240),
				loot("first_aid", 8, 1, 1, "急救包",
						BukovLootItem.Category.MEDICAL, 0.75f, 680),
				loot("tourniquet", 12, 1, 2, "止血带",
						BukovLootItem.Category.MEDICAL, 0.15f, 320),
				loot("antiseptic", 13, 1, 2, "消毒剂",
						BukovLootItem.Category.MEDICAL, 0.35f, 260),
				loot("splint", 11, 1, 2, "夹板",
						BukovLootItem.Category.MEDICAL, 0.45f, 300),
				loot("stim", 4, 1, 1, "战地注射器",
						BukovLootItem.Category.MEDICAL, 0.10f, 850)));

		register(tables, table(INDUSTRIAL,
				loot("copper_wire", 18, 1, 3, "铜线",
						BukovLootItem.Category.LOOT, 0.35f, 220),
				loot("electric_motor", 8, 1, 1, "电机",
						BukovLootItem.Category.LOOT, 1.20f, 520),
				loot("bearing", 13, 1, 2, "轴承",
						BukovLootItem.Category.LOOT, 0.55f, 340),
				loot("circuit_board", 8, 1, 2, "电路板",
						BukovLootItem.Category.HIGH_VALUE, 0.18f, 620),
				loot("fuel_can", 7, 1, 1, "燃料罐",
						BukovLootItem.Category.LOOT, 1.80f, 480),
				loot("tool_set", 6, 1, 1, "工具组",
						BukovLootItem.Category.TOOL, 1.40f, 760),
				loot("welding_rod", 14, 1, 3, "焊条",
						BukovLootItem.Category.TOOL, 0.60f, 280),
				loot("pressure_gauge", 9, 1, 1, "压力表",
						BukovLootItem.Category.LOOT, 0.65f, 410),
				loot("relay_module", 7, 1, 2, "继电模块",
						BukovLootItem.Category.LOOT, 0.25f, 560),
				loot("copper_coil", 10, 1, 2, "铜线圈",
						BukovLootItem.Category.LOOT, 0.80f, 390),
				loot("machine_oil", 9, 1, 1, "机械润滑油",
						BukovLootItem.Category.LOOT, 0.90f, 360),
				firearm("shuttle_9", 3, "梭子-9", 2.20f, 2100),
				firearm("hive_9", 2, "蜂巢-9", 2.55f, 3200),
				firearm("whisper_9", 2, "低语-9", 2.35f, 3900),
				firearm("bolt_12", 2, "门栓-12", 3.10f, 4800),
				firearm("breaker_12", 1, "破门-12", 3.55f, 5900),
				firearm("river_556", 1, "河谷-556", 2.85f, 4700),
				ammo("ammo_556_standard", 8, 5, 16, 0.013f, 18),
				ammo("ammo_556_armor_piercing", 4, 4, 10, 0.013f, 42),
				ammo("ammo_12g_buckshot", 5, 3, 8, 0.045f, 28)));

		register(tables, table(HIGH_VALUE,
				loot("gold_watch", 9, 1, 1, "金表",
						BukovLootItem.Category.HIGH_VALUE, 0.12f, 1800),
				loot("encrypted_drive", 7, 1, 1, "加密硬盘",
						BukovLootItem.Category.HIGH_VALUE, 0.20f, 2400),
				loot("antique_coin", 10, 1, 2, "古董硬币",
						BukovLootItem.Category.HIGH_VALUE, 0.05f, 1600),
				loot("camera_lens", 11, 1, 1, "相机镜头",
						BukovLootItem.Category.HIGH_VALUE, 0.35f, 1350),
				loot("military_chip", 4, 1, 1, "军用芯片",
						BukovLootItem.Category.HIGH_VALUE, 0.08f, 3200),
				loot("radio_crystal", 6, 1, 1, "军用频率晶体",
						BukovLootItem.Category.HIGH_VALUE, 0.06f, 2100),
				loot("optical_sensor", 5, 1, 1, "精密光学传感器",
						BukovLootItem.Category.HIGH_VALUE, 0.18f, 2700),
				firearm("ward_556", 3, "城防-556", 3.00f, 4200),
				firearm("mountain_762", 2, "山路-762", 3.60f, 6100),
				firearm("longstreet_762", 1, "长街-762", 4.00f, 7600),
				firearm("jackal_9", 2, "胡狼-9", 2.70f, 4400),
				firearm("foundry_762", 1, "铸炉-762", 4.15f, 7200),
				firearm("carbine_556", 2, "岗哨-556", 2.65f, 5600),
				firearm("watchtower_556", 1, "瞭望-556", 3.70f, 8900),
				firearm("frontier_762", 1, "边界-762", 4.60f, 11200),
				ammo("ammo_762_standard", 7, 4, 12, 0.024f, 24),
				ammo("ammo_762_expanding", 4, 3, 9, 0.025f, 48)));

		register(tables, table(BOSS,
				loot("officer_badge", 12, 1, 1, "军官徽章",
						BukovLootItem.Category.BOSS, 0.10f, 2800),
				loot("command_key", 10, 1, 1, "指挥钥匙",
						BukovLootItem.Category.BOSS, 0.06f, 4200),
				loot("prototype_core", 5, 1, 1, "原型核心",
						BukovLootItem.Category.BOSS, 1.50f, 6800),
				loot("classified_docs", 8, 1, 1, "机密文件",
						BukovLootItem.Category.BOSS, 0.25f, 5200),
				loot("titanium_case", 4, 1, 1, "钛合金箱",
						BukovLootItem.Category.BOSS, 2.20f, 7600),
				firearm("rainstorm_12", 1, "暴雨-12", 4.80f, 9800)));

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
			String displayName,
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
						displayName,
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
			String displayName,
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
						displayName,
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
