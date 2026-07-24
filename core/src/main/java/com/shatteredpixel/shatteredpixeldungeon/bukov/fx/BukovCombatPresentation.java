package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractRegistry;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.input.ControllerHandler;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Main-thread adapter from committed realtime pulses to sprite animation and
 * optional feedback. It has no path back into combat, input, RNG, or saves.
 */
public final class BukovCombatPresentation
		implements CombatPresentationEvent.Consumer {

	private final ExperienceContract contract;
	private final CombatFeedbackPlan feedbackPlan = new CombatFeedbackPlan();
	private final HitstopBudget hitstopBudget = new HitstopBudget();
	private final IdentityHashMap<CharSprite, SpriteHitstop> spriteHitstops =
			new IdentityHashMap<>();

	public BukovCombatPresentation() {
		this(new ExperienceContractRegistry().loadDefault());
	}

	BukovCombatPresentation(ExperienceContract contract) {
		if (contract == null) {
			throw new IllegalArgumentException("contract is required");
		}
		this.contract = contract;
	}

	@Override
	public void accept(CombatPresentationEvent event) {
		if (event == null) return;
		Char source = findChar(event.sourceId());
		Char target = findChar(event.targetId());
		switch (event.type()) {
			case PLAYER_FIRE:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).firearmFire(event.targetCell());
				}
				break;
			case PLAYER_RELOAD:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).reloadFirearm(
							event.targetCell(),
							event.durationSeconds());
				}
				break;
			case PLAYER_RELOAD_END:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).reloadFinished();
				}
				break;
			case PLAYER_HIT:
				if (target != null && target.sprite instanceof HeroSprite) {
					((HeroSprite)target.sprite).hitReaction(
							event.targetCell(),
							event.durationSeconds());
				}
				break;
			case PLAYER_MEDICAL_START:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).medicalUse();
				}
				break;
			case PLAYER_MEDICAL_END:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).medicalFinished();
				}
				break;
			case PLAYER_DEATH:
				playDeath(target);
				break;
			case PLAYER_EXTRACTION:
				if (source != null && source.sprite instanceof HeroSprite) {
					((HeroSprite)source.sprite).extractionRadio();
				}
				break;
			case ENEMY_FIRE:
			case ENEMY_MELEE:
				if (source != null && source.sprite != null) {
					source.sprite.realtimeAttack(event.targetCell());
				}
				break;
			case ENEMY_HIT:
				if (target != null && target.sprite != null) {
					target.sprite.realtimeHitReaction();
				}
				break;
			case ENEMY_DEATH:
				playDeath(target);
				break;
			default:
				break;
		}
		applyFeedback(event, source, target);
	}

	/**
	 * Advances only presentation-owned sprite timers. The realtime fixed-step,
	 * world state and input remain completely untouched.
	 */
	public void update(float elapsedSeconds) {
		if (!(elapsedSeconds > 0f)) {
			return;
		}
		hitstopBudget.advance(elapsedSeconds);
		if (spriteHitstops.isEmpty()) return;
		Iterator<Map.Entry<CharSprite, SpriteHitstop>> iterator =
				spriteHitstops.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<CharSprite, SpriteHitstop> entry = iterator.next();
			SpriteHitstop hitstop = entry.getValue();
			hitstop.remainingSeconds -= elapsedSeconds;
			if (hitstop.remainingSeconds <= 0f) {
				entry.getKey().paused = hitstop.originalPaused;
				iterator.remove();
			}
		}
	}

	/**
	 * Scene teardown must never leave a sprite paused if the scene is replaced
	 * while a hitstop window is active.
	 */
	public void dispose() {
		for (Map.Entry<CharSprite, SpriteHitstop> entry
				: spriteHitstops.entrySet()) {
			entry.getKey().paused = entry.getValue().originalPaused;
		}
		spriteHitstops.clear();
		hitstopBudget.clear();
	}

	private void applyFeedback(
			CombatPresentationEvent event, Char source, Char target) {
		int combatFeedback = SPDSettings.combatFeedback();
		boolean reduceMotion = SPDSettings.bukovReduceMotion();
		if (event.feedbackType() == null || combatFeedback <= 0) {
			return;
		}
		float feedbackScale = combatFeedback / 2f;
		float shakeScale = SPDSettings.screenShake() / 4f;
		float vibrationScale =
				SPDSettings.bukovControllerVibration() / 2f;
		feedbackPlan.clear();
		CombatFeedbackResolver.add(
				event.feedbackType(),
				0f,
				event.intensity(),
				contract,
				true,
				SPDSettings.soundFx() ? 1f : 0f,
				shakeScale * feedbackScale,
				SPDSettings.vibration()
						? vibrationScale * feedbackScale
						: 0f,
				hitstopEnabled(combatFeedback, reduceMotion),
				reduceMotion,
				SPDSettings.bukovReduceFlashes(),
				feedbackPlan);
		if (isHitOutcome(event.type()) && feedbackPlan.hitstopMs() > 0) {
			int hitstopMs = scaledHitstopMs(
					feedbackPlan.hitstopMs(), combatFeedback);
			hitstopMs = hitstopBudget.request(hitstopMs);
			applySpriteHitstop(
					source == null ? null : source.sprite,
					target == null ? null : target.sprite,
					hitstopMs);
		}
		if (feedbackPlan.shakeAmplitudePx() > 0f
				&& Camera.main != null) {
			Camera.main.shake(
					feedbackPlan.shakeAmplitudePx(),
					feedbackPlan.shakeDurationMs() / 1000f);
		}
		if (feedbackPlan.vibrationAmplitude() > 0f
				&& feedbackPlan.vibrationDurationMs() > 0) {
			int duration = Math.max(
					1,
					Math.round(
							feedbackPlan.vibrationDurationMs()
									* feedbackPlan.vibrationAmplitude()));
			if (ControllerHandler.vibrationSupported()) {
				ControllerHandler.vibrate(duration);
			} else {
				Game.vibrate(duration);
			}
		}
	}

	void applySpriteHitstop(
			CharSprite source, CharSprite target, int durationMs) {
		if (durationMs <= 0) {
			return;
		}
		freeze(source, durationMs / 1000f);
		if (target != source) {
			freeze(target, durationMs / 1000f);
		}
	}

	int activeHitstopCount() {
		return spriteHitstops.size();
	}

	int rollingHitstopMs() {
		return hitstopBudget.rollingTotalMs();
	}

	private void freeze(CharSprite sprite, float durationSeconds) {
		if (sprite == null) {
			return;
		}
		SpriteHitstop active = spriteHitstops.get(sprite);
		if (active == null) {
			active = new SpriteHitstop(sprite.paused, durationSeconds);
			spriteHitstops.put(sprite, active);
		} else {
			active.remainingSeconds = Math.max(
					active.remainingSeconds,
					durationSeconds);
		}
		sprite.paused = true;
	}

	static boolean hitstopEnabled(int combatFeedback, boolean reduceMotion) {
		return combatFeedback > 0 && !reduceMotion;
	}

	static int scaledHitstopMs(int requestedMs, int combatFeedback) {
		if (requestedMs <= 0 || combatFeedback <= 0) {
			return 0;
		}
		return combatFeedback == 1
				? Math.max(1, (requestedMs + 1) / 2)
				: requestedMs;
	}

	static boolean isHitOutcome(CombatPresentationEvent.Type type) {
		return type == CombatPresentationEvent.Type.PLAYER_HIT
				|| type == CombatPresentationEvent.Type.ENEMY_HIT
				|| type == CombatPresentationEvent.Type.PLAYER_DEATH
				|| type == CombatPresentationEvent.Type.ENEMY_DEATH;
	}

	private static void playDeath(Char target) {
		if (target != null && target.sprite != null) {
			target.sprite.die();
		}
	}

	private static Char findChar(int id) {
		if (Dungeon.hero != null && Dungeon.hero.id() == id) {
			return Dungeon.hero;
		}
		Actor actor = Actor.findById(id);
		return actor instanceof Char ? (Char)actor : null;
	}

	private static final class SpriteHitstop {
		private final boolean originalPaused;
		private float remainingSeconds;

		private SpriteHitstop(
				boolean originalPaused, float remainingSeconds) {
			this.originalPaused = originalPaused;
			this.remainingSeconds = remainingSeconds;
		}
	}
}
