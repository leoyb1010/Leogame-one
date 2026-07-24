package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;

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
							"找回维修档案",
							"取得维修间档案并成功撤离",
							"rust_workshop",
							1,
							0L),
					new Step(
							SAFE_RETURN,
							"安全返航",
							"累计完成 2 次成功撤离",
							"flooded_passage",
							2,
							0L),
					new Step(
							FIELD_SUPPLIER,
							"前线补给",
							"累计撤离 3 次，并带回价值 3000 的物资",
							"overgrown_yard",
							3,
							3000L),
					new Step(
							WHITE_LINE_HUNT,
							"白线追猎",
							"累计撤离 4 次，并带回价值 8000 的物资",
							"cold_storage",
							4,
							8000L),
					new Step(
							SEALED_LAB_CLEARANCE,
							"封存层许可",
							"累计撤离 5 次，并带回价值 15000 的物资",
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
				next == null ? "全部合同已完成" : next.title,
				next == null ? "封存层已开放，继续自由搜掠" : next.objective,
				next == null ? null : next.mapId);
	}

	public static List<String> allMapIds() {
		return ALL_MAPS;
	}

	public static String mapDisplayName(String mapId) {
		if ("fog_depot".equals(mapId)) return "雾港回收区";
		if ("rust_workshop".equals(mapId)) return "锈蚀工场";
		if ("flooded_passage".equals(mapId)) return "沉水通道";
		if ("overgrown_yard".equals(mapId)) return "荒草货场";
		if ("cold_storage".equals(mapId)) return "冷库环线";
		if ("sealed_lab".equals(mapId)) return "封存实验层";
		return "未知区域";
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
			return "合同 " + completedContracts + "/" + totalContracts
					+ " · 区域 " + unlockedMaps + "/" + totalMaps;
		}
	}
}
