package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovConcurrentSoundPlayerTest {

	@Test
	public void wrapperStopsEvictedReleasedAndTimedOutPlayback() {
		FakeSink sink = new FakeSink();
		BukovConcurrentSoundPlayer player =
				new BukovConcurrentSoundPlayer(sink);
		long first = SoundConcurrencyBudget.NO_TOKEN;
		for (int index = 0;
				index < SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;
				index++) {
			long token = play(
					player,
					"low-" + index,
					SoundConcurrencyBudget.Priority.LOW,
					false,
					1f);
			if (index == 0) first = token;
		}

		long critical = play(
				player,
				"player-gunshot",
				SoundConcurrencyBudget.Priority.CRITICAL,
				true,
				1f);
		assertTrue(critical > 0L);
		assertTrue(first > 0L);
		assertEquals(1, sink.stopped.size());
		assertEquals("low-0", sink.stopped.get(0));

		assertTrue(player.release(critical));
		assertEquals(2, sink.stopped.size());

		long timed = play(
				player,
				"timeout",
				SoundConcurrencyBudget.Priority.NORMAL,
				false,
				0.1f);
		assertTrue(timed > 0L);
		player.update(0.11f);
		assertTrue(sink.stopped.contains("timeout"));
	}

	@Test
	public void mutedPlaybackDoesNotConsumeVoiceOrTouchDevice() {
		FakeSink sink = new FakeSink();
		BukovConcurrentSoundPlayer player =
				new BukovConcurrentSoundPlayer(sink);
		long token = player.play(
				"muted",
				AudioChannel.SFX,
				SoundConcurrencyBudget.Priority.CRITICAL,
				true,
				1f,
				0f,
				0f,
				1f);
		assertEquals(SoundConcurrencyBudget.NO_TOKEN, token);
		assertEquals(0, player.activeCount(AudioChannel.SFX));
		assertEquals(0, sink.played.size());
	}

	private static long play(
			BukovConcurrentSoundPlayer player,
			String asset,
			SoundConcurrencyBudget.Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		return player.play(
				asset,
				AudioChannel.SFX,
				priority,
				protectedSource,
				timeoutSeconds,
				1f,
				1f,
				1f);
	}

	private static final class FakeSink
			implements BukovSoundPlaybackSink {

		private final List<String> played = new ArrayList<>();
		private final List<String> stopped = new ArrayList<>();

		@Override
		public long play(
				String asset,
				float leftVolume,
				float rightVolume,
				float pitch) {
			played.add(asset);
			return played.size();
		}

		@Override
		public void stop(String asset, long playbackId) {
			stopped.add(asset);
		}
	}
}
