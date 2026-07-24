package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

/** Optional boss White Line: tall white raincoat, black face and red eye. */
public final class BukovWhiteLineSprite extends BukovEnemySprite {

	private final Animation shieldPhase;
	private final Animation decoyPhase;
	private final Animation overloadPhase;
	private final Animation vulnerablePhase;
	private int encounterVisual;

	public BukovWhiteLineSprite() {
		super(Assets.Sprites.BUKOV_WHITE_LINE, 0xFF3F4545,
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
	}

	/**
	 * Presentation-only mirror of the encounter state. No boss rules live in
	 * this class; it only makes all three phases and the damage window visible.
	 */
	public void setEncounterVisual(int phase, boolean vulnerable) {
		if (curAnim == die || curAnim == attack
				|| curAnim == hit || curAnim == special) {
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
