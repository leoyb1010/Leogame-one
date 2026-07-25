/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Production projection from one generated Bukov host level to its resumable
 * raid definitions. GameScene and headless player-path acceptance tests use
 * this same boundary so objective and extraction topology cannot drift.
 */
public final class BukovRaidWorldDefinitions {

	public static List<ExtractionState> extractions(BukovLevel level) {
		if (level == null
				|| level.raidLayout() == null
				|| level.raidLayout().extractions.isEmpty()) {
			return Collections.singletonList(ExtractionState.basic());
		}
		return BukovExtractionStates.fromLayout(level.raidLayout());
	}

	public static List<BukovContainerDefinition> containers(
			BukovLevel level) {
		if (level == null) {
			return Collections.emptyList();
		}
		boolean missionEnabled = !level.raidMode().trainingGround();
		List<BukovContainerDefinition> result = new ArrayList<>();
		for (BukovRaidLayout.LootAnchor anchor : level.lootAnchors()) {
			if (anchor.cell < 0) continue;
			if (!missionEnabled
					&& FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							anchor.lootTableId)) {
				continue;
			}
			result.add(new BukovContainerDefinition(
					anchor.id,
					anchor.cell,
					anchor.lootTableId,
					FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							anchor.lootTableId) ? 3 : 2,
					anchor.searchSeconds,
					false));
		}

		int maintenanceCell = level.semanticCell("scrap_compactor");
		if (maintenanceCell < 0) {
			throw new IllegalStateException(
					"Bukov raid is missing the optional maintenance cache anchor");
		}
		result.add(new BukovContainerDefinition(
				BukovFirstRaidLootTables.MAINTENANCE_CACHE_CONTAINER_ID,
				maintenanceCell,
				BukovFirstRaidLootTables.MAINTENANCE_CACHE,
				3,
				3.2f,
				true));
		if (!missionEnabled) {
			return result;
		}

		BukovRaidLayout.MissionGate missionGate = level.missionGate();
		if (missionGate == null || missionGate.archiveCell < 0) {
			throw new IllegalStateException(
					"Bukov first-raid layout is missing Q01 archive anchor");
		}
		result.add(new BukovContainerDefinition(
				FirstRaidMission.ARCHIVE_CONTAINER_ID,
				missionGate.archiveCell,
				FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
				1,
				1.4f,
				false));
		return result;
	}

	private BukovRaidWorldDefinitions() {
	}
}
