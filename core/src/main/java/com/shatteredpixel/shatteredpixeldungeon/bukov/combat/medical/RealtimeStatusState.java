package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Realtime operator health and injury state.
 *
 * The caller owns the fixed timestep. A recommended production value is
 * 1/60 second. This class never schedules Actor time and never creates Buffs.
 */
public final class RealtimeStatusState implements Bundlable {

	private static final String MAXIMUM_HEALTH = "maximum_health";
	private static final String HEALTH = "health";
	private static final String BLEEDING = "bleeding_per_second";
	private static final String FRACTURED = "fractured";
	private static final String PAIN = "pain_severity";
	private static final String PAIN_SUPPRESSION =
			"pain_suppression_remaining";
	private static final String CONCUSSION = "concussion_remaining";
	private static final String STIMULANT = "stimulant_remaining";

	private float maximumHealth;
	private float health;
	private float bleedingPerSecond;
	private boolean fractured;
	private float painSeverity;
	private float painSuppressionRemaining;
	private float concussionRemaining;
	private float stimulantRemaining;

	/** Required by the project bundle format. */
	public RealtimeStatusState() {
		this(1f, 1f);
	}

	public RealtimeStatusState(float maximumHealth, float health) {
		this.maximumHealth = finitePositive(maximumHealth, "maximumHealth");
		if (!BukovNumbers.isFinite(health)
				|| health < 0f
				|| health > maximumHealth) {
			throw new IllegalArgumentException(
					"health must be finite and between zero and maximumHealth");
		}
		this.health = health;
	}

	/**
	 * Advances bleeding and temporary states by one caller-owned fixed step.
	 */
	public void fixedStep(float deltaSeconds) {
		requireDelta(deltaSeconds);
		if (deltaSeconds == 0f) {
			return;
		}
		if (health > 0f && bleedingPerSecond > 0f) {
			health = Math.max(
					0f,
					health - bleedingPerSecond * deltaSeconds);
		}
		painSuppressionRemaining =
				decrement(painSuppressionRemaining, deltaSeconds);
		concussionRemaining = decrement(concussionRemaining, deltaSeconds);
		stimulantRemaining = decrement(stimulantRemaining, deltaSeconds);
	}

	public void applyDamage(float damage) {
		float amount = finiteNonNegative(damage, "damage");
		if (health <= 0f || amount == 0f) {
			return;
		}
		health = Math.max(0f, health - amount);
	}

	public void addBleeding(float damagePerSecond) {
		float amount = finiteNonNegative(damagePerSecond, "damagePerSecond");
		bleedingPerSecond = Math.min(25f, bleedingPerSecond + amount);
	}

	public void reduceBleeding(float amount) {
		float reduction = finiteNonNegative(amount, "amount");
		bleedingPerSecond = Math.max(0f, bleedingPerSecond - reduction);
	}

	public void setFractured(boolean fractured) {
		this.fractured = fractured;
		if (fractured) {
			painSeverity = Math.max(painSeverity, 0.55f);
		}
	}

	public void addPain(float severity) {
		float amount = finiteNonNegative(severity, "severity");
		painSeverity = clamp(painSeverity + amount, 0f, 1f);
	}

	public void addConcussion(float durationSeconds) {
		float duration =
				finiteNonNegative(durationSeconds, "durationSeconds");
		concussionRemaining =
				Math.max(concussionRemaining, duration);
	}

	void heal(float amount) {
		float healing = finiteNonNegative(amount, "amount");
		if (health <= 0f) {
			return;
		}
		health = Math.min(maximumHealth, health + healing);
	}

	void clearFracture() {
		fractured = false;
	}

	void suppressPain(float durationSeconds) {
		float duration =
				finiteNonNegative(durationSeconds, "durationSeconds");
		painSuppressionRemaining =
				Math.max(painSuppressionRemaining, duration);
	}

	void reduceConcussion(float durationSeconds) {
		float duration =
				finiteNonNegative(durationSeconds, "durationSeconds");
		concussionRemaining =
				Math.max(0f, concussionRemaining - duration);
	}

	void applyStimulant(float durationSeconds) {
		float duration =
				finiteNonNegative(durationSeconds, "durationSeconds");
		stimulantRemaining = Math.max(stimulantRemaining, duration);
	}

	public float maximumHealth() {
		return maximumHealth;
	}

	public float health() {
		return health;
	}

	public boolean isDead() {
		return health <= 0f;
	}

	public float bleedingPerSecond() {
		return bleedingPerSecond;
	}

	public boolean fractured() {
		return fractured;
	}

	public float painSeverity() {
		return painSeverity;
	}

	public float painSuppressionRemaining() {
		return painSuppressionRemaining;
	}

	public boolean painSuppressed() {
		return painSuppressionRemaining > 0f;
	}

	public float concussionRemaining() {
		return concussionRemaining;
	}

	public float stimulantRemaining() {
		return stimulantRemaining;
	}

	public float movementMultiplier() {
		float result = fractured ? 0.68f : 1f;
		if (stimulantRemaining > 0f) {
			result *= 1.12f;
		}
		return result;
	}

	public float aimMultiplier() {
		float result = 1f;
		if (fractured) {
			result *= 0.78f;
		}
		if (concussionRemaining > 0f) {
			result *= 0.72f;
		}
		if (!painSuppressed()) {
			result *= 1f - painSeverity * 0.28f;
		}
		return clamp(result, 0.35f, 1f);
	}

	private static float decrement(float value, float deltaSeconds) {
		return Math.max(0f, value - deltaSeconds);
	}

	private static void requireDelta(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
	}

	private static float finitePositive(float value, String name) {
		if (!BukovNumbers.isFinite(value) || value <= 0f) {
			throw new IllegalArgumentException(name + " must be finite and positive");
		}
		return value;
	}

	private static float finiteNonNegative(float value, String name) {
		if (!BukovNumbers.isFinite(value) || value < 0f) {
			throw new IllegalArgumentException(
					name + " must be finite and non-negative");
		}
		return value;
	}

	private static float clamp(float value, float minimum, float maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(MAXIMUM_HEALTH, maximumHealth);
		bundle.put(HEALTH, health);
		bundle.put(BLEEDING, bleedingPerSecond);
		bundle.put(FRACTURED, fractured);
		bundle.put(PAIN, painSeverity);
		bundle.put(PAIN_SUPPRESSION, painSuppressionRemaining);
		bundle.put(CONCUSSION, concussionRemaining);
		bundle.put(STIMULANT, stimulantRemaining);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		float restoredMaximum = finitePositive(
				bundle.getFloat(MAXIMUM_HEALTH), MAXIMUM_HEALTH);
		float restoredHealth = bundle.getFloat(HEALTH);
		if (!BukovNumbers.isFinite(restoredHealth)
				|| restoredHealth < 0f
				|| restoredHealth > restoredMaximum) {
			throw new IllegalStateException("Invalid restored realtime health");
		}
		maximumHealth = restoredMaximum;
		health = restoredHealth;
		bleedingPerSecond = restoredNonNegative(bundle, BLEEDING);
		if (bleedingPerSecond > 25f) {
			throw new IllegalStateException("Invalid restored bleeding");
		}
		fractured = bundle.getBoolean(FRACTURED);
		painSeverity = restoredNonNegative(bundle, PAIN);
		if (painSeverity > 1f) {
			throw new IllegalStateException("Invalid restored pain");
		}
		painSuppressionRemaining =
				restoredNonNegative(bundle, PAIN_SUPPRESSION);
		concussionRemaining = restoredNonNegative(bundle, CONCUSSION);
		stimulantRemaining = restoredNonNegative(bundle, STIMULANT);
	}

	private static float restoredNonNegative(Bundle bundle, String key) {
		float value = bundle.getFloat(key);
		if (!BukovNumbers.isFinite(value) || value < 0f) {
			throw new IllegalStateException(
					"Invalid restored realtime status: " + key);
		}
		return value;
	}
}
