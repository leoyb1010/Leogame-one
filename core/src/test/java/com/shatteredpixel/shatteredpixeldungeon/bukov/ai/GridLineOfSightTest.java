package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GridLineOfSightTest {

	@Test
	public void clearGridHasLineOfSight() {
		assertTrue(GridLineOfSight.visible(
				1.5f,
				1.5f,
				5.5f,
				3.5f,
				8f,
				new TestMap(8, 8)
		));
	}

	@Test
	public void solidCellBlocksLineOfSight() {
		TestMap map = new TestMap(8, 8);
		map.blocked[3 + 2 * map.width] = true;

		assertFalse(GridLineOfSight.visible(
				1.5f,
				1.5f,
				5.5f,
				3.5f,
				8f,
				map
		));
	}

	@Test
	public void maximumDistanceIsEnforcedBeforeGridWalk() {
		assertFalse(GridLineOfSight.visible(
				1.5f,
				1.5f,
				7.5f,
				1.5f,
				4f,
				new TestMap(10, 4)
		));
	}

	private static final class TestMap implements CollisionMap {
		private final int width;
		private final int height;
		private final boolean[] blocked;

		private TestMap(int width, int height) {
			this.width = width;
			this.height = height;
			blocked = new boolean[width * height];
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
			return x < 0 || y < 0 || x >= width || y >= height
					|| blocked[x + y * width];
		}
	}
}
