package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealtimeDamageTest {

	@Test
	public void appliesZoneAndRangeMultipliersWithoutArmor() {
		assertEquals(
				20f,
				RealtimeDamage.resolve(
						20f, 1f, 5f, 10f, 0f,
						RealtimeDamage.HitZone.CORE, null
				),
				0.0001f
		);
		assertEquals(
				15f,
				RealtimeDamage.resolve(
						20f, 1f, 20f, 10f, 0f,
						RealtimeDamage.HitZone.CORE, null
				),
				0.0001f
		);
		assertEquals(
				14.4f,
				RealtimeDamage.resolve(
						20f, 1f, 5f, 10f, 0f,
						RealtimeDamage.HitZone.LIMB, null
				),
				0.0001f
		);
	}

	@Test
	public void armorAbsorbsDamageAndLosesDurability() {
		RealtimeDamage.ArmorState armor = new RealtimeDamage.ArmorState();
		armor.durability = 1f;
		armor.resistance = 20f;

		float resolved = RealtimeDamage.resolve(
				20f, 1f, 5f, 10f, 10f,
				RealtimeDamage.HitZone.CORE, armor
		);

		assertEquals(10.8f, resolved, 0.0001f);
		assertEquals(9.2f, armor.absorbedLastHit, 0.0001f);
		assertEquals(0.7f, armor.durability, 0.0001f);
	}

	@Test
	public void damageHasOnePointFloor() {
		float result = RealtimeDamage.resolve(
				0f, 0f, 0f, 1f, 0f,
				RealtimeDamage.HitZone.CORE, null
		);
		assertEquals(1f, result, 0.0001f);
	}

	@Test
	public void veryLongRangeFalloffStopsAtFortyFivePercent() {
		float result = RealtimeDamage.resolve(
				100f, 1f, 100f, 10f, 0f,
				RealtimeDamage.HitZone.CORE, null
		);
		assertEquals(45f, result, 0.0001f);
		assertTrue(result > 0f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNegativeDistance() {
		RealtimeDamage.resolve(
				10f, 1f, -1f, 10f, 0f,
				RealtimeDamage.HitZone.CORE, null
		);
	}
}
