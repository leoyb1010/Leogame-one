package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;

public final class ExperienceContractRegistry {

	public static final String DEFAULT_PATH =
			"bukov/content/experience_contract.json";

	public ExperienceContract loadDefault() {
		return load(Gdx.files.internal(DEFAULT_PATH));
	}

	public ExperienceContract load(FileHandle file) {
		if (file == null) {
			throw new IllegalArgumentException("file is required");
		}
		return loadJson(file.readString("UTF-8"));
	}

	public ExperienceContract loadJson(String json) {
		if (json == null) {
			throw new IllegalArgumentException("json is required");
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
		out.maximumShakePx = feedback.getFloat("maximumShakePx");
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
			profile.shakeAmplitudePx =
					node.getFloat("shakeAmplitudePx");
			profile.shakeDurationMs = node.getInt("shakeDurationMs");
			profile.vibrationAmplitude =
					node.getFloat("vibrationAmplitude");
			profile.vibrationDurationMs =
					node.getInt("vibrationDurationMs");
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
