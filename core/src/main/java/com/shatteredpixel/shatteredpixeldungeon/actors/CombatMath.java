/*
 * Leo's Dungeon Siege combat regression helpers.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors;

/** Pure combat math used by the hit path and its regression tests. */
public final class CombatMath {

	private CombatMath() {
	}

	public static boolean rollHits(float accuracyRoll, float defenseRoll) {
		return accuracyRoll >= defenseRoll;
	}

	public static float baseHitChance(float accuracy, float defense) {
		if (defense <= 0f) return 1f;
		if (accuracy <= 0f) return 0f;
		if (accuracy >= defense) return 1f - defense / (2f * accuracy);
		return accuracy / (2f * defense);
	}
}
