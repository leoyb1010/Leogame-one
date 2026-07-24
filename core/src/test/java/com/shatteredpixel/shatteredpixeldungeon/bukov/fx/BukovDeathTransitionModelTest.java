package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovDeathTransitionModelTest {

	private static final float EPSILON = 0.000001f;

	@Test
	public void completesAtOneSecondAndCompletionIsConsumedOnce() {
		BukovDeathTransitionModel model =
				new BukovDeathTransitionModel();

		assertEquals(0f, model.progress(), 0f);
		assertFalse(model.complete());
		assertFalse(model.consumeCompletion());

		model.advance(0.999f);
		assertEquals(0.999f, model.progress(), EPSILON);
		assertFalse(model.complete());
		assertFalse(model.consumeCompletion());

		model.advance(0.001f);
		assertEquals(1f, model.progress(), 0f);
		assertTrue(model.complete());
		assertTrue(model.consumeCompletion());
		assertFalse(model.consumeCompletion());
	}

	@Test
	public void largeDeltaClampsWithoutOverflowOrRepeatedCompletion() {
		BukovDeathTransitionModel model =
				new BukovDeathTransitionModel();

		model.advance(Float.MAX_VALUE);
		model.advance(Float.MAX_VALUE);

		assertEquals(1f, model.progress(), 0f);
		assertTrue(model.complete());
		assertTrue(model.consumeCompletion());
		assertFalse(model.consumeCompletion());
	}

	@Test
	public void veilRespectsMotionAndFlashAccessibility() {
		BukovDeathTransitionModel model =
				new BukovDeathTransitionModel();

		assertEquals(0f, model.veilAlpha(false, false), 0f);
		assertEquals(0.50f, model.veilAlpha(true, false), 0f);
		assertEquals(0.25f, model.veilAlpha(true, true), 0f);

		model.advance(0.25f);
		assertEquals(0.125f, model.veilAlpha(false, false), EPSILON);
		assertEquals(0.0625f, model.veilAlpha(false, true), EPSILON);

		model.advance(0.75f);
		assertEquals(0.50f, model.veilAlpha(false, false), 0f);
		assertEquals(0.25f, model.veilAlpha(false, true), 0f);
	}

	@Test
	public void zeroDeltaIsStable() {
		BukovDeathTransitionModel model =
				new BukovDeathTransitionModel();

		model.advance(0f);

		assertEquals(0f, model.progress(), 0f);
		assertFalse(model.complete());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNegativeDelta() {
		new BukovDeathTransitionModel().advance(-0.001f);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNanDelta() {
		new BukovDeathTransitionModel().advance(Float.NaN);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsPositiveInfinityDelta() {
		new BukovDeathTransitionModel().advance(
				Float.POSITIVE_INFINITY);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNegativeInfinityDelta() {
		new BukovDeathTransitionModel().advance(
				Float.NEGATIVE_INFINITY);
	}
}
