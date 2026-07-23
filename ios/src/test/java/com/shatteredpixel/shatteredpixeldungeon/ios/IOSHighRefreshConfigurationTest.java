package com.shatteredpixel.shatteredpixeldungeon.ios;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IOSHighRefreshConfigurationTest {

	@Test
	public void requestsProMotionRefreshRate() {
		assertEquals(120, IOSLauncher.PREFERRED_FRAMES_PER_SECOND);
	}

	@Test
	public void infoPlistAllowsAdaptiveHighRefresh() throws Exception {
		String plist = new String(
				Files.readAllBytes(Paths.get("Info.plist")),
				StandardCharsets.UTF_8);
		assertTrue(plist.contains(
				"<key>CADisableMinimumFrameDurationOnPhone</key>"));
		assertTrue(plist.contains(
				"<key>CADisableMinimumFrameDurationOnPhone</key>\n\t\t<true/>"));
	}
}
