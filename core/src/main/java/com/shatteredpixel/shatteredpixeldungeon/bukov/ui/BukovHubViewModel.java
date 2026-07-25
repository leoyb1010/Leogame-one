package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovCareerProgression;
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
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable, renderer-independent data for the Bukov hideout. */
public final class BukovHubViewModel {

	public enum InventoryFilter {
		ALL("all"),
		WEAPONS("weapons"),
		AMMUNITION("ammunition"),
		MEDICAL("medical"),
		EQUIPMENT("equipment"),
		GEAR("gear");

		public final String label;

		InventoryFilter(String key) {
			label = BukovMessages.get("bukov.economy.hub.filter_" + key);
		}

		public InventoryFilter next() {
			InventoryFilter[] values = values();
			return values[(ordinal() + 1) % values.length];
		}

		boolean matches(ItemRow row) {
			if (this == ALL) return true;
			if (this == WEAPONS) return row.slot == LoadoutSlot.PRIMARY;
			if (this == AMMUNITION) return row.slot == LoadoutSlot.AMMUNITION;
			if (this == MEDICAL) return row.slot == LoadoutSlot.MEDICAL;
			if (this == EQUIPMENT) {
				return row.slot == LoadoutSlot.ARMOR
						|| row.slot == LoadoutSlot.BACKPACK;
			}
			return row.slot == LoadoutSlot.GEAR;
		}
	}

	public enum InventorySort {
		STASH_ORDER("stash"),
		VALUE_DESC("value"),
		WEIGHT_ASC("weight"),
		NAME_ASC("name");

		public final String label;

		InventorySort(String key) {
			label = BukovMessages.get("bukov.economy.hub.sort_" + key);
		}

		public InventorySort next() {
			InventorySort[] values = values();
			return values[(ordinal() + 1) % values.length];
		}
	}

	public enum ItemRarity {
		COMMON("common", "rarity.common"),
		UNCOMMON("uncommon", "rarity.uncommon"),
		RARE("rare", "rarity.rare"),
		LEGENDARY("legendary", "rarity.legendary");

		public final String label;
		public final String colorToken;

		ItemRarity(String key, String colorToken) {
			label = BukovMessages.get("bukov.economy.hub.rarity_" + key);
			this.colorToken = colorToken;
		}
	}

	public enum LoadoutSlot {
		PRIMARY("primary"),
		AMMUNITION("ammunition"),
		MEDICAL("medical"),
		ARMOR("armor"),
		BACKPACK("backpack"),
		GEAR("gear");

		public final String label;
		public final String code;

		LoadoutSlot(String key) {
			label = BukovMessages.get("bukov.economy.hub.slot_" + key);
			code = BukovMessages.get("bukov.economy.hub.slot_" + key + "_code");
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
		public final int unitValue;
		public final int valueComparisonPercent;
		public final ItemRarity rarity;
		public final boolean selected;
		public final boolean deployable;

		private ItemRow(
				RaidItem item,
				boolean selected,
				long categoryAverageUnitValue) {
			itemUid = item.itemUid();
			definitionId = item.definitionId();
			label = displayName(item.definitionId());
			slot = slotFor(item.definitionId());
			quantity = item.quantity();
			weight = item.totalWeight();
			value = item.totalValue();
			unitValue = item.unitValue();
			valueComparisonPercent = comparisonPercent(
					unitValue, categoryAverageUnitValue);
			rarity = rarityFor(unitValue);
			this.selected = selected;
			deployable = BukovLoadout.deployable(item);
		}

		public String summary() {
			return BukovMessages.get(
					"bukov.economy.hub.item_summary",
					label,
					quantity,
					formatWeight(weight),
					value);
		}

		public String comparisonLabel() {
			if (valueComparisonPercent == 0) {
				return BukovMessages.get(
						"bukov.economy.hub.comparison_baseline");
			}
			return BukovMessages.get(
					"bukov.economy.hub.comparison",
					valueComparisonPercent > 0 ? "+" : "",
					valueComparisonPercent);
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
			return BukovMessages.get(
					outcome == RaidOutcome.SUCCESS
							? "bukov.economy.hub.settlement_success"
							: "bukov.economy.hub.settlement_failed",
					quantity,
					value);
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
	public final String careerSummary;
	public final String activeContract;
	public final String activeContractObjective;
	public final String selectedMapId;
	public final String selectedMapName;
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
			BukovRaidMode raidMode,
			BukovCareerProgression.Snapshot career,
			String selectedMapId) {
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
		raidModeName = BukovRaidModeSelectionViewModel.modeName(raidMode);
		raidModeSummary = BukovMessages.get(
				"bukov.economy.mode.summary_"
						+ raidMode.name().toLowerCase(Locale.ROOT));
		careerSummary = BukovMessages.get(
				"bukov.economy.hub.career_summary",
				career.completedContracts,
				career.totalContracts,
				career.unlockedMaps,
				career.totalMaps);
		String careerKey = career.nextMapId == null
				? "complete"
				: career.nextMapId;
		activeContract = BukovMessages.get(
				"bukov.economy.hub.contract_" + careerKey + "_title");
		activeContractObjective = BukovMessages.get(
				"bukov.economy.hub.contract_" + careerKey + "_objective");
		this.selectedMapId = selectedMapId;
		selectedMapName = BukovMessages.get(
				BukovCareerProgression.allMapIds().contains(selectedMapId)
						? "bukov.economy.hub.map_" + selectedMapId
						: "bukov.economy.hub.map_unknown");
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
		List<RaidItem> sourceItems = activeCheckpoint != null
				? activeCheckpoint.loot().items()
				: profile.stash().items();
		Map<LoadoutSlot, Long> totals =
				new EnumMap<>(LoadoutSlot.class);
		Map<LoadoutSlot, Integer> counts =
				new EnumMap<>(LoadoutSlot.class);
		for (RaidItem item : sourceItems) {
			LoadoutSlot slot = slotFor(item.definitionId());
			Long total = totals.get(slot);
			Integer count = counts.get(slot);
			totals.put(
					slot,
					(total == null ? 0L : total) + item.unitValue());
			counts.put(slot, (count == null ? 0 : count) + 1);
		}
		List<ItemRow> rows = new ArrayList<>();
		boolean activeRaid = activeCheckpoint != null;
		for (RaidItem item : sourceItems) {
			LoadoutSlot slot = slotFor(item.definitionId());
			long average = totals.get(slot) / counts.get(slot);
			rows.add(new ItemRow(
					item,
					activeRaid
							|| profile.loadout().contains(item.itemUid()),
					average));
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
		BukovCareerProgression.Snapshot career =
				BukovCareerProgression.snapshot(profile);
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
				raidMode,
				career,
				profile.selectedMap());
	}

	private static String deploymentBlockReason(
			List<RaidItem> selectedItems,
			boolean overweight,
			float weightLimit,
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		if (overweight) {
			return BukovMessages.get(
					"bukov.economy.hub.block_overweight",
					formatWeight(weightLimit));
		}
		RaidItem primary = null;
		for (RaidItem item : selectedItems) {
			if (requiredCaliber(item.definitionId(), firearms) != null) {
				primary = item;
				break;
			}
		}
		if (primary == null) {
			return BukovMessages.get(
					"bukov.economy.hub.block_no_primary");
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
		return BukovMessages.get(
				"bukov.economy.hub.block_no_ammo",
				displayName(primary.definitionId()));
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
		return BukovMessages.get(
				"bukov.economy.hub.weight_summary",
				formatWeight(totalWeight),
				formatWeight(weightLimit));
	}

	/**
	 * One short, action-oriented status shared by both hideout surfaces.
	 * Validation remains authoritative; this only makes its current result
	 * unmistakable to the player.
	 */
	public String deploymentReadinessHeadline() {
		if (activeRaid) {
			return BukovMessages.get(
					"bukov.economy.hub.readiness_resume");
		}
		if (!canDeploy) {
			return BukovMessages.get(
					"bukov.economy.hub.readiness_blocked",
					deploymentBlockReason);
		}
		if (!raidMode.usesPlayerLoadout()) {
			return BukovMessages.get(
					"bukov.economy.hub.readiness_training");
		}
		return BukovMessages.get("bukov.economy.hub.readiness_ready");
	}

	public String activeRaidSummary() {
		if (!activeRaid) {
			return "";
		}
		int elapsed = Math.max(0, (int) activeElapsedSeconds);
		int minutes = elapsed / 60;
		int seconds = elapsed % 60;
		return BukovMessages.get(
				"bukov.economy.hub.checkpoint_summary",
				minutes,
				seconds);
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
			return BukovMessages.get("bukov.economy.hub.slot_empty");
		}
		if (slot == LoadoutSlot.AMMUNITION) {
			return BukovMessages.get(
					stacks > 1
							? "bukov.economy.hub.ammo_stacks"
							: "bukov.economy.hub.ammo_single",
					quantity,
					stacks);
		}
		first = compact(first, slot == LoadoutSlot.MEDICAL ? 7 : 10);
		if (stacks == 1) {
			return quantity > 1
					? BukovMessages.get(
							"bukov.economy.hub.slot_quantity",
							first,
							quantity)
					: first;
		}
		return BukovMessages.get(
				"bukov.economy.hub.slot_more",
				first,
				stacks - 1);
	}

	public List<ItemRow> inventoryItems(InventoryFilter filter) {
		return inventoryItems(
				filter,
				InventorySort.STASH_ORDER,
				"");
	}

	public List<ItemRow> inventoryItems(
			InventoryFilter filter,
			InventorySort sort,
			String query) {
		if (filter == null) {
			throw new IllegalArgumentException("filter is required");
		}
		if (sort == null) {
			throw new IllegalArgumentException("sort is required");
		}
		String normalizedQuery = query == null
				? ""
				: query.trim().toLowerCase(Locale.ROOT);
		List<ItemRow> result = new ArrayList<>();
		for (ItemRow row : stashItems) {
			if (filter.matches(row)
					&& matchesQuery(row, normalizedQuery)) {
				result.add(row);
			}
		}
		Comparator<ItemRow> comparator = comparator(sort);
		if (comparator != null) {
			Collections.sort(result, comparator);
		}
		return Collections.unmodifiableList(result);
	}

	private static boolean matchesQuery(
			ItemRow row, String normalizedQuery) {
		if (normalizedQuery.isEmpty()) return true;
		return row.label.toLowerCase(Locale.ROOT)
				.contains(normalizedQuery)
				|| row.definitionId.toLowerCase(Locale.ROOT)
				.contains(normalizedQuery)
				|| row.slot.label.toLowerCase(Locale.ROOT)
				.contains(normalizedQuery)
				|| row.rarity.label.toLowerCase(Locale.ROOT)
				.contains(normalizedQuery);
	}

	private static Comparator<ItemRow> comparator(
			final InventorySort sort) {
		if (sort == InventorySort.STASH_ORDER) {
			return null;
		}
		return new Comparator<ItemRow>() {
			@Override
			public int compare(ItemRow left, ItemRow right) {
				int primary;
				switch (sort) {
					case VALUE_DESC:
						primary = left.value < right.value
								? 1
								: left.value > right.value ? -1 : 0;
						break;
					case WEIGHT_ASC:
						primary = left.weight < right.weight
								? -1
								: left.weight > right.weight ? 1 : 0;
						break;
					case NAME_ASC:
						primary = left.label.compareTo(right.label);
						break;
					default:
						primary = 0;
				}
				return primary != 0
						? primary
						: left.itemUid.compareTo(right.itemUid);
			}
		};
	}

	public String inventoryFilterSummary(InventoryFilter filter) {
		return BukovMessages.get(
				"bukov.economy.hub.filter_summary",
				filter.label,
				inventoryItems(filter).size(),
				stashItems.size());
	}

	private static int comparisonPercent(
			int unitValue, long categoryAverageUnitValue) {
		if (categoryAverageUnitValue <= 0L) return 0;
		double raw = (unitValue - categoryAverageUnitValue)
				* 100d / categoryAverageUnitValue;
		return (int) Math.max(-999d, Math.min(999d, Math.round(raw)));
	}

	/** Shared with the raid backpack so both surfaces read value the same way. */
	static ItemRarity rarityFor(int unitValue) {
		if (unitValue >= 5_000) return ItemRarity.LEGENDARY;
		if (unitValue >= 1_800) return ItemRarity.RARE;
		if (unitValue >= 500) return ItemRarity.UNCOMMON;
		return ItemRarity.COMMON;
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
			return BukovMessages.get("bukov.economy.item.unknown");
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
		return friendly.length() == 0
				? BukovMessages.get("bukov.economy.item.unknown")
				: friendly.toString();
	}

	static String formatWeight(float value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	private static final Map<String, String> KNOWN_NAMES = knownNames();

	private static Map<String, String> knownNames() {
		Map<String, String> values = new LinkedHashMap<>();
		putName(values, "firearm:needle_9", "firearm_needle_9");
		putName(values, "firearm:shuttle_9", "firearm_shuttle_9");
		putName(values, "firearm:ward_556", "firearm_ward_556");
		putName(values, "firearm:mountain_762", "firearm_mountain_762");
		putName(values, "firearm:bolt_12", "firearm_bolt_12");
		putName(values, "firearm:longstreet_762", "firearm_longstreet_762");
		putName(values, "firearm:sentinel_9", "firearm_sentinel_9");
		putName(values, "firearm:sparrow_9", "firearm_sparrow_9");
		putName(values, "firearm:hive_9", "firearm_hive_9");
		putName(values, "firearm:whisper_9", "firearm_whisper_9");
		putName(values, "firearm:jackal_9", "firearm_jackal_9");
		putName(values, "firearm:river_556", "firearm_river_556");
		putName(values, "firearm:foundry_762", "firearm_foundry_762");
		putName(values, "firearm:carbine_556", "firearm_carbine_556");
		putName(values, "firearm:breaker_12", "firearm_breaker_12");
		putName(values, "firearm:rainstorm_12", "firearm_rainstorm_12");
		putName(values, "firearm:watchtower_556", "firearm_watchtower_556");
		putName(values, "firearm:frontier_762", "firearm_frontier_762");
		putName(values, "ammo:ammo_9_training", "ammo_9_training");
		putName(values, "ammo:ammo_9_standard", "ammo_9_standard");
		putName(values, "ammo:ammo_9_subsonic", "ammo_9_subsonic");
		putName(values, "ammo:ammo_556_standard", "ammo_556_standard");
		putName(values, "ammo:ammo_556_armor_piercing",
				"ammo_556_armor_piercing");
		putName(values, "ammo:ammo_762_standard", "ammo_762_standard");
		putName(values, "ammo:ammo_762_expanding", "ammo_762_expanding");
		putName(values, "ammo:ammo_12g_buckshot", "ammo_12g_buckshot");
		putName(values, "bandage", "bandage");
		putName(values, "medical:bandage", "bandage");
		putName(values, "medical:field_medkit", "field_medkit");
		putName(values, "armor:soft_vest", "soft_vest");
		putName(values, "armor:patrol_vest", "patrol_vest");
		putName(values, "armor:ceramic_rig", "ceramic_rig");
		putName(values, "backpack:scout_pack", "scout_pack");
		putName(values, "backpack:field_pack", "field_pack");
		return Collections.unmodifiableMap(values);
	}

	private static void putName(
			Map<String, String> values, String definitionId, String key) {
		values.put(
				definitionId,
				BukovMessages.get("bukov.economy.item." + key));
	}
}
