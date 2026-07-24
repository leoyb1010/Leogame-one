package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovStarterProvisioning;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
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
	public void firstOpenCreatesAndPersistsRecommendedStarterDeployment()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		BukovProfile profile = saves.loadProfile();
		assertEquals(3, profile.stash().distinctItemCount());
		assertEquals(3, profile.loadout().distinctItemCount());
		assertTrue(hub.summary().contains("风险价值"));
		assertEquals(3, hub.viewModel().stashItems.size());
		assertEquals(3, hub.viewModel().selectedCount);
		assertFalse(hub.viewModel().overweight);
		assertTrue(hub.viewModel().canDeploy);

		hub.confirmDeployment();
	}

	@Test
	public void clearAndRecommendArePersisted() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.clearLoadout();
		assertEquals(0, saves.loadProfile().loadout().distinctItemCount());
		assertFalse(hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().deploymentBlockReason.contains("主武器"));
		assertBlocked(hub, "主武器");

		hub.recommendLoadout();
		assertEquals(3, saves.loadProfile().loadout().distinctItemCount());

		String weaponUid = hub.viewModel().stashItems.get(0).itemUid;
		hub.toggleItem(weaponUid);
		assertEquals(2, hub.viewModel().selectedCount);
		assertFalse(hub.viewModel().stashItems.get(0).selected);
		assertFalse(hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().deploymentBlockReason.contains("主武器"));
	}

	@Test
	public void modeCycleReachesRiskFreeTrainingGround() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);
		hub.selectRaidMode(BukovRaidMode.BOSS_CONTRACT);

		hub.cycleRaidMode();

		assertEquals(BukovRaidMode.TRAINING_GROUND,
				saves.loadProfile().selectedRaidMode());
		assertEquals("演练场", hub.viewModel().raidModeName);
		assertTrue(hub.viewModel().raidModeSummary.contains("无仓库损失"));
		assertEquals(0, hub.viewModel().selectedCount);
		assertTrue(hub.viewModel().canDeploy);
	}

	@Test
	public void standardDeploymentRequiresACompatibleAmmoStack()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController hub = new BukovHubController(saves);

		hub.clearLoadout();
		hub.toggleItem(BukovStarterProvisioning.WEAPON_UID);
		assertFalse(hub.viewModel().canDeploy);
		assertTrue(hub.viewModel().deploymentBlockReason
				.contains("兼容弹药"));
		assertBlocked(hub, "兼容弹药");

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
		hub.toggleItem("wrong-ammo");
		assertFalse(hub.viewModel().canDeploy);
		assertBlocked(hub, "兼容弹药");

		hub.toggleItem(BukovStarterProvisioning.AMMO_UID);
		assertTrue(hub.viewModel().canDeploy);
		assertEquals(null, hub.viewModel().deploymentBlockReason);
		hub.confirmDeployment();
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

		assertEquals("53.2/56.0kg", model.loadoutSummary());
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
			assertTrue(expected.getMessage().contains("行动进行中"));
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

	private static void assertBlocked(
			BukovHubController hub,
			String expectedReason) throws IOException {
		try {
			hub.confirmDeployment();
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains(expectedReason));
			return;
		}
		throw new AssertionError("invalid standard deployment should be blocked");
	}

	private interface IoAction {
		void run() throws IOException;
	}
}
