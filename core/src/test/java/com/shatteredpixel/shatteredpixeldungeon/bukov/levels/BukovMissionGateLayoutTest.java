package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovMissionGateLayoutTest {

	@Test
	public void assignsStableArchiveAndRealGateCells() {
		BukovRaidLayout first = BukovZonePlanner.generateFirstRaid(991177L);
		BukovRaidLayout second = BukovZonePlanner.generateFirstRaid(991177L);
		Surface surface = surface(first);

		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map,
				first, -1, -1).valid);
		assertTrue(BukovAnchorPlanner.assign(
				surface.width, surface.height, surface.map,
				second, -1, -1).valid);

		BukovRaidLayout.MissionGate gate = first.missionGate();
		assertNotNull(gate);
		assertEquals("south_maintenance",
				first.mark(gate.archiveRoomId).semanticId);
		assertEquals("fog_lamp_pump_station",
				first.mark(gate.gateRoomId).semanticId);
		assertTrue(gate.gateCells.length > 0);
		assertEquals(gate.gateCell, gate.gateCells[0]);
		assertTrue(gate.archiveCell != gate.gateCell);
		assertEquals(gate.archiveCell, second.missionGate().archiveCell);
		assertEquals(gate.gateCell, second.missionGate().gateCell);
		assertTrue(BukovAnchorPlanner.validate(
				surface.width, surface.height, surface.map, first).valid);

		Bundle bundle = new Bundle();
		bundle.put("layout", first);
		BukovRaidLayout restored =
				(BukovRaidLayout) bundle.get("layout");
		assertNotNull(restored.missionGate());
		assertEquals(gate.archiveCell,
				restored.missionGate().archiveCell);
		assertEquals(gate.gateCell,
				restored.missionGate().gateCell);
		assertTrue(Arrays.equals(
				gate.gateCells, restored.missionGate().gateCells));

		BukovRaidLayout.Mark pump = first.mark(gate.gateRoomId);
		int target = (pump.left + pump.right) / 2
				+ (pump.top + pump.bottom) / 2 * surface.width;
		int start = outsidePumpCell(first, pump, surface.width);
		Set<Integer> closedGate = new HashSet<>();
		for (int cell : gate.gateCells) closedGate.add(cell);
		assertFalse(reachable(
				start, target, surface.width, surface.height,
				closedGate));
		assertTrue(reachable(
				start, target, surface.width, surface.height,
				new HashSet<Integer>()));
	}

	private static int outsidePumpCell(
			BukovRaidLayout layout,
			BukovRaidLayout.Mark pump,
			int width) {
		for (BukovRaidLayout.Mark mark : layout.marks) {
			if (!mark.roomId().equals(pump.roomId())) {
				return (mark.left + mark.right) / 2
						+ (mark.top + mark.bottom) / 2 * width;
			}
		}
		throw new AssertionError("No room outside the pump station");
	}

	private static boolean reachable(
			int start,
			int target,
			int width,
			int height,
			Set<Integer> blocked) {
		boolean[] seen = new boolean[width * height];
		ArrayDeque<Integer> open = new ArrayDeque<>();
		if (blocked.contains(start) || blocked.contains(target)) return false;
		seen[start] = true;
		open.add(start);
		int[] offsets = {-1, 1, -width, width};
		while (!open.isEmpty()) {
			int cell = open.removeFirst();
			if (cell == target) return true;
			int x = cell % width;
			int y = cell / width;
			for (int offset : offsets) {
				int next = cell + offset;
				if (next < 0 || next >= seen.length
						|| offset == -1 && x == 0
						|| offset == 1 && x == width - 1
						|| offset == -width && y == 0
						|| offset == width && y == height - 1
						|| seen[next] || blocked.contains(next)) continue;
				seen[next] = true;
				open.add(next);
			}
		}
		return false;
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
