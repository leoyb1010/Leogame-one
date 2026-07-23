package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Converts authored room semantics into deterministic host spawn points. */
public final class BukovEnemySpawnPlanner {

	public static final class SpawnPoint {
		public final int cell;
		public final int distanceFromSpawnRooms;
		public final boolean mandatorySingleRoute;
		public final boolean bossArena;

		private SpawnPoint(
				int cell,
				int distanceFromSpawnRooms,
				boolean mandatorySingleRoute,
				boolean bossArena) {
			this.cell = cell;
			this.distanceFromSpawnRooms = distanceFromSpawnRooms;
			this.mandatorySingleRoute = mandatorySingleRoute;
			this.bossArena = bossArena;
		}
	}

	public static List<SpawnPoint> plan(
			int width,
			int height,
			int[] map,
			BukovRaidLayout layout) {
		if (width <= 0 || height <= 0 || map == null
				|| map.length != width * height || layout == null) {
			throw new IllegalArgumentException(
					"valid dimensions, map, and layout are required");
		}
		Map<String, Integer> roomDistances = roomDistances(layout);
		Set<Integer> occupied = occupiedAnchors(layout);
		List<SpawnPoint> result = new ArrayList<>();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (!spawnEligible(mark.zone)) continue;
			int roomDistance = roomDistances.containsKey(mark.roomId())
					? roomDistances.get(mark.roomId())
					: 0;
			boolean bossArena = mark.zone == BukovRaidLayout.Zone.BOSS;
			boolean mandatory = !optionalRoute(mark.zone);
			int minimumX = Math.max(1, mark.left + 1);
			int maximumX = Math.min(width - 2, mark.right - 1);
			int minimumY = Math.max(1, mark.top + 1);
			int maximumY = Math.min(height - 2, mark.bottom - 1);
			for (int y = minimumY; y <= maximumY; y++) {
				for (int x = minimumX; x <= maximumX; x++) {
					int cell = x + y * width;
					if (!occupied.contains(cell) && passable(map[cell])) {
						result.add(new SpawnPoint(
								cell,
								roomDistance,
								mandatory,
								bossArena));
					}
				}
			}
		}
		Collections.sort(result, new Comparator<SpawnPoint>() {
			@Override
			public int compare(SpawnPoint first, SpawnPoint second) {
				return Integer.compare(first.cell, second.cell);
			}
		});
		return Collections.unmodifiableList(result);
	}

	private static Map<String, Integer> roomDistances(BukovRaidLayout layout) {
		Map<String, Integer> distance = new HashMap<>();
		ArrayDeque<String> queue = new ArrayDeque<>();
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (mark.zone == BukovRaidLayout.Zone.SPAWN) {
				distance.put(mark.roomId(), 0);
				queue.addLast(mark.roomId());
			}
		}
		while (!queue.isEmpty()) {
			String roomId = queue.removeFirst();
			int nextDistance = distance.get(roomId) + 1;
			for (String neighbour : layout.neighbours(roomId, true)) {
				if (!distance.containsKey(neighbour)) {
					distance.put(neighbour, nextDistance);
					queue.addLast(neighbour);
				}
			}
		}
		return distance;
	}

	private static Set<Integer> occupiedAnchors(BukovRaidLayout layout) {
		Set<Integer> occupied = new HashSet<>();
		for (com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition
				extraction : layout.extractions) {
			occupied.add(extraction.interactionCell);
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			occupied.add(anchor.cell);
		}
		return occupied;
	}

	private static boolean spawnEligible(BukovRaidLayout.Zone zone) {
		return zone != BukovRaidLayout.Zone.SPAWN
				&& zone != BukovRaidLayout.Zone.EXTRACTION
				&& zone != BukovRaidLayout.Zone.TRANSIT;
	}

	private static boolean optionalRoute(BukovRaidLayout.Zone zone) {
		return zone == BukovRaidLayout.Zone.HIGH_VALUE
				|| zone == BukovRaidLayout.Zone.SECRET
				|| zone == BukovRaidLayout.Zone.BOSS;
	}

	private static boolean passable(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private BukovEnemySpawnPlanner() {
	}
}
