package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the visual assets from becoming unused files after future host-engine
 * merges. Runtime rendering itself is covered by the Apple simulator pass.
 */
public class BukovRuntimeVisualWiringTest {

	@Test
	public void operatorUsesEightDirectionFilmAndAllThirtyTwoFrames()
			throws Exception {
		String hero = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroSprite.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String facing = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovFacing8.java");
		String pose = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovOperatorPose.java");
		String manifest = source(
				"src/main/assets/sprites/bukov_operator_manifest.json");

		assertTrue(hero.contains(
				"TextureCache.get(Assets.Sprites.BUKOV_OPERATOR)"));
		assertTrue(hero.contains("bukovDirection = BukovFacing8.S.row"));
		assertTrue(hero.contains("new BukovOperatorPose()"));
		assertTrue(hero.contains("Assets.Sprites.BUKOV_OPERATOR_LOWER"));
		assertTrue(hero.contains("Assets.Sprites.BUKOV_OPERATOR_UPPER"));
		assertTrue(hero.contains("setBukovLocomotionDirection("));
		assertTrue(hero.contains("bukovPose.upperBodyFacing().row"));
		assertTrue(hero.contains("bukovLowerLayer.draw()"));
		assertTrue(hero.contains("bukovUpperLayer.draw()"));
		assertTrue(hero.contains("if (curAnim != die)"));
		assertTrue(hero.contains("idle.frames( film, 0, 1, 0, 1 )"));
		assertTrue(hero.contains(
				"run.frames( film, 2, 3, 4, 5, 6, 7, 4, 3 )"));
		assertTrue(hero.contains("aim.frames( film, 8 )"));
		assertTrue(hero.contains("fire.frames( film, 10, 11, 12, 8 )"));
		assertTrue(hero.contains(
				"reload.frames( film, 13, 14, 15, 16, 15, 8 )"));
		assertTrue(hero.contains("hit.frames( film, 17, 18 )"));
		assertTrue(hero.contains(
				"medical.frames( film, 20, 21, 22, 23, 8 )"));
		assertTrue(hero.contains(
				"die.frames( film, 24, 25, 26, 27, 27, 27 )"));
		assertTrue(hero.contains(
				"extract.frames( film, 28, 29, 30, 31, 28 )"));
		assertTrue(hero.contains("void firearmFire("));
		assertTrue(hero.contains("void reloadFirearm("));
		assertTrue(hero.contains("void hitReaction("));
		assertTrue(hero.contains("void medicalUse("));
		assertTrue(hero.contains("void extractionRadio("));
		assertTrue(hero.contains("setBukovRealtimeOrientation("));
		assertTrue(pose.contains("locomotionFacing = BukovFacing8.resolve("));
		assertTrue(pose.contains("upperBodyFacing = BukovFacing8.resolve("));
		assertTrue(world.contains("setBukovRealtimeOrientation("));
		assertTrue(facing.contains("N(0)"));
		assertTrue(facing.contains("NW(7)"));

		assertTrue(manifest.contains("\"width\": 384"));
		assertTrue(manifest.contains("\"height\": 128"));
		assertTrue(manifest.contains("\"frameCount\": 32"));
		assertTrue(manifest.contains("\"schemaVersion\": 2"));
		assertTrue(manifest.contains("\"lowerBody\""));
		assertTrue(manifest.contains("\"upperBodyWeapon\""));
		assertTrue(manifest.contains("\"directions\""));
		assertTrue(manifest.contains("\"footAnchor\""));
		assertTrue(manifest.contains("\"muzzleAnchor\""));
		assertTrue(manifest.contains("\"name\": \"medical\""));
		assertTrue(manifest.contains("\"name\": \"extract\""));

		// Classic classes still select their own sheet and tier film.
		assertTrue(hero.contains("Dungeon.hero.heroClass.spritesheet()"));
		assertTrue(hero.contains(
				"TextureFilm film = new TextureFilm(\n"
						+ "\t\t\t\ttiers(),\n"
						+ "\t\t\t\tDungeon.hero.tier()"));
		assertFalse(hero.contains("bukovOperator ? bukovTiers() : tiers()"));
	}

	@Test
	public void firstRaidLandmarksAreAddedOnlyInsideBukovSceneBranch()
			throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String landmarks = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov/BukovFirstRaidLandmarks.java");

		assertTrue(scene.contains(
				"if (BukovMode.active() && Dungeon.level instanceof BukovLevel)"));
			assertTrue(scene.contains(
					"new BukovFirstRaidLandmarks(bukovLevel)"));

		assertTrue(landmarks.contains("FRAME_GATE_LEFT = 1"));
		assertTrue(landmarks.contains("FRAME_GATE_MIDDLE = 2"));
		assertTrue(landmarks.contains("FRAME_GATE_RIGHT = 3"));
		assertTrue(landmarks.contains("Dungeon.level.map[cell] != Terrain.OPEN_DOOR"));
		assertFalse(landmarks.contains("raidLayout().extraction("));
		assertFalse(landmarks.contains("lootAnchors()"));
		assertFalse(landmarks.contains("FRAME_ARCHIVE_CABINET, gate.archiveCell"));
		assertFalse(landmarks.contains("semanticCell("));

		// Static landmarks are owned by the semantic tile layer. This runtime
		// overlay only hides the three gate panels after the task unlocks them.
		assertFalse(landmarks.contains("Level.set("));
		assertFalse(landmarks.contains("map[cell] ="));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
