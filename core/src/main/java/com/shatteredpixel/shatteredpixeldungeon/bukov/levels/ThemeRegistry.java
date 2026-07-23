package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads and validates the six data-driven abstract raid themes. */
public final class ThemeRegistry {

	public static final String DEFAULT_PATH = "bukov/content/themes.json";
	public static final int REQUIRED_THEME_COUNT = 6;

	private final Map<String, ThemeDefinition> definitions =
			new LinkedHashMap<>();

	public void loadDefault() {
		load(Gdx.files == null
				? new FileHandle("src/main/assets/" + DEFAULT_PATH)
				: Gdx.files.internal(DEFAULT_PATH));
	}

	public void load(FileHandle file) {
		if (file == null) throw new IllegalArgumentException("file is required");
		loadJson(file.readString("UTF-8"));
	}

	public void loadJson(String json) {
		if (json == null) throw new IllegalArgumentException("json is required");
		JsonValue root = new JsonReader().parse(json);
		int schema = root.getInt("schemaVersion", -1);
		if (schema != 1) {
			throw new IllegalArgumentException(
					"Unsupported theme schema: " + schema);
		}
		JsonValue themes = root.get("themes");
		if (themes == null || !themes.isArray()) {
			throw new IllegalArgumentException("themes array is required");
		}

		Map<String, ThemeDefinition> parsed = new LinkedHashMap<>();
		for (JsonValue node = themes.child; node != null; node = node.next) {
			ThemeDefinition definition = parse(node);
			if (parsed.put(definition.id, definition) != null) {
				throw new IllegalArgumentException(
						"Duplicate theme id: " + definition.id);
			}
		}
		if (parsed.size() != REQUIRED_THEME_COUNT) {
			throw new IllegalStateException(
					"Raid content requires exactly six themes");
		}
		definitions.clear();
		definitions.putAll(parsed);
	}

	public ThemeDefinition require(String id) {
		ThemeDefinition value = definitions.get(id);
		if (value == null) {
			throw new IllegalArgumentException("Unknown raid theme: " + id);
		}
		return value;
	}

	public ThemeDefinition forSeed(long seed) {
		if (definitions.isEmpty()) {
			throw new IllegalStateException("Theme registry is not loaded");
		}
		long mixed = seed;
		mixed ^= mixed >>> 33;
		mixed *= 0xff51afd7ed558ccdl;
		mixed ^= mixed >>> 33;
		mixed *= 0xc4ceb9fe1a85ec53l;
		mixed ^= mixed >>> 33;
		int index = nonNegativeIndex(mixed, definitions.size());
		return new ArrayList<>(definitions.values()).get(index);
	}

	/**
	 * RoboVM 2.3.x does not provide Math.floorMod. Remainder normalization is
	 * safe even for Long.MIN_VALUE because it never negates the input.
	 */
	static int nonNegativeIndex(long value, int size) {
		if (size <= 0) throw new IllegalArgumentException("size must be positive");
		long remainder = value % size;
		return (int)(remainder < 0L ? remainder + size : remainder);
	}

	public Collection<ThemeDefinition> all() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	private static ThemeDefinition parse(JsonValue node) {
		return new ThemeDefinition(
				node.getString("id"),
				node.getString("name"),
				parseColor(node.getString("primaryColor")),
				parseColor(node.getString("secondaryColor")),
				node.getFloat("riskMultiplier"),
				parseRoomWeights(node.get("roomWeights")),
				parseStringWeights(node.get("lootWeights"), "lootWeights"),
				parseStringWeights(node.get("enemyWeights"), "enemyWeights"),
				parseStringList(node.get("coverCombination"), "coverCombination"));
	}

	private static Map<BukovRaidLayout.Zone, Float> parseRoomWeights(
			JsonValue node) {
		requireObject(node, "roomWeights");
		Map<BukovRaidLayout.Zone, Float> result = new LinkedHashMap<>();
		for (JsonValue entry = node.child; entry != null; entry = entry.next) {
			BukovRaidLayout.Zone zone;
			try {
				zone = BukovRaidLayout.Zone.valueOf(entry.name());
			} catch (IllegalArgumentException error) {
				throw new IllegalArgumentException(
						"Unknown room weight: " + entry.name(), error);
			}
			result.put(zone, entry.asFloat());
		}
		return result;
	}

	private static Map<String, Float> parseStringWeights(
			JsonValue node, String field) {
		requireObject(node, field);
		Map<String, Float> result = new LinkedHashMap<>();
		for (JsonValue entry = node.child; entry != null; entry = entry.next) {
			result.put(entry.name(), entry.asFloat());
		}
		return result;
	}

	private static List<String> parseStringList(JsonValue node, String field) {
		if (node == null || !node.isArray()) {
			throw new IllegalArgumentException(field + " array is required");
		}
		List<String> result = new ArrayList<>();
		for (JsonValue entry = node.child; entry != null; entry = entry.next) {
			result.add(entry.asString());
		}
		return result;
	}

	private static int parseColor(String value) {
		if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) {
			throw new IllegalArgumentException(
					"Theme color must be #RRGGBB: " + value);
		}
		return Integer.parseInt(value.substring(1), 16);
	}

	private static void requireObject(JsonValue node, String field) {
		if (node == null || !node.isObject()) {
			throw new IllegalArgumentException(field + " object is required");
		}
	}
}
