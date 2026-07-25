package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFxEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRuntimeLoadoutAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidBalanceTelemetry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudState;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.input.ControllerHandler;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Headless driver for the production realtime combat loop.
 *
 * This deliberately supplies only the platform boundaries (files, mouse and
 * camera). Enemy spawning, AI, hitscan, damage, death and raid telemetry all
 * remain owned by {@link BukovRealtimeWorld}.
 */
final class BukovRealtimeCombatHarness {

	private static final float RENDER_STEP = 1f / 60f;
	private static final int ENEMY_FIRE_TIMEOUT_FRAMES = 600;
	private static final int PLAYER_KILL_TIMEOUT_FRAMES = 900;
	private static final int REMOVAL_REFRESH_FRAMES = 30;

	static final class Result {
		final int generatedEnemyCount;
		final String targetDefinitionId;
		final int initialTargetHealth;
		final int finalTargetHealth;
		final int initialMagazine;
		final int finalMagazine;
		final int friendlyTracers;
		final int hostileTracers;
		final boolean nonZeroFriendlyTracer;
		final boolean nonZeroHostileTracer;
		final int damageTaken;
		final int firefights;
		final int killCount;
		final boolean targetBodyActive;
		final boolean targetStillInLevel;
		final BackpackPauseEvidence backpackPause;
		final FirstRaidJourneyEvidence firstRaidJourney;

		private Result(
				int generatedEnemyCount,
				String targetDefinitionId,
				int initialTargetHealth,
				int finalTargetHealth,
				int initialMagazine,
				int finalMagazine,
				FxEvidence fx,
				RaidBalanceTelemetry telemetry,
				int killCount,
				boolean targetBodyActive,
				boolean targetStillInLevel,
				BackpackPauseEvidence backpackPause,
				FirstRaidJourneyEvidence firstRaidJourney) {
			this.generatedEnemyCount = generatedEnemyCount;
			this.targetDefinitionId = targetDefinitionId;
			this.initialTargetHealth = initialTargetHealth;
			this.finalTargetHealth = finalTargetHealth;
			this.initialMagazine = initialMagazine;
			this.finalMagazine = finalMagazine;
			friendlyTracers = fx.friendlyTracers;
			hostileTracers = fx.hostileTracers;
			nonZeroFriendlyTracer = fx.nonZeroFriendlyTracer;
			nonZeroHostileTracer = fx.nonZeroHostileTracer;
			damageTaken = telemetry.damageTaken();
			firefights = telemetry.firefights();
			this.killCount = killCount;
			this.targetBodyActive = targetBodyActive;
			this.targetStillInLevel = targetStillInLevel;
			this.backpackPause = backpackPause;
			this.firstRaidJourney = firstRaidJourney;
		}

		@Override
		public String toString() {
			return "generated=" + generatedEnemyCount
					+ ", target=" + targetDefinitionId
					+ ", hp=" + initialTargetHealth + "->"
							+ finalTargetHealth
					+ ", magazine=" + initialMagazine + "->"
							+ finalMagazine
					+ ", tracers=" + friendlyTracers + "/"
							+ hostileTracers
					+ ", damageTaken=" + damageTaken
					+ ", firefights=" + firefights
					+ ", kills=" + killCount
					+ ", bodyActive=" + targetBodyActive
					+ ", stillInLevel=" + targetStillInLevel;
		}
	}

	static final class FirstRaidJourneyEvidence {
		final boolean gateInitiallyBlocked;
		final boolean archiveSearchPrompted;
		final boolean archiveCarried;
		final boolean gateUnlockedByArchive;
		final boolean crossedUnlockedGate;
		final boolean highValueSearchPrompted;
		final boolean highValueLootCollected;
		final boolean missionCompleted;
		final boolean extractionPrompted;
		final boolean extractionCompleted;

		private FirstRaidJourneyEvidence(
				boolean gateInitiallyBlocked,
				boolean archiveSearchPrompted,
				boolean archiveCarried,
				boolean gateUnlockedByArchive,
				boolean crossedUnlockedGate,
				boolean highValueSearchPrompted,
				boolean highValueLootCollected,
				boolean missionCompleted,
				boolean extractionPrompted,
				boolean extractionCompleted) {
			this.gateInitiallyBlocked = gateInitiallyBlocked;
			this.archiveSearchPrompted = archiveSearchPrompted;
			this.archiveCarried = archiveCarried;
			this.gateUnlockedByArchive = gateUnlockedByArchive;
			this.crossedUnlockedGate = crossedUnlockedGate;
			this.highValueSearchPrompted = highValueSearchPrompted;
			this.highValueLootCollected = highValueLootCollected;
			this.missionCompleted = missionCompleted;
			this.extractionPrompted = extractionPrompted;
			this.extractionCompleted = extractionCompleted;
		}
	}

	static final class BackpackPauseEvidence {
		final int damageBeforePause;
		final float pausedElapsedDelta;
		final int pausedHealthDelta;
		final int pausedMagazineDelta;
		final float pausedEnemyMovementSquared;
		final int pausedDamageDelta;
		final float resumedElapsedDelta;
		final int resumedMagazineDelta;
		final int resumedDamageDelta;

		private BackpackPauseEvidence(
				int damageBeforePause,
				float pausedElapsedDelta,
				int pausedHealthDelta,
				int pausedMagazineDelta,
				float pausedEnemyMovementSquared,
				int pausedDamageDelta,
				float resumedElapsedDelta,
				int resumedMagazineDelta,
				int resumedDamageDelta) {
			this.damageBeforePause = damageBeforePause;
			this.pausedElapsedDelta = pausedElapsedDelta;
			this.pausedHealthDelta = pausedHealthDelta;
			this.pausedMagazineDelta = pausedMagazineDelta;
			this.pausedEnemyMovementSquared = pausedEnemyMovementSquared;
			this.pausedDamageDelta = pausedDamageDelta;
			this.resumedElapsedDelta = resumedElapsedDelta;
			this.resumedMagazineDelta = resumedMagazineDelta;
			this.resumedDamageDelta = resumedDamageDelta;
		}
	}

	static Result killOneGeneratedEnemy(
			BukovRaidCoordinator raid,
			BukovLevel level) throws IOException {
		return runGeneratedEnemyCombat(raid, level, false, false);
	}

	static Result verifyBackpackPauseAgainstGeneratedEnemy(
			BukovRaidCoordinator raid,
			BukovLevel level) throws IOException {
		return runGeneratedEnemyCombat(raid, level, true, false);
	}

	static Result completeFirstRaidThroughWorld(
			BukovRaidCoordinator raid,
			BukovLevel level) throws IOException {
		return runGeneratedEnemyCombat(raid, level, false, true);
	}

	private static Result runGeneratedEnemyCombat(
			BukovRaidCoordinator raid,
			BukovLevel level,
			boolean verifyBackpackPause,
			boolean completeFirstRaid) throws IOException {
		if (raid == null || level == null) {
			throw new IllegalArgumentException("raid and level are required");
		}

		Files previousFiles = Gdx.files;
		Input previousInput = Gdx.input;
		Camera previousCamera = Camera.main;
		int previousWidth = Game.width;
		int previousHeight = Game.height;
		boolean previousControllerActive = ControllerHandler.controllerActive;
		PointF previousLeftStick =
				new PointF(ControllerHandler.leftStickPosition);
		PointF previousRightStick =
				new PointF(ControllerHandler.rightStickPosition);
		PointF previousHover = PointerEvent.currentHoverPos();
		int previousMasterVolume = SPDSettings.bukovMasterVolume();
		int previousSfxVolume = SPDSettings.bukovSfxVolume();

		InputState input = new InputState();
		BukovRealtimeWorld world = null;
		RealtimeRaidSystem system = null;
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime = null;
		try {
			Gdx.files = headlessFiles(locateAssets());
			Gdx.input = headlessInput(input);
			SPDSettings.bukovMasterVolume(0);
			SPDSettings.bukovSfxVolume(0);
			ControllerHandler.controllerActive = false;
			ControllerHandler.leftStickPosition.set(0f, 0f);
			ControllerHandler.rightStickPosition.set(0f, 0f);

			Game.width = 1280;
			Game.height = 960;
			Camera.main = new Camera(
					0,
					0,
					Game.width,
					Game.height,
					1f);

			Hero hero = new Hero();
			hero.pos = level.entrance();
			// The first enemy must be allowed to prove its real damage path
			// without ending the acceptance journey before the return fire.
			hero.HT = 500;
			hero.HP = hero.HT;
			// No legacy Actor turn is running to resume interrupted movement in
			// this headless world. Production realtime damage itself is unchanged.
			hero.resume();
			Dungeon.level = level;
			Dungeon.hero = hero;
			level.updateFieldOfView(hero, level.heroFOV);

			FirearmRegistry firearms = new FirearmRegistry();
			firearms.loadDefault();
			AmmoRegistry ammunition = new AmmoRegistry();
			ammunition.loadDefault();
			runtime = new BukovRuntimeLoadoutAdapter(
					firearms,
					ammunition).materialize(raid);
			runtime.installOn(hero);
			Firearm firearm = runtime.primaryWeapon();
			if (firearm == null || firearm.magazineAmmo() <= 0) {
				throw new AssertionError(
						"Production raid loadout must contain a loaded firearm");
			}
			int initialMagazine = firearm.magazineAmmo();

			world = new BukovRealtimeWorld(
					hero,
					raid,
					raid::saveCheckpoint,
					false);
			system = new RealtimeRaidSystem(world, raid);
			int generatedEnemyCount = level.mobs.size();
			BukovHostMob target = onboardingGunner(level);
			if (target == null) {
				throw new AssertionError(
						"Production initial roster did not generate the onboarding gunner");
			}
			if (target.realtimeBody == null) {
				throw new AssertionError(
						"Generated enemy is missing its production realtime body");
			}
			target.sprite = new HeadlessCharSprite();
			int initialTargetHealth = target.HP;

			// Isolate the generated onboarding contact. This removes accidental
			// crossfire while retaining the exact runtime created by production.
			for (Mob mob : new ArrayList<>(level.mobs)) {
				if (mob == target) continue;
				level.mobs.remove(mob);
				mob.HP = 0;
				if (mob.realtimeBody != null) {
					mob.realtimeBody.active = false;
				}
			}
			placeHeroForVisibleContact(hero, level, target);

			FxEvidence fx = new FxEvidence();
			for (int frame = 0;
					frame < ENEMY_FIRE_TIMEOUT_FRAMES
							&& raid.session().balanceTelemetry()
									.damageTaken() == 0;
					frame++) {
				aimAt(target);
				system.update(RENDER_STEP);
				system.drainCombatFx(fx::accept);
			}

			BackpackPauseEvidence backpackPause = verifyBackpackPause
					? exerciseBackpackPause(
							world,
							system,
							raid,
							hero,
							firearm,
							target,
							input)
					: null;

			for (int frame = 0;
					frame < PLAYER_KILL_TIMEOUT_FRAMES
							&& target.isAlive();
					frame++) {
				// The starter sidearm is semi-automatic, so drive real trigger
				// release/press edges instead of holding an automatic-fire input.
				input.fire = frame % 2 == 0;
				aimAt(target);
				system.update(RENDER_STEP);
				system.drainCombatFx(fx::accept);
			}
			input.fire = false;
			for (int frame = 0; frame < REMOVAL_REFRESH_FRAMES; frame++) {
				aimAt(target);
				system.update(RENDER_STEP);
				system.drainCombatFx(fx::accept);
			}

			FirstRaidJourneyEvidence firstRaidJourney =
					completeFirstRaid
							? exerciseFirstRaidJourney(
									world,
									system,
									raid,
									level,
									hero,
									input)
							: null;

			runtime.writeBack(raid.loot());
			RaidBalanceTelemetry telemetry =
					raid.session().balanceTelemetry();
			return new Result(
					generatedEnemyCount,
					target.definitionId(),
					initialTargetHealth,
					target.HP,
					initialMagazine,
					firearm.magazineAmmo(),
					fx,
					telemetry,
					raid.session().killCount(),
					target.realtimeBody.active,
					level.mobs.contains(target),
					backpackPause,
					firstRaidJourney);
		} finally {
			if (system != null) {
				system.dispose();
			} else if (world != null) {
				world.disposeRealtimeObjects();
			}
			Gdx.files = previousFiles;
			Gdx.input = previousInput;
			Camera.main = previousCamera;
			Game.width = previousWidth;
			Game.height = previousHeight;
			ControllerHandler.controllerActive = previousControllerActive;
			ControllerHandler.leftStickPosition.set(previousLeftStick);
			ControllerHandler.rightStickPosition.set(previousRightStick);
			PointerEvent.setHoverPos(previousHover);
			SPDSettings.bukovMasterVolume(previousMasterVolume);
			SPDSettings.bukovSfxVolume(previousSfxVolume);
		}
	}

	private static BackpackPauseEvidence exerciseBackpackPause(
			BukovRealtimeWorld world,
			RealtimeRaidSystem system,
			BukovRaidCoordinator raid,
			Hero hero,
			Firearm firearm,
			BukovHostMob target,
			InputState input) {
		aimAwayFrom(target, hero);
		input.fire = true;
		float elapsedBeforePause = raid.session().elapsedSeconds;
		int heroHealthBeforePause = hero.HP;
		int magazineBeforePause = firearm.magazineAmmo();
		float enemyXBeforePause = target.realtimeBody.x;
		float enemyYBeforePause = target.realtimeBody.y;
		int damageBeforePause =
				raid.session().balanceTelemetry().damageTaken();

		world.setBackpackOpen(true);
		system.update(1f);

		float elapsedDuringPause = raid.session().elapsedSeconds;
		int heroHealthDuringPause = hero.HP;
		int magazineDuringPause = firearm.magazineAmmo();
		float enemyXDuringPause = target.realtimeBody.x;
		float enemyYDuringPause = target.realtimeBody.y;
		int damageDuringPause =
				raid.session().balanceTelemetry().damageTaken();

		world.setBackpackOpen(false);
		system.update(RENDER_STEP);
		float elapsedAfterResumeFrame = raid.session().elapsedSeconds;
		int magazineAfterResumeFrame = firearm.magazineAmmo();

		input.fire = false;
		for (int frame = 0;
				frame < ENEMY_FIRE_TIMEOUT_FRAMES
						&& raid.session().balanceTelemetry().damageTaken()
								<= damageBeforePause;
				frame++) {
			aimAt(target);
			system.update(RENDER_STEP);
		}

		return new BackpackPauseEvidence(
				damageBeforePause,
				elapsedDuringPause - elapsedBeforePause,
				heroHealthDuringPause - heroHealthBeforePause,
				magazineDuringPause - magazineBeforePause,
				(enemyXDuringPause - enemyXBeforePause)
						* (enemyXDuringPause - enemyXBeforePause)
						+ (enemyYDuringPause - enemyYBeforePause)
						* (enemyYDuringPause - enemyYBeforePause),
				damageDuringPause - damageBeforePause,
				elapsedAfterResumeFrame - elapsedBeforePause,
				magazineAfterResumeFrame - magazineBeforePause,
				raid.session().balanceTelemetry().damageTaken()
						- damageBeforePause);
	}

	private static FirstRaidJourneyEvidence exerciseFirstRaidJourney(
			BukovRealtimeWorld world,
			RealtimeRaidSystem system,
			BukovRaidCoordinator raid,
			BukovLevel level,
			Hero hero,
			InputState input) {
		BukovRaidLayout.MissionGate gate = level.missionGate();
		if (gate == null || gate.gateCells.length == 0) {
			throw new AssertionError(
					"Production first raid is missing its mission gate");
		}
		LevelCollisionMap collision = new LevelCollisionMap(level);
		boolean gateInitiallyBlocked =
				allGateCellsBlocked(collision, level.width(), gate.gateCells);

		BukovRaidCoordinator.ContainerSnapshot archive =
				raid.container(FirstRaidMission.ARCHIVE_CONTAINER_ID);
		if (archive == null) {
			throw new AssertionError(
					"Production first raid is missing the archive container");
		}
		placeHero(hero, level, archive.cell);
		idleFrame(system, input);
		boolean archiveSearchPrompted =
				interaction(world) == BukovRaidHudState.Interaction.SEARCH;
		holdInteract(system, input, archive.searchSeconds + 0.25f);
		for (int frame = 0;
				frame < 30
						&& raid.loot().firstItemUidForDefinition(
								FirstRaidMission.ARCHIVE_DEFINITION_ID) == null;
				frame++) {
			idleFrame(system, input);
		}
		boolean archiveCarried =
				raid.loot().firstItemUidForDefinition(
						FirstRaidMission.ARCHIVE_DEFINITION_ID) != null;
		boolean gateUnlockedByArchive =
				raid.eventCompleted(FirstRaidMission.EVENT_ID)
						&& allGateCellsOpen(
								collision,
								level.width(),
								gate.gateCells);
		boolean crossedUnlockedGate =
				gateUnlockedByArchive
						&& crossMissionGate(
								system,
								input,
								hero,
								level,
								collision,
								gate.gateCells);

		BukovRaidCoordinator.ContainerSnapshot highValue =
				containerForTable(
						raid,
						FirstRaidMission.HIGH_VALUE_LOOT_TABLE_ID);
		if (highValue == null) {
			throw new AssertionError(
					"Production first raid is missing its high-value container");
		}
		placeHero(hero, level, highValue.cell);
		idleFrame(system, input);
		boolean highValueSearchPrompted =
				interaction(world) == BukovRaidHudState.Interaction.SEARCH;
		long lootBeforeSearch = raid.loot().totalQuantity();
		holdInteract(system, input, highValue.searchSeconds + 0.25f);
		for (int attempt = 0;
				attempt < 8
						&& raid.loot().totalQuantity() <= lootBeforeSearch;
				attempt++) {
			tapInteract(system, input);
		}
		boolean highValueLootCollected =
				raid.loot().totalQuantity() > lootBeforeSearch;
		boolean missionCompleted = raid.firstRaidMissionCompleted();

		ExtractionState extraction = raid.extraction("E01");
		int extractionCell = level.extractionCell("E01");
		if (extraction == null || extractionCell < 0) {
			throw new AssertionError(
					"Production first raid is missing baseline extraction E01");
		}
		placeHero(hero, level, extractionCell);
		idleFrame(system, input);
		boolean extractionPrompted =
				interaction(world) == BukovRaidHudState.Interaction.EXTRACT;
		holdInteract(
				system,
				input,
				extraction.interactionSeconds() + 0.25f);

		return new FirstRaidJourneyEvidence(
				gateInitiallyBlocked,
				archiveSearchPrompted,
				archiveCarried,
				gateUnlockedByArchive,
				crossedUnlockedGate,
				highValueSearchPrompted,
				highValueLootCollected,
				missionCompleted,
				extractionPrompted,
				extraction.completed());
	}

	private static BukovRaidHudState.Interaction interaction(
			BukovRealtimeWorld world) {
		BukovRaidHudState state = new BukovRaidHudState();
		world.readRaidHudState(state);
		return state.interaction();
	}

	private static void holdInteract(
			RealtimeRaidSystem system,
			InputState input,
			float seconds) {
		int frames = Math.max(
				2,
				(int)Math.ceil(seconds / RENDER_STEP) + 2);
		input.interactHeld = true;
		for (int frame = 0; frame < frames; frame++) {
			input.interactPressed = frame == 0;
			system.update(RENDER_STEP);
		}
		input.interactHeld = false;
		input.interactPressed = false;
		system.update(RENDER_STEP);
	}

	private static void tapInteract(
			RealtimeRaidSystem system,
			InputState input) {
		input.interactHeld = true;
		input.interactPressed = true;
		system.update(RENDER_STEP);
		input.interactHeld = false;
		input.interactPressed = false;
		system.update(RENDER_STEP);
	}

	private static void idleFrame(
			RealtimeRaidSystem system,
			InputState input) {
		input.interactHeld = false;
		input.interactPressed = false;
		input.movementKey = -1;
		system.update(RENDER_STEP);
	}

	static void placeHero(
			Hero hero,
			BukovLevel level,
			int cell) {
		hero.pos = cell;
		RealtimeBody body = hero.ensureRealtimeBody();
		body.x = cell % level.width() + 0.5f;
		body.y = cell / level.width() + 0.5f;
		body.previousX = body.x;
		body.previousY = body.y;
		body.velocityX = 0f;
		body.velocityY = 0f;
		level.updateFieldOfView(hero, level.heroFOV);
	}

	private static void placeHeroForVisibleContact(
			Hero hero,
			BukovLevel level,
			BukovHostMob target) {
		LevelCollisionMap collision = new LevelCollisionMap(level);
		int targetX = target.pos % level.width();
		int targetY = target.pos / level.width();
		int[] directions = {
				-1, 0,
				1, 0,
				0, -1,
				0, 1
		};
		for (int distance = 4; distance >= 2; distance--) {
			for (int index = 0; index < directions.length; index += 2) {
				int deltaX = directions[index];
				int deltaY = directions[index + 1];
				int heroX = targetX + deltaX * distance;
				int heroY = targetY + deltaY * distance;
				if (heroX < 0
						|| heroX >= level.width()
						|| heroY < 0
						|| heroY >= level.height()
						|| collision.blocked(heroX, heroY)) {
					continue;
				}
				boolean clear = true;
				for (int step = 1; step < distance; step++) {
					if (collision.blocksLine(
							targetX + deltaX * step,
							targetY + deltaY * step)) {
						clear = false;
						break;
					}
				}
				if (clear) {
					placeHero(
							hero,
							level,
							heroX + heroY * level.width());
					return;
				}
			}
		}
		throw new AssertionError(
				"Generated onboarding contact has no visible combat lane");
	}

	private static boolean allGateCellsBlocked(
			LevelCollisionMap collision,
			int width,
			int[] gateCells) {
		for (int cell : gateCells) {
			if (!collision.blocked(cell % width, cell / width)) {
				return false;
			}
		}
		return true;
	}

	private static boolean allGateCellsOpen(
			LevelCollisionMap collision,
			int width,
			int[] gateCells) {
		for (int cell : gateCells) {
			if (collision.blocked(cell % width, cell / width)) {
				return false;
			}
		}
		return true;
	}

	private static boolean crossMissionGate(
			RealtimeRaidSystem system,
			InputState input,
			Hero hero,
			BukovLevel level,
			LevelCollisionMap collision,
			int[] gateCells) {
		GateCrossing crossing =
				findGateCrossing(level, collision, gateCells);
		if (crossing == null) return false;
		placeHero(hero, level, crossing.startCell);
		input.movementKey = crossing.movementKey;
		boolean crossed = false;
		for (int frame = 0; frame < 180; frame++) {
			system.update(RENDER_STEP);
			if (hero.pos == crossing.endCell) {
				crossed = true;
				break;
			}
		}
		input.movementKey = -1;
		system.update(RENDER_STEP);
		return crossed;
	}

	private static GateCrossing findGateCrossing(
			BukovLevel level,
			LevelCollisionMap collision,
			int[] gateCells) {
		int width = level.width();
		for (int gateCell : gateCells) {
			GateCrossing horizontal = gateCrossing(
					gateCell - 1,
					gateCell + 1,
					Input.Keys.D,
					level,
					collision,
					gateCells);
			if (horizontal != null) return horizontal;
			GateCrossing vertical = gateCrossing(
					gateCell - width,
					gateCell + width,
					Input.Keys.S,
					level,
					collision,
					gateCells);
			if (vertical != null) return vertical;
		}
		return null;
	}

	private static GateCrossing gateCrossing(
			int startCell,
			int endCell,
			int movementKey,
			BukovLevel level,
			LevelCollisionMap collision,
			int[] gateCells) {
		if (!walkableNonGate(
					startCell, level, collision, gateCells)
				|| !walkableNonGate(
						endCell, level, collision, gateCells)) {
			return null;
		}
		return new GateCrossing(startCell, endCell, movementKey);
	}

	private static boolean walkableNonGate(
			int cell,
			BukovLevel level,
			LevelCollisionMap collision,
			int[] gateCells) {
		if (cell < 0 || cell >= level.length()) return false;
		for (int gateCell : gateCells) {
			if (cell == gateCell) return false;
		}
		return !collision.blocked(
				cell % level.width(),
				cell / level.width());
	}

	private static BukovRaidCoordinator.ContainerSnapshot containerForTable(
			BukovRaidCoordinator raid,
			String lootTableId) {
		for (BukovRaidCoordinator.ContainerSnapshot container :
				raid.containers()) {
			if (lootTableId.equals(container.lootTableId)) {
				return container;
			}
		}
		return null;
	}

	private static final class GateCrossing {
		private final int startCell;
		private final int endCell;
		private final int movementKey;

		private GateCrossing(
				int startCell,
				int endCell,
				int movementKey) {
			this.startCell = startCell;
			this.endCell = endCell;
			this.movementKey = movementKey;
		}
	}

	private static BukovHostMob onboardingGunner(BukovLevel level) {
		BukovHostMob fallback = null;
		for (Mob mob : level.mobs) {
			if (!(mob instanceof BukovHostMob)) continue;
			BukovHostMob candidate = (BukovHostMob)mob;
			if (!FirstRaidEnemySpawnDirector.FIRST_GUNNER.equals(
					candidate.definitionId())) {
				continue;
			}
			if (candidate.onboardingContact()) return candidate;
			fallback = candidate;
		}
		return fallback;
	}

	static void aimAt(BukovHostMob target) {
		float worldX = target.realtimeBody.x * DungeonTilemap.SIZE;
		float worldY = target.realtimeBody.y * DungeonTilemap.SIZE;
		Point screen = Camera.main.cameraToScreen(worldX, worldY);
		PointerEvent.setHoverPos(new PointF(screen.x, screen.y));
	}

	private static void aimAwayFrom(BukovHostMob target, Hero hero) {
		RealtimeBody body = hero.ensureRealtimeBody();
		float awayX = body.x - target.realtimeBody.x;
		float awayY = body.y - target.realtimeBody.y;
		if (awayX * awayX + awayY * awayY <= 0.0001f) {
			awayX = 1f;
		}
		Point screen = Camera.main.cameraToScreen(
				(body.x + awayX * 4f) * DungeonTilemap.SIZE,
				(body.y + awayY * 4f) * DungeonTilemap.SIZE);
		PointerEvent.setHoverPos(new PointF(screen.x, screen.y));
	}

	static Files headlessFiles(Path assets) {
		InvocationHandler handler = (proxy, method, arguments) -> {
			String name = method.getName();
			if ("internal".equals(name)
					|| "classpath".equals(name)
					|| "local".equals(name)) {
				return new FileHandle(
						assets.resolve((String)arguments[0]).toFile());
			}
			if ("absolute".equals(name) || "external".equals(name)) {
				return new FileHandle(new File((String)arguments[0]));
			}
			if ("getFileHandle".equals(name)) {
				return new FileHandle(
						assets.resolve((String)arguments[0]).toFile());
			}
			if ("getExternalStoragePath".equals(name)
					|| "getLocalStoragePath".equals(name)) {
				return assets.toString();
			}
			if ("isExternalStorageAvailable".equals(name)
					|| "isLocalStorageAvailable".equals(name)) {
				return true;
			}
			return objectMethod(proxy, method, arguments);
		};
		return (Files)Proxy.newProxyInstance(
				Files.class.getClassLoader(),
				new Class<?>[]{Files.class},
				handler);
	}

	static Input headlessInput(InputState input) {
		InvocationHandler handler = (proxy, method, arguments) -> {
			String name = method.getName();
			if ("isButtonPressed".equals(name)
					|| "isButtonJustPressed".equals(name)) {
				return input.fire
						&& ((Integer)arguments[0]) == Input.Buttons.LEFT;
			}
			if ("isKeyPressed".equals(name)) {
				int key = (Integer)arguments[0];
				return key == input.movementKey
						|| key == Input.Keys.E && input.interactHeld;
			}
			if ("isKeyJustPressed".equals(name)) {
				int key = (Integer)arguments[0];
				return key == Input.Keys.E && input.interactPressed
						|| key == Input.Keys.R && input.reloadPressed;
			}
			Class<?> type = method.getReturnType();
			if (type == boolean.class) return false;
			if (type == int.class) return 0;
			if (type == long.class) return 0L;
			if (type == float.class) return 0f;
			if (type == double.class) return 0d;
			if (type == void.class) return null;
			return objectMethod(proxy, method, arguments);
		};
		return (Input)Proxy.newProxyInstance(
				Input.class.getClassLoader(),
				new Class<?>[]{Input.class},
				handler);
	}

	private static Object objectMethod(
			Object proxy,
			Method method,
			Object[] arguments) {
		switch (method.getName()) {
			case "toString":
				return "BukovHeadless" + proxy.getClass().getInterfaces()[0]
						.getSimpleName();
			case "hashCode":
				return System.identityHashCode(proxy);
			case "equals":
				return proxy == arguments[0];
			default:
				return null;
		}
	}

	static Path locateAssets() {
		Path current = Paths.get(System.getProperty("user.dir", "."))
				.toAbsolutePath()
				.normalize();
		for (int depth = 0; depth < 8 && current != null; depth++) {
			Path moduleAssets = current.resolve("src/main/assets");
			if (hasContent(moduleAssets)) return moduleAssets;
			Path repositoryAssets = current.resolve("core/src/main/assets");
			if (hasContent(repositoryAssets)) return repositoryAssets;
			current = current.getParent();
		}
		throw new IllegalStateException(
				"Cannot locate core/src/main/assets for realtime combat");
	}

	private static boolean hasContent(Path assets) {
		return assets.resolve("bukov/content/firearms.json")
				.toFile()
				.isFile();
	}

	static final class InputState {
		boolean fire;
		boolean interactHeld;
		boolean interactPressed;
		boolean reloadPressed;
		int movementKey = -1;
	}

	private static final class FxEvidence {
		private int friendlyTracers;
		private int hostileTracers;
		private boolean nonZeroFriendlyTracer;
		private boolean nonZeroHostileTracer;

		private void accept(CombatFxEvent event) {
			if (event.type() != CombatFxEvent.Type.TRACER) return;
			float deltaX = event.toX() - event.fromX();
			float deltaY = event.toY() - event.fromY();
			boolean nonZero = event.intensity() > 0f
					&& deltaX * deltaX + deltaY * deltaY > 0.0001f;
			if (event.hostile()) {
				hostileTracers++;
				nonZeroHostileTracer |= nonZero;
			} else {
				friendlyTracers++;
				nonZeroFriendlyTracer |= nonZero;
			}
		}
	}

	static final class HeadlessCharSprite extends CharSprite {
		HeadlessCharSprite() {
			visible = false;
		}

		@Override
		public void showStatus(int color, String text, Object... arguments) {
			// Floating text is presentation-only and has no scene in this test.
		}

		@Override
		public void die() {
			// Host death remains authoritative; only animation is suppressed.
		}
	}

	private BukovRealtimeCombatHarness() {
	}
}
