package com.shatteredpixel.shatteredpixeldungeon.sprites.bukov;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;

/**
 * Shared animation contract for original Bukov raid combatants.
 *
 * The shared code is intentionally limited to frame timing. Each archetype
 * owns an independent sheet and remains visually distinguishable at 1x.
 */
public abstract class BukovEnemySprite extends MobSprite {

	private final int bloodColor;

	protected BukovEnemySprite(String asset, int bloodColor) {
		super();
		this.bloodColor = bloodColor;

		texture(asset);
		TextureFilm frames = new TextureFilm(texture, 16, 18);

		idle = new Animation(3, true);
		idle.frames(frames, 0, 0, 1, 0);

		run = new Animation(12, true);
		run.frames(frames, 4, 5, 6, 7);

		attack = new Animation(12, false);
		attack.frames(frames, 2, 3, 0);

		die = new Animation(10, false);
		die.frames(frames, 8, 9, 10);

		play(idle);
	}

	@Override
	public int blood() {
		return bloodColor;
	}
}
