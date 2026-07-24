package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the production persistence stack against a real temporary
 * filesystem. This complements {@link BukovSaveStressTest}, which intentionally
 * isolates the coordinator with an in-memory service.
 */
public class BukovDiskSaveStressTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void repeatedDiskRestartAndSettlementPreserveEconomicLedgers()
			throws IOException {
		int iterations = Integer.getInteger("bukov.save.iterations", 10);
		File directory = temporaryFolder.newFolder("bukov-disk-stress");
		TrackingFileStorage storage = new TrackingFileStorage(directory);
		BukovSaveService saves = service(storage);
		BukovProfile initial = new BukovProfile();
		long expectedCurrency = 50_000L;
		initial.setCurrency(expectedCurrency);
		saves.saveProfile(initial);

		long expectedQuantity = 0L;
		long expectedValue = 0L;
		Map<String, RaidItem> expectedItems = new LinkedHashMap<>();
		for (int iteration = 0; iteration < iterations; iteration++) {
			String raidId = "disk-stress-" + iteration;
			String itemUid = raidId + ":loot";
			int quantity = iteration % 5 + 1;
			int unitValue = 100 + iteration;
			RaidItem item = new RaidItem(
					itemUid,
					"loot:disk_stress",
					quantity,
					0.05f,
					unitValue,
					true,
					false,
					1f);
			LootTransaction loot = new LootTransaction(raidId, 100f);
			assertEquals(LootTransaction.PickupResult.ADDED, loot.pickup(item));

			BukovProfile beforeRaid = saves.loadProfile();
			assertEquals(expectedCurrency, beforeRaid.currency());
			saves.saveProfile(beforeRaid);

			RaidSession session = RaidSession.create(
					0x42554B4F5600L + iteration,
					raidId);
			BukovRaidCheckpoint checkpoint = new BukovRaidCheckpoint(
					session,
					loot,
					Collections.emptyList());
			saves.saveRaidCheckpoint(checkpoint);
			session.advance(0.25f + iteration);
			saves.saveRaidCheckpoint(checkpoint);
			assertSaveFamilyIsCommitted(directory, AtomicBukovSaveService.PROFILE_FILE);
			assertSaveFamilyIsCommitted(directory, AtomicBukovSaveService.RAID_FILE);

			// Recreate the service to prove both documents survive a process restart.
			saves = service(storage);
			BukovProfile restartedProfile = saves.loadProfile();
			BukovRaidCheckpoint restartedRaid = saves.loadRaidCheckpoint();
			assertNotNull(restartedRaid);
			assertEquals(raidId, restartedRaid.session().raidId);
			assertEquals(0.25f + iteration,
					restartedRaid.session().elapsedSeconds, 0.0001f);
			assertEquals(1, restartedRaid.loot().distinctItemCount());
			assertEquals(quantity, restartedRaid.loot().totalQuantity());
			assertEquals(item.totalValue(), restartedRaid.loot().totalValue());
			assertEquals(item, restartedRaid.loot().item(itemUid));

			long stashQuantityBefore = restartedProfile.stash().totalQuantity();
			long stashValueBefore = restartedProfile.stash().totalValue();
			int stashItemsBefore = restartedProfile.stash().distinctItemCount();
			RaidSettlement settlement = new RaidSettlement();
			RaidResult first = settlement.settle(
					restartedProfile,
					restartedRaid.loot(),
					RaidOutcome.SUCCESS);
			assertFalse(first.replayed());
			assertEquals(quantity, first.transferredQuantity());
			assertEquals(item.totalValue(), first.transferredValue());
			saves.saveProfile(restartedProfile);
			saves.deleteRaid();

			expectedQuantity += quantity;
			expectedValue += item.totalValue();
			RaidItem settledItem = item.withFoundInRaid(false);
			expectedItems.put(itemUid, settledItem);
			saves = service(storage);
			BukovProfile committed = saves.loadProfile();
			assertNull(saves.loadRaidCheckpoint());
			assertEquals(expectedCurrency, committed.currency());
			assertEquals(stashItemsBefore + 1,
					committed.stash().distinctItemCount());
			assertEquals(stashQuantityBefore + quantity,
					committed.stash().totalQuantity());
			assertEquals(stashValueBefore + item.totalValue(),
					committed.stash().totalValue());
			assertEquals(expectedQuantity, committed.stash().totalQuantity());
			assertEquals(expectedValue, committed.stash().totalValue());
			assertEquals(settledItem, committed.stash().item(itemUid));
			assertProfileLedger(committed, expectedCurrency, expectedItems);

			// Replaying the identical durable receipt must not grant the same UID
			// or quantity twice, even after another disk round trip.
			RaidResult replay = settlement.settle(
					committed,
					restartedRaid.loot(),
					RaidOutcome.SUCCESS);
			assertTrue(replay.replayed());
			saves.saveProfile(committed);
			saves = service(storage);
			BukovProfile afterReplay = saves.loadProfile();
			assertEquals(expectedCurrency, afterReplay.currency());
			assertEquals(stashItemsBefore + 1,
					afterReplay.stash().distinctItemCount());
			assertEquals(expectedQuantity, afterReplay.stash().totalQuantity());
			assertEquals(expectedValue, afterReplay.stash().totalValue());
			assertEquals(settledItem, afterReplay.stash().item(itemUid));
			assertProfileLedger(afterReplay, expectedCurrency, expectedItems);
			assertNoTemporaryFiles(directory);
		}

		assertTrue(storage.profilePrimaryPromotions >= 1 + iterations * 3);
		assertTrue(storage.raidPrimaryPromotions >= iterations * 2);
		assertTrue(storage.backupPromotions >= iterations * 3);
	}

	@Test
	public void corruptedDiskPrimaryFallsBackToLastCommittedBackup()
			throws IOException {
		File directory = temporaryFolder.newFolder("bukov-disk-corruption");
		BukovSaveService saves = service(new TrackingFileStorage(directory));

		BukovProfile firstProfile = new BukovProfile();
		firstProfile.setCurrency(100L);
		saves.saveProfile(firstProfile);
		BukovProfile secondProfile = new BukovProfile();
		secondProfile.setCurrency(200L);
		saves.saveProfile(secondProfile);

		RaidSession firstRaid = RaidSession.create(1L, "backup-raid");
		firstRaid.advance(1f);
		saves.saveRaid(firstRaid);
		RaidSession secondRaid = RaidSession.create(1L, "backup-raid");
		secondRaid.advance(2f);
		saves.saveRaid(secondRaid);
		assertSaveFamilyIsCommitted(directory, AtomicBukovSaveService.PROFILE_FILE);
		assertSaveFamilyIsCommitted(directory, AtomicBukovSaveService.RAID_FILE);

		Files.write(
				new File(directory, AtomicBukovSaveService.PROFILE_FILE).toPath(),
				new byte[]{1, 2, 3});
		Files.write(
				new File(directory, AtomicBukovSaveService.RAID_FILE).toPath(),
				new byte[]{4, 5, 6});

		BukovSaveService restarted = service(new TrackingFileStorage(directory));
		assertEquals(100L, restarted.loadProfile().currency());
		assertEquals(1f, restarted.loadRaid().elapsedSeconds, 0.0001f);
		assertNoTemporaryFiles(directory);
	}

	private static BukovSaveService service(TrackingFileStorage storage) {
		return new AtomicBukovSaveService(storage);
	}

	private static void assertSaveFamilyIsCommitted(File directory, String file) {
		assertTrue(new File(directory, file).isFile());
		assertTrue(new File(directory, file + ".bak").isFile());
		assertFalse(new File(directory, file + ".tmp").exists());
		assertFalse(new File(directory, file + ".bak.tmp").exists());
	}

	private static void assertNoTemporaryFiles(File directory) {
		assertFalse(new File(
				directory,
				AtomicBukovSaveService.PROFILE_FILE + ".tmp").exists());
		assertFalse(new File(
				directory,
				AtomicBukovSaveService.PROFILE_FILE + ".bak.tmp").exists());
		assertFalse(new File(
				directory,
				AtomicBukovSaveService.RAID_FILE + ".tmp").exists());
		assertFalse(new File(
				directory,
				AtomicBukovSaveService.RAID_FILE + ".bak.tmp").exists());
	}

	private static void assertProfileLedger(
			BukovProfile profile,
			long expectedCurrency,
			Map<String, RaidItem> expectedItems) {
		assertEquals(expectedCurrency, profile.currency());
		assertEquals(expectedItems.size(), profile.stash().distinctItemCount());
		for (Map.Entry<String, RaidItem> expected : expectedItems.entrySet()) {
			assertEquals(
					"physical item changed for UID " + expected.getKey(),
					expected.getValue(),
					profile.stash().item(expected.getKey()));
		}
	}

	/**
	 * Delegates every byte to the production file storage while recording which
	 * temporary files were promoted. It is deliberately not a storage fake.
	 */
	private static final class TrackingFileStorage implements BukovSaveStorage {

		private final FileBukovSaveStorage delegate;
		private int profilePrimaryPromotions;
		private int raidPrimaryPromotions;
		private int backupPromotions;

		private TrackingFileStorage(File directory) {
			delegate = new FileBukovSaveStorage(directory);
		}

		@Override
		public boolean exists(String name) throws IOException {
			return delegate.exists(name);
		}

		@Override
		public byte[] read(String name) throws IOException {
			return delegate.read(name);
		}

		@Override
		public void write(String name, byte[] data) throws IOException {
			delegate.write(name, data);
		}

		@Override
		public void replaceAtomically(String sourceName, String targetName)
				throws IOException {
			assertTrue(sourceName.endsWith(".tmp"));
			delegate.replaceAtomically(sourceName, targetName);
			if (AtomicBukovSaveService.PROFILE_FILE.equals(targetName)) {
				profilePrimaryPromotions++;
			} else if (AtomicBukovSaveService.RAID_FILE.equals(targetName)) {
				raidPrimaryPromotions++;
			} else if (targetName.endsWith(".bak")) {
				backupPromotions++;
			}
		}

		@Override
		public void delete(String name) throws IOException {
			delegate.delete(name);
		}
	}
}
