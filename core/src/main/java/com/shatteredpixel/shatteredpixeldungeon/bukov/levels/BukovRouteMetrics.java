/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import java.util.List;

public final class BukovRouteMetrics {

	private BukovRouteMetrics() {
	}

	public static float averageThreat(
			BukovRaidLayout layout, List<String> roomIds) {
		float total = 0f;
		for (String roomId : roomIds) {
			BukovRaidLayout.Mark mark = layout.mark(roomId);
			if (mark == null) throw new IllegalArgumentException("Unknown room " + roomId);
			total += threat(mark.zone);
		}
		return total / roomIds.size();
	}

	public static float threat(BukovRaidLayout.Zone zone) {
		switch (zone) {
			case LOW_LOOT:
			case MEDICAL:
			case SECRET:
				return 1f;
			case COMBAT:
				return 3f;
			case HAZARD:
				return 4f;
			case HIGH_VALUE:
				return 5f;
			case BOSS:
				return 8f;
			default:
				return 0f;
		}
	}
}
