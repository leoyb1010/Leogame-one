package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovOperator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovFirstRaidLootTables;
import com.shatteredpixel.shatteredpixeldungeon.bukov.content.BukovLootItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge from deployed RaidItem instances to host runtime Items.
 *
 * Lifecycle:
 * materialize() after a coordinator starts/resumes, installOn() after the host
 * Hero exists, and writeBack() before settlement or checkpoint persistence.
 * The durable raid ledger remains the source of truth across save/resume.
 */
public final class BukovRuntimeLoadoutAdapter {

	private static final String FIREARM_PREFIX = "firearm:";
	private static final String AMMO_PREFIX = "ammo:";

	public static final class RuntimeLoadout {
		private final String raidId;
		private final Firearm primaryWeapon;
		private final List<Firearm> secondaryWeapons;
		private final List<AmmoStack> reserveAmmo;
		private final List<Item> supplies;
		private final Map<String, Binding> bindings;
		private final FirearmRegistry firearms;
		private final BukovRaidCheckpoint checkpoint;
		private final BukovEquippedGear equippedGear;

		private RuntimeLoadout(
				String raidId,
				Firearm primaryWeapon,
				List<Firearm> secondaryWeapons,
				List<AmmoStack> reserveAmmo,
				List<Item> supplies,
				Map<String, Binding> bindings,
				FirearmRegistry firearms,
				BukovRaidCheckpoint checkpoint,
				BukovEquippedGear equippedGear) {
			this.raidId = raidId;
			this.primaryWeapon = primaryWeapon;
			this.secondaryWeapons = Collections.unmodifiableList(
					new ArrayList<>(secondaryWeapons));
			this.reserveAmmo = Collections.unmodifiableList(
					new ArrayList<>(reserveAmmo));
			this.supplies = Collections.unmodifiableList(
					new ArrayList<>(supplies));
			this.bindings = new LinkedHashMap<>(bindings);
			this.firearms = firearms;
			this.checkpoint = checkpoint;
			this.equippedGear = equippedGear;
		}

		public Firearm primaryWeapon() {
			return primaryWeapon;
		}

		public List<Firearm> secondaryWeapons() {
			return secondaryWeapons;
		}

		public List<AmmoStack> reserveAmmo() {
			return reserveAmmo;
		}

		public List<Item> supplies() {
			return supplies;
		}

		public List<Item> allHostItems() {
			List<Item> result = new ArrayList<>();
			if (primaryWeapon != null) {
				result.add(primaryWeapon);
			}
			result.addAll(secondaryWeapons);
			result.addAll(reserveAmmo);
			result.addAll(supplies);
			return Collections.unmodifiableList(result);
		}

		public BukovEquippedGear equippedGear() {
			return equippedGear;
		}

		/**
		 * Replaces every legacy dungeon item with exactly this deployed raid
		 * loadout. Repeated installation is safe and does not mutate the raid
		 * ledger or grant fallback equipment.
		 */
		public void installOn(Hero hero) {
			BukovOperator.sanitizeHostHero(hero);
			if (primaryWeapon != null) {
				hero.belongings.weapon = primaryWeapon;
				primaryWeapon.activate(hero);
				Dungeon.quickslot.setSlot(0, primaryWeapon);
			}
			for (Firearm firearm : secondaryWeapons) {
				addToHostBackpack(hero, firearm);
			}
			for (AmmoStack ammo : reserveAmmo) {
				addToHostBackpack(hero, ammo);
			}
			for (Item supply : supplies) {
				addToHostBackpack(hero, supply);
			}
		}

		public void writeBack(LootTransaction ledger) {
			if (ledger == null || !raidId.equals(ledger.raidId())) {
				throw new IllegalArgumentException("matching raid ledger is required");
			}
			for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
				String itemUid = entry.getKey();
				Binding binding = entry.getValue();
				if (!ledger.contains(itemUid)) {
					continue;
				}
				int quantity = binding.host.quantity();
				float durability = binding.original.durability();
				float fouling = binding.original.fouling();
				if (binding.host instanceof Firearm) {
					quantity = 1;
					durability = ((Firearm) binding.host).durability();
					fouling = ((Firearm) binding.host).fouling();
				}

				if (quantity <= 0) {
					ledger.drop(itemUid);
				} else {
					ledger.replace(
							binding.original.withRuntimeState(
									quantity,
									durability,
									fouling));
				}
			}
			refundMagazine(ledger, primaryWeapon);
			for (Firearm secondary : secondaryWeapons) {
				refundMagazine(ledger, secondary);
			}
			// Host supply Items do not know ballistic durability. Publish the
			// dedicated armor state last so generic supply writeback cannot
			// overwrite its condition with the deployment-time value.
			equippedGear.writeBack(ledger);
		}

		private void refundMagazine(
				LootTransaction ledger,
				Firearm firearm) {
			if (firearm == null || firearm.magazineAmmo() <= 0) {
				return;
			}
			String loadedDefinitionId = firearm.loadedAmmoDefinitionId(
					firearm.definition(firearms));
			for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
				Binding binding = entry.getValue();
				if (!(binding.host instanceof AmmoStack)
						|| !loadedDefinitionId.equals(
								((AmmoStack)binding.host).definitionId())) {
					continue;
				}
				RaidItem current = ledger.item(entry.getKey());
				int quantity = (current == null
						? binding.host.quantity()
						: current.quantity())
						+ firearm.magazineAmmo();
				RaidItem updated = binding.original.withRuntimeState(
						quantity,
						binding.original.durability());
				if (ledger.contains(entry.getKey())) {
					ledger.replace(updated);
				} else if (ledger.pickup(updated)
						!= LootTransaction.PickupResult.ADDED) {
					throw new IllegalStateException(
							"Unable to restore loaded ammunition to raid ledger");
				}
				if (checkpoint != null
						&& checkpoint.hostItem(entry.getKey()) == null) {
					checkpoint.carryHostItem(
							entry.getKey(),
							binding.host);
				}
				return;
			}
			throw new IllegalStateException(
					"No carried ammunition stack matches loaded variant "
							+ loadedDefinitionId);
		}

		private static void addToHostBackpack(Hero hero, Item item) {
			if (!hero.belongings.backpack.items.contains(item)) {
				// The raid ledger already enforces weight. Host Bag capacity is
				// deliberately not a second, conflicting inventory limit.
				hero.belongings.backpack.items.add(item);
			}
		}
	}

	private static final class Binding {
		private final RaidItem original;
		private final Item host;

		private Binding(RaidItem original, Item host) {
			this.original = original;
			this.host = host;
		}
	}

	private final FirearmRegistry firearms;
	private final AmmoRegistry ammunition;

	public BukovRuntimeLoadoutAdapter(
			FirearmRegistry firearms,
			AmmoRegistry ammunition) {
		if (firearms == null || ammunition == null) {
			throw new IllegalArgumentException("firearm and ammunition registries are required");
		}
		this.firearms = firearms;
		this.ammunition = ammunition;
		firearms.validateAmmunition(ammunition);
	}

	public RuntimeLoadout materialize(LootTransaction ledger) {
		return materialize(ledger, null, null);
	}

	/**
	 * Materializes a live loadout owned by the raid checkpoint.
	 *
	 * This is the production entry point: every deployed host item is indexed
	 * by the same physical UID used by found-in-raid loot. On resume the exact
	 * checkpoint host instances are reused, preserving firearm magazine and
	 * loaded-ammunition state instead of reconstructing a fresh weapon.
	 */
	public RuntimeLoadout materialize(BukovRaidCoordinator raid) {
		if (raid == null) {
			throw new IllegalArgumentException("raid is required");
		}
		return materialize(
				raid.loot(),
				raid.checkpoint(),
				raid.profile().firearmBuilds());
	}

	private RuntimeLoadout materialize(
			LootTransaction ledger,
			BukovRaidCheckpoint checkpoint,
			BukovFirearmBuilds firearmBuilds) {
		if (ledger == null) {
			throw new IllegalArgumentException("ledger is required");
		}
		Firearm primary = null;
		FirearmDefinition primaryDefinition = null;
		boolean primaryRestored = false;
		List<Firearm> secondary = new ArrayList<>();
		List<AmmoStack> reserve = new ArrayList<>();
		List<Item> supplies = new ArrayList<>();
		Map<String, Binding> bindings = new LinkedHashMap<>();
		String preferredPrimaryUid = checkpoint == null
				? null : checkpoint.equippedFirearmUid();

		for (RaidItem item : ledger.items()) {
			if (item.foundInRaid()
					&& !BukovActiveRaidRecovery
							.disposableEmergencyItem(item)) {
				continue;
			}
			String firearmId = firearmId(item.definitionId());
			if (firearmId != null) {
				Item restored = checkpoint == null
						? null : checkpoint.hostItem(item.itemUid());
				Firearm firearm;
				boolean restoredFromCheckpoint = restored != null;
				if (restoredFromCheckpoint) {
					if (!(restored instanceof Firearm)) {
						throw hostTypeMismatch(item, restored, "firearm");
					}
					firearm = (Firearm) restored;
					if (!firearmId.equals(firearm.definitionId())) {
						throw new IllegalStateException(
								"Checkpoint firearm definition mismatch for "
										+ item.itemUid());
					}
				} else {
					FirearmDefinition definition = firearms.require(firearmId);
					firearm = new Firearm().configure(
							firearmId,
							item.itemUid(),
							0,
							definition.defaultAmmo);
					firearm.setDurability(item.durability());
					firearm.setCondition(0f, item.fouling());
					registerHost(checkpoint, item, firearm);
				}
				if (firearmBuilds != null) {
					com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms
							.FirearmBuild build =
							firearmBuilds.build(item.itemUid());
					if (build != null) firearm.applyBuild(build);
				}
				bind(item, firearm, bindings);
				boolean preferred = item.itemUid().equals(
						preferredPrimaryUid);
				if (primary == null || preferred) {
					if (primary != null) {
						secondary.add(primary);
					}
					primary = firearm;
					primaryDefinition = firearm.definition(firearms);
					primaryRestored = restoredFromCheckpoint;
				} else {
					secondary.add(firearm);
				}
				continue;
			}

			String ammoId = ammoId(item.definitionId());
			if (ammoId != null) {
				AmmoDefinition definition = ammunition.require(ammoId);
				Item restored = checkpoint == null
						? null : checkpoint.hostItem(item.itemUid());
				AmmoStack stack;
				if (restored != null) {
					if (!(restored instanceof AmmoStack)) {
						throw hostTypeMismatch(item, restored, "ammunition");
					}
					stack = (AmmoStack) restored;
					if (!ammoId.equals(stack.definitionId())) {
						throw new IllegalStateException(
								"Checkpoint ammunition definition mismatch for "
										+ item.itemUid());
					}
				} else {
					stack = new AmmoStack().configure(
							definition.id,
							item.quantity(),
							item.unitWeight(),
							item.unitValue());
					registerHost(checkpoint, item, stack);
				}
				bind(item, stack, bindings);
				reserve.add(stack);
				continue;
			}

			Item supply = checkpoint == null
					? null : checkpoint.hostItem(item.itemUid());
			if (supply == null) {
				supply = supply(item);
				registerHost(checkpoint, item, supply);
			}
			bind(item, supply, bindings);
			supplies.add(supply);
		}

		if (primary != null) {
			AmmoStack source = selectMagazineSource(
					reserve,
					primaryDefinition);
			if (source != null) {
				if (!primaryRestored) {
					int loaded = source.takeUpTo(primaryDefinition.magazineSize);
					primary.loadRounds(
							source.definitionId(),
							loaded,
							primaryDefinition);
				}
			}
		}
		if (checkpoint != null) {
			checkpoint.setEquippedFirearmUid(
					primary == null ? null : primary.bukovItemUid());
		}
		for (int i = reserve.size() - 1; i >= 0; i--) {
			if (reserve.get(i).quantity() <= 0) {
				reserve.remove(i);
			}
		}
		return new RuntimeLoadout(
				ledger.raidId(),
				primary,
				secondary,
				reserve,
				supplies,
				bindings,
				firearms,
				checkpoint,
				BukovEquippedGear.from(ledger.items()));
	}

	private static IllegalStateException hostTypeMismatch(
			RaidItem item,
			Item restored,
			String expected) {
		return new IllegalStateException(
				"Checkpoint host type mismatch for "
						+ item.itemUid()
						+ ": expected "
						+ expected
						+ ", found "
						+ restored.getClass().getName());
	}

	private static void registerHost(
			BukovRaidCheckpoint checkpoint,
			RaidItem item,
			Item host) {
		if (checkpoint == null) {
			return;
		}
		host.assignBukovItemUid(item.itemUid());
		checkpoint.carryHostItem(item.itemUid(), host);
	}

	private String firearmId(String storedDefinitionId) {
		String candidate = storedDefinitionId.startsWith(FIREARM_PREFIX)
				? storedDefinitionId.substring(FIREARM_PREFIX.length())
				: storedDefinitionId;
		for (FirearmDefinition definition : firearms.all()) {
			if (definition.id.equals(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private String ammoId(String storedDefinitionId) {
		String candidate = storedDefinitionId.startsWith(AMMO_PREFIX)
				? storedDefinitionId.substring(AMMO_PREFIX.length())
				: storedDefinitionId;
		return ammunition.find(candidate) == null ? null : candidate;
	}

	private static Item supply(RaidItem item) {
		Item authored = BukovFirstRaidLootTables.createByEconomicDefinitionId(
				item.definitionId());
		BukovLootItem.Category category = authored instanceof BukovLootItem
				? ((BukovLootItem) authored).category()
				: BukovLootItem.Category.LOOT;
		String displayName = authored == null
				? item.definitionId()
				: authored.name();
		return new BukovLootItem().configure(
				item.definitionId(),
				displayName,
				category,
				item.unitWeight(),
				item.unitValue()).quantity(item.quantity());
	}

	private AmmoStack selectMagazineSource(
			List<AmmoStack> reserve,
			FirearmDefinition firearm) {
		AmmoStack compatible = null;
		for (AmmoStack stack : reserve) {
			if (firearm.defaultAmmo.equals(stack.definitionId())
					&& ammunition.compatible(
							stack.definitionId(),
							firearm.caliber)) {
				return stack;
			}
		}
		for (AmmoStack stack : reserve) {
			if (compatible == null
					&& ammunition.compatible(
							stack.definitionId(),
							firearm.caliber)) {
				compatible = stack;
			}
		}
		return compatible;
	}

	private static void bind(
			RaidItem item,
			Item host,
			Map<String, Binding> bindings) {
		host.assignBukovItemUid(item.itemUid());
		if (bindings.put(item.itemUid(), new Binding(item, host)) != null) {
			throw new IllegalStateException("Duplicate runtime UID: " + item.itemUid());
		}
	}
}
