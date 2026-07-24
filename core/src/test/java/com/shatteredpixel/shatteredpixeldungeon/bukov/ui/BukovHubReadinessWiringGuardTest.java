package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Ensures both player-reachable hub surfaces expose the validated ready state. */
public class BukovHubReadinessWiringGuardTest {

	@Test
	public void fullHideoutShowsReadyStateBesideLoadoutSummary()
			throws Exception {
		String source = source("scenes/BukovHubScene.java");

		assertTrue(source.contains(
				"state.deploymentReadinessHeadline()"));
		assertTrue(source.contains(
				"state.canDeploy\n"
						+ "\t\t\t\t\t\t\t\t\t? \"accent.extract\""));
		assertTrue(source.contains("\"确认出击\""));
		assertTrue(source.contains("\"补齐并出击\""));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();\n"
						+ "\t\t\t\t\t\t\tdeploy();"));
	}

	@Test
	public void loadoutWindowShowsImmediateActionAfterRepair()
			throws Exception {
		String source = source("bukov/ui/WndBukovHub.java");

		assertTrue(source.contains(
				"return viewModel.deploymentReadinessHeadline();"));
		assertTrue(source.contains(
				"\"配装已就绪 / 可立即出击 · \""));
		assertTrue(source.contains(
				"!viewModel.canDeploy ? \"补齐并出击\" : \"确认出击\""));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();\n"
						+ "\t\t\t\t\t\thide();\n"
						+ "\t\t\t\t\t\tdeploy.call();"));
	}

	private static String source(String relative) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/"
								+ relative)),
				StandardCharsets.UTF_8);
	}
}
