package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure presentation contract for the five explicit raid-mode cards. */
public final class BukovRaidModeSelectionViewModel {

	public static final class ModeCard {

		public final BukovRaidMode mode;
		public final String code;
		public final String name;
		public final String equipmentSource;
		public final String deathLoss;
		public final String durationAndExtraction;
		public final String rewardAndBoss;
		public final boolean current;

		private ModeCard(
				BukovRaidMode mode,
				String code,
				String equipmentSource,
				String deathLoss,
				String durationAndExtraction,
				String rewardAndBoss,
				boolean current) {
			this.mode = mode;
			this.code = code;
			name = mode.displayName;
			this.equipmentSource = equipmentSource;
			this.deathLoss = deathLoss;
			this.durationAndExtraction = durationAndExtraction;
			this.rewardAndBoss = rewardAndBoss;
			this.current = current;
		}
	}

	public final List<ModeCard> cards;
	public final BukovRaidMode currentMode;
	public final boolean locked;
	public final String stateMessage;

	private BukovRaidModeSelectionViewModel(
			List<ModeCard> cards,
			BukovRaidMode currentMode,
			boolean locked) {
		this.cards = Collections.unmodifiableList(cards);
		this.currentMode = currentMode;
		this.locked = locked;
		stateMessage = locked
				? "行动进行中 · 模式只读锁定"
				: "选择模式并应用 · 出击仍需单独确认";
	}

	public static BukovRaidModeSelectionViewModel from(
			BukovRaidMode currentMode,
			boolean locked) {
		if (currentMode == null) {
			throw new IllegalArgumentException("currentMode is required");
		}
		List<ModeCard> cards = new ArrayList<>();
		BukovRaidMode[] modes = BukovRaidMode.values();
		for (int index = 0; index < modes.length; index++) {
			BukovRaidMode mode = modes[index];
			cards.add(new ModeCard(
					mode,
					String.format(Locale.ROOT, "%02d", index + 1),
					equipmentSource(mode),
					deathLoss(mode),
					durationAndExtraction(mode),
					rewardAndBoss(mode),
					mode == currentMode));
		}
		return new BukovRaidModeSelectionViewModel(
				cards,
				currentMode,
				locked);
	}

	private static String equipmentSource(BukovRaidMode mode) {
		switch (mode) {
			case SCAVENGER:
				return "装备：系统拾荒套装";
			case TRAINING_GROUND:
				return "装备：免费制式装备";
			default:
				return "装备：自备仓库配装";
		}
	}

	private static String deathLoss(BukovRaidMode mode) {
		switch (mode) {
			case QUICK_SWEEP:
				return "死亡：保留最高价值带入物";
			case SCAVENGER:
				return "死亡：拾取物损失，仓库无风险";
			case TRAINING_GROUND:
				return "死亡：无仓库损失，不计经济结算";
			default:
				return "死亡：带入与拾取物全部损失";
		}
	}

	private static String durationAndExtraction(BukovRaidMode mode) {
		String duration = String.format(
				Locale.ROOT,
				"%.0f–%.0f分钟",
				mode.targetMinutesMinimum,
				mode.targetMinutesMaximum);
		switch (mode) {
			case QUICK_SWEEP:
				return duration + " · 快速撤离，超时增压";
			case SCAVENGER:
				return duration + " · 拾荒撤离，超时增压";
			case BOSS_CONTRACT:
				return duration + " · Boss目标/条件撤离";
			case TRAINING_GROUND:
				return duration + " · 随时撤离，短程演练";
			default:
				return duration + " · 条件撤离，超时增压";
		}
	}

	private static String rewardAndBoss(BukovRaidMode mode) {
		String multiplier = String.format(
				Locale.ROOT,
				"倍率 ×%.2f",
				mode.lootValueMultiplier);
		if (mode == BukovRaidMode.TRAINING_GROUND) {
			return multiplier + "（不结算） · Boss关闭";
		}
		if (mode == BukovRaidMode.BOSS_CONTRACT) {
			return multiplier + " · Boss合同目标";
		}
		return multiplier + " · Boss"
				+ (mode.bossEnabled ? "开启" : "关闭");
	}
}
