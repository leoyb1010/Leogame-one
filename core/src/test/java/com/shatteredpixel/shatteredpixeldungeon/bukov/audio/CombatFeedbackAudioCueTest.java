package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CombatFeedbackAudioCueTest {

	@Test
	public void everyGateFiveCombatOutcomeHasAnExplicitCue() {
		assertEquals(
				Assets.Sounds.Bukov.KILL_CONFIRM,
				CombatFeedbackAudioCue.asset(CombatFeedbackType.KILL));
		assertEquals(
				Assets.Sounds.Bukov.KILL_CONFIRM,
				CombatFeedbackAudioCue.asset(
						CombatFeedbackType.WEAKPOINT_KILL));
		assertEquals(
				Assets.Sounds.Bukov.BOSS_PHASE_BREAK,
				CombatFeedbackAudioCue.asset(
						CombatFeedbackType.BOSS_PHASE_BREAK));
		assertEquals(
				Assets.Sounds.Bukov.BOSS_SLAM,
				CombatFeedbackAudioCue.asset(
						CombatFeedbackType.BOSS_SLAM));
		assertEquals(
				Assets.Sounds.Bukov.BOSS_OVERLOAD,
				CombatFeedbackAudioCue.asset(
						CombatFeedbackType.EXPLOSION));

		for (CombatFeedbackType type : new CombatFeedbackType[]{
				CombatFeedbackType.KILL,
				CombatFeedbackType.WEAKPOINT_KILL,
				CombatFeedbackType.BOSS_PHASE_BREAK,
				CombatFeedbackType.BOSS_SLAM,
				CombatFeedbackType.EXPLOSION}) {
			assertTrue(type.name(),
					CombatFeedbackAudioCue.volume(type) > 0f);
			assertEquals(type.name(),
					1f,
					CombatFeedbackAudioCue.pitch(type),
					0f);
			assertTrue(type.name(),
					CombatFeedbackAudioCue.category(type) != null);
		}
	}

	@Test
	public void ordinaryShotsDoNotManufactureAnExtraFeedbackCue() {
		assertNull(CombatFeedbackAudioCue.asset(
				CombatFeedbackType.RIFLE_SHOT));
		assertNull(CombatFeedbackAudioCue.category(
				CombatFeedbackType.PLAYER_HIT));
		assertEquals(
				0f,
				CombatFeedbackAudioCue.volume(
						CombatFeedbackType.SHOTGUN_NEAR),
				0f);
		assertEquals(
				0f,
				CombatFeedbackAudioCue.pitch(
						CombatFeedbackType.SHOTGUN_NEAR),
				0f);
	}

	@Test
	public void bossAndKillCuesUseTheExpectedConcurrencyCategories() {
		assertEquals(
				SoundCategory.COMBAT_FEEDBACK,
				CombatFeedbackAudioCue.category(
						CombatFeedbackType.KILL));
		assertEquals(
				SoundCategory.COMBAT_FEEDBACK,
				CombatFeedbackAudioCue.category(
						CombatFeedbackType.WEAKPOINT_KILL));
		assertEquals(
				SoundCategory.BOSS_CUE,
				CombatFeedbackAudioCue.category(
						CombatFeedbackType.BOSS_PHASE_BREAK));
		assertEquals(
				SoundCategory.BOSS_CUE,
				CombatFeedbackAudioCue.category(
						CombatFeedbackType.BOSS_SLAM));
		assertEquals(
				SoundCategory.BOSS_CUE,
				CombatFeedbackAudioCue.category(
						CombatFeedbackType.EXPLOSION));
	}
}
