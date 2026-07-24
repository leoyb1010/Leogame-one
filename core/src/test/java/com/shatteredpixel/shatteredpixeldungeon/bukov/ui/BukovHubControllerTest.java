package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovStarterProvisioning;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHubControllerTest {

	@Test
	public void firstHubVisitPersistsStartingRegionAndCareerStatus()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();

		BukovHubController hub = new BukovHubController(saves);

		assertTrue(saves.loadProfile().unlockedMaps().contains("fog_depot"));
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.career_summary",
						0, 5, 1, 6),
				hub.viewModel().careerSummary);
	}

	@Test
	public void firstOpenCreatesAndPersistsRecommendedStarterDeployment()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		BukovProfile profile = saves.loadProfile();
		assertEquals(3, profile.stash().distinctItemCount());
		assertEquals(3, profile.loadout().distinctItemCount());
		assertFalse(hub.summary().isEmpty());
		assertEquals(3, hub.viewModel().stashItems.size());
		assertEquals(3, hub.viewModel().selectedCount);
		assertFalse(hub.viewModel().overweight);
		assertTrue(hub.viewModel().canDeploy);

		hub.confirmDeployment();
	}

	@Test
	public void unlockedRegionsCycleAndPersistAsNextDeploymentChoice()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovProfile profile = saves.loadProfile();
		profile.unlockMap("rust_workshop");
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		hub.cycleSelectedMap();

		assertEquals("rust_workshop", saves.loadProfile().selectedMap());
		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.map_rust_workshop"),
				hub.viewModel().selectedMapName);
		hub.cycleSelectedMap();
		assertEquals("fog_depot", saves.loadProfile().selectedMap());
	}

	@Test
	public void clearAndRecommendArePersisted() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.clearLoadout();
		assertEquals(0, saves.loadProfile().loadout().distinctItemCount());
		assertFalse(hub.viewModel().canDeploy);
		assertEquals(
				BukovMessages.get("bukov.economy.hub.block_no_primary"),
				hub.viewModel().deploymentBlockReason);
		hub.confirmDeployment();
		assertTrue(hub.viewModel().canDeploy);

		hub.recommendLoadout();
		assertEquals(3, saves.loadProfile().loadout().distinctItemCount());

		String weaponUid = hub.viewModel().stashItems.get(0).itemUid;
		hub.toggleItem(weaponUid);
		assertEquals(2, hub.viewModel().selectedCount);
		assertFalse(hub.viewModel().stashItems.get(0).selected);
		assertFalse(hub.viewModel().canDeploy);
		assertEquals(
				BukovMessages.get("bukov.economy.hub.block_no_primary"),
				hub.viewModel().deploymentBlockReason);
	}

	@Test
	public void oneActionRepairsAndConfirmsADeployment() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.clearLoadout();

		hub.prepareAndConfirmDeployment();

		assertTrue(hub.viewModel().canDeploy);
		assertEquals(null, hub.viewModel().deploymentBlockReason);
		assertTrue(saves.loadProfile().loadout().contains(
				BukovStarterProvisioning.WEAPON_UID));
		assertTrue(saves.loadProfile().loadout().contains(
				BukovStarterProvisioning.AMMO_UID));
	}

	@Test
	public void recommendationSkipsUnsupportedGunAndProvisionsUsablePair()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"unsupported-first",
				"firearm:prototype_unknown",
				1,
				1f,
				100,
				false,
				false,
				1f));
		saves.saveProfile(profile);
		BukovHubController hub = new BukovHubController(saves);
		hub.clearLoadout();

		hub.prepareAndConfirmDeployment();

		BukovProfile repaired = saves.loadProfile();
		assertFalse(repaired.loadout().contains("unsupported-first"));
		assertTrue(hasSelectedDefinition(
				repaired,
				"firearm:needle_9"));
		assertTrue(hasSelectedDefinition(
				repaired,
				"ammo:ammo_9_standard"));
		assertTrue(hub.viewModel().canDeploy);
	}

	@Test
	public void fullInvalidLoadoutStillOpensAndOneClickRepairsIt()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		for (int index = 0; index < 12; index++) {
			RaidItem filler = new RaidItem(
					"filler-" + index,
					"medical:filler_" + index,
					1,
					0.1f,
					1,
					false,
					false,
					1f);
			profile.stash().deposit(filler);
			profile.loadout().select(
					filler.itemUid(), profile.stash());
		}
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		assertFalse(hub.viewModel().canDeploy);

		hub.prepareAndConfirmDeployment();

		assertTrue(hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().selectedCount >= 2);
	}

	@Test
	public void oversizedAmmoStackGetsBoundedRecoveryAmmo()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem weapon = new RaidItem(
				"retained-needle",
				"firearm:needle_9",
				1,
				0.9f,
				850,
				false,
				false,
				1f);
		RaidItem oversizedAmmo = new RaidItem(
				"oversized-nine",
				"ammo:ammo_9_standard",
				4_000,
				0.012f,
				12,
				false,
				false,
				1f);
		profile.stash().deposit(weapon);
		profile.stash().deposit(oversizedAmmo);
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		hub.prepareAndConfirmDeployment();

		BukovProfile repaired = saves.loadProfile();
		assertTrue(hub.viewModel().canDeploy);
		assertFalse(repaired.loadout().contains(
				oversizedAmmo.itemUid()));
		assertEquals(3, repaired.stash().distinctItemCount());

		new BukovHubController(saves);
		assertEquals(3,
				saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void selectedOversizedAmmoIsReplacedByOneClickDeployment()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem weapon = raidItem(
				"selected-needle",
				"firearm:needle_9",
				1,
				0.9f,
				850);
		RaidItem oversizedAmmo = raidItem(
				"selected-oversized-nine",
				"ammo:ammo_9_standard",
				4_000,
				0.012f,
				12);
		profile.stash().deposit(weapon);
		profile.stash().deposit(oversizedAmmo);
		profile.loadout().select(weapon.itemUid(), profile.stash());
		profile.loadout().select(
				oversizedAmmo.itemUid(), profile.stash());
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		assertFalse(hub.viewModel().canDeploy);

		hub.confirmDeployment();

		BukovProfile repaired = saves.loadProfile();
		assertTrue(hub.viewModel().canDeploy);
		assertFalse(repaired.loadout().contains(
				oversizedAmmo.itemUid()));
		assertTrue(hasSelectedDefinition(
				repaired,
				"ammo:ammo_9_standard"));
	}

	@Test
	public void nearCapacityLegacyGunFallsBackToStandardRecoveryPair()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem legacyHeavyGun = raidItem(
				"legacy-heavy-needle",
				"firearm:needle_9",
				1,
				39.9f,
				850);
		profile.stash().deposit(legacyHeavyGun);
		profile.loadout().select(
				legacyHeavyGun.itemUid(), profile.stash());
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		hub.confirmDeployment();

		BukovProfile repaired = saves.loadProfile();
		assertTrue(hub.viewModel().canDeploy);
		assertFalse(repaired.loadout().contains(
				legacyHeavyGun.itemUid()));
		assertTrue(hasSelectedDefinition(
				repaired,
				"firearm:needle_9"));
		assertTrue(hasSelectedDefinition(
				repaired,
				"ammo:ammo_9_standard"));
	}

	@Test
	public void malformedStackedGearIsSkippedDuringAutomaticRepair()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(raidItem(
				"stacked-armor",
				"armor:soft_vest",
				2,
				2.4f,
				1_400));
		profile.stash().deposit(raidItem(
				"stacked-pack",
				"backpack:field_pack",
				2,
				2f,
				3_100));
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		hub.clearLoadout();
		hub.confirmDeployment();

		BukovProfile repaired = saves.loadProfile();
		assertTrue(hub.viewModel().canDeploy);
		assertFalse(repaired.loadout().contains("stacked-armor"));
		assertFalse(repaired.loadout().contains("stacked-pack"));
	}

	@Test
	public void modeCycleReachesRiskFreeTrainingGround() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);

		hub.cycleRaidMode();

		assertEquals(BukovRaidMode.TRAINING_GROUND,
				saves.loadProfile().selectedRaidMode());
		assertEquals(
				BukovMessages.get(
						"bukov.economy.mode.name_training_ground"),
				hub.viewModel().raidModeName);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.mode.summary_training_ground"),
				hub.viewModel().raidModeSummary);
		assertEquals(0, hub.viewModel().selectedCount);
		assertTrue(hub.viewModel().canDeploy);
	}

	@Test
	public void trainingIsDirectWhileFormalCycleNeverDependsOnIt()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.selectTrainingGround();
		assertEquals(
				BukovRaidMode.TRAINING_GROUND,
				hub.selectedRaidMode());
		assertEquals(0, hub.viewModel().selectedCount);

		hub.cycleFormalRaidMode();
		assertEquals(BukovRaidMode.EXPEDITION, hub.selectedRaidMode());
		assertTrue("returning from training must immediately restore a usable kit",
				hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().selectedCount >= 2);
		hub.cycleFormalRaidMode();
		assertEquals(BukovRaidMode.QUICK_SWEEP, hub.selectedRaidMode());
		hub.cycleFormalRaidMode();
		assertEquals(BukovRaidMode.SCAVENGER, hub.selectedRaidMode());
		hub.cycleFormalRaidMode();
		assertEquals(BukovRaidMode.BOSS_CONTRACT, hub.selectedRaidMode());
		hub.cycleFormalRaidMode();
		assertEquals(BukovRaidMode.EXPEDITION, hub.selectedRaidMode());
	}

	@Test
	public void scavengerToFormalRestoresLoadoutOnSameController()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.selectRaidMode(BukovRaidMode.SCAVENGER);
		assertEquals(0, hub.viewModel().selectedCount);
		assertTrue(hub.viewModel().canDeploy);

		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);

		assertTrue(hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().selectedCount >= 2);
		hub.confirmDeployment();
	}

	@Test
	public void reopeningRiskFreeModeDoesNotSelectFormalLoadout()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.TRAINING_GROUND);

		BukovHubController reopened = new BukovHubController(saves);

		assertEquals(BukovRaidMode.TRAINING_GROUND,
				reopened.selectedRaidMode());
		assertEquals(0, reopened.viewModel().selectedCount);
		assertTrue(reopened.viewModel().canDeploy);
	}

	@Test
	public void activeRaidCanOnlyBeAbandonedThroughDurableSettlement()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovRaidCoordinator.start(
				saves,
				13L,
				"hub-abandon",
				40f,
				Collections.singletonList(ExtractionState.basic()));

		BukovHubController hub = new BukovHubController(saves);
		assertTrue(hub.hasActiveRaid());
		assertEquals(
				RaidOutcome.DEATH,
				hub.abandonActiveRaid().outcome());
		assertTrue(saves.loadRaidCheckpoint() == null);
		assertTrue(saves.loadProfile().isSettled("hub-abandon"));
	}

	@Test
	public void standardDeploymentRequiresACompatibleAmmoStack()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.clearLoadout();
		hub.toggleItem(BukovStarterProvisioning.WEAPON_UID);
		assertFalse(hub.viewModel().canDeploy);
		String noCompatibleAmmo = BukovMessages.get(
				"bukov.economy.hub.block_no_ammo",
				BukovMessages.get(
						"bukov.economy.item.firearm_needle_9"));
		assertEquals(
				noCompatibleAmmo,
				hub.viewModel().deploymentBlockReason);
		hub.confirmDeployment();
		assertTrue(hub.viewModel().canDeploy);

		BukovProfile profile = saves.loadProfile();
		profile.stash().deposit(new RaidItem(
				"wrong-ammo",
				"ammo:ammo_556_standard",
				30,
				0.013f,
				18,
				false,
				false,
				1f));
		saves.saveProfile(profile);
		hub = new BukovHubController(saves);
		assertTrue("hub reopen should restore the compatible reserve",
				hub.viewModel().canDeploy);
		hub.toggleItem(BukovStarterProvisioning.AMMO_UID);
		hub.toggleItem("wrong-ammo");
		assertFalse(hub.viewModel().canDeploy);
		hub.confirmDeployment();
		assertTrue(hub.viewModel().canDeploy);
		assertEquals(null, hub.viewModel().deploymentBlockReason);
		assertFalse(saves.loadProfile().loadout().contains("wrong-ammo"));
	}

	@Test
	public void recommendationPairsTheChosenWeaponWithCompatibleAmmunition()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"ward",
				"firearm:ward_556",
				1,
				3f,
				4200,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				"wrong-ammo-first",
				"ammo:ammo_9_standard",
				36,
				0.012f,
				12,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				"ward-ammo",
				"ammo:ammo_556_standard",
				24,
				0.013f,
				18,
				false,
				false,
				1f));
		saves.saveProfile(profile);

		BukovHubController hub = new BukovHubController(saves);
		hub.recommendLoadout();

		BukovProfile selected = saves.loadProfile();
		assertTrue(selected.loadout().contains("ward"));
		assertFalse(selected.loadout().contains("wrong-ammo-first"));
		assertTrue(selected.loadout().contains("ward-ammo"));
		assertTrue(hub.viewModel().canDeploy);
	}

	@Test
	public void vendorEntryRefreshesHubCurrencyAndPurchasedGear()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		profile.setCurrency(2_000L);
		// Avoid starter auto-provisioning changing the assertion surface.
		profile.stash().deposit(new RaidItem(
				"owned-firearm",
				"firearm:needle_9",
				1,
				0.9f,
				850,
				false,
				false,
				1f));
		saves.saveProfile(profile);
		BukovHubController hub = new BukovHubController(saves);

		assertEquals(32, hub.vendorOffers().size());
		hub.buy("hub-buy-001", "scout_pack_1");

		assertEquals(400L, hub.viewModel().currency);
		assertTrue(saves.loadProfile().stash()
				.contains("vendor:hub-buy-001"));
	}

	@Test
	public void equippedFieldPackRaisesTheSameDeploymentCapacityShownByHub()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem firearm = raidItem(
				"weapon", "firearm:needle_9", 1, 0.9f, 850);
		RaidItem ammo = raidItem(
				"ammo", "ammo:ammo_9_standard", 24, 0.012f, 12);
		RaidItem pack = raidItem(
				"pack", "backpack:field_pack", 1, 2f, 3_100);
		RaidItem heavy = raidItem(
				"heavy", "titanium_case", 1, 50f, 7_600);
		profile.stash().deposit(firearm);
		profile.stash().deposit(ammo);
		profile.stash().deposit(pack);
		profile.stash().deposit(heavy);
		profile.loadout().select(firearm.itemUid(), profile.stash());
		profile.loadout().select(ammo.itemUid(), profile.stash());
		profile.loadout().select(pack.itemUid(), profile.stash());
		profile.loadout().select(heavy.itemUid(), profile.stash());
		saves.saveProfile(profile);

		BukovHubViewModel model =
				new BukovHubController(saves).viewModel();

		assertEquals(
				BukovMessages.get(
						"bukov.economy.hub.weight_summary",
						"53.2",
						"56.0"),
				model.loadoutSummary());
		assertFalse(model.overweight);
		assertTrue(model.canDeploy);
	}

	@Test
	public void successfulSettlementCanRepeatLastDeployment() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				7L,
				"repeat-loadout",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
		raid.settleSuccess();

		BukovHubController hub = new BukovHubController(saves);
		assertTrue(hub.viewModel().canRepeatLoadout);
		assertEquals(3, hub.repeatLastLoadout());
		assertEquals(3, hub.viewModel().selectedCount);
	}

	@Test
	public void deathRepeatUsesRecoveryItemsWithoutReusingLostUids()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				9L,
				"repeat-after-death",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		Set<String> lostUids = new HashSet<>();
		raid.loot().items().forEach(item -> lostUids.add(item.itemUid()));
		raid.settleDeath();

		BukovHubController hub = new BukovHubController(saves);
		assertTrue(hub.viewModel().canRepeatLoadout);
		assertEquals(3, hub.repeatLastLoadout());

		BukovProfile recovered = saves.loadProfile();
		assertEquals(3, recovered.loadout().distinctItemCount());
		for (String selectedUid : recovered.loadout().selectedUids()) {
			assertFalse(lostUids.contains(selectedUid));
			assertTrue(selectedUid.startsWith("provision:recovery:"));
		}
		assertEquals(3, recovered.stash().distinctItemCount());
		assertEquals(1, recovered.settlements().size());
	}

	@Test
	public void quickSweepDeathRetainsGunAndRepairsMissingAmmunition()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovProfile prepared = saves.loadProfile();
		prepared.selectRaidMode(BukovRaidMode.QUICK_SWEEP);
		saves.saveProfile(prepared);

		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				13L,
				"quick-sweep-retained-gun",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		raid.settleDeath();

		BukovProfile afterDeath = saves.loadProfile();
		assertEquals(1, afterDeath.stash().distinctItemCount());
		assertTrue(afterDeath.stash().contains(
				BukovStarterProvisioning.WEAPON_UID));
		assertEquals(0, afterDeath.loadout().distinctItemCount());

		BukovHubController recoveredHub =
				new BukovHubController(saves);
		BukovProfile recovered = saves.loadProfile();
		assertEquals(2, recovered.stash().distinctItemCount());
		assertEquals(2, recovered.loadout().distinctItemCount());
		assertTrue(recovered.loadout().contains(
				BukovStarterProvisioning.WEAPON_UID));
		assertTrue(hasSelectedDefinition(
				recovered,
				"ammo:ammo_9_standard"));
		assertTrue(recoveredHub.viewModel().canDeploy);

		// Reopening the hideout must not mint a second recovery stack.
		new BukovHubController(saves);
		BukovProfile reopened = saves.loadProfile();
		assertEquals(2, reopened.stash().distinctItemCount());
		assertEquals(24, quantity(
				reopened,
				"ammo:ammo_9_standard"));

		BukovRaidCoordinator nextRaid = BukovRaidCoordinator.start(
				saves,
				14L,
				"quick-sweep-recovery-redeploy",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		assertTrue(hasDefinition(
				nextRaid,
				"firearm:needle_9"));
		assertTrue(hasDefinition(
				nextRaid,
				"ammo:ammo_9_standard"));
	}

	@Test
	public void tenHubRoundTripsKeepActiveCheckpointAuthoritative()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		new BukovHubController(saves);
		BukovRaidCoordinator.start(
				saves,
				11L,
				"active-round-trip",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		BukovProfile stored = saves.loadProfile();
		stored.stash().deposit(new RaidItem(
				"reserve-radio",
				"utility:compact_field_radio",
				1,
				0.4f,
				250,
				false,
				false,
				1f));
		stored.loadout().select("reserve-radio", stored.stash());
		saves.saveProfile(stored);

		Set<String> deployedUids = new HashSet<>();
		saves.loadRaidCheckpoint().loot().items().forEach(
				item -> deployedUids.add(item.itemUid()));
		assertEquals(3, deployedUids.size());

		for (int i = 0; i < 10; i++) {
			BukovHubController hub = new BukovHubController(saves);
			BukovHubViewModel model = hub.viewModel();

			assertTrue(model.activeRaid);
			assertFalse(model.canEditLoadout);
			assertFalse(model.canRepeatLoadout);
			assertEquals("active-round-trip", model.activeRaidId);
			assertEquals(3, model.selectedCount);
			BukovProfile persisted = saves.loadProfile();
			assertEquals(1, persisted.stash().distinctItemCount());
			assertEquals(1, persisted.loadout().distinctItemCount());
			assertTrue(persisted.stash().contains("reserve-radio"));
			assertTrue(persisted.loadout().contains("reserve-radio"));

			Set<String> currentUids = new HashSet<>();
			saves.loadRaidCheckpoint().loot().items().forEach(
					item -> currentUids.add(item.itemUid()));
			assertEquals(deployedUids, currentUids);
			currentUids.addAll(persisted.loadout().selectedUids());
			assertEquals(4, currentUids.size());

			assertLocked(() -> hub.recommendLoadout());
			assertLocked(() -> hub.clearLoadout());
			assertLocked(() -> hub.toggleItem(
					BukovStarterProvisioning.WEAPON_UID));
			assertLocked(() -> hub.repeatLastLoadout());

			// Continue is deliberately a no-write operation in recovery mode.
			hub.confirmDeployment();
			assertEquals(3, saves.loadRaidCheckpoint()
					.loot()
					.distinctItemCount());
		}
	}

	private static void assertLocked(IoAction action) throws IOException {
		try {
			action.run();
		} catch (IllegalStateException expected) {
			assertEquals(
					BukovMessages.get(
							"bukov.economy.feedback.loadout_raid_locked"),
					expected.getMessage());
			return;
		}
		throw new AssertionError("active raid mutation should be locked");
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

	private static boolean hasSelectedDefinition(
			BukovProfile profile, String definitionId) {
		for (RaidItem item : profile.loadout().items(profile.stash())) {
			if (definitionId.equals(item.definitionId())) return true;
		}
		return false;
	}

	private static int quantity(
			BukovProfile profile, String definitionId) {
		for (RaidItem item : profile.stash().items()) {
			if (definitionId.equals(item.definitionId())) {
				return item.quantity();
			}
		}
		return 0;
	}

	private static boolean hasDefinition(
			BukovRaidCoordinator raid, String definitionId) {
		for (RaidItem item : raid.loot().items()) {
			if (definitionId.equals(item.definitionId())) return true;
		}
		return false;
	}

	private interface IoAction {
		void run() throws IOException;
	}
}
