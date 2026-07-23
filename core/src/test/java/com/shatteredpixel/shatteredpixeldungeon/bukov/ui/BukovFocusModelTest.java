package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovFocusModelTest {

	@Test
	public void focusWrapsAndSkipsDisabledTargets() {
		BukovFocusModel focus = new BukovFocusModel(5, 2);
		boolean[] enabled = {true, false, true, false, true};

		focus.move(1, enabled);
		assertEquals(4, focus.index());
		focus.move(1, enabled);
		assertEquals(0, focus.index());
		focus.move(-1, enabled);
		assertEquals(4, focus.index());
	}

	@Test
	public void twentyChildOpenCloseCyclesRestoreExactFocus() {
		BukovFocusModel focus = new BukovFocusModel(7, 4);

		for (int cycle = 0; cycle < 20; cycle++) {
			focus.pushChild();
			focus.focus((cycle + 1) % focus.count());
			focus.popChild();
			assertEquals(4, focus.index());
			assertEquals(0, focus.depth());
		}
	}

	@Test
	public void shrinkingDynamicListKeepsValidReturnTarget() {
		BukovFocusModel focus = new BukovFocusModel(8, 7);
		focus.pushChild();
		focus.setCount(3);
		focus.popChild();

		assertEquals(2, focus.index());
	}
}
