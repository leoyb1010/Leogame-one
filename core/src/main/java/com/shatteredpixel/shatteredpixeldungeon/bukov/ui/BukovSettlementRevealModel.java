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
	private final int stampDurationMs;
	private final boolean reduceMotion;
	private float elapsedSeconds;
	private float stampElapsedSeconds;
	private boolean skipped;

	public BukovSettlementRevealModel(
			int rowCount,
			long totalValue,
			int ritualDurationMs,
			int reducedMotionDurationMs,
			boolean reduceMotion) {
		this(
				rowCount,
				totalValue,
				ritualDurationMs,
				reducedMotionDurationMs,
				320,
				180,
				reduceMotion);
	}

	public BukovSettlementRevealModel(
			int rowCount,
			long totalValue,
			int ritualDurationMs,
			int reducedMotionDurationMs,
			int stampDurationMs,
			int reducedMotionStampDurationMs,
			boolean reduceMotion) {
		if (rowCount < 0
				|| totalValue < 0L
				|| ritualDurationMs <= 0
				|| reducedMotionDurationMs <= 0
				|| stampDurationMs <= 0
				|| reducedMotionStampDurationMs <= 0) {
			throw new IllegalArgumentException(
					"row count and total value must be non-negative"
							+ " and motion durations must be positive");
		}
		this.rowCount = rowCount;
		this.totalValue = totalValue;
		this.reduceMotion = reduceMotion;
		durationMs = reduceMotion
				? reducedMotionDurationMs
				: ritualDurationMs;
		this.stampDurationMs = reduceMotion
				? reducedMotionStampDurationMs
				: stampDurationMs;
	}

	public void advance(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (stampAnimationComplete()) return;
		float revealRemaining = Math.max(
				0f,
				durationSeconds() - elapsedSeconds);
		float revealDelta = Math.min(deltaSeconds, revealRemaining);
		elapsedSeconds += revealDelta;
		if (complete()) {
			stampElapsedSeconds = Math.min(
					stampDurationSeconds(),
					stampElapsedSeconds + deltaSeconds - revealDelta);
		}
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

	public float stampAlpha() {
		if (!stampVisible()) return 0f;
		if (skipped) return 1f;
		float progress = stampProgress();
		return 1f - (float) Math.pow(1f - progress, 3);
	}

	public float stampScale() {
		if (!stampVisible() || skipped || reduceMotion) return 1f;
		float progress = stampProgress();
		float eased = 1f - (float) Math.pow(1f - progress, 4);
		return 1.3f - 0.3f * eased;
	}

	public boolean stampAnimationComplete() {
		return skipped
				|| (complete()
				&& stampElapsedSeconds >= stampDurationSeconds());
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

	private float stampProgress() {
		return Math.min(
				1f,
				stampElapsedSeconds / stampDurationSeconds());
	}

	private float stampDurationSeconds() {
		return stampDurationMs / 1000f;
	}
}
