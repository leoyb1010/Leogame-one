package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovDeploymentFlowTest {

	@Test
	public void groundStarterPairIsNeededOnlyForIncompleteCombatLoadouts() {
		RaidItem needle = item(
				"needle",
				"firearm:needle_9",
				1,
				0.90f);
		RaidItem standard = item(
				"standard",
				"ammo:ammo_9_standard",
				24,
				0.012f);
		RaidItem training = item(
				"training",
				"ammo:ammo_9_training",
				18,
				0.012f);
		RaidItem wrongCaliber = item(
				"wrong",
				"ammo:ammo_556_standard",
				24,
				0.013f);
		RaidItem unknownNineMillimeter = item(
				"unknown",
				"ammo:ammo_9_unregistered",
				24,
				0.012f);

		assertTrue(BukovStarterProvisioning
				.requiresGroundCombatPair(Collections.<RaidItem>emptyList()));
		assertTrue(BukovStarterProvisioning
				.requiresGroundCombatPair(Collections.singletonList(needle)));
		assertTrue(BukovStarterProvisioning.requiresGroundCombatPair(
				Arrays.asList(needle, wrongCaliber)));
		assertTrue(BukovStarterProvisioning.requiresGroundCombatPair(
				Arrays.asList(needle, unknownNineMillimeter)));
		assertFalse(BukovStarterProvisioning.requiresGroundCombatPair(
				Arrays.asList(needle, standard)));
		assertFalse(BukovStarterProvisioning.requiresGroundCombatPair(
				Arrays.asList(needle, training)));
	}

	@Test
	public void successfulRaidMovesSelectedUidsOutAndBackIntoStash()
			throws IOException {
		BukovSaveService saves = preparedProfile();
		BukovRaidCoordinator raid = start(saves, "loadout-success");

		BukovProfile deployed = saves.loadProfile();
		assertEquals(0, deployed.stash().distinctItemCount());
		assertEquals(0, deployed.loadout().distinctItemCount());
		assertEquals(3, raid.loot().distinctItemCount());
		assertEquals(40L, raid.loot().totalQuantity());
		assertEquals(3, deployed.lastLoadoutDefinitions().size());

		completeExtraction(raid);
		RaidResult result = raid.settleSuccess();
		BukovProfile settled = saves.loadProfile();

		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertEquals(3, settled.stash().distinctItemCount());
		assertTrue(settled.stash().contains(BukovStarterProvisioning.WEAPON_UID));
		assertTrue(settled.stash().contains(BukovStarterProvisioning.AMMO_UID));
		assertTrue(settled.stash().contains(BukovStarterProvisioning.MEDICAL_UID));
	}

	@Test
	public void deathLosesSelectedLoadoutButKeepsDurableReceipt()
			throws IOException {
		BukovSaveService saves = preparedProfile();
		BukovRaidCoordinator raid = start(saves, "loadout-death");

		RaidResult result = raid.settleDeath();
		BukovProfile settled = saves.loadProfile();

		assertEquals(RaidOutcome.DEATH, result.outcome());
		assertEquals(40L, result.lostQuantity());
		assertEquals(3, result.lostUids().size());
		assertEquals(3, settled.settlement("loadout-death").lostUids().size());
		assertEquals(0, settled.stash().distinctItemCount());
		assertTrue(settled.isSettled("loadout-death"));
		assertTrue(saves.loadRaidCheckpoint() == null);

		assertTrue(BukovStarterProvisioning.ensure(settled));
		assertEquals(3, settled.stash().distinctItemCount());
		assertEquals(3, settled.loadout().distinctItemCount());
		assertFalse(BukovStarterProvisioning.ensure(settled));
		saves.saveProfile(settled);

		BukovRaidCoordinator recovery = start(saves, "loadout-recovery-after-death");
		boolean hasFirearm = false;
		for (RaidItem item : recovery.loot().items()) {
			hasFirearm |= item.definitionId().startsWith("firearm:");
		}
		assertTrue("recovery deployment must remain combat-capable", hasFirearm);
	}

	@Test
	public void retainedFirearmReceivesItsOwnCaliberWithoutDuplication() {
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"retained-ward",
				"firearm:ward_556",
				1,
				3f,
				4_200,
				false,
				false,
				1f));

		assertTrue(BukovStarterProvisioning.ensure(profile));
		assertEquals(2, profile.stash().distinctItemCount());
		assertEquals(2, profile.loadout().distinctItemCount());
		assertEquals(
				24,
				findQuantity(
						profile,
						"ammo:ammo_556_standard"));
		assertEquals(
				0,
				findQuantity(
						profile,
						"ammo:ammo_9_standard"));
		assertFalse(BukovStarterProvisioning.ensure(profile));
		assertEquals(2, profile.stash().distinctItemCount());

		assertFalse(BukovStarterProvisioning.ensure(
				new BukovProfile(),
				true));
	}

	@Test
	public void existingCompatibleAmmoIsSelectedWithoutMintingAnotherStack() {
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"stashed-ward",
				"firearm:ward_556",
				1,
				3f,
				4_200,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				"stashed-556",
				"ammo:ammo_556_standard",
				7,
				0.012f,
				24,
				false,
				false,
				1f));

		assertTrue(BukovStarterProvisioning.ensure(profile));
		assertEquals(2, profile.stash().distinctItemCount());
		assertEquals(2, profile.loadout().distinctItemCount());
		assertTrue(profile.loadout().contains("stashed-ward"));
		assertTrue(profile.loadout().contains("stashed-556"));
		assertEquals(
				7,
				findQuantity(
						profile,
						"ammo:ammo_556_standard"));

		assertFalse(BukovStarterProvisioning.ensure(profile));
		assertEquals(2, profile.stash().distinctItemCount());
		assertEquals(
				7,
				findQuantity(
						profile,
						"ammo:ammo_556_standard"));
	}

	@Test
	public void partialRecoveryKitRepairsMissingWeaponWithoutUidCollision() {
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"provision:recovery:0:ammo_9_standard",
				"ammo:ammo_9_standard",
				11,
				0.012f,
				12,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				"provision:recovery:0:bandage",
				"bandage",
				1,
				0.12f,
				180,
				false,
				false,
				1f));

		assertTrue(BukovStarterProvisioning.ensure(profile));

		assertEquals(3, profile.stash().distinctItemCount());
		assertEquals(3, profile.loadout().distinctItemCount());
		assertEquals(11, findQuantity(
				profile,
				"ammo:ammo_9_standard"));
		assertTrue(profile.loadout().contains(
				"provision:recovery:0:ammo_9_standard"));
		assertTrue(profile.loadout().contains(
				"provision:recovery:0:bandage"));
		assertFalse(BukovStarterProvisioning.ensure(profile));
	}

	@Test
	public void interruptedProfileWriteIsReconciledFromDurableRaid()
			throws IOException {
		BukovSaveService prepared = preparedProfile();
		FailingProfileSaveService saves =
				new FailingProfileSaveService(prepared);
		saves.failProfileSave = true;

		try {
			start(saves, "loadout-recovery");
			fail("profile failure must be reported");
		} catch (IOException expected) {
			assertTrue(saves.loadRaidCheckpoint() != null);
		}

		saves.failProfileSave = false;
		BukovRaidCoordinator resumed =
				BukovRaidCoordinator.resume(saves);
		assertEquals(3, resumed.loot().distinctItemCount());
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
		assertEquals(0, saves.loadProfile().loadout().distinctItemCount());
	}

	@Test
	public void trainingDeploymentLeavesStashAndFormalRaidCounterUntouched()
			throws IOException {
		BukovSaveService saves = preparedProfile();
		BukovProfile profile = saves.loadProfile();
		profile.selectRaidMode(BukovRaidMode.TRAINING_GROUND);
		profile.setCurrency(2_400L);
		saves.saveProfile(profile);

		BukovRaidCoordinator raid = start(saves, "training-safe");
		BukovProfile deployed = saves.loadProfile();
		assertEquals(3, deployed.stash().distinctItemCount());
		assertEquals(0, deployed.loadout().distinctItemCount());
		assertEquals(0, deployed.raidsStarted());
		assertEquals(5, raid.loot().distinctItemCount());
		assertEquals(
				BukovRaidCoordinator.TRAINING_FIREARM_DEFINITION,
				findByDefinition(
						raid,
						BukovRaidCoordinator.TRAINING_FIREARM_DEFINITION)
						.definitionId());
		assertEquals(
				BukovRaidCoordinator.TRAINING_AMMO_QUANTITY,
				findByDefinition(
						raid,
						BukovRaidCoordinator.TRAINING_AMMO_DEFINITION)
						.quantity());
		assertEquals(
				BukovRaidCoordinator.TRAINING_ARMOR_DEFINITION,
				findByDefinition(
						raid,
						BukovRaidCoordinator.TRAINING_ARMOR_DEFINITION)
						.definitionId());
		assertEquals(2, findByDefinition(
				raid, BukovRaidCoordinator.TRAINING_BLEED_DEFINITION)
				.quantity());
		assertEquals(2, findByDefinition(
				raid, BukovRaidCoordinator.TRAINING_HEAL_DEFINITION)
				.quantity());
		assertEquals(BukovRaidMode.TRAINING_GROUND,
				raid.session().raidMode());

		raid.pickup(new RaidItem(
				"practice-ammo",
				"ammo:ammo_9_training",
				18,
				0.012f,
				0,
				true,
				false,
				1f));
		RaidResult result = raid.settleDeath();
		BukovProfile settled = saves.loadProfile();

		assertEquals(0L, result.lostQuantity());
		assertEquals(3, settled.stash().distinctItemCount());
		assertEquals(2_400L, settled.currency());
		assertEquals(0, settled.raidsStarted());
		assertEquals(0, settled.statistics().deaths());
		assertTrue(settled.isSettled("training-safe"));
		for (RaidItem item : settled.stash().items()) {
			assertFalse(item.itemUid().startsWith("training:"));
		}
	}

	@Test
	public void trainingCheckpointResumeKeepsOneDisposableKit()
			throws IOException {
		BukovSaveService saves = preparedProfile();
		BukovProfile profile = saves.loadProfile();
		profile.selectRaidMode(BukovRaidMode.TRAINING_GROUND);
		saves.saveProfile(profile);

		BukovRaidCoordinator started = start(saves, "training-resume");
		assertEquals(5, started.loot().distinctItemCount());
		started.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		assertEquals(5, resumed.loot().distinctItemCount());
		assertEquals(
				1,
				findByDefinition(
						resumed,
						BukovRaidCoordinator.TRAINING_FIREARM_DEFINITION)
						.quantity());
		assertEquals(
				BukovRaidCoordinator.TRAINING_AMMO_QUANTITY,
				findByDefinition(
						resumed,
						BukovRaidCoordinator.TRAINING_AMMO_DEFINITION)
						.quantity());
	}

	private static BukovSaveService preparedProfile() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = saves.loadProfile();
		assertTrue(BukovStarterProvisioning.ensure(profile));
		saves.saveProfile(profile);
		return saves;
	}

	private static BukovRaidCoordinator start(
			BukovSaveService saves,
			String raidId) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				123L,
				raidId,
				40f,
				Collections.singletonList(ExtractionState.basic()));
	}

	private static void completeExtraction(BukovRaidCoordinator raid) {
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
	}

	private static RaidItem findByDefinition(
			BukovRaidCoordinator raid,
			String definitionId) {
		for (RaidItem item : raid.loot().items()) {
			if (definitionId.equals(item.definitionId())) {
				return item;
			}
		}
		throw new AssertionError("Missing raid item: " + definitionId);
	}

	private static RaidItem item(
			String uid,
			String definitionId,
			int quantity,
			float unitWeight) {
		return new RaidItem(
				uid,
				definitionId,
				quantity,
				unitWeight,
				1,
				false,
				false,
				1f);
	}

	private static int findQuantity(
			BukovProfile profile, String definitionId) {
		for (RaidItem item : profile.stash().items()) {
			if (definitionId.equals(item.definitionId())) {
				return item.quantity();
			}
		}
		return 0;
	}

	private static final class FailingProfileSaveService
			implements BukovSaveService {

		private final BukovSaveService delegate;
		private boolean failProfileSave;

		private FailingProfileSaveService(BukovSaveService delegate) {
			this.delegate = delegate;
		}

		@Override
		public BukovProfile loadProfile() throws IOException {
			return delegate.loadProfile();
		}

		@Override
		public void saveProfile(BukovProfile profile) throws IOException {
			if (failProfileSave) {
				throw new IOException("injected profile failure");
			}
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
		public BukovRaidCheckpoint loadRaidCheckpoint()
				throws IOException {
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
