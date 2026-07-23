package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RaidSettlementDebriefTest {

	@Test
	public void debriefAndMissionProgressCommitWithLootExactlyOnce()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction("debrief", 40f);
		loot.pickup(new RaidItem(
				"archive",
				FirstRaidMission.ARCHIVE_DEFINITION_ID,
				1,
				0.2f,
				900,
				true,
				false,
				1f));

		RaidSettlement settlement = new RaidSettlement();
		RaidResult first = settlement.settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				91.5f,
				3,
				true);
		saves.saveProfile(profile);
		BukovProfile restored = saves.loadProfile();

		assertFalse(first.replayed());
		assertTrue(first.debriefAvailable());
		assertEquals(91.5f, first.elapsedSeconds(), 0f);
		assertEquals(3, first.kills());
		assertTrue(first.missionCompleted());
		assertTrue(restored.completedContracts().contains(
				FirstRaidMission.EVENT_ID));
		assertEquals(1, restored.stash().distinctItemCount());
		try {
			restored.loadout().select("archive", restored.stash());
			fail("archived mission evidence must not deploy again");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Mission"));
		}

		RaidResult replay = settlement.settle(
				restored,
				loot,
				RaidOutcome.SUCCESS,
				91.5f,
				3,
				true);
		assertTrue(replay.replayed());
		assertEquals(1, restored.stash().distinctItemCount());
		assertEquals(1, restored.settlements().size());
		assertEquals(1, restored.statistics().successfulRaids());

		try {
			settlement.settle(
					restored,
					loot,
					RaidOutcome.SUCCESS,
					92f,
					3,
					true);
			fail("changed durable debrief must be rejected");
		} catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("payload changed"));
		}
		assertEquals(1, restored.stash().distinctItemCount());
	}

	@Test
	public void coordinatorSnapshotsCheckpointStatsIntoReceipt()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				19L,
				"coordinator-debrief",
				40f,
				Collections.singletonList(ExtractionState.basic()));
		raid.session().recordKill();
		raid.session().recordKill();
		assertTrue(raid.completeEvent(FirstRaidMission.EVENT_ID));
		assertTrue(raid.beginExtraction("E01"));
		raid.tick(5f, ExtractionState.Interaction.ACTIVE);

		RaidResult result = raid.settleSuccess();
		SettlementReceipt receipt =
				saves.loadProfile().settlement("coordinator-debrief");

		assertEquals(5f, result.elapsedSeconds(), 0f);
		assertEquals(2, result.kills());
		assertTrue(result.missionCompleted());
		assertTrue(receipt.debriefAvailable());
		assertEquals(result.elapsedSeconds(), receipt.elapsedSeconds(), 0f);
		assertEquals(result.kills(), receipt.kills());
		assertTrue(receipt.missionCompleted());
		assertTrue(saves.loadRaidCheckpoint() == null);
	}
}
