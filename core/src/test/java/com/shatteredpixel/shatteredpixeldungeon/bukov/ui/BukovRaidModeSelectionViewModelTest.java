package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
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
			assertTrue(card.equipmentSource.startsWith("装备："));
			assertTrue(card.deathLoss.startsWith("死亡："));
			assertTrue(card.durationAndExtraction.contains("分钟"));
			assertTrue(card.durationAndExtraction.contains("撤离"));
			assertTrue(card.rewardAndBoss.contains("倍率 ×"));
			assertTrue(card.rewardAndBoss.contains("Boss"));
			if (card.current) currentCount++;
		}
		assertEquals(1, currentCount);
		assertTrue(model.cards.get(BukovRaidMode.QUICK_SWEEP.ordinal()).current);
		assertTrue(model.stateMessage.contains("出击仍需单独确认"));
	}

	@Test
	public void economyAndBossPoliciesMatchRuntimeModes() {
		BukovRaidModeSelectionViewModel model =
				BukovRaidModeSelectionViewModel.from(
						BukovRaidMode.EXPEDITION,
						false);

		BukovRaidModeSelectionViewModel.ModeCard expedition =
				model.cards.get(BukovRaidMode.EXPEDITION.ordinal());
		assertTrue(expedition.equipmentSource.contains("自备"));
		assertTrue(expedition.deathLoss.contains("全部损失"));
		assertTrue(expedition.rewardAndBoss.contains("×1.00"));
		assertTrue(expedition.rewardAndBoss.contains("Boss开启"));

		BukovRaidModeSelectionViewModel.ModeCard quick =
				model.cards.get(BukovRaidMode.QUICK_SWEEP.ordinal());
		assertTrue(quick.deathLoss.contains("保留最高价值"));
		assertTrue(quick.rewardAndBoss.contains("×0.72"));

		BukovRaidModeSelectionViewModel.ModeCard scavenger =
				model.cards.get(BukovRaidMode.SCAVENGER.ordinal());
		assertTrue(scavenger.equipmentSource.contains("系统拾荒"));
		assertTrue(scavenger.deathLoss.contains("仓库无风险"));
		assertTrue(scavenger.rewardAndBoss.contains("Boss关闭"));

		BukovRaidModeSelectionViewModel.ModeCard boss =
				model.cards.get(BukovRaidMode.BOSS_CONTRACT.ordinal());
		assertTrue(boss.rewardAndBoss.contains("×1.25"));
		assertTrue(boss.rewardAndBoss.contains("Boss合同目标"));

		BukovRaidModeSelectionViewModel.ModeCard training =
				model.cards.get(BukovRaidMode.TRAINING_GROUND.ordinal());
		assertTrue(training.equipmentSource.contains("免费制式"));
		assertTrue(training.deathLoss.contains("无仓库损失"));
		assertTrue(training.rewardAndBoss.contains("不结算"));
	}

	@Test
	public void activeRaidPresentationIsReadOnlyAndKeepsCurrentMode() {
		BukovRaidModeSelectionViewModel model =
				BukovRaidModeSelectionViewModel.from(
						BukovRaidMode.BOSS_CONTRACT,
						true);

		assertTrue(model.locked);
		assertEquals(BukovRaidMode.BOSS_CONTRACT, model.currentMode);
		assertTrue(model.stateMessage.contains("只读锁定"));
		assertTrue(model.cards.get(
				BukovRaidMode.BOSS_CONTRACT.ordinal()).current);
	}
}
