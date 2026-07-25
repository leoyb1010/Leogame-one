package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovAnchorPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovEnvironmentOverlayTilemap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Game;
import com.watabou.utils.GameSettings;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
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
		Set<String> overlayHashes = new HashSet<>();

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
			assertAsset(
					theme.environmentOverlayTexture(),
					overlayHashes,
					250L);
		}

		assertEquals(6, tileHashes.size());
		assertEquals(6, waterHashes.size());
		assertEquals(6, landmarkHashes.size());
		assertEquals(6, overlayHashes.size());
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
		assertTrue(semantic.contains("placeEnvironmentOverlays("));
		assertTrue(gate.contains("level.landmarkTex()"));
		assertTrue(Files.size(
				ASSET_ROOT.resolve("theme_visual_contact_sheet.png"))
				> 30000L);
		assertTrue(Files.size(
				ASSET_ROOT.resolve("theme_visual_manifest.json"))
				> 5000L);
	}

	@Test
	public void sixRealGeneratedLevelsUseBoundedThemeOverlaysAndSafeRoutes()
			throws IOException {
		int previousDepth = Dungeon.depth;
		int previousBranch = Dungeon.branch;
		long previousSeed = Dungeon.seed;
		com.shatteredpixel.shatteredpixeldungeon.levels.Level previousLevel =
				Dungeon.level;
		String previousVersion = Game.version;
		BukovRaidMode previousMode = BukovMode.raidMode();
		ArrayList<String> previousMaps =
				new ArrayList<>(BukovMode.unlockedRaidThemes());
		String previousSelected = BukovMode.selectedRaidTheme();
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();
		try {
			GameSettings.set(defaultPreferences());
			if (Game.version == null) Game.version = "test";
			BukovMode.prepareRaidMode(BukovRaidMode.EXPEDITION);
			for (ThemeDefinition theme : registry.all()) {
				BukovMode.prepareUnlockedMaps(
						Collections.singletonList(theme.id));
				BukovMode.prepareSelectedMap(theme.id);
				Dungeon.depth = 1;
				Dungeon.branch = 0;
				Dungeon.seed = 0x71000000L + theme.id.hashCode();
				Dungeon.level = null;
				BukovLevel level = new BukovLevel();
				level.create();

				assertEquals(theme.tilesTexture(), level.tilesTex());
				assertEquals(theme.waterTexture(), level.waterTex());
				assertEquals(theme.landmarkTexture(), level.landmarkTex());
				int overlays = 0;
				for (CustomTilemap visual : level.customTiles) {
					if (visual
							instanceof BukovEnvironmentOverlayTilemap) {
						assertEquals(
								theme.visualAssetId,
								((BukovEnvironmentOverlayTilemap)visual)
										.visualAssetId());
						overlays++;
					}
				}
				assertTrue(theme.id + " produced no production overlay",
						overlays > 0);
				assertTrue(theme.id + " exceeded overlay budget",
						overlays <= theme.environmentOverlayCount
								&& overlays <= 3);
				BukovRaidLayout layout = level.raidLayout();
				BukovAnchorPlanner.Result traversal =
						BukovAnchorPlanner
								.validateLockedMissionTraversal(
										level.width(),
										level.height(),
										level.map,
										layout,
										level.entrance());
				assertTrue(theme.id + ": " + traversal.reason,
						traversal.valid);
			}
		} finally {
			Dungeon.depth = previousDepth;
			Dungeon.branch = previousBranch;
			Dungeon.seed = previousSeed;
			Dungeon.level = previousLevel;
			Game.version = previousVersion;
			BukovMode.prepareRaidMode(previousMode);
			BukovMode.prepareUnlockedMaps(previousMaps);
			BukovMode.prepareSelectedMap(previousSelected);
			GameSettings.set(null);
		}
	}

	private static Preferences defaultPreferences() {
		return (Preferences)Proxy.newProxyInstance(
				Preferences.class.getClassLoader(),
				new Class<?>[]{Preferences.class},
				(proxy, method, arguments) -> {
					Class<?> type = method.getReturnType();
					if (type == Preferences.class) return proxy;
					if ("get".equals(method.getName())
							&& (arguments == null
									|| arguments.length == 0)) {
						return Collections.emptyMap();
					}
					if (method.getName().startsWith("get")
							&& arguments != null
							&& arguments.length >= 2) {
						return arguments[1];
					}
					if (type == boolean.class) return false;
					if (type == int.class) return 0;
					if (type == long.class) return 0L;
					if (type == float.class) return 0f;
					if (type == String.class) return "";
					return null;
				});
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
