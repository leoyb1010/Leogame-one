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

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Set;

public final class ExtractionDefinition implements Bundlable {

	public enum Type {
		BASELINE,
		CONDITIONAL,
		TEMPORARY
	}

	public String id = "";
	public Type type = Type.BASELINE;
	public String roomId = "";
	public String requiredEvent = "";
	public int interactionCell = -1;
	public int interactionX = -1;
	public int interactionY = -1;
	public float interactionSeconds;
	public float rollbackFractionPerSecond = 0.25f;
	public float availableFromSeconds;
	public float availableUntilSeconds = Float.MAX_VALUE;
	public boolean keyRequired;
	public boolean bossKillRequired;

	public ExtractionDefinition() {
	}

	public static ExtractionDefinition baseline(String roomId) {
		ExtractionDefinition result = new ExtractionDefinition();
		result.id = "E01";
		result.type = Type.BASELINE;
		result.roomId = roomId;
		result.interactionSeconds = 5f;
		return result;
	}

	public static ExtractionDefinition conditional(String roomId) {
		ExtractionDefinition result = new ExtractionDefinition();
		result.id = "E02";
		result.type = Type.CONDITIONAL;
		result.roomId = roomId;
		result.requiredEvent = "pump_power";
		result.interactionSeconds = 8f;
		return result;
	}

	public static ExtractionDefinition temporary(String roomId, float availableFromSeconds) {
		ExtractionDefinition result = new ExtractionDefinition();
		result.id = "E03";
		result.type = Type.TEMPORARY;
		result.roomId = roomId;
		result.interactionSeconds = 5f;
		result.availableFromSeconds = availableFromSeconds;
		result.availableUntilSeconds = availableFromSeconds + 120f;
		return result;
	}

	public boolean isKeylessAndBossIndependent() {
		return !keyRequired && !bossKillRequired;
	}

	public boolean isAvailable(float elapsedSeconds, Set<String> completedEvents) {
		if (elapsedSeconds < availableFromSeconds || elapsedSeconds > availableUntilSeconds) {
			return false;
		}
		return requiredEvent.isEmpty() || completedEvents.contains(requiredEvent);
	}

	public void validate() {
		require(text(id), "extraction id is required");
		require(type != null, "extraction type is required: " + id);
		require(text(roomId), "extraction room is required: " + id);
		require(finitePositive(interactionSeconds),
				"interactionSeconds must be finite and positive: " + id);
		require(finite(rollbackFractionPerSecond)
						&& rollbackFractionPerSecond >= 0f
						&& rollbackFractionPerSecond <= 1f,
				"rollbackFractionPerSecond must be between 0 and 1: " + id);
		require(finite(availableFromSeconds) && availableFromSeconds >= 0f,
				"availableFromSeconds must be finite and non-negative: " + id);
		require(!Float.isNaN(availableUntilSeconds)
						&& availableUntilSeconds >= availableFromSeconds,
				"invalid availability window: " + id);
		if (type == Type.CONDITIONAL) {
			require(text(requiredEvent),
					"conditional extraction requires an event: " + id);
		}
		if (type == Type.TEMPORARY) {
			require(availableUntilSeconds < Float.MAX_VALUE,
					"temporary extraction requires a closing time: " + id);
		}
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static boolean finite(float value) {
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(value);
	}

	private static boolean finitePositive(float value) {
		return finite(value) && value > 0f;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("id", id);
		bundle.put("type", type);
		bundle.put("room_id", roomId);
		bundle.put("required_event", requiredEvent);
		bundle.put("interaction_cell", interactionCell);
		bundle.put("interaction_x", interactionX);
		bundle.put("interaction_y", interactionY);
		bundle.put("interaction_seconds", interactionSeconds);
		bundle.put("rollback_fraction_per_second", rollbackFractionPerSecond);
		bundle.put("available_from_seconds", availableFromSeconds);
		bundle.put("available_until_seconds", availableUntilSeconds);
		bundle.put("key_required", keyRequired);
		bundle.put("boss_kill_required", bossKillRequired);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		type = bundle.getEnum("type", Type.class);
		roomId = bundle.getString("room_id");
		requiredEvent = bundle.getString("required_event");
		interactionCell = bundle.contains("interaction_cell")
				? bundle.getInt("interaction_cell") : -1;
		interactionX = bundle.contains("interaction_x")
				? bundle.getInt("interaction_x") : -1;
		interactionY = bundle.contains("interaction_y")
				? bundle.getInt("interaction_y") : -1;
		interactionSeconds = bundle.getFloat("interaction_seconds");
		rollbackFractionPerSecond = bundle.getFloat("rollback_fraction_per_second");
		availableFromSeconds = bundle.getFloat("available_from_seconds");
		availableUntilSeconds = bundle.getFloat("available_until_seconds");
		keyRequired = bundle.getBoolean("key_required");
		bossKillRequired = bundle.getBoolean("boss_kill_required");
	}
}
