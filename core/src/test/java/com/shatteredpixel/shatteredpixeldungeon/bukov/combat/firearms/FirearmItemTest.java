package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FirearmItemTest {

	@Test
	public void consumesRoundsAndClampsMagazineToDefinition() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		Firearm firearm = new Firearm().configure("test", "uid-1", 3);

		assertTrue(firearm instanceof Weapon);
		assertEquals(8, firearm.STRReq(0));
		assertEquals("3", firearm.status());
		assertTrue(firearm.consumeRound());
		assertEquals(2, firearm.magazineAmmo());
		assertEquals("2", firearm.status());

		firearm.setMagazineAmmo(100, definition);
		assertEquals(definition.magazineSize, firearm.magazineAmmo());

		firearm.setMagazineAmmo(-1, definition);
		assertEquals(0, firearm.magazineAmmo());
		assertFalse(firearm.consumeRound());
	}

	@Test
	public void bundleRoundTripPreservesInstanceState() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		Firearm original = new Firearm().configure(
				"test",
				"uid-7",
				6,
				"test_expanding");
		original.setDurability(0.42f);
		original.setCondition(0.37f, 0.18f);

		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		Firearm restored = new Firearm();
		restored.restoreFromBundle(bundle);

		assertEquals("test", restored.definitionId());
		assertEquals("uid-7", restored.itemUid());
		assertEquals(6, restored.magazineAmmo());
		assertEquals(
				"test_expanding",
				restored.loadedAmmoDefinitionId(definition));
		assertEquals(0.42f, restored.durability(), 0.0001f);
		assertEquals(0.37f, restored.heat(), 0.0001f);
		assertEquals(0.18f, restored.fouling(), 0.0001f);
	}

	@Test
	public void legacyBundleDefaultsNewConditionToCleanAndCool() {
		Firearm original = new Firearm().configure(
				"test",
				"uid-legacy",
				3);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);
		bundle.remove("heat");
		bundle.remove("fouling");

		Firearm restored = new Firearm();
		restored.restoreFromBundle(bundle);

		assertEquals(0f, restored.heat(), 0f);
		assertEquals(0f, restored.fouling(), 0f);
		assertEquals(1f, restored.durability(), 0f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void durabilityRejectsNonFiniteState() {
		new Firearm().setDurability(Float.NaN);
	}

	@Test
	public void liveShotsAgeHeatAndFoulingAndCoolingOnlyRemovesHeat() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		Firearm firearm = new Firearm().configure("test", "uid-condition", 6);

		float initialDurability = firearm.durability();
		firearm.recordShot(definition);

		assertTrue(firearm.heat() > 0f);
		assertTrue(firearm.fouling() > 0f);
		assertTrue(firearm.durability() < initialDurability);
		float foulingAfterShot = firearm.fouling();
		float durabilityAfterShot = firearm.durability();

		firearm.cool(1f);

		assertEquals(0f, firearm.heat(), 0.0001f);
		assertEquals(foulingAfterShot, firearm.fouling(), 0.0001f);
		assertEquals(durabilityAfterShot, firearm.durability(), 0.0001f);
	}

	@Test
	public void poorHotDirtyConditionAddsReadableSpreadPenalty() {
		Firearm firearm = new Firearm().configure(
				"test",
				"uid-condition-spread",
				6);
		assertEquals(0f, firearm.conditionSpreadPenaltyDeg(), 0.0001f);

		firearm.setDurability(0.35f);
		firearm.setCondition(0.90f, 0.75f);

		assertTrue(firearm.conditionSpreadPenaltyDeg() > 2f);
	}

	@Test
	public void ammoStacksMergeOnlyWhenAmmoTypesMatch() {
		AmmoStack first = new AmmoStack().configure("ammo_9_standard", 10);
		AmmoStack same = new AmmoStack().configure("ammo_9_standard", 5);
		AmmoStack different = new AmmoStack().configure("ammo_556_standard", 5);

		assertTrue(first.isSimilar(same));
		assertFalse(first.isSimilar(different));

		assertEquals(4, first.takeUpTo(4));
		assertEquals(6, first.quantity());
		assertEquals(6, first.takeUpTo(20));
		assertEquals(0, first.quantity());
	}

	@Test
	public void ammoBundleRoundTripPreservesTypeAndStacking() {
		AmmoStack original = new AmmoStack().configure("ammo_556_standard", 30);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		AmmoStack restored = new AmmoStack();
		restored.restoreFromBundle(bundle);

		assertEquals("ammo_556_standard", restored.definitionId());
		assertEquals(30, restored.quantity());
		assertTrue(restored.stackable);
	}

	@Test
	public void magazineCannotMixVariantsButCanSwitchWhenEmpty() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		Firearm firearm = new Firearm().configure(
				"test",
				"uid-variants",
				2,
				"test_standard");

		try {
			firearm.loadRounds("test_expanding", 2, definition);
			fail("mixed ammunition should be rejected");
		} catch (IllegalArgumentException expected) {
			assertEquals(2, firearm.magazineAmmo());
		}

		assertTrue(firearm.consumeRound());
		assertTrue(firearm.consumeRound());
		assertEquals(3, firearm.loadRounds("test_expanding", 3, definition));
		assertEquals("test_expanding", firearm.loadedAmmoDefinitionId(definition));
	}
}
