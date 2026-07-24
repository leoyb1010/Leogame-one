/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

public class Assets {

	public static class Effects {
		public static final String EFFECTS      = "effects/effects.png";
		public static final String FIREBALL     = "effects/fireball.png";
		public static final String SPECKS       = "effects/specks.png";
		public static final String SPELL_ICONS  = "effects/spell_icons.png";
		public static final String TEXT_ICONS   = "effects/text_icons.png";
	}

	public static class Environment {
		public static final String TERRAIN_FEATURES = "environment/terrain_features.png";

		public static final String VISUAL_GRID  = "environment/visual_grid.png";
		public static final String WALL_BLOCKING= "environment/wall_blocking.png";

		public static final String TILES_SEWERS = "environment/tiles_sewers.png";
		public static final String TILES_PRISON = "environment/tiles_prison.png";
		public static final String TILES_CAVES  = "environment/tiles_caves.png";
		public static final String TILES_CITY   = "environment/tiles_city.png";
		public static final String TILES_HALLS  = "environment/tiles_halls.png";
		public static final String TILES_BUKOV_FOG_DEPOT =
				"environment/bukov/tiles_fog_depot.png";
		public static final String BUKOV_FIRST_RAID_LANDMARKS =
				"environment/bukov/first_raid_landmarks.png";

		public static final String TILES_CAVES_CRYSTAL  = "environment/tiles_caves_crystal.png";
		public static final String TILES_CAVES_GNOLL    = "environment/tiles_caves_gnoll.png";

		public static final String WATER_SEWERS = "environment/water0.png";
		public static final String WATER_PRISON = "environment/water1.png";
		public static final String WATER_CAVES  = "environment/water2.png";
		public static final String WATER_CITY   = "environment/water3.png";
		public static final String WATER_HALLS  = "environment/water4.png";
		public static final String WATER_BUKOV_FOG_DEPOT =
				"environment/bukov/water_fog_depot.png";

		public static final String WEAK_FLOOR       = "environment/custom_tiles/weak_floor.png";
		public static final String SEWER_BOSS       = "environment/custom_tiles/sewer_boss.png";
		public static final String PRISON_QUEST     = "environment/custom_tiles/prison_quest.png";
		public static final String PRISON_EXIT      = "environment/custom_tiles/prison_exit.png";
		public static final String CAVES_QUEST      = "environment/custom_tiles/caves_quest.png";
		public static final String CAVES_BOSS       = "environment/custom_tiles/caves_boss.png";
		public static final String CITY_QUEST        = "environment/custom_tiles/city_quest.png";
		public static final String CITY_BOSS        = "environment/custom_tiles/city_boss.png";
		public static final String HALLS_SP         = "environment/custom_tiles/halls_special.png";
	}
	
	//TODO include other font assets here? Some are platform specific though...
	public static class Fonts {
		public static final String PIXELFONT= "fonts/pixel_font.png";
	}

	public static class Interfaces {
		public static final String ARCS_BG  = "interfaces/arcs1.png";
		public static final String ARCS_FG  = "interfaces/arcs2.png";

		public static final String BANNERS  = "interfaces/banners.png";
		public static final String BADGES   = "interfaces/badges.png";
		public static final String LOCKED   = "interfaces/locked_badge.png";

		public static final String CHROME   = "interfaces/chrome.png";
		public static final String ICONS    = "interfaces/icons.png";
		public static final String STATUS   = "interfaces/status_pane.png";
		public static final String MENU     = "interfaces/menu_pane.png";
		public static final String MENU_BTN = "interfaces/menu_button.png";
		public static final String TOOLBAR  = "interfaces/toolbar.png";
		public static final String SHADOW   = "interfaces/shadow.png";
		public static final String BOSSHP   = "interfaces/boss_hp.png";

		public static final String SURFACE  = "interfaces/surface.png";

		public static final String BUFFS_SMALL      = "interfaces/buffs.png";
		public static final String BUFFS_LARGE      = "interfaces/large_buffs.png";

		public static final String TALENT_ICONS     = "interfaces/talent_icons.png";
		public static final String TALENT_BUTTON    = "interfaces/talent_button.png";

		public static final String HERO_ICONS       = "interfaces/hero_icons.png";

		public static final String LEO_TITLE_EMBLEM = "interfaces/leo_title_emblem.png";
		public static final String LEO_MENU_PANEL   = "interfaces/leo_menu_panel.png";
		public static final String LEO_DIALOG_FRAME = "interfaces/leo_dialog_frame.png";
		public static final String LEO_BUTTON       = "interfaces/leo_button_normal.png";
		public static final String LEO_BUTTON_DOWN  = "interfaces/leo_button_pressed.png";
		public static final String BUKOV_UI         = "interfaces/bukov_ui.png";

		public static final String RADIAL_MENU      = "interfaces/radial_menu.png";
	}

	//these points to resource bundles, not raw asset files
	public static class Messages {
		public static final String ACTORS   = "messages/actors/actors";
		public static final String ITEMS    = "messages/items/items";
		public static final String JOURNAL  = "messages/journal/journal";
		public static final String LEVELS   = "messages/levels/levels";
		public static final String MISC     = "messages/misc/misc";
		public static final String PLANTS   = "messages/plants/plants";
		public static final String SCENES   = "messages/scenes/scenes";
		public static final String UI       = "messages/ui/ui";
		public static final String WINDOWS  = "messages/windows/windows";
	}

	public static class Music {
		public static final String THEME_1              = "music/theme_1.ogg";
		public static final String THEME_2              = "music/theme_2.ogg";
		public static final String THEME_FINALE         = "music/theme_finale.ogg";

		public static final String SEWERS_1             = "music/sewers_1.ogg";
		public static final String SEWERS_2             = "music/sewers_2.ogg";
		public static final String SEWERS_3             = "music/sewers_3.ogg";
		public static final String SEWERS_TENSE         = "music/sewers_tense.ogg";
		public static final String SEWERS_BOSS          = "music/sewers_boss.ogg";

		public static final String PRISON_1             = "music/prison_1.ogg";
		public static final String PRISON_2             = "music/prison_2.ogg";
		public static final String PRISON_3             = "music/prison_3.ogg";
		public static final String PRISON_TENSE         = "music/prison_tense.ogg";
		public static final String PRISON_BOSS          = "music/prison_boss.ogg";

		public static final String CAVES_1              = "music/caves_1.ogg";
		public static final String CAVES_2              = "music/caves_2.ogg";
		public static final String CAVES_3              = "music/caves_3.ogg";
		public static final String CAVES_TENSE          = "music/caves_tense.ogg";
		public static final String CAVES_BOSS           = "music/caves_boss.ogg";
		public static final String CAVES_BOSS_FINALE    = "music/caves_boss_finale.ogg";

		public static final String CITY_1               = "music/city_1.ogg";
		public static final String CITY_2               = "music/city_2.ogg";
		public static final String CITY_3               = "music/city_3.ogg";
		public static final String CITY_TENSE           = "music/city_tense.ogg";
		public static final String CITY_BOSS            = "music/city_boss.ogg";
		public static final String CITY_BOSS_FINALE     = "music/city_boss_finale.ogg";

		public static final String HALLS_1              = "music/halls_1.ogg";
		public static final String HALLS_2              = "music/halls_2.ogg";
		public static final String HALLS_3              = "music/halls_3.ogg";
		public static final String HALLS_TENSE          = "music/halls_tense.ogg";
		public static final String HALLS_BOSS           = "music/halls_boss.ogg";
		public static final String HALLS_BOSS_FINALE    = "music/halls_boss_finale.ogg";
	}

	public static class Sounds {
		public static final String CLICK    = "sounds/click.mp3";
		public static final String BADGE    = "sounds/badge.mp3";
		public static final String GOLD     = "sounds/gold.mp3";

		public static final String OPEN     = "sounds/door_open.mp3";
		public static final String UNLOCK   = "sounds/unlock.mp3";
		public static final String ITEM     = "sounds/item.mp3";
		public static final String DEWDROP  = "sounds/dewdrop.mp3";
		public static final String STEP     = "sounds/step.mp3";
		public static final String WATER    = "sounds/water.mp3";
		public static final String GRASS    = "sounds/grass.mp3";
		public static final String TRAMPLE  = "sounds/trample.mp3";
		public static final String STURDY   = "sounds/sturdy.mp3";

		public static final String HIT              = "sounds/hit.mp3";
		public static final String MISS             = "sounds/miss.mp3";
		public static final String HIT_SLASH        = "sounds/hit_slash.mp3";
		public static final String HIT_STAB         = "sounds/hit_stab.mp3";
		public static final String HIT_CRUSH        = "sounds/hit_crush.mp3";
		public static final String HIT_MAGIC        = "sounds/hit_magic.mp3";
		public static final String HIT_STRONG       = "sounds/hit_strong.mp3";
		public static final String HIT_PARRY        = "sounds/hit_parry.mp3";
		public static final String HIT_ARROW        = "sounds/hit_arrow.mp3";
		public static final String ATK_SPIRITBOW    = "sounds/atk_spiritbow.mp3";
		public static final String ATK_CROSSBOW     = "sounds/atk_crossbow.mp3";
		public static final String HEALTH_WARN      = "sounds/health_warn.mp3";
		public static final String HEALTH_CRITICAL  = "sounds/health_critical.mp3";

		public static final String DESCEND  = "sounds/descend.mp3";
		public static final String EAT      = "sounds/eat.mp3";
		public static final String READ     = "sounds/read.mp3";
		public static final String LULLABY  = "sounds/lullaby.mp3";
		public static final String DRINK    = "sounds/drink.mp3";
		public static final String SHATTER  = "sounds/shatter.mp3";
		public static final String ZAP      = "sounds/zap.mp3";
		public static final String LIGHTNING= "sounds/lightning.mp3";
		public static final String LEVELUP  = "sounds/levelup.mp3";
		public static final String DEATH    = "sounds/death.mp3";
		public static final String CHALLENGE= "sounds/challenge.mp3";
		public static final String CURSED   = "sounds/cursed.mp3";
		public static final String TRAP     = "sounds/trap.mp3";
		public static final String EVOKE    = "sounds/evoke.mp3";
		public static final String TOMB     = "sounds/tomb.mp3";
		public static final String ALERT    = "sounds/alert.mp3";
		public static final String MELD     = "sounds/meld.mp3";
		public static final String BOSS     = "sounds/boss.mp3";
		public static final String BLAST    = "sounds/blast.mp3";
		public static final String PLANT    = "sounds/plant.mp3";
		public static final String RAY      = "sounds/ray.mp3";
		public static final String BEACON   = "sounds/beacon.mp3";
		public static final String TELEPORT = "sounds/teleport.mp3";
		public static final String CHARMS   = "sounds/charms.mp3";
		public static final String MASTERY  = "sounds/mastery.mp3";
		public static final String PUFF     = "sounds/puff.mp3";
		public static final String ROCKS    = "sounds/rocks.mp3";
		public static final String BURNING  = "sounds/burning.mp3";
		public static final String FALLING  = "sounds/falling.mp3";
		public static final String GHOST    = "sounds/ghost.mp3";
		public static final String SECRET   = "sounds/secret.mp3";
		public static final String BONES    = "sounds/bones.mp3";
		public static final String BEE      = "sounds/bee.mp3";
		public static final String DEGRADE  = "sounds/degrade.mp3";
		public static final String MIMIC    = "sounds/mimic.mp3";
		public static final String DEBUFF   = "sounds/debuff.mp3";
		public static final String CHARGEUP = "sounds/chargeup.mp3";
		public static final String GAS      = "sounds/gas.mp3";
		public static final String CHAINS   = "sounds/chains.mp3";
		public static final String SCAN     = "sounds/scan.mp3";
		public static final String SHEEP    = "sounds/sheep.mp3";
		public static final String MINE    = "sounds/mine.mp3";

		public static class Bukov {
			public static final String GUNSHOT_PLAYER =
					"sounds/bukov/gunshot_player.wav";
			public static final String GUNSHOT_ENEMY =
					"sounds/bukov/gunshot_enemy.wav";
			public static final String GUNSHOT_PISTOL =
					"sounds/bukov/gunshot_pistol.wav";
			public static final String GUNSHOT_SMG =
					"sounds/bukov/gunshot_smg.wav";
			public static final String GUNSHOT_CARBINE =
					"sounds/bukov/gunshot_carbine.wav";
			public static final String GUNSHOT_RIFLE =
					"sounds/bukov/gunshot_rifle.wav";
			public static final String GUNSHOT_SHOTGUN =
					"sounds/bukov/gunshot_shotgun.wav";
			public static final String GUNSHOT_HEAVY =
					"sounds/bukov/gunshot_heavy.wav";
			public static final String[] GUNSHOT_PISTOL_MECHANICAL = {
					"sounds/bukov/gunshot_pistol_mechanical_1.wav",
					"sounds/bukov/gunshot_pistol_mechanical_2.wav",
					"sounds/bukov/gunshot_pistol_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_PISTOL_BODY = {
					"sounds/bukov/gunshot_pistol_body_1.wav",
					"sounds/bukov/gunshot_pistol_body_2.wav",
					"sounds/bukov/gunshot_pistol_body_3.wav"
			};
			public static final String[] GUNSHOT_SMG_MECHANICAL = {
					"sounds/bukov/gunshot_smg_mechanical_1.wav",
					"sounds/bukov/gunshot_smg_mechanical_2.wav",
					"sounds/bukov/gunshot_smg_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_SMG_BODY = {
					"sounds/bukov/gunshot_smg_body_1.wav",
					"sounds/bukov/gunshot_smg_body_2.wav",
					"sounds/bukov/gunshot_smg_body_3.wav"
			};
			public static final String[] GUNSHOT_CARBINE_MECHANICAL = {
					"sounds/bukov/gunshot_carbine_mechanical_1.wav",
					"sounds/bukov/gunshot_carbine_mechanical_2.wav",
					"sounds/bukov/gunshot_carbine_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_CARBINE_BODY = {
					"sounds/bukov/gunshot_carbine_body_1.wav",
					"sounds/bukov/gunshot_carbine_body_2.wav",
					"sounds/bukov/gunshot_carbine_body_3.wav"
			};
			public static final String[] GUNSHOT_RIFLE_MECHANICAL = {
					"sounds/bukov/gunshot_rifle_mechanical_1.wav",
					"sounds/bukov/gunshot_rifle_mechanical_2.wav",
					"sounds/bukov/gunshot_rifle_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_RIFLE_BODY = {
					"sounds/bukov/gunshot_rifle_body_1.wav",
					"sounds/bukov/gunshot_rifle_body_2.wav",
					"sounds/bukov/gunshot_rifle_body_3.wav"
			};
			public static final String[] GUNSHOT_SHOTGUN_MECHANICAL = {
					"sounds/bukov/gunshot_shotgun_mechanical_1.wav",
					"sounds/bukov/gunshot_shotgun_mechanical_2.wav",
					"sounds/bukov/gunshot_shotgun_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_SHOTGUN_BODY = {
					"sounds/bukov/gunshot_shotgun_body_1.wav",
					"sounds/bukov/gunshot_shotgun_body_2.wav",
					"sounds/bukov/gunshot_shotgun_body_3.wav"
			};
			public static final String[] GUNSHOT_HEAVY_MECHANICAL = {
					"sounds/bukov/gunshot_heavy_mechanical_1.wav",
					"sounds/bukov/gunshot_heavy_mechanical_2.wav",
					"sounds/bukov/gunshot_heavy_mechanical_3.wav"
			};
			public static final String[] GUNSHOT_HEAVY_BODY = {
					"sounds/bukov/gunshot_heavy_body_1.wav",
					"sounds/bukov/gunshot_heavy_body_2.wav",
					"sounds/bukov/gunshot_heavy_body_3.wav"
			};
			public static final String[] GUNSHOT_TAIL_INDOOR = {
					"sounds/bukov/gunshot_tail_indoor_1.wav",
					"sounds/bukov/gunshot_tail_indoor_2.wav",
					"sounds/bukov/gunshot_tail_indoor_3.wav"
			};
			public static final String[] GUNSHOT_TAIL_CORRIDOR = {
					"sounds/bukov/gunshot_tail_corridor_1.wav",
					"sounds/bukov/gunshot_tail_corridor_2.wav",
					"sounds/bukov/gunshot_tail_corridor_3.wav"
			};
			public static final String[] GUNSHOT_TAIL_OPEN = {
					"sounds/bukov/gunshot_tail_open_1.wav",
					"sounds/bukov/gunshot_tail_open_2.wav",
					"sounds/bukov/gunshot_tail_open_3.wav"
			};
			public static final String BULLET_HIT =
					"sounds/bukov/bullet_hit.wav";
			public static final String CONTACT_HIT =
					"sounds/bukov/contact_hit.wav";
			public static final String DRY_FIRE =
					"sounds/bukov/dry_fire.wav";
			public static final String RELOAD_START =
					"sounds/bukov/reload_start.wav";
			public static final String RELOAD_FINISH =
					"sounds/bukov/reload_finish.wav";
			public static final String RELOAD_MAG_OUT =
					"sounds/bukov/reload_mag_out.wav";
			public static final String RELOAD_MAG_IN =
					"sounds/bukov/reload_mag_in.wav";
			public static final String RELOAD_CHARGE =
					"sounds/bukov/reload_charge.wav";
			public static final String LOOT_PICKUP =
					"sounds/bukov/loot_pickup.wav";
			public static final String SEARCH_COMPLETE =
					"sounds/bukov/search_complete.wav";
			public static final String GATE_UNLOCK =
					"sounds/bukov/gate_unlock.wav";
			public static final String EXTRACTION_START =
					"sounds/bukov/extraction_start.wav";
			public static final String EXTRACTION_COMPLETE =
					"sounds/bukov/extraction_complete.wav";
			public static final String UI_FOCUS =
					"sounds/bukov/ui_focus.wav";
			public static final String UI_CONFIRM =
					"sounds/bukov/ui_confirm.wav";
			public static final String UI_CANCEL =
					"sounds/bukov/ui_cancel.wav";
			public static final String UI_ERROR =
					"sounds/bukov/ui_error.wav";
			public static final String AMBIENCE_CALM =
					"sounds/bukov/ambience_calm.wav";
			public static final String AMBIENCE_TENSE =
					"sounds/bukov/ambience_tense.wav";
			public static final String AMBIENCE_COMBAT =
					"sounds/bukov/ambience_combat.wav";
			public static final String[] FOOTSTEP_HARD = {
					"sounds/bukov/footstep_hard_1.wav",
					"sounds/bukov/footstep_hard_2.wav"
			};
			public static final String[] FOOTSTEP_WATER = {
					"sounds/bukov/footstep_water_1.wav",
					"sounds/bukov/footstep_water_2.wav"
			};
			public static final String[] FOOTSTEP_METAL = {
					"sounds/bukov/footstep_metal_1.wav",
					"sounds/bukov/footstep_metal_2.wav"
			};

			private Bukov() {
			}
		}

		public static final String[] all = new String[]{
				CLICK, BADGE, GOLD,

				OPEN, UNLOCK, ITEM, DEWDROP, STEP, WATER, GRASS, TRAMPLE, STURDY,

				HIT, MISS, HIT_SLASH, HIT_STAB, HIT_CRUSH, HIT_MAGIC, HIT_STRONG, HIT_PARRY,
				HIT_ARROW, ATK_SPIRITBOW, ATK_CROSSBOW, HEALTH_WARN, HEALTH_CRITICAL,

				DESCEND, EAT, READ, LULLABY, DRINK, SHATTER, ZAP, LIGHTNING, LEVELUP, DEATH,
				CHALLENGE, CURSED, TRAP, EVOKE, TOMB, ALERT, MELD, BOSS, BLAST, PLANT, RAY, BEACON,
				TELEPORT, CHARMS, MASTERY, PUFF, ROCKS, BURNING, FALLING, GHOST, SECRET, BONES,
				BEE, DEGRADE, MIMIC, DEBUFF, CHARGEUP, GAS, CHAINS, SCAN, SHEEP, MINE,

				Bukov.GUNSHOT_PLAYER, Bukov.GUNSHOT_ENEMY,
				Bukov.GUNSHOT_PISTOL, Bukov.GUNSHOT_SMG,
				Bukov.GUNSHOT_CARBINE, Bukov.GUNSHOT_RIFLE,
				Bukov.GUNSHOT_SHOTGUN, Bukov.GUNSHOT_HEAVY,
				Bukov.GUNSHOT_PISTOL_MECHANICAL[0],
				Bukov.GUNSHOT_PISTOL_MECHANICAL[1],
				Bukov.GUNSHOT_PISTOL_MECHANICAL[2],
				Bukov.GUNSHOT_PISTOL_BODY[0],
				Bukov.GUNSHOT_PISTOL_BODY[1],
				Bukov.GUNSHOT_PISTOL_BODY[2],
				Bukov.GUNSHOT_SMG_MECHANICAL[0],
				Bukov.GUNSHOT_SMG_MECHANICAL[1],
				Bukov.GUNSHOT_SMG_MECHANICAL[2],
				Bukov.GUNSHOT_SMG_BODY[0],
				Bukov.GUNSHOT_SMG_BODY[1],
				Bukov.GUNSHOT_SMG_BODY[2],
				Bukov.GUNSHOT_CARBINE_MECHANICAL[0],
				Bukov.GUNSHOT_CARBINE_MECHANICAL[1],
				Bukov.GUNSHOT_CARBINE_MECHANICAL[2],
				Bukov.GUNSHOT_CARBINE_BODY[0],
				Bukov.GUNSHOT_CARBINE_BODY[1],
				Bukov.GUNSHOT_CARBINE_BODY[2],
				Bukov.GUNSHOT_RIFLE_MECHANICAL[0],
				Bukov.GUNSHOT_RIFLE_MECHANICAL[1],
				Bukov.GUNSHOT_RIFLE_MECHANICAL[2],
				Bukov.GUNSHOT_RIFLE_BODY[0],
				Bukov.GUNSHOT_RIFLE_BODY[1],
				Bukov.GUNSHOT_RIFLE_BODY[2],
				Bukov.GUNSHOT_SHOTGUN_MECHANICAL[0],
				Bukov.GUNSHOT_SHOTGUN_MECHANICAL[1],
				Bukov.GUNSHOT_SHOTGUN_MECHANICAL[2],
				Bukov.GUNSHOT_SHOTGUN_BODY[0],
				Bukov.GUNSHOT_SHOTGUN_BODY[1],
				Bukov.GUNSHOT_SHOTGUN_BODY[2],
				Bukov.GUNSHOT_HEAVY_MECHANICAL[0],
				Bukov.GUNSHOT_HEAVY_MECHANICAL[1],
				Bukov.GUNSHOT_HEAVY_MECHANICAL[2],
				Bukov.GUNSHOT_HEAVY_BODY[0],
				Bukov.GUNSHOT_HEAVY_BODY[1],
				Bukov.GUNSHOT_HEAVY_BODY[2],
				Bukov.GUNSHOT_TAIL_INDOOR[0],
				Bukov.GUNSHOT_TAIL_INDOOR[1],
				Bukov.GUNSHOT_TAIL_INDOOR[2],
				Bukov.GUNSHOT_TAIL_CORRIDOR[0],
				Bukov.GUNSHOT_TAIL_CORRIDOR[1],
				Bukov.GUNSHOT_TAIL_CORRIDOR[2],
				Bukov.GUNSHOT_TAIL_OPEN[0],
				Bukov.GUNSHOT_TAIL_OPEN[1],
				Bukov.GUNSHOT_TAIL_OPEN[2],
				Bukov.BULLET_HIT,
				Bukov.CONTACT_HIT,
				Bukov.DRY_FIRE, Bukov.RELOAD_START, Bukov.RELOAD_FINISH,
				Bukov.RELOAD_MAG_OUT, Bukov.RELOAD_MAG_IN,
				Bukov.RELOAD_CHARGE,
				Bukov.LOOT_PICKUP, Bukov.SEARCH_COMPLETE, Bukov.GATE_UNLOCK,
				Bukov.EXTRACTION_START, Bukov.EXTRACTION_COMPLETE,
				Bukov.UI_FOCUS, Bukov.UI_CONFIRM, Bukov.UI_CANCEL,
				Bukov.UI_ERROR,
				Bukov.FOOTSTEP_HARD[0], Bukov.FOOTSTEP_HARD[1],
				Bukov.FOOTSTEP_WATER[0], Bukov.FOOTSTEP_WATER[1],
				Bukov.FOOTSTEP_METAL[0], Bukov.FOOTSTEP_METAL[1]
		};
	}

	public static class Splashes {
		public static final String WARRIOR  = "splashes/warrior.jpg";
		public static final String MAGE     = "splashes/mage.jpg";
		public static final String ROGUE    = "splashes/rogue.jpg";
		public static final String HUNTRESS = "splashes/huntress.jpg";
		public static final String DUELIST  = "splashes/duelist.jpg";
		public static final String CLERIC   = "splashes/cleric.jpg";

		public static final String SEWERS   = "splashes/sewers.jpg";
		public static final String PRISON   = "splashes/prison.jpg";
		public static final String CAVES    = "splashes/caves.jpg";
		public static final String CITY     = "splashes/city.jpg";
		public static final String HALLS    = "splashes/halls.jpg";

		public static class Bukov {
			public static final String FIRST_RAID =
					"splashes/bukov/first_raid_portrait.png";
			public static final String TITLE_LANDSCAPE =
					"splashes/bukov/title_landscape.png";
			public static final String TITLE_PORTRAIT =
					"splashes/bukov/title_portrait.png";
			public static final String TITLE_INDUSTRIAL_LANDSCAPE_V2 =
					"splashes/bukov/title_industrial_landscape_v2.png";
			public static final String TITLE_INDUSTRIAL_PORTRAIT_V2 =
					"splashes/bukov/title_industrial_portrait_v2.png";
		}

		public static class Title {
			public static final String LEO_LANDSCAPE = "splashes/title/leo_landscape.jpg";
			public static final String LEO_PORTRAIT  = "splashes/title/leo_portrait.jpg";

			public static final String ARCHS         = "splashes/title/archs.png";
			public static final String BACK_CLUSTERS = "splashes/title/back_clusters.png";
			public static final String MID_MIXED     = "splashes/title/mid_mixed.png";
			public static final String FRONT_SMALL   = "splashes/title/front_small.png";
		}
	}

	public static class Sprites {
		public static final String ITEMS        = "sprites/items.png";
		public static final String ITEM_ICONS   = "sprites/item_icons.png";

		public static final String WARRIOR  = "sprites/warrior.png";
		public static final String MAGE     = "sprites/mage.png";
		public static final String ROGUE    = "sprites/rogue.png";
		public static final String BUKOV_OPERATOR = "sprites/bukov_operator.png";
		public static final String BUKOV_OPERATOR_LOWER =
				"sprites/bukov_operator_lower.png";
		public static final String BUKOV_OPERATOR_UPPER =
				"sprites/bukov_operator_upper.png";
		public static final String BUKOV_SCAVENGER = "sprites/bukov/scavenger.png";
		public static final String BUKOV_GUNNER    = "sprites/bukov/gunner.png";
		public static final String BUKOV_ARMORED   = "sprites/bukov/armored.png";
		public static final String BUKOV_CAPTAIN   = "sprites/bukov/captain.png";
		public static final String BUKOV_DRONE     = "sprites/bukov/drone.png";
		public static final String BUKOV_WHITE_LINE= "sprites/bukov/white_line.png";
		public static final String BUKOV_ALLEY_SCOUT =
				"sprites/bukov/alley_scout.png";
		public static final String BUKOV_DEPOT_SHOTGUNNER =
				"sprites/bukov/depot_shotgunner.png";
		public static final String BUKOV_LINE_RIFLEMAN =
				"sprites/bukov/line_rifleman.png";
		public static final String BUKOV_FOG_STALKER =
				"sprites/bukov/fog_stalker.png";
		public static final String BUKOV_SIGNAL_OPERATOR =
				"sprites/bukov/signal_operator.png";
		public static final String BUKOV_IRON_CLASP_MARKSMAN =
				"sprites/bukov/iron_clasp_marksman.png";
		public static final String BUKOV_BREACH_VETERAN =
				"sprites/bukov/breach_veteran.png";
		public static final String HUNTRESS = "sprites/huntress.png";
		public static final String DUELIST  = "sprites/duelist.png";
		public static final String CLERIC   = "sprites/cleric.png";
		public static final String AVATARS  = "sprites/avatars.png";
		public static final String PET      = "sprites/pet.png";
		public static final String AMULET   = "sprites/amulet.png";

		public static final String RAT      = "sprites/rat.png";
		public static final String BRUTE    = "sprites/brute.png";
		public static final String SPINNER  = "sprites/spinner.png";
		public static final String DM300    = "sprites/dm300.png";
		public static final String WRAITH   = "sprites/wraith.png";
		public static final String UNDEAD   = "sprites/undead.png";
		public static final String KING     = "sprites/king.png";
		public static final String PIRANHA  = "sprites/piranha.png";
		public static final String EYE      = "sprites/eye.png";
		public static final String GNOLL    = "sprites/gnoll.png";
		public static final String CRAB     = "sprites/crab.png";
		public static final String GOO      = "sprites/goo.png";
		public static final String SWARM    = "sprites/swarm.png";
		public static final String SKELETON = "sprites/skeleton.png";
		public static final String SHAMAN   = "sprites/shaman.png";
		public static final String THIEF    = "sprites/thief.png";
		public static final String TENGU    = "sprites/tengu.png";
		public static final String SHEEP    = "sprites/sheep.png";
		public static final String KEEPER   = "sprites/shopkeeper.png";
		public static final String BAT      = "sprites/bat.png";
		public static final String ELEMENTAL= "sprites/elemental.png";
		public static final String MONK     = "sprites/monk.png";
		public static final String WARLOCK  = "sprites/warlock.png";
		public static final String GOLEM    = "sprites/golem.png";
		public static final String STATUE   = "sprites/statue.png";
		public static final String SUCCUBUS = "sprites/succubus.png";
		public static final String SCORPIO  = "sprites/scorpio.png";
		public static final String FISTS    = "sprites/yog_fists.png";
		public static final String YOG      = "sprites/yog.png";
		public static final String LARVA    = "sprites/larva.png";
		public static final String GHOST    = "sprites/ghost.png";
		public static final String MAKER    = "sprites/wandmaker.png";
		public static final String TROLL    = "sprites/blacksmith.png";
		public static final String IMP      = "sprites/demon.png";
		public static final String RATKING  = "sprites/ratking.png";
		public static final String BEE      = "sprites/bee.png";
		public static final String MIMIC    = "sprites/mimic.png";
		public static final String ROT_LASH = "sprites/rot_lasher.png";
		public static final String ROT_HEART= "sprites/rot_heart.png";
		public static final String GUARD    = "sprites/guard.png";
		public static final String WARDS    = "sprites/wards.png";
		public static final String GUARDIAN = "sprites/guardian.png";
		public static final String SLIME    = "sprites/slime.png";
		public static final String SNAKE    = "sprites/snake.png";
		public static final String NECRO    = "sprites/necromancer.png";
		public static final String GHOUL    = "sprites/ghoul.png";
		public static final String RIPPER   = "sprites/ripper.png";
		public static final String SPAWNER  = "sprites/spawner.png";
		public static final String DM100    = "sprites/dm100.png";
		public static final String PYLON    = "sprites/pylon.png";
		public static final String DM200    = "sprites/dm200.png";
		public static final String LOTUS    = "sprites/lotus.png";
		public static final String NINJA_LOG        = "sprites/ninja_log.png";
		public static final String SPIRIT_HAWK      = "sprites/spirit_hawk.png";
		public static final String RED_SENTRY       = "sprites/red_sentry.png";
		public static final String CRYSTAL_WISP     = "sprites/crystal_wisp.png";
		public static final String CRYSTAL_GUARDIAN = "sprites/crystal_guardian.png";
		public static final String CRYSTAL_SPIRE    = "sprites/crystal_spire.png";
		public static final String GNOLL_GUARD      = "sprites/gnoll_guard.png";
		public static final String GNOLL_SAPPER     = "sprites/gnoll_sapper.png";
		public static final String GNOLL_GEOMANCER  = "sprites/gnoll_geomancer.png";
		public static final String FUNGAL_SPINNER   = "sprites/fungal_spinner.png";
		public static final String FUNGAL_SENTRY    = "sprites/fungal_sentry.png";
		public static final String FUNGAL_CORE      = "sprites/fungal_core.png";
	}
}
