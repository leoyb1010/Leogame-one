package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory storage double. Arrays are copied at the boundary so callers cannot
 * mutate persisted data by retaining a reference.
 */
public final class InMemoryBukovSaveStorage implements BukovSaveStorage {

	private final Map<String, byte[]> files = new LinkedHashMap<>();

	@Override
	public boolean exists(String name) {
		byte[] data = files.get(name);
		return data != null && data.length > 0;
	}

	@Override
	public byte[] read(String name) throws IOException {
		byte[] data = files.get(name);
		if (data == null) {
			throw new FileNotFoundException(name);
		}
		return data.clone();
	}

	@Override
	public void write(String name, byte[] data) {
		if (data == null || data.length == 0) {
			throw new IllegalArgumentException("save data is required");
		}
		files.put(name, data.clone());
	}

	@Override
	public void replaceAtomically(String sourceName, String targetName) throws IOException {
		byte[] data = files.remove(sourceName);
		if (data == null) {
			throw new FileNotFoundException(sourceName);
		}
		files.put(targetName, data);
	}

	@Override
	public void delete(String name) {
		files.remove(name);
	}
}
