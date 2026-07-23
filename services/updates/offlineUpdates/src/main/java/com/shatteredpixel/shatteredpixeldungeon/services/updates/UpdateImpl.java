/*
 * Escape from Bukov offline update adapter.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.services.updates;

/** Production adapter: this personal edition never initializes a network update service. */
public final class UpdateImpl {

	private UpdateImpl() {
	}

	public static UpdateService getUpdateService() {
		return null;
	}

	public static boolean supportsUpdates() {
		return false;
	}
}
