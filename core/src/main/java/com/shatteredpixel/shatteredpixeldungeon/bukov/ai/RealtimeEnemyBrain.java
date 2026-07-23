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

		private State state = State.IDLE;
		private float perceptionRemaining;
		private float lastSeenAge = Float.MAX_VALUE;
		private float lastSeenX;
		private float lastSeenY;
		private float desiredX;
		private float desiredY;
		private float attackCooldown;
		private boolean seesPlayer;
		private boolean investigatingSound;
		private boolean attackRequested;

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
			desiredX = brain.desiredX;
			desiredY = brain.desiredY;
			attackCooldown = brain.attackCooldown;
			seesPlayer = brain.seesPlayer;
			investigatingSound = brain.investigatingSound;
			attackRequested = brain.attackRequested;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("state", state);
			bundle.put("perception_remaining", perceptionRemaining);
			bundle.put("last_seen_age", lastSeenAge);
			bundle.put("last_seen_x", lastSeenX);
			bundle.put("last_seen_y", lastSeenY);
			bundle.put("desired_x", desiredX);
			bundle.put("desired_y", desiredY);
			bundle.put("attack_cooldown", attackCooldown);
			bundle.put("sees_player", seesPlayer);
			bundle.put("investigating_sound", investigatingSound);
			bundle.put("attack_requested", attackRequested);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			state = bundle.getEnum("state", State.class);
			perceptionRemaining = finiteNonNegative(
					bundle.getFloat("perception_remaining"),
					"perception_remaining");
			lastSeenAge = finiteNonNegative(
					bundle.getFloat("last_seen_age"), "last_seen_age");
			lastSeenX = finite(bundle.getFloat("last_seen_x"), "last_seen_x");
			lastSeenY = finite(bundle.getFloat("last_seen_y"), "last_seen_y");
			desiredX = finite(bundle.getFloat("desired_x"), "desired_x");
			desiredY = finite(bundle.getFloat("desired_y"), "desired_y");
			attackCooldown = finiteNonNegative(
					bundle.getFloat("attack_cooldown"), "attack_cooldown");
			seesPlayer = bundle.getBoolean("sees_player");
			investigatingSound = bundle.getBoolean("investigating_sound");
			attackRequested = bundle.getBoolean("attack_requested");
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
	}

	public enum State {
		IDLE,
		INVESTIGATE,
		CHASE,
		ATTACK,
		DEAD
	}

	public static final float PERCEPTION_PERIOD_SECONDS = 0.10f;
	public static final float SIGHT_MEMORY_SECONDS = 1.25f;
	public static final float SOUND_MEMORY_SECONDS = 4f;

	private State state = State.IDLE;
	private float perceptionRemaining;
	private float lastSeenAge = Float.POSITIVE_INFINITY;
	private float lastSeenX;
	private float lastSeenY;
	private float desiredX;
	private float desiredY;
	private float attackCooldown;
	private boolean seesPlayer;
	private boolean investigatingSound;
	private boolean attackRequested;

	public RealtimeEnemyBrain(int stableKey) {
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
		seesPlayer = visible;
		if (visible) {
			investigatingSound = false;
			lastSeenX = playerX;
			lastSeenY = playerY;
			lastSeenAge = 0f;
		}
	}

	/** Records an audible but not necessarily visible player action. */
	public void recordSound(float sourceX, float sourceY) {
		if (state == State.DEAD || seesPlayer
				|| Float.isNaN(sourceX) || Float.isNaN(sourceY)
				|| Float.isInfinite(sourceX) || Float.isInfinite(sourceY)) {
			return;
		}
		lastSeenX = sourceX;
		lastSeenY = sourceY;
		lastSeenAge = 0f;
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

		boolean remembersPlayer = seesPlayer
				|| lastSeenAge <= (investigatingSound
						? SOUND_MEMORY_SECONDS : SIGHT_MEMORY_SECONDS);
		if (!remembersPlayer) {
			state = State.IDLE;
			stop();
			return;
		}

		float targetX = seesPlayer ? playerX : lastSeenX;
		float targetY = seesPlayer ? playerY : lastSeenY;
		float deltaX = targetX - selfX;
		float deltaY = targetY - selfY;
		float distanceSquared = deltaX * deltaX + deltaY * deltaY;

		if (seesPlayer && distanceSquared <= attackRange * attackRange) {
			state = State.ATTACK;
			stop();
			attackRequested = attackCooldown <= 0f;
			return;
		}

		state = investigatingSound && !seesPlayer
				? State.INVESTIGATE : State.CHASE;
		if (distanceSquared <= 0.000001f) {
			stop();
			return;
		}
		float inverseDistance = 1f / (float)Math.sqrt(distanceSquared);
		desiredX = deltaX * inverseDistance;
		desiredY = deltaY * inverseDistance;
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

	public boolean investigatingSound() {
		return investigatingSound;
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
		desiredX = snapshot.desiredX;
		desiredY = snapshot.desiredY;
		attackCooldown = snapshot.attackCooldown;
		seesPlayer = snapshot.seesPlayer;
		investigatingSound = snapshot.investigatingSound;
		attackRequested = snapshot.attackRequested;
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
