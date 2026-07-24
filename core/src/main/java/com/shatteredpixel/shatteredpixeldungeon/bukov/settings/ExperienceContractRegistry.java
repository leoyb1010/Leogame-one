package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;

public final class ExperienceContractRegistry {

	public static final String DEFAULT_PATH =
			"bukov/content/experience_contract.json";

	public ExperienceContract loadDefault() {
		return load(
				Gdx.files == null
						? new FileHandle(
								"src/main/assets/" + DEFAULT_PATH)
						: Gdx.files.internal(DEFAULT_PATH),
				BukovUiTokens.loadDefault());
	}

	public ExperienceContract load(FileHandle file) {
		return load(file, BukovUiTokens.loadDefault());
	}

	ExperienceContract load(
			FileHandle file, BukovUiTokens uiTokens) {
		if (file == null) {
			throw new IllegalArgumentException("file is required");
		}
		return loadJson(file.readString("UTF-8"), uiTokens);
	}

	public ExperienceContract loadJson(String json) {
		return loadJson(json, BukovUiTokens.loadDefault());
	}

	ExperienceContract loadJson(
			String json, BukovUiTokens uiTokens) {
		if (json == null) {
			throw new IllegalArgumentException("json is required");
		}
		if (uiTokens == null) {
			throw new IllegalArgumentException("uiTokens are required");
		}
		JsonValue root = new JsonReader().parse(json);
		int schema = root.getInt("schemaVersion", -1);
		if (schema != 1) {
			throw new IllegalArgumentException(
					"Unsupported experience contract schema: " + schema
			);
		}
		ExperienceContract out = new ExperienceContract();
		JsonValue mix = requiredObject(root, "mixDefaults");
		out.defaultMasterVolume = unitPercent(mix, "master");
		out.defaultMusicVolume = unitPercent(mix, "music");
		out.defaultSfxVolume = unitPercent(mix, "sfx");
		out.defaultAmbienceVolume = unitPercent(mix, "ambience");

		JsonValue spatial = requiredObject(root, "spatialAudio");
		out.fullVolumeDistance = spatial.getFloat("fullVolumeDistance");
		out.referenceDistance = spatial.getFloat("referenceDistance");
		out.referenceDecibels = spatial.getFloat("referenceDecibels");
		out.wallDecibels = spatial.getFloat("wallDecibels");
		out.lowPassDistance = spatial.getFloat("lowPassDistance");
		out.lowPassHz = spatial.getFloat("lowPassHz");
		out.minimumAudibleDecibels =
				spatial.getFloat("minimumAudibleDecibels");

		JsonValue visualization = requiredObject(
				root,
				"keySoundVisualization"
		);
		out.visualizationNearDistance =
				visualization.getFloat("nearDistance");
		out.visualizationMidDistance =
				visualization.getFloat("midDistance");

		JsonValue feedback = requiredObject(root, "feedback");
		out.maximumShakePx = uiTokens.maximumShakePx();
		JsonValue profiles = feedback.get("profiles");
		if (profiles == null || !profiles.isArray()) {
			throw new IllegalArgumentException(
					"feedback profiles array is required"
			);
		}
		for (JsonValue node = profiles.child;
			node != null;
			node = node.next) {
			CombatFeedbackType type = CombatFeedbackType.valueOf(
					node.getString("type")
			);
			ExperienceContract.FeedbackProfile profile =
					new ExperienceContract.FeedbackProfile();
			BukovUiTokens.Haptic haptic =
					uiTokens.haptic(type.name());
			profile.shakeAmplitudePx =
					haptic.shakeAmplitudePx();
			profile.shakeDurationMs =
					haptic.shakeDurationMs();
			profile.vibrationAmplitude =
					haptic.vibrationAmplitude();
			profile.vibrationDurationMs =
					haptic.vibrationDurationMs();
			profile.frequency = haptic.frequency();
			profile.hitstopMs = node.getInt("hitstopMs");
			profile.visualIntensity =
					node.getFloat("visualIntensity");
			out.put(type, profile);
		}
		out.validate();
		return out;
	}

	private static JsonValue requiredObject(JsonValue root, String name) {
		JsonValue value = root.get(name);
		if (value == null || !value.isObject()) {
			throw new IllegalArgumentException(name + " object is required");
		}
		return value;
	}

	private static float unitPercent(JsonValue node, String name) {
		return node.getFloat(name) / 100f;
	}
}
