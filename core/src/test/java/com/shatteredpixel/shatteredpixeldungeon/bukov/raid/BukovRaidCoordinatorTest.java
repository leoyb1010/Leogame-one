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

public class BukovRaidCoordinatorTest {

	@Test
	public void checkpointRestoresLootElapsedAndActiveExtraction() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = start(saves, "resume");
		assertEquals(
				LootTransaction.PickupResult.ADDED,
				raid.pickup(item("resume-loot")));
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(2f, ExtractionState.Interaction.ACTIVE);
		raid.saveCheckpoint();

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		assertEquals(2f, resumed.session().elapsedSeconds, 0.0001f);
		assertTrue(resumed.loot().contains("resume-loot"));
		assertEquals("E01", resumed.activeExtractionId());
		assertEquals(2f, resumed.extraction("E01").progressSeconds(), 0.0001f);

		resumed.tick(3f, ExtractionState.Interaction.ACTIVE);
		RaidResult result = resumed.settleSuccess();

		assertEquals(RaidOutcome.SUCCESS, result.outcome());
		assertEquals(1L, result.transferredQuantity());
		assertEquals(1, saves.loadProfile().stash().distinctItemCount());
		assertTrue(saves.loadRaidCheckpoint() == null);
	}

	@Test
	public void deathPersistsLossAndDeletesRaid() throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = start(saves, "death");
		raid.pickup(item("lost"));

		RaidResult result = raid.settleDeath();

		assertEquals(RaidOutcome.DEATH, result.outcome());
		assertEquals(1L, result.lostQuantity());
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
		assertTrue(saves.loadRaidCheckpoint() == null);
	}

	@Test
	public void failedProfileWriteDoesNotMutateLiveProfileOrDeleteRaid() throws IOException {
		FailingSaveService saves = new FailingSaveService();
		BukovRaidCoordinator raid = start(saves, "profile-failure");
		raid.pickup(item("still-carried"));
		completeExtraction(raid);
		saves.failProfileSave = true;

		try {
			raid.settleSuccess();
			fail("profile write failure must abort settlement");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("profile"));
		}

		assertEquals(0, raid.profile().stash().distinctItemCount());
		assertTrue(saves.loadRaidCheckpoint() != null);
		saves.failProfileSave = false;
		assertEquals(0, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void deleteFailureLeavesReceiptForIdempotentRecovery() throws IOException {
		FailingSaveService saves = new FailingSaveService();
		BukovRaidCoordinator raid = start(saves, "delete-failure");
		raid.pickup(item("paid-once"));
		completeExtraction(raid);
		saves.failRaidDelete = true;

		try {
			raid.settleSuccess();
			fail("delete failure must be visible");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("delete"));
		}

		BukovProfile committed = saves.loadProfile();
		assertTrue(committed.isSettled("delete-failure"));
		assertEquals(1, committed.stash().distinctItemCount());
		assertTrue(saves.loadRaidCheckpoint() != null);

		saves.failRaidDelete = false;
		assertTrue(BukovRaidCoordinator.resume(saves) == null);
		assertTrue(saves.loadRaidCheckpoint() == null);
		assertEquals(1, saves.loadProfile().stash().distinctItemCount());
	}

	@Test
	public void checkpointRecoversLastLoadoutWhenDeploymentProfileWriteFails()
			throws IOException {
		FailingSaveService saves = new FailingSaveService();
		BukovProfile profile = new BukovProfile();
		profile.stash().deposit(new RaidItem(
				"weapon",
				"firearm:needle_9",
				1,
				0.9f,
				850,
				false,
				false,
				1f));
		profile.stash().deposit(new RaidItem(
				"ammo",
				"ammo:ammo_9_standard",
				24,
				0.012f,
				12,
				false,
				false,
				1f));
		profile.loadout().select("weapon", profile.stash());
		profile.loadout().select("ammo", profile.stash());
		saves.saveProfile(profile);
		saves.failProfileSave = true;

		try {
			start(saves, "deployment-profile-failure");
			fail("deployment profile write failure must be visible");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("profile"));
		}
		assertTrue(saves.loadRaidCheckpoint() != null);
		assertTrue(saves.loadProfile().lastLoadoutDefinitions().isEmpty());

		saves.failProfileSave = false;
		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		assertEquals(2, resumed.profile().lastLoadoutDefinitions().size());
		assertEquals(
				"firearm:needle_9",
				resumed.profile().lastLoadoutDefinitions().get(0));
		assertEquals(
				"ammo:ammo_9_standard",
				resumed.profile().lastLoadoutDefinitions().get(1));
		assertEquals(
				resumed.profile().lastLoadoutDefinitions(),
				saves.loadProfile().lastLoadoutDefinitions());
	}

	@Test
	public void cancelExtractionResetsProgress() throws IOException {
		BukovRaidCoordinator raid = start(
				new InMemoryBukovSaveService(),
				"cancel");
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(2f, ExtractionState.Interaction.ACTIVE);

		raid.cancelExtraction();

		assertEquals(0f, raid.extraction("E01").progressSeconds(), 0f);
		assertEquals(null, raid.activeExtractionId());
	}

	private static BukovRaidCoordinator start(
			BukovSaveService saves,
			String raidId) throws IOException {
		return BukovRaidCoordinator.start(
				saves,
				17L,
				raidId,
				25f,
				Collections.singletonList(ExtractionState.basic()));
	}

	private static void completeExtraction(BukovRaidCoordinator raid) {
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);
		assertTrue(raid.extraction("E01").completed());
	}

	private static RaidItem item(String uid) {
		return new RaidItem(uid, "loot", 1, 0.5f, 100, true, false, 1f);
	}

	private static final class FailingSaveService implements BukovSaveService {

		private final BukovSaveService delegate = new InMemoryBukovSaveService();
		private boolean failProfileSave;
		private boolean failRaidDelete;

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
		public BukovRaidCheckpoint loadRaidCheckpoint() throws IOException {
			return delegate.loadRaidCheckpoint();
		}

		@Override
		public void saveRaidCheckpoint(BukovRaidCheckpoint checkpoint) throws IOException {
			delegate.saveRaidCheckpoint(checkpoint);
		}

		@Override
		public void deleteRaid() throws IOException {
			if (failRaidDelete) {
				throw new IOException("injected delete failure");
			}
			delegate.deleteRaid();
		}
	}
}
