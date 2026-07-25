package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Allocation-free decision state for one realtime enemy.
 *
 * Host Mob calls stay outside this class so the decision model can be tested
 * without starting the legacy Actor scheduler.
 */
public final class RealtimeEnemyBrain {

	public static final class Snapshot implements Bundlable {

		private State state = State.PATROL;
		private float perceptionRemaining;
		private float lastSeenAge = Float.MAX_VALUE;
		private float lastSeenX;
		private float lastSeenY;
		private float lastHeardAge = Float.MAX_VALUE;
		private float lastHeardX;
		private float lastHeardY;
		private float desiredX;
		private float desiredY;
		private float navigationTargetX;
		private float navigationTargetY;
		private float attackCooldown;
		private float searchRemaining;
		private float waypointRemaining;
		private float searchOriginX;
		private float searchOriginY;
		private float patrolAnchorX;
		private float patrolAnchorY;
		private int searchSequence;
		private int patrolSequence;
		private boolean seesPlayer;
		private boolean investigatingSound;
		private boolean attackRequested;
		private boolean searchSweeping;
		private boolean hasPatrolAnchor;

		public Snapshot() {
			// Required by Bundle reflection.
		}

		private Snapshot(RealtimeEnemyBrain brain) {
			state = brain.state;
			perceptionRemaining = brain.perceptionRemaining;
			lastSeenAge = BukovNumbers.isFinite(brain.lastSeenAge)
					? brain.lastSeenAge : Float.MAX_VALUE;
			lastSeenX = brain.lastSeenX;
			lastSeenY = brain.lastSeenY;
			lastHeardAge = brain.lastHeardAge;
			lastHeardX = brain.lastHeardX;
			lastHeardY = brain.lastHeardY;
			desiredX = brain.desiredX;
			desiredY = brain.desiredY;
			navigationTargetX = brain.navigationTargetX;
			navigationTargetY = brain.navigationTargetY;
			attackCooldown = brain.attackCooldown;
			searchRemaining = brain.searchRemaining;
			waypointRemaining = brain.waypointRemaining;
			searchOriginX = brain.searchOriginX;
			searchOriginY = brain.searchOriginY;
			patrolAnchorX = brain.patrolAnchorX;
			patrolAnchorY = brain.patrolAnchorY;
			searchSequence = brain.searchSequence;
			patrolSequence = brain.patrolSequence;
			seesPlayer = brain.seesPlayer;
			investigatingSound = brain.investigatingSound;
			attackRequested = brain.attackRequested;
			searchSweeping = brain.searchSweeping;
			hasPatrolAnchor = brain.hasPatrolAnchor;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("state", state);
			bundle.put("perception_remaining", perceptionRemaining);
			bundle.put("last_seen_age", lastSeenAge);
			bundle.put("last_seen_x", lastSeenX);
			bundle.put("last_seen_y", lastSeenY);
			bundle.put("last_heard_age", lastHeardAge);
			bundle.put("last_heard_x", lastHeardX);
			bundle.put("last_heard_y", lastHeardY);
			bundle.put("desired_x", desiredX);
			bundle.put("desired_y", desiredY);
			bundle.put("navigation_target_x", navigationTargetX);
			bundle.put("navigation_target_y", navigationTargetY);
			bundle.put("attack_cooldown", attackCooldown);
			bundle.put("search_remaining", searchRemaining);
			bundle.put("waypoint_remaining", waypointRemaining);
			bundle.put("search_origin_x", searchOriginX);
			bundle.put("search_origin_y", searchOriginY);
			bundle.put("patrol_anchor_x", patrolAnchorX);
			bundle.put("patrol_anchor_y", patrolAnchorY);
			bundle.put("search_sequence", searchSequence);
			bundle.put("patrol_sequence", patrolSequence);
			bundle.put("sees_player", seesPlayer);
			bundle.put("investigating_sound", investigatingSound);
			bundle.put("attack_requested", attackRequested);
			bundle.put("search_sweeping", searchSweeping);
			bundle.put("has_patrol_anchor", hasPatrolAnchor);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			state = bundle.getEnum("state", State.class);
			if (state == State.IDLE) {
				state = State.PATROL;
			}
			perceptionRemaining = finiteNonNegative(
					bundle.getFloat("perception_remaining"),
					"perception_remaining");
			lastSeenAge = finiteNonNegative(
					bundle.getFloat("last_seen_age"), "last_seen_age");
			lastSeenX = finite(bundle.getFloat("last_seen_x"), "last_seen_x");
			lastSeenY = finite(bundle.getFloat("last_seen_y"), "last_seen_y");
			lastHeardAge = bundle.contains("last_heard_age")
					? finiteNonNegative(
							bundle.getFloat("last_heard_age"),
							"last_heard_age")
					: Float.MAX_VALUE;
			lastHeardX = bundle.contains("last_heard_x")
					? finite(bundle.getFloat("last_heard_x"),
							"last_heard_x") : 0f;
			lastHeardY = bundle.contains("last_heard_y")
					? finite(bundle.getFloat("last_heard_y"),
							"last_heard_y") : 0f;
			desiredX = finite(bundle.getFloat("desired_x"), "desired_x");
			desiredY = finite(bundle.getFloat("desired_y"), "desired_y");
			navigationTargetX = bundle.contains("navigation_target_x")
					? finite(bundle.getFloat("navigation_target_x"),
							"navigation_target_x") : lastSeenX;
			navigationTargetY = bundle.contains("navigation_target_y")
					? finite(bundle.getFloat("navigation_target_y"),
							"navigation_target_y") : lastSeenY;
			attackCooldown = finiteNonNegative(
					bundle.getFloat("attack_cooldown"), "attack_cooldown");
			searchRemaining = optionalNonNegative(
					bundle, "search_remaining");
			waypointRemaining = optionalNonNegative(
					bundle, "waypoint_remaining");
			searchOriginX = optionalFinite(bundle, "search_origin_x");
			searchOriginY = optionalFinite(bundle, "search_origin_y");
			patrolAnchorX = optionalFinite(bundle, "patrol_anchor_x");
			patrolAnchorY = optionalFinite(bundle, "patrol_anchor_y");
			searchSequence = optionalNonNegativeInt(
					bundle, "search_sequence");
			patrolSequence = optionalNonNegativeInt(
					bundle, "patrol_sequence");
			seesPlayer = bundle.getBoolean("sees_player");
			investigatingSound = bundle.getBoolean("investigating_sound");
			attackRequested = bundle.getBoolean("attack_requested");
			searchSweeping = bundle.getBoolean("search_sweeping");
			hasPatrolAnchor = bundle.getBoolean("has_patrol_anchor");
			if (!bundle.contains("last_heard_age")
					&& investigatingSound) {
				// Alpha31 and earlier stored audible positions in the shared
				// last-seen fields. Preserve that investigation on migration.
				lastHeardAge = lastSeenAge;
				lastHeardX = lastSeenX;
				lastHeardY = lastSeenY;
			}
			if (state == State.DEAD && (seesPlayer || attackRequested)) {
				throw new IllegalStateException(
						"Dead enemy snapshot cannot target or attack");
			}
		}

		private static float finite(float value, String label) {
			if (!BukovNumbers.isFinite(value)) {
				throw new IllegalStateException(
						"Invalid enemy brain snapshot: " + label);
			}
			return value;
		}

		private static float finiteNonNegative(float value, String label) {
			if (value < 0f) {
				throw new IllegalStateException(
						"Invalid enemy brain snapshot: " + label);
			}
			return finite(value, label);
		}

		private static float optionalFinite(Bundle bundle, String label) {
			return bundle.contains(label)
					? finite(bundle.getFloat(label), label) : 0f;
		}

		private static float optionalNonNegative(
				Bundle bundle, String label) {
			return bundle.contains(label)
					? finiteNonNegative(bundle.getFloat(label), label) : 0f;
		}

		private static int optionalNonNegativeInt(
				Bundle bundle, String label) {
			int value = bundle.contains(label) ? bundle.getInt(label) : 0;
			if (value < 0) {
				throw new IllegalStateException(
						"Invalid enemy brain snapshot: " + label);
			}
			return value;
		}
	}

	public enum State {
		PATROL,
		IDLE,
		INVESTIGATE,
		CHASE,
		ATTACK,
		SEARCH,
		DEAD
	}

	public static final float PERCEPTION_PERIOD_SECONDS = 0.10f;
	public static final float SOUND_MEMORY_SECONDS = 4f;
	public static final float MINIMUM_SEARCH_SECONDS = 3f;
	public static final float MAXIMUM_SEARCH_SECONDS = 6f;
	public static final float SEARCH_WAYPOINT_SECONDS = 0.75f;
	public static final float PATROL_WAYPOINT_SECONDS = 2.5f;
	private static final float ARRIVAL_DISTANCE = 0.4f;
	private static final float[] SEARCH_OFFSET_X =
			{1f, 1f, 0f, -1f, -1f, -1f, 0f, 1f};
	private static final float[] SEARCH_OFFSET_Y =
			{0f, 1f, 1f, 1f, 0f, -1f, -1f, -1f};
	private static final float[] PATROL_OFFSET_X =
			{1f, 0f, -1f, 0f};
	private static final float[] PATROL_OFFSET_Y =
			{0f, 1f, 0f, -1f};

	private final int stableKey;
	private State state = State.PATROL;
	private float perceptionRemaining;
	private float lastSeenAge = Float.MAX_VALUE;
	private float lastSeenX;
	private float lastSeenY;
	private float lastHeardAge = Float.MAX_VALUE;
	private float lastHeardX;
	private float lastHeardY;
	private float desiredX;
	private float desiredY;
	private float navigationTargetX;
	private float navigationTargetY;
	private float attackCooldown;
	private float searchRemaining;
	private float waypointRemaining;
	private float searchOriginX;
	private float searchOriginY;
	private float patrolAnchorX;
	private float patrolAnchorY;
	private int searchSequence;
	private int patrolSequence;
	private boolean seesPlayer;
	private boolean investigatingSound;
	private boolean attackRequested;
	private boolean searchSweeping;
	private boolean hasPatrolAnchor;

	public RealtimeEnemyBrain(int stableKey) {
		this.stableKey = stableKey;
		int slot = com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.floorMod(stableKey, 10);
		perceptionRemaining =
				slot * (PERCEPTION_PERIOD_SECONDS / 10f);
	}

	/**
	 * Ages memory every fixed tick and returns true only for this enemy's
	 * staggered 10 Hz perception slot.
	 */
	public boolean perceptionDue(float dt) {
		requireNonNegative(dt, "dt");
		if (state == State.DEAD) {
			return false;
		}
		lastSeenAge = Math.min(
				Float.MAX_VALUE,
				lastSeenAge + dt
		);
		lastHeardAge = Math.min(
				Float.MAX_VALUE,
				lastHeardAge + dt
		);
		perceptionRemaining -= dt;
		if (perceptionRemaining > 0f) {
			return false;
		}
		do {
			perceptionRemaining += PERCEPTION_PERIOD_SECONDS;
		} while (perceptionRemaining <= 0f);
		return true;
	}

	public void recordPlayer(boolean visible, float playerX, float playerY) {
		if (state == State.DEAD) {
			return;
		}
		boolean lostVisualContact = seesPlayer && !visible;
		seesPlayer = visible;
		if (visible) {
			investigatingSound = false;
			lastSeenX = playerX;
			lastSeenY = playerY;
			lastSeenAge = 0f;
		} else if (lostVisualContact) {
			beginSearch(lastSeenX, lastSeenY);
		}
	}

	/** Records an audible but not necessarily visible player action. */
	public void recordSound(float sourceX, float sourceY) {
		if (state == State.DEAD || seesPlayer
				|| Float.isNaN(sourceX) || Float.isNaN(sourceY)
				|| Float.isInfinite(sourceX) || Float.isInfinite(sourceY)) {
			return;
		}
		lastHeardX = sourceX;
		lastHeardY = sourceY;
		lastHeardAge = 0f;
		investigatingSound = true;
	}

	/**
	 * Chooses a target and movement direction. Attack execution remains queued
	 * for the world's mob and damage stages.
	 */
	public void decide(float dt,
					   float selfX,
					   float selfY,
					   float playerX,
					   float playerY,
					   float attackRange) {
		requireNonNegative(dt, "dt");
		requireNonNegative(attackRange, "attackRange");
		attackRequested = false;
		attackCooldown = Math.max(0f, attackCooldown - dt);
		if (state == State.DEAD) {
			stop();
			return;
		}
		ensurePatrolAnchor(selfX, selfY);

		if (seesPlayer) {
			decideVisiblePlayer(
					selfX,
					selfY,
					playerX,
					playerY,
					attackRange);
			return;
		}

		if (investigatingSound
				&& lastHeardAge <= SOUND_MEMORY_SECONDS) {
			state = State.INVESTIGATE;
			setNavigationTarget(lastHeardX, lastHeardY, selfX, selfY);
			if (arrived(selfX, selfY, lastHeardX, lastHeardY)) {
				beginSearch(lastHeardX, lastHeardY);
				advanceSearch(dt, selfX, selfY);
			}
			return;
		}

		if (state == State.CHASE || state == State.ATTACK) {
			beginSearch(lastSeenX, lastSeenY);
		} else if (state == State.INVESTIGATE) {
			beginSearch(lastHeardX, lastHeardY);
		}

		if (state == State.SEARCH) {
			advanceSearch(dt, selfX, selfY);
			return;
		}

		advancePatrol(dt, selfX, selfY);
	}

	/**
	 * Feeds the navigator's reachability result back into the deterministic
	 * state machine. An unreachable remembered point starts the local sweep
	 * from the closest position actually reached; patrol simply advances to
	 * its next authored waypoint.
	 */
	public void observeNavigation(
			boolean targetUnreachable, float selfX, float selfY) {
		if (!targetUnreachable || state == State.DEAD || seesPlayer) {
			return;
		}
		if (state == State.INVESTIGATE) {
			beginSearch(selfX, selfY);
			beginSearchSweep(selfX, selfY);
		} else if (state == State.SEARCH) {
			if (!searchSweeping) {
				beginSearchSweep(selfX, selfY);
			}
			selectSearchWaypoint();
			setDirectionToTarget(selfX, selfY);
		} else if (state == State.PATROL || state == State.IDLE) {
			selectPatrolWaypoint();
			setDirectionToTarget(selfX, selfY);
		}
	}

	private void decideVisiblePlayer(
			float selfX,
			float selfY,
			float playerX,
			float playerY,
			float attackRange) {
		navigationTargetX = playerX;
		navigationTargetY = playerY;
		float deltaX = playerX - selfX;
		float deltaY = playerY - selfY;
		float distanceSquared = deltaX * deltaX + deltaY * deltaY;
		if (distanceSquared <= attackRange * attackRange) {
			state = State.ATTACK;
			stop();
			attackRequested = attackCooldown <= 0f;
		} else {
			state = State.CHASE;
			setDirection(deltaX, deltaY);
		}
	}

	private void beginSearch(float originX, float originY) {
		state = State.SEARCH;
		investigatingSound = false;
		searchOriginX = originX;
		searchOriginY = originY;
		navigationTargetX = originX;
		navigationTargetY = originY;
		searchRemaining = searchDuration();
		waypointRemaining = 0f;
		searchSequence = 0;
		searchSweeping = false;
	}

	private void advanceSearch(float dt, float selfX, float selfY) {
		if (!searchSweeping) {
			if (!arrived(
					selfX,
					selfY,
					searchOriginX,
					searchOriginY)) {
				setNavigationTarget(
						searchOriginX,
						searchOriginY,
						selfX,
						selfY);
				return;
			}
			beginSearchSweep(searchOriginX, searchOriginY);
		}

		searchRemaining = Math.max(0f, searchRemaining - dt);
		if (searchRemaining <= 0f) {
			enterPatrol(selfX, selfY);
			return;
		}
		waypointRemaining = Math.max(0f, waypointRemaining - dt);
		if (waypointRemaining <= 0f
				|| arrived(
						selfX,
						selfY,
						navigationTargetX,
						navigationTargetY)) {
			selectSearchWaypoint();
		}
		setDirectionToTarget(selfX, selfY);
	}

	private void beginSearchSweep(float originX, float originY) {
		searchOriginX = originX;
		searchOriginY = originY;
		searchSweeping = true;
		waypointRemaining = 0f;
	}

	private void selectSearchWaypoint() {
		int direction = BukovNumbers.floorMod(
				stableKey + searchSequence * 3,
				SEARCH_OFFSET_X.length);
		float radius = (searchSequence & 1) == 0 ? 1.5f : 2.5f;
		navigationTargetX =
				searchOriginX + SEARCH_OFFSET_X[direction] * radius;
		navigationTargetY =
				searchOriginY + SEARCH_OFFSET_Y[direction] * radius;
		searchSequence++;
		waypointRemaining = SEARCH_WAYPOINT_SECONDS;
	}

	private void advancePatrol(float dt, float selfX, float selfY) {
		if (state == State.IDLE) state = State.PATROL;
		state = State.PATROL;
		waypointRemaining = Math.max(0f, waypointRemaining - dt);
		if (waypointRemaining <= 0f
				|| arrived(
						selfX,
						selfY,
						navigationTargetX,
						navigationTargetY)) {
			selectPatrolWaypoint();
		}
		setDirectionToTarget(selfX, selfY);
	}

	private void enterPatrol(float selfX, float selfY) {
		state = State.PATROL;
		investigatingSound = false;
		searchSweeping = false;
		searchRemaining = 0f;
		waypointRemaining = 0f;
		if (!hasPatrolAnchor) {
			patrolAnchorX = selfX;
			patrolAnchorY = selfY;
			hasPatrolAnchor = true;
		}
		selectPatrolWaypoint();
		setDirectionToTarget(selfX, selfY);
	}

	private void ensurePatrolAnchor(float selfX, float selfY) {
		if (hasPatrolAnchor) return;
		patrolAnchorX = selfX;
		patrolAnchorY = selfY;
		navigationTargetX = selfX;
		navigationTargetY = selfY;
		hasPatrolAnchor = true;
	}

	private void selectPatrolWaypoint() {
		int direction = BukovNumbers.floorMod(
				stableKey + patrolSequence,
				PATROL_OFFSET_X.length);
		float radius = 2f + BukovNumbers.floorMod(stableKey, 2);
		navigationTargetX =
				patrolAnchorX + PATROL_OFFSET_X[direction] * radius;
		navigationTargetY =
				patrolAnchorY + PATROL_OFFSET_Y[direction] * radius;
		patrolSequence++;
		waypointRemaining = PATROL_WAYPOINT_SECONDS;
	}

	private void setNavigationTarget(
			float targetX,
			float targetY,
			float selfX,
			float selfY) {
		navigationTargetX = targetX;
		navigationTargetY = targetY;
		setDirectionToTarget(selfX, selfY);
	}

	private void setDirectionToTarget(float selfX, float selfY) {
		setDirection(
				navigationTargetX - selfX,
				navigationTargetY - selfY);
	}

	private void setDirection(float deltaX, float deltaY) {
		float distanceSquared = deltaX * deltaX + deltaY * deltaY;
		if (distanceSquared <= 0.000001f) {
			stop();
			return;
		}
		float inverseDistance = 1f / (float)Math.sqrt(distanceSquared);
		desiredX = deltaX * inverseDistance;
		desiredY = deltaY * inverseDistance;
	}

	private boolean arrived(
			float selfX,
			float selfY,
			float targetX,
			float targetY) {
		float deltaX = targetX - selfX;
		float deltaY = targetY - selfY;
		return deltaX * deltaX + deltaY * deltaY
				<= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
	}

	private float searchDuration() {
		int slots = (int)(MAXIMUM_SEARCH_SECONDS
				- MINIMUM_SEARCH_SECONDS) + 1;
		return MINIMUM_SEARCH_SECONDS
				+ BukovNumbers.floorMod(stableKey, slots);
	}

	public boolean consumeAttack(float cooldownSeconds) {
		if (!attackRequested || state == State.DEAD) {
			return false;
		}
		if (cooldownSeconds <= 0f) {
			throw new IllegalArgumentException(
					"cooldownSeconds must be positive"
			);
		}
		attackRequested = false;
		attackCooldown = cooldownSeconds;
		return true;
	}

	public void markDead() {
		state = State.DEAD;
		seesPlayer = false;
		investigatingSound = false;
		attackRequested = false;
		stop();
	}

	public State state() {
		return state;
	}

	public float desiredX() {
		return desiredX;
	}

	public float desiredY() {
		return desiredY;
	}

	public float navigationTargetX() {
		return navigationTargetX;
	}

	public float navigationTargetY() {
		return navigationTargetY;
	}

	public float attackCooldown() {
		return attackCooldown;
	}

	public boolean seesPlayer() {
		return seesPlayer;
	}

	public float lastSeenAge() {
		return lastSeenAge;
	}

	public float lastSeenX() {
		return lastSeenX;
	}

	public float lastSeenY() {
		return lastSeenY;
	}

	public float lastHeardAge() {
		return lastHeardAge;
	}

	public float lastHeardX() {
		return lastHeardX;
	}

	public float lastHeardY() {
		return lastHeardY;
	}

	public boolean investigatingSound() {
		return investigatingSound;
	}

	public float searchRemaining() {
		return searchRemaining;
	}

	public boolean searchSweeping() {
		return searchSweeping;
	}

	public float perceptionRemaining() {
		return perceptionRemaining;
	}

	public Snapshot snapshot() {
		return new Snapshot(this);
	}

	public void restoreSnapshot(Snapshot snapshot) {
		if (snapshot == null) return;
		state = snapshot.state;
		perceptionRemaining = snapshot.perceptionRemaining;
		lastSeenAge = snapshot.lastSeenAge;
		lastSeenX = snapshot.lastSeenX;
		lastSeenY = snapshot.lastSeenY;
		lastHeardAge = snapshot.lastHeardAge;
		lastHeardX = snapshot.lastHeardX;
		lastHeardY = snapshot.lastHeardY;
		desiredX = snapshot.desiredX;
		desiredY = snapshot.desiredY;
		navigationTargetX = snapshot.navigationTargetX;
		navigationTargetY = snapshot.navigationTargetY;
		attackCooldown = snapshot.attackCooldown;
		searchRemaining = snapshot.searchRemaining;
		waypointRemaining = snapshot.waypointRemaining;
		searchOriginX = snapshot.searchOriginX;
		searchOriginY = snapshot.searchOriginY;
		patrolAnchorX = snapshot.patrolAnchorX;
		patrolAnchorY = snapshot.patrolAnchorY;
		searchSequence = snapshot.searchSequence;
		patrolSequence = snapshot.patrolSequence;
		seesPlayer = snapshot.seesPlayer;
		investigatingSound = snapshot.investigatingSound;
		attackRequested = snapshot.attackRequested;
		searchSweeping = snapshot.searchSweeping;
		hasPatrolAnchor = snapshot.hasPatrolAnchor;
	}

	private void stop() {
		desiredX = 0f;
		desiredY = 0f;
	}

	private static void requireNonNegative(float value, String label) {
		if (value < 0f || Float.isNaN(value)) {
			throw new IllegalArgumentException(
					label + " must not be negative"
			);
		}
	}
}
