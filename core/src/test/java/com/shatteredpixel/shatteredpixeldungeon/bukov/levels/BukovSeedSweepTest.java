package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BukovSeedSweepTest {

	private static final long[] REGRESSION_SEEDS = {
			1L, 2L, 3L, 7L, 11L,
			42L, 99L, 256L, 1024L, 4096L,
			94823742L, 117013337L, 314159265L, 987654321L, 2147483647L,
			-1L, -42L, Long.MIN_VALUE, Long.MAX_VALUE, 0x5EEDB00FL
	};

	@Test
	public void twentyFixedSeedsMeetFirstRaidHardConstraints() {
		for (long seed : REGRESSION_SEEDS) {
			RaidMapValidator.Result result =
					RaidMapValidator.validate(BukovZonePlanner.generateFirstRaid(seed));
			assertTrue("seed=" + seed + " failure=" + result.failure + " reason=" + result.reason,
					result.valid);
		}
	}

	@Test
	public void optionalSweepCountAlsoProducesOnlyValidSeeds() {
		int count = Integer.getInteger("bukov.seed.count", 20);
		long state = 0x42554B4F56L;
		for (int i = 0; i < count; i++) {
			state = state * 6364136223846793005L + 1442695040888963407L;
			RaidMapValidator.Result result =
					RaidMapValidator.validate(BukovZonePlanner.generateFirstRaid(state));
			assertTrue("index=" + i + " seed=" + state + " failure=" + result.failure
					+ " reason=" + result.reason, result.valid);
		}
	}
}
