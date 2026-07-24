package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeRegistry;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovThemeVisualProductionWiringTest {

	private static final Path ASSET_ROOT =
			Paths.get("src/main/assets/environment/bukov");

	@Test
	public void gameplayThemesOwnSixStableVisualAssetFamilies()
			throws IOException, NoSuchAlgorithmException {
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("fog_depot", "fog_depot");
		expected.put("rust_workshop", "rust_works");
		expected.put("flooded_passage", "flooded_bunker");
		expected.put("overgrown_yard", "container_yard");
		expected.put("cold_storage", "cold_storage");
		expected.put("sealed_lab", "underground_lab");

		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();
		Set<String> tileHashes = new HashSet<>();
		Set<String> waterHashes = new HashSet<>();
		Set<String> landmarkHashes = new HashSet<>();

		for (ThemeDefinition theme : registry.all()) {
			assertEquals(expected.get(theme.id), theme.visualAssetId);
			assertAsset(
					theme.tilesTexture(),
					tileHashes,
					20000L);
			assertAsset(
					theme.waterTexture(),
					waterHashes,
					500L);
			assertAsset(
					theme.landmarkTexture(),
					landmarkHashes,
					3000L);
		}

		assertEquals(6, tileHashes.size());
		assertEquals(6, waterHashes.size());
		assertEquals(6, landmarkHashes.size());
	}

	@Test
	public void runtimeResolvesTerrainWaterAndLandmarksFromCurrentTheme()
			throws IOException {
		String level = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/levels/"
						+ "BukovLevel.java");
		String semantic = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/bukov/levels/"
						+ "BukovSemanticVisualLayer.java");
		String gate = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/sprites/bukov/"
						+ "BukovFirstRaidLandmarks.java");

		assertTrue(level.contains("return visualTheme().tilesTexture();"));
		assertTrue(level.contains("return visualTheme().waterTexture();"));
		assertTrue(level.contains("return visualTheme().landmarkTexture();"));
		assertTrue(semantic.contains("visualAssetId(theme)"));
		assertTrue(gate.contains("level.landmarkTex()"));
		assertTrue(Files.size(
				ASSET_ROOT.resolve("theme_visual_contact_sheet.png"))
				> 30000L);
		assertTrue(Files.size(
				ASSET_ROOT.resolve("theme_visual_manifest.json"))
				> 5000L);
	}

	private static void assertAsset(
			String runtimePath,
			Set<String> hashes,
			long minimumBytes)
			throws IOException, NoSuchAlgorithmException {
		String prefix = "environment/bukov/";
		assertTrue(runtimePath.startsWith(prefix));
		Path asset = ASSET_ROOT.resolve(runtimePath.substring(prefix.length()));
		assertTrue(runtimePath + " is missing", Files.isRegularFile(asset));
		assertTrue(runtimePath + " is unexpectedly small",
				Files.size(asset) >= minimumBytes);
		hashes.add(sha256(asset));
	}

	private static String sha256(Path path)
			throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] bytes = digest.digest(Files.readAllBytes(path));
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			result.append(String.format("%02x", value & 0xff));
		}
		return result.toString();
	}

	private static String source(String path) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
