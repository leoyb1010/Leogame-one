package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;

public final class InputFrame {
	public final PointF movement = new PointF();
	public final PointF aim = new PointF(1f, 0f);
	public boolean fireHeld;
	public boolean firePressed;
	public boolean reloadPressed;
	public boolean interactHeld;
	public boolean interactPressed;
	public boolean medicalPressed;
	/**
	 * 0 means the context-sensitive medical button, 1-4 are the four explicit
	 * quick slots. This remains an edge value and is cleared after every poll.
	 */
	public int medicalSlot;
	public boolean sprintHeld;
	public boolean dropPressed;
	public boolean backpackPressed;
	public float aimAssistScale;

	public void clearEdges() {
		firePressed = false;
		reloadPressed = false;
		interactPressed = false;
		medicalPressed = false;
		medicalSlot = 0;
		dropPressed = false;
		backpackPressed = false;
	}
}
