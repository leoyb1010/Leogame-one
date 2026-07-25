package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;

/**
 * Deterministic per-bus voice budget for short Bukov sounds.
 *
 * <p>Every bus owns six logical voices. A new request may replace the oldest
 * voice in the lowest eligible priority. An unprotected request can never
 * replace a protected voice; a protected request may replace the oldest
 * protected voice at an eligible priority so rapid critical cues do not
 * deadlock their own bus. Explicit release handles playback-complete callbacks
 * where available; timeouts guarantee recovery on backends that expose no
 * completion event.</p>
 */
public final class SoundConcurrencyBudget {

	public static final int MAX_ACTIVE_PER_BUS = 6;
	public static final long NO_TOKEN = -1L;

	public enum Priority {
		LOW,
		NORMAL,
		HIGH,
		CRITICAL
	}

	public static final class Admission {

		private final long token;
		private final long evictedToken;

		private Admission(long token, long evictedToken) {
			this.token = token;
			this.evictedToken = evictedToken;
		}

		public boolean admitted() {
			return token != NO_TOKEN;
		}

		public long token() {
			return token;
		}

		public long evictedToken() {
			return evictedToken;
		}
	}

	private static final class Voice {

		private long token;
		private long ownerId;
		private long order;
		private Priority priority;
		private boolean protectedSource;
		private float remainingSeconds;

		private boolean active() {
			return token != 0L;
		}

		private void clear() {
			token = 0L;
			ownerId = 0L;
			order = 0L;
			priority = null;
			protectedSource = false;
			remainingSeconds = 0f;
		}
	}

	private final Voice[][] voices =
			new Voice[AudioChannel.values().length][MAX_ACTIVE_PER_BUS];
	private long nextToken = 1L;
	private long nextOrder = 1L;

	public SoundConcurrencyBudget() {
		for (int channel = 0; channel < voices.length; channel++) {
			for (int index = 0; index < MAX_ACTIVE_PER_BUS; index++) {
				voices[channel][index] = new Voice();
			}
		}
	}

	public Admission admit(
			AudioChannel channel,
			Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		return admit(
				0L,
				channel,
				priority,
				protectedSource,
				timeoutSeconds);
	}

	Admission admit(
			long ownerId,
			AudioChannel channel,
			Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		if (ownerId < 0L) {
			throw new IllegalArgumentException(
					"ownerId must be non-negative");
		}
		if (channel == null) {
			throw new IllegalArgumentException("channel is required");
		}
		if (priority == null) {
			throw new IllegalArgumentException("priority is required");
		}
		if (!BukovNumbers.isFinite(timeoutSeconds)
				|| timeoutSeconds <= 0f) {
			throw new IllegalArgumentException(
					"timeoutSeconds must be finite and positive");
		}

		Voice[] channelVoices = voices[channel.ordinal()];
		int target = freeVoice(channelVoices);
		long evictedToken = NO_TOKEN;
		if (target < 0) {
			target = replacementVoice(
					channelVoices, priority, protectedSource);
			if (target < 0) {
				return new Admission(NO_TOKEN, NO_TOKEN);
			}
			evictedToken = channelVoices[target].token;
		}

		Voice voice = channelVoices[target];
		voice.token = nextToken();
		voice.ownerId = ownerId;
		voice.order = nextOrder++;
		voice.priority = priority;
		voice.protectedSource = protectedSource;
		voice.remainingSeconds = timeoutSeconds;
		return new Admission(voice.token, evictedToken);
	}

	public void update(float deltaSeconds) {
		update(0L, deltaSeconds);
	}

	void update(long ownerId, float deltaSeconds) {
		if (ownerId < 0L) {
			throw new IllegalArgumentException(
					"ownerId must be non-negative");
		}
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
		if (deltaSeconds == 0f) return;
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				if (!voice.active() || voice.ownerId != ownerId) continue;
				voice.remainingSeconds -= deltaSeconds;
				if (voice.remainingSeconds <= 0f) {
					voice.clear();
				}
			}
		}
	}

	public boolean release(long token) {
		return release(0L, token);
	}

	boolean release(long ownerId, long token) {
		if (token <= 0L) return false;
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				if (voice.token == token && voice.ownerId == ownerId) {
					voice.clear();
					return true;
				}
			}
		}
		return false;
	}

	public boolean active(long token) {
		return active(0L, token);
	}

	boolean active(long ownerId, long token) {
		if (token <= 0L) return false;
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				if (voice.token == token && voice.ownerId == ownerId) {
					return true;
				}
			}
		}
		return false;
	}

	public int activeCount(AudioChannel channel) {
		if (channel == null) {
			throw new IllegalArgumentException("channel is required");
		}
		int count = 0;
		for (Voice voice : voices[channel.ordinal()]) {
			if (voice.active()) count++;
		}
		return count;
	}

	int activeCount(long ownerId) {
		int count = 0;
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				if (voice.active() && voice.ownerId == ownerId) count++;
			}
		}
		return count;
	}

	void clear(long ownerId) {
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				if (voice.ownerId == ownerId) voice.clear();
			}
		}
	}

	public void clear() {
		for (Voice[] channelVoices : voices) {
			for (Voice voice : channelVoices) {
				voice.clear();
			}
		}
	}

	public static Priority defaultPriority(SoundCategory category) {
		if (category == null) {
			throw new IllegalArgumentException("category is required");
		}
		switch (category) {
			case PLAYER_GUNSHOT:
			case EXTRACTION_CUE:
				return Priority.CRITICAL;
			case BOSS_CUE:
			case COMBAT_FEEDBACK:
			case UI:
				return Priority.HIGH;
			case ENEMY_GUNSHOT:
				return Priority.NORMAL;
			case FOOTSTEP:
			case AMBIENCE:
				return Priority.LOW;
			default:
				throw new IllegalStateException(
						"unknown category: " + category);
		}
	}

	public static boolean protectedByDefault(SoundCategory category) {
		return category == SoundCategory.PLAYER_GUNSHOT
				|| category == SoundCategory.EXTRACTION_CUE
				|| category == SoundCategory.UI;
	}

	private static int freeVoice(Voice[] channelVoices) {
		for (int index = 0; index < channelVoices.length; index++) {
			if (!channelVoices[index].active()) return index;
		}
		return -1;
	}

	private static int replacementVoice(
			Voice[] channelVoices,
			Priority incomingPriority,
			boolean incomingProtected) {
		int replacement = -1;
		for (int index = 0; index < channelVoices.length; index++) {
			Voice candidate = channelVoices[index];
			if ((candidate.protectedSource && !incomingProtected)
					|| candidate.priority.ordinal()
							> incomingPriority.ordinal()) {
				continue;
			}
			if (replacement < 0
					|| candidate.priority.ordinal()
							< channelVoices[replacement].priority.ordinal()
					|| candidate.priority
							== channelVoices[replacement].priority
					&& candidate.order
							< channelVoices[replacement].order) {
				replacement = index;
			}
		}
		return replacement;
	}

	private long nextToken() {
		if (nextToken <= 0L) nextToken = 1L;
		return nextToken++;
	}
}
