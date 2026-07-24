package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Keeps the raid product boundary independent from the classic dungeon UI. */
public class BukovGameUiShellGuardTest {

	@Test
	public void classicHudIsConstructedOnlyOutsideBukovMode() throws Exception {
		String source = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String uiBoundary = between(
				source,
				"if (!BukovMode.active()) {\n"
						+ "\t\t\t//display cutouts",
				"// Bukov enters directly from BukovDeploymentScene");

		assertTrue(uiBoundary.contains("new MenuPane()"));
		assertTrue(uiBoundary.contains("new StatusPane("));
		assertTrue(uiBoundary.contains("new BossHealthBar()"));
		assertTrue(uiBoundary.contains("new ResumeIndicator()"));
		assertTrue(uiBoundary.contains("new ActionIndicator()"));
		assertTrue(uiBoundary.contains("new LootIndicator()"));
		assertTrue(uiBoundary.contains("new AttackIndicator()"));
		assertTrue(uiBoundary.contains("new Toolbar()"));
		assertTrue(uiBoundary.contains("new InventoryPane()"));
	}

	@Test
	public void bukovBackAndWindowGateCannotExposeClassicSurfaces()
			throws Exception {
		String source = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String onBack = between(
				source,
				"protected void onBackPressed()",
				"public void addCustomTile");
		assertTrue(onBack.contains("openBukovPause()"));
		assertTrue(onBack.contains("if (BukovMode.active())"));
		assertTrue(onBack.contains("new WndBukovPause"));
		assertTrue(source.contains("new BukovPauseButton"));
		assertTrue(source.contains("installBukovBackpackShortcut()"));
		assertTrue(source.contains(
				"KeyEvent.addKeyListener(bukovBackpackKeyListener)"));
		assertTrue(source.contains(
				"KeyEvent.removeKeyListener(bukovBackpackKeyListener)"));
		assertTrue(source.contains("openBukovBackpack()"));

		String show = between(
				source,
				"public static void show( Window wnd )",
				"public static boolean showingWindow()");
		assertTrue(show.contains("wnd instanceof WndGame"));
		assertTrue(show.contains("wnd instanceof WndBag"));
		assertTrue(show.contains("wnd instanceof WndHero"));

		String selectItem = between(
				source,
				"public static WndBag selectItem",
				"private static WndBag.ItemSelector savedSelector");
		assertTrue(selectItem.contains("if (BukovMode.active())"));
		assertTrue(selectItem.contains("return null;"));
	}

	@Test
	public void raidPauseHasOnlyBukovSafeActions() throws Exception {
		String pause = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/WndBukovPause.java");

		assertTrue(pause.contains(
				"\"bukov.raid.pause.resume_label\""));
		assertTrue(pause.contains(
				"\"bukov.raid.pause.settings_label\""));
		assertTrue(pause.contains(
				"\"bukov.raid.pause.leave_label\""));
		assertTrue(pause.contains("new BukovFocusModel(3, CONTINUE)"));
		assertTrue(pause.contains("new ActionButton[3]"));
		assertFalse(pause.contains("WndGame"));
		assertFalse(pause.contains("WndBag"));
		assertFalse(pause.contains("WndHero"));
		assertFalse(pause.contains("HeroSelectScene"));
		assertFalse(pause.contains("InterlevelScene"));
		assertFalse(pause.contains("RedButton"));
	}

	@Test
	public void staleClassicInterlevelStateIsClearedBeforeVfx() throws Exception {
		String source = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String entry = between(
				source,
				"// Bukov enters directly from BukovDeploymentScene",
				"ArrayList<Item> dropped");
		int guard = entry.indexOf("if (BukovMode.active())");
		int clear = entry.indexOf("InterlevelScene.mode = InterlevelScene.Mode.NONE");
		int vfx = entry.indexOf("ScrollOfTeleportation.appearVFX");
		assertTrue(guard >= 0);
		assertTrue(clear > guard);
		assertTrue(vfx > clear);
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
