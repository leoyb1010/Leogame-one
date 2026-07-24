package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Settlement-driven, replay-safe progress ledger for long-term contracts. */
public final class BukovLongTermContractLedger implements Bundlable {

	private static final String PROGRESS = "progress";
	private static final String APPLIED_RAIDS = "applied_raids";

	private final Map<String, BukovContractProgress> progress =
			new LinkedHashMap<>();
	private final Set<String> appliedRaidIds = new LinkedHashSet<>();

	public BukovLongTermContractLedger() {
		for (BukovLongTermContractDefinition definition
				: BukovLongTermContractCatalog.all()) {
			progress.put(definition.id, new BukovContractProgress(definition.id));
		}
	}

	/**
	 * Applies a formal settlement once. Training never calls this method.
	 * Kills and completed raids count on death; extracted metrics require success.
	 */
	boolean recordSettlement(
			String raidId,
			RaidOutcome outcome,
			long extractedValue,
			int kills) {
		if (raidId == null || raidId.trim().isEmpty() || outcome == null) {
			throw new IllegalArgumentException("raidId and outcome are required");
		}
		if (extractedValue < 0L || kills < 0) {
			throw new IllegalArgumentException(
					"settlement contract metrics cannot be negative");
		}
		if (!appliedRaidIds.add(raidId)) return false;
		for (BukovLongTermContractDefinition definition
				: BukovLongTermContractCatalog.all()) {
			long delta;
			switch (definition.metric) {
				case SUCCESSFUL_EXTRACTIONS:
					delta = outcome == RaidOutcome.SUCCESS ? 1L : 0L;
					break;
				case EXTRACTED_VALUE:
					delta = outcome == RaidOutcome.SUCCESS ? extractedValue : 0L;
					break;
				case KILLS:
					delta = kills;
					break;
				case RAIDS_COMPLETED:
					delta = 1L;
					break;
				default:
					throw new IllegalStateException(
							"Unhandled contract metric: " + definition.metric);
			}
			progress.get(definition.id).add(delta);
		}
		return true;
	}

	public BukovContractProgress progress(String contractId) {
		BukovContractProgress value = progress.get(contractId);
		if (value == null) {
			throw new IllegalArgumentException(
					"Unknown long-term contract: " + contractId);
		}
		return value.copy();
	}

	public List<BukovContractProgress> allProgress() {
		List<BukovContractProgress> result = new ArrayList<>();
		for (BukovContractProgress value : progress.values()) {
			result.add(value.copy());
		}
		return Collections.unmodifiableList(result);
	}

	long claim(String contractId) {
		BukovContractProgress value = progress.get(contractId);
		if (value == null) {
			throw new IllegalArgumentException(
					"Unknown long-term contract: " + contractId);
		}
		return value.claim();
	}

	BukovLongTermContractLedger copy() {
		BukovLongTermContractLedger result =
				new BukovLongTermContractLedger();
		result.progress.clear();
		for (Map.Entry<String, BukovContractProgress> entry
				: progress.entrySet()) {
			result.progress.put(entry.getKey(), entry.getValue().copy());
		}
		result.appliedRaidIds.addAll(appliedRaidIds);
		return result;
	}

	void replaceWith(BukovLongTermContractLedger replacement) {
		progress.clear();
		for (Map.Entry<String, BukovContractProgress> entry
				: replacement.progress.entrySet()) {
			progress.put(entry.getKey(), entry.getValue().copy());
		}
		appliedRaidIds.clear();
		appliedRaidIds.addAll(replacement.appliedRaidIds);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(PROGRESS, progress.values());
		bundle.put(APPLIED_RAIDS, appliedRaidIds.toArray(new String[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		Map<String, BukovContractProgress> restored = new LinkedHashMap<>();
		Collection<Bundlable> stored = bundle.getCollection(PROGRESS);
		for (Bundlable entry : stored) {
			if (!(entry instanceof BukovContractProgress)) {
				throw new IllegalStateException(
						"Unexpected long-term contract progress entry");
			}
			BukovContractProgress value = (BukovContractProgress) entry;
			if (restored.put(value.contractId(), value.copy()) != null) {
				throw new IllegalStateException(
						"Duplicate long-term contract progress: "
								+ value.contractId());
			}
		}
		for (BukovLongTermContractDefinition definition
				: BukovLongTermContractCatalog.all()) {
			if (!restored.containsKey(definition.id)) {
				restored.put(
						definition.id,
						new BukovContractProgress(definition.id));
			}
		}
		progress.clear();
		progress.putAll(restored);
		appliedRaidIds.clear();
		for (String raidId : bundle.getStringArray(APPLIED_RAIDS)) {
			if (raidId == null || raidId.trim().isEmpty()
					|| !appliedRaidIds.add(raidId)) {
				throw new IllegalStateException(
						"Invalid applied contract raid ID");
			}
		}
	}
}
