package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/** Keeps automatic pickup limited to ammunition and lightweight objectives. */
public final class BukovAutoPickupPolicy {

	static final float MAXIMUM_TASK_ITEM_WEIGHT = 0.25f;

	private BukovAutoPickupPolicy() {
	}

	public static boolean shouldPickup(Item item) {
		if (item instanceof AmmoStack
				|| item instanceof BukovMissionArchive) {
			return true;
		}
		if (!(item instanceof BukovEconomicItem)) {
			return false;
		}
		BukovEconomicItem economic = (BukovEconomicItem)item;
		String definitionId = economic.bukovDefinitionId();
		return definitionId != null
				&& definitionId.startsWith("key:")
				&& economic.bukovUnitWeight()
						<= MAXIMUM_TASK_ITEM_WEIGHT;
	}
}
