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

		Set<Integer> protectedCells = protectedCells(level, layout);
		for (BukovRaidLayout.Mark mark : layout.marks) {
			styleFloor(level, layout.seed, mark, theme, protectedCells);
			styleWallEdge(level, layout.seed, mark, theme);
		}

		placeMissionLandmarks(level, layout);
		placeExtractionLandmarks(level, layout);
		placeSemanticLandmark(
				level, layout, "fog_lamp_pump_station", Kind.PUMP_STATION);
		placeSemanticLandmark(
				level, layout, "flooded_warehouse", Kind.INDUSTRIAL_CACHE);
		placeCover(
				level, layout, "flooded_warehouse",
				coverKind(theme, 0, Kind.CONCRETE_COVER),
				coverClusters(theme), protectedCells);
		placeCover(
				level, layout, "broken_rail_loading",
				coverKind(theme, 1, Kind.SANDBAG_COVER),
				coverClusters(theme), protectedCells);
		placeCover(
				level, layout, "umbrella_frame_workshop",
				coverKind(theme, 2, Kind.CONCRETE_COVER),
				coverClusters(theme), protectedCells);
	}

	private static int coverClusters(ThemeDefinition theme) {
		return theme == null ? 1 : theme.coverClusters;
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
			BukovLevel level,
			long seed,
			BukovRaidLayout.Mark mark,
			ThemeDefinition theme,
			Set<Integer> protectedCells) {
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				int cell = x + y * level.width();
				if (!isStyleableFloor(level.map[cell])) continue;
				level.map[cell] = theme == null
						? floorFor(mark, x, y)
						: themedFloorFor(
								theme, mark, seed, x, y,
								protectedCells.contains(cell));
			}
		}
	}

	/**
	 * Six deliberately different spatial grammars, built from the existing
	 * Bukov atlas. The pattern changes silhouettes and navigation cues, not
	 * merely tint: channels, lanes, grids and circuit paths remain readable
	 * even in a monochrome screenshot.
	 */
	private static int themedFloorFor(
			ThemeDefinition theme,
			BukovRaidLayout.Mark mark,
			long seed,
			int x,
			int y,
			boolean protectedCell) {
		int localX = x - mark.left - 1;
		int localY = y - mark.top - 1;
		long noise = visualNoise(seed, theme.id, x, y);
		switch (theme.floorPattern) {
			case "FOG_PATCHES":
				if (!protectedCell && noise % 17L == 0L) return Terrain.WATER;
				return noise % 5L == 0L
						? Terrain.EMPTY_DECO
						: (noise % 3L == 0L
								? Terrain.CUSTOM_DECO_EMPTY : Terrain.EMPTY);
			case "RUST_STRIPES":
				if ((localX + localY) % 5 == 0) return Terrain.EMBERS;
				return localX % 3 == 0
						? Terrain.EMPTY_SP : Terrain.EMPTY_DECO;
			case "FLOOD_CHANNELS":
				if (!protectedCell
						&& (localY % 4 == 1 || localY % 4 == 2)) {
					return Terrain.WATER;
				}
				return localX % 4 == 0
						? Terrain.EMPTY_SP : Terrain.CUSTOM_DECO_EMPTY;
			case "YARD_BLOCKS":
				if (!protectedCell && noise % 13L == 0L) return Terrain.WATER;
				return ((localX / 2) + (localY / 2)) % 2 == 0
						? Terrain.CUSTOM_DECO_EMPTY : Terrain.EMPTY_DECO;
			case "COLD_GRID":
				return localX % 4 == 0 || localY % 4 == 0
						? Terrain.EMPTY_SP : Terrain.CUSTOM_DECO_EMPTY;
			case "LAB_CIRCUIT":
				if (localX == localY || localX + localY == 5) {
					return Terrain.EMBERS;
				}
				return (localX + localY) % 2 == 0
						? Terrain.CUSTOM_DECO_EMPTY : Terrain.EMPTY_SP;
			default:
				throw new IllegalArgumentException(
						"Unknown floor pattern: " + theme.floorPattern);
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
			BukovLevel level,
			long seed,
			BukovRaidLayout.Mark mark,
			ThemeDefinition theme) {
		int width = level.width();
		for (int x = mark.left; x <= mark.right; x++) {
			decorateWall(level, seed, x + mark.top * width, theme);
			decorateWall(level, seed, x + mark.bottom * width, theme);
		}
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			decorateWall(level, seed, mark.left + y * width, theme);
			decorateWall(level, seed, mark.right + y * width, theme);
		}
	}

	private static void decorateWall(
			BukovLevel level,
			long seed,
			int cell,
			ThemeDefinition theme) {
		if (cell < 0 || cell >= level.length()
				|| level.map[cell] != Terrain.WALL) {
			return;
		}
		long signature = seed ^ (cell * 0x9E3779B97F4A7C15L);
		int modulo = theme == null ? 4 : theme.wallDecoModulo;
		if (com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(signature, modulo) == 0L) {
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
			int clusters,
			Set<Integer> protectedCells) {
		BukovRaidLayout.Mark mark = semanticMark(layout, semanticId);
		if (mark == null) return;
		int placed = 0;
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
				int first = x + y * level.width();
				int second = x + 1 + y * level.width();
				level.map[first] = Terrain.STATUE;
				level.map[second] = Terrain.STATUE;
				protectedCells.add(first);
				protectedCells.add(second);
				placed++;
				if (placed >= clusters) return;
			}
		}
		// Small or already structured rooms still get the visual language, but
		// never at the cost of a narrow traversal lane.
		if (placed == 0) {
			addLandmark(
					level,
					mark,
					(mark.left + mark.right) / 2,
					(mark.top + mark.bottom) / 2,
					kind);
		}
	}

	private static long visualNoise(
			long seed, String themeId, int x, int y) {
		long value = seed
				^ ((long)themeId.hashCode() << 32)
				^ (x * 0x9E3779B97F4A7C15L)
				^ (y * 0xC2B2AE3D27D4EB4FL);
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return (value ^ value >>> 31) & Long.MAX_VALUE;
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
