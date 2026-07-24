package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Fixed-capacity ring buffer with no allocations after construction.
 *
 * When saturated, the oldest cosmetic event is discarded so the newest combat
 * feedback remains visible. Gameplay never depends on delivery.
 */
public final class CombatFxEventPool {

	private final CombatFxEvent[] events;
	private int head;
	private int size;
	private long dropped;

	public CombatFxEventPool(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		events = new CombatFxEvent[capacity];
		for (int index = 0; index < capacity; index++) {
			events[index] = new CombatFxEvent();
		}
	}

	public void muzzle(int sourceId,
					   int sequence,
					   boolean hostile,
					   float x,
					   float y,
					   float directionX,
					   float directionY,
					   float intensity) {
		emit(
				CombatFxEvent.Type.MUZZLE_FLASH,
				sourceId,
				sequence,
				hostile,
				x,
				y,
				x + directionX * 0.35f,
				y + directionY * 0.35f,
				intensity
		);
	}

	public void tracer(int sourceId,
					   int sequence,
					   boolean hostile,
					   float fromX,
					   float fromY,
					   float toX,
					   float toY,
					   float intensity) {
		emit(
				CombatFxEvent.Type.TRACER,
				sourceId,
				sequence,
				hostile,
				fromX,
				fromY,
				toX,
				toY,
				intensity
		);
	}

	public void shell(int sourceId,
					  int sequence,
					  boolean hostile,
					  float x,
					  float y,
					  float directionX,
					  float directionY,
					  float intensity) {
		emit(
				CombatFxEvent.Type.SHELL,
				sourceId,
				sequence,
				hostile,
				x,
				y,
				x + directionX,
				y + directionY,
				intensity
		);
	}

	public void impact(int sourceId,
					   int sequence,
					   boolean hostile,
					   float x,
					   float y,
					   float intensity) {
		emit(
				CombatFxEvent.Type.IMPACT,
				sourceId,
				sequence,
				hostile,
				x,
				y,
				x,
				y,
				intensity
		);
	}

	public int drain(CombatFxEvent.Consumer consumer) {
		if (consumer == null) {
			throw new IllegalArgumentException("consumer is required");
		}
		int drained = size;
		while (size > 0) {
			CombatFxEvent event = events[head];
			head = (head + 1) % events.length;
			size--;
			consumer.accept(event);
		}
		return drained;
	}

	public int size() {
		return size;
	}

	public int capacity() {
		return events.length;
	}

	public long dropped() {
		return dropped;
	}

	public void clear() {
		head = 0;
		size = 0;
	}

	private void emit(CombatFxEvent.Type type,
					  int sourceId,
					  int sequence,
					  boolean hostile,
					  float fromX,
					  float fromY,
					  float toX,
					  float toY,
					  float intensity) {
		if (type == null) {
			throw new IllegalArgumentException("type is required");
		}
		requireFinite(fromX, "fromX");
		requireFinite(fromY, "fromY");
		requireFinite(toX, "toX");
		requireFinite(toY, "toY");
		if (intensity < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						intensity)) {
			throw new IllegalArgumentException(
					"intensity must be finite and non-negative"
			);
		}
		if (size == events.length) {
			head = (head + 1) % events.length;
			size--;
			dropped++;
		}
		int tail = (head + size) % events.length;
		events[tail].set(
				type,
				sourceId,
				sequence,
				hostile,
				fromX,
				fromY,
				toX,
				toY,
				intensity
		);
		size++;
	}

	private static void requireFinite(float value, String label) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(label + " must be finite");
		}
	}
}
