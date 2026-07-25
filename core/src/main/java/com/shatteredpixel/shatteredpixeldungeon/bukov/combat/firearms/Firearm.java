package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

public class Firearm extends Weapon {

	private static final String DEF = "definition_id";
	private static final String UID = "item_uid";
	private static final String MAG = "magazine_ammo";
	private static final String LOADED_AMMO = "loaded_ammo_definition_id";
	private static final String DURABILITY = "durability";
	private static final String HEAT = "heat";
	private static final String FOULING = "fouling";
	private static final String ATTACHMENT_BUILD = "attachment_build";

	private String definitionId;
	private String itemUid;
	private int magazineAmmo;
	private String loadedAmmoDefinitionId;
	private float durability = 1f;
	private float heat;
	private float fouling;
	private FirearmBuild attachmentBuild;

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
		if (this.itemUid != null && !this.itemUid.equals(itemUid)) {
			attachmentBuild = null;
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
		FirearmDefinition base = registry.require(definitionId);
		if (attachmentBuild == null) return base;
		EffectiveFirearmStats effective =
				attachmentBuild.effectiveStats(base);
		FirearmDefinition result = copyDefinition(base);
		result.damage = effective.damage;
		result.penetration = effective.penetration;
		result.rpm = effective.rpm;
		result.magazineSize = effective.magazineSize;
		result.reloadSeconds = effective.reloadSeconds;
		result.effectiveRangeTiles = effective.effectiveRangeTiles;
		result.baseSpreadDeg = effective.baseSpreadDeg;
		result.movingSpreadDeg = effective.movingSpreadDeg;
		result.recoilPerShot = effective.recoilPerShot;
		result.noiseRadiusTiles = effective.noiseRadiusTiles;
		result.weightKg = effective.weightKg;
		// Validate the fully materialized definition, not only the registry
		// base. This keeps future attachment data from publishing impossible
		// magazine, reload, presentation or audio values into live combat.
		result.validate();
		return result;
	}

	public void applyBuild(FirearmBuild build) {
		if (build == null) {
			attachmentBuild = null;
			return;
		}
		if (itemUid == null || !itemUid.equals(build.firearmUid())) {
			throw new IllegalArgumentException(
					"firearm build UID does not match runtime firearm");
		}
		attachmentBuild = build.copy();
	}

	public FirearmBuild attachmentBuild() {
		return attachmentBuild == null ? null : attachmentBuild.copy();
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
		if (!BukovNumbers.isFinite(value)) {
			throw new IllegalArgumentException("durability must be finite");
		}
		durability = unit(value);
	}

	/**
	 * Commits the persistent condition cost of one live round. Heat cools
	 * continuously, while fouling and durability are written back with the
	 * firearm so two otherwise identical weapons can age differently.
	 */
	public void recordShot(FirearmDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		float heatGain = 0.035f + Math.min(
				0.055f,
				definition.recoilPerShot * 0.004f);
		heat = unit(heat + heatGain);
		fouling = unit(fouling + 0.0015f
				+ definition.pellets * 0.00025f);
		durability = unit(durability
				- 0.00025f
				- heat * 0.00030f);
	}

	public void cool(float elapsedSeconds) {
		if (Float.isNaN(elapsedSeconds)
				|| Float.isInfinite(elapsedSeconds)
				|| elapsedSeconds < 0f) {
			throw new IllegalArgumentException(
					"elapsedSeconds must be finite and non-negative");
		}
		heat = Math.max(0f, heat - elapsedSeconds * 0.22f);
	}

	public float heat() {
		return heat;
	}

	public float fouling() {
		return fouling;
	}

	public void setCondition(float heat, float fouling) {
		if (Float.isNaN(heat)
				|| Float.isInfinite(heat)
				|| Float.isNaN(fouling)
				|| Float.isInfinite(fouling)) {
			throw new IllegalArgumentException(
					"condition values must be finite");
		}
		this.heat = unit(heat);
		this.fouling = unit(fouling);
	}

	/**
	 * Accuracy penalty consumed by the production hitscan path. This keeps
	 * condition readable without introducing a surprise random hard jam.
	 */
	public float conditionSpreadPenaltyDeg() {
		return fouling * 0.9f
				+ Math.max(0f, heat - 0.50f) * 2.5f
				+ (1f - durability) * 1.2f;
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
		bundle.put(HEAT, heat);
		bundle.put(FOULING, fouling);
		if (attachmentBuild != null) {
			bundle.put(ATTACHMENT_BUILD, attachmentBuild);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		definitionId = bundle.getString(DEF);
		itemUid = bundle.getString(UID);
		magazineAmmo = Math.max(0, bundle.getInt(MAG));
		loadedAmmoDefinitionId = bundle.getString(LOADED_AMMO);
		durability = bundle.contains(DURABILITY)
				? unitOrDefault(bundle.getFloat(DURABILITY), 1f)
				: 1f;
		heat = bundle.contains(HEAT)
				? unitOrDefault(bundle.getFloat(HEAT), 0f)
				: 0f;
		fouling = bundle.contains(FOULING)
				? unitOrDefault(bundle.getFloat(FOULING), 0f)
				: 0f;
		if (bundle.contains(ATTACHMENT_BUILD)) {
			com.watabou.utils.Bundlable restoredBuild =
					bundle.get(ATTACHMENT_BUILD);
			if (!(restoredBuild instanceof FirearmBuild)) {
				throw new IllegalStateException(
						"Invalid firearm attachment build");
			}
			applyBuild((FirearmBuild) restoredBuild);
		} else {
			attachmentBuild = null;
		}
	}

	private static float unit(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	private static float unitOrDefault(float value, float fallback) {
		return BukovNumbers.isFinite(value) ? unit(value) : fallback;
	}

	private static FirearmDefinition copyDefinition(FirearmDefinition source) {
		FirearmDefinition result = new FirearmDefinition();
		result.id = source.id;
		result.name = source.name;
		result.weaponClass = source.weaponClass;
		result.caliber = source.caliber;
		result.defaultAmmo = source.defaultAmmo;
		result.fireMode = source.fireMode;
		result.damage = source.damage;
		result.penetration = source.penetration;
		result.rpm = source.rpm;
		result.magazineSize = source.magazineSize;
		result.reloadSeconds = source.reloadSeconds;
		result.effectiveRangeTiles = source.effectiveRangeTiles;
		result.baseSpreadDeg = source.baseSpreadDeg;
		result.movingSpreadDeg = source.movingSpreadDeg;
		result.recoilPerShot = source.recoilPerShot;
		result.recoilRecovery = source.recoilRecovery;
		result.pellets = source.pellets;
		result.noiseRadiusTiles = source.noiseRadiusTiles;
		result.weightKg = source.weightKg;
		result.value = source.value;
		result.feedbackProfile = source.feedbackProfile;
		result.soundPitch = source.soundPitch;
		result.soundGain = source.soundGain;
		// Attachments alter ballistics, not the authored weapon timbre or
		// reload cue timeline. Preserve the registry profile on the effective
		// definition so a suppressed rifle cannot silently become a pistol.
		result.audioProfile = source.audioProfile;
		result.muzzleIntensity = source.muzzleIntensity;
		result.tracerIntensity = source.tracerIntensity;
		result.impactIntensity = source.impactIntensity;
		result.feedbackIntensity = source.feedbackIntensity;
		return result;
	}
}
