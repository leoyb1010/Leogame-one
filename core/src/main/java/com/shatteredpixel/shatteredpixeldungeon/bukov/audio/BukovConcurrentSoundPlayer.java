package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Runtime wrapper that applies {@link SoundConcurrencyBudget} before touching
 * the platform sound backend.
 */
public final class BukovConcurrentSoundPlayer {

	private static final int MAX_LAYERS_PER_SOURCE = 3;
	private static final int MAX_TRACKED_PLAYBACKS =
			AudioChannel.values().length
					* SoundConcurrencyBudget.MAX_ACTIVE_PER_BUS
					* MAX_LAYERS_PER_SOURCE;
	private static final BukovSoundConcurrencyRuntime PRODUCTION_RUNTIME =
			new BukovSoundConcurrencyRuntime();

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

	private final BukovSoundConcurrencyRuntime runtime;
	private final long ownerId;
	private final BukovSoundPlaybackSink sink;
	private final Playback[] playbacks =
			new Playback[MAX_TRACKED_PLAYBACKS];

	/**
	 * Creates an isolated player for tests and non-production tools.
	 * Production call sites must use {@link #production(BukovSoundPlaybackSink)}
	 * so every producer shares the same six-voice-per-bus contract.
	 */
	public BukovConcurrentSoundPlayer(BukovSoundPlaybackSink sink) {
		this(sink, new BukovSoundConcurrencyRuntime());
	}

	BukovConcurrentSoundPlayer(
			BukovSoundPlaybackSink sink,
			BukovSoundConcurrencyRuntime runtime) {
		if (sink == null) {
			throw new IllegalArgumentException("sink is required");
		}
		if (runtime == null) {
			throw new IllegalArgumentException("runtime is required");
		}
		this.sink = sink;
		this.runtime = runtime;
		ownerId = runtime.newOwnerId();
		for (int index = 0; index < playbacks.length; index++) {
			playbacks[index] = new Playback();
		}
	}

	public static BukovConcurrentSoundPlayer production(
			BukovSoundPlaybackSink sink) {
		return new BukovConcurrentSoundPlayer(sink, PRODUCTION_RUNTIME);
	}

	public long play(
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

		long token = begin(
				channel, priority, protectedSource, timeoutSeconds);
		if (token == SoundConcurrencyBudget.NO_TOKEN) {
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		if (!playLayer(
				token, asset, leftVolume, rightVolume, pitch)) {
			release(token);
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		return token;
	}

	public long begin(
			AudioChannel channel,
			SoundConcurrencyBudget.Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		SoundConcurrencyBudget.Admission admission = runtime.admit(
				this,
				ownerId,
				channel,
				priority,
				protectedSource,
				timeoutSeconds);
		if (!admission.admitted()) {
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		return admission.token();
	}

	public boolean playLayer(
			long token,
			String asset,
			float leftVolume,
			float rightVolume,
			float pitch) {
		if (!runtime.active(ownerId, token)) return false;
		if (asset == null || asset.trim().isEmpty()) {
			throw new IllegalArgumentException("asset is required");
		}
		requireNonNegative(leftVolume, "leftVolume");
		requireNonNegative(rightVolume, "rightVolume");
		if (!BukovNumbers.isFinite(pitch) || pitch <= 0f) {
			throw new IllegalArgumentException(
					"pitch must be finite and positive");
		}
		if (Math.max(leftVolume, rightVolume) <= 0f
				|| layerCount(token) >= MAX_LAYERS_PER_SOURCE) {
			return false;
		}
		long playbackId = sink.play(
				asset, leftVolume, rightVolume, pitch);
		if (playbackId < 0L) return false;
		Playback playback = freePlayback();
		if (playback == null) {
			sink.stop(asset, playbackId);
			return false;
		}
		playback.token = token;
		playback.asset = asset;
		playback.playbackId = playbackId;
		return true;
	}

	public void update(float deltaSeconds) {
		runtime.update(ownerId, deltaSeconds);
		stopInactivePlaybacks();
		runtime.ownerBecameIdle(this, ownerId);
	}

	public boolean release(long token) {
		boolean released = runtime.release(ownerId, token);
		stopInactivePlaybacks();
		runtime.ownerBecameIdle(this, ownerId);
		return released;
	}

	/**
	 * Releases a logical voice while allowing its one-shot backend instances
	 * to finish naturally. This is only for scene-transition cues; looping or
	 * unbounded sounds must use {@link #release(long)} or {@link #stopAll()}.
	 */
	public boolean detach(long token) {
		if (!runtime.release(ownerId, token)) return false;
		for (Playback playback : playbacks) {
			if (playback.token == token) {
				playback.clear();
			}
		}
		runtime.ownerBecameIdle(this, ownerId);
		return true;
	}

	public int activeCount(AudioChannel channel) {
		return runtime.activeCount(channel);
	}

	public void stopAll() {
		runtime.clear(ownerId);
		stopInactivePlaybacks();
		runtime.ownerBecameIdle(this, ownerId);
	}

	long ownerId() {
		return ownerId;
	}

	private Playback freePlayback() {
		for (Playback playback : playbacks) {
			if (playback.token == 0L) return playback;
		}
		return null;
	}

	private int layerCount(long token) {
		int count = 0;
		for (Playback playback : playbacks) {
			if (playback.token == token) count++;
		}
		return count;
	}

	void stopInactivePlaybacks() {
		for (Playback playback : playbacks) {
			if (playback.token == 0L
					|| runtime.active(ownerId, playback.token)) {
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
