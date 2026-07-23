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

/** Pure formatting helpers kept independent from Noosa for small unit tests. */
public final class BukovHudFormat {

	public static final String DEFAULT_OBJECTIVE = "搜集物资并前往撤离点";
	public static final String TOUCH_OBJECTIVE =
			"左拖移动 · 右拖射击 · 左上互动 · 右上装填";

	private BukovHudFormat() {
	}

	public static String health(int current, int maximum, int shield) {
		int safeMaximum = Math.max(1, maximum);
		int safeCurrent = clamp(current, 0, safeMaximum);
		int safeShield = Math.max(0, shield);
		return safeShield > 0
				? "HP " + safeCurrent + "/" + safeMaximum + " +" + safeShield
				: "HP " + safeCurrent + "/" + safeMaximum;
	}

	public static String armor(Integer minimumReduction, Integer maximumReduction) {
		if (minimumReduction == null || maximumReduction == null) return "护甲 --";
		int minimum = Math.max(0, minimumReduction);
		int maximum = Math.max(minimum, maximumReduction);
		return minimum == maximum ? "护甲 " + maximum : "护甲 " + minimum + "-" + maximum;
	}

	public static String ammo(Integer magazine, Integer reserve) {
		if (magazine == null) return "弹药 -- / --";
		return "弹药 " + Math.max(0, magazine) + " / " + Math.max(0, reserve == null ? 0 : reserve);
	}

	public static String tacticalAmmo(
			String weaponName,
			int magazine,
			int magazineCapacity,
			int reserve) {
		if (weaponName == null || weaponName.trim().isEmpty()
				|| magazineCapacity <= 0) {
			return "-- | --";
		}
		return Math.max(0, Math.min(magazine, magazineCapacity))
				+ " | " + Math.max(0, reserve);
	}

	public static String weapon(String name, boolean automatic) {
		if (name == null || name.trim().isEmpty()) return "未装备枪械";
		return name.trim() + " · " + (automatic ? "自动" : "单发");
	}

	public static String reload(boolean reloading, float progress) {
		if (!reloading) return "";
		return "换弹 " + percent(progress);
	}

	public static String status(
			float bleedingPerSecond,
			boolean fractured,
			float painSeverity,
			float concussionRemaining,
			float stimulantRemaining) {
		StringBuilder result = new StringBuilder();
		if (bleedingPerSecond > 0.001f) {
			append(result, "流血 " + oneDecimal(bleedingPerSecond) + "/秒");
		}
		if (fractured) append(result, "骨折");
		if (concussionRemaining > 0.001f) {
			append(result, "震荡 " + oneDecimal(concussionRemaining) + "秒");
		}
		if (painSeverity > 0.001f) append(result, "疼痛");
		if (stimulantRemaining > 0.001f) {
			append(result, "强化 " + oneDecimal(stimulantRemaining) + "秒");
		}
		return result.length() == 0 ? "状态稳定" : result.toString();
	}

	public static String interaction(
			BukovRaidHudState.Interaction type,
			String label,
			float progress,
			float seconds) {
		if (type == null || type == BukovRaidHudState.Interaction.NONE) {
			return "";
		}
		String action = label == null || label.trim().isEmpty()
				? interactionVerb(type) : label.trim();
		if (progress > 0f) {
			return action + " " + percent(progress);
		}
		if (seconds > 0f
				&& (type == BukovRaidHudState.Interaction.SEARCH
				|| type == BukovRaidHudState.Interaction.EXTRACT
				|| type == BukovRaidHudState.Interaction.MEDICAL)) {
			return "按住互动 · " + action + " " + oneDecimal(seconds) + "秒";
		}
		return type == BukovRaidHudState.Interaction.LOCKED
				? action : "按互动 · " + action;
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
			return "撤离 " + id(extractionId) + " · " + oneDecimal(remaining) + "秒";
		}
		if (extractionId != null) {
			return "撤离 " + id(extractionId) + (available ? " · 可用" : " · 未开放");
		}
		return "撤离点 " + Math.max(0, availableCount) + " 可用";
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
				return "搜索容器";
			case PICKUP:
				return "拾取物资";
			case EXTRACT:
				return "开始撤离";
			case PUMP:
				return "启动泵站";
			case MEDICAL:
				return "治疗";
			case LOCKED:
				return "目标未开放";
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
