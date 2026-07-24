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
				"tokens.color(state.canDeploy\n"
						+ "\t\t\t\t\t\t? \"accent.extract\""));
		assertTrue(source.contains(
				"readiness.setPos(innerX, modeCard.bottom() + uiGap);"));
		assertTrue(source.contains("\"hub.button_confirm\""));
		assertTrue(source.contains("\"hub.button_prepare\""));
		assertTrue(source.contains(
				"return BukovMessages.get(\"bukov.entry.\" + key, args);"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();\n"
						+ "\t\t\t\t\t\t\tenterDeploymentScene();"));
		assertTrue(source.contains(
				"BukovHubViewModel currentState =\n"
						+ "\t\t\t\t\t\t\t\tcontroller.viewModel();"));
		assertTrue(source.contains(
				"new WndBukovHub(\n"
						+ "\t\t\t\tcontroller,"));
		assertTrue(source.contains(
				"public void call() {\n"
						+ "\t\t\t\t\t\treload();"));
	}

	@Test
	public void loadoutWindowShowsImmediateActionAfterRepair()
			throws Exception {
		String source = source("bukov/ui/WndBukovHub.java");

		assertTrue(source.contains(
				"return viewModel.deploymentReadinessHeadline();"));
		assertTrue(source.contains(
				"bukov.economy.hub.status_last_success"));
		assertTrue(source.contains(
				"bukov.economy.hub.repair_deploy"));
		assertTrue(source.contains(
				"bukov.economy.hub.confirm_deploy"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();"));
		assertTrue(source.contains(
				"controller.prepareAndConfirmDeployment();\n"
						+ "\t\t\t\t\t\thide();\n"
						+ "\t\t\t\t\t\tdeploy.call();"));
		assertTrue(source.contains("private final Callback closed;"));
		assertTrue(source.contains(
				"hide();\n"
						+ "\t\t\t\t\tclosed.call();"));
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
