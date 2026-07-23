package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovPerformancePolicyTest {

	@Test
	public void highQualityKeepsAllAuthoredCombatFx() {
		for (CombatFxEvent.Type type : CombatFxEvent.Type.values()) {
			for (int sequence = 0; sequence < 8; sequence++) {
				assertTrue(BukovPerformancePolicy.renderCombatFx(
						BukovPerformancePolicy.HIGH_QUALITY,
						type,
						sequence));
			}
		}
	}

	@Test
	public void highFrameRateDeterministicallyReducesPresentationOnly() {
		assertTrue(BukovPerformancePolicy.renderCombatFx(
				BukovPerformancePolicy.HIGH_FRAME_RATE,
				CombatFxEvent.Type.TRACER,
				2));
		assertFalse(BukovPerformancePolicy.renderCombatFx(
				BukovPerformancePolicy.HIGH_FRAME_RATE,
				CombatFxEvent.Type.TRACER,
				3));
		assertTrue(BukovPerformancePolicy.renderCombatFx(
				BukovPerformancePolicy.HIGH_FRAME_RATE,
				CombatFxEvent.Type.IMPACT,
				4));
		assertFalse(BukovPerformancePolicy.renderCombatFx(
				BukovPerformancePolicy.HIGH_FRAME_RATE,
				CombatFxEvent.Type.IMPACT,
				2));
	}
}
