package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Allocation-free realtime maneuver planner for one enemy.
 *
 * <p>The planner deliberately runs tactical decisions at 8 Hz while still
 * emitting a movement intent every simulation step. This keeps a large fight
 * bounded without making movement look stepped or returning combat to the host
 * turn scheduler.</p>
 */
public final class RealtimeEnemyTactics {

	public enum Profile {
		STANDARD,
		SUPPRESSOR,
		FLANKER,
		RUSHER,
		RETREATING_SKIRMISHER
	}

	public enum Maneuver {
		FOLLOW_BRAIN,
		ANCHOR_AND_SUPPRESS,
		FLANK_LEFT,
		FLANK_RIGHT,
		DASH,
		RETREAT
	}

	public static final float DECISION_PERIOD_SECONDS = 0.125f;
	public static final float FLANK_SECONDS = 1.15f;
	public static final float FLANK_COOLDOWN_SECONDS = 2.75f;
	public static final float DASH_SECONDS = 0.65f;
	public static final float DASH_COOLDOWN_SECONDS = 2.2f;
	public static final float RETREAT_SECONDS = 0.9f;
	public static final float RETREAT_COOLDOWN_SECONDS = 2.5f;
	public static final float MAXIMUM_SPEED_MULTIPLIER = 1.6f;

	public static final class Intent {
		private Maneuver maneuver = Maneuver.FOLLOW_BRAIN;
		private float desiredX;
		private float desiredY;
		private float speedMultiplier = 1f;

		private void set(
				Maneuver maneuver,
				float desiredX,
				float desiredY,
				float speedMultiplier) {
			this.maneuver = maneuver;
			this.desiredX = desiredX;
			this.desiredY = desiredY;
			this.speedMultiplier = Math.max(
					0f,
					Math.min(MAXIMUM_SPEED_MULTIPLIER, speedMultiplier));
		}

		public Maneuver maneuver() {
			return maneuver;
		}

		public float desiredX() {
			return desiredX;
		}

		public float desiredY() {
			return desiredY;
		}

		public float speedMultiplier() {
			return speedMultiplier;
		}
	}

	private final Profile profile;
	private final int flankSide;
	private Maneuver maneuver = Maneuver.FOLLOW_BRAIN;
	private float decisionRemaining;
	private float maneuverRemaining;
	private float maneuverCooldown;
	private int decisionSequence;

	public RealtimeEnemyTactics(Profile profile, int stableKey) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		this.profile = profile;
		flankSide = (stableKey & 1) == 0 ? -1 : 1;
		int slot = BukovNumbers.floorMod(stableKey, 4);
		decisionRemaining = slot * (DECISION_PERIOD_SECONDS / 4f);
	}

	/**
	 * Advances one realtime maneuver. LOS is supplied by the world's grid
	 * perception; tactical movement never targets a player through a wall.
	 */
	public void step(
			float dt,
			boolean hasLineOfSight,
			float selfX,
			float selfY,
			float targetX,
			float targetY,
			float engagementRange,
			float brainDesiredX,
			float brainDesiredY,
			Intent out) {
		requireNonNegative(dt, "dt");
		requireNonNegative(engagementRange, "engagementRange");
		requireFinite(selfX, "selfX");
		requireFinite(selfY, "selfY");
		requireFinite(targetX, "targetX");
		requireFinite(targetY, "targetY");
		requireFinite(brainDesiredX, "brainDesiredX");
		requireFinite(brainDesiredY, "brainDesiredY");
		if (out == null) {
			throw new IllegalArgumentException("out is required");
		}

		maneuverRemaining = Math.max(0f, maneuverRemaining - dt);
		maneuverCooldown = Math.max(0f, maneuverCooldown - dt);
		decisionRemaining -= dt;

		if (!hasLineOfSight || profile == Profile.STANDARD) {
			maneuver = Maneuver.FOLLOW_BRAIN;
			maneuverRemaining = 0f;
			decisionRemaining = Math.max(0f, decisionRemaining);
			out.set(maneuver, brainDesiredX, brainDesiredY, 1f);
			return;
		}

		if (decisionRemaining <= 0f) {
			do {
				decisionRemaining += DECISION_PERIOD_SECONDS;
			} while (decisionRemaining <= 0f);
			decisionSequence++;
			chooseManeuver(
					selfX,
					selfY,
					targetX,
					targetY,
					engagementRange);
		}

		emitIntent(
				targetX - selfX,
				targetY - selfY,
				brainDesiredX,
				brainDesiredY,
				out);
	}

	private void chooseManeuver(
			float selfX,
			float selfY,
			float targetX,
			float targetY,
			float engagementRange) {
		float deltaX = targetX - selfX;
		float deltaY = targetY - selfY;
		float distanceSquared = deltaX * deltaX + deltaY * deltaY;
		float distance = (float)Math.sqrt(distanceSquared);
		float safeRange = Math.max(0.75f, engagementRange);

		if (maneuverRemaining > 0f) {
			return;
		}

		switch (profile) {
			case SUPPRESSOR:
				if (distance <= safeRange && distance >= 2.1f) {
					maneuver = Maneuver.ANCHOR_AND_SUPPRESS;
				} else {
					maneuver = Maneuver.FOLLOW_BRAIN;
				}
				break;
			case FLANKER:
				if (distance > 1.6f
						&& distance <= safeRange * 1.15f
						&& maneuverCooldown <= 0f) {
					maneuver = flankSide < 0
							? Maneuver.FLANK_LEFT : Maneuver.FLANK_RIGHT;
					maneuverRemaining = FLANK_SECONDS;
					maneuverCooldown = FLANK_COOLDOWN_SECONDS;
				} else if (distance <= safeRange) {
					maneuver = Maneuver.ANCHOR_AND_SUPPRESS;
				} else {
					maneuver = Maneuver.FOLLOW_BRAIN;
				}
				break;
			case RUSHER:
				if (distance > safeRange * 0.85f
						&& maneuverCooldown <= 0f) {
					maneuver = Maneuver.DASH;
					maneuverRemaining = DASH_SECONDS;
					maneuverCooldown = DASH_COOLDOWN_SECONDS;
				} else {
					maneuver = Maneuver.FOLLOW_BRAIN;
				}
				break;
			case RETREATING_SKIRMISHER:
				float retreatDistance = Math.max(2.25f, safeRange * 0.58f);
				if (distance < retreatDistance
						&& maneuverCooldown <= 0f) {
					maneuver = Maneuver.RETREAT;
					maneuverRemaining = RETREAT_SECONDS;
					maneuverCooldown = RETREAT_COOLDOWN_SECONDS;
				} else if (distance <= safeRange) {
					maneuver = Maneuver.ANCHOR_AND_SUPPRESS;
				} else {
					maneuver = Maneuver.FOLLOW_BRAIN;
				}
				break;
			default:
				maneuver = Maneuver.FOLLOW_BRAIN;
				break;
		}
	}

	private void emitIntent(
			float targetDeltaX,
			float targetDeltaY,
			float brainDesiredX,
			float brainDesiredY,
			Intent out) {
		float distanceSquared = targetDeltaX * targetDeltaX
				+ targetDeltaY * targetDeltaY;
		float inverseDistance = distanceSquared > 0.000001f
				? 1f / (float)Math.sqrt(distanceSquared)
				: 0f;
		float towardX = targetDeltaX * inverseDistance;
		float towardY = targetDeltaY * inverseDistance;

		switch (maneuver) {
			case ANCHOR_AND_SUPPRESS:
				out.set(maneuver, 0f, 0f, 0f);
				break;
			case FLANK_LEFT:
			case FLANK_RIGHT:
				float side = maneuver == Maneuver.FLANK_LEFT ? -1f : 1f;
				float flankX = -towardY * side + towardX * 0.18f;
				float flankY = towardX * side + towardY * 0.18f;
				float inverseFlankLength = 1f / (float)Math.sqrt(
						flankX * flankX + flankY * flankY);
				out.set(
						maneuver,
						flankX * inverseFlankLength,
						flankY * inverseFlankLength,
						1.08f);
				break;
			case DASH:
				out.set(maneuver, towardX, towardY, 1.6f);
				break;
			case RETREAT:
				out.set(maneuver, -towardX, -towardY, 1.2f);
				break;
			default:
				out.set(maneuver, brainDesiredX, brainDesiredY, 1f);
				break;
		}
	}

	public Profile profile() {
		return profile;
	}

	public Maneuver maneuver() {
		return maneuver;
	}

	public float maneuverCooldown() {
		return maneuverCooldown;
	}

	public int decisionSequence() {
		return decisionSequence;
	}

	public static Profile profileFor(EnemyArchetypeDefinition definition) {
		if (definition == null || definition.role == null) {
			return Profile.STANDARD;
		}
		if (definition.role == EnemyRole.MELEE_RUSHER
				|| hasAbility(definition.abilities, "SHORT_DASH")
				|| hasAbility(definition.abilities, "BREACH_CORNERS")) {
			return Profile.RUSHER;
		}
		if (definition.role == EnemyRole.ELITE_COMMANDER
				|| hasAbility(definition.abilities, "ORDER_FLANK")) {
			return Profile.FLANKER;
		}
		if (definition.role == EnemyRole.ARMORED_SUPPRESSOR
				|| hasAbility(definition.abilities, "SHORT_SUPPRESSION")
				|| hasAbility(definition.abilities, "BURST_SUPPRESSION")) {
			return Profile.SUPPRESSOR;
		}
		if (definition.role == EnemyRole.SCOUT_ALARM
				|| hasAbility(definition.abilities, "TACTICAL_RETREAT")
				|| hasAbility(
						definition.abilities,
						"RETREAT_FROM_STRONG_TARGET")
				|| hasAbility(definition.abilities, "AVOID_DIRECT_FIRE")) {
			return Profile.RETREATING_SKIRMISHER;
		}
		return Profile.STANDARD;
	}

	private static boolean hasAbility(String[] abilities, String required) {
		if (abilities == null) return false;
		for (String ability : abilities) {
			if (required.equals(ability)) return true;
		}
		return false;
	}

	private static void requireFinite(float value, String label) {
		if (!BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(label + " must be finite");
		}
	}

	private static void requireNonNegative(float value, String label) {
		requireFinite(value, label);
		if (value < 0f) {
			throw new IllegalArgumentException(
					label + " must not be negative");
		}
	}
}
