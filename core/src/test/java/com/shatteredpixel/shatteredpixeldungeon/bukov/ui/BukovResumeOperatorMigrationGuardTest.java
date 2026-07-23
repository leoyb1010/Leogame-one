package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovResumeOperatorMigrationGuardTest {

	@Test
	public void gameSceneRestoresModeAndOperatorBeforeSpriteCreation()
			throws Exception {
		String source = read(
				"core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/GameScene.java");
		int guard = source.indexOf(
				"if (BukovMode.ensureActiveForHostState())");
		int normalize = source.indexOf(
				"BukovOperator.normalize(Dungeon.hero)", guard);
		int superCreate = source.indexOf("super.create()", normalize);

		assertTrue(guard >= 0);
		assertTrue(normalize > guard);
		assertTrue(superCreate > normalize);
		assertTrue(source.substring(guard, superCreate)
				.contains("InterlevelScene.mode = InterlevelScene.Mode.NONE"));
	}

	@Test
	public void heroSpriteUsesDurableBukovHostBoundary()
			throws Exception {
		String source = read(
				"core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/sprites/HeroSprite.java");
		assertTrue(source.contains(
				"bukovOperator = BukovMode.ensureActiveForHostState()"));
		assertTrue(source.contains(
				"? Assets.Sprites.BUKOV_OPERATOR"));
	}

	@Test
	public void classicDungeonReturnCopyCannotRunOnBukovBranch()
			throws Exception {
		String source = read(
				"core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/GameScene.java");
		assertTrue(source.contains(
				"if (BukovMode.active()) {\n"
						+ "\t\t\tInterlevelScene.mode = "
						+ "InterlevelScene.Mode.NONE;\n"
						+ "\t\t} else switch (InterlevelScene.mode)"));
		assertTrue(source.contains(
				"GLog.h(Messages.get(this, \"return\"), Dungeon.depth)"));
	}

	private static String read(String relative) throws Exception {
		Path path = Paths.get(relative);
		if (!Files.exists(path) && relative.startsWith("core/")) {
			path = Paths.get(relative.substring("core/".length()));
		}
		return new String(
				Files.readAllBytes(path),
				StandardCharsets.UTF_8);
	}
}
