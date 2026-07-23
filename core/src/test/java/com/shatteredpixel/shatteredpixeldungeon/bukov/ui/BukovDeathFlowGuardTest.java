package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents raid death from falling back into the host campaign restart UI. */
public class BukovDeathFlowGuardTest {

	@Test
	public void heroDeathReturnsBeforeClassicDeathPipeline() throws Exception {
		String source = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java");
		String die = between(
				source,
				"public void die( Object cause )",
				"public static void reallyDie");
		int guard = die.indexOf("if (BukovMode.active())");
		int classicPipeline = die.indexOf("Ankh ankh");
		assertTrue(guard >= 0);
		assertTrue(classicPipeline > guard);
		String bukovBranch = die.substring(
				guard, die.indexOf("Ankh ankh", guard));
		assertTrue(bukovBranch.contains("super.die(cause)"));
		assertTrue(bukovBranch.contains("return;"));
		assertFalse(bukovBranch.contains("reallyDie"));
	}

	@Test
	public void gameOverCannotExposeHeroSelectInBukovMode() throws Exception {
		String source = read(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String gameOver = between(
				source,
				"public static void gameOver()",
				"public static void bossSlain()");
		int guard = gameOver.indexOf("if (BukovMode.active())");
		int selector = gameOver.indexOf("HeroSelectScene.class");
		assertTrue(guard >= 0);
		assertTrue(selector > guard);
		String bukovBranch = gameOver.substring(
				guard, gameOver.indexOf("Banner gameOver", guard));
		assertTrue(bukovBranch.contains("return;"));
	}

	private static String read(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}
}
