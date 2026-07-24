package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import org.junit.Test;

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
	public void everyProfileKeepsCompleteFeedbackForEveryShot() {
		for (int profile = BukovPerformancePolicy.HIGH_QUALITY;
				profile <= BukovPerformancePolicy.HIGH_FRAME_RATE;
				profile++) {
			for (int sequence = 0; sequence < 16; sequence++) {
				for (CombatFxEvent.Type type : CombatFxEvent.Type.values()) {
					assertTrue(BukovPerformancePolicy.renderCombatFx(
							profile,
							type,
							sequence));
				}
			}
		}
	}
}
