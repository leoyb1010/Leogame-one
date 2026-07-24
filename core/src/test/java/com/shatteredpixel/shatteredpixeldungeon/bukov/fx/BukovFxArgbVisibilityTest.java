package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Noosa ColorBlock colors are ARGB. A six-digit RGB literal therefore creates
 * a texture whose alpha byte is zero even when the Visual's runtime alpha is 1.
 */
public class BukovFxArgbVisibilityTest {

	@Test
	public void everyBallisticColorBlockTextureHasOpaqueArgbAlpha() {
		assertOpaque(BukovTracerFx.FRIENDLY_COLOR);
		assertOpaque(BukovTracerFx.HOSTILE_COLOR);
		assertOpaque(BukovMuzzleFx.FRIENDLY_COLOR);
		assertOpaque(BukovImpactFx.FRIENDLY_COLOR);
		assertOpaque(BukovShellFx.FRIENDLY_COLOR);
		assertOpaque(BukovShellFx.HOSTILE_COLOR);
	}

	private static void assertOpaque(int argb) {
		assertEquals(0xFF, argb >>> 24);
	}
}
