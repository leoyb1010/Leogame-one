package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHubEntranceMotionWiringTest {

	@Test
	public void entranceCurveIsClampedAndEndsExactly() {
		assertEquals(0f, WndBukovHub.entranceEase(-1f), 0f);
		assertEquals(0.875f, WndBukovHub.entranceEase(0.5f), 0.0001f);
		assertEquals(1f, WndBukovHub.entranceEase(1f), 0f);
		assertEquals(1f, WndBukovHub.entranceEase(2f), 0f);
	}

	@Test
	public void hubUsesTokenMotionWithoutBlockingInput() throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/ui/"
								+ "WndBukovHub.java")),
				StandardCharsets.UTF_8);

		assertTrue(source.contains("new BukovUiMotionScheduler()"));
		assertTrue(source.contains("tokens.motionMs(\"slow\")"));
		assertTrue(source.contains("SPDSettings.bukovReduceMotion()"));
		assertTrue(source.contains(
				"motionScheduler.cancelToEnd(this, MOTION_ENTRANCE)"));
		assertTrue(source.contains(
				"motionScheduler.update(Game.elapsed)"));
		assertTrue(source.contains("applyEntranceMotion();"));

		String onSignal = source.substring(
				source.indexOf("public boolean onSignal(KeyEvent event)"),
				source.indexOf("@Override\n\tpublic void update()"));
		assertFalse(onSignal.contains("motionScheduler"));
		assertFalse(onSignal.contains("MOTION_ENTRANCE"));
	}
}
