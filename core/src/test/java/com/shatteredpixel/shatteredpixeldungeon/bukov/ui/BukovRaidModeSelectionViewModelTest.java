package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovRaidModeSelectionViewModelTest {

	@Test
	public void allFiveModesExposeCompleteDecisionInformation() {
		BukovRaidModeSelectionViewModel model =
				BukovRaidModeSelectionViewModel.from(
						BukovRaidMode.QUICK_SWEEP,
						false);

		assertEquals(5, model.cards.size());
		int currentCount = 0;
		for (int index = 0; index < model.cards.size(); index++) {
			BukovRaidModeSelectionViewModel.ModeCard card =
					model.cards.get(index);
			assertEquals(BukovRaidMode.values()[index], card.mode);
			assertEquals(String.format("%02d", index + 1), card.code);
			assertFalse(card.name.isEmpty());
			assertFalse(card.equipmentSource.isEmpty());
			assertFalse(card.deathLoss.isEmpty());
			assertFalse(card.durationAndExtraction.isEmpty());
			assertFalse(card.rewardAndBoss.isEmpty());
			if (card.current) currentCount++;
		}
		assertEquals(1, currentCount);
		assertTrue(model.cards.get(BukovRaidMode.QUICK_SWEEP.ordinal()).current);
		assertEquals(
				BukovMessages.get("bukov.economy.mode.state_select"),
				model.stateMessage);
	}

	@Test
	public void economyAndBossPoliciesMatchRuntimeModes() {
		BukovRaidModeSelectionViewModel model =
				BukovRaidModeSelectionViewModel.from(
						BukovRaidMode.EXPEDITION,
						false);

		BukovRaidModeSelectionViewModel.ModeCard expedition =
				model.cards.get(BukovRaidMode.EXPEDITION.ordinal());
		assertEquals(
				BukovMessages.get("bukov.economy.mode.equipment_player"),
				expedition.equipmentSource);
		assertEquals(
				BukovMessages.get("bukov.economy.mode.loss_formal"),
				expedition.deathLoss);
		assertEquals(
				reward("bukov.economy.mode.reward_enabled", 1f),
				expedition.rewardAndBoss);

		BukovRaidModeSelectionViewModel.ModeCard quick =
				model.cards.get(BukovRaidMode.QUICK_SWEEP.ordinal());
		assertEquals(
				BukovMessages.get("bukov.economy.mode.loss_quick"),
				quick.deathLoss);
		assertEquals(
				reward("bukov.economy.mode.reward_disabled", 0.72f),
				quick.rewardAndBoss);

		BukovRaidModeSelectionViewModel.ModeCard scavenger =
				model.cards.get(BukovRaidMode.SCAVENGER.ordinal());
		assertEquals(
				BukovMessages.get(
						"bukov.economy.mode.equipment_scavenger"),
				scavenger.equipmentSource);
		assertEquals(
				BukovMessages.get("bukov.economy.mode.loss_scavenger"),
				scavenger.deathLoss);
		assertEquals(
				reward("bukov.economy.mode.reward_disabled", 0.58f),
				scavenger.rewardAndBoss);

		BukovRaidModeSelectionViewModel.ModeCard boss =
				model.cards.get(BukovRaidMode.BOSS_CONTRACT.ordinal());
		assertEquals(
				reward("bukov.economy.mode.reward_boss", 1.25f),
				boss.rewardAndBoss);

		BukovRaidModeSelectionViewModel.ModeCard training =
				model.cards.get(BukovRaidMode.TRAINING_GROUND.ordinal());
		assertEquals(
				BukovMessages.get(
						"bukov.economy.mode.equipment_training"),
				training.equipmentSource);
		assertEquals(
				BukovMessages.get("bukov.economy.mode.loss_training"),
				training.deathLoss);
		assertEquals(
				reward("bukov.economy.mode.reward_training", 1f),
				training.rewardAndBoss);
	}

	@Test
	public void activeRaidPresentationIsReadOnlyAndKeepsCurrentMode() {
		BukovRaidModeSelectionViewModel model =
				BukovRaidModeSelectionViewModel.from(
						BukovRaidMode.BOSS_CONTRACT,
						true);

		assertTrue(model.locked);
		assertEquals(BukovRaidMode.BOSS_CONTRACT, model.currentMode);
		assertEquals(
				BukovMessages.get("bukov.economy.mode.state_locked"),
				model.stateMessage);
		assertTrue(model.cards.get(
				BukovRaidMode.BOSS_CONTRACT.ordinal()).current);
	}

	private static String reward(String key, float multiplier) {
		return BukovMessages.get(
				key,
				BukovMessages.get(
						"bukov.economy.mode.multiplier",
						multiplier));
	}
}
