package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovRuntimeLoadoutAdapterTest {

	@After
	public void resetHostState() {
		Dungeon.hero = null;
		Dungeon.quickslot.reset();
	}

	@Test
	public void materializesPrimaryMagazineReserveAndMedicalWithOriginalUids()
			throws IOException {
		LootTransaction ledger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_standard");
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(ledger);

		Firearm firearm = runtime.primaryWeapon();
		assertNotNull(firearm);
		assertEquals("needle_9", firearm.definitionId());
		assertEquals(BukovStarterProvisioning.WEAPON_UID, firearm.itemUid());
		assertEquals(BukovStarterProvisioning.WEAPON_UID, firearm.bukovItemUid());
		assertEquals(12, firearm.magazineAmmo());

		assertEquals(1, runtime.reserveAmmo().size());
		AmmoStack reserve = runtime.reserveAmmo().get(0);
		assertEquals("ammo_9_standard", reserve.definitionId());
		assertEquals(BukovStarterProvisioning.AMMO_UID, reserve.bukovItemUid());
		assertEquals(24, reserve.quantity());

		assertEquals(1, runtime.supplies().size());
		Item medical = runtime.supplies().get(0);
		assertEquals(BukovStarterProvisioning.MEDICAL_UID, medical.bukovItemUid());
		assertEquals(3, medical.quantity());
		assertEquals(3, runtime.allHostItems().size());
	}

	@Test
	public void distinctRaidUidsPreventHostStackMerging() throws IOException {
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(deployedLedger(
						"firearm:needle_9",
						"ammo:ammo_9_standard"));
		AmmoStack reserve = runtime.reserveAmmo().get(0);
		AmmoStack another = new AmmoStack().configure(
				"ammo_9_standard",
				5);
		another.assignBukovItemUid("another-physical-stack");

		assertFalse(reserve.isSimilar(another));
	}

	@Test
	public void installsOnlyDeployedItemsAndIsIdempotent() throws IOException {
		LootTransaction ledger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_standard");
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(ledger);
		Hero hero = new Hero();
		Dungeon.hero = hero;

		runtime.installOn(hero);
		runtime.installOn(hero);

		assertSame(runtime.primaryWeapon(), hero.belongings.weapon);
		assertEquals(2, hero.belongings.backpack.items.size());
		assertTrue(hero.belongings.backpack.items.contains(
				runtime.reserveAmmo().get(0)));
		assertTrue(hero.belongings.backpack.items.contains(
				runtime.supplies().get(0)));
		assertSame(runtime.primaryWeapon(), Dungeon.quickslot.getItem(0));
		for (int i = 1; i < Dungeon.quickslot.SIZE; i++) {
			assertNull(Dungeon.quickslot.getItem(i));
		}
		assertEquals(3, ledger.distinctItemCount());
		assertEquals(40L, ledger.totalQuantity());
	}

	@Test
	public void emptyDeploymentDoesNotGrantFallbackEquipment()
			throws IOException {
		LootTransaction ledger = new LootTransaction("zero-risk", 40f);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(ledger);
		Hero hero = new Hero();
		Dungeon.hero = hero;

		runtime.installOn(hero);

		assertNull(hero.belongings.weapon);
		assertTrue(hero.belongings.backpack.items.isEmpty());
		assertTrue(runtime.allHostItems().isEmpty());
		assertEquals(0, ledger.distinctItemCount());
	}

	@Test
	public void trainingDeploymentMaterializesLoadedDisposableWeapon()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = saves.loadProfile();
		profile.selectRaidMode(BukovRaidMode.TRAINING_GROUND);
		saves.saveProfile(profile);
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				91L,
				"runtime-training",
				40f,
				Collections.singletonList(ExtractionState.basic()));

		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(raid);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		runtime.installOn(hero);

		assertNotNull(runtime.primaryWeapon());
		assertEquals("needle_9", runtime.primaryWeapon().definitionId());
		assertEquals(12, runtime.primaryWeapon().magazineAmmo());
		assertSame(runtime.primaryWeapon(), hero.belongings.weapon);
		assertSame(runtime.primaryWeapon(), Dungeon.quickslot.getItem(0));
		assertEquals(1, runtime.reserveAmmo().size());
		assertEquals(
				BukovRaidCoordinator.TRAINING_AMMO_QUANTITY - 12,
				runtime.reserveAmmo().get(0).quantity());
		assertEquals(
				"ammo_9_training",
				runtime.reserveAmmo().get(0).definitionId());
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void runtimeConsumptionWritesBackBeforeSuccessOrDeath()
			throws IOException {
		LootTransaction successLedger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_standard");
		BukovRuntimeLoadoutAdapter adapter = adapter();
		BukovRuntimeLoadoutAdapter.RuntimeLoadout successRuntime =
				adapter.materialize(successLedger);
		consumeRuntimeResources(successRuntime);
		successRuntime.writeBack(successLedger);
		String firstWrite = successLedger.fingerprint();
		successRuntime.writeBack(successLedger);
		assertEquals(firstWrite, successLedger.fingerprint());

		assertEquals(30, successLedger.item(
				BukovStarterProvisioning.AMMO_UID).quantity());
		assertEquals(1, successLedger.item(
				BukovStarterProvisioning.MEDICAL_UID).quantity());
		assertEquals(0.50f, successLedger.item(
				BukovStarterProvisioning.WEAPON_UID).durability(), 0.0001f);

		BukovProfile extracted = new BukovProfile();
		RaidResult success = new RaidSettlement().settle(
				extracted,
				successLedger,
				RaidOutcome.SUCCESS);
		assertEquals(32L, success.transferredQuantity());
		assertEquals(30, extracted.stash().item(
				BukovStarterProvisioning.AMMO_UID).quantity());

		LootTransaction deathLedger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_standard");
		BukovRuntimeLoadoutAdapter.RuntimeLoadout deathRuntime =
				adapter.materialize(deathLedger);
		consumeRuntimeResources(deathRuntime);
		deathRuntime.writeBack(deathLedger);
		BukovProfile dead = new BukovProfile();
		RaidResult death = new RaidSettlement().settle(
				dead,
				deathLedger,
				RaidOutcome.DEATH);
		assertEquals(32L, death.lostQuantity());
		assertEquals(0, dead.stash().distinctItemCount());
	}

	@Test
	public void acceptsLegacyUnprefixedFirearmAndAmmoDefinitionIds()
			throws IOException {
		LootTransaction legacy = deployedLedger(
				"needle_9",
				"ammo_9_standard");

		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(legacy);

		assertNotNull(runtime.primaryWeapon());
		assertEquals("needle_9", runtime.primaryWeapon().definitionId());
		assertEquals(12, runtime.primaryWeapon().magazineAmmo());
		assertEquals(24, runtime.reserveAmmo().get(0).quantity());
	}

	@Test
	public void extractedFoundLootBecomesDeployableInTheNextRaid()
			throws IOException {
		LootTransaction extracted = new LootTransaction("found-loot", 40f);
		extracted.pickup(new RaidItem(
				"found-gun",
				"firearm:needle_9",
				1,
				0.90f,
				850,
				true,
				false,
				0.75f));
		extracted.pickup(new RaidItem(
				"found-ammo",
				"ammo:ammo_9_standard",
				24,
				0.012f,
				12,
				true,
				false,
				1f));
		BukovProfile profile = new BukovProfile();

		new RaidSettlement().settle(
				profile,
				extracted,
				RaidOutcome.SUCCESS);

		RaidItem stashedGun = profile.stash().withdraw("found-gun");
		RaidItem stashedAmmo = profile.stash().withdraw("found-ammo");
		assertNotNull(stashedGun);
		assertNotNull(stashedAmmo);
		assertFalse(stashedGun.foundInRaid());
		assertFalse(stashedAmmo.foundInRaid());

		LootTransaction nextRaid = new LootTransaction("next-raid", 40f);
		nextRaid.pickup(stashedGun);
		nextRaid.pickup(stashedAmmo);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(nextRaid);
		assertNotNull(runtime.primaryWeapon());
		assertEquals("found-gun", runtime.primaryWeapon().bukovItemUid());
		assertEquals(12, runtime.primaryWeapon().magazineAmmo());
		assertEquals(12, runtime.reserveAmmo().get(0).quantity());
	}

	@Test
	public void alternativeCompatibleVariantCanFillInitialMagazine()
			throws IOException {
		LootTransaction ledger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_subsonic");
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(ledger);
		FirearmDefinition definition = firearms().require("needle_9");

		assertEquals(
				"ammo_9_subsonic",
				runtime.primaryWeapon().loadedAmmoDefinitionId(definition));
		assertEquals(12, runtime.primaryWeapon().magazineAmmo());
	}

	@Test
	public void alternativeReloadKeepsEachAmmoVariantAcrossResumeAndSettlement()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = raidWith(
				saves,
				raidItem(
						"variant-gun",
						"firearm:needle_9",
						1,
						0.90f,
						850),
				raidItem(
						"standard-stack",
						"ammo:ammo_9_standard",
						36,
						0.012f,
						12),
				raidItem(
						"subsonic-stack",
						"ammo:ammo_9_subsonic",
						12,
						0.012f,
						18));
		BukovRuntimeLoadoutAdapter adapter = adapter();
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter.materialize(raid);
		Firearm firearm = runtime.primaryWeapon();
		FirearmDefinition definition = firearms().require("needle_9");
		AmmoStack subsonic = ammo(runtime, "ammo_9_subsonic");

		while (firearm.magazineAmmo() > 0) {
			assertTrue(firearm.consumeRound());
		}
		int loaded = subsonic.takeUpTo(definition.magazineSize);
		firearm.loadRounds(
				subsonic.definitionId(),
				loaded,
				definition);
		// The production heap synchronizer removes an exhausted reserve stack
		// immediately before loadout writeback. The rounds in the magazine
		// must restore both its ledger entry and checkpoint host identity.
		assertEquals(0, subsonic.quantity());
		assertNotNull(raid.loot().drop("subsonic-stack"));
		assertSame(
				subsonic,
				raid.checkpoint().releaseHostItem("subsonic-stack"));
		runtime.writeBack(raid.loot());
		String firstWrite = raid.loot().fingerprint();
		runtime.writeBack(raid.loot());
		assertEquals(firstWrite, raid.loot().fingerprint());
		assertSame(
				subsonic,
				raid.checkpoint().hostItem("subsonic-stack"));
		raid.saveCheckpoint();

		assertEquals(24, raid.loot().item("standard-stack").quantity());
		assertEquals(12, raid.loot().item("subsonic-stack").quantity());

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout restored =
				adapter.materialize(resumed);
		assertEquals(
				"ammo_9_subsonic",
				restored.primaryWeapon().loadedAmmoDefinitionId(definition));
		assertEquals(12, restored.primaryWeapon().magazineAmmo());
		assertEquals(24, ammo(restored, "ammo_9_standard").quantity());
		assertEquals(0, ammoQuantity(restored, "ammo_9_subsonic"));

		restored.writeBack(resumed.loot());
		BukovProfile extracted = new BukovProfile();
		new RaidSettlement().settle(
				extracted,
				resumed.loot(),
				RaidOutcome.SUCCESS);

		assertEquals(24, extracted.stash().item("standard-stack").quantity());
		assertEquals(12, extracted.stash().item("subsonic-stack").quantity());
	}

	@Test
	public void multipleDeployedFirearmsInstallPrimaryAndBackpackSecondaries()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = raidWith(
				saves,
				raidItem("primary-gun", "firearm:needle_9", 1, 0.90f, 850),
				raidItem("backup-gun", "firearm:shuttle_9", 1, 2.20f, 2100),
				raidItem("shared-ammo", "ammo:ammo_9_standard", 36, 0.012f, 12));

		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter().materialize(raid);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		runtime.installOn(hero);
		new BukovHeapLootAdapter(raid)
				.installCarriedRuntimeItems(hero);

		assertEquals("primary-gun",
				runtime.primaryWeapon().bukovItemUid());
		assertEquals(1, runtime.secondaryWeapons().size());
		assertEquals("backup-gun",
				runtime.secondaryWeapons().get(0).bukovItemUid());
		assertSame(runtime.primaryWeapon(), hero.belongings.weapon);
		assertFalse(hero.belongings.backpack.items.contains(
				runtime.primaryWeapon()));
		assertTrue(hero.belongings.backpack.items.contains(
				runtime.secondaryWeapons().get(0)));
		assertEquals(3, runtime.allHostItems().size());
		assertSame(
				runtime.primaryWeapon(),
				new BukovHeapLootAdapter(raid)
						.carriedHostItem("primary-gun"));
	}

	@Test
	public void checkpointResumeKeepsExactRuntimeWeaponStatesByUid()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = raidWith(
				saves,
				raidItem(
						"persistent-gun",
						"firearm:needle_9",
						1,
						0.90f,
						850),
				raidItem(
						"persistent-ammo",
						"ammo:ammo_9_standard",
						36,
						0.012f,
						12));
		BukovRuntimeLoadoutAdapter adapter = adapter();
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime =
				adapter.materialize(raid);
		runtime.primaryWeapon().consumeRound();
		runtime.primaryWeapon().consumeRound();
		runtime.primaryWeapon().setDurability(0.42f);
		runtime.primaryWeapon().setCondition(0.37f, 0.28f);
		runtime.writeBack(raid.loot());
		assertEquals(
				0.28f,
				raid.loot().item("persistent-gun").fouling(),
				0.0001f);
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout restored =
				adapter.materialize(resumed);

		assertEquals("persistent-gun",
				restored.primaryWeapon().bukovItemUid());
		assertEquals(10, restored.primaryWeapon().magazineAmmo());
		assertEquals(
				0.42f,
				restored.primaryWeapon().durability(),
				0.0001f);
		assertEquals(
				0.28f,
				restored.primaryWeapon().fouling(),
				0.0001f);
		assertSame(
				restored.primaryWeapon(),
				new BukovHeapLootAdapter(resumed)
						.carriedHostItem("persistent-gun"));
		assertEquals(24, restored.reserveAmmo().get(0).quantity());
	}

	@Test
	public void writeBackCarriesPersistentConditionIntoAFreshRaidRuntime()
			throws IOException {
		LootTransaction ledger = deployedLedger(
				"firearm:needle_9",
				"ammo:ammo_9_standard");
		BukovRuntimeLoadoutAdapter adapter = adapter();
		BukovRuntimeLoadoutAdapter.RuntimeLoadout first =
				adapter.materialize(ledger);
		first.primaryWeapon().setDurability(0.61f);
		first.primaryWeapon().setCondition(0.84f, 0.33f);

		first.writeBack(ledger);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout nextRaid =
				adapter.materialize(ledger);

		assertEquals(0.61f, nextRaid.primaryWeapon().durability(), 0.0001f);
		assertEquals(0.33f, nextRaid.primaryWeapon().fouling(), 0.0001f);
		assertEquals(
				"Heat is transient and must start cool in a new raid",
				0f,
				nextRaid.primaryWeapon().heat(),
				0f);
	}

	@Test
	public void selectedSecondFirearmRemainsPrimaryAfterCheckpointResume()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = raidWith(
				saves,
				raidItem(
						"first-gun",
						"firearm:needle_9",
						1,
						0.90f,
						850),
				raidItem(
						"selected-gun",
						"firearm:shuttle_9",
						1,
						2.20f,
						2100),
				raidItem(
						"shared-ammo",
						"ammo:ammo_9_standard",
						48,
						0.012f,
						12));
		BukovRuntimeLoadoutAdapter adapter = adapter();
		BukovRuntimeLoadoutAdapter.RuntimeLoadout initial =
				adapter.materialize(raid);
		assertEquals("first-gun", initial.primaryWeapon().bukovItemUid());

		raid.equipFirearm("selected-gun");
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		BukovRuntimeLoadoutAdapter.RuntimeLoadout restored =
				adapter.materialize(resumed);
		Hero hero = new Hero();
		Dungeon.hero = hero;
		restored.installOn(hero);

		assertEquals("selected-gun",
				restored.primaryWeapon().bukovItemUid());
		assertEquals(1, restored.secondaryWeapons().size());
		assertEquals("first-gun",
				restored.secondaryWeapons().get(0).bukovItemUid());
		assertSame(restored.primaryWeapon(), hero.belongings.weapon);
		assertEquals("selected-gun", resumed.equippedFirearmUid());
	}

	private static void consumeRuntimeResources(
			BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime) {
		runtime.primaryWeapon().consumeRound();
		runtime.primaryWeapon().consumeRound();
		runtime.primaryWeapon().setDurability(0.50f);
		assertEquals(4, runtime.reserveAmmo().get(0).takeUpTo(4));
		runtime.supplies().get(0).quantity(1);
	}

	private static LootTransaction deployedLedger(
			String firearmDefinition,
			String ammunitionDefinition) {
		LootTransaction ledger = new LootTransaction("runtime-loadout", 40f);
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(new RaidItem(
						BukovStarterProvisioning.WEAPON_UID,
						firearmDefinition,
						1,
						0.90f,
						850,
						false,
						false,
						1f)));
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(new RaidItem(
						BukovStarterProvisioning.AMMO_UID,
						ammunitionDefinition,
						36,
						0.012f,
						12,
						false,
						false,
						1f)));
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				ledger.pickup(new RaidItem(
						BukovStarterProvisioning.MEDICAL_UID,
						"bandage",
						3,
						0.12f,
						180,
						false,
						false,
						1f)));
		return ledger;
	}

	private static BukovRaidCoordinator raidWith(
			BukovSaveService saves,
			RaidItem... items) throws IOException {
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				77L,
				"runtime-owner",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		for (RaidItem item : items) {
			assertEquals(
					LootTransaction.PickupResult.ADDED,
					raid.loot().pickup(item));
		}
		return raid;
	}

	private static RaidItem raidItem(
			String uid,
			String definitionId,
			int quantity,
			float unitWeight,
			int unitValue) {
		return new RaidItem(
				uid,
				definitionId,
				quantity,
				unitWeight,
				unitValue,
				false,
				false,
				1f);
	}

	private static AmmoStack ammo(
			BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime,
			String definitionId) {
		for (AmmoStack stack : runtime.reserveAmmo()) {
			if (definitionId.equals(stack.definitionId())) {
				return stack;
			}
		}
		throw new AssertionError("Missing runtime ammunition: " + definitionId);
	}

	private static int ammoQuantity(
			BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime,
			String definitionId) {
		for (AmmoStack stack : runtime.reserveAmmo()) {
			if (definitionId.equals(stack.definitionId())) {
				return stack.quantity();
			}
		}
		return 0;
	}

	private static BukovRuntimeLoadoutAdapter adapter() throws IOException {
		return new BukovRuntimeLoadoutAdapter(firearms(), ammunition());
	}

	private static FirearmRegistry firearms() throws IOException {
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(read("firearms.json"));
		return registry;
	}

	private static AmmoRegistry ammunition() throws IOException {
		AmmoRegistry registry = new AmmoRegistry();
		registry.loadJson(read("ammunition.json"));
		return registry;
	}

	private static String read(String fileName) throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + fileName)),
				StandardCharsets.UTF_8);
	}
}
