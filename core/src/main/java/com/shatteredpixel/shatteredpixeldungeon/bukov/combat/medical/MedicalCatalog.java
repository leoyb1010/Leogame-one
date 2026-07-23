package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gate-4 medical roster. Values are deliberately centralized so loot, HUD and
 * runtime code do not grow separate definitions.
 */
public final class MedicalCatalog {

	private static final String MEDICAL_PREFIX = "medical:";
	private static final Map<String, MedicalDefinition> DEFINITIONS =
			createDefinitions();

	public static MedicalDefinition find(String storedDefinitionId) {
		if (storedDefinitionId == null) {
			return null;
		}
		String id = storedDefinitionId.startsWith(MEDICAL_PREFIX)
				? storedDefinitionId.substring(MEDICAL_PREFIX.length())
				: storedDefinitionId;
		return DEFINITIONS.get(id);
	}

	public static MedicalDefinition require(String storedDefinitionId) {
		MedicalDefinition definition = find(storedDefinitionId);
		if (definition == null) {
			throw new IllegalArgumentException(
					"Unknown Bukov medical item: " + storedDefinitionId);
		}
		return definition;
	}

	public static List<MedicalDefinition> all() {
		return Collections.unmodifiableList(
				new ArrayList<>(DEFINITIONS.values()));
	}

	private static Map<String, MedicalDefinition> createDefinitions() {
		Map<String, MedicalDefinition> result = new LinkedHashMap<>();
		register(result, new MedicalDefinition(
				"bandage",
				6f, 0.75f, false, 0f, 0f, 0f,
				1.5f, 0.45f,
				true, true, true));
		register(result, new MedicalDefinition(
				"painkiller",
				0f, 0f, false, 50f, 12f, 0f,
				1.2f, 0.75f,
				false, true, true));
		register(result, new MedicalDefinition(
				"first_aid",
				42f, 0.50f, false, 0f, 0f, 0f,
				2.5f, 1.0f,
				true, true, true));
		register(result, new MedicalDefinition(
				"tourniquet",
				0f, 25f, false, 0f, 0f, 0f,
				2.0f, 0.75f,
				true, true, true));
		register(result, new MedicalDefinition(
				"antiseptic",
				12f, 0.30f, false, 0f, 0f, 0f,
				2.2f, 0.70f,
				true, true, true));
		register(result, new MedicalDefinition(
				"splint",
				0f, 0f, true, 0f, 0f, 0f,
				4.0f, 1.0f,
				true, true, true));
		register(result, new MedicalDefinition(
				"stim",
				24f, 0f, false, 35f, 20f, 18f,
				1.0f, 1.5f,
				false, true, true));
		return Collections.unmodifiableMap(result);
	}

	private static void register(
			Map<String, MedicalDefinition> definitions,
			MedicalDefinition definition) {
		if (definitions.put(definition.id, definition) != null) {
			throw new IllegalStateException(
					"Duplicate medical definition: " + definition.id);
		}
	}

	private MedicalCatalog() {
	}
}
