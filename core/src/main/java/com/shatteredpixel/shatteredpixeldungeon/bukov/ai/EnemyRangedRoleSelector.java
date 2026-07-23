package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

/**
 * Pure deterministic policy for choosing the baseline ranged enemy.
 */
public final class EnemyRangedRoleSelector {

	/**
	 * Returns true when the candidate should replace the current selection.
	 *
	 * Host ranged types are preferred, then the floor-one Snake fallback, then
	 * the lowest stable id of any remaining enemy.
	 */
	public static boolean shouldReplace(int currentStableId,
										String currentClassName,
										int candidateStableId,
										String candidateClassName) {
		if (candidateClassName == null) {
			throw new IllegalArgumentException("candidateClassName is required");
		}
		if (currentClassName == null) {
			return true;
		}
		int currentPriority = priority(currentClassName);
		int candidatePriority = priority(candidateClassName);
		return candidatePriority < currentPriority
				|| candidatePriority == currentPriority
				&& candidateStableId < currentStableId;
	}

	static int priority(String className) {
		if ("GnollTrickster".equals(className)
				|| "DM100".equals(className)) {
			return 0;
		}
		if ("Snake".equals(className)) {
			return 1;
		}
		return 2;
	}

	private EnemyRangedRoleSelector() {
	}
}
