package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Rolling presentation-only budget for hitstop. Requests inside the current
 * pause pay only for their extension, so the highest tier wins without
 * stacking.
 */
final class HitstopBudget {

	static final int MAXIMUM_PER_MINUTE_MS = 600;
	private static final double WINDOW_SECONDS = 60d;
	private static final int HISTORY_CAPACITY = 128;

	private final double[] timestamps = new double[HISTORY_CAPACITY];
	private final int[] durationsMs = new int[HISTORY_CAPACITY];
	private int head;
	private int size;
	private int rollingTotalMs;
	private double elapsedSeconds;
	private float activeRemainingMs;

	int request(int requestedMs) {
		if (requestedMs <= 0) return 0;
		prune();
		int activeMs = Math.max(0, (int)Math.ceil(activeRemainingMs));
		int extensionMs = Math.max(0, requestedMs - activeMs);
		if (extensionMs > 0) {
			int acceptedMs = Math.min(
					extensionMs,
					Math.max(0, MAXIMUM_PER_MINUTE_MS - rollingTotalMs));
			if (acceptedMs > 0 && record(acceptedMs)) {
				activeRemainingMs += acceptedMs;
			}
		}
		return Math.min(
				requestedMs,
				Math.max(0, (int)Math.ceil(activeRemainingMs)));
	}

	void advance(float seconds) {
		if (!(seconds > 0f) || Float.isInfinite(seconds)
				|| Float.isNaN(seconds)) {
			return;
		}
		elapsedSeconds += seconds;
		activeRemainingMs = Math.max(
				0f,
				activeRemainingMs - seconds * 1000f);
		prune();
	}

	int rollingTotalMs() {
		prune();
		return rollingTotalMs;
	}

	void clear() {
		head = 0;
		size = 0;
		rollingTotalMs = 0;
		elapsedSeconds = 0d;
		activeRemainingMs = 0f;
	}

	private boolean record(int durationMs) {
		if (size > 0) {
			int tail = (head + size - 1) % HISTORY_CAPACITY;
			if (Math.abs(timestamps[tail] - elapsedSeconds) < 0.000001d) {
				durationsMs[tail] += durationMs;
				rollingTotalMs += durationMs;
				return true;
			}
		}
		if (size == HISTORY_CAPACITY) return false;
		int tail = (head + size) % HISTORY_CAPACITY;
		timestamps[tail] = elapsedSeconds;
		durationsMs[tail] = durationMs;
		size++;
		rollingTotalMs += durationMs;
		return true;
	}

	private void prune() {
		double cutoff = elapsedSeconds - WINDOW_SECONDS;
		while (size > 0 && timestamps[head] <= cutoff) {
			rollingTotalMs -= durationsMs[head];
			durationsMs[head] = 0;
			head = (head + 1) % HISTORY_CAPACITY;
			size--;
		}
	}
}
