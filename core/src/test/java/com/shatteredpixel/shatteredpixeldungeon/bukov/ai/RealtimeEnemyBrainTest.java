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
		assertEquals(RealtimeEnemyBrain.State.PATROL, brain.state());

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
	public void searchReacquiresOnlyThroughANewVisualRecord() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(2);
		brain.recordPlayer(true, 4f, 0f);
		brain.decide(0f, 0f, 0f, 4f, 0f, 1f);
		brain.recordPlayer(false, 40f, 40f);
		brain.decide(0f, 1f, 0f, 40f, 40f, 1f);
		assertEquals(RealtimeEnemyBrain.State.SEARCH, brain.state());
		assertEquals(4f, brain.navigationTargetX(), 0f);

		brain.decide(0.5f, 1f, 0f, 20f, 20f, 1f);
		assertEquals(4f, brain.navigationTargetX(), 0f);

		brain.recordPlayer(true, 7f, 1f);
		brain.decide(0f, 1f, 0f, 7f, 1f, 1f);
		assertEquals(RealtimeEnemyBrain.State.CHASE, brain.state());
		assertEquals(7f, brain.navigationTargetX(), 0f);
		assertEquals(1f, brain.navigationTargetY(), 0f);
	}

	@Test
	public void lostSightSearchesOnlyTheLastSeenPositionThenPatrols() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);
		brain.recordPlayer(true, 3f, 0f);
		brain.recordPlayer(false, 9f, 9f);

		brain.perceptionDue(0.5f);
		brain.decide(0.5f, 0f, 0f, 9f, 9f, 1f);
		assertEquals(RealtimeEnemyBrain.State.SEARCH, brain.state());
		assertEquals(3f, brain.lastSeenX(), 0f);
		assertEquals(0f, brain.lastSeenY(), 0f);
		assertEquals(3f, brain.navigationTargetX(), 0f);
		assertEquals(0f, brain.navigationTargetY(), 0f);
		assertEquals(1f, brain.desiredX(), 0f);
		assertFalse(brain.searchSweeping());

		brain.decide(0f, 3f, 0f, 9f, 9f, 1f);
		assertTrue(brain.searchSweeping());
		assertEquals(RealtimeEnemyBrain.MINIMUM_SEARCH_SECONDS,
				brain.searchRemaining(), 0f);
		assertTrue(brain.navigationTargetX() != 9f
				|| brain.navigationTargetY() != 9f);

		brain.decide(
				RealtimeEnemyBrain.MINIMUM_SEARCH_SECONDS + 0.01f,
				3f,
				0f,
				99f,
				99f,
				1f);
		assertEquals(RealtimeEnemyBrain.State.PATROL, brain.state());
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
		assertEquals(Float.MAX_VALUE, brain.lastSeenAge(), 0f);
		assertEquals(5f, brain.lastHeardX(), 0f);

		brain.perceptionDue(RealtimeEnemyBrain.SOUND_MEMORY_SECONDS + 0.01f);
		brain.decide(0.1f, 0f, 0f, 50f, 50f, 1f);
		assertEquals(RealtimeEnemyBrain.State.SEARCH, brain.state());
		assertEquals(5f, brain.navigationTargetX(), 0f);
		assertEquals(0f, brain.navigationTargetY(), 0f);
	}

	@Test
	public void searchDurationAndPatrolRouteAreStablePerEnemy() {
		for (int stableKey = 0; stableKey < 4; stableKey++) {
			RealtimeEnemyBrain brain =
					new RealtimeEnemyBrain(stableKey);
			brain.recordPlayer(true, 4f, 5f);
			brain.recordPlayer(false, 100f, 100f);
			brain.decide(0f, 4f, 5f, 100f, 100f, 1f);

			assertEquals(3f + stableKey, brain.searchRemaining(), 0f);
			assertTrue(brain.searchSweeping());
		}

		RealtimeEnemyBrain first = new RealtimeEnemyBrain(6);
		RealtimeEnemyBrain replay = new RealtimeEnemyBrain(6);
		first.decide(0f, 10f, 10f, 99f, 99f, 1f);
		replay.decide(0f, 10f, 10f, -99f, -99f, 1f);
		assertEquals(RealtimeEnemyBrain.State.PATROL, first.state());
		assertEquals(first.navigationTargetX(),
				replay.navigationTargetX(), 0f);
		assertEquals(first.navigationTargetY(),
				replay.navigationTargetY(), 0f);
	}

	@Test
	public void unreachableMemoryStartsSweepFromReachedPosition() {
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(0);
		brain.recordPlayer(true, 12f, 8f);
		brain.recordPlayer(false, 90f, 90f);
		brain.decide(0f, 1f, 1f, 90f, 90f, 1f);
		assertFalse(brain.searchSweeping());

		brain.observeNavigation(true, 4f, 3f);

		assertTrue(brain.searchSweeping());
		assertEquals(RealtimeEnemyBrain.State.SEARCH, brain.state());
		assertTrue(Math.abs(brain.navigationTargetX() - 4f) <= 2.5f);
		assertTrue(Math.abs(brain.navigationTargetY() - 3f) <= 2.5f);
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
		assertEquals(original.lastHeardAge(),
				restored.lastHeardAge(), 0f);
		assertEquals(original.lastHeardX(), restored.lastHeardX(), 0f);
		assertEquals(original.lastHeardY(), restored.lastHeardY(), 0f);
		assertEquals(original.investigatingSound(),
				restored.investigatingSound());
		assertEquals(original.navigationTargetX(),
				restored.navigationTargetX(), 0f);
		assertEquals(original.navigationTargetY(),
				restored.navigationTargetY(), 0f);
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
	public void searchStateTargetAndTimerResumeExactly() {
		RealtimeEnemyBrain original = new RealtimeEnemyBrain(3);
		original.recordPlayer(true, 8f, 3f);
		original.decide(0f, 1f, 1f, 8f, 3f, 1f);
		original.recordPlayer(false, 80f, 30f);
		original.decide(0f, 8f, 3f, 80f, 30f, 1f);
		original.decide(0.65f, 8f, 3f, 90f, 90f, 1f);

		Bundle bundle = new Bundle();
		bundle.put("brain", original.snapshot());
		RealtimeEnemyBrain restored = new RealtimeEnemyBrain(3);
		restored.restoreSnapshot(
				(RealtimeEnemyBrain.Snapshot)bundle.get("brain"));

		assertEquals(RealtimeEnemyBrain.State.SEARCH, restored.state());
		assertEquals(original.searchRemaining(),
				restored.searchRemaining(), 0f);
		assertEquals(original.searchSweeping(),
				restored.searchSweeping());
		assertEquals(original.navigationTargetX(),
				restored.navigationTargetX(), 0f);
		assertEquals(original.navigationTargetY(),
				restored.navigationTargetY(), 0f);

		original.decide(0.2f, 8f, 3f, 40f, 40f, 1f);
		restored.decide(0.2f, 8f, 3f, -40f, -40f, 1f);
		assertEquals(original.state(), restored.state());
		assertEquals(original.searchRemaining(),
				restored.searchRemaining(), 0f);
		assertEquals(original.navigationTargetX(),
				restored.navigationTargetX(), 0f);
		assertEquals(original.navigationTargetY(),
				restored.navigationTargetY(), 0f);
	}

	@Test
	public void legacySharedSoundMemoryMigratesToHeardTarget() {
		Bundle legacy = new Bundle();
		legacy.put("state", RealtimeEnemyBrain.State.INVESTIGATE);
		legacy.put("perception_remaining", 0.04f);
		legacy.put("last_seen_age", 0.7f);
		legacy.put("last_seen_x", 6f);
		legacy.put("last_seen_y", 2f);
		legacy.put("desired_x", 1f);
		legacy.put("desired_y", 0f);
		legacy.put("attack_cooldown", 0f);
		legacy.put("sees_player", false);
		legacy.put("investigating_sound", true);
		legacy.put("attack_requested", false);
		RealtimeEnemyBrain.Snapshot snapshot =
				new RealtimeEnemyBrain.Snapshot();
		snapshot.restoreFromBundle(legacy);
		RealtimeEnemyBrain brain = new RealtimeEnemyBrain(1);
		brain.restoreSnapshot(snapshot);

		assertEquals(0.7f, brain.lastHeardAge(), 0f);
		assertEquals(6f, brain.lastHeardX(), 0f);
		assertEquals(2f, brain.lastHeardY(), 0f);
		brain.decide(0f, 1f, 2f, 99f, 99f, 1f);
		assertEquals(RealtimeEnemyBrain.State.INVESTIGATE, brain.state());
		assertEquals(6f, brain.navigationTargetX(), 0f);
		assertEquals(2f, brain.navigationTargetY(), 0f);
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
