/*
 * Leo's Dungeon Siege desktop launcher messages.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;

import java.util.Locale;
import java.util.ResourceBundle;

final class DesktopLaunchMessages {

	private static final String BUNDLE =
			"com.shatteredpixel.shatteredpixeldungeon.desktop.launch";

	private DesktopLaunchMessages() {
	}

	static String get(String key) {
		return get(currentLocale(), key);
	}

	static String get(Locale locale, String key) {
		return ResourceBundle.getBundle(BUNDLE, locale).getString(key);
	}

	static Locale currentLocale() {
		// Launcher validation runs before libGDX creates Gdx.app. The product
		// default is Chinese, and touching SPDSettings here would recurse through
		// an unavailable Preferences backend and obscure the real launch error.
		if (Gdx.app == null) return Locale.SIMPLIFIED_CHINESE;
		try {
			return SPDSettings.language() == Languages.ENGLISH
					? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
		} catch (RuntimeException ignored) {
			return Locale.SIMPLIFIED_CHINESE;
		}
	}
}
