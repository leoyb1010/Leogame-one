package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Mutable pooled combat feedback event.
 *
 * Consumers must read an event synchronously and must not retain its reference
 * after {@link CombatFxEventPool#drain(Consumer)} returns.
 */
public final class CombatFxEvent {

	public enum Type {
		MUZZLE_FLASH,
		SHELL,
		TRACER,
		IMPACT,
		BLOOD_MIST,
		BULLET_MARK,
		EXPLOSION
	}

	public interface Consumer {
		void accept(CombatFxEvent event);
	}

	private Type type;
	private int sourceId;
	private int sequence;
	private boolean hostile;
	private float fromX;
	private float fromY;
	private float toX;
	private float toY;
	private float intensity;

	void set(Type type,
			 int sourceId,
			 int sequence,
			 boolean hostile,
			 float fromX,
			 float fromY,
			 float toX,
			 float toY,
			 float intensity) {
		this.type = type;
		this.sourceId = sourceId;
		this.sequence = sequence;
		this.hostile = hostile;
		this.fromX = fromX;
		this.fromY = fromY;
		this.toX = toX;
		this.toY = toY;
		this.intensity = intensity;
	}

	public Type type() {
		return type;
	}

	public int sourceId() {
		return sourceId;
	}

	public int sequence() {
		return sequence;
	}

	public boolean hostile() {
		return hostile;
	}

	public float fromX() {
		return fromX;
	}

	public float fromY() {
		return fromY;
	}

	public float toX() {
		return toX;
	}

	public float toY() {
		return toY;
	}

	public float intensity() {
		return intensity;
	}
}
