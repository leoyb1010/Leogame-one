package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AmmoRegistry {

	public static final String DEFAULT_PATH = "bukov/content/ammunition.json";

	private final Map<String, AmmoDefinition> definitions = new LinkedHashMap<>();

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
			throw new IllegalArgumentException("Unsupported ammunition schema: " + schema);
		}
		JsonValue ammunition = root.get("ammunition");
		if (ammunition == null || !ammunition.isArray()) {
			throw new IllegalArgumentException("ammunition array is required");
		}

		Map<String, AmmoDefinition> parsed = new LinkedHashMap<>();
		for (JsonValue node = ammunition.child; node != null; node = node.next) {
			AmmoDefinition value = parse(node);
			value.validate();
			if (parsed.put(value.id, value) != null) {
				throw new IllegalArgumentException("Duplicate ammunition id: " + value.id);
			}
		}
		if (parsed.isEmpty()) {
			throw new IllegalStateException("No ammunition loaded");
		}
		definitions.clear();
		definitions.putAll(parsed);
	}

	public AmmoDefinition require(String id) {
		AmmoDefinition value = definitions.get(id);
		if (value == null) {
			throw new IllegalArgumentException("Unknown ammunition definition: " + id);
		}
		return value;
	}

	public AmmoDefinition find(String id) {
		return definitions.get(id);
	}

	public boolean compatible(String definitionId, String caliber) {
		AmmoDefinition definition = find(definitionId);
		return definition != null
				&& caliber != null
				&& caliber.equals(definition.caliber);
	}

	public Collection<AmmoDefinition> all() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	private static AmmoDefinition parse(JsonValue node) {
		AmmoDefinition out = new AmmoDefinition();
		out.id = node.getString("id");
		out.name = node.getString("name");
		out.variant = AmmoVariant.valueOf(node.getString("variant"));
		out.caliber = node.getString("caliber");
		out.damageMultiplier = node.getFloat("damageMultiplier");
		out.penetrationMultiplier = node.getFloat("penetrationMultiplier");
		out.noiseMultiplier = node.getFloat("noiseMultiplier");
		out.weightKg = node.getFloat("weightKg");
		out.value = node.getInt("value");
		return out;
	}
}
