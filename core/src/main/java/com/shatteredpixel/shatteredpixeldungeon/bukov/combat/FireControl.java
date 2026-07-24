package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.ReloadAudioCueResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FireMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;

public final class FireControl {

	public static final class AmmoSelection {
		public final String definitionId;
		public final int quantity;

		public AmmoSelection(String definitionId, int quantity) {
			if (quantity < 0 || (quantity > 0
					&& (definitionId == null || definitionId.isEmpty()))) {
				throw new IllegalArgumentException("invalid ammunition selection");
			}
			this.definitionId = definitionId;
			this.quantity = quantity;
		}

		public static AmmoSelection none() {
			return new AmmoSelection(null, 0);
		}
	}

	public interface Sink {
		void fire(Firearm firearm, FirearmDefinition definition);
		AmmoSelection requestAmmo(
				String caliber,
				String preferredDefinitionId,
				int maximum,
				boolean allowAlternative);
		void dryFire();
		void reloadStarted(float seconds);
		void reloadAudioCues(FirearmDefinition definition, int cueMask);
		void reloadFinished();
	}

	private float shotCooldown;
	private float reloadRemaining;
	private float reloadDuration;
	private float recoilSpreadDeg;
	private boolean previousHeld;

	public void update(float dt,
					   boolean fireHeld,
					   boolean firePressed,
					   boolean reloadPressed,
					   Firearm firearm,
					   FirearmDefinition definition,
					   Sink sink) {
		update(
				dt,
				fireHeld,
				firePressed,
				reloadPressed,
				firearm,
				definition,
				1f,
				sink);
	}

	public void update(float dt,
					   boolean fireHeld,
					   boolean firePressed,
					   boolean reloadPressed,
					   Firearm firearm,
					   FirearmDefinition definition,
					   float reloadDurationMultiplier,
					   Sink sink) {
		if (dt < 0f) {
			throw new IllegalArgumentException("dt must not be negative");
		}
		if (firearm == null || definition == null || sink == null) {
			throw new IllegalArgumentException("firearm, definition, and sink are required");
		}
		if (reloadDurationMultiplier <= 0f
				|| Float.isNaN(reloadDurationMultiplier)
				|| Float.isInfinite(reloadDurationMultiplier)) {
			throw new IllegalArgumentException(
					"reloadDurationMultiplier must be finite and positive");
		}

		shotCooldown = Math.max(0f, shotCooldown - dt);
		recoilSpreadDeg = Math.max(
				0f,
				recoilSpreadDeg - definition.recoilRecovery * dt);

		if (reloadRemaining > 0f) {
			float previousElapsed = reloadDuration - reloadRemaining;
			reloadRemaining = Math.max(0f, reloadRemaining - dt);
			float currentElapsed = reloadDuration - reloadRemaining;
			int cueMask = ReloadAudioCueResolver.crossed(
					definition.audioProfile,
					previousElapsed,
					currentElapsed,
					reloadDuration);
			if (cueMask != 0) {
				sink.reloadAudioCues(definition, cueMask);
			}
			if (reloadRemaining <= 0f) {
				int missing = definition.magazineSize - firearm.magazineAmmo();
				AmmoSelection selection = sink.requestAmmo(
						definition.caliber,
						firearm.loadedAmmoDefinitionId(definition),
						missing,
						firearm.magazineAmmo() == 0);
				if (selection == null || selection.quantity > missing) {
					throw new IllegalStateException("invalid ammunition selection from sink");
				}
				if (selection.quantity > 0) {
					firearm.loadRounds(
							selection.definitionId,
							selection.quantity,
							definition);
				}
				reloadDuration = 0f;
				sink.reloadFinished();
			}
			previousHeld = fireHeld;
			return;
		}

		if (reloadPressed && firearm.magazineAmmo() < definition.magazineSize) {
			reloadDuration =
					definition.reloadSeconds * reloadDurationMultiplier;
			reloadRemaining = reloadDuration;
			sink.reloadStarted(reloadDuration);
			previousHeld = fireHeld;
			return;
		}

		boolean trigger = definition.fireMode == FireMode.AUTO
				? fireHeld
				: firePressed && !previousHeld;

		if (trigger && shotCooldown <= 0f) {
			if (firearm.consumeRound()) {
				shotCooldown = definition.secondsPerShot();
				sink.fire(firearm, definition);
				recoilSpreadDeg = Math.min(
						12f,
						recoilSpreadDeg + definition.recoilPerShot);
			} else {
				shotCooldown = 0.15f;
				sink.dryFire();
			}
		}
		previousHeld = fireHeld;
	}

	public boolean isReloading() {
		return reloadRemaining > 0f;
	}

	public float shotCooldown() {
		return shotCooldown;
	}

	public float reloadRemaining() {
		return reloadRemaining;
	}

	public float reloadDuration() {
		return reloadDuration;
	}

	/**
	 * Accumulated bloom from the previously fired rounds. The live hitscan
	 * path adds this to authored base/moving spread, making recoil stats real
	 * gameplay instead of dead content.
	 */
	public float recoilSpreadDeg() {
		return recoilSpreadDeg;
	}

	/** Cancels a reload without leaking its remaining audio cues. */
	public void cancelReload() {
		reloadRemaining = 0f;
		reloadDuration = 0f;
	}

	/** Cancels transient trigger/reload state when the equipped firearm changes. */
	public void resetForWeaponSwap() {
		shotCooldown = 0f;
		cancelReload();
		recoilSpreadDeg = 0f;
		previousHeld = false;
	}
}
