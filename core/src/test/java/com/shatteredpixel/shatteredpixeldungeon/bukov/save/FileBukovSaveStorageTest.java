package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileBukovSaveStorageTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void fileStoragePersistsAndRotatesBackup() throws IOException {
		Path directory = temporaryFolder.newFolder("bukov-save").toPath();
		BukovSaveService firstService = new FileBukovSaveService(directory.toFile());

		BukovProfile first = new BukovProfile();
		first.setCurrency(10L);
		firstService.saveProfile(first);

		BukovProfile second = new BukovProfile();
		second.setCurrency(20L);
		firstService.saveProfile(second);

		BukovSaveService restartedService = new FileBukovSaveService(directory.toFile());
		assertEquals(20L, restartedService.loadProfile().currency());
		assertTrue(directory.resolve(AtomicBukovSaveService.PROFILE_FILE).toFile().isFile());
		assertTrue(directory.resolve(AtomicBukovSaveService.PROFILE_FILE + ".bak").toFile().isFile());
	}
}
