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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DemonSpawner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovOperator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmospherePlayer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereSignal;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAudioBusMix;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovUiSoundPlayer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeMedicalSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.BukovImpactFx;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.BukovMuzzleFx;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.BukovShellFx;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.BukovTracerFx;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.BukovCombatPresentation;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ExtractionDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContainerDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovGearRules;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRuntimeLoadoutAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.BukovRealtimeWorld;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.BukovRaidPersistence;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.BukovViewport;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeRaidSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveServices;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovExperienceSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.BukovPerformancePolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHudFormat;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovBackpackViewModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovHubController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovPauseButton;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHud;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchControls;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovBackpack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovPause;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.WndBukovSettlement;
import com.shatteredpixel.shatteredpixeldungeon.effects.BannerSprites;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.EmoIcon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Ripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.journal.Guidebook;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.InventoryScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.DimensionalSundial;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DiscardedItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovFirstRaidLandmarks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTerrainTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.FogOfWar;
import com.shatteredpixel.shatteredpixeldungeon.tiles.GridTileMap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.RaisedTerrainTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.WallBlockingTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Banner;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.CharHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.GameLog;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.LootIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.MenuPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.ResumeIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.RightClickMenu;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Tag;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Toast;
import com.shatteredpixel.shatteredpixeldungeon.ui.Toolbar;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndGame;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoCell;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoMob;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoPlant;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoTrap;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndResurrect;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUpgrade;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.input.ControllerHandler;
import com.watabou.input.KeyBindings;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GameScene extends PixelScene {

	static GameScene scene;

	private SkinnedBlock water;
	private DungeonTerrainTilemap tiles;
	private GridTileMap visualGrid;
	private TerrainFeaturesTilemap terrainFeatures;
	private RaisedTerrainTilemap raisedTerrain;
	private DungeonWallsTilemap walls;
	private WallBlockingTilemap wallBlocking;
	private FogOfWar fog;
	private HeroSprite hero;

	private MenuPane menu;
	private StatusPane status;

	private BossHealthBar boss;

	private GameLog log;

	private static CellSelector cellSelector;
	
	private Group terrain;
	private Group customTiles;
	private Group levelVisuals;
	private Group levelWallVisuals;
	private Group customWalls;
	private Group ripples;
	private Group plants;
	private Group traps;
	private Group heaps;
	private Group mobs;
	private Group floorEmitters;
	private Group emitters;
	private Group effects;
	private Group gases;
	private Group spells;
	private Group statuses;
	private Group emoicons;
	private Group overFogEffects;
	private Group healthIndicators;

	private InventoryPane inventory;
	private static boolean invVisible = true;

	private Toolbar toolbar;
	private Toast prompt;

	private AttackIndicator attack;
	private LootIndicator loot;
	private ActionIndicator action;
	private ResumeIndicator resume;
	private static final float BUKOV_CHECKPOINT_SECONDS = 5f;
	private RealtimeRaidSystem bukovRealtime;
	private BukovRealtimeWorld bukovWorld;
	private BukovRaidCoordinator bukovRaid;
	private BukovRuntimeLoadoutAdapter.RuntimeLoadout bukovRuntimeLoadout;
	private boolean bukovEmergencyLoadoutRecovered;
	private BukovSaveService bukovSaves;
	private BukovRaidHud bukovHud;
	private BukovPauseButton bukovPause;
	private BukovTouchControls bukovTouchControls;
	private WndBukovBackpack bukovBackpack;
	private BukovCombatPresentation bukovCombatPresentation;
	private BukovExperienceSettings bukovAudioDefaults;
	private BukovAudioBusMix bukovAudioMix;
	private BukovUiSoundPlayer bukovUiSounds;
	private BukovAtmosphereController bukovAtmosphere;
	private BukovAtmosphereSignal bukovAtmosphereSignal;
	private BukovAtmospherePlayer bukovAtmospherePlayer;
	private final CombatFxEvent.Consumer bukovFxConsumer =
			this::consumeBukovCombatFx;
	private float nextBukovCheckpoint;
	private float bukovLifecycleRetryDelay;
	private boolean bukovSettlementTransition;

	{
		inGameScene = true;
	}

	@Override
	public void create() {
		
		if (Dungeon.hero == null || Dungeon.level == null){
			ShatteredPixelDungeon.switchNoFade(TitleScene.class);
			return;
		}

		// Static product-mode state is not part of the host save. Re-establish
		// it from the reserved slot/level before super.create() constructs the
		// HeroSprite or any classic interlevel log can run.
		if (BukovMode.ensureActiveForHostState()) {
			BukovOperator.normalize(Dungeon.hero);
			InterlevelScene.mode = InterlevelScene.Mode.NONE;
		}

		if (BukovMode.active()) {
			// The host level music carries the old dungeon identity. Bukov
			// owns a deterministic three-state ambience mix instead.
			Music.INSTANCE.end();
		} else {
			Dungeon.level.playLevelMusic();
		}

		SPDSettings.lastClass(Dungeon.hero.heroClass.ordinal());
		
		super.create();
		Camera.main.zoom(GameMath.gate(
				minZoom,
				defaultZoom + SPDSettings.zoom(),
				maxZoom));
		// The right stick is a realtime aiming input in Bukov. Letting the
		// legacy controller cursor edge-scroll at the same time fights the
		// action camera and can leave the raid apparently stuck on one screen.
		Camera.main.edgeScroll.set(BukovMode.active() ? 0 : 1);

		switch (SPDSettings.cameraFollow()) {
			case 4: default:    Camera.main.setFollowDeadzone(0);      break;
			case 3:             Camera.main.setFollowDeadzone(0.2f);   break;
			case 2:             Camera.main.setFollowDeadzone(0.5f);   break;
			case 1:             Camera.main.setFollowDeadzone(0.9f);   break;
		}

		RectF insets = getCommonInsets();
		//we want to check if large is the same as blocking here
		float largeInsetTop = Game.platform.getSafeInsets(PlatformSupport.INSET_LRG).scale(1f/defaultZoom).top;

		scene = this;

		terrain = new Group();
		add( terrain );

		water = new SkinnedBlock(
			Dungeon.level.width() * DungeonTilemap.SIZE,
			Dungeon.level.height() * DungeonTilemap.SIZE,
			Dungeon.level.waterTex() ){

			@Override
			protected NoosaScript script() {
				return NoosaScriptNoLighting.get();
			}

			@Override
			public void draw() {
				//water has no alpha component, this improves performance
				Blending.disable();
				super.draw();
				Blending.enable();
			}
		};
		water.autoAdjust = true;
		terrain.add( water );

		ripples = new Group();
		terrain.add( ripples );

		DungeonTileSheet.setupVariance(Dungeon.level.map.length, Dungeon.seedCurDepth());
		
		tiles = new DungeonTerrainTilemap();
		terrain.add( tiles );

		customTiles = new Group();
		terrain.add(customTiles);

		for( CustomTilemap visual : Dungeon.level.customTiles){
			addCustomTile(visual);
		}

		visualGrid = new GridTileMap();
		terrain.add( visualGrid );

		terrainFeatures = new TerrainFeaturesTilemap(Dungeon.level.plants, Dungeon.level.traps);
		terrain.add(terrainFeatures);
		
		levelVisuals = Dungeon.level.addVisuals();
		add(levelVisuals);

		// Dedicated first-raid landmarks are a visual overlay only. The source
		// cells remain owned by BukovLevel/RealtimeWorld, so rendering archive,
		// gate, pump, extraction and cover art cannot mutate task topology.
		if (BukovMode.active() && Dungeon.level instanceof BukovLevel) {
			add(new BukovFirstRaidLandmarks((BukovLevel) Dungeon.level));
		}

		floorEmitters = new Group();
		add(floorEmitters);

		heaps = new Group();
		add( heaps );
		
		for ( Heap heap : Dungeon.level.heaps.valueList() ) {
			addHeapSprite( heap );
		}

		emitters = new Group();
		effects = new Group();
		healthIndicators = new Group();
		emoicons = new Group();
		overFogEffects = new Group();
		
		mobs = new Group();
		add( mobs );

		hero = new HeroSprite();
		hero.place( Dungeon.hero.pos );
		hero.updateArmor();
		mobs.add( hero );
		
		for (Mob mob : Dungeon.level.mobs) {
			addMobSprite( mob );
		}
		
		raisedTerrain = new RaisedTerrainTilemap();
		add( raisedTerrain );

		walls = new DungeonWallsTilemap();
		add(walls);

		customWalls = new Group();
		add(customWalls);

		for( CustomTilemap visual : Dungeon.level.customWalls){
			addCustomWall(visual);
		}

		levelWallVisuals = Dungeon.level.addWallVisuals();
		add( levelWallVisuals );

		wallBlocking = new WallBlockingTilemap();
		add (wallBlocking);

		add( emitters );
		add( effects );

		gases = new Group();
		add( gases );

		for (Blob blob : Dungeon.level.blobs.values()) {
			blob.emitter = null;
			addBlobSprite( blob );
		}


		fog = new FogOfWar(
				Dungeon.level.width(),
				Dungeon.level.height(),
				BukovMode.active());
		add( fog );

		spells = new Group();
		add( spells );

		add(overFogEffects);
		
		statuses = new Group();
		add( statuses );
		
		add( healthIndicators );
		//always appears ontop of other health indicators
		add( new TargetHealthIndicator() );
		
		add( emoicons );
		
		add( cellSelector = new CellSelector( tiles ) );

		int uiSize = SPDSettings.interfaceSize();
		float screentop = largeInsetTop;
		if (screentop == 0 && uiSize == 0){
			screentop--; //on mobile UI, if we render in fullscreen, clip the top 1px;
		}

		/*
		 * The Bukov runtime owns its UI from this boundary onward. Do not even
		 * instantiate classic dungeon controls: hidden legacy components still
		 * register input handlers and can leak the backpack/hero/pause flow.
		 */
		if (!BukovMode.active()) {
			//display cutouts can obstruct various UI elements, so we need to adjust for that sometimes
			float heroPaneExtraWidth = insets.left;
			float menuBarMaxLeft = uiCamera.width-insets.right-MenuPane.WIDTH;
			int hpBarMaxWidth = 50; //default max width
			float[] buffBarRowLimits = new float[9];
			float[] buffBarRowAdjusts = new float[9];

			if (largeInsetTop == 0 && insets.top > 0){
				//smaller non-notch cutouts are of varying size and may obstruct various UI elements
				// some are small hole punches, some are huge dynamic islands
				RectF cutout = Game.platform.getDisplayCutout().scale(1f / defaultZoom);
				//if the cutout is positioned to obstruct the hero portrait in the status pane
				if (cutout.top < 30
						&& cutout.left < 20
						&& cutout.right > 12) {
					heroPaneExtraWidth = Math.max(heroPaneExtraWidth, cutout.right-12);
					//make sure we have space to actually move it though
					heroPaneExtraWidth = Math.min(heroPaneExtraWidth, uiCamera.width - PixelScene.MIN_WIDTH_P);
				}
				//if the cutout is positioned to obstruct the menu bar
				else if (cutout.top < 20
						&& cutout.left < menuBarMaxLeft + MenuPane.WIDTH
						&& cutout.right > menuBarMaxLeft) {
					menuBarMaxLeft = Math.min(menuBarMaxLeft, cutout.left - MenuPane.WIDTH);
					//make sure we have space to actually move it though
					menuBarMaxLeft = Math.max(menuBarMaxLeft, PixelScene.MIN_WIDTH_P-MenuPane.WIDTH);
				}
				//if the cutout is positioned to obstruct the HP bar
				else if (cutout.left < 78
						&& cutout.top < 4
						&& cutout.right > 32) {
					//subtract starting position, but add a bit back due to end of bar
					hpBarMaxWidth = Math.round(cutout.left - 32 + 4);
					hpBarMaxWidth = Math.max(hpBarMaxWidth, 21); //cannot go below 21 (30 effective)
				}
				//if the cutout is positioned to obstruct the buff bar
				if (cutout.left < 84
						&& cutout.top < 10
						&& cutout.right > 32
						&& cutout.bottom > 11) {
					int i = 1;
					int rowTop = 11;
					//in most cases this just obstructs one row, but dynamic island can block more =S
					while (cutout.bottom > rowTop){
						if (i == 1 || cutout.bottom > rowTop+2 ) { //always shorten first row
							//subtract starting position, add a bit back to allow slight overlap
							buffBarRowLimits[i] = cutout.left - 32 + 3;
						} else {
							//if row is only slightly cut off, lower it instead of limiting width
							buffBarRowAdjusts[i] = cutout.bottom - rowTop + 1;
							rowTop += buffBarRowAdjusts[i];
						}
						i++;
						rowTop += 8;
					}
				}
			}

			menu = new MenuPane();
			menu.camera = uiCamera;
			menu.setPos( menuBarMaxLeft, screentop);
			add(menu);

			float extraRight = uiCamera.width - (menuBarMaxLeft + MenuPane.WIDTH);
			if (extraRight > 0){
				SkinnedBlock bar = new SkinnedBlock(extraRight, 20, TextureCache.createSolid(0x88000000));
				bar.x = uiCamera.width - extraRight;
				bar.camera = uiCamera;
				add(bar);

				PointerArea blocker = new PointerArea(uiCamera.width - extraRight, 0, extraRight, 20);
				blocker.camera = uiCamera;
				add(blocker);
			}

			status = new StatusPane( SPDSettings.interfaceSize() > 0 );
			status.camera = uiCamera;
			StatusPane.heroPaneExtraWidth = heroPaneExtraWidth;
			StatusPane.hpBarMaxWidth = hpBarMaxWidth;
			StatusPane.buffBarRowMaxWidths = buffBarRowLimits;
			StatusPane.buffBarRowAdjusts = buffBarRowAdjusts;
			status.setRect(insets.left, uiSize > 0 ? uiCamera.height-39-insets.bottom : screentop, uiCamera.width - insets.left - insets.right, 0 );
			add(status);

			if (uiSize < 2 && largeInsetTop != 0) {
				SkinnedBlock bar = new SkinnedBlock(uiCamera.width, largeInsetTop, TextureCache.createSolid(0x88000000));
				bar.camera = uiCamera;
				add(bar);

				PointerArea blocker = new PointerArea(0, 0, uiCamera.width, largeInsetTop);
				blocker.camera = uiCamera;
				add(blocker);
			}

			boss = new BossHealthBar();
			boss.camera = uiCamera;
			boss.setPos( (uiCamera.width - boss.width())/2, screentop + (landscape() ? 7 : 26));
			if (buffBarRowLimits[2] != 0){
				//if we potentially have a 3rd buff bar row, lower by 7px
				boss.setPos(boss.left(), boss.top() + 7);
			} else if (buffBarRowAdjusts[2] != 0){
				//
				boss.setPos(boss.left(), boss.top() + buffBarRowAdjusts[2]);
			}
			add(boss);

			resume = new ResumeIndicator();
			resume.camera = uiCamera;
			add( resume );

			action = new ActionIndicator();
			action.camera = uiCamera;
			add( action );

			loot = new LootIndicator();
			loot.camera = uiCamera;
			add( loot );

			attack = new AttackIndicator();
			attack.camera = uiCamera;
			add( attack );

			log = new GameLog();
			log.camera = uiCamera;
			log.newLine();
			add( log );

			if (uiSize > 0){
				bringToFront(status);
			}

			toolbar = new Toolbar();
			toolbar.camera = uiCamera;
			add( toolbar );

			if (uiSize == 2) {
				inventory = new InventoryPane();
				inventory.camera = uiCamera;
				inventory.setPos(uiCamera.width - inventory.width() - insets.right, uiCamera.height - inventory.height() - insets.bottom);
				add(inventory);

				toolbar.setRect( insets.left, uiCamera.height - toolbar.height() - inventory.height() - insets.bottom, uiCamera.width - insets.right, toolbar.height() );
			} else {
				toolbar.setRect( insets.left, uiCamera.height - toolbar.height() - insets.bottom, uiCamera.width - insets.right, toolbar.height() );
			}

			if (insets.bottom > 0){
				SkinnedBlock bar = new SkinnedBlock(uiCamera.width, insets.bottom, TextureCache.createSolid(0x88000000));
				bar.camera = uiCamera;
				bar.y = uiCamera.height - insets.bottom;
				add(bar);

				PointerArea blocker = new PointerArea(0, uiCamera.height - insets.bottom, uiCamera.width, insets.bottom);
				blocker.camera = uiCamera;
				add(blocker);
			}

			layoutTags();
		}

		// Bukov enters directly from BukovDeploymentScene. Any stale classic
		// interlevel mode belongs to another save and must not trigger dungeon
		// teleport/descend VFX, badges, story logs or camera pans.
		if (BukovMode.active()) {
			InterlevelScene.mode = InterlevelScene.Mode.NONE;
		} else switch (InterlevelScene.mode) {
			case RESURRECT:
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				ScrollOfTeleportation.appearVFX( Dungeon.hero );
				SpellSprite.show(Dungeon.hero, SpellSprite.ANKH);
				new Flare( 5, 16 ).color( 0xFFFF00, true ).show( hero, 4f ) ;
				break;
			case RETURN:
				if (Dungeon.level.pit[Dungeon.hero.pos] && !Dungeon.hero.flying){
					//delay this so falling into the chasm processes properly
					ShatteredPixelDungeon.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							ScrollOfTeleportation.appearVFX(Dungeon.hero);
						}
					});
				} else {
					ScrollOfTeleportation.appearVFX(Dungeon.hero);
				}
				break;
			case DESCEND:
			case FALL:
				if (Dungeon.hero.isAlive()) {
					Badges.validateNoKilling();
				}
				break;
		}

		ArrayList<Item> dropped = BukovMode.active()
				? null : Dungeon.droppedItems.get( Dungeon.depth );
		if (dropped != null) {
			for (Item item : dropped) {
				int pos = Dungeon.level.randomRespawnCell( null );
				if (pos == -1) pos = Dungeon.level.entrance();
				if (item instanceof Potion) {
					((Potion) item).shatter(pos);
				} else if (item instanceof Plant.Seed && !Dungeon.isChallenged(Challenges.NO_HERBALISM)) {
					Dungeon.level.plant((Plant.Seed) item, pos);
				} else if (item instanceof Honeypot) {
					Dungeon.level.drop(((Honeypot) item).shatter(null, pos), pos);
				} else {
					Dungeon.level.drop(item, pos);
				}
			}
			Dungeon.droppedItems.remove( Dungeon.depth );
		}

		Dungeon.hero.next();

		if (BukovMode.active()) {
			Camera.main.snapTo(hero.center().x, hero.center().y);
		} else {
			switch (InterlevelScene.mode){
				case FALL: case DESCEND: case CONTINUE:
					Camera.main.snapTo(hero.center().x, hero.center().y - DungeonTilemap.SIZE * (defaultZoom/Camera.main.zoom));
					break;
				case ASCEND:
					Camera.main.snapTo(hero.center().x, hero.center().y + DungeonTilemap.SIZE * (defaultZoom/Camera.main.zoom));
					break;
				default:
					Camera.main.snapTo(hero.center().x, hero.center().y);
			}
			Camera.main.panTo(hero.center(), 2.5f);
		}

		if (InterlevelScene.mode != InterlevelScene.Mode.NONE) {
			if (BukovMode.active()) {
				GLog.h(Messages.get(this,
						InterlevelScene.mode == InterlevelScene.Mode.CONTINUE
								? "bukov_resume"
								: "bukov_enter"));
				InterlevelScene.mode = InterlevelScene.Mode.NONE;
			} else {
				if (Dungeon.depth == Statistics.deepestFloor
					&& (InterlevelScene.mode == InterlevelScene.Mode.DESCEND || InterlevelScene.mode == InterlevelScene.Mode.FALL)) {
				GLog.h(Messages.get(this, "descend"), Dungeon.depth);
				Sample.INSTANCE.play(Assets.Sounds.DESCEND);
				
				for (Char ch : Actor.chars()){
					if (ch instanceof DriedRose.GhostHero){
						((DriedRose.GhostHero) ch).sayAppeared();
					}
				}

				int spawnersAbove = Statistics.spawnersAlive;
				if (spawnersAbove > 0 && Dungeon.depth <= 25) {
					for (Mob m : Dungeon.level.mobs) {
						if (m instanceof DemonSpawner && ((DemonSpawner) m).spawnRecorded) {
							spawnersAbove--;
						}
					}

					if (spawnersAbove > 0) {
						if (Dungeon.bossLevel()) {
							GLog.n(Messages.get(this, "spawner_warn_final"));
						} else {
							GLog.n(Messages.get(this, "spawner_warn"));
						}
					}
				}
				
			} else if (InterlevelScene.mode == InterlevelScene.Mode.RESET) {
				GLog.h(Messages.get(this, "warp"));
			} else if (InterlevelScene.mode == InterlevelScene.Mode.RESURRECT) {
				GLog.h(Messages.get(this, "resurrect"), Dungeon.depth);
			} else {
				GLog.h(Messages.get(this, "return"), Dungeon.depth);
			}

			if (Dungeon.hero.hasTalent(Talent.ROGUES_FORESIGHT)
					&& Dungeon.level instanceof RegularLevel && Dungeon.branch == 0){
				int reqSecrets = Dungeon.level.feeling == Level.Feeling.SECRETS ? 2 : 1;
				for (Room r : ((RegularLevel) Dungeon.level).rooms()){
					if (r instanceof SecretRoom) reqSecrets--;
				}

				//75%/100% chance, use level's seed so that we get the same result for the same level
				//offset seed slightly to avoid output patterns
				Random.pushGenerator(Dungeon.seedCurDepth()+1);
					if (reqSecrets <= 0 && Random.Int(4) < 2+Dungeon.hero.pointsInTalent(Talent.ROGUES_FORESIGHT)){
						GLog.p(Messages.get(this, "secret_hint"));
					}
				Random.popGenerator();
			}

			boolean unspentTalents = false;
			for (int i = 1; i <= Dungeon.hero.talents.size(); i++){
				if (Dungeon.hero.talentPointsAvailable(i) > 0){
					unspentTalents = true;
					break;
				}
			}
			if (unspentTalents){
				GLog.newLine();
				GLog.w( Messages.get(Dungeon.hero, "unspent") );
				StatusPane.talentBlink = 10f;
				WndHero.lastIdx = 1;
			}

			switch (Dungeon.level.feeling) {
				case CHASM:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.CHASM_FLOOR);
					break;
				case WATER:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.WATER_FLOOR);
					break;
				case GRASS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.GRASS_FLOOR);
					break;
				case DARK:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.DARK_FLOOR);
					break;
				case LARGE:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.LARGE_FLOOR);
					break;
				case TRAPS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.TRAPS_FLOOR);
					break;
				case SECRETS:
					GLog.w(Dungeon.level.feeling.desc());
					Notes.add(Notes.Landmark.SECRETS_FLOOR);
					break;
			}

			for (Mob mob : Dungeon.level.mobs) {
				if (!mob.buffs(ChampionEnemy.class).isEmpty()) {
					GLog.w(Messages.get(ChampionEnemy.class, "warn"));
				}
			}

			if (Dungeon.hero.buff(AscensionChallenge.class) != null){
				Dungeon.hero.buff(AscensionChallenge.class).saySwitch();
			}

			DimensionalSundial.sundialWarned = true;
			if (DimensionalSundial.spawnMultiplierAtCurrentTime() > 1){
				GLog.w(Messages.get(DimensionalSundial.class, "warning"));
			} else {
				DimensionalSundial.sundialWarned = false;
			}

			InterlevelScene.mode = InterlevelScene.Mode.NONE;
			}
		}

		//Tutorial
		if (SPDSettings.intro()){

			if (BukovMode.active()) {
				// The raid HUD is the tutorial surface. Never hide it behind the
				// inherited first-floor tutorial transition.
				GameLog.wipe();
				SPDSettings.intro(false);
			} else if (Document.ADVENTURERS_GUIDE.isPageFound(Document.GUIDE_INTRO)){
				GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_INTRO);
			} else if (ControllerHandler.isControllerConnected()) {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_controller"));
			} else if (SPDSettings.interfaceSize() == 0) {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_mobile"));
			} else {
				GameLog.wipe();
				GLog.p(Messages.get(GameScene.class, "tutorial_move_desktop"));
			}
			if (!BukovMode.active()) {
				toolbar.visible = toolbar.active = false;
				status.visible = status.active = false;
				if (inventory != null) inventory.visible = inventory.active = false;
			}
		}

		if (!BukovMode.active() &&
				!SPDSettings.intro() &&
				Rankings.INSTANCE.totalNumber > 0 &&
				!Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_DIEING)){
			GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_DIEING);
		}

		TrinketCatalyst cata = Dungeon.hero.belongings.getItem(TrinketCatalyst.class);
		if (!BukovMode.active() && cata != null && cata.hasRolledTrinkets()){
			addToFront(new TrinketCatalyst.WndTrinket(cata));
		}

		if (BukovMode.active()) {
			invVisible = false;
		} else if (!invVisible) {
			toggleInvPane();
		}
		fadeIn();

		//re-show WndResurrect if needed
			if (!Dungeon.hero.isAlive() && !BukovMode.active()){
			//check if hero has an unblessed ankh
			Ankh ankh = null;
			for (Ankh i : Dungeon.hero.belongings.getAllItems(Ankh.class)){
				if (!i.isBlessed()){
					ankh = i;
				}
			}
			if (ankh != null && GamesInProgress.gameExists(GamesInProgress.curSlot)) {
				add(new WndResurrect(ankh));
			} else {
				gameOver();
				}
			}

			if (BukovMode.active()) {
				if (!initializeBukovRaid()) {
					return;
				}
				installBukovRuntimeLoadout();
				if (bukovEmergencyLoadoutRecovered) {
					GLog.p(Messages.get(
							GameScene.class,
							"bukov_emergency_loadout_recovered"));
				}
				bukovWorld = new BukovRealtimeWorld(
						Dungeon.hero,
						bukovRaid,
						new BukovRaidPersistence.Commit() {
							@Override
							public void persist() throws IOException {
								persistBukovHostAndCheckpoint();
							}
						}
				);
				bukovWorld.installEquippedGear(
						bukovRuntimeLoadout.equippedGear());
				initializeBukovAudio();
				bukovCombatPresentation = new BukovCombatPresentation();
				bukovRealtime = new RealtimeRaidSystem(
						bukovWorld,
						bukovRaid
				);
				bukovHud = new BukovRaidHud().bind(
						Dungeon.hero,
						bukovWorld.firearmRegistry(),
						bukovRaid.session(),
						bukovWorld
				);
				if (!DeviceCompat.isDesktop()) {
					bukovHud.objective(BukovHudFormat.TOUCH_OBJECTIVE);
				}
				bukovHud.camera = uiCamera;
				if (DeviceCompat.isDesktop()) {
					bukovPause = new BukovPauseButton(new Callback() {
						@Override
						public void call() {
							openBukovPause();
						}
					});
					bukovPause.camera = uiCamera;
					bukovPause.setRect(
							uiCamera.width - insets.right - 36f,
							screentop + 4f,
							32f,
							18f);
					add(bukovPause);
				} else {
					bukovTouchControls = new BukovTouchControls()
							.listener(new BukovTouchControls.Listener() {
								@Override
								public void onActionPressed(BukovTouchState.Action action) {
									if (action == BukovTouchState.Action.PAUSE) {
										openBukovPause();
									} else if (action
											== BukovTouchState.Action.BACKPACK) {
										openBukovBackpack();
									}
								}
							})
							.safeInsets(
									insets.left,
									Math.max(0f, screentop),
									insets.right,
									insets.bottom);
					bukovTouchControls.camera = uiCamera;
					bukovTouchControls.setRect(0f, 0f, uiCamera.width, uiCamera.height);
					add(bukovTouchControls);
					bukovWorld.touchControls(bukovTouchControls);
				}

				float hudWidth = uiCamera.width - insets.left - insets.right
						- (DeviceCompat.isDesktop() ? 44f : 8f);
				bukovHud.setRect(
						insets.left + 4f,
						screentop + 4f,
						hudWidth,
						BukovRaidHud.preferredHeight(
								hudWidth,
								SPDSettings.bukovUiScale())
				);
				if (bukovTouchControls != null) {
					bukovTouchControls.hudBottom(bukovHud.bottom() + 2f);
				}
				add(bukovHud);
			}

		}

		private void installBukovRuntimeLoadout() {
			FirearmRegistry firearms = new FirearmRegistry();
			AmmoRegistry ammunition = new AmmoRegistry();
			firearms.loadDefault();
			ammunition.loadDefault();
			firearms.validateAmmunition(ammunition);
			bukovRuntimeLoadout =
					new BukovRuntimeLoadoutAdapter(firearms, ammunition)
							.materialize(bukovRaid);
			bukovRuntimeLoadout.installOn(Dungeon.hero);
		}

		private void initializeBukovAudio() {
			bukovAudioDefaults = BukovExperienceSettings.defaults(
					new ExperienceContractRegistry().loadDefault());
			bukovAudioMix = new BukovAudioBusMix();
			bukovUiSounds = new BukovUiSoundPlayer();
			bukovAtmosphere = new BukovAtmosphereController();
			bukovAtmosphereSignal = new BukovAtmosphereSignal();
			bukovAtmospherePlayer = new BukovAtmospherePlayer();
			refreshBukovAudioMix();
			bukovAtmospherePlayer.start();
		}

		private void updateBukovAudio(float deltaSeconds) {
			if (bukovWorld == null
					|| bukovAudioMix == null
					|| bukovAtmosphere == null
					|| bukovAtmosphereSignal == null
					|| bukovAtmospherePlayer == null) {
				return;
			}
			bukovWorld.readAtmosphereSignal(bukovAtmosphereSignal);
			bukovAtmosphere.update(deltaSeconds, bukovAtmosphereSignal);
			refreshBukovAudioMix();
			bukovAtmospherePlayer.update(
					bukovAtmosphere,
					bukovAudioMix.gain(
							AudioChannel.AMBIENCE,
							bukovAtmosphere.combatBlend()));
			if (bukovUiSounds != null) {
				bukovUiSounds.update(deltaSeconds);
			}
		}

		private void refreshBukovAudioMix() {
			if (bukovAudioDefaults == null || bukovAudioMix == null) return;
			bukovAudioMix.set(
					bukovAudioDefaults.masterVolume
							* SPDSettings.bukovVolumeGain(
									SPDSettings.bukovMasterVolume()),
					bukovAudioDefaults.musicVolume
							* SPDSettings.bukovVolumeGain(
									SPDSettings.bukovMusicVolume()),
					bukovAudioDefaults.sfxVolume
							* SPDSettings.bukovVolumeGain(
									SPDSettings.bukovSfxVolume()),
					bukovAudioDefaults.ambienceVolume
							* SPDSettings.bukovVolumeGain(
									SPDSettings.bukovAmbienceVolume()));
		}

		private void disposeBukovAudio() {
			if (bukovAtmospherePlayer != null) {
				bukovAtmospherePlayer.dispose();
			}
			bukovAtmospherePlayer = null;
			bukovAtmosphereSignal = null;
			bukovAtmosphere = null;
			bukovUiSounds = null;
			bukovAudioMix = null;
			bukovAudioDefaults = null;
		}

		public static boolean playBukovUiCue(BukovUiSoundPlayer.Cue cue) {
			if (scene == null
					|| !BukovMode.active()
					|| scene.bukovUiSounds == null
					|| scene.bukovAudioMix == null
					|| scene.bukovAtmosphere == null) {
				return false;
			}
			scene.refreshBukovAudioMix();
			return scene.bukovUiSounds.play(
					cue,
					scene.bukovAudioMix.gain(
							AudioChannel.SFX,
							scene.bukovAtmosphere.combatBlend()));
		}

		private boolean initializeBukovRaid() {
			try {
				bukovSaves = BukovSaveServices.platformDefault();
				String raidId = currentBukovRaidId();
				List<ExtractionState> extractionDefinitions =
						bukovExtractionDefinitions();
				List<BukovContainerDefinition> containerDefinitions =
						bukovContainerDefinitions();
				BukovRaidCoordinator resumed =
						BukovRaidCoordinator.resume(bukovSaves);
				if (resumed != null) {
					bukovRaid = resumed;
					bukovEmergencyLoadoutRecovered =
							resumed.emergencyLoadoutRecovered();
					if (bukovRaid.session().seed != Dungeon.seed) {
						throw new IOException(
								"Bukov checkpoint does not match the loaded host save");
					}
					bukovRaid.ensureWorldDefinitions(
							extractionDefinitions,
							containerDefinitions);
				} else {
					bukovEmergencyLoadoutRecovered = false;
					// A durable receipt with no checkpoint means settlement
					// committed before host-save cleanup completed.
					BukovProfile profile = bukovSaves.loadProfile();
					if (profile.isSettled(raidId)) {
						finishBukovHostSave();
						return false;
					}
					float raidWeightCapacity =
							BukovGearRules.resolve(
									profile.loadout().items(profile.stash()))
									.weightCapacityKg;
					bukovRaid = BukovRaidCoordinator.start(
							bukovSaves,
							Dungeon.seed,
							raidId,
							raidWeightCapacity,
							extractionDefinitions,
							containerDefinitions
					);
				}
				nextBukovCheckpoint =
						bukovRaid.session().elapsedSeconds + BUKOV_CHECKPOINT_SECONDS;
				return true;
			} catch (IOException | RuntimeException e) {
				ShatteredPixelDungeon.reportException(e);
				GLog.n(Messages.get(GameScene.class, "bukov_save_init_failed"));
				bukovSettlementTransition = true;
				ShatteredPixelDungeon.switchScene(BukovHubScene.class);
				return false;
			}
			}

			private List<ExtractionState> bukovExtractionDefinitions() {
				if (!(Dungeon.level instanceof BukovLevel)) {
					return Collections.singletonList(ExtractionState.basic());
				}
				BukovRaidLayout layout = ((BukovLevel)Dungeon.level).raidLayout();
				if (layout == null || layout.extractions.isEmpty()) {
					return Collections.singletonList(ExtractionState.basic());
				}
				List<ExtractionState> result = new ArrayList<>();
				for (ExtractionDefinition definition : layout.extractions) {
					ExtractionState.Type type;
					switch (definition.type) {
						case CONDITIONAL:
							type = ExtractionState.Type.CONDITIONAL;
							break;
						case TEMPORARY:
							type = ExtractionState.Type.TEMPORARY;
							break;
						case BASELINE:
						default:
							type = ExtractionState.Type.BASIC;
							break;
					}
					result.add(new ExtractionState(
							definition.id,
							type,
							definition.interactionSeconds,
							definition.availableFromSeconds,
							definition.availableUntilSeconds));
				}
				return result;
			}

			private List<BukovContainerDefinition> bukovContainerDefinitions() {
				if (!(Dungeon.level instanceof BukovLevel)) {
					return Collections.emptyList();
				}
				List<BukovContainerDefinition> result = new ArrayList<>();
				for (BukovRaidLayout.LootAnchor anchor :
						((BukovLevel)Dungeon.level).lootAnchors()) {
					if (anchor.cell < 0) continue;
					result.add(new BukovContainerDefinition(
							anchor.id,
							anchor.cell,
							anchor.lootTableId,
							"high_value".equals(anchor.lootTableId) ? 3 : 2,
							anchor.searchSeconds,
							false));
				}
				int maintenanceCell = ((BukovLevel)Dungeon.level)
						.semanticCell("scrap_compactor");
				if (maintenanceCell < 0) {
					throw new IllegalStateException(
							"Bukov raid is missing the optional maintenance cache anchor");
				}
				result.add(new BukovContainerDefinition(
						BukovFirstRaidLootTables
								.MAINTENANCE_CACHE_CONTAINER_ID,
						maintenanceCell,
						BukovFirstRaidLootTables.MAINTENANCE_CACHE,
						3,
						3.2f,
						true));
				BukovRaidLayout.MissionGate missionGate =
						((BukovLevel)Dungeon.level).missionGate();
				if (missionGate == null || missionGate.archiveCell < 0) {
					throw new IllegalStateException(
							"Bukov first-raid layout is missing Q01 archive anchor");
				}
				result.add(new BukovContainerDefinition(
						FirstRaidMission.ARCHIVE_CONTAINER_ID,
						missionGate.archiveCell,
						FirstRaidMission.ARCHIVE_LOOT_TABLE_ID,
						1,
						1.4f,
						false));
				return result;
			}

			private void consumeBukovCombatFx(CombatFxEvent event) {
				if (event == null || Dungeon.level == null) {
					return;
				}
				if (!BukovPerformancePolicy.renderCombatFx(
						SPDSettings.bukovPerformanceProfile(),
						event.type(),
						event.sequence())) {
					return;
				}
				float tileSize = DungeonTilemap.SIZE;
				float center = tileSize * 0.5f;
				PointF from = new PointF(
						event.fromX() * tileSize + center,
						event.fromY() * tileSize + center);
				PointF to = new PointF(
						event.toX() * tileSize + center,
						event.toY() * tileSize + center);
				switch (event.type()) {
					case MUZZLE_FLASH:
						overFogEffects.add(new BukovMuzzleFx(
								from,
								new PointF(
										event.toX() - event.fromX(),
										event.toY() - event.fromY()),
								event.hostile(),
								event.intensity()));
						break;
					case SHELL:
						overFogEffects.add(new BukovShellFx(
								from,
								new PointF(
										event.toX() - event.fromX(),
										event.toY() - event.fromY()),
								event.hostile(),
								event.intensity()));
						break;
					case TRACER:
						overFogEffects.add(new BukovTracerFx(
								from,
								to,
								event.hostile(),
								event.intensity()));
						break;
					case IMPACT:
						overFogEffects.add(new BukovImpactFx(
								to,
								event.hostile(),
								event.intensity()));
						break;
					default:
						break;
				}
			}

			private void updateBukovLifecycle() {
			if (bukovRaid == null
					|| bukovRaid.finished()
					|| bukovSettlementTransition) {
				return;
			}
			if (bukovLifecycleRetryDelay > 0f) {
				bukovLifecycleRetryDelay =
						Math.max(0f, bukovLifecycleRetryDelay - Game.elapsed);
				return;
			}
			try {
				if (!Dungeon.hero.isAlive()) {
					float elapsed = bukovRaid.session().elapsedSeconds;
					int kills = bukovWorld == null ? 0 : bukovWorld.killCount();
					writeBackBukovRuntimeLoadout();
					if (bukovWorld != null) {
						bukovWorld.finishMedicalRuntime();
					}
					RaidResult result = bukovRaid.settleDeath();
					finishBukovHostSave(result, elapsed, kills);
					return;
				}
				for (ExtractionState extraction : bukovRaid.extractions()) {
					if (extraction.completed()) {
						float elapsed = bukovRaid.session().elapsedSeconds;
						int kills = bukovWorld == null ? 0 : bukovWorld.killCount();
						writeBackBukovRuntimeLoadout();
						if (bukovWorld != null) {
							bukovWorld.finishMedicalRuntime();
						}
						RaidResult result = bukovRaid.settleSuccess();
						finishBukovHostSave(result, elapsed, kills);
						return;
					}
				}
				if (bukovRaid.session().elapsedSeconds >= nextBukovCheckpoint) {
					persistBukovHostAndCheckpoint();
					nextBukovCheckpoint =
							bukovRaid.session().elapsedSeconds
									+ BUKOV_CHECKPOINT_SECONDS;
				}
			} catch (IOException | RuntimeException e) {
				// Keep the current host save and raid checkpoint available for
				// retry; the coordinator's receipt makes settlement idempotent.
				ShatteredPixelDungeon.reportException(e);
				playBukovUiCue(BukovUiSoundPlayer.Cue.ERROR);
				GLog.n(Messages.get(GameScene.class, "bukov_checkpoint_failed"));
				bukovLifecycleRetryDelay = 1f;
			}
		}

		private void saveBukovCheckpoint() {
			if (bukovRaid == null
					|| bukovRaid.finished()
					|| bukovSettlementTransition) {
				return;
			}
			try {
				persistBukovHostAndCheckpoint();
			} catch (IOException | RuntimeException e) {
				ShatteredPixelDungeon.reportException(e);
			}
		}

		/**
		 * Commits one coherent raid snapshot.
		 *
		 * Runtime quantities are written back first, then the host hero/level
		 * is persisted, and finally the Bukov checkpoint records the same
		 * ledger/container state. If the second write fails, callers retain a
		 * dirty retry rather than accepting the mutation as saved.
		 */
		private void persistBukovHostAndCheckpoint() throws IOException {
			if (bukovRaid == null
					|| bukovRaid.finished()
					|| bukovSettlementTransition) {
				return;
			}
			writeBackBukovRuntimeLoadout();
			Dungeon.saveAll();
			bukovRaid.saveCheckpoint();
		}

		private void writeBackBukovRuntimeLoadout() {
			if (bukovWorld != null) {
				bukovWorld.writeBackCarriedRuntimeItems();
			}
			if (bukovRuntimeLoadout != null
					&& bukovRaid != null
					&& !bukovRaid.finished()) {
				bukovRuntimeLoadout.writeBack(bukovRaid.loot());
			}
		}

		private void finishBukovHostSave() {
			bukovSettlementTransition = true;
			Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);
			closeBukovBackpack();
			resetBukovInputState();
			disposeBukovAudio();
			if (bukovRealtime != null) {
				bukovRealtime.dispose();
				bukovRealtime = null;
			}
			disposeBukovCombatPresentation();
			ShatteredPixelDungeon.switchScene(BukovHubScene.class);
		}

		private void finishBukovHostSave(
				RaidResult result, float elapsed, int kills) {
			bukovSettlementTransition = true;
			Dungeon.deleteGame(BukovMode.SAVE_SLOT, true);
			closeBukovBackpack();
			resetBukovInputState();
			disposeBukovAudio();
			if (bukovRealtime != null) {
				bukovRealtime.dispose();
				bukovRealtime = null;
			}
			bukovWorld = null;
			disposeBukovCombatPresentation();
			bukovRuntimeLoadout = null;
			show(new WndBukovSettlement(
					result,
					elapsed,
					kills,
					new WndBukovSettlement.ReturnToHideout() {
						@Override
						public void run() {
							ShatteredPixelDungeon.switchScene(BukovHubScene.class);
						}
					},
					new WndBukovSettlement.RepeatLastLoadout() {
						@Override
						public void run() throws Exception {
							BukovHubController hub = new BukovHubController(
									BukovSaveServices.platformDefault());
							hub.repeatLastLoadout();
						}
					}));
		}

		private void saveBukovAndReturnToHub() {
			if (!BukovMode.active() || bukovSettlementTransition) {
				return;
			}
			try {
				persistBukovHostAndCheckpoint();
				bukovSettlementTransition = true;
				closeBukovBackpack();
				resetBukovInputState();
				disposeBukovAudio();
				if (bukovRealtime != null) {
					bukovRealtime.dispose();
					bukovRealtime = null;
				}
				ShatteredPixelDungeon.switchScene(BukovHubScene.class);
			} catch (IOException | RuntimeException error) {
				ShatteredPixelDungeon.reportException(error);
				playBukovUiCue(BukovUiSoundPlayer.Cue.ERROR);
				show(new WndMessage(
						"行动保存失败：\n"
								+ (error.getMessage() == null
								? error.getClass().getSimpleName()
								: error.getMessage())));
			}
		}

		private static String currentBukovRaidId() {
			return "raid-"
					+ com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
							.toUnsignedString(Dungeon.seed);
		}
		
		public void destroy() {

			saveBukovCheckpoint();
			closeBukovBackpack();
			resetBukovInputState();
			disposeBukovAudio();
			if (bukovRealtime != null) {
				bukovRealtime.dispose();
				bukovRealtime = null;
			}
			bukovWorld = null;
			disposeBukovCombatPresentation();
			bukovRaid = null;
			bukovRuntimeLoadout = null;
			bukovSaves = null;
			bukovHud = null;
			bukovPause = null;
			bukovBackpack = null;
			if (bukovTouchControls != null) {
				bukovTouchControls.resetInput();
				bukovTouchControls = null;
			}

			//tell the actor thread to finish, then wait for it to complete any actions it may be doing.
		if (!waitForActorThread( 4500, true )){
			Throwable t = new Throwable();
			t.setStackTrace(actorThread.getStackTrace());
			throw new RuntimeException("timeout waiting for actor thread! ", t);
		}

		Emitter.freezeEmitters = false;
		
		scene = null;
		Badges.saveGlobal();
		Journal.saveGlobal();
		
		super.destroy();
	}

	private void disposeBukovCombatPresentation() {
		if (bukovCombatPresentation != null) {
			bukovCombatPresentation.dispose();
			bukovCombatPresentation = null;
		}
	}
	
	public static void endActorThread(){
		if (actorThread != null && actorThread.isAlive()){
			Actor.keepActorThreadAlive = false;
			actorThread.interrupt();
		}
	}

	public boolean waitForActorThread(int msToWait, boolean interrupt){
		if (actorThread == null || !actorThread.isAlive()) {
			return true;
		}
		synchronized (actorThread) {
			if (interrupt) actorThread.interrupt();
			try {
				actorThread.wait(msToWait);
			} catch (InterruptedException e) {
				ShatteredPixelDungeon.reportException(e);
			}
			return !Actor.processing();
		}
	}
	
	@Override
	public synchronized void onPause() {
		resetBukovInputState();
		try {
			if (!Dungeon.hero.ready) waitForActorThread(500, false);
			if (BukovMode.active()) {
				persistBukovHostAndCheckpoint();
			} else {
				Dungeon.saveAll();
			}
			Badges.saveGlobal();
			Journal.saveGlobal();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
	}

	private static Thread actorThread;
	
	//sometimes UI changes can be prompted by the actor thread.
	// We queue any removed element destruction, rather than destroying them in the actor thread.
	private ArrayList<Gizmo> toDestroy = new ArrayList<>();

	//the actor thread processes at a maximum of 60 times a second
	//this caps the speed of resting for higher refresh rate displays
	private float notifyDelay = 1/60f;

	public static boolean updateItemDisplays = false;

	public static boolean tagDisappeared = false;
	public static boolean updateTags = false;

	private static float waterOfs = 0;

	@Override
	public synchronized void update() {
		lastOffset = null;

		if (updateItemDisplays){
			updateItemDisplays = false;
			if (BukovMode.active()) {
				if (bukovHud != null) {
					bukovHud.refresh();
				}
			} else {
				QuickSlotButton.refresh();
				InventoryPane.refresh();
				if (ActionIndicator.action instanceof MeleeWeapon.Charger) {
					//Champion weapon swap uses items, needs refreshing whenever item displays are updated
					ActionIndicator.refresh();
				}
			}
		}

		if (Dungeon.hero == null || scene == null) {
			return;
		}

		super.update();
		if (bukovTouchControls != null) {
			bukovTouchControls.inputBlocked(showingWindow());
		}

		if (notifyDelay > 0) notifyDelay -= Game.elapsed;

			if (!Emitter.freezeEmitters) {
				waterOfs -= 5 * Game.elapsed;
				water.offsetTo( 0, waterOfs );
				waterOfs = water.offsetY(); //re-assign to account for auto adjust
			}

			if (BukovMode.active() && bukovRealtime != null) {
				bukovRealtime.update(Game.elapsed);
				/*
				 * RealtimeWorld owns the smooth camera, while this final
				 * presentation guard keeps that camera inside the authored
				 * map, aligned to physical pixels and unable to strand the
				 * operator outside a safe central viewport if another legacy
				 * pan/follow caller interferes.
				 */
				enforceBukovViewport();
				updateBukovAudio(Game.elapsed);
				bukovRealtime.drainCombatFx(bukovFxConsumer);
				if (bukovCombatPresentation != null) {
					bukovCombatPresentation.update(Game.elapsed);
					bukovRealtime.drainCombatPresentation(
							bukovCombatPresentation);
				}
				if (bukovWorld.consumeBackpackRequested()) {
					openBukovBackpack();
				}
				updateBukovLifecycle();
			}

			if (!BukovMode.active() && !Actor.processing() && Dungeon.hero.isAlive()) {
			if (actorThread == null || !actorThread.isAlive()) {
				
				actorThread = new Thread() {
					@Override
					public void run() {
						Actor.process();
					}
				};

				//if cpu cores are limited, game should prefer drawing the current frame
				if (Runtime.getRuntime().availableProcessors() == 1) {
					actorThread.setPriority(Thread.NORM_PRIORITY - 1);
				}
				actorThread.setName("SHPD Actor Thread");
				Thread.currentThread().setName("SHPD Render Thread");
				Actor.keepActorThreadAlive = true;
				actorThread.start();
			} else if (notifyDelay <= 0f) {
				notifyDelay += 1/60f;
				synchronized (actorThread) {
					actorThread.notify();
				}
			}
		}

		if (log != null && Dungeon.hero.ready && Dungeon.hero.paralysed == 0) {
			log.newLine();
		}

		if (!BukovMode.active() && attack != null && loot != null
				&& action != null && resume != null && updateTags){
			tagAttack = attack.active;
			tagLoot = loot.visible;
			tagAction = action.visible;
			tagResume = resume.visible;

			layoutTags();

		} else if (!BukovMode.active() && attack != null && loot != null
				&& action != null && resume != null && (tagAttack != attack.active ||
				tagLoot != loot.visible ||
				tagAction != action.visible ||
				tagResume != resume.visible)) {

			boolean tagAppearing = (attack.active && !tagAttack) ||
									(loot.visible && !tagLoot) ||
									(action.visible && !tagAction) ||
									(resume.visible && !tagResume);

			tagAttack = attack.active;
			tagLoot = loot.visible;
			tagAction = action.visible;
			tagResume = resume.visible;

			//if a new tag appears, re-layout tags immediately
			//otherwise, wait until the hero acts, so as to not suddenly change their position
			if (tagAppearing)   layoutTags();
			else                tagDisappeared = true;

		}

			cellSelector.enable(!BukovMode.active() && Dungeon.hero.ready);

		if (!toDestroy.isEmpty()) {
			for (Gizmo g : toDestroy) {
				g.destroy();
			}
			toDestroy.clear();
		}
	}

	private void enforceBukovViewport() {
		if (hero == null || Camera.main == null || Dungeon.level == null) {
			return;
		}
		float focusX = hero.x + hero.width() * 0.5f;
		float focusY = hero.y + hero.height() * 0.5f;
		Camera camera = Camera.main;
		camera.scroll.set(
				BukovViewport.resolveScroll(
						camera.scroll.x,
						focusX,
						camera.width,
						Dungeon.level.width() * DungeonTilemap.SIZE,
						camera.zoom),
				BukovViewport.resolveScroll(
						camera.scroll.y,
						focusY,
						camera.height,
						Dungeon.level.height() * DungeonTilemap.SIZE,
						camera.zoom)
		);
	}

	private static Point lastOffset = null;

	@Override
	public synchronized Gizmo erase (Gizmo g) {
		Gizmo result = super.erase(g);
		if (result instanceof Window){
			lastOffset = ((Window) result).getOffset();
		}
		return result;
	}

	private boolean tagAttack    = false;
	private boolean tagLoot      = false;
	private boolean tagAction    = false;
	private boolean tagResume    = false;

	public static void layoutTags() {

		updateTags = false;

		if (scene == null || BukovMode.active()
				|| scene.toolbar == null || scene.status == null
				|| scene.log == null || scene.attack == null
				|| scene.loot == null || scene.action == null
				|| scene.resume == null) {
			return;
		}

		//move the camera center up a bit if we're on full UI and it is taking up lots of space
		if (scene.inventory != null && scene.inventory.visible
				&& (uiCamera.width < 460 && uiCamera.height < 300)){
			Camera.main.setCenterOffset(0, Math.min(300-uiCamera.height, 460-uiCamera.width) / Camera.main.zoom);
		} else {
			Camera.main.setCenterOffset(0, 0);
		}
		//Camera.main.panTo(Dungeon.hero.sprite.center(), 5f);

		//adjust spacing for elements based on display cutouts
		// We use ALL here as some elements can be a fair but up the side of the screen
		RectF insets = Game.platform.getSafeInsets( PlatformSupport.INSET_ALL );
		insets = insets.scale(1f / uiCamera.zoom);

		boolean tagsOnLeft = SPDSettings.flipTags();
		float tagWidth = Tag.SIZE + (tagsOnLeft ? insets.left : insets.right);
		float tagLeft = tagsOnLeft ? 0 : uiCamera.width - tagWidth;

		float y = SPDSettings.interfaceSize() == 0 ? scene.toolbar.top()-2 : scene.status.top()-2;
		if (SPDSettings.interfaceSize() == 0){
			if (tagsOnLeft) {
				scene.log.setRect(tagWidth, y, uiCamera.width - tagWidth - insets.right, 0);
			} else {
				scene.log.setRect(insets.left, y, uiCamera.width - tagWidth - insets.left, 0);
			}
		} else {
			if (tagsOnLeft) {
				scene.log.setRect(tagWidth, y, 160 - tagWidth, 0);
			} else {
				scene.log.setRect(insets.left, y, 160 - insets.left, 0);
			}
		}

		float pos = scene.toolbar.top();
		if (tagsOnLeft && SPDSettings.interfaceSize() > 0){
			pos = scene.status.top();
		}

		if (scene.tagAttack){
			scene.attack.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.attack.flip(tagsOnLeft);
			pos = scene.attack.top();
		}

		if (scene.tagLoot) {
			scene.loot.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.loot.flip(tagsOnLeft);
			pos = scene.loot.top();
		}

		if (scene.tagAction) {
			scene.action.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.action.flip(tagsOnLeft);
			pos = scene.action.top();
		}

		if (scene.tagResume) {
			scene.resume.setRect( tagLeft, pos - Tag.SIZE, tagWidth, Tag.SIZE );
			scene.resume.flip(tagsOnLeft);
		}
	}
	
	@Override
	protected void onBackPressed() {
		if (!cancel()) {
			if (BukovMode.active()) {
				openBukovPause();
			} else {
				add( new WndGame() );
			}
		}
	}

	private void openBukovPause() {
		if (!BukovMode.active() || showingWindow()) {
			return;
		}
		add(new WndBukovPause(new WndBukovPause.SaveAndReturn() {
			@Override
			public void run() {
				saveBukovAndReturnToHub();
			}
		}));
		playBukovUiCue(BukovUiSoundPlayer.Cue.CONFIRM);
	}

	private void openBukovBackpack() {
		if (!BukovMode.active()
				|| bukovWorld == null
				|| showingWindow()) {
			return;
		}
		final BukovRealtimeWorld backpackWorld = bukovWorld;
		bukovBackpack = new WndBukovBackpack(
				new WndBukovBackpack.Controller() {
			@Override
			public BukovBackpackViewModel snapshot() {
				return backpackWorld.backpackSnapshot();
			}

			@Override
			public WndBukovBackpack.ActionFeedback drop(String itemUid) {
				BukovHeapLootAdapter.DropResult result =
						backpackWorld.dropCarriedItem(itemUid);
				if (result == BukovHeapLootAdapter.DropResult.DROPPED) {
					playBukovUiCue(BukovUiSoundPlayer.Cue.CONFIRM);
					return WndBukovBackpack.ActionFeedback.changed(
							"物品已丢在脚下");
				}
				playBukovUiCue(BukovUiSoundPlayer.Cue.ERROR);
				return WndBukovBackpack.ActionFeedback.rejected(
						result == BukovHeapLootAdapter.DropResult.PROTECTED_ITEM
								? "任务档案不可丢弃"
								: result == BukovHeapLootAdapter.DropResult.IN_USE_ITEM
								? "治疗中的物品不能丢弃"
								: "这里无法丢弃该物品");
			}

			@Override
			public WndBukovBackpack.ActionFeedback useMedical(String itemUid) {
				RealtimeMedicalSystem.BeginResult result =
						backpackWorld.beginMedical(itemUid);
				if (result == RealtimeMedicalSystem.BeginResult.STARTED) {
					playBukovUiCue(BukovUiSoundPlayer.Cue.CONFIRM);
					return WndBukovBackpack.ActionFeedback.startedUse(
							"开始治疗，移动、受击或射击会中断");
				}
				String message;
				switch (result) {
					case NO_EFFECT:
						message = "当前状态无需治疗";
						break;
					case BUSY:
					case COOLDOWN:
						message = "医疗操作暂不可用";
						break;
					case EMPTY:
						message = "该医疗品已用完";
						break;
					default:
						message = "无法使用该医疗品";
						break;
				}
				playBukovUiCue(BukovUiSoundPlayer.Cue.ERROR);
				return WndBukovBackpack.ActionFeedback.rejected(message);
			}

			@Override
			public WndBukovBackpack.ActionFeedback equipFirearm(String itemUid) {
				if (backpackWorld.equipCarriedFirearm(itemUid)) {
					playBukovUiCue(BukovUiSoundPlayer.Cue.CONFIRM);
					return WndBukovBackpack.ActionFeedback.changed(
							"武器已切换");
				}
				playBukovUiCue(BukovUiSoundPlayer.Cue.ERROR);
				return WndBukovBackpack.ActionFeedback.rejected(
						"无法装备该武器");
			}

			@Override
			public void setBackpackOpen(boolean open) {
				backpackWorld.setBackpackOpen(open);
				if (!open) {
					playBukovUiCue(BukovUiSoundPlayer.Cue.CANCEL);
					bukovBackpack = null;
				}
			}
		});
		add(bukovBackpack);
		playBukovUiCue(BukovUiSoundPlayer.Cue.CONFIRM);
	}

	private void closeBukovBackpack() {
		WndBukovBackpack openBackpack = bukovBackpack;
		bukovBackpack = null;
		if (openBackpack != null) {
			openBackpack.hide();
		} else if (bukovWorld != null) {
			// Repairs a stale flag if a scene transition removed the window
			// before its normal destroy callback.
			bukovWorld.setBackpackOpen(false);
		}
	}

	private void resetBukovInputState() {
		if (bukovTouchControls != null) {
			bukovTouchControls.resetInput();
		}
		if (bukovWorld != null) {
			bukovWorld.resetInputState();
		}
	}

	public void addCustomTile( CustomTilemap visual){
		customTiles.add( visual.create() );
	}

	public void addCustomWall( CustomTilemap visual){
		customWalls.add( visual.create() );
	}

	private void addHeapSprite( Heap heap ) {
		ItemSprite sprite;
		if (BukovMode.active()) {
			sprite = heap.sprite =
					(BukovItemSprite)heaps.recycle(BukovItemSprite.class);
		} else {
			sprite = heap.sprite =
					(ItemSprite)heaps.recycle(ItemSprite.class);
		}
		sprite.revive();
		sprite.link( heap );
		heaps.add( sprite );
	}
	
	private void addDiscardedSprite( Heap heap ) {
		heap.sprite = (DiscardedItemSprite)heaps.recycle( DiscardedItemSprite.class );
		heap.sprite.revive();
		heap.sprite.link( heap );
		heaps.add( heap.sprite );
	}
	
	private void addBlobSprite( final Blob gas ) {
		if (gas.emitter == null) {
			gases.add( new BlobEmitter( gas ) );
		}
	}
	
	private synchronized void addMobSprite( Mob mob ) {
		CharSprite sprite = mob.sprite();
		sprite.visible = Dungeon.level.heroFOV[mob.pos];
		mobs.add( sprite );
		sprite.link( mob );
		sortMobSprites();
	}

	//ensures that mob sprites are drawn from top to bottom, in case of overlap
	public static void sortMobSprites(){
		if (scene != null){
			synchronized (scene) {
				scene.mobs.sort(new Comparator() {
					@Override
					public int compare(Object a, Object b) {
						//elements that aren't visual go to the end of the list
						if (a instanceof Visual && b instanceof Visual) {
							return (int) Math.signum((((Visual) a).y + ((Visual) a).height())
									- (((Visual) b).y + ((Visual) b).height()));
						} else if (a instanceof Visual){
							return -1;
						} else if (b instanceof Visual){
							return 1;
						} else {
							return 0;
						}
					}
				});
			}
		}
	}
	
	private synchronized void prompt( String text ) {
		
		if (prompt != null) {
			prompt.killAndErase();
			toDestroy.add(prompt);
			prompt = null;
		}
		
		if (text != null) {
			prompt = new Toast( text ) {
				@Override
				protected void onClose() {
					cancel();
				}
			};
			prompt.camera = uiCamera;
			prompt.setPos( (uiCamera.width - prompt.width()) / 2, uiCamera.height - 60 );

			if (inventory != null && inventory.visible && prompt.right() > inventory.left() - 10){
				prompt.setPos(inventory.left() - prompt.width() - 10, prompt.top());
			}

			add( prompt );
		}
	}
	
	private void showBanner( Banner banner ) {
		banner.camera = uiCamera;

		float offset = Camera.main.centerOffset.y;
		banner.x = align( uiCamera, (uiCamera.width - banner.width) / 2 );
		banner.y = align( uiCamera, (uiCamera.height - banner.height) / 2 - 32 - offset );

		addToFront( banner );
	}
	
	// -------------------------------------------------------
	
	public static void add( Blob gas ) {
		Actor.add( gas );
		if (scene != null) {
			scene.addBlobSprite( gas );
		}
	}
	
	public static void add( Heap heap ) {
		if (scene != null) {
			//heaps that aren't added as part of levelgen don't count for exploration bonus
			heap.autoExplored = true;
			scene.addHeapSprite( heap );
		}
	}
	
	public static void discard( Heap heap ) {
		if (scene != null) {
			scene.addDiscardedSprite( heap );
		}
	}
	
	public static void add( Mob mob ) {
		add( mob, 0);
	}

	public static void addSprite( Mob mob ) {
		scene.addMobSprite( mob );
	}
	
	public static void add( Mob mob, float delay ) {
		Dungeon.level.mobs.add( mob );
		//mobs added on partial turns wait until next full turn to act
		delay = (float)Math.ceil(Actor.now() + delay) - Actor.now();
		if (scene != null) {
			scene.addMobSprite(mob);
			Actor.addDelayed(mob, delay);
			mob.spendToWhole();
		}
	}
	
	public static void add( EmoIcon icon ) {
		scene.emoicons.add( icon );
	}
	
	public static void add( CharHealthIndicator indicator ){
		if (scene != null) scene.healthIndicators.add(indicator);
	}
	
	public static void add( CustomTilemap t, boolean wall ){
		if (scene == null) return;
		if (wall){
			scene.addCustomWall(t);
		} else {
			scene.addCustomTile(t);
		}
	}
	
	public static void effect( Visual effect ) {
		if (scene != null) scene.effects.add( effect );
	}

	public static void effectOverFog( Visual effect ) {
		scene.overFogEffects.add( effect );
	}
	
	public static Ripple ripple( int pos ) {
		if (scene != null) {
			Ripple ripple = (Ripple) scene.ripples.recycle(Ripple.class);
			ripple.reset(pos);
			return ripple;
		} else {
			return null;
		}
	}
	
	public static synchronized SpellSprite spellSprite() {
		return (SpellSprite)scene.spells.recycle( SpellSprite.class );
	}
	
	public static synchronized Emitter emitter() {
		if (scene != null) {
			Emitter emitter = (Emitter)scene.emitters.recycle( Emitter.class );
			emitter.revive();
			return emitter;
		} else {
			return null;
		}
	}

	public static synchronized Emitter floorEmitter() {
		if (scene != null) {
			Emitter emitter = (Emitter)scene.floorEmitters.recycle( Emitter.class );
			emitter.revive();
			return emitter;
		} else {
			return null;
		}
	}
	
	public static FloatingText status() {
		return scene != null ? (FloatingText)scene.statuses.recycle( FloatingText.class ) : null;
	}
	
	public static void pickUp( Item item, int pos ) {
		if (scene != null && scene.toolbar != null && !BukovMode.active()) {
			scene.toolbar.pickup( item, pos );
		}
	}

	public static void pickUpJournal( Item item, int pos ) {
		if (scene != null && scene.menu != null && !BukovMode.active()) {
			scene.menu.pickup( item, pos );
		}
	}

	public static void flashForDocument( Document doc, String page ){
		if (scene != null && !BukovMode.active()) {
			if (doc == Document.ADVENTURERS_GUIDE){
				if (!page.equals(Document.GUIDE_INTRO)) {
					if (SPDSettings.interfaceSize() == 0) {
						GLog.p(Messages.get(Guidebook.class, "hint_mobile"));
					} else {
						GLog.p(Messages.get(Guidebook.class, "hint_desktop", KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.JOURNAL, ControllerHandler.isControllerConnected()))));
					}
				}
				Dungeon.hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Guidebook.class, "hint_status"));
			}
			scene.menu.flashForPage( doc, page );
		}
	}

	public static void endIntro(){
		if (BukovMode.active()) {
			SPDSettings.intro(false);
			return;
		}
		if (scene != null){
			SPDSettings.intro(false);
			scene.add(new Tweener(scene, 2f){
				@Override
				protected void updateValues(float progress) {
					if (progress <= 0.5f) {
						scene.status.alpha(2*progress);
						scene.status.visible = scene.status.active = true;
						scene.toolbar.visible = scene.toolbar.active = false;
						if (scene.inventory != null) scene.inventory.visible = scene.inventory.active = false;
					} else {
						scene.status.alpha(1f);
						scene.status.visible = scene.status.active = true;
						scene.toolbar.alpha((progress - 0.5f)*2);
						scene.toolbar.visible = scene.toolbar.active = true;
						if (scene.inventory != null){
							scene.inventory.visible = scene.inventory.active = true;
							scene.inventory.alpha((progress - 0.5f)*2);
						}
					}
				}
			});
			GameLog.wipe();
			if (SPDSettings.interfaceSize() == 0){
				GLog.p(Messages.get(GameScene.class, "tutorial_ui_mobile"));
			} else {
				GLog.p(Messages.get(GameScene.class, "tutorial_ui_desktop",
						KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.HERO_INFO, ControllerHandler.isControllerConnected())),
						KeyBindings.getKeyName(KeyBindings.getFirstKeyForAction(SPDAction.INVENTORY, ControllerHandler.isControllerConnected()))));
			}

			//clear hidden doors, it's floor 1 so there are only the entrance ones
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (Dungeon.level.map[i] == Terrain.SECRET_DOOR){
					Dungeon.level.discover(i);
					discoverTile(i, Terrain.SECRET_DOOR);
				}
			}
		}
	}
	
	public static void updateKeyDisplay(){
		if (scene != null && scene.menu != null) scene.menu.updateKeys();
	}

	public static void showlevelUpStars(){
		if (scene != null && scene.status != null) scene.status.showStarParticles();
	}

	public static void updateAvatar(){
		if (scene != null && scene.status != null) scene.status.updateAvatar();
	}

	public static void resetMap() {
		if (scene != null) {
			scene.tiles.map(Dungeon.level.map, Dungeon.level.width() );
			scene.visualGrid.map(Dungeon.level.map, Dungeon.level.width() );
			scene.terrainFeatures.map(Dungeon.level.map, Dungeon.level.width() );
			scene.raisedTerrain.map(Dungeon.level.map, Dungeon.level.width() );
			scene.walls.map(Dungeon.level.map, Dungeon.level.width() );
		}
		updateFog();
	}

	//updates the whole map
	public static void updateMap() {
		if (scene != null) {
			scene.tiles.updateMap();
			scene.visualGrid.updateMap();
			scene.terrainFeatures.updateMap();
			scene.raisedTerrain.updateMap();
			scene.walls.updateMap();
			updateFog();
		}
	}
	
	public static void updateMap( int cell ) {
		if (scene != null) {
			scene.tiles.updateMapCell( cell );
			scene.visualGrid.updateMapCell( cell );
			scene.terrainFeatures.updateMapCell( cell );
			scene.raisedTerrain.updateMapCell( cell );
			scene.walls.updateMapCell( cell );
			//update adjacent cells too
			updateFog( cell, 1 );
		}
	}

	public static void plantSeed( int cell ) {
		if (scene != null) {
			scene.terrainFeatures.growPlant( cell );
		}
	}

	public static void discoverTile( int pos, int oldValue ) {
		if (scene != null) {
			scene.tiles.discover( pos, oldValue );
		}
	}
	
	public static void show( Window wnd ) {
		if (scene != null) {
			if (BukovMode.active()
					&& (wnd instanceof WndGame
					|| wnd instanceof WndBag
					|| wnd instanceof WndHero)) {
				// Hard product boundary: no classic pause, backpack or hero
				// sheet may be surfaced from a stale key binding/caller.
				wnd.destroy();
				return;
			}
			cancel();

			//If a window is already present (or was just present)
			// then inherit the offset it had
			if (scene.inventory != null && scene.inventory.visible){
				Point offsetToInherit = null;
				for (Gizmo g : scene.members){
					if (g instanceof Window) offsetToInherit = ((Window) g).getOffset();
				}
				if (lastOffset != null) {
					offsetToInherit = lastOffset;
				}
				if (offsetToInherit != null && !offsetToInherit.isZero()) {
					wnd.offset(offsetToInherit);
					wnd.boundOffsetWithMargin(3);
				}
			}

			scene.addToFront(wnd);
		}
	}

	public static boolean showingWindow(){
		if (scene == null) return false;

		for (Gizmo g : scene.members){
			if (g instanceof Window) return true;
		}

		return false;
	}

	public static boolean interfaceBlockingHero(){
		if (scene == null) return false;

		if (showingWindow()) return true;

		if (scene.inventory != null && scene.inventory.isSelecting()){
			return true;
		}

		return false;
	}

	public static void toggleInvPane(){
		if (scene != null && scene.inventory != null){
			if (scene.inventory.visible){
				scene.inventory.visible = scene.inventory.active = invVisible = false;
				scene.toolbar.setPos(scene.toolbar.left(), uiCamera.height-scene.toolbar.height());
			} else {
				scene.inventory.visible = scene.inventory.active = invVisible = true;
				scene.toolbar.setPos(scene.toolbar.left(), scene.inventory.top()-scene.toolbar.height());
			}
			layoutTags();
		}
	}

	public static void centerNextWndOnInvPane(){
		if (scene != null && scene.inventory != null && scene.inventory.visible){
			lastOffset = new Point((int)scene.inventory.centerX() - uiCamera.width/2,
					(int)scene.inventory.centerY() - uiCamera.height/2);
		}
	}

	public static void updateFog(){
		if (scene != null) {
			rememberBukovVisibility();
			scene.fog.updateFog();
			scene.wallBlocking.updateMap();
		}
	}

	private static void rememberBukovVisibility() {
		if (!BukovMode.active()
				|| Dungeon.level == null
				|| Dungeon.level.heroFOV == null
				|| Dungeon.level.visited == null) {
			return;
		}
		int length = Math.min(
				Dungeon.level.heroFOV.length,
				Dungeon.level.visited.length);
		for (int cell = 0; cell < length; cell++) {
			if (Dungeon.level.heroFOV[cell]) {
				Dungeon.level.visited[cell] = true;
			}
		}
	}

	public static void updateFog(int x, int y, int w, int h){
		if (scene != null) {
			rememberBukovVisibility();
			scene.fog.updateFogArea(x, y, w, h);
			scene.wallBlocking.updateArea(x, y, w, h);
		}
	}
	
	public static void updateFog( int cell, int radius ){
		if (scene != null) {
			rememberBukovVisibility();
			scene.fog.updateFog( cell, radius );
			scene.wallBlocking.updateArea( cell, radius );
		}
	}
	
	public static void afterObserve() {
		if (scene != null) {
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.sprite != null) {
					if (mob instanceof Mimic && mob.state == mob.PASSIVE && ((Mimic) mob).stealthy() && Dungeon.level.visited[mob.pos]){
						//mimics stay visible in fog of war after being first seen
						mob.sprite.visible = true;
					} else {
						mob.sprite.visible = Dungeon.level.heroFOV[mob.pos];
					}
				}
				if (mob instanceof Ghoul){
					for (Ghoul.GhoulLifeLink link : mob.buffs(Ghoul.GhoulLifeLink.class)){
						link.updateVisibility();
					}
				}
			}
		}
	}

	public static void flash( int color ) {
		flash( color, true);
	}

	public static void flash( int color, boolean lightmode ) {
		if (scene != null) {
			//don't want to do this on the actor thread
			ShatteredPixelDungeon.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					//greater than 0 to account for negative values (which have the first bit set to 1)
					if (scene != null) {
						if (color > 0 && color < 0x01000000) {
							scene.fadeIn(0xFF000000 | color, lightmode);
						} else {
							scene.fadeIn(color, lightmode);
						}
					}
				}
			});
		}
	}

	public static void gameOver() {
		if (scene == null) return;
		if (BukovMode.active()) {
			// Defensive guard: Bukov deaths are settled by updateBukovLifecycle
			// and must never expose the inherited class-selection restart UI.
			return;
		}

		Banner gameOver = new Banner( BannerSprites.get( BannerSprites.Type.GAME_OVER ) );
		gameOver.show( 0x000000, 2f );
		scene.showBanner( gameOver );

		StyledButton restart = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(StartScene.class, "new"), 9){
			@Override
			protected void onClick() {
				GamesInProgress.selectedClass = Dungeon.hero.heroClass;
				GamesInProgress.curSlot = GamesInProgress.firstEmpty();
				ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
			}

			@Override
			public void update() {
				alpha((float)Math.pow(gameOver.am, 2));
				super.update();
			}
		};
		restart.icon(Icons.get(Icons.ENTER));
		restart.alpha(0);
		restart.camera = uiCamera;
		float offset = Camera.main.centerOffset.y;
		restart.setSize(Math.max(80, restart.reqWidth()), 20);
		restart.setPos(
				align(uiCamera, (restart.camera.width - restart.width()) / 2),
				align(uiCamera, (restart.camera.height - restart.height()) / 2 + 8 - offset)
		);
		scene.add(restart);

		StyledButton menu = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(WndKeyBindings.class, "menu"), 9){
			@Override
			protected void onClick() {
				GameScene.show(new WndGame());
			}

			@Override
			public void update() {
				alpha((float)Math.pow(gameOver.am, 2));
				super.update();
			}
		};
		menu.icon(Icons.get(Icons.PREFS));
		menu.alpha(0);
		menu.camera = uiCamera;
		menu.setSize(Math.max(80, menu.reqWidth()), 20);
		menu.setPos(
				align(uiCamera, (menu.camera.width - menu.width()) / 2),
				restart.bottom() + 2
		);
		scene.add(menu);
	}
	
	public static void bossSlain() {
		if (Dungeon.hero.isAlive()) {
			Banner bossSlain = new Banner( BannerSprites.get( BannerSprites.Type.BOSS_SLAIN ) );
			bossSlain.show( 0xFFFFFF, 0.3f, 5f );
			scene.showBanner( bossSlain );
			
			Sample.INSTANCE.play( Assets.Sounds.BOSS );
		}
	}
	
	public static void handleCell( int cell ) {
		if (!BukovMode.active()) {
			cellSelector.select( cell, PointerEvent.LEFT );
		}
	}
	
	public static void selectCell( CellSelector.Listener listener ) {
		if (BukovMode.active()) {
			return;
		}
		if (cellSelector.listener != null && cellSelector.listener != defaultCellListener){
			cellSelector.listener.onSelect(null);
		}
		cellSelector.listener = listener;
		cellSelector.enabled = Dungeon.hero.ready;
		if (scene != null) {
			scene.prompt(listener.prompt());
		}
	}
	
	public static boolean cancelCellSelector() {
		if (cellSelector.listener != null && cellSelector.listener != defaultCellListener) {
			cellSelector.resetKeyHold();
			cellSelector.cancel();
			return true;
		} else {
			return false;
		}
	}
	
	public static WndBag selectItem( WndBag.ItemSelector listener ) {
		if (BukovMode.active()) {
			return null;
		}
		cancel();

		if (scene != null) {
			//TODO can the inventory pane work in these cases? bad to fallback to mobile window
			if (scene.inventory != null && scene.inventory.visible && !showingWindow()){
				scene.inventory.setSelector(listener);
				return null;
			} else {
				WndBag wnd = WndBag.getBag( listener );
				show(wnd);
				return wnd;
			}
		}

		return null;
	}

	//logic for preserving inventory selection windows on scene reset (e.g. via auto-rotate)
	private static WndBag.ItemSelector savedSelector;

	@Override
	public synchronized void saveWindows() {
		if (members == null) return;

		super.saveWindows();
		if (scene != null && scene.inventory != null && scene.inventory.getSelector() != null){
			savedSelector = scene.inventory.getSelector();
		} else {
			for (Gizmo g : members.toArray(new Gizmo[0])){
				if (g instanceof WndBag){
					savedSelector = ((WndBag) g).getSelector();
				//also keeps selector active over inventory scroll cancel and upgrade window
				} else if (g instanceof InventoryScroll.WndConfirmCancel){
					savedSelector = ((InventoryScroll.WndConfirmCancel) g).getItemSelector();
				} else if (g instanceof WndUpgrade){
					savedSelector = ((WndUpgrade) g).getItemSelector();
				}
			}
		}
	}

	@Override
	public synchronized void restoreWindows() {
		super.restoreWindows();
		if (savedSelector != null){
			if (BukovMode.active()) {
				savedSelector = null;
				return;
			} else if (scene != null && scene.inventory != null){
				scene.inventory.setSelector(savedSelector);
			} else {
				addToFront(new WndBag(Dungeon.hero.belongings.backpack, savedSelector));
			}
			savedSelector = null;
		}
	}

	public static boolean cancel() {
		cellSelector.resetKeyHold();
		if (Dungeon.hero != null && (Dungeon.hero.curAction != null || Dungeon.hero.resting)) {
			
			Dungeon.hero.curAction = null;
			Dungeon.hero.resting = false;
			return true;
			
		} else {
			
			return cancelCellSelector();
			
		}
	}
	
	public static void ready() {
		if (BukovMode.active()) {
			if (cellSelector != null) {
				cellSelector.enable(false);
			}
			return;
		}
		selectCell( defaultCellListener );
		QuickSlotButton.cancel();
		InventoryPane.cancelTargeting();
		if (scene != null && scene.toolbar != null) scene.toolbar.examining = false;
		if (tagDisappeared) {
			tagDisappeared = false;
			updateTags = true;
		}
	}
	
	public static void checkKeyHold(){
		cellSelector.processKeyHold();
	}
	
	public static void resetKeyHold(){
		cellSelector.resetKeyHold();
	}

	public static void examineCell( Integer cell ) {
		if (cell == null
				|| cell < 0
				|| cell > Dungeon.level.length()
				|| (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell])) {
			return;
		}

		ArrayList<Object> objects = getObjectsAtCell(cell);

		if (objects.isEmpty()) {
			GameScene.show(new WndInfoCell(cell));
		} else if (objects.size() == 1){
			examineObject(objects.get(0));
		} else {
			String[] names = getObjectNames(objects).toArray(new String[0]);

			GameScene.show(new WndOptions(Icons.get(Icons.INFO),
					Messages.get(GameScene.class, "choose_examine"),
					Messages.get(GameScene.class, "multiple_examine"),
					names){
				@Override
				protected void onSelect(int index) {
					examineObject(objects.get(index));
				}
			});

		}
	}

	private static ArrayList<Object> getObjectsAtCell( int cell ){
		ArrayList<Object> objects = new ArrayList<>();

		if (cell == Dungeon.hero.pos) {
			objects.add(Dungeon.hero);

		} else if (Dungeon.level.heroFOV[cell]) {
			Mob mob = (Mob) Actor.findChar(cell);
			if (mob != null) objects.add(mob);
		}

		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null && heap.seen) objects.add(heap);

		Plant plant = Dungeon.level.plants.get( cell );
		if (plant != null) objects.add(plant);

		Trap trap = Dungeon.level.traps.get( cell );
		if (trap != null && trap.visible) objects.add(trap);

		return objects;
	}

	private static ArrayList<String> getObjectNames( ArrayList<Object> objects ){
		ArrayList<String> names = new ArrayList<>();
		for (Object obj : objects){
			if (obj instanceof Hero)        names.add(((Hero) obj).className().toUpperCase(Locale.ENGLISH));
			else if (obj instanceof Mob)    names.add(Messages.titleCase( ((Mob)obj).name() ));
			else if (obj instanceof Heap)   names.add(Messages.titleCase( ((Heap)obj).title() ));
			else if (obj instanceof Plant)  names.add(Messages.titleCase( ((Plant) obj).name() ));
			else if (obj instanceof Trap)   names.add(Messages.titleCase( ((Trap) obj).name() ));
		}
		return names;
	}

	public static void examineObject(Object o){
		if (o == Dungeon.hero){
			if (!BukovMode.active()) {
				GameScene.show( new WndHero() );
			}
		} else if ( o instanceof Mob && ((Mob) o).isActive() ){
			GameScene.show(new WndInfoMob((Mob) o));
			if (o instanceof Snake && !Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_SURPRISE_ATKS)){
				GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_SURPRISE_ATKS);
			}
		} else if ( o instanceof Heap && !((Heap) o).isEmpty() ){
			GameScene.show(new WndInfoItem((Heap)o));
		} else if ( o instanceof Plant ){
			GameScene.show( new WndInfoPlant((Plant) o) );
			//plants can be harmful to trample, so let the player ID just by examine
			Bestiary.setSeen(o.getClass());
		} else if ( o instanceof Trap ){
			GameScene.show( new WndInfoTrap((Trap) o));
			//traps are often harmful to trigger, so let the player ID just by examine
			Bestiary.setSeen(o.getClass());
		} else {
			GameScene.show( new WndMessage( Messages.get(GameScene.class, "dont_know") ) ) ;
		}
	}

	
	private static final CellSelector.Listener defaultCellListener = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer cell ) {
			if (Dungeon.hero.handle( cell )) {
				Dungeon.hero.next();
			}
		}

		@Override
		public void onRightClick(Integer cell) {
			if (cell == null
					|| cell < 0
					|| cell > Dungeon.level.length()
					|| (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell])) {
				return;
			}

			ArrayList<Object> objects = getObjectsAtCell(cell);
			ArrayList<String> textLines = getObjectNames(objects);

			//determine title and image
			String title = null;
			Image image = null;
			if (objects.isEmpty()) {
				title = WndInfoCell.cellName(cell);
				image = WndInfoCell.cellImage(cell);
			} else if (objects.size() > 1){
				title = Messages.get(GameScene.class, "multiple");
				image = Icons.get(Icons.INFO);
			} else if (objects.get(0) instanceof Hero) {
				title = textLines.remove(0);
				image = HeroSprite.avatar((Hero) objects.get(0));
			} else if (objects.get(0) instanceof Mob) {
				title = textLines.remove(0);
				image = ((Mob) objects.get(0)).sprite();
			} else if (objects.get(0) instanceof Heap) {
				title = textLines.remove(0);
				image = BukovMode.active()
						? new BukovItemSprite((Heap) objects.get(0))
						: new ItemSprite((Heap) objects.get(0));
			} else if (objects.get(0) instanceof Plant) {
				title = textLines.remove(0);
				image = TerrainFeaturesTilemap.tile(cell, Dungeon.level.map[cell]);
			} else if (objects.get(0) instanceof Trap) {
				title = textLines.remove(0);
				image = TerrainFeaturesTilemap.tile(cell, Dungeon.level.map[cell]);
			}

			//determine first text line
			if (objects.isEmpty()) {
				textLines.add(0, Messages.get(GameScene.class, "go_here"));
			} else if (objects.get(0) instanceof Hero) {
				textLines.add(0, Messages.get(GameScene.class, "go_here"));
			} else if (objects.get(0) instanceof Mob) {
				if (((Mob) objects.get(0)).alignment != Char.Alignment.ENEMY) {
					textLines.add(0, Messages.get(GameScene.class, "interact"));
				} else {
					textLines.add(0, Messages.get(GameScene.class, "attack"));
				}
			} else if (objects.get(0) instanceof Heap) {
				switch (((Heap) objects.get(0)).type) {
					case HEAP:
						textLines.add(0, Messages.get(GameScene.class, "pick_up"));
						break;
					case FOR_SALE:
						textLines.add(0, Messages.get(GameScene.class, "purchase"));
						break;
					default:
						textLines.add(0, Messages.get(GameScene.class, "interact"));
						break;
				}
			} else if (objects.get(0) instanceof Plant) {
				textLines.add(0, Messages.get(GameScene.class, "trample"));
			} else if (objects.get(0) instanceof Trap) {
				textLines.add(0, Messages.get(GameScene.class, "interact"));
			}

			//final text formatting
			if (objects.size() > 1){
				textLines.add(0, "_" + textLines.remove(0) + ":_ " + textLines.get(0));
				for (int i = 1; i < textLines.size(); i++){
					textLines.add(i, "_" + Messages.get(GameScene.class, "examine") + ":_ " + textLines.remove(i));
				}
			} else {
				textLines.add(0, "_" + textLines.remove(0) + "_");
				textLines.add(1, "_" + Messages.get(GameScene.class, "examine") + "_");
			}

			RightClickMenu menu = new RightClickMenu(image,
					title,
					textLines.toArray(new String[0])){
				@Override
				public void onSelect(int index) {
					if (index == 0){
						handleCell(cell);
					} else {
						if (objects.size() == 0){
							GameScene.show(new WndInfoCell(cell));
						} else {
							examineObject(objects.get(index-1));
						}
					}
				}
			};
			scene.addToFront(menu);
			menu.camera = PixelScene.uiCamera;
			PointF mousePos = PointerEvent.currentHoverPos();
			mousePos = menu.camera.screenToCamera((int)mousePos.x, (int)mousePos.y);
			menu.setPos(mousePos.x-3, mousePos.y-3);

		}

		@Override
		public String prompt() {
			return null;
		}
	};
}
