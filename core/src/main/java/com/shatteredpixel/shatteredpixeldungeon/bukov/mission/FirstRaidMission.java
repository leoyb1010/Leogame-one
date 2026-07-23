package com.shatteredpixel.shatteredpixeldungeon.bukov.mission;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

/** Shared identifiers and objective copy for the first-raid archive gate. */
public final class FirstRaidMission {

	public static final String ARCHIVE_ANCHOR_ID = "Q01";
	public static final String GATE_ID = "G01";
	public static final String ARCHIVE_CONTAINER_ID = "Q01_ARCHIVE";
	public static final String ARCHIVE_LOOT_TABLE_ID = "mission_archive";
	public static final String ARCHIVE_DEFINITION_ID =
			"maintenance_access_archive";
	public static final String EVENT_ID = "maintenance_archive_recovered";
	public static final String LOCKED_OBJECTIVE = "主线：搜索南侧维修间档案";
	public static final String UNLOCKED_OBJECTIVE = "主线：通道已开放，前往泵站";

	private FirstRaidMission() {
	}

	public static boolean isArchive(RaidItem item) {
		return item != null
				&& ARCHIVE_DEFINITION_ID.equals(item.definitionId());
	}
}
