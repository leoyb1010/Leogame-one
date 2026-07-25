package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.MedicalCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Immutable, renderer-independent snapshot for the raid-only backpack. */
public final class BukovBackpackViewModel {

	public enum Category {
		FIREARM("bukov.raid.backpack.category_firearm", "GUN"),
		AMMUNITION("bukov.raid.backpack.category_ammunition", "AMMO"),
		MEDICAL("bukov.raid.backpack.category_medical", "MED"),
		MISSION("bukov.raid.backpack.category_mission", "TASK"),
		LOOT("bukov.raid.backpack.category_loot", "LOOT");

		public final String label;
		public final String code;

		Category(String labelKey, String code) {
			label = BukovMessages.get(labelKey);
			this.code = code;
		}
	}

	/** Runtime-only firearm state not represented by the durable raid ledger. */
	public static final class EquippedFirearm {
		public final String itemUid;
		public final int magazineAmmo;
		public final int magazineCapacity;
		public final float durability;
		public final float heat;
		public final float fouling;

		public EquippedFirearm(
				String itemUid,
				int magazineAmmo,
				int magazineCapacity) {
			this(
					itemUid,
					magazineAmmo,
					magazineCapacity,
					1f,
					0f,
					0f);
		}

		public EquippedFirearm(
				String itemUid,
				int magazineAmmo,
				int magazineCapacity,
				float durability,
				float heat,
				float fouling) {
			if (itemUid == null || itemUid.trim().isEmpty()) {
				throw new IllegalArgumentException("itemUid is required");
			}
			if (magazineAmmo < 0 || magazineCapacity < 0
					|| magazineAmmo > magazineCapacity) {
				throw new IllegalArgumentException("invalid magazine state");
			}
			if (!BukovNumbers.isFinite(durability)
					|| !BukovNumbers.isFinite(heat)
					|| !BukovNumbers.isFinite(fouling)
					|| durability < 0f || durability > 1f
					|| heat < 0f || heat > 1f
					|| fouling < 0f || fouling > 1f) {
				throw new IllegalArgumentException(
						"firearm condition must be between 0 and 1");
			}
			this.itemUid = itemUid;
			this.magazineAmmo = magazineAmmo;
			this.magazineCapacity = magazineCapacity;
			this.durability = durability;
			this.heat = heat;
			this.fouling = fouling;
		}

		public static EquippedFirearm from(
				Firearm firearm,
				FirearmRegistry registry) {
			if (firearm == null) {
				return null;
			}
			FirearmDefinition definition = firearm.definition(registry);
			return new EquippedFirearm(
					firearm.itemUid(),
					firearm.magazineAmmo(),
					definition.magazineSize,
					firearm.durability(),
					firearm.heat(),
					firearm.fouling());
		}
	}

	public static final class ItemRow {
		public final String itemUid;
		public final String definitionId;
		public final String name;
		public final Category category;
		public final int quantity;
		public final float unitWeight;
		public final float totalWeight;
		public final int unitValue;
		public final long totalValue;
		public final float durability;
		public final float heat;
		public final float fouling;
		public final int magazineAmmo;
		public final int magazineCapacity;
		public final String weaponProfile;
		public final boolean equipped;
		public final boolean canDrop;
		public final boolean canUse;
		public final boolean canEquip;

		private ItemRow(
				RaidItem item,
				FirearmRegistry firearms,
				EquippedFirearm equippedFirearm) {
			itemUid = item.itemUid();
			definitionId = item.definitionId();
			category = category(item.definitionId());
			name = localizedDisplayName(item.definitionId(), firearms);
			quantity = item.quantity();
			unitWeight = item.unitWeight();
			totalWeight = item.totalWeight();
			unitValue = item.unitValue();
			totalValue = item.totalValue();
			equipped = equippedFirearm != null
					&& itemUid.equals(equippedFirearm.itemUid);
			durability = equipped
					? equippedFirearm.durability
					: item.durability();
			heat = equipped ? equippedFirearm.heat : 0f;
			fouling = equipped
					? equippedFirearm.fouling
					: item.fouling();
			if (category == Category.FIREARM) {
				FirearmDefinition definition = firearmDefinition(
						item.definitionId(),
						firearms);
				magazineCapacity = equipped
						? equippedFirearm.magazineCapacity
						: definition == null ? 0 : definition.magazineSize;
				magazineAmmo = equipped
						? equippedFirearm.magazineAmmo
						: 0;
				weaponProfile = definition == null
						? BukovMessages.get(
								"bukov.raid.backpack.weapon_data_unavailable")
						: BukovMessages.get(
								"bukov.raid.backpack.weapon_profile_format",
								firearmClassName(definition.weaponClass),
								definition.caliber,
								definition.fireMode.name().equals("AUTO")
										? BukovMessages.get(
												"bukov.raid.backpack.fire_mode_auto")
										: BukovMessages.get(
												"bukov.raid.backpack.fire_mode_semi"),
								compactNumber(definition.damage),
								compactNumber(definition.recoilPerShot));
			} else {
				magazineCapacity = 0;
				magazineAmmo = 0;
				weaponProfile = "";
			}
			canDrop = category != Category.MISSION;
			canUse = category == Category.MEDICAL;
			canEquip = category == Category.FIREARM && !equipped;
		}

		public String title() {
			return BukovMessages.get(
					equipped
							? "bukov.raid.backpack.item_title_equipped_format"
							: "bukov.raid.backpack.item_title_format",
					name,
					quantity);
		}

		public String economySummary() {
			return BukovMessages.get(
					"bukov.raid.backpack.economy_summary_format",
					formatWeight(unitWeight),
					formatWeight(totalWeight),
					unitValue,
					totalValue);
		}

		/** Compact enough for a 154px iPhone portrait tactical row. */
		public String rowEconomySummary() {
			return BukovMessages.get(
					"bukov.raid.backpack.row_economy_summary_format",
					formatWeight(unitWeight),
					formatWeight(totalWeight),
					totalValue);
		}

		public String stateSummary() {
			if (category == Category.FIREARM) {
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_state_format",
						weaponProfile,
						magazineAmmo,
						magazineCapacity,
						Math.round(durability * 100f),
						Math.round(fouling * 100f),
						Math.round(heat * 100f));
			}
			if (category == Category.MISSION) {
				return BukovMessages.get(
						"bukov.raid.backpack.mission_state");
			}
			if (category == Category.AMMUNITION) {
				return BukovMessages.get(
						"bukov.raid.backpack.ammunition_state_format",
						quantity);
			}
			if (category == Category.MEDICAL) {
				return BukovMessages.get(
						"bukov.raid.backpack.medical_state");
			}
			return BukovMessages.get(
					"bukov.raid.backpack.loot_state");
		}
	}

	public final List<ItemRow> items;
	public final float totalWeight;
	public final float maximumWeight;
	public final long totalValue;

	private BukovBackpackViewModel(
			List<ItemRow> items,
			float totalWeight,
			float maximumWeight,
			long totalValue) {
		this.items = Collections.unmodifiableList(items);
		this.totalWeight = totalWeight;
		this.maximumWeight = maximumWeight;
		this.totalValue = totalValue;
	}

	public static BukovBackpackViewModel from(
			LootTransaction ledger,
			FirearmRegistry firearms,
			EquippedFirearm equippedFirearm) {
		if (ledger == null || firearms == null) {
			throw new IllegalArgumentException("ledger and firearms are required");
		}
		List<ItemRow> rows = new ArrayList<>();
		for (RaidItem item : ledger.items()) {
			rows.add(new ItemRow(item, firearms, equippedFirearm));
		}
		Collections.sort(rows, new Comparator<ItemRow>() {
			@Override
			public int compare(ItemRow first, ItemRow second) {
				int categoryOrder = Integer.compare(
						first.category.ordinal(),
						second.category.ordinal());
				return categoryOrder != 0
						? categoryOrder
						: first.name.compareTo(second.name);
			}
		});
		return new BukovBackpackViewModel(
				rows,
				ledger.totalWeight(),
				ledger.maxWeight(),
				ledger.totalValue());
	}

	public ItemRow find(String itemUid) {
		for (ItemRow item : items) {
			if (item.itemUid.equals(itemUid)) {
				return item;
			}
		}
		return null;
	}

	public String weightSummary() {
		return formatWeight(totalWeight) + "/"
				+ formatWeight(maximumWeight) + "kg";
	}

	/**
	 * One-line raid summary sized for the narrow portrait backpack header.
	 * The old copy repeated labels and spaces until the carried value was
	 * clipped off on the smallest supported viewport.
	 */
	public String totalsSummary() {
		return BukovMessages.get(
				"bukov.raid.backpack.totals_format",
				weightSummary(),
				totalValue);
	}

	public static String formatWeight(float weight) {
		return String.format(Locale.ROOT, "%.2f", weight);
	}

	private static String compactNumber(float value) {
		return value == Math.round(value)
				? Integer.toString(Math.round(value))
				: String.format(Locale.ROOT, "%.2f", value)
						.replaceAll("0+$", "")
						.replaceAll("\\.$", "");
	}

	private static String firearmClassName(FirearmClass weaponClass) {
		if (weaponClass == null) {
			return BukovMessages.get(
					"bukov.raid.backpack.firearm_class_default");
		}
		switch (weaponClass) {
			case PISTOL:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_pistol");
			case SUBMACHINE_GUN:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_smg");
			case CARBINE:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_carbine");
			case ASSAULT_RIFLE:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_assault_rifle");
			case SHOTGUN:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_shotgun");
			case MARKSMAN_RIFLE:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_marksman");
			case HEAVY_WEAPON:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_heavy");
			default:
				return BukovMessages.get(
						"bukov.raid.backpack.firearm_class_default");
		}
	}

	private static Category category(String definitionId) {
		String normalized = normalize(definitionId);
		if (normalized.startsWith("firearm:")) {
			return Category.FIREARM;
		}
		if (normalized.startsWith("ammo:")) {
			return Category.AMMUNITION;
		}
		if (MedicalCatalog.find(normalized) != null) {
			return Category.MEDICAL;
		}
		if (FirstRaidMission.ARCHIVE_DEFINITION_ID.equals(definitionId)) {
			return Category.MISSION;
		}
		return Category.LOOT;
	}

	public static String localizedDisplayName(
			String definitionId,
			FirearmRegistry firearms) {
		FirearmDefinition firearm = firearmDefinition(definitionId, firearms);
		if (firearm != null) {
			return localizedFirearmName(firearm);
		}
		String normalized = normalize(definitionId);
		String knownAmmo = ammoName(normalized);
		if (knownAmmo != null) {
			return knownAmmo;
		}
		String knownMedical = medicalName(normalized);
		if (knownMedical != null) {
			return knownMedical;
		}
		if (FirstRaidMission.ARCHIVE_DEFINITION_ID.equals(normalized)) {
			return BukovMessages.get(
					"bukov.raid.item.maintenance_access_archive");
		}
		String knownLoot = lootName(normalized);
		if (knownLoot != null) {
			return knownLoot;
		}
		Item authored = BukovFirstRaidLootTables
				.createByEconomicDefinitionId(definitionId);
		if (authored != null) {
			return authored.name();
		}
		String readable = normalized;
		int separator = readable.indexOf(':');
		if (separator >= 0) {
			readable = readable.substring(separator + 1);
		}
		readable = readable.replace('_', ' ').trim();
		return readable.isEmpty()
				? BukovMessages.get(
						"bukov.raid.backpack.unknown_item")
				: readable;
	}

	public static String localizedFirearmName(
			FirearmDefinition definition) {
		if (definition == null || definition.id == null) {
			return BukovMessages.get(
					"bukov.raid.backpack.unknown_item");
		}
		switch (definition.id) {
			case "needle_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_needle_9");
			case "shuttle_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_shuttle_9");
			case "ward_556":
				return BukovMessages.get(
						"bukov.raid.item.firearm_ward_556");
			case "mountain_762":
				return BukovMessages.get(
						"bukov.raid.item.firearm_mountain_762");
			case "bolt_12":
				return BukovMessages.get(
						"bukov.raid.item.firearm_bolt_12");
			case "longstreet_762":
				return BukovMessages.get(
						"bukov.raid.item.firearm_longstreet_762");
			case "sentinel_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_sentinel_9");
			case "sparrow_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_sparrow_9");
			case "hive_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_hive_9");
			case "whisper_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_whisper_9");
			case "jackal_9":
				return BukovMessages.get(
						"bukov.raid.item.firearm_jackal_9");
			case "river_556":
				return BukovMessages.get(
						"bukov.raid.item.firearm_river_556");
			case "foundry_762":
				return BukovMessages.get(
						"bukov.raid.item.firearm_foundry_762");
			case "carbine_556":
				return BukovMessages.get(
						"bukov.raid.item.firearm_carbine_556");
			case "breaker_12":
				return BukovMessages.get(
						"bukov.raid.item.firearm_breaker_12");
			case "rainstorm_12":
				return BukovMessages.get(
						"bukov.raid.item.firearm_rainstorm_12");
			case "watchtower_556":
				return BukovMessages.get(
						"bukov.raid.item.firearm_watchtower_556");
			case "frontier_762":
				return BukovMessages.get(
						"bukov.raid.item.firearm_frontier_762");
			default:
				return definition.name == null
						? BukovMessages.get(
								"bukov.raid.backpack.unknown_item")
						: definition.name;
		}
	}

	private static FirearmDefinition firearmDefinition(
			String definitionId,
			FirearmRegistry firearms) {
		String normalized = normalize(definitionId);
		if (!normalized.startsWith("firearm:")) {
			return null;
		}
		String id = normalized.substring("firearm:".length());
		for (FirearmDefinition definition : firearms.all()) {
			if (definition.id.equals(id)) {
				return definition;
			}
		}
		return null;
	}

	private static String ammoName(String definitionId) {
		switch (definitionId) {
			case "ammo:ammo_9_training":
				return BukovMessages.get(
						"bukov.raid.item.ammo_9_training");
			case "ammo:ammo_9_standard":
				return BukovMessages.get(
						"bukov.raid.item.ammo_9_standard");
			case "ammo:ammo_9_subsonic":
				return BukovMessages.get(
						"bukov.raid.item.ammo_9_subsonic");
			case "ammo:ammo_556_standard":
				return BukovMessages.get(
						"bukov.raid.item.ammo_556_standard");
			case "ammo:ammo_556_armor_piercing":
				return BukovMessages.get(
						"bukov.raid.item.ammo_556_armor_piercing");
			case "ammo:ammo_762_standard":
				return BukovMessages.get(
						"bukov.raid.item.ammo_762_standard");
			case "ammo:ammo_762_expanding":
				return BukovMessages.get(
						"bukov.raid.item.ammo_762_expanding");
			case "ammo:ammo_12g_buckshot":
				return BukovMessages.get(
						"bukov.raid.item.ammo_12g_buckshot");
			default:
				return null;
		}
	}

	private static String medicalName(String definitionId) {
		switch (definitionId) {
			case "bandage":
				return BukovMessages.get("bukov.raid.item.bandage");
			case "first_aid":
				return BukovMessages.get("bukov.raid.item.first_aid");
			case "tourniquet":
				return BukovMessages.get("bukov.raid.item.tourniquet");
			case "painkiller":
				return BukovMessages.get("bukov.raid.item.painkiller");
			case "antiseptic":
				return BukovMessages.get("bukov.raid.item.antiseptic");
			case "splint":
				return BukovMessages.get("bukov.raid.item.splint");
			case "stim":
				return BukovMessages.get("bukov.raid.item.stim");
			default:
				return null;
		}
	}

	private static String lootName(String definitionId) {
		switch (definitionId) {
			case "canned_food":
				return BukovMessages.get(
						"bukov.raid.item.canned_food");
			case "water_filter":
				return BukovMessages.get(
						"bukov.raid.item.water_filter");
			case "duct_tape":
				return BukovMessages.get(
						"bukov.raid.item.duct_tape");
			case "bolts":
				return BukovMessages.get("bukov.raid.item.bolts");
			case "scrap_metal":
				return BukovMessages.get(
						"bukov.raid.item.scrap_metal");
			case "cloth_roll":
				return BukovMessages.get(
						"bukov.raid.item.cloth_roll");
			case "battery":
				return BukovMessages.get("bukov.raid.item.battery");
			case "ceramic_shard":
				return BukovMessages.get(
						"bukov.raid.item.ceramic_shard");
			case "rubber_hose":
				return BukovMessages.get(
						"bukov.raid.item.rubber_hose");
			case "sealed_coffee":
				return BukovMessages.get(
						"bukov.raid.item.sealed_coffee");
			case "lighter":
				return BukovMessages.get("bukov.raid.item.lighter");
			case "maintenance_key":
				return BukovMessages.get(
						"bukov.raid.item.maintenance_key");
			case "copper_wire":
				return BukovMessages.get(
						"bukov.raid.item.copper_wire");
			case "electric_motor":
				return BukovMessages.get(
						"bukov.raid.item.electric_motor");
			case "bearing":
				return BukovMessages.get("bukov.raid.item.bearing");
			case "circuit_board":
				return BukovMessages.get(
						"bukov.raid.item.circuit_board");
			case "fuel_can":
				return BukovMessages.get("bukov.raid.item.fuel_can");
			case "tool_set":
				return BukovMessages.get("bukov.raid.item.tool_set");
			case "welding_rod":
				return BukovMessages.get(
						"bukov.raid.item.welding_rod");
			case "pressure_gauge":
				return BukovMessages.get(
						"bukov.raid.item.pressure_gauge");
			case "relay_module":
				return BukovMessages.get(
						"bukov.raid.item.relay_module");
			case "copper_coil":
				return BukovMessages.get(
						"bukov.raid.item.copper_coil");
			case "machine_oil":
				return BukovMessages.get(
						"bukov.raid.item.machine_oil");
			case "gold_watch":
				return BukovMessages.get(
						"bukov.raid.item.gold_watch");
			case "encrypted_drive":
				return BukovMessages.get(
						"bukov.raid.item.encrypted_drive");
			case "antique_coin":
				return BukovMessages.get(
						"bukov.raid.item.antique_coin");
			case "camera_lens":
				return BukovMessages.get(
						"bukov.raid.item.camera_lens");
			case "military_chip":
				return BukovMessages.get(
						"bukov.raid.item.military_chip");
			case "radio_crystal":
				return BukovMessages.get(
						"bukov.raid.item.radio_crystal");
			case "optical_sensor":
				return BukovMessages.get(
						"bukov.raid.item.optical_sensor");
			case "maintenance_optic":
				return BukovMessages.get(
						"bukov.raid.item.maintenance_optic");
			case "maintenance_servo":
				return BukovMessages.get(
						"bukov.raid.item.maintenance_servo");
			case "maintenance_controller":
				return BukovMessages.get(
						"bukov.raid.item.maintenance_controller");
			case "maintenance_bearing_case":
				return BukovMessages.get(
						"bukov.raid.item.maintenance_bearing_case");
			case "officer_badge":
				return BukovMessages.get(
						"bukov.raid.item.officer_badge");
			case "command_key":
				return BukovMessages.get(
						"bukov.raid.item.command_key");
			case "prototype_core":
				return BukovMessages.get(
						"bukov.raid.item.prototype_core");
			case "classified_docs":
				return BukovMessages.get(
						"bukov.raid.item.classified_docs");
			case "titanium_case":
				return BukovMessages.get(
						"bukov.raid.item.titanium_case");
			default:
				return null;
		}
	}

	private static String normalize(String definitionId) {
		return definitionId == null
				? ""
				: definitionId.trim().toLowerCase(Locale.ROOT);
	}
}
