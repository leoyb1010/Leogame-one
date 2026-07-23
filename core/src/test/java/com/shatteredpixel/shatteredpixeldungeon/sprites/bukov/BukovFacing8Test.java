package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovFacing8Test {

	@Test
	public void resolvesEveryScreenSpaceOctantToManifestRow() {
		assertEquals(BukovFacing8.N, BukovFacing8.resolve(0, -1));
		assertEquals(BukovFacing8.NE, BukovFacing8.resolve(1, -1));
		assertEquals(BukovFacing8.E, BukovFacing8.resolve(1, 0));
		assertEquals(BukovFacing8.SE, BukovFacing8.resolve(1, 1));
		assertEquals(BukovFacing8.S, BukovFacing8.resolve(0, 1));
		assertEquals(BukovFacing8.SW, BukovFacing8.resolve(-1, 1));
		assertEquals(BukovFacing8.W, BukovFacing8.resolve(-1, 0));
		assertEquals(BukovFacing8.NW, BukovFacing8.resolve(-1, -1));
	}

	@Test
	public void stationaryFallbackFacesSouth() {
		assertEquals(BukovFacing8.S, BukovFacing8.resolve(0, 0));
	}
}
