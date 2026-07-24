package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidSettlement;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovSettlementViewModelTest {

	@Test
	public void extractionShowsReadableManifestAndRealStats() {
		LootTransaction loot = new LootTransaction("raid-success", 30f);
		loot.pickup(new RaidItem(
				"uid-bandage",
				"bandage",
				3,
				0.12f,
				180,
				true,
				false,
				1f));
		RaidResult result = new RaidSettlement().settle(
				new BukovProfile(),
				loot,
				RaidOutcome.SUCCESS);

		BukovSettlementViewModel viewModel =
				BukovSettlementViewModel.from(result, 125f, 4);

		assertEquals(
				BukovMessages.get(
						"bukov.economy.settlement.headline_success"),
				viewModel.headline);
		assertEquals("02:05", viewModel.duration);
		assertEquals(4, viewModel.kills);
		assertEquals(3L, viewModel.quantity);
		assertEquals(540L, viewModel.value);
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.item_summary",
						BukovMessages.get("bukov.economy.item.bandage"),
						3,
						540L),
				viewModel.items.get(0).summary());
		assertFalse(viewModel.legacyDetails);
	}

	@Test
	public void deathShowsLossInsteadOfDungeonGameOverLanguage() {
		LootTransaction loot = new LootTransaction("raid-death", 30f);
		loot.pickup(new RaidItem(
				"uid-weapon",
				"firearm:needle_9",
				1,
				0.9f,
				850,
				false,
				false,
				0.7f));
		RaidResult result = new RaidSettlement().settle(
				new BukovProfile(),
				loot,
				RaidOutcome.DEATH);

		BukovSettlementViewModel viewModel =
				BukovSettlementViewModel.from(result, 9f, 0);

		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.headline_failed"),
				viewModel.headline);
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.manifest_failed"),
				viewModel.manifestTitle);
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.totals_failed",
						1L,
						850L),
				viewModel.totals());
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.item_summary",
						BukovMessages.get(
								"bukov.economy.item.firearm_needle_9"),
						1,
						850L),
				viewModel.items.get(0).summary());
	}

	@Test
	public void durableDebriefOverridesTransientUiArguments() {
		LootTransaction loot = new LootTransaction("durable-debrief", 30f);
		loot.pickup(new RaidItem(
				"uid-archive",
				"maintenance_access_archive",
				1,
				0.2f,
				900,
				true,
				false,
				1f));
		RaidResult result = new RaidSettlement().settle(
				new BukovProfile(),
				loot,
				RaidOutcome.SUCCESS,
				185f,
				6,
				true);

		BukovSettlementViewModel viewModel =
				BukovSettlementViewModel.from(result, 1f, 0);

		assertEquals("03:05", viewModel.duration);
		assertEquals(6, viewModel.kills);
		assertTrue(viewModel.missionCompleted);
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.mission_returned"),
				viewModel.mission());
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.totals_success",
						1L,
						900L),
				viewModel.totals());
		assertEquals(BukovMessages.get(
						"bukov.economy.settlement.earnings_success",
						900L),
				viewModel.earnings());
	}
}
