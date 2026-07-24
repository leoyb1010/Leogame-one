package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovOperator;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the primary Bukov path from falling back into classic game UI. */
public class BukovEntryFlowGuardTest {

	@Test
	public void operatorUsesOneStableHostClass() {
		assertEquals(HeroClass.ROGUE, BukovOperator.HOST_CLASS);
	}

	@Test
	public void titleEntryRoutesOnlyToDedicatedBukovHubScene() throws Exception {
		String title = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");
		String bukovEntry = between(
				title,
				"private void openBukovMode()",
				"private abstract class TacticalTitleButton");

		assertTrue(bukovEntry.contains("BukovMode.enter()"));
		assertTrue(bukovEntry.contains(
				"GamesInProgress.curSlot = BukovMode.SAVE_SLOT"));
		assertTrue(bukovEntry.contains("BukovHubScene.class"));
		assertFalse(bukovEntry.contains("BukovDeploymentScene.class"));
		assertFalse(bukovEntry.contains("new WndBukovHub"));
		assertFalse(bukovEntry.contains("HeroSelectScene.class"));
		assertFalse(bukovEntry.contains("StartScene.class"));
		assertFalse(bukovEntry.contains("InterlevelScene.class"));
	}

	@Test
	public void titleContainsNoLegacyPlayerRoute() throws Exception {
		String title = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");

		assertFalse(title.contains("openClassicMode"));
		assertFalse(title.contains("AboutScene"));
		assertFalse(title.contains("HeroSelectScene"));
		assertFalse(title.contains("StartScene"));
		assertFalse(title.contains("\"关于\""));
	}

	@Test
	public void everyPlayerVisiblePrimaryTitleActionRoutesToBukov()
			throws Exception {
		String title = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java");
		String visibleActions = between(
				title,
				"btnContinue = new TacticalTitleButton",
				"version = new BitmapText");

		assertEquals(2, occurrences(visibleActions, "openBukovMode();"));
		assertFalse(visibleActions.contains("BukovDeploymentScene.class"));
		assertFalse(visibleActions.contains("HeroSelectScene.class"));
		assertFalse(visibleActions.contains("StartScene.class"));
		assertFalse(visibleActions.contains("InterlevelScene.class"));
		assertFalse(visibleActions.contains("BukovMode.leave()"));
	}

	@Test
	public void hubDeployCallbackUsesOnlyBukovDeploymentScene()
			throws Exception {
		String hub = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovHubScene.java");
		String deploy = between(
				hub,
				"private void deploy()",
				"private void confirmAbandon()");

		assertTrue(deploy.contains("controller.confirmDeployment()"));
		assertTrue(deploy.contains(
				"BukovMode.prepareRaidMode(controller.selectedRaidMode())"));
		assertTrue(deploy.contains("BukovDeploymentScene.class"));
		assertFalse(deploy.contains("HeroSelectScene.class"));
		assertFalse(deploy.contains("StartScene.class"));
		assertFalse(deploy.contains("InterlevelScene.class"));
	}

	@Test
	public void deploymentCreatesOrRestoresBukovLevelDirectly() throws Exception {
		String deployment = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovDeploymentScene.java");

		assertTrue(deployment.contains("BukovOperator.prepareNewRaid()"));
		assertTrue(deployment.contains("BukovOperator.normalize(Dungeon.hero)"));
		assertTrue(deployment.contains("Dungeon.loadGame(BukovMode.SAVE_SLOT)"));
		assertTrue(deployment.contains("Dungeon.newLevel()"));
		assertTrue(deployment.contains("instanceof BukovLevel"));
		assertTrue(deployment.contains("Assets.Splashes.Bukov.FIRST_RAID"));
		assertFalse(deployment.contains("HeroSelectScene"));
		assertFalse(deployment.contains("InterlevelScene"));
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Flow boundary not found");
		}
		return source.substring(from, to);
	}

	private static int occurrences(String source, String value) {
		int count = 0;
		int from = 0;
		while ((from = source.indexOf(value, from)) >= 0) {
			count++;
			from += value.length();
		}
		return count;
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
