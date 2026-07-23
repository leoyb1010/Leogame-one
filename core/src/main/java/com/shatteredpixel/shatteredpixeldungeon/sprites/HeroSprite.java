/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovFacing8;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	private static final int RUN_FRAMERATE	= 20;
	
	private static TextureFilm tiers;
	private static TextureFilm bukovTiers;

	private final boolean bukovOperator;
	private int bukovDirection = BukovFacing8.S.row;
	private boolean bukovAimActive;
	private boolean bukovActionPlaying;
	
	private Animation fly;
	private Animation read;
	private Animation hit;
	private Animation aim;
	private Animation fire;
	private Animation reload;
	private Animation medical;
	private Animation extract;

	public HeroSprite() {
		super();

		bukovOperator = BukovMode.ensureActiveForHostState();
		texture(bukovOperator
				? Assets.Sprites.BUKOV_OPERATOR
				: Dungeon.hero.heroClass.spritesheet());
		updateArmor();
		
		link( Dungeon.hero );

		if (ch.isAlive())
			idle();
		else
			die();
	}

	public void disguise(HeroClass cls){
		if (bukovOperator) {
			return;
		}
		texture( cls.spritesheet() );
		updateArmor();
	}
	
	public void updateArmor() {
		if (bukovOperator) {
			rebuildBukovAnimations();
		} else {
			rebuildClassicAnimations();
		}

		if (Dungeon.hero.isAlive())
			idle();
		else
			die();
	}

	private void rebuildClassicAnimations() {
		TextureFilm film = new TextureFilm(
				tiers(),
				Dungeon.hero.tier(),
				FRAME_WIDTH,
				FRAME_HEIGHT
		);
		
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 8, 9, 10, 11, 12, 11 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		zap = attack.clone();
		
		operate = new Animation( 8, false );
		operate.frames( film, 16, 17, 16, 17 );
		
		fly = new Animation( 1, true );
		fly.frames( film, 18 );

		hit = new Animation( 12, false );
		hit.frames( film, 18, 1, 0 );

		read = new Animation( 20, false );
		read.frames( film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19 );
	}

	private void rebuildBukovAnimations() {
		TextureFilm film = new TextureFilm(
				bukovTiers(),
				bukovDirection,
				FRAME_WIDTH,
				FRAME_HEIGHT
		);

		idle = new Animation( 5, true );
		idle.frames( film, 0, 1 );

		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );

		aim = new Animation( 8, true );
		aim.frames( film, 8, 9 );

		fire = new Animation( 18, false );
		fire.frames( film, 10, 11, 12, 8 );
		attack = fire;
		zap = fire.clone();

		reload = new Animation( 9, false );
		reload.frames( film, 13, 14, 15, 16, 8 );
		operate = reload;

		hit = new Animation( 14, false );
		hit.frames( film, 17, 18, 19, 8 );

		medical = new Animation( 9, false );
		medical.frames( film, 20, 21, 22, 23, 8 );

		die = new Animation( 10, false );
		die.frames( film, 24, 25, 26, 27, 27 );

		extract = new Animation( 8, false );
		extract.frames( film, 28, 29, 30, 31, 28 );
		read = extract;

		fly = aim.clone();
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	@Override
	public void idle() {
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				ch.onOperateComplete();
			}
		};
		play( read );
	}

	/**
	 * Realtime-only firearm animation. It deliberately completes back to idle
	 * without invoking Hero.onAttackComplete(), which belongs to the inherited
	 * turn scheduler.
	 */
	public synchronized void firearmFire(int targetCell, final Callback callback) {
		if (!bukovOperator) {
			attack(targetCell, callback);
			return;
		}
		faceBukovTarget(ch.pos, targetCell);
		playBukovAction(fire, targetCell, 1, callback);
	}

	public void firearmFire(int targetCell) {
		firearmFire(targetCell, null);
	}

	/**
	 * Realtime reload/operate animation, also isolated from host turn timing.
	 */
	public synchronized void reloadFirearm(int targetCell, final Callback callback) {
		if (!bukovOperator) {
			operate(targetCell, callback);
			return;
		}
		faceBukovTarget(ch.pos, targetCell);
		playBukovAction(reload, targetCell, 2, callback);
	}

	public void reloadFirearm(int targetCell) {
		reloadFirearm(targetCell, null);
	}

	public synchronized void hitReaction(final Callback callback) {
		if (!bukovOperator) {
			if (callback != null) callback.call();
			return;
		}
		playBukovAction(hit, ch.pos, 3, callback);
	}

	public void hitReaction() {
		hitReaction(null);
	}

	public synchronized void extractionRadio(final Callback callback) {
		if (!bukovOperator) {
			if (callback != null) callback.call();
			return;
		}
		playBukovAction(extract, ch.pos, 2, callback);
	}

	public void extractionRadio() {
		extractionRadio(null);
	}

	public synchronized void medicalUse() {
		if (!bukovOperator) {
			return;
		}
		add(State.HEALING);
		playBukovAction(medical, ch.pos, 2, null);
	}

	public synchronized void medicalFinished() {
		if (bukovOperator) {
			remove(State.HEALING);
		}
	}

	public boolean usesBukovOperator() {
		return bukovOperator;
	}

	private void playBukovAction(
			final Animation animation,
			int targetCell,
			int priority,
			final Callback callback ) {
		final Callback completion = new Callback() {
			@Override
			public void call() {
				bukovActionPlaying = false;
				if (callback != null) {
					callback.call();
				}
			}
		};
		if (playRealtimeAction(animation, targetCell, priority, completion)) {
			bukovActionPlaying = true;
		}
	}

	public void setBukovRealtimeOrientation(
			float moveX, float moveY, float aimX, float aimY ) {
		if (!bukovOperator) {
			return;
		}
		bukovAimActive = aimX != 0f || aimY != 0f;
		float facingX = bukovAimActive ? aimX : moveX;
		float facingY = bukovAimActive ? aimY : moveY;
		if (facingX != 0f || facingY != 0f) {
			setBukovDirection(BukovFacing8.resolve(facingX, facingY).row);
		}
	}

	@Override
	public void setRealtimeMoving(boolean moving) {
		super.setRealtimeMoving(moving);
		if (bukovOperator && bukovAimActive && !moving
				&& !bukovActionPlaying && curAnim != die) {
			play(aim);
		}
	}

	@Override
	public void turnTo(int from, int to) {
		if (!bukovOperator || Dungeon.level == null) {
			super.turnTo(from, to);
			return;
		}
		faceBukovTarget(from, to);
		flipHorizontal = false;
	}

	private void faceBukovTarget(int from, int to) {
		int width = Dungeon.level.width();
		int dx = to % width - from % width;
		int dy = to / width - from / width;
		if (dx != 0 || dy != 0) {
			setBukovDirection(BukovFacing8.resolve(dx, dy).row);
		}
	}

	private void setBukovDirection(int direction) {
		if (direction == bukovDirection) {
			return;
		}
		bukovDirection = direction;
		rebuildBukovAnimations();
		flipHorizontal = false;
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//Does nothing.

		/*
		 * This is both for visual clarity, and also for content ratings regarding violence
		 * towards human characters. The heroes are the only human or human-like characters which
		 * participate in combat, so removing all blood associated with them is a simple way to
		 * reduce the violence rating of the game.
		 */
	}

	@Override
	public void update() {
		sleeping = ch.isAlive() && ((Hero)ch).resting;
		
		super.update();
	}
	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
	}
	
	public static TextureFilm tiers() {
		if (tiers == null) {
			SmartTexture texture = TextureCache.get( Assets.Sprites.ROGUE );
			tiers = new TextureFilm( texture, texture.width, FRAME_HEIGHT );
		}
		
		return tiers;
	}

	private static TextureFilm bukovTiers() {
		if (bukovTiers == null) {
			SmartTexture texture = TextureCache.get(Assets.Sprites.BUKOV_OPERATOR);
			bukovTiers = new TextureFilm(texture, texture.width, FRAME_HEIGHT);
		}
		return bukovTiers;
	}

	public static Image avatar( Hero hero ){
		if (BukovMode.active()) {
			return avatar(
					Assets.Sprites.BUKOV_OPERATOR,
					BukovFacing8.S.row,
					bukovTiers()
			);
		}
		if (hero.buff(HeroDisguise.class) != null){
			return avatar(hero.buff(HeroDisguise.class).getDisguise(), hero.tier());
		} else {
			return avatar(hero.heroClass, hero.tier());
		}
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {
		return avatar(cl.spritesheet(), armorTier);
	}

	private static Image avatar(String spritesheet, int armorTier) {
		return avatar(spritesheet, armorTier, tiers());
	}

	private static Image avatar(String spritesheet, int armorTier, TextureFilm tierFilm) {
		RectF patch = tierFilm.get( armorTier );
		Image avatar = new Image(spritesheet);
		RectF frame = avatar.texture.uvRect( 1, 0, FRAME_WIDTH, FRAME_HEIGHT );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );
		
		return avatar;
	}
}
