package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure offline acceptance report for one fixed ten-seed balance pass. */
public final class BukovBalanceReport {

	public static final int REQUIRED_SEED_COUNT = 10;
	public static final int EXPEDITION_MIN_FIREFIGHTS = 3;
	public static final int EXPEDITION_MAX_FIREFIGHTS = 8;

	private final int raidCount;
	private final int completeRaidCount;
	private final float averageDurationSeconds;
	private final float averageFirefights;
	private final long totalExtractedValue;
	private final int deaths;
	private final List<String> violations;

	private BukovBalanceReport(
			int raidCount,
			int completeRaidCount,
			float averageDurationSeconds,
			float averageFirefights,
			long totalExtractedValue,
			int deaths,
			List<String> violations) {
		this.raidCount = raidCount;
		this.completeRaidCount = completeRaidCount;
		this.averageDurationSeconds = averageDurationSeconds;
		this.averageFirefights = averageFirefights;
		this.totalExtractedValue = totalExtractedValue;
		this.deaths = deaths;
		this.violations = Collections.unmodifiableList(
				new ArrayList<>(violations));
	}

	public static BukovBalanceReport analyze(
			Collection<RaidBalanceTelemetry> raids) {
		if (raids == null) {
			throw new IllegalArgumentException("raids are required");
		}
		List<String> violations = new ArrayList<>();
		Set<Long> seeds = new HashSet<>();
		int complete = 0;
		double durationTotal = 0d;
		long firefightTotal = 0L;
		long extractedTotal = 0L;
		int deathCount = 0;
		int index = 0;
		for (RaidBalanceTelemetry raid : raids) {
			if (raid == null || !raid.available() || !raid.settled()) {
				violations.add("raid " + index + " lacks complete telemetry");
				index++;
				continue;
			}
			complete++;
			durationTotal += raid.durationSeconds();
			firefightTotal += raid.firefights();
			extractedTotal = saturatedAdd(
					extractedTotal,
					raid.extractedValue());
			if (raid.end() == RaidBalanceTelemetry.End.DEATH) {
				deathCount++;
			}
			if (!seeds.add(raid.seed())) {
				violations.add("duplicate seed " + raid.seed());
			}
			if (raid.themeId().isEmpty() || raid.routeId().isEmpty()) {
				violations.add(
						"seed " + raid.seed()
								+ " lacks theme or route");
			}
			float minimum = raid.mode().targetMinimumSeconds();
			float maximum = raid.mode().targetMaximumSeconds();
			if (raid.durationSeconds() < minimum
					|| raid.durationSeconds() > maximum) {
				violations.add(
						"seed " + raid.seed() + " duration outside "
								+ raid.mode().name() + " target");
			}
			if (raid.mode() == BukovRaidMode.EXPEDITION
					&& (raid.firefights() < EXPEDITION_MIN_FIREFIGHTS
					|| raid.firefights() > EXPEDITION_MAX_FIREFIGHTS)) {
				violations.add(
						"seed " + raid.seed()
								+ " expedition firefights outside 3-8");
			}
			index++;
		}
		if (raids.size() != REQUIRED_SEED_COUNT) {
			violations.add(
					"expected 10 raids but found " + raids.size());
		}
		if (seeds.size() != REQUIRED_SEED_COUNT) {
			violations.add(
					"expected 10 unique seeds but found " + seeds.size());
		}
		return new BukovBalanceReport(
				raids.size(),
				complete,
				complete == 0 ? 0f
						: (float)(durationTotal / complete),
				complete == 0 ? 0f
						: firefightTotal / (float)complete,
				extractedTotal,
				deathCount,
				violations);
	}

	public int raidCount() {
		return raidCount;
	}

	public int completeRaidCount() {
		return completeRaidCount;
	}

	public float averageDurationSeconds() {
		return averageDurationSeconds;
	}

	public float averageFirefights() {
		return averageFirefights;
	}

	public long totalExtractedValue() {
		return totalExtractedValue;
	}

	public int deaths() {
		return deaths;
	}

	public boolean passes() {
		return violations.isEmpty();
	}

	public List<String> violations() {
		return violations;
	}

	private static long saturatedAdd(long first, long second) {
		return first > Long.MAX_VALUE - second
				? Long.MAX_VALUE : first + second;
	}
}
