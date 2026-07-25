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
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRuntimeLoadoutAdapter;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidBalanceTelemetry;
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
				boolean targetStillInLevel) {
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

	static Result killOneGeneratedEnemy(
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

		MouseState mouse = new MouseState();
		BukovRealtimeWorld world = null;
		RealtimeRaidSystem system = null;
		BukovRuntimeLoadoutAdapter.RuntimeLoadout runtime = null;
		try {
			Gdx.files = headlessFiles(locateAssets());
			Gdx.input = headlessInput(mouse);
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

			for (int frame = 0;
					frame < PLAYER_KILL_TIMEOUT_FRAMES
							&& target.isAlive();
					frame++) {
				// The starter sidearm is semi-automatic, so drive real trigger
				// release/press edges instead of holding an automatic-fire input.
				mouse.fire = frame % 2 == 0;
				aimAt(target);
				system.update(RENDER_STEP);
				system.drainCombatFx(fx::accept);
			}
			mouse.fire = false;
			for (int frame = 0; frame < REMOVAL_REFRESH_FRAMES; frame++) {
				aimAt(target);
				system.update(RENDER_STEP);
				system.drainCombatFx(fx::accept);
			}

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
					level.mobs.contains(target));
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

	private static void aimAt(BukovHostMob target) {
		float worldX = target.realtimeBody.x * DungeonTilemap.SIZE;
		float worldY = target.realtimeBody.y * DungeonTilemap.SIZE;
		Point screen = Camera.main.cameraToScreen(worldX, worldY);
		PointerEvent.setHoverPos(new PointF(screen.x, screen.y));
	}

	private static Files headlessFiles(Path assets) {
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

	private static Input headlessInput(MouseState mouse) {
		InvocationHandler handler = (proxy, method, arguments) -> {
			String name = method.getName();
			if ("isButtonPressed".equals(name)
					|| "isButtonJustPressed".equals(name)) {
				return mouse.fire
						&& ((Integer)arguments[0]) == Input.Buttons.LEFT;
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

	private static Path locateAssets() {
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

	private static final class MouseState {
		private boolean fire;
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

	private static final class HeadlessCharSprite extends CharSprite {
		private HeadlessCharSprite() {
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
