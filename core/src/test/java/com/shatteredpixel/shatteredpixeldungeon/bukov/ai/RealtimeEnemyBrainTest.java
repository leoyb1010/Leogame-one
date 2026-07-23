package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.watabou.utils.Bundle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeEnemyBrainTest {

	@Test
	public void visiblePlayerTransitionsFromChaseToCooldownGatedAttack() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);

		assertTrue(brain.perceptionDue(0.01f));
		brain.recordPlayer(true, 4f, 0f);
		brain.decide(0.01f, 0f, 0f, 4f, 0f, 1f);

		assertEquals(RealtimeEnemyBrain.State.CHASE, brain.state());
		assertEquals(1f, brain.desiredX(), 0.0001f);
		assertEquals(0f, brain.desiredY(), 0.0001f);

		brain.decide(0.01f, 3.25f, 0f, 4f, 0f, 1f);
		assertEquals(RealtimeEnemyBrain.State.ATTACK, brain.state());
		assertTrue(brain.consumeAttack(0.8f));
		assertFalse(brain.consumeAttack(0.8f));

		brain.decide(0.4f, 3.25f, 0f, 4f, 0f, 1f);
		assertFalse(brain.consumeAttack(0.8f));
		brain.decide(0.4f, 3.25f, 0f, 4f, 0f, 1f);
		assertTrue(brain.consumeAttack(0.8f));
	}

	@Test
	public void sightMemoryExpiresInsteadOfTrackingPlayerThroughWalls() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);
		brain.recordPlayer(true, 3f, 0f);
		brain.recordPlayer(false, 9f, 9f);

		brain.perceptionDue(0.5f);
		brain.decide(0.5f, 0f, 0f, 9f, 9f, 1f);
		assertEquals(RealtimeEnemyBrain.State.CHASE, brain.state());
		assertTrue(brain.desiredX() > 0f);

		brain.perceptionDue(0.8f);
		brain.decide(0.8f, 0f, 0f, 9f, 9f, 1f);
		assertEquals(RealtimeEnemyBrain.State.IDLE, brain.state());
		assertEquals(0f, brain.desiredX(), 0f);
		assertEquals(0f, brain.desiredY(), 0f);
	}

	@Test
	public void perceptionSlotsAreDeterministicallyStaggered() {
		RealtimeEnemyBrain first = new RealtimeEnemyBrain(0);
		RealtimeEnemyBrain fifth = new RealtimeEnemyBrain(5);

		assertTrue(first.perceptionDue(0.01f));
		assertFalse(fifth.perceptionDue(0.01f));
		assertFalse(fifth.perceptionDue(0.03f));
		assertTrue(fifth.perceptionDue(0.02f));
	}

	@Test
	public void audibleGunshotCreatesFiniteInvestigationWithoutWallTracking() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);

		brain.recordSound(5f, 0f);
		brain.decide(0.1f, 0f, 0f, 50f, 50f, 1f);

		assertEquals(RealtimeEnemyBrain.State.INVESTIGATE, brain.state());
		assertEquals(1f, brain.desiredX(), 0.0001f);
		assertFalse(brain.seesPlayer());

		brain.perceptionDue(RealtimeEnemyBrain.SOUND_MEMORY_SECONDS + 0.01f);
		brain.decide(0.1f, 0f, 0f, 50f, 50f, 1f);
		assertEquals(RealtimeEnemyBrain.State.IDLE, brain.state());
	}

	@Test
	public void deadBrainNeverMovesOrAttacks() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);
		brain.recordPlayer(true, 1f, 0f);
		brain.markDead();
		brain.decide(1f, 0f, 0f, 1f, 0f, 2f);

		assertEquals(RealtimeEnemyBrain.State.DEAD, brain.state());
		assertEquals(0f, brain.desiredX(), 0f);
		assertFalse(brain.consumeAttack(1f));
		assertFalse(brain.perceptionDue(1f));
	}

	@Test
	public void alertMemoryAndContactCooldownResumeExactly() {
		RealtimeEnemyBrain original = new RealtimeEnemyBrain(3);
		original.recordSound(7.5f, 2.25f);
		original.perceptionDue(0.35f);
		original.decide(0.1f, 1f, 1f, 99f, 99f, 1f);

		Bundle bundle = new Bundle();
		bundle.put("brain", original.snapshot());
		RealtimeEnemyBrain.Snapshot snapshot =
				(RealtimeEnemyBrain.Snapshot)bundle.get("brain");
		RealtimeEnemyBrain restored = new RealtimeEnemyBrain(3);
		restored.restoreSnapshot(snapshot);

		assertEquals(original.state(), restored.state());
		assertEquals(original.lastSeenAge(), restored.lastSeenAge(), 0f);
		assertEquals(original.lastSeenX(), restored.lastSeenX(), 0f);
		assertEquals(original.lastSeenY(), restored.lastSeenY(), 0f);
		assertEquals(original.investigatingSound(),
				restored.investigatingSound());
		assertEquals(original.perceptionRemaining(),
				restored.perceptionRemaining(), 0f);
		assertEquals(original.attackCooldown(),
				restored.attackCooldown(), 0f);

		original.perceptionDue(0.4f);
		restored.perceptionDue(0.4f);
		original.decide(0.4f, 1f, 1f, 99f, 99f, 1f);
		restored.decide(0.4f, 1f, 1f, 99f, 99f, 1f);
		assertEquals(original.state(), restored.state());
		assertEquals(original.desiredX(), restored.desiredX(), 0f);
		assertEquals(original.desiredY(), restored.desiredY(), 0f);
	}

	@Test
	public void contactAttackCooldownDoesNotResetOnResume() {
		RealtimeEnemyBrain original = new RealtimeEnemyBrain(0);
		original.recordPlayer(true, 1f, 0f);
		original.decide(0f, 0f, 0f, 1f, 0f, 2f);
		assertTrue(original.consumeAttack(0.8f));

		RealtimeEnemyBrain restored = new RealtimeEnemyBrain(0);
		restored.restoreSnapshot(original.snapshot());
		assertEquals(0.8f, restored.attackCooldown(), 0f);

		original.decide(0.3f, 0f, 0f, 1f, 0f, 2f);
		restored.decide(0.3f, 0f, 0f, 1f, 0f, 2f);
		assertFalse(original.consumeAttack(0.8f));
		assertFalse(restored.consumeAttack(0.8f));
		assertEquals(original.attackCooldown(),
				restored.attackCooldown(), 0f);
	}
}
