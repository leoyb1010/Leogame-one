package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/**
 * Fixed-capacity, renderer-independent scheduler for small UI motions.
 *
 * Callers own the meaning of a channel and apply {@link #value(Object, int)}
 * to their views. Durations are supplied by the caller, including any
 * reduced-motion substitution. The scheduler deliberately has no dependency
 * on settings, rendering, or wall-clock time.
 */
public final class BukovUiMotionScheduler {

	public static final int MAX_ACTIVE_MOTIONS = 8;

	private final Object[] owners =
			new Object[MAX_ACTIVE_MOTIONS];
	private final int[] channels =
			new int[MAX_ACTIVE_MOTIONS];
	private final float[] starts =
			new float[MAX_ACTIVE_MOTIONS];
	private final float[] ends =
			new float[MAX_ACTIVE_MOTIONS];
	private final float[] values =
			new float[MAX_ACTIVE_MOTIONS];
	private final float[] elapsedSeconds =
			new float[MAX_ACTIVE_MOTIONS];
	private final float[] durationSeconds =
			new float[MAX_ACTIVE_MOTIONS];
	private final long[] startOrders =
			new long[MAX_ACTIVE_MOTIONS];
	private final boolean[] active =
			new boolean[MAX_ACTIVE_MOTIONS];

	/*
	 * A slot may be reused immediately after completion or eviction. This
	 * fixed ring keeps the last terminal value queryable without allocating.
	 */
	private final Object[] terminalOwners =
			new Object[MAX_ACTIVE_MOTIONS];
	private final int[] terminalChannels =
			new int[MAX_ACTIVE_MOTIONS];
	private final float[] terminalValues =
			new float[MAX_ACTIVE_MOTIONS];
	private final long[] terminalOrders =
			new long[MAX_ACTIVE_MOTIONS];
	private int nextTerminalIndex;
	private long orderClock;

	/**
	 * Starts a linear motion.
	 *
	 * Restarting the same owner and channel redirects from its current value;
	 * {@code fromValue} is only used when that motion has never been seen.
	 */
	public void start(
			Object owner,
			int channel,
			float fromValue,
			float toValue,
			int durationMs) {
		validateStart(owner, fromValue, toValue, durationMs);

		int slot = findSlot(owner, channel);
		float effectiveStart;
		if (slot >= 0) {
			effectiveStart = values[slot];
		} else {
			int terminal = findTerminal(owner, channel);
			effectiveStart = terminal >= 0
					? terminalValues[terminal]
					: fromValue;
			slot = acquireSlot();
		}

		owners[slot] = owner;
		channels[slot] = channel;
		starts[slot] = effectiveStart;
		ends[slot] = toValue;
		values[slot] = effectiveStart;
		elapsedSeconds[slot] = 0f;
		durationSeconds[slot] = durationMs / 1000f;
		startOrders[slot] = nextOrder();
		active[slot] = true;
	}

	/**
	 * Advances every active motion by simulation time, not frame count.
	 */
	public void update(float deltaSeconds) {
		if (!finite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (deltaSeconds == 0f) return;

		for (int index = 0; index < active.length; index++) {
			if (!active[index]) continue;

			float elapsed = elapsedSeconds[index] + deltaSeconds;
			float duration = durationSeconds[index];
			if (elapsed >= duration) {
				elapsedSeconds[index] = duration;
				values[index] = ends[index];
				active[index] = false;
			} else {
				elapsedSeconds[index] = elapsed;
				float progress = elapsed / duration;
				values[index] = starts[index]
						+ (ends[index] - starts[index]) * progress;
			}
		}
	}

	public int activeCount() {
		int count = 0;
		for (int index = 0; index < active.length; index++) {
			if (active[index]) count++;
		}
		return count;
	}

	public float value(Object owner, int channel) {
		validateOwner(owner);
		int slot = findSlot(owner, channel);
		if (slot >= 0) return values[slot];

		int terminal = findTerminal(owner, channel);
		if (terminal >= 0) return terminalValues[terminal];
		throw new IllegalArgumentException(
				"no motion exists for this owner and channel");
	}

	public boolean isActive(Object owner, int channel) {
		validateOwner(owner);
		int slot = findSlot(owner, channel);
		return slot >= 0 && active[slot];
	}

	/**
	 * Completes an active motion immediately.
	 *
	 * @return true when an active motion was completed
	 */
	public boolean cancelToEnd(Object owner, int channel) {
		validateOwner(owner);
		int slot = findSlot(owner, channel);
		if (slot < 0 || !active[slot]) return false;

		elapsedSeconds[slot] = durationSeconds[slot];
		values[slot] = ends[slot];
		active[slot] = false;
		return true;
	}

	private int acquireSlot() {
		for (int index = 0; index < owners.length; index++) {
			if (owners[index] == null) return index;
		}
		for (int index = 0; index < active.length; index++) {
			if (!active[index]) {
				rememberTerminal(index, values[index]);
				return index;
			}
		}

		int oldest = 0;
		for (int index = 1; index < startOrders.length; index++) {
			if (startOrders[index] < startOrders[oldest]) {
				oldest = index;
			}
		}
		values[oldest] = ends[oldest];
		active[oldest] = false;
		rememberTerminal(oldest, values[oldest]);
		return oldest;
	}

	private void rememberTerminal(int slot, float value) {
		int terminal = nextTerminalIndex;
		terminalOwners[terminal] = owners[slot];
		terminalChannels[terminal] = channels[slot];
		terminalValues[terminal] = value;
		terminalOrders[terminal] = nextOrder();
		nextTerminalIndex =
				(nextTerminalIndex + 1) % MAX_ACTIVE_MOTIONS;
	}

	private int findSlot(Object owner, int channel) {
		for (int index = 0; index < owners.length; index++) {
			if (owners[index] == owner && channels[index] == channel) {
				return index;
			}
		}
		return -1;
	}

	private int findTerminal(Object owner, int channel) {
		int newest = -1;
		for (int index = 0; index < terminalOwners.length; index++) {
			if (terminalOwners[index] == owner
					&& terminalChannels[index] == channel
					&& (newest < 0
					|| terminalOrders[index] > terminalOrders[newest])) {
				newest = index;
			}
		}
		return newest;
	}

	private long nextOrder() {
		orderClock++;
		return orderClock;
	}

	private static void validateStart(
			Object owner,
			float fromValue,
			float toValue,
			int durationMs) {
		validateOwner(owner);
		if (!finite(fromValue) || !finite(toValue)) {
			throw new IllegalArgumentException(
					"motion values must be finite");
		}
		if (durationMs <= 0) {
			throw new IllegalArgumentException(
					"durationMs must be positive");
		}
	}

	private static void validateOwner(Object owner) {
		if (owner == null) {
			throw new IllegalArgumentException(
					"motion owner is required");
		}
	}

	private static boolean finite(float value) {
		return value == value
				&& value != Float.POSITIVE_INFINITY
				&& value != Float.NEGATIVE_INFINITY;
	}
}
