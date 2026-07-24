package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Objects;

/**
 * A concrete item instance carried through a Bukov raid.
 *
 * The UID identifies the physical instance and must survive drop, pickup,
 * extraction and stash transfer unchanged.
 */
public final class RaidItem implements Bundlable {

	private static final String ITEM_UID = "item_uid";
	private static final String DEFINITION_ID = "definition_id";
	private static final String QUANTITY = "quantity";
	private static final String UNIT_WEIGHT = "unit_weight";
	private static final String UNIT_VALUE = "unit_value";
	private static final String FOUND_IN_RAID = "found_in_raid";
	private static final String INSURED = "insured";
	private static final String DURABILITY = "durability";

	private String itemUid;
	private String definitionId;
	private int quantity;
	private float unitWeight;
	private int unitValue;
	private boolean foundInRaid;
	private boolean insured;
	private float durability;

	public RaidItem() {
		// Required by Bundle reflection.
	}

	public RaidItem(
			String itemUid,
			String definitionId,
			int quantity,
			float unitWeight,
			int unitValue,
			boolean foundInRaid,
			boolean insured,
			float durability) {
		this.itemUid = requireId(itemUid, "itemUid");
		this.definitionId = requireId(definitionId, "definitionId");
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				unitWeight) || unitWeight < 0f) {
			throw new IllegalArgumentException("unitWeight must be finite and non-negative");
		}
		if (unitValue < 0) {
			throw new IllegalArgumentException("unitValue must be non-negative");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				durability) || durability < 0f || durability > 1f) {
			throw new IllegalArgumentException("durability must be between 0 and 1");
		}
		this.quantity = quantity;
		this.unitWeight = unitWeight;
		this.unitValue = unitValue;
		this.foundInRaid = foundInRaid;
		this.insured = insured;
		this.durability = durability;
	}

	public String itemUid() {
		return itemUid;
	}

	public String definitionId() {
		return definitionId;
	}

	public int quantity() {
		return quantity;
	}

	public float unitWeight() {
		return unitWeight;
	}

	public int unitValue() {
		return unitValue;
	}

	public boolean foundInRaid() {
		return foundInRaid;
	}

	public boolean insured() {
		return insured;
	}

	public float durability() {
		return durability;
	}

	public float totalWeight() {
		return unitWeight * quantity;
	}

	public long totalValue() {
		return (long) unitValue * quantity;
	}

	public RaidItem copy() {
		return new RaidItem(
				itemUid,
				definitionId,
				quantity,
				unitWeight,
				unitValue,
				foundInRaid,
				insured,
				durability);
	}

	public RaidItem withRuntimeState(int updatedQuantity, float updatedDurability) {
		return new RaidItem(
				itemUid,
				definitionId,
				updatedQuantity,
				unitWeight,
				unitValue,
				foundInRaid,
				insured,
				updatedDurability);
	}

	/**
	 * Returns the same physical item with only its raid-origin marker changed.
	 * Successful settlement normalizes extracted loot before it can be selected
	 * for a later deployment; the UID and all economic/runtime state remain
	 * unchanged.
	 */
	public RaidItem withFoundInRaid(boolean updatedFoundInRaid) {
		return new RaidItem(
				itemUid,
				definitionId,
				quantity,
				unitWeight,
				unitValue,
				updatedFoundInRaid,
				insured,
				durability);
	}

	/** Insurance is consumed when an item is returned and must be repurchased. */
	public RaidItem withInsured(boolean updatedInsured) {
		return new RaidItem(
				itemUid,
				definitionId,
				quantity,
				unitWeight,
				unitValue,
				foundInRaid,
				updatedInsured,
				durability);
	}

	public RaidItem withUnitValue(int updatedUnitValue) {
		if (updatedUnitValue < 0) {
			throw new IllegalArgumentException(
					"updatedUnitValue must be non-negative");
		}
		return new RaidItem(
				itemUid,
				definitionId,
				quantity,
				unitWeight,
				updatedUnitValue,
				foundInRaid,
				insured,
				durability);
	}

	String fingerprintPart() {
		StringBuilder out = new StringBuilder();
		appendLengthPrefixed(out, itemUid);
		appendLengthPrefixed(out, definitionId);
		out.append(quantity).append(':')
				.append(Float.floatToIntBits(unitWeight)).append(':')
				.append(unitValue).append(':')
				.append(foundInRaid ? 1 : 0).append(':')
				.append(insured ? 1 : 0).append(':')
				.append(Float.floatToIntBits(durability));
		return out.toString();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(ITEM_UID, itemUid);
		bundle.put(DEFINITION_ID, definitionId);
		bundle.put(QUANTITY, quantity);
		bundle.put(UNIT_WEIGHT, unitWeight);
		bundle.put(UNIT_VALUE, unitValue);
		bundle.put(FOUND_IN_RAID, foundInRaid);
		bundle.put(INSURED, insured);
		bundle.put(DURABILITY, durability);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		RaidItem restored = new RaidItem(
				bundle.getString(ITEM_UID),
				bundle.getString(DEFINITION_ID),
				bundle.getInt(QUANTITY),
				bundle.getFloat(UNIT_WEIGHT),
				bundle.getInt(UNIT_VALUE),
				bundle.getBoolean(FOUND_IN_RAID),
				bundle.getBoolean(INSURED),
				bundle.getFloat(DURABILITY));
		itemUid = restored.itemUid;
		definitionId = restored.definitionId;
		quantity = restored.quantity;
		unitWeight = restored.unitWeight;
		unitValue = restored.unitValue;
		foundInRaid = restored.foundInRaid;
		insured = restored.insured;
		durability = restored.durability;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof RaidItem)) return false;
		RaidItem item = (RaidItem) other;
		return quantity == item.quantity
				&& Float.compare(unitWeight, item.unitWeight) == 0
				&& unitValue == item.unitValue
				&& foundInRaid == item.foundInRaid
				&& insured == item.insured
				&& Float.compare(durability, item.durability) == 0
				&& itemUid.equals(item.itemUid)
				&& definitionId.equals(item.definitionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				itemUid,
				definitionId,
				quantity,
				unitWeight,
				unitValue,
				foundInRaid,
				insured,
				durability);
	}

	private static String requireId(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}

	private static void appendLengthPrefixed(StringBuilder out, String value) {
		out.append(value.length()).append('#').append(value).append(':');
	}
}
