package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Fixed-capacity player-noise history used by realtime enemy hearing.
 *
 * Events and slots are allocated once. Saturation discards the oldest noise,
 * preserving the newest fixed-step evidence without touching gameplay state.
 */
public final class PlayerSoundEventBuffer {

	public static final int DEFAULT_CAPACITY = 16;

	public static final class Event {
		private boolean active;
		private int sequence;
		private float x;
		private float y;
		private float radius;
		private float remainingSeconds;

		public boolean active() {
			return active;
		}

		public int sequence() {
			return sequence;
		}

		public float x() {
			return x;
		}

		public float y() {
			return y;
		}

		public float radius() {
			return radius;
		}

		public float remainingSeconds() {
			return remainingSeconds;
		}
	}

	public static final class Snapshot implements Bundlable {

		private static final String NEXT_SEQUENCE = "next_sequence";
		private static final String SEQUENCES = "sequences";
		private static final String X = "x";
		private static final String Y = "y";
		private static final String RADII = "radii";
		private static final String REMAINING = "remaining";
		private static final String DROPPED = "dropped";
		private static final String LEGACY_SEQUENCE = "sequence";
		private static final String LEGACY_X = "sound_x";
		private static final String LEGACY_Y = "sound_y";
		private static final String LEGACY_RADIUS = "sound_radius";
		private static final String LEGACY_REMAINING = "sound_remaining";

		private int nextSequence = 1;
		private int[] sequences = new int[0];
		private float[] x = new float[0];
		private float[] y = new float[0];
		private float[] radii = new float[0];
		private float[] remaining = new float[0];
		private long dropped;

		public Snapshot() {
			// Required by Bundle reflection.
		}

		public static Snapshot legacySingleSlot(
				int sequence,
				float x,
				float y,
				float radius,
				float remainingSeconds) {
			Snapshot snapshot = new Snapshot();
			if (sequence > 0 && radius > 0f && remainingSeconds > 0f) {
				snapshot.nextSequence = sequence == Integer.MAX_VALUE
						? 1 : sequence + 1;
				snapshot.sequences = new int[]{sequence};
				snapshot.x = new float[]{x};
				snapshot.y = new float[]{y};
				snapshot.radii = new float[]{radius};
				snapshot.remaining = new float[]{remainingSeconds};
			}
			return snapshot;
		}

		public int size() {
			return sequences.length;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put(NEXT_SEQUENCE, nextSequence);
			bundle.put(SEQUENCES, sequences);
			bundle.put(X, x);
			bundle.put(Y, y);
			bundle.put(RADII, radii);
			bundle.put(REMAINING, remaining);
			bundle.put(DROPPED, dropped);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			if (!bundle.contains(SEQUENCES)
					&& bundle.contains(LEGACY_SEQUENCE)) {
				Snapshot legacy = legacySingleSlot(
						bundle.getInt(LEGACY_SEQUENCE),
						bundle.getFloat(LEGACY_X),
						bundle.getFloat(LEGACY_Y),
						bundle.getFloat(LEGACY_RADIUS),
						bundle.getFloat(LEGACY_REMAINING));
				copyFrom(legacy);
				return;
			}
			nextSequence = bundle.getInt(NEXT_SEQUENCE);
			if (nextSequence <= 0) nextSequence = 1;
			sequences = bundle.getIntArray(SEQUENCES);
			x = bundle.getFloatArray(X);
			y = bundle.getFloatArray(Y);
			radii = bundle.getFloatArray(RADII);
			remaining = bundle.getFloatArray(REMAINING);
			dropped = bundle.getLong(DROPPED);
			validate();
		}

		private void copyFrom(Snapshot source) {
			nextSequence = source.nextSequence;
			sequences = source.sequences;
			x = source.x;
			y = source.y;
			radii = source.radii;
			remaining = source.remaining;
			dropped = source.dropped;
		}

		private void validate() {
			int count = sequences == null ? 0 : sequences.length;
			if (x == null || y == null || radii == null || remaining == null
					|| x.length != count || y.length != count
					|| radii.length != count || remaining.length != count) {
				throw new IllegalStateException(
						"Invalid player sound event snapshot");
			}
			for (int index = 0; index < count; index++) {
				if (sequences[index] <= 0
						|| !validEvent(
								x[index],
								y[index],
								radii[index],
								remaining[index])) {
					throw new IllegalStateException(
							"Invalid player sound event");
				}
			}
		}
	}

	private final Event[] events;
	private int nextSequence = 1;
	private long dropped;

	public PlayerSoundEventBuffer() {
		this(DEFAULT_CAPACITY);
	}

	public PlayerSoundEventBuffer(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		events = new Event[capacity];
		for (int index = 0; index < capacity; index++) {
			events[index] = new Event();
		}
	}

	public int emit(
			float x,
			float y,
			float radius,
			float lifetimeSeconds) {
		if (!validEvent(x, y, radius, lifetimeSeconds)) {
			throw new IllegalArgumentException("invalid player sound event");
		}
		int slot = firstInactiveSlot();
		if (slot < 0) {
			slot = oldestSlot();
			dropped++;
		}
		Event event = events[slot];
		event.active = true;
		event.sequence = nextSequence;
		event.x = x;
		event.y = y;
		event.radius = radius;
		event.remainingSeconds = lifetimeSeconds;
		nextSequence = nextSequence == Integer.MAX_VALUE
				? 1 : nextSequence + 1;
		return event.sequence;
	}

	public void advance(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (deltaSeconds == 0f) return;
		for (Event event : events) {
			if (!event.active) continue;
			event.remainingSeconds = Math.max(
					0f, event.remainingSeconds - deltaSeconds);
			if (event.remainingSeconds == 0f) {
				event.active = false;
			}
		}
	}

	public Event eventAt(int slot) {
		if (slot < 0 || slot >= events.length) {
			throw new IndexOutOfBoundsException("slot=" + slot);
		}
		return events[slot];
	}

	public int capacity() {
		return events.length;
	}

	public int activeCount() {
		int count = 0;
		for (Event event : events) {
			if (event.active) count++;
		}
		return count;
	}

	public int latestSequence() {
		int latest = 0;
		for (Event event : events) {
			if (event.active && event.sequence > latest) {
				latest = event.sequence;
			}
		}
		return latest;
	}

	public long dropped() {
		return dropped;
	}

	public Snapshot snapshot() {
		Snapshot snapshot = new Snapshot();
		int count = activeCount();
		snapshot.nextSequence = nextSequence;
		snapshot.sequences = new int[count];
		snapshot.x = new float[count];
		snapshot.y = new float[count];
		snapshot.radii = new float[count];
		snapshot.remaining = new float[count];
		snapshot.dropped = dropped;
		int output = 0;
		for (Event event : events) {
			if (!event.active) continue;
			snapshot.sequences[output] = event.sequence;
			snapshot.x[output] = event.x;
			snapshot.y[output] = event.y;
			snapshot.radii[output] = event.radius;
			snapshot.remaining[output] = event.remainingSeconds;
			output++;
		}
		return snapshot;
	}

	public void restore(Snapshot snapshot) {
		clear();
		if (snapshot == null) return;
		snapshot.validate();
		nextSequence = snapshot.nextSequence;
		dropped = snapshot.dropped;
		int start = Math.max(0, snapshot.size() - events.length);
		for (int source = start, slot = 0;
				source < snapshot.size(); source++, slot++) {
			Event event = events[slot];
			event.active = true;
			event.sequence = snapshot.sequences[source];
			event.x = snapshot.x[source];
			event.y = snapshot.y[source];
			event.radius = snapshot.radii[source];
			event.remainingSeconds = snapshot.remaining[source];
		}
		if (start > 0) dropped += start;
	}

	public void clear() {
		for (Event event : events) {
			event.active = false;
			event.remainingSeconds = 0f;
		}
		nextSequence = 1;
		dropped = 0L;
	}

	private int firstInactiveSlot() {
		for (int index = 0; index < events.length; index++) {
			if (!events[index].active) return index;
		}
		return -1;
	}

	private int oldestSlot() {
		int oldest = 0;
		for (int index = 1; index < events.length; index++) {
			if (events[index].sequence < events[oldest].sequence) {
				oldest = index;
			}
		}
		return oldest;
	}

	private static boolean validEvent(
			float x,
			float y,
			float radius,
			float remainingSeconds) {
		return BukovNumbers.isFinite(x)
				&& BukovNumbers.isFinite(y)
				&& BukovNumbers.isFinite(radius)
				&& BukovNumbers.isFinite(remainingSeconds)
				&& radius > 0f
				&& remainingSeconds > 0f;
	}
}
