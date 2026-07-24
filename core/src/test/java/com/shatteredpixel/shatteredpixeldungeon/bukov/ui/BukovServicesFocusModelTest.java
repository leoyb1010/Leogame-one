package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovServicesFocusModelTest {

	@Test
	public void rowsAndFourActionsWrapForControllerNavigation() {
		BukovServicesFocusModel focus =
				new BukovServicesFocusModel(3, 1);
		assertTrue(focus.rowFocused());
		assertEquals(1, focus.rowIndex());

		for (int index = 0; index < 2; index++) focus.move(1);
		assertFalse(focus.rowFocused());
		assertEquals(
				BukovServicesFocusModel.ACTION_TAB,
				focus.actionIndex());
		for (int index = 0;
				index < BukovServicesFocusModel.ACTION_COUNT;
				index++) {
			focus.move(1);
		}
		assertTrue(focus.rowFocused());
		assertEquals(0, focus.rowIndex());
		focus.move(-1);
		assertEquals(
				BukovServicesFocusModel.ACTION_BACK,
				focus.actionIndex());
	}

	@Test
	public void emptyListStillExposesEveryAction() {
		BukovServicesFocusModel focus =
				new BukovServicesFocusModel(0, 0);
		assertFalse(focus.rowFocused());
		assertEquals(
				BukovServicesFocusModel.ACTION_TAB,
				focus.actionIndex());
		focus.move(-1);
		assertEquals(
				BukovServicesFocusModel.ACTION_BACK,
				focus.actionIndex());
	}
}
