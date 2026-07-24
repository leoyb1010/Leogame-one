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
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovOperatorPose;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	private static final int RUN_FRAMERATE	= 12;
	
	private static TextureFilm tiers;
	private static TextureFilm bukovTiers;
	private static TextureFilm bukovLowerTiers;

	private final boolean bukovOperator;
	private final BukovOperatorPose bukovPose = new BukovOperatorPose();
	private int bukovDirection = BukovFacing8.S.row;
	private int bukovLocomotionDirection = BukovFacing8.S.row;
	private boolean bukovAimActive;
	private boolean bukovActionPlaying;
	private boolean bukovMoving;
	private boolean bukovReloading;
	private MovieClip bukovLowerLayer;
	private Image bukovUpperLayer;
	private Animation bukovLowerIdle;
	private Animation bukovLowerRun;
	private int bukovUpperRenderedDirection = -1;
	private int bukovUpperRenderedFrame = -1;
	
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
		if (bukovOperator) {
			initializeBukovLayers();
		}
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
				BukovFacing8.S.row,
				FRAME_WIDTH,
				FRAME_HEIGHT
		);

		idle = new Animation( 8, true );
		idle.frames( film, 0, 1, 0, 1 );

		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7, 4, 3 );

		aim = new Animation( 1, true );
		aim.frames( film, 8 );

		fire = new Animation( 30, false );
		fire.frames( film, 10, 11, 12, 8 );
		attack = fire;
		zap = fire.clone();

		reload = new Animation( 12, false );
		reload.frames( film, 13, 14, 15, 16, 15, 8 );
		operate = reload;

		hit = new Animation( 30, false );
		hit.frames( film, 17, 18 );

		medical = new Animation( 9, false );
		medical.frames( film, 20, 21, 22, 23, 8 );

		die = new Animation( 12, false );
		die.frames( film, 24, 25, 26, 27, 27, 27 );

		extract = new Animation( 8, false );
		extract.frames( film, 28, 29, 30, 31, 28 );
		read = extract;

		fly = aim.clone();
		rebuildBukovLocomotionAnimations();
	}

	private void initializeBukovLayers() {
		bukovLowerLayer = new MovieClip(Assets.Sprites.BUKOV_OPERATOR_LOWER);
		bukovUpperLayer = new Image(Assets.Sprites.BUKOV_OPERATOR_UPPER);
		bukovLowerLayer.texture.filter(
				SmartTexture.NEAREST, SmartTexture.NEAREST);
		bukovUpperLayer.texture.filter(
				SmartTexture.NEAREST, SmartTexture.NEAREST);
	}

	private void rebuildBukovLocomotionAnimations() {
		TextureFilm film = new TextureFilm(
				bukovLowerTiers(),
				bukovLocomotionDirection,
				FRAME_WIDTH,
				FRAME_HEIGHT
		);
		bukovLowerIdle = new Animation(8, true);
		bukovLowerIdle.frames(film, 0, 1, 0, 1);
		bukovLowerRun = new Animation(RUN_FRAMERATE, true);
		bukovLowerRun.frames(film, 2, 3, 4, 5, 6, 7, 4, 3);
		if (bukovLowerLayer != null) {
			bukovLowerLayer.play(
					bukovMoving ? bukovLowerRun : bukovLowerIdle);
		}
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
		playBukovAction(fire, targetCell, 1, true, callback);
	}

	public void firearmFire(int targetCell) {
		firearmFire(targetCell, null);
	}

	/**
	 * Realtime reload/operate animation, also isolated from host turn timing.
	 */
	public synchronized void reloadFirearm(int targetCell, final Callback callback) {
		reloadFirearm(targetCell, 0f, callback);
	}

	public synchronized void reloadFirearm(
			int targetCell,
			float durationSeconds,
			final Callback callback) {
		if (!bukovOperator) {
			operate(targetCell, callback);
			return;
		}
		bukovReloading = true;
		reload.delay = reloadFrameDelay(
				durationSeconds,
				reload.frames == null ? 0 : reload.frames.length);
		faceBukovTarget(ch.pos, targetCell);
		playBukovAction(reload, targetCell, 2, callback);
	}

	public void reloadFirearm(int targetCell) {
		reloadFirearm(targetCell, null);
	}

	public void reloadFirearm(int targetCell, float durationSeconds) {
		reloadFirearm(targetCell, durationSeconds, null);
	}

	public synchronized void reloadFinished() {
		bukovReloading = false;
		if (cancelRealtimeAction(reload)) {
			bukovActionPlaying = false;
		}
	}

	public synchronized void hitReaction(final Callback callback) {
		hitReaction(ch == null ? 0 : ch.pos, 0f, callback);
	}

	public synchronized void hitReaction(
			int reloadTargetCell,
			float reloadRemainingSeconds,
			final Callback callback) {
		if (!bukovOperator) {
			if (callback != null) callback.call();
			return;
		}
		flash();
		final float resumeSeconds = Math.max(
				0f,
				reloadRemainingSeconds - animationDuration(hit));
		playBukovAction(hit, ch.pos, 3, new Callback() {
			@Override
			public void call() {
				if (bukovReloading && resumeSeconds > 0f) {
					reloadFirearm(reloadTargetCell, resumeSeconds);
				}
				if (callback != null) {
					callback.call();
				}
			}
		});
	}

	public void hitReaction() {
		hitReaction(null);
	}

	public void hitReaction(
			int reloadTargetCell,
			float reloadRemainingSeconds) {
		hitReaction(reloadTargetCell, reloadRemainingSeconds, null);
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
		playBukovAction(
				animation,
				targetCell,
				priority,
				false,
				callback);
	}

	private void playBukovAction(
			final Animation animation,
			int targetCell,
			int priority,
			boolean restartSamePriority,
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
		if (playRealtimeAction(
				animation,
				targetCell,
				priority,
				restartSamePriority,
				completion)) {
			bukovActionPlaying = true;
		}
	}

	static float reloadFrameDelay(float durationSeconds, int frameCount) {
		if (!(durationSeconds > 0f)
				|| Float.isInfinite(durationSeconds)
				|| frameCount <= 0) {
			return 1f / 12f;
		}
		return durationSeconds / frameCount;
	}

	private static float animationDuration(Animation animation) {
		return animation == null || animation.frames == null
				? 0f : animation.delay * animation.frames.length;
	}

	public void setBukovRealtimeOrientation(
			float moveX, float moveY, float aimX, float aimY ) {
		if (!bukovOperator) {
			return;
		}
		bukovPose.update(moveX, moveY, aimX, aimY);
		bukovAimActive = bukovPose.aimActive();
		setBukovLocomotionDirection(bukovPose.locomotionFacing().row);
		setBukovDirection(bukovPose.upperBodyFacing().row);
	}

	@Override
	public void setRealtimeMoving(boolean moving) {
		bukovMoving = moving;
		super.setRealtimeMoving(moving);
		if (bukovOperator && bukovLowerLayer != null) {
			bukovLowerLayer.play(
					moving ? bukovLowerRun : bukovLowerIdle);
		}
		if (bukovOperator && bukovAimActive
				&& !bukovActionPlaying && curAnim != die) {
			// Locomotion belongs to the lower film. A live aim vector keeps the
			// torso and weapon in the aim film even while the legs are running.
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
			bukovPose.faceUpperBody(dx, dy);
			setBukovDirection(bukovPose.upperBodyFacing().row);
		}
	}

	private void setBukovDirection(int direction) {
		if (direction == bukovDirection) {
			return;
		}
		bukovDirection = direction;
		bukovUpperRenderedDirection = -1;
		flipHorizontal = false;
	}

	private void setBukovLocomotionDirection(int direction) {
		if (direction == bukovLocomotionDirection) {
			return;
		}
		bukovLocomotionDirection = direction;
		rebuildBukovLocomotionAnimations();
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
		if (bukovOperator && bukovLowerLayer != null) {
			bukovLowerLayer.update();
		}
	}

	@Override
	public void draw() {
		if (!bukovOperator || bukovLowerLayer == null
				|| bukovUpperLayer == null) {
			super.draw();
			return;
		}

		int frameColumn = currentBukovFrameColumn();
		if (bukovUpperRenderedDirection != bukovDirection
				|| bukovUpperRenderedFrame != frameColumn) {
			bukovUpperLayer.frame(
					frameColumn * FRAME_WIDTH,
					bukovDirection * FRAME_HEIGHT,
					FRAME_WIDTH,
					FRAME_HEIGHT
			);
			bukovUpperRenderedDirection = bukovDirection;
			bukovUpperRenderedFrame = frameColumn;
		}

		syncBukovLayer(bukovLowerLayer);
		syncBukovLayer(bukovUpperLayer);
		if (curAnim != die) {
			bukovLowerLayer.draw();
		}
		bukovUpperLayer.draw();
	}

	private int currentBukovFrameColumn() {
		RectF activeFrame = frame();
		return Math.max(0, Math.min(31, Math.round(
				activeFrame.left * texture.width / FRAME_WIDTH)));
	}

	private void syncBukovLayer(Image layer) {
		layer.camera = camera();
		layer.x = x;
		layer.y = y;
		layer.scale.set(scale);
		layer.origin.set(origin);
		layer.angle = angle;
		layer.rm = rm;
		layer.gm = gm;
		layer.bm = bm;
		layer.am = am;
		layer.ra = ra;
		layer.ga = ga;
		layer.ba = ba;
		layer.aa = aa;
	}

	@Override
	public void destroy() {
		if (bukovLowerLayer != null) {
			bukovLowerLayer.destroy();
			bukovLowerLayer = null;
		}
		if (bukovUpperLayer != null) {
			bukovUpperLayer.destroy();
			bukovUpperLayer = null;
		}
		super.destroy();
	}
	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
		if (bukovLowerRun != null) {
			bukovLowerRun.delay = 1f / speed / RUN_FRAMERATE;
		}
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

	private static TextureFilm bukovLowerTiers() {
		if (bukovLowerTiers == null) {
			SmartTexture texture =
					TextureCache.get(Assets.Sprites.BUKOV_OPERATOR_LOWER);
			bukovLowerTiers =
					new TextureFilm(texture, texture.width, FRAME_HEIGHT);
		}
		return bukovLowerTiers;
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
