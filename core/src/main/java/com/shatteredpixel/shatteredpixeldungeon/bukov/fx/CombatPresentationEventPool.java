package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Fixed-capacity cosmetic queue. Saturation drops the oldest pulse; gameplay
 * state never observes queue contents or delivery.
 */
public final class CombatPresentationEventPool {

	private final CombatPresentationEvent[] events;
	private int head;
	private int size;
	private long dropped;

	public CombatPresentationEventPool(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		events = new CombatPresentationEvent[capacity];
		for (int index = 0; index < capacity; index++) {
			events[index] = new CombatPresentationEvent();
		}
	}

	public void emit(CombatPresentationEvent.Type type,
					 int sourceId,
					 int targetId,
					 int sourceCell,
					 int targetCell,
					 CombatFeedbackType feedbackType,
					 float intensity) {
		emit(
				type,
				sourceId,
				targetId,
				sourceCell,
				targetCell,
				feedbackType,
				intensity,
				0f);
	}

	public void emit(CombatPresentationEvent.Type type,
					 int sourceId,
					 int targetId,
					 int sourceCell,
					 int targetCell,
					 CombatFeedbackType feedbackType,
					 float intensity,
					 float durationSeconds) {
		if (type == null) {
			throw new IllegalArgumentException("type is required");
		}
		if (intensity < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(intensity)) {
			throw new IllegalArgumentException(
					"intensity must be finite and non-negative");
		}
		if (durationSeconds < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(durationSeconds)) {
			throw new IllegalArgumentException(
					"durationSeconds must be finite and non-negative");
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
				targetId,
				sourceCell,
				targetCell,
				feedbackType,
				intensity,
				durationSeconds);
		size++;
	}

	public int drain(CombatPresentationEvent.Consumer consumer) {
		if (consumer == null) {
			throw new IllegalArgumentException("consumer is required");
		}
		int drained = size;
		while (size > 0) {
			CombatPresentationEvent event = events[head];
			head = (head + 1) % events.length;
			size--;
			consumer.accept(event);
		}
		return drained;
	}

	public int size() {
		return size;
	}

	public long dropped() {
		return dropped;
	}

	public void clear() {
		head = 0;
		size = 0;
	}
}
