package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Acceptance gate for the six player-facing first-raid firearm roles. */
public class BukovFirstRaidSixGunRolesTest {

	private static final Set<String> REPRESENTATIVE_SIX =
			new HashSet<>(Arrays.asList(
					"needle_9",
					"shuttle_9",
					"carbine_556",
					"bolt_12",
					"longstreet_762",
					"rainstorm_12"));

	@Test
	public void representativeSixCoverThePlannedRolesAndAreObtainable()
			throws Exception {
		FirearmRegistry registry = load();
		EnumSet<FirearmClass> roles = EnumSet.noneOf(FirearmClass.class);
		Set<String> calibers = new HashSet<>();
		Set<String> feelSignatures = new HashSet<>();
		for (String id : REPRESENTATIVE_SIX) {
			FirearmDefinition weapon = registry.require(id);
			roles.add(weapon.weaponClass);
			calibers.add(weapon.caliber);
			assertTrue(feelSignatures.add(
					weapon.rpm + ":" + weapon.magazineSize + ":"
							+ weapon.reloadSeconds + ":"
							+ weapon.baseSpreadDeg + ":"
							+ weapon.recoilPerShot + ":"
							+ weapon.damage + ":"
							+ weapon.effectiveRangeTiles + ":"
							+ weapon.feedbackProfile));
			Item loot = BukovFirstRaidLootTables
					.createByEconomicDefinitionId("firearm:" + id);
			assertNotNull("Representative firearm is not lootable: " + id, loot);
		}
		assertEquals(
				EnumSet.of(
						FirearmClass.PISTOL,
						FirearmClass.SUBMACHINE_GUN,
						FirearmClass.CARBINE,
						FirearmClass.SHOTGUN,
						FirearmClass.MARKSMAN_RIFLE,
						FirearmClass.HEAVY_WEAPON),
				roles);
		assertEquals(
				new HashSet<>(Arrays.asList(
						"9x19", "5.56x45", "7.62x39", "12g")),
				calibers);
	}

	@Test
	public void everyAuthoredGunHasItsOwnSoundAndFxSignature()
			throws Exception {
		FirearmRegistry registry = load();
		Set<String> profiles = new HashSet<>();
		for (FirearmDefinition weapon : registry.all()) {
			assertTrue(profiles.add(
					weapon.feedbackProfile + ":"
							+ weapon.soundPitch + ":"
							+ weapon.soundGain + ":"
							+ weapon.muzzleIntensity + ":"
							+ weapon.tracerIntensity + ":"
							+ weapon.impactIntensity + ":"
							+ weapon.feedbackIntensity));
		}
		assertEquals(18, profiles.size());
	}

	@Test
	public void sixGunProgressionIsDistributedAcrossRaidLootTiers() {
		assertTrue(contains(
				BukovFirstRaidLootTables.LOW, "firearm:needle_9"));
		assertTrue(contains(
				BukovFirstRaidLootTables.INDUSTRIAL, "firearm:shuttle_9"));
		assertTrue(contains(
				BukovFirstRaidLootTables.INDUSTRIAL, "firearm:bolt_12"));
		assertTrue(contains(
				BukovFirstRaidLootTables.HIGH_VALUE, "firearm:carbine_556"));
		assertTrue(contains(
				BukovFirstRaidLootTables.HIGH_VALUE, "firearm:longstreet_762"));
		assertTrue(contains(
				BukovFirstRaidLootTables.BOSS, "firearm:rainstorm_12"));
	}

	private static FirearmRegistry load() throws Exception {
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/firearms.json")),
				StandardCharsets.UTF_8);
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(json);
		return registry;
	}

	private static boolean contains(String tableId, String definitionId) {
		BukovLootTable table = BukovFirstRaidLootTables.require(tableId);
		for (BukovLootTable.Entry entry : table.entries()) {
			Item item = entry.createForValidation();
			if (item instanceof BukovEconomicItem
					&& definitionId.equals(
							((BukovEconomicItem)item).bukovDefinitionId())) {
				return true;
			}
		}
		return false;
	}
}
