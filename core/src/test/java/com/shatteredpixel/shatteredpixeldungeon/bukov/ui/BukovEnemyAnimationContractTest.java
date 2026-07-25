package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Asset-to-runtime closure for the first-raid enemy action set.
 *
 * The deterministic shell gate does the same checks before packaging. Keeping
 * this contract in core tests prevents a future sprite refactor from silently
 * returning to generic hit flashes or unreachable "special" frames.
 */
public class BukovEnemyAnimationContractTest {

	private static final int FRAME_WIDTH = 16;
	private static final int FRAME_HEIGHT = 18;
	private static final int FRAME_COUNT = 21;

	private static final String[] SHEETS = {
			"scavenger.png",
			"gunner.png",
			"armored.png",
			"captain.png",
			"drone.png",
			"white_line.png",
			"alley_scout.png",
			"depot_shotgunner.png",
			"line_rifleman.png",
			"fog_stalker.png",
			"signal_operator.png",
			"iron_clasp_marksman.png",
			"breach_veteran.png"
	};

	@Test
	public void manifestDeclaresCompleteActionAndSpecializationContract()
			throws Exception {
		String manifest = source(
				"src/main/assets/sprites/bukov/enemy_animation_manifest.json");
		assertTrue(manifest.contains("\"schemaVersion\": 2"));
		assertTrue(manifest.contains("\"frameCount\": 21"));
		assertTrue(manifest.contains("\"idle\": ["));
		assertTrue(manifest.contains("\"attack\": ["));
		assertTrue(manifest.contains("\"walk\": ["));
		assertTrue(manifest.contains("\"death\": ["));
		assertTrue(manifest.contains("\"hit\": ["));
		assertTrue(manifest.contains("\"special\": ["));
		assertTrue(manifest.contains("\"bossEncounterPhase\": ["));
		assertEquals(8, occurrences(manifest,
				"\"specialAction\": \"reload\""));
		assertEquals(2, occurrences(manifest,
				"\"specialAction\": \"rush\""));
		assertEquals(2, occurrences(manifest,
				"\"specialAction\": \"scan\""));
		assertEquals(1, occurrences(manifest,
				"\"specialAction\": \"phase_cast\""));
	}

	@Test
	public void everySheetOwnsDistinctHitAndSpecialFramesWithStableFeet()
			throws Exception {
		String manifest = source(
				"src/main/assets/sprites/bukov/enemy_animation_manifest.json");
		for (String sheetName : SHEETS) {
			Path path = Paths.get(
					"src/main/assets/sprites/bukov", sheetName);
			BufferedImage sheet = ImageIO.read(path.toFile());
			assertEquals(sheetName,
					FRAME_WIDTH * FRAME_COUNT, sheet.getWidth());
			assertEquals(sheetName, FRAME_HEIGHT, sheet.getHeight());

			byte[][] hashes = new byte[FRAME_COUNT][];
			Set<Integer> bottomRows = new HashSet<>();
			for (int frame = 0; frame < FRAME_COUNT; frame++) {
				hashes[frame] = hashFrame(sheet, frame);
				if (frame < 8 || frame >= 11) {
					bottomRows.add(bottomOpaqueRow(sheet, frame));
				}
			}
			assertEquals(sheetName + " foot anchor drift", 1,
					bottomRows.size());
			assertNotEquals(sheetName + " hit frames duplicated",
					Arrays.toString(hashes[11]),
					Arrays.toString(hashes[12]));
			Set<String> specialHashes = new HashSet<>();
			for (int frame = 13; frame <= 15; frame++) {
				String hash = Arrays.toString(hashes[frame]);
				assertTrue(sheetName + " special frame duplicated",
						specialHashes.add(hash));
				for (int base = 0; base < 8; base++) {
					assertNotEquals(sheetName
									+ " special frame reuses base action",
							Arrays.toString(hashes[base]), hash);
				}
			}

			String fileHash = hex(MessageDigest.getInstance("SHA-256")
					.digest(Files.readAllBytes(path)));
			assertTrue(sheetName + " hash missing from manifest",
					manifest.contains("\"sha256\": \"" + fileHash + "\""));
		}
	}

	@Test
	public void authoredFilmsAreConnectedToRealtimeStateTransitions()
			throws Exception {
		String sprite = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovEnemySprite.java");
		String boss = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovWhiteLineSprite.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(sprite.contains("hit.frames(frames, 11, 12)"));
		assertTrue(sprite.contains(
				"special.frames(frames, 13, 14, 15)"));
		assertTrue(sprite.contains(
				"public void realtimeHitReaction()"));
		assertTrue(sprite.contains(
				"playRealtimeAction(hit, ch == null ? 0 : ch.pos, 3, null)"));
		assertTrue(sprite.contains(
				"realtimeRush(int targetCell)"));
		assertTrue(sprite.contains(
				"realtimeReload(int targetCell)"));
		assertTrue(sprite.contains(
				"realtimeScan(int targetCell)"));
		assertTrue(sprite.contains(
				"realtimePhaseCast(int targetCell)"));

		assertTrue(world.contains("playEnemyRush(enemy);"));
		assertTrue(world.contains("playEnemyReload(enemy);"));
		assertTrue(world.contains("playEnemyScan(source);"));
		assertTrue(world.contains("realtimePhaseCast(hero.pos)"));
		assertTrue(boss.contains(
				"shieldPhase.frames(frames, 16, 17)"));
		assertTrue(boss.contains(
				"vulnerablePhase.frames(frames, 20, 20, 0)"));
		assertTrue(boss.contains(
				"phaseTransition.frames(frames, 16, 17, 18, 19, 20, 0)"));
		assertTrue(boss.contains(
				"playRealtimeAction(phaseTransition, targetCell, 2, null)"));
		assertTrue(boss.contains(
				"WEAK_POINT_SLOW_MOTION_SECONDS = 0.2f"));
		assertTrue(boss.contains(
				"WEAK_POINT_SLOW_MOTION_SCALE = 0.3f"));
	}

	@Test
	public void firstRaidActionGroupsRemainReachableInProduction()
			throws Exception {
		String sprite = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovEnemySprite.java");
		String host = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ai/BukovHostMob.java");
		String director = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/ai/FirstRaidEnemySpawnDirector.java");
		String presentation = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/fx/BukovCombatPresentation.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "bukov/runtime/BukovRealtimeWorld.java");

		assertTrue(sprite.contains("idle.frames(frames, 0, 0, 1, 0)"));
		assertTrue(sprite.contains("run.frames(frames, 4, 5, 6, 7)"));
		assertTrue(sprite.contains("attack.frames(frames, 2, 3, 0)"));
		assertTrue(sprite.contains("die.frames(frames, 8, 9, 10)"));
		assertTrue(sprite.contains("hit.frames(frames, 11, 12)"));
		assertTrue(sprite.contains("special.frames(frames, 13, 14, 15)"));

		assertFirstRaidMapping(
				director, host,
				"FIRST_GUNNER", "scavenger_gunner",
				"BukovGunnerSprite.class");
		assertFirstRaidMapping(
				director, host,
				"FIRST_RUSHER", "melee_rusher",
				"BukovScavengerSprite.class");
		assertFirstRaidMapping(
				director, host,
				"FIRST_GUARD", "iron_clasp_guard",
				"BukovArmoredSprite.class");
		assertFirstRaidMapping(
				director, host,
				"FIRST_ALARM", "sensor_doll",
				"BukovDroneSprite.class");
		assertFirstRaidMapping(
				director, host,
				"FIRST_BOSS", "boss_white_line",
				"BukovWhiteLineSprite.class");

		assertTrue(source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovGunnerSprite.java")
				.contains("SpecialAction.RELOAD"));
		assertTrue(source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovScavengerSprite.java")
				.contains("SpecialAction.RUSH"));
		assertTrue(source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovArmoredSprite.java")
				.contains("SpecialAction.RELOAD"));
		assertTrue(source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovDroneSprite.java")
				.contains("SpecialAction.SCAN"));
		assertTrue(source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
						+ "sprites/bukov/BukovWhiteLineSprite.java")
				.contains("SpecialAction.PHASE_CAST"));

		assertTrue(world.contains(
				"enemy.mob.sprite.setRealtimeMoving(enemy.moving)"));
		assertTrue(presentation.contains(
				"source.sprite.realtimeAttack(event.targetCell())"));
		assertTrue(presentation.contains(
				"target.sprite.realtimeHitReaction()"));
		assertTrue(presentation.contains("case ENEMY_DEATH:"));
		assertTrue(presentation.contains("playDeath(target);"));
		assertTrue(world.contains("playEnemyRush(enemy);"));
		assertTrue(world.contains("playEnemyReload(enemy);"));
		assertTrue(world.contains("playEnemyScan(source);"));
		assertTrue(world.contains("realtimePhaseCast(hero.pos)"));
	}

	private static void assertFirstRaidMapping(
			String director,
			String host,
			String constant,
			String enemyId,
			String spriteClass) {
		assertTrue(director.contains(
				"public static final String " + constant
						+ " = \"" + enemyId + "\""));
		int caseOffset = host.indexOf("case \"" + enemyId + "\":");
		assertTrue(enemyId + " is missing from BukovHostMob",
				caseOffset >= 0);
		int returnOffset = host.indexOf("return;", caseOffset);
		assertTrue(enemyId + " mapping has no terminal return",
				returnOffset > caseOffset);
		assertTrue(enemyId + " uses the wrong sprite",
				host.substring(caseOffset, returnOffset)
						.contains("spriteClass = " + spriteClass));
	}

	private static byte[] hashFrame(
			BufferedImage sheet, int frame) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		for (int y = 0; y < FRAME_HEIGHT; y++) {
			for (int x = 0; x < FRAME_WIDTH; x++) {
				int pixel = sheet.getRGB(frame * FRAME_WIDTH + x, y);
				digest.update((byte)(pixel >>> 24));
				digest.update((byte)(pixel >>> 16));
				digest.update((byte)(pixel >>> 8));
				digest.update((byte)pixel);
			}
		}
		return digest.digest();
	}

	private static int bottomOpaqueRow(
			BufferedImage sheet, int frame) {
		int bottom = -1;
		for (int y = 0; y < FRAME_HEIGHT; y++) {
			for (int x = 0; x < FRAME_WIDTH; x++) {
				if ((sheet.getRGB(
						frame * FRAME_WIDTH + x, y) >>> 24) != 0) {
					bottom = y;
				}
			}
		}
		return bottom;
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}

	private static int occurrences(String source, String token) {
		int count = 0;
		int offset = 0;
		while ((offset = source.indexOf(token, offset)) >= 0) {
			count++;
			offset += token.length();
		}
		return count;
	}

	private static String hex(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			result.append(String.format("%02x", value & 0xFF));
		}
		return result.toString();
	}
}
