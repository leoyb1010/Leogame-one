package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

/**
 * Reusable output from {@link EnemyRangedCombatController}.
 *
 * The realtime world owns one instance per enemy and translates the action
 * into movement, animation, sound, tracer, and damage application.
 */
public final class EnemyRangedCombatIntent {

	public enum Action {
		SEEK_TARGET,
		CLOSE_DISTANCE,
		AIM,
		HOLD_FIRE,
		RELOAD,
		FIRE,
		OUT_OF_AMMO
	}

	private Action action = Action.SEEK_TARGET;
	private boolean reloadStarted;
	private boolean reloadCompleted;
	private boolean damageEvent;
	private int damage;
	private int shotSequence = -1;
	private float directionX;
	private float directionY;
	private float targetDistance;

	void reset(Action action, float targetDistance) {
		this.action = action;
		this.targetDistance = targetDistance;
		reloadStarted = false;
		reloadCompleted = false;
		damageEvent = false;
		damage = 0;
		shotSequence = -1;
		directionX = 0f;
		directionY = 0f;
	}

	void markReloadStarted() {
		reloadStarted = true;
	}

	void markReloadCompleted() {
		reloadCompleted = true;
	}

	void emitDamage(int damage,
					int shotSequence,
					float directionX,
					float directionY) {
		action = Action.FIRE;
		damageEvent = true;
		this.damage = damage;
		this.shotSequence = shotSequence;
		this.directionX = directionX;
		this.directionY = directionY;
	}

	void action(Action action) {
		this.action = action;
	}

	public Action action() {
		return action;
	}

	public boolean reloadStarted() {
		return reloadStarted;
	}

	public boolean reloadCompleted() {
		return reloadCompleted;
	}

	public boolean hasDamageEvent() {
		return damageEvent;
	}

	public int damage() {
		return damage;
	}

	public int shotSequence() {
		return shotSequence;
	}

	public float directionX() {
		return directionX;
	}

	public float directionY() {
		return directionY;
	}

	public float targetDistance() {
		return targetDistance;
	}
}
