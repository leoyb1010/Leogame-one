package com.shatteredpixel.shatteredpixeldungeon.bukov.mission;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

/** Shared identifiers and objective copy for the first-raid archive gate. */
public final class FirstRaidMission {

	public enum Stage {
		RECOVER_ARCHIVE,
		SECURE_HIGH_VALUE_CACHE,
		EXTRACT
	}

	public static final String ARCHIVE_ANCHOR_ID = "Q01";
	public static final String GATE_ID = "G01";
	public static final String CONDITIONAL_EXTRACTION_ID = "E02";
	public static final String ARCHIVE_CONTAINER_ID = "Q01_ARCHIVE";
	public static final String ARCHIVE_LOOT_TABLE_ID = "mission_archive";
	public static final String HIGH_VALUE_LOOT_TABLE_ID = "high_value";
	public static final String ARCHIVE_DEFINITION_ID =
			"maintenance_access_archive";
	public static final String EVENT_ID = "maintenance_archive_recovered";
	public static final String HIGH_VALUE_EVENT_ID =
			"first_raid_high_value_cache_searched";
	public static final String LOCKED_OBJECTIVE =
			"主线 1/3：搜索维修间，取得通道档案";
	public static final String HIGH_VALUE_OBJECTIVE =
			"主线 2/3：通道已开放，搜查高价值仓";
	public static final String UNLOCKED_OBJECTIVE =
			"主线 3/3：物资已确认，前往撤离点";

	private FirstRaidMission() {
	}

	public static boolean isArchive(RaidItem item) {
		return item != null
				&& ARCHIVE_DEFINITION_ID.equals(item.definitionId());
	}

	public static Stage stage(
			boolean archiveRecovered,
			boolean highValueCacheSearched) {
		if (!archiveRecovered) return Stage.RECOVER_ARCHIVE;
		return highValueCacheSearched
				? Stage.EXTRACT
				: Stage.SECURE_HIGH_VALUE_CACHE;
	}

	public static String objective(Stage stage) {
		if (stage == null) {
			throw new IllegalArgumentException("stage is required");
		}
		switch (stage) {
			case RECOVER_ARCHIVE:
				return LOCKED_OBJECTIVE;
			case SECURE_HIGH_VALUE_CACHE:
				return HIGH_VALUE_OBJECTIVE;
			case EXTRACT:
				return UNLOCKED_OBJECTIVE;
			default:
				throw new IllegalStateException(
						"Unsupported first-raid stage: " + stage);
		}
	}
}
