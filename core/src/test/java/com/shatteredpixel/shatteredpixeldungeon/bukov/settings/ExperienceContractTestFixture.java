package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class ExperienceContractTestFixture {

	public static ExperienceContract load() {
		try {
			String json = new String(
					Files.readAllBytes(Paths.get(
							"src/main/assets/bukov/content/experience_contract.json"
					)),
					StandardCharsets.UTF_8
			);
			return new ExperienceContractRegistry().loadJson(json);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public static BukovExperienceSettings visualizationEnabled(
			ExperienceContract contract) {
		BukovExperienceSettings defaults =
				BukovExperienceSettings.defaults(contract);
		return new BukovExperienceSettings(
				defaults.masterVolume,
				defaults.musicVolume,
				defaults.sfxVolume,
				defaults.ambienceVolume,
				defaults.visualEffects,
				defaults.screenShakeScale,
				defaults.controllerVibrationScale,
				defaults.hitstopEnabled,
				true,
				defaults.reduceMotion,
				defaults.reduceFlashes
		);
	}

	private ExperienceContractTestFixture() {
	}
}
