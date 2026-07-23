package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLandmarkTilemap.Kind;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.painters.BukovPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BukovSemanticVisualLayerTest {

	@Test
	public void dedicatedPainterRemovesFantasyTerrainLanguage() {
		BukovLevel level = level(9, 5);
		int[] fantasy = {
				Terrain.GRASS,
				Terrain.HIGH_GRASS,
				Terrain.FURROWED_GRASS,
				Terrain.WELL,
				Terrain.EMPTY_WELL,
				Terrain.BOOKSHELF,
				Terrain.ALCHEMY,
				Terrain.PEDESTAL,
				Terrain.SECRET_DOOR,
				Terrain.SECRET_TRAP,
				Terrain.TRAP,
				Terrain.INACTIVE_TRAP,
				Terrain.CRYSTAL_DOOR,
				Terrain.BARRICADE,
				Terrain.MINE_CRYSTAL,
				Terrain.MINE_BOULDER
		};
		for (int index = 0; index < fantasy.length; index++) {
			level.map[10 + index] = fantasy[index];
		}

		assertTrue(new BukovPainter().paint(level, null));

		for (int terrain : level.map) {
			assertFalse(isFantasyTerrain(terrain));
		}
		assertTrue(level.tileName(Terrain.STATUE).contains("掩体"));
		assertTrue(level.tileName(Terrain.EMBERS).contains("危险"));
	}

	@Test
	public void semanticRoomsGetDistinctFloorsLandmarksAndSafeCover() {
		BukovLevel level = level(42, 20);
		BukovRaidLayout layout = new BukovRaidLayout();
		layout.seed = 772244L;
		BukovRaidLayout.Mark maintenance = room(
				level, 1, 1, "south_maintenance",
				BukovRaidLayout.Zone.LOW_LOOT);
		BukovRaidLayout.Mark pump = room(
				level, 10, 1, "fog_lamp_pump_station",
				BukovRaidLayout.Zone.COMBAT);
		BukovRaidLayout.Mark warehouse = room(
				level, 19, 1, "flooded_warehouse",
				BukovRaidLayout.Zone.HIGH_VALUE);
		BukovRaidLayout.Mark loading = room(
				level, 28, 1, "broken_rail_loading",
				BukovRaidLayout.Zone.COMBAT);
		BukovRaidLayout.Mark extraction = room(
				level, 10, 10, "",
				BukovRaidLayout.Zone.EXTRACTION);
		layout.marks.add(maintenance);
		layout.marks.add(pump);
		layout.marks.add(warehouse);
		layout.marks.add(loading);
		layout.marks.add(extraction);
		BukovRaidLayout.MissionGate gate =
				new BukovRaidLayout.MissionGate();
		gate.archiveRoomId = maintenance.roomId();
		gate.archiveX = maintenance.left + 2;
		gate.archiveY = maintenance.top + 2;
		gate.archiveCell =
				gate.archiveX + gate.archiveY * level.width();
		gate.gateRoomId = pump.roomId();
		gate.gateX = pump.left + 2;
		gate.gateY = pump.top + 2;
		gate.gateCell = gate.gateX + gate.gateY * level.width();
		gate.gateCells = new int[]{gate.gateCell};
		layout.missionGate(gate);

		ExtractionDefinition e01 =
				ExtractionDefinition.baseline(extraction.roomId());
		e01.interactionX = 13;
		e01.interactionY = 13;
		e01.interactionCell =
				e01.interactionX + e01.interactionY * level.width();
		layout.extractions.add(e01);

		BukovSemanticVisualLayer.apply(level, layout);

		assertNotEquals(
				floorSignature(level, maintenance),
				floorSignature(level, pump));
		assertNotEquals(
				floorSignature(level, pump),
				floorSignature(level, warehouse));
		assertNotEquals(
				floorSignature(level, warehouse),
				floorSignature(level, extraction));

		EnumSet<Kind> landmarks = EnumSet.noneOf(Kind.class);
		for (CustomTilemap visual : level.customTiles) {
			if (visual instanceof BukovLandmarkTilemap) {
				landmarks.add(((BukovLandmarkTilemap)visual).kind());
			}
		}
		assertTrue(landmarks.contains(Kind.PUMP_STATION));
		assertTrue(landmarks.contains(Kind.INDUSTRIAL_CACHE));
		assertTrue(landmarks.contains(Kind.BASE_EXTRACTION));
		assertTrue(landmarks.contains(Kind.ARCHIVE_CABINET));
		assertFalse(landmarks.contains(Kind.MAINTENANCE_GATE));
		assertTrue(landmarks.contains(Kind.CONCRETE_COVER));
		assertTrue(landmarks.contains(Kind.SANDBAG_COVER));

		assertTrue(solidCoverCount(level, warehouse) >= 2);
		assertTrue(solidCoverCount(level, loading) >= 2);
		assertTrue(allWalkableCellsConnected(level, warehouse));
		assertTrue(allWalkableCellsConnected(level, loading));
	}

	@Test
	public void landmarkKindAndGridFootprintSurviveBundleRestore() {
		BukovLandmarkTilemap original =
				new BukovLandmarkTilemap(Kind.MAINTENANCE_GATE);
		original.pos(7, 9);
		Bundle stored = new Bundle();
		stored.put("landmark", original);

		BukovLandmarkTilemap restored =
				(BukovLandmarkTilemap)stored.get("landmark");
		assertTrue(restored.kind() == Kind.MAINTENANCE_GATE);
		assertTrue(restored.tileX == 7 && restored.tileY == 9);
		assertTrue(restored.tileW == 6 && restored.tileH == 2);
	}

	private static BukovLevel level(int width, int height) {
		BukovLevel result = new BukovLevel();
		result.setSize(width, height);
		result.transitions = new ArrayList<LevelTransition>();
		result.customTiles = new ArrayList<CustomTilemap>();
		Arrays.fill(result.map, Terrain.WALL);
		return result;
	}

	private static BukovRaidLayout.Mark room(
			BukovLevel level,
			int left,
			int top,
			String semanticId,
			BukovRaidLayout.Zone zone) {
		BukovRaidLayout.Mark result = new BukovRaidLayout.Mark(
				left, top, left + 7, top + 7, zone, semanticId);
		for (int y = result.top + 1; y < result.bottom; y++) {
			for (int x = result.left + 1; x < result.right; x++) {
				level.map[x + y * level.width()] = Terrain.EMPTY;
			}
		}
		return result;
	}

	private static Set<Integer> floorSignature(
			BukovLevel level, BukovRaidLayout.Mark mark) {
		Set<Integer> result = new HashSet<>();
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				int terrain = level.map[x + y * level.width()];
				if ((Terrain.flags[terrain] & Terrain.PASSABLE) != 0) {
					result.add(terrain);
				}
			}
		}
		return result;
	}

	private static int solidCoverCount(
			BukovLevel level, BukovRaidLayout.Mark mark) {
		int count = 0;
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				if (level.map[x + y * level.width()] == Terrain.STATUE) {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean allWalkableCellsConnected(
			BukovLevel level, BukovRaidLayout.Mark mark) {
		int start = -1;
		int walkable = 0;
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				int cell = x + y * level.width();
				if ((Terrain.flags[level.map[cell]] & Terrain.PASSABLE) != 0) {
					if (start < 0) start = cell;
					walkable++;
				}
			}
		}
		if (start < 0) return false;
		boolean[] visited = new boolean[level.length()];
		ArrayDeque<Integer> pending = new ArrayDeque<>();
		pending.add(start);
		visited[start] = true;
		int reached = 0;
		int[] steps = {-1, 1, -level.width(), level.width()};
		while (!pending.isEmpty()) {
			int cell = pending.removeFirst();
			reached++;
			for (int step : steps) {
				int next = cell + step;
				int x = next % level.width();
				int y = next / level.width();
				if (next < 0 || next >= level.length()
						|| x <= mark.left || x >= mark.right
						|| y <= mark.top || y >= mark.bottom
						|| visited[next]
						|| (Terrain.flags[level.map[next]]
								& Terrain.PASSABLE) == 0) {
					continue;
				}
				visited[next] = true;
				pending.addLast(next);
			}
		}
		return reached == walkable;
	}

	private static boolean isFantasyTerrain(int terrain) {
		return terrain == Terrain.GRASS
				|| terrain == Terrain.HIGH_GRASS
				|| terrain == Terrain.FURROWED_GRASS
				|| terrain == Terrain.WELL
				|| terrain == Terrain.EMPTY_WELL
				|| terrain == Terrain.BOOKSHELF
				|| terrain == Terrain.ALCHEMY
				|| terrain == Terrain.PEDESTAL
				|| terrain == Terrain.SECRET_DOOR
				|| terrain == Terrain.SECRET_TRAP
				|| terrain == Terrain.TRAP
				|| terrain == Terrain.INACTIVE_TRAP
				|| terrain == Terrain.CRYSTAL_DOOR
				|| terrain == Terrain.BARRICADE
				|| terrain == Terrain.MINE_CRYSTAL
				|| terrain == Terrain.MINE_BOULDER;
	}
}
