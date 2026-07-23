package com.shatteredpixel.shatteredpixeldungeon.bukov.util;

import java.util.Iterator;

/** RoboVM-compatible replacements for newer java.lang.String helpers. */
public final class BukovStrings {

	private BukovStrings() {
	}

	public static String join(String delimiter, Iterable<?> values) {
		if (delimiter == null || values == null) {
			throw new IllegalArgumentException("delimiter and values are required");
		}
		StringBuilder result = new StringBuilder();
		Iterator<?> iterator = values.iterator();
		while (iterator.hasNext()) {
			if (result.length() > 0) {
				result.append(delimiter);
			}
			result.append(String.valueOf(iterator.next()));
		}
		return result.toString();
	}
}
