package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovVendorFocusModelTest {

	@Test
	public void rowsAndActionsWrapForKeyboardAndController() {
		BukovVendorFocusModel focus = new BukovVendorFocusModel(2, 0);

		assertTrue(focus.itemFocused());
		assertEquals(0, focus.selectedItem());
		focus.move(1);
		assertEquals(0, focus.selectedItem());
		focus.move(1);
		assertFalse(focus.itemFocused());
		assertEquals(BukovVendorFocusModel.ACTION_TAB, focus.actionIndex());
		for (int i = 0; i < BukovVendorFocusModel.ACTION_COUNT; i++) {
			focus.move(1);
		}
		assertTrue(focus.itemFocused());
		assertEquals(0, focus.itemIndex());
		focus.move(-1);
		assertEquals(BukovVendorFocusModel.ACTION_BACK, focus.actionIndex());
	}

	@Test
	public void emptyStockStillExposesAllActions() {
		BukovVendorFocusModel focus = new BukovVendorFocusModel(0, 9);

		assertFalse(focus.itemFocused());
		assertEquals(-1, focus.selectedItem());
		assertEquals(BukovVendorFocusModel.ACTION_TAB, focus.actionIndex());
		focus.focus(99);
		assertEquals(BukovVendorFocusModel.ACTION_BACK, focus.actionIndex());
	}

	@Test
	public void mouseSelectionAlsoMovesFocusToExactRow() {
		BukovVendorFocusModel focus = new BukovVendorFocusModel(4, 0);

		focus.selectItem(3);

		assertTrue(focus.itemFocused());
		assertEquals(3, focus.itemIndex());
		assertEquals(3, focus.selectedItem());
	}
}
