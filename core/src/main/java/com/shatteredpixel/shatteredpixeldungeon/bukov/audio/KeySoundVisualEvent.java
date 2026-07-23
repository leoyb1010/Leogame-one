package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

public final class KeySoundVisualEvent {

	public enum Direction {
		N, NE, E, SE, S, SW, W, NW
	}

	public enum DistanceBand {
		NEAR, MID, FAR
	}

	private boolean visible;
	private SoundCategory category;
	private Direction direction;
	private DistanceBand distanceBand;
	private float strength;
	private int sequence;
	private float remainingSeconds;

	void set(boolean visible,
			 SoundCategory category,
			 Direction direction,
			 DistanceBand distanceBand,
			 float strength) {
		this.visible = visible;
		this.category = category;
		this.direction = direction;
		this.distanceBand = distanceBand;
		this.strength = strength;
		if (!visible) {
			remainingSeconds = 0f;
		}
	}

	public void activate(int sequence, float lifetimeSeconds) {
		if (!visible || lifetimeSeconds <= 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						lifetimeSeconds)) {
			visible = false;
			remainingSeconds = 0f;
			return;
		}
		this.sequence = sequence;
		remainingSeconds = lifetimeSeconds;
	}

	public void advance(float deltaSeconds) {
		if (!visible || deltaSeconds <= 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						deltaSeconds)) {
			return;
		}
		remainingSeconds = Math.max(0f, remainingSeconds - deltaSeconds);
		if (remainingSeconds == 0f) {
			visible = false;
		}
	}

	public void copyTo(KeySoundVisualEvent target) {
		if (target == null) {
			throw new IllegalArgumentException("target is required");
		}
		target.visible = visible;
		target.category = category;
		target.direction = direction;
		target.distanceBand = distanceBand;
		target.strength = strength;
		target.sequence = sequence;
		target.remainingSeconds = remainingSeconds;
	}

	public boolean visible() {
		return visible;
	}

	public SoundCategory category() {
		return category;
	}

	public Direction direction() {
		return direction;
	}

	public DistanceBand distanceBand() {
		return distanceBand;
	}

	public float strength() {
		return strength;
	}

	public int sequence() {
		return sequence;
	}

	public float remainingSeconds() {
		return remainingSeconds;
	}
}
