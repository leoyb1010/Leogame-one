package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Guards the recovery edge that previously looped at the title screen. */
public class BukovDeploymentRecoveryGuardTest {

	@Test
	public void missingOrMismatchedHostSlotSettlesCheckpointBeforeNewRaid()
			throws Exception {
		String source = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/"
								+ "BukovDeploymentScene.java")),
				StandardCharsets.UTF_8);

		assertTrue(source.contains(
				"checkpoint.session().seed != Dungeon.seed"));
		assertTrue(source.contains(
				"} else if (checkpoint != null) {"));
		assertTrue(source.contains(
				"recoverIncompatibleRaid("));
		assertTrue(source.contains(
				"interrupted.settleDeath();"));
		assertTrue(source.contains(
				"Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);"));
	}
}
