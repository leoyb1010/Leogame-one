package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class DesktopHighRefreshConfigurationTest {

	@Test
	public void desktopUsesNativeMonitorRefreshWithoutTearing()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/desktop/"
								+ "DesktopLauncher.java")),
				StandardCharsets.UTF_8);
		assertTrue(source.contains("config.useVsync(true);"));
		assertTrue(source.contains("config.setForegroundFPS(0);"));
	}
}
