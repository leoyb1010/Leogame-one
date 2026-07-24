/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

/** Pure formatting helpers kept independent from Noosa for small unit tests. */
public final class BukovHudFormat {

	public static final String DEFAULT_OBJECTIVE =
			BukovMessages.get("bukov.raid.hud.default_objective");
	public static final String TOUCH_OBJECTIVE =
			BukovMessages.get("bukov.raid.hud.touch_objective");

	private BukovHudFormat() {
	}

	public static String health(int current, int maximum, int shield) {
		int safeMaximum = Math.max(1, maximum);
		int safeCurrent = clamp(current, 0, safeMaximum);
		int safeShield = Math.max(0, shield);
		return safeShield > 0
				? BukovMessages.get(
						"bukov.raid.hud.health_with_shield_format",
						safeCurrent,
						safeMaximum,
						safeShield)
				: BukovMessages.get(
						"bukov.raid.hud.health_format",
						safeCurrent,
						safeMaximum);
	}

	public static String armor(Integer minimumReduction, Integer maximumReduction) {
		if (minimumReduction == null || maximumReduction == null) {
			return BukovMessages.get("bukov.raid.hud.armor_unavailable");
		}
		int minimum = Math.max(0, minimumReduction);
		int maximum = Math.max(minimum, maximumReduction);
		return minimum == maximum
				? BukovMessages.get(
						"bukov.raid.hud.armor_value_format",
						maximum)
				: BukovMessages.get(
						"bukov.raid.hud.armor_range_format",
						minimum,
						maximum);
	}

	public static String ammo(Integer magazine, Integer reserve) {
		if (magazine == null) {
			return BukovMessages.get("bukov.raid.hud.ammo_unavailable");
		}
		return BukovMessages.get(
				"bukov.raid.hud.ammo_format",
				Math.max(0, magazine),
				Math.max(0, reserve == null ? 0 : reserve));
	}

	public static String tacticalAmmo(
			String weaponName,
			int magazine,
			int magazineCapacity,
			int reserve) {
		if (weaponName == null || weaponName.trim().isEmpty()
				|| magazineCapacity <= 0) {
			return BukovMessages.get(
					"bukov.raid.hud.tactical_ammo_unavailable");
		}
		return BukovMessages.get(
				"bukov.raid.hud.tactical_ammo_format",
				Math.max(0, Math.min(magazine, magazineCapacity)),
				Math.max(0, reserve));
	}

	public static String weapon(String name, boolean automatic) {
		if (name == null || name.trim().isEmpty()) {
			return BukovMessages.get("bukov.raid.hud.weapon_unarmed");
		}
		return BukovMessages.get(
				"bukov.raid.hud.weapon_format",
				name.trim(),
				automatic
						? BukovMessages.get("bukov.raid.hud.weapon_mode_auto")
						: BukovMessages.get("bukov.raid.hud.weapon_mode_single"));
	}

	public static String reload(boolean reloading, float progress) {
		if (!reloading) return "";
		return BukovMessages.get(
				"bukov.raid.hud.reload_format",
				percent(progress));
	}

	/**
	 * Compact timer copy paired with the injury icons. Bleeding and fractures
	 * persist until treated in the realtime medical model, so an infinity
	 * marker is more truthful than inventing a countdown for them.
	 */
	public static String injuryRemaining(
			boolean active, float remainingSeconds) {
		if (!active) return "";
		if (remainingSeconds <= 0f
				|| Float.isNaN(remainingSeconds)
				|| Float.isInfinite(remainingSeconds)) {
			return "∞";
		}
		return BukovMessages.get(
				"bukov.raid.hud.injury_seconds_format",
				Math.max(1, (int)Math.ceil(remainingSeconds)));
	}

	public static String status(
			float bleedingPerSecond,
			boolean fractured,
			float painSeverity,
			float concussionRemaining,
			float stimulantRemaining) {
		StringBuilder result = new StringBuilder();
		if (bleedingPerSecond > 0.001f) {
			append(result, BukovMessages.get(
					"bukov.raid.hud.status_bleeding_format",
					oneDecimal(bleedingPerSecond)));
		}
		if (fractured) {
			append(result, BukovMessages.get(
					"bukov.raid.hud.status_fractured"));
		}
		if (concussionRemaining > 0.001f) {
			append(result, BukovMessages.get(
					"bukov.raid.hud.status_concussion_format",
					oneDecimal(concussionRemaining)));
		}
		if (painSeverity > 0.001f) {
			append(result, BukovMessages.get(
					"bukov.raid.hud.status_pain"));
		}
		if (stimulantRemaining > 0.001f) {
			append(result, BukovMessages.get(
					"bukov.raid.hud.status_stimulant_format",
					oneDecimal(stimulantRemaining)));
		}
		return result.length() == 0
				? BukovMessages.get("bukov.raid.hud.status_stable")
				: result.toString();
	}

	public static String interaction(
			BukovRaidHudState.Interaction type,
			String label,
			float progress,
			float seconds) {
		return interaction(type, label, progress, seconds, false);
	}

	public static String interaction(
			BukovRaidHudState.Interaction type,
			String label,
			float progress,
			float seconds,
			boolean desktop) {
		if (type == null || type == BukovRaidHudState.Interaction.NONE) {
			return "";
		}
		String action = label == null || label.trim().isEmpty()
				? interactionVerb(type) : label.trim();
		if (progress > 0f) {
			return BukovMessages.get(
					"bukov.raid.hud.interaction_progress_format",
					action,
					percent(progress));
		}
		if (seconds > 0f
				&& (type == BukovRaidHudState.Interaction.SEARCH
				|| type == BukovRaidHudState.Interaction.EXTRACT
				|| type == BukovRaidHudState.Interaction.MEDICAL)) {
			return BukovMessages.get(
					desktop
							? "bukov.raid.hud.interaction_hold_desktop_format"
							: "bukov.raid.hud.interaction_hold_touch_format",
					action,
					oneDecimal(seconds));
		}
		if (type == BukovRaidHudState.Interaction.PICKUP) {
			return BukovMessages.get(
					desktop
							? "bukov.raid.hud.interaction_pickup_desktop_format"
							: "bukov.raid.hud.interaction_pickup_touch_format",
					action);
		}
		return type == BukovRaidHudState.Interaction.LOCKED
				? BukovMessages.get(
						"bukov.raid.hud.interaction_locked_format",
						action)
				: BukovMessages.get(
						desktop
								? "bukov.raid.hud.interaction_prompt_desktop_format"
								: "bukov.raid.hud.interaction_prompt_touch_format",
						action);
	}

	public static String extraction(
			int availableCount,
			String extractionId,
			boolean available,
			boolean active,
			float progress,
			float seconds) {
		if (active) {
			float remaining = Math.max(0f, seconds * (1f - clamp01(progress)));
			return BukovMessages.get(
					"bukov.raid.hud.extraction_active_format",
					id(extractionId),
					oneDecimal(remaining));
		}
		if (extractionId != null) {
			return BukovMessages.get(
					"bukov.raid.hud.extraction_state_format",
					id(extractionId),
					available
							? BukovMessages.get(
									"bukov.raid.hud.extraction_available")
							: BukovMessages.get(
									"bukov.raid.hud.extraction_unavailable"));
		}
		return BukovMessages.get(
				"bukov.raid.hud.extraction_count_format",
				Math.max(0, availableCount));
	}

	public static String clock(float elapsedSeconds) {
		int totalSeconds = Math.max(0, (int)Math.floor(elapsedSeconds));
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds / 60) % 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) {
			return twoDigits(hours) + ":" + twoDigits(minutes) + ":" + twoDigits(seconds);
		}
		return twoDigits(totalSeconds / 60) + ":" + twoDigits(seconds);
	}

	public static String objective(String value) {
		if (value == null || value.trim().isEmpty()) return DEFAULT_OBJECTIVE;
		return value.trim();
	}

	public static float healthFraction(int current, int maximum) {
		if (maximum <= 0) return 0f;
		return Math.max(0f, Math.min(1f, current / (float)maximum));
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static String twoDigits(int value) {
		return value < 10 ? "0" + value : Integer.toString(value);
	}

	private static String percent(float value) {
		return Math.round(clamp01(value) * 100f) + "%";
	}

	private static String oneDecimal(float value) {
		float safe = Math.max(0f, Float.isNaN(value) || Float.isInfinite(value)
				? 0f : value);
		return String.format(java.util.Locale.ROOT, "%.1f", safe);
	}

	private static void append(StringBuilder result, String value) {
		if (result.length() > 0) result.append(" · ");
		result.append(value);
	}

	private static String interactionVerb(BukovRaidHudState.Interaction type) {
		switch (type) {
			case SEARCH:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_search");
			case PICKUP:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_pickup");
			case EXTRACT:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_extract");
			case PUMP:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_pump");
			case MEDICAL:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_medical");
			case UNLOCK:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_unlock");
			case LOCKED:
				return BukovMessages.get(
						"bukov.raid.hud.interaction_unavailable");
			default:
				return "";
		}
	}

	private static String id(String extractionId) {
		return extractionId == null || extractionId.trim().isEmpty()
				? "--" : extractionId.trim();
	}

	private static float clamp01(float value) {
		if (Float.isNaN(value) || Float.isInfinite(value)) return 0f;
		return Math.max(0f, Math.min(1f, value));
	}
}
