package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovResponsiveUiWiringGuardTest {

	@Test
	public void productionHudConsumesCachedGeometryAndTextContracts()
			throws Exception {
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");
		assertTrue(hud.contains(
				"new BukovResponsiveUiLayout.Cache()"));
		assertTrue(hud.contains("responsiveUiCache.layout("));
		assertFalse(hud.contains(
				"BukovResponsiveUiLayout.calculateMobile("));
		assertTrue(hud.contains(
				"new BukovRaidHudLayout.FitCache()"));
		assertTrue(hud.contains(
				"navigationFit.bodyLine("));
		assertTrue(hud.contains(
				"threatFit.bodyLine("));
		assertTrue(hud.contains(
				"objectiveFit.objective("));
		assertTrue(hud.contains(
				"healthFit.primaryLine("));
		assertTrue(hud.contains(
				"bossFit.bodyLine("));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
