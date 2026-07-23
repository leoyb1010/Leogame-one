package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementItemSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable renderer-independent data for the dedicated raid result window. */
public final class BukovSettlementViewModel {

	public static final class ItemRow {
		public final String itemUid;
		public final String name;
		public final int quantity;
		public final long value;
		public final boolean legacy;

		private ItemRow(SettlementItemSnapshot snapshot) {
			itemUid = snapshot.itemUid();
			name = displayName(snapshot.definitionId());
			quantity = snapshot.quantity();
			value = snapshot.totalValue();
			legacy = false;
		}

		private ItemRow(String legacyUid) {
			itemUid = legacyUid;
			name = "历史行动物资";
			quantity = 0;
			value = 0L;
			legacy = true;
		}

		public String summary() {
			return legacy
					? name + " · 旧记录未保存明细"
					: name + " ×" + quantity + "    价值 " + value;
		}
	}

	public final RaidOutcome outcome;
	public final String headline;
	public final String manifestTitle;
	public final String duration;
	public final int kills;
	public final long quantity;
	public final long value;
	public final List<ItemRow> items;
	public final boolean legacyDetails;
	public final boolean missionCompleted;

	private BukovSettlementViewModel(
			RaidOutcome outcome,
			String headline,
			String manifestTitle,
			String duration,
			int kills,
			long quantity,
			long value,
			List<ItemRow> items,
			boolean legacyDetails,
			boolean missionCompleted) {
		this.outcome = outcome;
		this.headline = headline;
		this.manifestTitle = manifestTitle;
		this.duration = duration;
		this.kills = kills;
		this.quantity = quantity;
		this.value = value;
		this.items = Collections.unmodifiableList(items);
		this.legacyDetails = legacyDetails;
		this.missionCompleted = missionCompleted;
	}

	/**
	 * @param elapsedSeconds snapshot this before calling settleSuccess/death
	 * @param kills real-time world kill count, or zero when no combat occurred
	 */
	public static BukovSettlementViewModel from(
			RaidResult result,
			float elapsedSeconds,
			int kills) {
		if (result == null) {
			throw new IllegalArgumentException("result is required");
		}
		if (!BukovNumbers.isFinite(elapsedSeconds) || elapsedSeconds < 0f) {
			throw new IllegalArgumentException(
					"elapsedSeconds must be finite and non-negative");
		}
		if (kills < 0) {
			throw new IllegalArgumentException("kills must be non-negative");
		}

		boolean success = result.outcome() == RaidOutcome.SUCCESS;
		float durableElapsed = result.debriefAvailable()
				? result.elapsedSeconds()
				: elapsedSeconds;
		int durableKills = result.debriefAvailable()
				? result.kills()
				: kills;
		List<SettlementItemSnapshot> snapshots = success
				? result.transferredItems()
				: result.lostItems();
		List<String> legacyUids = success
				? result.transferredUids()
				: result.lostUids();
		List<ItemRow> rows = new ArrayList<>();
		for (SettlementItemSnapshot snapshot : snapshots) {
			rows.add(new ItemRow(snapshot));
		}
		boolean legacy = rows.isEmpty() && !legacyUids.isEmpty();
		if (legacy) {
			for (String uid : legacyUids) {
				rows.add(new ItemRow(uid));
			}
		}
		return new BukovSettlementViewModel(
				result.outcome(),
				success ? "已撤离" : "未归还",
				success ? "安全带回" : "行动损失",
				BukovHudFormat.clock(durableElapsed),
				durableKills,
				success
						? result.transferredQuantity()
						: result.lostQuantity(),
				success ? result.transferredValue() : result.lostValue(),
				rows,
				legacy,
				result.debriefAvailable() && result.missionCompleted());
	}

	public String totals() {
		return (outcome == RaidOutcome.SUCCESS ? "带回 " : "损失 ")
				+ quantity + " 件    本局收益 "
				+ (outcome == RaidOutcome.SUCCESS ? "+" : "-")
				+ value;
	}

	public String stats() {
		return "行动时长 " + duration + "    击杀 " + kills;
	}

	public String mission() {
		if (!missionCompleted) {
			return "任务：维修档案未完成";
		}
		return outcome == RaidOutcome.SUCCESS
				? "任务：维修档案已带回"
				: "任务：维修档案未带回";
	}

	public String earnings() {
		return "本局收益 "
				+ (outcome == RaidOutcome.SUCCESS ? "+" : "-")
				+ value;
	}

	public String emptyManifest() {
		return outcome == RaidOutcome.SUCCESS
				? "本次撤离未携带物资"
				: "本次行动没有可损失物资";
	}

	private static String displayName(String definitionId) {
		String normalized = definitionId == null ? "" : definitionId;
		Item authored = BukovFirstRaidLootTables
				.createByEconomicDefinitionId(normalized);
		if (authored != null && !(normalized.startsWith("ammo:"))) {
			return authored.name();
		}
		String known = KNOWN_NAMES.get(normalized);
		if (known != null) {
			return known;
		}
		if (normalized.equals(
				com.shatteredpixel.shatteredpixeldungeon.bukov.mission
						.FirstRaidMission.ARCHIVE_DEFINITION_ID)) {
			return new BukovMissionArchive().name();
		}
		int separator = normalized.indexOf(':');
		String readable = separator >= 0
				? normalized.substring(separator + 1)
				: normalized;
		readable = readable.replace('_', ' ').trim();
		return readable.isEmpty() ? "未知物资" : readable;
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
		values.put("firearm:sentinel_9", "哨兵-9");
		values.put("firearm:sparrow_9", "雀翎-9");
		values.put("firearm:hive_9", "蜂巢-9");
		values.put("firearm:whisper_9", "低语-9");
		values.put("firearm:jackal_9", "胡狼-9");
		values.put("firearm:river_556", "河谷-556");
		values.put("firearm:foundry_762", "铸炉-762");
		values.put("firearm:carbine_556", "岗哨-556");
		values.put("firearm:breaker_12", "破门-12");
		values.put("firearm:rainstorm_12", "暴雨-12");
		values.put("firearm:watchtower_556", "瞭望-556");
		values.put("firearm:frontier_762", "边界-762");
		values.put("ammo:ammo_9_training", "9毫米训练弹");
		values.put("ammo:ammo_9_standard", "9毫米标准弹");
		values.put("ammo:ammo_9_subsonic", "9毫米亚音速弹");
		values.put("ammo:ammo_556_standard", "5.56毫米标准弹");
		values.put("ammo:ammo_556_armor_piercing", "5.56毫米硬芯弹");
		values.put("ammo:ammo_762_standard", "7.62毫米标准弹");
		values.put("ammo:ammo_762_expanding", "7.62毫米扩张弹");
		values.put("ammo:ammo_12g_buckshot", "12号鹿弹");
		return Collections.unmodifiableMap(values);
	}
}
