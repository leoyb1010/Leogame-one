package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class BukovDeathTransitionWiringGuardTest {

	@Test
	public void deathSettlesBeforePresentationDelayAndSuccessStaysImmediate()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "scenes/GameScene.java");

		int settleDeath = scene.indexOf(
				"RaidResult result = bukovRaid.settleDeath()");
		int beginDeath = scene.indexOf(
				"beginBukovDeathTransition(result, elapsed, kills)");
		int settleSuccess = scene.indexOf(
				"RaidResult result = bukovRaid.settleSuccess()");
		int finishSuccess = scene.indexOf(
				"finishBukovHostSave(result, elapsed, kills)",
				settleSuccess);

		assertTrue(settleDeath >= 0);
		assertTrue(beginDeath > settleDeath);
		assertTrue(settleSuccess >= 0);
		assertTrue(finishSuccess > settleSuccess);
		assertTrue(scene.contains("prepareBukovSettlement();"));
		assertTrue(scene.contains("updateBukovDeathTransition();"));
		assertTrue(scene.contains(
				"new BukovDeathTransitionModel()"));
		assertTrue(scene.contains(
				"bukovTouchControls.inputBlocked("));
		assertTrue(scene.contains(
				"showingWindow() || bukovDeathTransition != null"));
		assertTrue(scene.contains(
				"bukovDeathTransition.consumeCompletion()"));
		assertTrue(scene.indexOf(
				"showBukovSettlement(result, elapsed, kills)",
				scene.indexOf("private void updateBukovDeathTransition()"))
				> scene.indexOf(
						"bukovDeathTransition.consumeCompletion()"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
