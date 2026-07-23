package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.watabou.utils.FileUtils;

/**
 * Platform storage binding. Desktop and iOS launchers already configure
 * FileUtils to their writable application-data directory.
 */
public final class BukovSaveServices {

	private static final String BUKOV_DIRECTORY = "bukov";

	private BukovSaveServices() {
	}

	public static BukovSaveService platformDefault() {
		return new FileBukovSaveService(
				FileUtils.getFileHandle(BUKOV_DIRECTORY).file());
	}
}
