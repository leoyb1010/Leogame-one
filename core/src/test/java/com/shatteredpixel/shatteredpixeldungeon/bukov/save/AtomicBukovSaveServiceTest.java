package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AtomicBukovSaveServiceTest {

	@Test
	public void profileAndRaidUseIndependentDocuments() throws IOException {
		InMemoryBukovSaveStorage storage = new InMemoryBukovSaveStorage();
		BukovSaveService saves = new AtomicBukovSaveService(storage);

		BukovProfile profile = new BukovProfile();
		profile.setCurrency(1250L);
		saves.saveProfile(profile);

		RaidSession raid = RaidSession.create(77L, "raid-77");
		raid.mapHash = "layout-77";
		raid.advance(12.5f);
		saves.saveRaid(raid);

		assertEquals(1250L, saves.loadProfile().currency());
		assertTrue(saves.loadRaid() != null);
		assertEquals("raid-77", saves.loadRaid().raidId);

		saves.deleteRaid();

		assertEquals(1250L, saves.loadProfile().currency());
		assertTrue(saves.loadRaid() == null);
	}

	@Test
	public void corruptedPrimaryFallsBackToLastGoodBackup() throws IOException {
		InMemoryBukovSaveStorage storage = new InMemoryBukovSaveStorage();
		BukovSaveService saves = new AtomicBukovSaveService(storage);

		BukovProfile first = new BukovProfile();
		first.setCurrency(100L);
		saves.saveProfile(first);

		BukovProfile second = new BukovProfile();
		second.setCurrency(200L);
		saves.saveProfile(second);
		storage.write(AtomicBukovSaveService.PROFILE_FILE, new byte[]{1, 2, 3});

		assertEquals(100L, saves.loadProfile().currency());
	}

	@Test
	public void corruptedPrimaryNeverReplacesGoodBackup() throws IOException {
		InMemoryBukovSaveStorage storage = new InMemoryBukovSaveStorage();
		BukovSaveService saves = new AtomicBukovSaveService(storage);

		BukovProfile first = new BukovProfile();
		first.setCurrency(100L);
		saves.saveProfile(first);

		BukovProfile second = new BukovProfile();
		second.setCurrency(200L);
		saves.saveProfile(second);

		storage.write(AtomicBukovSaveService.PROFILE_FILE, new byte[]{9, 9});
		BukovProfile third = new BukovProfile();
		third.setCurrency(300L);
		saves.saveProfile(third);

		assertEquals(300L, saves.loadProfile().currency());

		storage.write(AtomicBukovSaveService.PROFILE_FILE, new byte[]{8, 8});
		assertEquals(100L, saves.loadProfile().currency());
	}

	@Test(expected = IOException.class)
	public void invalidPrimaryWithoutBackupIsReportedInsteadOfReset() throws IOException {
		InMemoryBukovSaveStorage storage = new InMemoryBukovSaveStorage();
		storage.write(AtomicBukovSaveService.PROFILE_FILE, new byte[]{4, 5, 6});

		new AtomicBukovSaveService(storage).loadProfile();
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidRaidIsRejectedBeforeWriting() throws IOException {
		RaidSession raid = RaidSession.create(1L, "raid");
		raid.elapsedSeconds = Float.NaN;

		new AtomicBukovSaveService(new InMemoryBukovSaveStorage()).saveRaid(raid);
	}
}
