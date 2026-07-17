package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopLaunchMessagesTest {
	@Test
	public void launcherDefaultsToChineseBeforeLibGDXInitialization() {
		assertEquals(Locale.SIMPLIFIED_CHINESE, DesktopLaunchMessages.currentLocale());
	}

	@Test
	public void launcherErrorsFollowSelectedLanguage() {
		String english = DesktopLaunchMessages.get(Locale.ENGLISH, "fatal_error_body");
		String chinese = DesktopLaunchMessages.get(Locale.SIMPLIFIED_CHINESE, "fatal_error_body");
		assertTrue(english.contains("unrecoverable error"));
		assertFalse(english.contains("错误"));
		assertTrue(chinese.contains("错误"));
	}
}
