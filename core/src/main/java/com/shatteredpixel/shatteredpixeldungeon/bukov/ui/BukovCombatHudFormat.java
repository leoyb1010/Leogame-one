package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

/** Text and shape redundancy for directional combat awareness. */
public final class BukovCombatHudFormat {

	public static String sound(BukovRaidHudState state) {
		if (state == null || !state.soundVisible()) return "";
		return BukovMessages.get(
				"bukov.raid.combat.sound_format",
				state.colorblindAssist() ? "* " : "",
				directionShape(state.soundDirection()),
				directionText(state.soundDirection()),
				distanceShape(state.soundDistance()),
				distanceText(state.soundDistance()),
				categoryText(state.soundCategory()));
	}

	public static String hit(BukovRaidHudState state) {
		if (state == null || !state.hitVisible()) return "";
		StringBuilder directions = new StringBuilder();
		for (int index = 0; index < state.hitCount(); index++) {
			if (index > 0) directions.append(" · ");
			directions.append(directionShape(state.hitDirection(index)))
					.append(' ')
					.append(directionText(state.hitDirection(index)));
		}
		return (state.colorblindAssist() ? "^ " : "")
				+ BukovMessages.get(
						"bukov.raid.combat.hit_format",
						BukovMessages.get("bukov.raid.combat.hit_prefix"),
						directions);
	}

	public static String bossTitle(BukovRaidHudState state) {
		if (state == null || !state.bossActive()) return "";
		return BukovMessages.get(
				"bukov.raid.combat.boss_title_format",
				safe(
						state.bossName(),
						BukovMessages.get(
								"bukov.raid.combat.boss_name_default")),
				state.bossPhase(),
				state.bossPhaseCount(),
				safe(state.bossPhaseLabel(), ""),
				state.bossHealth(),
				state.bossMaximumHealth());
	}

	public static String bossObjective(BukovRaidHudState state) {
		if (state == null || !state.bossActive()) return "";
		String marker = state.bossVulnerable()
				? BukovMessages.get(
						"bukov.raid.combat.boss_weak_open")
				: BukovMessages.get(
						"bukov.raid.combat.boss_weak_locked");
		String objective = safe(
				state.bossObjective(),
				BukovMessages.get(
						"bukov.raid.combat.boss_objective_default"));
		return BukovMessages.get(
				"bukov.raid.combat.boss_objective_format",
				marker,
				objective,
				state.bossRetreatWarning()
						? BukovMessages.get(
								"bukov.raid.combat.boss_retreat_warning")
						: "");
	}

	public static String navigation(BukovRaidHudState state) {
		if (state == null || !state.navigationVisible()) return "";
		String label = safe(
				state.navigationLabel(),
				cueText(state.navigationCue()));
		String availability = state.navigationAvailable()
				? ""
				: BukovMessages.get(
						"bukov.raid.combat.navigation_unavailable");
		return BukovMessages.get(
				"bukov.raid.combat.navigation_format",
				cueShape(state.navigationCue()),
				directionShape(state.navigationDirection()),
				label,
				distanceText(state.navigationDistance()),
				availability);
	}

	public static String threat(BukovRaidHudState state) {
		if (state == null || !state.threatVisible()) return "";
		return BukovMessages.get(
				"bukov.raid.combat.threat_format",
				state.threatUrgent() ? "! " : "~ ",
				directionShape(state.threatDirection()),
				safe(
						state.threatLabel(),
						BukovMessages.get(
								"bukov.raid.combat.threat_default")),
				distanceText(state.threatDistance()));
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
		if (direction == null) {
			return BukovMessages.get(
					"bukov.raid.combat.direction_unknown");
		}
		switch (direction) {
			case N:
				return BukovMessages.get(
						"bukov.raid.combat.direction_north");
			case NE:
				return BukovMessages.get(
						"bukov.raid.combat.direction_northeast");
			case E:
				return BukovMessages.get(
						"bukov.raid.combat.direction_east");
			case SE:
				return BukovMessages.get(
						"bukov.raid.combat.direction_southeast");
			case S:
				return BukovMessages.get(
						"bukov.raid.combat.direction_south");
			case SW:
				return BukovMessages.get(
						"bukov.raid.combat.direction_southwest");
			case W:
				return BukovMessages.get(
						"bukov.raid.combat.direction_west");
			case NW:
				return BukovMessages.get(
						"bukov.raid.combat.direction_northwest");
			default:
				return BukovMessages.get(
						"bukov.raid.combat.direction_unknown");
		}
	}

	private static String distanceShape(BukovRaidHudState.Distance distance) {
		if (distance == BukovRaidHudState.Distance.NEAR) return "@";
		if (distance == BukovRaidHudState.Distance.MID) return "o";
		return ".";
	}

	private static String distanceText(BukovRaidHudState.Distance distance) {
		if (distance == BukovRaidHudState.Distance.NEAR) {
			return BukovMessages.get(
					"bukov.raid.combat.distance_near");
		}
		if (distance == BukovRaidHudState.Distance.MID) {
			return BukovMessages.get(
					"bukov.raid.combat.distance_mid");
		}
		return BukovMessages.get("bukov.raid.combat.distance_far");
	}

	private static String categoryText(SoundCategory category) {
		if (category == SoundCategory.ENEMY_GUNSHOT) {
			return BukovMessages.get(
					"bukov.raid.combat.sound_enemy_gunshot");
		}
		if (category == SoundCategory.FOOTSTEP) {
			return BukovMessages.get(
					"bukov.raid.combat.sound_footstep");
		}
		if (category == SoundCategory.BOSS_CUE) {
			return BukovMessages.get(
					"bukov.raid.combat.sound_boss_cue");
		}
		if (category == SoundCategory.EXTRACTION_CUE) {
			return BukovMessages.get(
					"bukov.raid.combat.sound_extraction_cue");
		}
		return BukovMessages.get("bukov.raid.combat.sound_critical");
	}

	private static String cueShape(BukovRaidHudState.Cue cue) {
		if (cue == BukovRaidHudState.Cue.PICKUP) return "+";
		if (cue == BukovRaidHudState.Cue.MISSION) return "*";
		if (cue == BukovRaidHudState.Cue.EXTRACTION) return "#";
		return "•";
	}

	private static String cueText(BukovRaidHudState.Cue cue) {
		if (cue == BukovRaidHudState.Cue.PICKUP) {
			return BukovMessages.get("bukov.raid.combat.cue_pickup");
		}
		if (cue == BukovRaidHudState.Cue.MISSION) {
			return BukovMessages.get("bukov.raid.combat.cue_mission");
		}
		if (cue == BukovRaidHudState.Cue.EXTRACTION) {
			return BukovMessages.get(
					"bukov.raid.combat.cue_extraction");
		}
		return BukovMessages.get("bukov.raid.combat.cue_default");
	}

	private static String safe(String value, String fallback) {
		return value == null || value.isEmpty() ? fallback : value;
	}

	private BukovCombatHudFormat() {
	}
}
