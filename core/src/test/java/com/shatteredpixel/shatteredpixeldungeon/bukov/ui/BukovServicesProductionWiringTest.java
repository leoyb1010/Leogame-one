package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Guards the player-reachable scene and runtime attachment consumption path. */
public class BukovServicesProductionWiringTest {

	@Test
	public void hideoutHasDirectContractInsuranceAndWorkshopEntrypoints()
			throws IOException {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "scenes/BukovHubScene.java");
		assertTrue(scene.contains("\"hub.button_contracts\""));
		assertTrue(scene.contains("\"hub.button_insurance\""));
		assertTrue(scene.contains("\"hub.button_firearms\""));
		assertTrue(scene.contains("WndBukovServices.Tab.CONTRACTS"));
		assertTrue(scene.contains("WndBukovServices.Tab.INSURANCE"));
		assertTrue(scene.contains("WndBukovServices.Tab.FIREARMS"));
	}

	@Test
	public void serviceWindowCallsDurableControllerOperations()
			throws IOException {
		String window = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ui/WndBukovServices.java");
		assertTrue(window.contains("controller.claimContract"));
		assertTrue(window.contains("controller.toggleInsurance"));
		assertTrue(window.contains("controller.claimInsuranceReturns"));
		assertTrue(window.contains("controller.toggleAttachment"));
		assertTrue(window.contains("BukovServicesFocusModel"));
	}

	@Test
	public void runtimeFirearmReceivesProfileBuildAndReturnsEffectiveDefinition()
			throws IOException {
		String adapter = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/raid/BukovRuntimeLoadoutAdapter.java");
		String firearm = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/combat/firearms/Firearm.java");
		assertTrue(adapter.contains("raid.profile().firearmBuilds()"));
		assertTrue(adapter.contains("firearm.applyBuild(build)"));
		assertTrue(adapter.contains("firearm.definition(firearms)"));
		assertTrue(firearm.contains("attachmentBuild.effectiveStats(base)"));
	}

	private static String source(String path) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
