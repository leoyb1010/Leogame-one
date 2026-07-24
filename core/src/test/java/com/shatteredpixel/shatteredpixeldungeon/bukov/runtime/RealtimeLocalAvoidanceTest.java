package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealtimeLocalAvoidanceTest {

	@Test
	public void exactOverlapProducesOppositeDeterministicSeparation() {
		RealtimeLocalAvoidance first = new RealtimeLocalAvoidance(10);
		RealtimeLocalAvoidance second = new RealtimeLocalAvoidance(20);
		first.begin(0f, 0f);
		second.begin(0f, 0f);

		first.avoid(3f, 3f, 3f, 3f, 20, 0.8f);
		second.avoid(3f, 3f, 3f, 3f, 10, 0.8f);

		assertEquals(-first.desiredX(), second.desiredX(), 0f);
		assertEquals(-first.desiredY(), second.desiredY(), 0f);
		assertTrue(Math.abs(first.desiredX())
				+ Math.abs(first.desiredY()) > 0.9f);
	}

	@Test
	public void nearbyAllyDeflectsMovementWithoutReversingProgress() {
		RealtimeLocalAvoidance avoidance = new RealtimeLocalAvoidance(1);
		avoidance.begin(1f, 0f);

		avoidance.avoid(2f, 2f, 2.3f, 2f, 2, 0.8f);

		assertTrue(avoidance.desiredX() > 0f);
		assertTrue(Math.abs(avoidance.desiredY()) <= 1f);
		float length = (float)Math.sqrt(
				avoidance.desiredX() * avoidance.desiredX()
						+ avoidance.desiredY() * avoidance.desiredY());
		assertEquals(1f, length, 0.0001f);
	}

	@Test
	public void distantAllyDoesNotChangeIntent() {
		RealtimeLocalAvoidance avoidance = new RealtimeLocalAvoidance(1);
		avoidance.begin(0.6f, 0.8f);

		avoidance.avoid(1f, 1f, 5f, 5f, 2, 0.8f);

		assertEquals(0.6f, avoidance.desiredX(), 0.0001f);
		assertEquals(0.8f, avoidance.desiredY(), 0.0001f);
	}
}
