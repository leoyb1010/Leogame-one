package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the production-wide per-bus voice budget while keeping each
 * playback owner responsible for its own clock and lifecycle.
 */
final class BukovSoundConcurrencyRuntime {

	private final SoundConcurrencyBudget budget =
			new SoundConcurrencyBudget();
	private final List<BukovConcurrentSoundPlayer> activePlayers =
			new ArrayList<>();
	private long nextOwnerId = 1L;

	long newOwnerId() {
		if (nextOwnerId <= 0L) nextOwnerId = 1L;
		return nextOwnerId++;
	}

	SoundConcurrencyBudget.Admission admit(
			BukovConcurrentSoundPlayer player,
			long ownerId,
			AudioChannel channel,
			SoundConcurrencyBudget.Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		register(player);
		SoundConcurrencyBudget.Admission admission = budget.admit(
				ownerId,
				channel,
				priority,
				protectedSource,
				timeoutSeconds);
		if (admission.admitted()) {
			stopEvictedPlaybackAcrossOwners();
		}
		pruneInactivePlayers();
		return admission;
	}

	void update(long ownerId, float deltaSeconds) {
		budget.update(ownerId, deltaSeconds);
	}

	boolean release(long ownerId, long token) {
		return budget.release(ownerId, token);
	}

	boolean active(long ownerId, long token) {
		return budget.active(ownerId, token);
	}

	int activeCount(AudioChannel channel) {
		return budget.activeCount(channel);
	}

	void clear(long ownerId) {
		budget.clear(ownerId);
	}

	void ownerBecameIdle(BukovConcurrentSoundPlayer player, long ownerId) {
		if (budget.activeCount(ownerId) == 0) {
			activePlayers.remove(player);
		}
	}

	private void register(BukovConcurrentSoundPlayer player) {
		if (!activePlayers.contains(player)) {
			activePlayers.add(player);
		}
	}

	private void stopEvictedPlaybackAcrossOwners() {
		// stopInactivePlaybacks() never changes the owner registry, so iterating
		// the warmed list directly keeps rapid gunfire allocation-free.
		for (int index = 0; index < activePlayers.size(); index++) {
			activePlayers.get(index).stopInactivePlaybacks();
		}
	}

	private void pruneInactivePlayers() {
		for (int index = activePlayers.size() - 1; index >= 0; index--) {
			BukovConcurrentSoundPlayer player = activePlayers.get(index);
			if (budget.activeCount(player.ownerId()) == 0) {
				activePlayers.remove(index);
			}
		}
	}
}
