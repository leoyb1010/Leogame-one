package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

/**
 * Carries render-frame input edges to exactly one fixed simulation step.
 *
 * A render frame may contain zero or several 120 Hz steps. Boolean OR capture
 * keeps an edge alive across a zero-step frame, while drain clears it before a
 * second fixed step can observe the same libGDX justPressed state.
 */
final class InputEdgeLatch {

	private boolean firePressed;
	private boolean reloadPressed;
	private boolean interactPressed;
	private boolean medicalPressed;
	private int medicalSlot;
	private boolean dropPressed;
	private boolean backpackPressed;

	void capture(InputFrame sampled) {
		if (sampled == null) {
			throw new IllegalArgumentException("sampled input is required");
		}
		firePressed |= sampled.firePressed;
		reloadPressed |= sampled.reloadPressed;
		interactPressed |= sampled.interactPressed;
		medicalPressed |= sampled.medicalPressed;
		if (sampled.medicalSlot > 0) {
			medicalSlot = sampled.medicalSlot;
		}
		dropPressed |= sampled.dropPressed;
		backpackPressed |= sampled.backpackPressed;
	}

	void drainTo(InputFrame target) {
		if (target == null) {
			throw new IllegalArgumentException("target input is required");
		}
		target.clearEdges();
		target.firePressed = firePressed;
		target.reloadPressed = reloadPressed;
		target.interactPressed = interactPressed;
		target.medicalPressed = medicalPressed;
		target.medicalSlot = medicalSlot;
		target.dropPressed = dropPressed;
		target.backpackPressed = backpackPressed;
		reset();
	}

	void reset() {
		firePressed = false;
		reloadPressed = false;
		interactPressed = false;
		medicalPressed = false;
		medicalSlot = 0;
		dropPressed = false;
		backpackPressed = false;
	}
}
