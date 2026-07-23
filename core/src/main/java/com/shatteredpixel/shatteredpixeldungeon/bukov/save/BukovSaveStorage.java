package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import java.io.IOException;

/**
 * Small storage seam used by the file implementation and deterministic tests.
 */
public interface BukovSaveStorage {

	boolean exists(String name) throws IOException;

	byte[] read(String name) throws IOException;

	void write(String name, byte[] data) throws IOException;

	void replaceAtomically(String sourceName, String targetName) throws IOException;

	void delete(String name) throws IOException;
}
