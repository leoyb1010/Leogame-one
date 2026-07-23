package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

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
		void reloadFinished();
	}

	private float shotCooldown;
	private float reloadRemaining;
	private boolean previousHeld;

	public void update(float dt,
					   boolean fireHeld,
					   boolean firePressed,
					   boolean reloadPressed,
					   Firearm firearm,
					   FirearmDefinition definition,
					   Sink sink) {
		if (dt < 0f) {
			throw new IllegalArgumentException("dt must not be negative");
		}
		if (firearm == null || definition == null || sink == null) {
			throw new IllegalArgumentException("firearm, definition, and sink are required");
		}

		shotCooldown = Math.max(0f, shotCooldown - dt);

		if (reloadRemaining > 0f) {
			reloadRemaining -= dt;
			if (reloadRemaining <= 0f) {
				reloadRemaining = 0f;
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
				sink.reloadFinished();
			}
			previousHeld = fireHeld;
			return;
		}

		if (reloadPressed && firearm.magazineAmmo() < definition.magazineSize) {
			reloadRemaining = definition.reloadSeconds;
			sink.reloadStarted(reloadRemaining);
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

	/** Cancels transient trigger/reload state when the equipped firearm changes. */
	public void resetForWeaponSwap() {
		shotCooldown = 0f;
		reloadRemaining = 0f;
		previousHeld = false;
	}
}
