package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

/** Authored long-term objective and its one-time currency reward. */
public final class BukovLongTermContractDefinition {

	public enum Metric {
		SUCCESSFUL_EXTRACTIONS,
		EXTRACTED_VALUE,
		KILLS,
		RAIDS_COMPLETED
	}

	public final String id;
	public final String title;
	public final String objective;
	public final Metric metric;
	public final long target;
	public final long rewardCurrency;

	BukovLongTermContractDefinition(
			String id,
			String title,
			String objective,
			Metric metric,
			long target,
			long rewardCurrency) {
		if (!text(id) || !text(title) || !text(objective) || metric == null) {
			throw new IllegalArgumentException(
					"contract identity, objective and metric are required");
		}
		if (target <= 0L || rewardCurrency <= 0L) {
			throw new IllegalArgumentException(
					"contract target and reward must be positive");
		}
		this.id = id;
		this.title = title;
		this.objective = objective;
		this.metric = metric;
		this.target = target;
		this.rewardCurrency = rewardCurrency;
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
