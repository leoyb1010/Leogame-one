package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;

/** Optional boss White Line: tall white raincoat, black face and red eye. */
public final class BukovWhiteLineSprite extends BukovEnemySprite {

	public static final int PHASE_TRANSITION_FRAME_COUNT = 6;
	public static final float WEAK_POINT_SLOW_MOTION_SECONDS = 0.2f;
	public static final float WEAK_POINT_SLOW_MOTION_SCALE = 0.3f;

	private final Animation shieldPhase;
	private final Animation decoyPhase;
	private final Animation overloadPhase;
	private final Animation vulnerablePhase;
	private final Animation phaseTransition;
	private int encounterVisual;
	private float weakPointSlowMotionRemaining;

	public BukovWhiteLineSprite() {
		super(Assets.Sprites.BUKOV_WHITE_LINE,
				"combat.enemy.blood.whiteLine",
				SpecialAction.PHASE_CAST);
		TextureFilm frames = new TextureFilm(texture, 16, 18);

		shieldPhase = new Animation(5, true);
		shieldPhase.frames(frames, 16, 17);

		decoyPhase = new Animation(4, true);
		decoyPhase.frames(frames, 18, 0, 18, 1);

		overloadPhase = new Animation(7, true);
		overloadPhase.frames(frames, 19, 1, 19, 0);

		vulnerablePhase = new Animation(8, true);
		vulnerablePhase.frames(frames, 20, 20, 0);

		/*
		 * A phase break owns a dedicated, non-looping six-frame bridge. The
		 * encounter state remains authoritative in the realtime world; this
		 * film only carries the sprite from one readable silhouette to the
		 * next.
		 */
		phaseTransition = new Animation(18, false);
		phaseTransition.frames(frames, 16, 17, 18, 19, 20, 0);
	}

	@Override
	public boolean realtimePhaseCast(int targetCell) {
		return playRealtimeAction(phaseTransition, targetCell, 2, null);
	}

	/**
	 * Presentation-only mirror of the encounter state. No boss rules live in
	 * this class; it only makes all three phases and the damage window visible.
	 */
	public void setEncounterVisual(int phase, boolean vulnerable) {
		if (curAnim == die || curAnim == attack
				|| curAnim == hit || curAnim == special
				|| curAnim == phaseTransition) {
			return;
		}
		int requested = vulnerable ? 10 : phase;
		Animation requestedAnimation = animationFor(requested);
		if (requested == encounterVisual && curAnim == requestedAnimation) {
			return;
		}
		encounterVisual = requested;
		play(requestedAnimation);
	}

	/**
	 * Starts the authored weak-point emphasis without changing simulation time.
	 * Only this boss sprite's animation clock is scaled; movement, input,
	 * damage, AI, RNG and the realtime fixed-step continue at full speed.
	 */
	public void beginWeakPointSlowMotion() {
		weakPointSlowMotionRemaining = Math.max(
				weakPointSlowMotionRemaining,
				WEAK_POINT_SLOW_MOTION_SECONDS);
	}

	@Override
	protected synchronized void updateAnimation() {
		float realElapsed = Game.elapsed;
		if (weakPointSlowMotionRemaining <= 0f || !(realElapsed > 0f)) {
			super.updateAnimation();
			return;
		}
		float slowedRealSeconds = Math.min(
				realElapsed,
				weakPointSlowMotionRemaining);
		weakPointSlowMotionRemaining = Math.max(
				0f,
				weakPointSlowMotionRemaining - slowedRealSeconds);
		float presentationElapsed = scaledPresentationElapsed(
				realElapsed,
				slowedRealSeconds);
		Game.elapsed = presentationElapsed;
		try {
			super.updateAnimation();
		} finally {
			Game.elapsed = realElapsed;
		}
	}

	float weakPointSlowMotionRemaining() {
		return weakPointSlowMotionRemaining;
	}

	static float scaledPresentationElapsed(
			float realElapsed,
			float slowedRealSeconds) {
		return slowedRealSeconds * WEAK_POINT_SLOW_MOTION_SCALE
				+ (realElapsed - slowedRealSeconds);
	}

	private Animation animationFor(int requested) {
		switch (requested) {
			case 1:
				return shieldPhase;
			case 2:
				return decoyPhase;
			case 3:
				return overloadPhase;
			case 10:
				return vulnerablePhase;
			default:
				return idle;
		}
	}
}
