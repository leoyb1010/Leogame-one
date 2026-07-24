package com.shatteredpixel.shatteredpixeldungeon.ios;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.iosrobovm.IOSAudio;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SilentIOSAudioTest {

	@After
	public void clearBackendMarker() {
		System.clearProperty(IOSAudioStartupPolicy.BACKEND_PROPERTY);
	}

	@Test
	public void musicAcceptsNormalGameOperationsWithoutNativeAudio() {
		Music music = new SilentIOSAudio().newMusic(null);
		music.setLooping(true);
		music.setVolume(0.4f);
		music.play();

		assertFalse(music.isPlaying());
		assertEquals(0.4f, music.getVolume(), 0.0001f);
	}

	@Test
	public void simulatorKeepsSuccessfullyInitializedNativeAudio() {
		final IOSAudio expected = new SilentIOSAudio();
		final boolean[] loggedFailure = {false};

		IOSAudio actual = IOSAudioStartupPolicy.createForSimulator(
				new IOSAudioStartupPolicy.Factory() {
					@Override
					public IOSAudio create() {
						return expected;
					}
				},
				new IOSAudioStartupPolicy.FailureLogger() {
					@Override
					public void failed(String message, Throwable failure) {
						loggedFailure[0] = true;
					}
				});

		assertSame(expected, actual);
		assertFalse(loggedFailure[0]);
		assertEquals(
				IOSAudioStartupPolicy.NATIVE_BACKEND,
				System.getProperty(
						IOSAudioStartupPolicy.BACKEND_PROPERTY));
		assertFalse(IOSAudioStartupPolicy.usingSilentFallback());
	}

	@Test
	public void simulatorFallsBackOnlyAfterInitializationFailure() {
		final RuntimeException expectedFailure =
				new RuntimeException("native audio unavailable");
		final String[] message = {null};
		final Throwable[] reportedFailure = {null};

		IOSAudio actual = IOSAudioStartupPolicy.createForSimulator(
				new IOSAudioStartupPolicy.Factory() {
					@Override
					public IOSAudio create() {
						throw expectedFailure;
					}
				},
				new IOSAudioStartupPolicy.FailureLogger() {
					@Override
					public void failed(
							String value,
							Throwable failure) {
						message[0] = value;
						reportedFailure[0] = failure;
					}
				});

		assertTrue(actual instanceof SilentIOSAudio);
		assertSame(expectedFailure, reportedFailure[0]);
		assertTrue(message[0].contains(
				"Simulator native audio initialization failed"));
		assertEquals(
				IOSAudioStartupPolicy.SILENT_FALLBACK_BACKEND,
				System.getProperty(
						IOSAudioStartupPolicy.BACKEND_PROPERTY));
		assertTrue(IOSAudioStartupPolicy.usingSilentFallback());
	}
}
