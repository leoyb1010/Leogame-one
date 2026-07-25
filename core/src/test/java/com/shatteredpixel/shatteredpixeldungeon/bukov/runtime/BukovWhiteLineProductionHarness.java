package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.BukovHostMob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.FirstRaidEnemySpawnDirector;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.GridLineOfSight;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.WhiteLineBossStateMachine;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatPresentationEvent;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovLevel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.levels.BukovRaidLayout;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRuntimeLoadoutAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovRaidHudState;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.watabou.input.ControllerHandler;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;

import java.io.IOException;

/**
 * Narrow headless acceptance driver for the production White Line encounter.
 *
 * <p>Only platform boundaries and operator position are supplied by the test.
 * Spawn eligibility, objective interactions, firearm input, damage, phase
 * transitions, death, loot release, contract completion and extraction all
 * remain owned by {@link BukovRealtimeWorld}.</p>
 */
final class BukovWhiteLineProductionHarness {

	private static final float RENDER_STEP = 1f / 60f;
	private static final int SPAWN_TIMEOUT_FRAMES = 7200;
	private static final int PHASE_DAMAGE_TIMEOUT_FRAMES = 2400;
	private static final int RELOAD_TIMEOUT_FRAMES = 360;

	static final class Evidence {
		final int initialHealth;
		final int finalHealth;
		final int playerFireEvents;
		final int phaseBreakEvents;
		final int slamEvents;
		final int overloadEvents;
		final int weakpointKillEvents;
		final int bossLootCount;
		final int killCount;
		final boolean bodyInactive;
		final boolean removedFromLevel;
		final boolean levelResolved;
		final boolean contractCompleted;
		final boolean extractionAvailable;
		final boolean extractionPrompted;
		final boolean extractionCompleted;

		private Evidence(
				int initialHealth,
				int finalHealth,
				PresentationEvidence presentation,
				int bossLootCount,
				int killCount,
				boolean bodyInactive,
				boolean removedFromLevel,
				boolean levelResolved,
				boolean contractCompleted,
				boolean extractionAvailable,
				boolean extractionPrompted,
				boolean extractionCompleted) {
			this.initialHealth = initialHealth;
			this.finalHealth = finalHealth;
			playerFireEvents = presentation.playerFireEvents;
			phaseBreakEvents = presentation.phaseBreakEvents;
			slamEvents = presentation.slamEvents;
			overloadEvents = presentation.overloadEvents;
			weakpointKillEvents = presentation.weakpointKillEvents;
			this.bossLootCount = bossLootCount;
			this.killCount = killCount;
			this.bodyInactive = bodyInactive;
			this.removedFromLevel = removedFromLevel;
			this.levelResolved = levelResolved;
			this.contractCompleted = contractCompleted;
			this.extractionAvailable = extractionAvailable;
			this.extractionPrompted = extractionPrompted;
			this.extractionCompleted = extractionCompleted;
		}

		@Override
		public String toString() {
			return "bossHp=" + initialHealth + "->" + finalHealth
					+ ", fire=" + playerFireEvents
					+ ", phaseBreak=" + phaseBreakEvents
					+ ", slam=" + slamEvents
					+ ", overload=" + overloadEvents
					+ ", weakpointKill=" + weakpointKillEvents
					+ ", loot=" + bossLootCount
					+ ", kills=" + killCount
					+ ", contract=" + contractCompleted
					+ ", extraction=" + extractionCompleted;
		}
	}

	static Evidence defeatThroughProductionWorld(
			BukovRaidCoordinator raid,
			BukovLevel level) throws IOException {
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

		BukovRealtimeCombatHarness.InputState input =
				new BukovRealtimeCombatHarness.InputState();
		BukovRealtimeWorld world = null;
		RealtimeRaidSystem system = null;
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime = null;
		try {
			Gdx.files = BukovRealtimeCombatHarness.headlessFiles(
					BukovRealtimeCombatHarness.locateAssets());
			Gdx.input = BukovRealtimeCombatHarness.headlessInput(input);
			SPDSettings.bukovMasterVolume(0);
			SPDSettings.bukovSfxVolume(0);
			ControllerHandler.controllerActive = false;
			ControllerHandler.leftStickPosition.set(0f, 0f);
			ControllerHandler.rightStickPosition.set(0f, 0f);
			Game.width = 1280;
			Game.height = 960;
			Camera.main = new Camera(
					0, 0, Game.width, Game.height, 1f);

			Hero hero = new Hero();
			hero.pos = level.entrance();
			// Survive the authored 90-second first-raid protection window. Boss
			// health and all hostile damage remain untouched production values.
			hero.HT = 50_000;
			hero.HP = hero.HT;
			hero.resume();
			Dungeon.level = level;
			Dungeon.hero = hero;
			level.updateFieldOfView(hero, level.heroFOV);

			FirearmRegistry firearms = new FirearmRegistry();
			firearms.loadDefault();
			AmmoRegistry ammunition = new AmmoRegistry();
			ammunition.loadDefault();
			runtime = new BukovRuntimeLoadoutAdapter(
					firearms, ammunition).materialize(raid);
			runtime.installOn(hero);
			Firearm firearm = runtime.primaryWeapon();
			if (firearm == null || firearm.magazineAmmo() <= 0) {
				throw new AssertionError(
						"Boss contract must materialize a loaded firearm");
			}

			world = new BukovRealtimeWorld(
					hero, raid, raid::saveCheckpoint, false);
			system = new RealtimeRaidSystem(world, raid);
			PresentationEvidence presentation =
					new PresentationEvidence();
			BukovHostMob boss = awaitBossSpawn(
					system, level, input, presentation);
			boss.sprite =
					new BukovRealtimeCombatHarness.HeadlessCharSprite();
			int initialHealth = boss.HP;
			WhiteLineBossStateMachine state =
					level.whiteLineState(boss.HT);
			BukovRaidLayout.BossMechanism mechanism =
					level.bossMechanism();
			if (mechanism == null) {
				throw new AssertionError(
						"Generated Boss contract has no White Line mechanism");
			}
			LevelCollisionMap collision = new LevelCollisionMap(level);

			engageAndExposeUmbrella(
					system,
					input,
					presentation,
					hero,
					boss,
					state,
					level,
					collision,
					mechanism.fogLampCell);
			fireUntilPhase(
					system,
					input,
					presentation,
					world,
					hero,
					boss,
					state,
					level,
					collision,
					firearm,
					WhiteLineBossStateMachine.Phase.DECOY_SEARCH);
			awaitFeedback(
					system,
					input,
					presentation,
					hero,
					boss,
					level,
					collision,
					CombatFeedbackType.BOSS_SLAM);

			BukovRealtimeCombatHarness.placeHero(
					hero,
					level,
					mechanism.bodyTraceCells[state.trueBodyIndex()]);
			interactUntilVulnerable(
					system,
					input,
					presentation,
					state,
					"true-body interaction");
			fireUntilPhase(
					system,
					input,
					presentation,
					world,
					hero,
					boss,
					state,
					level,
					collision,
					firearm,
					WhiteLineBossStateMachine.Phase.FOG_LAMP_OVERLOAD);
			awaitFeedback(
					system,
					input,
					presentation,
					hero,
					boss,
					level,
					collision,
					CombatFeedbackType.BOSS_OVERLOAD);

			BukovRealtimeCombatHarness.placeHero(
					hero,
					level,
					interactionCellNear(
							mechanism.fogLampCell,
							level,
							collision));
			interactUntilVulnerable(
					system,
					input,
					presentation,
					state,
					"fog-lamp interaction");
			long carriedBeforeFinalDamage =
					raid.loot().totalQuantity();
			fireUntilPhase(
					system,
					input,
					presentation,
					world,
					hero,
					boss,
					state,
					level,
					collision,
					firearm,
					WhiteLineBossStateMachine.Phase.DEFEATED);

			input.fire = false;
			for (int frame = 0; frame < 30; frame++) {
				step(system, input, presentation);
			}
			int deathCell = boss.pos;
			Heap bossHeap = level.heaps.get(deathCell);
			long autoPickedBossLoot = Math.max(
					0L,
					raid.loot().totalQuantity()
							- carriedBeforeFinalDamage);
			int bossLootCount = (bossHeap == null
					? 0 : bossHeap.items.size())
					+ (int)Math.min(Integer.MAX_VALUE, autoPickedBossLoot);

			ExtractionState extraction = raid.extraction("E01");
			boolean extractionAvailable = extraction != null
					&& extraction.availableAt(
							raid.session().elapsedSeconds);
			int extractionCell = level.extractionCell("E01");
			BukovRealtimeCombatHarness.placeHero(
					hero, level, extractionCell);
			step(system, input, presentation);
			BukovRaidHudState hud = new BukovRaidHudState();
			world.readRaidHudState(hud);
			boolean extractionPrompted =
					hud.interaction()
							== BukovRaidHudState.Interaction.EXTRACT;
			holdInteract(
					system,
					input,
					presentation,
					extraction == null
							? 0f : extraction.interactionSeconds() + 0.25f);

			runtime.writeBack(raid.loot());
			return new Evidence(
					initialHealth,
					state.health(),
					presentation,
					bossLootCount,
					raid.session().killCount(),
					boss.realtimeBody == null
							|| !boss.realtimeBody.active,
					!level.mobs.contains(boss),
					level.whiteLineResolved(),
					raid.bossContractCompleted(),
					extractionAvailable,
					extractionPrompted,
					extraction != null && extraction.completed());
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

	private static BukovHostMob awaitBossSpawn(
			RealtimeRaidSystem system,
			BukovLevel level,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation) {
		for (int frame = 0; frame < SPAWN_TIMEOUT_FRAMES; frame++) {
			step(system, input, presentation);
			BukovHostMob boss = whiteLine(level);
			if (boss != null) return boss;
		}
		throw new AssertionError(
				"Production Boss contract did not spawn White Line");
	}

	private static BukovHostMob whiteLine(BukovLevel level) {
		for (Mob mob : level.mobs) {
			if (mob instanceof BukovHostMob
					&& FirstRaidEnemySpawnDirector.FIRST_BOSS.equals(
							((BukovHostMob)mob).definitionId())
					&& mob.isAlive()) {
				return (BukovHostMob)mob;
			}
		}
		return null;
	}

	private static void engageAndExposeUmbrella(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			Hero hero,
			BukovHostMob boss,
			WhiteLineBossStateMachine state,
			BukovLevel level,
			LevelCollisionMap collision,
			int facingCell) {
		int flankCell = flankCell(
				boss.pos, facingCell, level, collision);
		BukovRealtimeCombatHarness.placeHero(
				hero, level, flankCell);
		BukovRealtimeCombatHarness.aimAt(boss);
		for (int frame = 0;
				frame < 120
						&& state.phase()
								== WhiteLineBossStateMachine.Phase.DORMANT;
				frame++) {
			step(system, input, presentation);
			BukovRealtimeCombatHarness.aimAt(boss);
		}
		if (state.phase()
				!= WhiteLineBossStateMachine.Phase.UMBRELLA_SHIELD) {
			throw new AssertionError(
					"Player contact did not engage White Line");
		}
		interactUntilVulnerable(
				system,
				input,
				presentation,
				state,
				"umbrella flank");
	}

	private static void fireUntilPhase(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			BukovRealtimeWorld world,
			Hero hero,
			BukovHostMob boss,
			WhiteLineBossStateMachine state,
			BukovLevel level,
			LevelCollisionMap collision,
			Firearm firearm,
			WhiteLineBossStateMachine.Phase expected) {
		BukovRealtimeCombatHarness.placeHero(
				hero,
				level,
				shootingCell(boss, level, collision));
		for (int frame = 0;
				frame < PHASE_DAMAGE_TIMEOUT_FRAMES;
				frame++) {
			if (state.phase() == expected) {
				input.fire = false;
				step(system, input, presentation);
				return;
			}
			if (!boss.isAlive()) {
				throw new AssertionError(
						"White Line died before expected phase " + expected);
			}
			if (firearm.magazineAmmo() == 0) {
				reload(
						system,
						input,
						presentation,
						world,
						firearm);
			}
			if (frame % 120 == 0) {
				BukovRealtimeCombatHarness.placeHero(
						hero,
						level,
						shootingCell(boss, level, collision));
			}
			input.fire = frame % 2 == 0;
			BukovRealtimeCombatHarness.aimAt(boss);
			step(system, input, presentation);
		}
		throw new AssertionError(
				"Production player fire did not reach phase " + expected
						+ "; current=" + state.phase()
						+ ", health=" + state.health());
	}

	private static void reload(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			BukovRealtimeWorld world,
			Firearm firearm) {
		input.fire = false;
		if (!world.reloadActionAvailable()) {
			throw new AssertionError(
					"Boss production run exhausted firearm reserves");
		}
		input.reloadPressed = true;
		step(system, input, presentation);
		input.reloadPressed = false;
		for (int frame = 0;
				frame < RELOAD_TIMEOUT_FRAMES
						&& firearm.magazineAmmo() == 0;
				frame++) {
			step(system, input, presentation);
		}
		if (firearm.magazineAmmo() == 0) {
			throw new AssertionError(
					"Production reload did not refill the magazine");
		}
	}

	private static void awaitFeedback(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			Hero hero,
			BukovHostMob boss,
			BukovLevel level,
			LevelCollisionMap collision,
			CombatFeedbackType expected) {
		int baseline = presentation.count(expected);
		BukovRealtimeCombatHarness.placeHero(
				hero,
				level,
				shootingCell(boss, level, collision));
		for (int frame = 0; frame < 360; frame++) {
			BukovRealtimeCombatHarness.aimAt(boss);
			step(system, input, presentation);
			if (presentation.count(expected) > baseline) return;
		}
		throw new AssertionError(
				"Production White Line never emitted " + expected);
	}

	private static int shootingCell(
			BukovHostMob boss,
			BukovLevel level,
			LevelCollisionMap collision) {
		int width = level.width();
		float bossX = boss.realtimeBody.x;
		float bossY = boss.realtimeBody.y;
		int best = -1;
		float bestDistance = Float.MAX_VALUE;
		for (int cell = 0; cell < level.length(); cell++) {
			int x = cell % width;
			int y = cell / width;
			if (collision.blocked(x, y)) continue;
			float centerX = x + 0.5f;
			float centerY = y + 0.5f;
			float dx = centerX - bossX;
			float dy = centerY - bossY;
			float distance = (float)Math.sqrt(dx * dx + dy * dy);
			if (distance < 2f || distance > 5f
					|| !GridLineOfSight.visible(
							centerX,
							centerY,
							bossX,
							bossY,
							6f,
							collision)) {
				continue;
			}
			float error = Math.abs(distance - 3f);
			if (error < bestDistance) {
				best = cell;
				bestDistance = error;
			}
		}
		if (best < 0) {
			throw new AssertionError(
					"White Line has no clear production firing lane");
		}
		return best;
	}

	private static int flankCell(
			int bossCell,
			int facingCell,
			BukovLevel level,
			LevelCollisionMap collision) {
		int width = level.width();
		int bossX = bossCell % width;
		int bossY = bossCell / width;
		float facingX = facingCell % width - bossX;
		float facingY = facingCell / width - bossY;
		int best = -1;
		float bestDot = Float.MAX_VALUE;
		int[][] offsets = {
				{1, 0}, {-1, 0}, {0, 1}, {0, -1},
				{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
		};
		for (int[] offset : offsets) {
			int x = bossX + offset[0];
			int y = bossY + offset[1];
			if (x < 0 || x >= width
					|| y < 0 || y >= level.height()
					|| collision.blocked(x, y)) {
				continue;
			}
			float dot = facingX * offset[0] + facingY * offset[1];
			if (dot < bestDot) {
				bestDot = dot;
				best = x + y * width;
			}
		}
		if (best < 0) {
			throw new AssertionError(
					"White Line has no reachable umbrella flank");
		}
		return best;
	}

	private static int interactionCellNear(
			int targetCell,
			BukovLevel level,
			LevelCollisionMap collision) {
		int width = level.width();
		int targetX = targetCell % width;
		int targetY = targetCell / width;
		int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] offset : offsets) {
			int x = targetX + offset[0];
			int y = targetY + offset[1];
			if (x >= 0 && x < width
					&& y >= 0 && y < level.height()
					&& !collision.blocked(x, y)) {
				return x + y * width;
			}
		}
		throw new AssertionError(
				"White Line fog-lamp control has no interaction cell");
	}

	private static void interactUntilVulnerable(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			WhiteLineBossStateMachine state,
			String objective) {
		for (int attempt = 0;
				attempt < 8 && !state.vulnerable();
				attempt++) {
			tapInteract(system, input, presentation);
		}
		if (!state.vulnerable()) {
			throw new AssertionError(
					"Production " + objective
							+ " did not expose the weak point");
		}
	}

	private static void tapInteract(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation) {
		input.fire = false;
		input.interactHeld = true;
		input.interactPressed = true;
		step(system, input, presentation);
		input.interactHeld = false;
		input.interactPressed = false;
		step(system, input, presentation);
	}

	private static void holdInteract(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation,
			float seconds) {
		int frames = Math.max(
				2,
				(int)Math.ceil(seconds / RENDER_STEP) + 2);
		input.fire = false;
		input.interactHeld = true;
		for (int frame = 0; frame < frames; frame++) {
			input.interactPressed = frame == 0;
			step(system, input, presentation);
		}
		input.interactHeld = false;
		input.interactPressed = false;
		step(system, input, presentation);
	}

	private static void step(
			RealtimeRaidSystem system,
			BukovRealtimeCombatHarness.InputState input,
			PresentationEvidence presentation) {
		system.update(RENDER_STEP);
		system.drainCombatPresentation(presentation);
		input.reloadPressed = false;
	}

	private static final class PresentationEvidence
			implements CombatPresentationEvent.Consumer {
		private int playerFireEvents;
		private int phaseBreakEvents;
		private int slamEvents;
		private int overloadEvents;
		private int weakpointKillEvents;

		@Override
		public void accept(CombatPresentationEvent event) {
			if (event.type()
					== CombatPresentationEvent.Type.PLAYER_FIRE) {
				playerFireEvents++;
			}
			if (event.feedbackType()
					== CombatFeedbackType.BOSS_PHASE_BREAK) {
				phaseBreakEvents++;
			} else if (event.feedbackType()
					== CombatFeedbackType.BOSS_SLAM) {
				slamEvents++;
			} else if (event.feedbackType()
					== CombatFeedbackType.BOSS_OVERLOAD) {
				overloadEvents++;
			} else if (event.feedbackType()
					== CombatFeedbackType.WEAKPOINT_KILL) {
				weakpointKillEvents++;
			}
		}

		private int count(CombatFeedbackType type) {
			switch (type) {
				case BOSS_PHASE_BREAK:
					return phaseBreakEvents;
				case BOSS_SLAM:
					return slamEvents;
				case BOSS_OVERLOAD:
					return overloadEvents;
				case WEAKPOINT_KILL:
					return weakpointKillEvents;
				default:
					return 0;
			}
		}
	}

	private BukovWhiteLineProductionHarness() {
	}
}
