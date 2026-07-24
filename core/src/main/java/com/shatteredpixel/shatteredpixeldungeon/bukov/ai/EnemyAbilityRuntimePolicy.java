package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

/**
 * Closed production contract for authored enemy ability tags.
 *
 * <p>Each content tag must name a runtime behavior family. Shared families
 * intentionally reuse the existing tactics, hearing, alarm, damage, armor,
 * and White Line state machines instead of introducing per-enemy scripts.</p>
 */
public final class EnemyAbilityRuntimePolicy {

	public enum Use {
		TACTICAL_MANEUVER,
		SOUND_INVESTIGATION,
		CONTACT_BROADCAST,
		REINFORCEMENT,
		WEAK_TARGET_PRESSURE,
		FRONTAL_ARMOR,
		WHITE_LINE_PHASE
	}

	private static final float WEAK_TARGET_HEALTH_FRACTION = 0.35f;
	private static final float WEAK_TARGET_DAMAGE_MULTIPLIER = 1.20f;
	private static final float INVESTIGATOR_HEARING_MULTIPLIER = 1.20f;

	public static Use useFor(String ability) {
		if (ability == null) return null;
		switch (ability) {
			case "PRESS_WEAK_TARGET":
				return Use.WEAK_TARGET_PRESSURE;
			case "INVESTIGATE_SOUND":
				return Use.SOUND_INVESTIGATION;
			case "BROADCAST_CONTACT":
			case "ORDER_FLANK":
				return Use.CONTACT_BROADCAST;
			case "CALL_INVESTIGATORS":
				return Use.REINFORCEMENT;
			case "ARMORED_FRONT":
				return Use.FRONTAL_ARMOR;
			case "UMBRELLA_SHIELD":
			case "DECOY_BODIES":
			case "FOG_LAMP_OVERLOAD":
				return Use.WHITE_LINE_PHASE;
			case "RETREAT_FROM_STRONG_TARGET":
			case "CORNER_AMBUSH":
			case "SHORT_DASH":
			case "USE_COVER":
			case "SHORT_SUPPRESSION":
			case "AVOID_DIRECT_FIRE":
			case "BURST_SUPPRESSION":
			case "TACTICAL_RETREAT":
			case "BREACH_CORNERS":
				return Use.TACTICAL_MANEUVER;
			default:
				return null;
		}
	}

	public static boolean hasAbility(
			EnemyArchetypeDefinition definition,
			String ability) {
		return definition != null
				&& hasAbility(definition.abilities, ability);
	}

	public static boolean hasAbility(
			String[] abilities,
			String ability) {
		if (abilities == null || ability == null) return false;
		for (String candidate : abilities) {
			if (ability.equals(candidate)) return true;
		}
		return false;
	}

	public static int damageAgainstTarget(
			EnemyArchetypeDefinition definition,
			int baselineDamage,
			int targetHealth,
			int targetMaximumHealth) {
		int safeDamage = Math.max(0, baselineDamage);
		if (safeDamage == 0
				|| targetMaximumHealth <= 0
				|| targetHealth > targetMaximumHealth
						* WEAK_TARGET_HEALTH_FRACTION
				|| !hasAbility(definition, "PRESS_WEAK_TARGET")) {
			return safeDamage;
		}
		return Math.max(
				safeDamage + 1,
				Math.round(safeDamage * WEAK_TARGET_DAMAGE_MULTIPLIER));
	}

	public static float hearingMultiplier(
			EnemyArchetypeDefinition definition) {
		return hasAbility(definition, "INVESTIGATE_SOUND")
				? INVESTIGATOR_HEARING_MULTIPLIER : 1f;
	}

	public static boolean hasWhiteLinePhaseKit(
			EnemyArchetypeDefinition definition) {
		return hasAbility(definition, "UMBRELLA_SHIELD")
				&& hasAbility(definition, "DECOY_BODIES")
				&& hasAbility(definition, "FOG_LAMP_OVERLOAD");
	}

	private EnemyAbilityRuntimePolicy() {
	}
}
