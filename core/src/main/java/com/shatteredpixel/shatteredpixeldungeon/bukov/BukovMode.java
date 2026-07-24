package com.shatteredpixel.shatteredpixeldungeon.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovCareerProgression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Explicit mode boundary between the Bukov product and the preserved classic mode. */
public final class BukovMode {

	public static final int SAVE_SLOT = 100;
	private static boolean active;
	private static boolean hubRequested;
	private static boolean launchHubRequested = true;
	private static volatile BukovRaidMode deploymentRaidMode =
			BukovRaidMode.EXPEDITION;
	private static volatile List<String> deploymentMapIds =
			Collections.singletonList("fog_depot");
	private static volatile String deploymentSelectedMap = "fog_depot";

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

	public static void prepareUnlockedMaps(Collection<String> mapIds) {
		List<String> prepared = new ArrayList<>();
		if (mapIds != null) {
			for (String mapId : mapIds) {
				if (mapId != null
						&& BukovCareerProgression.allMapIds().contains(mapId)
						&& !prepared.contains(mapId)) {
					prepared.add(mapId);
				}
			}
		}
		if (prepared.isEmpty()) {
			prepared.add("fog_depot");
		}
		deploymentMapIds = Collections.unmodifiableList(prepared);
		if (!prepared.contains(deploymentSelectedMap)) {
			deploymentSelectedMap = prepared.get(0);
		}
	}

	public static void prepareSelectedMap(String mapId) {
		deploymentSelectedMap = deploymentMapIds.contains(mapId)
				? mapId : deploymentMapIds.get(0);
	}

	public static List<String> unlockedRaidThemes() {
		return deploymentMapIds;
	}

	public static String selectedRaidTheme() {
		return deploymentSelectedMap;
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
		deploymentMapIds = Collections.singletonList("fog_depot");
		deploymentSelectedMap = "fog_depot";
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
	 * Opens the Bukov hub once after a cold application start.
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
