package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovUiMotionSchedulerTest {

	@Test
	public void ninthMotionCompletesOldestAndKeepsEightActive() {
		BukovUiMotionScheduler scheduler =
				new BukovUiMotionScheduler();
		Object[] owners =
				new Object[BukovUiMotionScheduler.MAX_ACTIVE_MOTIONS + 1];

		for (int index = 0; index < owners.length; index++) {
			owners[index] = new Object();
			scheduler.start(
					owners[index],
					0,
					0f,
					10f + index,
					1000);
		}

		assertEquals(
				BukovUiMotionScheduler.MAX_ACTIVE_MOTIONS,
				scheduler.activeCount());
		assertFalse(scheduler.isActive(owners[0], 0));
		assertEquals(10f, scheduler.value(owners[0], 0), 0f);
		assertTrue(scheduler.isActive(owners[1], 0));
		assertTrue(scheduler.isActive(owners[8], 0));
	}

	@Test
	public void restartRedirectsFromCurrentValueWithoutJump() {
		BukovUiMotionScheduler scheduler =
				new BukovUiMotionScheduler();
		Object owner = new Object();
		scheduler.start(owner, 7, 0f, 10f, 1000);
		scheduler.update(0.5f);

		assertEquals(5f, scheduler.value(owner, 7), 0.0001f);

		scheduler.start(owner, 7, 999f, 20f, 1000);

		assertEquals(5f, scheduler.value(owner, 7), 0.0001f);
		assertEquals(1, scheduler.activeCount());
		scheduler.update(0.5f);
		assertEquals(12.5f, scheduler.value(owner, 7), 0.0001f);
	}

	@Test
	public void elapsedTimeProducesSameValueAtDifferentFrameRates() {
		BukovUiMotionScheduler oneFrame =
				new BukovUiMotionScheduler();
		BukovUiMotionScheduler tenFrames =
				new BukovUiMotionScheduler();
		Object oneFrameOwner = new Object();
		Object tenFramesOwner = new Object();
		oneFrame.start(oneFrameOwner, 0, -4f, 16f, 2000);
		tenFrames.start(tenFramesOwner, 0, -4f, 16f, 2000);

		oneFrame.update(1f);
		for (int index = 0; index < 10; index++) {
			tenFrames.update(0.1f);
		}

		assertEquals(
				oneFrame.value(oneFrameOwner, 0),
				tenFrames.value(tenFramesOwner, 0),
				0.0001f);
		assertEquals(6f, oneFrame.value(oneFrameOwner, 0), 0.0001f);
	}

	@Test
	public void cancelToEndPublishesExactTerminalValue() {
		BukovUiMotionScheduler scheduler =
				new BukovUiMotionScheduler();
		Object owner = new Object();
		scheduler.start(owner, 2, 3f, 9f, 1200);
		scheduler.update(0.2f);

		assertTrue(scheduler.cancelToEnd(owner, 2));
		assertEquals(9f, scheduler.value(owner, 2), 0f);
		assertFalse(scheduler.isActive(owner, 2));
		assertEquals(0, scheduler.activeCount());
		assertFalse(scheduler.cancelToEnd(owner, 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void zeroDurationIsRejected() {
		new BukovUiMotionScheduler().start(
				new Object(), 0, 0f, 1f, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeDurationIsRejected() {
		new BukovUiMotionScheduler().start(
				new Object(), 0, 0f, 1f, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidDeltaIsRejected() {
		new BukovUiMotionScheduler().update(Float.NaN);
	}
}
