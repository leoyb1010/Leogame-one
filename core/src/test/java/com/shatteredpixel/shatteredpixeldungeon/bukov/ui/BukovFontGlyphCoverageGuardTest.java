package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.SoundCategory;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards every authored Bukov text surface against the actual bundled font
 * cmap tables. Non-Asian runs can use pixel_font or droid_sans depending on
 * the system-font setting, while Asian runs always use droid_sans.
 */
public class BukovFontGlyphCoverageGuardTest {

	@Test
	public void authoredPlayerTextIsCoveredByBundledFonts()
			throws Exception {
		Path assets = coreAssets();
		FontCoverage fonts = bundledFonts(assets);

		for (Path directory : new Path[] {
				assets.resolve("messages/bukov_economy"),
				assets.resolve("messages/bukov_entry"),
				assets.resolve("messages/bukov_raid")
		}) {
			try (Stream<Path> paths = Files.walk(directory)) {
				for (Path path : (Iterable<Path>)paths
						.filter(value -> value.toString()
								.endsWith(".properties"))::iterator) {
					Properties properties = new Properties();
					try (BufferedReader reader = Files.newBufferedReader(
							path, StandardCharsets.UTF_8)) {
						properties.load(reader);
					}
					for (String key : properties.stringPropertyNames()) {
						fonts.assertRuntimeText(
								path + ":" + key,
								properties.getProperty(key));
					}
				}
			}
		}

		Path content = assets.resolve("bukov/content");
		try (Stream<Path> paths = Files.walk(content)) {
			for (Path path : (Iterable<Path>)paths
					.filter(Files::isRegularFile)::iterator) {
				fonts.assertRuntimeText(
						path.toString(),
						new String(
								Files.readAllBytes(path),
								StandardCharsets.UTF_8));
			}
		}

		assertBukovJavaLiterals(fonts, coreSource());
	}

	@Test
	public void combatHudProductionFormatsStayWithinBundledFonts()
			throws Exception {
		FontCoverage fonts = bundledFonts(coreAssets());
		BukovRaidHudState state = new BukovRaidHudState();
		state.beginFrame("Objective", 12f);
		state.presentationSettings(true, 2);

		for (BukovRaidHudState.Direction direction
				: BukovRaidHudState.Direction.values()) {
			for (BukovRaidHudState.Distance distance
					: BukovRaidHudState.Distance.values()) {
				state.sound(
						SoundCategory.ENEMY_GUNSHOT,
						direction,
						distance,
						1f,
						1f);
				fonts.assertRuntimeText(
						"combat sound " + direction + "/" + distance,
						BukovCombatHudFormat.sound(state));
			}
			state.hit(direction, 1f, 1f);
			fonts.assertRuntimeText(
					"combat hit " + direction,
					BukovCombatHudFormat.hit(state));
			state.threat(1f, 0f, 2f, "Threat", false);
			fonts.assertRuntimeText(
					"combat threat " + direction,
					BukovCombatHudFormat.threat(state));
		}

		for (BukovRaidHudState.Cue cue
				: BukovRaidHudState.Cue.values()) {
			state.navigation(cue, 1f, 0f, 2f, "Target", true);
			fonts.assertRuntimeText(
					"combat navigation " + cue,
					BukovCombatHudFormat.navigation(state));
		}

		state.boss(
				"White Line",
				2,
				3,
				"Overload",
				80,
				100,
				true,
				"Break contact",
				true);
		fonts.assertRuntimeText(
				"boss title",
				BukovCombatHudFormat.bossTitle(state));
		fonts.assertRuntimeText(
				"boss objective",
				BukovCombatHudFormat.bossObjective(state));
	}

	private static void assertBukovJavaLiterals(
			FontCoverage fonts, Path sourceRoot) throws Exception {
		try (Stream<Path> paths = Files.walk(sourceRoot)) {
			for (Path path : (Iterable<Path>)paths
					.filter(value -> value.toString().endsWith(".java"))
					::iterator) {
				String source = new String(
						Files.readAllBytes(path),
						StandardCharsets.UTF_8);
				if (!source.contains("Bukov") && !source.contains("bukov")) {
					continue;
				}
				assertJavaLiteralValues(fonts, path, source);
			}
		}
	}

	private static void assertJavaLiteralValues(
			FontCoverage fonts, Path path, String source) {
		boolean lineComment = false;
		boolean blockComment = false;
		for (int index = 0; index < source.length(); index++) {
			char current = source.charAt(index);
			char next = index + 1 < source.length()
					? source.charAt(index + 1)
					: '\0';
			if (lineComment) {
				if (current == '\n') lineComment = false;
				continue;
			}
			if (blockComment) {
				if (current == '*' && next == '/') {
					blockComment = false;
					index++;
				}
				continue;
			}
			if (current == '/' && next == '/') {
				lineComment = true;
				index++;
				continue;
			}
			if (current == '/' && next == '*') {
				blockComment = true;
				index++;
				continue;
			}
			if (current != '"') continue;

			int start = index;
			StringBuilder literal = new StringBuilder();
			while (++index < source.length()) {
				current = source.charAt(index);
				if (current == '\\' && index + 1 < source.length()) {
					literal.append(current);
					literal.append(source.charAt(++index));
				} else if (current == '"') {
					break;
				} else {
					literal.append(current);
				}
			}
			fonts.assertRuntimeText(
					path + "@" + start,
					decodeJavaLiteral(literal.toString()));
		}
	}

	private static String decodeJavaLiteral(String value) {
		StringBuilder result = new StringBuilder();
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (current != '\\' || index + 1 >= value.length()) {
				result.append(current);
				continue;
			}
			char escaped = value.charAt(++index);
			if (escaped == 'u' && index + 4 < value.length()) {
				String digits = value.substring(index + 1, index + 5);
				try {
					result.append((char)Integer.parseInt(digits, 16));
					index += 4;
				} catch (NumberFormatException ignored) {
					result.append('\\').append(escaped);
				}
			} else if (escaped == 'n') {
				result.append('\n');
			} else if (escaped == 'r') {
				result.append('\r');
			} else if (escaped == 't') {
				result.append('\t');
			} else {
				result.append(escaped);
			}
		}
		return result.toString();
	}

	private static FontCoverage bundledFonts(Path assets) throws Exception {
		Path absoluteAssets = assets.toAbsolutePath().normalize();
		Path repository = absoluteAssets.getParent().getParent().getParent()
				.getParent();
		Path pixel = absoluteAssets.resolve("fonts/pixel_font.ttf");
		Path droid = repository.resolve(
				"desktop/src/main/assets/fonts/droid_sans.ttf");
		assertTrue("Missing bundled pixel font: " + pixel, Files.isRegularFile(pixel));
		assertTrue("Missing bundled Droid Sans font: " + droid,
				Files.isRegularFile(droid));
		return new FontCoverage(
				TrueTypeCmap.read(pixel),
				TrueTypeCmap.read(droid));
	}

	private static Path coreAssets() {
		Path module = Paths.get("src/main/assets");
		if (Files.isDirectory(module)) return module;
		return Paths.get("core/src/main/assets");
	}

	private static Path coreSource() {
		Path module = Paths.get(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon");
		if (Files.isDirectory(module)) return module;
		return Paths.get(
				"core/src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon");
	}

	private static final class FontCoverage {
		private final Set<Integer> pixel;
		private final Set<Integer> droid;

		private FontCoverage(Set<Integer> pixel, Set<Integer> droid) {
			this.pixel = pixel;
			this.droid = droid;
		}

		private void assertRuntimeText(String label, String text) {
			List<String> missing = new ArrayList<>();
			for (int offset = 0; offset < text.length();) {
				int codePoint = text.codePointAt(offset);
				offset += Character.charCount(codePoint);
				if (codePoint == '\n' || codePoint == '\r'
						|| codePoint == '\t') {
					continue;
				}
				// Java-side sort sentinels are not displayable text.
				if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF
						|| (codePoint & 0xFFFF) == 0xFFFE
						|| (codePoint & 0xFFFF) == 0xFFFF) {
					continue;
				}
				if (codePoint > Character.MAX_VALUE) {
					missing.add(codePoint(codePoint) + " astral");
				} else if (usesAsianGenerator(codePoint)) {
					if (!droid.contains(codePoint)) {
						missing.add(codePoint(codePoint) + " droid");
					}
				} else {
					if (!pixel.contains(codePoint)) {
						missing.add(codePoint(codePoint) + " pixel");
					}
					if (!droid.contains(codePoint)) {
						missing.add(codePoint(codePoint) + " droid");
					}
				}
			}
			assertTrue(label + " has unsupported glyphs " + missing,
					missing.isEmpty());
		}

		private static boolean usesAsianGenerator(int codePoint) {
			Character.UnicodeBlock block =
					Character.UnicodeBlock.of(codePoint);
			return block == Character.UnicodeBlock.HANGUL_SYLLABLES
					|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
					|| block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
					|| block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
					|| block == Character.UnicodeBlock.HIRAGANA
					|| block == Character.UnicodeBlock.KATAKANA;
		}

		private static String codePoint(int value) {
			return String.format(
					"U+%04X(%s)",
					value,
					new String(Character.toChars(value)));
		}
	}

	private static final class TrueTypeCmap {

		private static Set<Integer> read(Path path) throws IOException {
			byte[] bytes = Files.readAllBytes(path);
			ByteBuffer data = ByteBuffer.wrap(bytes)
					.order(ByteOrder.BIG_ENDIAN);
			int tableCount = unsignedShort(data, 4);
			int cmapOffset = -1;
			for (int table = 0; table < tableCount; table++) {
				int record = 12 + table * 16;
				if (record + 16 > bytes.length) break;
				String tag = new String(
						bytes,
						record,
						4,
						StandardCharsets.US_ASCII);
				if ("cmap".equals(tag)) {
					cmapOffset = checkedOffset(
							unsignedInt(data, record + 8),
							bytes.length);
					break;
				}
			}
			assertTrue("No cmap table in " + path, cmapOffset >= 0);

			int encodingCount = unsignedShort(data, cmapOffset + 2);
			Set<Integer> result = new HashSet<>();
			for (int encoding = 0; encoding < encodingCount; encoding++) {
				int record = cmapOffset + 4 + encoding * 8;
				int subtable = cmapOffset + checkedOffset(
						unsignedInt(data, record + 4),
						bytes.length - cmapOffset);
				int format = unsignedShort(data, subtable);
				if (format == 4) {
					readFormat4(data, subtable, result);
				} else if (format == 12) {
					readFormat12(data, subtable, result);
				}
			}
			assertFalse("No Unicode cmap entries in " + path,
					result.isEmpty());
			return result;
		}

		private static void readFormat4(
				ByteBuffer data,
				int base,
				Set<Integer> result) {
			int length = unsignedShort(data, base + 2);
			int limit = base + length;
			int segmentCount = unsignedShort(data, base + 6) / 2;
			int endCodes = base + 14;
			int startCodes = endCodes + segmentCount * 2 + 2;
			int deltas = startCodes + segmentCount * 2;
			int rangeOffsets = deltas + segmentCount * 2;
			for (int segment = 0; segment < segmentCount; segment++) {
				int start = unsignedShort(data, startCodes + segment * 2);
				int end = unsignedShort(data, endCodes + segment * 2);
				int delta = unsignedShort(data, deltas + segment * 2);
				int rangeAddress = rangeOffsets + segment * 2;
				int range = unsignedShort(data, rangeAddress);
				for (int codePoint = start;
						codePoint <= end && codePoint != 0xFFFF;
						codePoint++) {
					int glyph;
					if (range == 0) {
						glyph = (codePoint + delta) & 0xFFFF;
					} else {
						int glyphAddress = rangeAddress + range
								+ (codePoint - start) * 2;
						if (glyphAddress + 2 > limit) continue;
						glyph = unsignedShort(data, glyphAddress);
						if (glyph != 0) {
							glyph = (glyph + delta) & 0xFFFF;
						}
					}
					if (glyph != 0) result.add(codePoint);
				}
			}
		}

		private static void readFormat12(
				ByteBuffer data,
				int base,
				Set<Integer> result) {
			int groups = checkedOffset(
					unsignedInt(data, base + 12),
					Integer.MAX_VALUE);
			for (int group = 0; group < groups; group++) {
				int record = base + 16 + group * 12;
				int start = checkedOffset(
						unsignedInt(data, record),
						Character.MAX_CODE_POINT);
				int end = checkedOffset(
						unsignedInt(data, record + 4),
						Character.MAX_CODE_POINT);
				long firstGlyph = unsignedInt(data, record + 8);
				for (int codePoint = start; codePoint <= end; codePoint++) {
					if (firstGlyph + codePoint - start != 0) {
						result.add(codePoint);
					}
				}
			}
		}

		private static int unsignedShort(ByteBuffer data, int offset) {
			return data.getShort(offset) & 0xFFFF;
		}

		private static long unsignedInt(ByteBuffer data, int offset) {
			return data.getInt(offset) & 0xFFFFFFFFL;
		}

		private static int checkedOffset(long value, int maximum) {
			assertTrue("Invalid TTF offset " + value,
					value >= 0 && value <= maximum);
			return (int)value;
		}

		private TrueTypeCmap() {
		}
	}
}
