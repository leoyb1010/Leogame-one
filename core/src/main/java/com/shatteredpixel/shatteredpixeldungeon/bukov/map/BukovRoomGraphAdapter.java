/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.map;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRouteMetrics;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.RaidMapValidator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.connection.ConnectionRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Read-only bridge from the existing RegularLevel room graph to Bukov data.
 *
 * It deliberately does not change Room, Builder, Painter, Level transitions or
 * terrain. Runtime systems use the returned room index for loot, AI and
 * extraction placement while the original map remains authoritative.
 */
public final class BukovRoomGraphAdapter {

	public static final class AdaptedMap {

		public final BukovRaidLayout layout;
		public final List<String> diagnostics;
		private final Map<String, Room> roomsById;

		private AdaptedMap(BukovRaidLayout layout, Map<String, Room> roomsById,
				List<String> diagnostics) {
			this.layout = layout;
			this.roomsById = roomsById;
			this.diagnostics = Collections.unmodifiableList(diagnostics);
		}

		public boolean readyForRaid() {
			return diagnostics.isEmpty();
		}

		public Room room(String roomId) {
			return roomsById.get(roomId);
		}

		public BukovRaidLayout.Mark mark(Room room) {
			return room == null ? null : layout.mark(roomId(room));
		}

		public List<Room> rooms(BukovRaidLayout.Zone zone) {
			List<Room> result = new ArrayList<>();
			for (BukovRaidLayout.Mark mark : layout.marks) {
				if (mark.zone == zone) {
					Room room = roomsById.get(mark.roomId());
					if (room != null) result.add(room);
				}
			}
			return result;
		}
	}

	private static final Comparator<Room> ROOM_ID_ORDER =
			new Comparator<Room>() {
				@Override
				public int compare(Room first, Room second) {
					return roomId(first).compareTo(roomId(second));
				}
			};
	private static final int MAX_EXTRACTION_TRIPLET_EVALUATIONS = 24;

	private BukovRoomGraphAdapter() {
	}

	public static AdaptedMap adapt(RegularLevel level, long seed, String themeId) {
		return adapt(level, seed, themeId, BukovRaidMode.EXPEDITION);
	}

	public static AdaptedMap adapt(
			RegularLevel level,
			long seed,
			String themeId,
			BukovRaidMode raidMode) {
		if (level == null) throw new IllegalArgumentException("level is required");
		return adapt(level.rooms(), seed, themeId, raidMode);
	}

	/**
	 * Rebinds a saved semantic layout to Room objects restored by RegularLevel.
	 * No roles are regenerated here: the saved layout remains authoritative.
	 */
	public static AdaptedMap bind(RegularLevel level, BukovRaidLayout savedLayout) {
		return bind(level, savedLayout, BukovRaidMode.EXPEDITION);
	}

	public static AdaptedMap bind(
			RegularLevel level,
			BukovRaidLayout savedLayout,
			BukovRaidMode raidMode) {
		if (level == null) throw new IllegalArgumentException("level is required");
		if (savedLayout == null) throw new IllegalArgumentException("savedLayout is required");
		if (raidMode == null) throw new IllegalArgumentException("raidMode is required");

		Map<String, Room> roomsById = new LinkedHashMap<>();
		List<String> diagnostics = new ArrayList<>();
		for (Room room : level.rooms()) {
			String id = roomId(room);
			if (roomsById.put(id, room) != null) diagnostics.add("DUPLICATE_ROOM_RECT:" + id);
		}
		for (BukovRaidLayout.Mark mark : savedLayout.marks) {
			if (!roomsById.containsKey(mark.roomId())) {
				diagnostics.add("SAVED_ROOM_MISSING_FROM_LEVEL:" + mark.roomId());
			}
		}
		for (String roomId : roomsById.keySet()) {
			if (savedLayout.mark(roomId) == null) {
				diagnostics.add("LEVEL_ROOM_MISSING_FROM_SAVED_LAYOUT:" + roomId);
			}
		}
		addValidationDiagnostic(savedLayout, raidMode, diagnostics);
		return new AdaptedMap(savedLayout, roomsById, diagnostics);
	}

	static AdaptedMap adapt(List<? extends Room> sourceRooms, long seed, String themeId) {
		return adapt(sourceRooms, seed, themeId, BukovRaidMode.EXPEDITION);
	}

	static AdaptedMap adapt(
			List<? extends Room> sourceRooms,
			long seed,
			String themeId,
			BukovRaidMode raidMode) {
		if (sourceRooms == null) throw new IllegalArgumentException("rooms are required");
		if (raidMode == null) throw new IllegalArgumentException("raidMode is required");

		List<Room> rooms = new ArrayList<>(sourceRooms);
		Collections.sort(rooms, ROOM_ID_ORDER);

		BukovRaidLayout layout = new BukovRaidLayout();
		layout.seed = seed;
		layout.themeId = themeId == null || themeId.isEmpty() ? "fog_depot" : themeId;
		Map<String, Room> roomsById = new LinkedHashMap<>();
		Map<String, BukovRaidLayout.Mark> marksById = new LinkedHashMap<>();
		List<String> diagnostics = new ArrayList<>();

		for (Room room : rooms) {
			String id = roomId(room);
			if (roomsById.put(id, room) != null) {
				diagnostics.add("DUPLICATE_ROOM_RECT:" + id);
				continue;
			}
			BukovRaidLayout.Mark mark = new BukovRaidLayout.Mark(
					room.left, room.top, room.right, room.bottom, initialZone(room), "");
			mark.minimumPassageWidthTiles = minimumDimension(room);
			mark.structuralTransit = room instanceof ConnectionRoom;
			layout.marks.add(mark);
			marksById.put(id, mark);
		}

		Set<String> linkKeys = new HashSet<>();
		for (Room room : rooms) {
			String first = roomId(room);
			for (Room connected : room.connected.keySet()) {
				String second = roomId(connected);
				if (!roomsById.containsKey(second)) continue;
				String key = first.compareTo(second) < 0
						? first + "|" + second : second + "|" + first;
				if (linkKeys.add(key)) layout.links.add(new BukovRaidLayout.Link(first, second));
			}
		}

		Room entrance = findSpecialRoom(rooms, true);
		Room originalExit = findSpecialRoom(rooms, false);
		if (entrance == null) diagnostics.add("NO_ENTRANCE_ROOM");
		if (originalExit == null) diagnostics.add("NO_EXIT_ROOM");
		if (entrance == null || originalExit == null) {
			return new AdaptedMap(layout, roomsById, diagnostics);
		}

		Map<String, Integer> entranceDistances = distances(layout, roomId(entrance), null);
		if (!entranceDistances.containsKey(roomId(originalExit))) {
			diagnostics.add("ENTRANCE_CANNOT_REACH_ORIGINAL_EXIT");
			return new AdaptedMap(layout, roomsById, diagnostics);
		}

		BukovRaidLayout.Mark highValue = chooseHighValue(
				rooms, marksById, entranceDistances, entrance, originalExit, seed);
		if (highValue == null) {
			diagnostics.add("NO_HIGH_VALUE_ROOM_AT_DISTANCE_4");
		}

		int requiredSpawns = raidMode.trainingGround() ? 1
				: raidMode == BukovRaidMode.QUICK_SWEEP
						|| raidMode == BukovRaidMode.SCAVENGER ? 2 : 3;
		List<BukovRaidLayout.Mark> spawns = chooseSpawnCandidates(
				rooms,
				marksById,
				layout,
				entrance,
				originalExit,
				highValue,
				requiredSpawns,
				seed);
		if (spawns.size() < requiredSpawns) {
			diagnostics.add("FEWER_THAN_MODE_SAFE_SPAWN_CANDIDATES:"
					+ requiredSpawns);
		}

		BukovRaidLayout.Mark boss = chooseBossRoom(
				rooms, marksById, layout, spawns, originalExit, highValue, seed);
		if (boss == null) diagnostics.add("NO_BOSS_ROOM_WITH_SAFE_EXIT_BYPASS");

		List<BukovRaidLayout.Mark> extractionMarks = chooseExtractions(
				rooms, marksById, layout, spawns,
				highValue, boss, originalExit, raidMode, seed);
		if (extractionMarks.size() < 3) diagnostics.add("FEWER_THAN_3_EXTRACTION_ROOMS");

		assignCoreSemantics(rooms, layout, marksById, entranceDistances,
				spawns, highValue, boss, extractionMarks, seed, diagnostics);
		normalizeSupportingZones(rooms, marksById, spawns, extractionMarks,
				highValue, boss, raidMode, seed);
		addExtractions(layout, extractionMarks, seed, raidMode);
		addRiskRoutes(
				layout,
				roomId(entrance),
				extractionMarks,
				raidMode,
				boss,
				diagnostics);

		if (!raidMode.acceptsContentRoomCount(layout.playableRoomCount())) {
			diagnostics.add("ROOM_COUNT_NOT_IN_MODE_RANGE:"
					+ raidMode.name() + ":" + layout.playableRoomCount()
					+ ":physical=" + layout.marks.size());
		}
		if (layout.links.size() < layout.marks.size()) {
			diagnostics.add("ROOM_GRAPH_HAS_NO_LOOP");
		}
		addValidationDiagnostic(layout, raidMode, diagnostics);

		return new AdaptedMap(layout, roomsById, diagnostics);
	}

	private static void addValidationDiagnostic(
			BukovRaidLayout layout,
			BukovRaidMode raidMode,
			List<String> diagnostics) {
		// Expedition remains the generation-time compatibility contract.
		// The mode-aware acceptance matrix validates compact, scavenger, boss,
		// and fixed training layouts after their terrain anchors are assigned.
		if (raidMode != BukovRaidMode.EXPEDITION) {
			return;
		}
		RaidMapValidator.Result validation = RaidMapValidator.validate(layout);
		if (validation.valid) {
			return;
		}
		diagnostics.add("STRICT_VALIDATION:"
				+ validation.failure + ":" + validation.reason);
	}

	private static BukovRaidLayout.Zone initialZone(Room room) {
		if (room instanceof SecretRoom) return BukovRaidLayout.Zone.SECRET;
		if (room instanceof ConnectionRoom || room.isEntrance() || room.isExit()) {
			return BukovRaidLayout.Zone.TRANSIT;
		}
		String name = room.getClass().getSimpleName();
		if (containsAny(name, "Toxic", "Trap", "Fire", "Pit", "Chasm", "Sentry")) {
			return BukovRaidLayout.Zone.HAZARD;
		}
		if (containsAny(name, "Laboratory", "Garden", "Pool", "Well")) {
			return BukovRaidLayout.Zone.MEDICAL;
		}
		if (containsAny(name, "Storage", "Library", "Larder", "Runestone", "Treasury")) {
			return BukovRaidLayout.Zone.LOW_LOOT;
		}
		if (room instanceof StandardRoom) return BukovRaidLayout.Zone.COMBAT;
		return BukovRaidLayout.Zone.LOW_LOOT;
	}

	private static BukovRaidLayout.Mark chooseHighValue(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, Map<String, Integer> entranceDistances,
			Room entrance, Room exit, long seed) {
		Room selected = null;
		long selectedScore = Long.MIN_VALUE;
		for (Room room : rooms) {
			Integer distance = entranceDistances.get(roomId(room));
			if (room == entrance || room == exit || distance == null || distance < 4
					|| room.connected.size() < 2 || room instanceof ConnectionRoom
					|| room instanceof SecretRoom) {
				continue;
			}
			long score = distance * 10_000L + area(room) * 100L
					+ room.connected.size() * 10L + stableTie(seed, room);
			if (score > selectedScore) {
				selected = room;
				selectedScore = score;
			}
		}
		return selected == null ? null : marksById.get(roomId(selected));
	}

	private static List<BukovRaidLayout.Mark> chooseSpawnCandidates(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, BukovRaidLayout layout,
			Room entrance, Room exit, BukovRaidLayout.Mark highValue,
			int targetCount, long seed) {
		List<BukovRaidLayout.Mark> result = new ArrayList<>();
		BukovRaidLayout.Mark entranceMark = marksById.get(roomId(entrance));
		entranceMark.zone = BukovRaidLayout.Zone.SPAWN;
		result.add(entranceMark);

		List<Room> candidates = new ArrayList<>();
		for (Room room : rooms) {
			if (room == entrance || room == exit || room instanceof ConnectionRoom
					|| room instanceof SecretRoom || room.connected.size() < 2) {
				continue;
			}
			if (highValue != null) {
				Integer distance = distances(layout, roomId(room), null).get(highValue.roomId());
				if (distance == null || distance < 4) continue;
			}
			candidates.add(room);
		}
		Collections.sort(candidates, (first, second) -> Long.compare(
				spawnScore(second, result, layout, seed),
				spawnScore(first, result, layout, seed)));

		for (Room candidate : candidates) {
			if (result.size() >= targetCount) break;
			String candidateId = roomId(candidate);
			boolean separated = true;
			for (BukovRaidLayout.Mark spawn : result) {
				Integer distance = distances(layout, candidateId, null).get(spawn.roomId());
				if (distance == null || distance < 3) {
					separated = false;
					break;
				}
			}
			if (separated) {
				BukovRaidLayout.Mark mark = marksById.get(candidateId);
				mark.zone = BukovRaidLayout.Zone.SPAWN;
				result.add(mark);
			}
		}
		return result;
	}

	private static long spawnScore(Room room, List<BukovRaidLayout.Mark> selected,
			BukovRaidLayout layout, long seed) {
		int minimumDistance = Integer.MAX_VALUE;
		Map<String, Integer> candidateDistances = distances(layout, roomId(room), null);
		for (BukovRaidLayout.Mark spawn : selected) {
			Integer distance = candidateDistances.get(spawn.roomId());
			if (distance != null) minimumDistance = Math.min(minimumDistance, distance);
		}
		if (minimumDistance == Integer.MAX_VALUE) minimumDistance = 0;
		return minimumDistance * 10_000L + room.connected.size() * 100L
				+ area(room) + stableTie(seed, room);
	}

	private static BukovRaidLayout.Mark chooseBossRoom(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> spawns, Room exit, BukovRaidLayout.Mark highValue, long seed) {
		List<Room> candidates = new ArrayList<>();
		for (Room room : rooms) {
			BukovRaidLayout.Mark mark = marksById.get(roomId(room));
			if (mark == highValue || mark.zone == BukovRaidLayout.Zone.SPAWN || room == exit
					|| room instanceof ConnectionRoom || room instanceof SecretRoom
					|| minimumDimension(room) < 4) {
				continue;
			}
			candidates.add(room);
		}
		Collections.sort(candidates, (first, second) -> Long.compare(
				bossScore(second, highValue, layout, seed),
				bossScore(first, highValue, layout, seed)));
		for (Room candidate : candidates) {
			String excluded = roomId(candidate);
			boolean blocksExit = false;
			for (BukovRaidLayout.Mark spawn : spawns) {
				if (!distances(layout, spawn.roomId(), excluded).containsKey(roomId(exit))) {
					blocksExit = true;
					break;
				}
			}
			if (!blocksExit) return marksById.get(excluded);
		}
		return null;
	}

	private static long bossScore(Room room, BukovRaidLayout.Mark highValue,
			BukovRaidLayout layout, long seed) {
		Integer distance = highValue == null ? null
				: distances(layout, highValue.roomId(), null).get(roomId(room));
		int proximityScore = distance == null ? 0 : Math.max(0, 8 - distance);
		return proximityScore * 100_000L + area(room) * 100L
				+ room.connected.size() * 10L + stableTie(seed, room);
	}

	private static List<BukovRaidLayout.Mark> chooseExtractions(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> spawns, BukovRaidLayout.Mark highValue,
			BukovRaidLayout.Mark boss, Room originalExit,
			BukovRaidMode raidMode, long seed) {
		List<Room> secondaryCandidates = new ArrayList<>();
		List<Room> baselineCandidates = new ArrayList<>();
		float averageTraversalSeconds =
				averageTraversalSeconds(layout);
		float minimumDirectSeconds =
				RaidMapValidator.minimumDirectSeconds(raidMode);
		float maximumDirectSeconds =
				RaidMapValidator.maximumDirectSeconds(raidMode);
		for (Room room : rooms) {
			BukovRaidLayout.Mark mark = marksById.get(roomId(room));
			int minimumSpawnDistance = minimumDistanceFromSpawns(
					layout, spawns, roomId(room));
			int maximumSpawnDistance = maximumDistanceFromSpawns(
					layout, spawns, roomId(room));
			if (minimumSpawnDistance < 0
					|| mark == highValue || mark == boss
					|| mark.zone == BukovRaidLayout.Zone.SPAWN
					|| room instanceof ConnectionRoom
					|| room instanceof SecretRoom
					|| room != originalExit && room.connected.size() < 2) {
				continue;
			}
			if (boss != null
					&& !distances(layout, roomId(room), boss.roomId())
							.containsKey(roomId(originalExit))) {
				continue;
			}
			secondaryCandidates.add(room);
			if (minimumSpawnDistance * averageTraversalSeconds
							>= minimumDirectSeconds
					&& maximumSpawnDistance * averageTraversalSeconds
							<= maximumDirectSeconds) {
				baselineCandidates.add(room);
			}
		}
		Comparator<Room> distanceOrder = (first, second) -> {
			int firstDistance = minimumDistanceFromSpawns(
					layout, spawns, roomId(first));
			int secondDistance = minimumDistanceFromSpawns(
					layout, spawns, roomId(second));
			int comparison = Integer.compare(secondDistance, firstDistance);
			if (comparison != 0) return comparison;
			return Long.compare(stableTie(seed, second), stableTie(seed, first));
		};
		Collections.sort(baselineCandidates, distanceOrder);
		Collections.sort(secondaryCandidates, distanceOrder);

		String entranceId = spawns.isEmpty()
				? null : spawns.get(0).roomId();
		int evaluatedTriplets = 0;
		for (Room baseline : baselineCandidates) {
			for (int firstIndex = 0;
				firstIndex < secondaryCandidates.size();
				firstIndex++) {
				Room first = secondaryCandidates.get(firstIndex);
				if (first == baseline) continue;
				for (int secondIndex = firstIndex + 1;
					secondIndex < secondaryCandidates.size();
					secondIndex++) {
					Room second = secondaryCandidates.get(secondIndex);
					if (second == baseline) continue;
					List<BukovRaidLayout.Mark> result =
							new ArrayList<>();
					result.add(marksById.get(roomId(baseline)));
					result.add(marksById.get(roomId(first)));
					result.add(marksById.get(roomId(second)));
					if (!extractionsSeparated(layout, result, boss)) {
						continue;
					}
					if (++evaluatedTriplets
							> MAX_EXTRACTION_TRIPLET_EVALUATIONS) {
						return new ArrayList<>();
					}
					if (riskTriplet(
									layout,
									entranceId,
									result,
									raidMode,
									boss) != null) {
						return result;
					}
				}
			}
		}
		return new ArrayList<>();
	}

	private static boolean extractionsSeparated(
			BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> extractions,
			BukovRaidLayout.Mark boss) {
		for (int first = 0; first < extractions.size(); first++) {
			for (int second = first + 1;
				second < extractions.size();
				second++) {
				Integer distance = distances(
						layout,
						extractions.get(first).roomId(),
						boss == null ? null : boss.roomId())
						.get(extractions.get(second).roomId());
				if (distance == null || distance < 3) return false;
			}
		}
		return true;
	}

	private static void assignCoreSemantics(List<Room> rooms, BukovRaidLayout layout,
			Map<String, BukovRaidLayout.Mark> marksById, Map<String, Integer> entranceDistances,
			List<BukovRaidLayout.Mark> spawns, BukovRaidLayout.Mark highValue,
			BukovRaidLayout.Mark boss, List<BukovRaidLayout.Mark> extractions,
			long seed, List<String> diagnostics) {
		for (BukovRaidLayout.Mark spawn : spawns) spawn.zone = BukovRaidLayout.Zone.SPAWN;
		for (BukovRaidLayout.Mark extraction : extractions) {
			extraction.zone = BukovRaidLayout.Zone.EXTRACTION;
		}
		if (highValue != null) {
			highValue.zone = BukovRaidLayout.Zone.HIGH_VALUE;
			highValue.semanticId = "flooded_warehouse";
		}
		if (boss != null) {
			boss.zone = BukovRaidLayout.Zone.BOSS;
			boss.semanticId = "scrap_compactor";
			boss.eliteSpawnAllowed = true;
		}

		Set<String> reserved = reservedIds(spawns, extractions, highValue, boss);
		BukovRaidLayout.Mark maintenance = chooseSemantic(
				rooms, marksById, entranceDistances, reserved, 1, 3, false, seed + 1);
		if (maintenance != null) {
			maintenance.zone = BukovRaidLayout.Zone.LOW_LOOT;
			maintenance.semanticId = "south_maintenance";
			reserved.add(maintenance.roomId());
		}

		BukovRaidLayout.Mark loading = chooseSemantic(
				rooms, marksById, entranceDistances, reserved, 2, Integer.MAX_VALUE, true, seed + 2);
		if (loading != null) {
			loading.zone = BukovRaidLayout.Zone.COMBAT;
			loading.semanticId = "broken_rail_loading";
			reserved.add(loading.roomId());
		}

		BukovRaidLayout.Mark pump = chooseCentralSemantic(
				rooms, marksById, entranceDistances, reserved, seed + 3);
		if (pump != null) {
			pump.zone = BukovRaidLayout.Zone.COMBAT;
			pump.semanticId = "fog_lamp_pump_station";
			reserved.add(pump.roomId());
		}

		BukovRaidLayout.Mark workshop = chooseSemantic(
				rooms, marksById, entranceDistances, reserved, 2, Integer.MAX_VALUE, true, seed + 4);
		if (workshop != null) {
			workshop.zone = BukovRaidLayout.Zone.COMBAT;
			workshop.semanticId = "umbrella_frame_workshop";
		}

		if (maintenance == null || loading == null || pump == null || workshop == null
				|| highValue == null || boss == null) {
			diagnostics.add("COULD_NOT_ASSIGN_ALL_6_SEMANTIC_ROOMS");
		}
	}

	private static void normalizeSupportingZones(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById,
			List<BukovRaidLayout.Mark> spawns,
			List<BukovRaidLayout.Mark> extractions,
			BukovRaidLayout.Mark highValue,
			BukovRaidLayout.Mark boss,
			BukovRaidMode raidMode,
			long seed) {
		Set<String> reserved = reservedIds(spawns, extractions, highValue, boss);
		for (BukovRaidLayout.Mark mark : marksById.values()) {
			if (!mark.semanticId.isEmpty()) reserved.add(mark.roomId());
		}
		for (Room room : rooms) {
			BukovRaidLayout.Mark mark = marksById.get(roomId(room));
			if (!reserved.contains(mark.roomId())) mark.zone = BukovRaidLayout.Zone.TRANSIT;
		}

		int hazardTarget = raidMode.trainingGround() ? 0
				: raidMode == BukovRaidMode.BOSS_CONTRACT ? 3
				: raidMode == BukovRaidMode.QUICK_SWEEP ? 1 : 2;
		int medicalTarget = raidMode.trainingGround() ? 2
				: raidMode == BukovRaidMode.QUICK_SWEEP
				|| raidMode == BukovRaidMode.BOSS_CONTRACT ? 1 : 2;
		int lowLootTarget = raidMode.trainingGround() ? 3
				: raidMode == BukovRaidMode.QUICK_SWEEP ? 4
				: raidMode == BukovRaidMode.SCAVENGER ? 5
				: raidMode == BukovRaidMode.BOSS_CONTRACT ? 4 : 6;
		int combatTarget = raidMode.trainingGround() ? 4
				: raidMode == BukovRaidMode.QUICK_SWEEP ? 3
				: raidMode == BukovRaidMode.SCAVENGER ? 4
				: raidMode == BukovRaidMode.BOSS_CONTRACT ? 6 : 5;

		assignSupportingZone(rooms, marksById, reserved,
				BukovRaidLayout.Zone.SECRET, 1, seed + 11);
		assignSupportingZone(rooms, marksById, reserved,
				BukovRaidLayout.Zone.HAZARD, hazardTarget, seed + 12);
		assignSupportingZone(rooms, marksById, reserved,
				BukovRaidLayout.Zone.MEDICAL, medicalTarget, seed + 13);
		assignSupportingZone(rooms, marksById, reserved,
				BukovRaidLayout.Zone.LOW_LOOT, lowLootTarget, seed + 14);
		assignSupportingZone(rooms, marksById, reserved,
				BukovRaidLayout.Zone.COMBAT, combatTarget, seed + 15);
	}

	private static void assignSupportingZone(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById,
			Set<String> reserved,
			BukovRaidLayout.Zone zone,
			int targetTotal,
			long seed) {
		int existing = 0;
		for (BukovRaidLayout.Mark mark : marksById.values()) {
			if (mark.zone == zone) existing++;
		}
		if (existing >= targetTotal) return;

		List<Room> candidates = new ArrayList<>();
		for (Room room : rooms) {
			String id = roomId(room);
			if (!reserved.contains(id) && !(room instanceof ConnectionRoom)
					&& !room.isEntrance() && !room.isExit()) {
				candidates.add(room);
			}
		}
		Collections.sort(candidates, (first, second) -> {
			boolean firstPreferred = initialZone(first) == zone;
			boolean secondPreferred = initialZone(second) == zone;
			if (firstPreferred != secondPreferred) return firstPreferred ? -1 : 1;
			int degreeOrder = Integer.compare(second.connected.size(), first.connected.size());
			if (degreeOrder != 0) return degreeOrder;
			return Long.compare(stableTie(seed, second), stableTie(seed, first));
		});
		for (Room candidate : candidates) {
			if (existing >= targetTotal) break;
			BukovRaidLayout.Mark mark = marksById.get(roomId(candidate));
			mark.zone = zone;
			reserved.add(mark.roomId());
			existing++;
		}
	}

	private static BukovRaidLayout.Mark chooseSemantic(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, Map<String, Integer> distances,
			Set<String> reserved, int minimumDistance, int maximumDistance,
			boolean needsTwoEntrances, long seed) {
		Room selected = null;
		long selectedScore = Long.MIN_VALUE;
		for (Room room : rooms) {
			String id = roomId(room);
			Integer distance = distances.get(id);
			if (reserved.contains(id) || distance == null || distance < minimumDistance
					|| distance > maximumDistance || room instanceof ConnectionRoom
					|| room instanceof SecretRoom
					|| needsTwoEntrances && room.connected.size() < 2) {
				continue;
			}
			long score = area(room) * 100L + room.connected.size() * 10L + stableTie(seed, room);
			if (score > selectedScore) {
				selected = room;
				selectedScore = score;
			}
		}
		return selected == null ? null : marksById.get(roomId(selected));
	}

	private static BukovRaidLayout.Mark chooseCentralSemantic(List<Room> rooms,
			Map<String, BukovRaidLayout.Mark> marksById, Map<String, Integer> distances,
			Set<String> reserved, long seed) {
		Room selected = null;
		long selectedScore = Long.MIN_VALUE;
		for (Room room : rooms) {
			String id = roomId(room);
			Integer distance = distances.get(id);
			if (reserved.contains(id) || distance == null || distance < 2
					|| room instanceof ConnectionRoom || room instanceof SecretRoom) {
				continue;
			}
			long score = room.connected.size() * 100_000L + area(room) * 100L
					- distance * 10L + stableTie(seed, room);
			if (score > selectedScore) {
				selected = room;
				selectedScore = score;
			}
		}
		return selected == null ? null : marksById.get(roomId(selected));
	}

	private static void addExtractions(BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> extractionMarks,
			long seed,
			BukovRaidMode raidMode) {
		if (extractionMarks.isEmpty()) return;
		layout.extractions.add(ExtractionDefinition.baseline(extractionMarks.get(0).roomId()));
		if (extractionMarks.size() > 1) {
			layout.extractions.add(ExtractionDefinition.conditional(extractionMarks.get(1).roomId()));
		}
		if (extractionMarks.size() > 2) {
			float temporaryStart =
					raidMode.temporaryExtractionStartSeconds(seed);
			layout.extractions.add(ExtractionDefinition.temporary(
					extractionMarks.get(2).roomId(), temporaryStart));
		}
	}

	private static void addRiskRoutes(BukovRaidLayout layout, String entranceId,
			List<BukovRaidLayout.Mark> extractionMarks,
			BukovRaidMode raidMode,
			BukovRaidLayout.Mark boss,
			List<String> diagnostics) {
		RouteTriplet selected = riskTriplet(
				layout,
				entranceId,
				extractionMarks,
				raidMode,
				boss);
		if (selected == null) {
			diagnostics.add("NO_STRICT_RISK_ROUTE_TRIPLET");
			return;
		}
		layout.routes.add(new BukovRaidLayout.Route(
				"safe_long", BukovRaidLayout.RouteRisk.SAFE, selected.safe.roomIds));
		layout.routes.add(new BukovRaidLayout.Route(
				"balanced_mid", BukovRaidLayout.RouteRisk.BALANCED, selected.balanced.roomIds));
		layout.routes.add(new BukovRaidLayout.Route(
				"high_risk_short", BukovRaidLayout.RouteRisk.HIGH_RISK, selected.high.roomIds));
	}

	private static RouteTriplet riskTriplet(
			BukovRaidLayout layout,
			String entranceId,
			List<BukovRaidLayout.Mark> extractionMarks,
			BukovRaidMode raidMode,
			BukovRaidLayout.Mark boss) {
		if (entranceId == null || extractionMarks.size() < 3) return null;
		List<RouteCandidate> candidates = new ArrayList<>();
		Set<String> extractionIds = new HashSet<>();
		for (BukovRaidLayout.Mark extraction : extractionMarks) {
			extractionIds.add(extraction.roomId());
		}
		for (BukovRaidLayout.Mark extraction : extractionMarks) {
			collectRouteCandidates(
					layout,
					entranceId,
					extraction.roomId(),
					extractionIds,
					raidMode.routeDetourAllowance,
					candidates);
		}
		if (candidates.size() < 3) {
			return null;
		}
		Collections.sort(candidates, new Comparator<RouteCandidate>() {
			@Override
			public int compare(RouteCandidate first, RouteCandidate second) {
				return first.signature.compareTo(second.signature);
			}
		});
		return selectRiskTriplet(
				candidates,
				raidMode == BukovRaidMode.BOSS_CONTRACT && boss != null
						? boss.roomId() : null);
	}

	private static void collectRouteCandidates(BukovRaidLayout layout,
			String start, String target, Set<String> extractionIds,
			int detourAllowance,
			List<RouteCandidate> destination) {
		List<String> shortest = shortestPath(layout, start, target);
		if (shortest.isEmpty()) return;
		int maximumRooms = Math.min(
				layout.marks.size(), shortest.size() + detourAllowance);
		List<String> path = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		path.add(start);
		visited.add(start);
		List<List<String>> targetPaths = new ArrayList<>();
		collectSimplePaths(layout, target, extractionIds, maximumRooms,
				path, visited, targetPaths, 128);
		for (List<String> candidate : targetPaths) {
			destination.add(new RouteCandidate(layout, candidate));
		}
	}

	private static void collectSimplePaths(BukovRaidLayout layout,
			String target, Set<String> extractionIds, int maximumRooms,
			List<String> path, Set<String> visited,
			List<List<String>> destination, int limit) {
		if (destination.size() >= limit) return;
		String current = path.get(path.size() - 1);
		if (current.equals(target)) {
			destination.add(new ArrayList<>(path));
			return;
		}
		if (path.size() >= maximumRooms) return;

		List<String> neighbours = layout.neighbours(current, false);
		Collections.sort(neighbours);
		for (String neighbour : neighbours) {
			if (destination.size() >= limit) return;
			if (visited.contains(neighbour)
					|| extractionIds.contains(neighbour) && !target.equals(neighbour)) {
				continue;
			}
			visited.add(neighbour);
			path.add(neighbour);
			collectSimplePaths(layout, target, extractionIds, maximumRooms,
					path, visited, destination, limit);
			path.remove(path.size() - 1);
			visited.remove(neighbour);
		}
	}

	private static RouteTriplet selectRiskTriplet(
			List<RouteCandidate> candidates, String requiredHighRoomId) {
		RouteTriplet best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (RouteCandidate safe : candidates) {
			for (RouteCandidate high : candidates) {
				if (requiredHighRoomId != null
						&& !high.roomIds.contains(requiredHighRoomId)) {
					continue;
				}
				if (high.hops >= safe.hops || high.threat <= safe.threat) continue;
				for (RouteCandidate balanced : candidates) {
					if (balanced.threat <= safe.threat
							|| balanced.threat >= high.threat) {
						continue;
					}
					float middle = (safe.threat + high.threat) * 0.5f;
					double score = (high.threat - safe.threat) * 100_000d
							+ (safe.hops - high.hops) * 1_000d
							- Math.abs(balanced.threat - middle) * 100d;
					if (score > bestScore) {
						bestScore = score;
						best = new RouteTriplet(safe, balanced, high);
					}
				}
			}
		}
		return best;
	}

	private static final class RouteCandidate {
		final List<String> roomIds;
		final int hops;
		final float threat;
		final String signature;

		RouteCandidate(BukovRaidLayout layout, List<String> roomIds) {
			this.roomIds = roomIds;
			hops = roomIds.size() - 1;
			threat = BukovRouteMetrics.averageThreat(layout, roomIds);
			signature =
					com.shatteredpixel.shatteredpixeldungeon.bukov.util.BukovStrings.join(
							">", roomIds);
		}
	}

	private static final class RouteTriplet {
		final RouteCandidate safe;
		final RouteCandidate balanced;
		final RouteCandidate high;

		RouteTriplet(RouteCandidate safe, RouteCandidate balanced, RouteCandidate high) {
			this.safe = safe;
			this.balanced = balanced;
			this.high = high;
		}
	}

	private static List<String> shortestPath(
			BukovRaidLayout layout, String start, String target) {
		Map<String, String> previous = new HashMap<>();
		Queue<String> pending = new ArrayDeque<>();
		previous.put(start, null);
		pending.add(start);
		while (!pending.isEmpty()) {
			String current = pending.remove();
			if (current.equals(target)) break;
			List<String> neighbours = layout.neighbours(current, false);
			Collections.sort(neighbours);
			for (String neighbour : neighbours) {
				if (!previous.containsKey(neighbour)) {
					previous.put(neighbour, current);
					pending.add(neighbour);
				}
			}
		}
		if (!previous.containsKey(target)) return Collections.emptyList();
		List<String> result = new ArrayList<>();
		for (String current = target; current != null; current = previous.get(current)) {
			result.add(current);
		}
		Collections.reverse(result);
		return result;
	}

	private static Map<String, Integer> distances(
			BukovRaidLayout layout, String start, String excludedRoom) {
		Map<String, Integer> result = new HashMap<>();
		if (start == null || start.equals(excludedRoom)) return result;
		Queue<String> pending = new ArrayDeque<>();
		result.put(start, 0);
		pending.add(start);
		while (!pending.isEmpty()) {
			String current = pending.remove();
			for (String neighbour : layout.neighbours(current, false)) {
				if (!neighbour.equals(excludedRoom) && !result.containsKey(neighbour)) {
					result.put(neighbour, result.get(current) + 1);
					pending.add(neighbour);
				}
			}
		}
		return result;
	}

	private static float averageTraversalSeconds(
			BukovRaidLayout layout) {
		if (layout.links.isEmpty()) return 0f;
		float total = 0f;
		for (BukovRaidLayout.Link link : layout.links) {
			total += link.traversalSeconds;
		}
		return total / layout.links.size();
	}

	private static int minimumDistanceFromSpawns(
			BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> spawns,
			String targetRoomId) {
		int result = Integer.MAX_VALUE;
		for (BukovRaidLayout.Mark spawn : spawns) {
			Integer distance = distances(
					layout, spawn.roomId(), null).get(targetRoomId);
			if (distance == null) return -1;
			result = Math.min(result, distance);
		}
		return result == Integer.MAX_VALUE ? -1 : result;
	}

	private static int maximumDistanceFromSpawns(
			BukovRaidLayout layout,
			List<BukovRaidLayout.Mark> spawns,
			String targetRoomId) {
		int result = -1;
		for (BukovRaidLayout.Mark spawn : spawns) {
			Integer distance = distances(
					layout, spawn.roomId(), null).get(targetRoomId);
			if (distance == null) return Integer.MAX_VALUE;
			result = Math.max(result, distance);
		}
		return result;
	}

	private static Room findSpecialRoom(List<Room> rooms, boolean entrance) {
		for (Room room : rooms) {
			if (entrance ? room.isEntrance() : room.isExit()) return room;
		}
		return null;
	}

	private static Set<String> reservedIds(List<BukovRaidLayout.Mark> spawns,
			List<BukovRaidLayout.Mark> extractions, BukovRaidLayout.Mark highValue,
			BukovRaidLayout.Mark boss) {
		Set<String> result = new HashSet<>();
		for (BukovRaidLayout.Mark mark : spawns) result.add(mark.roomId());
		for (BukovRaidLayout.Mark mark : extractions) result.add(mark.roomId());
		if (highValue != null) result.add(highValue.roomId());
		if (boss != null) result.add(boss.roomId());
		return result;
	}

	private static boolean containsAny(String value, String... fragments) {
		for (String fragment : fragments) {
			if (value.contains(fragment)) return true;
		}
		return false;
	}

	private static int minimumDimension(Room room) {
		return Math.min(room.width(), room.height());
	}

	private static int area(Room room) {
		return room.width() * room.height();
	}

	private static long stableTie(long seed, Room room) {
		long value = seed ^ roomId(room).hashCode();
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.floorMod(value, 10L);
	}

	private static String roomId(Room room) {
		return room.left + "," + room.top + "," + room.right + "," + room.bottom;
	}
}
