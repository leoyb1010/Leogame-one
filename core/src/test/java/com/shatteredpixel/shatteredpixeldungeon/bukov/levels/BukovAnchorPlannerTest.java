package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovAnchorPlannerTest {

	@Test
	public void assignsThreeDistinctExtractionsAndLootCells() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(445566L);
		Surface surface = surface(layout);
		BukovRaidLayout.Mark baselineRoom =
				layout.mark(layout.extraction("E01").roomId);
		int preferredExit = baselineRoom.left
				+ ((baselineRoom.top + baselineRoom.bottom) / 2) * surface.width;

		BukovAnchorPlanner.Result result = BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map, layout, -1, preferredExit);

		assertTrue(result.reason, result.valid);
		assertEquals(preferredExit, layout.extraction("E01").interactionCell);
		assertEquals(3, layout.lootAnchors.size());

		Set<Integer> cells = new HashSet<>();
		for (ExtractionDefinition extraction : layout.extractions) {
			assertTrue(cells.add(extraction.interactionCell));
		}
		Set<String> lootRooms = new HashSet<>();
		Set<BukovRaidLayout.Zone> lootZones = new HashSet<>();
		for (BukovRaidLayout.LootAnchor anchor : layout.lootAnchors) {
			assertTrue(cells.add(anchor.cell));
			assertTrue(lootRooms.add(anchor.roomId));
			assertTrue(lootZones.add(layout.mark(anchor.roomId).zone));
		}
		BukovRaidLayout.BossMechanism boss = layout.bossMechanism();
		assertNotNull(boss);
		assertEquals(4, boss.bodyTraceCells.length);
		assertEquals(
				"fog_lamp_pump_station", boss.fogLampAnchorId);
		assertTrue(cells.add(boss.fogLampCell));
		BukovRaidLayout.Mark bossRoom = layout.mark(boss.bossRoomId);
		assertEquals(BukovRaidLayout.Zone.BOSS, bossRoom.zone);
		for (int bodyCell : boss.bodyTraceCells) {
			assertTrue(cells.add(bodyCell));
			assertTrue(bodyCell % surface.width >= bossRoom.left);
			assertTrue(bodyCell % surface.width <= bossRoom.right);
			assertTrue(bodyCell / surface.width >= bossRoom.top);
			assertTrue(bodyCell / surface.width <= bossRoom.bottom);
		}
	}

	@Test
	public void sameSeedAndTerrainProduceSameAnchors() {
		BukovRaidLayout first = BukovZonePlanner.generateFirstRaid(778899L);
		BukovRaidLayout second = BukovZonePlanner.generateFirstRaid(778899L);
		Surface surface = surface(first);

		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map, first, -1, -1).valid);
		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map, second, -1, -1).valid);

		for (int i = 0; i < first.extractions.size(); i++) {
			assertEquals(first.extractions.get(i).interactionCell,
					second.extractions.get(i).interactionCell);
		}
		for (int i = 0; i < first.lootAnchors.size(); i++) {
			assertEquals(first.lootAnchors.get(i).roomId,
					second.lootAnchors.get(i).roomId);
			assertEquals(first.lootAnchors.get(i).cell,
					second.lootAnchors.get(i).cell);
		}
		assertArrayEquals(
				first.bossMechanism().bodyTraceCells,
				second.bossMechanism().bodyTraceCells);
		assertEquals(
				first.bossMechanism().fogLampCell,
				second.bossMechanism().fogLampCell);
	}

	@Test
	public void missingBossAnchorsMigrateWithoutMovingExistingAnchors() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(221144L);
		Surface surface = surface(layout);
		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map,
				layout, -1, -1).valid);
		int extraction = layout.extraction("E01").interactionCell;
		int loot = layout.lootAnchors.get(0).cell;
		int[] expectedBodies =
				layout.bossMechanism().bodyTraceCells.clone();
		int expectedFog = layout.bossMechanism().fogLampCell;

		layout.bossMechanism(null);
		assertTrue(BukovAnchorPlanner.ensureBossMechanism(
				surface.width, surface.height, surface.map, layout).valid);

		assertEquals(extraction, layout.extraction("E01").interactionCell);
		assertEquals(loot, layout.lootAnchors.get(0).cell);
		assertArrayEquals(
				expectedBodies, layout.bossMechanism().bodyTraceCells);
		assertEquals(expectedFog, layout.bossMechanism().fogLampCell);
	}

	@Test
	public void bossSceneAnchorsRoundTripWithRaidLayout() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(771133L);
		Surface surface = surface(layout);
		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map,
				layout, -1, -1).valid);

		Bundle bundle = new Bundle();
		bundle.put("layout", layout);
		BukovRaidLayout restored =
				(BukovRaidLayout)bundle.get("layout");

		assertNotNull(restored.bossMechanism());
		assertArrayEquals(
				layout.bossMechanism().bodyTraceCells,
				restored.bossMechanism().bodyTraceCells);
		assertEquals(
				layout.bossMechanism().fogLampCell,
				restored.bossMechanism().fogLampCell);
		assertTrue(BukovAnchorPlanner.validate(
				surface.width, surface.height, surface.map,
				restored).valid);
	}

	private static Surface surface(BukovRaidLayout layout) {
		int width = 0;
		int height = 0;
		for (BukovRaidLayout.Mark mark : layout.marks) {
			width = Math.max(width, mark.right + 2);
			height = Math.max(height, mark.bottom + 2);
		}
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.EMPTY);
		return new Surface(width, height, map);
	}

	private static final class Surface {
		final int width;
		final int height;
		final int[] map;

		Surface(int width, int height, int[] map) {
			this.width = width;
			this.height = height;
			this.map = map;
		}
	}
}
