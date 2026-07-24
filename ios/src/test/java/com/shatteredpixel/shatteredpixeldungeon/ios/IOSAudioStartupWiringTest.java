package com.shatteredpixel.shatteredpixeldungeon.ios;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the player-visible distinction between real audio and recovery. */
public class IOSAudioStartupWiringTest {

	@Test
	public void simulatorAttemptsNativeAudioBeforeSilentRecovery()
			throws Exception {
		String launcher = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/ios/IOSLauncher.java");

		assertTrue(launcher.contains("config.useAudio = true;"));
		assertFalse(launcher.contains("config.useAudio = !simulator;"));
		assertTrue(launcher.contains("if (!simulator)"));
		assertTrue(launcher.contains("createNativeAudio(configuration)"));
		assertTrue(launcher.contains(
				"IOSAudioStartupPolicy.createForSimulator("));
		assertFalse(launcher.contains(
				"simulator ? new SilentIOSAudio()"));
	}

	@Test
	public void silentSwitchAvoidsObjectAlOnlyAfterExplicitFallback()
			throws Exception {
		String platform = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/ios/"
						+ "IOSPlatformSupport.java");

		assertTrue(platform.contains(
				"if (!IOSAudioStartupPolicy.usingSilentFallback())"));
		assertFalse(platform.contains(
				"if (!IOSRuntimeEnvironment.isSimulator(System.getenv()))"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
