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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovPerformancePolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.effects.Ripple;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded world-space atmosphere for generated Bukov raids.
 *
 * <p>The layer is inserted above terrain and below actors. It adds two
 * translucent fog depths to authored rooms, danger/extraction breathing
 * light, and deterministic water glints with a tiny recycled ripple pool.
 * It never changes terrain, visibility, navigation, combat, or raid state.</p>
 */
public final class BukovEnvironmentFxLayer extends Group {

	static final int MAX_FOG_PATCHES = 6;
	static final int MAX_DANGER_GLOWS = 6;
	static final int MAX_EXTRACTION_GLOWS = 3;
	static final int MAX_WATER_GLINTS = 12;
	static final int MAX_RIPPLES = 4;
	static final int MAX_DRAWABLES = 37;
	private static final Budget HIGH_QUALITY_BUDGET =
			new Budget(6, 6, 3, 12, 4);
	private static final Budget BALANCED_BUDGET =
			new Budget(4, 4, 3, 8, 2);
	private static final Budget HIGH_FRAME_RATE_BUDGET =
			new Budget(2, 3, 3, 4, 1);

	static final class Budget {
		final int fogPatches;
		final int dangerGlows;
		final int extractionGlows;
		final int waterGlints;
		final int ripples;

		private Budget(
				int fogPatches,
				int dangerGlows,
				int extractionGlows,
				int waterGlints,
				int ripples) {
			this.fogPatches = fogPatches;
			this.dangerGlows = dangerGlows;
			this.extractionGlows = extractionGlows;
			this.waterGlints = waterGlints;
			this.ripples = ripples;
		}

		int maximumDrawables() {
			return fogPatches * 2
					+ dangerGlows
					+ extractionGlows
					+ waterGlints
					+ ripples;
		}
	}

	private static final class FogPatch {
		final ColorBlock deep;
		final ColorBlock drift;
		final float left;
		final float top;
		final float width;
		final float height;
		final float driftWidth;
		final float driftHeight;
		final float phase;

		private FogPatch(
				ColorBlock deep,
				ColorBlock drift,
				float left,
				float top,
				float width,
				float height,
				float driftWidth,
				float driftHeight,
				float phase) {
			this.deep = deep;
			this.drift = drift;
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
			this.driftWidth = driftWidth;
			this.driftHeight = driftHeight;
			this.phase = phase;
		}

		void present(
				float seconds,
				boolean reduceMotion,
				boolean visible) {
			deep.visible = visible;
			drift.visible = visible;
			if (!visible) return;
			float pulse = pulseAt(seconds, phase, reduceMotion);
			deep.x = left;
			deep.y = top;
			deep.alpha(0.035f + pulse * 0.025f);
			float travel = Math.max(0f, width - driftWidth);
			float driftProgress = reduceMotion
					? 0.5f
					: 0.5f + 0.5f * (float)Math.sin(
							seconds * 0.38f + phase);
			drift.x = left + travel * driftProgress;
			drift.y = top + (height - driftHeight) * 0.5f;
			drift.alpha(0.025f + (1f - pulse) * 0.035f);
		}
	}

	private static final class PulseBlock {
		final ColorBlock block;
		final float phase;
		final float minimumAlpha;
		final float alphaRange;

		private PulseBlock(
				ColorBlock block,
				float phase,
				float minimumAlpha,
				float alphaRange) {
			this.block = block;
			this.phase = phase;
			this.minimumAlpha = minimumAlpha;
			this.alphaRange = alphaRange;
		}

		void present(
				float seconds,
				boolean reduceMotion,
				boolean visible) {
			block.visible = visible;
			if (visible) {
				block.alpha(minimumAlpha
						+ pulseAt(seconds, phase, reduceMotion)
								* alphaRange);
			}
		}
	}

	private static final class WaterGlint {
		final ColorBlock block;
		final float baseX;
		final float phase;

		private WaterGlint(
				ColorBlock block, float baseX, float phase) {
			this.block = block;
			this.baseX = baseX;
			this.phase = phase;
		}

		void present(
				float seconds,
				boolean reduceMotion,
				boolean visible) {
			block.visible = visible;
			if (!visible) return;
			float pulse = pulseAt(seconds, phase, reduceMotion);
			block.x = baseX + (reduceMotion ? 0f : (pulse - 0.5f) * 3f);
			block.alpha(0.14f + pulse * 0.28f);
		}
	}

	private final List<FogPatch> fogPatches = new ArrayList<>();
	private final List<PulseBlock> dangerGlows = new ArrayList<>();
	private final List<PulseBlock> extractionGlows = new ArrayList<>();
	private final List<WaterGlint> waterGlints = new ArrayList<>();
	private final int[] waterCells;
	private final Ripple[] ripples = new Ripple[MAX_RIPPLES];
	private final BukovUiTokens tokens = BukovUiTokens.loadDefault();
	private float elapsed;
	private float rippleCooldown;
	private int nextRipple;
	private int nextWaterCell;

	public BukovEnvironmentFxLayer(BukovLevel level) {
		if (level == null || level.raidLayout() == null) {
			throw new IllegalArgumentException(
					"A generated Bukov level is required");
		}
		buildFog(level);
		buildDangerGlows(level);
		buildExtractionGlows(level);
		waterCells = selectWaterCells(level.map, MAX_WATER_GLINTS);
		buildWaterGlints(level);
		for (int index = 0; index < ripples.length; index++) {
			Ripple ripple = new Ripple();
			ripple.kill();
			ripples[index] = ripple;
			add(ripple);
		}
	}

	private void buildFog(BukovLevel level) {
		List<BukovRaidLayout.Mark> selected = new ArrayList<>();
		for (BukovRaidLayout.Mark mark : level.raidLayout().marks) {
			if (atmosphericZone(mark.zone)) {
				selected.add(mark);
				if (selected.size() == MAX_FOG_PATCHES) break;
			}
		}
		if (selected.size() < MAX_FOG_PATCHES) {
			for (BukovRaidLayout.Mark mark : level.raidLayout().marks) {
				if (selected.contains(mark)
						|| mark.zone == BukovRaidLayout.Zone.SPAWN
						|| mark.zone == BukovRaidLayout.Zone.EXTRACTION) {
					continue;
				}
				selected.add(mark);
				if (selected.size() == MAX_FOG_PATCHES) break;
			}
		}
		for (int index = 0; index < selected.size(); index++) {
			BukovRaidLayout.Mark mark = selected.get(index);
			float left = (mark.left + 1) * DungeonTilemap.SIZE;
			float top = (mark.top + 1) * DungeonTilemap.SIZE;
			float width = Math.max(
					DungeonTilemap.SIZE,
					(mark.right - mark.left - 1)
							* DungeonTilemap.SIZE);
			float height = Math.max(
					DungeonTilemap.SIZE,
					(mark.bottom - mark.top - 1)
							* DungeonTilemap.SIZE);
			float driftWidth = Math.max(
					DungeonTilemap.SIZE,
					width * 0.58f);
			float driftHeight = Math.max(
					DungeonTilemap.SIZE * 0.65f,
					height * 0.32f);
			ColorBlock deep = new ColorBlock(
					width,
					height,
					BukovUiTokens.withAlpha(level.color1, 255));
			ColorBlock drift = new ColorBlock(
					driftWidth,
					driftHeight,
					BukovUiTokens.withAlpha(level.color2, 255));
			add(deep);
			add(drift);
			fogPatches.add(new FogPatch(
					deep,
					drift,
					left,
					top,
					width,
					height,
					driftWidth,
					driftHeight,
					index * 0.73f));
		}
	}

	private void buildDangerGlows(BukovLevel level) {
		for (BukovRaidLayout.Mark mark : level.raidLayout().marks) {
			if (!dangerZone(mark.zone)
					|| dangerGlows.size() >= MAX_DANGER_GLOWS) {
				continue;
			}
			float width = Math.max(
					DungeonTilemap.SIZE,
					(mark.right - mark.left - 1)
							* DungeonTilemap.SIZE);
			float height = Math.max(
					DungeonTilemap.SIZE,
					(mark.bottom - mark.top - 1)
							* DungeonTilemap.SIZE);
			ColorBlock glow = new ColorBlock(
					width,
					height,
					tokens.colorWithAlpha("accent.danger", 255));
			glow.x = (mark.left + 1) * DungeonTilemap.SIZE;
			glow.y = (mark.top + 1) * DungeonTilemap.SIZE;
			add(glow);
			dangerGlows.add(new PulseBlock(
					glow,
					dangerGlows.size() * 0.91f,
					0.025f,
					0.055f));
		}
	}

	private void buildExtractionGlows(BukovLevel level) {
		for (ExtractionDefinition extraction :
				level.raidLayout().extractions) {
			if (extractionGlows.size() >= MAX_EXTRACTION_GLOWS
					|| extraction.interactionCell < 0
					|| extraction.interactionCell >= level.length()) {
				continue;
			}
			float size = DungeonTilemap.SIZE * 1.5f;
			float centerX = (extraction.interactionCell % level.width()
					+ 0.5f) * DungeonTilemap.SIZE;
			float centerY = (extraction.interactionCell / level.width()
					+ 0.5f) * DungeonTilemap.SIZE;
			ColorBlock glow = new ColorBlock(
					size,
					size,
					tokens.colorWithAlpha("accent.extract", 255));
			glow.x = centerX - size * 0.5f;
			glow.y = centerY - size * 0.5f;
			add(glow);
			extractionGlows.add(new PulseBlock(
					glow,
					extractionGlows.size() * 1.17f,
					0.07f,
					0.15f));
		}
	}

	private void buildWaterGlints(BukovLevel level) {
		for (int index = 0; index < waterCells.length; index++) {
			int cell = waterCells[index];
			float width = DungeonTilemap.SIZE * 0.62f;
			ColorBlock glint = new ColorBlock(
					width,
					1f,
					tokens.colorWithAlpha("text.primary", 255));
			float baseX = (cell % level.width()) * DungeonTilemap.SIZE
					+ (DungeonTilemap.SIZE - width) * 0.5f;
			glint.x = baseX;
			glint.y = (cell / level.width()) * DungeonTilemap.SIZE
					+ DungeonTilemap.SIZE * 0.42f;
			add(glint);
			waterGlints.add(new WaterGlint(
					glint, baseX, index * 0.67f));
		}
	}

	@Override
	public synchronized void update() {
		boolean reduceMotion = SPDSettings.bukovReduceMotion();
		int profile = SPDSettings.bukovPerformanceProfile();
		Budget budget = budgetFor(profile);
		if (!reduceMotion) {
			elapsed = (elapsed + Game.elapsed) % 120f;
		}
		for (int index = 0; index < fogPatches.size(); index++) {
			fogPatches.get(index).present(
					elapsed,
					reduceMotion,
					index < budget.fogPatches);
		}
		for (int index = 0; index < dangerGlows.size(); index++) {
			dangerGlows.get(index).present(
					elapsed,
					reduceMotion,
					index < budget.dangerGlows);
		}
		for (int index = 0; index < extractionGlows.size(); index++) {
			extractionGlows.get(index).present(
					elapsed,
					reduceMotion,
					index < budget.extractionGlows);
		}
		for (int index = 0; index < waterGlints.size(); index++) {
			waterGlints.get(index).present(
					elapsed,
					reduceMotion,
					index < budget.waterGlints);
		}
		updateRipples(reduceMotion, profile, budget.ripples);
		super.update();
	}

	private void updateRipples(
			boolean reduceMotion,
			int profile,
			int rippleBudget) {
		for (int index = rippleBudget; index < ripples.length; index++) {
			if (ripples[index].exists) ripples[index].kill();
		}
		if (reduceMotion || rippleBudget == 0 || waterCells.length == 0) {
			for (int index = 0; index < rippleBudget; index++) {
				if (ripples[index].exists) ripples[index].kill();
			}
			rippleCooldown = 0f;
			return;
		}
		rippleCooldown -= Game.elapsed;
		if (rippleCooldown > 0f) return;
		int poolIndex = nextRipple++ % rippleBudget;
		int waterCell = waterCells[nextWaterCell++ % waterCells.length];
		ripples[poolIndex].reset(waterCell);
		rippleCooldown = rippleInterval(profile);
	}

	static Budget budgetFor(int profile) {
		switch (profile) {
			case BukovPerformancePolicy.HIGH_QUALITY:
				return HIGH_QUALITY_BUDGET;
			case BukovPerformancePolicy.BALANCED:
				return BALANCED_BUDGET;
			case BukovPerformancePolicy.HIGH_FRAME_RATE:
				return HIGH_FRAME_RATE_BUDGET;
			default:
				throw new IllegalArgumentException(
						"unknown Bukov performance profile: " + profile);
		}
	}

	static float rippleInterval(int profile) {
		switch (profile) {
			case BukovPerformancePolicy.HIGH_QUALITY:
				return 0.45f;
			case BukovPerformancePolicy.BALANCED:
				return 0.65f;
			case BukovPerformancePolicy.HIGH_FRAME_RATE:
				return 0.90f;
			default:
				throw new IllegalArgumentException(
						"unknown Bukov performance profile: " + profile);
		}
	}

	static float pulseAt(
			float seconds, float phase, boolean reduceMotion) {
		if (reduceMotion) return 0.62f;
		return 0.5f + 0.5f * (float)Math.sin(
				seconds * 1.35f + phase);
	}

	static boolean dangerZone(BukovRaidLayout.Zone zone) {
		return zone == BukovRaidLayout.Zone.HAZARD
				|| zone == BukovRaidLayout.Zone.HIGH_VALUE
				|| zone == BukovRaidLayout.Zone.BOSS;
	}

	private static boolean atmosphericZone(BukovRaidLayout.Zone zone) {
		return zone == BukovRaidLayout.Zone.COMBAT
				|| dangerZone(zone)
				|| zone == BukovRaidLayout.Zone.TRANSIT;
	}

	static int[] selectWaterCells(int[] map, int limit) {
		if (map == null) {
			throw new IllegalArgumentException("map is required");
		}
		if (limit < 0) {
			throw new IllegalArgumentException(
					"water glint limit must be non-negative");
		}
		int waterCount = 0;
		for (int terrain : map) {
			if (terrain == Terrain.WATER) waterCount++;
		}
		int selectedCount = Math.min(waterCount, limit);
		int[] result = new int[selectedCount];
		if (selectedCount == 0) return result;
		double stride = waterCount / (double)selectedCount;
		int targetOrdinal = (int)(stride * 0.5);
		int waterOrdinal = 0;
		int resultIndex = 0;
		for (int cell = 0;
				cell < map.length && resultIndex < result.length;
				cell++) {
			if (map[cell] != Terrain.WATER) continue;
			if (waterOrdinal == targetOrdinal) {
				result[resultIndex++] = cell;
				targetOrdinal = (int)((resultIndex + 0.5) * stride);
			}
			waterOrdinal++;
		}
		return result;
	}
}
