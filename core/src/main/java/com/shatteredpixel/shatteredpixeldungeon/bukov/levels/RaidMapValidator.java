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

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class RaidMapValidator {

	public enum Failure {
		NONE,
		ROOM_COUNT_OUT_OF_RANGE,
		DUPLICATE_ROOM_ID,
		BROKEN_LINK,
		DUPLICATE_SEMANTIC_ID,
		MISSING_REQUIRED_SEMANTIC_ROOM,
		INVALID_ZONE_COUNTS,
		INVALID_EXTRACTION,
		INVALID_ANCHOR,
		SPAWN_HAS_FEWER_THAN_TWO_RESOURCE_ROOMS,
		NO_KEYLESS_EXTRACTION_FROM_SPAWN,
		HIGH_VALUE_TOO_CLOSE_TO_SPAWN,
		BOSS_BLOCKS_BASE_EXTRACTION,
		NO_BOSS_FREE_PATH_BETWEEN_EXTRACTIONS,
		ELITE_ON_SINGLE_TILE_CHOKEPOINT,
		INVALID_ROUTE,
		MISSING_ROUTE_RISK_PROFILE,
		HIGH_RISK_ROUTE_NOT_SHORTER,
		ROUTE_RISK_ORDER_INVALID,
		DIRECT_WALK_TIME_OUT_OF_RANGE
	}

	public static final class Result {
		public final boolean valid;
		public final Failure failure;
		public final String reason;

		private Result(boolean valid, Failure failure, String reason) {
			this.valid = valid;
			this.failure = failure;
			this.reason = reason;
		}

		public static Result valid() {
			return new Result(true, Failure.NONE, "VALID");
		}

		public static Result invalid(Failure failure, String reason) {
			return new Result(false, failure, reason);
		}
	}

	private static final Set<String> REQUIRED_SEMANTICS = new HashSet<>();

	static {
		REQUIRED_SEMANTICS.add("south_maintenance");
		REQUIRED_SEMANTICS.add("broken_rail_loading");
		REQUIRED_SEMANTICS.add("fog_lamp_pump_station");
		REQUIRED_SEMANTICS.add("flooded_warehouse");
		REQUIRED_SEMANTICS.add("umbrella_frame_workshop");
		REQUIRED_SEMANTICS.add("scrap_compactor");
	}

	private RaidMapValidator() {
	}

	public static Result validate(BukovRaidLayout layout) {
		return validateInternal(layout, null);
	}

	public static Result validate(
			BukovRaidLayout layout,
			BukovRaidMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("raid mode is required");
		}
		return validateInternal(layout, mode);
	}

	private static Result validateInternal(
			BukovRaidLayout layout,
			BukovRaidMode mode) {
		boolean roomCountValid = layout != null
				&& (mode == null
						? layout.playableRoomCount() >= 26
								&& layout.playableRoomCount() <= 34
						: mode.acceptsContentRoomCount(
								layout.playableRoomCount()));
		if (!roomCountValid) {
			return Result.invalid(
					Failure.ROOM_COUNT_OUT_OF_RANGE,
					mode == null
							? "Expected 26-34 playable rooms"
							: "Expected " + mode.minimumContentRooms
									+ "-" + mode.maximumContentRooms
									+ " playable rooms for " + mode.name());
		}

		Map<String, BukovRaidLayout.Mark> marks = new LinkedHashMap<>();
		Set<String> semantics = new HashSet<>();
		Map<BukovRaidLayout.Zone, Integer> zoneCounts = new HashMap<>();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (marks.put(mark.roomId(), mark) != null) {
				return Result.invalid(Failure.DUPLICATE_ROOM_ID, mark.roomId());
			}
			zoneCounts.put(mark.zone, zoneCounts.containsKey(mark.zone)
					? zoneCounts.get(mark.zone) + 1 : 1);
			if (!mark.semanticId.isEmpty() && !semantics.add(mark.semanticId)) {
				return Result.invalid(Failure.DUPLICATE_SEMANTIC_ID, mark.semanticId);
			}
			if (mark.eliteSpawnAllowed && mark.minimumPassageWidthTiles <= 1) {
				return Result.invalid(Failure.ELITE_ON_SINGLE_TILE_CHOKEPOINT, mark.roomId());
			}
		}

		if (!semantics.containsAll(REQUIRED_SEMANTICS)) {
			return Result.invalid(Failure.MISSING_REQUIRED_SEMANTIC_ROOM,
					"Missing " + missing(REQUIRED_SEMANTICS, semantics));
		}
		if (!(mode == null
				? legacyZoneCounts(zoneCounts)
				: validZoneCounts(zoneCounts, mode))) {
			return Result.invalid(Failure.INVALID_ZONE_COUNTS, zoneCounts.toString());
		}

		for (BukovRaidLayout.Link link : layout.links) {
			if (link.firstRoomId.equals(link.secondRoomId)
					|| !marks.containsKey(link.firstRoomId)
					|| !marks.containsKey(link.secondRoomId)
					|| link.traversalSeconds <= 0f) {
				return Result.invalid(Failure.BROKEN_LINK,
						link.firstRoomId + " -> " + link.secondRoomId);
			}
		}

		Result extractionResult = validateExtractions(layout, marks, mode);
		if (!extractionResult.valid) return extractionResult;
		Result anchorResult = validateStoredAnchors(layout, marks);
		if (!anchorResult.valid) return anchorResult;

		List<String> spawns = roomIds(layout, BukovRaidLayout.Zone.SPAWN);
		List<String> resources = new ArrayList<>();
		resources.addAll(roomIds(layout, BukovRaidLayout.Zone.LOW_LOOT));
		resources.addAll(roomIds(layout, BukovRaidLayout.Zone.MEDICAL));
		List<String> highValueRooms = roomIds(layout, BukovRaidLayout.Zone.HIGH_VALUE);
		List<String> bosses = roomIds(layout, BukovRaidLayout.Zone.BOSS);
		Set<String> bossRooms = new HashSet<>(bosses);

		ExtractionDefinition baseline = baselineExtraction(layout);
		float minimumDirectSeconds = mode == null
				? 60f : minimumDirectSeconds(mode);
		float maximumDirectSeconds = mode == null
				? 150f : maximumDirectSeconds(mode);
		float averageTraversalSeconds =
				averageTraversalSeconds(layout);
		for (String spawn : spawns) {
			Map<String, Integer> initialDistances = distances(layout, spawn, false, new HashSet<String>());
			int reachableResources = 0;
			for (String resource : resources) {
				if (initialDistances.containsKey(resource)) reachableResources++;
			}
			if (reachableResources < 2) {
				return Result.invalid(Failure.SPAWN_HAS_FEWER_THAN_TWO_RESOURCE_ROOMS, spawn);
			}
			if (!initialDistances.containsKey(baseline.roomId)) {
				return Result.invalid(Failure.NO_KEYLESS_EXTRACTION_FROM_SPAWN, spawn);
			}
			float directSeconds =
					initialDistances.get(baseline.roomId)
							* averageTraversalSeconds;
			if (directSeconds < minimumDirectSeconds
					|| directSeconds > maximumDirectSeconds) {
				return Result.invalid(
						Failure.DIRECT_WALK_TIME_OUT_OF_RANGE,
						"Direct walk " + directSeconds
								+ " seconds from spawn " + spawn
								+ "; expected " + minimumDirectSeconds
								+ "-" + maximumDirectSeconds
								+ " for " + (mode == null
										? "default map"
										: mode.name()));
			}
			for (String highValue : highValueRooms) {
				Integer distance = initialDistances.get(highValue);
				if (distance == null || distance < 4) {
					return Result.invalid(Failure.HIGH_VALUE_TOO_CLOSE_TO_SPAWN,
							spawn + " -> " + highValue + " = " + distance);
				}
			}
			if (!distances(layout, spawn, false, bossRooms).containsKey(baseline.roomId)) {
				return Result.invalid(Failure.BOSS_BLOCKS_BASE_EXTRACTION, spawn);
			}
		}

		for (int i = 0; i < layout.extractions.size(); i++) {
			for (int j = i + 1; j < layout.extractions.size(); j++) {
				String first = layout.extractions.get(i).roomId;
				String second = layout.extractions.get(j).roomId;
				if (!distances(layout, first, false, bossRooms).containsKey(second)) {
					return Result.invalid(Failure.NO_BOSS_FREE_PATH_BETWEEN_EXTRACTIONS,
							layout.extractions.get(i).id + " -> " + layout.extractions.get(j).id);
				}
			}
		}

		Result routeResult = validateRoutes(layout, marks);
		if (!routeResult.valid) return routeResult;

		return Result.valid();
	}

	private static boolean legacyZoneCounts(
			Map<BukovRaidLayout.Zone, Integer> counts) {
		return between(counts, BukovRaidLayout.Zone.SPAWN, 3, 6)
				&& between(counts, BukovRaidLayout.Zone.LOW_LOOT, 4, 8)
				&& between(counts, BukovRaidLayout.Zone.COMBAT, 3, 6)
				&& between(counts, BukovRaidLayout.Zone.HIGH_VALUE, 1, 3)
				&& between(counts, BukovRaidLayout.Zone.MEDICAL, 1, 2)
				&& between(counts, BukovRaidLayout.Zone.HAZARD, 1, 3)
				&& between(counts, BukovRaidLayout.Zone.BOSS, 0, 1)
				&& between(counts, BukovRaidLayout.Zone.EXTRACTION, 2, 4)
				&& between(counts, BukovRaidLayout.Zone.SECRET, 1, 3);
	}

	private static boolean validZoneCounts(
			Map<BukovRaidLayout.Zone, Integer> counts,
			BukovRaidMode mode) {
		int spawn = mode.trainingGround() ? 1
				: mode == BukovRaidMode.QUICK_SWEEP
						|| mode == BukovRaidMode.SCAVENGER ? 2 : 3;
		int lowLoot = mode.trainingGround() ? 3
				: mode == BukovRaidMode.QUICK_SWEEP ? 4
				: mode == BukovRaidMode.SCAVENGER ? 5
				: mode == BukovRaidMode.BOSS_CONTRACT ? 4 : 6;
		int combat = mode.trainingGround() ? 4
				: mode == BukovRaidMode.QUICK_SWEEP ? 3
				: mode == BukovRaidMode.SCAVENGER ? 4
				: mode == BukovRaidMode.BOSS_CONTRACT ? 6 : 5;
		int medical = mode.trainingGround() ? 2
				: mode == BukovRaidMode.QUICK_SWEEP
						|| mode == BukovRaidMode.BOSS_CONTRACT ? 1 : 2;
		int hazard = mode.trainingGround() ? 0
				: mode == BukovRaidMode.BOSS_CONTRACT ? 3
				: mode == BukovRaidMode.QUICK_SWEEP ? 1 : 2;
		return exact(counts, BukovRaidLayout.Zone.SPAWN, spawn)
				&& exact(counts, BukovRaidLayout.Zone.LOW_LOOT, lowLoot)
				&& exact(counts, BukovRaidLayout.Zone.COMBAT, combat)
				&& exact(counts, BukovRaidLayout.Zone.HIGH_VALUE, 1)
				&& exact(counts, BukovRaidLayout.Zone.MEDICAL, medical)
				&& exact(counts, BukovRaidLayout.Zone.HAZARD, hazard)
				&& exact(counts, BukovRaidLayout.Zone.BOSS, 1)
				&& exact(counts, BukovRaidLayout.Zone.EXTRACTION, 3)
				&& exact(counts, BukovRaidLayout.Zone.SECRET, 1);
	}

	private static boolean exact(
			Map<BukovRaidLayout.Zone, Integer> counts,
			BukovRaidLayout.Zone zone,
			int expected) {
		Integer actual = counts.get(zone);
		return (actual == null ? 0 : actual) == expected;
	}

	public static float minimumDirectSeconds(BukovRaidMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("raid mode is required");
		}
		if (mode.trainingGround()) return 20f;
		if (mode == BukovRaidMode.QUICK_SWEEP) return 30f;
		if (mode == BukovRaidMode.SCAVENGER) return 45f;
		return 60f;
	}

	public static float maximumDirectSeconds(BukovRaidMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("raid mode is required");
		}
		if (mode == BukovRaidMode.BOSS_CONTRACT
				|| mode == BukovRaidMode.SCAVENGER) return 180f;
		if (mode == BukovRaidMode.EXPEDITION) return 150f;
		return 120f;
	}

	private static Result validateExtractions(BukovRaidLayout layout,
			Map<String, BukovRaidLayout.Mark> marks,
			BukovRaidMode mode) {
		if (layout.extractions.size() != 3) {
			return Result.invalid(Failure.INVALID_EXTRACTION, "Expected E01, E02 and E03");
		}
		Set<String> ids = new HashSet<>();
		Set<String> rooms = new HashSet<>();
		for (ExtractionDefinition extraction : layout.extractions) {
			BukovRaidLayout.Mark room = marks.get(extraction.roomId);
			if (!ids.add(extraction.id) || room == null
					|| !rooms.add(extraction.roomId)
					|| room.zone != BukovRaidLayout.Zone.EXTRACTION
					|| extraction.interactionSeconds <= 0f
					|| extraction.rollbackFractionPerSecond != 0.25f
					|| extraction.availableUntilSeconds < extraction.availableFromSeconds) {
				return Result.invalid(Failure.INVALID_EXTRACTION, extraction.id);
			}
		}
		ExtractionDefinition baseline = layout.extraction("E01");
		ExtractionDefinition conditional = layout.extraction("E02");
		ExtractionDefinition temporary = layout.extraction("E03");
		BukovRaidMode effectiveMode =
				mode == null ? BukovRaidMode.EXPEDITION : mode;
		if (baseline == null
				|| baseline.type != ExtractionDefinition.Type.BASELINE
				|| !baseline.isKeylessAndBossIndependent()
				|| !baseline.requiredEvent.isEmpty()
				|| baseline.availableFromSeconds != 0f
				|| baseline.availableUntilSeconds != Float.MAX_VALUE
				|| baseline.interactionSeconds != 5f
				|| conditional == null
				|| conditional.type != ExtractionDefinition.Type.CONDITIONAL
				|| !"pump_power".equals(conditional.requiredEvent)
				|| conditional.interactionSeconds != 8f
				|| temporary == null
				|| temporary.type != ExtractionDefinition.Type.TEMPORARY
				|| !temporary.requiredEvent.isEmpty()
				|| temporary.availableFromSeconds
						< effectiveMode.temporaryExtractionEarliestSeconds()
				|| temporary.availableFromSeconds
						> effectiveMode.temporaryExtractionLatestSeconds()
				|| temporary.availableUntilSeconds - temporary.availableFromSeconds != 120f
				|| !ids.contains("E01") || !ids.contains("E02") || !ids.contains("E03")) {
			return Result.invalid(Failure.INVALID_EXTRACTION, ids.toString());
		}
		return Result.valid();
	}

	private static Result validateStoredAnchors(BukovRaidLayout layout,
			Map<String, BukovRaidLayout.Mark> marks) {
		boolean hasExtractionAnchor = false;
		for (ExtractionDefinition extraction : layout.extractions) {
			if (extraction.interactionCell >= 0
					|| extraction.interactionX >= 0
					|| extraction.interactionY >= 0) {
				hasExtractionAnchor = true;
				break;
			}
		}
		if (!hasExtractionAnchor && layout.lootAnchors.isEmpty()) return Result.valid();
		if (layout.lootAnchors.size() != 3) {
			return Result.invalid(Failure.INVALID_ANCHOR, "Expected L01, L02 and L03");
		}

		Set<Integer> cells = new HashSet<>();
		for (ExtractionDefinition extraction : layout.extractions) {
			BukovRaidLayout.Mark mark = marks.get(extraction.roomId);
			if (!storedPointInside(mark, extraction.interactionX, extraction.interactionY, true)
					|| extraction.interactionCell < 0
					|| !cells.add(extraction.interactionCell)) {
				return Result.invalid(Failure.INVALID_ANCHOR, extraction.id);
			}
		}

		Set<String> ids = new HashSet<>();
		Set<String> rooms = new HashSet<>();
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			BukovRaidLayout.Mark mark = marks.get(anchor.roomId);
			if (!ids.add(anchor.id)
					|| !rooms.add(anchor.roomId)
					|| anchor.cell < 0
					|| !cells.add(anchor.cell)
					|| !lootEligible(mark)
					|| !storedPointInside(mark, anchor.x, anchor.y, false)) {
				return Result.invalid(Failure.INVALID_ANCHOR, anchor.id);
			}
		}
		if (!ids.contains("L01") || !ids.contains("L02") || !ids.contains("L03")) {
			return Result.invalid(Failure.INVALID_ANCHOR, ids.toString());
		}
		return Result.valid();
	}

	private static boolean storedPointInside(
			BukovRaidLayout.Mark mark, int x, int y, boolean allowBoundary) {
		if (mark == null) return false;
		return allowBoundary
				? x >= mark.left && x <= mark.right && y >= mark.top && y <= mark.bottom
				: x > mark.left && x < mark.right && y > mark.top && y < mark.bottom;
	}

	private static boolean lootEligible(BukovRaidLayout.Mark mark) {
		return mark != null
				&& mark.zone != BukovRaidLayout.Zone.SPAWN
				&& mark.zone != BukovRaidLayout.Zone.BOSS
				&& mark.zone != BukovRaidLayout.Zone.EXTRACTION;
	}

	private static Result validateRoutes(BukovRaidLayout layout,
			Map<String, BukovRaidLayout.Mark> marks) {
		Map<BukovRaidLayout.RouteRisk, Integer> routeLengths = new HashMap<>();
		Map<BukovRaidLayout.RouteRisk, Float> routeThreat = new HashMap<>();
		Set<String> routeIds = new HashSet<>();
		for (BukovRaidLayout.Route route : layout.routes) {
			if (!routeIds.add(route.routeId) || route.roomIds.size() < 2) {
				return Result.invalid(Failure.INVALID_ROUTE, route.routeId);
			}
			for (String roomId : route.roomIds) {
				if (!marks.containsKey(roomId)) {
					return Result.invalid(Failure.INVALID_ROUTE, route.routeId + " missing " + roomId);
				}
			}
			for (int i = 1; i < route.roomIds.size(); i++) {
				if (!linked(layout, route.roomIds.get(i - 1), route.roomIds.get(i))) {
					return Result.invalid(Failure.INVALID_ROUTE, route.routeId + " has a gap");
				}
			}
			if (marks.get(route.roomIds.get(0)).zone != BukovRaidLayout.Zone.SPAWN
					|| marks.get(route.roomIds.get(route.roomIds.size() - 1)).zone
					!= BukovRaidLayout.Zone.EXTRACTION) {
				return Result.invalid(Failure.INVALID_ROUTE,
						route.routeId + " must join a spawn to an extraction");
			}
			routeLengths.put(route.risk, route.roomIds.size() - 1);
			routeThreat.put(route.risk,
					BukovRouteMetrics.averageThreat(layout, route.roomIds));
		}
		for (BukovRaidLayout.RouteRisk risk : BukovRaidLayout.RouteRisk.values()) {
			if (!routeLengths.containsKey(risk)) {
				return Result.invalid(Failure.MISSING_ROUTE_RISK_PROFILE, risk.name());
			}
		}
		if (routeLengths.get(BukovRaidLayout.RouteRisk.HIGH_RISK)
				>= routeLengths.get(BukovRaidLayout.RouteRisk.SAFE)) {
			return Result.invalid(Failure.HIGH_RISK_ROUTE_NOT_SHORTER, routeLengths.toString());
		}
		if (!(routeThreat.get(BukovRaidLayout.RouteRisk.SAFE)
				< routeThreat.get(BukovRaidLayout.RouteRisk.BALANCED)
				&& routeThreat.get(BukovRaidLayout.RouteRisk.BALANCED)
				< routeThreat.get(BukovRaidLayout.RouteRisk.HIGH_RISK))) {
			return Result.invalid(Failure.ROUTE_RISK_ORDER_INVALID, routeThreat.toString());
		}
		return Result.valid();
	}

	private static boolean linked(BukovRaidLayout layout, String first, String second) {
		for (BukovRaidLayout.Link link : layout.links) {
			if (link.joins(first) && link.joins(second)) return true;
		}
		return false;
	}

	private static ExtractionDefinition baselineExtraction(BukovRaidLayout layout) {
		for (ExtractionDefinition extraction : layout.extractions) {
			if (extraction.type == ExtractionDefinition.Type.BASELINE) return extraction;
		}
		return null;
	}

	private static float averageTraversalSeconds(BukovRaidLayout layout) {
		float total = 0f;
		for (BukovRaidLayout.Link link : layout.links) total += link.traversalSeconds;
		return total / layout.links.size();
	}

	private static Map<String, Integer> distances(BukovRaidLayout layout, String start,
			boolean eventsApplied, Set<String> excludedRooms) {
		Map<String, Integer> result = new HashMap<>();
		if (excludedRooms.contains(start)) return result;
		Queue<String> pending = new ArrayDeque<>();
		result.put(start, 0);
		pending.add(start);
		while (!pending.isEmpty()) {
			String current = pending.remove();
			for (String neighbour : layout.neighbours(current, eventsApplied)) {
				if (!excludedRooms.contains(neighbour) && !result.containsKey(neighbour)) {
					result.put(neighbour, result.get(current) + 1);
					pending.add(neighbour);
				}
			}
		}
		return result;
	}

	private static List<String> roomIds(BukovRaidLayout layout, BukovRaidLayout.Zone zone) {
		List<String> result = new ArrayList<>();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (mark.zone == zone) result.add(mark.roomId());
		}
		return result;
	}

	private static boolean between(
			Map<BukovRaidLayout.Zone, Integer> counts,
			BukovRaidLayout.Zone zone,
			int minimum,
			int maximum) {
		int count = counts.containsKey(zone) ? counts.get(zone) : 0;
		return count >= minimum && count <= maximum;
	}

	private static Set<String> missing(Set<String> required, Set<String> present) {
		Set<String> result = new HashSet<>(required);
		result.removeAll(present);
		return result;
	}
}
