package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovBallisticsContentTest {

	@Test
	public void authoredAmmunitionContainsAllFiveVariantsAndEffectiveModifiers()
			throws IOException {
		AmmoRegistry ammunition = ammunition();
		EnumSet<AmmoVariant> variants = EnumSet.noneOf(AmmoVariant.class);
		for (AmmoDefinition definition : ammunition.all()) {
			variants.add(definition.variant);
		}

		assertEquals(EnumSet.allOf(AmmoVariant.class), variants);
		AmmoDefinition armorPiercing =
				ammunition.require("ammo_556_armor_piercing");
		assertTrue(armorPiercing.applyPenetration(10f) > 10f);
		AmmoDefinition expanding = ammunition.require("ammo_762_expanding");
		assertTrue(expanding.applyDamage(10f) > 10f);
		AmmoDefinition subsonic = ammunition.require("ammo_9_subsonic");
		assertTrue(subsonic.applyNoise(10f) < 10f);
	}

	@Test
	public void firearmDefaultsAndFirstRaidLootFormAValidContentGraph()
			throws IOException {
		AmmoRegistry ammunition = ammunition();
		FirearmRegistry firearms = firearms();
		firearms.validateAmmunition(ammunition);

		for (FirearmDefinition firearm : firearms.all()) {
			AmmoDefinition defaultAmmo = ammunition.require(firearm.defaultAmmo);
			assertEquals(firearm.caliber, defaultAmmo.caliber);
		}

		EnumSet<AmmoVariant> lootVariants =
				EnumSet.noneOf(AmmoVariant.class);
		for (BukovLootTable table : BukovFirstRaidLootTables.all().values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item item = entry.createForValidation();
				if (!(item instanceof AmmoStack)) {
					continue;
				}
				AmmoDefinition lootAmmo = ammunition.require(
						((AmmoStack) item).definitionId());
				lootVariants.add(lootAmmo.variant);
				assertTrue(hasCompatibleFirearm(firearms, lootAmmo));
				assertEquals(
						lootAmmo.weightKg,
						((AmmoStack) item).bukovUnitWeight(),
						0.0001f);
				assertEquals(
						lootAmmo.value,
						((AmmoStack) item).bukovUnitValue());
			}
		}
		assertEquals(EnumSet.allOf(AmmoVariant.class), lootVariants);
	}

	@Test
	public void everyAuthoredAmmunitionDefinitionFitsAnExistingFirearm()
			throws IOException {
		AmmoRegistry ammunition = ammunition();
		FirearmRegistry firearms = firearms();
		for (AmmoDefinition definition : ammunition.all()) {
			assertTrue(hasCompatibleFirearm(firearms, definition));
		}
	}

	private static boolean hasCompatibleFirearm(
			FirearmRegistry firearms,
			AmmoDefinition ammunition) {
		assertNotNull(ammunition);
		for (FirearmDefinition firearm : firearms.all()) {
			if (firearm.caliber.equals(ammunition.caliber)) {
				return true;
			}
		}
		return false;
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

	private static String read(String fileName) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + fileName)),
				StandardCharsets.UTF_8);
	}
}
