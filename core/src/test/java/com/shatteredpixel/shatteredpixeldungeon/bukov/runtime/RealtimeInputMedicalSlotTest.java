package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RealtimeInputMedicalSlotTest {

	@Test
	public void keyboardAndControllerCatalogMapFourIndependentSlots() {
		assertEquals(1, BukovInputBindings.medicalSlot(
				SPDAction.QUICKSLOT_1, false));
		assertEquals(4, BukovInputBindings.medicalSlot(
				SPDAction.QUICKSLOT_4, false));

		assertEquals(1, BukovInputBindings.medicalSlot(
				SPDAction.TAG_ACTION, true));
		assertEquals(2, BukovInputBindings.medicalSlot(
				SPDAction.TAG_LOOT, true));
		assertEquals(3, BukovInputBindings.medicalSlot(
				SPDAction.TAG_RESUME, true));
		assertEquals(4, BukovInputBindings.medicalSlot(
				SPDAction.CYCLE, true));
	}
}
