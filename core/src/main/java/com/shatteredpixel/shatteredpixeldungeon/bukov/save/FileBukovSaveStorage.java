package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Directory-backed storage using the Java I/O subset available on RoboVM.
 * Temporary and target files always share a directory, so rename remains the
 * preferred atomic replacement operation on desktop and iOS filesystems.
 */
public final class FileBukovSaveStorage implements BukovSaveStorage {

	private final File directory;

	public FileBukovSaveStorage(File directory) {
		if (directory == null) {
			throw new IllegalArgumentException("directory is required");
		}
		try {
			this.directory = directory.getCanonicalFile();
		} catch (IOException error) {
			throw new IllegalArgumentException("invalid save directory", error);
		}
	}

	@Override
	public boolean exists(String name) throws IOException {
		File file = resolve(name);
		return file.isFile() && file.length() > 0L;
	}

	@Override
	public byte[] read(String name) throws IOException {
		File file = resolve(name);
		if (file.length() > Integer.MAX_VALUE) {
			throw new IOException("Bukov save is too large: " + name);
		}
		try (FileInputStream input = new FileInputStream(file);
				ByteArrayOutputStream output =
						new ByteArrayOutputStream((int)Math.max(32L, file.length()))) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	@Override
	public void write(String name, byte[] data) throws IOException {
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("save data is required");
		}
		ensureDirectory();
		try (FileOutputStream output = new FileOutputStream(resolve(name), false)) {
			output.write(data);
			output.flush();
			output.getFD().sync();
		}
	}

	@Override
	public void replaceAtomically(String sourceName, String targetName) throws IOException {
		File source = resolve(sourceName);
		File target = resolve(targetName);
		if (!source.isFile()) {
			throw new IOException("Missing Bukov temporary save: " + sourceName);
		}
		if (source.renameTo(target)) {
			return;
		}

		// Some Java runtimes refuse rename-over-existing even on a filesystem
		// that supports it. The service keeps a decoded backup, so a synced
		// copy is the safe compatibility fallback.
		copyAndSync(source, target);
		if (!source.delete() && source.exists()) {
			throw new IOException("Unable to remove Bukov temporary save: " + sourceName);
		}
	}

	@Override
	public void delete(String name) throws IOException {
		File file = resolve(name);
		if (file.exists() && !file.delete()) {
			throw new IOException("Unable to delete Bukov save: " + name);
		}
	}

	private void ensureDirectory() throws IOException {
		if (directory.isDirectory()) {
			return;
		}
		if (!directory.mkdirs() && !directory.isDirectory()) {
			throw new IOException("Unable to create Bukov save directory");
		}
	}

	private File resolve(String name) throws IOException {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("file name is required");
		}
		File result = new File(directory, name).getCanonicalFile();
		if (!directory.equals(result.getParentFile())) {
			throw new IllegalArgumentException("file must remain inside the save directory");
		}
		return result;
	}

	private static void copyAndSync(File source, File target) throws IOException {
		try (FileInputStream input = new FileInputStream(source);
				FileOutputStream output = new FileOutputStream(target, false)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			output.flush();
			output.getFD().sync();
		}
	}
}
