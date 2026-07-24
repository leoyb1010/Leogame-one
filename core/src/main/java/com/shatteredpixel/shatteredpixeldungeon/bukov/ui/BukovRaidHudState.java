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
		UNLOCK,
		LOCKED
	}

	public enum Direction {
		N, NE, E, SE, S, SW, W, NW
	}

	public enum Distance {
		NEAR, MID, FAR
	}

	public enum Cue {
		NONE,
		PICKUP,
		MISSION,
		EXTRACTION
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
	private float staminaFraction = 1f;
	private boolean sprinting;
	private float carriedLoadFraction;
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
	private float combatAwarenessAlpha = 1f;
	private float killConfirmationRemaining;
	private int hitCount;
	private final Direction[] hitDirections =
			new Direction[BukovCombatHudTimeline.MAX_HIT_DIRECTIONS];
	private final float[] hitStrengths =
			new float[BukovCombatHudTimeline.MAX_HIT_DIRECTIONS];
	private final float[] hitRemainingSeconds =
			new float[BukovCombatHudTimeline.MAX_HIT_DIRECTIONS];
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
	private boolean aimVisible;
	private float aimX;
	private float aimY;
	private boolean firing;
	private Cue navigationCue = Cue.NONE;
	private Direction navigationDirection;
	private Distance navigationDistance;
	private String navigationLabel;
	private boolean navigationAvailable;
	private boolean threatVisible;
	private Direction threatDirection;
	private Distance threatDistance;
	private String threatLabel;
	private boolean threatUrgent;

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
		staminaFraction = 1f;
		sprinting = false;
		carriedLoadFraction = 0f;
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
		combatAwarenessAlpha = 1f;
		killConfirmationRemaining = 0f;
		hitCount = 0;
		for (int index = 0; index < hitDirections.length; index++) {
			hitDirections[index] = null;
			hitStrengths[index] = 0f;
			hitRemainingSeconds[index] = 0f;
		}
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
		aimVisible = false;
		aimX = 0f;
		aimY = 0f;
		firing = false;
		navigationCue = Cue.NONE;
		navigationDirection = null;
		navigationDistance = null;
		navigationLabel = null;
		navigationAvailable = false;
		threatVisible = false;
		threatDirection = null;
		threatDistance = null;
		threatLabel = null;
		threatUrgent = false;
	}

	/**
	 * Stores the normalized direction shared by mouse, controller and touch.
	 * Keeping this presentation-only avoids guessing an input device in the
	 * HUD and guarantees that the reticle matches the live shot direction.
	 */
	public void aim(float x, float y, boolean firing) {
		if (!BukovNumbers.isFinite(x) || !BukovNumbers.isFinite(y)) {
			return;
		}
		float lengthSquared = x * x + y * y;
		if (lengthSquared <= 0.0001f) return;
		float inverseLength = 1f / (float)Math.sqrt(lengthSquared);
		aimVisible = true;
		aimX = x * inverseLength;
		aimY = y * inverseLength;
		this.firing = firing;
	}

	public void navigation(
			Cue cue,
			float deltaX,
			float deltaY,
			float distance,
			String label,
			boolean available) {
		if (cue == null || cue == Cue.NONE
				|| !validVector(deltaX, deltaY)) {
			return;
		}
		navigationCue = cue;
		navigationDirection = direction(deltaX, deltaY);
		navigationDistance = distance(distance);
		navigationLabel = text(label);
		navigationAvailable = available;
	}

	public void threat(
			float deltaX,
			float deltaY,
			float distance,
			String label,
			boolean urgent) {
		if (!validVector(deltaX, deltaY)) return;
		threatVisible = true;
		threatDirection = direction(deltaX, deltaY);
		threatDistance = distance(distance);
		threatLabel = text(label);
		threatUrgent = urgent;
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
		if (direction == null
				|| remainingSeconds <= 0f
				|| hitCount >= hitDirections.length) {
			return;
		}
		hitDirections[hitCount] = direction;
		hitStrengths[hitCount] = fraction(strength, 1f);
		hitRemainingSeconds[hitCount] = nonNegative(remainingSeconds);
		hitCount++;
	}

	public void combatAwareness(float alpha) {
		combatAwarenessAlpha = Math.max(
				BukovCombatHudTimeline.IDLE_ALPHA,
				fraction(alpha, 1f));
	}

	public void killConfirmation(float remainingSeconds) {
		killConfirmationRemaining = nonNegative(remainingSeconds);
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

	public void mobility(
			float staminaFraction,
			boolean sprinting,
			float carriedLoadFraction) {
		this.staminaFraction = fraction(staminaFraction, 1f);
		this.sprinting = sprinting && this.staminaFraction > 0f;
		this.carriedLoadFraction = fraction(carriedLoadFraction, 1f);
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

	public float staminaFraction() {
		return staminaFraction;
	}

	public boolean sprinting() {
		return sprinting;
	}

	public float carriedLoadFraction() {
		return carriedLoadFraction;
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
		return hitCount > 0;
	}

	public Direction hitDirection() {
		return hitDirection(0);
	}

	public float hitStrength() {
		return hitStrength(0);
	}

	public float hitRemainingSeconds() {
		return hitRemainingSeconds(0);
	}

	public int hitCount() {
		return hitCount;
	}

	public Direction hitDirection(int index) {
		return validHitIndex(index) ? hitDirections[index] : null;
	}

	public float hitStrength(int index) {
		return validHitIndex(index) ? hitStrengths[index] : 0f;
	}

	public float hitRemainingSeconds(int index) {
		return validHitIndex(index)
				? hitRemainingSeconds[index] : 0f;
	}

	public float combatAwarenessAlpha() {
		return combatAwarenessAlpha;
	}

	public boolean killConfirmationVisible() {
		return killConfirmationRemaining > 0f;
	}

	public float killConfirmationRemaining() {
		return killConfirmationRemaining;
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

	public boolean aimVisible() {
		return aimVisible;
	}

	public float aimX() {
		return aimX;
	}

	public float aimY() {
		return aimY;
	}

	public boolean firing() {
		return firing;
	}

	public Cue navigationCue() {
		return navigationCue;
	}

	public boolean navigationVisible() {
		return navigationCue != Cue.NONE
				&& navigationDirection != null
				&& navigationDistance != null;
	}

	public Direction navigationDirection() {
		return navigationDirection;
	}

	public Distance navigationDistance() {
		return navigationDistance;
	}

	public String navigationLabel() {
		return navigationLabel;
	}

	public boolean navigationAvailable() {
		return navigationAvailable;
	}

	public boolean threatVisible() {
		return threatVisible;
	}

	public Direction threatDirection() {
		return threatDirection;
	}

	public Distance threatDistance() {
		return threatDistance;
	}

	public String threatLabel() {
		return threatLabel;
	}

	public boolean threatUrgent() {
		return threatUrgent;
	}

	private static Direction direction(KeySoundVisualEvent.Direction value) {
		return value == null ? null : Direction.valueOf(value.name());
	}

	private static Distance distance(KeySoundVisualEvent.DistanceBand value) {
		return value == null ? null : Distance.valueOf(value.name());
	}

	private static Direction direction(float x, float y) {
		double angle = Math.atan2(y, x);
		int sector = (int)Math.floor(
				(angle + Math.PI / 8d) / (Math.PI / 4d));
		switch ((sector + 8) % 8) {
			case 0: return Direction.E;
			case 1: return Direction.SE;
			case 2: return Direction.S;
			case 3: return Direction.SW;
			case 4: return Direction.W;
			case 5: return Direction.NW;
			case 6: return Direction.N;
			default: return Direction.NE;
		}
	}

	private static Distance distance(float tiles) {
		float safe = nonNegative(tiles);
		if (safe <= 2.25f) return Distance.NEAR;
		if (safe <= 8f) return Distance.MID;
		return Distance.FAR;
	}

	private boolean validHitIndex(int index) {
		return index >= 0 && index < hitCount;
	}

	private static boolean validVector(float x, float y) {
		return BukovNumbers.isFinite(x)
				&& BukovNumbers.isFinite(y)
				&& x * x + y * y > 0.0001f;
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
