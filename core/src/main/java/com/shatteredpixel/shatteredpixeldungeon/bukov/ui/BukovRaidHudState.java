package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;

/**
 * Reused, allocation-free transfer object for realtime raid HUD state.
 *
 * Values are clamped at this presentation boundary so a partially restored or
 * rapidly changing runtime state can never produce NaN-sized UI geometry.
 */
public final class BukovRaidHudState {

	public enum Interaction {
		NONE,
		SEARCH,
		PICKUP,
		EXTRACT,
		PUMP,
		MEDICAL,
		LOCKED
	}

	public enum Direction {
		N, NE, E, SE, S, SW, W, NW
	}

	public enum Distance {
		NEAR, MID, FAR
	}

	private String objective;
	private float raidElapsedSeconds;
	private String weaponName;
	private boolean automaticFire;
	private int magazine;
	private int magazineCapacity;
	private int reserve;
	private boolean reloading;
	private float reloadProgress;
	private float bleedingPerSecond;
	private boolean fractured;
	private float painSeverity;
	private float concussionRemaining;
	private float stimulantRemaining;
	private Interaction interaction = Interaction.NONE;
	private String interactionLabel;
	private float interactionProgress;
	private float interactionSeconds;
	private int availableExtractions;
	private String extractionId;
	private boolean extractionAvailable;
	private boolean extractionActive;
	private float extractionProgress;
	private float extractionSeconds;
	private boolean colorblindAssist;
	private int damageNumbersMode;
	private boolean soundVisible;
	private SoundCategory soundCategory;
	private Direction soundDirection;
	private Distance soundDistance;
	private float soundStrength;
	private float soundRemainingSeconds;
	private boolean hitVisible;
	private Direction hitDirection;
	private float hitStrength;
	private float hitRemainingSeconds;
	private boolean bossActive;
	private String bossName;
	private int bossPhase;
	private int bossPhaseCount;
	private String bossPhaseLabel;
	private int bossHealth;
	private int bossMaximumHealth;
	private boolean bossVulnerable;
	private String bossObjective;
	private boolean bossRetreatWarning;

	public void beginFrame(String objective, float raidElapsedSeconds) {
		this.objective = text(objective);
		this.raidElapsedSeconds = nonNegative(raidElapsedSeconds);
		weaponName = null;
		automaticFire = false;
		magazine = 0;
		magazineCapacity = 0;
		reserve = 0;
		reloading = false;
		reloadProgress = 0f;
		bleedingPerSecond = 0f;
		fractured = false;
		painSeverity = 0f;
		concussionRemaining = 0f;
		stimulantRemaining = 0f;
		interaction = Interaction.NONE;
		interactionLabel = null;
		interactionProgress = 0f;
		interactionSeconds = 0f;
		availableExtractions = 0;
		extractionId = null;
		extractionAvailable = false;
		extractionActive = false;
		extractionProgress = 0f;
		extractionSeconds = 0f;
		colorblindAssist = false;
		damageNumbersMode = 1;
		soundVisible = false;
		soundCategory = null;
		soundDirection = null;
		soundDistance = null;
		soundStrength = 0f;
		soundRemainingSeconds = 0f;
		hitVisible = false;
		hitDirection = null;
		hitStrength = 0f;
		hitRemainingSeconds = 0f;
		bossActive = false;
		bossName = null;
		bossPhase = 0;
		bossPhaseCount = 3;
		bossPhaseLabel = null;
		bossHealth = 0;
		bossMaximumHealth = 0;
		bossVulnerable = false;
		bossObjective = null;
		bossRetreatWarning = false;
	}

	public void presentationSettings(
			boolean colorblindAssist, int damageNumbersMode) {
		this.colorblindAssist = colorblindAssist;
		this.damageNumbersMode = clamp(damageNumbersMode, 0, 2);
	}

	public void sound(KeySoundVisualEvent event) {
		if (event == null || !event.visible()) return;
		sound(
				event.category(),
				direction(event.direction()),
				distance(event.distanceBand()),
				event.strength(),
				event.remainingSeconds());
	}

	public void sound(
			SoundCategory category,
			Direction direction,
			Distance distance,
			float strength,
			float remainingSeconds) {
		soundVisible = category != null
				&& direction != null
				&& distance != null
				&& remainingSeconds > 0f;
		soundCategory = soundVisible ? category : null;
		soundDirection = soundVisible ? direction : null;
		soundDistance = soundVisible ? distance : null;
		soundStrength = soundVisible ? fraction(strength, 1f) : 0f;
		soundRemainingSeconds = soundVisible
				? nonNegative(remainingSeconds) : 0f;
	}

	public void hit(
			Direction direction,
			float strength,
			float remainingSeconds) {
		hitVisible = direction != null && remainingSeconds > 0f;
		hitDirection = hitVisible ? direction : null;
		hitStrength = hitVisible ? fraction(strength, 1f) : 0f;
		hitRemainingSeconds = hitVisible
				? nonNegative(remainingSeconds) : 0f;
	}

	public void boss(
			String name,
			int phase,
			int phaseCount,
			String phaseLabel,
			int health,
			int maximumHealth,
			boolean vulnerable,
			String objective,
			boolean retreatWarning) {
		bossActive = maximumHealth > 0 && phase > 0;
		bossName = text(name);
		bossPhaseCount = Math.max(1, phaseCount);
		bossPhase = clamp(phase, 0, bossPhaseCount);
		bossPhaseLabel = text(phaseLabel);
		bossMaximumHealth = Math.max(0, maximumHealth);
		bossHealth = clamp(health, 0, bossMaximumHealth);
		bossVulnerable = vulnerable;
		bossObjective = text(objective);
		bossRetreatWarning = retreatWarning;
	}

	public void weapon(
			String name,
			boolean automatic,
			int magazine,
			int capacity,
			int reserve,
			float reloadRemaining,
			float reloadSeconds) {
		weaponName = text(name);
		automaticFire = automatic;
		magazineCapacity = Math.max(0, capacity);
		this.magazine = clamp(magazine, 0, magazineCapacity);
		this.reserve = Math.max(0, reserve);
		float safeDuration = nonNegative(reloadSeconds);
		float safeRemaining = nonNegative(reloadRemaining);
		reloading = safeDuration > 0f && safeRemaining > 0f;
		reloadProgress = reloading
				? fraction(safeDuration - safeRemaining, safeDuration)
				: 0f;
	}

	public void status(
			float bleedingPerSecond,
			boolean fractured,
			float painSeverity,
			float concussionRemaining,
			float stimulantRemaining) {
		this.bleedingPerSecond = nonNegative(bleedingPerSecond);
		this.fractured = fractured;
		this.painSeverity = fraction(painSeverity, 1f);
		this.concussionRemaining = nonNegative(concussionRemaining);
		this.stimulantRemaining = nonNegative(stimulantRemaining);
	}

	public void interaction(
			Interaction type,
			String label,
			float progress,
			float seconds) {
		interaction = type == null ? Interaction.NONE : type;
		interactionLabel = text(label);
		interactionProgress = fraction(progress, 1f);
		interactionSeconds = nonNegative(seconds);
	}

	public void extraction(
			int availableCount,
			String extractionId,
			boolean available,
			boolean active,
			float progress,
			float seconds) {
		availableExtractions = Math.max(0, availableCount);
		this.extractionId = text(extractionId);
		extractionAvailable = available;
		extractionActive = active;
		extractionProgress = fraction(progress, 1f);
		extractionSeconds = nonNegative(seconds);
	}

	public String objective() {
		return objective;
	}

	public float raidElapsedSeconds() {
		return raidElapsedSeconds;
	}

	public String weaponName() {
		return weaponName;
	}

	public boolean automaticFire() {
		return automaticFire;
	}

	public int magazine() {
		return magazine;
	}

	public int magazineCapacity() {
		return magazineCapacity;
	}

	public int reserve() {
		return reserve;
	}

	public boolean reloading() {
		return reloading;
	}

	public float reloadProgress() {
		return reloadProgress;
	}

	public float bleedingPerSecond() {
		return bleedingPerSecond;
	}

	public boolean fractured() {
		return fractured;
	}

	public float painSeverity() {
		return painSeverity;
	}

	public float concussionRemaining() {
		return concussionRemaining;
	}

	public float stimulantRemaining() {
		return stimulantRemaining;
	}

	public Interaction interaction() {
		return interaction;
	}

	public String interactionLabel() {
		return interactionLabel;
	}

	public float interactionProgress() {
		return interactionProgress;
	}

	public float interactionSeconds() {
		return interactionSeconds;
	}

	public int availableExtractions() {
		return availableExtractions;
	}

	public String extractionId() {
		return extractionId;
	}

	public boolean extractionAvailable() {
		return extractionAvailable;
	}

	public boolean extractionActive() {
		return extractionActive;
	}

	public float extractionProgress() {
		return extractionProgress;
	}

	public float extractionSeconds() {
		return extractionSeconds;
	}

	public boolean colorblindAssist() {
		return colorblindAssist;
	}

	public int damageNumbersMode() {
		return damageNumbersMode;
	}

	public boolean soundVisible() {
		return soundVisible;
	}

	public SoundCategory soundCategory() {
		return soundCategory;
	}

	public Direction soundDirection() {
		return soundDirection;
	}

	public Distance soundDistance() {
		return soundDistance;
	}

	public float soundStrength() {
		return soundStrength;
	}

	public float soundRemainingSeconds() {
		return soundRemainingSeconds;
	}

	public boolean hitVisible() {
		return hitVisible;
	}

	public Direction hitDirection() {
		return hitDirection;
	}

	public float hitStrength() {
		return hitStrength;
	}

	public float hitRemainingSeconds() {
		return hitRemainingSeconds;
	}

	public boolean bossActive() {
		return bossActive;
	}

	public String bossName() {
		return bossName;
	}

	public int bossPhase() {
		return bossPhase;
	}

	public int bossPhaseCount() {
		return bossPhaseCount;
	}

	public String bossPhaseLabel() {
		return bossPhaseLabel;
	}

	public int bossHealth() {
		return bossHealth;
	}

	public int bossMaximumHealth() {
		return bossMaximumHealth;
	}

	public float bossHealthFraction() {
		return fraction(bossHealth, bossMaximumHealth);
	}

	public boolean bossVulnerable() {
		return bossVulnerable;
	}

	public String bossObjective() {
		return bossObjective;
	}

	public boolean bossRetreatWarning() {
		return bossRetreatWarning;
	}

	private static Direction direction(KeySoundVisualEvent.Direction value) {
		return value == null ? null : Direction.valueOf(value.name());
	}

	private static Distance distance(KeySoundVisualEvent.DistanceBand value) {
		return value == null ? null : Distance.valueOf(value.name());
	}

	private static String text(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static float fraction(float value, float maximum) {
		if (!BukovNumbers.isFinite(value)
				|| !BukovNumbers.isFinite(maximum)
				|| maximum <= 0f) {
			return 0f;
		}
		return Math.max(0f, Math.min(1f, value / maximum));
	}

	private static float nonNegative(float value) {
		return BukovNumbers.isFinite(value) ? Math.max(0f, value) : 0f;
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}
}
