package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/** Pure focus state shared by keyboard and controller navigation. */
public final class BukovHubFocusModel {

	public static final int ACTION_VENDOR = 0;
	public static final int ACTION_REPEAT = 1;
	public static final int ACTION_CLEAR = 2;
	public static final int ACTION_DEPLOY = 3;
	public static final int ACTION_BACK = 4;
	public static final int ACTION_COUNT = 5;
	private static final int UTILITY_COUNT = 4;

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
		int count = itemCount + UTILITY_COUNT + ACTION_COUNT;
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

	public boolean sortFocused() {
		return index == itemCount + 2;
	}

	public boolean searchFocused() {
		return index == itemCount + 3;
	}

	public int itemIndex() {
		return itemFocused() ? index : -1;
	}

	public int actionIndex() {
		return itemFocused()
				|| modeFocused()
				|| filterFocused()
				|| sortFocused()
				|| searchFocused()
				? -1 : index - itemCount - UTILITY_COUNT;
	}

	public void move(int delta) {
		int count = itemCount + UTILITY_COUNT + ACTION_COUNT;
		index = (index + delta) % count;
		if (index < 0) {
			index += count;
		}
	}
}
