package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

/** Pure focus model shared by the three long-term service tabs. */
public final class BukovServicesFocusModel {

	public static final int ACTION_TAB = 0;
	public static final int ACTION_PRIMARY = 1;
	public static final int ACTION_SECONDARY = 2;
	public static final int ACTION_BACK = 3;
	public static final int ACTION_COUNT = 4;

	private final int rowCount;
	private int index;
	private int selectedRow;

	public BukovServicesFocusModel(int rowCount, int selectedRow) {
		if (rowCount < 0) {
			throw new IllegalArgumentException("rowCount must be non-negative");
		}
		this.rowCount = rowCount;
		this.selectedRow = rowCount == 0 ? -1
				: Math.max(0, Math.min(selectedRow, rowCount - 1));
		index = rowCount == 0 ? rowCount : this.selectedRow;
	}

	public boolean rowFocused() {
		return index < rowCount;
	}

	public int rowIndex() {
		return rowFocused() ? index : -1;
	}

	public int selectedRow() {
		return selectedRow;
	}

	public int actionIndex() {
		return rowFocused() ? -1 : index - rowCount;
	}

	public void selectRow(int row) {
		if (row < 0 || row >= rowCount) {
			throw new IllegalArgumentException("row is out of range");
		}
		selectedRow = row;
		index = row;
	}

	public void move(int delta) {
		if (delta == 0) return;
		int count = rowCount + ACTION_COUNT;
		index = (index + (delta < 0 ? -1 : 1)) % count;
		if (index < 0) index += count;
	}
}
