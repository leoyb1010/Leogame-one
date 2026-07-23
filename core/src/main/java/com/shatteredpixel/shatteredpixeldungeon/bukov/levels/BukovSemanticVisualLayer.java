package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLandmarkTilemap.Kind;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Applies deterministic, semantic industrial dressing after the proven room
 * graph and gameplay anchors have been authored.
 */
public final class BukovSemanticVisualLayer {

	private BukovSemanticVisualLayer() {
	}

	public static void apply(BukovLevel level, BukovRaidLayout layout) {
		apply(level, layout, null);
	}

	public static void apply(
			BukovLevel level,
			BukovRaidLayout layout,
			ThemeDefinition theme) {
		if (level == null || layout == null) {
			throw new IllegalArgumentException("level and layout are required");
		}
		if (level.customTiles == null) {
			level.customTiles = new ArrayList<>();
		}
		removeOldLandmarks(level);

		for (BukovRaidLayout.Mark mark : layout.marks) {
			styleFloor(level, mark);
			styleWallEdge(level, layout.seed, mark);
		}

		Set<Integer> protectedCells = protectedCells(level, layout);
		placeMissionLandmarks(level, layout);
		placeExtractionLandmarks(level, layout);
		placeSemanticLandmark(
				level, layout, "fog_lamp_pump_station", Kind.PUMP_STATION);
		placeSemanticLandmark(
				level, layout, "flooded_warehouse", Kind.INDUSTRIAL_CACHE);
		placeCover(
				level, layout, "flooded_warehouse",
				coverKind(theme, 0, Kind.CONCRETE_COVER), protectedCells);
		placeCover(
				level, layout, "broken_rail_loading",
				coverKind(theme, 1, Kind.SANDBAG_COVER), protectedCells);
		placeCover(
				level, layout, "umbrella_frame_workshop",
				coverKind(theme, 2, Kind.CONCRETE_COVER), protectedCells);
	}

	private static Kind coverKind(
			ThemeDefinition theme, int index, Kind fallback) {
		if (theme == null || index < 0
				|| index >= theme.coverCombination().size()) {
			return fallback;
		}
		return Kind.valueOf(theme.coverCombination().get(index));
	}

	private static void removeOldLandmarks(BukovLevel level) {
		Iterator<CustomTilemap> iterator = level.customTiles.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() instanceof BukovLandmarkTilemap) {
				iterator.remove();
			}
		}
	}

	private static void styleFloor(
			BukovLevel level, BukovRaidLayout.Mark mark) {
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				int cell = x + y * level.width();
				if (!isStyleableFloor(level.map[cell])) continue;
				level.map[cell] = floorFor(mark, x, y);
			}
		}
	}

	private static int floorFor(
			BukovRaidLayout.Mark mark, int x, int y) {
		String semantic = mark.semanticId;
		if ("south_maintenance".equals(semantic)) {
			return (x + y) % 4 == 0
					? Terrain.EMPTY_SP : Terrain.CUSTOM_DECO_EMPTY;
		}
		if ("fog_lamp_pump_station".equals(semantic)) {
			return x == (mark.left + mark.right) / 2
					|| y == (mark.top + mark.bottom) / 2
					? Terrain.EMBERS : Terrain.EMPTY_DECO;
		}
		if ("flooded_warehouse".equals(semantic)) {
			return y % 3 == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY_SP;
		}
		if ("broken_rail_loading".equals(semantic)) {
			return (x + y) % 5 == 0
					? Terrain.EMBERS : Terrain.EMPTY_DECO;
		}
		if ("umbrella_frame_workshop".equals(semantic)) {
			return x % 3 == 0
					? Terrain.EMPTY_SP : Terrain.CUSTOM_DECO_EMPTY;
		}
		if ("scrap_compactor".equals(semantic)) {
			return (x + y) % 3 == 0
					? Terrain.EMBERS : Terrain.EMPTY_SP;
		}
		switch (mark.zone) {
			case SPAWN:
				return (x + y) % 3 == 0
						? Terrain.EMPTY_SP : Terrain.CUSTOM_DECO_EMPTY;
			case EXTRACTION:
				return (x + y) % 2 == 0
						? Terrain.EMBERS : Terrain.EMPTY_SP;
			case HAZARD:
				return (x + y) % 3 == 0
						? Terrain.EMBERS : Terrain.EMPTY_DECO;
			case MEDICAL:
				return Terrain.CUSTOM_DECO_EMPTY;
			default:
				return Terrain.EMPTY;
		}
	}

	private static void styleWallEdge(
			BukovLevel level, long seed, BukovRaidLayout.Mark mark) {
		int width = level.width();
		for (int x = mark.left; x <= mark.right; x++) {
			decorateWall(level, seed, x + mark.top * width);
			decorateWall(level, seed, x + mark.bottom * width);
		}
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			decorateWall(level, seed, mark.left + y * width);
			decorateWall(level, seed, mark.right + y * width);
		}
	}

	private static void decorateWall(
			BukovLevel level, long seed, int cell) {
		if (cell < 0 || cell >= level.length()
				|| level.map[cell] != Terrain.WALL) {
			return;
		}
		long signature = seed ^ (cell * 0x9E3779B97F4A7C15L);
		if ((signature & 3L) == 0L) {
			level.map[cell] = Terrain.WALL_DECO;
		}
	}

	private static void placeMissionLandmarks(
			BukovLevel level, BukovRaidLayout layout) {
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		if (gate == null) return;
		addLandmark(
				level,
				layout.mark(gate.archiveRoomId),
				gate.archiveX,
				gate.archiveY,
				Kind.ARCHIVE_CABINET);
	}

	private static void placeExtractionLandmarks(
			BukovLevel level, BukovRaidLayout layout) {
		for (ExtractionDefinition extraction : layout.extractions) {
			addLandmark(
					level,
					layout.mark(extraction.roomId),
					extraction.interactionX,
					extraction.interactionY,
					extraction.type == ExtractionDefinition.Type.BASELINE
							? Kind.BASE_EXTRACTION
							: Kind.CONDITIONAL_EXTRACTION);
		}
	}

	private static void placeSemanticLandmark(
			BukovLevel level,
			BukovRaidLayout layout,
			String semanticId,
			Kind kind) {
		BukovRaidLayout.Mark mark = semanticMark(layout, semanticId);
		if (mark == null) return;
		addLandmark(
				level,
				mark,
				(mark.left + mark.right) / 2,
				(mark.top + mark.bottom) / 2,
				kind);
	}

	private static void placeCover(
			BukovLevel level,
			BukovRaidLayout layout,
			String semanticId,
			Kind kind,
			Set<Integer> protectedCells) {
		BukovRaidLayout.Mark mark = semanticMark(layout, semanticId);
		if (mark == null) return;
		for (int y = mark.top + 2; y <= mark.bottom - 3; y++) {
			for (int x = mark.left + 2; x <= mark.right - 3; x++) {
				if (!safeTwoCellCover(
						level, mark, x, y, protectedCells)) {
					continue;
				}
				BukovLandmarkTilemap landmark =
						new BukovLandmarkTilemap(kind);
				landmark.pos(x, y - 1);
				level.customTiles.add(landmark);
				level.map[x + y * level.width()] = Terrain.STATUE;
				level.map[x + 1 + y * level.width()] = Terrain.STATUE;
				return;
			}
		}
		// Small or already structured rooms still get the visual language, but
		// never at the cost of a narrow traversal lane.
		addLandmark(
				level,
				mark,
				(mark.left + mark.right) / 2,
				(mark.top + mark.bottom) / 2,
				kind);
	}

	private static boolean safeTwoCellCover(
			BukovLevel level,
			BukovRaidLayout.Mark mark,
			int x,
			int y,
			Set<Integer> protectedCells) {
		int width = level.width();
		for (int checkY = y - 1; checkY <= y + 1; checkY++) {
			for (int checkX = x - 1; checkX <= x + 2; checkX++) {
				if (checkX <= mark.left || checkX >= mark.right
						|| checkY <= mark.top || checkY >= mark.bottom) {
					return false;
				}
				int cell = checkX + checkY * width;
				if (protectedCells.contains(cell)
						|| !isWalkable(level.map[cell])) {
					return false;
				}
			}
		}
		return true;
	}

	private static void addLandmark(
			BukovLevel level,
			BukovRaidLayout.Mark mark,
			int preferredX,
			int preferredY,
			Kind kind) {
		if (mark == null) return;
		BukovLandmarkTilemap landmark = new BukovLandmarkTilemap(kind);
		int minX = mark.left + 1;
		int minY = mark.top + 1;
		int maxX = mark.right - landmark.tileW;
		int maxY = mark.bottom - landmark.tileH;
		if (maxX < minX || maxY < minY) return;
		int x = clamp(preferredX - landmark.tileW / 2, minX, maxX);
		int y = clamp(preferredY - landmark.tileH / 2, minY, maxY);
		landmark.pos(x, y);
		level.customTiles.add(landmark);
	}

	private static Set<Integer> protectedCells(
			BukovLevel level, BukovRaidLayout layout) {
		Set<Integer> result = new HashSet<>();
		result.add(level.entrance());
		result.add(level.exit());
		for (ExtractionDefinition extraction : layout.extractions) {
			result.add(extraction.interactionCell);
		}
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			result.add(anchor.cell);
		}
		BukovRaidLayout.MissionGate gate = layout.missionGate();
		if (gate != null) {
			result.add(gate.archiveCell);
			for (int cell : gate.gateCells) result.add(cell);
		}
		return result;
	}

	private static BukovRaidLayout.Mark semanticMark(
			BukovRaidLayout layout, String semanticId) {
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (semanticId.equals(mark.semanticId)) return mark;
		}
		return null;
	}

	private static boolean isStyleableFloor(int terrain) {
		return terrain == Terrain.EMPTY
				|| terrain == Terrain.EMPTY_DECO
				|| terrain == Terrain.EMPTY_SP
				|| terrain == Terrain.CUSTOM_DECO_EMPTY
				|| terrain == Terrain.EMBERS;
	}

	private static boolean isWalkable(int terrain) {
		return terrain >= 0
				&& terrain < Terrain.flags.length
				&& (Terrain.flags[terrain] & Terrain.PASSABLE) != 0;
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
