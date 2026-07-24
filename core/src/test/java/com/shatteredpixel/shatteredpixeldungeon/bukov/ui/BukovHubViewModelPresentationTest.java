package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovCareerProgression;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHubViewModelPresentationTest {

	@Test
	public void knownRuntimeDefinitionsUsePlayerFacingNamesAndSlots() {
		BukovProfile profile = new BukovProfile();
		depositAndSelect(profile, item(
				"weapon",
				"firearm:needle_9",
				1,
				0.9f,
				850));
		depositAndSelect(profile, item(
				"ammo",
				"ammo:ammo_9_standard",
				36,
				0.012f,
				12));
		depositAndSelect(profile, item(
				"medical",
				"bandage",
				3,
				0.12f,
				180));

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertEquals(
				BukovMessages.get(
						"bukov.economy.item.firearm_needle_9"),
				model.stashItems.get(0).label);
		assertEquals(
				BukovMessages.get("bukov.economy.item.ammo_9_standard"),
				model.stashItems.get(1).label);
		assertEquals(
				BukovMessages.get("bukov.economy.item.bandage"),
				model.stashItems.get(2).label);
		assertFalse(model.stashItems.get(0).summary().contains("needle_9"));
		assertFalse(model.stashItems.get(1).summary().contains("ammo_9_standard"));
		assertEquals(
				BukovHubViewModel.LoadoutSlot.PRIMARY,
				model.stashItems.get(0).slot);
		assertEquals(
				BukovHubViewModel.LoadoutSlot.AMMUNITION,
				model.stashItems.get(1).slot);
		assertEquals(
				BukovHubViewModel.LoadoutSlot.MEDICAL,
				model.stashItems.get(2).slot);
		assertTrue(model.slotSummary(
				BukovHubViewModel.LoadoutSlot.PRIMARY).contains(
						BukovMessages.get(
								"bukov.economy.item.firearm_needle_9")));
		assertTrue(model.canDeploy);
		assertEquals(null, model.deploymentBlockReason);
		assertEquals(
				BukovMessages.get("bukov.economy.hub.readiness_ready"),
				model.deploymentReadinessHeadline());
	}

	@Test
	public void deploymentReadinessExplainsMissingAndWrongAmmunition() {
		BukovProfile profile = new BukovProfile();
		depositAndSelect(profile, item(
				"weapon",
				"firearm:needle_9",
				1,
				0.9f,
				850));

		BukovHubViewModel missing =
				BukovHubViewModel.from(profile, 40f);
		assertFalse(missing.canDeploy);
		assertEquals(BukovMessages.get(
						"bukov.economy.hub.block_no_ammo",
						BukovMessages.get(
								"bukov.economy.item.firearm_needle_9")),
				missing.deploymentBlockReason);
		assertEquals(BukovMessages.get(
						"bukov.economy.hub.readiness_blocked",
						missing.deploymentBlockReason),
				missing.deploymentReadinessHeadline());

		depositAndSelect(profile, item(
				"wrong-ammo",
				"ammo:ammo_556_standard",
				30,
				0.013f,
				18));
		BukovHubViewModel wrong =
				BukovHubViewModel.from(profile, 40f);
		assertFalse(wrong.canDeploy);
		assertEquals(BukovMessages.get(
						"bukov.economy.hub.block_no_ammo",
						BukovMessages.get(
								"bukov.economy.item.firearm_needle_9")),
				wrong.deploymentBlockReason);

		depositAndSelect(profile, item(
				"compatible-ammo",
				"ammo:ammo_9_subsonic",
				12,
				0.013f,
				22));
		assertTrue(BukovHubViewModel.from(profile, 40f).canDeploy);
	}

	@Test
	public void emptyFormalLoadoutPointsToEquipmentOrFreeTraining() {
		BukovProfile profile = new BukovProfile();

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertFalse(model.canDeploy);
		assertEquals(
				BukovMessages.get("bukov.economy.hub.block_no_primary"),
				model.deploymentBlockReason);
	}

	@Test
	public void freeTrainingAdvertisesImmediateSafeTesting() {
		BukovProfile profile = new BukovProfile();
		profile.selectRaidMode(
				com.shatteredpixel.shatteredpixeldungeon.bukov.raid
						.BukovRaidMode.TRAINING_GROUND);

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertTrue(model.canDeploy);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.readiness_training"),
				model.deploymentReadinessHeadline());
	}

	@Test
	public void everyAuthoredFirearmUsesRegistryCaliberForDeployment()
			throws Exception {
		FirearmRegistry firearms = new FirearmRegistry();
		firearms.loadJson(asset("firearms.json"));
		AmmoRegistry ammunition = new AmmoRegistry();
		ammunition.loadJson(asset("ammunition.json"));
		firearms.validateAmmunition(ammunition);

		assertEquals(18, firearms.all().size());
		for (FirearmDefinition firearm : firearms.all()) {
			BukovProfile profile = new BukovProfile();
			depositAndSelect(profile, item(
					"weapon-" + firearm.id,
					"firearm:" + firearm.id,
					1,
					firearm.weightKg,
					firearm.value));
			depositAndSelect(profile, item(
					"ammo-" + firearm.id,
					"ammo:" + firearm.defaultAmmo,
					30,
					0.01f,
					10));

			BukovHubViewModel model = BukovHubViewModel.from(
					profile,
					40f,
					firearms,
					ammunition);

			assertTrue(
					firearm.id + " must accept " + firearm.defaultAmmo,
					model.canDeploy);
		}
	}

	@Test
	public void unknownEnglishDefinitionIsHumanizedInsteadOfLeakingRawId() {
		assertEquals(
				"Compact Field Radio",
				BukovHubViewModel.displayName(
						"utility:compact_field_radio"));
		assertEquals(
				BukovMessages.get("bukov.economy.item.unknown"),
				BukovHubViewModel.displayName(""));
	}

	@Test
	public void inventoryFiltersExposeRarityAndCategoryValueComparison() {
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(item(
				"cheap-weapon",
				"firearm:needle_9",
				1,
				0.9f,
				100));
		profile.stash().deposit(item(
				"rare-weapon",
				"firearm:mountain_762",
				1,
				4.1f,
				1_900));
		profile.stash().deposit(item(
				"medical",
				"bandage",
				1,
				0.12f,
				180));

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertEquals(
				2,
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.WEAPONS).size());
		assertEquals(
				1,
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.MEDICAL).size());
		assertEquals(
				0,
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.AMMUNITION).size());
		assertEquals(
				BukovHubViewModel.ItemRarity.COMMON,
				model.stashItems.get(0).rarity);
		assertEquals(
				BukovHubViewModel.ItemRarity.RARE,
				model.stashItems.get(1).rarity);
		assertTrue(model.stashItems.get(0).valueComparisonPercent < 0);
		assertTrue(model.stashItems.get(1).valueComparisonPercent > 0);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.filter_summary",
						BukovHubViewModel.InventoryFilter.WEAPONS.label,
						2,
						3),
				model.inventoryFilterSummary(
						BukovHubViewModel.InventoryFilter.WEAPONS));
	}

	@Test
	public void inventorySearchAndSortUsePlayerFacingMetadata() {
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(item(
				"needle",
				"firearm:needle_9",
				1,
				0.9f,
				850));
		profile.stash().deposit(item(
				"mountain",
				"firearm:mountain_762",
				1,
				4.1f,
				6_100));
		profile.stash().deposit(item(
				"bandage",
				"bandage",
				2,
				0.12f,
				180));
		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertEquals(
				"mountain",
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.ALL,
						BukovHubViewModel.InventorySort.VALUE_DESC,
						BukovHubViewModel.LoadoutSlot.PRIMARY.label)
						.get(0).itemUid);
		assertEquals(
				"needle",
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.ALL,
						BukovHubViewModel.InventorySort.WEIGHT_ASC,
						"firearm").get(0).itemUid);
		assertEquals(
				"bandage",
				model.inventoryItems(
						BukovHubViewModel.InventoryFilter.MEDICAL,
						BukovHubViewModel.InventorySort.NAME_ASC,
						BukovMessages.get(
								"bukov.economy.item.bandage"))
						.get(0).itemUid);
		assertTrue(model.inventoryItems(
				BukovHubViewModel.InventoryFilter.ALL,
				BukovHubViewModel.InventorySort.STASH_ORDER,
				"no-match-token").isEmpty());
	}

	@Test
	public void portraitAndLandscapeKeepScrollableInventoryAboveFooter() {
		assertEquals(42, WndBukovHub.inventoryViewportHeight(226, false));
		assertEquals(23, WndBukovHub.inventoryViewportHeight(180, true));
		assertEquals(33, WndBukovHub.inventoryViewportHeight(217, false));
		assertEquals(38, WndBukovHub.inventoryViewportHeight(152, true));
		assertEquals(5, WndBukovHub.inventoryViewportHeight(119, true));
		assertEquals(22f, WndBukovHub.mobileControlHeight(15f), 0f);
		assertEquals(24f, WndBukovHub.mobileControlHeight(24f), 0f);
		assertEquals("Confirm", WndBukovHub.shortActionLabel(
				"Confirm deployment"));
		assertEquals("确认出击", WndBukovHub.shortActionLabel("确认出击"));
		assertEquals("Filter", WndBukovHub.shortActionLabel(
				"Filter: All · 12"));
		assertEquals("筛选", WndBukovHub.shortActionLabel(
				"筛选：全部 · 12"));
		assertEquals("Extraor…", WndBukovHub.shortActionLabel(
				"Extraordinary"));
	}

	@Test
	public void activeRaidPresentsCheckpointLoadoutAsLockedRecoveryState() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("active-ui", 40f);
		loot.pickup(item(
				"deployed-weapon",
				"firearm:needle_9",
				1,
				0.9f,
				850));
		RaidSession session = RaidSession.create(17L, "active-ui");
		session.advance(125f);
		BukovRaidCheckpoint checkpoint = new BukovRaidCheckpoint(
				session,
				loot,
				Collections.singletonList(ExtractionState.basic()));

		BukovHubViewModel model =
				BukovHubViewModel.from(profile, checkpoint, 40f);

		assertTrue(model.activeRaid);
		assertTrue(model.canDeploy);
		assertFalse(model.canEditLoadout);
		assertFalse(model.canRepeatLoadout);
		assertEquals("active-ui", model.activeRaidId);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.checkpoint_summary",
						2,
						5),
				model.activeRaidSummary());
		assertEquals(1, model.stashItems.size());
		assertTrue(model.stashItems.get(0).selected);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.item.firearm_needle_9"),
				model.stashItems.get(0).label);
	}

	@Test
	public void hideoutPresentsCareerAndCurrentContractWithoutRawIds() {
		BukovProfile profile = new BukovProfile();
		BukovCareerProgression.reconcile(profile);

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.career_summary",
						0,
						5,
						1,
						6),
				model.careerSummary);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.contract_rust_workshop_title"),
				model.activeContract);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.contract_rust_workshop_objective"),
				model.activeContractObjective);
		assertFalse(model.activeContract.contains("maintenance_"));
	}

	private static RaidItem item(
			String uid,
			String definition,
			int quantity,
			float unitWeight,
			int unitValue) {
		return new RaidItem(
				uid,
				definition,
				quantity,
				unitWeight,
				unitValue,
				false,
				false,
				1f);
	}

	private static void depositAndSelect(BukovProfile profile, RaidItem item) {
		profile.stash().deposit(item);
		profile.loadout().select(item.itemUid(), profile.stash());
	}

	private static String asset(String file) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/" + file)),
				StandardCharsets.UTF_8);
	}
}
