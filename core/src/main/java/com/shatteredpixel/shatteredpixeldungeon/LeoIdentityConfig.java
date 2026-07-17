/*
 * Leo's Dungeon Siege personal-edition identity configuration.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Central identity values so personal branding is not scattered through scenes. */
public final class LeoIdentityConfig {

	public static final String OWNER = "Leo";
	public static final int CHARCOAL = 0x111414;
	public static final int ANTIQUE_GOLD = 0xB58A45;
	public static final int EMERALD = 0x2E9B73;
	public static final int DANGER_RED = 0x8F3131;

	private LeoIdentityConfig() {
	}

	public static String gameTitle() {
		return Messages.get(LeoIdentityConfig.class, "game_title");
	}

	public static String motto() {
		return Messages.get(LeoIdentityConfig.class, "motto");
	}

	public static String archiveTitle() {
		return Messages.get(LeoIdentityConfig.class, "archive_title");
	}
}
