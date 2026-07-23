package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Immutable realtime medical-item tuning.
 *
 * All durations are wall-clock seconds. None of these values are expressed in
 * Actor turns, so the runtime can advance them from a fixed-step loop.
 */
public final class MedicalDefinition {

	public final String id;
	public final float healAmount;
	public final float bleedingReduction;
	public final boolean clearsFracture;
	public final float painSuppressionSeconds;
	public final float concussionReductionSeconds;
	public final float stimulantSeconds;
	public final float useSeconds;
	public final float cooldownSeconds;
	public final boolean interruptOnMove;
	public final boolean interruptOnDamage;
	public final boolean interruptOnShot;

	public MedicalDefinition(
			String id,
			float healAmount,
			float bleedingReduction,
			boolean clearsFracture,
			float painSuppressionSeconds,
			float concussionReductionSeconds,
			float stimulantSeconds,
			float useSeconds,
			float cooldownSeconds,
			boolean interruptOnMove,
			boolean interruptOnDamage,
			boolean interruptOnShot) {
		this.id = requireId(id);
		this.healAmount = finiteNonNegative(healAmount, "healAmount");
		this.bleedingReduction =
				finiteNonNegative(bleedingReduction, "bleedingReduction");
		this.clearsFracture = clearsFracture;
		this.painSuppressionSeconds =
				finiteNonNegative(painSuppressionSeconds, "painSuppressionSeconds");
		this.concussionReductionSeconds =
				finiteNonNegative(concussionReductionSeconds, "concussionReductionSeconds");
		this.stimulantSeconds =
				finiteNonNegative(stimulantSeconds, "stimulantSeconds");
		this.useSeconds = finitePositive(useSeconds, "useSeconds");
		this.cooldownSeconds =
				finiteNonNegative(cooldownSeconds, "cooldownSeconds");
		this.interruptOnMove = interruptOnMove;
		this.interruptOnDamage = interruptOnDamage;
		this.interruptOnShot = interruptOnShot;
		if (healAmount == 0f
				&& bleedingReduction == 0f
				&& !clearsFracture
				&& painSuppressionSeconds == 0f
				&& concussionReductionSeconds == 0f
				&& stimulantSeconds == 0f) {
			throw new IllegalArgumentException("medical item must have an effect");
		}
	}

	private static String requireId(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("id is required");
		}
		return value;
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
}
