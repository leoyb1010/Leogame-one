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
	public boolean dropPressed;
	public boolean backpackPressed;
	public float aimAssistScale;

	public void clearEdges() {
		firePressed = false;
		reloadPressed = false;
		interactPressed = false;
		medicalPressed = false;
		dropPressed = false;
		backpackPressed = false;
	}
}
