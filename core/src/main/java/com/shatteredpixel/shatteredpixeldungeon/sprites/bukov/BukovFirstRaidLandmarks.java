/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.GameMath;

/**
 * Dynamic maintenance-gate overlay for the first raid.
 *
 * Static landmarks are rendered exactly once by BukovSemanticVisualLayer.
 * This visual-only layer owns only the gate panels because their visibility
 * changes when the task unlocks the passage.
 */
public final class BukovFirstRaidLandmarks extends Group {

	public static final int FRAME_ARCHIVE_CABINET = 0;
	public static final int FRAME_GATE_LEFT = 1;
	public static final int FRAME_GATE_MIDDLE = 2;
	public static final int FRAME_GATE_RIGHT = 3;
	public static final int FRAME_PUMP_STATION = 4;
	public static final int FRAME_FIXED_EXTRACTION = 5;
	public static final int FRAME_CONDITIONAL_EXTRACTION = 6;
	public static final int FRAME_INDUSTRIAL_CRATE = 7;
	public static final int FRAME_CONCRETE_COVER = 8;
	public static final int FRAME_SANDBAG_COVER = 9;

	private static final int SOURCE_FRAME_SIZE = 32;
	private static final float WORLD_SCALE =
			(float) DungeonTilemap.SIZE / SOURCE_FRAME_SIZE;

	private final BukovLevel level;
	private final TextureFilm frames;
	private final Image[] gatePanels = new Image[3];
	private final int[] gateCells;

	public BukovFirstRaidLandmarks(BukovLevel level) {
		if (level == null || level.raidLayout() == null) {
			throw new IllegalArgumentException("A generated Bukov level is required");
		}
		this.level = level;
		frames = new TextureFilm(
				level.landmarkTex(),
				SOURCE_FRAME_SIZE,
				SOURCE_FRAME_SIZE
		);

		BukovRaidLayout.MissionGate gate = level.missionGate();
		gateCells = gate == null || gate.gateCells == null
				? new int[0] : gate.gateCells.clone();

		if (gate != null) {
			addThreeCellGate(gate.gateCell);
		}
	}

	private void addThreeCellGate(int taskCell) {
		if (!validCell(taskCell)) return;
		int y = taskCell / level.width();
		int centerX = (int) GameMath.gate(
				1,
				taskCell % level.width(),
				level.width() - 2
		);
		int[] visualCells = {
				centerX - 1 + y * level.width(),
				centerX + y * level.width(),
				centerX + 1 + y * level.width()
		};
		gatePanels[0] = addLandmark(FRAME_GATE_LEFT, visualCells[0]);
		gatePanels[1] = addLandmark(FRAME_GATE_MIDDLE, visualCells[1]);
		gatePanels[2] = addLandmark(FRAME_GATE_RIGHT, visualCells[2]);
	}

	private Image addLandmark(int frame, int cell) {
		if (!validCell(cell)) return null;
		Image image = new Image(level.landmarkTex());
		image.frame(frames.get(frame));
		image.scale.set(WORLD_SCALE);
		image.x = cell % level.width() * DungeonTilemap.SIZE;
		image.y = cell / level.width() * DungeonTilemap.SIZE;
		add(image);
		return image;
	}

	private boolean validCell(int cell) {
		return cell >= 0 && cell < level.length();
	}

	private boolean gateIsOpen() {
		if (gateCells.length == 0) return true;
		for (int cell : gateCells) {
			if (validCell(cell)
					&& Dungeon.level.map[cell] != Terrain.OPEN_DOOR) {
				return false;
			}
		}
		return true;
	}

	@Override
	public synchronized void update() {
		boolean showClosedGate = !gateIsOpen();
		for (Image panel : gatePanels) {
			if (panel != null) {
				panel.visible = showClosedGate;
				panel.active = showClosedGate;
			}
		}
		super.update();
	}
}
