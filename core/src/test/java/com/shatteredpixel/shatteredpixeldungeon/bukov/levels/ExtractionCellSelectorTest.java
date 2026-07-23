package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExtractionCellSelectorTest {

	@Test
	public void selectsWalkableCenterOfMarkedRoom() {
		int width = 9;
		int height = 9;
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.WALL);
		fill(map, width, 2, 2, 6, 6, Terrain.EMPTY);
		BukovRaidLayout.Mark mark = new BukovRaidLayout.Mark(
				1,
				1,
				7,
				7,
				BukovRaidLayout.Zone.EXTRACTION,
				"E01"
		);

		assertEquals(
				4 + 4 * width,
				ExtractionCellSelector.select(
						width,
						height,
						map,
						mark,
						-1,
						-1
				)
		);
	}

	@Test
	public void skipsBlockedAndForbiddenCenterDeterministically() {
		int width = 9;
		int height = 9;
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.WALL);
		fill(map, width, 2, 2, 6, 6, Terrain.EMPTY);
		BukovRaidLayout.Mark mark = new BukovRaidLayout.Mark(
				1,
				1,
				7,
				7,
				BukovRaidLayout.Zone.EXTRACTION,
				"E01"
		);
		int center = 4 + 4 * width;
		map[center] = Terrain.WALL;
		int firstTie = 4 + 3 * width;

		assertEquals(
				3 + 4 * width,
				ExtractionCellSelector.select(
						width,
						height,
						map,
						mark,
						firstTie,
						-1
				)
		);
	}

	@Test
	public void returnsMinusOneWhenRoomHasNoWalkableInterior() {
		int width = 7;
		int height = 7;
		int[] map = new int[width * height];
		Arrays.fill(map, Terrain.WALL);
		BukovRaidLayout.Mark mark = new BukovRaidLayout.Mark(
				1,
				1,
				5,
				5,
				BukovRaidLayout.Zone.EXTRACTION,
				"E01"
		);

		assertEquals(
				-1,
				ExtractionCellSelector.select(
						width,
						height,
						map,
						mark,
						-1,
						-1
				)
		);
	}

	private static void fill(int[] map,
							 int width,
							 int left,
							 int top,
							 int right,
							 int bottom,
							 int terrain) {
		for (int y = top; y <= bottom; y++) {
			for (int x = left; x <= right; x++) {
				map[x + y * width] = terrain;
			}
		}
	}
}
