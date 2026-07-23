package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;

/** One data-driven host Item class for first-raid loot, medicine and tools. */
public final class BukovLootItem extends Item implements BukovEconomicItem {

	public enum Category {
		LOOT,
		MEDICAL,
		TOOL,
		HIGH_VALUE,
		BOSS
	}

	private static final String DEFINITION_ID = "bukov_definition_id";
	private static final String DISPLAY_NAME = "bukov_display_name";
	private static final String CATEGORY = "bukov_category";
	private static final String UNIT_WEIGHT = "bukov_unit_weight";
	private static final String UNIT_VALUE = "bukov_unit_value";

	private String definitionId;
	private String displayName;
	private Category category;
	private float unitWeight;
	private int unitValue;

	public BukovLootItem() {
		stackable = true;
	}

	public BukovLootItem configure(
			String definitionId,
			String displayName,
			Category category,
			float unitWeight,
			int unitValue) {
		if (definitionId == null || definitionId.trim().isEmpty()) {
			throw new IllegalArgumentException("definitionId is required");
		}
		if (displayName == null || displayName.trim().isEmpty()) {
			throw new IllegalArgumentException("displayName is required");
		}
		if (category == null) {
			throw new IllegalArgumentException("category is required");
		}
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				unitWeight) || unitWeight < 0f) {
			throw new IllegalArgumentException("unitWeight must be finite and non-negative");
		}
		if (unitValue < 0) {
			throw new IllegalArgumentException("unitValue must be non-negative");
		}
		this.definitionId = definitionId;
		this.displayName = displayName;
		this.category = category;
		this.unitWeight = unitWeight;
		this.unitValue = unitValue;
		return this;
	}

	public Category category() {
		return category;
	}

	@Override
	public String bukovDefinitionId() {
		return definitionId;
	}

	@Override
	public float bukovUnitWeight() {
		return unitWeight;
	}

	@Override
	public int bukovUnitValue() {
		return unitValue;
	}

	@Override
	public String name() {
		return displayName == null ? "未配置战利品" : displayName;
	}

	@Override
	public String desc() {
		return name();
	}

	@Override
	public int value() {
		return unitValue;
	}

	@Override
	public boolean isSimilar(Item item) {
		return item instanceof BukovLootItem
				&& definitionId != null
				&& definitionId.equals(((BukovLootItem) item).definitionId)
				&& (bukovItemUid() == null
				|| item.bukovItemUid() == null
				|| bukovItemUid().equals(item.bukovItemUid()));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEFINITION_ID, definitionId);
		bundle.put(DISPLAY_NAME, displayName);
		bundle.put(CATEGORY, category);
		bundle.put(UNIT_WEIGHT, unitWeight);
		bundle.put(UNIT_VALUE, unitValue);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		configure(
				bundle.getString(DEFINITION_ID),
				bundle.getString(DISPLAY_NAME),
				bundle.getEnum(CATEGORY, Category.class),
				bundle.getFloat(UNIT_WEIGHT),
				bundle.getInt(UNIT_VALUE));
	}
}
