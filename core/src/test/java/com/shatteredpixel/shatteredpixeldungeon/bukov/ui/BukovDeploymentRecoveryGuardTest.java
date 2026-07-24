package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guard for the platform scene wiring around the pure matrix. */
public class BukovDeploymentRecoveryGuardTest {

	@Test
	public void deploymentUsesRecoveryMatrixAndArchivesEveryReservedHost()
			throws Exception {
		String deployment = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/"
								+ "BukovDeploymentScene.java")),
				StandardCharsets.UTF_8);

		assertTrue(deployment.contains(
				"BukovHostRecoveryPolicy.decide("));
		assertTrue(deployment.contains("case RESUME_MATCHED_HOST:"));
		assertTrue(deployment.contains(
				"case SETTLE_INTERRUPTED_CHECKPOINT:"));
		assertTrue(deployment.contains("case ARCHIVE_ORPHAN_HOST:"));
		assertTrue(deployment.contains("case CREATE_NEW_HOST:"));
		assertTrue(deployment.contains(
				"Level level = Dungeon.loadLevel(BukovMode.SAVE_SLOT);"));
		assertTrue(deployment.contains("requireBukovLevel(level);"));
		assertTrue(deployment.contains("source.moveTo(target);"));
		assertTrue(deployment.contains(
				"target.child(source.name())"));
		assertTrue(deployment.contains(
				"archivedFolder.child(\"game.dat\")"));
		assertTrue(deployment.contains(
				"archiveUnverifiedReservedHost();"));
		assertTrue(deployment.contains(
				"\"bukov_legacy_archives\","));
		assertTrue(deployment.contains(
				"GamesInProgress.delete(BukovMode.SAVE_SLOT);"));
		assertFalse(deployment.contains(
				"Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);"));
		assertTrue(deployment.contains("interrupted.settleDeath();"));

		int interruptedCase = deployment.indexOf(
				"case SETTLE_INTERRUPTED_CHECKPOINT:");
		int orphanCase = deployment.indexOf(
				"case ARCHIVE_ORPHAN_HOST:",
				interruptedCase);
		String interruptedWiring =
				deployment.substring(interruptedCase, orphanCase);
		assertTrue(interruptedWiring.contains(
				"settleInterruptedCheckpoint("));
		assertFalse(interruptedWiring.contains("createNewRaid();"));

		int createCase = deployment.indexOf(
				"case CREATE_NEW_HOST:",
				orphanCase);
		String orphanWiring =
				deployment.substring(orphanCase, createCase);
		assertTrue(orphanWiring.contains(
				"archiveVerifiedOrphanHost();"));
		assertTrue(orphanWiring.contains("createNewRaid();"));
	}

	@Test
	public void gameSceneRequiresOneShotFreshHostProofBeforeNewCheckpoint()
			throws Exception {
		String gameScene = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/"
								+ "GameScene.java")),
				StandardCharsets.UTF_8);

		int proof = gameScene.indexOf(
				"BukovDeploymentHandoff.consumeFreshHost(");
		int start = gameScene.indexOf(
				"BukovRaidCoordinator.start(",
				proof);
		assertTrue(proof >= 0);
		assertTrue(start > proof);
		assertTrue(gameScene.contains(
				"Refusing to deploy a new loadout into an "));
	}
}
