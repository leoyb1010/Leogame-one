package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Local-only, non-identifying balance facts for one raid.
 *
 * The object is stored inside the raid checkpoint and settlement receipt. It
 * has no network dependency and deliberately contains no account, device,
 * wall-clock, or free-form player data.
 */
public final class RaidBalanceTelemetry implements Bundlable {

	public enum End {
		UNKNOWN,
		BASIC_EXTRACTION,
		CONDITIONAL_EXTRACTION,
		TEMPORARY_EXTRACTION,
		DEATH
	}

	private static final String AVAILABLE = "available";
	private static final String SEED = "seed";
	private static final String MODE = "mode";
	private static final String THEME_ID = "theme_id";
	private static final String ROUTE_ID = "route_id";
	private static final String DURATION_SECONDS = "duration_seconds";
	private static final String CONTAINER_SEARCHES = "container_searches";
	private static final String FIREFIGHTS = "firefights";
	private static final String FIREFIGHT_ACTIVE = "firefight_active";
	private static final String KILLS = "kills";
	private static final String DAMAGE_TAKEN = "damage_taken";
	private static final String EXTRACTED_VALUE = "extracted_value";
	private static final String END = "end";

	private boolean available;
	private long seed;
	private BukovRaidMode mode = BukovRaidMode.EXPEDITION;
	private String themeId = "";
	private String routeId = "";
	private float durationSeconds;
	private int containerSearches;
	private int firefights;
	private boolean firefightActive;
	private int kills;
	private int damageTaken;
	private long extractedValue;
	private End end = End.UNKNOWN;

	public RaidBalanceTelemetry() {
		// Required by Bundle reflection.
	}

	static RaidBalanceTelemetry begin(long seed, BukovRaidMode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("mode is required");
		}
		RaidBalanceTelemetry result = new RaidBalanceTelemetry();
		result.available = true;
		result.seed = seed;
		result.mode = mode;
		return result;
	}

	static RaidBalanceTelemetry unavailable() {
		return new RaidBalanceTelemetry();
	}

	public boolean available() {
		return available;
	}

	public long seed() {
		return seed;
	}

	public BukovRaidMode mode() {
		return mode;
	}

	public String themeId() {
		return themeId;
	}

	public String routeId() {
		return routeId;
	}

	public float durationSeconds() {
		return durationSeconds;
	}

	public int containerSearches() {
		return containerSearches;
	}

	public int firefights() {
		return firefights;
	}

	public int kills() {
		return kills;
	}

	public int damageTaken() {
		return damageTaken;
	}

	public long extractedValue() {
		return extractedValue;
	}

	public End end() {
		return end;
	}

	public boolean settled() {
		return end != End.UNKNOWN;
	}

	void identifyContext(String themeId, String routeId) {
		if (!available) return;
		requireMutable();
		identifyTheme(themeId);
		identifyRoute(routeId);
	}

	void identifyTheme(String themeId) {
		if (!available) return;
		requireMutable();
		this.themeId = requireIdentifier(themeId, "themeId");
	}

	void identifyRoute(String routeId) {
		if (!available) return;
		requireMutable();
		this.routeId = requireIdentifier(routeId, "routeId");
	}

	void recordContainerSearch() {
		if (!available) return;
		requireMutable();
		containerSearches = increment(containerSearches, "container searches");
	}

	void recordFirefight() {
		if (!available) return;
		requireMutable();
		firefights = increment(firefights, "firefights");
	}

	/**
	 * Counts a firefight once on the authoritative transition into combat.
	 * Keeping the latch in the checkpoint prevents a resumed encounter from
	 * being counted again after a save/reload.
	 */
	void updateFirefightState(
			boolean combatActive,
			boolean searchingAfterContact) {
		if (!available) return;
		requireMutable();
		if (combatActive && !firefightActive) {
			recordFirefight();
			firefightActive = true;
		} else if (firefightActive
				&& !combatActive
				&& !searchingAfterContact) {
			firefightActive = false;
		}
	}

	void recordDamageTaken(int amount) {
		if (!available) return;
		requireMutable();
		if (amount < 0) {
			throw new IllegalArgumentException(
					"damage amount must be non-negative");
		}
		if (damageTaken > Integer.MAX_VALUE - amount) {
			throw new IllegalStateException("damage taken exhausted");
		}
		damageTaken += amount;
	}

	RaidBalanceTelemetry settle(
			float durationSeconds,
			int kills,
			long extractedValue,
			End end) {
		if (!available) return unavailable();
		if (!BukovNumbers.isFinite(durationSeconds)
				|| durationSeconds < 0f) {
			throw new IllegalArgumentException(
					"durationSeconds must be finite and non-negative");
		}
		if (kills < 0 || extractedValue < 0L) {
			throw new IllegalArgumentException(
					"kills and extractedValue must be non-negative");
		}
		if (end == null || end == End.UNKNOWN) {
			throw new IllegalArgumentException(
					"settlement end is required");
		}
		RaidBalanceTelemetry result = copy();
		result.durationSeconds = durationSeconds;
		result.kills = kills;
		result.extractedValue = extractedValue;
		result.end = end;
		result.firefightActive = false;
		return result;
	}

	RaidBalanceTelemetry withExtractedValue(long value) {
		if (value < 0L) {
			throw new IllegalArgumentException(
					"extracted value must be non-negative");
		}
		RaidBalanceTelemetry result = copy();
		if (result.available) result.extractedValue = value;
		return result;
	}

	boolean matches(RaidBalanceTelemetry other) {
		if (other == null) return false;
		if (!available || !other.available) return true;
		return seed == other.seed
				&& mode == other.mode
				&& themeId.equals(other.themeId)
				&& routeId.equals(other.routeId)
				&& Float.floatToIntBits(durationSeconds)
						== Float.floatToIntBits(other.durationSeconds)
				&& containerSearches == other.containerSearches
				&& firefights == other.firefights
				&& kills == other.kills
				&& damageTaken == other.damageTaken
				&& extractedValue == other.extractedValue
				&& end == other.end;
	}

	RaidBalanceTelemetry copy() {
		RaidBalanceTelemetry result = new RaidBalanceTelemetry();
		result.available = available;
		result.seed = seed;
		result.mode = mode;
		result.themeId = themeId;
		result.routeId = routeId;
		result.durationSeconds = durationSeconds;
		result.containerSearches = containerSearches;
		result.firefights = firefights;
		result.firefightActive = firefightActive;
		result.kills = kills;
		result.damageTaken = damageTaken;
		result.extractedValue = extractedValue;
		result.end = end;
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(AVAILABLE, available);
		bundle.put(SEED, seed);
		bundle.put(MODE, mode);
		bundle.put(THEME_ID, themeId);
		bundle.put(ROUTE_ID, routeId);
		bundle.put(DURATION_SECONDS, durationSeconds);
		bundle.put(CONTAINER_SEARCHES, containerSearches);
		bundle.put(FIREFIGHTS, firefights);
		bundle.put(FIREFIGHT_ACTIVE, firefightActive);
		bundle.put(KILLS, kills);
		bundle.put(DAMAGE_TAKEN, damageTaken);
		bundle.put(EXTRACTED_VALUE, extractedValue);
		bundle.put(END, end);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		available = bundle.getBoolean(AVAILABLE);
		seed = bundle.getLong(SEED);
		BukovRaidMode restoredMode =
				bundle.getEnum(MODE, BukovRaidMode.class);
		mode = restoredMode == null
				? BukovRaidMode.EXPEDITION : restoredMode;
		themeId = safeText(bundle.getString(THEME_ID));
		routeId = safeText(bundle.getString(ROUTE_ID));
		durationSeconds = bundle.getFloat(DURATION_SECONDS);
		containerSearches = bundle.getInt(CONTAINER_SEARCHES);
		firefights = bundle.getInt(FIREFIGHTS);
		firefightActive = bundle.getBoolean(FIREFIGHT_ACTIVE);
		kills = bundle.getInt(KILLS);
		damageTaken = bundle.getInt(DAMAGE_TAKEN);
		extractedValue = bundle.getLong(EXTRACTED_VALUE);
		End restoredEnd = bundle.getEnum(END, End.class);
		end = restoredEnd == null ? End.UNKNOWN : restoredEnd;
		validate();
	}

	private void validate() {
		if (!BukovNumbers.isFinite(durationSeconds)
				|| durationSeconds < 0f
				|| containerSearches < 0
				|| firefights < 0
				|| kills < 0
				|| damageTaken < 0
				|| extractedValue < 0L) {
			throw new IllegalStateException(
					"Invalid raid balance telemetry");
		}
	}

	private void requireMutable() {
		if (settled()) {
			throw new IllegalStateException(
					"Settled balance telemetry is immutable");
		}
	}

	private static int increment(int value, String name) {
		if (value == Integer.MAX_VALUE) {
			throw new IllegalStateException(name + " exhausted");
		}
		return value + 1;
	}

	private static String requireIdentifier(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value.trim();
	}

	private static String safeText(String value) {
		return value == null ? "" : value;
	}
}
