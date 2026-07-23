package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.InMemoryBukovSaveService;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovRealtimeResumePersistenceTest {

	@Test
	public void tenCoordinatorResumesDoNotRerunInitialSpawnOrDriftProgress()
			throws IOException {
		BukovSaveService saves = new InMemoryBukovSaveService();
		BukovRaidCoordinator raid = BukovRaidCoordinator.start(
				saves,
				918273645L,
				"realtime-resume",
				30f,
				Collections.singletonList(ExtractionState.basic()));

		RaidSession session = raid.session();
		assertEquals(0L, session.claimEnemySpawnEpoch());
		assertEquals(1L, session.claimEnemySpawnEpoch());
		assertEquals(2L, session.claimEnemySpawnEpoch());
		session.markInitialEnemySpawnCompleted();
		session.recordKill();
		session.recordKill();
		raid.saveCheckpoint();

		for (int resume = 0; resume < 10; resume++) {
			raid = BukovRaidCoordinator.resume(saves);
			assertTrue(raid.session().initialEnemySpawnCompleted());
			assertEquals(3L, raid.session().enemySpawnEpoch());
			assertEquals(2, raid.session().killCount());
			raid.saveCheckpoint();
		}
	}

	@Test
	public void tenHostBundleResumesKeepEnemyCountAndActorIdsStable() {
		Actor.clear();
		List<BukovHostMob> enemies = new ArrayList<>();
		enemies.add(new BukovHostMob());
		enemies.add(new BukovHostMob());
		List<Integer> expectedIds = actorIds(enemies);

		for (int resume = 0; resume < 10; resume++) {
			Bundle stored = new Bundle();
			stored.put("enemies", enemies);
			Actor.clear();
			enemies = restoreEnemies(stored.getCollection("enemies"));
			assertEquals(2, enemies.size());
			assertEquals(expectedIds, actorIds(enemies));
		}
	}

	private static List<Integer> actorIds(List<BukovHostMob> enemies) {
		List<Integer> result = new ArrayList<>();
		for (BukovHostMob enemy : enemies) {
			result.add(enemy.id());
		}
		return result;
	}

	private static List<BukovHostMob> restoreEnemies(
			Collection<Bundlable> stored) {
		List<BukovHostMob> result = new ArrayList<>();
		for (Bundlable entry : stored) {
			result.add((BukovHostMob)entry);
		}
		return result;
	}
}
