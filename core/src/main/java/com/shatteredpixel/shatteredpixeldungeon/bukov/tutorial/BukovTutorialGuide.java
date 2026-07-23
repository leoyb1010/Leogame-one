package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;

import java.io.IOException;

/**
 * Claims persistent tutorial events. The coordinator saves the profile before
 * a hint becomes visible, which prevents resume/relaunch spam.
 */
public final class BukovTutorialGuide {

	public static final float DISPLAY_SECONDS = 4f;

	private final BukovRaidCoordinator raid;

	public BukovTutorialGuide(BukovRaidCoordinator raid) {
		if (raid == null) {
			throw new IllegalArgumentException("raid is required");
		}
		this.raid = raid;
	}

	public BukovTutorialEvent claim(BukovTutorialEvent event)
			throws IOException {
		if (event == null) {
			throw new IllegalArgumentException("event is required");
		}
		if (raid.session().raidOrdinal() > 2) {
			return null;
		}
		return raid.claimTutorial(event) ? event : null;
	}
}
