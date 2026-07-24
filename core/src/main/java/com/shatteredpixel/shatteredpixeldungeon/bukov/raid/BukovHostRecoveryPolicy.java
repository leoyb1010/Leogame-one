package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/**
 * Pure recovery matrix for the durable raid checkpoint and its host map.
 *
 * Neither document is sufficient to resume by itself: the checkpoint owns
 * raid economy/runtime state, while the host save owns the exact map, actors
 * and heaps.
 */
public final class BukovHostRecoveryPolicy {

	public enum Action {
		CREATE_NEW_HOST,
		RESUME_MATCHED_HOST,
		SETTLE_INTERRUPTED_CHECKPOINT,
		ARCHIVE_ORPHAN_HOST
	}

	private BukovHostRecoveryPolicy() {
	}

	public static Action decide(
			boolean checkpointPresent,
			boolean hostMapPresent) {
		if (checkpointPresent) {
			return hostMapPresent
					? Action.RESUME_MATCHED_HOST
					: Action.SETTLE_INTERRUPTED_CHECKPOINT;
		}
		return hostMapPresent
				? Action.ARCHIVE_ORPHAN_HOST
				: Action.CREATE_NEW_HOST;
	}
}
