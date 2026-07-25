package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;

import java.io.File;

/**
 * Child-process entry point used only by {@link BukovProcessCrashRecoveryTest}.
 */
public final class BukovSaveCrashProcessHarness {

	private static final long NEW_CURRENCY = 200L;

	public static void main(String[] arguments) throws Exception {
		if (arguments.length != 3) {
			throw new IllegalArgumentException(
					"expected action, crash point, and isolated directory");
		}
		String action = arguments[0];
		HaltingBukovSaveStorage.CrashPoint crashPoint =
				HaltingBukovSaveStorage.CrashPoint.valueOf(arguments[1]);
		File directory = new File(arguments[2]).getCanonicalFile();
		BukovSaveService saves = new AtomicBukovSaveService(
				new HaltingBukovSaveStorage(directory, crashPoint));

		if ("profile-save".equals(action)) {
			BukovProfile profile = saves.loadProfile();
			profile.setCurrency(NEW_CURRENCY);
			saves.saveProfile(profile);
		} else if ("settlement-delete".equals(action)) {
			BukovProfile profile = saves.loadProfile();
			BukovRaidCheckpoint raid = saves.loadRaidCheckpoint();
			if (raid == null) {
				throw new IllegalStateException(
						"settlement crash case requires an active raid");
			}
			new RaidSettlement().settle(
					profile,
					raid.loot(),
					RaidOutcome.SUCCESS);
			saves.saveProfile(profile);
			saves.deleteRaid();
		} else {
			throw new IllegalArgumentException("unknown action: " + action);
		}

		throw new AssertionError(
				"fault injection did not halt at " + crashPoint);
	}

	private BukovSaveCrashProcessHarness() {
	}
}
