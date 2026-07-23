package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;

import java.io.File;
import java.io.IOException;
/**
 * Production-facing file service. Callers depend on {@link BukovSaveService},
 * while platform launchers provide their writable save directory here.
 */
public final class FileBukovSaveService implements BukovSaveService {

	private final BukovSaveService delegate;

	public FileBukovSaveService(File directory) {
		delegate = new AtomicBukovSaveService(new FileBukovSaveStorage(directory));
	}

	@Override
	public BukovProfile loadProfile() throws IOException {
		return delegate.loadProfile();
	}

	@Override
	public void saveProfile(BukovProfile profile) throws IOException {
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
		delegate.deleteRaid();
	}
}
