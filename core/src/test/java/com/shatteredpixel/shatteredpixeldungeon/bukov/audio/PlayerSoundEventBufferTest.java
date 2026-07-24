package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayerSoundEventBufferTest {

	@Test
	public void sameFixedStepKeepsDifferentPositionsAndRadii() {
		PlayerSoundEventBuffer sounds = new PlayerSoundEventBuffer(4);
		int footsteps = sounds.emit(2f, 3f, 4f, 0.35f);
		int gunshot = sounds.emit(12f, 3f, 18f, 0.35f);

		assertNotEquals(footsteps, gunshot);
		assertEquals(2, sounds.activeCount());
		assertEquals(footsteps, sounds.eventAt(0).sequence());
		assertEquals(4f, sounds.eventAt(0).radius(), 0f);
		assertEquals(gunshot, sounds.eventAt(1).sequence());
		assertEquals(18f, sounds.eventAt(1).radius(), 0f);
	}

	@Test
	public void differentListenersCanChooseDifferentEventsFromSameBatch() {
		PlayerSoundEventBuffer sounds = new PlayerSoundEventBuffer(4);
		sounds.emit(2f, 2f, 8f, 0.35f);
		sounds.emit(12f, 2f, 8f, 0.35f);

		PlayerSoundEventBuffer.Event left = bestFor(sounds, 3f, 2f);
		PlayerSoundEventBuffer.Event right = bestFor(sounds, 11f, 2f);

		assertNotNull(left);
		assertNotNull(right);
		assertEquals(2f, left.x(), 0f);
		assertEquals(12f, right.x(), 0f);
	}

	@Test
	public void expiredEventsAreRemovedWithoutMovingOtherSlots() {
		PlayerSoundEventBuffer sounds = new PlayerSoundEventBuffer(3);
		sounds.emit(1f, 1f, 5f, 0.1f);
		int survivor = sounds.emit(2f, 2f, 6f, 0.5f);

		sounds.advance(0.11f);

		assertEquals(1, sounds.activeCount());
		assertTrue(!sounds.eventAt(0).active());
		assertEquals(survivor, sounds.eventAt(1).sequence());
		assertTrue(sounds.eventAt(1).active());
	}

	@Test
	public void saturationDropsOldestAndKeepsNewestEvidence() {
		PlayerSoundEventBuffer sounds = new PlayerSoundEventBuffer(2);
		int oldest = sounds.emit(1f, 1f, 5f, 1f);
		int middle = sounds.emit(2f, 1f, 5f, 1f);
		int newest = sounds.emit(3f, 1f, 5f, 1f);

		assertEquals(2, sounds.activeCount());
		assertEquals(1L, sounds.dropped());
		assertTrue(!contains(sounds, oldest));
		assertTrue(contains(sounds, middle));
		assertTrue(contains(sounds, newest));
	}

	@Test
	public void snapshotAndLegacySingleSlotRestoreExactly() {
		PlayerSoundEventBuffer original = new PlayerSoundEventBuffer(4);
		original.emit(4f, 5f, 9f, 0.35f);
		original.emit(7f, 8f, 12f, 0.2f);
		PlayerSoundEventBuffer restored = new PlayerSoundEventBuffer(4);
		restored.restore(roundTrip(original.snapshot()));

		assertEquals(2, restored.activeCount());
		assertEquals(4f, restored.eventAt(0).x(), 0f);
		assertEquals(12f, restored.eventAt(1).radius(), 0f);

		PlayerSoundEventBuffer legacy = new PlayerSoundEventBuffer(4);
		legacy.restore(
				PlayerSoundEventBuffer.Snapshot.legacySingleSlot(
						41, 9f, 3f, 14f, 0.25f));
		assertEquals(1, legacy.activeCount());
		assertEquals(41, legacy.eventAt(0).sequence());
		assertEquals(9f, legacy.eventAt(0).x(), 0f);
		int next = legacy.emit(10f, 3f, 14f, 0.25f);
		assertEquals(42, next);
	}

	private static PlayerSoundEventBuffer.Snapshot roundTrip(
			PlayerSoundEventBuffer.Snapshot source) {
		Bundle bundle = new Bundle();
		source.storeInBundle(bundle);
		PlayerSoundEventBuffer.Snapshot restored =
				new PlayerSoundEventBuffer.Snapshot();
		restored.restoreFromBundle(bundle);
		return restored;
	}

	private static PlayerSoundEventBuffer.Event bestFor(
			PlayerSoundEventBuffer sounds,
			float listenerX,
			float listenerY) {
		PlayerSoundEventBuffer.Event best = null;
		float bestThreat = -1f;
		for (int slot = 0; slot < sounds.capacity(); slot++) {
			PlayerSoundEventBuffer.Event event = sounds.eventAt(slot);
			if (!event.active()) continue;
			float dx = event.x() - listenerX;
			float dy = event.y() - listenerY;
			float distance = (float)Math.sqrt(dx * dx + dy * dy);
			if (distance > event.radius()) continue;
			float threat = event.radius() / (1f + distance);
			if (threat > bestThreat) {
				best = event;
				bestThreat = threat;
			}
		}
		return best;
	}

	private static boolean contains(
			PlayerSoundEventBuffer sounds, int sequence) {
		for (int slot = 0; slot < sounds.capacity(); slot++) {
			PlayerSoundEventBuffer.Event event = sounds.eventAt(slot);
			if (event.active() && event.sequence() == sequence) return true;
		}
		return false;
	}
}
