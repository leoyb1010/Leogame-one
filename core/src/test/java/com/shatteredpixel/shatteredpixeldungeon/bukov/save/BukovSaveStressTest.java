package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSaveStressTest {

	@Test
	public void repeatedCheckpointResumeAndSettlementRemainConsistent()
			throws IOException {
		int iterations = Integer.getInteger("bukov.save.iterations", 10);
		for (int iteration = 0; iteration < iterations; iteration++) {
			InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
			String raidId = "stress-" + iteration;
			BukovRaidCoordinator raid = BukovRaidCoordinator.start(
					saves,
					0x42554B4F56L + iteration,
					raidId,
					40f,
					Collections.singletonList(ExtractionState.basic())
			);
			assertEquals(
					com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction
							.PickupResult.ADDED,
					raid.pickup(new RaidItem(
							"item-" + iteration,
							"stress_loot",
							1,
							0.25f,
							100,
							true,
							false,
							1f
					))
			);
			raid.tick(1.25f, ExtractionState.Interaction.NONE);
			raid.saveCheckpoint();

			BukovRaidCoordinator resumed =
					BukovRaidCoordinator.resume(saves);
			assertEquals(1.25f, resumed.session().elapsedSeconds, 0.0001f);
			assertEquals(1, resumed.loot().items().size());
			assertTrue(resumed.beginExtraction("E01"));
			for (int step = 0; step < 601; step++) {
				resumed.tick(1f / 120f, ExtractionState.Interaction.ACTIVE);
			}

			RaidResult result = resumed.settleSuccess();
			assertEquals(RaidOutcome.SUCCESS, result.outcome());
			assertEquals(100L, result.transferredValue());
			assertTrue(saves.loadProfile().isSettled(raidId));
			assertTrue(saves.loadRaidCheckpoint() == null);
		}
	}
}
