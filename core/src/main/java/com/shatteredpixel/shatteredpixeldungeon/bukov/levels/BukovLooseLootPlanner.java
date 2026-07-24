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

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Places a very small authored loose-loot trail in the first raid.
 *
 * Containers remain the main source of loot. This readable starter trail
 * guarantees that even a future cloth-run mode can recover a real Needle-9
 * plus two compatible ammunition stacks before the first container search.
 */
public final class BukovLooseLootPlanner {

	public static final int REQUIRED_PLACEMENT_COUNT = 5;
	public static final int BASE_PLACEMENT_COUNT = 2;
	public static final int MINIMUM_DISTANCE_FROM_DEPLOYMENT = 3;
	public static final int INTRODUCTION_RADIUS = 12;
	public static final int TRAINING_INTRODUCTION_RADIUS = 10;
	public static final int GROUND_STARTER_RADIUS = 6;

	public enum Kind {
		WEAPON,
		AMMUNITION,
		RESERVE_AMMUNITION,
		MEDICAL,
		SALVAGE
	}

	public static final class Placement {
		public final Kind kind;
		public final int cell;
		public final int distanceFromDeployment;
		public final String roomId;

		private Placement(
				Kind kind,
				int cell,
				int distanceFromDeployment,
				String roomId) {
			this.kind = kind;
			this.cell = cell;
			this.distanceFromDeployment = distanceFromDeployment;
			this.roomId = roomId;
		}
	}

	private static final class Candidate {
		final int cell;
		final int distance;
		final BukovRaidLayout.Mark mark;

		Candidate(int cell, int distance, BukovRaidLayout.Mark mark) {
			this.cell = cell;
			this.distance = distance;
			this.mark = mark;
		}
	}

	private BukovLooseLootPlanner() {
	}

	public static List<Placement> plan(
			int width,
			int height,
			boolean[] passable,
			BukovRaidLayout layout,
			int entranceCell) {
		return plan(
				width,
				height,
				passable,
				layout,
				entranceCell,
				BukovRaidMode.EXPEDITION);
	}

	public static List<Placement> plan(
			int width,
			int height,
			boolean[] passable,
			BukovRaidLayout layout,
			int entranceCell,
			BukovRaidMode raidMode) {
		return plan(
				width,
				height,
				passable,
				layout,
				entranceCell,
				raidMode,
				-1,
				true);
	}

	/**
	 * Reserves a stable optional-container cell before authoring loose loot.
	 * The value is derived from immutable layout data, so host heap pickup/drop
	 * state can never move the maintenance cache on resume.
	 */
	public static List<Placement> plan(
			int width,
			int height,
			boolean[] passable,
			BukovRaidLayout layout,
			int entranceCell,
			BukovRaidMode raidMode,
			int optionalReservedCell) {
		return plan(
				width,
				height,
				passable,
				layout,
				entranceCell,
				raidMode,
				optionalReservedCell,
				true);
	}

	public static List<Placement> plan(
			int width,
			int height,
			boolean[] passable,
			BukovRaidLayout layout,
			int entranceCell,
			BukovRaidMode raidMode,
			int optionalReservedCell,
			boolean groundStarterKitRequired) {
		if (width <= 0 || height <= 0
				|| passable == null || passable.length != width * height
				|| layout == null
				|| raidMode == null
				|| entranceCell < 0 || entranceCell >= passable.length
				|| !passable[entranceCell]) {
			throw new IllegalArgumentException(
					"A valid generated Bukov surface is required");
		}

		int[] distances = distances(width, height, passable, entranceCell);
		BukovRaidLayout.Mark deploymentMark =
				containingMark(layout, width, entranceCell);
		if (deploymentMark == null) {
			throw new IllegalArgumentException(
					"Deployment cell must belong to an authored room");
		}
		int introductionRadius = raidMode.trainingGround()
				? TRAINING_INTRODUCTION_RADIUS : INTRODUCTION_RADIUS;
		Set<Integer> occupied = reservedCells(layout, entranceCell);
		if (optionalReservedCell >= 0) {
			if (optionalReservedCell >= passable.length
					|| !passable[optionalReservedCell]) {
				throw new IllegalArgumentException(
						"optional reserved cell must be passable");
			}
			occupied.add(optionalReservedCell);
		}
		List<Candidate> groundStarterCandidates = new ArrayList<>();
		List<Candidate> introductionCandidates = new ArrayList<>();
		List<Candidate> candidates = new ArrayList<>();
		for (int cell = 0; cell < passable.length; cell++) {
			if (!passable[cell]
					|| distances[cell] < 0
					|| occupied.contains(cell)) {
				continue;
			}
			BukovRaidLayout.Mark mark = containingMark(layout, width, cell);
			if (mark == null) {
				continue;
			}
			Candidate candidate = new Candidate(cell, distances[cell], mark);
			if (groundStarterKitRequired
					&& distances[cell] > 0
					&& distances[cell] <= GROUND_STARTER_RADIUS
					&& deploymentMark.roomId().equals(mark.roomId())) {
				groundStarterCandidates.add(candidate);
			}
			if (distances[cell] < MINIMUM_DISTANCE_FROM_DEPLOYMENT
					|| !raidMode.trainingGround()
							&& mark.zone == BukovRaidLayout.Zone.SPAWN) {
				continue;
			}
			// The weapon tutorial belongs on the guaranteed deployment route.
			// A long ConnectionRoom is still a safe and more discoverable trail
			// than silently moving the starter gun to a distant side room.
			if (candidate.distance <= introductionRadius) {
				introductionCandidates.add(candidate);
			}
			if (mark.zone != BukovRaidLayout.Zone.TRANSIT
					&& !mark.structuralTransit) {
				candidates.add(candidate);
			}
		}
		sortCandidates(groundStarterCandidates);
		sortCandidates(introductionCandidates);
		sortCandidates(candidates);

		List<Placement> placements = new ArrayList<>();
		Set<Integer> usedCells = new HashSet<>();
		Set<String> usedRooms = new HashSet<>();
		if (groundStarterKitRequired) {
			Candidate weapon = firstMatching(
					groundStarterCandidates,
					usedCells,
					usedRooms,
					null,
					GROUND_STARTER_RADIUS,
					false);
			add(placements, usedCells, usedRooms, Kind.WEAPON, weapon);

			Candidate ammunition = firstMatching(
					groundStarterCandidates,
					usedCells,
					usedRooms,
					null,
					GROUND_STARTER_RADIUS,
					false);
			add(placements, usedCells, usedRooms, Kind.AMMUNITION, ammunition);

			Candidate reserveAmmunition = firstMatching(
					groundStarterCandidates,
					usedCells,
					usedRooms,
					null,
					GROUND_STARTER_RADIUS,
					false);
			add(placements, usedCells, usedRooms,
					Kind.RESERVE_AMMUNITION, reserveAmmunition);
		}

		Candidate medical = raidMode.trainingGround()
				? firstMatching(
						introductionCandidates,
						usedCells,
						usedRooms,
						null,
						introductionRadius,
						false)
				: firstMatching(
						candidates,
						usedCells,
						usedRooms,
						BukovRaidLayout.Zone.MEDICAL,
						Integer.MAX_VALUE,
						true);
		if (medical == null && !raidMode.trainingGround()) {
			medical = firstMatching(
					candidates,
					usedCells,
					usedRooms,
					null,
					Integer.MAX_VALUE,
					true);
		}
		add(placements, usedCells, usedRooms, Kind.MEDICAL, medical);

		Candidate salvage = raidMode.trainingGround()
				? firstMatching(
						introductionCandidates,
						usedCells,
						usedRooms,
						null,
						introductionRadius,
						false)
				: firstMatching(
						candidates,
						usedCells,
						usedRooms,
						BukovRaidLayout.Zone.LOW_LOOT,
						Integer.MAX_VALUE,
						true);
		if (salvage == null && !raidMode.trainingGround()) {
			salvage = firstMatching(
					candidates,
					usedCells,
					usedRooms,
					null,
					Integer.MAX_VALUE,
					true);
		}
		add(placements, usedCells, usedRooms, Kind.SALVAGE, salvage);

		// Room diversity is preferred, but never allow an unusual generated
		// topology to remove the player-facing pickup tutorial.
		for (Kind kind : Kind.values()) {
			boolean starterKind = kind == Kind.WEAPON
					|| kind == Kind.AMMUNITION
					|| kind == Kind.RESERVE_AMMUNITION;
			if (!groundStarterKitRequired && starterKind) {
				continue;
			}
			if (contains(placements, kind)) continue;
			List<Candidate> fallbackCandidates = starterKind
					? groundStarterCandidates
					: raidMode.trainingGround()
							? introductionCandidates : candidates;
			Candidate fallback = firstMatching(
					fallbackCandidates,
					usedCells,
					Collections.<String>emptySet(),
					null,
					starterKind
							? GROUND_STARTER_RADIUS
							: raidMode.trainingGround()
							? introductionRadius : Integer.MAX_VALUE,
					false);
			add(placements, usedCells, usedRooms, kind, fallback);
		}
		int requiredCount = groundStarterKitRequired
				? REQUIRED_PLACEMENT_COUNT : BASE_PLACEMENT_COUNT;
		if (placements.size() != requiredCount) {
			throw new IllegalStateException(
					"Generated Bukov raid has no safe loose-loot trail");
		}
		return Collections.unmodifiableList(placements);
	}

	private static void sortCandidates(List<Candidate> candidates) {
		Collections.sort(candidates, new Comparator<Candidate>() {
			@Override
			public int compare(Candidate first, Candidate second) {
				int byDistance = Integer.compare(
						first.distance,
						second.distance);
				if (byDistance != 0) return byDistance;
				return Integer.compare(first.cell, second.cell);
			}
		});
	}

	public static List<Placement> place(BukovLevel level) {
		if (level == null || level.raidLayout() == null) {
			throw new IllegalArgumentException("A generated Bukov level is required");
		}
		List<Placement> placements = plan(
				level.width(),
				level.height(),
				level.passable,
				level.raidLayout(),
				level.entrance(),
				level.raidMode(),
				level.semanticCell("scrap_compactor"),
				BukovMode.groundStarterKitRequired());
		for (Placement placement : placements) {
			Heap heap = level.drop(item(placement.kind), placement.cell);
			heap.type = Heap.Type.HEAP;
			heap.hidden = false;
			heap.seen = true;
		}
		return placements;
	}

	private static Item item(Kind kind) {
		String definitionId;
		int quantity;
		switch (kind) {
			case WEAPON:
				definitionId = "firearm:needle_9";
				quantity = 1;
				break;
			case AMMUNITION:
				definitionId = "ammo:ammo_9_training";
				quantity = 18;
				break;
			case RESERVE_AMMUNITION:
				definitionId = "ammo:ammo_9_standard";
				quantity = 18;
				break;
			case MEDICAL:
				definitionId = "bandage";
				quantity = 2;
				break;
			case SALVAGE:
				definitionId = "duct_tape";
				quantity = 1;
				break;
			default:
				throw new IllegalArgumentException("Unsupported loose-loot kind");
		}
		Item item = BukovFirstRaidLootTables.createByEconomicDefinitionId(
				definitionId);
		if (item == null) {
			throw new IllegalStateException(
					"Missing authored loose-loot definition: " + definitionId);
		}
		item.quantity(quantity);
		return item;
	}

	private static int[] distances(
			int width,
			int height,
			boolean[] passable,
			int start) {
		int[] result = new int[passable.length];
		java.util.Arrays.fill(result, -1);
		Queue<Integer> queue = new ArrayDeque<>();
		result[start] = 0;
		queue.add(start);
		while (!queue.isEmpty()) {
			int cell = queue.remove();
			int x = cell % width;
			int y = cell / width;
			if (x > 0) visit(cell - 1, cell, passable, result, queue);
			if (x + 1 < width) visit(cell + 1, cell, passable, result, queue);
			if (y > 0) visit(cell - width, cell, passable, result, queue);
			if (y + 1 < height) visit(cell + width, cell, passable, result, queue);
		}
		return result;
	}

	private static void visit(
			int next,
			int current,
			boolean[] passable,
			int[] distances,
			Queue<Integer> queue) {
		if (passable[next] && distances[next] < 0) {
			distances[next] = distances[current] + 1;
			queue.add(next);
		}
	}

	private static Set<Integer> reservedCells(
			BukovRaidLayout layout,
			int entranceCell) {
		Set<Integer> result = new HashSet<>();
		result.add(entranceCell);
		for (ExtractionDefinition extraction : layout.extractions) {
			result.add(extraction.interactionCell);
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			result.add(anchor.cell);
		}
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		if (gate != null) {
			result.add(gate.archiveCell);
			for (int gateCell : gate.gateCells) result.add(gateCell);
		}
		return result;
	}

	private static BukovRaidLayout.Mark containingMark(
			BukovRaidLayout layout,
			int width,
			int cell) {
		int x = cell % width;
		int y = cell / width;
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (x >= mark.left && x <= mark.right
					&& y >= mark.top && y <= mark.bottom) {
				return mark;
			}
		}
		return null;
	}

	private static Candidate firstMatching(
			List<Candidate> candidates,
			Set<Integer> usedCells,
			Set<String> usedRooms,
			BukovRaidLayout.Zone requiredZone,
			int maximumDistance,
			boolean requireUnusedRoom) {
		for (Candidate candidate : candidates) {
			if (candidate.distance > maximumDistance
					|| usedCells.contains(candidate.cell)
					|| requiredZone != null
							&& candidate.mark.zone != requiredZone
					|| requireUnusedRoom
							&& usedRooms.contains(candidate.mark.roomId())) {
				continue;
			}
			return candidate;
		}
		return null;
	}

	private static void add(
			List<Placement> placements,
			Set<Integer> usedCells,
			Set<String> usedRooms,
			Kind kind,
			Candidate candidate) {
		if (candidate == null) return;
		placements.add(new Placement(
				kind,
				candidate.cell,
				candidate.distance,
				candidate.mark.roomId()));
		usedCells.add(candidate.cell);
		usedRooms.add(candidate.mark.roomId());
	}

	private static boolean contains(List<Placement> placements, Kind kind) {
		for (Placement placement : placements) {
			if (placement.kind == kind) return true;
		}
		return false;
	}
}
