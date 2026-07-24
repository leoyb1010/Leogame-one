package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class DesktopWindowModeConfigurationTest {

	@Test
	public void desktopDefaultsToAUsableWindowAndKeepsFullscreenOptional()
			throws Exception {
		String settings = source(
				"../core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/SPDSettings.java");
		String launcher = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/desktop/"
						+ "DesktopLauncher.java");
		String actions = source(
				"../core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/SPDAction.java");
		String scene = source(
				"../core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/scenes/PixelScene.java");

		assertTrue(settings.contains(
				"getBoolean( KEY_FULLSCREEN, !DeviceCompat.isDesktop() )"));
		assertTrue(settings.contains(
				"getInt( KEY_WINDOW_WIDTH, 1100"));
		assertTrue(settings.contains(
				"getInt( KEY_WINDOW_HEIGHT, 680"));
		assertTrue(launcher.contains("config.setWindowedMode( p.x, p.y );"));
		assertTrue(actions.contains(
				"KeyBindings.addHardBinding( Input.Keys.F11"));
		assertTrue(scene.contains("keyEvent.code == Input.Keys.F11"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
