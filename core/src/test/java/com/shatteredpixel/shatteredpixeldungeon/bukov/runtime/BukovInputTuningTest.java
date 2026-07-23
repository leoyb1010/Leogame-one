package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovInputTuningTest {

	@Test
	public void innerDeadZoneRejectsDriftAndOuterZoneSaturates() {
		PointF output = new PointF();

		BukovInputTuning.sampleStick(
				0.10f, 0.05f, 0.16f, 0.96f, false, output);
		assertEquals(0f, output.x, 0f);
		assertEquals(0f, output.y, 0f);

		BukovInputTuning.sampleStick(
				1f, 0f, 0.16f, 0.96f, false, output);
		assertEquals(1f, output.x, 0.0001f);
		assertEquals(0f, output.y, 0f);
	}

	@Test
	public void movementResponsePreservesAnalogMagnitude() {
		PointF output = new PointF();
		BukovInputTuning.sampleStick(
				0.40f, 0f, 0.10f, 1f, false, output);

		assertTrue(output.x > 0.30f);
		assertTrue(output.x < 0.40f);
	}

	@Test
	public void classicCurveIsSofterNearCenterAndAssistMatchesPlan() {
		PointF linear = new PointF();
		PointF classic = new PointF();
		BukovInputTuning.sampleStick(
				0.35f, 0f, 0.16f, 0.96f, false, linear);
		BukovInputTuning.sampleStick(
				0.35f, 0f, 0.16f, 0.96f, true, classic);

		assertTrue(classic.x < linear.x);
		assertEquals(0f, BukovInputTuning.aimAssistScale(0), 0f);
		assertEquals(0.15f, BukovInputTuning.aimAssistScale(1), 0f);
		assertEquals(0.30f, BukovInputTuning.aimAssistScale(2), 0f);
	}
}
