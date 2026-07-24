package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovEnemySpawnPlanner;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyArchetypeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatIntent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.GridLineOfSight;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyTactics;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereSignal;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereSignalSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAudioPlan;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAudioResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualizationResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualizationSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.ReloadAudioCue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.ReloadAudioCueResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SpatialAudioModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.FireControl;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FireMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.MedicalCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeMedicalSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeStatusState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEnemyFirearmDropPolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatPresentationEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatPresentationEventPool;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.ThemeRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovHeapLootAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovEquippedGear;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovKeyDoorState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovSearchableContainer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.LootTransaction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContract;
import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovBackpackViewModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchControls;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialGuide;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialHintSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialHintState;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovInteractionMarker;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovWhiteLineSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Adapter between the host level/hero and the realtime simulation.
 */
public final class BukovRealtimeWorld
		implements RealtimeRaidSystem.World, FireControl.Sink,
		RaidObjectiveSource, BukovRaidHudSource, BukovTutorialHintSource,
		KeySoundVisualizationSource, BukovAtmosphereSignalSource {

	private static final float PLAYER_SPEED_TILES_PER_SECOND = 4.25f;
	private static final float TARGET_REFRESH_SECONDS = 0.25f;
	private static final float MOB_SPEED_TILES_PER_SECOND = 2.15f;
	private static final float CONTACT_ATTACK_RANGE_TILES = 1.05f;
	private static final float BODY_SEPARATION_TILES = 0.08f;
	private static final float MINIMUM_PLAYER_SPAWN_DISTANCE_TILES = 6f;
	private static final String BASELINE_EXTRACTION_ID = "E01";
	private static final String CONDITIONAL_EXTRACTION_ID = "E02";
	private static final String PUMP_SEMANTIC_ID = "fog_lamp_pump_station";
	private static final int BASELINE_ENEMY_RESERVE_AMMO = 15;
	private static final float ALARM_REINFORCEMENT_DELAY_SECONDS = 1.25f;
	private static final float WHITE_LINE_PHASE_TWO_PULSE_SECONDS = 3.2f;
	private static final float WHITE_LINE_PHASE_THREE_PULSE_SECONDS = 2.2f;
	private static final float CAMERA_HALF_DEAD_ZONE_X = 12f;
	private static final float CAMERA_HALF_DEAD_ZONE_Y = 8f;
	private static final float CAMERA_RESPONSIVENESS = 8f;
	private static final float KEY_SOUND_LIFETIME_SECONDS = 0.9f;
	private static final float HIT_DIRECTION_LIFETIME_SECONDS = 0.85f;
	private static final PointF ZERO_CAMERA_SHIFT = new PointF();

	private final Hero hero;
	private final BukovRaidCoordinator raid;
	private final BukovRaidPersistence persistence;
	private final BukovTutorialGuide tutorialGuide;
	private final BukovRaidMode raidMode;
	private final ThemeDefinition raidTheme;
	private final BukovHeapLootAdapter lootAdapter;
	private final RealtimeStatusState medicalStatus;
	private final RealtimeMedicalSystem medicalSystem;
	private final RealtimeBody heroBody;
	private final CollisionMap collisionMap;
	private final GridCollision collision;
	private final int extractionCell;
	private final int pumpCell;
	private final int missionGateCell;
	private final int[] missionGateCells;
	private final RealtimeInput input = new RealtimeInput();
	private final RealtimeCameraFollow cameraFollow = new RealtimeCameraFollow(
			CAMERA_HALF_DEAD_ZONE_X,
			CAMERA_HALF_DEAD_ZONE_Y,
			CAMERA_RESPONSIVENESS
	);
	private final FirearmRegistry firearmRegistry = new FirearmRegistry();
	private final AmmoRegistry ammoRegistry = new AmmoRegistry();
	private final EnemyArchetypeRegistry enemyArchetypes =
			new EnemyArchetypeRegistry();
	private final List<BukovEnemySpawnPlanner.SpawnPoint> enemySpawnPoints;
	private final FireControl fireControl = new FireControl();
	private final CombatFxEventPool combatFx = new CombatFxEventPool(128);
	private final CombatPresentationEventPool combatPresentation =
			new CombatPresentationEventPool(96);
	private final ExperienceContract audioContract =
			new ExperienceContractRegistry().loadDefault();
	private final SpatialAudioModel.Result aiSoundSpatial =
			new SpatialAudioModel.Result();
	private final SpatialAudioModel.Result playbackSpatial =
			new SpatialAudioModel.Result();
	private final GunshotAudioPlan gunshotAudio = new GunshotAudioPlan();
	private final KeySoundVisualEvent keySoundVisual =
			new KeySoundVisualEvent();
	private final HitscanResolver.Hit shotHit = new HitscanResolver.Hit();
	private final HitscanResolver.Hit enemyShotHit = new HitscanResolver.Hit();
	private final PointF assistedAim = new PointF();
	private final ArrayList<RealtimeBody> targetBodies = new ArrayList<>();
	private final ArrayList<RealtimeBody> enemyShotTargetBodies =
			new ArrayList<>();
	private final IdentityHashMap<RealtimeBody, Char> charsByBody =
			new IdentityHashMap<>();
	private final ArrayList<PendingHit> pendingHits = new ArrayList<>();
	private final ArrayList<PendingEnemyShot> pendingEnemyShots =
			new ArrayList<>();
	private final ArrayList<BukovInteractionMarker> interactionMarkers =
			new ArrayList<>();
	private final ArrayList<BukovInteractionMarker> bossMechanismMarkers =
			new ArrayList<>();
	private final ArrayList<EnemyRuntime> enemies = new ArrayList<>();
	private final IdentityHashMap<Mob, EnemyRuntime> enemiesByMob =
			new IdentityHashMap<>();
	private final ArrayList<Mob> pendingEnemyAttacks = new ArrayList<>();
	private final HitscanResolver.TargetQuery targetQuery =
			(minX, minY, maxX, maxY) -> targetBodies;
	private final HitscanResolver.TargetQuery enemyShotTargetQuery =
			(minX, minY, maxX, maxY) -> enemyShotTargetBodies;
	private InputFrame inputFrame;
	private Firearm equippedFirearm;
	private FirearmDefinition equippedDefinition;
	private BukovEquippedGear equippedGear;
	private float targetRefreshRemaining;
	private boolean moving;
	private int heroEffectiveHealthAtStep;
	private int heroHealthAtStep;
	private int lastExtractionCountdown = Integer.MIN_VALUE;
	private int lastContainerCountdown = Integer.MIN_VALUE;
	private float extractionHintCooldown;
	private float missionGateHintCooldown;
	private float missionEventRetryCooldown;
	private boolean missionGateUnlocked;
	private boolean previousInteractHeld;
	private boolean fireAimReadyLastStep;
	private boolean firedShotThisStep;
	private boolean backpackOpen;
	private boolean backpackRequested;
	private boolean playerDeathPresented;
	private boolean extractionCompleteCuePlayed;
	private int lastMedicalCountdown = Integer.MIN_VALUE;
	private String activeContainerId;
	private float nextEnemySpawnSeconds;
	private int playerFxSequence;
	private int playerSoundSequence;
	private int audioSequence;
	private int keySoundSequence;
	private int transientKillCount;
	private float playerSoundX;
	private float playerSoundY;
	private float playerSoundRadius;
	private float playerSoundRemaining;
	private float environmentStepNoiseRemaining;
	private ExtractionState.Interaction extractionInteraction =
			ExtractionState.Interaction.NONE;
	private BukovTutorialEvent tutorialEvent;
	private float tutorialRemaining;
	private BukovRaidHudState.Direction hitIndicatorDirection;
	private float hitIndicatorStrength;
	private float hitIndicatorRemaining;
	private boolean modeConvergenceAnnounced;
	private boolean modeOvertimeAnnounced;

	public BukovRealtimeWorld(Hero hero) {
		this(hero, null);
	}

	public BukovRealtimeWorld(Hero hero, BukovRaidCoordinator raid) {
		this(
				hero,
				raid,
				raid == null ? null : new BukovRaidPersistence.Commit() {
					@Override
					public void persist() throws IOException {
						raid.saveCheckpoint();
					}
				});
	}

	public BukovRealtimeWorld(
			Hero hero,
			BukovRaidCoordinator raid,
			BukovRaidPersistence.Commit persistenceCommit) {
		if (hero == null || Dungeon.level == null) {
			throw new IllegalArgumentException("hero and level are required");
		}
		if (raid != null && persistenceCommit == null) {
			throw new IllegalArgumentException(
					"raid persistence is required");
		}
		this.hero = hero;
		this.raid = raid;
		raidMode = raid == null
				? BukovRaidMode.EXPEDITION : raid.session().raidMode();
		raidTheme = resolveRaidTheme();
		tutorialGuide = raid == null ? null : new BukovTutorialGuide(raid);
		persistence = persistenceCommit == null
				? null : new BukovRaidPersistence(persistenceCommit);
		lootAdapter = raid == null ? null : new BukovHeapLootAdapter(raid);
		if (lootAdapter != null) {
			lootAdapter.installCarriedRuntimeItems(hero);
		}
		RealtimeStatusState restoredStatus =
				raid == null ? null : raid.realtimeStatus();
		if (restoredStatus != null
				&& restoredStatus.maximumHealth() != Math.max(1, hero.HT)) {
			throw new IllegalStateException(
					"Realtime status does not match restored hero health");
		}
		medicalStatus = raid == null ? null
				: restoredStatus != null ? restoredStatus
				: new RealtimeStatusState(
						Math.max(1, hero.HT),
						Math.max(0, Math.min(hero.HT, hero.HP)));
		medicalSystem = raid == null ? null
				: RealtimeMedicalSystem.fromLedger(
						raid.loot(),
						medicalStatus);
		if (medicalSystem != null) {
			medicalSystem.restoreSnapshot(raid.medicalRuntime());
			hero.HP = Math.max(
					0,
					Math.min(hero.HT, Math.round(medicalStatus.health())));
		}
		if (lootAdapter != null) {
			lootAdapter.dropGuard(itemUid ->
					medicalSystem != null
							&& itemUid != null
							&& itemUid.equals(
									medicalSystem.activeItemUid()));
		}
		heroBody = hero.ensureRealtimeBody();
		collisionMap = new LevelCollisionMap(Dungeon.level);
		collision = new GridCollision(collisionMap);
		extractionCell = resolveExtractionCell(BASELINE_EXTRACTION_ID);
		pumpCell = resolvePumpCell();
		missionGateCells = resolveMissionGateCells();
		missionGateCell = missionGateCells.length == 0
				? -1 : missionGateCells[0];
		if (raid != null && Dungeon.level instanceof BukovLevel
				&& missionGateCell < 0) {
			throw new IllegalStateException(
					"Bukov first-raid layout is missing the mission gate anchor");
		}
		missionGateUnlocked = raid != null
				&& raid.eventCompleted(FirstRaidMission.EVENT_ID);
		applyMissionGateTerrain();
		firearmRegistry.loadDefault();
		ammoRegistry.loadDefault();
		firearmRegistry.validateAmmunition(ammoRegistry);
		enemyArchetypes.loadDefault();
		enemyArchetypes.validateFirearms(firearmRegistry);
		enemySpawnPoints = Dungeon.level instanceof BukovLevel
				? ((BukovLevel)Dungeon.level).enemySpawnPoints()
				: java.util.Collections
						.<BukovEnemySpawnPlanner.SpawnPoint>emptyList();
		resolveEquippedFirearm();
		spawnInitialEnemies();
		refreshEnemiesAndTargets();
		publishRealtimeState();
		targetRefreshRemaining = TARGET_REFRESH_SECONDS;
		nextEnemySpawnSeconds = raid == null
				? Float.MAX_VALUE
				: nextSpawnBoundary(raid.session().elapsedSeconds);
		input.start();
		ensureContainerMarkers();
		createInteractionMarkers();
	}

	public FirearmRegistry firearmRegistry() {
		return firearmRegistry;
	}

	public int killCount() {
		return raid == null ? transientKillCount : raid.session().killCount();
	}

	public int extractionCell() {
		return extractionCell;
	}

	public void touchControls(BukovTouchControls controls) {
		input.touchControls(controls);
	}

	/** Clears held controller/touch state across app and scene lifecycle edges. */
	public void resetInputState() {
		input.resetTransientState();
		backpackRequested = false;
	}

	public Firearm equippedFirearm() {
		resolveEquippedFirearm();
		return equippedFirearm;
	}

	/**
	 * Installs the loadout-owned equipment projection before the first frame.
	 * Armor durability remains owned by RuntimeLoadout.writeBack().
	 */
	public void installEquippedGear(BukovEquippedGear gear) {
		if (gear == null) {
			throw new IllegalArgumentException("gear is required");
		}
		if (equippedGear != null) {
			throw new IllegalStateException("equipped gear already installed");
		}
		equippedGear = gear;
	}

	public BukovBackpackViewModel backpackSnapshot() {
		if (raid == null) {
			throw new IllegalStateException("Backpack requires an active raid");
		}
		resolveEquippedFirearm();
		return BukovBackpackViewModel.from(
				raid.loot(),
				firearmRegistry,
				BukovBackpackViewModel.EquippedFirearm.from(
						equippedFirearm,
						firearmRegistry));
	}

	public void setBackpackOpen(boolean open) {
		backpackOpen = open;
		input.cancelTouches();
	}

	public boolean consumeBackpackRequested() {
		boolean requested = backpackRequested;
		backpackRequested = false;
		return requested;
	}

	@Override
	public String raidObjective() {
		if (raid != null && raid.firstRaidMissionActive()) {
			return raid.firstRaidObjective();
		}
		return missionGateUnlocked
				? FirstRaidMission.UNLOCKED_OBJECTIVE
				: FirstRaidMission.LOCKED_OBJECTIVE;
	}

	/**
	 * Copies live presentation values without advancing the simulation.
	 * BukovRaidHud owns and reuses {@code target}, so normal render frames do
	 * not allocate a snapshot object.
	 */
	@Override
	public void readRaidHudState(BukovRaidHudState target) {
		if (target == null) {
			throw new IllegalArgumentException("HUD state target is required");
		}
		float elapsed = raid == null ? 0f : raid.session().elapsedSeconds;
		target.beginFrame(raidObjective(), elapsed);

		resolveEquippedFirearm();
		if (equippedFirearm != null && equippedDefinition != null) {
			target.weapon(
					equippedDefinition.name,
					equippedDefinition.fireMode == FireMode.AUTO,
					equippedFirearm.magazineAmmo(),
					equippedDefinition.magazineSize,
					reserveAmmo(equippedDefinition.caliber),
					fireControl.reloadRemaining(),
					equippedDefinition.reloadSeconds);
		}
		if (medicalStatus != null) {
			target.status(
					medicalStatus.bleedingPerSecond(),
					medicalStatus.fractured(),
					medicalStatus.painSeverity(),
					medicalStatus.concussionRemaining(),
					medicalStatus.stimulantRemaining());
		}
		target.presentationSettings(
				SPDSettings.bukovColorblindAssist(),
				SPDSettings.bukovDamageNumbers());
		if (SPDSettings.bukovSoundVisualization()) {
			target.sound(keySoundVisual);
		}
		target.hit(
				hitIndicatorDirection,
				hitIndicatorStrength,
				hitIndicatorRemaining);
		readBossHudState(target);
		if (inputFrame != null) {
			target.aim(
					inputFrame.aim.x,
					inputFrame.aim.y,
					inputFrame.fireHeld);
		}
		readNavigationHudState(target, elapsed);
		readThreatHudState(target);
		if (raid == null) return;

		int availableExtractions = 0;
		for (ExtractionState extraction : raid.extractions()) {
			if (!extraction.completed()
					&& extractionAvailable(extraction, elapsed)) {
				availableExtractions++;
			}
		}

		String activeExtractionId = raid.activeExtractionId();
		if (activeExtractionId != null) {
			ExtractionState active = raid.extraction(activeExtractionId);
			if (active != null) {
				int activeCell = resolveExtractionCell(activeExtractionId);
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.EXTRACTION,
						activeCell,
						"撤离 " + activeExtractionId,
						extractionAvailable(active, elapsed));
				target.extraction(
						availableExtractions,
						activeExtractionId,
						extractionAvailable(active, elapsed),
						true,
						active.progressFraction(),
						active.interactionSeconds());
				target.interaction(
						BukovRaidHudState.Interaction.EXTRACT,
						"撤离中",
						active.progressFraction(),
						active.interactionSeconds());
				return;
			}
		}

		ExtractionState extractionHere = extractionAtCell(hero.pos);
		target.extraction(
				availableExtractions,
				extractionHere == null ? null : extractionHere.extractionId(),
				extractionHere != null
						&& extractionAvailable(extractionHere, elapsed),
				false,
				0f,
				extractionHere == null
						? 0f : extractionHere.interactionSeconds());

		if (medicalSystem != null && medicalSystem.isUsing()) {
			target.interaction(
					BukovRaidHudState.Interaction.MEDICAL,
					"治疗中",
					medicalSystem.useProgress(),
					0f);
			return;
		}

		if (activeContainerId != null) {
			BukovRaidCoordinator.ContainerSnapshot active =
					raid.container(activeContainerId);
			if (active != null) {
				target.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"搜索容器",
						active.progressFraction,
						active.searchSeconds);
				return;
			}
		}

		if (extractionHere != null) {
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.EXTRACTION,
					resolveExtractionCell(extractionHere.extractionId()),
					"撤离 " + extractionHere.extractionId(),
					extractionAvailable(extractionHere, elapsed));
			target.interaction(
					extractionAvailable(extractionHere, elapsed)
							? BukovRaidHudState.Interaction.EXTRACT
							: BukovRaidHudState.Interaction.LOCKED,
					extractionAvailable(extractionHere, elapsed)
							? "开始撤离" : "撤离点未开放",
					0f,
					extractionHere.interactionSeconds());
			return;
		}

		if (withinInteractionRange(hero.pos, pumpCell)) {
			ExtractionState conditional =
					raid.extraction(CONDITIONAL_EXTRACTION_ID);
			boolean ready = conditional != null
					&& conditional.conditionMet();
			target.interaction(
					ready
							? BukovRaidHudState.Interaction.LOCKED
							: BukovRaidHudState.Interaction.PUMP,
					ready ? "泵站供电正常" : "启动泵站",
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.MISSION,
					pumpCell,
					ready ? "泵站已启动" : "启动泵站",
					!ready);
			return;
		}

		BukovRaidCoordinator.ContainerSnapshot nearby =
				containerWithinRange(hero.pos);
		if (nearby != null) {
			boolean locked = nearby.state
					== BukovSearchableContainer.State.LOCKED;
			boolean maintenanceLock =
					locked && isMaintenanceCache(nearby);
			target.interaction(
					locked
							? BukovRaidHudState.Interaction.LOCKED
							: BukovRaidHudState.Interaction.SEARCH,
					maintenanceLock
							? hasMaintenanceKey()
									? "按E解锁 · 维修钥匙"
									: "需要维修钥匙"
							: locked ? "容器已锁定" : "搜索容器",
					0f,
					nearby.searchSeconds);
			return;
		}

		int heapCell = selectVisibleLootHeap(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				Dungeon.level.heroFOV,
				Dungeon.level.heaps,
				extractionCell);
		if (heapCell >= 0) {
			target.interaction(
					BukovRaidHudState.Interaction.PICKUP,
					"拾取物资",
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.PICKUP,
					heapCell,
					"可拾取物资",
					true);
			return;
		}

		if (!missionGateUnlocked
				&& withinInteractionRange(hero.pos, missionGateCell)) {
			target.interaction(
					BukovRaidHudState.Interaction.LOCKED,
					"通道锁定 · 先找到维修档案",
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.MISSION,
					missionGateCell,
					"维修通道",
					false);
		}
	}

	@Override
	public void readTutorialHint(BukovTutorialHintState target) {
		if (target == null) {
			throw new IllegalArgumentException(
					"tutorial hint target is required");
		}
		target.clear();
		if (tutorialEvent == null || tutorialRemaining <= 0f) {
			return;
		}
		target.event = tutorialEvent;
		target.message = tutorialEvent.message;
		target.remainingSeconds = tutorialRemaining;
	}

	@Override
	public int drainCombatFx(CombatFxEvent.Consumer consumer) {
		return combatFx.drain(consumer);
	}

	@Override
	public int drainCombatPresentation(
			CombatPresentationEvent.Consumer consumer) {
		return combatPresentation.drain(consumer);
	}

	@Override
	public boolean paused() {
		boolean paused = backpackOpen
				|| GameScene.interfaceBlockingHero()
				|| !hero.isAlive();
		if (paused) {
			input.cancelTouches();
		}
		return paused;
	}

	@Override
	public void beginFixedStep() {
		heroEffectiveHealthAtStep = hero.HP + hero.shielding();
		heroHealthAtStep = hero.HP;
		firedShotThisStep = false;
		heroBody.beginStep();
		for (EnemyRuntime enemy : enemies) {
			enemy.body.beginStep();
		}
	}

	@Override
	public void pollInput() {
		inputFrame = input.poll(heroBody);
		if (inputFrame.backpackPressed) {
			backpackRequested = true;
		}
	}

	@Override
	public void updatePlayer(float dt) {
		if (inputFrame == null) {
			return;
		}
		missionGateHintCooldown = Math.max(
				0f, missionGateHintCooldown - dt);
		float movementMultiplier = medicalStatus == null
				? 1f : medicalStatus.movementMultiplier();
		if (equippedGear != null) {
			movementMultiplier *= equippedGear.movementMultiplier();
		}
		if (raidTheme != null) {
			movementMultiplier *= raidTheme.environmentRules
					.movementMultiplier(heroTerrain());
		}
		float deltaX = inputFrame.movement.x
				* PLAYER_SPEED_TILES_PER_SECOND * movementMultiplier * dt;
		float deltaY = inputFrame.movement.y
				* PLAYER_SPEED_TILES_PER_SECOND * movementMultiplier * dt;
		heroBody.velocityX = deltaX / dt;
		heroBody.velocityY = deltaY / dt;
		collision.move(heroBody, deltaX, deltaY);
		if (heroOverlapsEnemy()) {
			heroBody.x = heroBody.previousX;
			heroBody.y = heroBody.previousY;
			heroBody.velocityX = 0f;
			heroBody.velocityY = 0f;
		}
		if (!missionGateUnlocked
				&& missionGateHintCooldown <= 0f
				&& movementWasBlocked(deltaX, deltaY)
				&& movementPointsTowardMissionGate(deltaX, deltaY)) {
			showHeroStatus("通道锁定：先找到维修档案");
			missionGateHintCooldown = 1.25f;
		}
		moving = Math.abs(heroBody.x - heroBody.previousX) > 0.00001f
				|| Math.abs(heroBody.y - heroBody.previousY) > 0.00001f;

		int previousCell = hero.pos;
		hero.pos = heroBody.cell(Dungeon.level.width());
		environmentStepNoiseRemaining = Math.max(
				0f,
				environmentStepNoiseRemaining - dt);
		if (raidTheme != null && moving
				&& environmentStepNoiseRemaining <= 0f) {
			float movementNoise = raidTheme.environmentRules
					.movementNoiseRadius(heroTerrain());
			if (movementNoise > 0f) {
				emitPlayerSound(movementNoise);
				environmentStepNoiseRemaining = 0.28f;
			}
		}
		if (hero.pos != previousCell) {
			Dungeon.level.updateFieldOfView(hero, Dungeon.level.heroFOV);
			GameScene.updateFog();
		}
	}

	@Override
	public void emitPlayerActions(float dt) {
		if (inputFrame == null) {
			return;
		}
		resolveEquippedFirearm();
		if (equippedFirearm == null || equippedDefinition == null) {
			return;
		}
		applyPlayerAimAssist(equippedDefinition.effectiveRangeTiles);
		boolean aimReady =
				inputFrame.aim.x != 0f || inputFrame.aim.y != 0f;
		boolean aimedPress = aimReady
				&& (inputFrame.firePressed
						|| inputFrame.fireHeld && !fireAimReadyLastStep);
		fireControl.update(
				dt,
				aimReady && inputFrame.fireHeld,
				aimedPress,
				inputFrame.reloadPressed,
				equippedFirearm,
				equippedDefinition,
				raidTheme == null
						? 1f
						: raidTheme.environmentRules
								.reloadDurationMultiplier(heroTerrain()),
				this
		);
		fireAimReadyLastStep = aimReady;
	}

	private void applyPlayerAimAssist(float maximumRange) {
		float strength = inputFrame == null
				? 0f : inputFrame.aimAssistScale;
		if (strength <= 0f
				|| inputFrame.aim.x == 0f && inputFrame.aim.y == 0f) {
			return;
		}
		EnemyRuntime best = null;
		float bestScore = Float.POSITIVE_INFINITY;
		for (EnemyRuntime enemy : enemies) {
			if (enemy == null
					|| enemy.mob == null
					|| !enemy.mob.isAlive()
					|| !enemy.body.active
					|| enemy.mob.pos < 0
					|| enemy.mob.pos >= Dungeon.level.heroFOV.length
					|| !Dungeon.level.heroFOV[enemy.mob.pos]) {
				continue;
			}
			float targetX = enemy.body.x - heroBody.x;
			float targetY = enemy.body.y - heroBody.y;
			boolean visible = GridLineOfSight.visible(
					heroBody.x,
					heroBody.y,
					enemy.body.x,
					enemy.body.y,
					maximumRange,
					collisionMap);
			if (!BukovAimAssist.accepts(
					inputFrame.aim.x,
					inputFrame.aim.y,
					targetX,
					targetY,
					maximumRange,
					visible)) {
				continue;
			}
			float score = BukovAimAssist.score(
					inputFrame.aim.x,
					inputFrame.aim.y,
					targetX,
					targetY,
					maximumRange);
			if (score < bestScore) {
				best = enemy;
				bestScore = score;
			}
		}
		if (best == null) return;
		BukovAimAssist.blend(
				inputFrame.aim.x,
				inputFrame.aim.y,
				best.body.x - heroBody.x,
				best.body.y - heroBody.y,
				strength,
				assistedAim);
		inputFrame.aim.set(assistedAim);
	}

	@Override
	public void updateSoundField(float dt) {
		keySoundVisual.advance(dt);
		hitIndicatorRemaining = Math.max(
				0f, hitIndicatorRemaining - dt);
		playerSoundRemaining = Math.max(0f, playerSoundRemaining - dt);
		if (playerSoundRemaining <= 0f || playerSoundRadius <= 0f) {
			return;
		}
		float themedSoundRadius = playerSoundRadius
				* (raidTheme == null
						? 1f
						: raidTheme.environmentRules
								.enemyHearingMultiplier(
										terrainAt(
												playerSoundX,
												playerSoundY)));
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()
					|| enemy.heardSoundSequence == playerSoundSequence) {
				continue;
			}
			float dx = playerSoundX - enemy.body.x;
			float dy = playerSoundY - enemy.body.y;
			float distanceSquared = dx * dx + dy * dy;
			if (distanceSquared
					> themedSoundRadius * themedSoundRadius) {
				continue;
			}
			float wallOcclusion = blockedCellsOnLine(
					enemy.body.x,
					enemy.body.y,
					playerSoundX,
					playerSoundY);
			SpatialAudioModel.resolve(
					audioContract,
					1f,
					(float)Math.sqrt(distanceSquared),
					wallOcclusion,
					false,
					aiSoundSpatial);
			if (aiSoundSpatial.perceivable()) {
				enemy.heardSoundSequence = playerSoundSequence;
				enemy.brain.recordSound(playerSoundX, playerSoundY);
				if (hasAbility(enemy, "INVESTIGATE_SOUND")
						|| hasAbility(enemy, "CALL_INVESTIGATORS")) {
					showEnemyStatus(enemy, CharSprite.WARNING, "听到枪声");
				}
			}
		}
	}

	@Override
	public void updatePerception(float dt) {
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()) {
				enemy.brain.markDead();
				enemy.body.active = false;
				continue;
			}
			if (enemy.brain.perceptionDue(dt)) {
				boolean visible = GridLineOfSight.visible(
						enemy.body.x,
						enemy.body.y,
						heroBody.x,
						heroBody.y,
						enemy.perceptionRange()
								* (raidTheme == null
										? 1f
										: raidTheme.environmentRules
												.enemySightMultiplier(
														heroTerrain())),
						collisionMap
				);
				enemy.brain.recordPlayer(
						visible,
						heroBody.x,
						heroBody.y
				);
				if (visible && !enemy.broadcastedContact
						&& (hasAbility(enemy, "BROADCAST_CONTACT")
								|| hasAbility(enemy, "ORDER_FLANK"))) {
					broadcastPlayerContact(enemy);
					enemy.broadcastedContact = true;
				} else if (!visible) {
					enemy.broadcastedContact = false;
				}
			}
		}
	}

	@Override
	public void updateBrains(float dt) {
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()) {
				enemy.brain.markDead();
				continue;
			}
			enemy.brain.decide(
					dt,
					enemy.body.x,
					enemy.body.y,
					heroBody.x,
					heroBody.y,
					enemy.engagementRange()
			);
			float navigationTargetX = enemy.brain.seesPlayer()
					? heroBody.x : enemy.brain.lastSeenX();
			float navigationTargetY = enemy.brain.seesPlayer()
					? heroBody.y : enemy.brain.lastSeenY();
			enemy.navigator.step(
					dt,
					enemy.body.x,
					enemy.body.y,
					navigationTargetX,
					navigationTargetY,
					enemy.brain.seesPlayer(),
					enemy.brain.desiredX(),
					enemy.brain.desiredY(),
					collisionMap,
					enemy.navigationIntent
			);
			enemy.tactics.step(
					dt,
					enemy.brain.seesPlayer(),
					enemy.body.x,
					enemy.body.y,
					heroBody.x,
					heroBody.y,
					enemy.engagementRange(),
					enemy.navigationIntent.desiredX(),
					enemy.navigationIntent.desiredY(),
					enemy.tacticalIntent
			);
			if (enemy.bossState != null) {
				enemy.bossState.update(dt);
				if (enemy.brain.seesPlayer()
						&& enemy.bossState.phase()
								== WhiteLineBossStateMachine.Phase.DORMANT) {
					enemy.bossState.engage();
					showBossObjective(enemy);
				}
			}
		}
	}

	@Override
	public void updateMobs(float dt) {
		updateEnemySpawning();
		targetRefreshRemaining -= dt;
		if (targetRefreshRemaining <= 0f) {
			refreshEnemiesAndTargets();
			targetRefreshRemaining = TARGET_REFRESH_SECONDS;
		}
		for (EnemyRuntime enemy : enemies) {
			updateEnemyMovement(enemy, dt);
			if (enemy.bossState != null) {
				updateWhiteLineOffense(enemy, dt);
			}
			if (enemy.rangedCombat != null) {
				updateEnemyRangedCombat(enemy, dt);
			} else if (enemy.brain.consumeAttack(
					enemyAttackCooldown(enemy.mob)
			) && canContactAttack(enemy)) {
				pendingEnemyAttacks.add(enemy.mob);
			}
		}
	}

	@Override
	public void updateProjectiles(float dt) {
		// Current starter definitions are hitscan; slow projectiles are separate.
	}

	@Override
	public void resolveDamageAndDeaths(float dt) {
		for (PendingHit event : pendingHits) {
			Char target = event.target;
			if (target == null || !target.isAlive()) {
				continue;
			}
			int damage = Math.max(1, Math.round(event.damage));
			damage = target.defenseProc(hero, damage);
			damage = Math.max(1, damage - target.drRoll());
			if (equippedFirearm != null) {
				playSfx(
						Assets.Sounds.Bukov.BULLET_HIT,
						0.78f,
						nextAudioPitch(1f, 0.06f)
				);
			}
			if (target instanceof Mob) {
				EnemyRuntime enemy = enemiesByMob.get((Mob)target);
				if (enemy != null && enemy.bossState != null) {
					boolean wasAlive = target.isAlive();
					resolveWhiteLineDamage(enemy, damage);
					emitEnemyHitOutcome(target, wasAlive, damage);
					continue;
				}
				if (enemy != null) {
					int armored = resolveEnemyArmor(
							enemy.definition,
							damage,
							equippedDefinition == null
									? 0f
									: equippedDefinition.penetration);
					if (armored < damage) {
						showEnemyStatus(
								enemy,
								CharSprite.NEUTRAL,
								"护甲吸收");
					}
					damage = armored;
				}
			}
			// Keep Mob.damage/die authoritative so XP, loot rolls, death VFX, and
			// Dungeon.level.mobs removal stay in the host implementation.
			boolean wasAlive = target.isAlive();
			target.damage(damage, hero);
			emitEnemyHitOutcome(target, wasAlive, damage);
			if (wasAlive && !target.isAlive() && target instanceof Mob) {
				EnemyRuntime defeated =
						enemiesByMob.get((Mob)target);
				if (defeated != null) {
					releaseEnemyLoot(defeated, target.pos);
				}
				releaseEnemyFirearm((Mob)target);
				recordEnemyKill();
			}
			if (!target.isAlive() && target.realtimeBody != null) {
				target.realtimeBody.active = false;
				if (target instanceof Mob) {
					EnemyRuntime enemy = enemiesByMob.get((Mob)target);
					if (enemy != null) {
						enemy.brain.markDead();
					}
				}
			}
		}
		pendingHits.clear();

		for (PendingEnemyShot event : pendingEnemyShots) {
			if (!hero.isAlive()) {
				break;
			}
			int incomingDamage = event.damage;
			if (equippedGear != null) {
				EnemyRuntime shooter = enemiesByMob.get(event.attacker);
				float penetration = 0f;
				if (shooter != null
						&& shooter.definition.weaponDefinitionId != null) {
					penetration = firearmRegistry.require(
							shooter.definition.weaponDefinitionId).penetration;
				}
				incomingDamage = Math.max(
						0,
						Math.round(equippedGear.resolveIncomingBullet(
								incomingDamage,
								penetration,
								RealtimeDamage.HitZone.CORE)));
			}
			int damage = hero.defenseProc(event.attacker, incomingDamage);
			if (damage >= 0) {
				damage = Math.max(0, damage - hero.drRoll());
			}
			if (damage > 0) {
				playSfx(
						Assets.Sounds.Bukov.BULLET_HIT,
						0.75f,
						nextAudioPitch(1f, 0.04f)
				);
				boolean wasAlive = hero.isAlive();
				hero.damage(damage, event.attacker);
				emitPlayerHitOutcome(event.attacker, wasAlive, damage);
			} else if (hero.sprite != null) {
				hero.sprite.showStatus(
						CharSprite.NEUTRAL,
						hero.defenseVerb()
				);
			}
		}
		pendingEnemyShots.clear();

		for (Mob attacker : pendingEnemyAttacks) {
			if (!hero.isAlive()) {
				break;
			}
			EnemyRuntime enemy = enemiesByMob.get(attacker);
			if (enemy == null
					|| !attacker.isAlive()
					|| !canContactAttack(enemy)) {
				continue;
			}
			resolveEnemyContactAttack(attacker);
		}
		pendingEnemyAttacks.clear();
	}

	@Override
	public void updateStatuses(float dt) {
		if (medicalSystem == null || medicalStatus == null
				|| inputFrame == null) {
			return;
		}
		float observedDamage = Math.max(
				0f,
				heroHealthAtStep - hero.HP);
		boolean tookDamage = observedDamage > 0.0001f;
		if (tookDamage) {
			medicalStatus.applyDamage(observedDamage);
			float severity = observedDamage
					/ Math.max(1f, medicalStatus.maximumHealth());
			if (severity >= 0.18f) {
				medicalStatus.addBleeding(
						Math.min(0.65f, severity * 1.2f));
				medicalStatus.addPain(Math.min(0.45f, severity));
				showTutorial(BukovTutorialEvent.BLEEDING);
			}
			if (severity >= 0.38f) {
				medicalStatus.setFractured(true);
				medicalStatus.addConcussion(3f);
			}
		}
		if (inputFrame.medicalPressed && !medicalSystem.isUsing()) {
			beginBestAvailableMedical();
		}
		String activeUid = medicalSystem.activeItemUid();
		RealtimeMedicalSystem.StepResult result = medicalSystem.fixedStep(
				dt,
				moving,
				tookDamage,
				firedShotThisStep,
				raidTheme == null
						? 1f
						: raidTheme.environmentRules
								.medicalDurationMultiplier(heroTerrain()));
		hero.HP = Math.max(
				0,
				Math.min(
						hero.HT,
						Math.round(medicalStatus.health())));
		handleMedicalStepResult(result, activeUid);
	}

	@Override
	public void updateLootAndExtraction(float dt) {
		extractionInteraction = ExtractionState.Interaction.NONE;
		if (persistence != null && persistence.dirty()) {
			persistence.update(dt);
		}
		extractionHintCooldown = Math.max(
				0f,
				extractionHintCooldown - dt
		);
		missionEventRetryCooldown = Math.max(
				0f,
				missionEventRetryCooldown - dt
		);
		if (raid == null || inputFrame == null) {
			return;
		}
		applyModeConvergence();
		if (!missionGateUnlocked
				&& missionEventRetryCooldown <= 0f
				&& carriesMissionArchive()) {
			unlockMissionGateIfCarried();
		}

		boolean interactPressed = inputFrame.interactPressed
				|| (inputFrame.interactHeld && !previousInteractHeld);
		previousInteractHeld = inputFrame.interactHeld;
		boolean movementIntent =
				inputFrame.movement.x != 0f
						|| inputFrame.movement.y != 0f;
		boolean stationary = !moving && !movementIntent;
		boolean reloading =
				inputFrame.reloadPressed || fireControl.isReloading();
		int damageTaken = Math.max(
				0,
				heroEffectiveHealthAtStep
						- (hero.HP + hero.shielding())
		);

		if (inputFrame.dropPressed) {
			dropLatestCarriedItem();
		}

		String activeExtractionId = raid.activeExtractionId();
		if (activeExtractionId != null) {
			ExtractionState extraction = raid.extraction(activeExtractionId);
			boolean insideZone = extraction != null
					&& hero.pos == resolveExtractionCell(activeExtractionId);
			extractionInteraction = ExtractionIntentResolver.resolve(
					extraction != null,
					insideZone,
					inputFrame.interactHeld,
					stationary,
					reloading,
					damageTaken
			);
			if (extractionInteraction == ExtractionState.Interaction.ACTIVE) {
				showExtractionCountdown(extraction);
			} else if (extractionInteraction != ExtractionState.Interaction.NONE) {
				lastExtractionCountdown = Integer.MIN_VALUE;
				showHeroStatus("撤离中断");
			}
			return;
		}

		if (activeContainerId != null) {
			BukovRaidCoordinator.ContainerSnapshot active =
					raid.container(activeContainerId);
			if (active == null) {
				activeContainerId = null;
			} else {
				boolean continuing = inputFrame.interactHeld
						&& withinInteractionRange(hero.pos, active.cell);
				BukovSearchableContainer.UpdateResult result =
						raid.updateContainerSearch(
								active.containerId,
								dt,
								continuing,
								moving || movementIntent,
								damageTaken > 0,
								reloading,
								BukovFirstRaidLootTables.require(
										active.lootTableId));
				if (result == BukovSearchableContainer.UpdateResult.PROGRESSED) {
					showContainerCountdown(raid.container(active.containerId));
					return;
				}
				if (result == BukovSearchableContainer.UpdateResult.INTERRUPTED) {
					activeContainerId = null;
					lastContainerCountdown = Integer.MIN_VALUE;
					showHeroStatus("搜索中断");
					checkpointLootChange();
					return;
				}
				if (result == BukovSearchableContainer.UpdateResult.COMPLETED) {
					activeContainerId = null;
					lastContainerCountdown = Integer.MIN_VALUE;
					playSfx(
							Assets.Sounds.Bukov.SEARCH_COMPLETE,
							0.72f,
							nextAudioPitch(1f, 0.02f)
					);
					releaseCompletedContainer(active.containerId);
					if (FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							active.lootTableId)) {
						showHeroStatus(
								"高价值物资已确认，条件撤离许可已解锁");
					}
					return;
				}
			}
		}

		if (!interactPressed) {
			return;
		}

		if (completeNearbyBossObjective()) {
			return;
		}

		if (withinInteractionRange(hero.pos, pumpCell)) {
			activatePump();
			return;
		}

		BukovRaidCoordinator.ContainerSnapshot container =
				containerWithinRange(hero.pos);
		if (container != null) {
			if (container.state == BukovSearchableContainer.State.SEARCHED
					&& !container.contentsReleased) {
				releaseCompletedContainer(container.containerId);
			} else if (container.state == BukovSearchableContainer.State.LOCKED) {
				if (isMaintenanceCache(container)) {
					unlockMaintenanceCache(container);
				} else {
					showHeroStatus("容器已锁定");
				}
			} else if (raid.beginContainerSearch(container.containerId)) {
				activeContainerId = container.containerId;
				lastContainerCountdown = Integer.MIN_VALUE;
				showTutorial(BukovTutorialEvent.CONTAINER_OPENED);
				showContainerCountdown(raid.container(container.containerId));
			}
			return;
		}

		ExtractionState extraction = extractionAtCell(hero.pos);
		if (extraction != null) {
			showTutorial(BukovTutorialEvent.EXTRACTION_NEAR);
			boolean wantsToStart = ExtractionIntentResolver.wantsToStart(
					true,
					inputFrame.interactHeld,
					stationary,
					reloading,
					damageTaken
			);
			if (wantsToStart
					&& raid.beginExtraction(extraction.extractionId())) {
				bypassWhiteLineForExtraction();
				combatPresentation.emit(
						CombatPresentationEvent.Type.PLAYER_EXTRACTION,
						hero.id(),
						hero.id(),
						hero.pos,
						hero.pos,
						null,
						1f);
				playSfx(
						Assets.Sounds.Bukov.EXTRACTION_START,
						0.82f,
						1f
				);
				lastExtractionCountdown = Integer.MIN_VALUE;
				extractionInteraction = ExtractionState.Interaction.ACTIVE;
				showExtractionCountdown(extraction);
			} else if (extractionHintCooldown <= 0f) {
				showHeroStatus("撤离点未开放");
				extractionHintCooldown = 0.75f;
			}
			return;
		}

		pickupNearbyHeap();
	}

	@Override
	public ExtractionState.Interaction extractionInteraction() {
		return extractionInteraction;
	}

	private void pickupNearbyHeap() {
		int heapCell = selectVisibleLootHeap(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				Dungeon.level.heroFOV,
				Dungeon.level.heaps,
				extractionCell
		);
		if (heapCell < 0) {
			return;
		}
		Heap heap = Dungeon.level.heaps.get(heapCell);
		Item item = heap.peek();
		LootTransaction.PickupResult result = lootAdapter.pickupTop(heap, hero);
		if (result == LootTransaction.PickupResult.ADDED) {
			resolveEquippedFirearm();
			playSfx(
					Assets.Sounds.Bukov.LOOT_PICKUP,
					0.68f,
					nextAudioPitch(1f, 0.03f)
			);
			RaidItem carried = carried(item.bukovItemUid());
			if (carried != null) {
				if (medicalSystem != null) {
					medicalSystem.track(carried);
				}
				showHeroStatus(
						"拾取 "
								+ item.name()
								+ " · "
								+ formatWeight(carried.totalWeight())
								+ "kg · "
								+ carried.totalValue()
				);
			}
			if (carried != null
					&& carried.definitionId().startsWith("firearm:")) {
				showTutorial(BukovTutorialEvent.FIREARM_PICKUP);
			}
			if (FirstRaidMission.isArchive(carried)) {
				unlockMissionGateIfCarried();
			}
			checkpointLootChange();
		} else if (result == LootTransaction.PickupResult.OVERWEIGHT) {
			showTutorial(BukovTutorialEvent.OVERWEIGHT);
			showHeroStatus("负重已满");
		} else {
			showHeroStatus("该物品已拾取");
		}
	}

	private void dropLatestCarriedItem() {
		List<RaidItem> carried = raid.loot().items();
		if (carried.isEmpty()) {
			showHeroStatus("没有可丢弃物品");
			return;
		}
		RaidItem latest = carried.get(carried.size() - 1);
		dropCarriedItem(latest.itemUid());
	}

	public BukovHeapLootAdapter.DropResult dropCarriedItem(String itemUid) {
		RaidItem carriedItem = carried(itemUid);
		if (carriedItem == null) {
			showHeroStatus("物品已不在背包");
			return BukovHeapLootAdapter.DropResult.UNKNOWN_UID;
		}
		if (FirstRaidMission.isArchive(carriedItem)) {
			showHeroStatus("维修档案是任务物品，不能丢弃");
			return BukovHeapLootAdapter.DropResult.PROTECTED_ITEM;
		}
		Heap heap = Dungeon.level.heaps.get(hero.pos);
		boolean created = false;
		if (heap == null) {
			heap = new Heap();
			heap.pos = hero.pos;
			heap.seen = true;
			created = true;
		} else if (heap.type != Heap.Type.HEAP) {
			showHeroStatus("这里无法丢弃");
			return BukovHeapLootAdapter.DropResult.UNKNOWN_UID;
		}

		Item hostItem = lootAdapter.carriedHostItem(itemUid);
		BukovHeapLootAdapter.DropResult dropResult =
				lootAdapter.drop(itemUid, heap, hero);
		if (dropResult != BukovHeapLootAdapter.DropResult.DROPPED) {
			showHeroStatus(
					dropResult == BukovHeapLootAdapter.DropResult.PROTECTED_ITEM
							? "任务物品不能丢弃"
							: dropResult
									== BukovHeapLootAdapter.DropResult.IN_USE_ITEM
									? "治疗中的物品不能丢弃"
									: "丢弃失败");
			return dropResult;
		}
		if (created) {
			Dungeon.level.heaps.put(hero.pos, heap);
			GameScene.add(heap);
		}
		showHeroStatus(
				"丢弃 "
						+ (hostItem == null
								? carriedItem.definitionId()
								: hostItem.name())
		);
		checkpointLootChange();
		return BukovHeapLootAdapter.DropResult.DROPPED;
	}

	private RaidItem carried(String itemUid) {
		for (RaidItem item : raid.loot().items()) {
			if (item.itemUid().equals(itemUid)) {
				return item;
			}
		}
		return null;
	}

	private boolean carriesMissionArchive() {
		for (RaidItem item : raid.loot().items()) {
			if (FirstRaidMission.isArchive(item)) return true;
		}
		return false;
	}

	private boolean hasMaintenanceKey() {
		return raid != null && raid.loot().containsDefinition(
				BukovFirstRaidLootTables.MAINTENANCE_KEY_DEFINITION_ID);
	}

	private static boolean isMaintenanceCache(
			BukovRaidCoordinator.ContainerSnapshot container) {
		return container != null
				&& BukovFirstRaidLootTables.MAINTENANCE_CACHE_CONTAINER_ID
						.equals(container.containerId)
				&& BukovFirstRaidLootTables.MAINTENANCE_CACHE
						.equals(container.lootTableId);
	}

	private boolean unlockMaintenanceCache(
			BukovRaidCoordinator.ContainerSnapshot container) {
		if (!isMaintenanceCache(container)
				|| container.state
						!= BukovSearchableContainer.State.LOCKED) {
			return false;
		}
		String consumedKeyUid = raid.loot().firstItemUidForDefinition(
				BukovFirstRaidLootTables.MAINTENANCE_KEY_DEFINITION_ID);
		BukovKeyDoorState.UnlockResult result =
				raid.session().keyDoors().unlock(
						BukovFirstRaidLootTables
								.MAINTENANCE_CACHE_DOOR_ID,
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID,
						raid.loot());
		if (result == BukovKeyDoorState.UnlockResult.KEY_MISSING) {
			showHeroStatus("需要维修钥匙");
			return false;
		}
		if (result == BukovKeyDoorState.UnlockResult.UNLOCKED
				&& lootAdapter != null) {
			lootAdapter.reconcileConsumedRuntimeItem(
					consumedKeyUid,
					hero);
		}
		boolean containerChanged = raid.unlockContainer(
				container.containerId);
		BukovRaidCoordinator.ContainerSnapshot current =
				raid.container(container.containerId);
		if (current == null || current.state
				== BukovSearchableContainer.State.LOCKED) {
			throw new IllegalStateException(
					"Maintenance key ledger unlocked without its container");
		}
		if (result == BukovKeyDoorState.UnlockResult.UNLOCKED
				|| containerChanged) {
			checkpointLootChange();
		}
		showHeroStatus("维修柜已解锁，按E搜索");
		return true;
	}

	private boolean unlockMissionGateIfCarried() {
		if (missionGateUnlocked) return true;
		if (raid == null || !carriesMissionArchive()) return false;
		try {
			raid.completeEvent(FirstRaidMission.EVENT_ID);
			if (!raid.eventCompleted(FirstRaidMission.EVENT_ID)) {
				return false;
			}
			missionGateUnlocked = true;
			applyMissionGateTerrain();
			playSfx(
					Assets.Sounds.Bukov.GATE_UNLOCK,
					0.9f,
					1f
			);
			showHeroStatus("档案验证成功，维修通道已开放");
			return true;
		} catch (IOException failure) {
			ShatteredPixelDungeon.reportException(failure);
			missionEventRetryCooldown = 2f;
			showHeroStatus("档案已取得，但通道状态保存失败，正在重试");
			return false;
		}
	}

	private void checkpointLootChange() {
		writeBackCarriedRuntimeItems();
		if (persistence == null || persistence.criticalStateChanged()) {
			return;
		}
		Throwable failure = persistence.lastFailure();
		if (failure != null) {
			ShatteredPixelDungeon.reportException(failure);
			showHeroStatus("检查点保存失败");
		}
	}

	private void recordEnemyKill() {
		if (raid == null) {
			transientKillCount++;
			return;
		}
		raid.session().recordKill();
		checkpointRuntimeCombatState();
	}

	private void checkpointRuntimeCombatState() {
		if (raid == null) return;
		try {
			publishRealtimeState();
			raid.saveCheckpoint();
		} catch (IOException failure) {
			// The in-memory session stays authoritative and the normal scene
			// lifecycle will retry the same checkpoint without rerolling state.
			ShatteredPixelDungeon.reportException(failure);
		}
	}

	public void writeBackCarriedRuntimeItems() {
		if (medicalSystem != null && raid != null
				&& !medicalSystem.closed()) {
			medicalSystem.writeBack(raid.loot());
		}
		if (lootAdapter != null) {
			lootAdapter.syncRuntimeState(hero);
		}
		publishRealtimeState();
	}

	private void publishRealtimeState() {
		if (raid == null || medicalStatus == null
				|| medicalSystem == null || raid.finished()) {
			return;
		}
		ArrayList<BukovRaidCheckpoint.EnemyRuntimeState> snapshots =
				new ArrayList<>();
		for (EnemyRuntime enemy : enemies) {
			if (enemy == null
					|| enemy.bossState != null
					|| !enemy.mob.isAlive()
					|| enemy.brain.state()
							== RealtimeEnemyBrain.State.DEAD) {
				continue;
			}
			snapshots.add(new BukovRaidCheckpoint.EnemyRuntimeState(
					enemy.stableId,
					enemy.definition == null ? "" : enemy.definition.id,
					enemy.brain.snapshot(),
					enemy.rangedCombat == null
							? null : enemy.rangedCombat.snapshot()));
		}
		raid.updateRealtimeState(
				medicalStatus,
				medicalSystem.snapshot(),
				snapshots);
	}

	public void finishMedicalRuntime() {
		if (medicalSystem != null && raid != null
				&& !medicalSystem.closed()) {
			medicalSystem.finishRaid(raid.loot());
		}
	}

	private void beginBestAvailableMedical() {
		RealtimeMedicalSystem.BeginResult finalResult =
				RealtimeMedicalSystem.BeginResult.UNKNOWN_ITEM;
		for (RaidItem item : raid.loot().items()) {
			if (MedicalCatalog.find(item.definitionId()) == null) continue;
			finalResult = beginMedical(item.itemUid());
			if (finalResult
					== RealtimeMedicalSystem.BeginResult.STARTED) {
				return;
			}
			if (finalResult == RealtimeMedicalSystem.BeginResult.BUSY
					|| finalResult
							== RealtimeMedicalSystem.BeginResult.COOLDOWN) {
				break;
			}
		}
		showHeroStatus(finalResult == RealtimeMedicalSystem.BeginResult.NO_EFFECT
				? "当前状态无需治疗"
				: "没有可用医疗品");
	}

	public RealtimeMedicalSystem.BeginResult beginMedical(String itemUid) {
		if (medicalSystem == null || raid == null) {
			return RealtimeMedicalSystem.BeginResult.UNKNOWN_ITEM;
		}
		RealtimeMedicalSystem.BeginResult result =
				medicalSystem.beginUse(itemUid);
		if (result == RealtimeMedicalSystem.BeginResult.STARTED) {
			lastMedicalCountdown = Integer.MIN_VALUE;
			combatPresentation.emit(
					CombatPresentationEvent.Type.PLAYER_MEDICAL_START,
					hero.id(),
					hero.id(),
					hero.pos,
					hero.pos,
					null,
					1f);
			RaidItem item = carried(itemUid);
			showHeroStatus("开始使用 "
					+ (item == null
					? "医疗品"
					: medicalDisplayName(item)));
		}
		return result;
	}

	public boolean equipCarriedFirearm(String itemUid) {
		if (lootAdapter == null || itemUid == null) {
			return false;
		}
		Item hostItem = lootAdapter.carriedHostItem(itemUid);
		if (!(hostItem instanceof Firearm)) {
			showHeroStatus("该物品不是可装备枪械");
			return false;
		}
		Firearm next = (Firearm)hostItem;
		resolveEquippedFirearm();
		if (next == equippedFirearm) {
			showHeroStatus("该武器已经装备");
			return false;
		}
		Firearm previous = equippedFirearm;
		if (previous != null
				&& !hero.belongings.backpack.items.contains(previous)) {
			hero.belongings.backpack.items.add(previous);
		}
		hero.belongings.backpack.items.remove(next);
		hero.belongings.weapon = next;
		next.activate(hero);
		fireControl.resetForWeaponSwap();
		resolveEquippedFirearm();
		showHeroStatus("已装备 " + equippedDefinition.name);
		checkpointLootChange();
		return true;
	}

	private void handleMedicalStepResult(
			RealtimeMedicalSystem.StepResult result,
			String activeUid) {
		switch (result) {
			case IN_PROGRESS:
				int percent = Math.min(
						99,
						Math.max(
								1,
								Math.round(
										medicalSystem.useProgress() * 100f)));
				int bucket = percent / 10;
				if (bucket != lastMedicalCountdown) {
					lastMedicalCountdown = bucket;
					showHeroStatus("治疗中 " + percent + "%");
				}
				break;
			case COMPLETED:
				emitMedicalEnded();
				medicalSystem.writeBack(raid.loot());
				syncMedicalHostQuantity(activeUid);
				showHeroStatus("治疗完成");
				lastMedicalCountdown = Integer.MIN_VALUE;
				checkpointLootChange();
				break;
			case INTERRUPTED_DAMAGE:
				emitMedicalEnded();
				showHeroStatus("治疗被受击中断");
				break;
			case INTERRUPTED_MOVE:
				emitMedicalEnded();
				showHeroStatus("治疗被移动中断");
				break;
			case INTERRUPTED_SHOT:
				emitMedicalEnded();
				showHeroStatus("治疗被射击中断");
				break;
			case CANCELED_NO_EFFECT:
			case DEAD:
			case CLOSED:
				emitMedicalEnded();
				break;
			default:
				break;
		}
	}

	private void syncMedicalHostQuantity(String itemUid) {
		if (itemUid == null) return;
		RaidItem carried = raid.loot().item(itemUid);
		for (int index = hero.belongings.backpack.items.size() - 1;
				index >= 0; index--) {
			Item item = hero.belongings.backpack.items.get(index);
			if (!itemUid.equals(item.bukovItemUid())) continue;
			if (carried == null) {
				hero.belongings.backpack.items.remove(index);
			} else {
				item.quantity(carried.quantity());
			}
			break;
		}
		if (lootAdapter != null) {
			lootAdapter.reconcileConsumedRuntimeItem(itemUid, hero);
		}
	}

	private static String medicalDisplayName(RaidItem item) {
		String id = item.definitionId();
		if ("bandage".equals(id)) return "绷带";
		if ("first_aid".equals(id)) return "急救包";
		if ("tourniquet".equals(id)) return "止血带";
		if ("painkiller".equals(id)) return "止痛药";
		if ("antiseptic".equals(id)) return "消毒剂";
		if ("splint".equals(id)) return "夹板";
		if ("stim".equals(id)) return "战地注射器";
		return id;
	}

	private int[] resolveMissionGateCells() {
		if (!(Dungeon.level instanceof BukovLevel)) return new int[0];
		BukovRaidLayout.MissionGate gate =
				((BukovLevel)Dungeon.level).missionGate();
		if (gate == null || gate.gateCells == null) return new int[0];
		return gate.gateCells.clone();
	}

	private void applyMissionGateTerrain() {
		int desired = missionGateUnlocked
				? Terrain.OPEN_DOOR : Terrain.LOCKED_DOOR;
		boolean changed = false;
		for (int cell : missionGateCells) {
			if (cell < 0 || cell >= Dungeon.level.length()
					|| Dungeon.level.map[cell] == desired) {
				continue;
			}
			Level.set(cell, desired, Dungeon.level);
			GameScene.updateMap(cell);
			changed = true;
		}
		if (!changed) return;
		Dungeon.level.updateFieldOfView(
				hero, Dungeon.level.heroFOV);
		GameScene.updateFog();
	}

	private boolean movementWasBlocked(float requestedX, float requestedY) {
		float actualX = heroBody.x - heroBody.previousX;
		float actualY = heroBody.y - heroBody.previousY;
		return Math.abs(requestedX - actualX) > 0.0001f
				|| Math.abs(requestedY - actualY) > 0.0001f;
	}

	private boolean movementPointsTowardMissionGate(
			float requestedX, float requestedY) {
		if (missionGateCell < 0
				|| requestedX == 0f && requestedY == 0f) {
			return false;
		}
		int width = Dungeon.level.width();
		float gateX = missionGateCell % width + 0.5f;
		float gateY = missionGateCell / width + 0.5f;
		float toGateX = gateX - heroBody.previousX;
		float toGateY = gateY - heroBody.previousY;
		return toGateX * toGateX + toGateY * toGateY <= 3.0625f
				&& requestedX * toGateX + requestedY * toGateY > 0f;
	}

	static int selectVisibleLootHeap(
			int heroCell,
			int width,
			int length,
			boolean[] visible,
			SparseArray<Heap> heaps,
			int excludedCell) {
		if (heroCell < 0
				|| heroCell >= length
				|| width <= 0
				|| visible == null
				|| visible.length < length
				|| heaps == null) {
			return -1;
		}
		int heroX = heroCell % width;
		int heroY = heroCell / width;
		for (int radius = 0; radius <= 1; radius++) {
			for (int deltaY = -radius; deltaY <= radius; deltaY++) {
				for (int deltaX = -radius; deltaX <= radius; deltaX++) {
					if (radius == 1
							&& Math.max(Math.abs(deltaX), Math.abs(deltaY)) != 1) {
						continue;
					}
					int x = heroX + deltaX;
					int y = heroY + deltaY;
					if (x < 0 || x >= width || y < 0) {
						continue;
					}
					int cell = x + y * width;
					if (cell < 0
							|| cell >= length
							|| cell == excludedCell
							|| !visible[cell]) {
						continue;
					}
					Heap heap = heaps.get(cell);
					if (heap != null
							&& heap.type == Heap.Type.HEAP
							&& heap.peek() != null) {
						return cell;
					}
				}
			}
		}
		return -1;
	}

	private static String formatWeight(float weight) {
		return String.format(java.util.Locale.ROOT, "%.2f", weight);
	}

	@Override
	public void updateCameraAndHud(float dt) {
		tutorialRemaining = Math.max(0f, tutorialRemaining - dt);
		if (tutorialRemaining <= 0f) {
			tutorialEvent = null;
		}
		// Camera tracking happens after render interpolation so it follows the
		// same visual position that is drawn, rather than the last fixed step.
	}

	@Override
	public void endFixedStep() {
		if (raid == null || extractionCompleteCuePlayed) return;
		for (ExtractionState extraction : raid.extractions()) {
			if (!extraction.completed()) continue;
			extractionCompleteCuePlayed = true;
			playSfx(
					Assets.Sounds.Bukov.EXTRACTION_COMPLETE,
					0.88f,
					1f);
			combatPresentation.emit(
					CombatPresentationEvent.Type.EXTRACTION_COMPLETE,
					hero.id(),
					hero.id(),
					hero.pos,
					hero.pos,
					CombatFeedbackType.EXTRACT_STAMP,
					1f);
			return;
		}
	}

	@Override
	public void renderInterpolate(float alpha) {
		float clamped = Math.max(0f, Math.min(1f, alpha));
		if (hero.sprite != null) {
			float renderX = interpolate(
					heroBody.previousX,
					heroBody.x,
					clamped
			);
			float renderY = interpolate(
					heroBody.previousY,
					heroBody.y,
					clamped
			);
			hero.sprite.place(renderX, renderY);
			if (hero.sprite instanceof HeroSprite) {
				float aimX = inputFrame == null ? 0f : inputFrame.aim.x;
				float aimY = inputFrame == null ? 0f : inputFrame.aim.y;
				((HeroSprite)hero.sprite).setBukovRealtimeOrientation(
						heroBody.velocityX,
						heroBody.velocityY,
						aimX,
						aimY
				);
			}
			hero.sprite.setRealtimeMoving(moving);
			if (inputFrame != null
					&& (inputFrame.aim.x != 0f || inputFrame.aim.y != 0f)) {
				int aimX = (int)Math.floor(heroBody.x + inputFrame.aim.x);
				int aimY = (int)Math.floor(heroBody.y + inputFrame.aim.y);
				hero.sprite.turnTo(
						hero.pos,
						aimX + aimY * Dungeon.level.width()
				);
			}
			updateRealtimeCamera(Game.elapsed);
		}

		for (EnemyRuntime enemy : enemies) {
			if (!enemy.body.active || enemy.mob.sprite == null) {
				continue;
			}
			enemy.mob.sprite.place(
					interpolate(
							enemy.body.previousX,
							enemy.body.x,
							clamped
					),
					interpolate(
							enemy.body.previousY,
							enemy.body.y,
							clamped
					)
			);
			enemy.mob.sprite.setRealtimeMoving(enemy.moving);
			if (enemy.brain.state() != RealtimeEnemyBrain.State.IDLE) {
				enemy.mob.sprite.turnTo(enemy.mob.pos, hero.pos);
			}
			if (enemy.bossState != null
					&& enemy.mob.sprite instanceof BukovWhiteLineSprite) {
				((BukovWhiteLineSprite)enemy.mob.sprite).setEncounterVisual(
						bossPhase(enemy.bossState.phase()),
						enemy.bossState.vulnerable()
				);
			}
		}
	}

	private void updateRealtimeCamera(float renderDelta) {
		Camera camera = Camera.main;
		if (camera == null || hero.sprite == null) {
			return;
		}

		// Scene creation leaves a legacy panTo active. A zero shift cancels
		// that panner without changing the current center, so it cannot pull
		// against the realtime follower on the next Camera.updateAll pass.
		camera.shift(ZERO_CAMERA_SHIFT);
		if (!cameraFollow.initialized()) {
			// Track the focus coordinate separately from the UI center offset.
			// Camera shake is applied later by Camera.updateMatrix and remains
			// untouched by this scroll update.
			cameraFollow.reset(
					camera.scroll.x + camera.width * 0.5f
							- camera.centerOffset.x,
					camera.scroll.y + camera.height * 0.5f
							- camera.centerOffset.y
			);
		}
		float lookAheadPixels = inputFrame == null
				? 0f
				: inputFrame.cameraLookAheadTiles * DungeonTilemap.SIZE;
		cameraFollow.update(
				hero.sprite.x + hero.sprite.width() * 0.5f,
				hero.sprite.y + hero.sprite.height() * 0.5f,
				inputFrame == null ? 0f : inputFrame.aim.x * lookAheadPixels,
				inputFrame == null ? 0f : inputFrame.aim.y * lookAheadPixels,
				renderDelta
		);
		camera.scroll.set(
				cameraFollow.centerX() - camera.width * 0.5f
						+ camera.centerOffset.x,
				cameraFollow.centerY() - camera.height * 0.5f
						+ camera.centerOffset.y
		);
	}

	/**
	 * Render-space focus consumed by the scene's clamp/alignment guard.
	 *
	 * This deliberately exposes no simulation body state: the returned point is
	 * the already-smoothed presentation camera target, including aim look-ahead.
	 */
	public float presentationCameraFocusX() {
		return cameraFollow.initialized()
				? cameraFollow.centerX()
				: hero.sprite.x + hero.sprite.width() * 0.5f;
	}

	public float presentationCameraFocusY() {
		return cameraFollow.initialized()
				? cameraFollow.centerY()
				: hero.sprite.y + hero.sprite.height() * 0.5f;
	}

	@Override
	public void disposeRealtimeObjects() {
		input.stop();
		inputFrame = null;
		fireControl.resetForWeaponSwap();
		targetBodies.clear();
		enemyShotTargetBodies.clear();
		charsByBody.clear();
		pendingHits.clear();
		pendingEnemyShots.clear();
		combatFx.clear();
		combatPresentation.clear();
		enemies.clear();
		enemiesByMob.clear();
		pendingEnemyAttacks.clear();
		for (BukovInteractionMarker marker : interactionMarkers) {
			marker.killAndErase();
		}
		interactionMarkers.clear();
		bossMechanismMarkers.clear();
	}

	@Override
	public void fire(Firearm firearm, FirearmDefinition definition) {
		firedShotThisStep = true;
		Item.updateQuickslot();
		AmmoDefinition ammunition = ammoRegistry.require(
				firearm.loadedAmmoDefinitionId(definition)
		);
		emitPlayerSound(
				Math.max(
						1f,
						definition.noiseRadiusTiles
								* ammunition.noiseMultiplier
								* (equippedGear == null
										? 1f
										: equippedGear.noiseMultiplier())));
		playPlayerGunshot(
				definition,
				Math.max(
						0.22f,
						Math.min(
								1.5f,
								0.85f
										* ammunition.noiseMultiplier
										* definition.soundGain)),
				definition.soundPitch);
		if (inputFrame == null
				|| (inputFrame.aim.x == 0f && inputFrame.aim.y == 0f)) {
			return;
		}
		combatPresentation.emit(
				CombatPresentationEvent.Type.PLAYER_FIRE,
				hero.id(),
				-1,
				hero.pos,
				playerAimCell(),
				CombatFeedbackType.RIFLE_SHOT,
				definition.feedbackIntensity);

		int fxSequence = playerFxSequence++;
		combatFx.muzzle(
				hero.id(),
				fxSequence,
				false,
				heroBody.x,
				heroBody.y,
				inputFrame.aim.x,
				inputFrame.aim.y,
				definition.muzzleIntensity
		);
		combatFx.shell(
				hero.id(),
				fxSequence,
				false,
				heroBody.x,
				heroBody.y,
				-inputFrame.aim.y,
				inputFrame.aim.x,
				definition.muzzleIntensity
		);
		float spread = moving
				? definition.movingSpreadDeg
				: definition.baseSpreadDeg;
		spread += fireControl.recoilSpreadDeg();
		for (int pellet = 0; pellet < definition.pellets; pellet++) {
			float radians = (float)Math.toRadians(
					Random.Float(-spread, spread)
			);
			float cos = (float)Math.cos(radians);
			float sin = (float)Math.sin(radians);
			float directionX = inputFrame.aim.x * cos - inputFrame.aim.y * sin;
			float directionY = inputFrame.aim.x * sin + inputFrame.aim.y * cos;

			HitscanResolver.cast(
					heroBody.x,
					heroBody.y,
					directionX,
					directionY,
					definition.effectiveRangeTiles * 2f,
					collisionMap,
					targetQuery,
					heroBody,
					shotHit
			);
			combatFx.tracer(
					hero.id(),
					fxSequence,
					false,
					heroBody.x,
					heroBody.y,
					shotHit.x,
					shotHit.y,
					definition.tracerIntensity
			);
			// The endpoint is meaningful even when the ray stops on geometry:
			// without this wall spark, misses looked like the round vanished.
			combatFx.impact(
					hero.id(),
					fxSequence,
					false,
					shotHit.x,
					shotHit.y,
					definition.impactIntensity
			);
			Char target = charsByBody.get(shotHit.body);
			if (target != null && target.isAlive()) {
				float damage = RealtimeDamage.resolve(
						ammunition.applyDamage(definition.damage),
						1f,
						shotHit.distance,
						definition.effectiveRangeTiles,
						ammunition.applyPenetration(definition.penetration),
						RealtimeDamage.HitZone.CORE,
						null
				);
				pendingHits.add(new PendingHit(target, damage));
			}
		}
	}

	@Override
	public FireControl.AmmoSelection requestAmmo(
			String caliber,
			String preferredDefinitionId,
			int maximum,
			boolean allowAlternative) {
		if (caliber == null || maximum <= 0) {
			return FireControl.AmmoSelection.none();
		}
		String selectedDefinitionId = null;
		int loaded = 0;
		for (int pass = 0; pass < (allowAlternative ? 2 : 1); pass++) {
			for (AmmoStack stack : hero.belongings.getAllItems(AmmoStack.class)) {
				String definitionId = stack.definitionId();
				boolean preferred = preferredDefinitionId != null
						&& preferredDefinitionId.equals(definitionId);
				if ((pass == 0 && !preferred)
						|| (pass == 1 && preferred)
						|| !ammoRegistry.compatible(definitionId, caliber)
						|| (selectedDefinitionId != null
						&& !selectedDefinitionId.equals(definitionId))) {
					continue;
				}
				if (selectedDefinitionId == null) {
					selectedDefinitionId = definitionId;
				}
				loaded += stack.takeUpTo(maximum - loaded);
				if (stack.quantity() <= 0) {
					stack.detachAll(hero.belongings.backpack);
				}
				if (loaded >= maximum) {
					break;
				}
			}
			if (loaded >= maximum) {
				break;
			}
		}
		Item.updateQuickslot();
		return loaded == 0
				? FireControl.AmmoSelection.none()
				: new FireControl.AmmoSelection(selectedDefinitionId, loaded);
	}

	@Override
	public void dryFire() {
		playSfx(Assets.Sounds.Bukov.DRY_FIRE, 0.75f, 1f);
		showTutorial(BukovTutorialEvent.EMPTY_MAGAZINE);
		showHeroStatus("空仓");
	}

	@Override
	public void reloadStarted(float seconds) {
		combatPresentation.emit(
				CombatPresentationEvent.Type.PLAYER_RELOAD,
				hero.id(),
				hero.id(),
				hero.pos,
				playerAimCell(),
				null,
				1f);
		showHeroStatus("换弹");
	}

	@Override
	public void reloadAudioCues(
			FirearmDefinition definition,
			int cueMask) {
		float baseGain = Math.max(
				0.30f,
				Math.min(0.78f, 0.54f * definition.soundGain));
		float basePitch = Math.max(
				0.72f,
				Math.min(1.28f, definition.soundPitch));
		for (ReloadAudioCue cue : ReloadAudioCue.values()) {
			if (!ReloadAudioCueResolver.contains(cueMask, cue)) {
				continue;
			}
			float cueGain = cue == ReloadAudioCue.MAG_OUT
					? 0.88f
					: cue == ReloadAudioCue.MAG_IN ? 1f : 1.08f;
			float cuePitch = cue == ReloadAudioCue.MAG_OUT
					? 0.96f
					: cue == ReloadAudioCue.MAG_IN ? 1f : 1.03f;
			playSfx(
					cue.asset(),
					baseGain * cueGain,
					basePitch * cuePitch);
		}
	}

	@Override
	public void reloadFinished() {
		if (equippedFirearm != null) {
			if (equippedFirearm.magazineAmmo() == 0) {
				showHeroStatus("没有备用弹药");
			} else {
				showHeroStatus("弹匣 " + equippedFirearm.magazineAmmo());
			}
		}
	}

	private void resolveEquippedFirearm() {
		KindOfWeapon weapon = hero.belongings.weapon();
		if (weapon instanceof Firearm) {
			Firearm firearm = (Firearm)weapon;
			if (firearm != equippedFirearm) {
				fireControl.resetForWeaponSwap();
				equippedFirearm = firearm;
				equippedDefinition = firearm.definition(firearmRegistry);
			}
		} else {
			if (equippedFirearm != null) {
				fireControl.resetForWeaponSwap();
			}
			equippedFirearm = null;
			equippedDefinition = null;
		}
	}

	private int reserveAmmo(String caliber) {
		int total = 0;
		for (AmmoStack stack : hero.belongings.getAllItems(AmmoStack.class)) {
			if (ammoRegistry.compatible(stack.definitionId(), caliber)) {
				total += stack.quantity();
			}
		}
		return total;
	}

	private void spawnInitialEnemies() {
		if (raid == null || raid.session().initialEnemySpawnCompleted()) return;
		for (int i = 0; i < raidMode.initialEnemyCount; i++) {
			if (!attemptEnemySpawn()) break;
		}
		// Empty maps and fully rejected spawn plans are completed too. Otherwise
		// every resume would retry and eventually duplicate or reroll enemies.
		raid.session().markInitialEnemySpawnCompleted();
		checkpointRuntimeCombatState();
	}

	private void updateEnemySpawning() {
		if (raid == null
				|| raid.session().elapsedSeconds < nextEnemySpawnSeconds) {
			return;
		}
		nextEnemySpawnSeconds = nextSpawnBoundary(
				raid.session().elapsedSeconds);
		boolean spawned = attemptEnemySpawn();
		checkpointRuntimeCombatState();
		if (spawned) {
			refreshEnemiesAndTargets();
		}
	}

	private boolean attemptEnemySpawn() {
		if (enemySpawnPoints.isEmpty()) return false;
		float elapsed = raid.session().elapsedSeconds;
		long spawnEpoch = raid.session().claimEnemySpawnEpoch();
		if (attemptWhiteLineSpawn(elapsed)) return true;
		int start = (int)com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(
				Dungeon.seed + spawnEpoch * 0x9E3779B97F4A7C15L,
				enemySpawnPoints.size());
		for (int offset = 0; offset < enemySpawnPoints.size(); offset++) {
			BukovEnemySpawnPlanner.SpawnPoint point =
					enemySpawnPoints.get((start + offset)
							% enemySpawnPoints.size());
			if (point.cell < 0
					|| point.cell >= Dungeon.level.length()
					|| Dungeon.level.heroFOV[point.cell]
					|| Actor.findChar(point.cell) != null
					|| tooCloseToHero(point.cell)
					|| (point.bossArena && whiteLineResolved())) {
				continue;
			}
			if (!point.bossArena
					&& activeNonBossEnemies()
							>= raidMode.maximumActiveEnemiesAt(elapsed)) {
				continue;
			}
			EnemyArchetypeDefinition selected;
			if (point.bossArena) {
				selected = enemyArchetypes.require(
						FirstRaidEnemySpawnDirector.FIRST_BOSS);
				float bossEarliest = raidMode.bossEarliestSeconds;
				if (raid.session().raidOrdinal() == 1
						&& raidMode != BukovRaidMode.BOSS_CONTRACT) {
					bossEarliest = Math.max(
							bossEarliest,
							selected.firstRaidMinimumSeconds);
				}
				if (!raidMode.bossEnabled
						|| elapsed < bossEarliest
						|| raid.session().firstRaidProtectionActive()) {
					continue;
				}
				if (activeEnemyCount(selected.id) > 0) {
					continue;
				}
			} else {
				boolean firstRaid = raid.session().raidOrdinal() == 1;
				FirstRaidEnemySpawnDirector.Context context =
					new FirstRaidEnemySpawnDirector.Context(
							elapsed,
							firstRaid,
							point.distanceFromSpawnRooms,
							false,
							point.mandatorySingleRoute,
							point.bossArena);
				selected = FirstRaidEnemySpawnDirector
						.selectFirstRaidMilestone(
								enemyArchetypes.all(),
								context,
								this::activeEnemyCount);
				if (selected == null) {
					selected = FirstRaidEnemySpawnDirector.select(
							enemyArchetypes.all(),
							context,
							this::activeEnemyCount,
							Dungeon.seed ^ point.cell ^ (spawnEpoch + 1L),
							new FirstRaidEnemySpawnDirector.SpawnWeights() {
								@Override
								public int weight(
										EnemyArchetypeDefinition definition) {
									return raidTheme == null
											? definition.spawnWeight
											: raidTheme.adjustedEnemyWeight(
													definition.id,
													definition.spawnWeight);
								}
							});
				}
			}
			if (selected == null
					|| (selected.tier == EnemyTier.BOSS
							&& whiteLineResolved())) {
				continue;
			}
			BukovHostMob mob = new BukovHostMob().configure(selected);
			mob.pos = point.cell;
			mob.state = mob.WANDERING;
			GameScene.add(mob);
			return true;
		}
		return false;
	}

	private boolean attemptWhiteLineSpawn(float elapsed) {
		if (!raidMode.bossEnabled || whiteLineResolved()) return false;
		EnemyArchetypeDefinition boss = enemyArchetypes.require(
				FirstRaidEnemySpawnDirector.FIRST_BOSS);
		float earliest = raidMode.bossEarliestSeconds;
		if (raid.session().raidOrdinal() == 1
				&& raidMode != BukovRaidMode.BOSS_CONTRACT) {
			earliest = Math.max(
					earliest,
					boss.firstRaidMinimumSeconds);
		}
		if (elapsed < earliest
				|| raid.session().firstRaidProtectionActive()
				|| activeEnemyCount(boss.id) > 0) {
			return false;
		}
		for (BukovEnemySpawnPlanner.SpawnPoint point :
				enemySpawnPoints) {
			if (!point.bossArena
					|| point.cell < 0
					|| point.cell >= Dungeon.level.length()
					|| Dungeon.level.heroFOV[point.cell]
					|| Actor.findChar(point.cell) != null
					|| tooCloseToHero(point.cell)) {
				continue;
			}
			BukovHostMob mob = new BukovHostMob().configure(boss);
			mob.pos = point.cell;
			mob.state = mob.WANDERING;
			GameScene.add(mob);
			return true;
		}
		return false;
	}

	private int activeEnemyCount(String definitionId) {
		int count = 0;
		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof BukovHostMob
					&& definitionId.equals(
							((BukovHostMob)mob).definitionId())
					&& mob.isAlive()) {
				count++;
			}
		}
		return count;
	}

	private int activeNonBossEnemies() {
		int count = 0;
		for (Mob mob : Dungeon.level.mobs) {
			EnemyArchetypeDefinition definition = definitionFor(mob);
			if (definition != null
					&& definition.tier != EnemyTier.BOSS
					&& mob.isAlive()) {
				count++;
			}
		}
		return count;
	}

	private EnemyArchetypeDefinition definitionFor(Mob mob) {
		if (mob instanceof BukovHostMob) {
			return enemyArchetypes.require(
					((BukovHostMob)mob).definitionId());
		}
		for (EnemyArchetypeDefinition definition : enemyArchetypes.all()) {
			if (definition.hostClassHint.equals(
					mob.getClass().getSimpleName())) {
				return definition;
			}
		}
		return null;
	}

	private boolean tooCloseToHero(int cell) {
		int width = Dungeon.level.width();
		float deltaX = cell % width - heroBody.x;
		float deltaY = cell / width - heroBody.y;
		return deltaX * deltaX + deltaY * deltaY
				< MINIMUM_PLAYER_SPAWN_DISTANCE_TILES
						* MINIMUM_PLAYER_SPAWN_DISTANCE_TILES;
	}

	private float nextSpawnBoundary(float elapsedSeconds) {
		float interval = themedSpawnInterval(
				raidMode.spawnIntervalAt(elapsedSeconds), raidTheme);
		return ((float)Math.floor(
				elapsedSeconds / interval) + 1f) * interval;
	}

	private void applyModeConvergence() {
		float elapsed = raid.session().elapsedSeconds;
		if (raidMode.convergenceStarted(elapsed)) {
			ExtractionState conditional =
					raid.extraction(CONDITIONAL_EXTRACTION_ID);
			if (conditional != null && !conditional.conditionMet()) {
				raid.setExtractionCondition(
						CONDITIONAL_EXTRACTION_ID, true);
				checkpointLootChange();
			}
			if (!modeConvergenceAnnounced) {
				modeConvergenceAnnounced = true;
				showHeroStatus("行动进入收束阶段 · 额外撤离点已开放");
			}
		}
		if (raidMode.overtime(elapsed) && !modeOvertimeAnnounced) {
			modeOvertimeAnnounced = true;
			showHeroStatus("行动超时 · 敌方增援压力上升");
		}
	}

	static float themedSpawnInterval(
			float baselineSeconds, ThemeDefinition theme) {
		return theme == null
				? baselineSeconds
				: theme.pressureAdjustedSeconds(baselineSeconds);
	}

	private static ThemeDefinition resolveRaidTheme() {
		if (!(Dungeon.level instanceof BukovLevel)) return null;
		BukovRaidLayout layout = ((BukovLevel)Dungeon.level).raidLayout();
		if (layout == null || layout.themeId == null
				|| layout.themeId.isEmpty()) {
			return null;
		}
		ThemeRegistry registry = new ThemeRegistry();
		registry.loadDefault();
		return registry.require(layout.themeId);
	}

	private boolean whiteLineResolved() {
		return Dungeon.level instanceof BukovLevel
				&& ((BukovLevel)Dungeon.level).whiteLineResolved();
	}

	private void refreshEnemiesAndTargets() {
		for (EnemyRuntime enemy : enemies) {
			enemy.present = false;
		}
		for (Mob mob : Dungeon.level.mobs) {
			if (mob == null
					|| !mob.isAlive()
					|| mob.alignment != Char.Alignment.ENEMY) {
				continue;
			}
			EnemyRuntime enemy = enemiesByMob.get(mob);
			if (enemy == null) {
				EnemyArchetypeDefinition definition =
						definitionFor(mob);
				enemy = new EnemyRuntime(
						mob,
						definition,
						raid == null
								? null
								: raid.enemyRuntime(mob.id()),
						collisionMap.width(),
						collisionMap.height());
				enemiesByMob.put(mob, enemy);
				enemies.add(enemy);
				announceEnemyIdentity(enemy);
			}
			enemy.present = true;
			enemy.body.active = true;
		}
		for (int i = enemies.size() - 1; i >= 0; i--) {
			EnemyRuntime enemy = enemies.get(i);
			if (!enemy.present || !enemy.mob.isAlive()) {
				enemy.brain.markDead();
				enemy.body.active = false;
				enemiesByMob.remove(enemy.mob);
				enemies.remove(i);
			}
		}
		sortEnemiesByStableId();

		targetBodies.clear();
		enemyShotTargetBodies.clear();
		enemyShotTargetBodies.add(heroBody);
		charsByBody.clear();
		for (EnemyRuntime enemy : enemies) {
			targetBodies.add(enemy.body);
			enemyShotTargetBodies.add(enemy.body);
			charsByBody.put(enemy.body, enemy.mob);
		}
	}

	private void sortEnemiesByStableId() {
		// Stable insertion sort avoids allocating a temporary array every refresh.
		for (int i = 1; i < enemies.size(); i++) {
			EnemyRuntime value = enemies.get(i);
			int j = i - 1;
			while (j >= 0
					&& enemies.get(j).stableId > value.stableId) {
				enemies.set(j + 1, enemies.get(j));
				j--;
			}
			enemies.set(j + 1, value);
		}
	}

	private void updateEnemyMovement(EnemyRuntime enemy, float dt) {
		if (!enemy.mob.isAlive() || !enemy.body.active) {
			enemy.moving = false;
			enemy.body.velocityX = 0f;
			enemy.body.velocityY = 0f;
			return;
		}

		announceEnemyManeuver(enemy);
		enemy.avoidance.begin(
				enemy.tacticalIntent.desiredX(),
				enemy.tacticalIntent.desiredY());
		for (EnemyRuntime other : enemies) {
			if (other == enemy || !other.body.active
					|| !other.mob.isAlive()) {
				continue;
			}
			enemy.avoidance.avoid(
					enemy.body.x,
					enemy.body.y,
					other.body.x,
					other.body.y,
					other.stableId,
					Math.max(
							RealtimeLocalAvoidance.DEFAULT_CLEARANCE_TILES,
							enemy.body.radius + other.body.radius
									+ BODY_SEPARATION_TILES));
		}
		float speed = enemy.movementSpeed()
				* enemy.tacticalIntent.speedMultiplier();
		float deltaX = enemy.avoidance.desiredX() * speed * dt;
		float deltaY = enemy.avoidance.desiredY() * speed * dt;
		float originalX = enemy.body.x;
		float originalY = enemy.body.y;
		enemy.body.velocityX = dt > 0f ? deltaX / dt : 0f;
		enemy.body.velocityY = dt > 0f ? deltaY / dt : 0f;
		collision.move(enemy.body, deltaX, deltaY);

		if (overlapsAnotherBody(enemy)) {
			enemy.body.x = originalX;
			enemy.body.y = originalY;
			collision.move(enemy.body, deltaX, 0f);
			if (overlapsAnotherBody(enemy)) {
				enemy.body.x = originalX;
				enemy.body.y = originalY;
				collision.move(enemy.body, 0f, deltaY);
				if (overlapsAnotherBody(enemy)) {
					enemy.body.x = originalX;
					enemy.body.y = originalY;
				}
			}
		}

		enemy.moving =
				Math.abs(enemy.body.x - originalX) > 0.00001f
				|| Math.abs(enemy.body.y - originalY) > 0.00001f;
		enemy.body.velocityX = dt > 0f
				? (enemy.body.x - originalX) / dt : 0f;
		enemy.body.velocityY = dt > 0f
				? (enemy.body.y - originalY) / dt : 0f;
		enemy.navigator.observePosition(
				dt,
				enemy.body.x,
				enemy.body.y);
		int nextCell = enemy.body.cell(Dungeon.level.width());
		if (nextCell != enemy.mob.pos) {
			enemy.mob.pos = nextCell;
		}
	}

	private void announceEnemyManeuver(EnemyRuntime enemy) {
		RealtimeEnemyTactics.Maneuver maneuver =
				enemy.tacticalIntent.maneuver();
		if (maneuver == enemy.previousTacticalManeuver) return;
		enemy.previousTacticalManeuver = maneuver;
		if (!enemy.brain.seesPlayer()) return;
		switch (maneuver) {
			case ANCHOR_AND_SUPPRESS:
				showEnemyStatus(enemy, CharSprite.WARNING, "火力压制");
				break;
			case FLANK_LEFT:
				showEnemyStatus(enemy, CharSprite.WARNING, "左侧迂回");
				break;
			case FLANK_RIGHT:
				showEnemyStatus(enemy, CharSprite.WARNING, "右侧迂回");
				break;
			case DASH:
				showEnemyStatus(enemy, CharSprite.NEGATIVE, "突进");
				break;
			case RETREAT:
				showEnemyStatus(enemy, CharSprite.NEUTRAL, "战术换位");
				break;
			default:
				break;
		}
	}

	private void updateEnemyRangedCombat(EnemyRuntime enemy, float dt) {
		float deltaX = heroBody.x - enemy.body.x;
		float deltaY = heroBody.y - enemy.body.y;
		boolean lineOfSight = enemy.brain.seesPlayer()
				&& GridLineOfSight.visible(
						enemy.body.x,
						enemy.body.y,
						heroBody.x,
						heroBody.y,
						enemy.rangedConfig.maximumRange,
						collisionMap
				);
		enemy.rangedCombat.step(
				dt,
				lineOfSight,
				deltaX,
				deltaY,
				enemy.rangedIntent
		);

		EnemyRangedCombatIntent.Action action =
				enemy.rangedIntent.action();
		if (action == EnemyRangedCombatIntent.Action.AIM
				&& enemy.previousRangedAction != action) {
			showEnemyStatus(enemy, CharSprite.WARNING, "锁定");
		} else if (enemy.rangedIntent.reloadStarted()) {
			showEnemyStatus(enemy, CharSprite.NEUTRAL, "换弹");
			playSfx(
					Assets.Sounds.Bukov.RELOAD_START,
					0.28f,
					0.88f
			);
		} else if (action == EnemyRangedCombatIntent.Action.OUT_OF_AMMO
				&& enemy.previousRangedAction != action) {
			showEnemyStatus(enemy, CharSprite.NEUTRAL, "空仓");
		}

		if (action == EnemyRangedCombatIntent.Action.FIRE) {
			fireEnemyShot(enemy);
		}
		enemy.previousRangedAction = action;
	}

	private void fireEnemyShot(EnemyRuntime enemy) {
		combatPresentation.emit(
				CombatPresentationEvent.Type.ENEMY_FIRE,
				enemy.stableId,
				hero.id(),
				enemy.mob.pos,
				hero.pos,
				null,
				1f);
		playEnemyGunshot(enemy, enemyGunNoiseRadius(enemy));
		showEnemyStatus(enemy, CharSprite.NEGATIVE, "砰");
		combatFx.muzzle(
				enemy.stableId,
				enemy.rangedIntent.shotSequence(),
				true,
				enemy.body.x,
				enemy.body.y,
				enemy.rangedIntent.directionX(),
				enemy.rangedIntent.directionY(),
				0.9f
		);
		combatFx.shell(
				enemy.stableId,
				enemy.rangedIntent.shotSequence(),
				true,
				enemy.body.x,
				enemy.body.y,
				-enemy.rangedIntent.directionY(),
				enemy.rangedIntent.directionX(),
				0.75f
		);

		HitscanResolver.cast(
				enemy.body.x,
				enemy.body.y,
				enemy.rangedIntent.directionX(),
				enemy.rangedIntent.directionY(),
				enemy.rangedConfig.maximumRange,
				collisionMap,
				enemyShotTargetQuery,
				enemy.body,
				enemyShotHit
		);
		combatFx.tracer(
				enemy.stableId,
				enemy.rangedIntent.shotSequence(),
				true,
				enemy.body.x,
				enemy.body.y,
				enemyShotHit.x,
				enemyShotHit.y,
				0.75f
		);
		combatFx.impact(
				enemy.stableId,
				enemy.rangedIntent.shotSequence(),
				true,
				enemyShotHit.x,
				enemyShotHit.y,
				0.9f
		);
		if (enemyShotHit.body == heroBody
				&& enemy.rangedIntent.hasDamageEvent()) {
			pendingEnemyShots.add(new PendingEnemyShot(
					enemy.mob,
					enemy.rangedIntent.damage()
			));
		}
	}

	private static void showEnemyStatus(EnemyRuntime enemy,
										int color,
										String text) {
		if (enemy.mob.sprite != null) {
			enemy.mob.sprite.showStatus(color, text);
		}
	}

	private static void announceEnemyIdentity(EnemyRuntime enemy) {
		if (enemy == null || enemy.definition == null) return;
		showEnemyStatus(
				enemy,
				enemy.definition.tier == EnemyTier.BOSS
						? CharSprite.NEGATIVE : CharSprite.WARNING,
				enemyRoleLabel(enemy.definition));
	}

	static String enemyRoleLabel(EnemyArchetypeDefinition definition) {
		if (definition == null || definition.role == null) return "敌对目标";
		switch (definition.role) {
			case RANGED_SKIRMISHER:
				return "游击射手";
			case MELEE_RUSHER:
				return "突击近战";
			case ARMORED_SUPPRESSOR:
				return "正面装甲";
			case SCOUT_ALARM:
				return "侦测报警";
			case ELITE_COMMANDER:
				return "精英指挥";
			case OPTIONAL_BOSS:
				return "可选Boss · 白线";
			default:
				return "敌对目标";
		}
	}

	private boolean overlapsAnotherBody(EnemyRuntime movingEnemy) {
		if (overlaps(
				movingEnemy.body,
				heroBody,
				BODY_SEPARATION_TILES
		)) {
			return true;
		}
		for (EnemyRuntime other : enemies) {
			if (other == movingEnemy || !other.body.active) {
				continue;
			}
			if (overlaps(
					movingEnemy.body,
					other.body,
					BODY_SEPARATION_TILES
			)) {
				return true;
			}
		}
		return false;
	}

	private boolean heroOverlapsEnemy() {
		for (EnemyRuntime enemy : enemies) {
			if (enemy.body.active
					&& overlaps(
							heroBody,
							enemy.body,
							BODY_SEPARATION_TILES
					)) {
				return true;
			}
		}
		return false;
	}

	private boolean canContactAttack(EnemyRuntime enemy) {
		float deltaX = heroBody.x - enemy.body.x;
		float deltaY = heroBody.y - enemy.body.y;
		float range = Math.min(
				CONTACT_ATTACK_RANGE_TILES,
				enemy.engagementRange());
		return deltaX * deltaX + deltaY * deltaY
				<= range * range
				&& GridLineOfSight.visible(
						enemy.body.x,
						enemy.body.y,
						heroBody.x,
						heroBody.y,
						range,
						collisionMap
				);
	}

	private void resolveEnemyContactAttack(Mob attacker) {
		// Do not call Mob.doAttack/sprite.attack: their completion callback spends
		// turns and would reactivate the legacy Actor execution path.
		EnemyRuntime runtime = enemiesByMob.get(attacker);
		combatPresentation.emit(
				CombatPresentationEvent.Type.ENEMY_MELEE,
				attacker.id(),
				hero.id(),
				attacker.pos,
				hero.pos,
				null,
				1f);
		int damage = runtime == null
				? Math.max(1, attacker.damageRoll())
				: Random.NormalIntRange(
						runtime.minimumDamage(),
						runtime.maximumDamage());
		damage = hero.defenseProc(attacker, damage);
		if (!attacker.isAlive()) {
			EnemyRuntime enemy = enemiesByMob.get(attacker);
			if (enemy != null) {
				enemy.brain.markDead();
				enemy.body.active = false;
			}
			return;
		}
		if (damage >= 0) {
			damage = Math.max(0, damage - hero.drRoll());
		}
		if (damage > 0) {
			playSfx(
					Assets.Sounds.Bukov.CONTACT_HIT,
					0.7f,
					nextAudioPitch(1f, 0.06f)
			);
			boolean wasAlive = hero.isAlive();
			hero.damage(damage, attacker);
			emitPlayerHitOutcome(attacker, wasAlive, damage);
		} else if (hero.sprite != null) {
			hero.sprite.showStatus(
					CharSprite.NEUTRAL,
					hero.defenseVerb()
			);
		}
	}

	private static float enemyAttackCooldown(Mob mob) {
		return Math.max(0.45f, Math.min(2.5f, mob.attackDelay()));
	}

	private static boolean overlaps(RealtimeBody first,
									RealtimeBody second,
									float extraDistance) {
		float deltaX = first.x - second.x;
		float deltaY = first.y - second.y;
		float minimum = first.radius + second.radius + extraDistance;
		return deltaX * deltaX + deltaY * deltaY
				< minimum * minimum;
	}

	private int heroTerrain() {
		return terrainAt(heroBody.x, heroBody.y);
	}

	private int terrainAt(float x, float y) {
		int cellX = Math.max(
				0,
				Math.min(Dungeon.level.width() - 1, (int)Math.floor(x)));
		int cellY = Math.max(
				0,
				Math.min(Dungeon.level.height() - 1, (int)Math.floor(y)));
		return Dungeon.level.map[
				cellX + cellY * Dungeon.level.width()];
	}

	private void emitPlayerSound(float radius) {
		playerSoundSequence++;
		if (playerSoundSequence == 0) {
			playerSoundSequence = 1;
		}
		playerSoundX = heroBody.x;
		playerSoundY = heroBody.y;
		playerSoundRadius = radius;
		playerSoundRemaining = 0.35f;
	}

	@Override
	public void readKeySoundVisualEvent(KeySoundVisualEvent target) {
		keySoundVisual.copyTo(target);
	}

	@Override
	public void readAtmosphereSignal(BukovAtmosphereSignal target) {
		if (target == null) {
			throw new IllegalArgumentException("target is required");
		}
		boolean tense = false;
		boolean combat = false;
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()) continue;
			switch (enemy.brain.state()) {
				case ATTACK:
				case CHASE:
					combat = true;
					break;
				case INVESTIGATE:
					tense = true;
					break;
				default:
					break;
			}
			if (combat) break;
		}
		target.set(tense, combat);
	}

	private void playPlayerGunshot(
			FirearmDefinition definition,
			float gainScale,
			float pitchScale) {
		SpatialAudioModel.resolve(
				audioContract,
				1f,
				0f,
				0f,
				true,
				playbackSpatial);
		GunshotAudioResolver.resolve(
				true,
				nextAudioSequence(),
				0f,
				0f,
				playbackSpatial,
				gunshotAudio);
		playGunshotLayers(
				definition.audioProfile.gunshotFamily.asset(),
				gainScale,
				pitchScale);
	}

	private void playEnemyGunshot(EnemyRuntime enemy, float noiseRadius) {
		float deltaX = enemy.body.x - heroBody.x;
		float deltaY = enemy.body.y - heroBody.y;
		float distance = (float)Math.sqrt(
				deltaX * deltaX + deltaY * deltaY);
		float wallOcclusion = blockedCellsOnLine(
				heroBody.x,
				heroBody.y,
				enemy.body.x,
				enemy.body.y);
		boolean insideSoundRadius = distance <= noiseRadius;
		SpatialAudioModel.resolve(
				audioContract,
				insideSoundRadius ? 1f : 0f,
				distance,
				wallOcclusion,
				false,
				playbackSpatial);
		GunshotAudioResolver.resolve(
				false,
				nextAudioSequence(),
				deltaX,
				deltaY,
				playbackSpatial,
				gunshotAudio);
		if (insideSoundRadius) {
			playGunshotLayers(
					Assets.Sounds.Bukov.GUNSHOT_ENEMY,
					0.78f);
		}
		KeySoundVisualizationResolver.resolve(
				SoundCategory.ENEMY_GUNSHOT,
				deltaX,
				deltaY,
				playbackSpatial,
				audioContract,
				insideSoundRadius
						&& SPDSettings.bukovSoundVisualization(),
				keySoundVisual);
		if (keySoundVisual.visible()) {
			keySoundVisual.activate(
					nextKeySoundSequence(),
					KEY_SOUND_LIFETIME_SECONDS);
		}
	}

	private void playGunshotLayers(String bodyAsset, float gainScale) {
		playGunshotLayers(bodyAsset, gainScale, 1f);
	}

	private void playGunshotLayers(
			String bodyAsset,
			float gainScale,
			float pitchScale) {
		if (!gunshotAudio.audible()) return;
		float safePitchScale = Math.max(0.5f, Math.min(2f, pitchScale));
		playSfxStereo(
				Assets.Sounds.Bukov.DRY_FIRE,
				gunshotAudio.mechanicalLeft() * gainScale,
				gunshotAudio.mechanicalRight() * gainScale,
				Math.max(
						0.5f,
						Math.min(
								2f,
								gunshotAudio.mechanicalPitch()
										* safePitchScale)));
		playSfxStereo(
				bodyAsset,
				gunshotAudio.bodyLeft() * gainScale,
				gunshotAudio.bodyRight() * gainScale,
				Math.max(
						0.5f,
						Math.min(
								2f,
								gunshotAudio.bodyPitch()
										* safePitchScale)));
		playSfxStereo(
				bodyAsset,
				gunshotAudio.tailLeft() * gainScale,
				gunshotAudio.tailRight() * gainScale,
				Math.max(
						0.5f,
						Math.min(
								2f,
								gunshotAudio.tailPitch()
										* safePitchScale)));
	}

	private float realtimeSfxGain() {
		return audioContract.defaultMasterVolume
				* audioContract.defaultSfxVolume
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovMasterVolume())
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovSfxVolume());
	}

	private void playSfx(String asset, float volume, float pitch) {
		float mixedVolume = volume * realtimeSfxGain();
		if (mixedVolume <= 0f) return;
		Sample.INSTANCE.play(asset, mixedVolume, pitch);
	}

	private void playSfxStereo(
			String asset,
			float leftVolume,
			float rightVolume,
			float pitch) {
		float gain = realtimeSfxGain();
		if (gain <= 0f) return;
		Sample.INSTANCE.play(
				asset,
				leftVolume * gain,
				rightVolume * gain,
				pitch);
	}

	private float enemyGunNoiseRadius(EnemyRuntime enemy) {
		if (enemy == null || enemy.definition == null
				|| enemy.definition.weaponDefinitionId == null) {
			return 12f;
		}
		return Math.max(
				1f,
				firearmRegistry.require(
						enemy.definition.weaponDefinitionId)
						.noiseRadiusTiles);
	}

	private int nextAudioSequence() {
		audioSequence++;
		if (audioSequence == Integer.MIN_VALUE) {
			audioSequence = 1;
		}
		return audioSequence;
	}

	private int nextKeySoundSequence() {
		keySoundSequence++;
		if (keySoundSequence == Integer.MIN_VALUE) {
			keySoundSequence = 1;
		}
		return keySoundSequence;
	}

	private float nextAudioPitch(float center, float halfRange) {
		float normalized = (GunshotAudioResolver.variationPitch(
				nextAudioSequence()) - 1f) / 0.04f;
		return center + normalized * halfRange;
	}

	private int blockedCellsOnLine(
			float fromX,
			float fromY,
			float toX,
			float toY) {
		int x0 = (int)Math.floor(fromX);
		int y0 = (int)Math.floor(fromY);
		int x1 = (int)Math.floor(toX);
		int y1 = (int)Math.floor(toY);
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int stepX = x0 < x1 ? 1 : -1;
		int stepY = y0 < y1 ? 1 : -1;
		int error = dx - dy;
		int blocked = 0;
		while (x0 != x1 || y0 != y1) {
			int twice = error * 2;
			if (twice > -dy) {
				error -= dy;
				x0 += stepX;
			}
			if (twice < dx) {
				error += dx;
				y0 += stepY;
			}
			if ((x0 != x1 || y0 != y1)
					&& collisionMap.blocked(x0, y0)) {
				blocked++;
			}
		}
		return blocked;
	}

	private static boolean hasAbility(
			EnemyRuntime enemy,
			String ability) {
		return enemy != null && hasAbility(enemy.definition, ability);
	}

	private static boolean hasAbility(
			EnemyArchetypeDefinition definition,
			String ability) {
		if (definition == null || definition.abilities == null) {
			return false;
		}
		for (String candidate : definition.abilities) {
			if (ability.equals(candidate)) {
				return true;
			}
		}
		return false;
	}

	private void broadcastPlayerContact(EnemyRuntime source) {
		for (EnemyRuntime ally : enemies) {
			if (ally == source || !ally.mob.isAlive()) continue;
			float dx = ally.body.x - source.body.x;
			float dy = ally.body.y - source.body.y;
			if (dx * dx + dy * dy <= 100f) {
				float targetX = heroBody.x;
				float targetY = heroBody.y;
				if (hasAbility(source, "ORDER_FLANK")) {
					float side = (ally.stableId & 1) == 0 ? -2f : 2f;
					targetX += -source.body.velocityY * side;
					targetY += source.body.velocityX * side;
				}
				ally.brain.recordSound(targetX, targetY);
			}
		}
		showEnemyStatus(
				source,
				CharSprite.WARNING,
				hasAbility(source, "ORDER_FLANK")
						? "指挥夹击" : "广播警报");
		if (raid != null && hasAbility(source, "CALL_INVESTIGATORS")) {
			nextEnemySpawnSeconds = Math.min(
					nextEnemySpawnSeconds,
					raid.session().elapsedSeconds
							+ ALARM_REINFORCEMENT_DELAY_SECONDS);
			showHeroStatus("警报已广播 · 敌方增援正在接近");
		}
	}

	private void updateWhiteLineOffense(EnemyRuntime boss, float dt) {
		if (boss.bossState == null || !boss.bossState.active()) {
			boss.bossPulseRemaining = 0f;
			return;
		}
		WhiteLineBossStateMachine.Phase phase =
				boss.bossState.phase();
		if (phase == WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD) {
			return;
		}
		boss.bossPulseRemaining -= dt;
		if (boss.bossPulseRemaining > 0f) return;
		float range = phase
				== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
				? 4.5f : 7.5f;
		if (!GridLineOfSight.visible(
				boss.body.x,
				boss.body.y,
				heroBody.x,
				heroBody.y,
				range,
				collisionMap)) {
			return;
		}
		int damage = phase
				== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
				? Math.max(4, boss.minimumDamage() - 2)
				: boss.maximumDamage();
		pendingEnemyShots.add(new PendingEnemyShot(boss.mob, damage));
		combatPresentation.emit(
				CombatPresentationEvent.Type.ENEMY_FIRE,
				boss.stableId,
				hero.id(),
				boss.mob.pos,
				hero.pos,
				null,
				phase
						== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
						? 0.7f : 1f);
		combatFx.impact(
				boss.stableId,
				++boss.bossPulseSequence,
				true,
				heroBody.x,
				heroBody.y,
				phase
						== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
						? 0.65f : 1f);
		showEnemyStatus(
				boss,
				CharSprite.NEGATIVE,
				phase == WhiteLineBossStateMachine.Phase.DECOY_SEARCH
						? "诱饵冲击" : "雾灯过载");
		boss.bossPulseRemaining = phase
				== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
				? WHITE_LINE_PHASE_TWO_PULSE_SECONDS
				: WHITE_LINE_PHASE_THREE_PULSE_SECONDS;
	}

	private static float interpolate(float from, float to, float alpha) {
		return from + (to - from) * alpha;
	}

	private void ensureContainerMarkers() {
		if (raid == null) return;
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (container.state == BukovSearchableContainer.State.SEARCHING
					&& activeContainerId == null) {
				activeContainerId = container.containerId;
			}
			if (container.contentsReleased
					|| container.cell < 0
					|| container.cell >= Dungeon.level.length()) {
				continue;
			}
			Heap heap = Dungeon.level.heaps.get(container.cell);
			if (heap == null) {
				heap = new Heap();
				heap.pos = container.cell;
				heap.type = Heap.Type.CHEST;
				heap.seen = true;
				Dungeon.level.heaps.put(container.cell, heap);
				GameScene.add(heap);
			}
			if (heap.type == Heap.Type.CHEST
					&& heap.items.isEmpty()
					&& heap.sprite != null) {
				heap.sprite.view(heap);
			}
		}
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (container.state == BukovSearchableContainer.State.SEARCHED
					&& !container.contentsReleased) {
				releaseCompletedContainer(container.containerId);
			}
		}
		ensureReleasedMissionArchiveExists();
	}

	private void ensureReleasedMissionArchiveExists() {
		if (missionGateUnlocked || carriesMissionArchive()) return;
		BukovRaidCoordinator.ContainerSnapshot missionContainer =
				raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
		if (missionContainer == null
				|| missionContainer.state
						!= BukovSearchableContainer.State.SEARCHED
				|| !missionContainer.contentsReleased) {
			return;
		}
		for (Heap existing : Dungeon.level.heaps.valueList()) {
			for (Item item : existing.items) {
				if (item instanceof BukovMissionArchive) return;
			}
		}

		// A v3-era host save could have persisted "contents released" before
		// the matching heap was durable. Recreate only the unique zero-value
		// mission document; normal randomized loot is never rerolled.
		Heap heap = Dungeon.level.heaps.get(missionContainer.cell);
		boolean created = false;
		if (heap == null) {
			heap = new Heap();
			heap.pos = missionContainer.cell;
			heap.seen = true;
			created = true;
		}
		if (heap.type != Heap.Type.HEAP
				&& heap.type != Heap.Type.CHEST) {
			throw new IllegalStateException(
					"Mission archive cell is occupied by an incompatible heap");
		}
		heap.type = Heap.Type.HEAP;
		heap.items.addFirst(new BukovMissionArchive());
		if (created) {
			Dungeon.level.heaps.put(missionContainer.cell, heap);
			GameScene.add(heap);
		} else if (heap.sprite != null) {
			heap.sprite.link();
			heap.sprite.drop();
		}
	}

	private void createInteractionMarkers() {
		if (raid == null) return;
		for (ExtractionState extraction : raid.extractions()) {
			addInteractionMarker(
					resolveExtractionCell(extraction.extractionId()),
					extraction.type() == ExtractionState.Type.BASIC
							? BukovInteractionMarker.Kind.FIXED_EXTRACTION
							: BukovInteractionMarker.Kind.CONDITIONAL_EXTRACTION);
		}
		addInteractionMarker(
				pumpCell,
				BukovInteractionMarker.Kind.PUMP_STATION);
		refreshBossMechanismMarkers();
	}

	private void refreshBossMechanismMarkers() {
		EnemyRuntime boss = activeWhiteLine();
		if (boss == null
				|| boss.bossState.phase()
						!= WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
			clearBossMechanismMarkers();
			return;
		}
		if (!bossMechanismMarkers.isEmpty()
				|| !(Dungeon.level instanceof BukovLevel)) {
			return;
		}
		BukovRaidLayout.BossMechanism mechanism =
				((BukovLevel)Dungeon.level).bossMechanism();
		if (mechanism == null
				|| mechanism.bodyTraceCells.length
						!= boss.bossState.bodyCount()) {
			showHeroStatus("白线轨迹锚点损坏，仍可选择非Boss撤离");
			return;
		}
		for (int i = 0; i < mechanism.bodyTraceCells.length; i++) {
			BukovInteractionMarker.Kind kind =
					boss.bossState.synchronizedTrace(i)
							? BukovInteractionMarker.Kind
									.BOSS_SYNCHRONIZED_TRACE
							: BukovInteractionMarker.Kind.BOSS_DECOY;
			BukovInteractionMarker marker =
					new BukovInteractionMarker(kind)
							.placeAtCell(mechanism.bodyTraceCells[i]);
			bossMechanismMarkers.add(marker);
			interactionMarkers.add(marker);
			GameScene.effect(marker);
		}
	}

	private void clearBossMechanismMarkers() {
		if (bossMechanismMarkers.isEmpty()) return;
		for (BukovInteractionMarker marker : bossMechanismMarkers) {
			marker.killAndErase();
			interactionMarkers.remove(marker);
		}
		bossMechanismMarkers.clear();
	}

	private void addInteractionMarker(
			int cell,
			BukovInteractionMarker.Kind kind) {
		if (cell < 0 || cell >= Dungeon.level.length()) return;
		BukovInteractionMarker marker =
				new BukovInteractionMarker(kind).placeAtCell(cell);
		interactionMarkers.add(marker);
		GameScene.effect(marker);
	}

	private ExtractionState extractionAtCell(int cell) {
		for (ExtractionState extraction : raid.extractions()) {
			if (cell == resolveExtractionCell(extraction.extractionId())) {
				return extraction;
			}
		}
		return null;
	}

	private BukovRaidCoordinator.ContainerSnapshot containerWithinRange(
			int cell) {
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (!container.contentsReleased
					&& withinInteractionRange(cell, container.cell)) {
				return container;
			}
		}
		return null;
	}

	private boolean withinInteractionRange(int firstCell, int secondCell) {
		if (firstCell < 0 || secondCell < 0) return false;
		int width = Dungeon.level.width();
		return Math.max(
				Math.abs(firstCell % width - secondCell % width),
				Math.abs(firstCell / width - secondCell / width)
		) <= 1;
	}

	private void readNavigationHudState(
			BukovRaidHudState target,
			float elapsed) {
		if (raid == null) return;
		if (!missionGateUnlocked) {
			if (carriesMissionArchive()) {
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.MISSION,
						missionGateCell,
						"维修通道",
						true);
				return;
			}
			BukovRaidCoordinator.ContainerSnapshot archive =
					raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
			if (archive != null && !archive.contentsReleased) {
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.MISSION,
						archive.cell,
						"维修档案",
						archive.state
								!= BukovSearchableContainer.State.LOCKED);
				return;
			}
			for (Heap heap : Dungeon.level.heaps.valueList()) {
				if (heap == null || heap.peek() == null) continue;
				for (Item item : heap.items) {
					if (!(item instanceof BukovMissionArchive)) continue;
					pointHudNavigation(
							target,
							BukovRaidHudState.Cue.PICKUP,
							heap.pos,
							"拾取维修档案",
							true);
					return;
				}
			}
		}

		if (raid.firstRaidMissionActive()
				&& raid.firstRaidStage()
						== FirstRaidMission.Stage.SECURE_HIGH_VALUE_CACHE) {
			for (BukovRaidCoordinator.ContainerSnapshot container :
					raid.containers()) {
				if (!FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							container.lootTableId)
						|| container.contentsReleased) {
					continue;
				}
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.MISSION,
						container.cell,
						"搜查高价值仓",
						container.state
								!= BukovSearchableContainer.State.LOCKED);
				return;
			}
		}

		ExtractionState nearest = null;
		float nearestDistanceSquared = Float.MAX_VALUE;
		for (ExtractionState extraction : raid.extractions()) {
			if (extraction.completed()
					|| !extractionAvailable(extraction, elapsed)) {
				continue;
			}
			int cell = resolveExtractionCell(extraction.extractionId());
			float distanceSquared = distanceSquaredToCell(cell);
			if (distanceSquared < nearestDistanceSquared) {
				nearest = extraction;
				nearestDistanceSquared = distanceSquared;
			}
		}
		if (nearest != null) {
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.EXTRACTION,
					resolveExtractionCell(nearest.extractionId()),
					"撤离 " + nearest.extractionId(),
					true);
		}
	}

	private boolean extractionAvailable(
			ExtractionState extraction,
			float elapsed) {
		if (extraction == null || !extraction.availableAt(elapsed)) {
			return false;
		}
		return !FirstRaidMission.CONDITIONAL_EXTRACTION_ID.equals(
					extraction.extractionId())
				|| raid == null
				|| raid.firstRaidConditionalExtractionUnlocked();
	}

	private void readThreatHudState(BukovRaidHudState target) {
		boolean[] visible = Dungeon.level.heroFOV;
		if (visible == null || visible.length < Dungeon.level.length()) return;
		EnemyRuntime nearest = null;
		float nearestDistanceSquared = Float.MAX_VALUE;
		for (EnemyRuntime enemy : enemies) {
			if (enemy == null
					|| !enemy.body.active
					|| !enemy.mob.isAlive()
					|| enemy.mob.pos < 0
					|| enemy.mob.pos >= Dungeon.level.length()
					|| !visible[enemy.mob.pos]) {
				continue;
			}
			float deltaX = enemy.body.x - heroBody.x;
			float deltaY = enemy.body.y - heroBody.y;
			float distanceSquared = deltaX * deltaX + deltaY * deltaY;
			if (distanceSquared < nearestDistanceSquared) {
				nearest = enemy;
				nearestDistanceSquared = distanceSquared;
			}
		}
		if (nearest == null || nearestDistanceSquared > 18f * 18f) return;
		float deltaX = nearest.body.x - heroBody.x;
		float deltaY = nearest.body.y - heroBody.y;
		float distance = (float)Math.sqrt(nearestDistanceSquared);
		target.threat(
				deltaX,
				deltaY,
				distance,
				nearest.definition == null
						? "敌人" : nearest.definition.name,
				distance <= 4f);
	}

	private void pointHudNavigation(
			BukovRaidHudState target,
			BukovRaidHudState.Cue cue,
			int cell,
			String label,
			boolean available) {
		if (cell < 0 || cell >= Dungeon.level.length()) return;
		int width = Dungeon.level.width();
		float deltaX = cell % width + 0.5f - heroBody.x;
		float deltaY = cell / width + 0.5f - heroBody.y;
		float distance = (float)Math.sqrt(
				deltaX * deltaX + deltaY * deltaY);
		target.navigation(
				cue,
				deltaX,
				deltaY,
				distance,
				label,
				available);
	}

	private float distanceSquaredToCell(int cell) {
		if (cell < 0 || cell >= Dungeon.level.length()) {
			return Float.MAX_VALUE;
		}
		int width = Dungeon.level.width();
		float deltaX = cell % width + 0.5f - heroBody.x;
		float deltaY = cell / width + 0.5f - heroBody.y;
		return deltaX * deltaX + deltaY * deltaY;
	}

	private void activatePump() {
		ExtractionState conditional = raid.extraction(
				CONDITIONAL_EXTRACTION_ID);
		if (conditional == null) {
			showHeroStatus("泵站没有连接撤离系统");
			return;
		}
		if (conditional.conditionMet()) {
			showHeroStatus("泵站供电正常");
			return;
		}
		raid.setExtractionCondition(CONDITIONAL_EXTRACTION_ID, true);
		showHeroStatus("泵站供电已恢复，E02撤离点开放");
		checkpointLootChange();
	}

	private boolean completeNearbyBossObjective() {
		EnemyRuntime boss = activeWhiteLine();
		if (boss == null) return false;
		if (boss.bossState.phase()
				== WhiteLineBossStateMachine.Phase.DORMANT) {
			if (!withinInteractionRange(hero.pos, boss.mob.pos)) {
				return false;
			}
			boss.bossState.engage();
			showBossObjective(boss);
		}

		WhiteLineBossStateMachine.Result result;
		switch (boss.bossState.objective()) {
			case FLANK_UMBRELLA:
				if (!withinInteractionRange(hero.pos, boss.mob.pos)) {
					return false;
				}
				int bossX = boss.mob.pos % Dungeon.level.width();
				int bossY = boss.mob.pos / Dungeon.level.width();
				int facingCell = pumpCell >= 0 ? pumpCell : extractionCell;
				int facingX = facingCell % Dungeon.level.width() - bossX;
				int facingY = facingCell / Dungeon.level.width() - bossY;
				int approachX = hero.pos % Dungeon.level.width() - bossX;
				int approachY = hero.pos / Dungeon.level.width() - bossY;
				result = boss.bossState.flankUmbrella(
						facingX, facingY, approachX, approachY);
				if (result
						== WhiteLineBossStateMachine.Result.MECHANISM_REJECTED) {
					showHeroStatus("伞盾正面封锁，绕到白线侧后方再交互");
					showEnemyStatus(
							boss, CharSprite.WARNING, "正面免疫");
					return true;
				}
				break;
			case IDENTIFY_TRUE_BODY:
				int bodyIndex = bodyTraceWithinRange(hero.pos);
				if (bodyIndex < 0) return false;
				result = boss.bossState.identifyTrueBody(bodyIndex);
				if (result
						== WhiteLineBossStateMachine.Result.MECHANISM_REJECTED) {
					showHeroStatus("诱饵信号空洞，寻找缓慢同步的稳定轨迹");
					return true;
				}
				break;
			case DISABLE_FOG_LAMPS:
				if (!withinInteractionRange(hero.pos, pumpCell)) {
					return false;
				}
				result = boss.bossState.disableFogLamp(
						PUMP_SEMANTIC_ID);
				if (result
						== WhiteLineBossStateMachine.Result.MECHANISM_REJECTED) {
					showHeroStatus("需要操作泵站的雾灯控制器");
					return true;
				}
				break;
			default:
				return false;
		}

		if (result
				== WhiteLineBossStateMachine.Result.OBJECTIVE_COMPLETED) {
			if (boss.bossState.phase()
					== WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
				clearBossMechanismMarkers();
			}
			if (boss.bossState.phase()
					== WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) {
				activatePump();
			}
			showHeroStatus("白线弱点已暴露");
			checkpointRuntimeCombatState();
			return true;
		}
		return false;
	}

	private EnemyRuntime activeWhiteLine() {
		for (EnemyRuntime enemy : enemies) {
			if (enemy.bossState != null
					&& enemy.mob.isAlive()
					&& enemy.bossState.phase()
							!= WhiteLineBossStateMachine.Phase.BYPASSED
					&& enemy.bossState.phase()
							!= WhiteLineBossStateMachine.Phase.DEFEATED) {
				return enemy;
			}
		}
		return null;
	}

	private int bodyTraceWithinRange(int cell) {
		if (!(Dungeon.level instanceof BukovLevel)) return -1;
		BukovRaidLayout.BossMechanism mechanism =
				((BukovLevel)Dungeon.level).bossMechanism();
		if (mechanism == null) return -1;
		// Exact marker occupancy wins before the one-tile accessibility radius,
		// so two nearby traces can never make the player's explicit choice
		// ambiguous.
		for (int i = 0; i < mechanism.bodyTraceCells.length; i++) {
			if (cell == mechanism.bodyTraceCells[i]) return i;
		}
		for (int i = 0; i < mechanism.bodyTraceCells.length; i++) {
			if (withinInteractionRange(cell, mechanism.bodyTraceCells[i])) {
				return i;
			}
		}
		return -1;
	}

	private void resolveWhiteLineDamage(EnemyRuntime enemy, int damage) {
		if (enemy.bossState.phase()
				== WhiteLineBossStateMachine.Phase.DORMANT) {
			enemy.bossState.engage();
			showBossObjective(enemy);
		}
		WhiteLineBossStateMachine.Result result =
				enemy.bossState.applyDamage(damage);
		if (result == WhiteLineBossStateMachine.Result.DAMAGE_BLOCKED) {
			showEnemyStatus(enemy, CharSprite.WARNING, "机制保护");
			return;
		}
		enemy.mob.HP = Math.max(1, enemy.bossState.health());
		if (result == WhiteLineBossStateMachine.Result.PHASE_CHANGED) {
			showBossObjective(enemy);
		} else if (result == WhiteLineBossStateMachine.Result.DEFEATED) {
			clearBossMechanismMarkers();
			resolveWhiteLineLevel();
			int cell = enemy.mob.pos;
			enemy.mob.damage(enemy.mob.HP, hero);
			recordEnemyKill();
			releaseWhiteLineLoot(cell);
		}
	}

	private void showBossObjective(EnemyRuntime enemy) {
		showTutorial(BukovTutorialEvent.BOSS_WARNING);
		String text;
		switch (enemy.bossState.objective()) {
			case FLANK_UMBRELLA:
				text = "预警：绕至侧后方交互破伞盾";
				break;
			case IDENTIFY_TRUE_BODY:
				text = "预警：检查四个轨迹，稳定慢闪为真身";
				refreshBossMechanismMarkers();
				break;
			case DISABLE_FOG_LAMPS:
				text = "预警：前往泵站操作雾灯控制器";
				break;
			default:
				return;
		}
		showEnemyStatus(enemy, CharSprite.WARNING, text);
	}

	private void bypassWhiteLineForExtraction() {
		boolean available = nonBossExtractionAvailable();
		for (EnemyRuntime enemy : enemies) {
			if (enemy.bossState == null) continue;
			if (enemy.bossState.bypass(available)
					== WhiteLineBossStateMachine.Result.BYPASSED) {
				clearBossMechanismMarkers();
				enemy.body.active = false;
				enemy.brain.markDead();
				enemy.mob.destroy();
				if (enemy.mob.sprite != null) {
					enemy.mob.sprite.killAndErase();
				}
			}
		}
		if (available) resolveWhiteLineLevel();
	}

	private boolean nonBossExtractionAvailable() {
		for (ExtractionState extraction : raid.extractions()) {
			if (!extraction.completed()
					&& extraction.availableAt(
							raid.session().elapsedSeconds)) {
				return true;
			}
		}
		return false;
	}

	private void resolveWhiteLineLevel() {
		if (Dungeon.level instanceof BukovLevel) {
			((BukovLevel)Dungeon.level).resolveWhiteLine();
		}
	}

	private void releaseWhiteLineLoot(int cell) {
		Heap heap = Dungeon.level.heaps.get(cell);
		boolean created = false;
		if (heap == null) {
			heap = new Heap();
			heap.pos = cell;
			heap.seen = true;
			created = true;
		} else if (heap.type != Heap.Type.HEAP) {
			return;
		}
		for (Item item : BukovFirstRaidLootTables
				.require(BukovFirstRaidLootTables.BOSS)
				.roll(Dungeon.seed, "boss-white-line", 3)) {
			heap.items.addLast(item);
		}
		if (created) {
			Dungeon.level.heaps.put(cell, heap);
			GameScene.add(heap);
		} else if (heap.sprite != null) {
			heap.sprite.link();
			heap.sprite.drop();
		}
	}

	private void releaseCompletedContainer(String containerId) {
		BukovRaidCoordinator.ContainerSnapshot container =
				raid.container(containerId);
		if (container == null || container.contentsReleased) return;

		Heap heap = Dungeon.level.heaps.get(container.cell);
		boolean created = false;
		if (heap == null) {
			heap = new Heap();
			heap.pos = container.cell;
			heap.seen = true;
			created = true;
		} else if (heap.type != Heap.Type.HEAP
				&& heap.type != Heap.Type.CHEST) {
			showHeroStatus("容器内容无法释放");
			return;
		}

		heap.type = Heap.Type.HEAP;
		int released = raid.releaseContainerContents(containerId, heap);
		if (created) {
			Dungeon.level.heaps.put(container.cell, heap);
			GameScene.add(heap);
		} else if (heap.sprite != null) {
			heap.sprite.link();
			heap.sprite.drop();
		}
		showHeroStatus(released > 0
				? "搜索完成，发现 " + released + " 件物品"
				: "搜索完成，容器为空");
		checkpointLootChange();
	}

	private void releaseEnemyLoot(EnemyRuntime enemy, int cell) {
		if (enemy.definition == null
				|| enemy.definition.tier == EnemyTier.BOSS) {
			return;
		}
		boolean normalDrop = enemyDropsLoot(
				Dungeon.seed,
				enemy.stableId,
				enemy.definition.tier);
		boolean maintenanceKeyDrop =
				BukovFirstRaidLootTables.maintenanceKeyDrops(
						Dungeon.seed,
						enemy.stableId);
		if (!normalDrop && !maintenanceKeyDrop) {
			return;
		}
		String table = enemy.definition.tier == EnemyTier.ELITE
				? BukovFirstRaidLootTables.INDUSTRIAL
				: BukovFirstRaidLootTables.LOW;
		Heap heap = Dungeon.level.heaps.get(cell);
		boolean created = false;
		if (heap == null) {
			heap = new Heap();
			heap.pos = cell;
			heap.seen = true;
			created = true;
		} else if (heap.type != Heap.Type.HEAP) {
			return;
		}
		if (normalDrop) {
			int rolls = enemy.definition.tier == EnemyTier.ELITE ? 2 : 1;
			for (Item item : BukovFirstRaidLootTables.require(table).roll(
					Dungeon.seed,
					"enemy:" + enemy.stableId,
					rolls)) {
				heap.items.addLast(item);
			}
		}
		if (maintenanceKeyDrop
				&& !heapContainsDefinition(
						heap,
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID)) {
			Item key = BukovFirstRaidLootTables
					.createByEconomicDefinitionId(
							BukovFirstRaidLootTables
									.MAINTENANCE_KEY_DEFINITION_ID);
			if (key == null) {
				throw new IllegalStateException(
						"Maintenance key loot definition is missing");
			}
			heap.items.addLast(key);
		}
		if (created) {
			Dungeon.level.heaps.put(cell, heap);
			GameScene.add(heap);
		} else if (heap.sprite != null) {
			heap.sprite.link();
			heap.sprite.drop();
		}
	}

	private static boolean heapContainsDefinition(
			Heap heap,
			String definitionId) {
		for (Item item : heap.items) {
			if (item instanceof BukovEconomicItem
					&& definitionId.equals(
							((BukovEconomicItem)item)
									.bukovDefinitionId())) {
				return true;
			}
		}
		return false;
	}

	static boolean enemyDropsLoot(
			long seed,
			int stableId,
			EnemyTier tier) {
		if (tier == null || tier == EnemyTier.BOSS) return false;
		if (tier == EnemyTier.ELITE) return true;
		long mixed = seed ^ stableId * 0x9E3779B97F4A7C15L;
		mixed ^= mixed >>> 33;
		return com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(mixed, 100L) < 35L;
	}

	static int resolveEnemyArmor(
			EnemyArchetypeDefinition definition,
			int damage,
			float penetration) {
		if (definition == null || damage <= 0
				|| !hasAbility(definition, "ARMORED_FRONT")) {
			return Math.max(0, damage);
		}
		float absorbed = penetration >= 25f
				? 0.10f : penetration >= 15f ? 0.20f : 0.35f;
		return Math.max(1, Math.round(damage * (1f - absorbed)));
	}

	private void releaseEnemyFirearm(Mob defeated) {
		EnemyRuntime enemy = enemiesByMob.get(defeated);
		if (enemy == null
				|| enemy.definition.weaponDefinitionId == null
				|| !BukovEnemyFirearmDropPolicy.shouldDrop(
						enemy.definition.tier,
						enemy.definition.id,
						defeated.id())) {
			return;
		}
		Item weapon = BukovFirstRaidLootTables
				.createByEconomicDefinitionId(
						"firearm:"
								+ enemy.definition.weaponDefinitionId);
		if (weapon == null) {
			throw new IllegalStateException(
					"Enemy weapon is not obtainable: "
							+ enemy.definition.weaponDefinitionId);
		}
		Heap heap = Dungeon.level.drop(weapon, defeated.pos);
		heap.type = Heap.Type.HEAP;
		heap.hidden = false;
		heap.seen = true;
	}

	private int resolveExtractionCell(String extractionId) {
		if (Dungeon.level instanceof BukovLevel) {
			return ((BukovLevel)Dungeon.level).extractionCell(
					extractionId
			);
		}
		return BASELINE_EXTRACTION_ID.equals(extractionId)
				? Dungeon.level.exit()
				: -1;
	}

	private int resolvePumpCell() {
		return Dungeon.level instanceof BukovLevel
				? ((BukovLevel)Dungeon.level).semanticCell(PUMP_SEMANTIC_ID)
				: -1;
	}

	private void showExtractionCountdown(ExtractionState extraction) {
		int remaining = Math.max(
				0,
				(int)Math.ceil(
						extraction.interactionSeconds()
								- extraction.progressSeconds()
				)
		);
		if (remaining == lastExtractionCountdown) {
			return;
		}
		lastExtractionCountdown = remaining;
		showHeroStatus(
				remaining > 0
						? "撤离 " + remaining + "秒"
						: "撤离完成"
		);
	}

	private void showContainerCountdown(
			BukovRaidCoordinator.ContainerSnapshot container) {
		if (container == null) return;
		int remaining = Math.max(
				0,
				(int)Math.ceil(
						container.searchSeconds - container.progressSeconds
				)
		);
		if (remaining == lastContainerCountdown) return;
		lastContainerCountdown = remaining;
		showHeroStatus("搜索 " + remaining + "秒");
	}

	private void showHeroStatus(String text) {
		if (hero.sprite != null) {
			hero.sprite.showStatus(CharSprite.NEUTRAL, text);
		}
	}

	private void showTutorial(BukovTutorialEvent event) {
		if (tutorialGuide == null) {
			return;
		}
		try {
			BukovTutorialEvent claimed = tutorialGuide.claim(event);
			if (claimed != null) {
				tutorialEvent = claimed;
				tutorialRemaining = BukovTutorialGuide.DISPLAY_SECONDS;
			}
		} catch (IOException failure) {
			// A hint is never worth risking profile/checkpoint divergence.
			ShatteredPixelDungeon.reportException(failure);
		}
	}

	private int playerAimCell() {
		if (inputFrame == null
				|| inputFrame.aim.x == 0f && inputFrame.aim.y == 0f) {
			return hero.pos;
		}
		int width = Dungeon.level.width();
		int height = Dungeon.level.length() / width;
		int x = Math.max(
				0,
				Math.min(
						width - 1,
						(int)Math.floor(
								heroBody.x + inputFrame.aim.x * 2f)));
		int y = Math.max(
				0,
				Math.min(
						height - 1,
						(int)Math.floor(
								heroBody.y + inputFrame.aim.y * 2f)));
		return x + y * width;
	}

	private void emitEnemyHitOutcome(
			Char target, boolean wasAlive, int damage) {
		if (target == null) return;
		if (target.sprite != null && shouldShowDamageNumber(
				SPDSettings.bukovDamageNumbers(), damage, target.HT)) {
			target.sprite.showStatus(CharSprite.NEGATIVE, "-" + damage);
		}
		float intensity = Math.max(
				0.35f,
				Math.min(1.5f, damage / 12f));
		combatPresentation.emit(
				CombatPresentationEvent.Type.ENEMY_HIT,
				hero.id(),
				target.id(),
				hero.pos,
				target.pos,
				null,
				intensity);
		if (wasAlive && !target.isAlive()) {
			combatPresentation.emit(
					CombatPresentationEvent.Type.ENEMY_DEATH,
					hero.id(),
					target.id(),
					hero.pos,
					target.pos,
					CombatFeedbackType.KILL,
					intensity);
		}
	}

	private void emitPlayerHitOutcome(
			Char attacker, boolean wasAlive, int damage) {
		recordHitDirection(attacker, damage);
		showHeroStatus(shouldShowDamageNumber(
				SPDSettings.bukovDamageNumbers(), damage, hero.HT)
				? "受击 -" + damage : "受击");
		float intensity = Math.max(
				0.45f,
				Math.min(
						1.5f,
						damage / Math.max(1f, hero.HT * 0.2f)));
		combatPresentation.emit(
				CombatPresentationEvent.Type.PLAYER_HIT,
				attacker == null ? -1 : attacker.id(),
				hero.id(),
				attacker == null ? hero.pos : attacker.pos,
				hero.pos,
				CombatFeedbackType.PLAYER_HIT,
				intensity);
		if (wasAlive && !hero.isAlive() && !playerDeathPresented) {
			playerDeathPresented = true;
			showTutorial(BukovTutorialEvent.FIRST_DEATH);
			combatPresentation.emit(
					CombatPresentationEvent.Type.PLAYER_DEATH,
					attacker == null ? -1 : attacker.id(),
					hero.id(),
					attacker == null ? hero.pos : attacker.pos,
					hero.pos,
					null,
					1f);
		}
	}

	static boolean shouldShowDamageNumber(
			int mode, int damage, int maximumHealth) {
		if (mode <= 0 || damage <= 0) return false;
		if (mode >= 2) return true;
		return damage >= Math.max(
				8, Math.round(Math.max(1, maximumHealth) * 0.15f));
	}

	private void recordHitDirection(Char attacker, int damage) {
		if (attacker == null || Dungeon.level == null) return;
		int width = Dungeon.level.width();
		float deltaX = attacker.pos % width - hero.pos % width;
		float deltaY = attacker.pos / width - hero.pos / width;
		hitIndicatorDirection = direction(deltaX, deltaY);
		hitIndicatorStrength = Math.max(
				0.2f,
				Math.min(1f, damage / Math.max(1f, hero.HT * 0.35f)));
		hitIndicatorRemaining = HIT_DIRECTION_LIFETIME_SECONDS;
	}

	static BukovRaidHudState.Direction direction(float deltaX, float deltaY) {
		if (deltaX == 0f && deltaY == 0f) {
			return BukovRaidHudState.Direction.N;
		}
		double angle = Math.atan2(deltaY, deltaX);
		int octant = (int)Math.round(angle / (Math.PI / 4d));
		switch ((octant + 8) % 8) {
			case 0: return BukovRaidHudState.Direction.E;
			case 1: return BukovRaidHudState.Direction.SE;
			case 2: return BukovRaidHudState.Direction.S;
			case 3: return BukovRaidHudState.Direction.SW;
			case 4: return BukovRaidHudState.Direction.W;
			case 5: return BukovRaidHudState.Direction.NW;
			case 6: return BukovRaidHudState.Direction.N;
			default: return BukovRaidHudState.Direction.NE;
		}
	}

	private void readBossHudState(BukovRaidHudState target) {
		for (EnemyRuntime enemy : enemies) {
			if (enemy.bossState == null
					|| !enemy.mob.isAlive()
					|| !enemy.bossState.active()) {
				continue;
			}
			target.boss(
					"白线",
					bossPhase(enemy.bossState.phase()),
					3,
					bossPhaseLabel(enemy.bossState.phase()),
					enemy.bossState.health(),
					enemy.bossState.maximumHealth(),
					enemy.bossState.vulnerable(),
					bossObjectiveLabel(enemy.bossState.objective()),
					enemy.bossState.retreatRecommended());
			return;
		}
	}

	private static int bossPhase(WhiteLineBossStateMachine.Phase phase) {
		if (phase == WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD) return 1;
		if (phase == WhiteLineBossStateMachine.Phase.DECOY_SEARCH) return 2;
		if (phase == WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) return 3;
		return 0;
	}

	private static String bossPhaseLabel(
			WhiteLineBossStateMachine.Phase phase) {
		if (phase == WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD) {
			return "伞盾封锁";
		}
		if (phase == WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
			return "诱饵搜索";
		}
		if (phase == WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) {
			return "雾灯过载";
		}
		return "";
	}

	private static String bossObjectiveLabel(
			WhiteLineBossStateMachine.Objective objective) {
		if (objective == WhiteLineBossStateMachine.Objective.FLANK_UMBRELLA) {
			return "绕开伞盾";
		}
		if (objective
				== WhiteLineBossStateMachine.Objective.IDENTIFY_TRUE_BODY) {
			return "识别真身";
		}
		if (objective
				== WhiteLineBossStateMachine.Objective.DISABLE_FOG_LAMPS) {
			return "关闭雾灯";
		}
		return "攻击弱点";
	}

	private void emitMedicalEnded() {
		combatPresentation.emit(
				CombatPresentationEvent.Type.PLAYER_MEDICAL_END,
				hero.id(),
				hero.id(),
				hero.pos,
				hero.pos,
				null,
				1f);
	}

	private static final class PendingHit {
		private final Char target;
		private final float damage;

		private PendingHit(Char target, float damage) {
			this.target = target;
			this.damage = damage;
		}
	}

	private static final class PendingEnemyShot {
		private final Mob attacker;
		private final int damage;

		private PendingEnemyShot(Mob attacker, int damage) {
			this.attacker = attacker;
			this.damage = damage;
		}
	}

	private static final class EnemyRuntime {
		private final Mob mob;
		private final RealtimeBody body;
		private final RealtimeEnemyBrain brain;
		private final RealtimeEnemyNavigator navigator;
		private final RealtimeEnemyNavigator.Intent navigationIntent;
		private final RealtimeEnemyTactics tactics;
		private final RealtimeEnemyTactics.Intent tacticalIntent;
		private final RealtimeLocalAvoidance avoidance;
		private final int stableId;
		private final EnemyArchetypeDefinition definition;
		private final EnemyRangedCombatController.Config rangedConfig;
		private final WhiteLineBossStateMachine bossState;
		private EnemyRangedCombatController rangedCombat;
		private EnemyRangedCombatIntent rangedIntent;
		private EnemyRangedCombatIntent.Action previousRangedAction;
		private RealtimeEnemyTactics.Maneuver previousTacticalManeuver =
				RealtimeEnemyTactics.Maneuver.FOLLOW_BRAIN;
		private boolean present;
		private boolean moving;
		private boolean broadcastedContact;
		private float bossPulseRemaining;
		private int bossPulseSequence;
		private int heardSoundSequence = Integer.MIN_VALUE;

		private EnemyRuntime(
				Mob mob,
				EnemyArchetypeDefinition definition,
				BukovRaidCheckpoint.EnemyRuntimeState restoredState,
				int mapWidth,
				int mapHeight) {
			this.mob = mob;
			this.definition = definition;
			body = mob.ensureRealtimeBody();
			stableId = mob.id();
			brain = new RealtimeEnemyBrain(stableId);
			navigator = new RealtimeEnemyNavigator(
					stableId,
					mapWidth,
					mapHeight);
			navigationIntent = new RealtimeEnemyNavigator.Intent();
			tactics = new RealtimeEnemyTactics(
					RealtimeEnemyTactics.profileFor(definition),
					stableId);
			tacticalIntent = new RealtimeEnemyTactics.Intent();
			avoidance = new RealtimeLocalAvoidance(stableId);
			String definitionId =
					definition == null ? "" : definition.id;
			boolean matchingSnapshot = restoredState != null
					&& definitionId.equals(restoredState.definitionId());
			if (matchingSnapshot) {
				brain.restoreSnapshot(restoredState.brain());
			}
			if (definition != null
					&& definition.weaponDefinitionId != null) {
				boolean suppressor = tactics.profile()
						== RealtimeEnemyTactics.Profile.SUPPRESSOR;
				boolean flanker = tactics.profile()
						== RealtimeEnemyTactics.Profile.FLANKER;
				rangedConfig = new EnemyRangedCombatController.Config(
						suppressor ? 8
								: definition.tier == EnemyTier.ELITE ? 6 : 5,
						suppressor ? 240f
								: flanker ? 180f : 150f,
						suppressor ? 1.85f : 1.6f,
						definition.engagementRange,
						suppressor ? 0.22f
								: definition.tier == EnemyTier.ELITE
										? 0.3f : 0.45f,
						definition.minimumDamage,
						definition.maximumDamage);
				enableRangedCombat();
				if (matchingSnapshot
						&& restoredState.rangedCombat() != null) {
					rangedCombat.restoreSnapshot(
							restoredState.rangedCombat());
				}
			} else {
				rangedConfig = null;
				if (matchingSnapshot
						&& restoredState.rangedCombat() != null) {
					throw new IllegalStateException(
							"Non-ranged enemy has ranged checkpoint state");
				}
			}
			bossState = definition != null
					&& definition.tier == EnemyTier.BOSS
					? Dungeon.level instanceof BukovLevel
							? ((BukovLevel)Dungeon.level).whiteLineState(
									definition.health)
							: new WhiteLineBossStateMachine(
									definition.health, Dungeon.seed)
					: null;
		}

		private void enableRangedCombat() {
			rangedCombat = new EnemyRangedCombatController(
					rangedConfig,
					rangedConfig.magazineSize,
					BASELINE_ENEMY_RESERVE_AMMO,
					stableId ^ (int)(Dungeon.seed ^ Dungeon.seed >>> 32)
			);
			rangedIntent = new EnemyRangedCombatIntent();
		}

		private float movementSpeed() {
			float baseline = definition == null
					? Math.max(
							1.35f,
							Math.min(
									3.2f,
									MOB_SPEED_TILES_PER_SECOND
											* mob.speed()))
					: definition.movementSpeed;
			if (bossState == null) return baseline;
			if (bossState.phase()
					== WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD) {
				return baseline * 0.82f;
			}
			if (bossState.phase()
					== WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
				return baseline * 1.18f;
			}
			if (bossState.phase()
					== WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) {
				return baseline * 1.08f;
			}
			return baseline;
		}

		private float perceptionRange() {
			return definition == null
					? Math.max(1f, Dungeon.level.viewDistance)
					: definition.perceptionRange;
		}

		private float engagementRange() {
			return definition == null
					? CONTACT_ATTACK_RANGE_TILES
					: definition.engagementRange;
		}

		private int minimumDamage() {
			return definition == null ? 1 : definition.minimumDamage;
		}

		private int maximumDamage() {
			return definition == null
					? Math.max(1, mob.damageRoll())
					: definition.maximumDamage;
		}
	}
}
