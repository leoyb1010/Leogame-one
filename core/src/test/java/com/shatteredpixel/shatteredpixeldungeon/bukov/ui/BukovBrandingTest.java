package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovBrandingTest {

	@Test
	public void keepsClassicAndBukovMessageKeysSeparate() {
		assertEquals("title", BukovBranding.messageKey(false, "title"));
		assertEquals("bukov_title", BukovBranding.messageKey(true, "title"));
		assertEquals("start", BukovBranding.messageKey(false, "start"));
		assertEquals("bukov_start", BukovBranding.messageKey(true, "start"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingClassicKey() {
		BukovBranding.messageKey(true, "");
	}
}
