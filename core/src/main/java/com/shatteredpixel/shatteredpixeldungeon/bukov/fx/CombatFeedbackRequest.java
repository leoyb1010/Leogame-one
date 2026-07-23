package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

/**
 * Value-only snapshot created after simulation has committed an outcome.
 */
public final class CombatFeedbackRequest {

	public final CombatFeedbackType type;
	public final float distanceTiles;
	public final float intensityScale;

	public CombatFeedbackRequest(CombatFeedbackType type,
								 float distanceTiles,
								 float intensityScale) {
		if (type == null) {
			throw new IllegalArgumentException("type is required");
		}
		if (distanceTiles < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						distanceTiles)) {
			throw new IllegalArgumentException("invalid distanceTiles");
		}
		if (intensityScale < 0f
				|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						intensityScale)) {
			throw new IllegalArgumentException("invalid intensityScale");
		}
		this.type = type;
		this.distanceTiles = distanceTiles;
		this.intensityScale = intensityScale;
	}
}
