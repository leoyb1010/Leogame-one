package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;

import java.io.IOException;
/**
 * Injectable persistence boundary for Bukov's long-lived profile and active raid.
 */
public interface BukovSaveService {

	BukovProfile loadProfile() throws IOException;

	void saveProfile(BukovProfile profile) throws IOException;

	/** Returns null when no active raid exists. */
	RaidSession loadRaid() throws IOException;

	void saveRaid(RaidSession raid) throws IOException;

	/** Returns null when no active raid checkpoint exists. */
	BukovRaidCheckpoint loadRaidCheckpoint() throws IOException;

	void saveRaidCheckpoint(BukovRaidCheckpoint checkpoint) throws IOException;

	void deleteRaid() throws IOException;
}
