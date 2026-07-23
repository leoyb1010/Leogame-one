package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnemyArchetypeRegistry {

	public static final String DEFAULT_PATH = "bukov/content/enemies.json";

	private final Map<String, EnemyArchetypeDefinition> definitions =
			new LinkedHashMap<>();

	public void loadDefault() {
		load(Gdx.files.internal(DEFAULT_PATH));
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
			throw new IllegalArgumentException(
					"Unsupported enemy schema: " + schema
			);
		}
		JsonValue enemies = root.get("enemies");
		if (enemies == null || !enemies.isArray()) {
			throw new IllegalArgumentException("enemies array is required");
		}

		Map<String, EnemyArchetypeDefinition> parsed = new LinkedHashMap<>();
		int common = 0;
		int elite = 0;
		int boss = 0;
		for (JsonValue node = enemies.child; node != null; node = node.next) {
			EnemyArchetypeDefinition definition = parse(node);
			definition.validate();
			if (parsed.put(definition.id, definition) != null) {
				throw new IllegalArgumentException(
						"Duplicate enemy id: " + definition.id
				);
			}
			switch (definition.tier) {
				case COMMON:
					common++;
					break;
				case ELITE:
					elite++;
					break;
				case BOSS:
					boss++;
					break;
			}
		}
		if (common + elite < 12 || common < 1 || elite < 1 || boss != 1) {
			throw new IllegalStateException(
					"Raid roster requires at least 12 common/elite archetypes "
							+ "and exactly 1 boss"
			);
		}
		definitions.clear();
		definitions.putAll(parsed);
	}

	public EnemyArchetypeDefinition require(String id) {
		EnemyArchetypeDefinition definition = definitions.get(id);
		if (definition == null) {
			throw new IllegalArgumentException("Unknown enemy: " + id);
		}
		return definition;
	}

	public Collection<EnemyArchetypeDefinition> all() {
		return Collections.unmodifiableCollection(definitions.values());
	}

	/** Validates all authored enemy firearm references after both registries load. */
	public void validateFirearms(FirearmRegistry firearms) {
		if (firearms == null) {
			throw new IllegalArgumentException("firearm registry is required");
		}
		for (EnemyArchetypeDefinition enemy : definitions.values()) {
			if (enemy.weaponDefinitionId != null
					&& !enemy.weaponDefinitionId.trim().isEmpty()) {
				firearms.require(enemy.weaponDefinitionId);
			}
		}
	}

	private static EnemyArchetypeDefinition parse(JsonValue node) {
		EnemyArchetypeDefinition out = new EnemyArchetypeDefinition();
		out.id = node.getString("id");
		out.name = node.getString("name");
		out.tier = EnemyTier.valueOf(node.getString("tier"));
		out.role = EnemyRole.valueOf(node.getString("role"));
		out.hostClassHint = node.getString("hostClassHint");
		out.weaponDefinitionId = node.getString(
				"weaponDefinitionId",
				null
		);
		out.health = node.getInt("health");
		out.movementSpeed = node.getFloat("movementSpeed");
		out.perceptionRange = node.getFloat("perceptionRange");
		out.engagementRange = node.getFloat("engagementRange");
		out.minimumDamage = node.getInt("minimumDamage");
		out.maximumDamage = node.getInt("maximumDamage");
		out.spawnWeight = node.getInt("spawnWeight");
		out.minimumSpawnSeconds = node.getFloat("minimumSpawnSeconds");
		out.minimumDistanceFromSpawnRooms =
				node.getInt("minimumDistanceFromSpawnRooms");
		out.maximumActive = node.getInt("maximumActive");
		out.firstRaidMinimumSeconds =
				node.getFloat("firstRaidMinimumSeconds");
		out.firstRaidMaximumActive =
				node.getInt("firstRaidMaximumActive");
		out.optionalRouteOnly = node.getBoolean("optionalRouteOnly");
		out.bossArenaOnly = node.getBoolean("bossArenaOnly");
		JsonValue abilities = node.get("abilities");
		if (abilities == null || !abilities.isArray()) {
			throw new IllegalArgumentException(
					"abilities array is required: " + out.id
			);
		}
		out.abilities = new String[abilities.size];
		int index = 0;
		for (JsonValue ability = abilities.child;
				ability != null;
				ability = ability.next) {
			out.abilities[index++] = ability.asString();
		}
		return out;
	}
}
