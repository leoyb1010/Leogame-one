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

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Converts room-level raid semantics into stable cells after terrain painting.
 *
 * The planner reads the finished map only. It never changes host rooms or
 * terrain and therefore produces the same anchors for the same seed and map.
 */
public final class BukovAnchorPlanner {

	public static final class Result {
		public final boolean valid;
		public final String reason;

		private Result(boolean valid, String reason) {
			this.valid = valid;
			this.reason = reason;
		}

		public static Result valid() {
			return new Result(true, "VALID");
		}

		public static Result invalid(String reason) {
			return new Result(false, reason);
		}
	}

	private BukovAnchorPlanner() {
	}

	public static Result assign(int width, int height, int[] map,
			BukovRaidLayout layout, int entranceCell, int baselineExitCell) {
		Result surface = validateSurface(width, height, map, layout);
		if (!surface.valid) return surface;
		if (layout.extractions.size() != 3) {
			return Result.invalid("Expected exactly three extraction definitions");
		}

		int[] extractionCells = new int[layout.extractions.size()];
		Set<Integer> occupied = new HashSet<>();
		for (int i = 0; i < layout.extractions.size(); i++) {
			ExtractionDefinition extraction = layout.extractions.get(i);
			BukovRaidLayout.Mark mark = layout.mark(extraction.roomId);
			if (mark == null) return Result.invalid("Missing extraction room " + extraction.roomId);

			int cell = -1;
			if ("E01".equals(extraction.id)
					&& extraction.type == ExtractionDefinition.Type.BASELINE
					&& baselineExitCell != entranceCell
					&& usableInside(width, height, map, mark, baselineExitCell, true)) {
				cell = baselineExitCell;
			}
			if (cell < 0) {
				cell = ExtractionCellSelector.select(
						width, height, map, mark, excluded(occupied, entranceCell));
			}
			if (cell < 0) {
				return Result.invalid("No walkable cell for extraction " + extraction.id);
			}
			extractionCells[i] = cell;
			occupied.add(cell);
		}

		List<BukovRaidLayout.Mark> lootRooms = new ArrayList<>();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (lootEligible(mark.zone)) lootRooms.add(mark);
		}
		Collections.sort(lootRooms, new Comparator<BukovRaidLayout.Mark>() {
			@Override
			public int compare(
					BukovRaidLayout.Mark first,
					BukovRaidLayout.Mark second) {
				int byPriority = Integer.compare(
						lootPriority(first.zone),
						lootPriority(second.zone));
				if (byPriority != 0) return byPriority;
				int byRank = Long.compare(
						stableRank(layout.seed, first.roomId()),
						stableRank(layout.seed, second.roomId()));
				if (byRank != 0) return byRank;
				return first.roomId().compareTo(second.roomId());
			}
		});

		List<BukovRaidLayout.LootAnchor> plannedLoot = new ArrayList<>();
		Set<String> usedRooms = new HashSet<>();
		Set<BukovRaidLayout.Zone> usedZones = new HashSet<>();
		for (int pass = 0; pass < 2 && plannedLoot.size() < 3; pass++) {
			for (BukovRaidLayout.Mark mark : lootRooms) {
				if (plannedLoot.size() == 3) break;
				if (usedRooms.contains(mark.roomId())
						|| pass == 0 && usedZones.contains(mark.zone)) {
					continue;
				}
				int cell = ExtractionCellSelector.select(
						width, height, map, mark, excluded(occupied, entranceCell));
				if (cell < 0) continue;
				usedRooms.add(mark.roomId());
				usedZones.add(mark.zone);
				occupied.add(cell);
				int ordinal = plannedLoot.size() + 1;
				BukovRaidLayout.LootAnchor anchor = new BukovRaidLayout.LootAnchor(
						String.format("L%02d", ordinal),
						mark.roomId(),
						cell,
						cell % width,
						cell / width);
				anchor.lootTableId = lootTable(mark.zone);
				anchor.searchSeconds = searchSeconds(mark.zone);
				plannedLoot.add(anchor);
			}
		}
		if (plannedLoot.size() != 3) {
			return Result.invalid("Could not place three distinct loot anchors");
		}

		for (int i = 0; i < layout.extractions.size(); i++) {
			ExtractionDefinition extraction = layout.extractions.get(i);
			extraction.interactionCell = extractionCells[i];
			extraction.interactionX = extractionCells[i] % width;
			extraction.interactionY = extractionCells[i] / width;
		}
		layout.lootAnchors.clear();
		layout.lootAnchors.addAll(plannedLoot);
		BukovRaidLayout.MissionGate missionGate =
				planMissionGate(width, height, map, layout, occupied);
		if (missionGate == null) {
			return Result.invalid(
					"Could not place maintenance archive and locked passage");
		}
		layout.missionGate(missionGate);
		occupied.add(missionGate.archiveCell);
		for (int gateCell : missionGate.gateCells) occupied.add(gateCell);
		BukovRaidLayout.BossMechanism bossMechanism =
				planBossMechanism(width, height, map, layout, occupied);
		if (bossMechanism == null) {
			return Result.invalid(
					"Could not place White Line body traces and fog lamp");
		}
		layout.bossMechanism(bossMechanism);
		if (entranceCell >= 0) {
			Result lockedTraversal = validateLockedMissionTraversal(
					width, height, map, layout, entranceCell);
			if (!lockedTraversal.valid) return lockedTraversal;
		}
		return validate(width, height, map, layout);
	}

	/**
	 * Adds the new boss anchors to an older otherwise-valid raid save without
	 * rerolling any extraction, loot or mission cells.
	 */
	public static Result ensureBossMechanism(
			int width,
			int height,
			int[] map,
			BukovRaidLayout layout) {
		Result surface = validateSurface(width, height, map, layout);
		if (!surface.valid) return surface;
		if (layout.bossMechanism() != null) return Result.valid();

		Set<Integer> occupied = new HashSet<>();
		for (ExtractionDefinition extraction : layout.extractions) {
			if (extraction.interactionCell >= 0) {
				occupied.add(extraction.interactionCell);
			}
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			if (anchor.cell >= 0) occupied.add(anchor.cell);
		}
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		if (gate != null) {
			if (gate.archiveCell >= 0) occupied.add(gate.archiveCell);
			for (int cell : gate.gateCells) occupied.add(cell);
		}
		BukovRaidLayout.BossMechanism planned =
				planBossMechanism(width, height, map, layout, occupied);
		if (planned == null) {
			return Result.invalid(
					"Could not migrate White Line scene anchors");
		}
		layout.bossMechanism(planned);
		return Result.valid();
	}

	/**
	 * Proves the first-raid gate cannot turn deployment into a sealed spawn
	 * room. With G01 closed, Q01 and a useful part of the raid must remain
	 * reachable; with G01 open, it must reveal at least one additional cell.
	 */
	public static Result validateLockedMissionTraversal(
			int width,
			int height,
			int[] map,
			BukovRaidLayout layout,
			int entranceCell) {
		if (layout == null || entranceCell < 0 || entranceCell >= map.length
				|| !terrainPassable(map[entranceCell])) {
			return Result.invalid("Invalid Bukov deployment cell");
		}
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		if (gate == null || gate.gateCells == null || gate.gateCells.length == 0) {
			return Result.invalid("Missing first-raid locked passage");
		}

		Set<Integer> blocked = new HashSet<>();
		for (int cell : gate.gateCells) blocked.add(cell);
		boolean[] closedReachable = flood(
				width, height, map, entranceCell, blocked,
				Collections.<Integer>emptySet());
		if (gate.archiveCell < 0 || gate.archiveCell >= closedReachable.length
				|| !closedReachable[gate.archiveCell]) {
			return Result.invalid("Q01 archive is unreachable while G01 is locked");
		}

		int reachableCells = countTrue(closedReachable);
		if (reachableCells < 64) {
			return Result.invalid(
					"G01 traps deployment in a tiny area: " + reachableCells);
		}
		int reachablePlayableRooms = 0;
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (mark.structuralTransit
					|| mark.zone == BukovRaidLayout.Zone.SPAWN
					|| mark.roomId().equals(gate.gateRoomId)) {
				continue;
			}
			if (roomHasReachableCell(width, height, map, mark, closedReachable)) {
				reachablePlayableRooms++;
			}
		}
		if (reachablePlayableRooms < 4) {
			return Result.invalid(
					"G01 leaves too little main raid area reachable: "
							+ reachablePlayableRooms);
		}

		boolean[] openReachable = flood(
				width, height, map, entranceCell,
				Collections.<Integer>emptySet(), blocked);
		int postObjectiveCells = 0;
		for (int cell = 0; cell < openReachable.length; cell++) {
			if (openReachable[cell] && !closedReachable[cell]
					&& !blocked.contains(cell)) {
				postObjectiveCells++;
			}
		}
		if (postObjectiveCells < 8) {
			return Result.invalid(
					"G01 does not guard a meaningful post-objective area: "
							+ postObjectiveCells);
		}
		return Result.valid();
	}

	public static Result validate(int width, int height, int[] map, BukovRaidLayout layout) {
		Result surface = validateSurface(width, height, map, layout);
		if (!surface.valid) return surface;
		if (layout.extractions.size() != 3) {
			return Result.invalid("Expected exactly three extraction definitions");
		}
		if (layout.lootAnchors.size() != 3) {
			return Result.invalid("Expected exactly three loot anchors");
		}

		Set<String> extractionIds = new HashSet<>();
		Set<String> extractionRooms = new HashSet<>();
		Set<Integer> occupied = new HashSet<>();
		for (ExtractionDefinition extraction : layout.extractions) {
			BukovRaidLayout.Mark mark = layout.mark(extraction.roomId);
			if (!extractionIds.add(extraction.id)
					|| !extractionRooms.add(extraction.roomId)
					|| !validStoredCell(width, height, map, mark,
							extraction.interactionCell,
							extraction.interactionX,
							extraction.interactionY,
							true)
					|| !occupied.add(extraction.interactionCell)) {
				return Result.invalid("Invalid extraction anchor " + extraction.id);
			}
		}
		if (!extractionIds.contains("E01")
				|| !extractionIds.contains("E02")
				|| !extractionIds.contains("E03")) {
			return Result.invalid("Missing E01, E02 or E03");
		}

		Set<String> lootIds = new HashSet<>();
		Set<String> lootRooms = new HashSet<>();
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			BukovRaidLayout.Mark mark = layout.mark(anchor.roomId);
			if (!lootIds.add(anchor.id)
					|| !lootRooms.add(anchor.roomId)
					|| mark == null
					|| !lootEligible(mark.zone)
					|| anchor.lootTableId.isEmpty()
					|| anchor.searchSeconds <= 0f
					|| !validStoredCell(width, height, map, mark,
							anchor.cell, anchor.x, anchor.y, false)
					|| !occupied.add(anchor.cell)) {
				return Result.invalid("Invalid loot anchor " + anchor.id);
			}
		}
		if (!lootIds.contains("L01")
				|| !lootIds.contains("L02")
				|| !lootIds.contains("L03")) {
			return Result.invalid("Missing L01, L02 or L03");
		}
		BukovRaidLayout.MissionGate missionGate = layout.missionGate();
		BukovRaidLayout.Mark archiveRoom = missionGate == null
				? null : layout.mark(missionGate.archiveRoomId);
		BukovRaidLayout.Mark gateRoom = missionGate == null
				? null : layout.mark(missionGate.gateRoomId);
		if (missionGate == null
				|| !FirstRaidMission.ARCHIVE_ANCHOR_ID.equals(
						missionGate.archiveAnchorId)
				|| !FirstRaidMission.GATE_ID.equals(missionGate.gateId)
				|| !FirstRaidMission.EVENT_ID.equals(
						missionGate.requiredEvent)
				|| archiveRoom == null
				|| !"south_maintenance".equals(archiveRoom.semanticId)
				|| gateRoom == null
				|| !"fog_lamp_pump_station".equals(gateRoom.semanticId)
				|| !validStoredCell(
						width, height, map, archiveRoom,
						missionGate.archiveCell,
						missionGate.archiveX,
						missionGate.archiveY,
						false)
				|| !validGateCells(width, height, map, gateRoom, missionGate)
				|| !occupied.add(missionGate.archiveCell)
				|| !addAllDistinct(occupied, missionGate.gateCells)) {
			return Result.invalid("Invalid first-raid mission gate");
		}
		BukovRaidLayout.BossMechanism bossMechanism =
				layout.bossMechanism();
		BukovRaidLayout.Mark bossRoom = bossMechanism == null
				? null : layout.mark(bossMechanism.bossRoomId);
		BukovRaidLayout.Mark fogLampRoom = bossMechanism == null
				? null : layout.mark(bossMechanism.fogLampRoomId);
		if (bossMechanism == null
				|| bossRoom == null
				|| bossRoom.zone != BukovRaidLayout.Zone.BOSS
				|| !"scrap_compactor".equals(bossRoom.semanticId)
				|| fogLampRoom == null
				|| !"fog_lamp_pump_station".equals(
						fogLampRoom.semanticId)
				|| !"fog_lamp_pump_station".equals(
						bossMechanism.fogLampAnchorId)
				|| bossMechanism.bodyTraceCells == null
				|| bossMechanism.bodyTraceCells.length != 4
				|| !validStoredCell(
						width, height, map, fogLampRoom,
						bossMechanism.fogLampCell,
						bossMechanism.fogLampX,
						bossMechanism.fogLampY,
						false)
				|| !occupied.add(bossMechanism.fogLampCell)) {
			return Result.invalid("Invalid White Line scene anchors");
		}
		for (int bodyCell : bossMechanism.bodyTraceCells) {
			if (!validBodyTraceCell(
						width, height, map, bossRoom, bodyCell)
					|| !occupied.add(bodyCell)) {
				return Result.invalid("Invalid White Line body trace");
			}
		}
		return Result.valid();
	}

	private static BukovRaidLayout.BossMechanism planBossMechanism(
			int width,
			int height,
			int[] map,
			BukovRaidLayout layout,
			Set<Integer> occupied) {
		BukovRaidLayout.Mark bossRoom =
				semanticMark(layout, "scrap_compactor");
		BukovRaidLayout.Mark fogLampRoom =
				semanticMark(layout, "fog_lamp_pump_station");
		if (bossRoom == null
				|| bossRoom.zone != BukovRaidLayout.Zone.BOSS
				|| fogLampRoom == null) {
			return null;
		}

		int fogLampCell = ExtractionCellSelector.select(
				width, height, map, fogLampRoom, excluded(occupied, -1));
		if (fogLampCell < 0) return null;
		Set<Integer> sceneOccupied = new HashSet<>(occupied);
		sceneOccupied.add(fogLampCell);

		int[] bodyCells = selectSpreadBodyTraces(
				width, height, map, bossRoom, sceneOccupied, layout.seed);
		if (bodyCells.length != 4) return null;

		BukovRaidLayout.BossMechanism result =
				new BukovRaidLayout.BossMechanism();
		result.bossRoomId = bossRoom.roomId();
		result.bodyTraceCells = bodyCells;
		result.fogLampRoomId = fogLampRoom.roomId();
		result.fogLampCell = fogLampCell;
		result.fogLampX = fogLampCell % width;
		result.fogLampY = fogLampCell / width;
		return result;
	}

	private static int[] selectSpreadBodyTraces(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			Set<Integer> occupied,
			long seed) {
		List<Integer> candidates = new ArrayList<>();
		for (int y = Math.max(1, room.top);
				y <= Math.min(height - 2, room.bottom); y++) {
			for (int x = Math.max(1, room.left);
					x <= Math.min(width - 2, room.right); x++) {
				int cell = x + y * width;
				if (!occupied.contains(cell)
						&& terrainPassable(map[cell])) {
					candidates.add(cell);
				}
			}
		}
		if (candidates.size() < 4) return new int[0];

		int[] selected = new int[4];
		for (int index = 0; index < selected.length; index++) {
			int bestCell = -1;
			int bestSeparation = -1;
			long bestRank = Long.MAX_VALUE;
			for (Integer candidate : candidates) {
				boolean alreadySelected = false;
				int minimumSeparation = Integer.MAX_VALUE;
				for (int prior = 0; prior < index; prior++) {
					if (selected[prior] == candidate) {
						alreadySelected = true;
						break;
					}
					int dx = selected[prior] % width - candidate % width;
					int dy = selected[prior] / width - candidate / width;
					minimumSeparation = Math.min(
							minimumSeparation, dx * dx + dy * dy);
				}
				if (alreadySelected) continue;
				if (index == 0) minimumSeparation = Integer.MAX_VALUE;
				long rank = stableRank(
						seed ^ candidate,
						room.roomId() + ":white-line:" + index);
				if (minimumSeparation > bestSeparation
						|| minimumSeparation == bestSeparation
								&& rank < bestRank) {
					bestCell = candidate;
					bestSeparation = minimumSeparation;
					bestRank = rank;
				}
			}
			if (bestCell < 0) return new int[0];
			selected[index] = bestCell;
		}
		return selected;
	}

	private static boolean validBodyTraceCell(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			int cell) {
		return cell >= 0
				&& cell < map.length
				&& cell % width >= room.left
				&& cell % width <= room.right
				&& cell / width >= room.top
				&& cell / width <= room.bottom
				&& terrainPassable(map[cell]);
	}

	private static BukovRaidLayout.MissionGate planMissionGate(
			int width,
			int height,
			int[] map,
			BukovRaidLayout layout,
			Set<Integer> occupied) {
		BukovRaidLayout.Mark archiveRoom =
				semanticMark(layout, "south_maintenance");
		BukovRaidLayout.Mark gateRoom =
				semanticMark(layout, "fog_lamp_pump_station");
		if (archiveRoom == null || gateRoom == null
				|| archiveRoom.roomId().equals(gateRoom.roomId())) {
			return null;
		}
		int archiveCell = ExtractionCellSelector.select(
				width, height, map, archiveRoom, excluded(occupied, -1));
		if (archiveCell < 0) return null;

		Set<Integer> gateExcluded = new HashSet<>(occupied);
		gateExcluded.add(archiveCell);
		int[] gateCells = selectGateCells(
				width, height, map, gateRoom, gateExcluded, layout.seed);
		if (gateCells.length == 0) return null;
		int gateCell = gateCells[0];

		BukovRaidLayout.MissionGate result =
				new BukovRaidLayout.MissionGate();
		result.archiveRoomId = archiveRoom.roomId();
		result.archiveCell = archiveCell;
		result.archiveX = archiveCell % width;
		result.archiveY = archiveCell / width;
		result.gateRoomId = gateRoom.roomId();
		result.gateCell = gateCell;
		result.gateCells = gateCells;
		result.gateX = gateCell % width;
		result.gateY = gateCell / width;
		return result;
	}

	private static BukovRaidLayout.Mark semanticMark(
			BukovRaidLayout layout,
			String semanticId) {
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (semanticId.equals(mark.semanticId)) return mark;
		}
		return null;
	}

	private static int[] selectGateCells(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			Set<Integer> excluded,
			long seed) {
		List<Integer> selected = new ArrayList<>();
		for (int y = room.top; y <= room.bottom; y++) {
			for (int x = room.left; x <= room.right; x++) {
				if (x != room.left && x != room.right
						&& y != room.top && y != room.bottom) {
					continue;
				}
				int cell = x + y * width;
				if (x <= 0 || x >= width - 1
						|| y <= 0 || y >= height - 1
						|| cell < 0 || cell >= map.length
						|| map[cell] < 0
						|| map[cell] >= Terrain.flags.length
						|| (Terrain.flags[map[cell]] & Terrain.PASSABLE) == 0
						&& map[cell] != Terrain.LOCKED_DOOR) {
					continue;
				}
				// Every passable boundary cell is part of the gate. Skipping a
				// second doorway would turn the mission into optional flavour.
				if (excluded.contains(cell)) return new int[0];
				selected.add(cell);
			}
		}
		Collections.sort(selected, (first, second) -> Long.compare(
				stableRank(seed ^ first, room.roomId()),
				stableRank(seed ^ second, room.roomId())));
		int[] result = new int[selected.size()];
		for (int i = 0; i < result.length; i++) result[i] = selected.get(i);
		return result;
	}

	private static boolean validGateCells(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			BukovRaidLayout.MissionGate gate) {
		if (gate.gateCells == null || gate.gateCells.length == 0
				|| gate.gateCell != gate.gateCells[0]) {
			return false;
		}
		Set<Integer> stored = new HashSet<>();
		for (int cell : gate.gateCells) {
			if (!stored.add(cell)
					|| !validGateCell(width, height, map, room, cell)) {
				return false;
			}
		}
		Set<Integer> boundaryPassages = new HashSet<>();
		for (int y = room.top; y <= room.bottom; y++) {
			for (int x = room.left; x <= room.right; x++) {
				if (x != room.left && x != room.right
						&& y != room.top && y != room.bottom) continue;
				int cell = x + y * width;
				if (cell >= 0 && cell < map.length
						&& map[cell] >= 0
						&& map[cell] < Terrain.flags.length
						&& ((Terrain.flags[map[cell]] & Terrain.PASSABLE) != 0
						|| map[cell] == Terrain.LOCKED_DOOR)) {
					boundaryPassages.add(cell);
				}
			}
		}
		return stored.equals(boundaryPassages)
				&& gate.gateX == gate.gateCell % width
				&& gate.gateY == gate.gateCell / width;
	}

	private static boolean[] flood(
			int width,
			int height,
			int[] map,
			int start,
			Set<Integer> blocked,
			Set<Integer> forcedPassable) {
		boolean[] reached = new boolean[map.length];
		if (blocked.contains(start)
				|| (!terrainPassable(map[start])
						&& !forcedPassable.contains(start))) {
			return reached;
		}
		Queue<Integer> pending = new ArrayDeque<>();
		reached[start] = true;
		pending.add(start);
		while (!pending.isEmpty()) {
			int cell = pending.remove();
			int x = cell % width;
			int y = cell / width;
			if (x > 0) enqueuePassable(
					cell - 1, map, blocked, forcedPassable, reached, pending);
			if (x + 1 < width) enqueuePassable(
					cell + 1, map, blocked, forcedPassable, reached, pending);
			if (y > 0) enqueuePassable(
					cell - width, map, blocked, forcedPassable, reached, pending);
			if (y + 1 < height) enqueuePassable(
					cell + width, map, blocked, forcedPassable, reached, pending);
		}
		return reached;
	}

	private static void enqueuePassable(
			int cell,
			int[] map,
			Set<Integer> blocked,
			Set<Integer> forcedPassable,
			boolean[] reached,
			Queue<Integer> pending) {
		if (!reached[cell] && !blocked.contains(cell)
				&& (terrainPassable(map[cell])
						|| forcedPassable.contains(cell))) {
			reached[cell] = true;
			pending.add(cell);
		}
	}

	private static boolean terrainPassable(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private static boolean roomHasReachableCell(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			boolean[] reachable) {
		int left = Math.max(0, room.left);
		int right = Math.min(width - 1, room.right);
		int top = Math.max(0, room.top);
		int bottom = Math.min(height - 1, room.bottom);
		for (int y = top; y <= bottom; y++) {
			for (int x = left; x <= right; x++) {
				int cell = x + y * width;
				if (reachable[cell] && terrainPassable(map[cell])) return true;
			}
		}
		return false;
	}

	private static int countTrue(boolean[] values) {
		int result = 0;
		for (boolean value : values) if (value) result++;
		return result;
	}

	private static boolean validGateCell(
			int width,
			int height,
			int[] map,
			BukovRaidLayout.Mark room,
			int cell) {
		if (cell < 0 || cell >= map.length) {
			return false;
		}
		int x = cell % width;
		int y = cell / width;
		if (x <= 0 || x >= width - 1 || y <= 0 || y >= height - 1
				|| x < room.left || x > room.right
				|| y < room.top || y > room.bottom
				|| x != room.left && x != room.right
				&& y != room.top && y != room.bottom) {
			return false;
		}
		int terrain = map[cell];
		return terrain == Terrain.LOCKED_DOOR
				|| terrain == Terrain.OPEN_DOOR
				|| terrain == Terrain.DOOR
				|| terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private static boolean addAllDistinct(
			Set<Integer> occupied, int[] cells) {
		if (cells == null || cells.length == 0) return false;
		for (int cell : cells) {
			if (!occupied.add(cell)) return false;
		}
		return true;
	}

	private static Result validateSurface(
			int width, int height, int[] map, BukovRaidLayout layout) {
		if (width <= 0 || height <= 0 || map == null || map.length != width * height) {
			return Result.invalid("Invalid map dimensions");
		}
		if (layout == null) return Result.invalid("Raid layout is required");
		return Result.valid();
	}

	private static boolean validStoredCell(int width, int height, int[] map,
			BukovRaidLayout.Mark mark, int cell, int x, int y, boolean allowBoundary) {
		return cell >= 0
				&& cell < map.length
				&& x == cell % width
				&& y == cell / width
				&& usableInside(width, height, map, mark, cell, allowBoundary);
	}

	private static boolean usableInside(int width, int height, int[] map,
			BukovRaidLayout.Mark mark, int cell, boolean allowBoundary) {
		if (mark == null || cell < 0 || cell >= map.length) return false;
		int x = cell % width;
		int y = cell / width;
		boolean inside = allowBoundary
				? x >= mark.left && x <= mark.right && y >= mark.top && y <= mark.bottom
				: x > mark.left && x < mark.right && y > mark.top && y < mark.bottom;
		return inside
				&& x > 0 && x < width - 1
				&& y > 0 && y < height - 1
				&& map[cell] >= 0
				&& map[cell] < Terrain.flags.length
				&& (Terrain.flags[map[cell]] & Terrain.PASSABLE) != 0;
	}

	private static boolean lootEligible(BukovRaidLayout.Zone zone) {
		return zone != BukovRaidLayout.Zone.SPAWN
				&& zone != BukovRaidLayout.Zone.BOSS
				&& zone != BukovRaidLayout.Zone.EXTRACTION;
	}

	private static int lootPriority(BukovRaidLayout.Zone zone) {
		switch (zone) {
			case HIGH_VALUE:
				return 0;
			case SECRET:
				return 1;
			case LOW_LOOT:
				return 2;
			case MEDICAL:
				return 3;
			case COMBAT:
				return 4;
			case HAZARD:
				return 5;
			default:
				return 6;
		}
	}

	private static String lootTable(BukovRaidLayout.Zone zone) {
		switch (zone) {
			case HIGH_VALUE:
				return "high_value";
			case SECRET:
				return "high_value";
			case MEDICAL:
				return "medical";
			case LOW_LOOT:
				return "low";
			default:
				return "industrial";
		}
	}

	private static float searchSeconds(BukovRaidLayout.Zone zone) {
		switch (zone) {
			case HIGH_VALUE:
				return 4.5f;
			case SECRET:
				return 5f;
			case MEDICAL:
				return 3f;
			case LOW_LOOT:
				return 2.5f;
			default:
				return 3.5f;
		}
	}

	private static long stableRank(long seed, String roomId) {
		long value = seed ^ roomId.hashCode();
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		value ^= value >>> 33;
		return value & Long.MAX_VALUE;
	}

	private static int[] excluded(Set<Integer> occupied, int extraCell) {
		int[] result = new int[occupied.size() + (extraCell >= 0 ? 1 : 0)];
		int index = 0;
		for (Integer cell : occupied) result[index++] = cell;
		if (extraCell >= 0) result[index] = extraCell;
		return result;
	}
}
