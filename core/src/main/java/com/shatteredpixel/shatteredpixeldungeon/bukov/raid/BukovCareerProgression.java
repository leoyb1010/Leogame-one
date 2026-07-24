package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Durable hideout progression derived from settlement evidence.
 *
 * Contract and map IDs live in the existing profile sets, so old saves gain
 * the campaign without another schema migration. Reconciliation is
 * intentionally idempotent and can safely run after settlement or profile
 * load.
 */
public final class BukovCareerProgression {

	public static final String STARTING_MAP = "fog_depot";
	public static final String SAFE_RETURN = "contract_safe_return";
	public static final String FIELD_SUPPLIER = "contract_field_supplier";
	public static final String WHITE_LINE_HUNT = "contract_white_line_hunt";
	public static final String SEALED_LAB_CLEARANCE =
			"contract_sealed_lab_clearance";

	private static final List<Step> STEPS = Collections.unmodifiableList(
			Arrays.asList(
					new Step(
							FirstRaidMission.EVENT_ID,
							BukovMessages.get(
									"bukov.economy.hub.contract_rust_workshop_title"),
							BukovMessages.get(
									"bukov.economy.hub.contract_rust_workshop_objective"),
							"rust_workshop",
							1,
							0L),
					new Step(
							SAFE_RETURN,
							BukovMessages.get(
									"bukov.economy.hub.contract_flooded_passage_title"),
							BukovMessages.get(
									"bukov.economy.hub.contract_flooded_passage_objective"),
							"flooded_passage",
							2,
							0L),
					new Step(
							FIELD_SUPPLIER,
							BukovMessages.get(
									"bukov.economy.hub.contract_overgrown_yard_title"),
							BukovMessages.get(
									"bukov.economy.hub.contract_overgrown_yard_objective"),
							"overgrown_yard",
							3,
							3000L),
					new Step(
							WHITE_LINE_HUNT,
							BukovMessages.get(
									"bukov.economy.hub.contract_cold_storage_title"),
							BukovMessages.get(
									"bukov.economy.hub.contract_cold_storage_objective"),
							"cold_storage",
							4,
							8000L),
					new Step(
							SEALED_LAB_CLEARANCE,
							BukovMessages.get(
									"bukov.economy.hub.contract_sealed_lab_title"),
							BukovMessages.get(
									"bukov.economy.hub.contract_sealed_lab_objective"),
							"sealed_lab",
							5,
							15000L)));

	private static final List<String> ALL_MAPS = Collections.unmodifiableList(
			Arrays.asList(
					STARTING_MAP,
					"rust_workshop",
					"flooded_passage",
					"overgrown_yard",
					"cold_storage",
					"sealed_lab"));

	private BukovCareerProgression() {
	}

	/**
	 * Adds every milestone already proven by the durable profile.
	 *
	 * The first archive contract is authored by mission settlement. Later
	 * contracts require it and then derive from successful extractions and
	 * extracted value, so deaths and training can never advance the chain.
	 */
	public static boolean reconcile(BukovProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		Set<String> mapsBefore = profile.unlockedMaps();
		Set<String> contractsBefore = profile.completedContracts();

		profile.unlockMap(STARTING_MAP);
		if (!profile.unlockedMaps().contains(profile.selectedMap())) {
			profile.selectMap(STARTING_MAP);
		}
		boolean chainStarted =
				profile.completedContracts().contains(FirstRaidMission.EVENT_ID);
		for (Step step : STEPS) {
			if (step.contractId.equals(FirstRaidMission.EVENT_ID)) {
				if (chainStarted) {
					profile.unlockMap(step.mapId);
				}
				continue;
			}
			if (!chainStarted || !eligible(profile.statistics(), step)) {
				break;
			}
			profile.completeContract(step.contractId);
			profile.unlockMap(step.mapId);
		}
		return !mapsBefore.equals(profile.unlockedMaps())
				|| !contractsBefore.equals(profile.completedContracts());
	}

	public static Snapshot snapshot(BukovProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		int completed = 0;
		Step next = null;
		for (Step step : STEPS) {
			if (profile.completedContracts().contains(step.contractId)) {
				completed++;
			} else {
				next = step;
				break;
			}
		}
		int unlocked = 0;
		for (String mapId : ALL_MAPS) {
			if (profile.unlockedMaps().contains(mapId)) {
				unlocked++;
			}
		}
		return new Snapshot(
				completed,
				STEPS.size(),
				unlocked,
				ALL_MAPS.size(),
				next == null
						? BukovMessages.get(
								"bukov.economy.hub.contract_complete_title")
						: next.title,
				next == null
						? BukovMessages.get(
								"bukov.economy.hub.contract_complete_objective")
						: next.objective,
				next == null ? null : next.mapId);
	}

	public static List<String> allMapIds() {
		return ALL_MAPS;
	}

	public static String mapDisplayName(String mapId) {
		if (ALL_MAPS.contains(mapId)) {
			return BukovMessages.get(
					"bukov.economy.hub.map_" + mapId);
		}
		return BukovMessages.get("bukov.economy.hub.map_unknown");
	}

	public static List<String> availableMapIds(BukovProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		List<String> result = new ArrayList<>();
		for (String mapId : ALL_MAPS) {
			if (profile.unlockedMaps().contains(mapId)) {
				result.add(mapId);
			}
		}
		if (result.isEmpty()) {
			result.add(STARTING_MAP);
		}
		return Collections.unmodifiableList(result);
	}

	private static boolean eligible(BukovStatistics statistics, Step step) {
		return statistics.successfulRaids() >= step.requiredSuccesses
				&& statistics.extractedValue() >= step.requiredExtractedValue;
	}

	private static final class Step {
		private final String contractId;
		private final String title;
		private final String objective;
		private final String mapId;
		private final int requiredSuccesses;
		private final long requiredExtractedValue;

		private Step(
				String contractId,
				String title,
				String objective,
				String mapId,
				int requiredSuccesses,
				long requiredExtractedValue) {
			this.contractId = contractId;
			this.title = title;
			this.objective = objective;
			this.mapId = mapId;
			this.requiredSuccesses = requiredSuccesses;
			this.requiredExtractedValue = requiredExtractedValue;
		}
	}

	public static final class Snapshot {
		public final int completedContracts;
		public final int totalContracts;
		public final int unlockedMaps;
		public final int totalMaps;
		public final String activeContract;
		public final String activeObjective;
		public final String nextMapId;

		private Snapshot(
				int completedContracts,
				int totalContracts,
				int unlockedMaps,
				int totalMaps,
				String activeContract,
				String activeObjective,
				String nextMapId) {
			this.completedContracts = completedContracts;
			this.totalContracts = totalContracts;
			this.unlockedMaps = unlockedMaps;
			this.totalMaps = totalMaps;
			this.activeContract = activeContract;
			this.activeObjective = activeObjective;
			this.nextMapId = nextMapId;
		}

		public String careerSummary() {
			return BukovMessages.get(
					"bukov.economy.hub.career_summary",
					completedContracts,
					totalContracts,
					unlockedMaps,
					totalMaps);
		}
	}
}
