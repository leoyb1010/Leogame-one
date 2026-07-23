package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovGearRules;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLoadout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementReceipt;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable, renderer-independent data for the Bukov hideout. */
public final class BukovHubViewModel {

	public enum LoadoutSlot {
		PRIMARY("主武器", "WEAPON"),
		AMMUNITION("弹药", "AMMO"),
		MEDICAL("医疗", "MED"),
		ARMOR("护甲", "ARMOR"),
		BACKPACK("背包", "PACK"),
		GEAR("物资", "GEAR");

		public final String label;
		public final String code;

		LoadoutSlot(String label, String code) {
			this.label = label;
			this.code = code;
		}
	}

	public static final class ItemRow {
		public final String itemUid;
		public final String definitionId;
		public final String label;
		public final LoadoutSlot slot;
		public final int quantity;
		public final float weight;
		public final long value;
		public final boolean selected;
		public final boolean deployable;

		private ItemRow(RaidItem item, boolean selected) {
			itemUid = item.itemUid();
			definitionId = item.definitionId();
			label = displayName(item.definitionId());
			slot = slotFor(item.definitionId());
			quantity = item.quantity();
			weight = item.totalWeight();
			value = item.totalValue();
			this.selected = selected;
			deployable = BukovLoadout.deployable(item);
		}

		public String summary() {
			return label + " ×" + quantity + "  "
					+ formatWeight(weight) + "kg · 价值" + value;
		}
	}

	public static final class Settlement {
		public final String raidId;
		public final RaidOutcome outcome;
		public final List<String> itemUids;
		public final long quantity;
		public final long value;
		public final int kills;
		public final boolean missionCompleted;

		private Settlement(SettlementReceipt receipt) {
			raidId = receipt.raidId();
			outcome = receipt.outcome();
			if (outcome == RaidOutcome.SUCCESS) {
				itemUids = receipt.transferredUids();
				quantity = receipt.transferredQuantity();
				value = receipt.transferredValue();
			} else {
				itemUids = receipt.lostUids();
				quantity = receipt.lostQuantity();
				value = receipt.lostValue();
			}
			kills = receipt.debriefAvailable() ? receipt.kills() : 0;
			missionCompleted =
					receipt.debriefAvailable() && receipt.missionCompleted();
		}

		public String headline() {
			return outcome == RaidOutcome.SUCCESS
					? "撤离成功 · 带回 " + quantity + " 件 · 价值+" + value
					: "行动失败 · 损失 " + quantity + " 件 · 价值-" + value;
		}
	}

	public final List<ItemRow> stashItems;
	public final int selectedCount;
	public final float totalWeight;
	public final long riskValue;
	public final long stashValue;
	public final long currency;
	public final boolean overweight;
	public final boolean canDeploy;
	public final String deploymentBlockReason;
	public final boolean canRepeatLoadout;
	public final Settlement latestSettlement;
	public final boolean activeRaid;
	public final String activeRaidId;
	public final float activeElapsedSeconds;
	public final boolean canEditLoadout;
	public final BukovRaidMode raidMode;
	public final String raidModeName;
	public final String raidModeSummary;
	private final float weightLimit;

	private BukovHubViewModel(
			List<ItemRow> stashItems,
			int selectedCount,
			float totalWeight,
			long riskValue,
			long stashValue,
			long currency,
			boolean overweight,
			boolean canDeploy,
			String deploymentBlockReason,
			boolean canRepeatLoadout,
			Settlement latestSettlement,
			boolean activeRaid,
			String activeRaidId,
			float activeElapsedSeconds,
			float weightLimit,
			BukovRaidMode raidMode) {
		this.stashItems = Collections.unmodifiableList(stashItems);
		this.selectedCount = selectedCount;
		this.totalWeight = totalWeight;
		this.riskValue = riskValue;
		this.stashValue = stashValue;
		this.currency = currency;
		this.overweight = overweight;
		this.canDeploy = canDeploy;
		this.deploymentBlockReason = deploymentBlockReason;
		this.canRepeatLoadout = canRepeatLoadout;
		this.latestSettlement = latestSettlement;
		this.activeRaid = activeRaid;
		this.activeRaidId = activeRaidId;
		this.activeElapsedSeconds = activeElapsedSeconds;
		this.canEditLoadout = !activeRaid;
		this.weightLimit = weightLimit;
		this.raidMode = raidMode;
		raidModeName = raidMode.displayName;
		raidModeSummary = raidMode.summary;
	}

	static BukovHubViewModel from(BukovProfile profile, float weightLimit) {
		return from(profile, null, weightLimit, null, null);
	}

	static BukovHubViewModel from(
			BukovProfile profile,
			BukovRaidCheckpoint activeCheckpoint,
			float weightLimit) {
		return from(profile, activeCheckpoint, weightLimit, null, null);
	}

	static BukovHubViewModel from(
			BukovProfile profile,
			float weightLimit,
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		return from(profile, null, weightLimit, firearms, ammunition);
	}

	private static BukovHubViewModel from(
			BukovProfile profile,
			BukovRaidCheckpoint activeCheckpoint,
			float weightLimit,
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		List<ItemRow> rows = new ArrayList<>();
		boolean activeRaid = activeCheckpoint != null;
		if (activeRaid) {
			for (RaidItem item : activeCheckpoint.loot().items()) {
				rows.add(new ItemRow(item, true));
			}
		} else {
			for (RaidItem item : profile.stash().items()) {
				rows.add(new ItemRow(
						item,
						profile.loadout().contains(item.itemUid())));
			}
		}
		List<SettlementReceipt> receipts = profile.settlements();
		Settlement latest = receipts.isEmpty()
				? null
				: new Settlement(receipts.get(receipts.size() - 1));
		float effectiveWeightLimit = activeRaid
				? activeCheckpoint.loot().maxWeight()
				: BukovGearRules.resolve(
						profile.loadout().items(profile.stash()),
						weightLimit).weightCapacityKg;
		float weight = activeRaid
				? activeCheckpoint.loot().totalWeight()
				: profile.loadout().totalWeight(profile.stash());
		List<RaidItem> selectedItems = activeRaid
				? activeCheckpoint.loot().items()
				: profile.loadout().items(profile.stash());
		boolean overweight = !activeRaid
				&& (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(weight)
				|| weight > effectiveWeightLimit);
		BukovRaidMode raidMode = activeRaid
				? activeCheckpoint.session().raidMode()
				: profile.selectedRaidMode();
		String deploymentBlockReason = activeRaid
				? null
				: !raidMode.usesPlayerLoadout()
				? null
				: deploymentBlockReason(
						selectedItems,
						overweight,
						effectiveWeightLimit,
						firearms,
						ammunition);
		return new BukovHubViewModel(
				rows,
				activeRaid
						? activeCheckpoint.loot().distinctItemCount()
						: profile.loadout().distinctItemCount(),
				weight,
				activeRaid
						? activeCheckpoint.loot().totalValue()
						: profile.loadout().totalValue(profile.stash()),
				profile.stash().totalValue(),
				profile.currency(),
				overweight,
				deploymentBlockReason == null,
				deploymentBlockReason,
				!activeRaid && !profile.lastLoadoutDefinitions().isEmpty(),
				latest,
				activeRaid,
				activeRaid ? activeCheckpoint.session().raidId : null,
				activeRaid ? activeCheckpoint.session().elapsedSeconds : 0f,
				effectiveWeightLimit,
				raidMode);
	}

	private static String deploymentBlockReason(
			List<RaidItem> selectedItems,
			boolean overweight,
			float weightLimit,
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		if (overweight) {
			return "出战配置超过 "
					+ formatWeight(weightLimit)
					+ " kg 上限";
		}
		RaidItem primary = null;
		for (RaidItem item : selectedItems) {
			if (requiredCaliber(item.definitionId(), firearms) != null) {
				primary = item;
				break;
			}
		}
		if (primary == null) {
			return "至少选择一把主武器";
		}
		for (RaidItem item : selectedItems) {
			if (compatible(
					primary.definitionId(),
					item.definitionId(),
					firearms,
					ammunition)) {
				return null;
			}
		}
		return displayName(primary.definitionId()) + "缺少兼容弹药";
	}

	static boolean compatible(
			String firearmDefinitionId,
			String ammunitionDefinitionId) {
		return compatible(
				firearmDefinitionId,
				ammunitionDefinitionId,
				null,
				null);
	}

	static boolean compatible(
			String firearmDefinitionId,
			String ammunitionDefinitionId,
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		if (firearmDefinitionId == null
				|| !firearmDefinitionId.startsWith("firearm:")
				|| ammunitionDefinitionId == null
				|| !ammunitionDefinitionId.startsWith("ammo:")) {
			return false;
		}
		RegistryPair defaults = null;
		if (firearms == null || ammunition == null) {
			defaults = defaultRegistries();
			if (defaults != null) {
				firearms = defaults.firearms;
				ammunition = defaults.ammunition;
			}
		}
		String firearmId = stripPrefix(
				firearmDefinitionId,
				"firearm:");
		String ammunitionId = stripPrefix(
				ammunitionDefinitionId,
				"ammo:");
		if (firearms != null && ammunition != null) {
			FirearmDefinition firearm = firearms.find(firearmId);
			return firearm != null
					&& ammunition.compatible(
							ammunitionId,
							firearm.caliber);
		}
		// Unit tests can build this pure view model before libGDX owns Files.
		// Production always takes the registry branch above.
		String caliberCode = authoredCaliberCode(firearmId);
		return caliberCode != null
				&& ammunitionId.startsWith(
						"ammo_" + caliberCode + "_");
	}

	private static String requiredCaliber(
			String firearmDefinitionId,
			FirearmRegistry firearms) {
		if (firearmDefinitionId == null
				|| !firearmDefinitionId.startsWith("firearm:")) {
			return null;
		}
		if (firearms == null) {
			RegistryPair defaults = defaultRegistries();
			firearms = defaults == null ? null : defaults.firearms;
		}
		String firearmId = stripPrefix(
				firearmDefinitionId,
				"firearm:");
		if (firearms != null) {
			FirearmDefinition firearm = firearms.find(firearmId);
			return firearm == null ? null : firearm.caliber;
		}
		return authoredCaliberCode(firearmId);
	}

	private static String authoredCaliberCode(String firearmId) {
		if (firearmId == null) {
			return null;
		}
		int separator = firearmId.lastIndexOf('_');
		if (separator < 0 || separator == firearmId.length() - 1) {
			return null;
		}
		String code = firearmId.substring(separator + 1);
		return "12".equals(code) ? "12g" : code;
	}

	private static RegistryPair cachedRegistries;

	private static synchronized RegistryPair defaultRegistries() {
		if (cachedRegistries != null) {
			return cachedRegistries;
		}
		if (Gdx.files == null) {
			return null;
		}
		FirearmRegistry firearms = new FirearmRegistry();
		firearms.loadDefault();
		AmmoRegistry ammunition = new AmmoRegistry();
		ammunition.loadDefault();
		firearms.validateAmmunition(ammunition);
		cachedRegistries = new RegistryPair(firearms, ammunition);
		return cachedRegistries;
	}

	private static final class RegistryPair {
		private final FirearmRegistry firearms;
		private final AmmoRegistry ammunition;

		private RegistryPair(
				FirearmRegistry firearms,
				AmmoRegistry ammunition) {
			this.firearms = firearms;
			this.ammunition = ammunition;
		}
	}

	private static String stripPrefix(String value, String prefix) {
		if (value == null) {
			return "";
		}
		return value.startsWith(prefix)
				? value.substring(prefix.length())
				: value;
	}

	public String loadoutSummary() {
		return formatWeight(totalWeight) + "/"
				+ formatWeight(weightLimit)
				+ "kg";
	}

	public String activeRaidSummary() {
		if (!activeRaid) {
			return "";
		}
		int elapsed = Math.max(0, (int) activeElapsedSeconds);
		int minutes = elapsed / 60;
		int seconds = elapsed % 60;
		return "检查点已保存 · "
				+ String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
	}

	public String slotSummary(LoadoutSlot slot) {
		int stacks = 0;
		int quantity = 0;
		String first = null;
		for (ItemRow row : stashItems) {
			if (row.selected && row.slot == slot) {
				stacks++;
				quantity += row.quantity;
				if (first == null) {
					first = row.label;
				}
			}
		}
		if (first == null) {
			return "未配置";
		}
		if (slot == LoadoutSlot.AMMUNITION) {
			return quantity + " 发" + (stacks > 1 ? " / " + stacks + "组" : "");
		}
		first = compact(first, slot == LoadoutSlot.MEDICAL ? 7 : 10);
		if (stacks == 1) {
			return first + (quantity > 1 ? " ×" + quantity : "");
		}
		return first + " +" + (stacks - 1);
	}

	private static String compact(String value, int maxCharacters) {
		if (value.length() <= maxCharacters) {
			return value;
		}
		return value.substring(0, maxCharacters - 3) + "...";
	}

	static LoadoutSlot slotFor(String definitionId) {
		String value = definitionId == null
				? ""
				: definitionId.toLowerCase(Locale.ROOT);
		if (value.startsWith("firearm:") || value.contains("weapon")) {
			return LoadoutSlot.PRIMARY;
		}
		if (value.startsWith("ammo:") || value.contains("ammunition")) {
			return LoadoutSlot.AMMUNITION;
		}
		if (BukovGearRules.slotFor(definitionId)
				== BukovGearRules.Slot.ARMOR) {
			return LoadoutSlot.ARMOR;
		}
		if (BukovGearRules.slotFor(definitionId)
				== BukovGearRules.Slot.BACKPACK) {
			return LoadoutSlot.BACKPACK;
		}
		if (value.equals("bandage") || value.contains("medical")
				|| value.contains("medkit") || value.contains("medicine")) {
			return LoadoutSlot.MEDICAL;
		}
		return LoadoutSlot.GEAR;
	}

	static String displayName(String definitionId) {
		String normalized = definitionId == null ? "" : definitionId.trim();
		if (normalized.isEmpty()) {
			return "未知物资";
		}
		String known = KNOWN_NAMES.get(normalized.toLowerCase(Locale.ROOT));
		if (known != null) {
			return known;
		}
		Item authored = BukovFirstRaidLootTables
				.createByEconomicDefinitionId(normalized);
		if (authored != null && !normalized.startsWith("ammo:")) {
			return authored.name();
		}
		int separator = normalized.indexOf(':');
		String readable = separator >= 0
				? normalized.substring(separator + 1)
				: normalized;
		if (readable.startsWith("ammo_")) {
			readable = readable.substring("ammo_".length());
		}
		String[] words = readable.replace('-', ' ')
				.replace('_', ' ')
				.trim()
				.split("\\s+");
		StringBuilder friendly = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (friendly.length() > 0) {
				friendly.append(' ');
			}
			friendly.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				friendly.append(word.substring(1));
			}
		}
		return friendly.length() == 0 ? "未知物资" : friendly.toString();
	}

	static String formatWeight(float value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static final Map<String, String> KNOWN_NAMES = knownNames();

	private static Map<String, String> knownNames() {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("firearm:needle_9", "针蜂-9");
		values.put("firearm:shuttle_9", "梭子-9");
		values.put("firearm:ward_556", "城防-556");
		values.put("firearm:mountain_762", "山路-762");
		values.put("firearm:bolt_12", "门栓-12");
		values.put("firearm:longstreet_762", "长街-762");
		values.put("ammo:ammo_9_training", "9毫米训练弹");
		values.put("ammo:ammo_9_standard", "9毫米标准弹");
		values.put("ammo:ammo_9_subsonic", "9毫米亚音速弹");
		values.put("ammo:ammo_556_standard", "5.56毫米标准弹");
		values.put("ammo:ammo_556_armor_piercing", "5.56毫米硬芯弹");
		values.put("ammo:ammo_762_standard", "7.62毫米标准弹");
		values.put("ammo:ammo_762_expanding", "7.62毫米扩张弹");
		values.put("ammo:ammo_12g_buckshot", "12号鹿弹");
		values.put("bandage", "战术绷带");
		values.put("medical:bandage", "战术绷带");
		values.put("medical:field_medkit", "野战医疗包");
		values.put("armor:soft_vest", "软质防弹衣");
		values.put("armor:patrol_vest", "巡逻防弹背心");
		values.put("armor:ceramic_rig", "陶瓷战术甲");
		values.put("backpack:scout_pack", "侦察背包");
		values.put("backpack:field_pack", "野战背包");
		return Collections.unmodifiableMap(values);
	}
}
