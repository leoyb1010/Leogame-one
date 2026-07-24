package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.PlayerSoundEventBuffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovPumpActivationPolicyTest {

	@Test
	public void pumpBroadcastUsesPumpPositionAndFeedsInvestigation() {
		PlayerSoundEventBuffer sounds = new PlayerSoundEventBuffer(2);
		int sequence = BukovRealtimeWorld.emitPumpBroadcast(
				sounds,
				23,
				10);

		assertEquals(1, sounds.activeCount());
		PlayerSoundEventBuffer.Event event = sounds.eventAt(0);
		assertEquals(sequence, event.sequence());
		assertEquals(3.5f, event.x(), 0f);
		assertEquals(2.5f, event.y(), 0f);
		assertTrue(event.radius() >= 20f);
		assertTrue(event.remainingSeconds() >= 1f);

		RealtimeEnemyBrain investigator = new RealtimeEnemyBrain(0);
		investigator.recordSound(event.x(), event.y());
		investigator.decide(0.1f, 0.5f, 2.5f, 99f, 99f, 1f);

		assertEquals(
				RealtimeEnemyBrain.State.INVESTIGATE,
				investigator.state());
		assertEquals(1f, investigator.desiredX(), 0.0001f);
		assertEquals(0f, investigator.desiredY(), 0.0001f);
	}

	@Test
	public void investigatorDeadlineOnlyAcceleratesExistingSpawn() {
		assertEquals(
				21.25f,
				BukovRealtimeWorld.investigatorSpawnDeadline(50f, 20f),
				0f);
		assertEquals(
				10f,
				BukovRealtimeWorld.investigatorSpawnDeadline(10f, 20f),
				0f);
	}
}
