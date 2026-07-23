package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovDeploymentFlowTest {

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
