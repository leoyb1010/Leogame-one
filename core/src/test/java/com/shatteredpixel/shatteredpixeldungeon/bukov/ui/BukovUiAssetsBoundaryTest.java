package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Proves the Bukov UI skin is original, sealed, reusable, and wired. */
public class BukovUiAssetsBoundaryTest {

	private static final Path ASSETS = Paths.get("src/main/assets");
	private static final String EXPECTED_SHA256 =
			"caa273d6141352ea441e28397146950d1e350fce83372226da63a6bd5e7ec0b5";

	@Test
	public void atlasHasSealedRgbaPixelContract() throws Exception {
		byte[] png = Files.readAllBytes(
				ASSETS.resolve("interfaces/bukov_ui.png"));

		assertTrue("PNG signature is required",
				png.length > 32
						&& (png[0] & 0xFF) == 0x89
						&& png[1] == 'P'
						&& png[2] == 'N'
						&& png[3] == 'G');
		assertEquals(256, bigEndianInt(png, 16));
		assertEquals(64, bigEndianInt(png, 20));
		assertEquals("atlas must stay RGBA", 6, png[25] & 0xFF);
		assertEquals(EXPECTED_SHA256, sha256(png));
		assertTrue(BukovUiAssets.atlasAvailable());
	}

	@Test
	public void manifestCoversAllV2UiStatesAndLicense() throws Exception {
		String manifest = text(
				ASSETS.resolve("interfaces/bukov_ui_manifest.json"));

		assertTrue(manifest.contains("\"schemaVersion\": 2"));
		assertTrue(manifest.contains("\"pixelSampling\": \"nearest\""));
		assertTrue(manifest.contains("\"apiName\": \"PANEL\""));
		assertTrue(manifest.contains("\"apiName\": \"PANEL_RAISED\""));
		assertTrue(manifest.contains("\"apiName\": \"BUTTON\""));
		assertTrue(manifest.contains("\"apiName\": \"BUTTON_PRESSED\""));
		assertTrue(manifest.contains("\"apiName\": \"BUTTON_FOCUSED\""));
		assertTrue(manifest.contains("\"apiName\": \"BUTTON_DISABLED\""));
		assertTrue(manifest.contains("\"apiName\": \"ROW_FOCUSED\""));
		assertTrue(manifest.contains("\"apiName\": \"RARITY_COMMON\""));
		assertTrue(manifest.contains("\"apiName\": \"RARITY_UNCOMMON\""));
		assertTrue(manifest.contains("\"apiName\": \"RARITY_RARE\""));
		assertTrue(manifest.contains("\"apiName\": \"RARITY_LEGENDARY\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_HEALTH\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_ARMOR\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_AMMO\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_INTERACT\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_OBJECTIVE\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_TIMER\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_SOUND\""));
		assertTrue(manifest.contains("\"apiName\": \"HUD_HIT\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_ACTION\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_LOOT\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_EXTRACT\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_DANGER\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_BLEEDING\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_FRACTURE\""));
		assertTrue(manifest.contains("\"apiName\": \"STATUS_CONCUSSION\""));
		assertTrue(manifest.contains("\"apiName\": \"STAMP_EXTRACTED\""));
		assertTrue(manifest.contains("\"apiName\": \"STAMP_LOST\""));
		assertTrue(manifest.contains("\"sha256\": \"" + EXPECTED_SHA256));
		assertTrue(manifest.contains("project original cleared"));
	}

	@Test
	public void sharedLoaderHasSafeFallbackAndPlayerVisibleWiring()
			throws Exception {
		String loader = javaSource(
				"bukov/ui/BukovUiAssets.java");
		assertTrue(loader.contains("Assets.Interfaces.BUKOV_UI"));
		assertTrue(loader.contains("TextureCache.createSolid(fallbackColor)"));
		assertTrue(loader.contains("Gdx.files.internal("));
		assertTrue(loader.contains("SmartTexture.NEAREST"));

		for (String scene : new String[] {
				"TitleScene.java",
				"WelcomeScene.java",
				"BukovDeploymentScene.java"
		}) {
			String source = javaSource("scenes/" + scene);
			assertTrue(scene, source.contains("BukovUiAssets.surface("));
			assertFalse(scene + " must not revive the old Leo UI skin",
					source.contains("Assets.Interfaces.LEO_"));
		}

		String hub = javaSource("bukov/ui/WndBukovHub.java");
		assertTrue(hub.contains("BukovUiAssets.rarityFrame("));
		assertTrue(hub.contains("Surface.BUTTON_FOCUSED"));
		assertTrue(hub.contains("Surface.BUTTON_DISABLED"));
		assertTrue(hub.contains("Surface.ROW_FOCUSED"));
		String hubScene = javaSource("scenes/BukovHubScene.java");
		assertTrue(hubScene.contains("Surface.BUTTON_FOCUSED"));
		assertTrue(hubScene.contains("Surface.BUTTON_DISABLED"));
		assertTrue(hubScene.contains("Surface.BUTTON_PRESSED"));

		String hud = javaSource("bukov/ui/BukovRaidHud.java");
		for (String element : new String[] {
				"HEALTH", "ARMOR", "AMMO", "INTERACT",
				"OBJECTIVE", "TIMER", "SOUND", "HIT"
		}) {
			assertTrue(element, hud.contains(
					"BukovUiAssets.HudElement." + element));
		}

		String settlement =
				javaSource("bukov/ui/WndBukovSettlement.java");
		assertTrue(settlement.contains("BukovUiAssets.Stamp.EXTRACTED"));
		assertTrue(settlement.contains("BukovUiAssets.Stamp.LOST"));
	}

	private static String javaSource(String relative) throws Exception {
		return text(Paths.get(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon").resolve(relative));
	}

	private static String text(Path path) throws Exception {
		return new String(
				Files.readAllBytes(path), StandardCharsets.UTF_8);
	}

	private static int bigEndianInt(byte[] bytes, int offset) {
		return ((bytes[offset] & 0xFF) << 24)
				| ((bytes[offset + 1] & 0xFF) << 16)
				| ((bytes[offset + 2] & 0xFF) << 8)
				| (bytes[offset + 3] & 0xFF);
	}

	private static String sha256(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(bytes);
		StringBuilder out = new StringBuilder(digest.length * 2);
		for (byte value : digest) {
			out.append(String.format("%02x", value & 0xFF));
		}
		return out.toString();
	}
}
