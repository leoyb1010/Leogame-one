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
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Serializable, engine-independent description of a generated Bukov raid.
 *
 * Room rectangles are the stable identity. Runtime Room objects may be rebuilt
 * while this data remains safe to save and validate.
 */
public final class BukovRaidLayout implements Bundlable {

	public enum Zone {
		SPAWN,
		LOW_LOOT,
		COMBAT,
		HIGH_VALUE,
		MEDICAL,
		HAZARD,
		BOSS,
		EXTRACTION,
		SECRET,
		TRANSIT
	}

	public enum RouteRisk {
		SAFE,
		BALANCED,
		HIGH_RISK
	}

	public static final class Mark implements Bundlable {

		public int left;
		public int top;
		public int right;
		public int bottom;
		public Zone zone = Zone.TRANSIT;
		public String semanticId = "";
		public int minimumPassageWidthTiles = 2;
		public boolean eliteSpawnAllowed;
		public boolean structuralTransit;

		public Mark() {
		}

		public Mark(int left, int top, int right, int bottom, Zone zone, String semanticId) {
			if (right < left || bottom < top) {
				throw new IllegalArgumentException("Invalid room rectangle");
			}
			this.left = left;
			this.top = top;
			this.right = right;
			this.bottom = bottom;
			this.zone = zone;
			this.semanticId = semanticId == null ? "" : semanticId;
		}

		public String roomId() {
			return left + "," + top + "," + right + "," + bottom;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("left", left);
			bundle.put("top", top);
			bundle.put("right", right);
			bundle.put("bottom", bottom);
			bundle.put("zone", zone);
			bundle.put("semantic_id", semanticId);
			bundle.put("minimum_passage_width", minimumPassageWidthTiles);
			bundle.put("elite_spawn_allowed", eliteSpawnAllowed);
			bundle.put("structural_transit", structuralTransit);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			left = bundle.getInt("left");
			top = bundle.getInt("top");
			right = bundle.getInt("right");
			bottom = bundle.getInt("bottom");
			zone = bundle.getEnum("zone", Zone.class);
			semanticId = bundle.getString("semantic_id");
			minimumPassageWidthTiles = bundle.getInt("minimum_passage_width");
			eliteSpawnAllowed = bundle.getBoolean("elite_spawn_allowed");
			structuralTransit = bundle.contains("structural_transit")
					&& bundle.getBoolean("structural_transit");
		}
	}

	public static final class Link implements Bundlable {

		public String firstRoomId = "";
		public String secondRoomId = "";
		public String requiredEvent = "";
		public float traversalSeconds = 15f;

		public Link() {
		}

		public Link(String firstRoomId, String secondRoomId) {
			this(firstRoomId, secondRoomId, "");
		}

		public Link(String firstRoomId, String secondRoomId, String requiredEvent) {
			if (firstRoomId == null || secondRoomId == null || firstRoomId.equals(secondRoomId)) {
				throw new IllegalArgumentException("A link requires two distinct rooms");
			}
			this.firstRoomId = firstRoomId;
			this.secondRoomId = secondRoomId;
			this.requiredEvent = requiredEvent == null ? "" : requiredEvent;
		}

		public boolean joins(String roomId) {
			return firstRoomId.equals(roomId) || secondRoomId.equals(roomId);
		}

		public String other(String roomId) {
			if (firstRoomId.equals(roomId)) return secondRoomId;
			if (secondRoomId.equals(roomId)) return firstRoomId;
			return null;
		}

		public boolean openWithoutEvents() {
			return requiredEvent.isEmpty();
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("first", firstRoomId);
			bundle.put("second", secondRoomId);
			bundle.put("required_event", requiredEvent);
			bundle.put("traversal_seconds", traversalSeconds);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			firstRoomId = bundle.getString("first");
			secondRoomId = bundle.getString("second");
			requiredEvent = bundle.getString("required_event");
			traversalSeconds = bundle.getFloat("traversal_seconds");
		}
	}

	public static final class Route implements Bundlable {

		public String routeId = "";
		public RouteRisk risk = RouteRisk.BALANCED;
		public final List<String> roomIds = new ArrayList<>();

		public Route() {
		}

		public Route(String routeId, RouteRisk risk, List<String> roomIds) {
			this.routeId = routeId;
			this.risk = risk;
			this.roomIds.addAll(roomIds);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("route_id", routeId);
			bundle.put("risk", risk);
			bundle.put("room_ids", roomIds.toArray(new String[0]));
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			routeId = bundle.getString("route_id");
			risk = bundle.getEnum("risk", RouteRisk.class);
			roomIds.clear();
			String[] restoredIds = bundle.getStringArray("room_ids");
			if (restoredIds != null) Collections.addAll(roomIds, restoredIds);
		}
	}

	public static final class LootAnchor implements Bundlable {

		public String id = "";
		public String roomId = "";
		public int cell = -1;
		public int x = -1;
		public int y = -1;
		public String lootTableId = "";
		public float searchSeconds;

		public LootAnchor() {
		}

		public LootAnchor(String id, String roomId, int cell, int x, int y) {
			this.id = id == null ? "" : id;
			this.roomId = roomId == null ? "" : roomId;
			this.cell = cell;
			this.x = x;
			this.y = y;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("id", id);
			bundle.put("room_id", roomId);
			bundle.put("cell", cell);
			bundle.put("x", x);
			bundle.put("y", y);
			bundle.put("loot_table_id", lootTableId);
			bundle.put("search_seconds", searchSeconds);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			id = bundle.getString("id");
			roomId = bundle.getString("room_id");
			cell = bundle.contains("cell") ? bundle.getInt("cell") : -1;
			x = bundle.contains("x") ? bundle.getInt("x") : -1;
			y = bundle.contains("y") ? bundle.getInt("y") : -1;
			lootTableId = bundle.getString("loot_table_id");
			searchSeconds = bundle.getFloat("search_seconds");
		}
	}

	/**
	 * Stable first-raid objective anchors. The archive is always authored as a
	 * dedicated container; the gate is a real level cell and never a UI-only
	 * objective.
	 */
	public static final class MissionGate implements Bundlable {

		public String archiveAnchorId = FirstRaidMission.ARCHIVE_ANCHOR_ID;
		public String archiveRoomId = "";
		public int archiveCell = -1;
		public int archiveX = -1;
		public int archiveY = -1;
		public String gateId = FirstRaidMission.GATE_ID;
		public String gateRoomId = "";
		public int gateCell = -1;
		public int[] gateCells = new int[0];
		public int gateX = -1;
		public int gateY = -1;
		public String requiredEvent = FirstRaidMission.EVENT_ID;

		public MissionGate() {
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("archive_anchor_id", archiveAnchorId);
			bundle.put("archive_room_id", archiveRoomId);
			bundle.put("archive_cell", archiveCell);
			bundle.put("archive_x", archiveX);
			bundle.put("archive_y", archiveY);
			bundle.put("gate_id", gateId);
			bundle.put("gate_room_id", gateRoomId);
			bundle.put("gate_cell", gateCell);
			bundle.put("gate_cells", gateCells);
			bundle.put("gate_x", gateX);
			bundle.put("gate_y", gateY);
			bundle.put("required_event", requiredEvent);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			archiveAnchorId = bundle.getString("archive_anchor_id");
			archiveRoomId = bundle.getString("archive_room_id");
			archiveCell = bundle.getInt("archive_cell");
			archiveX = bundle.getInt("archive_x");
			archiveY = bundle.getInt("archive_y");
			gateId = bundle.getString("gate_id");
			gateRoomId = bundle.getString("gate_room_id");
			gateCell = bundle.getInt("gate_cell");
			gateCells = bundle.contains("gate_cells")
					? bundle.getIntArray("gate_cells")
					: gateCell < 0 ? new int[0] : new int[]{gateCell};
			gateX = bundle.getInt("gate_x");
			gateY = bundle.getInt("gate_y");
			requiredEvent = bundle.getString("required_event");
		}
	}

	/**
	 * Stable world-space anchors for the optional White Line encounter.
	 *
	 * The body traces are real walkable cells in the boss room. The fog-lamp
	 * cell is a separate scene control in the pump-station room, so phase three
	 * cannot be completed by touching the boss.
	 */
	public static final class BossMechanism implements Bundlable {

		public String bossRoomId = "";
		public int[] bodyTraceCells = new int[0];
		public String fogLampAnchorId = "fog_lamp_pump_station";
		public String fogLampRoomId = "";
		public int fogLampCell = -1;
		public int fogLampX = -1;
		public int fogLampY = -1;

		public BossMechanism() {
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("boss_room_id", bossRoomId);
			bundle.put("body_trace_cells", bodyTraceCells);
			bundle.put("fog_lamp_anchor_id", fogLampAnchorId);
			bundle.put("fog_lamp_room_id", fogLampRoomId);
			bundle.put("fog_lamp_cell", fogLampCell);
			bundle.put("fog_lamp_x", fogLampX);
			bundle.put("fog_lamp_y", fogLampY);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			bossRoomId = bundle.getString("boss_room_id");
			bodyTraceCells = bundle.getIntArray("body_trace_cells");
			if (bodyTraceCells == null) bodyTraceCells = new int[0];
			fogLampAnchorId = bundle.getString("fog_lamp_anchor_id");
			if (fogLampAnchorId == null || fogLampAnchorId.isEmpty()) {
				fogLampAnchorId = "fog_lamp_pump_station";
			}
			fogLampRoomId = bundle.getString("fog_lamp_room_id");
			fogLampCell = bundle.contains("fog_lamp_cell")
					? bundle.getInt("fog_lamp_cell") : -1;
			fogLampX = bundle.contains("fog_lamp_x")
					? bundle.getInt("fog_lamp_x") : -1;
			fogLampY = bundle.contains("fog_lamp_y")
					? bundle.getInt("fog_lamp_y") : -1;
		}
	}

	public long seed;
	public String themeId = "fog_depot";
	public final List<Mark> marks = new ArrayList<>();
	public final List<Link> links = new ArrayList<>();
	public final List<ExtractionDefinition> extractions = new ArrayList<>();
	public final List<LootAnchor> lootAnchors = new ArrayList<>();
	public final List<Route> routes = new ArrayList<>();
	private MissionGate missionGate;
	private BossMechanism bossMechanism;

	public Mark mark(String roomId) {
		for (Mark mark : marks) {
			if (mark.roomId().equals(roomId)) return mark;
		}
		return null;
	}

	public ExtractionDefinition extraction(String extractionId) {
		for (ExtractionDefinition extraction : extractions) {
			if (extraction.id.equals(extractionId)) return extraction;
		}
		return null;
	}

	public LootAnchor lootAnchor(String anchorId) {
		for (LootAnchor anchor : lootAnchors) {
			if (anchor.id.equals(anchorId)) return anchor;
		}
		return null;
	}

	public MissionGate missionGate() {
		return missionGate;
	}

	void missionGate(MissionGate missionGate) {
		this.missionGate = missionGate;
	}

	public BossMechanism bossMechanism() {
		return bossMechanism;
	}

	void bossMechanism(BossMechanism bossMechanism) {
		this.bossMechanism = bossMechanism;
	}

	public int playableRoomCount() {
		int count = 0;
		for (Mark mark : marks) {
			if (!mark.structuralTransit) count++;
		}
		return count;
	}

	public List<String> neighbours(String roomId, boolean eventsApplied) {
		List<String> result = new ArrayList<>();
		for (Link link : links) {
			if (eventsApplied || link.openWithoutEvents()) {
				String other = link.other(roomId);
				if (other != null) result.add(other);
			}
		}
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("seed", seed);
		bundle.put("theme_id", themeId);
		bundle.put("marks", marks);
		bundle.put("links", links);
		bundle.put("extractions", extractions);
		bundle.put("loot_anchors", lootAnchors);
		bundle.put("routes", routes);
		if (missionGate != null) bundle.put("mission_gate", missionGate);
		if (bossMechanism != null) {
			bundle.put("boss_mechanism", bossMechanism);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		seed = bundle.getLong("seed");
		themeId = bundle.getString("theme_id");
		restoreCollection(bundle, "marks", marks, Mark.class);
		restoreCollection(bundle, "links", links, Link.class);
		restoreCollection(bundle, "extractions", extractions, ExtractionDefinition.class);
		if (bundle.contains("loot_anchors")) {
			restoreCollection(bundle, "loot_anchors", lootAnchors, LootAnchor.class);
		} else {
			lootAnchors.clear();
		}
		restoreCollection(bundle, "routes", routes, Route.class);
		Bundlable restoredMissionGate = bundle.get("mission_gate");
		missionGate = restoredMissionGate instanceof MissionGate
				? (MissionGate) restoredMissionGate
				: null;
		Bundlable restoredBossMechanism = bundle.get("boss_mechanism");
		bossMechanism = restoredBossMechanism instanceof BossMechanism
				? (BossMechanism)restoredBossMechanism
				: null;
	}

	private static <T extends Bundlable> void restoreCollection(
			Bundle bundle, String key, List<T> destination, Class<T> type) {
		destination.clear();
		for (Bundlable item : bundle.getCollection(key)) {
			if (type.isInstance(item)) destination.add(type.cast(item));
		}
	}
}
