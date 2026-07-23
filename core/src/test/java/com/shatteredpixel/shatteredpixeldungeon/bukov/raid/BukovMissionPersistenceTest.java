package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovMissionPersistenceTest {

	@Test
	public void completedArchiveEventSurvivesCoordinatorResume()
			throws IOException {
		InMemoryBukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator started = BukovRaidCoordinator.start(
				saves,
				334455L,
				"mission-persistence",
				40f,
				Collections.singletonList(ExtractionState.basic()));

		assertFalse(started.eventCompleted(FirstRaidMission.EVENT_ID));
		assertTrue(started.completeEvent(FirstRaidMission.EVENT_ID));

		BukovRaidCoordinator resumed = BukovRaidCoordinator.resume(saves);
		assertNotNull(resumed);
		assertTrue(resumed.eventCompleted(FirstRaidMission.EVENT_ID));
	}
}
