package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSession;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/**
 * Versioned Bukov persistence with independent profile/raid files.
 *
 * Saves are serialized and read back before the current file is touched. A
 * valid current file is promoted to a backup via its own temporary file, then
 * the new document atomically replaces the current file. An invalid current
 * file never replaces a valid backup.
 */
public final class AtomicBukovSaveService implements BukovSaveService {

	static final String PROFILE_FILE = "bukov_profile.dat";
	static final String RAID_FILE = "bukov_raid.dat";

	private static final int ENVELOPE_VERSION = 1;
	private static final String PROFILE_TYPE = "profile";
	private static final String RAID_TYPE = "raid";

	private static final String ENVELOPE_VERSION_KEY = "save_envelope_version";
	private static final String DOCUMENT_TYPE_KEY = "document_type";
	private static final String PAYLOAD_VERSION_KEY = "payload_version";
	private static final String PAYLOAD_KEY = "payload";

	private final BukovSaveStorage storage;

	public AtomicBukovSaveService(BukovSaveStorage storage) {
		if (storage == null) {
			throw new IllegalArgumentException("storage is required");
		}
		this.storage = storage;
	}

	@Override
	public BukovProfile loadProfile() throws IOException {
		BukovProfile profile = load(
				PROFILE_FILE,
				PROFILE_TYPE,
				BukovProfile.CURRENT_VERSION,
				BukovProfile.class);
		return profile == null ? new BukovProfile() : profile;
	}

	@Override
	public void saveProfile(BukovProfile profile) throws IOException {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		save(
				PROFILE_FILE,
				PROFILE_TYPE,
				BukovProfile.CURRENT_VERSION,
				profile,
				BukovProfile.class);
	}

	@Override
	public RaidSession loadRaid() throws IOException {
		BukovRaidCheckpoint checkpoint = loadRaidCheckpoint();
		return checkpoint == null ? null : checkpoint.session();
	}

	@Override
	public void saveRaid(RaidSession raid) throws IOException {
		validateRaid(raid);
		saveRaidCheckpoint(BukovRaidCheckpoint.sessionOnly(raid));
	}

	@Override
	public BukovRaidCheckpoint loadRaidCheckpoint() throws IOException {
		return load(
				RAID_FILE,
				RAID_TYPE,
				BukovRaidCheckpoint.CURRENT_VERSION,
				BukovRaidCheckpoint.class);
	}

	@Override
	public void saveRaidCheckpoint(BukovRaidCheckpoint checkpoint) throws IOException {
		validateCheckpoint(checkpoint);
		save(
				RAID_FILE,
				RAID_TYPE,
				BukovRaidCheckpoint.CURRENT_VERSION,
				checkpoint,
				BukovRaidCheckpoint.class);
	}

	@Override
	public void deleteRaid() throws IOException {
		deleteFamily(RAID_FILE);
	}

	private <T extends Bundlable> T load(
			String file,
			String type,
			int payloadVersion,
			Class<T> payloadClass) throws IOException {
		IOException primaryFailure = null;
		boolean primaryExists = storage.exists(file);
		if (primaryExists) {
			try {
				return decode(
						storage.read(file),
						type,
						payloadVersion,
						payloadClass);
			} catch (IOException e) {
				primaryFailure = e;
			}
		}

		String backup = backup(file);
		boolean backupExists = storage.exists(backup);
		if (backupExists) {
			try {
				return decode(
						storage.read(backup),
						type,
						payloadVersion,
						payloadClass);
			} catch (IOException backupFailure) {
				if (primaryFailure != null) {
					backupFailure.addSuppressed(primaryFailure);
				}
				throw new IOException("Both Bukov save copies are invalid: " + file, backupFailure);
			}
		}

		if (primaryFailure != null) {
			throw new IOException("Bukov save is invalid and has no backup: " + file, primaryFailure);
		}
		return null;
	}

	private <T extends Bundlable> void save(
			String file,
			String type,
			int payloadVersion,
			T payload,
			Class<T> payloadClass) throws IOException {
		byte[] encoded = encode(type, payloadVersion, payload);
		decode(encoded, type, payloadVersion, payloadClass);

		String temporary = temporary(file);
		String backup = backup(file);
		String backupTemporary = temporary(backup);
		try {
			storage.write(temporary, encoded);
			decode(storage.read(temporary), type, payloadVersion, payloadClass);

			if (storage.exists(file)) {
				byte[] current = storage.read(file);
				if (isValid(current, type, payloadVersion, payloadClass)) {
					storage.write(backupTemporary, current);
					decode(
							storage.read(backupTemporary),
							type,
							payloadVersion,
							payloadClass);
					storage.replaceAtomically(backupTemporary, backup);
				}
			}

			storage.replaceAtomically(temporary, file);
			// This also verifies the non-atomic platform fallback before the
			// operation is reported as successful; the previous valid backup
			// remains available when this check fails.
			decode(storage.read(file), type, payloadVersion, payloadClass);
		} finally {
			deleteQuietly(temporary);
			deleteQuietly(backupTemporary);
		}
	}

	private byte[] encode(String type, int payloadVersion, Bundlable payload) throws IOException {
		Bundle bundle = new Bundle();
		bundle.put(ENVELOPE_VERSION_KEY, ENVELOPE_VERSION);
		bundle.put(DOCUMENT_TYPE_KEY, type);
		bundle.put(PAYLOAD_VERSION_KEY, payloadVersion);
		bundle.put(PAYLOAD_KEY, payload);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (!Bundle.write(bundle, output, false)) {
			throw new IOException("Unable to encode Bukov " + type + " save");
		}
		return output.toByteArray();
	}

	private <T extends Bundlable> T decode(
			byte[] data,
			String expectedType,
			int expectedPayloadVersion,
			Class<T> payloadClass) throws IOException {
		if (data == null || data.length == 0) {
			throw new IOException("Empty Bukov save");
		}
		try {
			Bundle bundle = Bundle.read(new ByteArrayInputStream(data));
			if (bundle.getInt(ENVELOPE_VERSION_KEY) != ENVELOPE_VERSION) {
				throw new IOException("Unsupported Bukov save envelope");
			}
			if (!expectedType.equals(bundle.getString(DOCUMENT_TYPE_KEY))) {
				throw new IOException("Unexpected Bukov save document type");
			}
			int storedPayloadVersion = bundle.getInt(PAYLOAD_VERSION_KEY);
			boolean migratableRaid =
					RAID_TYPE.equals(expectedType)
							&& storedPayloadVersion >= 2
							&& storedPayloadVersion <= expectedPayloadVersion;
			boolean migratableProfile =
					PROFILE_TYPE.equals(expectedType)
							&& storedPayloadVersion >= 1
							&& storedPayloadVersion <= expectedPayloadVersion;
			if (storedPayloadVersion != expectedPayloadVersion
					&& !migratableRaid
					&& !migratableProfile) {
				throw new IOException("Unsupported Bukov " + expectedType + " save version");
			}
			Bundlable payload = bundle.get(PAYLOAD_KEY);
			if (!payloadClass.isInstance(payload)) {
				throw new IOException("Missing Bukov " + expectedType + " payload");
			}
			T result = payloadClass.cast(payload);
			if (result instanceof BukovProfile
					&& ((BukovProfile) result).profileVersion() != expectedPayloadVersion) {
				throw new IOException("Bukov profile version does not match its envelope");
			}
			if (result instanceof RaidSession) {
				validateRaid((RaidSession) result);
			}
			return result;
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new IOException("Invalid Bukov " + expectedType + " save", e);
		}
	}

	private <T extends Bundlable> boolean isValid(
			byte[] data,
			String type,
			int payloadVersion,
			Class<T> payloadClass) {
		try {
			decode(data, type, payloadVersion, payloadClass);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	private static void validateRaid(RaidSession raid) {
		if (raid == null) {
			throw new IllegalArgumentException("raid is required");
		}
		if (raid.raidId == null || raid.raidId.trim().isEmpty()) {
			throw new IllegalArgumentException("raidId is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				raid.elapsedSeconds) || raid.elapsedSeconds < 0f) {
			throw new IllegalArgumentException("elapsedSeconds must be finite and non-negative");
		}
	}

	private static void validateCheckpoint(BukovRaidCheckpoint checkpoint) {
		if (checkpoint == null) {
			throw new IllegalArgumentException("checkpoint is required");
		}
		if (checkpoint.version() != BukovRaidCheckpoint.CURRENT_VERSION) {
			throw new IllegalArgumentException("Unsupported raid checkpoint version");
		}
		validateRaid(checkpoint.session());
		if (checkpoint.loot() == null
				|| !checkpoint.session().raidId.equals(checkpoint.loot().raidId())) {
			throw new IllegalArgumentException("Checkpoint raid IDs must match");
		}
	}

	private void deleteFamily(String file) throws IOException {
		IOException failure = null;
		for (String name : new String[]{
				file,
				backup(file),
				temporary(file),
				temporary(backup(file))}) {
			try {
				storage.delete(name);
			} catch (IOException e) {
				if (failure == null) {
					failure = e;
				} else {
					failure.addSuppressed(e);
				}
			}
		}
		if (failure != null) {
			throw failure;
		}
	}

	private void deleteQuietly(String name) {
		try {
			storage.delete(name);
		} catch (IOException ignored) {
			// A stale temporary file is never considered during load.
		}
	}

	private static String backup(String file) {
		return file + ".bak";
	}

	private static String temporary(String file) {
		return file + ".tmp";
	}
}
