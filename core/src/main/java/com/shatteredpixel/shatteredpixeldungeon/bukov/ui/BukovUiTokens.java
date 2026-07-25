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
	private final Map<String, Integer> typographyPx;
	private final Map<String, Integer> motionMs;
	private final float maximumShakePx;
	private final Map<String, Haptic> haptics;
	private final Map<String, Integer> vfxPoolCapacity;

	private BukovUiTokens(
			Map<String, Integer> colors,
			Map<String, Integer> typographyPx,
			Map<String, Integer> motionMs,
			float maximumShakePx,
			Map<String, Haptic> haptics,
			Map<String, Integer> vfxPoolCapacity) {
		this.colors = Collections.unmodifiableMap(colors);
		this.typographyPx = Collections.unmodifiableMap(typographyPx);
		this.motionMs = Collections.unmodifiableMap(motionMs);
		this.maximumShakePx = maximumShakePx;
		this.haptics = Collections.unmodifiableMap(haptics);
		this.vfxPoolCapacity = Collections.unmodifiableMap(vfxPoolCapacity);
	}

	public static BukovUiTokens loadDefault() {
		return load(Gdx.files == null
				? new FileHandle("src/main/assets/" + DEFAULT_PATH)
				: Gdx.files.internal(DEFAULT_PATH));
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
		Map<String, Integer> typography = parsePositiveInts(
				root.get("typographyPx"),
				"typographyPx"
		);
		Map<String, Integer> motion = parsePositiveInts(root.get("motionMs"), "motionMs");
		float maximumShakePx =
				root.getFloat("hapticMaximumShakePx", -1f);
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
						maximumShakePx)
				|| maximumShakePx <= 0f
				|| maximumShakePx > 8f) {
			throw new IllegalArgumentException(
					"hapticMaximumShakePx must be within Gate 5 ceiling");
		}
		Map<String, Haptic> haptics = parseHaptics(root.get("haptics"));
		Map<String, Integer> pools = parsePositiveInts(
				root.get("vfxPoolCapacity"),
				"vfxPoolCapacity"
		);
		requireKeys(colors,
				"ink.background", "ink.shadow", "ink.loading",
				"ink.failure", "panel.surface", "panel.deep",
				"panel.result", "panel.border",
				"accent.interact", "accent.valuable", "accent.danger",
				"accent.extract", "text.primary", "text.secondary",
				"text.disabled",
				"combat.fx.solid",
				"combat.fx.tracer.friendly",
				"combat.fx.tracer.hostile",
				"combat.fx.tracer.outline",
				"combat.fx.muzzle.friendly",
				"combat.fx.impact.friendly",
				"combat.fx.shell.friendly",
				"combat.fx.shell.hostile",
				"combat.fx.blood.dark",
				"combat.fx.blood.bright",
				"combat.fx.bulletMark.edge",
				"combat.fx.bulletMark.hole",
				"combat.fx.explosion.hot",
				"combat.fx.explosion.flame",
				"combat.fx.explosion.smoke",
				"combat.enemy.contact",
				"combat.enemy.blood.alleyScout",
				"combat.enemy.blood.armored",
				"combat.enemy.blood.breachVeteran",
				"combat.enemy.blood.captain",
				"combat.enemy.blood.depotShotgunner",
				"combat.enemy.blood.drone",
				"combat.enemy.blood.fogStalker",
				"combat.enemy.blood.gunner",
				"combat.enemy.blood.ironClaspMarksman",
				"combat.enemy.blood.lineRifleman",
				"combat.enemy.blood.scavenger",
				"combat.enemy.blood.signalOperator",
				"combat.enemy.blood.whiteLine",
				"level.default.primary",
				"level.default.secondary");
		requireKeys(typography,
				"hud", "body", "section", "title", "display");
		validateTypography(typography);
		requireKeys(motion, "instant", "fast", "base", "slow", "ritual");
		requireKeys(haptics,
				"RIFLE_SHOT", "PLAYER_HIT", "SHOTGUN_NEAR",
				"EXPLOSION", "BOSS_SLAM", "BOSS_OVERLOAD",
				"EXTRACT_STAMP",
				"KILL", "WEAKPOINT_KILL", "BOSS_PHASE_BREAK");
		requireKeys(pools,
				"muzzleFlash", "tracer", "shell", "impactSpark",
				"bloodMist", "bulletMark", "explosion");
		return new BukovUiTokens(
				colors, typography, motion,
				maximumShakePx, haptics, pools);
	}

	public int color(String token) {
		return require(colors, token, "color");
	}

	public int colorWithAlpha(String token, int alpha) {
		return withAlpha(color(token), alpha);
	}

	public static int withAlpha(int color, int alpha) {
		if (alpha < 0 || alpha > 255) {
			throw new IllegalArgumentException(
					"alpha must be between zero and 255");
		}
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	public int motionMs(String token) {
		return require(motionMs, token, "motion");
	}

	public int typographyPx(String token) {
		return require(typographyPx, token, "typography");
	}

	/**
	 * Returns the authored font role for scaled layouts.
	 *
	 * PixelScene already maps logical font pixels to device density. Scaling
	 * the glyph size again made 150% layouts overflow their authored line
	 * boxes on 3x iPhones. The accessibility scale therefore grows windows,
	 * spacing and touch targets while typography keeps its tested role size.
	 */
	public int scaledTypographyPx(String token) {
		return typographyPx(token);
	}

	public float maximumShakePx() {
		return maximumShakePx;
	}

	public Haptic haptic(String token) {
		Haptic result = haptics.get(token);
		if (result == null) {
			throw new IllegalArgumentException(
					"Unknown haptic token: " + token);
		}
		return result;
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

	private static void validateTypography(Map<String, Integer> values) {
		if (values.size() != 5) {
			throw new IllegalArgumentException(
					"typographyPx must define exactly five roles");
		}
		String[] tokens = {
				"hud", "body", "section", "title", "display"};
		int[] authoredScale = {7, 8, 9, 12, 16};
		for (int index = 0; index < tokens.length; index++) {
			String token = tokens[index];
			int size = require(values, token, "typography");
			if (size != authoredScale[index]) {
				throw new IllegalArgumentException(
						"typographyPx must use the authored 7/8/9/12/16 scale");
			}
		}
	}

	private static Map<String, Haptic> parseHaptics(JsonValue haptics) {
		if (haptics == null || !haptics.isObject()) {
			throw new IllegalArgumentException("haptics object is required");
		}
		Map<String, Haptic> result = new LinkedHashMap<>();
		for (JsonValue event = haptics.child; event != null; event = event.next) {
			float shakeAmplitude =
					event.getFloat("shakeAmplitudePx", -1f);
			int shakeDuration =
					event.getInt("shakeDurationMs", -1);
			float vibrationAmplitude =
					event.getFloat("vibrationAmplitude", -1f);
			int vibrationDuration =
					event.getInt("vibrationDurationMs", -1);
			String frequency = event.getString("frequency", "");
			if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
							shakeAmplitude)
					|| shakeAmplitude < 0f
					|| shakeAmplitude > 8f
					|| shakeDuration < 0
					|| shakeDuration > 1000
					|| (shakeAmplitude == 0f) != (shakeDuration == 0)
					|| !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
							vibrationAmplitude)
					|| vibrationAmplitude < 0f
					|| vibrationAmplitude > 1f
					|| vibrationDuration < 0
					|| vibrationDuration > 1000
					|| (vibrationAmplitude == 0f)
							!= (vibrationDuration == 0)
					|| (!"low".equals(frequency)
					&& !"medium".equals(frequency)
					&& !"high".equals(frequency))) {
				throw new IllegalArgumentException("Invalid haptic event: " + event.name);
			}
			if (result.put(event.name, new Haptic(
					shakeAmplitude,
					shakeDuration,
					vibrationAmplitude,
					vibrationDuration,
					frequency)) != null) {
				throw new IllegalArgumentException(
						"Duplicate haptic event: " + event.name);
			}
		}
		if (result.size() != 10) {
			throw new IllegalArgumentException(
					"haptics must define exactly ten feedback events");
		}
		return result;
	}

	private static void requireKeys(Map<String, ?> values, String... required) {
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

	public static final class Haptic {

		private final float shakeAmplitudePx;
		private final int shakeDurationMs;
		private final float vibrationAmplitude;
		private final int vibrationDurationMs;
		private final String frequency;

		private Haptic(
				float shakeAmplitudePx,
				int shakeDurationMs,
				float vibrationAmplitude,
				int vibrationDurationMs,
				String frequency) {
			this.shakeAmplitudePx = shakeAmplitudePx;
			this.shakeDurationMs = shakeDurationMs;
			this.vibrationAmplitude = vibrationAmplitude;
			this.vibrationDurationMs = vibrationDurationMs;
			this.frequency = frequency;
		}

		public float shakeAmplitudePx() {
			return shakeAmplitudePx;
		}

		public int shakeDurationMs() {
			return shakeDurationMs;
		}

		public float vibrationAmplitude() {
			return vibrationAmplitude;
		}

		public int vibrationDurationMs() {
			return vibrationDurationMs;
		}

		public String frequency() {
			return frequency;
		}
	}
}
