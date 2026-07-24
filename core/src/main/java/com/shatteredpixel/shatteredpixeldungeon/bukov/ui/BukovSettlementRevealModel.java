package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Renderer-independent, token-timed settlement reveal.
 *
 * One clock drives both row disclosure and the rolling total, so frame rate
 * cannot change the order or final economic value. The caller supplies the
 * duration selected from the shared motion tokens, including the shortened
 * reduced-motion duration.
 */
public final class BukovSettlementRevealModel {

	private final int rowCount;
	private final long totalValue;
	private final int durationMs;
	private float elapsedSeconds;
	private boolean skipped;

	public BukovSettlementRevealModel(
			int rowCount,
			long totalValue,
			int ritualDurationMs,
			int reducedMotionDurationMs,
			boolean reduceMotion) {
		if (rowCount < 0
				|| totalValue < 0L
				|| ritualDurationMs <= 0
				|| reducedMotionDurationMs <= 0) {
			throw new IllegalArgumentException(
					"row count and total value must be non-negative"
							+ " and motion durations must be positive");
		}
		this.rowCount = rowCount;
		this.totalValue = totalValue;
		durationMs = reduceMotion
				? reducedMotionDurationMs
				: ritualDurationMs;
	}

	public void advance(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (complete()) return;
		elapsedSeconds = Math.min(
				durationSeconds(),
				elapsedSeconds + deltaSeconds);
	}

	public void skip() {
		skipped = true;
	}

	public boolean complete() {
		return skipped || elapsedSeconds >= durationSeconds();
	}

	public int visibleRows() {
		if (rowCount == 0) return 0;
		if (complete()) return rowCount;
		return Math.min(
				rowCount,
				(int) Math.floor(progress() * rowCount + 0.0001f));
	}

	public long displayedValue() {
		if (complete()) return totalValue;
		return Math.round(totalValue * progress());
	}

	public boolean stampVisible() {
		return complete();
	}

	private float progress() {
		if (skipped) return 1f;
		return Math.min(
				1f,
				elapsedSeconds / durationSeconds());
	}

	private float durationSeconds() {
		return durationMs / 1000f;
	}
}
