package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeEnvironmentRules;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.CollisionMap;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

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
		if (GunshotAudioResolver.variationIndex(-1) != 2
				|| GunshotSoundFamily.RIFLE.mechanicalAsset(0).equals(
						GunshotSoundFamily.RIFLE.mechanicalAsset(1))
				|| GunshotSoundFamily.RIFLE.bodyAsset(1).equals(
						GunshotSoundFamily.RIFLE.bodyAsset(2))) {
			throw new AssertionError(
					"gunshot layer variants must be deterministic and unique");
		}
		assertSpace(GunshotAcousticSpace.INDOOR, new ProbeMap(true, true));
		assertSpace(GunshotAcousticSpace.CORRIDOR, new ProbeMap(true, false));
		assertSpace(GunshotAcousticSpace.OPEN, new ProbeMap(false, false));

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

		assertFootstepAudio();
		assertConcurrencyBudget();

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

	private static void assertFootstepAudio() {
		if (FootstepSurface.resolve(Terrain.WATER, null)
						!= FootstepSurface.WATER
				|| FootstepSurface.resolve(Terrain.OPEN_DOOR, null)
						!= FootstepSurface.METAL
				|| FootstepSurface.resolve(Terrain.EMPTY, null)
						!= FootstepSurface.HARD) {
			throw new AssertionError(
					"terrain must select water, metal and hard footsteps");
		}
		if (FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.WATER)
						!= FootstepSurface.WATER
				|| FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.EMBERS)
						!= FootstepSurface.METAL
				|| FootstepSurface.forThemeSurface(
						ThemeEnvironmentRules.Surface.CUSTOM_DECO_EMPTY)
						!= FootstepSurface.HARD) {
			throw new AssertionError(
					"theme surface must select an authored footstep family");
		}
		for (FootstepSurface surface : FootstepSurface.values()) {
			if (surface.asset(0).equals(surface.asset(1))
					|| !surface.asset(0).equals(surface.asset(2))
					|| surface.gain() <= 0f
					|| surface.pitch(0) <= 0f
					|| surface.pitch(0) == surface.pitch(1)) {
				throw new AssertionError(
						surface + " footstep variants must be audible "
								+ "and deterministic");
			}
		}
		FootstepCadence cadence = new FootstepCadence();
		if (cadence.advance(0f, 0f, 1f)
				|| cadence.advance(0.1f, 0f, 0.1f)
				|| !cadence.advance(0.3f, 0f, 0.1f)) {
			throw new AssertionError(
					"footstep cadence must follow accepted movement distance");
		}
	}

	private static void assertConcurrencyBudget() {
		SoundConcurrencyBudget budget = new SoundConcurrencyBudget();
		long oldest = SoundConcurrencyBudget.NO_TOKEN;
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			long token = budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					1f).token();
			if (index == 0) oldest = token;
		}
		SoundConcurrencyBudget.Admission critical = budget.admit(
				AudioChannel.SFX,
				SoundConcurrencyBudget.defaultPriority(
						SoundCategory.PLAYER_GUNSHOT),
				SoundConcurrencyBudget.protectedByDefault(
						SoundCategory.PLAYER_GUNSHOT),
				1f);
		if (!critical.admitted()
				|| critical.evictedToken() != oldest
				|| !budget.active(critical.token())
				|| budget.activeCount(AudioChannel.SFX)
						!= SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS) {
			throw new AssertionError(
					"critical source must replace the oldest low voice");
		}
		for (int index = 0; index < 12; index++) {
			budget.admit(
					AudioChannel.SFX,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					1f);
		}
		if (!budget.active(critical.token())) {
			throw new AssertionError(
					"low priority sources must not evict player gunfire");
		}
		budget.update(1.1f);
		if (budget.activeCount(AudioChannel.SFX) != 0) {
			throw new AssertionError("voice timeout must recover capacity");
		}

		CountingSoundSink sink = new CountingSoundSink();
		BukovConcurrentSoundPlayer player =
				new BukovConcurrentSoundPlayer(sink);
		long token = player.play(
				"critical",
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.CRITICAL,
				true,
				0.1f,
				1f,
				1f,
				1f);
		if (token <= 0L || sink.played != 1) {
			throw new AssertionError(
					"admitted source must reach the audio backend");
		}
		player.update(0.11f);
		if (sink.stopped != 1
				|| player.activeCount(AudioChannel.SFX) != 0) {
			throw new AssertionError(
					"timeout must stop backend playback and free its voice");
		}
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

	private static void assertSpace(
			GunshotAcousticSpace expected,
			CollisionMap collisionMap) {
		GunshotAcousticSpace actual = GunshotAcousticSpaceResolver.resolve(
				collisionMap,
				10.5f,
				10.5f);
		if (actual != expected
				|| actual.tailAsset(0).equals(actual.tailAsset(1))
				|| !actual.tailAsset(0).equals(actual.tailAsset(3))) {
			throw new AssertionError(
					"acoustic space/variant mismatch: expected "
							+ expected + ", got " + actual);
		}
	}

	private static final class ProbeMap implements CollisionMap {

		private final boolean verticalWalls;
		private final boolean horizontalWalls;

		private ProbeMap(boolean verticalWalls, boolean horizontalWalls) {
			this.verticalWalls = verticalWalls;
			this.horizontalWalls = horizontalWalls;
		}

		@Override
		public int width() {
			return 32;
		}

		@Override
		public int height() {
			return 32;
		}

		@Override
		public boolean blocked(int x, int y) {
			return (verticalWalls && (y == 8 || y == 12))
					|| (horizontalWalls && (x == 8 || x == 12));
		}
	}

	private static final class CountingSoundSink
			implements BukovSoundPlaybackSink {

		private int played;
		private int stopped;

		@Override
		public long play(
				String asset,
				float leftVolume,
				float rightVolume,
				float pitch) {
			played++;
			return played;
		}

		@Override
		public void stop(String asset, long playbackId) {
			stopped++;
		}
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
