package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FirearmDefinitionTest {

	@Test
	public void calculatesSecondsPerShotFromRpm() {
		FirearmDefinition definition = validDefinition();
		definition.rpm = 600f;

		assertEquals(0.1f, definition.secondsPerShot(), 0.0001f);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsNonPositiveRpmWhenCalculatingCadence() {
		FirearmDefinition definition = validDefinition();
		definition.rpm = 0f;
		definition.secondsPerShot();
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidMagazineSize() {
		FirearmDefinition definition = validDefinition();
		definition.magazineSize = 0;
		definition.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNegativePenetration() {
		FirearmDefinition definition = validDefinition();
		definition.penetration = -1f;
		definition.validate();
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnusableWeaponFeedbackTuning() {
		FirearmDefinition definition = validDefinition();
		definition.muzzleIntensity = 0f;
		definition.validate();
	}

	public static FirearmDefinition validDefinition() {
		FirearmDefinition definition = new FirearmDefinition();
		definition.id = "test";
		definition.name = "测试枪";
		definition.weaponClass = FirearmClass.PISTOL;
		definition.caliber = "test_caliber";
		definition.defaultAmmo = "test_ammo";
		definition.fireMode = FireMode.SEMI;
		definition.damage = 20f;
		definition.penetration = 5f;
		definition.rpm = 600f;
		definition.magazineSize = 10;
		definition.reloadSeconds = 1f;
		definition.effectiveRangeTiles = 8f;
		definition.baseSpreadDeg = 1f;
		definition.movingSpreadDeg = 2f;
		definition.recoilPerShot = 0.5f;
		definition.recoilRecovery = 4f;
		definition.pellets = 1;
		definition.noiseRadiusTiles = 10f;
		definition.weightKg = 2f;
		definition.value = 100;
		return definition;
	}
}
