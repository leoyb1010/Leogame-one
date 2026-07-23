package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovEconomicItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovMissionArchive;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

/**
 * Transactional bridge between one host Heap item and the Bukov loot ledger.
 *
 * The host Item is never cloned. Its UID travels in Item's normal Bundle data,
 * while the checkpoint owns the exact carried host instance.
 */
public final class BukovHeapLootAdapter {

	public interface ItemEconomy {
		float unitWeight(Item item);
		int unitValue(Item item);
	}

	public interface DropGuard {
		boolean protectedWhileInUse(String itemUid);
	}

	public enum DropResult {
		DROPPED,
		PROTECTED_ITEM,
		IN_USE_ITEM,
		UNKNOWN_UID
	}

	private static final ItemEconomy LEGACY_DEFAULTS = new ItemEconomy() {
		@Override
		public float unitWeight(Item item) {
			BukovEconomicItem authored = authoredFirearmEconomy(item);
			return item instanceof BukovEconomicItem
					? ((BukovEconomicItem) item).bukovUnitWeight()
					: authored != null
					? authored.bukovUnitWeight()
					: 0.25f;
		}

		@Override
		public int unitValue(Item item) {
			BukovEconomicItem authored = authoredFirearmEconomy(item);
			return item instanceof BukovEconomicItem
					? ((BukovEconomicItem) item).bukovUnitValue()
					: authored != null
					? authored.bukovUnitValue()
					: Math.max(0, item.value());
		}
	};

	private final BukovRaidCoordinator raid;
	private final ItemEconomy economy;
	private DropGuard dropGuard;

	public BukovHeapLootAdapter(BukovRaidCoordinator raid) {
		this(raid, LEGACY_DEFAULTS);
	}

	public BukovHeapLootAdapter(
			BukovRaidCoordinator raid,
			ItemEconomy economy) {
		if (raid == null) {
			throw new IllegalArgumentException("raid is required");
		}
		if (economy == null) {
			throw new IllegalArgumentException("economy is required");
		}
		this.raid = raid;
		this.economy = economy;
	}

	public void dropGuard(DropGuard dropGuard) {
		this.dropGuard = dropGuard;
	}

	public LootTransaction.PickupResult pickupTop(Heap heap) {
		return pickupTop(heap, null);
	}

	public LootTransaction.PickupResult pickupTop(Heap heap, Hero hero) {
		if (heap == null || heap.peek() == null) {
			throw new IllegalArgumentException("non-empty heap is required");
		}
		Item item = heap.peek();
		String sourceKey = "heap-" + heap.pos;
		String uid = raid.checkpoint().itemUid(item, sourceKey);
		RaidItem raidItem = describe(uid, item);
		LootTransaction.PickupResult result = raid.pickup(raidItem);
		if (result != LootTransaction.PickupResult.ADDED) {
			return result;
		}

		Item runtimeItem;
		try {
			runtimeItem = runtimeHostItem(uid, item);
		} catch (RuntimeException e) {
			raid.drop(uid);
			throw e;
		}
		if (heap.peek() != item || !heap.items.removeFirstOccurrence(item)) {
			raid.drop(uid);
			throw new IllegalStateException("Heap changed during Bukov pickup");
		}
		try {
			raid.checkpoint().carryHostItem(uid, runtimeItem);
			attachRuntimeItem(hero, runtimeItem);
		} catch (RuntimeException e) {
			detachRuntimeItem(hero, runtimeItem);
			raid.checkpoint().releaseHostItem(uid);
			heap.items.addFirst(item);
			raid.drop(uid);
			throw e;
		}
		refreshAfterRemoval(heap);
		return result;
	}

	public DropResult drop(String itemUid, Heap heap) {
		return drop(itemUid, heap, null);
	}

	public DropResult drop(String itemUid, Heap heap, Hero hero) {
		if (itemUid == null || itemUid.trim().isEmpty()) {
			throw new IllegalArgumentException("itemUid is required");
		}
		if (heap == null) {
			throw new IllegalArgumentException("heap is required");
		}
		Item hostItem = raid.checkpoint().hostItem(itemUid);
		if (hostItem == null) {
			return DropResult.UNKNOWN_UID;
		}
		if (hostItem instanceof BukovMissionArchive) {
			return DropResult.PROTECTED_ITEM;
		}
		if (dropGuard != null && dropGuard.protectedWhileInUse(itemUid)) {
			return DropResult.IN_USE_ITEM;
		}
		RaidItem removed = raid.drop(itemUid);
		if (removed == null) {
			throw new IllegalStateException("Host item exists without ledger entry: " + itemUid);
		}

		// Avoid Heap.drop's stack merge: a Bukov UID identifies this exact stack.
		heap.hidden = false;
		heap.items.addFirst(hostItem);
		raid.checkpoint().releaseHostItem(itemUid);
		detachRuntimeItem(hero, hostItem);
		if (heap.sprite != null) {
			heap.sprite.view(heap).place(heap.pos);
		}
		return DropResult.DROPPED;
	}

	/** Reattaches checkpoint-owned consumables after a host save resume. */
	public void installCarriedRuntimeItems(Hero hero) {
		if (hero == null) return;
		for (RaidItem carried : raid.loot().items()) {
			Item host = raid.checkpoint().hostItem(carried.itemUid());
			if (host != null) {
				attachRuntimeItem(hero, host);
			}
		}
	}

	/**
	 * Writes quantities/durability consumed by the realtime host representation
	 * back to the authoritative raid ledger before checkpoint or settlement.
	 */
	public void syncRuntimeState(Hero hero) {
		for (RaidItem carried : raid.loot().items()) {
			Item host = raid.checkpoint().hostItem(carried.itemUid());
			if (host == null) continue;
			if (host instanceof BukovLootItem
					&& ((BukovLootItem)host).category()
							== BukovLootItem.Category.MEDICAL) {
				// The fixed-step medical controller owns these quantities.
				continue;
			}
			int quantity = host instanceof Firearm ? 1 : host.quantity();
			float durability = host instanceof Firearm
					? ((Firearm) host).durability()
					: carried.durability();
			if (quantity <= 0) {
				raid.drop(carried.itemUid());
				raid.checkpoint().releaseHostItem(carried.itemUid());
				detachRuntimeItem(hero, host);
			} else {
				raid.loot().replace(
						carried.withRuntimeState(quantity, durability));
			}
		}
	}

	public void reconcileConsumedRuntimeItem(String itemUid, Hero hero) {
		if (itemUid == null) return;
		Item host = raid.checkpoint().hostItem(itemUid);
		if (host == null) return;
		RaidItem carried = raid.loot().item(itemUid);
		if (carried == null) {
			raid.checkpoint().releaseHostItem(itemUid);
			detachRuntimeItem(hero, host);
		} else {
			host.quantity(carried.quantity());
		}
	}

	public Item carriedHostItem(String itemUid) {
		return raid.checkpoint().hostItem(itemUid);
	}

	private static void attachRuntimeItem(Hero hero, Item item) {
		if (hero == null || item == null || !usableDuringRaid(item)) return;
		if (item instanceof Firearm) {
			if (hero.belongings.weapon == item) {
				return;
			}
			if (hero.belongings.weapon == null) {
				hero.belongings.weapon = (Firearm)item;
				((Firearm)item).activate(hero);
				return;
			}
		}
		if (!hero.belongings.backpack.items.contains(item)) {
			hero.belongings.backpack.items.add(item);
		}
	}

	private static void detachRuntimeItem(Hero hero, Item item) {
		if (hero == null || item == null) return;
		if (hero.belongings.weapon == item) {
			hero.belongings.weapon = null;
		}
		hero.belongings.backpack.items.remove(item);
	}

	private static boolean usableDuringRaid(Item item) {
		if (item instanceof Firearm || item instanceof AmmoStack) {
			return true;
		}
		return item instanceof BukovLootItem
				&& ((BukovLootItem)item).category()
						== BukovLootItem.Category.MEDICAL;
	}

	private RaidItem describe(String uid, Item item) {
		float unitWeight = economy.unitWeight(item);
		int unitValue = economy.unitValue(item);
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				unitWeight) || unitWeight < 0f) {
			throw new IllegalArgumentException("unitWeight must be finite and non-negative");
		}
		if (unitValue < 0) {
			throw new IllegalArgumentException("unitValue must be non-negative");
		}
		return new RaidItem(
				uid,
				economicDefinitionId(item),
				item.quantity(),
				unitWeight,
				unitValue,
				true,
				false,
				1f);
	}

	private static String economicDefinitionId(Item item) {
		if (item instanceof BukovEconomicItem) {
			return ((BukovEconomicItem) item).bukovDefinitionId();
		}
		if (item instanceof Firearm) {
			return "firearm:" + ((Firearm) item).definitionId();
		}
		return item.getClass().getName();
	}

	private static BukovEconomicItem authoredFirearmEconomy(Item item) {
		if (!(item instanceof Firearm)) {
			return null;
		}
		Item authored = BukovFirstRaidLootTables.createByEconomicDefinitionId(
				"firearm:" + ((Firearm) item).definitionId());
		return authored instanceof BukovEconomicItem
				? (BukovEconomicItem) authored
				: null;
	}

	/**
	 * Authored firearm loot uses the generic economic item only while lying on
	 * the map. Crossing the pickup transaction boundary materializes the exact
	 * host Firearm instance that is then checkpointed, equipped and dropped.
	 */
	private static Item runtimeHostItem(String uid, Item source) {
		if (!(source instanceof BukovEconomicItem)) {
			return source;
		}
		String definitionId =
				((BukovEconomicItem) source).bukovDefinitionId();
		if (definitionId == null || !definitionId.startsWith("firearm:")) {
			return source;
		}
		String firearmId = definitionId.substring("firearm:".length());
		if (firearmId.isEmpty()) {
			throw new IllegalStateException("Firearm loot definition is empty");
		}
		Firearm firearm = new Firearm().configure(
				firearmId,
				uid,
				0,
				null);
		firearm.assignBukovItemUid(uid);
		return firearm;
	}

	private static void refreshAfterRemoval(Heap heap) {
		if (heap.items.isEmpty()) {
			if (Dungeon.level != null
					&& Dungeon.level.heaps.get(heap.pos) == heap) {
				heap.destroy();
			} else if (heap.sprite != null) {
				heap.sprite.kill();
			}
		} else if (heap.sprite != null) {
			heap.sprite.view(heap).place(heap.pos);
		}
	}
}
