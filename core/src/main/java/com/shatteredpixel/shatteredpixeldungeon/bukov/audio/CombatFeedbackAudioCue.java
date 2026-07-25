package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;

/**
 * Explicit audio half of the Gate 5 combat-feedback contract.
 *
 * <p>The visual and haptic envelopes remain owned by the presentation layer;
 * this resolver only selects the matching project-original SFX cue.</p>
 */
public final class CombatFeedbackAudioCue {

	public static String asset(CombatFeedbackType type) {
		if (type == null) return null;
		switch (type) {
			case KILL:
			case WEAKPOINT_KILL:
				return Assets.Sounds.Bukov.KILL_CONFIRM;
			case BOSS_PHASE_BREAK:
				return Assets.Sounds.Bukov.BOSS_PHASE_BREAK;
			case BOSS_SLAM:
				return Assets.Sounds.Bukov.BOSS_SLAM;
			case EXPLOSION:
				return Assets.Sounds.Bukov.BOSS_OVERLOAD;
			default:
				return null;
		}
	}

	public static float volume(CombatFeedbackType type) {
		if (type == null) return 0f;
		switch (type) {
			case KILL:
				return 0.46f;
			case WEAKPOINT_KILL:
				return 0.58f;
			case BOSS_PHASE_BREAK:
				return 0.78f;
			case BOSS_SLAM:
				return 0.72f;
			case EXPLOSION:
				return 0.82f;
			default:
				return 0f;
		}
	}

	public static float pitch(CombatFeedbackType type) {
		return asset(type) == null ? 0f : 1f;
	}

	public static SoundCategory category(CombatFeedbackType type) {
		if (type == CombatFeedbackType.BOSS_PHASE_BREAK
				|| type == CombatFeedbackType.BOSS_SLAM
				|| type == CombatFeedbackType.EXPLOSION) {
			return SoundCategory.BOSS_CUE;
		}
		return asset(type) == null ? null : SoundCategory.COMBAT_FEEDBACK;
	}

	private CombatFeedbackAudioCue() {
		throw new AssertionError();
	}
}
