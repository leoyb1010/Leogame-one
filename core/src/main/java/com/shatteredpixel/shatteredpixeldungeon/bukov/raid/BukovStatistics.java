package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/** Profile statistics updated only by first-time settlement. */
public final class BukovStatistics implements Bundlable {

	private static final String SUCCESSFUL_RAIDS = "successful_raids";
	private static final String DEATHS = "deaths";
	private static final String EXTRACTED_VALUE = "extracted_value";
	private static final String LOST_VALUE = "lost_value";

	private int successfulRaids;
	private int deaths;
	private long extractedValue;
	private long lostValue;

	public BukovStatistics() {
		// Required by Bundle reflection.
	}

	public int successfulRaids() {
		return successfulRaids;
	}

	public int deaths() {
		return deaths;
	}

	public long extractedValue() {
		return extractedValue;
	}

	public long lostValue() {
		return lostValue;
	}

	void record(RaidOutcome outcome, long lootValue) {
		if (outcome == RaidOutcome.SUCCESS) {
			successfulRaids++;
			extractedValue += lootValue;
		} else {
			deaths++;
			lostValue += lootValue;
		}
	}

	BukovStatistics copy() {
		BukovStatistics result = new BukovStatistics();
		result.successfulRaids = successfulRaids;
		result.deaths = deaths;
		result.extractedValue = extractedValue;
		result.lostValue = lostValue;
		return result;
	}

	void replaceWith(BukovStatistics replacement) {
		successfulRaids = replacement.successfulRaids;
		deaths = replacement.deaths;
		extractedValue = replacement.extractedValue;
		lostValue = replacement.lostValue;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(SUCCESSFUL_RAIDS, successfulRaids);
		bundle.put(DEATHS, deaths);
		bundle.put(EXTRACTED_VALUE, extractedValue);
		bundle.put(LOST_VALUE, lostValue);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		successfulRaids = bundle.getInt(SUCCESSFUL_RAIDS);
		deaths = bundle.getInt(DEATHS);
		extractedValue = bundle.getLong(EXTRACTED_VALUE);
		lostValue = bundle.getLong(LOST_VALUE);
		if (successfulRaids < 0 || deaths < 0 || extractedValue < 0L || lostValue < 0L) {
			throw new IllegalStateException("Profile statistics cannot be negative");
		}
	}
}
