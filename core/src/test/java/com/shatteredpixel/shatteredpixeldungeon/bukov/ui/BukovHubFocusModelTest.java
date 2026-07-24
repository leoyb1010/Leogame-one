package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovHubFocusModelTest {

	@Test
	public void focusTraversesItemsAndActionsAndWraps() {
		BukovHubFocusModel focus = new BukovHubFocusModel(2);

		assertTrue(focus.itemFocused());
		assertEquals(0, focus.itemIndex());
		focus.move(2);
		assertTrue(focus.modeFocused());
		assertEquals(-1, focus.actionIndex());
		focus.move(1);
		assertTrue(focus.filterFocused());
		assertEquals(-1, focus.actionIndex());
		focus.move(1);
		assertTrue(focus.sortFocused());
		assertEquals(-1, focus.actionIndex());
		focus.move(1);
		assertTrue(focus.searchFocused());
		assertEquals(-1, focus.actionIndex());
		focus.move(1);
		assertFalse(focus.itemFocused());
		assertEquals(BukovHubFocusModel.ACTION_VENDOR, focus.actionIndex());
		focus.move(BukovHubFocusModel.ACTION_COUNT);
		assertTrue(focus.itemFocused());
		assertEquals(0, focus.itemIndex());
		focus.move(-1);
		assertEquals(BukovHubFocusModel.ACTION_BACK, focus.actionIndex());
	}

	@Test
	public void focusRestoresAcrossWindowRebuildAndClampsSafely() {
		BukovHubFocusModel focus = new BukovHubFocusModel(3);
		focus.focus(3);
		assertTrue(focus.modeFocused());
		focus.focus(99);
		assertEquals(BukovHubFocusModel.ACTION_BACK, focus.actionIndex());
	}

	@Test
	public void nestedVendorReturnRestoresExactHubActionFocus() {
		BukovHubFocusModel beforeVendor = new BukovHubFocusModel(3);
		beforeVendor.focus(3 + 4 + BukovHubFocusModel.ACTION_VENDOR);
		int savedFocus = beforeVendor.index();

		BukovHubFocusModel restored = new BukovHubFocusModel(3);
		restored.focus(savedFocus);

		assertFalse(restored.itemFocused());
		assertEquals(
				BukovHubFocusModel.ACTION_VENDOR,
				restored.actionIndex());
	}

	@Test
	public void searchReturnRecomputesSemanticFocusAfterQueryChangesRows() {
		BukovHubFocusModel beforeSearch = new BukovHubFocusModel(5);
		beforeSearch.focus(5 + 3);
		assertTrue(beforeSearch.searchFocused());

		BukovHubFocusModel afterSearch = new BukovHubFocusModel(1);
		afterSearch.focus(1 + 3);

		assertTrue(afterSearch.searchFocused());
		assertFalse(afterSearch.itemFocused());
		assertEquals(-1, afterSearch.actionIndex());
	}
}
