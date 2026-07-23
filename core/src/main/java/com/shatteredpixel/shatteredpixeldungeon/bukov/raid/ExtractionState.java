package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Pure-data extraction progress for the first level.
 *
 * Availability is derived from raid elapsed time and the condition flag.
 * UI may read progressFraction but cannot mutate the state directly.
 */
public final class ExtractionState implements Bundlable {

	public enum Type {
		BASIC,
		CONDITIONAL,
		TEMPORARY
	}

	public enum Interaction {
		NONE,
		ACTIVE,
		MOVED,
		RELOADED,
		LIGHT_HIT,
		HEAVY_HIT
	}

	private static final String EXTRACTION_ID = "extraction_id";
	private static final String TYPE = "type";
	private static final String INTERACTION_SECONDS = "interaction_seconds";
	private static final String OPENS_AT = "opens_at";
	private static final String CLOSES_AT = "closes_at";
	private static final String CONDITION_MET = "condition_met";
	private static final String PROGRESS = "progress";
	private static final String COMPLETED = "completed";

	public static final float TEMPORARY_EARLIEST_SECONDS = 8f * 60f;
	public static final float TEMPORARY_LATEST_SECONDS = 14f * 60f;
	public static final float TEMPORARY_WINDOW_SECONDS = 120f;

	private String extractionId;
	private Type type;
	private float interactionSeconds;
	private float opensAtSeconds;
	private float closesAtSeconds;
	private boolean conditionMet;
	private float progressSeconds;
	private boolean completed;

	public ExtractionState() {
		// Required by Bundle reflection.
	}

	public static ExtractionState basic() {
		return new ExtractionState("E01", Type.BASIC, 5f, 0f, Float.MAX_VALUE);
	}

	public static ExtractionState conditional() {
		return new ExtractionState("E02", Type.CONDITIONAL, 8f, 0f, Float.MAX_VALUE);
	}

	public static ExtractionState temporary(float opensAtSeconds) {
		if (opensAtSeconds < TEMPORARY_EARLIEST_SECONDS
				|| opensAtSeconds > TEMPORARY_LATEST_SECONDS) {
			throw new IllegalArgumentException(
					"Temporary extraction must open between minute 8 and minute 14");
		}
		return new ExtractionState(
				"E03",
				Type.TEMPORARY,
				5f,
				opensAtSeconds,
				opensAtSeconds + TEMPORARY_WINDOW_SECONDS);
	}

	public ExtractionState(
			String extractionId,
			Type type,
			float interactionSeconds,
			float opensAtSeconds,
			float closesAtSeconds) {
		if (extractionId == null || extractionId.trim().isEmpty()) {
			throw new IllegalArgumentException("extractionId is required");
		}
		if (type == null) {
			throw new IllegalArgumentException("type is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				interactionSeconds) || interactionSeconds <= 0f) {
			throw new IllegalArgumentException("interactionSeconds must be positive");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				opensAtSeconds) || opensAtSeconds < 0f) {
			throw new IllegalArgumentException("opensAtSeconds must be finite and non-negative");
		}
		if (Float.isNaN(closesAtSeconds)
				|| closesAtSeconds <= opensAtSeconds) {
			throw new IllegalArgumentException("closesAtSeconds must follow opensAtSeconds");
		}
		this.extractionId = extractionId;
		this.type = type;
		this.interactionSeconds = interactionSeconds;
		this.opensAtSeconds = opensAtSeconds;
		this.closesAtSeconds = closesAtSeconds;
		this.conditionMet = type != Type.CONDITIONAL;
	}

	public String extractionId() {
		return extractionId;
	}

	public Type type() {
		return type;
	}

	public float interactionSeconds() {
		return interactionSeconds;
	}

	public float opensAtSeconds() {
		return opensAtSeconds;
	}

	public float closesAtSeconds() {
		return closesAtSeconds;
	}

	public boolean conditionMet() {
		return conditionMet;
	}

	public void setConditionMet(boolean conditionMet) {
		this.conditionMet = conditionMet;
		if (!conditionMet && type == Type.CONDITIONAL) {
			progressSeconds = 0f;
		}
	}

	public float progressSeconds() {
		return progressSeconds;
	}

	public float progressFraction() {
		return progressSeconds / interactionSeconds;
	}

	public boolean completed() {
		return completed;
	}

	public boolean availableAt(float raidElapsedSeconds) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				raidElapsedSeconds) || raidElapsedSeconds < 0f) {
			return false;
		}
		if (type == Type.CONDITIONAL && !conditionMet) {
			return false;
		}
		return raidElapsedSeconds >= opensAtSeconds && raidElapsedSeconds < closesAtSeconds;
	}

	public void update(float raidElapsedSeconds, float deltaSeconds, Interaction interaction) {
		if (completed) return;
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
		}
		if (interaction == null) {
			throw new IllegalArgumentException("interaction is required");
		}
		if (!availableAt(raidElapsedSeconds)) {
			progressSeconds = 0f;
			return;
		}

		switch (interaction) {
			case ACTIVE:
				progressSeconds = Math.min(
						interactionSeconds,
						progressSeconds + deltaSeconds);
				if (progressSeconds >= interactionSeconds) {
					completed = true;
				}
				break;
			case LIGHT_HIT:
				progressSeconds = Math.max(
						0f,
						progressSeconds - interactionSeconds * 0.25f * deltaSeconds);
				break;
			case MOVED:
			case RELOADED:
			case HEAVY_HIT:
				progressSeconds = 0f;
				break;
			case NONE:
			default:
				break;
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(EXTRACTION_ID, extractionId);
		bundle.put(TYPE, type);
		bundle.put(INTERACTION_SECONDS, interactionSeconds);
		bundle.put(OPENS_AT, opensAtSeconds);
		bundle.put(CLOSES_AT, closesAtSeconds);
		bundle.put(CONDITION_MET, conditionMet);
		bundle.put(PROGRESS, progressSeconds);
		bundle.put(COMPLETED, completed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		ExtractionState restored = new ExtractionState(
				bundle.getString(EXTRACTION_ID),
				bundle.getEnum(TYPE, Type.class),
				bundle.getFloat(INTERACTION_SECONDS),
				bundle.getFloat(OPENS_AT),
				bundle.getFloat(CLOSES_AT));
		restored.conditionMet = bundle.getBoolean(CONDITION_MET);
		restored.progressSeconds = bundle.getFloat(PROGRESS);
		restored.completed = bundle.getBoolean(COMPLETED);
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				restored.progressSeconds)
				|| restored.progressSeconds < 0f
				|| restored.progressSeconds > restored.interactionSeconds) {
			throw new IllegalStateException("Invalid extraction progress");
		}
		if (restored.completed
				&& restored.progressSeconds < restored.interactionSeconds) {
			throw new IllegalStateException("Completed extraction lacks full progress");
		}

		extractionId = restored.extractionId;
		type = restored.type;
		interactionSeconds = restored.interactionSeconds;
		opensAtSeconds = restored.opensAtSeconds;
		closesAtSeconds = restored.closesAtSeconds;
		conditionMet = restored.conditionMet;
		progressSeconds = restored.progressSeconds;
		completed = restored.completed;
	}
}
