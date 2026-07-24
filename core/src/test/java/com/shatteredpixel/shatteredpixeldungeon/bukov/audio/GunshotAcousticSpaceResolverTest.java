package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GunshotAcousticSpaceResolverTest {

	@Test
	public void enclosureChoosesIndoorCorridorAndOpenTails() {
		assertEquals(
				GunshotAcousticSpace.INDOOR,
				GunshotAcousticSpaceResolver.resolve(
						new ProbeMap(true, true),
						10.5f,
						10.5f));
		assertEquals(
				GunshotAcousticSpace.CORRIDOR,
				GunshotAcousticSpaceResolver.resolve(
						new ProbeMap(true, false),
						10.5f,
						10.5f));
		assertEquals(
				GunshotAcousticSpace.OPEN,
				GunshotAcousticSpaceResolver.resolve(
						new ProbeMap(false, false),
						10.5f,
						10.5f));
	}

	@Test
	public void everySpaceHasThreeDeterministicTailVariants() {
		Set<String> all = new HashSet<>();
		for (GunshotAcousticSpace space : GunshotAcousticSpace.values()) {
			Set<String> variants = new HashSet<>();
			for (int sequence = 0; sequence < 3; sequence++) {
				variants.add(space.tailAsset(sequence));
				all.add(space.tailAsset(sequence));
			}
			assertEquals(3, variants.size());
			assertEquals(space.tailAsset(0), space.tailAsset(3));
		}
		assertEquals(9, all.size());
	}

	private static final class ProbeMap implements CollisionMap {

		private final boolean verticalWalls;
		private final boolean horizontalWalls;

		private ProbeMap(boolean verticalWalls, boolean horizontalWalls) {
			this.verticalWalls = verticalWalls;
			this.horizontalWalls = horizontalWalls;
		}

		@Override
		public int width() {
			return 32;
		}

		@Override
		public int height() {
			return 32;
		}

		@Override
		public boolean blocked(int x, int y) {
			return (verticalWalls && (y == 8 || y == 12))
					|| (horizontalWalls && (x == 8 || x == 12));
		}
	}
}
