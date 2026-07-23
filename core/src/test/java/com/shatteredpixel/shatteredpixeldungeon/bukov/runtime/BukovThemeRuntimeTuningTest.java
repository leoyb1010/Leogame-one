package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeRegistry;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovThemeRuntimeTuningTest {

	@Test
	public void realtimeWorldConsumesThemeRiskForSpawnCadence()
			throws IOException {
		ThemeRegistry themes = themes();
		ThemeDefinition fog = themes.require("fog_depot");
		ThemeDefinition sealed = themes.require("sealed_lab");
		assertEquals(12f,
				BukovRealtimeWorld.themedSpawnInterval(12f, null),
				0f);
		assertTrue(
				BukovRealtimeWorld.themedSpawnInterval(12f, sealed)
						< BukovRealtimeWorld.themedSpawnInterval(12f, fog));
	}

	private static ThemeRegistry themes() throws IOException {
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadJson(new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/themes.json")),
				StandardCharsets.UTF_8));
		return registry;
	}
}
