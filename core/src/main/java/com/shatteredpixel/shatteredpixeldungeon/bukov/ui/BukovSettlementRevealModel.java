package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Renderer-independent 600ms settlement reveal.
 *
 * One clock drives both row disclosure and the rolling total, so frame rate
 * cannot change the order or final economic value.
 */
public final class BukovSettlementRevealModel {

	public static final int DURATION_MS = 600;

	private final int rowCount;
	private final long totalValue;
	private float elapsedSeconds;
	private boolean skipped;

	public BukovSettlementRevealModel(int rowCount, long totalValue) {
		if (rowCount < 0 || totalValue < 0L) {
			throw new IllegalArgumentException(
					"row count and total value must be non-negative");
		}
		this.rowCount = rowCount;
		this.totalValue = totalValue;
	}

	public void advance(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (complete()) return;
		elapsedSeconds = Math.min(
				DURATION_MS / 1000f,
				elapsedSeconds + deltaSeconds);
	}

	public void skip() {
		skipped = true;
	}

	public boolean complete() {
		return skipped || elapsedSeconds >= DURATION_MS / 1000f;
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
				elapsedSeconds / (DURATION_MS / 1000f));
	}
}
