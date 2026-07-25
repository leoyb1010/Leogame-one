package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * Makes every collision query declare whether it is body/path topology or a
 * line trace. A new caller must be classified instead of silently inheriting
 * the ordinary-door movement exception.
 */
public class CollisionMapSemanticWiringTest {

	private static final Path BUKOV_SOURCE = Paths.get(
			"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov");

	@Test
	public void everyCollisionQueryHasAnAuditedSemantic() throws Exception {
		Map<String, Integer> movement = occurrences(".blocked(");
		Map<String, Integer> traces = occurrences(".blocksLine(");

		assertEquals(expected(
				"runtime/RealtimeEnemyNavigator.java", 2,
				"runtime/RealtimeHeroBodyRecovery.java", 2,
				// Loose-loot placement plus onboarding spawn placement.
				"runtime/BukovRealtimeWorld.java", 2,
				"runtime/GridCollision.java", 1), movement);
		assertEquals(expected(
				"combat/RealtimeProjectile.java", 1,
				"combat/HitscanResolver.java", 3,
				"audio/GunshotAcousticSpaceResolver.java", 1,
				"ai/GridLineOfSight.java", 1,
				// Sound occlusion, diagonal target, both seams, and the
				// onboarding spawn's line-of-fire check.
				"runtime/BukovRealtimeWorld.java", 5), traces);
	}

	private static Map<String, Integer> occurrences(String marker)
			throws IOException {
		Map<String, Integer> result = new LinkedHashMap<>();
		try (Stream<Path> files = Files.walk(BUKOV_SOURCE)) {
			files.filter(path -> path.toString().endsWith(".java"))
					.sorted()
					.forEach(path -> {
						try {
							String source = new String(
									Files.readAllBytes(path),
									StandardCharsets.UTF_8);
							int count = count(source, marker);
							if (count > 0) {
								result.put(
										BUKOV_SOURCE.relativize(path)
												.toString()
												.replace('\\', '/'),
										count);
							}
						} catch (IOException error) {
							throw new UncheckedIOException(error);
						}
					});
		} catch (UncheckedIOException error) {
			throw error.getCause();
		}
		return result;
	}

	private static int count(String source, String marker) {
		int result = 0;
		int offset = 0;
		while ((offset = source.indexOf(marker, offset)) >= 0) {
			result++;
			offset += marker.length();
		}
		return result;
	}

	private static Map<String, Integer> expected(Object... entries) {
		Map<String, Integer> result = new LinkedHashMap<>();
		for (int index = 0; index < entries.length; index += 2) {
			result.put((String)entries[index], (Integer)entries[index + 1]);
		}
		return result;
	}

}
