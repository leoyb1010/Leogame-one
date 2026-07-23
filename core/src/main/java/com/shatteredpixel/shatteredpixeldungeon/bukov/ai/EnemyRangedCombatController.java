package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Deterministic, allocation-free firearm state for one enemy.
 *
 * Line-of-sight is supplied by the world so this class stays independent from
 * the host level, actor scheduler, rendering, and damage systems.
 */
public final class EnemyRangedCombatController {

	public static final class Snapshot implements Bundlable {

		private int magazineAmmo;
		private int reserveAmmo;
		private int shotSequence;
		private float shotCooldown;
		private float reloadRemaining;
		private float aimRemaining;
		private boolean targetLocked;

		public Snapshot() {
			// Required by Bundle reflection.
		}

		private Snapshot(EnemyRangedCombatController controller) {
			magazineAmmo = controller.magazineAmmo;
			reserveAmmo = controller.reserveAmmo;
			shotSequence = controller.shotSequence;
			shotCooldown = controller.shotCooldown;
			reloadRemaining = controller.reloadRemaining;
			aimRemaining = controller.aimRemaining;
			targetLocked = controller.targetLocked;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("magazine_ammo", magazineAmmo);
			bundle.put("reserve_ammo", reserveAmmo);
			bundle.put("shot_sequence", shotSequence);
			bundle.put("shot_cooldown", shotCooldown);
			bundle.put("reload_remaining", reloadRemaining);
			bundle.put("aim_remaining", aimRemaining);
			bundle.put("target_locked", targetLocked);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			magazineAmmo = nonNegative(
					bundle.getInt("magazine_ammo"), "magazine_ammo");
			reserveAmmo = nonNegative(
					bundle.getInt("reserve_ammo"), "reserve_ammo");
			shotSequence = nonNegative(
					bundle.getInt("shot_sequence"), "shot_sequence");
			shotCooldown = finiteNonNegative(
					bundle.getFloat("shot_cooldown"), "shot_cooldown");
			reloadRemaining = finiteNonNegative(
					bundle.getFloat("reload_remaining"),
					"reload_remaining");
			aimRemaining = finiteNonNegative(
					bundle.getFloat("aim_remaining"), "aim_remaining");
			targetLocked = bundle.getBoolean("target_locked");
		}

		public int magazineAmmo() {
			return magazineAmmo;
		}

		public int reserveAmmo() {
			return reserveAmmo;
		}

		public int shotSequence() {
			return shotSequence;
		}

		public float shotCooldown() {
			return shotCooldown;
		}

		public float reloadRemaining() {
			return reloadRemaining;
		}

		public float aimRemaining() {
			return aimRemaining;
		}

		public boolean targetLocked() {
			return targetLocked;
		}

		private static int nonNegative(int value, String label) {
			if (value < 0) {
				throw new IllegalStateException(
						"Invalid ranged snapshot: " + label);
			}
			return value;
		}

		private static float finiteNonNegative(float value, String label) {
			if (!BukovNumbers.isFinite(value) || value < 0f) {
				throw new IllegalStateException(
						"Invalid ranged snapshot: " + label);
			}
			return value;
		}
	}

	public static final class Config {
		public final int magazineSize;
		public final float roundsPerMinute;
		public final float reloadSeconds;
		public final float maximumRange;
		public final float aimSeconds;
		public final int minimumDamage;
		public final int maximumDamage;

		public Config(int magazineSize,
					  float roundsPerMinute,
					  float reloadSeconds,
					  float maximumRange,
					  float aimSeconds,
					  int minimumDamage,
					  int maximumDamage) {
			if (magazineSize <= 0) {
				throw new IllegalArgumentException("magazineSize must be positive");
			}
			requirePositive(roundsPerMinute, "roundsPerMinute");
			requirePositive(reloadSeconds, "reloadSeconds");
			requirePositive(maximumRange, "maximumRange");
			requireNonNegative(aimSeconds, "aimSeconds");
			if (minimumDamage < 0 || maximumDamage < minimumDamage) {
				throw new IllegalArgumentException("invalid damage range");
			}
			this.magazineSize = magazineSize;
			this.roundsPerMinute = roundsPerMinute;
			this.reloadSeconds = reloadSeconds;
			this.maximumRange = maximumRange;
			this.aimSeconds = aimSeconds;
			this.minimumDamage = minimumDamage;
			this.maximumDamage = maximumDamage;
		}

		float secondsPerShot() {
			return 60f / roundsPerMinute;
		}
	}

	private final Config config;
	private final int stableSeed;
	private int magazineAmmo;
	private int reserveAmmo;
	private int shotSequence;
	private float shotCooldown;
	private float reloadRemaining;
	private float aimRemaining;
	private boolean targetLocked;

	public EnemyRangedCombatController(Config config,
									   int magazineAmmo,
									   int reserveAmmo,
									   int stableSeed) {
		if (config == null) {
			throw new IllegalArgumentException("config is required");
		}
		if (magazineAmmo < 0 || magazineAmmo > config.magazineSize) {
			throw new IllegalArgumentException("invalid magazineAmmo");
		}
		if (reserveAmmo < 0) {
			throw new IllegalArgumentException("reserveAmmo must not be negative");
		}
		this.config = config;
		this.magazineAmmo = magazineAmmo;
		this.reserveAmmo = reserveAmmo;
		this.stableSeed = stableSeed;
		aimRemaining = config.aimSeconds;
	}

	/**
	 * Advances the controller by one fixed step.
	 *
	 * A FIRE intent contains exactly one deterministic damage event. Callers
	 * remain responsible for the final ray cast before applying that damage.
	 */
	public void step(float dt,
					 boolean hasLineOfSight,
					 float targetDeltaX,
					 float targetDeltaY,
					 EnemyRangedCombatIntent out) {
		requireNonNegative(dt, "dt");
		if (out == null) {
			throw new IllegalArgumentException("out is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(targetDeltaX) || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(targetDeltaY)) {
			throw new IllegalArgumentException("target delta must be finite");
		}

		float distanceSquared =
				targetDeltaX * targetDeltaX + targetDeltaY * targetDeltaY;
		float distance = (float)Math.sqrt(distanceSquared);
		out.reset(EnemyRangedCombatIntent.Action.SEEK_TARGET, distance);
		shotCooldown = Math.max(0f, shotCooldown - dt);

		if (reloadRemaining > 0f) {
			reloadRemaining = Math.max(0f, reloadRemaining - dt);
			out.action(EnemyRangedCombatIntent.Action.RELOAD);
			if (reloadRemaining == 0f) {
				finishReload();
				out.markReloadCompleted();
			}
			return;
		}

		if (magazineAmmo == 0) {
			resetAim();
			if (reserveAmmo == 0) {
				out.action(EnemyRangedCombatIntent.Action.OUT_OF_AMMO);
				return;
			}
			reloadRemaining = config.reloadSeconds;
			out.action(EnemyRangedCombatIntent.Action.RELOAD);
			out.markReloadStarted();
			return;
		}

		if (!hasLineOfSight) {
			resetAim();
			out.action(EnemyRangedCombatIntent.Action.SEEK_TARGET);
			return;
		}
		if (distance > config.maximumRange) {
			resetAim();
			out.action(EnemyRangedCombatIntent.Action.CLOSE_DISTANCE);
			return;
		}

		if (!targetLocked) {
			targetLocked = true;
			aimRemaining = config.aimSeconds;
		}
		aimRemaining = Math.max(0f, aimRemaining - dt);
		if (aimRemaining > 0f) {
			out.action(EnemyRangedCombatIntent.Action.AIM);
			return;
		}
		if (shotCooldown > 0f) {
			out.action(EnemyRangedCombatIntent.Action.HOLD_FIRE);
			return;
		}

		int eventSequence = shotSequence++;
		magazineAmmo--;
		shotCooldown = config.secondsPerShot();
		aimRemaining = config.aimSeconds;

		float inverseDistance = distance > 0.00001f ? 1f / distance : 0f;
		out.emitDamage(
				damageFor(eventSequence),
				eventSequence,
				targetDeltaX * inverseDistance,
				targetDeltaY * inverseDistance
		);
	}

	public int magazineAmmo() {
		return magazineAmmo;
	}

	public int reserveAmmo() {
		return reserveAmmo;
	}

	public float shotCooldown() {
		return shotCooldown;
	}

	public float reloadRemaining() {
		return reloadRemaining;
	}

	public float aimRemaining() {
		return aimRemaining;
	}

	public int shotSequence() {
		return shotSequence;
	}

	public boolean targetLocked() {
		return targetLocked;
	}

	public Snapshot snapshot() {
		return new Snapshot(this);
	}

	public void restoreSnapshot(Snapshot snapshot) {
		if (snapshot == null) return;
		if (snapshot.magazineAmmo > config.magazineSize
				|| snapshot.reloadRemaining
						> config.reloadSeconds + 0.00001f
				|| snapshot.aimRemaining
						> config.aimSeconds + 0.00001f) {
			throw new IllegalStateException(
					"Ranged snapshot does not match enemy definition");
		}
		magazineAmmo = snapshot.magazineAmmo;
		reserveAmmo = snapshot.reserveAmmo;
		shotSequence = snapshot.shotSequence;
		shotCooldown = snapshot.shotCooldown;
		reloadRemaining = snapshot.reloadRemaining;
		aimRemaining = snapshot.aimRemaining;
		targetLocked = snapshot.targetLocked;
	}

	private void finishReload() {
		int loaded = Math.min(
				config.magazineSize - magazineAmmo,
				reserveAmmo
		);
		magazineAmmo += loaded;
		reserveAmmo -= loaded;
		resetAim();
	}

	private void resetAim() {
		targetLocked = false;
		aimRemaining = config.aimSeconds;
	}

	private int damageFor(int sequence) {
		int mixed = stableSeed ^ (sequence + 1) * 0x9E3779B9;
		mixed ^= mixed >>> 16;
		mixed *= 0x85EBCA6B;
		mixed ^= mixed >>> 13;
		mixed *= 0xC2B2AE35;
		mixed ^= mixed >>> 16;
		long span =
				(long)config.maximumDamage - config.minimumDamage + 1L;
		return config.minimumDamage
				+ (int)(com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.toUnsignedLong(mixed) % span);
	}

	private static void requirePositive(float value, String label) {
		if (!(value > 0f) || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(label + " must be finite and positive");
		}
	}

	private static void requireNonNegative(float value, String label) {
		if (value < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException(label + " must be finite and non-negative");
		}
	}
}
