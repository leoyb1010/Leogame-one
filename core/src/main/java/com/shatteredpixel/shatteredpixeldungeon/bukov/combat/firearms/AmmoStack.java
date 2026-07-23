package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;

public class AmmoStack extends Item implements BukovEconomicItem {

	private static final String DEFINITION_ID = "definition_id";
	private static final String LEGACY_AMMO_ID = "ammo_id";
	private static final String UNIT_WEIGHT = "unit_weight";
	private static final String UNIT_VALUE = "unit_value";

	private String definitionId;
	private float unitWeight = 0.02f;
	private int unitValue = 8;

	public AmmoStack() {
		stackable = true;
	}

	public AmmoStack configure(String definitionId, int amount) {
		return configure(definitionId, amount, 0.02f, 8);
	}

	public AmmoStack configure(
			String definitionId,
			int amount,
			float unitWeight,
			int unitValue) {
		if (definitionId == null || definitionId.isEmpty()) {
			throw new IllegalArgumentException("ammunition definition id is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(unitWeight) || unitWeight < 0f || unitValue < 0) {
			throw new IllegalArgumentException("invalid ammo economy");
		}
		this.definitionId = definitionId;
		this.unitWeight = unitWeight;
		this.unitValue = unitValue;
		quantity(Math.max(1, amount));
		return this;
	}

	public AmmoStack configure(AmmoDefinition definition, int amount) {
		if (definition == null) {
			throw new IllegalArgumentException("ammunition definition is required");
		}
		definition.validate();
		return configure(definition.id, amount, definition.weightKg, definition.value);
	}

	public String definitionId() {
		return definitionId;
	}

	/** Compatibility alias for saves and callers authored before ammo variants. */
	@Deprecated
	public String ammoId() {
		return definitionId;
	}

	@Override
	public String bukovDefinitionId() {
		return "ammo:" + definitionId;
	}

	@Override
	public float bukovUnitWeight() {
		return unitWeight;
	}

	@Override
	public int bukovUnitValue() {
		return unitValue;
	}

	public int takeUpTo(int requested) {
		int taken = Math.min(quantity(), Math.max(0, requested));
		quantity(quantity() - taken);
		return taken;
	}

	@Override
	public boolean isSimilar(Item item) {
		return item instanceof AmmoStack
				&& definitionId != null
				&& definitionId.equals(((AmmoStack)item).definitionId)
				&& uidsCanMerge(item);
	}

	private boolean uidsCanMerge(Item other) {
		return bukovItemUid() == null
				|| other.bukovItemUid() == null
				|| bukovItemUid().equals(other.bukovItemUid());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEFINITION_ID, definitionId);
		bundle.put(UNIT_WEIGHT, unitWeight);
		bundle.put(UNIT_VALUE, unitValue);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		definitionId = bundle.contains(DEFINITION_ID)
				? bundle.getString(DEFINITION_ID)
				: bundle.getString(LEGACY_AMMO_ID);
		if (bundle.contains(UNIT_WEIGHT)) {
			unitWeight = bundle.getFloat(UNIT_WEIGHT);
		}
		if (bundle.contains(UNIT_VALUE)) {
			unitValue = bundle.getInt(UNIT_VALUE);
		}
		stackable = true;
	}
}
