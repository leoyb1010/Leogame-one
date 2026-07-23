package com.shatteredpixel.shatteredpixeldungeon.bukov.save;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Guards the production path against APIs absent from RoboVM 2.3.24's runtime.
 */
public class BukovIosAotCompatibilityTest {

	@Test
	public void productionPathAvoidsKnownRoboVmPhantomApis() throws IOException {
		List<File> files = new ArrayList<>();
		collectJavaFiles(new File(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov"),
				files);
		files.add(new File(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java"));

		List<String> violations = new ArrayList<>();
		for (File file : files) {
			String source = read(file);
			reject(source, file, "java.util.Optional", violations);
			reject(source, file, "java.util.function", violations);
			reject(source, file, "java.nio.file", violations);
			reject(source, file, ".toPath(", violations);
			reject(source, file, "Comparator.comparing", violations);
			reject(source, file, ".removeIf(", violations);
			reject(source, file, "Float.isFinite(", violations);
			reject(source, file, "Double.isFinite(", violations);
			reject(source, file, "Math.addExact(", violations);
			reject(source, file, "Math.subtractExact(", violations);
			reject(source, file, "Math.multiplyExact(", violations);
			reject(source, file, "Math.toIntExact(", violations);
			reject(source, file, "Math.floorDiv(", violations);
			reject(source, file, "Math.floorMod(", violations);
			reject(source, file, "String.join(", violations);
			reject(source, file, "Long.remainderUnsigned(", violations);
			reject(source, file, "Long.toUnsignedString(", violations);
			reject(source, file, "Integer.toUnsignedLong(", violations);
		}
		assertTrue(
				"RoboVM-incompatible production references: " + violations,
				violations.isEmpty());
	}

	private static void collectJavaFiles(File directory, List<File> result) {
		File[] files = directory.listFiles();
		if (files == null) {
			throw new IllegalStateException("Missing source directory: " + directory);
		}
		for (File file : files) {
			if (file.isDirectory()) {
				collectJavaFiles(file, result);
			} else if (file.getName().endsWith(".java")) {
				result.add(file);
			}
		}
	}

	private static String read(File file) throws IOException {
		try (FileInputStream input = new FileInputStream(file);
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[4096];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toString("UTF-8");
		}
	}

	private static void reject(
			String source,
			File file,
			String needle,
			List<String> violations) {
		if (source.contains(needle)) {
			violations.add(file.getPath() + " -> " + needle);
		}
	}
}
