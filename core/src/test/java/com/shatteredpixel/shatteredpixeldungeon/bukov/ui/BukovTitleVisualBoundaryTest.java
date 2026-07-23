package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovTitleVisualBoundaryTest {

	@Test
	public void titleUsesIndustrialV2ArtAndBukovTokens() throws Exception {
		String source = text(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/TitleScene.java");
		String playerSurface = between(
				source,
				"public void create()",
				"private void openClassicMode()");

		assertTrue(playerSurface.contains(
				"TITLE_INDUSTRIAL_LANDSCAPE_V2"));
		assertTrue(playerSurface.contains(
				"TITLE_INDUSTRIAL_PORTRAIT_V2"));
		assertTrue(playerSurface.contains("BukovUiTokens.loadDefault()"));
		assertTrue(playerSurface.contains("\"逃离布科夫\""));
		assertTrue(playerSurface.contains("\"ESCAPE FROM BUKOV\""));
		assertTrue(playerSurface.contains("\"继续行动  /  CONTINUE\""));
		assertTrue(playerSurface.contains("\"进入基地  /  HIDEOUT\""));
		assertTrue(playerSurface.contains("new WndBukovSettings()"));
		assertTrue(playerSurface.contains("AboutScene.class"));
	}

	@Test
	public void playerSurfaceHasNoGothicOrLegacyMenuDependencies()
			throws Exception {
		String source = text(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/TitleScene.java");
		String playerSurface = between(
				source,
				"public void create()",
				"private void openClassicMode()");

		assertFalse(playerSurface.contains("Chrome"));
		assertFalse(playerSurface.contains("Fireball"));
		assertFalse(playerSurface.contains("LeoStyledButton"));
		assertFalse(playerSurface.contains("LEO_TITLE_EMBLEM"));
		assertFalse(playerSurface.contains("btnRankings"));
		assertFalse(playerSurface.contains("btnJournal"));
		assertFalse(playerSurface.contains("btnChanges"));
		assertFalse(playerSurface.contains("WndLeoWelcome"));
		assertFalse(playerSurface.contains("continueAfterIdentity"));
		assertFalse(playerSurface.toLowerCase().contains("gothic"));
		assertFalse(playerSurface.toLowerCase().contains("lion"));
		assertFalse(playerSurface.toLowerCase().contains("emblem"));
	}

	@Test
	public void activeRaidRequiresBothHostSaveAndBukovCheckpoint()
			throws Exception {
		String source = text(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/TitleScene.java");
		String guard = between(
				source,
				"private static boolean hasActiveRaid()",
				"private void openBukovMode()");
		assertTrue(guard.contains(
				"GamesInProgress.gameExists(BukovMode.SAVE_SLOT)"));
		assertTrue(guard.contains("loadRaidCheckpoint() != null"));
	}

	@Test
	public void versionedTitleAssetsAndProvenanceAreExact()
			throws Exception {
		assertPng(
				"src/main/assets/splashes/bukov/"
						+ "title_industrial_landscape_v2.png",
				1672,
				941);
		assertPng(
				"src/main/assets/splashes/bukov/"
						+ "title_industrial_portrait_v2.png",
				941,
				1672);
		String provenance = text(
				"../artwork/licenses/ASSET_PROVENANCE.csv");
		assertTrue(provenance.contains(
				"title_industrial_landscape_v2.png,"
						+ "project-generated original artwork"));
		assertTrue(provenance.contains(
				"013001c3da7295a97c23dab747f0bd43b089e504c"
						+ "daad6c39b35aa98e76eeaa4"));
		assertTrue(provenance.contains(
				"74c2f948eb5b9d3773dd0b1074ededf8307f5f753"
						+ "c26fde5b4ea3a1fc62560bc"));
		String manifest = text(
				"../docs/bukov/ART_ASSET_MANIFEST.md");
		assertTrue(manifest.contains("工业雾港标题主视觉 v2"));
		assertTrue(manifest.contains("1672×941"));
		assertTrue(manifest.contains("941×1672"));
	}

	@Test
	public void hideoutWindowDoesNotLeakClassicChrome() throws Exception {
		String hub = text(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/ui/WndBukovHub.java");
		assertTrue(hub.contains("BukovUiTokens.loadDefault()"));
		assertTrue(hub.contains("TextureCache.createSolid"));
		assertFalse(hub.contains("Chrome."));
		assertFalse(hub.contains("LeoStyledButton"));
		assertFalse(hub.contains("Fireball"));
		assertFalse(hub.contains("LEO_TITLE_EMBLEM"));
	}

	private static void assertPng(
			String path, int width, int height) throws Exception {
		try (DataInputStream input =
				new DataInputStream(new FileInputStream(path))) {
			assertEquals(0x89504E47, input.readInt());
			assertEquals(0x0D0A1A0A, input.readInt());
			assertEquals(13, input.readInt());
			assertEquals(0x49484452, input.readInt());
			assertEquals(width, input.readInt());
			assertEquals(height, input.readInt());
			assertEquals(8, input.readUnsignedByte());
			assertEquals(2, input.readUnsignedByte());
		}
	}

	private static String between(
			String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		if (from < 0 || to < 0) {
			throw new AssertionError("Source boundary not found");
		}
		return source.substring(from, to);
	}

	private static String text(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
