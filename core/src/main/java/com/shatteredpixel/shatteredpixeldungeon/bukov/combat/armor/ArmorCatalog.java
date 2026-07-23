package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.RealtimeDamage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArmorCatalog {

	private static final String ARMOR_PREFIX = "armor:";
	private static final Map<String, ArmorDefinition> DEFINITIONS =
			createDefinitions();

	public static ArmorDefinition find(String storedDefinitionId) {
		if (storedDefinitionId == null) {
			return null;
		}
		String id = storedDefinitionId.startsWith(ARMOR_PREFIX)
				? storedDefinitionId.substring(ARMOR_PREFIX.length())
				: storedDefinitionId;
		return DEFINITIONS.get(id);
	}

	public static ArmorDefinition require(String storedDefinitionId) {
		ArmorDefinition definition = find(storedDefinitionId);
		if (definition == null) {
			throw new IllegalArgumentException(
					"Unknown Bukov armor: " + storedDefinitionId);
		}
		return definition;
	}

	public static List<ArmorDefinition> all() {
		return Collections.unmodifiableList(
				new ArrayList<>(DEFINITIONS.values()));
	}

	private static Map<String, ArmorDefinition> createDefinitions() {
		Map<String, ArmorDefinition> result = new LinkedHashMap<>();
		register(result, new ArmorDefinition(
				"soft_vest",
				1, 48f, 6f,
				EnumSet.of(RealtimeDamage.HitZone.CORE),
				0.02f, 0.04f));
		register(result, new ArmorDefinition(
				"patrol_vest",
				2, 82f, 12f,
				EnumSet.of(
						RealtimeDamage.HitZone.CORE,
						RealtimeDamage.HitZone.LIMB),
				0.06f, 0.10f));
		register(result, new ArmorDefinition(
				"ceramic_rig",
				4, 112f, 22f,
				EnumSet.of(
						RealtimeDamage.HitZone.CORE,
						RealtimeDamage.HitZone.LIMB),
				0.12f, 0.18f));
		return Collections.unmodifiableMap(result);
	}

	private static void register(
			Map<String, ArmorDefinition> definitions,
			ArmorDefinition definition) {
		if (definitions.put(definition.id, definition) != null) {
			throw new IllegalStateException(
					"Duplicate armor definition: " + definition.id);
		}
	}

	private ArmorCatalog() {
	}
}
