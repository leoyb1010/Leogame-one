package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FootstepCadenceTest {

	@Test
	public void stationaryFixedStepsNeverEmitFootsteps() {
		FootstepCadence cadence = new FootstepCadence();
		for (int frame = 0; frame < 240; frame++) {
			assertFalse(cadence.advance(0f, 0f, 1f / 120f));
		}
	}

	@Test
	public void acceptedMovementEmitsByStrideRatherThanEveryFixedStep() {
		FootstepCadence cadence = new FootstepCadence();
		int emitted = 0;
		for (int frame = 0; frame < 120; frame++) {
			if (cadence.advance(4.25f / 120f, 0f, 1f / 120f)) {
				emitted++;
			}
		}
		assertTrue("one second of movement should have several steps",
				emitted >= 3);
		assertTrue("footsteps must remain cadence-limited", emitted <= 5);
	}

	@Test
	public void collisionBlockedInputDoesNotAdvanceCadence() {
		FootstepCadence cadence = new FootstepCadence();
		for (int frame = 0; frame < 120; frame++) {
			assertFalse(cadence.advance(0f, 0f, 1f / 120f));
		}
		assertTrue(cadence.advance(
				FootstepCadence.FIRST_STEP_DISTANCE_TILES,
				0f,
				1f / 120f));
	}
}
