package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import java.io.IOException;

/**
 * Small retry gate for a host-save + raid-checkpoint commit.
 *
 * The caller owns the transaction order. A failed commit remains dirty and is
 * retried by the realtime loop instead of silently accepting an in-memory item
 * or container mutation that was only written to one save surface.
 */
public final class BukovRaidPersistence {

	public interface Commit {
		void persist() throws IOException;
	}

	private static final float RETRY_SECONDS = 1f;

	private final Commit commit;
	private boolean dirty;
	private float retryRemaining;
	private Throwable lastFailure;

	public BukovRaidPersistence(Commit commit) {
		if (commit == null) {
			throw new IllegalArgumentException("commit is required");
		}
		this.commit = commit;
	}

	/**
	 * Marks a gameplay mutation as needing both save surfaces, then immediately
	 * attempts the commit. Failure is retained for an automatic retry.
	 */
	public boolean criticalStateChanged() {
		dirty = true;
		retryRemaining = 0f;
		return flush();
	}

	/**
	 * Retries a failed critical commit from the fixed-step loop.
	 */
	public boolean update(float deltaSeconds) {
		if (!dirty) {
			return true;
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		retryRemaining = Math.max(0f, retryRemaining - deltaSeconds);
		return retryRemaining > 0f ? false : flush();
	}

	public boolean dirty() {
		return dirty;
	}

	public Throwable lastFailure() {
		return lastFailure;
	}

	private boolean flush() {
		try {
			commit.persist();
			dirty = false;
			retryRemaining = 0f;
			lastFailure = null;
			return true;
		} catch (IOException | RuntimeException failure) {
			lastFailure = failure;
			retryRemaining = RETRY_SECONDS;
			return false;
		}
	}
}
