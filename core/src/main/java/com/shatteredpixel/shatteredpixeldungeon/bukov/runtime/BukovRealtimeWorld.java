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
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyAbilityRuntimePolicy;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatIntent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyTier;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.GridLineOfSight;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyTactics;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereSignal;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAtmosphereSignalSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovConcurrentSoundPlayer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovSamplePlaybackSink;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.CombatFeedbackAudioCue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.FootstepCadence;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.FootstepSurface;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAcousticSpace;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAcousticSpaceResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAudioPlan;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotAudioResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.GunshotSoundFamily;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualizationResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.KeySoundVisualizationSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.PlayerSoundEventBuffer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.ReloadAudioCue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.ReloadAudioCueResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundConcurrencyBudget;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SpatialAudioModel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.FireControl;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitZoneGeometry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeBodySpatialIndex;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.RealtimeArmorState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;
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
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovCombatHudTimeline;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchControls;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialGuide;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialHintSource;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialHintState;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovEnemySprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovInteractionMarker;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovWhiteLineSprite;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
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
	private static final float PUMP_BROADCAST_RADIUS_TILES = 26f;
	private static final float PUMP_BROADCAST_LIFETIME_SECONDS = 1.5f;
	private static final float WHITE_LINE_PHASE_TWO_PULSE_SECONDS = 3.2f;
	private static final float WHITE_LINE_PHASE_THREE_PULSE_SECONDS = 2.2f;
	private static final float CAMERA_HALF_DEAD_ZONE_X = 12f;
	private static final float CAMERA_HALF_DEAD_ZONE_Y = 8f;
	private static final float CAMERA_RESPONSIVENESS = 8f;
	private static final float SHORT_SFX_TIMEOUT_SECONDS = 0.40f;
	private static final float FOOTSTEP_TIMEOUT_SECONDS = 0.35f;
	private static final float GUNSHOT_TIMEOUT_SECONDS = 0.85f;
	private static final float CRITICAL_CUE_TIMEOUT_SECONDS = 0.85f;
	private static final PointF ZERO_CAMERA_SHIFT = new PointF();

	private enum SpawnVisibility {
		OFFSCREEN_ONLY,
		VISIBLE_REQUIRED,
		ANY_SAFE
	}

	interface ExtractionLookup {
		int cell(String extractionId);
		boolean available(ExtractionState extraction, float elapsed);
	}

	private final Hero hero;
	private final BukovRaidCoordinator raid;
	private final BukovRaidPersistence persistence;
	private final BukovTutorialGuide tutorialGuide;
	private final BukovRaidMode raidMode;
	private final boolean missionEnabled;
	private final ThemeDefinition raidTheme;
	private final BukovHeapLootAdapter lootAdapter;
	private final RealtimeStatusState medicalStatus;
	private final RealtimeMedicalSystem medicalSystem;
	private final RealtimeBody heroBody;
	private final CollisionMap collisionMap;
	private final GridCollision collision;
	private final ExtractionLookup extractionLookup =
			new ExtractionLookup() {
				@Override
				public int cell(String extractionId) {
					return resolveExtractionCell(extractionId);
				}

				@Override
				public boolean available(
						ExtractionState extraction, float elapsed) {
					return extractionAvailable(extraction, elapsed);
				}
			};
	private final int extractionCell;
	private final int pumpCell;
	private final int missionGateCell;
	private final int[] missionGateCells;
	private final boolean presentationObjectsEnabled;
	private final RealtimeInput input = new RealtimeInput();
	private final BukovSprintState sprintState = new BukovSprintState();
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
	private final BukovConcurrentSoundPlayer worldSounds =
			BukovConcurrentSoundPlayer.production(
					new BukovSamplePlaybackSink());
	private final SpatialAudioModel.Result aiSoundSpatial =
			new SpatialAudioModel.Result();
	private final SpatialAudioModel.Result playbackSpatial =
			new SpatialAudioModel.Result();
	private final GunshotAudioPlan gunshotAudio = new GunshotAudioPlan();
	private final KeySoundVisualEvent keySoundVisual =
			new KeySoundVisualEvent();
	private final float keySoundLifetimeSeconds =
			BukovUiTokens.loadDefault().motionSeconds("hud.soundRing");
	private final PlayerSoundEventBuffer playerSounds =
			new PlayerSoundEventBuffer();
	private final FootstepCadence footstepCadence =
			new FootstepCadence();
	private final BukovCombatHudTimeline combatHudTimeline =
			new BukovCombatHudTimeline();
	private final HitscanResolver.Hit shotHit = new HitscanResolver.Hit();
	private final HitscanResolver.Hit enemyShotHit = new HitscanResolver.Hit();
	private final PointF assistedAim = new PointF();
	private final ArrayList<RealtimeBody> targetBodies = new ArrayList<>();
	private final RealtimeBodySpatialIndex targetSpatialIndex;
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
	private BukovInteractionMarker missionArchiveMarker;
	private final ArrayList<EnemyRuntime> enemies = new ArrayList<>();
	private final IdentityHashMap<Mob, EnemyRuntime> enemiesByMob =
			new IdentityHashMap<>();
	private final ArrayList<Mob> pendingEnemyAttacks = new ArrayList<>();
	private final HitscanResolver.TargetQuery targetQuery =
			new HitscanResolver.TargetQuery() {
				@Override
				public Iterable<RealtimeBody> candidates(
						float minX,
						float minY,
						float maxX,
						float maxY) {
					return targetSpatialIndex.candidates(
							minX,
							minY,
							maxX,
							maxY);
				}

				@Override
				public RealtimeDamage.HitZone hitZone(
						RealtimeBody body,
						float originX,
						float originY,
						float directionX,
						float directionY) {
					Char target = charsByBody.get(body);
					EnemyRuntime enemy = target instanceof Mob
							? enemiesByMob.get((Mob)target) : null;
					boolean boss = enemy != null
							&& enemy.bossState != null;
					return HitZoneGeometry.resolve(
							body,
							originX,
							originY,
							directionX,
							directionY,
							boss,
							boss && enemy.bossState.vulnerable());
				}
			};
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
	private float autoPickupRetryCooldown;
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
	private CombatFeedbackType killConfirmFeedback = CombatFeedbackType.KILL;
	private long extractionCompleteSoundToken =
			SoundConcurrencyBudget.NO_TOKEN;
	private int lastMedicalCountdown = Integer.MIN_VALUE;
	private String activeContainerId;
	private float nextEnemySpawnSeconds;
	private int playerFxSequence;
	private int audioSequence;
	private int footstepSequence;
	private int keySoundSequence;
	private int transientKillCount;
	private float environmentStepNoiseRemaining;
	private ExtractionState.Interaction extractionInteraction =
			ExtractionState.Interaction.NONE;
	private BukovTutorialEvent tutorialEvent;
	private float tutorialRemaining;
	private Room lastHudRoom;
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
		this(hero, raid, persistenceCommit, true);
	}

	BukovRealtimeWorld(
			Hero hero,
			BukovRaidCoordinator raid,
			BukovRaidPersistence.Commit persistenceCommit,
			boolean createPresentationObjects) {
		if (hero == null || Dungeon.level == null) {
			throw new IllegalArgumentException("hero and level are required");
		}
		if (raid != null && persistenceCommit == null) {
			throw new IllegalArgumentException(
					"raid persistence is required");
		}
		this.hero = hero;
		this.raid = raid;
		presentationObjectsEnabled = createPresentationObjects;
		playerSounds.restore(
				raid == null ? null : raid.playerSoundEvents());
		raidMode = raid == null
				? BukovRaidMode.EXPEDITION : raid.session().raidMode();
		missionEnabled = raid != null && raid.firstRaidMissionActive();
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
		collisionMap = new LevelCollisionMap(
				Dungeon.level,
				cell -> {
					GameScene.updateMap(cell);
					refreshHeroVisibility();
				});
		collision = new GridCollision(collisionMap);
		targetSpatialIndex = new RealtimeBodySpatialIndex(
				collisionMap.width(),
				collisionMap.height());
		extractionCell = resolveExtractionCell(BASELINE_EXTRACTION_ID);
		pumpCell = resolvePumpCell();
		missionGateCells = resolveMissionGateCells();
		missionGateCell = missionGateCells.length == 0
				? -1 : missionGateCells[0];
		if (missionEnabled && Dungeon.level instanceof BukovLevel
				&& missionGateCell < 0) {
			throw new IllegalStateException(
					"Bukov first-raid layout is missing the mission gate anchor");
		}
		missionGateUnlocked = !missionEnabled
				|| raid.eventCompleted(FirstRaidMission.EVENT_ID);
		applyMissionGateTerrain();
		if (recoverHeroCheckpoint(hero, heroBody, collisionMap)) {
			refreshHeroVisibility();
		}
		lastHudRoom = currentHudRoom();
		recordBalanceRoom();
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
		reconcileLegacyBossContractCheckpoint();
		publishRealtimeState();
		targetRefreshRemaining = TARGET_REFRESH_SECONDS;
		nextEnemySpawnSeconds = raid == null
				? Float.MAX_VALUE
				: nextSpawnBoundary(raid.session().elapsedSeconds);
		input.start();
		ensureContainerMarkers();
		if (createPresentationObjects) {
			createInteractionMarkers();
		}
	}

	static boolean recoverHeroCheckpoint(
			Hero hero,
			RealtimeBody body,
			CollisionMap collisionMap) {
		if (hero == null || body == null || collisionMap == null) {
			throw new IllegalArgumentException(
					"hero, body, and collision map are required");
		}
		int restoredHeroCell = hero.pos;
		float restoredBodyX = body.x;
		float restoredBodyY = body.y;
		hero.pos = RealtimeHeroBodyRecovery.repair(
				body,
				hero.pos,
				collisionMap);
		return hero.pos != restoredHeroCell
				|| body.x != restoredBodyX
				|| body.y != restoredBodyY;
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

	public boolean reloadActionAvailable() {
		resolveEquippedFirearm();
		return equippedFirearm != null
				&& equippedDefinition != null
				&& FireControl.canStartReload(
						equippedFirearm.magazineAmmo(),
						equippedDefinition.magazineSize,
						reserveAmmo(equippedDefinition.caliber),
						fireControl.isReloading());
	}

	public boolean medicalActionAvailable() {
		return medicalSystem != null && medicalSystem.canBeginAny();
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
		// The same physical event can close the window before the next
		// RealtimeRaidSystem.paused() check. Drop it at the transition so it
		// cannot reopen the backpack or become a shot on the resumed frame.
		input.suppressInterfaceInputUntilRelease();
	}

	public boolean consumeBackpackRequested() {
		boolean requested = backpackRequested;
		backpackRequested = false;
		return requested;
	}

	@Override
	public String raidObjective() {
		if (raidMode.trainingGround()) {
			return trainingObjective();
		}
		if (missionEnabled) {
			return raid.firstRaidObjective();
		}
		return FirstRaidMission.objective(
				FirstRaidMission.Stage.EXTRACT);
	}

	static String trainingObjective() {
		return BukovMessages.get("bukov.raid.touch.movement")
				+ " · " + BukovMessages.get("bukov.raid.touch.aim_fire")
				+ " · " + BukovMessages.get("bukov.raid.touch.reload")
				+ " · " + BukovMessages.get(
						"bukov.raid.hud.interaction_extract");
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
					BukovBackpackViewModel.localizedFirearmName(
							equippedDefinition),
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
		target.mobility(
				sprintState.staminaFraction(),
				sprintState.sprinting(),
				carriedLoadFraction());
		target.presentationSettings(
				SPDSettings.bukovColorblindAssist(),
				SPDSettings.bukovDamageNumbers());
		if (SPDSettings.bukovSoundVisualization()) {
			target.sound(keySoundVisual);
		}
		combatHudTimeline.copyTo(target);
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
						BukovMessages.get(
								"bukov.raid.runtime.extraction_label_format",
								activeExtractionId),
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
						BukovMessages.get(
								"bukov.raid.runtime.extracting"),
						active.progressFraction(),
						active.interactionSeconds());
				return;
			}
		}

		ExtractionState nearbyExtraction = nearestExtraction(
				hero.pos,
				elapsed,
				1,
				false);
		int nearbyExtractionCell = nearbyExtraction == null
				? -1
				: resolveExtractionCell(
						nearbyExtraction.extractionId());
		boolean insideExtraction = nearbyExtractionCell == hero.pos;
		target.extraction(
				availableExtractions,
				nearbyExtraction == null
						? null : nearbyExtraction.extractionId(),
				nearbyExtraction != null
						&& extractionAvailable(
								nearbyExtraction, elapsed),
				false,
				0f,
				nearbyExtraction == null
						? 0f : nearbyExtraction.interactionSeconds());

		if (medicalSystem != null && medicalSystem.isUsing()) {
			target.interaction(
					BukovRaidHudState.Interaction.MEDICAL,
					BukovMessages.get(
							"bukov.raid.runtime.medical_in_progress"),
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
						containerSearchLabel(active.lootTableId, true),
						active.progressFraction,
						active.searchSeconds);
				return;
			}
		}

		if (nearbyExtraction != null && insideExtraction) {
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.EXTRACTION,
					nearbyExtractionCell,
					BukovMessages.get(
							"bukov.raid.runtime.extraction_label_format",
							nearbyExtraction.extractionId()),
					extractionAvailable(nearbyExtraction, elapsed));
			target.interaction(
					extractionAvailable(nearbyExtraction, elapsed)
							? BukovRaidHudState.Interaction.EXTRACT
							: BukovRaidHudState.Interaction.LOCKED,
					extractionAvailable(nearbyExtraction, elapsed)
							? BukovMessages.get(
									"bukov.raid.runtime.start_extraction_format",
									nearbyExtraction.extractionId())
							: BukovMessages.get(
									"bukov.raid.runtime.extraction_locked_format",
									nearbyExtraction.extractionId()),
					0f,
					nearbyExtraction.interactionSeconds());
			return;
		}

		BukovRaidCoordinator.ContainerSnapshot nearby =
				containerWithinRange(hero.pos);
		int heapCell = nearbyLootHeapCell();
		if (withinInteractionRange(hero.pos, pumpCell)
				&& (nearby == null || nearby.cell != hero.pos)
				&& heapCell != hero.pos) {
			ExtractionState conditional =
					raid.extraction(CONDITIONAL_EXTRACTION_ID);
			boolean ready = conditional != null
					&& conditional.conditionMet();
			target.interaction(
					ready
							? BukovRaidHudState.Interaction.LOCKED
							: BukovRaidHudState.Interaction.PUMP,
					ready
							? BukovMessages.get(
									"bukov.raid.runtime.pump_power_ready")
							: BukovMessages.get(
									"bukov.raid.runtime.start_pump"),
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.MISSION,
					pumpCell,
					ready
							? BukovMessages.get(
									"bukov.raid.runtime.pump_started")
							: BukovMessages.get(
									"bukov.raid.runtime.start_pump"),
					!ready);
			return;
		}

		if (nearby != null) {
			boolean locked = nearby.state
					== BukovSearchableContainer.State.LOCKED;
			boolean maintenanceLock =
					locked && isMaintenanceCache(nearby);
			boolean maintenanceUnlock =
					maintenanceLock && hasMaintenanceKey();
			target.interaction(
					maintenanceUnlock
							? BukovRaidHudState.Interaction.UNLOCK
							: locked
							? BukovRaidHudState.Interaction.LOCKED
							: BukovRaidHudState.Interaction.SEARCH,
					maintenanceUnlock
							? BukovMessages.get(
									"bukov.raid.runtime.unlock_with_maintenance_key")
							: maintenanceLock
									? BukovMessages.get(
											"bukov.raid.runtime.maintenance_key_required")
							: locked
									? BukovMessages.get(
											"bukov.raid.runtime.container_locked")
									: containerSearchLabel(
											nearby.lootTableId,
											false),
					0f,
					nearby.searchSeconds);
			return;
		}

		if (heapCell >= 0) {
			Heap nearbyHeap = Dungeon.level.heaps.get(heapCell);
			target.interaction(
					BukovRaidHudState.Interaction.PICKUP,
					heapPickupLabel(nearbyHeap),
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.PICKUP,
					heapCell,
					containsMissionArchive(nearbyHeap)
							? BukovMessages.get(
									"bukov.raid.runtime.pickup_archive")
							: BukovMessages.get(
									"bukov.raid.runtime.loot_available"),
					true);
			return;
		}

		if (nearbyExtraction != null) {
			boolean available =
					extractionAvailable(nearbyExtraction, elapsed);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.EXTRACTION,
					nearbyExtractionCell,
					BukovMessages.get(
							"bukov.raid.runtime.extraction_label_format",
							nearbyExtraction.extractionId()),
					available);
			target.interaction(
					BukovRaidHudState.Interaction.LOCKED,
					extractionApproachLabel(
							nearbyExtraction.extractionId(),
							available),
					0f,
					0f);
			return;
		}

		int nearbyGateCell = nearestMissionGateCell(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				missionGateCells);
		if (missionEnabled
				&& !missionGateUnlocked
				&& withinInteractionRange(hero.pos, nearbyGateCell)) {
			target.interaction(
					BukovRaidHudState.Interaction.LOCKED,
					BukovMessages.get(
							"bukov.raid.runtime.gate_locked_hint"),
					0f,
					0f);
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.MISSION,
					nearbyGateCell,
					BukovMessages.get(
							"bukov.raid.runtime.maintenance_gate"),
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
		target.message = tutorialEvent.message();
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
			// The realtime key listener observes the raw signal, so an open
			// window no longer consumes keystrokes before it sees them. A
			// paused frame never calls sample(), so latched edges and held
			// states would otherwise survive the window and fire on resume
			// (reopening the backpack, or shooting the moment it closes).
			input.suppressInterfaceInputUntilRelease();
			preserveExtractionCompleteCue();
			worldSounds.stopAll();
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
	public void sampleInput() {
		input.sample(heroBody);
	}

	@Override
	public void pollInput() {
		inputFrame = input.consumeFixedStep();
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
		boolean movementIntent =
				inputFrame.movement.x != 0f
						|| inputFrame.movement.y != 0f;
		float carriedLoadFraction = carriedLoadFraction();
		movementMultiplier *= sprintState.speedMultiplier(
				inputFrame.sprintHeld,
				movementIntent,
				carriedLoadFraction);
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
		if (missionEnabled
				&& !missionGateUnlocked
				&& missionGateHintCooldown <= 0f
				&& movementWasBlocked(deltaX, deltaY)
				&& movementPointsTowardMissionGate(deltaX, deltaY)) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.gate_locked_status"));
			missionGateHintCooldown = 1.25f;
		}
		moving = Math.abs(heroBody.x - heroBody.previousX) > 0.00001f
				|| Math.abs(heroBody.y - heroBody.previousY) > 0.00001f;
		sprintState.fixedStep(
				dt,
				inputFrame.sprintHeld,
				moving,
				carriedLoadFraction);
		if (footstepCadence.advance(
				heroBody.x - heroBody.previousX,
				heroBody.y - heroBody.previousY,
				dt)) {
			FootstepSurface footstepSurface = FootstepSurface.resolve(
					heroTerrain(),
					raidTheme == null
							? null : raidTheme.environmentRules);
			playSfx(
					footstepSurface.asset(footstepSequence),
					footstepSurface.gain(),
					footstepSurface.pitch(footstepSequence),
					SoundCategory.FOOTSTEP);
			footstepSequence++;
		}

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
			Room room = currentHudRoom();
			if (room != null && room != lastHudRoom) {
				if (lastHudRoom != null) {
					combatHudTimeline.activity();
				}
				lastHudRoom = room;
				recordBalanceRoom();
			}
			refreshHeroVisibility();
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
		equippedFirearm.cool(dt);
		boolean reloadInterrupted = fireControl.isReloading()
				&& reloadInterruptRequested(inputFrame);
		if (reloadInterrupted) {
			fireControl.cancelReload();
			emitReloadEndPresentation();
		}
		applyPlayerAimAssist(equippedDefinition.effectiveRangeTiles);
		boolean aimReady =
				inputFrame.aim.x != 0f || inputFrame.aim.y != 0f;
		boolean aimedPress = aimReady
				&& (inputFrame.firePressed
						|| inputFrame.fireHeld && !fireAimReadyLastStep);
		boolean reloadAvailable = reloadActionAvailable();
		if (!reloadInterrupted
				&& inputFrame.reloadPressed
				&& !reloadAvailable
				&& !fireControl.isReloading()
				&& equippedFirearm.magazineAmmo()
						< equippedDefinition.magazineSize
				&& reserveAmmo(equippedDefinition.caliber) <= 0) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.no_reserve_ammo"));
		}
		fireControl.update(
				dt,
				aimReady && inputFrame.fireHeld,
				aimedPress,
				!reloadInterrupted
						&& inputFrame.reloadPressed
						&& reloadAvailable,
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

	static boolean reloadInterruptRequested(InputFrame input) {
		if (input == null) {
			return false;
		}
		// Reloading while moving is intentional on both keyboard and the
		// continuously-held iOS stick. Deliberate actions cancel it.
		return input.firePressed
				|| input.fireHeld
				|| input.interactPressed
				|| input.interactHeld
				|| input.medicalPressed
				|| input.dropPressed
				|| input.backpackPressed;
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
		worldSounds.update(dt);
		keySoundVisual.advance(dt);
		combatHudTimeline.advance(dt);
		if (combatHudTimeline.consumeKillSoundCue()) {
			playCombatFeedbackCue(killConfirmFeedback);
			killConfirmFeedback = CombatFeedbackType.KILL;
		}
		playerSounds.advance(dt);
		if (playerSounds.activeCount() == 0) {
			return;
		}
		int newestSequence = playerSounds.latestSequence();
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()) continue;
			PlayerSoundEventBuffer.Event best = null;
			float bestThreat = -1f;
			float bestDistance = Float.MAX_VALUE;
			for (int slot = 0; slot < playerSounds.capacity(); slot++) {
				PlayerSoundEventBuffer.Event event =
						playerSounds.eventAt(slot);
				if (!event.active()
						|| event.sequence()
								<= enemy.heardSoundSequence) {
					continue;
				}
				float themedSoundRadius = event.radius()
						* (raidTheme == null
								? 1f
								: raidTheme.environmentRules
										.enemyHearingMultiplier(
												terrainAt(
														event.x(),
														event.y())))
						* EnemyAbilityRuntimePolicy.hearingMultiplier(
								enemy.definition);
				float dx = event.x() - enemy.body.x;
				float dy = event.y() - enemy.body.y;
				float distanceSquared = dx * dx + dy * dy;
				if (distanceSquared
						> themedSoundRadius * themedSoundRadius) {
					continue;
				}
				float distance = (float)Math.sqrt(distanceSquared);
				float wallOcclusion = blockedCellsOnLine(
						enemy.body.x,
						enemy.body.y,
						event.x(),
						event.y());
				SpatialAudioModel.resolve(
						audioContract,
						1f,
						distance,
						wallOcclusion,
						false,
						aiSoundSpatial);
				if (!aiSoundSpatial.perceivable()) continue;
				float threat = themedSoundRadius
						* aiSoundSpatial.perceptionGain();
				if (best == null
						|| threat > bestThreat
						|| threat == bestThreat
								&& distance < bestDistance) {
					best = event;
					bestThreat = threat;
					bestDistance = distance;
				}
			}
			if (best == null) continue;
			// One decision consumes the current batch for this enemy. Every
			// enemy owns its cursor, so simultaneous sounds can resolve to
			// different best sources without either source being overwritten.
			enemy.heardSoundSequence = newestSequence;
			enemy.brain.recordSound(best.x(), best.y());
			if (hasAbility(enemy, "INVESTIGATE_SOUND")
					|| hasAbility(enemy, "CALL_INVESTIGATORS")) {
				showEnemyStatus(
						enemy,
						CharSprite.WARNING,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_heard_noise"));
			}
		}
	}

	@Override
	public void updatePerception(float dt) {
		for (EnemyRuntime enemy : enemies) {
			if (!enemy.mob.isAlive()) {
				enemy.brain.markDead();
				deactivateEnemyBody(enemy);
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
		boolean combatActive = false;
		boolean searchingAfterContact = false;
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
			enemy.navigator.step(
					dt,
					enemy.body.x,
					enemy.body.y,
					enemy.brain.navigationTargetX(),
					enemy.brain.navigationTargetY(),
					enemy.brain.seesPlayer(),
					enemy.brain.desiredX(),
					enemy.brain.desiredY(),
					collisionMap,
					enemy.navigationIntent
			);
			enemy.brain.observeNavigation(
					enemy.navigationIntent.targetUnreachable(),
					enemy.body.x,
					enemy.body.y);
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
			switch (enemy.brain.state()) {
				case ATTACK:
				case CHASE:
					combatActive = true;
					break;
				case SEARCH:
					searchingAfterContact = true;
					break;
				default:
					break;
			}
		}
		if (raid != null) {
			raid.updateBalanceFirefightState(
					combatActive,
					searchingAfterContact);
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
			float ballisticDamage = event.damage;
			EnemyRuntime enemy = target instanceof Mob
					? enemiesByMob.get((Mob)target)
					: null;
			if (enemy != null && enemy.armor != null) {
				float armored = resolveEnemyArmor(
						enemy.armor,
						ballisticDamage,
						event.penetration,
						event.hitZone);
				if (armored < ballisticDamage) {
					showEnemyStatus(
							enemy,
							CharSprite.NEUTRAL,
							BukovMessages.get(
									"bukov.raid.runtime.armor_absorbed"));
				}
				ballisticDamage = armored;
			}
			int damage = Math.max(1, Math.round(ballisticDamage));
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
				if (enemy != null && enemy.bossState != null) {
					boolean wasAlive = target.isAlive();
					WhiteLineBossStateMachine.Result bossResult =
							resolveWhiteLineDamage(enemy, damage);
					emitEnemyHitOutcome(
							target,
							wasAlive,
							damage,
							bossHitFeedback(bossResult),
							bossDeathFeedback(bossResult));
					continue;
				}
			}
			// Keep Mob.damage/die authoritative so XP, loot rolls, death VFX, and
			// Dungeon.level.mobs removal stay in the host implementation.
			boolean wasAlive = target.isAlive();
			target.damageWithoutFloatingText(damage, hero);
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
				targetSpatialIndex.remove(target.realtimeBody);
				if (target instanceof Mob) {
					EnemyRuntime deadEnemy =
							enemiesByMob.get((Mob)target);
					if (deadEnemy != null) {
						deadEnemy.brain.markDead();
					}
				}
			}
		}
		pendingHits.clear();

		for (PendingEnemyShot event : pendingEnemyShots) {
			if (!queuedEnemyShotCanDamage(event.attacker, hero)) {
				if (!hero.isAlive()) {
					break;
				}
				continue;
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
			damage = raidMode.incomingDamage(damage, hero.HP);
			if (damage > 0) {
				playSfx(
						Assets.Sounds.Bukov.BULLET_HIT,
						0.75f,
						nextAudioPitch(1f, 0.04f)
				);
				boolean wasAlive = hero.isAlive();
				int healthBefore = hero.HP;
				hero.damageWithoutFloatingText(damage, event.attacker);
				int appliedDamage = Math.max(0, healthBefore - hero.HP);
				if (raid != null && appliedDamage > 0) {
					raid.recordDamageTaken(appliedDamage);
				}
				emitPlayerHitOutcome(
						event.attacker,
						wasAlive,
						appliedDamage);
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
			if (inputFrame.medicalSlot > 0) {
				beginMedicalQuickSlot(inputFrame.medicalSlot);
			} else {
				beginBestAvailableMedical();
			}
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
		autoPickupRetryCooldown = Math.max(
				0f,
				autoPickupRetryCooldown - dt
		);
		missionEventRetryCooldown = Math.max(
				0f,
				missionEventRetryCooldown - dt
		);
		if (raid == null || inputFrame == null) {
			return;
		}
		applyModeConvergence();
		if (missionEnabled
				&& !missionGateUnlocked
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
					damageTaken,
					hero.HT
			);
			if (extractionInteraction == ExtractionState.Interaction.ACTIVE) {
				showExtractionCountdown(extraction);
			} else if (extractionInteraction
					== ExtractionState.Interaction.LIGHT_HIT) {
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.extraction_disrupted"));
			} else if (extractionInteraction != ExtractionState.Interaction.NONE) {
				lastExtractionCountdown = Integer.MIN_VALUE;
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.extraction_interrupted"));
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
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.search_interrupted"));
					checkpointLootChange();
					return;
				}
				if (result == BukovSearchableContainer.UpdateResult.COMPLETED) {
					activeContainerId = null;
					lastContainerCountdown = Integer.MIN_VALUE;
					playSfx(
							Assets.Sounds.Bukov.SEARCH_COMPLETE,
							0.72f,
							nextAudioPitch(1f, 0.02f),
							SoundCategory.UI
					);
					releaseCompletedContainer(active.containerId);
					if (FirstRaidMission.ARCHIVE_LOOT_TABLE_ID.equals(
							active.lootTableId)) {
						showHeroStatus(BukovMessages.get(
								"bukov.raid.runtime.archive_cabinet_opened"));
					} else if (FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(
							active.lootTableId)) {
						showHeroStatus(BukovMessages.get(
								"bukov.raid.runtime.high_value_confirmed"));
					}
					return;
				}
			}
		}

		ExtractionState nearbyExtraction = nearestExtraction(
				hero.pos,
				raid.session().elapsedSeconds,
				1,
				false);
		if (nearbyExtraction != null) {
			showTutorial(BukovTutorialEvent.EXTRACTION_NEAR);
		}

		if (autoPickupRetryCooldown <= 0f
				&& pickupNearbyAutomaticItem()) {
			return;
		}

		if (!interactPressed) {
			return;
		}

		if (completeNearbyBossObjective()) {
			return;
		}

		BukovRaidCoordinator.ContainerSnapshot container =
				containerWithinRange(hero.pos);
		int heapCell = nearbyLootHeapCell();
		if (withinInteractionRange(hero.pos, pumpCell)
				&& (container == null || container.cell != hero.pos)
				&& heapCell != hero.pos) {
			activatePump();
			return;
		}

		if (container != null) {
			if (container.state == BukovSearchableContainer.State.SEARCHED
					&& !container.contentsReleased) {
				releaseCompletedContainer(container.containerId);
			} else if (container.state == BukovSearchableContainer.State.LOCKED) {
				if (isMaintenanceCache(container)) {
					unlockMaintenanceCache(container);
				} else {
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.container_locked"));
				}
			} else if (raid.beginContainerSearch(container.containerId)) {
				activeContainerId = container.containerId;
				lastContainerCountdown = Integer.MIN_VALUE;
				showTutorial(BukovTutorialEvent.CONTAINER_OPENED);
				showContainerCountdown(raid.container(container.containerId));
			}
			return;
		}

		ExtractionState extraction = nearbyExtraction;
		if (extraction != null
				&& hero.pos == resolveExtractionCell(
						extraction.extractionId())) {
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
						1f,
						SoundCategory.EXTRACTION_CUE
				);
				lastExtractionCountdown = Integer.MIN_VALUE;
				extractionInteraction = ExtractionState.Interaction.ACTIVE;
				showExtractionCountdown(extraction);
			} else if (extractionHintCooldown <= 0f) {
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.extraction_unavailable"));
				extractionHintCooldown = 0.75f;
			}
			return;
		}

		if (heapCell >= 0) {
			pickupHeap(heapCell, false);
		}
	}

	@Override
	public ExtractionState.Interaction extractionInteraction() {
		return extractionInteraction;
	}

	private void pickupNearbyHeap() {
		int heapCell = nearbyLootHeapCell();
		if (heapCell < 0) {
			return;
		}
		pickupHeap(heapCell, false);
	}

	private int nearbyLootHeapCell() {
		return selectVisibleLootHeap(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				Dungeon.level.heroFOV,
				Dungeon.level.heaps,
				extractionCell
		);
	}

	private boolean pickupNearbyAutomaticItem() {
		int heapCell = selectVisibleAutoPickupHeap(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				Dungeon.level.heroFOV,
				Dungeon.level.heaps,
				extractionCell
		);
		if (heapCell < 0) {
			return false;
		}
		LootTransaction.PickupResult result =
				pickupHeap(heapCell, true);
		if (result != LootTransaction.PickupResult.ADDED) {
			autoPickupRetryCooldown = 0.75f;
		}
		return result == LootTransaction.PickupResult.ADDED;
	}

	private LootTransaction.PickupResult pickupHeap(
			int heapCell,
			boolean automatic) {
		Heap heap = Dungeon.level.heaps.get(heapCell);
		if (heap == null || heap.peek() == null) {
			return LootTransaction.PickupResult.DUPLICATE_UID;
		}
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
						BukovMessages.get(
								"bukov.raid.runtime.pickup_result_format",
								BukovBackpackViewModel.localizedDisplayName(
										carried.definitionId(),
										firearmRegistry),
								formatWeight(carried.totalWeight()),
								carried.totalValue())
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
			if (!automatic) {
				showTutorial(BukovTutorialEvent.OVERWEIGHT);
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.overweight"));
			}
		} else if (!automatic) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.item_already_picked_up"));
		}
		return result;
	}

	private void dropLatestCarriedItem() {
		List<RaidItem> carried = raid.loot().items();
		if (carried.isEmpty()) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.no_droppable_item"));
			return;
		}
		RaidItem latest = carried.get(carried.size() - 1);
		dropCarriedItem(latest.itemUid());
	}

	public BukovHeapLootAdapter.DropResult dropCarriedItem(String itemUid) {
		RaidItem carriedItem = carried(itemUid);
		if (carriedItem == null) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.item_not_in_backpack"));
			return BukovHeapLootAdapter.DropResult.UNKNOWN_UID;
		}
		if (FirstRaidMission.isArchive(carriedItem)) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.archive_cannot_drop"));
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.cannot_drop_here"));
			return BukovHeapLootAdapter.DropResult.UNKNOWN_UID;
		}

		Item hostItem = lootAdapter.carriedHostItem(itemUid);
		BukovHeapLootAdapter.DropResult dropResult =
				lootAdapter.drop(itemUid, heap, hero);
		if (dropResult != BukovHeapLootAdapter.DropResult.DROPPED) {
			showHeroStatus(
					dropResult == BukovHeapLootAdapter.DropResult.PROTECTED_ITEM
							? BukovMessages.get(
									"bukov.raid.runtime.mission_item_cannot_drop")
							: dropResult
									== BukovHeapLootAdapter.DropResult.IN_USE_ITEM
									? BukovMessages.get(
											"bukov.raid.runtime.in_use_item_cannot_drop")
									: BukovMessages.get(
											"bukov.raid.runtime.drop_failed"));
			return dropResult;
		}
		if (created) {
			Dungeon.level.heaps.put(hero.pos, heap);
			GameScene.add(heap);
		}
		showHeroStatus(
				BukovMessages.get(
						"bukov.raid.runtime.drop_result_format",
						hostItem == null
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.maintenance_key_required"));
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
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.maintenance_cache_unlocked"));
		return true;
	}

	private boolean unlockMissionGateIfCarried() {
		if (!missionEnabled) return true;
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
					1f,
					SoundCategory.UI
			);
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.archive_verified"));
			return true;
		} catch (IOException failure) {
			ShatteredPixelDungeon.reportException(failure);
			missionEventRetryCooldown = 2f;
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.archive_save_retry"));
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.checkpoint_save_failed"));
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
							? null : enemy.rangedCombat.snapshot(),
					enemy.heardSoundSequence));
		}
		raid.updateRealtimeState(
				medicalStatus,
				medicalSystem.snapshot(),
				snapshots,
				playerSounds.snapshot());
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
				? BukovMessages.get(
						"bukov.raid.runtime.medical_not_needed")
				: BukovMessages.get(
						"bukov.raid.runtime.no_medical_available"));
	}

	private void beginMedicalQuickSlot(int slot) {
		RealtimeMedicalSystem.BeginResult finalResult =
				RealtimeMedicalSystem.BeginResult.UNKNOWN_ITEM;
		for (String itemUid :
				BukovMedicalQuickSlots.candidateItemUids(
						raid.loot(),
						slot)) {
			finalResult = beginMedical(itemUid);
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
				? BukovMessages.get(
						"bukov.raid.runtime.medical_not_needed")
				: BukovMessages.get(
						"bukov.raid.runtime.no_medical_available"));
	}

	private float carriedLoadFraction() {
		if (raid == null) return 0f;
		float maximum = raid.loot().maxWeight();
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.isFinite(maximum)
				|| maximum <= 0f
				|| maximum == Float.MAX_VALUE) {
			return 0f;
		}
		return Math.max(
				0f,
				Math.min(1f, raid.loot().totalWeight() / maximum));
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.medical_start_format",
					item == null
							? BukovMessages.get(
									"bukov.raid.runtime.medical_item")
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.not_equippable_firearm"));
			return false;
		}
		Firearm next = (Firearm)hostItem;
		resolveEquippedFirearm();
		if (next == equippedFirearm) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.weapon_already_equipped"));
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
		resetFireControlForWeaponSwap();
		resolveEquippedFirearm();
		raid.equipFirearm(itemUid);
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.weapon_equipped_format",
				BukovBackpackViewModel.localizedFirearmName(
						equippedDefinition)));
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
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.medical_progress_format",
							percent));
				}
				break;
			case COMPLETED:
				emitMedicalEnded();
				medicalSystem.writeBack(raid.loot());
				syncMedicalHostQuantity(activeUid);
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.medical_completed"));
				lastMedicalCountdown = Integer.MIN_VALUE;
				checkpointLootChange();
				break;
			case INTERRUPTED_DAMAGE:
				emitMedicalEnded();
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.medical_interrupted_damage"));
				break;
			case INTERRUPTED_MOVE:
				emitMedicalEnded();
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.medical_interrupted_move"));
				break;
			case INTERRUPTED_SHOT:
				emitMedicalEnded();
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.medical_interrupted_shot"));
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
		if ("bandage".equals(id)) {
			return BukovMessages.get("bukov.raid.item.bandage");
		}
		if ("first_aid".equals(id)) {
			return BukovMessages.get("bukov.raid.item.first_aid");
		}
		if ("tourniquet".equals(id)) {
			return BukovMessages.get("bukov.raid.item.tourniquet");
		}
		if ("painkiller".equals(id)) {
			return BukovMessages.get("bukov.raid.item.painkiller");
		}
		if ("antiseptic".equals(id)) {
			return BukovMessages.get("bukov.raid.item.antiseptic");
		}
		if ("splint".equals(id)) {
			return BukovMessages.get("bukov.raid.item.splint");
		}
		if ("stim".equals(id)) {
			return BukovMessages.get("bukov.raid.item.stim");
		}
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
		boolean changed = MissionGateTerrain.apply(
				Dungeon.level,
				missionGateCells,
				missionGateUnlocked,
				cell -> GameScene.updateMap(cell));
		if (!changed) return;
		refreshHeroVisibility();
	}

	private boolean movementWasBlocked(float requestedX, float requestedY) {
		float actualX = heroBody.x - heroBody.previousX;
		float actualY = heroBody.y - heroBody.previousY;
		return Math.abs(requestedX - actualX) > 0.0001f
				|| Math.abs(requestedY - actualY) > 0.0001f;
	}

	private boolean movementPointsTowardMissionGate(
			float requestedX, float requestedY) {
		int targetGateCell = nearestMissionGateCell(
				hero.pos,
				Dungeon.level.width(),
				Dungeon.level.length(),
				missionGateCells);
		if (targetGateCell < 0
				|| requestedX == 0f && requestedY == 0f) {
			return false;
		}
		int width = Dungeon.level.width();
		float gateX = targetGateCell % width + 0.5f;
		float gateY = targetGateCell / width + 0.5f;
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
		return selectVisibleLootHeap(
				heroCell,
				width,
				length,
				visible,
				heaps,
				excludedCell,
				false);
	}

	static int selectVisibleAutoPickupHeap(
			int heroCell,
			int width,
			int length,
			boolean[] visible,
			SparseArray<Heap> heaps,
			int excludedCell) {
		return selectVisibleLootHeap(
				heroCell,
				width,
				length,
				visible,
				heaps,
				excludedCell,
				true);
	}

	private static int selectVisibleLootHeap(
			int heroCell,
			int width,
			int length,
			boolean[] visible,
			SparseArray<Heap> heaps,
			int excludedCell,
			boolean automaticOnly) {
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
							&& heap.peek() != null
							&& (!automaticOnly
									|| BukovAutoPickupPolicy.shouldPickup(
											heap.peek()))) {
						return cell;
					}
				}
			}
		}
		return -1;
	}

	static int nearestMissionGateCell(
			int originCell,
			int width,
			int length,
			int[] gateCells) {
		if (originCell < 0 || originCell >= length || width <= 0
				|| gateCells == null || gateCells.length == 0) {
			return -1;
		}
		int originX = originCell % width;
		int originY = originCell / width;
		int nearest = -1;
		int nearestDistance = Integer.MAX_VALUE;
		for (int gateCell : gateCells) {
			if (gateCell < 0 || gateCell >= length) continue;
			int distance = Math.max(
					Math.abs(originX - gateCell % width),
					Math.abs(originY - gateCell / width));
			if (distance < nearestDistance) {
				nearest = gateCell;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	static String containerSearchLabel(
			String lootTableId,
			boolean active) {
		if (FirstRaidMission.ARCHIVE_LOOT_TABLE_ID.equals(lootTableId)) {
			return active
					? BukovMessages.get(
							"bukov.raid.runtime.search_archive_active")
					: BukovMessages.get(
							"bukov.raid.runtime.search_archive");
		}
		if (FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID.equals(lootTableId)) {
			return active
					? BukovMessages.get(
							"bukov.raid.runtime.search_high_value_active")
					: BukovMessages.get(
							"bukov.raid.runtime.search_high_value");
		}
		return active
				? BukovMessages.get(
						"bukov.raid.runtime.search_container_active")
				: BukovMessages.get(
						"bukov.raid.runtime.search_container");
	}

	static String heapPickupLabel(Heap heap) {
		return containsMissionArchive(heap)
				? BukovMessages.get(
						"bukov.raid.runtime.pickup_archive")
				: BukovMessages.get(
						"bukov.raid.runtime.pickup_loot");
	}

	private static boolean containsMissionArchive(Heap heap) {
		if (heap == null) return false;
		for (Item item : heap.items) {
			if (item instanceof BukovMissionArchive) return true;
		}
		return false;
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
			extractionCompleteSoundToken = playSfx(
					Assets.Sounds.Bukov.EXTRACTION_COMPLETE,
					0.88f,
					1f,
					SoundCategory.EXTRACTION_CUE);
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
			if (enemy.brain.desiredX() != 0f
					|| enemy.brain.desiredY() != 0f) {
				int facingX = Math.max(
						0,
						Math.min(
								Dungeon.level.width() - 1,
								(int)Math.floor(
										enemy.body.x
												+ enemy.brain.desiredX())));
				int facingY = Math.max(
						0,
						Math.min(
								Dungeon.level.height() - 1,
								(int)Math.floor(
										enemy.body.y
												+ enemy.brain.desiredY())));
				enemy.mob.sprite.turnTo(
						enemy.mob.pos,
						facingX + facingY * Dungeon.level.width());
			}
			if (enemy.bossState != null
					&& enemy.mob.sprite instanceof BukovWhiteLineSprite) {
				WhiteLineBossStateMachine.Phase phase =
						enemy.bossState.phase();
				if (phase != enemy.previousBossPhase) {
					enemy.previousBossPhase = phase;
					if (phase != WhiteLineBossStateMachine.Phase.DORMANT) {
						((BukovWhiteLineSprite)enemy.mob.sprite)
								.realtimePhaseCast(hero.pos);
					}
				}
				((BukovWhiteLineSprite)enemy.mob.sprite).setEncounterVisual(
						bossPhase(phase),
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
		cameraFollow.update(
				hero.sprite.x + hero.sprite.width() * 0.5f,
				hero.sprite.y + hero.sprite.height() * 0.5f,
				renderDelta
		);
		camera.scroll.set(
				cameraFollow.centerX() - camera.width * 0.5f
						+ camera.centerOffset.x,
				cameraFollow.centerY() - camera.height * 0.5f
						+ camera.centerOffset.y
		);
	}

	@Override
	public void disposeRealtimeObjects() {
		preserveExtractionCompleteCue();
		worldSounds.stopAll();
		input.stop();
		inputFrame = null;
		fireControl.resetForWeaponSwap();
		targetBodies.clear();
		targetSpatialIndex.clear();
		enemyShotTargetBodies.clear();
		charsByBody.clear();
		pendingHits.clear();
		pendingEnemyShots.clear();
		combatFx.clear();
		combatPresentation.clear();
		playerSounds.clear();
		enemies.clear();
		enemiesByMob.clear();
		pendingEnemyAttacks.clear();
		for (BukovInteractionMarker marker : interactionMarkers) {
			marker.killAndErase();
		}
		interactionMarkers.clear();
		bossMechanismMarkers.clear();
		missionArchiveMarker = null;
	}

	@Override
	public void fire(Firearm firearm, FirearmDefinition definition) {
		firedShotThisStep = true;
		firearm.recordShot(definition);
		combatHudTimeline.activity();
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
				playerShotFeedback(definition),
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
		spread += firearm.conditionSpreadPenaltyDeg();
		for (int pellet = 0; pellet < definition.pellets; pellet++) {
			float radians = (float)Math.toRadians(
					Random.Float(-spread, spread)
			);
			float cos = (float)Math.cos(radians);
			float sin = (float)Math.sin(radians);
			float directionX = inputFrame.aim.x * cos - inputFrame.aim.y * sin;
			float directionY = inputFrame.aim.x * sin + inputFrame.aim.y * cos;
			float maximumDistance = definition.effectiveRangeTiles * 2f;

			resolvePlayerShot(
					hero.id(),
					fxSequence,
					heroBody.x,
					heroBody.y,
					directionX,
					directionY,
					maximumDistance,
					definition.tracerIntensity,
					collisionMap,
					targetQuery,
					heroBody,
					shotHit,
					combatFx
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
			boolean canDamage = playerShotCanDamage(
					shotHit,
					heroBody,
					hero,
					target);
			if (canDamage) {
				combatFx.bloodMist(
						hero.id(),
						fxSequence,
						false,
						shotHit.x,
						shotHit.y,
						directionX,
						directionY,
						definition.impactIntensity);
				float damage = RealtimeDamage.resolve(
						ammunition.applyDamage(definition.damage),
						1f,
						shotHit.distance,
						definition.effectiveRangeTiles,
						ammunition.applyPenetration(definition.penetration),
						shotHit.zone,
						null
				);
				pendingHits.add(new PendingHit(
						target,
						damage,
						ammunition.applyPenetration(
								definition.penetration),
						shotHit.zone));
			} else if (shotHit.body == null
					&& shotHit.distance < maximumDistance - 0.001f) {
				combatFx.bulletMark(
						hero.id(),
						fxSequence,
						false,
						shotHit.x,
						shotHit.y,
						directionX,
						directionY,
						definition.impactIntensity);
			}
		}
	}

	/**
	 * Production player-shot resolver kept package-visible so the checkpoint
	 * recovery gate can exercise the exact hitscan-to-tracer path.
	 */
	static void resolvePlayerShot(
			int sourceId,
			int sequence,
			float originX,
			float originY,
			float directionX,
			float directionY,
			float maximumDistance,
			float tracerIntensity,
			CollisionMap collisionMap,
			HitscanResolver.TargetQuery targetQuery,
			RealtimeBody ignored,
			HitscanResolver.Hit hit,
			CombatFxEventPool combatFx) {
		HitscanResolver.cast(
				originX,
				originY,
				directionX,
				directionY,
				maximumDistance,
				collisionMap,
				targetQuery,
				ignored,
				hit);
		combatFx.tracer(
				sourceId,
				sequence,
				false,
				originX,
				originY,
				hit.x,
				hit.y,
				tracerIntensity);
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
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.empty_magazine"));
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
				1f,
				seconds);
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.reload"));
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
		emitReloadEndPresentation();
		if (equippedFirearm != null) {
			if (equippedFirearm.magazineAmmo() == 0) {
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.no_reserve_ammo"));
			} else {
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.magazine_count_format",
						equippedFirearm.magazineAmmo()));
			}
		}
	}

	private void emitReloadEndPresentation() {
		combatPresentation.emit(
				CombatPresentationEvent.Type.PLAYER_RELOAD_END,
				hero.id(),
				hero.id(),
				hero.pos,
				playerAimCell(),
				null,
				1f);
	}

	private void resolveEquippedFirearm() {
		KindOfWeapon weapon = hero.belongings.weapon();
		if (weapon instanceof Firearm) {
			Firearm firearm = (Firearm)weapon;
			if (firearm != equippedFirearm) {
				resetFireControlForWeaponSwap();
				equippedFirearm = firearm;
				equippedDefinition = firearm.definition(firearmRegistry);
			}
		} else {
			if (equippedFirearm != null) {
				resetFireControlForWeaponSwap();
			}
			equippedFirearm = null;
			equippedDefinition = null;
		}
	}

	private void resetFireControlForWeaponSwap() {
		boolean reloadWasActive = fireControl.isReloading();
		fireControl.resetForWeaponSwap();
		if (reloadWasActive) {
			combatPresentation.emit(
					CombatPresentationEvent.Type.PLAYER_RELOAD_END,
					hero.id(),
					hero.id(),
					hero.pos,
					playerAimCell(),
					null,
					1f);
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
		if (raid == null) return;
		int liveEnemies = activeNonBossEnemies();
		if (!InitialEnemyRosterPolicy.shouldPopulate(
				raid.session().initialEnemySpawnCompleted(),
				liveEnemies,
				raid.session().killCount(),
				raidMode.initialEnemyCount)) {
			return;
		}
		boolean visibleContact =
				InitialEnemyRosterPolicy.needsVisibleContact(
						raidMode,
						raid.session().raidOrdinal(),
						liveEnemies);
		boolean changed = false;
		while (liveEnemies < raidMode.initialEnemyCount) {
			boolean spawned;
			if (visibleContact) {
				spawned = attemptEnemySpawn(
						SpawnVisibility.VISIBLE_REQUIRED,
						false,
						true);
				if (!spawned) {
					spawned = attemptVisibleInitialContactSpawn();
				}
				if (!spawned) {
					spawned = attemptEnemySpawn(
							SpawnVisibility.ANY_SAFE,
							false,
							true);
				}
				visibleContact = false;
			} else {
				spawned = attemptEnemySpawn(
						SpawnVisibility.OFFSCREEN_ONLY,
						false,
						false);
			}
			if (!spawned) break;
			changed = true;
			liveEnemies = activeNonBossEnemies();
		}
		if (changed) {
			refreshEnemiesAndTargets();
		}
		if (InitialEnemyRosterPolicy.completed(
				liveEnemies,
				raidMode.initialEnemyCount)) {
			raid.session().markInitialEnemySpawnCompleted();
		}
		if (changed || raid.session().initialEnemySpawnCompleted()) {
			checkpointRuntimeCombatState();
		}
	}

	private void updateEnemySpawning() {
		if (raid == null
				|| raid.session().elapsedSeconds < nextEnemySpawnSeconds) {
			return;
		}
		nextEnemySpawnSeconds = nextSpawnBoundary(
				raid.session().elapsedSeconds);
		if (InitialEnemyRosterPolicy.shouldPopulate(
				raid.session().initialEnemySpawnCompleted(),
				activeNonBossEnemies(),
				raid.session().killCount(),
				raidMode.initialEnemyCount)) {
			spawnInitialEnemies();
			return;
		}
		boolean spawned = attemptEnemySpawn(
				SpawnVisibility.OFFSCREEN_ONLY);
		checkpointRuntimeCombatState();
		if (spawned) {
			refreshEnemiesAndTargets();
		}
	}

	private boolean attemptEnemySpawn(SpawnVisibility visibility) {
		return attemptEnemySpawn(visibility, true, false);
	}

	private boolean attemptEnemySpawn(
			SpawnVisibility visibility,
			boolean allowBoss,
			boolean onboardingContact) {
		if (visibility == null) {
			throw new IllegalArgumentException(
					"spawn visibility is required");
		}
		if (enemySpawnPoints.isEmpty()) return false;
		float elapsed = raid.session().elapsedSeconds;
		long spawnEpoch = raid.session().claimEnemySpawnEpoch();
		if (allowBoss && attemptWhiteLineSpawn(elapsed)) return true;
		int start = (int)com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(
				Dungeon.seed + spawnEpoch * 0x9E3779B97F4A7C15L,
				enemySpawnPoints.size());
		for (int offset = 0; offset < enemySpawnPoints.size(); offset++) {
			BukovEnemySpawnPlanner.SpawnPoint point =
					enemySpawnPoints.get((start + offset)
							% enemySpawnPoints.size());
			boolean insidePlayerFieldOfView =
					point.cell >= 0
							&& point.cell < Dungeon.level.length()
							&& Dungeon.level.heroFOV[point.cell];
			boolean occupied = point.cell >= 0
					&& point.cell < Dungeon.level.length()
					&& Actor.findChar(point.cell) != null;
			boolean tooClose = point.cell >= 0
					&& point.cell < Dungeon.level.length()
					&& tooCloseToHero(point.cell);
			if (point.cell < 0
					|| point.cell >= Dungeon.level.length()
					|| visibility == SpawnVisibility.OFFSCREEN_ONLY
							&& insidePlayerFieldOfView
					|| visibility == SpawnVisibility.VISIBLE_REQUIRED
							&& !insidePlayerFieldOfView
					|| occupied
					|| tooClose
					|| point.bossArena && !allowBoss
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
				if (!WhiteLineSpawnPolicy.eligible(
						raidMode,
						selected,
						raid.session().raidOrdinal(),
						elapsed,
						raid.session().firstRaidProtectionActive(),
						whiteLineResolved(),
						activeEnemyCount(selected.id))
						|| !WhiteLineSpawnPolicy.acceptsSpawnPoint(
								true,
								point.cell,
								Dungeon.level.length(),
								insidePlayerFieldOfView,
								occupied,
								tooClose)) {
					continue;
				}
			} else {
				boolean firstRaid = raid.session().raidOrdinal() == 1;
				FirstRaidEnemySpawnDirector.Context context =
					new FirstRaidEnemySpawnDirector.Context(
							elapsed,
							firstRaid,
							point.distanceFromSpawnRooms,
							insidePlayerFieldOfView,
							visibility != SpawnVisibility.OFFSCREEN_ONLY,
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
			if (onboardingContact
					&& !hasOnboardingCombatLane(point.cell)) {
				continue;
			}
			BukovHostMob mob = new BukovHostMob().configure(selected);
			if (onboardingContact) {
				mob.markOnboardingContact();
			}
			mob.pos = point.cell;
			mob.state = mob.WANDERING;
			GameScene.add(mob);
			return true;
		}
		return false;
	}

	/**
	 * The onboarding contact must be visible, reachable, and already inside
	 * perception range. Authored room spawn points remain preferred; this is a
	 * bounded fallback for open training maps and legacy layouts whose room
	 * semantics contain no currently visible spawn point.
	 */
	private boolean attemptVisibleInitialContactSpawn() {
		if (raid == null) return false;
		EnemyArchetypeDefinition contact = enemyArchetypes.require(
				FirstRaidEnemySpawnDirector.FIRST_GUNNER);
		if (activeEnemyCount(contact.id)
				>= (raid.session().raidOrdinal() == 1
						? contact.firstRaidMaximumActive
						: contact.maximumActive)) {
			return false;
		}
		int bestCell = -1;
		float bestDistanceError = Float.POSITIVE_INFINITY;
		float maximumDistance = Math.min(
				contact.perceptionRange,
				contact.engagementRange + 1f);
		float desiredDistance = Math.max(
				MINIMUM_PLAYER_SPAWN_DISTANCE_TILES,
				maximumDistance - 0.5f);
		int width = Dungeon.level.width();
		for (int cell = 0; cell < Dungeon.level.length(); cell++) {
			if (!Dungeon.level.heroFOV[cell]
					|| Actor.findChar(cell) != null
					|| cell == extractionCell
					|| cell == pumpCell
					|| cell == missionGateCell) {
				continue;
			}
			int x = cell % width;
			int y = cell / width;
			if (collisionMap.blocked(x, y)) continue;
			if (!hasOnboardingCombatLane(cell)) continue;
			float centerX = x + 0.5f;
			float centerY = y + 0.5f;
			float deltaX = centerX - heroBody.x;
			float deltaY = centerY - heroBody.y;
			float distance = (float)Math.sqrt(
					deltaX * deltaX + deltaY * deltaY);
			if (distance < MINIMUM_PLAYER_SPAWN_DISTANCE_TILES
					|| distance > maximumDistance
					|| !GridLineOfSight.visible(
							heroBody.x,
							heroBody.y,
							centerX,
							centerY,
							maximumDistance,
							collisionMap)) {
				continue;
			}
			float error = Math.abs(distance - desiredDistance);
			if (error < bestDistanceError
					|| error == bestDistanceError
							&& (bestCell < 0 || cell < bestCell)) {
				bestCell = cell;
				bestDistanceError = error;
			}
		}
		if (bestCell < 0) return false;
		BukovHostMob mob = new BukovHostMob()
				.configure(contact)
				.markOnboardingContact();
		mob.pos = bestCell;
		mob.state = mob.WANDERING;
		GameScene.add(mob);
		return true;
	}

	private boolean hasOnboardingCombatLane(int targetCell) {
		if (targetCell < 0 || targetCell >= Dungeon.level.length()) {
			return false;
		}
		int width = Dungeon.level.width();
		int targetX = targetCell % width;
		int targetY = targetCell / width;
		int[] directions = {-1, 0, 1, 0, 0, -1, 0, 1};
		for (int distance = 4; distance >= 2; distance--) {
			for (int index = 0; index < directions.length; index += 2) {
				int deltaX = directions[index];
				int deltaY = directions[index + 1];
				int playerX = targetX + deltaX * distance;
				int playerY = targetY + deltaY * distance;
				if (playerX < 0 || playerX >= width
						|| playerY < 0
						|| playerY >= Dungeon.level.height()
						|| collisionMap.blocked(playerX, playerY)) {
					continue;
				}
				boolean clear = true;
				for (int step = 1; step < distance; step++) {
					if (collisionMap.blocksLine(
							targetX + deltaX * step,
							targetY + deltaY * step)) {
						clear = false;
						break;
					}
				}
				if (clear) return true;
			}
		}
		return false;
	}

	private boolean attemptWhiteLineSpawn(float elapsed) {
		EnemyArchetypeDefinition boss = enemyArchetypes.require(
				FirstRaidEnemySpawnDirector.FIRST_BOSS);
		if (!WhiteLineSpawnPolicy.eligible(
				raidMode,
				boss,
				raid.session().raidOrdinal(),
				elapsed,
				raid.session().firstRaidProtectionActive(),
				whiteLineResolved(),
				activeEnemyCount(boss.id))) {
			return false;
		}
		for (BukovEnemySpawnPlanner.SpawnPoint point :
				enemySpawnPoints) {
			boolean validCell = point.cell >= 0
					&& point.cell < Dungeon.level.length();
			boolean visible = validCell
					&& Dungeon.level.heroFOV[point.cell];
			boolean occupied = validCell
					&& Actor.findChar(point.cell) != null;
			boolean tooClose = validCell && tooCloseToHero(point.cell);
			if (!WhiteLineSpawnPolicy.acceptsSpawnPoint(
					point.bossArena,
					point.cell,
					Dungeon.level.length(),
					visible,
					occupied,
					tooClose)) {
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
			if (!modeConvergenceAnnounced) {
				modeConvergenceAnnounced = true;
				showHeroStatus(BukovMessages.get(
						"bukov.raid.runtime.convergence_started"));
			}
		}
		if (raidMode.overtime(elapsed) && !modeOvertimeAnnounced) {
			modeOvertimeAnnounced = true;
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.overtime_pressure"));
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
				deactivateEnemyBody(enemy);
				enemiesByMob.remove(enemy.mob);
				enemies.remove(i);
			}
		}
		sortEnemiesByStableId();

		targetBodies.clear();
		enemyShotTargetBodies.clear();
		// Hostile rounds can only resolve gameplay damage against the player.
		// Keeping other hostiles out of the cast prevents an ally (or the
		// shooter itself after body overlap) from swallowing the ray and makes
		// source/target ownership identical for tracing and damage.
		enemyShotTargetBodies.add(heroBody);
		charsByBody.clear();
		for (EnemyRuntime enemy : enemies) {
			targetBodies.add(enemy.body);
			charsByBody.put(enemy.body, enemy.mob);
		}
		targetSpatialIndex.rebuild(targetBodies);
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
			targetSpatialIndex.remove(enemy.body);
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
			syncMovedEnemyVisibility(
					enemy.mob,
					nextCell,
					Dungeon.level.heroFOV);
		}
		targetSpatialIndex.update(enemy.body);
	}

	/**
	 * Keeps one moving enemy aligned with the player's already-computed FOV.
	 *
	 * <p>The player may remain stationary while an off-screen enemy crosses into
	 * a visible cell. In that case no player FOV refresh runs, so the sprite must
	 * be synchronized at the enemy's logical-cell boundary. This deliberately
	 * touches only the enemy that moved rather than scanning every mob at
	 * 120 Hz.</p>
	 */
	static void syncMovedEnemyVisibility(
			Mob mob, int nextCell, boolean[] heroFieldOfView) {
		if (mob == null) {
			throw new IllegalArgumentException("mob is required");
		}
		mob.pos = nextCell;
		if (mob.sprite != null
				&& heroFieldOfView != null
				&& nextCell >= 0
				&& nextCell < heroFieldOfView.length) {
			mob.sprite.visible = heroFieldOfView[nextCell];
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
				showEnemyStatus(
						enemy,
						CharSprite.WARNING,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_suppressing"));
				break;
			case FLANK_LEFT:
				showEnemyStatus(
						enemy,
						CharSprite.WARNING,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_flank_left"));
				break;
			case FLANK_RIGHT:
				showEnemyStatus(
						enemy,
						CharSprite.WARNING,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_flank_right"));
				break;
			case DASH:
				playEnemyRush(enemy);
				showEnemyStatus(
						enemy,
						CharSprite.NEGATIVE,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_dash"));
				break;
			case RETREAT:
				showEnemyStatus(
						enemy,
						CharSprite.NEUTRAL,
						BukovMessages.get(
								"bukov.raid.runtime.enemy_reposition"));
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
			showEnemyStatus(
					enemy,
					CharSprite.WARNING,
					BukovMessages.get(
							"bukov.raid.runtime.enemy_aiming"));
		} else if (enemy.rangedIntent.reloadStarted()) {
			playEnemyReload(enemy);
			showEnemyStatus(
					enemy,
					CharSprite.NEUTRAL,
					BukovMessages.get(
							"bukov.raid.runtime.reload"));
			playSfx(
					Assets.Sounds.Bukov.RELOAD_START,
					0.28f,
					0.88f
			);
		} else if (action == EnemyRangedCombatIntent.Action.OUT_OF_AMMO
				&& enemy.previousRangedAction != action) {
			showEnemyStatus(
					enemy,
					CharSprite.NEUTRAL,
					BukovMessages.get(
							"bukov.raid.runtime.empty_magazine"));
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
		showEnemyStatus(
				enemy,
				CharSprite.NEGATIVE,
				BukovMessages.get(
						"bukov.raid.runtime.enemy_gunshot"));
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
		boolean canDamage = enemyShotCanDamage(
				enemyShotHit,
				enemy.body,
				heroBody,
				enemy.mob,
				hero)
				&& enemy.rangedIntent.hasDamageEvent();
		if (canDamage) {
			combatFx.bloodMist(
					enemy.stableId,
					enemy.rangedIntent.shotSequence(),
					true,
					enemyShotHit.x,
					enemyShotHit.y,
					enemy.rangedIntent.directionX(),
					enemy.rangedIntent.directionY(),
					0.9f);
			pendingEnemyShots.add(new PendingEnemyShot(
					enemy.mob,
					EnemyAbilityRuntimePolicy.damageAgainstTarget(
							enemy.definition,
							enemy.rangedIntent.damage(),
							hero.HP,
							hero.HT)
			));
		} else if (enemyShotHit.body == null
				&& enemyShotHit.distance
						< enemy.rangedConfig.maximumRange - 0.001f) {
			combatFx.bulletMark(
					enemy.stableId,
					enemy.rangedIntent.shotSequence(),
					true,
					enemyShotHit.x,
					enemyShotHit.y,
					enemy.rangedIntent.directionX(),
					enemy.rangedIntent.directionY(),
					0.9f);
		}
	}

	static boolean playerShotCanDamage(
			HitscanResolver.Hit hit,
			RealtimeBody shooterBody,
			Char shooter,
			Char target) {
		return hit != null
				&& hit.body != null
				&& hit.body != shooterBody
				&& target != null
				&& target != shooter
				&& target.realtimeBody == hit.body
				&& target.isAlive()
				&& target.alignment == Char.Alignment.ENEMY;
	}

	static boolean enemyShotCanDamage(
			HitscanResolver.Hit hit,
			RealtimeBody shooterBody,
			RealtimeBody playerBody,
			Char shooter,
			Char player) {
		return hit != null
				&& hit.body == playerBody
				&& playerBody != null
				&& playerBody != shooterBody
				&& shooter != null
				&& shooter != player
				&& shooter.realtimeBody == shooterBody
				&& player != null
				&& player.realtimeBody == playerBody
				&& player.isAlive();
	}

	static boolean queuedEnemyShotCanDamage(Char attacker, Char player) {
		return attacker != null
				&& attacker != player
				&& attacker.isAlive()
				&& player != null
				&& player.isAlive();
	}

	private static void showEnemyStatus(EnemyRuntime enemy,
										int color,
										String text) {
		if (enemy.mob.sprite != null) {
			enemy.mob.sprite.showStatus(color, text);
		}
	}

	private static void playEnemyRush(EnemyRuntime enemy) {
		BukovEnemySprite sprite = bukovSprite(enemy);
		if (sprite != null) {
			sprite.realtimeRush(Dungeon.hero.pos);
		}
	}

	private static void playEnemyReload(EnemyRuntime enemy) {
		BukovEnemySprite sprite = bukovSprite(enemy);
		if (sprite != null) {
			sprite.realtimeReload(Dungeon.hero.pos);
		}
	}

	private static void playEnemyScan(EnemyRuntime enemy) {
		BukovEnemySprite sprite = bukovSprite(enemy);
		if (sprite != null) {
			sprite.realtimeScan(Dungeon.hero.pos);
		}
	}

	private static BukovEnemySprite bukovSprite(EnemyRuntime enemy) {
		return enemy != null && enemy.mob.sprite instanceof BukovEnemySprite
				? (BukovEnemySprite)enemy.mob.sprite : null;
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
		if (definition == null || definition.role == null) {
			return BukovMessages.get(
					"bukov.raid.runtime.enemy_role_default");
		}
		switch (definition.role) {
			case RANGED_SKIRMISHER:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_skirmisher");
			case MELEE_RUSHER:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_rusher");
			case ARMORED_SUPPRESSOR:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_armored");
			case SCOUT_ALARM:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_scout");
			case ELITE_COMMANDER:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_commander");
			case OPTIONAL_BOSS:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_white_line");
			default:
				return BukovMessages.get(
						"bukov.raid.runtime.enemy_role_default");
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
		damage = EnemyAbilityRuntimePolicy.damageAgainstTarget(
				runtime == null ? null : runtime.definition,
				damage,
				hero.HP,
				hero.HT);
		damage = hero.defenseProc(attacker, damage);
		if (!attacker.isAlive()) {
			EnemyRuntime enemy = enemiesByMob.get(attacker);
			if (enemy != null) {
				enemy.brain.markDead();
				deactivateEnemyBody(enemy);
			}
			return;
		}
		if (damage >= 0) {
			damage = Math.max(0, damage - hero.drRoll());
		}
		damage = raidMode.incomingDamage(damage, hero.HP);
		if (damage > 0) {
			playSfx(
					Assets.Sounds.Bukov.CONTACT_HIT,
					0.7f,
					nextAudioPitch(1f, 0.06f)
			);
			boolean wasAlive = hero.isAlive();
			int healthBefore = hero.HP;
			hero.damageWithoutFloatingText(damage, attacker);
			int appliedDamage = Math.max(0, healthBefore - hero.HP);
			if (raid != null && appliedDamage > 0) {
				raid.recordDamageTaken(appliedDamage);
			}
			emitPlayerHitOutcome(attacker, wasAlive, appliedDamage);
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

	private Room currentHudRoom() {
		return Dungeon.level instanceof BukovLevel
				? ((BukovLevel)Dungeon.level).room(hero.pos)
				: null;
	}

	private void recordBalanceRoom() {
		if (raid == null
				|| !(Dungeon.level instanceof BukovLevel)
				|| !raid.session().balanceTelemetry().routeId().isEmpty()) {
			return;
		}
		BukovLevel level = (BukovLevel)Dungeon.level;
		String roomId = level.stableRoomIdAt(hero.pos);
		if (roomId.isEmpty()) return;
		raid.recordBalanceRoom(roomId);
		String routeId = resolvedRouteId(
				level.raidLayout(),
				raid.session().balanceVisitedRooms());
		if (!routeId.isEmpty()) {
			raid.identifyBalanceRoute(routeId);
		}
	}

	static String resolvedRouteId(
			BukovRaidLayout layout,
			java.util.List<String> visitedRoomIds) {
		if (layout == null
				|| visitedRoomIds == null
				|| visitedRoomIds.isEmpty()) {
			return "";
		}
		String currentRoom =
				visitedRoomIds.get(visitedRoomIds.size() - 1);
		String result = "";
		for (BukovRaidLayout.Route route : layout.routes) {
			if (route == null
					|| route.routeId == null
					|| route.routeId.isEmpty()
					|| route.roomIds.isEmpty()
					|| !currentRoom.equals(
							route.roomIds.get(
									route.roomIds.size() - 1))
					|| !orderedSubsequence(
							route.roomIds,
							visitedRoomIds)) {
				continue;
			}
			if (!result.isEmpty()) {
				return "";
			}
			result = route.routeId;
		}
		return result;
	}

	private static boolean orderedSubsequence(
			java.util.List<String> required,
			java.util.List<String> visited) {
		int requiredIndex = 0;
		for (String roomId : visited) {
			if (requiredIndex < required.size()
					&& required.get(requiredIndex).equals(roomId)) {
				requiredIndex++;
			}
		}
		return requiredIndex == required.size();
	}

	private void refreshHeroVisibility() {
		Dungeon.level.updateFieldOfView(hero, Dungeon.level.heroFOV);
		GameScene.updateFog();
		// Bukov does not run the legacy Actor loop, so Dungeon.observe() is not
		// available to keep mob sprites in sync with the freshly computed FOV.
		GameScene.afterObserve();
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
		playerSounds.emit(
				heroBody.x,
				heroBody.y,
				radius,
				0.35f);
	}

	static int emitPumpBroadcast(
			PlayerSoundEventBuffer sounds,
			int pumpCell,
			int levelWidth) {
		if (sounds == null || pumpCell < 0 || levelWidth <= 0) {
			throw new IllegalArgumentException(
					"sounds, pumpCell, and levelWidth are required");
		}
		return sounds.emit(
				pumpCell % levelWidth + 0.5f,
				pumpCell / levelWidth + 0.5f,
				PUMP_BROADCAST_RADIUS_TILES,
				PUMP_BROADCAST_LIFETIME_SECONDS);
	}

	static float investigatorSpawnDeadline(
			float currentDeadline,
			float elapsedSeconds) {
		return Math.min(
				currentDeadline,
				elapsedSeconds + ALARM_REINFORCEMENT_DELAY_SECONDS);
	}

	private void scheduleInvestigators() {
		if (raid == null) return;
		nextEnemySpawnSeconds = investigatorSpawnDeadline(
				nextEnemySpawnSeconds,
				raid.session().elapsedSeconds);
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
				case SEARCH:
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
		int sequence = nextAudioSequence();
		SpatialAudioModel.resolve(
				audioContract,
				1f,
				0f,
				0f,
				true,
				playbackSpatial);
		GunshotAudioResolver.resolve(
				true,
				sequence,
				0f,
				0f,
				playbackSpatial,
				gunshotAudio);
		GunshotAcousticSpace acousticSpace =
				GunshotAcousticSpaceResolver.resolve(
						collisionMap,
						heroBody.x,
						heroBody.y);
		playGunshotLayers(
				definition.audioProfile.gunshotFamily.mechanicalAsset(sequence),
				definition.audioProfile.gunshotFamily.bodyAsset(sequence),
				acousticSpace.tailAsset(sequence),
				gainScale,
				pitchScale,
				SoundCategory.PLAYER_GUNSHOT);
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
		int sequence = nextAudioSequence();
		GunshotAudioResolver.resolve(
				false,
				sequence,
				deltaX,
				deltaY,
				playbackSpatial,
				gunshotAudio);
		if (insideSoundRadius) {
			GunshotSoundFamily gunshotFamily =
					GunshotSoundFamily.PISTOL;
			if (enemy.definition.weaponDefinitionId != null) {
				FirearmDefinition enemyDefinition =
						firearmRegistry.require(
								enemy.definition.weaponDefinitionId);
				gunshotFamily =
						enemyDefinition.audioProfile.gunshotFamily;
			}
			GunshotAcousticSpace acousticSpace =
					GunshotAcousticSpaceResolver.resolve(
							collisionMap,
							enemy.body.x,
							enemy.body.y);
			playGunshotLayers(
					gunshotFamily.mechanicalAsset(sequence),
					gunshotFamily.bodyAsset(sequence),
					acousticSpace.tailAsset(sequence),
					0.78f,
					1f,
					SoundCategory.ENEMY_GUNSHOT);
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
					keySoundLifetimeSeconds);
		}
	}

	private void playGunshotLayers(
			String mechanicalAsset,
			String bodyAsset,
			String tailAsset,
			float gainScale,
			float pitchScale,
			SoundCategory category) {
		if (!gunshotAudio.audible()) return;
		float safePitchScale = Math.max(0.5f, Math.min(2f, pitchScale));
		float gain = realtimeSfxGain();
		if (gain <= 0f) return;
		long source = worldSounds.begin(
				AudioChannel.SFX,
				SoundConcurrencyBudget.defaultPriority(category),
				SoundConcurrencyBudget.protectedByDefault(category),
				GUNSHOT_TIMEOUT_SECONDS);
		if (source == SoundConcurrencyBudget.NO_TOKEN) return;
		boolean played = worldSounds.playLayer(
				source,
				mechanicalAsset,
				gunshotAudio.mechanicalLeft() * gainScale * gain,
				gunshotAudio.mechanicalRight() * gainScale * gain,
				clampedPitch(
						gunshotAudio.mechanicalPitch()
								* safePitchScale));
		played |= worldSounds.playLayer(
				source,
				bodyAsset,
				gunshotAudio.bodyLeft() * gainScale * gain,
				gunshotAudio.bodyRight() * gainScale * gain,
				clampedPitch(
						gunshotAudio.bodyPitch() * safePitchScale));
		played |= worldSounds.playLayer(
				source,
				tailAsset,
				gunshotAudio.tailLeft() * gainScale * gain,
				gunshotAudio.tailRight() * gainScale * gain,
				clampedPitch(
						gunshotAudio.tailPitch() * safePitchScale));
		if (!played) {
			worldSounds.release(source);
		}
	}

	private static float clampedPitch(float pitch) {
		return Math.max(0.5f, Math.min(2f, pitch));
	}

	private float realtimeSfxGain() {
		return audioContract.defaultMasterVolume
				* audioContract.defaultSfxVolume
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovMasterVolume())
				* SPDSettings.bukovVolumeGain(
						SPDSettings.bukovSfxVolume());
	}

	private long playSfx(String asset, float volume, float pitch) {
		return playSfx(
				asset,
				volume,
				pitch,
				SoundConcurrencyBudget.Priority.NORMAL,
				false,
				SHORT_SFX_TIMEOUT_SECONDS);
	}

	private long playSfx(
			String asset,
			float volume,
			float pitch,
			SoundCategory category) {
		return playSfx(
				asset,
				volume,
				pitch,
				SoundConcurrencyBudget.defaultPriority(category),
				SoundConcurrencyBudget.protectedByDefault(category),
				category == SoundCategory.FOOTSTEP
						? FOOTSTEP_TIMEOUT_SECONDS
						: category == SoundCategory.EXTRACTION_CUE
								? CRITICAL_CUE_TIMEOUT_SECONDS
								: SHORT_SFX_TIMEOUT_SECONDS);
	}

	private long playSfx(
			String asset,
			float volume,
			float pitch,
			SoundConcurrencyBudget.Priority priority,
			boolean protectedSource,
			float timeoutSeconds) {
		float mixedVolume = volume * realtimeSfxGain();
		if (mixedVolume <= 0f) {
			return SoundConcurrencyBudget.NO_TOKEN;
		}
		return worldSounds.play(
				asset,
				AudioChannel.SFX,
				priority,
				protectedSource,
				timeoutSeconds,
				mixedVolume,
				mixedVolume,
				pitch);
	}

	private void preserveExtractionCompleteCue() {
		if (extractionCompleteSoundToken
				== SoundConcurrencyBudget.NO_TOKEN) {
			return;
		}
		worldSounds.detach(extractionCompleteSoundToken);
		extractionCompleteSoundToken = SoundConcurrencyBudget.NO_TOKEN;
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
					&& collisionMap.blocksLine(x0, y0)) {
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
		return EnemyAbilityRuntimePolicy.hasAbility(
				definition, ability);
	}

	private void broadcastPlayerContact(EnemyRuntime source) {
		playEnemyScan(source);
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
						? BukovMessages.get(
								"bukov.raid.runtime.enemy_order_flank")
						: BukovMessages.get(
								"bukov.raid.runtime.enemy_broadcast_alarm"));
		if (raid != null && hasAbility(source, "CALL_INVESTIGATORS")) {
			scheduleInvestigators();
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.enemy_reinforcements_incoming"));
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
		CombatFeedbackType pulseFeedback = bossPulseFeedback(phase);
		combatPresentation.emit(
				CombatPresentationEvent.Type.ENEMY_FIRE,
				boss.stableId,
				hero.id(),
				boss.mob.pos,
				hero.pos,
				pulseFeedback,
				phase
						== WhiteLineBossStateMachine.Phase.DECOY_SEARCH
						? 0.7f : 1f);
		playCombatFeedbackCue(pulseFeedback);
		combatFx.explosion(
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
						? BukovMessages.get(
								"bukov.raid.runtime.boss_decoy_impact")
						: BukovMessages.get(
								"bukov.raid.runtime.boss_fog_overload"));
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
		if (ensureReleasedMissionArchiveExists()) {
			// The repair changes the host-level heap, not only the raid
			// checkpoint. Commit both save surfaces now so another interruption
			// cannot make the unique objective document disappear again.
			checkpointLootChange();
		}
	}

	private boolean ensureReleasedMissionArchiveExists() {
		if (!missionEnabled
				|| missionGateUnlocked
				|| carriesMissionArchive()) {
			return false;
		}
		BukovRaidCoordinator.ContainerSnapshot missionContainer =
				raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
		if (missionContainer == null
				|| missionContainer.state
						!= BukovSearchableContainer.State.SEARCHED
				|| !missionContainer.contentsReleased) {
			return false;
		}
		for (Heap existing : Dungeon.level.heaps.valueList()) {
			for (Item item : existing.items) {
				if (item instanceof BukovMissionArchive) return false;
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
		return true;
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
		createMissionArchiveMarker();
		refreshBossMechanismMarkers();
	}

	private void createMissionArchiveMarker() {
		if (!missionEnabled
				|| missionGateUnlocked
				|| missionArchiveMarker != null) {
			return;
		}
		BukovRaidCoordinator.ContainerSnapshot archive =
				raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
		if (archive == null || archive.contentsReleased
				|| archive.cell < 0
				|| archive.cell >= Dungeon.level.length()) {
			return;
		}
		missionArchiveMarker =
				new BukovInteractionMarker(
						BukovInteractionMarker.Kind.MISSION_ARCHIVE)
						.placeAtCell(archive.cell);
		interactionMarkers.add(missionArchiveMarker);
		GameScene.effect(missionArchiveMarker);
	}

	private void clearMissionArchiveMarker() {
		if (missionArchiveMarker == null) return;
		missionArchiveMarker.killAndErase();
		interactionMarkers.remove(missionArchiveMarker);
		missionArchiveMarker = null;
	}

	private void refreshBossMechanismMarkers() {
		if (!presentationObjectsEnabled) return;
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.boss_trace_anchor_damaged"));
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

	private ExtractionState nearestExtraction(
			int originCell,
			float elapsed,
			int maximumCellDistance,
			boolean requireAvailable) {
		if (raid == null || Dungeon.level == null) return null;
		return nearestExtraction(
				originCell,
				Dungeon.level.width(),
				Dungeon.level.length(),
				elapsed,
				maximumCellDistance,
				requireAvailable,
				raid.extractions(),
				extractionLookup);
	}

	static ExtractionState nearestExtraction(
			int originCell,
			int width,
			int length,
			float elapsed,
			int maximumCellDistance,
			boolean requireAvailable,
			Iterable<ExtractionState> extractions,
			ExtractionLookup lookup) {
		if (extractions == null || lookup == null) return null;
		ExtractionState nearest = null;
		int nearestDistanceSquared = Integer.MAX_VALUE;
		for (ExtractionState extraction : extractions) {
			if (extraction == null || extraction.completed()
					|| requireAvailable
							&& !lookup.available(extraction, elapsed)) {
				continue;
			}
			int cell = lookup.cell(extraction.extractionId());
			int cellDistance = extractionCellDistance(
					originCell, cell, width, length);
			if (cellDistance == Integer.MAX_VALUE
					|| maximumCellDistance >= 0
							&& cellDistance > maximumCellDistance) {
				continue;
			}
			int deltaX = originCell % width - cell % width;
			int deltaY = originCell / width - cell / width;
			int distanceSquared = deltaX * deltaX + deltaY * deltaY;
			if (nearest == null
					|| distanceSquared < nearestDistanceSquared
					|| distanceSquared == nearestDistanceSquared
							&& extraction.extractionId().compareTo(
									nearest.extractionId()) < 0) {
				nearest = extraction;
				nearestDistanceSquared = distanceSquared;
			}
		}
		return nearest;
	}

	static int extractionCellDistance(
			int firstCell, int secondCell, int width, int length) {
		if (width <= 0 || length <= 0
				|| firstCell < 0 || firstCell >= length
				|| secondCell < 0 || secondCell >= length) {
			return Integer.MAX_VALUE;
		}
		return Math.max(
				Math.abs(firstCell % width - secondCell % width),
				Math.abs(firstCell / width - secondCell / width));
	}

	static String extractionApproachLabel(
			String extractionId, boolean available) {
		String target = extractionId == null
				|| extractionId.trim().isEmpty()
				? "--" : extractionId.trim();
		return available
				? BukovMessages.get(
						"bukov.raid.runtime.extraction_approach_available_format",
						target)
				: BukovMessages.get(
						"bukov.raid.runtime.extraction_approach_locked_format",
						target);
	}

	private BukovRaidCoordinator.ContainerSnapshot containerWithinRange(
			int cell) {
		/*
		 * The container the operator is standing on always wins. Returning the
		 * first in-range container instead let an ordinary loot cache generated
		 * beside the mission archive shadow it: the prompt described the wrong
		 * container, searching opened the wrong container, and the objective
		 * stayed permanently unreachable on roughly one first raid in six.
		 */
		BukovRaidCoordinator.ContainerSnapshot fallback = null;
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (container.contentsReleased
					|| !withinInteractionRange(cell, container.cell)) {
				continue;
			}
			if (container.cell == cell) {
				return container;
			}
			if (fallback == null) {
				fallback = container;
			}
		}
		return fallback;
	}

	private boolean withinInteractionRange(int firstCell, int secondCell) {
		int width = Dungeon.level.width();
		return withinInteractionRange(
				firstCell,
				secondCell,
				width,
				Dungeon.level.length(),
				collisionMap);
	}

	static boolean withinInteractionRange(
			int firstCell,
			int secondCell,
			int width,
			int length,
			CollisionMap collisionMap) {
		if (width <= 0
				|| length <= 0
				|| firstCell < 0
				|| secondCell < 0
				|| firstCell >= length
				|| secondCell >= length) {
			return false;
		}
		int firstX = firstCell % width;
		int firstY = firstCell / width;
		int secondX = secondCell % width;
		int secondY = secondCell / width;
		int deltaX = Math.abs(firstX - secondX);
		int deltaY = Math.abs(firstY - secondY);
		if (Math.max(deltaX, deltaY) > 1) {
			return false;
		}
		if (deltaX == 0 || deltaY == 0) {
			// Orthogonal adjacency deliberately allows an obstructing target
			// cell: doors and wall-mounted mechanisms must be operable from
			// the neighbouring floor tile.
			return true;
		}
		if (collisionMap == null) {
			return false;
		}
		// A diagonal reach crosses both cells at the shared corner. Requiring
		// the target and both seams to stay open prevents searching a container
		// or operating a pump/Boss mechanism through a wall corner. Obstructing
		// wall-mounted targets remain operable only from an orthogonal tile.
		return !collisionMap.blocksLine(secondX, secondY)
				&& !collisionMap.blocksLine(firstX, secondY)
				&& !collisionMap.blocksLine(secondX, firstY);
	}

	private void readNavigationHudState(
			BukovRaidHudState target,
			float elapsed) {
		if (raid == null) return;
		if (missionEnabled && !missionGateUnlocked) {
			if (carriesMissionArchive()) {
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.MISSION,
						missionGateCell,
						BukovMessages.get(
								"bukov.raid.runtime.maintenance_gate"),
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
						BukovMessages.get(
								"bukov.raid.runtime.maintenance_archive"),
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
							BukovMessages.get(
									"bukov.raid.runtime.pickup_archive"),
							true);
					return;
				}
			}
		}

		if (missionEnabled
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
						BukovMessages.get(
								"bukov.raid.runtime.search_high_value"),
						container.state
								!= BukovSearchableContainer.State.LOCKED);
				return;
			}
		}

		if (raid.bossContractRequired()
				&& !raid.bossContractCompleted()) {
			EnemyRuntime boss = activeWhiteLine();
			if (boss != null) {
				pointHudNavigation(
						target,
						BukovRaidHudState.Cue.MISSION,
						boss.mob.pos,
						BukovMessages.get(
								"bukov.raid.runtime.boss_contract_navigation"),
						true);
				return;
			}
		}

		ExtractionState nearest = nearestExtraction(
				hero.pos,
				elapsed,
				-1,
				true);
		if (nearest != null) {
			pointHudNavigation(
					target,
					BukovRaidHudState.Cue.EXTRACTION,
					resolveExtractionCell(nearest.extractionId()),
					BukovMessages.get(
							"bukov.raid.runtime.extraction_label_format",
							nearest.extractionId()),
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
						? BukovMessages.get(
								"bukov.raid.runtime.enemy_default")
						: enemyRoleLabel(nearest.definition),
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.pump_not_connected"));
			return;
		}
		if (conditional.conditionMet()) {
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.pump_power_ready"));
			return;
		}
		raid.setExtractionCondition(CONDITIONAL_EXTRACTION_ID, true);
		playSfx(
				Assets.Sounds.Bukov.GATE_UNLOCK,
				0.82f,
				0.92f,
				SoundCategory.UI);
		emitPumpBroadcast(
				playerSounds,
				pumpCell,
				Dungeon.level.width());
		scheduleInvestigators();
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.pump_restored_extraction"));
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
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.boss_umbrella_front_blocked"));
					showEnemyStatus(
							boss,
							CharSprite.WARNING,
							BukovMessages.get(
									"bukov.raid.runtime.boss_front_immune"));
					return true;
				}
				break;
			case IDENTIFY_TRUE_BODY:
				int bodyIndex = bodyTraceWithinRange(hero.pos);
				if (bodyIndex < 0) return false;
				result = boss.bossState.identifyTrueBody(bodyIndex);
				if (result
						== WhiteLineBossStateMachine.Result.MECHANISM_REJECTED) {
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.boss_decoy_false"));
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
					showHeroStatus(BukovMessages.get(
							"bukov.raid.runtime.boss_fog_controller_required"));
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.boss_weakpoint_exposed"));
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

	private WhiteLineBossStateMachine.Result resolveWhiteLineDamage(
			EnemyRuntime enemy, int damage) {
		if (enemy.bossState.phase()
				== WhiteLineBossStateMachine.Phase.DORMANT) {
			enemy.bossState.engage();
			showBossObjective(enemy);
		}
		WhiteLineBossStateMachine.Result result =
				enemy.bossState.applyDamage(damage);
		if (result == WhiteLineBossStateMachine.Result.DAMAGE_BLOCKED) {
			showEnemyStatus(
					enemy,
					CharSprite.WARNING,
					BukovMessages.get(
							"bukov.raid.runtime.boss_mechanism_protected"));
			return result;
		}
		enemy.mob.HP = Math.max(1, enemy.bossState.health());
		if (result == WhiteLineBossStateMachine.Result.PHASE_CHANGED) {
			showBossObjective(enemy);
		} else if (result == WhiteLineBossStateMachine.Result.DEFEATED) {
			clearBossMechanismMarkers();
			resolveWhiteLineLevel();
			int cell = enemy.mob.pos;
			enemy.mob.damageWithoutFloatingText(enemy.mob.HP, hero);
			enemy.brain.markDead();
			deactivateEnemyBody(enemy);
			if (raid != null && raid.bossContractRequired()) {
				raid.markBossContractCompleted();
			}
			recordEnemyKill();
			releaseWhiteLineLoot(cell);
		}
		return result;
	}

	static CombatFeedbackType bossHitFeedback(
			WhiteLineBossStateMachine.Result result) {
		return result == WhiteLineBossStateMachine.Result.PHASE_CHANGED
				? CombatFeedbackType.BOSS_PHASE_BREAK
				: null;
	}

	static CombatFeedbackType bossDeathFeedback(
			WhiteLineBossStateMachine.Result result) {
		return result == WhiteLineBossStateMachine.Result.DEFEATED
				? CombatFeedbackType.WEAKPOINT_KILL
				: CombatFeedbackType.KILL;
	}

	static CombatFeedbackType bossPulseFeedback(
			WhiteLineBossStateMachine.Phase phase) {
		if (phase == WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
			return CombatFeedbackType.BOSS_SLAM;
		}
		if (phase == WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) {
			return CombatFeedbackType.BOSS_OVERLOAD;
		}
		return null;
	}

	private void showBossObjective(EnemyRuntime enemy) {
		showTutorial(BukovTutorialEvent.BOSS_WARNING);
		String text;
		switch (enemy.bossState.objective()) {
			case FLANK_UMBRELLA:
				text = BukovMessages.get(
						"bukov.raid.runtime.boss_warning_flank");
				break;
			case IDENTIFY_TRUE_BODY:
				text = BukovMessages.get(
						"bukov.raid.runtime.boss_warning_trace");
				refreshBossMechanismMarkers();
				break;
			case DISABLE_FOG_LAMPS:
				text = BukovMessages.get(
						"bukov.raid.runtime.boss_warning_fog");
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
				deactivateEnemyBody(enemy);
				enemy.brain.markDead();
				enemy.mob.destroy();
				if (enemy.mob.sprite != null) {
					enemy.mob.sprite.killAndErase();
				}
			}
		}
		if (available) resolveWhiteLineLevel();
	}

	private void deactivateEnemyBody(EnemyRuntime enemy) {
		if (enemy == null) return;
		enemy.body.active = false;
		targetSpatialIndex.remove(enemy.body);
	}

	private void reconcileLegacyBossContractCheckpoint() {
		if (raid == null
				|| !raid.bossContractRequired()
				|| !(Dungeon.level instanceof BukovLevel)) {
			return;
		}
		WhiteLineBossStateMachine.Phase phase =
				((BukovLevel)Dungeon.level).whiteLinePhase();
		if (!raid.reconcileLegacyBossContractPhase(phase)) {
			return;
		}
		try {
			raid.saveCheckpoint();
		} catch (IOException failure) {
			// The restored host phase remains authoritative in memory. A later
			// lifecycle checkpoint retries the same idempotent reconciliation.
			ShatteredPixelDungeon.reportException(failure);
		}
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
			showHeroStatus(BukovMessages.get(
					"bukov.raid.runtime.container_release_failed"));
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
				? BukovMessages.get(
						"bukov.raid.runtime.search_completed_items_format",
						released)
				: BukovMessages.get(
						"bukov.raid.runtime.search_completed_empty"));
		if (FirstRaidMission.ARCHIVE_CONTAINER_ID.equals(containerId)) {
			clearMissionArchiveMarker();
		}
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

	static float resolveEnemyArmor(
			RealtimeArmorState armor,
			float damage,
			float penetration,
			RealtimeDamage.HitZone hitZone) {
		if (armor == null) {
			return damage;
		}
		return armor.resolveBullet(
				damage,
				penetration,
				hitZone).healthDamage;
	}

	static RealtimeArmorState createEnemyArmor(
			EnemyArchetypeDefinition definition) {
		if (definition == null
				|| !hasAbility(definition, "ARMORED_FRONT")) {
			return null;
		}
		return RealtimeArmorState.fresh(
				ArmorCatalog.require("patrol_vest"));
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
						? BukovMessages.get(
								"bukov.raid.runtime.extraction_countdown_format",
								remaining)
						: BukovMessages.get(
								"bukov.raid.runtime.extraction_completed")
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
		showHeroStatus(BukovMessages.get(
				"bukov.raid.runtime.search_countdown_format",
				remaining));
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
		emitEnemyHitOutcome(
				target,
				wasAlive,
				damage,
				null,
				CombatFeedbackType.KILL);
	}

	private void emitEnemyHitOutcome(
			Char target,
			boolean wasAlive,
			int damage,
			CombatFeedbackType hitFeedback,
			CombatFeedbackType deathFeedback) {
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
				hitFeedback,
				intensity);
		playCombatFeedbackCue(hitFeedback);
		if (wasAlive && !target.isAlive()) {
			scheduleKillConfirmation(
					deathFeedback,
					killDistanceTiles(target));
			combatPresentation.emit(
					CombatPresentationEvent.Type.ENEMY_DEATH,
					hero.id(),
					target.id(),
					hero.pos,
					target.pos,
					deathFeedback,
					intensity);
		}
	}

	private void scheduleKillConfirmation(
			CombatFeedbackType feedbackType,
			float distanceTiles) {
		CombatFeedbackType requested =
				feedbackType == CombatFeedbackType.WEAKPOINT_KILL
						? CombatFeedbackType.WEAKPOINT_KILL
						: CombatFeedbackType.KILL;
		if (requested == CombatFeedbackType.WEAKPOINT_KILL
				|| killConfirmFeedback != CombatFeedbackType.WEAKPOINT_KILL) {
			killConfirmFeedback = requested;
		}
		combatHudTimeline.kill(distanceTiles);
	}

	private void playCombatFeedbackCue(CombatFeedbackType feedbackType) {
		String asset = CombatFeedbackAudioCue.asset(feedbackType);
		SoundCategory category =
				CombatFeedbackAudioCue.category(feedbackType);
		if (asset == null || category == null) return;
		playSfx(
				asset,
				CombatFeedbackAudioCue.volume(feedbackType),
				CombatFeedbackAudioCue.pitch(feedbackType),
				category);
	}

	static CombatFeedbackType playerShotFeedback(
			FirearmDefinition definition) {
		return definition != null
				&& (definition.weaponClass == FirearmClass.SHOTGUN
						|| definition.pellets > 1)
				? CombatFeedbackType.SHOTGUN_NEAR
				: CombatFeedbackType.RIFLE_SHOT;
	}

	private float killDistanceTiles(Char target) {
		if (target != null && target.realtimeBody != null
				&& heroBody != null) {
			float deltaX = target.realtimeBody.x - heroBody.x;
			float deltaY = target.realtimeBody.y - heroBody.y;
			return (float)Math.sqrt(
					deltaX * deltaX + deltaY * deltaY);
		}
		if (target == null || Dungeon.level == null) return 0f;
		int width = Dungeon.level.width();
		float deltaX = target.pos % width - hero.pos % width;
		float deltaY = target.pos / width - hero.pos / width;
		return (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}

	private void emitPlayerHitOutcome(
			Char attacker, boolean wasAlive, int damage) {
		recordDirectHitDirection(attacker, damage);
		showHeroStatus(shouldShowDamageNumber(
				SPDSettings.bukovDamageNumbers(), damage, hero.HT)
				? BukovMessages.get(
						"bukov.raid.runtime.player_hit_damage_format",
						damage)
				: BukovMessages.get(
						"bukov.raid.runtime.player_hit"));
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
				playerAimCell(),
				CombatFeedbackType.PLAYER_HIT,
				intensity,
				fireControl.reloadRemaining());
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

	private void recordDirectHitDirection(Char attacker, int damage) {
		combatHudTimeline.activity();
		if (attacker == null
				|| attacker == hero
				|| Dungeon.level == null) {
			return;
		}
		int width = Dungeon.level.width();
		float deltaX = attacker.pos % width - hero.pos % width;
		float deltaY = attacker.pos / width - hero.pos / width;
		combatHudTimeline.damage(
				attacker.id(),
				direction(deltaX, deltaY),
				Math.max(
						0.2f,
						Math.min(
								1f,
								damage
										/ Math.max(
												1f,
												hero.HT * 0.35f))),
				false);
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
					BukovMessages.get(
							"bukov.raid.runtime.boss_white_line"),
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
			return BukovMessages.get(
					"bukov.raid.runtime.boss_phase_umbrella");
		}
		if (phase == WhiteLineBossStateMachine.Phase.DECOY_SEARCH) {
			return BukovMessages.get(
					"bukov.raid.runtime.boss_phase_decoy");
		}
		if (phase == WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD) {
			return BukovMessages.get(
					"bukov.raid.runtime.boss_phase_fog");
		}
		return "";
	}

	private static String bossObjectiveLabel(
			WhiteLineBossStateMachine.Objective objective) {
		if (objective == WhiteLineBossStateMachine.Objective.FLANK_UMBRELLA) {
			return BukovMessages.get(
					"bukov.raid.runtime.boss_objective_flank");
		}
		if (objective
				== WhiteLineBossStateMachine.Objective.IDENTIFY_TRUE_BODY) {
			return BukovMessages.get(
					"bukov.raid.runtime.boss_objective_identify");
		}
		if (objective
				== WhiteLineBossStateMachine.Objective.DISABLE_FOG_LAMPS) {
			return BukovMessages.get(
					"bukov.raid.runtime.boss_objective_disable_fog");
		}
		return BukovMessages.get(
				"bukov.raid.runtime.boss_objective_attack");
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
		private final float penetration;
		private final RealtimeDamage.HitZone hitZone;

		private PendingHit(
				Char target,
				float damage,
				float penetration,
				RealtimeDamage.HitZone hitZone) {
			this.target = target;
			this.damage = damage;
			this.penetration = penetration;
			this.hitZone = hitZone;
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
		private final RealtimeArmorState armor;
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
		private WhiteLineBossStateMachine.Phase previousBossPhase;
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
			armor = createEnemyArmor(definition);
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
				heardSoundSequence =
						restoredState.heardSoundSequence();
			}
			if (definition != null
					&& definition.weaponDefinitionId != null) {
				boolean suppressor = tactics.profile()
						== RealtimeEnemyTactics.Profile.SUPPRESSOR;
				boolean flanker = tactics.profile()
						== RealtimeEnemyTactics.Profile.FLANKER;
				boolean onboardingFireSafety =
						InitialContactCombatPolicy.applies(
								mob instanceof BukovHostMob
										&& ((BukovHostMob)mob)
												.onboardingContact(),
								true);
				float baselineAimSeconds = suppressor ? 0.22f
						: definition.tier == EnemyTier.ELITE
								? 0.3f : 0.45f;
				rangedConfig = new EnemyRangedCombatController.Config(
						suppressor ? 8
								: definition.tier == EnemyTier.ELITE ? 6 : 5,
						suppressor ? 240f
								: flanker ? 180f : 150f,
						suppressor ? 1.85f : 1.6f,
						definition.engagementRange,
						InitialContactCombatPolicy.aimSeconds(
								baselineAimSeconds,
								onboardingFireSafety),
						InitialContactCombatPolicy.minimumDamage(
								definition.minimumDamage,
								onboardingFireSafety),
						InitialContactCombatPolicy.maximumDamage(
								definition.minimumDamage,
								definition.maximumDamage,
								onboardingFireSafety),
						InitialContactCombatPolicy.openingWarningSeconds(
								onboardingFireSafety));
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
