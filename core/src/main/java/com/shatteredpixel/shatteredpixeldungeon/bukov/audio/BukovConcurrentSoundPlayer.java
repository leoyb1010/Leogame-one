package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Runtime wrapper that applies {@link SoundConcurrencyBudget} before touching
 * the platform sound backend.
 */
final class BukovConcurrentSoundPlayer {

	private static final int MAX_TRACKED_PLAYBACKS =
			AudioChannel.values().length
					* SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS;

	private static final class Playback {

		private long token;
		private String asset;
		private long playbackId;

		private void clear() {
			token = 0L;
			asset = null;
			playbackId = -1L;
		}
	}

	private final SoundConcurrencyBudget budget =
			new SoundConcurrencyBudget();
	private final BukovSoundPlaybackSink sink;
	private final Playback[] playbacks =
			new Playback[MAX_TRACKED_PLAYBACKS];

	BukovConcurrentSoundPlayer(BukovSoundPlaybackSink sink) {
		if (sink == null) {
			throw new IllegalArgumentException("sink is required");
		}
		this.sink = sink;
		for (int index = 0; index < playbacks.length; index++) {
			playbacks[index] = new Playback();
		}
	}

	long play(
			String asset,
			AudioChannel channel,
			SoundConcurrencyBudget.Priority priority,
			boolean protectedSource,
			float timeoutSeconds,
			float leftVolume,
			float rightVolume,
			float pitch) {
		if (asset == null || asset.trim().isEmpty()) {
			throw new IllegalArgumentException("asset is required");
		}
		requireNonNegative(leftVolume, "leftVolume");
		requireNonNegative(rightVolume, "rightVolume");
		if (!BukovNumbers.isFinite(pitch) || pitch <= 0f) {
			throw new IllegalArgumentException(
					"pitch must be finite and positive");
		}
		if (Math.max(leftVolume, rightVolume) <= 0f) {
			return SoundConcurrencyBudget.NO_TOKEN;
		}

		SoundConcurrencyBudget.Admission admission = budget.admit(
				channel, priority, protectedSource, timeoutSeconds);
		if (!admission.admitted()) {
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		stopInactivePlaybacks();

		long playbackId = sink.play(
				asset, leftVolume, rightVolume, pitch);
		if (playbackId < 0L) {
			budget.release(admission.token());
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		Playback playback = freePlayback();
		if (playback == null) {
			sink.stop(asset, playbackId);
			budget.release(admission.token());
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		playback.token = admission.token();
		playback.asset = asset;
		playback.playbackId = playbackId;
		return playback.token;
	}

	void update(float deltaSeconds) {
		budget.update(deltaSeconds);
		stopInactivePlaybacks();
	}

	boolean release(long token) {
		boolean released = budget.release(token);
		stopInactivePlaybacks();
		return released;
	}

	int activeCount(AudioChannel channel) {
		return budget.activeCount(channel);
	}

	private Playback freePlayback() {
		for (Playback playback : playbacks) {
			if (playback.token == 0L) return playback;
		}
		return null;
	}

	private void stopInactivePlaybacks() {
		for (Playback playback : playbacks) {
			if (playback.token == 0L
					|| budget.active(playback.token)) {
				continue;
			}
			sink.stop(playback.asset, playback.playbackId);
			playback.clear();
		}
	}

	private static void requireNonNegative(float value, String label) {
		if (!BukovNumbers.isFinite(value) || value < 0f) {
			throw new IllegalArgumentException(
					label + " must be finite and non-negative");
		}
	}
}
