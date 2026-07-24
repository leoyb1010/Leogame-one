package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidModeFocusModelTest {

	@Test
	public void cardSelectionChangesDraftWithoutChangingCurrentMode() {
		BukovRaidModeFocusModel focus =
				new BukovRaidModeFocusModel(
						BukovRaidMode.EXPEDITION,
						false);

		assertEquals(BukovRaidMode.EXPEDITION, focus.currentMode());
		assertEquals(BukovRaidMode.EXPEDITION, focus.draftMode());
		assertFalse(focus.applyEnabled());
		assertTrue(focus.selectMode(BukovRaidMode.SCAVENGER.ordinal()));
		assertEquals(BukovRaidMode.EXPEDITION, focus.currentMode());
		assertEquals(BukovRaidMode.SCAVENGER, focus.draftMode());
		assertTrue(focus.hasPendingSelection());
		assertTrue(focus.applyEnabled());
	}

	@Test
	public void oneFocusTraversesCardsAndEnabledFooterActions() {
		BukovRaidModeFocusModel focus =
				new BukovRaidModeFocusModel(
						BukovRaidMode.TRAINING_GROUND,
						false);

		assertTrue(focus.modeFocused());
		assertEquals(BukovRaidMode.TRAINING_GROUND.ordinal(),
				focus.modeIndex());
		// Apply is disabled while the draft equals the current mode.
		focus.move(1);
		assertFalse(focus.modeFocused());
		assertEquals(BukovRaidModeFocusModel.ACTION_BACK,
				focus.actionIndex());
		focus.move(1);
		assertTrue(focus.modeFocused());
		assertEquals(0, focus.modeIndex());

		focus.selectMode(BukovRaidMode.BOSS_CONTRACT.ordinal());
		focus.focus(BukovRaidModeFocusModel.MODE_COUNT
				+ BukovRaidModeFocusModel.ACTION_APPLY);
		assertFalse(focus.modeFocused());
		assertEquals(BukovRaidModeFocusModel.ACTION_APPLY,
				focus.actionIndex());
		assertTrue(focus.applyEnabled());
	}

	@Test
	public void activeRaidLocksDraftAndSkipsApplyAction() {
		BukovRaidModeFocusModel focus =
				new BukovRaidModeFocusModel(
						BukovRaidMode.QUICK_SWEEP,
						true);

		assertTrue(focus.locked());
		assertFalse(focus.selectMode(BukovRaidMode.BOSS_CONTRACT.ordinal()));
		assertEquals(BukovRaidMode.QUICK_SWEEP, focus.currentMode());
		assertEquals(BukovRaidMode.QUICK_SWEEP, focus.draftMode());
		assertFalse(focus.applyEnabled());

		focus.focus(BukovRaidMode.TRAINING_GROUND.ordinal());
		focus.move(1);
		assertEquals(BukovRaidModeFocusModel.ACTION_BACK,
				focus.actionIndex());
	}
}
