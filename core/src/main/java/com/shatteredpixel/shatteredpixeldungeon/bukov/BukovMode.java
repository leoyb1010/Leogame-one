package com.shatteredpixel.shatteredpixeldungeon.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

/** Explicit mode boundary between the Bukov product and the preserved classic mode. */
public final class BukovMode {

	public static final int SAVE_SLOT = 100;
	private static boolean active;
	private static boolean hubRequested;
	private static boolean launchHubRequested = true;
	private static volatile BukovRaidMode deploymentRaidMode =
			BukovRaidMode.EXPEDITION;

	private BukovMode() {
	}

	public static boolean active() {
		return active;
	}

	public static void enter() {
		active = true;
	}

	public static void prepareRaidMode(BukovRaidMode raidMode) {
		if (raidMode == null) {
			throw new IllegalArgumentException("raidMode is required");
		}
		deploymentRaidMode = raidMode;
	}

	public static BukovRaidMode raidMode() {
		return deploymentRaidMode;
	}

	/**
	 * Recovers the product boundary from durable host state. Static mode state
	 * is intentionally not serialized, so a cold resume or legacy interlevel
	 * path must be able to re-establish it before HeroSprite is constructed.
	 */
	public static boolean ensureActiveForHostState() {
		if (GamesInProgress.curSlot == SAVE_SLOT
				|| Dungeon.level instanceof BukovLevel) {
			active = true;
		}
		return active;
	}

	public static void leave() {
		active = false;
		hubRequested = false;
		deploymentRaidMode = BukovRaidMode.EXPEDITION;
	}

	public static void requestHub() {
		active = true;
		hubRequested = true;
	}

	public static boolean consumeHubRequest() {
		boolean requested = hubRequested;
		hubRequested = false;
		return requested;
	}

	/**
	 * Opens the Bukov hub once after a cold application start. Classic mode
	 * remains available from the title screen, but is never resumed implicitly.
	 */
	public static boolean consumeLaunchHubRequest() {
		boolean requested = launchHubRequested;
		launchHubRequested = false;
		if (requested) {
			active = true;
		}
		return requested;
	}
}
