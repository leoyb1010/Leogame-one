package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;

/**
 * Runs without Gradle, libGDX or an audio device.
 */
public final class BukovAudioModelStandaloneTest {

	public static void main(String[] args) {
		ExperienceContract contract = contract();
		SpatialAudioModel.Result spatial = new SpatialAudioModel.Result();
		SpatialAudioModel.resolve(
				contract, 1f, 8f, 1f, false, spatial);
		assertNear(-18f, spatial.spatialDecibels(), 0.0001f,
				"distance and one wall");
		assertNear(1000f, spatial.lowPassHz(), 0f, "wall low pass");

		GunshotAudioPlan gunshotPlan = new GunshotAudioPlan();
		GunshotAudioResolver.resolve(
				false, 2, 8f, 0f, spatial, gunshotPlan);
		if (!gunshotPlan.audible()
				|| gunshotPlan.bodyRight() <= gunshotPlan.bodyLeft()) {
			throw new AssertionError("right-side gunshot must pan right");
		}
		assertNear(1.04f, gunshotPlan.bodyPitch(), 0f, "variant pitch");

		KeySoundVisualEvent event = new KeySoundVisualEvent();
		KeySoundVisualizationResolver.resolve(
				SoundCategory.ENEMY_GUNSHOT,
				8f, 0f, spatial, contract, true, event);
		event.activate(7, 0.9f);
		event.advance(0.4f);
		if (!event.visible()
				|| event.direction() != KeySoundVisualEvent.Direction.E
				|| event.distanceBand()
						!= KeySoundVisualEvent.DistanceBand.MID) {
			throw new AssertionError("sound visualization state mismatch");
		}
		assertNear(0.5f, event.remainingSeconds(), 0.0001f,
				"sound event lifetime");

		SpatialAudioModel.resolve(
				contract, 0f, 8f, 0f, false, spatial);
		if (!spatial.perceivable() || spatial.audible()) {
			throw new AssertionError(
					"user volume must not modify AI perception");
		}
		BukovAudioBusMix mix = new BukovAudioBusMix();
		mix.set(0.8f, 0.7f, 0.9f, 0.7f);
		assertNear(0.72f, mix.gain(AudioChannel.SFX, 0f), 0.0001f,
				"SFX channel");
		assertNear(0.56f, mix.gain(AudioChannel.AMBIENCE, 0f), 0.0001f,
				"ambience channel");
		if (mix.gain(AudioChannel.MUSIC, 1f)
				>= mix.gain(AudioChannel.MUSIC, 0f)) {
			throw new AssertionError("combat must duck music by three dB");
		}

		BukovAtmosphereController atmosphere =
				new BukovAtmosphereController();
		BukovAtmosphereSignal signal = new BukovAtmosphereSignal();
		signal.set(false, true);
		atmosphere.update(1.5f, signal);
		if (atmosphere.target()
						!= BukovAtmosphereController.State.COMBAT
				|| atmosphere.gain(
						BukovAtmosphereController.State.COMBAT) != 1f) {
			throw new AssertionError("combat crossfade mismatch");
		}
		signal.set(false, false);
		atmosphere.update(7.9f, signal);
		if (atmosphere.target()
				!= BukovAtmosphereController.State.COMBAT) {
			throw new AssertionError("combat must hold for eight seconds");
		}
		atmosphere.update(0.2f, signal);
		if (atmosphere.target()
				!= BukovAtmosphereController.State.CALM) {
			throw new AssertionError("combat release must return to calm");
		}
		System.out.println("PASS: Bukov spatial/layered audio model");
	}

	private static ExperienceContract contract() {
		ExperienceContract contract = new ExperienceContract();
		contract.fullVolumeDistance = 1f;
		contract.referenceDistance = 8f;
		contract.referenceDecibels = -12f;
		contract.wallDecibels = -6f;
		contract.lowPassDistance = 15f;
		contract.lowPassHz = 1000f;
		contract.minimumAudibleDecibels = -48f;
		contract.visualizationNearDistance = 4f;
		contract.visualizationMidDistance = 10f;
		return contract;
	}

	private static void assertNear(
			float expected,
			float actual,
			float tolerance,
			String label) {
		if (Math.abs(expected - actual) > tolerance) {
			throw new AssertionError(
					label + ": expected " + expected + ", got " + actual);
		}
	}
}
