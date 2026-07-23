package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * Immutable economic snapshot used by the post-raid settlement screen.
 *
 * The snapshot deliberately stores definition data instead of resolving the
 * current item catalogue later. This keeps an old receipt truthful after
 * balance patches change item quantities or values.
 */
public final class SettlementItemSnapshot implements Bundlable {

	private static final String ITEM_UID = "item_uid";
	private static final String DEFINITION_ID = "definition_id";
	private static final String QUANTITY = "quantity";
	private static final String TOTAL_VALUE = "total_value";

	private String itemUid;
	private String definitionId;
	private int quantity;
	private long totalValue;

	public SettlementItemSnapshot() {
		// Required by Bundle reflection.
	}

	public SettlementItemSnapshot(
			String itemUid,
			String definitionId,
			int quantity,
			long totalValue) {
		this.itemUid = requireText(itemUid, "itemUid");
		this.definitionId = requireText(definitionId, "definitionId");
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity must be positive");
		}
		if (totalValue < 0L) {
			throw new IllegalArgumentException("totalValue must be non-negative");
		}
		this.quantity = quantity;
		this.totalValue = totalValue;
	}

	static SettlementItemSnapshot from(RaidItem item) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		return new SettlementItemSnapshot(
				item.itemUid(),
				item.definitionId(),
				item.quantity(),
				item.totalValue());
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

	public long totalValue() {
		return totalValue;
	}

	public SettlementItemSnapshot copy() {
		return new SettlementItemSnapshot(
				itemUid,
				definitionId,
				quantity,
				totalValue);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(ITEM_UID, itemUid);
		bundle.put(DEFINITION_ID, definitionId);
		bundle.put(QUANTITY, quantity);
		bundle.put(TOTAL_VALUE, totalValue);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		SettlementItemSnapshot restored = new SettlementItemSnapshot(
				bundle.getString(ITEM_UID),
				bundle.getString(DEFINITION_ID),
				bundle.getInt(QUANTITY),
				bundle.getLong(TOTAL_VALUE));
		itemUid = restored.itemUid;
		definitionId = restored.definitionId;
		quantity = restored.quantity;
		totalValue = restored.totalValue;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}
}
