package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

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
			name = modeName(mode);
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
				? BukovMessages.get("bukov.economy.mode.state_locked")
				: BukovMessages.get("bukov.economy.mode.state_select");
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
				return BukovMessages.get("bukov.economy.mode.equipment_scavenger");
			case TRAINING_GROUND:
				return BukovMessages.get("bukov.economy.mode.equipment_training");
			default:
				return BukovMessages.get("bukov.economy.mode.equipment_player");
		}
	}

	private static String deathLoss(BukovRaidMode mode) {
		switch (mode) {
			case QUICK_SWEEP:
				return BukovMessages.get("bukov.economy.mode.loss_quick");
			case SCAVENGER:
				return BukovMessages.get("bukov.economy.mode.loss_scavenger");
			case TRAINING_GROUND:
				return BukovMessages.get("bukov.economy.mode.loss_training");
			default:
				return BukovMessages.get("bukov.economy.mode.loss_formal");
		}
	}

	private static String durationAndExtraction(BukovRaidMode mode) {
		String duration = BukovMessages.get(
				"bukov.economy.mode.duration",
				mode.targetMinutesMinimum,
				mode.targetMinutesMaximum);
		switch (mode) {
			case QUICK_SWEEP:
				return BukovMessages.get(
						"bukov.economy.mode.extraction_quick", duration);
			case SCAVENGER:
				return BukovMessages.get(
						"bukov.economy.mode.extraction_scavenger", duration);
			case BOSS_CONTRACT:
				return BukovMessages.get(
						"bukov.economy.mode.extraction_boss", duration);
			case TRAINING_GROUND:
				return BukovMessages.get(
						"bukov.economy.mode.extraction_training", duration);
			default:
				return BukovMessages.get(
						"bukov.economy.mode.extraction_formal", duration);
		}
	}

	private static String rewardAndBoss(BukovRaidMode mode) {
		String multiplier = BukovMessages.get(
				"bukov.economy.mode.multiplier",
				mode.lootValueMultiplier);
		if (mode == BukovRaidMode.TRAINING_GROUND) {
			return BukovMessages.get(
					"bukov.economy.mode.reward_training", multiplier);
		}
		if (mode == BukovRaidMode.BOSS_CONTRACT) {
			return BukovMessages.get(
					"bukov.economy.mode.reward_boss", multiplier);
		}
		return BukovMessages.get(
				mode.bossEnabled
						? "bukov.economy.mode.reward_enabled"
						: "bukov.economy.mode.reward_disabled",
				multiplier);
	}

	static String modeName(BukovRaidMode mode) {
		return BukovMessages.get(
				"bukov.economy.mode.name_" + mode.name().toLowerCase(
						Locale.ROOT));
	}
}
