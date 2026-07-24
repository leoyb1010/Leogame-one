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

		assertEquals("针蜂-9", model.stashItems.get(0).label);
		assertEquals("9毫米标准弹", model.stashItems.get(1).label);
		assertEquals("战术绷带", model.stashItems.get(2).label);
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
				BukovHubViewModel.LoadoutSlot.PRIMARY).contains("针蜂-9"));
		assertTrue(model.canDeploy);
		assertEquals(null, model.deploymentBlockReason);
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
		assertEquals("针蜂-9缺少兼容弹药",
				missing.deploymentBlockReason);

		depositAndSelect(profile, item(
				"wrong-ammo",
				"ammo:ammo_556_standard",
				30,
				0.013f,
				18));
		BukovHubViewModel wrong =
				BukovHubViewModel.from(profile, 40f);
		assertFalse(wrong.canDeploy);
		assertEquals("针蜂-9缺少兼容弹药",
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
				"未知物资",
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
				"武器 2/3",
				model.inventoryFilterSummary(
						BukovHubViewModel.InventoryFilter.WEAPONS));
	}

	@Test
	public void portraitAndLandscapeKeepScrollableInventoryAboveFooter() {
		assertEquals(67, WndBukovHub.inventoryViewportHeight(226, false));
		assertEquals(48, WndBukovHub.inventoryViewportHeight(180, true));
		assertEquals(58, WndBukovHub.inventoryViewportHeight(217, false));
		assertEquals(20, WndBukovHub.inventoryViewportHeight(152, true));
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
		assertEquals("检查点已保存 · 02:05", model.activeRaidSummary());
		assertEquals(1, model.stashItems.size());
		assertTrue(model.stashItems.get(0).selected);
		assertEquals("针蜂-9", model.stashItems.get(0).label);
	}

	@Test
	public void hideoutPresentsCareerAndCurrentContractWithoutRawIds() {
		BukovProfile profile = new BukovProfile();
		BukovCareerProgression.reconcile(profile);

		BukovHubViewModel model = BukovHubViewModel.from(profile, 40f);

		assertEquals("合同 0/5 · 区域 1/6", model.careerSummary);
		assertEquals("找回维修档案", model.activeContract);
		assertTrue(model.activeContractObjective.contains("维修间档案"));
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
