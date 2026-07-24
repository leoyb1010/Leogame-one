package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bukov-only profile root. This must be saved independently from the original
 * adventure save.
 */
public final class BukovProfile implements Bundlable {

	public static final int CURRENT_VERSION = 7;

	private static final String PROFILE_VERSION = "profile_version";
	private static final String CURRENCY = "currency";
	private static final String STASH = "stash";
	private static final String LOADOUT = "loadout";
	private static final String UNLOCKED_MAPS = "unlocked_maps";
	private static final String COMPLETED_CONTRACTS = "completed_contracts";
	private static final String STATISTICS = "statistics";
	private static final String SETTLEMENTS = "settlements";
	private static final String LAST_LOADOUT_DEFINITIONS = "last_loadout_definitions";
	private static final String SELECTED_RAID_MODE = "selected_raid_mode";
	private static final String RAIDS_STARTED = "raids_started";
	private static final String SEEN_TUTORIAL_EVENTS = "seen_tutorial_events";
	private static final String ECONOMY_RECEIPTS = "economy_receipts";
	private static final String SELECTED_MAP = "selected_map";
	private static final String INSURANCE = "insurance";
	private static final String LONG_TERM_CONTRACTS = "long_term_contracts";
	private static final String FIREARM_BUILDS = "firearm_builds";

	private int profileVersion = CURRENT_VERSION;
	private long currency;
	private final BukovStash stash = new BukovStash();
	private final BukovLoadout loadout = new BukovLoadout();
	private final Set<String> unlockedMaps = new LinkedHashSet<>();
	private final Set<String> completedContracts = new LinkedHashSet<>();
	private final BukovStatistics statistics = new BukovStatistics();
	private final Map<String, SettlementReceipt> settlementsByRaid = new LinkedHashMap<>();
	private final List<String> lastLoadoutDefinitions = new ArrayList<>();
	private BukovRaidMode selectedRaidMode = BukovRaidMode.EXPEDITION;
	private int raidsStarted;
	private final Set<BukovTutorialEvent> seenTutorialEvents =
			new LinkedHashSet<>();
	private final Map<String, BukovEconomyReceipt> economyReceipts =
			new LinkedHashMap<>();
	private String selectedMap = BukovCareerProgression.STARTING_MAP;
	private final BukovInsuranceLedger insurance =
			new BukovInsuranceLedger();
	private final BukovLongTermContractLedger longTermContracts =
			new BukovLongTermContractLedger();
	private final BukovFirearmBuilds firearmBuilds =
			new BukovFirearmBuilds();

	public BukovProfile() {
	}

	public int profileVersion() {
		return profileVersion;
	}

	public long currency() {
		return currency;
	}

	public void setCurrency(long currency) {
		if (currency < 0L) {
			throw new IllegalArgumentException("currency must be non-negative");
		}
		this.currency = currency;
	}

	public BukovStash stash() {
		return stash;
	}

	public BukovLoadout loadout() {
		return loadout;
	}

	public BukovStatistics statistics() {
		return statistics;
	}

	public BukovInsuranceLedger insurance() {
		return insurance;
	}

	public BukovLongTermContractLedger longTermContracts() {
		return longTermContracts;
	}

	public BukovFirearmBuilds firearmBuilds() {
		return firearmBuilds;
	}

	public BukovRaidMode selectedRaidMode() {
		return selectedRaidMode;
	}

	public void selectRaidMode(BukovRaidMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("mode is required");
		}
		selectedRaidMode = mode;
	}

	public int raidsStarted() {
		return raidsStarted;
	}

	int beginRaid() {
		if (raidsStarted == Integer.MAX_VALUE) {
			throw new IllegalStateException("Raid counter exhausted");
		}
		return ++raidsStarted;
	}

	boolean ensureRaidStarted(int raidOrdinal) {
		if (raidOrdinal <= 0) {
			throw new IllegalArgumentException("raidOrdinal must be positive");
		}
		if (raidsStarted >= raidOrdinal) {
			return false;
		}
		raidsStarted = raidOrdinal;
		return true;
	}

	public boolean tutorialSeen(BukovTutorialEvent event) {
		return event != null && seenTutorialEvents.contains(event);
	}

	boolean markTutorialSeen(BukovTutorialEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("event is required");
		}
		return seenTutorialEvents.add(event);
	}

	public Set<BukovTutorialEvent> seenTutorialEvents() {
		return Collections.unmodifiableSet(
				new LinkedHashSet<>(seenTutorialEvents));
	}

	BukovEconomyReceipt economyReceipt(String transactionId) {
		BukovEconomyReceipt receipt = economyReceipts.get(transactionId);
		return receipt == null ? null : receipt.copy();
	}

	void recordEconomyReceipt(BukovEconomyReceipt receipt) {
		if (receipt == null) {
			throw new IllegalArgumentException("receipt is required");
		}
		if (economyReceipts.containsKey(receipt.transactionId())) {
			throw new IllegalStateException(
					"Economy transaction already committed: "
							+ receipt.transactionId());
		}
		economyReceipts.put(receipt.transactionId(), receipt.copy());
	}

	public void unlockMap(String mapId) {
		unlockedMaps.add(requireId(mapId, "mapId"));
	}

	public void completeContract(String contractId) {
		completedContracts.add(requireId(contractId, "contractId"));
	}

	public Set<String> unlockedMaps() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(unlockedMaps));
	}

	public Set<String> completedContracts() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(completedContracts));
	}

	public String selectedMap() {
		return selectedMap;
	}

	public void selectMap(String mapId) {
		String selected = requireId(mapId, "mapId");
		if (!unlockedMaps.contains(selected)) {
			throw new IllegalArgumentException("Map is not unlocked: " + selected);
		}
		selectedMap = selected;
	}

	public boolean isSettled(String raidId) {
		return settlementsByRaid.containsKey(raidId);
	}

	public SettlementReceipt settlement(String raidId) {
		SettlementReceipt receipt = settlementsByRaid.get(raidId);
		return receipt == null ? null : receipt.copy();
	}

	public List<SettlementReceipt> settlements() {
		List<SettlementReceipt> result = new ArrayList<>();
		for (SettlementReceipt receipt : settlementsByRaid.values()) {
			result.add(receipt.copy());
		}
		return Collections.unmodifiableList(result);
	}

	public List<String> lastLoadoutDefinitions() {
		return Collections.unmodifiableList(new ArrayList<>(lastLoadoutDefinitions));
	}

	void rememberCurrentLoadout() {
		List<String> definitions = new ArrayList<>();
		for (RaidItem item : loadout.items(stash)) {
			definitions.add(item.definitionId());
		}
		rememberLoadoutDefinitions(definitions);
	}

	void rememberLoadoutDefinitions(Collection<String> definitions) {
		if (definitions == null) {
			throw new IllegalArgumentException("definitions are required");
		}
		List<String> validated = new ArrayList<>();
		for (String definition : definitions) {
			String validatedDefinition =
					requireId(definition, "lastLoadoutDefinition");
			if (BukovLoadout.deployableDefinition(validatedDefinition)) {
				validated.add(validatedDefinition);
			}
		}
		lastLoadoutDefinitions.clear();
		lastLoadoutDefinitions.addAll(validated);
	}

	/** Detached snapshot for atomic application-layer mutations. */
	public BukovProfile copy() {
		BukovProfile result = new BukovProfile();
		result.profileVersion = profileVersion;
		result.currency = currency;
		result.stash.replaceWith(stash.copy());
		result.loadout.replaceWith(loadout.copy());
		result.unlockedMaps.addAll(unlockedMaps);
		result.completedContracts.addAll(completedContracts);
		result.statistics.replaceWith(statistics.copy());
		result.lastLoadoutDefinitions.addAll(lastLoadoutDefinitions);
		result.selectedRaidMode = selectedRaidMode;
		result.raidsStarted = raidsStarted;
		result.selectedMap = selectedMap;
		result.insurance.replaceWith(insurance.copy());
		result.longTermContracts.replaceWith(longTermContracts.copy());
		result.firearmBuilds.replaceWith(firearmBuilds.copy());
		result.seenTutorialEvents.addAll(seenTutorialEvents);
		for (BukovEconomyReceipt receipt : economyReceipts.values()) {
			result.economyReceipts.put(
					receipt.transactionId(),
					receipt.copy());
		}
		for (SettlementReceipt receipt : settlementsByRaid.values()) {
			result.settlementsByRaid.put(receipt.raidId(), receipt.copy());
		}
		return result;
	}

	void recordSettlement(SettlementReceipt receipt) {
		if (settlementsByRaid.containsKey(receipt.raidId())) {
			throw new IllegalStateException("Raid already settled: " + receipt.raidId());
		}
		settlementsByRaid.put(receipt.raidId(), receipt.copy());
	}

	void replaceWith(BukovProfile replacement) {
		profileVersion = replacement.profileVersion;
		currency = replacement.currency;
		stash.replaceWith(replacement.stash);
		loadout.replaceWith(replacement.loadout);
		unlockedMaps.clear();
		unlockedMaps.addAll(replacement.unlockedMaps);
		completedContracts.clear();
		completedContracts.addAll(replacement.completedContracts);
		statistics.replaceWith(replacement.statistics);
		lastLoadoutDefinitions.clear();
		lastLoadoutDefinitions.addAll(replacement.lastLoadoutDefinitions);
		selectedRaidMode = replacement.selectedRaidMode;
		raidsStarted = replacement.raidsStarted;
		selectedMap = replacement.selectedMap;
		insurance.replaceWith(replacement.insurance);
		longTermContracts.replaceWith(replacement.longTermContracts);
		firearmBuilds.replaceWith(replacement.firearmBuilds);
		seenTutorialEvents.clear();
		seenTutorialEvents.addAll(replacement.seenTutorialEvents);
		economyReceipts.clear();
		for (BukovEconomyReceipt receipt
				: replacement.economyReceipts.values()) {
			economyReceipts.put(receipt.transactionId(), receipt.copy());
		}
		settlementsByRaid.clear();
		for (SettlementReceipt receipt : replacement.settlementsByRaid.values()) {
			settlementsByRaid.put(receipt.raidId(), receipt.copy());
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(PROFILE_VERSION, profileVersion);
		bundle.put(CURRENCY, currency);
		bundle.put(STASH, stash);
		bundle.put(LOADOUT, loadout);
		bundle.put(UNLOCKED_MAPS, unlockedMaps.toArray(new String[0]));
		bundle.put(COMPLETED_CONTRACTS, completedContracts.toArray(new String[0]));
		bundle.put(STATISTICS, statistics);
		bundle.put(SETTLEMENTS, settlementsByRaid.values());
		bundle.put(
				LAST_LOADOUT_DEFINITIONS,
				lastLoadoutDefinitions.toArray(new String[0]));
		bundle.put(SELECTED_RAID_MODE, selectedRaidMode);
		bundle.put(RAIDS_STARTED, raidsStarted);
		String[] tutorialEvents = new String[seenTutorialEvents.size()];
		int tutorialIndex = 0;
		for (BukovTutorialEvent event : seenTutorialEvents) {
			tutorialEvents[tutorialIndex++] = event.name();
		}
		bundle.put(SEEN_TUTORIAL_EVENTS, tutorialEvents);
		bundle.put(ECONOMY_RECEIPTS, economyReceipts.values());
		bundle.put(SELECTED_MAP, selectedMap);
		bundle.put(INSURANCE, insurance);
		bundle.put(LONG_TERM_CONTRACTS, longTermContracts);
		bundle.put(FIREARM_BUILDS, firearmBuilds);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		int restoredVersion = bundle.getInt(PROFILE_VERSION);
		if (restoredVersion <= 0 || restoredVersion > CURRENT_VERSION) {
			throw new IllegalStateException("Unsupported Bukov profile version: " + restoredVersion);
		}
		long restoredCurrency = bundle.getLong(CURRENCY);
		if (restoredCurrency < 0L) {
			throw new IllegalStateException("Profile currency cannot be negative");
		}
		Bundlable restoredStash = bundle.get(STASH);
		Bundlable restoredStatistics = bundle.get(STATISTICS);
		if (!(restoredStash instanceof BukovStash)
				|| !(restoredStatistics instanceof BukovStatistics)) {
			throw new IllegalStateException("Incomplete Bukov profile");
		}

		BukovProfile restored = new BukovProfile();
		restored.profileVersion = restoredVersion;
		restored.currency = restoredCurrency;
		restored.stash.replaceWith((BukovStash) restoredStash);
		if (restoredVersion >= 2) {
			Bundlable restoredLoadout = bundle.get(LOADOUT);
			if (!(restoredLoadout instanceof BukovLoadout)) {
				throw new IllegalStateException("Incomplete Bukov profile loadout");
			}
			restored.loadout.replaceWith((BukovLoadout) restoredLoadout);
			restored.loadout.pruneMissing(restored.stash);
		}
		for (String mapId : bundle.getStringArray(UNLOCKED_MAPS)) {
			restored.unlockMap(mapId);
		}
		for (String contractId : bundle.getStringArray(COMPLETED_CONTRACTS)) {
			restored.completeContract(contractId);
		}
		restored.statistics.replaceWith((BukovStatistics) restoredStatistics);
		if (restoredVersion >= 3) {
			for (String definitionId
					: bundle.getStringArray(LAST_LOADOUT_DEFINITIONS)) {
				String validatedDefinition =
						requireId(definitionId, "lastLoadoutDefinition");
				if (BukovLoadout.deployableDefinition(
						validatedDefinition)) {
					restored.lastLoadoutDefinitions.add(
							validatedDefinition);
				}
			}
		}
		if (restoredVersion >= 4) {
			BukovRaidMode mode =
					bundle.getEnum(SELECTED_RAID_MODE, BukovRaidMode.class);
			restored.selectedRaidMode =
					mode == null ? BukovRaidMode.EXPEDITION : mode;
			restored.raidsStarted = bundle.getInt(RAIDS_STARTED);
			if (restored.raidsStarted < 0) {
				throw new IllegalStateException(
						"Profile raid count cannot be negative");
			}
			for (String eventName :
					bundle.getStringArray(SEEN_TUTORIAL_EVENTS)) {
				try {
					restored.seenTutorialEvents.add(
							BukovTutorialEvent.valueOf(eventName));
				} catch (IllegalArgumentException invalid) {
					throw new IllegalStateException(
							"Unknown tutorial event: " + eventName,
							invalid);
				}
			}
		}
		if (restoredVersion >= 5) {
			for (Bundlable stored
					: bundle.getCollection(ECONOMY_RECEIPTS)) {
				if (!(stored instanceof BukovEconomyReceipt)) {
					throw new IllegalStateException(
							"Unexpected economy receipt entry");
				}
				restored.recordEconomyReceipt(
						(BukovEconomyReceipt) stored);
			}
		}
		if (restoredVersion >= 6) {
			String restoredMap = bundle.getString(SELECTED_MAP);
			if (restoredMap != null
					&& restored.unlockedMaps.contains(restoredMap)) {
				restored.selectedMap = restoredMap;
			}
		}
		if (restoredVersion >= 7) {
			Bundlable restoredInsurance = bundle.get(INSURANCE);
			Bundlable restoredContracts = bundle.get(LONG_TERM_CONTRACTS);
			Bundlable restoredFirearmBuilds = bundle.get(FIREARM_BUILDS);
			if (!(restoredInsurance instanceof BukovInsuranceLedger)
					|| !(restoredContracts
							instanceof BukovLongTermContractLedger)
					|| !(restoredFirearmBuilds
							instanceof BukovFirearmBuilds)) {
				throw new IllegalStateException(
						"Incomplete Bukov long-term progression profile");
			}
			restored.insurance.replaceWith(
					(BukovInsuranceLedger) restoredInsurance);
			restored.longTermContracts.replaceWith(
					(BukovLongTermContractLedger) restoredContracts);
			restored.firearmBuilds.replaceWith(
					(BukovFirearmBuilds) restoredFirearmBuilds);
		}
		Collection<Bundlable> storedSettlements = bundle.getCollection(SETTLEMENTS);
		for (Bundlable stored : storedSettlements) {
			if (!(stored instanceof SettlementReceipt)) {
				throw new IllegalStateException("Unexpected settlement entry");
			}
			restored.recordSettlement((SettlementReceipt) stored);
		}
		// v1 had no loadout; v1-v2 had no remembered deployment template;
		// v1-v3 default to expedition and have no tutorial ledger.
		// v1-v4 have no durable vendor transaction receipts; v1-v5 select
		// the starting region until the player explicitly changes it.
		// v1-v6 safely begin with empty insurance/build ledgers and fresh
		// long-term contracts; existing completed career contracts are kept.
		restored.profileVersion = CURRENT_VERSION;
		replaceWith(restored);
	}

	private static String requireId(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}
}
