package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovBalanceReportTest {

	@Test
	public void tenFixedSeedsCoverModeDurationsAndExpeditionFirefights() {
		List<RaidBalanceTelemetry> raids = new ArrayList<>();
		BukovRaidMode[] modes = BukovRaidMode.values();
		for (int index = 0; index < 10; index++) {
			BukovRaidMode mode = modes[index % modes.length];
			float duration = (mode.targetMinimumSeconds()
					+ mode.targetMaximumSeconds()) / 2f;
			raids.add(settled(
					1000L + index,
					mode,
					duration,
					mode == BukovRaidMode.EXPEDITION ? 5 : 2));
		}

		BukovBalanceReport report =
				BukovBalanceReport.analyze(raids);

		assertTrue(report.passes());
		assertEquals(10, report.raidCount());
		assertEquals(10, report.completeRaidCount());
		assertEquals(1000L, report.totalExtractedValue());
		assertEquals(0, report.deaths());
		assertTrue(report.violations().isEmpty());
	}

	@Test
	public void reportFlagsDurationFirefightAndDuplicateSeedDrift() {
		List<RaidBalanceTelemetry> raids = new ArrayList<>();
		for (int index = 0; index < 10; index++) {
			raids.add(settled(
					index == 9 ? 0L : index,
					BukovRaidMode.EXPEDITION,
					index == 1 ? 60f : 15f * 60f,
					index == 2 ? 9 : 5));
		}

		BukovBalanceReport report =
				BukovBalanceReport.analyze(raids);

		assertFalse(report.passes());
		assertEquals(4, report.violations().size());
		assertTrue(report.violations().get(0).contains("duration"));
		assertTrue(report.violations().get(1).contains("firefights"));
		assertTrue(report.violations().get(2).contains("duplicate seed"));
		assertTrue(report.violations().get(3).contains("unique seeds"));
	}

	private static RaidBalanceTelemetry settled(
			long seed,
			BukovRaidMode mode,
			float duration,
			int firefights) {
		RaidBalanceTelemetry telemetry =
				RaidBalanceTelemetry.begin(seed, mode);
		telemetry.identifyContext("fog_depot", "balanced_mid");
		for (int index = 0; index < firefights; index++) {
			telemetry.recordFirefight();
		}
		return telemetry.settle(
				duration,
				1,
				100L,
				RaidBalanceTelemetry.End.BASIC_EXTRACTION);
	}
}
