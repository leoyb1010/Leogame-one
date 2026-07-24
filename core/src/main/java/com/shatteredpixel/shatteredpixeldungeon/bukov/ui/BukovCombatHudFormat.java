package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;

/** Text and shape redundancy for directional combat awareness. */
public final class BukovCombatHudFormat {

	public static String sound(BukovRaidHudState state) {
		if (state == null || !state.soundVisible()) return "";
		return (state.colorblindAssist() ? "◆ " : "")
				+ directionShape(state.soundDirection()) + " "
				+ directionText(state.soundDirection()) + " · "
				+ distanceShape(state.soundDistance()) + " "
				+ distanceText(state.soundDistance()) + " · "
				+ categoryText(state.soundCategory());
	}

	public static String hit(BukovRaidHudState state) {
		if (state == null || !state.hitVisible()) return "";
		return (state.colorblindAssist() ? "▲ " : "")
				+ "受击 "
				+ directionShape(state.hitDirection()) + " "
				+ directionText(state.hitDirection());
	}

	public static String bossTitle(BukovRaidHudState state) {
		if (state == null || !state.bossActive()) return "";
		return "BOSS " + safe(state.bossName(), "白线")
				+ " · 阶段 " + state.bossPhase()
				+ "/" + state.bossPhaseCount()
				+ " " + safe(state.bossPhaseLabel(), "")
				+ " · HP " + state.bossHealth()
				+ "/" + state.bossMaximumHealth();
	}

	public static String bossObjective(BukovRaidHudState state) {
		if (state == null || !state.bossActive()) return "";
		String marker = state.bossVulnerable() ? "◇ 弱点开放" : "◆ 弱点锁定";
		String objective = safe(state.bossObjective(), "观察机制");
		return marker + " · " + objective
				+ (state.bossRetreatWarning() ? " · ⚠ 建议撤离" : "");
	}

	public static String navigation(BukovRaidHudState state) {
		if (state == null || !state.navigationVisible()) return "";
		String label = safe(
				state.navigationLabel(),
				cueText(state.navigationCue()));
		String availability = state.navigationAvailable()
				? "" : " · 未开放";
		return cueShape(state.navigationCue()) + " "
				+ directionShape(state.navigationDirection()) + " "
				+ label + " · "
				+ distanceText(state.navigationDistance())
				+ availability;
	}

	public static String threat(BukovRaidHudState state) {
		if (state == null || !state.threatVisible()) return "";
		return (state.threatUrgent() ? "⚠ " : "△ ")
				+ directionShape(state.threatDirection()) + " "
				+ safe(state.threatLabel(), "敌情") + " · "
				+ distanceText(state.threatDistance());
	}

	static String directionShape(BukovRaidHudState.Direction direction) {
		if (direction == null) return "•";
		switch (direction) {
			case N: return "↑";
			case NE: return "↗";
			case E: return "→";
			case SE: return "↘";
			case S: return "↓";
			case SW: return "↙";
			case W: return "←";
			case NW: return "↖";
			default: return "•";
		}
	}

	static String directionText(BukovRaidHudState.Direction direction) {
		if (direction == null) return "未知";
		switch (direction) {
			case N: return "北";
			case NE: return "东北";
			case E: return "东";
			case SE: return "东南";
			case S: return "南";
			case SW: return "西南";
			case W: return "西";
			case NW: return "西北";
			default: return "未知";
		}
	}

	private static String distanceShape(BukovRaidHudState.Distance distance) {
		if (distance == BukovRaidHudState.Distance.NEAR) return "●";
		if (distance == BukovRaidHudState.Distance.MID) return "◎";
		return "○";
	}

	private static String distanceText(BukovRaidHudState.Distance distance) {
		if (distance == BukovRaidHudState.Distance.NEAR) return "近";
		if (distance == BukovRaidHudState.Distance.MID) return "中";
		return "远";
	}

	private static String categoryText(SoundCategory category) {
		if (category == SoundCategory.ENEMY_GUNSHOT) return "敌方枪声";
		if (category == SoundCategory.FOOTSTEP) return "脚步";
		if (category == SoundCategory.BOSS_CUE) return "Boss预警";
		if (category == SoundCategory.EXTRACTION_CUE) return "撤离提示";
		return "关键声音";
	}

	private static String cueShape(BukovRaidHudState.Cue cue) {
		if (cue == BukovRaidHudState.Cue.PICKUP) return "◇";
		if (cue == BukovRaidHudState.Cue.MISSION) return "◆";
		if (cue == BukovRaidHudState.Cue.EXTRACTION) return "▣";
		return "•";
	}

	private static String cueText(BukovRaidHudState.Cue cue) {
		if (cue == BukovRaidHudState.Cue.PICKUP) return "可拾取物资";
		if (cue == BukovRaidHudState.Cue.MISSION) return "任务目标";
		if (cue == BukovRaidHudState.Cue.EXTRACTION) return "撤离点";
		return "目标";
	}

	private static String safe(String value, String fallback) {
		return value == null || value.isEmpty() ? fallback : value;
	}

	private BukovCombatHudFormat() {
	}
}
