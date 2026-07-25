package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovInteractionReachabilityTest {

	private static final int WIDTH = 7;
	private static final int HEIGHT = 7;

	@Test
	public void orthogonalDoorOrMechanismCellRemainsOperable() {
		TraceMap map = new TraceMap(WIDTH, HEIGHT);
		int hero = cell(3, 3);
		int obstructingTarget = cell(4, 3);
		map.lineBlocked[obstructingTarget] = true;

		assertTrue(reachable(hero, obstructingTarget, map));
	}

	@Test
	public void openDiagonalIsReachable() {
		assertTrue(reachable(
				cell(3, 3),
				cell(4, 4),
				new TraceMap(WIDTH, HEIGHT)));
	}

	@Test
	public void eitherBlockedCornerSeamRejectsDiagonalInteraction() {
		int hero = cell(3, 3);
		int target = cell(4, 4);
		TraceMap horizontalWall = new TraceMap(WIDTH, HEIGHT);
		horizontalWall.lineBlocked[cell(4, 3)] = true;
		assertFalse(reachable(hero, target, horizontalWall));

		TraceMap verticalWall = new TraceMap(WIDTH, HEIGHT);
		verticalWall.lineBlocked[cell(3, 4)] = true;
		assertFalse(reachable(hero, target, verticalWall));
	}

	@Test
	public void obstructingTargetIsReachableOnlyOrthogonally() {
		int hero = cell(3, 3);
		TraceMap orthogonalMap = new TraceMap(WIDTH, HEIGHT);
		int orthogonalTarget = cell(4, 3);
		orthogonalMap.lineBlocked[orthogonalTarget] = true;
		assertTrue(reachable(hero, orthogonalTarget, orthogonalMap));

		TraceMap diagonalMap = new TraceMap(WIDTH, HEIGHT);
		int diagonalTarget = cell(4, 4);
		diagonalMap.lineBlocked[diagonalTarget] = true;
		assertFalse(reachable(hero, diagonalTarget, diagonalMap));
	}

	@Test
	public void openingTheCornerImmediatelyRestoresReach() {
		int hero = cell(3, 3);
		int target = cell(4, 4);
		TraceMap map = new TraceMap(WIDTH, HEIGHT);
		map.lineBlocked[cell(4, 3)] = true;
		assertFalse(reachable(hero, target, map));

		map.lineBlocked[cell(4, 3)] = false;
		assertTrue(reachable(hero, target, map));
	}

	@Test
	public void invalidOrDistantTargetsAreRejected() {
		TraceMap map = new TraceMap(WIDTH, HEIGHT);
		assertFalse(reachable(cell(1, 1), cell(3, 1), map));
		assertFalse(reachable(-1, cell(1, 1), map));
		assertFalse(reachable(cell(1, 1), WIDTH * HEIGHT, map));
		assertFalse(BukovRealtimeWorld.withinInteractionRange(
				cell(1, 1),
				cell(2, 2),
				WIDTH,
				WIDTH * HEIGHT,
				null));
	}

	private static boolean reachable(
			int firstCell, int secondCell, CollisionMap map) {
		return BukovRealtimeWorld.withinInteractionRange(
				firstCell,
				secondCell,
				WIDTH,
				WIDTH * HEIGHT,
				map);
	}

	private static int cell(int x, int y) {
		return x + y * WIDTH;
	}

	private static final class TraceMap implements CollisionMap {

		private final int width;
		private final int height;
		private final boolean[] lineBlocked;

		TraceMap(int width, int height) {
			this.width = width;
			this.height = height;
			lineBlocked = new boolean[width * height];
		}

		@Override
		public int width() {
			return width;
		}

		@Override
		public int height() {
			return height;
		}

		@Override
		public boolean blocked(int x, int y) {
			return false;
		}

		@Override
		public boolean blocksLine(int x, int y) {
			return x < 0
					|| y < 0
					|| x >= width
					|| y >= height
					|| lineBlocked[x + y * width];
		}
	}
}
