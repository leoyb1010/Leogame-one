package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the encounter from regressing to one generic proximity action. */
public class WhiteLineBossMechanismWiringGuardTest {

	@Test
	public void runtimeUsesThreeDistinctWorldSpaceInteractions()
			throws Exception {
		String world = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains("boss.bossState.flankUmbrella("));
		assertTrue(world.contains("boss.bossState.identifyTrueBody("));
		assertTrue(world.contains("boss.bossState.disableFogLamp("));
		assertTrue(world.contains("bodyTraceWithinRange(hero.pos)"));
		assertTrue(world.contains("withinInteractionRange(hero.pos, pumpCell)"));
		assertTrue(world.contains("BOSS_SYNCHRONIZED_TRACE"));
		assertFalse(world.contains(".completeObjective("));
	}

	@Test
	public void encounterIsSeededAndRestoredWithoutGlobalRandom()
			throws Exception {
		String state = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ai/WhiteLineBossStateMachine.java");
		String level = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/levels/BukovLevel.java");

		assertTrue(state.contains("implements Bundlable"));
		assertTrue(state.contains("deterministicBodyIndex("));
		assertFalse(state.contains("com.watabou.utils.Random"));
		assertFalse(state.contains("Random."));
		assertTrue(level.contains("WHITE_LINE_STATE"));
		assertTrue(level.contains("ensureBossMechanism("));
		assertTrue(level.contains("bundle.put(WHITE_LINE_STATE"));
	}

	@Test
	public void nonBossExtractionBypassRemainsAvailable() throws Exception {
		String world = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(world.contains("bypassWhiteLineForExtraction()"));
		assertTrue(world.contains("bossState.bypass(available)"));
		assertTrue(world.contains("nonBossExtractionAvailable()"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
