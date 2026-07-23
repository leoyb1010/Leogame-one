package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.watabou.utils.Bundle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BukovLoadoutTest {

	@Test
	public void selectedItemsReportDeploymentWeightAndRiskValue() {
		BukovStash stash = new BukovStash();
		stash.deposit(item("weapon", 1, 2f, 800));
		stash.deposit(item("ammo", 20, 0.01f, 10));
		BukovLoadout loadout = new BukovLoadout();

		loadout.select("weapon", stash);
		loadout.select("ammo", stash);

		assertEquals(2, loadout.distinctItemCount());
		assertEquals(2.2f, loadout.totalWeight(stash), 0.0001f);
		assertEquals(1000L, loadout.totalValue(stash));
	}

	@Test
	public void bundleRoundTripAndPruneKeepOnlyExistingStashUids() {
		BukovStash stash = new BukovStash();
		stash.deposit(item("kept", 1, 1f, 10));
		stash.deposit(item("removed", 1, 1f, 20));
		BukovLoadout loadout = new BukovLoadout();
		loadout.select("kept", stash);
		loadout.select("removed", stash);

		Bundle bundle = new Bundle();
		bundle.put("loadout", loadout);
		BukovLoadout restored = (BukovLoadout) bundle.get("loadout");
		stash.withdraw("removed");
		restored.pruneMissing(stash);

		assertTrue(restored.contains("kept"));
		assertFalse(restored.contains("removed"));
		assertEquals(1, restored.distinctItemCount());
	}

	@Test
	public void missionArchiveStaysInStashButCannotBeDeployed() {
		BukovStash stash = new BukovStash();
		RaidItem archive = new RaidItem(
				"archive",
				FirstRaidMission.ARCHIVE_DEFINITION_ID,
				1,
				0.2f,
				900,
				false,
				false,
				1f);
		stash.deposit(archive);
		BukovLoadout loadout = new BukovLoadout();

		try {
			loadout.select(archive.itemUid(), stash);
			fail("mission evidence must never enter a later raid");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Mission"));
		}

		assertTrue(stash.contains(archive.itemUid()));
		assertFalse(loadout.contains(archive.itemUid()));
		assertFalse(BukovLoadout.deployable(archive));
	}

	@Test
	public void migrationPrunesPreviouslySelectedMissionArchive() {
		BukovStash stash = new BukovStash();
		stash.deposit(new RaidItem(
				"archive",
				FirstRaidMission.ARCHIVE_DEFINITION_ID,
				1,
				0.2f,
				0,
				false,
				false,
				1f));
		Bundle stored = new Bundle();
		stored.put("selected_uids", new String[] {"archive"});
		BukovLoadout restored = new BukovLoadout();
		restored.restoreFromBundle(stored);

		restored.pruneMissing(stash);

		assertFalse(restored.contains("archive"));
		assertEquals(0, restored.distinctItemCount());
	}

	private static RaidItem item(
			String uid,
			int quantity,
			float weight,
			int value) {
		return new RaidItem(
				uid,
				"def-" + uid,
				quantity,
				weight,
				value,
				false,
				false,
				1f);
	}
}
