package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed-step treatment controller backed by physical RaidItem UIDs.
 *
 * Starting or interrupting a treatment never consumes an item. Exactly one
 * unit is consumed only when the uninterrupted use duration completes.
 */
public final class RealtimeMedicalSystem {

	public static final class Snapshot implements Bundlable {

		private static final String ACTIVE_ITEM_UID = "active_item_uid";
		private static final String ACTIVE_ELAPSED = "active_elapsed";
		private static final String COOLDOWN_REMAINING =
				"cooldown_remaining";
		private static final String CLOSED = "closed";

		private String activeItemUid;
		private float activeElapsed;
		private float cooldownRemaining;
		private boolean closed;

		public Snapshot() {
			// Required by Bundle reflection.
		}

		private Snapshot(
				String activeItemUid,
				float activeElapsed,
				float cooldownRemaining,
				boolean closed) {
			this.activeItemUid = activeItemUid;
			this.activeElapsed = activeElapsed;
			this.cooldownRemaining = cooldownRemaining;
			this.closed = closed;
		}

		public String activeItemUid() {
			return activeItemUid;
		}

		public float activeElapsed() {
			return activeElapsed;
		}

		public float cooldownRemaining() {
			return cooldownRemaining;
		}

		public boolean closed() {
			return closed;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put(ACTIVE_ITEM_UID, activeItemUid);
			bundle.put(ACTIVE_ELAPSED, activeElapsed);
			bundle.put(COOLDOWN_REMAINING, cooldownRemaining);
			bundle.put(CLOSED, closed);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			String restoredUid = bundle.getString(ACTIVE_ITEM_UID);
			activeItemUid = restoredUid == null || restoredUid.isEmpty()
					? null : restoredUid;
			activeElapsed = restoredTime(bundle, ACTIVE_ELAPSED);
			cooldownRemaining =
					restoredTime(bundle, COOLDOWN_REMAINING);
			closed = bundle.getBoolean(CLOSED);
			if (activeItemUid == null && activeElapsed != 0f) {
				throw new IllegalStateException(
						"Medical progress has no active item");
			}
		}

		private static float restoredTime(Bundle bundle, String key) {
			float value = bundle.getFloat(key);
			if (!BukovNumbers.isFinite(value) || value < 0f) {
				throw new IllegalStateException(
						"Invalid restored medical time: " + key);
			}
			return value;
		}
	}

	public enum BeginResult {
		STARTED,
		BUSY,
		COOLDOWN,
		UNKNOWN_ITEM,
		EMPTY,
		NO_EFFECT,
		DEAD,
		CLOSED
	}

	public enum StepResult {
		IDLE,
		IN_PROGRESS,
		COMPLETED,
		INTERRUPTED_MOVE,
		INTERRUPTED_DAMAGE,
		INTERRUPTED_SHOT,
		CANCELED_NO_EFFECT,
		DEAD,
		CLOSED
	}

	private static final class MedicalStack {
		private final RaidItem original;
		private final MedicalDefinition definition;
		private int quantity;

		private MedicalStack(
				RaidItem original,
				MedicalDefinition definition) {
			this.original = original;
			this.definition = definition;
			this.quantity = original.quantity();
		}
	}

	private final String raidId;
	private final RealtimeStatusState status;
	private final Map<String, MedicalStack> stacks = new LinkedHashMap<>();
	private MedicalStack active;
	private float activeElapsed;
	private float cooldownRemaining;
	private boolean closed;

	private RealtimeMedicalSystem(
			String raidId,
			RealtimeStatusState status) {
		this.raidId = raidId;
		this.status = status;
	}

	public static RealtimeMedicalSystem fromLedger(
			LootTransaction ledger,
			RealtimeStatusState status) {
		if (ledger == null || status == null) {
			throw new IllegalArgumentException("ledger and status are required");
		}
		RealtimeMedicalSystem result =
				new RealtimeMedicalSystem(ledger.raidId(), status);
		for (RaidItem item : ledger.items()) {
			MedicalDefinition definition =
					MedicalCatalog.find(item.definitionId());
			if (definition != null) {
				result.stacks.put(
						item.itemUid(),
						new MedicalStack(item, definition));
			}
		}
		return result;
	}

	/** Adds a newly looted medical stack without rebuilding active treatment. */
	public boolean track(RaidItem item) {
		if (closed || item == null || stacks.containsKey(item.itemUid())) {
			return false;
		}
		MedicalDefinition definition = MedicalCatalog.find(item.definitionId());
		if (definition == null) {
			return false;
		}
		stacks.put(item.itemUid(), new MedicalStack(item, definition));
		return true;
	}

	public BeginResult beginUse(String itemUid) {
		if (closed) {
			return BeginResult.CLOSED;
		}
		if (status.isDead()) {
			return BeginResult.DEAD;
		}
		if (active != null) {
			return BeginResult.BUSY;
		}
		if (cooldownRemaining > 0f) {
			return BeginResult.COOLDOWN;
		}
		MedicalStack stack = stacks.get(itemUid);
		if (stack == null) {
			return BeginResult.UNKNOWN_ITEM;
		}
		if (stack.quantity <= 0) {
			return BeginResult.EMPTY;
		}
		if (!applicable(stack.definition)) {
			return BeginResult.NO_EFFECT;
		}
		active = stack;
		activeElapsed = 0f;
		return BeginResult.STARTED;
	}

	/**
	 * Advances status effects, cooldown and an active treatment.
	 *
	 * Damage takes precedence over firing and movement if several interruption
	 * flags arrive in the same fixed step.
	 */
	public StepResult fixedStep(
			float deltaSeconds,
			boolean moved,
			boolean tookDamage,
			boolean firedShot) {
		requireDelta(deltaSeconds);
		if (closed) {
			return StepResult.CLOSED;
		}
		status.fixedStep(deltaSeconds);
		cooldownRemaining =
				Math.max(0f, cooldownRemaining - deltaSeconds);
		if (active == null) {
			return status.isDead() ? StepResult.DEAD : StepResult.IDLE;
		}
		if (status.isDead()) {
			cancelActive(0f);
			return StepResult.DEAD;
		}
		if (tookDamage && active.definition.interruptOnDamage) {
			cancelActive(0.15f);
			return StepResult.INTERRUPTED_DAMAGE;
		}
		if (firedShot && active.definition.interruptOnShot) {
			cancelActive(0.15f);
			return StepResult.INTERRUPTED_SHOT;
		}
		if (moved && active.definition.interruptOnMove) {
			cancelActive(0.15f);
			return StepResult.INTERRUPTED_MOVE;
		}
		activeElapsed += deltaSeconds;
		if (activeElapsed + 0.00001f < active.definition.useSeconds) {
			return StepResult.IN_PROGRESS;
		}
		if (!applicable(active.definition)) {
			cancelActive(0f);
			return StepResult.CANCELED_NO_EFFECT;
		}

		MedicalStack completed = active;
		completed.quantity--;
		apply(completed.definition);
		active = null;
		activeElapsed = 0f;
		cooldownRemaining = Math.max(
				cooldownRemaining,
				completed.definition.cooldownSeconds);
		return StepResult.COMPLETED;
	}

	/**
	 * Checkpoint write. The controller remains usable after this call.
	 */
	public void writeBack(LootTransaction ledger) {
		requireLedger(ledger);
		for (Map.Entry<String, MedicalStack> entry : stacks.entrySet()) {
			if (!ledger.contains(entry.getKey())) {
				continue;
			}
			MedicalStack stack = entry.getValue();
			if (stack.quantity <= 0) {
				ledger.drop(entry.getKey());
			} else {
				ledger.replace(stack.original.withRuntimeState(
						stack.quantity,
						stack.original.durability()));
			}
		}
	}

	/**
	 * Terminal write used by both death and successful extraction.
	 *
	 * An unfinished treatment is canceled without consumption. Repeating the
	 * call is safe and produces the same ledger state.
	 */
	public void finishRaid(LootTransaction ledger) {
		requireLedger(ledger);
		active = null;
		activeElapsed = 0f;
		writeBack(ledger);
		closed = true;
	}

	public RealtimeStatusState status() {
		return status;
	}

	public int quantity(String itemUid) {
		MedicalStack stack = stacks.get(itemUid);
		return stack == null ? 0 : stack.quantity;
	}

	public long totalTrackedQuantity() {
		long total = 0L;
		for (MedicalStack stack : stacks.values()) {
			total += stack.quantity;
		}
		return total;
	}

	public boolean isUsing() {
		return active != null;
	}

	public String activeItemUid() {
		return active == null ? null : active.original.itemUid();
	}

	public float useProgress() {
		if (active == null) {
			return 0f;
		}
		return Math.min(
				1f,
				activeElapsed / active.definition.useSeconds);
	}

	public float cooldownRemaining() {
		return cooldownRemaining;
	}

	public boolean closed() {
		return closed;
	}

	public Snapshot snapshot() {
		return new Snapshot(
				activeItemUid(),
				activeElapsed,
				cooldownRemaining,
				closed);
	}

	/**
	 * Restores controller-only state after the physical ledger has rebuilt the
	 * stack references. Missing or no-longer-applicable active items are
	 * canceled safely and never consumed.
	 */
	public void restoreSnapshot(Snapshot snapshot) {
		if (snapshot == null) return;
		cooldownRemaining = snapshot.cooldownRemaining;
		closed = snapshot.closed;
		active = null;
		activeElapsed = 0f;
		if (closed || snapshot.activeItemUid == null) return;
		MedicalStack restoredActive = stacks.get(snapshot.activeItemUid);
		if (restoredActive == null
				|| restoredActive.quantity <= 0
				|| !applicable(restoredActive.definition)) {
			return;
		}
		if (snapshot.activeElapsed
				> restoredActive.definition.useSeconds + 0.00001f) {
			throw new IllegalStateException(
					"Medical progress exceeds authored use duration");
		}
		active = restoredActive;
		activeElapsed = Math.min(
				snapshot.activeElapsed,
				restoredActive.definition.useSeconds);
	}

	private boolean applicable(MedicalDefinition definition) {
		if (status.isDead()) {
			return false;
		}
		return definition.healAmount > 0f
						&& status.health() < status.maximumHealth()
				|| definition.bleedingReduction > 0f
						&& status.bleedingPerSecond() > 0f
				|| definition.clearsFracture
						&& status.fractured()
				|| definition.painSuppressionSeconds > 0f
						&& status.painSeverity() > 0f
						&& !status.painSuppressed()
				|| definition.concussionReductionSeconds > 0f
						&& status.concussionRemaining() > 0f
				|| definition.stimulantSeconds > 0f
						&& status.stimulantRemaining() <= 0f;
	}

	private void apply(MedicalDefinition definition) {
		status.heal(definition.healAmount);
		status.reduceBleeding(definition.bleedingReduction);
		if (definition.clearsFracture) {
			status.clearFracture();
		}
		status.suppressPain(definition.painSuppressionSeconds);
		status.reduceConcussion(definition.concussionReductionSeconds);
		status.applyStimulant(definition.stimulantSeconds);
	}

	private void cancelActive(float cancelCooldown) {
		active = null;
		activeElapsed = 0f;
		cooldownRemaining =
				Math.max(cooldownRemaining, cancelCooldown);
	}

	private void requireLedger(LootTransaction ledger) {
		if (ledger == null || !raidId.equals(ledger.raidId())) {
			throw new IllegalArgumentException("matching raid ledger is required");
		}
	}

	private static void requireDelta(float deltaSeconds) {
		if (!BukovNumbers.isFinite(deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException(
					"deltaSeconds must be finite and non-negative");
		}
	}
}
