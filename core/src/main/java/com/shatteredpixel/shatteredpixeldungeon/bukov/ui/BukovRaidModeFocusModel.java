package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

/**
 * Single focus owner for mode cards and footer actions.
 *
 * Card activation changes only the draft. Applying the draft is a separate
 * action, and deployment remains owned by the hub after this window closes.
 */
public final class BukovRaidModeFocusModel {

	public static final int MODE_COUNT = 5;
	public static final int ACTION_APPLY = 0;
	public static final int ACTION_BACK = 1;
	public static final int ACTION_COUNT = 2;

	private final BukovFocusModel focus;
	private final BukovRaidMode currentMode;
	private final boolean locked;
	private BukovRaidMode draftMode;

	public BukovRaidModeFocusModel(
			BukovRaidMode currentMode,
			boolean locked) {
		if (currentMode == null) {
			throw new IllegalArgumentException("currentMode is required");
		}
		if (BukovRaidMode.values().length != MODE_COUNT) {
			throw new IllegalStateException("mode focus count is stale");
		}
		this.currentMode = currentMode;
		this.draftMode = currentMode;
		this.locked = locked;
		focus = new BukovFocusModel(
				MODE_COUNT + ACTION_COUNT,
				currentMode.ordinal());
	}

	public int index() {
		return focus.index();
	}

	public void focus(int target) {
		focus.focus(target);
	}

	public void move(int delta) {
		focus.move(delta, enabledTargets());
	}

	public boolean modeFocused() {
		return focus.index() < MODE_COUNT;
	}

	public int modeIndex() {
		return modeFocused() ? focus.index() : -1;
	}

	public int actionIndex() {
		return modeFocused() ? -1 : focus.index() - MODE_COUNT;
	}

	public BukovRaidMode currentMode() {
		return currentMode;
	}

	public BukovRaidMode draftMode() {
		return draftMode;
	}

	public boolean locked() {
		return locked;
	}

	public boolean hasPendingSelection() {
		return draftMode != currentMode;
	}

	public boolean applyEnabled() {
		return !locked && hasPendingSelection();
	}

	public boolean selectMode(int modeIndex) {
		if (locked || modeIndex < 0 || modeIndex >= MODE_COUNT) {
			return false;
		}
		draftMode = BukovRaidMode.values()[modeIndex];
		focus.focus(modeIndex);
		return true;
	}

	public boolean selectFocusedMode() {
		return modeFocused() && selectMode(modeIndex());
	}

	private boolean[] enabledTargets() {
		boolean[] enabled = new boolean[MODE_COUNT + ACTION_COUNT];
		for (int index = 0; index < MODE_COUNT; index++) {
			enabled[index] = true;
		}
		enabled[MODE_COUNT + ACTION_APPLY] = applyEnabled();
		enabled[MODE_COUNT + ACTION_BACK] = true;
		return enabled;
	}
}
