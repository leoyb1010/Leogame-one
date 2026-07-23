package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/** Pure focus and selection state for keyboard/controller vendor navigation. */
public final class BukovVendorFocusModel {

	public static final int ACTION_TAB = 0;
	public static final int ACTION_TRADE = 1;
	public static final int ACTION_BACK = 2;
	public static final int ACTION_COUNT = 3;

	private final int itemCount;
	private int index;
	private int selectedItem;

	public BukovVendorFocusModel(int itemCount, int selectedItem) {
		if (itemCount < 0) {
			throw new IllegalArgumentException("itemCount must be non-negative");
		}
		this.itemCount = itemCount;
		this.selectedItem = itemCount == 0
				? -1
				: Math.max(0, Math.min(selectedItem, itemCount - 1));
		index = itemCount == 0 ? itemCount : this.selectedItem;
	}

	public int index() {
		return index;
	}

	public int selectedItem() {
		return selectedItem;
	}

	public boolean itemFocused() {
		return index < itemCount;
	}

	public int itemIndex() {
		return itemFocused() ? index : -1;
	}

	public int actionIndex() {
		return itemFocused() ? -1 : index - itemCount;
	}

	public void selectItem(int item) {
		if (item < 0 || item >= itemCount) {
			throw new IllegalArgumentException("item selection is out of range");
		}
		selectedItem = item;
		index = item;
	}

	public void focus(int target) {
		int count = itemCount + ACTION_COUNT;
		index = Math.max(0, Math.min(target, count - 1));
	}

	public void move(int delta) {
		if (delta == 0) {
			return;
		}
		int count = itemCount + ACTION_COUNT;
		index = (index + (delta < 0 ? -1 : 1)) % count;
		if (index < 0) {
			index += count;
		}
	}
}
