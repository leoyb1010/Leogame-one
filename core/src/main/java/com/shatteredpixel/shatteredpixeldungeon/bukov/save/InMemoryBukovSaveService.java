package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;

import java.io.IOException;
/**
 * Lightweight service double for systems that need persistence without disk IO.
 */
public final class InMemoryBukovSaveService implements BukovSaveService {

	private final BukovSaveService delegate =
			new AtomicBukovSaveService(new InMemoryBukovSaveStorage());

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
