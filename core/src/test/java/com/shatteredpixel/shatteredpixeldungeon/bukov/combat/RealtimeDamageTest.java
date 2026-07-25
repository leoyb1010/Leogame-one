package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;
import org.junit.Test;

import java.util.EnumSet;

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
		RealtimeArmorState armor = RealtimeArmorState.fresh(
				new ArmorDefinition(
						"damage_test",
						2,
						100f,
						20f,
						EnumSet.of(RealtimeDamage.HitZone.CORE),
						0f,
						0f));

		float resolved = RealtimeDamage.resolve(
				20f, 1f, 5f, 10f, 10f,
				RealtimeDamage.HitZone.CORE, armor
		);

		assertEquals(9f, resolved, 0.0001f);
		assertTrue(armor.durabilityFraction() < 1f);
		assertTrue(armor.durabilityFraction() > 0f);
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
