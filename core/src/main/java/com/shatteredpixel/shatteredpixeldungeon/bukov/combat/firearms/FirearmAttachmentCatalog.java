package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small validated catalog with one meaningful first-party part per slot. */
public final class FirearmAttachmentCatalog {

	public static final String RED_DOT = "optic:red_dot";
	public static final String EXTENDED_MAG = "magazine:extended";
	public static final String SUPPRESSOR = "muzzle:suppressor";

	private static final Map<String, FirearmAttachmentDefinition> DEFINITIONS =
			new LinkedHashMap<>();

	static {
		register(new FirearmAttachmentDefinition(
				RED_DOT, "紧凑红点", FirearmAttachmentSlot.OPTIC,
				1f, 1.15f, 0.78f, 0.88f,
				1f, 1f, 1f, 1f, 0.18f));
		register(new FirearmAttachmentDefinition(
				EXTENDED_MAG, "扩容弹匣", FirearmAttachmentSlot.MAGAZINE,
				1f, 1f, 1f, 1f,
				1f, 1.14f, 1.40f, 1f, 0.42f));
		register(new FirearmAttachmentDefinition(
				SUPPRESSOR, "战术抑制器", FirearmAttachmentSlot.MUZZLE,
				0.96f, 0.94f, 1f, 1f,
				0.82f, 1f, 1f, 0.42f, 0.36f));
	}

	private FirearmAttachmentCatalog() {
	}

	public static FirearmAttachmentDefinition require(String id) {
		FirearmAttachmentDefinition definition = DEFINITIONS.get(id);
		if (definition == null) {
			throw new IllegalArgumentException("Unknown firearm attachment: " + id);
		}
		return definition;
	}

	public static List<FirearmAttachmentDefinition> all() {
		return Collections.unmodifiableList(
				new ArrayList<>(DEFINITIONS.values()));
	}

	private static void register(FirearmAttachmentDefinition definition) {
		if (DEFINITIONS.put(definition.id, definition) != null) {
			throw new IllegalStateException(
					"Duplicate firearm attachment: " + definition.id);
		}
	}
}
