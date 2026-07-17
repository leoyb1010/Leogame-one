/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.ios;

import java.util.Map;

final class IOSRuntimeEnvironment {

	private IOSRuntimeEnvironment() {
	}

	static boolean isSimulator(Map<String, String> environment) {
		return environment.containsKey("SIMULATOR_DEVICE_NAME")
				|| environment.containsKey("SIMULATOR_UDID");
	}
}
