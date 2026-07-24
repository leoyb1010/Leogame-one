package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidHudPresentationModelTest {

	@Test
	public void reloadRingFillsClockwiseAndReversesInterruptedProgress() {
		BukovReloadRingModel ring = new BukovReloadRingModel();
		ring.update(0f, false, 0f, 6, false, 0.12f);
		ring.update(0.5f, true, 0.5f, 6, false, 0.12f);

		assertTrue(ring.visible(true));
		assertEquals(4, ring.filledSegmentCount());

		ring.update(0f, false, 0f, 6, false, 0.12f);
		assertTrue(ring.visible(false));
		assertEquals(4, ring.filledSegmentCount());

		ring.update(0.06f, false, 0f, 6, false, 0.12f);
		assertEquals(2, ring.filledSegmentCount());
		ring.update(0.06f, false, 0f, 6, false, 0.12f);
		assertFalse(ring.visible(false));
		assertEquals(0, ring.filledSegmentCount());
	}

	@Test
	public void reducedMotionAndCompletedReloadSkipReverseAnimation() {
		BukovReloadRingModel reduced = new BukovReloadRingModel();
		reduced.update(0f, false, 0f, 6, true, 0.12f);
		reduced.update(0f, true, 0.75f, 6, true, 0.12f);
		reduced.update(0f, false, 0f, 6, true, 0.12f);
		assertFalse(reduced.visible(false));

		BukovReloadRingModel completed = new BukovReloadRingModel();
		completed.update(0f, false, 0f, 6, false, 0.12f);
		completed.update(0f, true, 0.9f, 6, false, 0.12f);
		completed.update(0f, false, 0f, 24, false, 0.12f);
		assertFalse(completed.visible(false));
	}

	@Test
	public void soundRingMapsAllDirectionsAndEncodesTypeDistanceAndLifetime() {
		for (BukovRaidHudState.Direction direction
				: BukovRaidHudState.Direction.values()) {
			assertEquals(
					direction.ordinal(),
					BukovSoundRingModel.segmentIndex(direction));
		}
		assertEquals(-1, BukovSoundRingModel.segmentIndex(null));
		assertFalse(BukovSoundRingModel.longArc(SoundCategory.FOOTSTEP));
		assertTrue(
				BukovSoundRingModel.longArc(SoundCategory.ENEMY_GUNSHOT));

		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("突围", 12f);
		state.sound(
				SoundCategory.FOOTSTEP,
				BukovRaidHudState.Direction.NE,
				BukovRaidHudState.Distance.NEAR,
				1f,
				BukovSoundRingModel.LIFETIME_SECONDS);
		assertEquals(1f, BukovSoundRingModel.alpha(state), 0f);

		state.sound(
				SoundCategory.ENEMY_GUNSHOT,
				BukovRaidHudState.Direction.SW,
				BukovRaidHudState.Distance.FAR,
				0.5f,
				BukovSoundRingModel.LIFETIME_SECONDS * 0.5f);
		assertEquals(
				0.45f * 0.675f * 0.5f,
				BukovSoundRingModel.alpha(state),
				0.0001f);
	}

	@Test
	public void medicalHintMatchesEveryLiveInputSurface() {
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.medical_hint_desktop"),
				BukovRaidHud.medicalHint(true, false));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.medical_hint_controller"),
				BukovRaidHud.medicalHint(true, true));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.medical_hint_touch"),
				BukovRaidHud.medicalHint(false, false));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.hud.medical_hint_controller"),
				BukovRaidHud.medicalHint(false, true));
	}
}
