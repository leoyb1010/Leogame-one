package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RaidSessionModeMigrationTest {

	@Test
	public void legacyCheckpointDefaultsToFirstExpedition() {
		Bundle legacy = new Bundle();
		legacy.put("seed", 1977L);
		legacy.put("raid_id", "legacy-active");
		legacy.put("elapsed", 42f);
		legacy.put("settled", false);
		legacy.put("map_hash", "legacy-map");
		legacy.put("initial_enemy_spawn_completed", true);
		legacy.put("enemy_spawn_epoch", 3L);
		legacy.put("kill_count", 1);

		RaidSession restored = new RaidSession();
		restored.restoreFromBundle(legacy);

		assertEquals(BukovRaidMode.EXPEDITION, restored.raidMode());
		assertEquals(1, restored.raidOrdinal());
		assertTrue(restored.firstRaidProtectionActive());
		assertTrue(restored.keyDoors().unlockedDoorIds().isEmpty());
	}
}
