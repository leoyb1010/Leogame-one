package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validated presentation parameters shared by Bukov UI and FX.
 */
public final class BukovUiTokens {

	public static final String DEFAULT_PATH = "bukov/content/ui_tokens.json";
	public static final int CURRENT_VERSION = 1;

	private final Map<String, Integer> colors;
	private final Map<String, Integer> motionMs;
	private final Map<String, Integer> vfxPoolCapacity;

	private BukovUiTokens(
			Map<String, Integer> colors,
			Map<String, Integer> motionMs,
			Map<String, Integer> vfxPoolCapacity) {
		this.colors = Collections.unmodifiableMap(colors);
		this.motionMs = Collections.unmodifiableMap(motionMs);
		this.vfxPoolCapacity = Collections.unmodifiableMap(vfxPoolCapacity);
	}

	public static BukovUiTokens loadDefault() {
		return load(Gdx.files.internal(DEFAULT_PATH));
	}

	public static BukovUiTokens load(FileHandle file) {
		if (file == null) {
			throw new IllegalArgumentException("file is required");
		}
		return parse(file.readString("UTF-8"));
	}

	public static BukovUiTokens parse(String json) {
		if (json == null) {
			throw new IllegalArgumentException("json is required");
		}
		JsonValue root = new JsonReader().parse(json);
		int version = root.getInt("uiTokensVersion", -1);
		if (version != CURRENT_VERSION) {
			throw new IllegalArgumentException("Unsupported UI tokens version: " + version);
		}

		Map<String, Integer> colors = parseColors(root.get("colors"));
		Map<String, Integer> motion = parsePositiveInts(root.get("motionMs"), "motionMs");
		Map<String, Integer> pools = parsePositiveInts(
				root.get("vfxPoolCapacity"),
				"vfxPoolCapacity"
		);
		validateTypography(root.get("typographyPx"));
		validateHaptics(root.get("haptics"));
		requireKeys(colors,
				"ink.background", "ink.shadow", "ink.loading",
				"ink.failure", "panel.surface", "panel.deep",
				"panel.result", "panel.border",
				"accent.interact", "accent.valuable", "accent.danger",
				"accent.extract", "text.primary", "text.secondary",
				"text.disabled");
		requireKeys(motion, "instant", "fast", "base", "slow", "ritual");
		requireKeys(pools,
				"muzzleFlash", "tracer", "shell", "impactSpark",
				"bloodMist", "bulletMark", "explosion");
		return new BukovUiTokens(colors, motion, pools);
	}

	public int color(String token) {
		return require(colors, token, "color");
	}

	public int colorWithAlpha(String token, int alpha) {
		if (alpha < 0 || alpha > 255) {
			throw new IllegalArgumentException(
					"alpha must be between zero and 255");
		}
		return (alpha << 24) | (color(token) & 0xFFFFFF);
	}

	public int motionMs(String token) {
		return require(motionMs, token, "motion");
	}

	public int vfxPoolCapacity(String token) {
		return require(vfxPoolCapacity, token, "VFX pool");
	}

	private static Map<String, Integer> parseColors(JsonValue object) {
		if (object == null || !object.isObject()) {
			throw new IllegalArgumentException("colors object is required");
		}
		Map<String, Integer> result = new LinkedHashMap<>();
		for (JsonValue value = object.child; value != null; value = value.next) {
			String raw = value.asString();
			if (raw == null || !raw.matches("#[0-9A-Fa-f]{6}")) {
				throw new IllegalArgumentException("Invalid color: " + value.name);
			}
			result.put(value.name, Integer.parseInt(raw.substring(1), 16));
		}
		return result;
	}

	private static Map<String, Integer> parsePositiveInts(
			JsonValue object,
			String label) {
		if (object == null || !object.isObject()) {
			throw new IllegalArgumentException(label + " object is required");
		}
		Map<String, Integer> result = new LinkedHashMap<>();
		for (JsonValue value = object.child; value != null; value = value.next) {
			int parsed = value.asInt();
			if (parsed <= 0) {
				throw new IllegalArgumentException(label + " values must be positive");
			}
			result.put(value.name, parsed);
		}
		return result;
	}

	private static void validateTypography(JsonValue values) {
		if (values == null || !values.isArray() || values.size != 5) {
			throw new IllegalArgumentException("typographyPx must contain five sizes");
		}
		int previous = 0;
		for (JsonValue value = values.child; value != null; value = value.next) {
			int size = value.asInt();
			if (size <= previous) {
				throw new IllegalArgumentException("typographyPx must be increasing");
			}
			previous = size;
		}
	}

	private static void validateHaptics(JsonValue haptics) {
		if (haptics == null || !haptics.isObject() || haptics.size < 6) {
			throw new IllegalArgumentException("haptics must define all core events");
		}
		for (JsonValue event = haptics.child; event != null; event = event.next) {
			float amplitude = event.getFloat("amplitudePx", 0f);
			int duration = event.getInt("durationMs", 0);
			String frequency = event.getString("frequency", "");
			if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(amplitude)
					|| amplitude <= 0f
					|| duration <= 0
					|| (!"low".equals(frequency)
					&& !"medium".equals(frequency)
					&& !"high".equals(frequency))) {
				throw new IllegalArgumentException("Invalid haptic event: " + event.name);
			}
		}
	}

	private static void requireKeys(Map<String, Integer> values, String... required) {
		for (String key : required) {
			if (!values.containsKey(key)) {
				throw new IllegalArgumentException("Missing UI token: " + key);
			}
		}
	}

	private static int require(Map<String, Integer> values, String key, String label) {
		Integer result = values.get(key);
		if (result == null) {
			throw new IllegalArgumentException("Unknown " + label + " token: " + key);
		}
		return result;
	}
}
