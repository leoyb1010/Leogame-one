package com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial;

/** Reusable HUD snapshot; intentionally allocation-free during render. */
public final class BukovTutorialHintState {

	public BukovTutorialEvent event;
	public String message;
	public float remainingSeconds;

	public void clear() {
		event = null;
		message = null;
		remainingSeconds = 0f;
	}

	public boolean visible() {
		return event != null && remainingSeconds > 0f;
	}
}
