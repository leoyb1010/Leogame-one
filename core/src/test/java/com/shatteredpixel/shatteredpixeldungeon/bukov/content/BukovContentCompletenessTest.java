package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRole;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoVariant;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.MedicalCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovGearRules;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovVendorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** One regression gate for the complete first-raid authored content graph. */
public class BukovContentCompletenessTest {

	private static final Set<String> FIRST_RAID_FIREARMS =
			new HashSet<>(Arrays.asList(
					"needle_9",
					"shuttle_9",
					"ward_556",
					"mountain_762",
					"bolt_12",
					"longstreet_762",
					"sentinel_9",
					"sparrow_9",
					"hive_9",
					"whisper_9",
					"jackal_9",
					"river_556",
					"foundry_762",
					"carbine_556",
					"breaker_12",
					"rainstorm_12",
					"watchtower_556",
					"frontier_762"));

	@Test
	public void allEighteenFirearmsAreRegisteredAndObtainableFromLootAndVendor()
			throws IOException {
		AmmoRegistry ammunition = ammunition();
		FirearmRegistry firearms = firearms();
		firearms.validateAmmunition(ammunition);

		Set<String> actual = new HashSet<>();
		Set<String> vendorFirearms = new HashSet<>();
		for (BukovVendorCatalog.Offer offer : BukovVendorCatalog.all()) {
			if (offer.definitionId.startsWith("firearm:")) {
				vendorFirearms.add(
						offer.definitionId.substring("firearm:".length()));
			}
		}
		EnumSet<FirearmClass> classes =
				EnumSet.noneOf(FirearmClass.class);
		Set<String> balanceProfiles = new HashSet<>();
		Set<String> feedbackProfiles = new HashSet<>();
		for (FirearmDefinition definition : firearms.all()) {
			actual.add(definition.id);
			classes.add(definition.weaponClass);
			assertTrue(
					"Duplicate firearm balance profile: " + definition.id,
					balanceProfiles.add(
							definition.damage + ":"
									+ definition.rpm + ":"
									+ definition.magazineSize + ":"
									+ definition.effectiveRangeTiles + ":"
									+ definition.pellets));
			assertTrue(
					"Duplicate firearm feedback profile: " + definition.id,
					feedbackProfiles.add(
							definition.feedbackProfile + ":"
									+ definition.soundPitch + ":"
									+ definition.soundGain + ":"
									+ definition.muzzleIntensity + ":"
									+ definition.tracerIntensity + ":"
									+ definition.impactIntensity + ":"
									+ definition.feedbackIntensity));
			Item obtainable = BukovFirstRaidLootTables
					.createByEconomicDefinitionId("firearm:" + definition.id);
			assertNotNull("No obtainable raid item for " + definition.id, obtainable);
			assertTrue(obtainable instanceof BukovEconomicItem);
			BukovEconomicItem economy = (BukovEconomicItem) obtainable;
			assertEquals(definition.weightKg, economy.bukovUnitWeight(), 0.0001f);
			assertEquals(definition.value, economy.bukovUnitValue());
		}
		assertEquals(FIRST_RAID_FIREARMS, actual);
		assertEquals(FIRST_RAID_FIREARMS, vendorFirearms);
		assertEquals(EnumSet.allOf(FirearmClass.class), classes);
	}

	@Test
	public void ammunitionCoversFiveRolesAndEveryDefinitionHasAWeapon()
			throws IOException {
		AmmoRegistry ammunition = ammunition();
		FirearmRegistry firearms = firearms();
		EnumSet<AmmoVariant> variants = EnumSet.noneOf(AmmoVariant.class);
		Set<String> calibers = new HashSet<>();
		Set<String> authoredAmmo = new HashSet<>();
		Set<String> vendorAmmo = new HashSet<>();
		for (BukovVendorCatalog.Offer offer : BukovVendorCatalog.all()) {
			if (offer.definitionId.startsWith("ammo:")) {
				vendorAmmo.add(
						offer.definitionId.substring("ammo:".length()));
			}
		}
		for (AmmoDefinition ammo : ammunition.all()) {
			authoredAmmo.add(ammo.id);
			variants.add(ammo.variant);
			calibers.add(ammo.caliber);
			boolean supported = false;
			for (FirearmDefinition firearm : firearms.all()) {
				if (firearm.caliber.equals(ammo.caliber)) {
					supported = true;
					break;
				}
			}
			assertTrue("No firearm accepts " + ammo.id, supported);
		}
		assertEquals(EnumSet.allOf(AmmoVariant.class), variants);
		assertEquals(authoredAmmo, vendorAmmo);
		assertEquals(
				new HashSet<>(Arrays.asList("9x19", "5.56x45", "7.62x39", "12g")),
				calibers);
	}

	@Test
	public void lootHasThirtyEconomicItemsAndAllFourMedicalResponses() {
		Set<String> authoredLoot = new HashSet<>();
		Set<String> medicalAndTools = new HashSet<>();
		Set<String> definitions = new HashSet<>();
		for (BukovLootTable table : BukovFirstRaidLootTables.all().values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item item = entry.createForValidation();
				if (!(item instanceof BukovLootItem)) continue;
				BukovLootItem loot = (BukovLootItem)item;
				if (loot.bukovDefinitionId().startsWith("firearm:")) continue;
				definitions.add(loot.bukovDefinitionId());
				if (loot.category() == BukovLootItem.Category.MEDICAL
						|| loot.category() == BukovLootItem.Category.TOOL) {
					medicalAndTools.add(loot.bukovDefinitionId());
				} else {
					authoredLoot.add(loot.bukovDefinitionId());
				}
			}
		}
		assertTrue("First raid needs at least 30 authored loot items",
				authoredLoot.size() >= 30);
		assertTrue("First raid needs at least 8 medical/tool items",
				medicalAndTools.size() >= 8);
		assertTrue(definitions.containsAll(Arrays.asList(
				"first_aid",
				"tourniquet",
				"splint",
				"stim")));
	}

	@Test
	public void equipmentAndMedicalRosterMeetsThePlanScale() {
		assertEquals(3, ArmorCatalog.all().size());
		assertEquals(
				new HashSet<>(Arrays.asList("scout_pack", "field_pack")),
				new HashSet<>(BukovGearRules.allBackpackIds()));

		Set<String> obtainableMedical = new HashSet<>();
		Set<String> medicalAndTools = new HashSet<>();
		for (BukovLootTable table : BukovFirstRaidLootTables.all().values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item item = entry.createForValidation();
				if (!(item instanceof BukovLootItem)) continue;
				BukovLootItem loot = (BukovLootItem)item;
				if (loot.category() == BukovLootItem.Category.MEDICAL) {
					obtainableMedical.add(loot.bukovDefinitionId());
					medicalAndTools.add(loot.bukovDefinitionId());
				} else if (loot.category() == BukovLootItem.Category.TOOL) {
					medicalAndTools.add(loot.bukovDefinitionId());
				}
			}
		}
		MedicalCatalog.all().forEach(definition ->
				assertTrue(
						"Medical item is not obtainable: " + definition.id,
						obtainableMedical.contains(definition.id)));
		assertTrue(medicalAndTools.size() >= 8);
	}

	@Test
	public void enemyRosterReferencesOnlyRegisteredFirearms()
			throws IOException {
		EnemyArchetypeRegistry enemies = enemies();
		FirearmRegistry firearms = firearms();
		enemies.validateFirearms(firearms);

		int common = 0;
		int elite = 0;
		int boss = 0;
		Set<String> behaviorProfiles = new HashSet<>();
		for (EnemyArchetypeDefinition enemy : enemies.all()) {
			if (enemy.tier == EnemyTier.COMMON) common++;
			if (enemy.tier == EnemyTier.ELITE) elite++;
			if (enemy.tier == EnemyTier.BOSS) boss++;
			assertTrue("Enemy has no authored abilities: " + enemy.id,
					enemy.abilities.length > 0);
			assertTrue(
					"Enemy lacks a distinct behavior profile: " + enemy.id,
					behaviorProfiles.add(
							enemy.role + ":"
									+ Arrays.toString(enemy.abilities) + ":"
									+ enemy.movementSpeed + ":"
									+ enemy.engagementRange));
			if (enemy.role == EnemyRole.RANGED_SKIRMISHER
					|| enemy.role == EnemyRole.ARMORED_SUPPRESSOR
					|| enemy.role == EnemyRole.ELITE_COMMANDER) {
				assertNotNull(enemy.weaponDefinitionId);
				assertNotNull(firearms.require(enemy.weaponDefinitionId));
			}
		}
		assertEquals(9, common);
		assertEquals(3, elite);
		assertEquals(1, boss);
	}

	@Test
	public void allThreeExtractionContractsValidateAndRemainDistinct() {
		ExtractionDefinition baseline = ExtractionDefinition.baseline("baseline_room");
		ExtractionDefinition conditional =
				ExtractionDefinition.conditional("pump_room");
		ExtractionDefinition temporary =
				ExtractionDefinition.temporary("temporary_room", 480f);
		baseline.validate();
		conditional.validate();
		temporary.validate();

		assertEquals("E01", baseline.id);
		assertEquals("E02", conditional.id);
		assertEquals("E03", temporary.id);
		assertTrue(baseline.isAvailable(0f, Collections.emptySet()));
		assertFalse(conditional.isAvailable(600f, Collections.emptySet()));
		assertTrue(conditional.isAvailable(
				600f, Collections.singleton("pump_power")));
		assertFalse(temporary.isAvailable(479.99f, Collections.emptySet()));
		assertTrue(temporary.isAvailable(480f, Collections.emptySet()));
		assertFalse(temporary.isAvailable(600.01f, Collections.emptySet()));
	}

	@Test
	public void firstRaidArchiveIsAUniqueGuaranteedMissionRoll() {
		BukovLootTable table = BukovFirstRaidLootTables.require(
				FirstRaidMission.ARCHIVE_LOOT_TABLE_ID);
		assertEquals(1, table.entries().size());
		assertEquals(1, table.entries().get(0).minimumQuantity());
		assertEquals(1, table.entries().get(0).maximumQuantity());
		Item archive = table.roll(
				884422L, FirstRaidMission.ARCHIVE_CONTAINER_ID, 1).get(0);
		assertTrue(archive instanceof BukovMissionArchive);
		assertEquals(
				FirstRaidMission.ARCHIVE_DEFINITION_ID,
				((BukovEconomicItem)archive).bukovDefinitionId());
	}

	private static AmmoRegistry ammunition() throws IOException {
		AmmoRegistry registry = new AmmoRegistry();
		registry.loadJson(read("ammunition.json"));
		return registry;
	}

	private static FirearmRegistry firearms() throws IOException {
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(read("firearms.json"));
		return registry;
	}

	private static EnemyArchetypeRegistry enemies() throws IOException {
		EnemyArchetypeRegistry registry = new EnemyArchetypeRegistry();
		registry.loadJson(read("enemies.json"));
		return registry;
	}

	private static String read(String fileName) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + fileName)),
				StandardCharsets.UTF_8);
	}
}
