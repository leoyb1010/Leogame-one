package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.armor.ArmorDefinition;

import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for the automatically equipped Bukov gear slots.
 *
 * The first armor and first backpack in loadout order occupy their slots.
 * Additional copies remain carried cargo. This keeps every physical item in
 * the UID ledger while making the two equipment slots deterministic.
 */
public final class BukovGearRules {

	public static final float BASE_WEIGHT_CAPACITY_KG = 40f;

	public enum Slot {
		ARMOR,
		BACKPACK
	}

	public static final class Snapshot {
		public final RaidItem armor;
		public final RaidItem backpack;
		public final float weightCapacityKg;
		public final float movementMultiplier;
		public final float noiseMultiplier;

		private Snapshot(
				RaidItem armor,
				RaidItem backpack,
				float weightCapacityKg,
				float movementMultiplier,
				float noiseMultiplier) {
			this.armor = armor == null ? null : armor.copy();
			this.backpack = backpack == null ? null : backpack.copy();
			this.weightCapacityKg = weightCapacityKg;
			this.movementMultiplier = movementMultiplier;
			this.noiseMultiplier = noiseMultiplier;
		}
	}

	private static final class BackpackDefinition {
		private final String id;
		private final float additionalCapacityKg;
		private final float movementPenalty;
		private final float noisePenalty;

		private BackpackDefinition(
				String id,
				float additionalCapacityKg,
				float movementPenalty,
				float noisePenalty) {
			this.id = id;
			this.additionalCapacityKg = additionalCapacityKg;
			this.movementPenalty = movementPenalty;
			this.noisePenalty = noisePenalty;
		}
	}

	private static final BackpackDefinition SCOUT_PACK =
			new BackpackDefinition("scout_pack", 8f, 0.01f, 0.03f);
	private static final BackpackDefinition FIELD_PACK =
			new BackpackDefinition("field_pack", 16f, 0.04f, 0.08f);

	private BukovGearRules() {
	}

	public static Snapshot resolve(Collection<RaidItem> carriedItems) {
		return resolve(carriedItems, BASE_WEIGHT_CAPACITY_KG);
	}

	public static Snapshot resolve(
			Collection<RaidItem> carriedItems,
			float baseCapacityKg) {
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.isFinite(baseCapacityKg) || baseCapacityKg < 0f) {
			throw new IllegalArgumentException(
					"baseCapacityKg must be finite and non-negative");
		}
		if (carriedItems == null) {
			throw new IllegalArgumentException("carriedItems are required");
		}

		RaidItem armor = null;
		RaidItem backpack = null;
		for (RaidItem item : carriedItems) {
			if (item == null) {
				throw new IllegalArgumentException("carried item is required");
			}
			if (item.quantity() != 1 && slotFor(item.definitionId()) != null) {
				throw new IllegalArgumentException(
						"equipped gear must be one physical item: "
								+ item.itemUid());
			}
			Slot slot = slotFor(item.definitionId());
			if (slot == Slot.ARMOR && armor == null) {
				armor = item;
			} else if (slot == Slot.BACKPACK && backpack == null) {
				backpack = item;
			}
		}

		float capacity = baseCapacityKg;
		float movementPenalty = 0f;
		float noisePenalty = 0f;
		if (armor != null) {
			ArmorDefinition definition =
					ArmorCatalog.require(armor.definitionId());
			movementPenalty += definition.movementPenalty;
			noisePenalty += definition.noisePenalty;
		}
		if (backpack != null) {
			BackpackDefinition definition =
					requireBackpack(backpack.definitionId());
			capacity += definition.additionalCapacityKg;
			movementPenalty += definition.movementPenalty;
			noisePenalty += definition.noisePenalty;
		}
		return new Snapshot(
				armor,
				backpack,
				capacity,
				Math.max(0.55f, 1f - movementPenalty),
				1f + noisePenalty);
	}

	public static Slot slotFor(String definitionId) {
		if (definitionId == null) {
			return null;
		}
		if (ArmorCatalog.find(definitionId) != null) {
			return Slot.ARMOR;
		}
		return backpackDefinition(definitionId) == null
				? null : Slot.BACKPACK;
	}

	public static boolean isKnownBackpack(String definitionId) {
		return backpackDefinition(definitionId) != null;
	}

	/** Stable content-contract view; equipment runtime remains encapsulated. */
	public static List<String> allBackpackIds() {
		return Collections.unmodifiableList(Arrays.asList(
				SCOUT_PACK.id, FIELD_PACK.id));
	}

	private static BackpackDefinition requireBackpack(String definitionId) {
		BackpackDefinition definition = backpackDefinition(definitionId);
		if (definition == null) {
			throw new IllegalArgumentException(
					"Unknown Bukov backpack: " + definitionId);
		}
		return definition;
	}

	private static BackpackDefinition backpackDefinition(String storedId) {
		if (storedId == null) {
			return null;
		}
		String id = storedId.startsWith("backpack:")
				? storedId.substring("backpack:".length())
				: storedId;
		if (SCOUT_PACK.id.equals(id)) {
			return SCOUT_PACK;
		}
		if (FIELD_PACK.id.equals(id)) {
			return FIELD_PACK;
		}
		return null;
	}
}
