package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovFootstepWiringGuardTest {

	@Test
	public void realtimeWorldRoutesAcceptedMovementThroughSfxBus()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains("footstepCadence.advance("));
		assertTrue(world.contains("heroBody.x - heroBody.previousX"));
		assertTrue(world.contains("FootstepSurface.resolve("));
		assertTrue(world.contains("playSfx("));
		assertTrue(world.contains("footstepSurface.asset(footstepSequence)"));
		assertTrue(world.contains("footstepSurface.gain()"));
		assertFalse(world.contains(
				"Sample.INSTANCE.play(footstepSurface"));
	}

	@Test
	public void generatorAndAssetBankOwnAllSixOriginalVariants()
			throws Exception {
		String generator = source("../scripts/generate_bukov_sfx.mjs");
		String assets = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java");
		for (String surface : new String[]{"hard", "water", "metal"}) {
			assertTrue(generator.contains(
					"sounds[`footstep_${surface}_${variant + 1}`]"));
			assertTrue(assets.contains(
					"sounds/bukov/footstep_" + surface + "_1.wav"));
			assertTrue(assets.contains(
					"sounds/bukov/footstep_" + surface + "_2.wav"));
		}
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
