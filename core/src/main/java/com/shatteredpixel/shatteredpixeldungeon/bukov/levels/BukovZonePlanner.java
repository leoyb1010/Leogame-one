/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deterministic first-raid semantic planner.
 *
 * The generated graph mirrors the FigureEightBuilder contract: an outer loop
 * supplies two long routes while three spokes provide a shorter high-risk
 * route through a central landmark. A later RegularLevel adapter can map these
 * marks onto generated Room rectangles without changing validation.
 */
public final class BukovZonePlanner {

	private static final int[][] RING_COORDINATES = {
			{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {6, 0},
			{6, 1}, {6, 2}, {6, 3}, {6, 4}, {6, 5}, {6, 6},
			{5, 6}, {4, 6}, {3, 6}, {2, 6}, {1, 6}, {0, 6},
			{0, 5}, {0, 4}, {0, 3}, {0, 2}, {0, 1}
	};

	private static final BukovRaidLayout.Zone[] RING_ZONES = {
			BukovRaidLayout.Zone.SPAWN,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.COMBAT,
			BukovRaidLayout.Zone.MEDICAL,
			BukovRaidLayout.Zone.EXTRACTION,
			BukovRaidLayout.Zone.HAZARD,
			BukovRaidLayout.Zone.COMBAT,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.SPAWN,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.COMBAT,
			BukovRaidLayout.Zone.TRANSIT,
			BukovRaidLayout.Zone.EXTRACTION,
			BukovRaidLayout.Zone.TRANSIT,
			BukovRaidLayout.Zone.COMBAT,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.SPAWN,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.LOW_LOOT,
			BukovRaidLayout.Zone.MEDICAL,
			BukovRaidLayout.Zone.EXTRACTION,
			BukovRaidLayout.Zone.HAZARD,
			BukovRaidLayout.Zone.TRANSIT,
			BukovRaidLayout.Zone.LOW_LOOT
	};

	private BukovZonePlanner() {
	}

	public static BukovRaidLayout generateFirstRaid(long seed) {
		return generateFirstRaid(seed, "fog_depot");
	}

	/**
	 * Runs the same proven topology and extraction rules for every theme.
	 * Theme-specific content weights are consumed by the runtime planners;
	 * structural reliability never depends on an art/content preset.
	 */
	public static BukovRaidLayout generateFirstRaid(
			long seed, ThemeDefinition theme) {
		if (theme == null) {
			throw new IllegalArgumentException("theme is required");
		}
		return generateFirstRaid(seed, theme.id, theme);
	}

	private static BukovRaidLayout generateFirstRaid(long seed, String themeId) {
		return generateFirstRaid(seed, themeId, null);
	}

	private static BukovRaidLayout generateFirstRaid(
			long seed, String themeId, ThemeDefinition theme) {
		Random random = new Random(seed);
		BukovRaidLayout layout = new BukovRaidLayout();
		layout.seed = seed;
		layout.themeId = themeId;

		int offsetX = random.nextInt(4) * 2;
		int offsetY = random.nextInt(4) * 2;
		List<BukovRaidLayout.Mark> indexed = new ArrayList<>();

		for (int i = 0; i < RING_COORDINATES.length; i++) {
			String semanticId = "";
			if (i == 1) semanticId = "south_maintenance";
			if (i == 6) semanticId = "broken_rail_loading";
			if (i == 14) semanticId = "umbrella_frame_workshop";
			indexed.add(addRoom(layout, RING_COORDINATES[i][0], RING_COORDINATES[i][1],
					offsetX, offsetY, RING_ZONES[i], semanticId));
		}

		indexed.add(addRoom(layout, 2, 2, offsetX, offsetY,
				BukovRaidLayout.Zone.TRANSIT, "")); // 24
		indexed.add(addRoom(layout, 4, 2, offsetX, offsetY,
				BukovRaidLayout.Zone.COMBAT, "fog_lamp_pump_station")); // 25
		indexed.add(addRoom(layout, 2, 4, offsetX, offsetY,
				BukovRaidLayout.Zone.TRANSIT, "")); // 26
		indexed.add(addRoom(layout, 3, 3, offsetX, offsetY,
				BukovRaidLayout.Zone.HIGH_VALUE, "flooded_warehouse")); // 27
		BukovRaidLayout.Mark boss = addRoom(layout, 4, 4, offsetX, offsetY,
				BukovRaidLayout.Zone.BOSS, "scrap_compactor");
		boss.minimumPassageWidthTiles = 4;
		boss.eliteSpawnAllowed = true;
		indexed.add(boss); // 28

		for (int i = 0; i < 24; i++) {
			connect(layout, indexed.get(i), indexed.get((i + 1) % 24), "");
		}

		connect(layout, indexed.get(2), indexed.get(24), "");
		connect(layout, indexed.get(24), indexed.get(27), "");
		connect(layout, indexed.get(10), indexed.get(25), "");
		connect(layout, indexed.get(25), indexed.get(27), "");
		connect(layout, indexed.get(18), indexed.get(26), "pump_power");
		connect(layout, indexed.get(26), indexed.get(27), "");
		connect(layout, indexed.get(27), indexed.get(28), "");

		int secretCount = 1 + random.nextInt(3);
		int[] attachmentIndices = {5, 13, 28};
		int[][] secretCoordinates = {{7, 0}, {7, 6}, {5, 4}};
		for (int i = 0; i < secretCount; i++) {
			BukovRaidLayout.Mark secret = addRoom(layout,
					secretCoordinates[i][0], secretCoordinates[i][1],
					offsetX, offsetY, BukovRaidLayout.Zone.SECRET, "secret_cache_" + (i + 1));
			connect(layout, indexed.get(attachmentIndices[i]), secret, i == 2 ? "boss_defeated" : "");
		}

		layout.extractions.add(ExtractionDefinition.baseline(indexed.get(12).roomId()));
		layout.extractions.add(ExtractionDefinition.conditional(indexed.get(4).roomId()));
		float temporaryStart = 480f + random.nextInt(361);
		layout.extractions.add(ExtractionDefinition.temporary(indexed.get(20).roomId(), temporaryStart));

		layout.routes.add(new BukovRaidLayout.Route("safe_long", BukovRaidLayout.RouteRisk.SAFE,
				roomIds(indexed, 0, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12)));
		layout.routes.add(new BukovRaidLayout.Route("balanced_mid", BukovRaidLayout.RouteRisk.BALANCED,
				roomIds(indexed, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)));
		layout.routes.add(new BukovRaidLayout.Route("high_risk_short", BukovRaidLayout.RouteRisk.HIGH_RISK,
				roomIds(indexed, 0, 1, 2, 24, 27, 25, 10, 11, 12)));

		if (theme != null) theme.applyRoomWeights(layout);
		assignSyntheticAnchors(layout);
		if (theme != null) theme.applyLootWeights(layout);
		return layout;
	}

	private static void assignSyntheticAnchors(BukovRaidLayout layout) {
		int width = 0;
		int height = 0;
		for (BukovRaidLayout.Mark mark : layout.marks) {
			width = Math.max(width, mark.right + 2);
			height = Math.max(height, mark.bottom + 2);
		}
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.EMPTY);
		BukovAnchorPlanner.Result result =
				BukovAnchorPlanner.assign(width, height, map, layout, -1, -1);
		if (!result.valid) {
			throw new IllegalStateException("Synthetic Bukov anchors failed: " + result.reason);
		}
	}

	private static BukovRaidLayout.Mark addRoom(
			BukovRaidLayout layout, int gridX, int gridY, int offsetX, int offsetY,
			BukovRaidLayout.Zone zone, String semanticId) {
		int left = offsetX + gridX * 10;
		int top = offsetY + gridY * 10;
		BukovRaidLayout.Mark result =
				new BukovRaidLayout.Mark(left, top, left + 6, top + 6, zone, semanticId);
		layout.marks.add(result);
		return result;
	}

	private static void connect(BukovRaidLayout layout, BukovRaidLayout.Mark first,
			BukovRaidLayout.Mark second, String requiredEvent) {
		layout.links.add(new BukovRaidLayout.Link(first.roomId(), second.roomId(), requiredEvent));
	}

	private static List<String> roomIds(List<BukovRaidLayout.Mark> indexed, Integer... indices) {
		List<String> result = new ArrayList<>();
		for (Integer index : Arrays.asList(indices)) result.add(indexed.get(index).roomId());
		return result;
	}
}
