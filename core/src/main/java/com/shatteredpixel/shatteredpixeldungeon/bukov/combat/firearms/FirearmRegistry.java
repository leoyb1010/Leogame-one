package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FirearmRegistry {

	public static final String DEFAULT_PATH = "bukov/content/firearms.json";

	private final Map<String, FirearmDefinition> definitions = new LinkedHashMap<>();

	public void loadDefault() {
		loadInternal(DEFAULT_PATH);
	}

	public void loadInternal(String path) {
		load(Gdx.files.internal(path));
	}

	public void load(FileHandle file) {
		if (file == null) {
			throw new IllegalArgumentException("file is required");
		}
		loadJson(file.readString("UTF-8"));
	}

	public void loadJson(String json) {
		if (json == null) {
			throw new IllegalArgumentException("json is required");
		}

		JsonValue root = new JsonReader().parse(json);
		int schema = root.getInt("schemaVersion", -1);
		if (schema != 1) {
			throw new IllegalArgumentException("Unsupported firearm schema: " + schema);
		}

		JsonValue firearms = root.get("firearms");
		if (firearms == null || !firearms.isArray()) {
			throw new IllegalArgumentException("firearms array is required");
		}

		Map<String, FirearmDefinition> parsed = new LinkedHashMap<>();
		for (JsonValue node = firearms.child; node != null; node = node.next) {
			FirearmDefinition value = parse(node);
			value.validate();
			if (parsed.put(value.id, value) != null) {
				throw new IllegalArgumentException("Duplicate firearm id: " + value.id);
			}
		}
		if (parsed.isEmpty()) {
			throw new IllegalStateException("No firearms loaded");
		}

		// Only replace a valid registry, so a malformed reload cannot erase live content.
		definitions.clear();
		definitions.putAll(parsed);
	}

	public FirearmDefinition require(String id) {
		FirearmDefinition value = definitions.get(id);
		if (value == null) {
			throw new IllegalArgumentException("Unknown firearm definition: " + id);
		}
		return value;
	}

	public FirearmDefinition find(String id) {
		return definitions.get(id);
	}

	public Collection<FirearmDefinition> all() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	public void validateAmmunition(AmmoRegistry ammunition) {
		if (ammunition == null) {
			throw new IllegalArgumentException("ammunition registry is required");
		}
		for (FirearmDefinition firearm : definitions.values()) {
			AmmoDefinition defaultAmmo = ammunition.find(firearm.defaultAmmo);
			if (defaultAmmo == null) {
				throw new IllegalStateException(
						"Unknown default ammunition for " + firearm.id
								+ ": " + firearm.defaultAmmo);
			}
			if (!firearm.caliber.equals(defaultAmmo.caliber)) {
				throw new IllegalStateException(
						"Incompatible default ammunition for " + firearm.id
								+ ": " + firearm.defaultAmmo);
			}
		}
	}

	private FirearmDefinition parse(JsonValue node) {
		FirearmDefinition out = new FirearmDefinition();
		out.id = node.getString("id");
		out.name = node.getString("name");
		out.weaponClass = FirearmClass.valueOf(
				node.getString("weaponClass", "PISTOL"));
		out.caliber = node.getString("caliber");
		out.defaultAmmo = node.getString("defaultAmmo");
		out.fireMode = FireMode.valueOf(node.getString("fireMode"));
		out.damage = node.getFloat("damage");
		out.penetration = node.getFloat("penetration");
		out.rpm = node.getFloat("rpm");
		out.magazineSize = node.getInt("magazineSize");
		out.reloadSeconds = node.getFloat("reloadSeconds");
		out.effectiveRangeTiles = node.getFloat("effectiveRangeTiles");
		out.baseSpreadDeg = node.getFloat("baseSpreadDeg");
		out.movingSpreadDeg = node.getFloat("movingSpreadDeg");
		out.recoilPerShot = node.getFloat("recoilPerShot");
		out.recoilRecovery = node.getFloat("recoilRecovery");
		out.pellets = node.getInt("pellets");
		out.noiseRadiusTiles = node.getFloat("noiseRadiusTiles");
		out.weightKg = node.getFloat("weightKg");
		out.value = node.getInt("value");
		out.feedbackProfile = node.getString(
				"feedbackProfile",
				defaultFeedbackProfile(out.weaponClass));
		out.soundPitch = node.getFloat("soundPitch", 1f);
		out.soundGain = node.getFloat("soundGain", 1f);
		out.muzzleIntensity = node.getFloat("muzzleIntensity", 1f);
		out.tracerIntensity = node.getFloat("tracerIntensity", 1f);
		out.impactIntensity = node.getFloat("impactIntensity", 1f);
		out.feedbackIntensity = node.getFloat("feedbackIntensity", 1f);
		return out;
	}

	private static String defaultFeedbackProfile(FirearmClass weaponClass) {
		return weaponClass == null ? "SIDEARM" : weaponClass.name();
	}
}
