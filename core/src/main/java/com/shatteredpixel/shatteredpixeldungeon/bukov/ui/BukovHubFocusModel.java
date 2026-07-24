package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/** Pure focus state shared by keyboard and controller navigation. */
public final class BukovHubFocusModel {

	public static final int ACTION_VENDOR = 0;
	public static final int ACTION_REPEAT = 1;
	public static final int ACTION_CLEAR = 2;
	public static final int ACTION_DEPLOY = 3;
	public static final int ACTION_BACK = 4;
	public static final int ACTION_COUNT = 5;

	private final int itemCount;
	private int index;

	public BukovHubFocusModel(int itemCount) {
		if (itemCount < 0) {
			throw new IllegalArgumentException("itemCount must be non-negative");
		}
		this.itemCount = itemCount;
	}

	public int index() {
		return index;
	}

	public int itemCount() {
		return itemCount;
	}

	public void focus(int target) {
		int count = itemCount + 2 + ACTION_COUNT;
		index = Math.max(0, Math.min(target, count - 1));
	}

	public boolean itemFocused() {
		return index < itemCount;
	}

	public boolean modeFocused() {
		return index == itemCount;
	}

	public boolean filterFocused() {
		return index == itemCount + 1;
	}

	public int itemIndex() {
		return itemFocused() ? index : -1;
	}

	public int actionIndex() {
		return itemFocused() || modeFocused() || filterFocused()
				? -1 : index - itemCount - 2;
	}

	public void move(int delta) {
		int count = itemCount + 2 + ACTION_COUNT;
		index = (index + delta) % count;
		if (index < 0) {
			index += count;
		}
	}
}
