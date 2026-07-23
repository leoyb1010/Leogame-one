package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovEnemySpawnPlannerTest {

	@Test
	public void roomSemanticsProduceProtectedOptionalAndBossPoints() {
		int width = 26;
		int height = 8;
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.WALL);
		BukovRaidLayout layout = new BukovRaidLayout();
		BukovRaidLayout.Mark spawn = mark(1, BukovRaidLayout.Zone.SPAWN);
		BukovRaidLayout.Mark combat = mark(7, BukovRaidLayout.Zone.COMBAT);
		BukovRaidLayout.Mark optional =
				mark(13, BukovRaidLayout.Zone.HIGH_VALUE);
		BukovRaidLayout.Mark boss = mark(19, BukovRaidLayout.Zone.BOSS);
		layout.marks.addAll(Arrays.asList(spawn, combat, optional, boss));
		layout.links.add(new BukovRaidLayout.Link(
				spawn.roomId(), combat.roomId()));
		layout.links.add(new BukovRaidLayout.Link(
				combat.roomId(), optional.roomId()));
		layout.links.add(new BukovRaidLayout.Link(
				optional.roomId(), boss.roomId()));
		for (BukovRaidLayout.Mark mark : layout.marks) {
			paintInterior(map, width, mark);
		}

		List<BukovEnemySpawnPlanner.SpawnPoint> points =
				BukovEnemySpawnPlanner.plan(width, height, map, layout);

		assertEquals(27, points.size());
		assertFalse(hasPointIn(points, 1, 5, width));
		BukovEnemySpawnPlanner.SpawnPoint mandatory =
				pointAt(points, 8 + 2 * width);
		assertEquals(1, mandatory.distanceFromSpawnRooms);
		assertTrue(mandatory.mandatorySingleRoute);
		assertFalse(mandatory.bossArena);
		BukovEnemySpawnPlanner.SpawnPoint optionalPoint =
				pointAt(points, 14 + 2 * width);
		assertEquals(2, optionalPoint.distanceFromSpawnRooms);
		assertFalse(optionalPoint.mandatorySingleRoute);
		assertFalse(optionalPoint.bossArena);
		BukovEnemySpawnPlanner.SpawnPoint bossPoint =
				pointAt(points, 20 + 2 * width);
		assertEquals(3, bossPoint.distanceFromSpawnRooms);
		assertFalse(bossPoint.mandatorySingleRoute);
		assertTrue(bossPoint.bossArena);
	}

	private static BukovRaidLayout.Mark mark(
			int left,
			BukovRaidLayout.Zone zone) {
		return new BukovRaidLayout.Mark(
				left, 1, left + 4, 5, zone, "");
	}

	private static void paintInterior(
			int[] map,
			int width,
			BukovRaidLayout.Mark mark) {
		for (int y = mark.top + 1; y < mark.bottom; y++) {
			for (int x = mark.left + 1; x < mark.right; x++) {
				map[x + y * width] = Terrain.EMPTY;
			}
		}
	}

	private static boolean hasPointIn(
			List<BukovEnemySpawnPlanner.SpawnPoint> points,
			int left,
			int right,
			int width) {
		for (BukovEnemySpawnPlanner.SpawnPoint point : points) {
			int x = point.cell % width;
			if (x >= left && x <= right) return true;
		}
		return false;
	}

	private static BukovEnemySpawnPlanner.SpawnPoint pointAt(
			List<BukovEnemySpawnPlanner.SpawnPoint> points,
			int cell) {
		for (BukovEnemySpawnPlanner.SpawnPoint point : points) {
			if (point.cell == cell) return point;
		}
		throw new AssertionError("missing point " + cell);
	}
}
