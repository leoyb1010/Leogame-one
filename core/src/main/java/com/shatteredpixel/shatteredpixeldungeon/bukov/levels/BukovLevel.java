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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovEnemySpawnPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.painters.BukovPainter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms.BukovEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.rooms.BukovExtractionAnchorRoom;
import com.shatteredpixel.shatteredpixeldungeon.bukov.map.BukovRoomGraphAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.BurnedRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.EmptyRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.FissureRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.HallwayRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.PillarsRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.PlatformRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.RuinsRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StripedRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.WaterBridgeRoom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Isolated first-raid level. It keeps only the proven room graph/building
 * primitives from the host engine. Room selection, deployment anchors,
 * painting, terrain skin, mobs, items, objectives and transitions are owned by
 * Bukov so the player path cannot fall back into the old dungeon campaign.
 */
public class BukovLevel extends RegularLevel {

	private static final String RAID_LAYOUT = "bukov_raid_layout";
	private static final String EXTRACTION_CELLS = "bukov_extraction_cells";
	private static final String WHITE_LINE_RESOLVED = "bukov_white_line_resolved";
	private static final String WHITE_LINE_STATE = "bukov_white_line_state";
	public static final int STANDARD_ROOM_BUDGET = 31;
	public static final int SPECIAL_ROOM_BUDGET = 0;
	private static final BukovUiTokens UI_TOKENS =
			BukovUiTokens.loadDefault();

	{
		color1 = UI_TOKENS.color("level.default.primary");
		color2 = UI_TOKENS.color("level.default.secondary");
		// Realtime firefights need enough forward awareness to read cover,
		// gunfire and objectives on a phone without turning the raid into a
		// black keyhole.
		viewDistance = 12;
	}

	/**
	 * Level.create() retries while build() is false. Cap failures so an
	 * accidentally registered, structurally impossible configuration fails
	 * explicitly instead of hanging forever.
	 */
	private static final int MAX_GENERATION_ATTEMPTS = 32;

	private int generationAttempts;
	private final BukovRaidMode raidMode = BukovMode.raidMode();
	private BukovRaidLayout raidLayout;
	private BukovRoomGraphAdapter.AdaptedMap adaptedMap;
	private List<String> lastDiagnostics = Collections.emptyList();
	private boolean whiteLineResolved;
	private WhiteLineBossStateMachine whiteLineState;

	/**
	 * Bukov owns the whole first-raid content lifecycle. The inherited
	 * Level.create() eagerly constructs fantasy food, potions, scrolls and
	 * trinkets before calling our overridden createItems(), which both leaks
	 * old-game content and requires a live texture backend in headless map
	 * validation. Keep the proven terrain builder, but never enter that legacy
	 * item generator.
	 */
	@Override
	public void create() {
		Random.pushGenerator(raidMode.mapSeed(Dungeon.seedCurDepth()));
		try {
			itemsToSpawn.clear();
			do {
				width = height = length = 0;
				transitions = new ArrayList<>();
				mobs = new HashSet<Mob>();
				heaps = new SparseArray<Heap>();
				blobs = new HashMap<Class<? extends Blob>, Blob>();
				plants = new SparseArray<Plant>();
				traps = new SparseArray<Trap>();
				customTiles = new ArrayList<CustomTilemap>();
				customWalls = new ArrayList<CustomTilemap>();
			} while (!build());

			buildFlagMaps();
			cleanWalls();
			createMobs();
			createItems();
		} finally {
			Random.popGenerator();
		}
	}

	@Override
	protected Builder builder() {
		FigureEightBuilder builder = new FigureEightBuilder()
				.setLoopShape(2, raidMode.loopShapeIntensity, 0f);
		builder.setExtraConnectionChance(raidMode.extraConnectionChance);
		builder.setTunnelLength(
				raidMode.pathTunnelChances(),
				raidMode.branchTunnelChances());
		return builder;
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> result = new ArrayList<>();
		result.add(roomEntrance = new BukovEntranceRoom());
		result.add(roomExit = new BukovExtractionAnchorRoom());

		// Mode contracts count playable rooms, not the host's size-unit budget.
		// Force the smallest size category so a large room cannot silently
		// consume two or three slots and shrink a raid below its advertised
		// range. Individual room classes and the builder still vary shape,
		// placement, loops, tunnels and connections.
		for (int index = 0; index < raidMode.standardRoomBudget; index++) {
			StandardRoom room = createIndustrialRoom(index);
			if (!room.setSizeCat(1)) {
				room = new EmptyRoom();
				room.setSizeCat(1);
			}
			result.add(room);
		}
		return result;
	}

	private static StandardRoom createIndustrialRoom(int index) {
		switch (index % 10) {
			case 0: return new HallwayRoom();
			case 1: return new PillarsRoom();
			case 2: return new RuinsRoom();
			case 3: return new PlatformRoom();
			case 4: return new WaterBridgeRoom();
			case 5: return new BurnedRoom();
			case 6: return new FissureRoom();
			case 7: return new StripedRoom();
			default: return new EmptyRoom();
		}
	}

	@Override
	protected Painter painter() {
		BukovPainter painter = new BukovPainter();
		painter.setWater(feeling == Feeling.WATER ? 0.45f : 0.14f, 4);
		painter.setGrass(0f, 0);
		painter.setTraps(0, trapClasses(), trapChances());
		return painter;
	}

	@Override
	public String tilesTex() {
		return visualTheme().tilesTexture();
	}

	@Override
	public String waterTex() {
		return visualTheme().waterTexture();
	}

	public String landmarkTex() {
		return visualTheme().landmarkTexture();
	}

	@Override
	public String tileName(int tile) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
				return BukovMessages.get(
						"bukov.economy.content.tile_worn_metal");
			case Terrain.EMPTY_SP:
				return BukovMessages.get(
						"bukov.economy.content.tile_anti_slip");
			case Terrain.CUSTOM_DECO_EMPTY:
				return BukovMessages.get(
						"bukov.economy.content.tile_maintenance_floor");
			case Terrain.EMBERS:
				return BukovMessages.get(
						"bukov.economy.content.tile_hazard_marking");
			case Terrain.WALL_DECO:
				return BukovMessages.get(
						"bukov.economy.content.tile_equipment_wall");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return BukovMessages.get(
						"bukov.economy.content.tile_industrial_cover");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return BukovMessages.get(
						"bukov.economy.content.tile_abandoned_equipment");
			default:
				return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.EMPTY_DECO:
			case Terrain.EMPTY_SP:
			case Terrain.CUSTOM_DECO_EMPTY:
				return BukovMessages.get(
						"bukov.economy.content.tile_desc_floor");
			case Terrain.EMBERS:
				return BukovMessages.get(
						"bukov.economy.content.tile_desc_hazard");
			case Terrain.WALL_DECO:
				return BukovMessages.get(
						"bukov.economy.content.tile_desc_wall");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return BukovMessages.get(
						"bukov.economy.content.tile_desc_cover");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return BukovMessages.get(
						"bukov.economy.content.tile_desc_equipment");
			default:
				return super.tileDesc(tile);
		}
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		// This is a size-unit budget: large host rooms consume multiple units.
		// Builder-injected ConnectionRooms remain physical corridors but are
		// excluded from the 26-34 content-room acceptance count.
		return raidMode.standardRoomBudget;
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		return SPECIAL_ROOM_BUDGET;
	}

	@Override
	protected void createMobs() {
		// Realtime Bukov enemies are spawned by FirstRaidEnemySpawnDirector.
	}

	@Override
	protected void createItems() {
		// Deterministic loose items teach the ground-pickup language. An
		// incomplete combat loadout also receives its visible starter pair here.
		// Valuable/randomized rewards still come from searchable containers.
		BukovLooseLootPlanner.place(this);
	}

	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		// The host transitions only provide deterministic spawn/graph anchors.
		// Raid exit is a realtime, transactional extraction handled elsewhere.
		return false;
	}

	@Override
	protected boolean build() {
		generationAttempts++;
		if (!super.build()) return false;

		// A fixed bright theme and longer sightline make the training slice a
		// readable ballistics range rather than another fog-heavy contract.
		if (raidMode.trainingGround()) viewDistance = 24;
		ThemeDefinition theme = raidMode.trainingGround()
				? themeForId("cold_storage")
				: themeForId(BukovMode.selectedRaidTheme());
		color1 = theme.primaryColor;
		color2 = theme.secondaryColor;
		BukovRoomGraphAdapter.AdaptedMap candidate = BukovRoomGraphAdapter.adapt(
				this,
				raidMode.mapSeed(Dungeon.seedCurDepth()),
				theme.id,
				raidMode);
		if (!candidate.readyForRaid()) {
			lastDiagnostics = candidate.diagnostics;
			if (generationAttempts >= MAX_GENERATION_ATTEMPTS) {
				throw new IllegalStateException(
						"Bukov level generation failed after "
						+ generationAttempts + " attempts: "
						+ com.shatteredpixel.shatteredpixeldungeon.bukov.util.BukovStrings.join(
								"; ", lastDiagnostics));
			}
			return false;
		}

		adaptedMap = candidate;
		raidLayout = candidate.layout;
		theme.applyRoomWeights(raidLayout);
		BukovAnchorPlanner.Result anchors = BukovAnchorPlanner.assign(
				width(), height(), map, raidLayout, entrance(), exit());
		if (!anchors.valid) {
			lastDiagnostics = Collections.singletonList(anchors.reason);
			adaptedMap = null;
			raidLayout = null;
			if (generationAttempts >= MAX_GENERATION_ATTEMPTS) {
				throw new IllegalStateException(
						"Bukov level generation failed after "
								+ generationAttempts
								+ " attempts: "
								+ anchors.reason);
			}
			return false;
		}
		theme.applyLootWeights(raidLayout);
		enforceFirstRaidCriticalLoot();
		applyBaselineMarker(false);
		BukovSemanticVisualLayer.apply(this, raidLayout, theme);
		BukovAnchorPlanner.Result dressedTraversal =
				BukovAnchorPlanner.validateLockedMissionTraversal(
						width(), height(), map, raidLayout, entrance());
		if (!dressedTraversal.valid) {
			lastDiagnostics = Collections.singletonList(
					"SEMANTIC_VISUAL_LAYER:" + dressedTraversal.reason);
			adaptedMap = null;
			raidLayout = null;
			if (generationAttempts >= MAX_GENERATION_ATTEMPTS) {
				throw new IllegalStateException(
						"Bukov semantic dressing blocked traversal: "
								+ dressedTraversal.reason);
			}
			return false;
		}
		lastDiagnostics = Collections.emptyList();
		return true;
	}

	public BukovRaidLayout raidLayout() {
		return raidLayout;
	}

	public BukovRaidMode raidMode() {
		return raidMode;
	}

	public List<String> generationDiagnostics() {
		return lastDiagnostics;
	}

	public Room room(String stableRoomId) {
		return adaptedMap == null ? null : adaptedMap.room(stableRoomId);
	}

	/**
	 * Returns the generated layout's persisted room identity for a live cell.
	 * Runtime Room instances can be rebuilt during save/load, so callers must
	 * not use object identity as route evidence.
	 */
	public String stableRoomIdAt(int cell) {
		if (adaptedMap == null || cell < 0 || cell >= length()) {
			return "";
		}
		BukovRaidLayout.Mark mark = adaptedMap.mark(room(cell));
		return mark == null ? "" : mark.roomId();
	}

	public List<Room> rooms(BukovRaidLayout.Zone zone) {
		return adaptedMap == null ? Collections.<Room>emptyList() : adaptedMap.rooms(zone);
	}

	public int extractionCell(String extractionId) {
		if (raidLayout == null || extractionId == null) {
			return -1;
		}
		ExtractionDefinition extraction = raidLayout.extraction(extractionId);
		return extraction == null ? -1 : extraction.interactionCell;
	}

	public int lootCell(String anchorId) {
		if (raidLayout == null || anchorId == null) return -1;
		BukovRaidLayout.LootAnchor anchor = raidLayout.lootAnchor(anchorId);
		return anchor == null ? -1 : anchor.cell;
	}

	public List<BukovRaidLayout.LootAnchor> lootAnchors() {
		return raidLayout == null
				? Collections.<BukovRaidLayout.LootAnchor>emptyList()
				: Collections.unmodifiableList(raidLayout.lootAnchors);
	}

	public BukovRaidLayout.MissionGate missionGate() {
		return raidLayout == null ? null : raidLayout.missionGate();
	}

	/**
	 * Returns the deterministic cache that completes the first-raid contract.
	 * The room graph remains generated, but the mission cache is never allowed
	 * to disappear behind theme weighting.
	 */
	public BukovRaidLayout.LootAnchor missionHighValueAnchor() {
		if (raidLayout == null) return null;
		for (BukovRaidLayout.LootAnchor anchor : raidLayout.lootAnchors) {
			BukovRaidLayout.Mark mark = raidLayout.mark(anchor.roomId);
			if (mark != null
					&& mark.zone == BukovRaidLayout.Zone.HIGH_VALUE) {
				return anchor;
			}
		}
		return null;
	}

	public List<BukovEnemySpawnPlanner.SpawnPoint> enemySpawnPoints() {
		return raidLayout == null
				? Collections.<BukovEnemySpawnPlanner.SpawnPoint>emptyList()
				: BukovEnemySpawnPlanner.plan(
						width(), height(), map, raidLayout);
	}

	public boolean whiteLineResolved() {
		return whiteLineResolved;
	}

	public void resolveWhiteLine() {
		whiteLineResolved = true;
	}

	public BukovRaidLayout.BossMechanism bossMechanism() {
		return raidLayout == null ? null : raidLayout.bossMechanism();
	}

	/**
	 * One optional White Line exists per raid. Keeping its state on the level
	 * makes phase, vulnerability and the seed-selected true body part of the
	 * same host save as the terrain and boss mob.
	 */
	public WhiteLineBossStateMachine whiteLineState(int maximumHealth) {
		if (whiteLineState == null) {
			long encounterKey = raidLayout == null
					? Dungeon.seed : raidLayout.seed;
			whiteLineState = new WhiteLineBossStateMachine(
					maximumHealth, encounterKey);
		} else if (whiteLineState.maximumHealth() != maximumHealth) {
			throw new IllegalStateException(
					"Restored White Line health contract does not match content");
		}
		return whiteLineState;
	}

	/** Returns null when this host save has never materialized White Line. */
	public WhiteLineBossStateMachine.Phase whiteLinePhase() {
		return whiteLineState == null ? null : whiteLineState.phase();
	}

	public int semanticCell(String semanticId) {
		if (raidLayout == null || semanticId == null) return -1;
		BukovRaidLayout.BossMechanism mechanism =
				raidLayout.bossMechanism();
		if (mechanism != null
				&& semanticId.equals(mechanism.fogLampAnchorId)) {
			return mechanism.fogLampCell;
		}
		BukovRaidLayout.Mark target = null;
		for (BukovRaidLayout.Mark mark : raidLayout.marks) {
			if (semanticId.equals(mark.semanticId)) {
				target = mark;
				break;
			}
		}
		if (target == null) return -1;

		BukovRaidLayout.MissionGate gate = raidLayout.missionGate();
		int gateCellCount = gate == null ? 0 : 1 + gate.gateCells.length;
		int[] forbidden = new int[
				2 + raidLayout.extractions.size()
						+ raidLayout.lootAnchors.size()
						+ gateCellCount];
		int index = 0;
		forbidden[index++] = entrance();
		forbidden[index++] = exit();
		for (ExtractionDefinition extraction : raidLayout.extractions) {
			forbidden[index++] = extraction.interactionCell;
		}
		for (BukovRaidLayout.LootAnchor anchor : raidLayout.lootAnchors) {
			forbidden[index++] = anchor.cell;
		}
		if (gate != null) {
			forbidden[index++] = gate.archiveCell;
			for (int gateCell : gate.gateCells) {
				forbidden[index++] = gateCell;
			}
		}
		return ExtractionCellSelector.select(
				width(), height(), map, target, forbidden);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (raidLayout != null) bundle.put(RAID_LAYOUT, raidLayout);
		bundle.put(WHITE_LINE_RESOLVED, whiteLineResolved);
		if (whiteLineState != null) {
			bundle.put(WHITE_LINE_STATE, whiteLineState);
		}
		// Retained for migration from the short-lived pre-layout anchor format.
		bundle.put(EXTRACTION_CELLS, extractionCells());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (raidMode.trainingGround()) viewDistance = 24;
		Bundlable restored = bundle.get(RAID_LAYOUT);
		if (!(restored instanceof BukovRaidLayout)) {
			throw new IllegalStateException("Bukov save is missing a valid raid layout");
		}
		raidLayout = (BukovRaidLayout) restored;
		Bundlable restoredWhiteLine = bundle.get(WHITE_LINE_STATE);
		whiteLineState = restoredWhiteLine instanceof WhiteLineBossStateMachine
				? (WhiteLineBossStateMachine)restoredWhiteLine : null;
		ThemeDefinition theme = themeForId(raidLayout.themeId);
		color1 = theme.primaryColor;
		color2 = theme.secondaryColor;
		whiteLineResolved = bundle.contains(WHITE_LINE_RESOLVED)
				&& bundle.getBoolean(WHITE_LINE_RESOLVED);
		adaptedMap = BukovRoomGraphAdapter.bind(this, raidLayout, raidMode);
		lastDiagnostics = adaptedMap.diagnostics;
		if (!adaptedMap.readyForRaid()) {
			throw new IllegalStateException(
					"Bukov save layout does not match restored rooms: "
					+ com.shatteredpixel.shatteredpixeldungeon.bukov.util.BukovStrings.join(
							"; ", lastDiagnostics));
		}
		BukovAnchorPlanner.Result bossMigration =
				BukovAnchorPlanner.ensureBossMechanism(
						width(), height(), map, raidLayout);
		if (!bossMigration.valid) {
			throw new IllegalStateException(
					"Restored Bukov raid is missing White Line anchors: "
							+ bossMigration.reason);
		}
		BukovAnchorPlanner.Result anchors = BukovAnchorPlanner.validate(
				width(), height(), map, raidLayout);
		if (!anchors.valid) {
			placeExtractionMarkers(true);
		} else {
			BukovAnchorPlanner.Result traversal =
					BukovAnchorPlanner.validateLockedMissionTraversal(
							width(), height(), map, raidLayout, entrance());
			if (!traversal.valid) {
				throw new IllegalStateException(
						"Restored Bukov gate topology is unsafe: "
								+ traversal.reason);
			}
			applyBaselineMarker(true);
		}
		BukovSemanticVisualLayer.apply(this, raidLayout, theme);
		BukovAnchorPlanner.Result dressedTraversal =
				BukovAnchorPlanner.validateLockedMissionTraversal(
						width(), height(), map, raidLayout, entrance());
		if (!dressedTraversal.valid) {
			throw new IllegalStateException(
					"Restored Bukov semantic dressing is unsafe: "
							+ dressedTraversal.reason);
		}
		buildFlagMaps();
		cleanWalls();
	}

	private static ThemeDefinition themeForId(String themeId) {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();
		return registry.require(
				themeId == null || themeId.isEmpty() ? "fog_depot" : themeId);
	}

	private ThemeDefinition visualTheme() {
		return themeForId(
				raidLayout == null || raidLayout.themeId == null
						|| raidLayout.themeId.isEmpty()
						? "fog_depot"
						: raidLayout.themeId);
	}

	private void placeExtractionMarkers(boolean updateRuntimeFlags) {
		BukovAnchorPlanner.Result result = BukovAnchorPlanner.assign(
				width(), height(), map, raidLayout, entrance(), exit());
		if (!result.valid) {
			throw new IllegalStateException("Could not place Bukov map anchors: " + result.reason);
		}
		applyBaselineMarker(updateRuntimeFlags);
	}

	private void applyBaselineMarker(boolean updateRuntimeFlags) {
		ExtractionDefinition baseline = raidLayout.extraction("E01");
		if (baseline == null || baseline.interactionCell < 0) {
			throw new IllegalStateException("Bukov layout is missing E01");
		}
		if (updateRuntimeFlags) {
			Level.set(baseline.interactionCell, Terrain.EXIT, this);
		} else {
			map[baseline.interactionCell] = Terrain.EXIT;
		}
	}

	private void enforceFirstRaidCriticalLoot() {
		BukovRaidLayout.LootAnchor critical = missionHighValueAnchor();
		if (critical == null) {
			throw new IllegalStateException(
					"Bukov first raid is missing its high-value objective cache");
		}
		critical.lootTableId =
				FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID;
	}

	private int[] extractionCells() {
		int[] result = new int[raidLayout == null ? 0 : raidLayout.extractions.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = raidLayout.extractions.get(i).interactionCell;
		}
		return result;
	}
}
