package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeArmorStateTest {

	@Test
	public void catalogProvidesThreeProgressiveArmorClasses() {
		assertEquals(3, ArmorCatalog.all().size());
		assertEquals(1, ArmorCatalog.require("soft_vest").armorClass);
		assertEquals(2, ArmorCatalog.require("armor:patrol_vest").armorClass);
		assertEquals(4, ArmorCatalog.require("ceramic_rig").armorClass);
		assertTrue(
				ArmorCatalog.require("ceramic_rig").penetrationResistance
						> ArmorCatalog.require("soft_vest").penetrationResistance);
	}

	@Test
	public void penetrationTrendAndCoverageAreResolvedTogether() {
		RaidItem armor = armorItem("armor:patrol_vest", 1f);
		ArmorDefinition definition = ArmorCatalog.require(armor.definitionId());

		RealtimeArmorState lowPen =
				RealtimeArmorState.fromRaidItem(armor, definition);
		RealtimeArmorState.HitResult stopped = lowPen.resolveBullet(
				30f,
				4f,
				RealtimeDamage.HitZone.CORE);
		assertTrue(stopped.armorCovered);
		assertFalse(stopped.penetrated);
		assertTrue(stopped.absorbedDamage > stopped.healthDamage);

		RealtimeArmorState highPen =
				RealtimeArmorState.fromRaidItem(armor, definition);
		RealtimeArmorState.HitResult penetrated = highPen.resolveBullet(
				30f,
				30f,
				RealtimeDamage.HitZone.CORE);
		assertTrue(penetrated.armorCovered);
		assertTrue(penetrated.penetrated);
		assertTrue(penetrated.healthDamage > stopped.healthDamage);
		assertTrue(penetrated.absorbedDamage < stopped.absorbedDamage);
	}

	@Test
	public void uncoveredHitDoesNotDamageArmor() {
		ArmorDefinition coreOnly = new ArmorDefinition(
				"core_only",
				1,
				50f,
				6f,
				EnumSet.of(RealtimeDamage.HitZone.CORE),
				0f,
				0f);
		RaidItem item = armorItem("core_only", 0.8f);
		RealtimeArmorState state =
				RealtimeArmorState.fromRaidItem(item, coreOnly);

		RealtimeArmorState.HitResult result = state.resolveBullet(
				25f,
				2f,
				RealtimeDamage.HitZone.LIMB);

		assertFalse(result.armorCovered);
		assertFalse(result.penetrated);
		assertEquals(25f, result.healthDamage, 0.0001f);
		assertEquals(0.8f, state.durabilityFraction(), 0.0001f);
	}

	@Test
	public void durabilityRoundTripsThroughRaidItemWithoutIdentityLoss() {
		RaidItem item = armorItem("armor:ceramic_rig", 0.75f);
		RealtimeArmorState state = RealtimeArmorState.fromRaidItem(
				item,
				ArmorCatalog.require(item.definitionId()));
		state.resolveBullet(
				40f,
				18f,
				RealtimeDamage.HitZone.CORE);
		RaidItem written = state.toRaidItem();

		assertEquals(item.itemUid(), written.itemUid());
		assertEquals(item.definitionId(), written.definitionId());
		assertEquals(item.quantity(), written.quantity());
		assertEquals(item.unitWeight(), written.unitWeight(), 0f);
		assertEquals(item.unitValue(), written.unitValue());
		assertEquals(item.foundInRaid(), written.foundInRaid());
		assertEquals(item.insured(), written.insured());
		assertTrue(written.durability() < 0.75f);
		assertTrue(written.durability() >= 0f);

		RealtimeArmorState restored = RealtimeArmorState.fromRaidItem(
				written,
				ArmorCatalog.require(written.definitionId()));
		assertEquals(
				written.durability(),
				restored.durabilityFraction(),
				0.0001f);
	}

	@Test
	public void repeatedHitsNeverProduceNegativeOrNonFiniteState() {
		RaidItem item = armorItem("soft_vest", 1f);
		RealtimeArmorState state = RealtimeArmorState.fromRaidItem(
				item,
				ArmorCatalog.require(item.definitionId()));
		for (int index = 0; index < 10_000; index++) {
			RealtimeArmorState.HitResult result = state.resolveBullet(
					1_000f,
					1_000f,
					RealtimeDamage.HitZone.CORE);
			assertTrue(Float.isFinite(result.healthDamage));
			assertTrue(Float.isFinite(result.absorbedDamage));
			assertTrue(Float.isFinite(result.durabilityFraction));
			assertTrue(result.durabilityFraction >= 0f);
		}
		assertTrue(state.broken());
		assertEquals(0f, state.toRaidItem().durability(), 0f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonFiniteDamage() {
		RaidItem item = armorItem("soft_vest", 1f);
		RealtimeArmorState.fromRaidItem(
				item,
				ArmorCatalog.require(item.definitionId())).resolveBullet(
				Float.NaN,
				1f,
				RealtimeDamage.HitZone.CORE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMismatchedDefinition() {
		RealtimeArmorState.fromRaidItem(
				armorItem("soft_vest", 1f),
				ArmorCatalog.require("ceramic_rig"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsStackedEquippedArmor() {
		RaidItem stacked = new RaidItem(
				"armor-uid",
				"soft_vest",
				2,
				3.5f,
				1_000,
				false,
				false,
				1f);
		RealtimeArmorState.fromRaidItem(
				stacked,
				ArmorCatalog.require(stacked.definitionId()));
	}

	private static RaidItem armorItem(String definitionId, float durability) {
		return new RaidItem(
				"armor-uid",
				definitionId,
				1,
				3.5f,
				1_000,
				false,
				true,
				durability);
	}
}
