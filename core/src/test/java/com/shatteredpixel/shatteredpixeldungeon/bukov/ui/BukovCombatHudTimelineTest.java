package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCombatHudTimelineTest {

	@Test
	public void awarenessHoldsEightSecondsThenFadesAndActivityRestoresIt() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();

		timeline.advance(8f);
		assertEquals(1f, timeline.awarenessAlpha(), 0f);

		timeline.advance(BukovCombatHudTimeline.FADE_SECONDS * 0.5f);
		assertEquals(
				0.65f,
				timeline.awarenessAlpha(),
				0.0001f);

		timeline.advance(BukovCombatHudTimeline.FADE_SECONDS);
		assertEquals(
				BukovCombatHudTimeline.IDLE_ALPHA,
				timeline.awarenessAlpha(),
				0f);

		timeline.activity();
		assertEquals(1f, timeline.awarenessAlpha(), 0f);
	}

	@Test
	public void hitArcsExpireAtFiveHundredMilliseconds() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		assertTrue(timeline.damage(
				1,
				BukovRaidHudState.Direction.E,
				0.8f,
				false));
		assertEquals(1, timeline.hitCount());

		timeline.advance(0.499f);
		assertEquals(1, timeline.hitCount());
		timeline.advance(0.0011f);
		assertEquals(0, timeline.hitCount());
	}

	@Test
	public void sameSourceIsDeduplicatedForTwoHundredMilliseconds() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		assertTrue(timeline.damage(
				7,
				BukovRaidHudState.Direction.N,
				0.5f,
				false));
		timeline.advance(0.199f);
		assertFalse(timeline.damage(
				7,
				BukovRaidHudState.Direction.NE,
				0.9f,
				false));
		assertEquals(1, timeline.hitCount());

		timeline.advance(0.002f);
		assertTrue(timeline.damage(
				7,
				BukovRaidHudState.Direction.NE,
				0.9f,
				false));
		assertEquals(2, timeline.hitCount());
	}

	@Test
	public void keepsAtMostThreeNewestDirections() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		for (int source = 1; source <= 4; source++) {
			assertTrue(timeline.damage(
					source,
					BukovRaidHudState.Direction.values()[source],
					source / 4f,
					false));
			timeline.advance(0.01f);
		}
		assertEquals(
				BukovCombatHudTimeline.MAX_HIT_DIRECTIONS,
				timeline.hitCount());

		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("测试", 0f);
		timeline.copyTo(state);
		assertEquals(3, state.hitCount());
		assertFalse(contains(
				state,
				BukovRaidHudState.Direction.NE));
		assertTrue(contains(
				state,
				BukovRaidHudState.Direction.S));
	}

	@Test
	public void ongoingSelfDamageWakesHudWithoutDirectionArc() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		timeline.advance(12f);
		assertEquals(
				BukovCombatHudTimeline.IDLE_ALPHA,
				timeline.awarenessAlpha(),
				0f);

		assertFalse(timeline.damage(
				-1,
				BukovRaidHudState.Direction.W,
				1f,
				true));
		assertEquals(1f, timeline.awarenessAlpha(), 0f);
		assertEquals(0, timeline.hitCount());
	}

	@Test
	public void killTickAndSoundLastTwoHundredFortyMilliseconds() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		timeline.kill(8f);

		assertTrue(timeline.consumeKillSoundCue());
		assertFalse(timeline.consumeKillSoundCue());
		assertEquals(
				BukovCombatHudTimeline.KILL_TICK_SECONDS,
				timeline.killTickRemainingSeconds(),
				0f);
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("测试", 0f);
		timeline.copyTo(state);
		assertTrue(state.killConfirmationVisible());

		timeline.advance(0.239f);
		assertTrue(timeline.killTickRemainingSeconds() > 0f);
		timeline.advance(0.002f);
		assertEquals(0f, timeline.killTickRemainingSeconds(), 0f);
	}

	@Test
	public void longRangeKillWaitsForBallisticCausality() {
		BukovCombatHudTimeline timeline = new BukovCombatHudTimeline();
		float delay =
				BukovCombatHudTimeline.killConfirmationDelaySeconds(24f);
		assertEquals(
				BukovCombatHudTimeline.MAX_KILL_CONFIRM_DELAY_SECONDS,
				delay,
				0f);

		timeline.kill(24f);
		assertFalse(timeline.consumeKillSoundCue());
		assertEquals(0f, timeline.killTickRemainingSeconds(), 0f);
		timeline.advance(delay - 0.001f);
		assertFalse(timeline.consumeKillSoundCue());
		timeline.advance(0.002f);
		assertTrue(timeline.consumeKillSoundCue());
		assertEquals(
				BukovCombatHudTimeline.KILL_TICK_SECONDS,
				timeline.killTickRemainingSeconds(),
				0f);
	}

	private static boolean contains(
			BukovRaidHudState state,
			BukovRaidHudState.Direction direction) {
		for (int index = 0; index < state.hitCount(); index++) {
			if (state.hitDirection(index) == direction) return true;
		}
		return false;
	}
}
