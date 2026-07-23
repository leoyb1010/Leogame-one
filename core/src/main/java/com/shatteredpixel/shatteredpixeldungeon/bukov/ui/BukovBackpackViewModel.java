package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.MedicalCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Immutable, renderer-independent snapshot for the raid-only backpack. */
public final class BukovBackpackViewModel {

	public enum Category {
		FIREARM("武器", "GUN"),
		AMMUNITION("弹药", "AMMO"),
		MEDICAL("医疗", "MED"),
		MISSION("任务", "TASK"),
		LOOT("物资", "LOOT");

		public final String label;
		public final String code;

		Category(String label, String code) {
			this.label = label;
			this.code = code;
		}
	}

	/** Runtime-only firearm state not represented by the durable raid ledger. */
	public static final class EquippedFirearm {
		public final String itemUid;
		public final int magazineAmmo;
		public final int magazineCapacity;

		public EquippedFirearm(
				String itemUid,
				int magazineAmmo,
				int magazineCapacity) {
			if (itemUid == null || itemUid.trim().isEmpty()) {
				throw new IllegalArgumentException("itemUid is required");
			}
			if (magazineAmmo < 0 || magazineCapacity < 0
					|| magazineAmmo > magazineCapacity) {
				throw new IllegalArgumentException("invalid magazine state");
			}
			this.itemUid = itemUid;
			this.magazineAmmo = magazineAmmo;
			this.magazineCapacity = magazineCapacity;
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
					definition.magazineSize);
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
		public final int magazineAmmo;
		public final int magazineCapacity;
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
			name = displayName(item.definitionId(), firearms);
			quantity = item.quantity();
			unitWeight = item.unitWeight();
			totalWeight = item.totalWeight();
			unitValue = item.unitValue();
			totalValue = item.totalValue();
			durability = item.durability();
			equipped = equippedFirearm != null
					&& itemUid.equals(equippedFirearm.itemUid);
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
			} else {
				magazineCapacity = 0;
				magazineAmmo = 0;
			}
			canDrop = category != Category.MISSION;
			canUse = category == Category.MEDICAL;
			canEquip = category == Category.FIREARM && !equipped;
		}

		public String title() {
			return (equipped ? "已装备 · " : "") + name + " ×" + quantity;
		}

		public String economySummary() {
			return formatWeight(unitWeight) + "kg/件 · 共"
					+ formatWeight(totalWeight) + "kg · 价值"
					+ unitValue + "/件 · 共" + totalValue;
		}

		/** Compact enough for a 154px iPhone portrait tactical row. */
		public String rowEconomySummary() {
			return "单" + formatWeight(unitWeight) + "kg"
					+ " · 总" + formatWeight(totalWeight) + "kg"
					+ " · 值" + totalValue;
		}

		public String stateSummary() {
			if (category == Category.FIREARM) {
				return "弹匣 " + magazineAmmo + "/" + magazineCapacity
						+ " · 耐久 " + Math.round(durability * 100f) + "%";
			}
			if (category == Category.MISSION) {
				return "任务档案 · 不可丢弃";
			}
			if (category == Category.AMMUNITION) {
				return "携带 " + quantity + " 发";
			}
			if (category == Category.MEDICAL) {
				return "可在行动中使用 · 使用时会关闭背包";
			}
			return "搜刽物资 · 成功撤离后进入仓库";
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

	public static String formatWeight(float weight) {
		return String.format(Locale.ROOT, "%.2f", weight);
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

	private static String displayName(
			String definitionId,
			FirearmRegistry firearms) {
		FirearmDefinition firearm = firearmDefinition(definitionId, firearms);
		if (firearm != null) {
			return firearm.name;
		}
		String normalized = normalize(definitionId);
		String knownAmmo = ammoName(normalized);
		if (knownAmmo != null) {
			return knownAmmo;
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
		return readable.isEmpty() ? "未知物资" : readable;
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
				return "9毫米训练弹";
			case "ammo:ammo_9_standard":
				return "9毫米标准弹";
			case "ammo:ammo_9_subsonic":
				return "9毫米亚音速弹";
			case "ammo:ammo_556_standard":
				return "5.56毫米标准弹";
			case "ammo:ammo_556_armor_piercing":
				return "5.56毫米硬芯弹";
			case "ammo:ammo_762_standard":
				return "7.62毫米标准弹";
			case "ammo:ammo_762_expanding":
				return "7.62毫米扩张弹";
			case "ammo:ammo_12g_buckshot":
				return "12号鹿弹";
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
