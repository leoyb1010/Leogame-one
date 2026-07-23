package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

public class Firearm extends Weapon {

	private static final String DEF = "definition_id";
	private static final String UID = "item_uid";
	private static final String MAG = "magazine_ammo";
	private static final String LOADED_AMMO = "loaded_ammo_definition_id";
	private static final String DURABILITY = "durability";

	private String definitionId;
	private String itemUid;
	private int magazineAmmo;
	private String loadedAmmoDefinitionId;
	private float durability = 1f;

	{
		// ponytail: Reuse the host crossbow icon until dedicated firearm art lands.
		image = ItemSpriteSheet.CROSSBOW;
	}

	public Firearm configure(String definitionId, String itemUid, int magazineAmmo) {
		return configure(definitionId, itemUid, magazineAmmo, null);
	}

	public Firearm configure(
			String definitionId,
			String itemUid,
			int magazineAmmo,
			String loadedAmmoDefinitionId) {
		if (definitionId == null || definitionId.isEmpty()) {
			throw new IllegalArgumentException("definitionId is required");
		}
		if (itemUid == null || itemUid.isEmpty()) {
			throw new IllegalArgumentException("itemUid is required");
		}
		this.definitionId = definitionId;
		this.itemUid = itemUid;
		this.magazineAmmo = Math.max(0, magazineAmmo);
		this.loadedAmmoDefinitionId = loadedAmmoDefinitionId;
		return this;
	}

	public FirearmDefinition definition(FirearmRegistry registry) {
		if (registry == null) {
			throw new IllegalArgumentException("registry is required");
		}
		return registry.require(definitionId);
	}

	public String definitionId() {
		return definitionId;
	}

	public String itemUid() {
		return itemUid;
	}

	public boolean hasRound() {
		return magazineAmmo > 0;
	}

	public boolean consumeRound() {
		if (magazineAmmo <= 0) {
			return false;
		}
		magazineAmmo--;
		return true;
	}

	public int magazineAmmo() {
		return magazineAmmo;
	}

	public String loadedAmmoDefinitionId(FirearmDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		return loadedAmmoDefinitionId == null || loadedAmmoDefinitionId.isEmpty()
				? definition.defaultAmmo
				: loadedAmmoDefinitionId;
	}

	public int loadRounds(
			String ammoDefinitionId,
			int amount,
			FirearmDefinition definition) {
		if (ammoDefinitionId == null || ammoDefinitionId.isEmpty() || amount < 0) {
			throw new IllegalArgumentException("valid ammunition and amount are required");
		}
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		String loadedDefinition = loadedAmmoDefinitionId(definition);
		if (magazineAmmo > 0 && !loadedDefinition.equals(ammoDefinitionId)) {
			throw new IllegalArgumentException("cannot mix ammunition variants in one magazine");
		}
		int loaded = Math.min(
				amount,
				Math.max(0, definition.magazineSize - magazineAmmo));
		if (loaded > 0) {
			loadedAmmoDefinitionId = ammoDefinitionId;
			magazineAmmo += loaded;
		}
		return loaded;
	}

	@Override
	public String status() {
		return Integer.toString(magazineAmmo);
	}

	public void setMagazineAmmo(int value, FirearmDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		magazineAmmo = Math.max(0, Math.min(value, definition.magazineSize));
	}

	public float durability() {
		return durability;
	}

	public void setDurability(float value) {
		durability = Math.max(0f, Math.min(1f, value));
	}

	@Override
	public int min(int level) {
		// Ballistic damage is resolved by RealtimeDamage, not the legacy melee path.
		return 1;
	}

	@Override
	public int max(int level) {
		return 1;
	}

	@Override
	public int STRReq(int level) {
		// Keeps compatibility with Hero's legacy weapon defense calculation.
		return 8;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEF, definitionId);
		bundle.put(UID, itemUid);
		bundle.put(MAG, magazineAmmo);
		bundle.put(LOADED_AMMO, loadedAmmoDefinitionId);
		bundle.put(DURABILITY, durability);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		definitionId = bundle.getString(DEF);
		itemUid = bundle.getString(UID);
		magazineAmmo = Math.max(0, bundle.getInt(MAG));
		loadedAmmoDefinitionId = bundle.getString(LOADED_AMMO);
		durability = bundle.contains(DURABILITY)
				? Math.max(0f, Math.min(1f, bundle.getFloat(DURABILITY)))
				: 1f;
	}
}
