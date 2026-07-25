package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Test-process fault injector. Production sources never reference this class.
 */
final class HaltingBukovSaveStorage implements BukovSaveStorage {

	enum CrashPoint {
		TEMPORARY_WRITE,
		BACKUP_PROMOTION,
		PRIMARY_REPLACE,
		RAID_DELETE
	}

	static final int HALT_EXIT_CODE = 86;
	static final String TEST_ROOT_MARKER = ".bukov-crash-test-root";
	static final String CRASH_MARKER = "fault-injected.json";

	private final File directory;
	private final FileBukovSaveStorage delegate;
	private final CrashPoint crashPoint;
	private boolean triggered;

	HaltingBukovSaveStorage(File directory, CrashPoint crashPoint)
			throws IOException {
		if (directory == null || crashPoint == null) {
			throw new IllegalArgumentException(
					"directory and crash point are required");
		}
		if (!Boolean.getBoolean("bukov.crash.child")) {
			throw new IllegalStateException(
					"halting storage is restricted to the child process");
		}
		this.directory = directory.getCanonicalFile();
		if (!new File(this.directory, TEST_ROOT_MARKER).isFile()) {
			throw new IllegalArgumentException(
					"fault injection requires an isolated marked directory");
		}
		this.crashPoint = crashPoint;
		delegate = new FileBukovSaveStorage(this.directory);
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
		if (crashPoint == CrashPoint.TEMPORARY_WRITE
				&& (AtomicBukovSaveService.PROFILE_FILE + ".tmp")
						.equals(name)) {
			halt("write", name, null);
		}
	}

	@Override
	public void replaceAtomically(String sourceName, String targetName)
			throws IOException {
		delegate.replaceAtomically(sourceName, targetName);
		if (crashPoint == CrashPoint.BACKUP_PROMOTION
				&& (AtomicBukovSaveService.PROFILE_FILE + ".bak")
						.equals(targetName)) {
			halt("replace", sourceName, targetName);
		}
		if (crashPoint == CrashPoint.PRIMARY_REPLACE
				&& AtomicBukovSaveService.PROFILE_FILE.equals(targetName)) {
			halt("replace", sourceName, targetName);
		}
	}

	@Override
	public void delete(String name) throws IOException {
		delegate.delete(name);
		if (crashPoint == CrashPoint.RAID_DELETE
				&& AtomicBukovSaveService.RAID_FILE.equals(name)) {
			halt("delete", name, null);
		}
	}

	private void halt(String operation, String source, String target)
			throws IOException {
		if (triggered) return;
		triggered = true;
		String json = "{\n"
				+ "  \"status\": \"injected\",\n"
				+ "  \"crashPoint\": \"" + crashPoint.name() + "\",\n"
				+ "  \"operation\": \"" + operation + "\",\n"
				+ "  \"source\": \"" + source + "\",\n"
				+ "  \"target\": "
				+ (target == null ? "null" : "\"" + target + "\"")
				+ ",\n"
				+ "  \"exitCode\": " + HALT_EXIT_CODE + "\n"
				+ "}\n";
		File marker = new File(directory, CRASH_MARKER);
		try (FileOutputStream output =
				new FileOutputStream(marker, false)) {
			output.write(json.getBytes(StandardCharsets.UTF_8));
			output.flush();
			output.getFD().sync();
		}
		Runtime.getRuntime().halt(HALT_EXIT_CODE);
		throw new AssertionError("Runtime.halt unexpectedly returned");
	}
}
