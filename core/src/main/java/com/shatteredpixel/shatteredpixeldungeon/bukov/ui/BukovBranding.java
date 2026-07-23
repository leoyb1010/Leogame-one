/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/** Pure message-key selection so classic and Bukov copy cannot bleed together. */
public final class BukovBranding {

	private BukovBranding() {
	}

	public static String messageKey(boolean bukovMode, String classicKey) {
		if (classicKey == null || classicKey.isEmpty()) {
			throw new IllegalArgumentException("classicKey is required");
		}
		return bukovMode ? "bukov_" + classicKey : classicKey;
	}
}
