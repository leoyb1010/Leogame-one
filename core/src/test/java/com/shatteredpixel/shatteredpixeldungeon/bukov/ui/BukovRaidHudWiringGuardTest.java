package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Prevents the realtime HUD from regressing into simulated or frame-based data. */
public class BukovRaidHudWiringGuardTest {

	@Test
	public void worldCopiesLiveWeaponStatusInteractionAndExtractionState()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");

		assertTrue(world.contains("implements RealtimeRaidSystem.World, FireControl.Sink,"));
		assertTrue(world.contains("RaidObjectiveSource, BukovRaidHudSource"));
		assertTrue(world.contains("void readRaidHudState(BukovRaidHudState target)"));
		assertTrue(world.contains("fireControl.reloadRemaining()"));
		assertTrue(world.contains("medicalStatus.bleedingPerSecond()"));
		assertTrue(world.contains("active.progressFraction()"));
		assertTrue(world.contains(
				"extractionAvailable(nearbyExtraction, elapsed)"));
		assertTrue(world.contains(
				"raid.firstRaidConditionalExtractionUnlocked()"));
		assertTrue(world.contains("selectVisibleLootHeap("));
		assertTrue(world.contains("target.sound(keySoundVisual)"));
		assertTrue(world.contains("combatHudTimeline.copyTo(target)"));
		assertTrue(world.contains("combatHudTimeline.activity()"));
		assertTrue(world.contains("combatHudTimeline.damage("));
		assertTrue(world.contains("target.aim("));
		assertTrue(world.contains("readNavigationHudState(target, elapsed)"));
		assertTrue(world.contains("readThreatHudState(target)"));
		assertTrue(world.contains("BukovRaidHudState.Cue.PICKUP"));
		assertTrue(world.contains("BukovRaidHudState.Cue.MISSION"));
		assertTrue(world.contains("BukovRaidHudState.Cue.EXTRACTION"));
		assertTrue(world.contains("readBossHudState(target)"));
		assertTrue(world.contains("SPDSettings.bukovDamageNumbers()"));
		assertTrue(world.contains("shouldShowDamageNumber("));

		assertTrue(hud.contains("final BukovRaidHudState live"));
		assertTrue(hud.contains("hudSource.readRaidHudState(live)"));
		assertTrue(hud.contains("live.reloadProgress()"));
		assertTrue(hud.contains("live.interactionProgress()"));
		assertTrue(hud.contains("live.extractionProgress()"));
		assertTrue(hud.contains("healthFlashRemaining = 0.07f"));
		assertTrue(hud.contains("Math.ceil(Math.max(1, lastMaxHp) / 10f)"));
		assertTrue(hud.contains("BukovCombatHudFormat.sound(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.hit(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.bossTitle(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.navigation(live)"));
		assertTrue(hud.contains("BukovCombatHudFormat.threat(live)"));
		assertTrue(hud.contains("live.combatAwarenessAlpha()"));
		assertTrue(hud.contains("BukovHitDirectionArc"));
		assertTrue(hud.contains("live.hitCount()"));
		assertTrue(hud.contains("positionReticle(crosshairX, crosshairY)"));
		assertTrue(hud.contains("PointerEvent.currentHoverPos()"));
		assertTrue(hud.contains("camera.screenToCamera("));
		assertTrue(hud.contains("ControllerHandler.controllerActive"));
		assertTrue(hud.contains("camera.width"));
		assertTrue(hud.contains("camera.height"));
		assertTrue(hud.contains("live.bossHealthFraction()"));
		assertFalse(hud.contains("Actor."));
		assertFalse(hud.contains("Random."));
	}

	@Test
	public void combatBarIsShallowAndKeepsWorldSpaceFeedbackOutOfIt()
			throws Exception {
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");
		String layout = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHudLayout.java");
		assertTrue(hud.contains("BukovRaidHudLayout.preferredHeight("));
		assertTrue(layout.contains("private static final float WIDE_HEIGHT = 38f"));
		assertTrue(layout.contains("private static final float COMPACT_HEIGHT = 68f"));
		assertTrue(layout.contains("compactObjective("));
		assertTrue(hud.contains("layoutCombatOverlay(actualHeight)"));
		assertTrue(hud.contains("centerY + aimRadius + 14f"));
		assertFalse(hud.contains(
				"availableWidth >= WIDE_THRESHOLD ? 60f : 86f"));
	}

	@Test
	public void sceneUsesCommonSafeTopAndReservesHudFromTouchNavigation()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		assertTrue(scene.contains(
				"float bukovSafeTop = Math.max("));
		assertTrue(scene.contains(
				"Math.max(0f, insets.top)"));
		assertTrue(scene.contains(
				"bukovTouchControls.hudBottom(bukovHud.bottom() + 2f)"));
		assertTrue(scene.contains(
				"bukovSafeTop + 4f"));
	}

	@Test
	public void hudBlinkAndProgressAreTimeBasedNotFrameBased()
			throws Exception {
		String hud = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui/BukovRaidHud.java");
		assertTrue(hud.contains("float elapsed = Math.max(0f, Game.elapsed)"));
		assertTrue(hud.contains("uiSeconds += elapsed"));
		assertTrue(hud.contains("Math.floor(uiSeconds * 2f)"));
		assertFalse(hud.contains("frameCount"));
		assertFalse(hud.contains("Actor.now"));
	}

	@Test
	public void combatFxUsesRealtimeWorldCoordinatesWithoutDoubleCentering()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String pool = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/BukovCombatFxViewPool.java");
		assertTrue(scene.contains(
				"bukovCombatFxViews.present(event, DungeonTilemap.SIZE)"));
		assertTrue(pool.contains("event.fromX() * tileSize"));
		assertTrue(pool.contains("event.fromY() * tileSize"));
		assertTrue(pool.contains("event.toX() * tileSize"));
		assertTrue(pool.contains("event.toY() * tileSize"));
		assertFalse(scene.contains("event.fromX() * tileSize + center"));
		assertFalse(scene.contains("event.toX() * tileSize + center"));
		assertFalse(pool.contains("event.fromX() * tileSize + center"));
		assertFalse(pool.contains("event.toX() * tileSize + center"));
	}

	@Test
	public void mouseMovementImmediatelySwitchesAimBackFromController()
			throws Exception {
		String inputHandler = source(
				"../SPD-classes/src/main/java/com/watabou/input/InputHandler.java");
		String mouseMoved = inputHandler.substring(
				inputHandler.indexOf("boolean mouseMoved("),
				inputHandler.indexOf("// *****************", inputHandler.indexOf(
						"boolean mouseMoved(")));
		assertTrue(mouseMoved.contains(
				"ControllerHandler.controllerActive = false"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
