package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.ControllerHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RealtimeInputMedicalSlotTest {

	@Test
	public void quickslotActionsAndDpadMapToFourIndependentSlots() {
		assertEquals(1, RealtimeInput.medicalSlotFor(
				SPDAction.QUICKSLOT_1, Input.Keys.NUM_1));
		assertEquals(4, RealtimeInput.medicalSlotFor(
				SPDAction.QUICKSLOT_4, Input.Keys.NUM_4));

		int offset = ControllerHandler.DPAD_KEY_OFFSET;
		assertEquals(1, RealtimeInput.medicalSlotFor(
				SPDAction.NONE, Input.Keys.DPAD_UP + offset));
		assertEquals(2, RealtimeInput.medicalSlotFor(
				SPDAction.NONE, Input.Keys.DPAD_RIGHT + offset));
		assertEquals(3, RealtimeInput.medicalSlotFor(
				SPDAction.NONE, Input.Keys.DPAD_DOWN + offset));
		assertEquals(4, RealtimeInput.medicalSlotFor(
				SPDAction.NONE, Input.Keys.DPAD_LEFT + offset));
	}
}
