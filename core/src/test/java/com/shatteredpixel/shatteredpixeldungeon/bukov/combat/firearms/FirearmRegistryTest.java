package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotSoundFamily;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class FirearmRegistryTest {

	@Test
	public void loadsAllAuthoredFirearms() throws IOException {
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/firearms.json"
				)),
				StandardCharsets.UTF_8
		);
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(json);

		assertEquals(18, registry.all().size());
		assertEquals("针蜂-9", registry.require("needle_9").name);
		assertEquals("9x19", registry.require("needle_9").caliber);
		assertEquals("ammo_9_standard", registry.require("needle_9").defaultAmmo);
		assertEquals(FireMode.AUTO, registry.require("ward_556").fireMode);
		// v2.0 Gate 2 baseline: Ward-556 must stay 24 / 600 / 1.4 s.
		assertEquals(24, registry.require("ward_556").magazineSize);
		assertEquals(
				600f,
				registry.require("ward_556").rpm,
				0.001f);
		assertEquals(
				1.4f,
				registry.require("ward_556").reloadSeconds,
				0.0001f);
		assertEquals(24, registry.require("shuttle_9").magazineSize);
		assertEquals(7, registry.require("bolt_12").pellets);
		assertEquals(FireMode.SEMI, registry.require("longstreet_762").fireMode);
		assertEquals(
				FirearmClass.PISTOL,
				registry.require("sentinel_9").weaponClass);
		assertEquals(
				FirearmClass.SUBMACHINE_GUN,
				registry.require("hive_9").weaponClass);
		assertEquals(
				FirearmClass.ASSAULT_RIFLE,
				registry.require("river_556").weaponClass);
		assertEquals(
				FirearmClass.SHOTGUN,
				registry.require("breaker_12").weaponClass);
		assertEquals(
				FirearmClass.CARBINE,
				registry.require("carbine_556").weaponClass);
		assertEquals(
				FirearmClass.HEAVY_WEAPON,
				registry.require("rainstorm_12").weaponClass);
		assertEquals(
				FirearmClass.MARKSMAN_RIFLE,
				registry.require("frontier_762").weaponClass);
		assertEquals(
				"HEAVY_AUTOSHOTGUN",
				registry.require("rainstorm_12").feedbackProfile);
		assertEquals(
				1.42f,
				registry.require("rainstorm_12").soundGain,
				0.0001f);
		assertEquals(
				GunshotSoundFamily.PISTOL,
				registry.require("needle_9")
						.audioProfile.gunshotFamily);
		assertEquals(
				GunshotSoundFamily.CARBINE,
				registry.require("carbine_556")
						.audioProfile.gunshotFamily);
		assertEquals(
				GunshotSoundFamily.HEAVY,
				registry.require("rainstorm_12")
						.audioProfile.gunshotFamily);
		assertEquals(
				0.93f,
				registry.require("rainstorm_12")
						.audioProfile.chargeFraction,
				0.0001f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnsupportedSchema() {
		new FirearmRegistry().loadJson(
				"{\"schemaVersion\":2,\"firearms\":[]}"
		);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsDuplicateIds() {
		String firearm = firearmJson("duplicate");
		new FirearmRegistry().loadJson(
				"{\"schemaVersion\":1,\"firearms\":["
						+ firearm + "," + firearm + "]}"
		);
	}

	@Test
	public void invalidReloadDoesNotEraseLastValidRegistry() {
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(
				"{\"schemaVersion\":1,\"firearms\":["
						+ firearmJson("stable") + "]}"
		);

		try {
			registry.loadJson("{\"schemaVersion\":1,\"firearms\":[]}");
		} catch (IllegalStateException expected) {
			// expected
		}

		assertEquals("stable", registry.require("stable").id);
	}

	private static String firearmJson(String id) {
		return "{"
				+ "\"id\":\"" + id + "\","
				+ "\"name\":\"Test\","
				+ "\"caliber\":\"test_caliber\","
				+ "\"defaultAmmo\":\"ammo\","
				+ "\"fireMode\":\"SEMI\","
				+ "\"damage\":10,"
				+ "\"penetration\":1,"
				+ "\"rpm\":300,"
				+ "\"magazineSize\":5,"
				+ "\"reloadSeconds\":1,"
				+ "\"effectiveRangeTiles\":8,"
				+ "\"baseSpreadDeg\":1,"
				+ "\"movingSpreadDeg\":2,"
				+ "\"recoilPerShot\":0.5,"
				+ "\"recoilRecovery\":4,"
				+ "\"pellets\":1,"
				+ "\"noiseRadiusTiles\":10,"
				+ "\"weightKg\":2,"
				+ "\"value\":100"
				+ "}";
	}
}
