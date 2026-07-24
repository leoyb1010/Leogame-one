package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Optional three-phase encounter for Boss "White Line".
 *
 * Each phase requires a distinct world-space mechanism before damage can
 * advance the encounter. All puzzle choices are derived from the raid seed;
 * this class never consumes the host game's global RNG. An available non-boss
 * route can bypass the fight at any point.
 */
public final class WhiteLineBossStateMachine implements Bundlable {

	public static final int DEFAULT_BODY_COUNT = 4;
	public static final String DEFAULT_FOG_LAMP_ANCHOR =
			"fog_lamp_pump_station";

	private static final String MAXIMUM_HEALTH = "maximum_health";
	private static final String HEALTH = "health";
	private static final String PHASE = "phase";
	private static final String OBJECTIVE = "objective";
	private static final String VULNERABLE = "vulnerable";
	private static final String ENCOUNTER_SECONDS = "encounter_seconds";
	private static final String PHASE_SECONDS = "phase_seconds";
	private static final String ENCOUNTER_KEY = "encounter_key";
	private static final String BODY_COUNT = "body_count";
	private static final String TRUE_BODY_INDEX = "true_body_index";
	private static final String FOG_LAMP_ANCHOR = "fog_lamp_anchor";

	public enum Phase {
		DORMANT,
		UMBRELLA_SHIELD,
		DECOY_SEARCH,
		FOG_LAMP_OVERLOAD,
		DEFEATED,
		BYPASSED
	}

	public enum Objective {
		NONE,
		FLANK_UMBRELLA,
		IDENTIFY_TRUE_BODY,
		DISABLE_FOG_LAMPS
	}

	public enum Result {
		NO_CHANGE,
		ENGAGED,
		OBJECTIVE_COMPLETED,
		MECHANISM_REJECTED,
		DAMAGE_BLOCKED,
		DAMAGED,
		PHASE_CHANGED,
		DEFEATED,
		BYPASSED
	}

	public static final float RETREAT_RECOMMENDATION_SECONDS = 120f;
	public static final float TARGET_MINIMUM_SECONDS = 45f;
	public static final float TARGET_MAXIMUM_SECONDS = 120f;

	private int maximumHealth;
	private int phaseTwoThreshold;
	private int phaseThreeThreshold;
	private int health;
	private Phase phase = Phase.DORMANT;
	private Objective objective = Objective.NONE;
	private boolean vulnerable;
	private float encounterSeconds;
	private float phaseSeconds;
	private long encounterKey;
	private int bodyCount = DEFAULT_BODY_COUNT;
	private int trueBodyIndex;
	private String fogLampAnchor = DEFAULT_FOG_LAMP_ANCHOR;

	/** Required by the project bundle format. */
	public WhiteLineBossStateMachine() {
		configure(3, 0L, DEFAULT_BODY_COUNT, DEFAULT_FOG_LAMP_ANCHOR);
	}

	public WhiteLineBossStateMachine(int maximumHealth) {
		this(maximumHealth, 0L);
	}

	public WhiteLineBossStateMachine(int maximumHealth, long encounterKey) {
		configure(
				maximumHealth,
				encounterKey,
				DEFAULT_BODY_COUNT,
				DEFAULT_FOG_LAMP_ANCHOR);
	}

	private void configure(
			int maximumHealth,
			long encounterKey,
			int bodyCount,
			String fogLampAnchor) {
		if (maximumHealth < 3) {
			throw new IllegalArgumentException(
					"maximumHealth must be at least 3"
			);
		}
		if (bodyCount < 3) {
			throw new IllegalArgumentException(
					"bodyCount must be at least 3");
		}
		if (fogLampAnchor == null || fogLampAnchor.isEmpty()) {
			throw new IllegalArgumentException(
					"fogLampAnchor is required");
		}
		this.maximumHealth = maximumHealth;
		this.encounterKey = encounterKey;
		this.bodyCount = bodyCount;
		this.fogLampAnchor = fogLampAnchor;
		health = maximumHealth;
		phaseTwoThreshold = Math.max(2, (int)Math.ceil(maximumHealth * 0.66f));
		phaseThreeThreshold = Math.max(1, (int)Math.ceil(maximumHealth * 0.33f));
		trueBodyIndex = deterministicBodyIndex(encounterKey, bodyCount);
	}

	public Result engage() {
		if (phase != Phase.DORMANT) {
			return Result.NO_CHANGE;
		}
		enter(
				Phase.UMBRELLA_SHIELD,
				Objective.FLANK_UMBRELLA
		);
		return Result.ENGAGED;
	}

	public void update(float dt) {
		if (dt < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(dt)) {
			throw new IllegalArgumentException(
					"dt must be finite and non-negative"
			);
		}
		if (active()) {
			encounterSeconds += dt;
			phaseSeconds += dt;
		}
	}

	/**
	 * Phase one: the umbrella only opens from its side or rear hemisphere.
	 *
	 * @param facingX shield-facing world direction
	 * @param facingY shield-facing world direction
	 * @param approachX vector from the boss to the interacting player
	 * @param approachY vector from the boss to the interacting player
	 */
	public Result flankUmbrella(
			float facingX,
			float facingY,
			float approachX,
			float approachY) {
		if (objective != Objective.FLANK_UMBRELLA) {
			return Result.NO_CHANGE;
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(facingX)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(facingY)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(approachX)
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(approachY)) {
			return Result.MECHANISM_REJECTED;
		}
		float facingLengthSquared = facingX * facingX + facingY * facingY;
		float approachLengthSquared =
				approachX * approachX + approachY * approachY;
		if (facingLengthSquared < 0.0001f
				|| approachLengthSquared < 0.0001f) {
			return Result.MECHANISM_REJECTED;
		}
		float normalizedDot = (facingX * approachX + facingY * approachY)
				/ (float)Math.sqrt(
						facingLengthSquared * approachLengthSquared);
		// The forward 75-degree cone is protected. Exact side and rear
		// positions are valid, so the player can circle instead of pixel-hunt.
		if (normalizedDot > 0.25f) {
			return Result.MECHANISM_REJECTED;
		}
		return revealWeakPoint();
	}

	/**
	 * Phase two: only the seed-selected synchronized trace is the true body.
	 */
	public Result identifyTrueBody(int selectedBodyIndex) {
		if (objective != Objective.IDENTIFY_TRUE_BODY) {
			return Result.NO_CHANGE;
		}
		if (selectedBodyIndex < 0
				|| selectedBodyIndex >= bodyCount
				|| selectedBodyIndex != trueBodyIndex) {
			return Result.MECHANISM_REJECTED;
		}
		return revealWeakPoint();
	}

	/**
	 * Phase three: the weak point opens only from the authored fog-lamp anchor.
	 */
	public Result disableFogLamp(String operatedAnchor) {
		if (objective != Objective.DISABLE_FOG_LAMPS) {
			return Result.NO_CHANGE;
		}
		if (!fogLampAnchor.equals(operatedAnchor)) {
			return Result.MECHANISM_REJECTED;
		}
		return revealWeakPoint();
	}

	private Result revealWeakPoint() {
		objective = Objective.NONE;
		vulnerable = true;
		return Result.OBJECTIVE_COMPLETED;
	}

	public Result applyDamage(int requestedDamage) {
		if (requestedDamage < 0) {
			throw new IllegalArgumentException(
					"requestedDamage must not be negative"
			);
		}
		if (!active() || requestedDamage == 0) {
			return Result.NO_CHANGE;
		}
		if (!vulnerable) {
			return Result.DAMAGE_BLOCKED;
		}

		int floor = phase == Phase.UMBRELLA_SHIELD
				? phaseTwoThreshold
				: phase == Phase.DECOY_SEARCH
				? phaseThreeThreshold
				: 0;
		health = Math.max(floor, health - requestedDamage);
		if (phase == Phase.UMBRELLA_SHIELD
				&& health <= phaseTwoThreshold) {
			enter(
					Phase.DECOY_SEARCH,
					Objective.IDENTIFY_TRUE_BODY
			);
			return Result.PHASE_CHANGED;
		}
		if (phase == Phase.DECOY_SEARCH
				&& health <= phaseThreeThreshold) {
			enter(
					Phase.FOG_LAMP_OVERLOAD,
					Objective.DISABLE_FOG_LAMPS
			);
			return Result.PHASE_CHANGED;
		}
		if (phase == Phase.FOG_LAMP_OVERLOAD && health == 0) {
			phase = Phase.DEFEATED;
			objective = Objective.NONE;
			vulnerable = false;
			return Result.DEFEATED;
		}
		return Result.DAMAGED;
	}

	public Result bypass(boolean nonBossExtractionRouteAvailable) {
		if (!nonBossExtractionRouteAvailable
				|| phase == Phase.DEFEATED
				|| phase == Phase.BYPASSED) {
			return Result.NO_CHANGE;
		}
		phase = Phase.BYPASSED;
		objective = Objective.NONE;
		vulnerable = false;
		return Result.BYPASSED;
	}

	public boolean active() {
		return phase == Phase.UMBRELLA_SHIELD
				|| phase == Phase.DECOY_SEARCH
				|| phase == Phase.FOG_LAMP_OVERLOAD;
	}

	public boolean retreatRecommended() {
		return active()
				&& encounterSeconds >= RETREAT_RECOMMENDATION_SECONDS;
	}

	public int health() {
		return health;
	}

	public int maximumHealth() {
		return maximumHealth;
	}

	public Phase phase() {
		return phase;
	}

	public Objective objective() {
		return objective;
	}

	public boolean vulnerable() {
		return vulnerable;
	}

	public float encounterSeconds() {
		return encounterSeconds;
	}

	public float phaseSeconds() {
		return phaseSeconds;
	}

	public boolean insideTargetEncounterWindow() {
		return encounterSeconds >= TARGET_MINIMUM_SECONDS
				&& encounterSeconds <= TARGET_MAXIMUM_SECONDS;
	}

	public int bodyCount() {
		return bodyCount;
	}

	public int trueBodyIndex() {
		return trueBodyIndex;
	}

	/**
	 * Stable readable clue used by the scene marker: the authentic trace is
	 * synchronized; every decoy is hollow. This is intentionally deterministic
	 * and remains identical after reload.
	 */
	public boolean synchronizedTrace(int bodyIndex) {
		return bodyIndex >= 0
				&& bodyIndex < bodyCount
				&& bodyIndex == trueBodyIndex;
	}

	public String fogLampAnchor() {
		return fogLampAnchor;
	}

	private void enter(Phase nextPhase, Objective nextObjective) {
		phase = nextPhase;
		objective = nextObjective;
		vulnerable = false;
		phaseSeconds = 0f;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(MAXIMUM_HEALTH, maximumHealth);
		bundle.put(HEALTH, health);
		bundle.put(PHASE, phase);
		bundle.put(OBJECTIVE, objective);
		bundle.put(VULNERABLE, vulnerable);
		bundle.put(ENCOUNTER_SECONDS, encounterSeconds);
		bundle.put(PHASE_SECONDS, phaseSeconds);
		bundle.put(ENCOUNTER_KEY, encounterKey);
		bundle.put(BODY_COUNT, bodyCount);
		bundle.put(TRUE_BODY_INDEX, trueBodyIndex);
		bundle.put(FOG_LAMP_ANCHOR, fogLampAnchor);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		int restoredMaximum = bundle.getInt(MAXIMUM_HEALTH);
		int restoredBodies = bundle.contains(BODY_COUNT)
				? bundle.getInt(BODY_COUNT) : DEFAULT_BODY_COUNT;
		String restoredAnchor = bundle.contains(FOG_LAMP_ANCHOR)
				? bundle.getString(FOG_LAMP_ANCHOR)
				: DEFAULT_FOG_LAMP_ANCHOR;
		configure(
				restoredMaximum,
				bundle.getLong(ENCOUNTER_KEY),
				restoredBodies,
				restoredAnchor);
		health = Math.max(0, Math.min(maximumHealth, bundle.getInt(HEALTH)));
		phase = bundle.getEnum(PHASE, Phase.class);
		objective = bundle.getEnum(OBJECTIVE, Objective.class);
		vulnerable = bundle.getBoolean(VULNERABLE);
		encounterSeconds = Math.max(
				0f, bundle.getFloat(ENCOUNTER_SECONDS));
		phaseSeconds = bundle.contains(PHASE_SECONDS)
				? Math.max(0f, bundle.getFloat(PHASE_SECONDS))
				: 0f;
		// Persist the value as an audit check, but a corrupt or older value
		// cannot change the seed-authored answer.
		trueBodyIndex = deterministicBodyIndex(encounterKey, bodyCount);
	}

	private static int deterministicBodyIndex(long key, int count) {
		long value = key ^ 0xD1B54A32D192ED03L;
		value ^= value >>> 33;
		value *= 0xff51afd7ed558ccdL;
		value ^= value >>> 33;
		value *= 0xc4ceb9fe1a85ec53L;
		value ^= value >>> 33;
		return (int)((value & Long.MAX_VALUE) % count);
	}
}
