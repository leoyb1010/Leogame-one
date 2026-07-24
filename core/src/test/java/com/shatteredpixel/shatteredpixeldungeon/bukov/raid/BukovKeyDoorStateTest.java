package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovKeyDoorStateTest {

	@Test
	public void keyStackIsConsumedOnceAndUnlockedDoorIsReplaySafe() {
		LootTransaction loot = new LootTransaction("key-raid", 20f);
		loot.pickup(new RaidItem(
				"keys-a", "key:maintenance", 2,
				0.05f, 100, true, false, 1f));
		BukovKeyDoorState doors = new BukovKeyDoorState();

		assertEquals(
				BukovKeyDoorState.UnlockResult.UNLOCKED,
				doors.unlock("door-maintenance", "key:maintenance", loot));
		assertEquals(1, loot.item("keys-a").quantity());
		assertEquals(
				BukovKeyDoorState.UnlockResult.ALREADY_UNLOCKED,
				doors.unlock("door-maintenance", "key:maintenance", loot));
		assertEquals(1, loot.item("keys-a").quantity());
		assertEquals(
				BukovKeyDoorState.UnlockResult.KEY_MISSING,
				doors.unlock("door-lab", "key:sealed-lab", loot));
		assertFalse(doors.unlocked("door-lab"));
	}

	@Test
	public void raidSessionRoundTripPreservesDoorUnlockWithoutAnotherKeyCost() {
		RaidSession session = RaidSession.create(4L, "door-persist");
		LootTransaction loot = new LootTransaction("door-persist", 20f);
		loot.pickup(new RaidItem(
				"key-one", "key:workshop", 1,
				0.05f, 50, true, false, 1f));
		assertEquals(
				BukovKeyDoorState.UnlockResult.UNLOCKED,
				session.keyDoors().unlock(
						"door-workshop", "key:workshop", loot));

		Bundle bundle = new Bundle();
		bundle.put("session", session);
		RaidSession restored = (RaidSession) bundle.get("session");

		assertTrue(restored.keyDoors().unlocked("door-workshop"));
		assertEquals(
				"key:workshop",
				restored.keyDoors().consumedKeyDefinition("door-workshop"));
		assertEquals(
				BukovKeyDoorState.UnlockResult.ALREADY_UNLOCKED,
				restored.keyDoors().unlock(
						"door-workshop", "key:workshop", loot));
	}
}
