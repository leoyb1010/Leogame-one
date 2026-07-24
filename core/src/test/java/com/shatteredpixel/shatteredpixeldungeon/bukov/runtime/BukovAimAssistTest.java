package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovAimAssistTest {

	@Test
	public void rejectsOccludedOutOfRangeAndBehindAimTargets() {
		assertFalse(BukovAimAssist.accepts(
				1f, 0f, 4f, 0.5f, 8f, false));
		assertFalse(BukovAimAssist.accepts(
				1f, 0f, 9f, 0f, 8f, true));
		assertFalse(BukovAimAssist.accepts(
				1f, 0f, -2f, 0f, 8f, true));
		assertTrue(BukovAimAssist.accepts(
				1f, 0f, 4f, 0.5f, 8f, true));
	}

	@Test
	public void strengthChangesDirectionWithoutHardLocking() {
		PointF low = new PointF();
		PointF high = new PointF();
		BukovAimAssist.blend(1f, 0f, 4f, 1f, 0.15f, low);
		BukovAimAssist.blend(1f, 0f, 4f, 1f, 0.30f, high);

		assertTrue(low.y > 0f);
		assertTrue(high.y > low.y);
		assertTrue(high.y < 1f / (float)Math.sqrt(17f));
		assertEquals(1f,
				(float)Math.sqrt(high.x * high.x + high.y * high.y),
				0.0001f);
	}
}
