package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentSlot;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovStarterProvisioning;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovServicesControllerTest {

	@Test
	public void hubExposesFourClaimableContractsAndPersistsReward()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		for (int index = 0; index < 3; index++) {
			LootTransaction loot = new LootTransaction(
					"ui-contract-" + index, 100f);
			loot.pickup(new RaidItem(
					"ui-loot-" + index,
					"loot:test",
					1, 1f, 5000,
					true, false, 1f));
			new RaidSettlement().settle(
					profile, loot, RaidOutcome.SUCCESS,
					30f, 9, false);
		}
		saves.saveProfile(profile);
		BukovHubController controller = new BukovHubController(saves);

		assertEquals(4, controller.servicesViewModel().contracts.size());
		assertTrue(controller.servicesViewModel().contracts.get(0).ready);
		assertEquals(
				BukovLongTermContractService.ClaimStatus.CLAIMED,
				controller.claimContract(
						BukovLongTermContractCatalog.SURVIVOR).status);
		assertEquals(600L, saves.loadProfile().currency());
		assertEquals(
				BukovLongTermContractService.ClaimStatus.ALREADY_CLAIMED,
				controller.claimContract(
						BukovLongTermContractCatalog.SURVIVOR).status);
		assertEquals(600L, saves.loadProfile().currency());
	}

	@Test
	public void selectedDeploymentItemInsuranceIsInteractiveAndPersistent()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController controller = new BukovHubController(saves);

		assertTrue(controller.toggleInsurance(
				BukovStarterProvisioning.WEAPON_UID));
		BukovServicesViewModel model = controller.servicesViewModel();
		boolean insured = false;
		for (BukovServicesViewModel.InsuranceRow row
				: model.insuranceItems) {
			if (BukovStarterProvisioning.WEAPON_UID.equals(row.itemUid)) {
				insured = row.insured;
			}
		}
		assertTrue(insured);
		assertTrue(saves.loadProfile().stash()
				.item(BukovStarterProvisioning.WEAPON_UID).insured());
		assertFalse(controller.toggleInsurance(
				BukovStarterProvisioning.WEAPON_UID));
		assertFalse(saves.loadProfile().stash()
				.item(BukovStarterProvisioning.WEAPON_UID).insured());
	}

	@Test
	public void workshopSlotsChangeDisplayedRuntimeParameters()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovHubController controller = new BukovHubController(saves);
		String firearmUid = BukovStarterProvisioning.WEAPON_UID;
		BukovServicesViewModel.FirearmRow base =
				findFirearm(controller.servicesViewModel(), firearmUid);

		controller.toggleAttachment(
				firearmUid, FirearmAttachmentSlot.OPTIC);
		controller.toggleAttachment(
				firearmUid, FirearmAttachmentSlot.MAGAZINE);
		controller.toggleAttachment(
				firearmUid, FirearmAttachmentSlot.MUZZLE);
		BukovServicesViewModel.FirearmRow modified =
				findFirearm(controller.servicesViewModel(), firearmUid);

		assertTrue(modified.effectiveMagazine > base.baseMagazine);
		assertTrue(modified.effectiveSpread < base.baseSpread);
		assertTrue(modified.effectiveRecoil < base.baseRecoil);
		assertTrue(modified.effectiveNoise < base.baseNoise);
		assertEquals(3, saves.loadProfile().firearmBuilds()
				.build(firearmUid).attachments().size());
	}

	@Test
	public void failedSaveLeavesControllerAndDurableProfileUnchanged()
			throws IOException {
		InMemoryBukovSaveService delegate =
				new InMemoryBukovSaveService();
		FailingSaveService saves = new FailingSaveService(delegate);
		BukovHubController controller = new BukovHubController(saves);
		saves.failProfileWrites = true;

		try {
			controller.toggleInsurance(
					BukovStarterProvisioning.WEAPON_UID);
			fail("save failure must be reported");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("forced"));
		}
		assertFalse(delegate.loadProfile().stash()
				.item(BukovStarterProvisioning.WEAPON_UID).insured());
		assertFalse(findInsurance(
				controller.servicesViewModel(),
				BukovStarterProvisioning.WEAPON_UID).insured);
	}

	@Test
	public void unknownLegacyFirearmDoesNotBreakServicesOrAcceptBuilds()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		RaidItem legacy = new RaidItem(
				"legacy-firearm",
				"firearm:removed_prototype",
				1,
				2f,
				500,
				false,
				false,
				1f);
		profile.stash().deposit(legacy);
		saves.saveProfile(profile);
		BukovHubController controller = new BukovHubController(saves);

		for (BukovServicesViewModel.FirearmRow row
				: controller.servicesViewModel().firearms) {
			assertFalse(legacy.itemUid().equals(row.itemUid));
		}
		try {
			controller.toggleAttachment(
					legacy.itemUid(), FirearmAttachmentSlot.OPTIC);
			fail("unsupported legacy firearm must reject workshop changes");
		} catch (IllegalArgumentException expected) {
			assertEquals(
					com.shatteredpixel.shatteredpixeldungeon.messages
							.BukovMessages.get(
									"bukov.economy.feedback.firearm_unsupported"),
					expected.getMessage());
		}
		assertEquals(0, saves.loadProfile().firearmBuilds().size());
	}

	private static BukovServicesViewModel.FirearmRow findFirearm(
			BukovServicesViewModel model, String uid) {
		for (BukovServicesViewModel.FirearmRow row : model.firearms) {
			if (uid.equals(row.itemUid)) return row;
		}
		throw new AssertionError("missing firearm " + uid);
	}

	private static BukovServicesViewModel.InsuranceRow findInsurance(
			BukovServicesViewModel model, String uid) {
		for (BukovServicesViewModel.InsuranceRow row
				: model.insuranceItems) {
			if (uid.equals(row.itemUid)) return row;
		}
		throw new AssertionError("missing insurance item " + uid);
	}

	private static final class FailingSaveService
			implements BukovSaveService {
		private final BukovSaveService delegate;
		private boolean failProfileWrites;

		private FailingSaveService(BukovSaveService delegate) {
			this.delegate = delegate;
		}

		@Override
		public BukovProfile loadProfile() throws IOException {
			return delegate.loadProfile();
		}

		@Override
		public void saveProfile(BukovProfile profile) throws IOException {
			if (failProfileWrites) throw new IOException("forced profile failure");
			delegate.saveProfile(profile);
		}

		@Override
		public RaidSession loadRaid() throws IOException {
			return delegate.loadRaid();
		}

		@Override
		public void saveRaid(RaidSession raid) throws IOException {
			delegate.saveRaid(raid);
		}

		@Override
		public BukovRaidCheckpoint loadRaidCheckpoint() throws IOException {
			return delegate.loadRaidCheckpoint();
		}

		@Override
		public void saveRaidCheckpoint(BukovRaidCheckpoint checkpoint)
				throws IOException {
			delegate.saveRaidCheckpoint(checkpoint);
		}

		@Override
		public void deleteRaid() throws IOException {
			delegate.deleteRaid();
		}
	}
}
