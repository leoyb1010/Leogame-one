package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent three-slot build bound to a physical firearm UID. */
public final class FirearmBuild implements Bundlable {

	private static final String FIREARM_UID = "firearm_uid";
	private static final String ATTACHMENTS = "attachments";

	private String firearmUid;
	private final EnumMap<FirearmAttachmentSlot, String> attachments =
			new EnumMap<>(FirearmAttachmentSlot.class);

	public FirearmBuild() {
		// Required by Bundle reflection.
	}

	public FirearmBuild(String firearmUid) {
		this.firearmUid = requireId(firearmUid, "firearmUid");
	}

	public String firearmUid() {
		return firearmUid;
	}

	public void install(String attachmentId) {
		FirearmAttachmentDefinition definition =
				FirearmAttachmentCatalog.require(attachmentId);
		attachments.put(definition.slot, definition.id);
	}

	public String remove(FirearmAttachmentSlot slot) {
		if (slot == null) throw new IllegalArgumentException("slot is required");
		return attachments.remove(slot);
	}

	public String attachment(FirearmAttachmentSlot slot) {
		return attachments.get(slot);
	}

	public Map<FirearmAttachmentSlot, String> attachments() {
		return Collections.unmodifiableMap(
				new LinkedHashMap<>(attachments));
	}

	public EffectiveFirearmStats effectiveStats(FirearmDefinition firearm) {
		if (firearm == null) {
			throw new IllegalArgumentException("firearm is required");
		}
		firearm.validate();
		float damage = firearm.damage;
		float range = firearm.effectiveRangeTiles;
		float baseSpread = firearm.baseSpreadDeg;
		float movingSpread = firearm.movingSpreadDeg;
		float recoil = firearm.recoilPerShot;
		float reload = firearm.reloadSeconds;
		float magazine = firearm.magazineSize;
		float noise = firearm.noiseRadiusTiles;
		float weight = firearm.weightKg;
		for (String attachmentId : attachments.values()) {
			FirearmAttachmentDefinition part =
					FirearmAttachmentCatalog.require(attachmentId);
			damage *= part.damageMultiplier;
			range *= part.rangeMultiplier;
			baseSpread *= part.baseSpreadMultiplier;
			movingSpread *= part.movingSpreadMultiplier;
			recoil *= part.recoilMultiplier;
			reload *= part.reloadMultiplier;
			magazine *= part.magazineMultiplier;
			noise *= part.noiseMultiplier;
			weight += part.addedWeightKg;
		}
		return new EffectiveFirearmStats(
				damage,
				firearm.penetration,
				firearm.rpm,
				Math.max(1, Math.round(magazine)),
				reload,
				range,
				baseSpread,
				movingSpread,
				recoil,
				noise,
				weight);
	}

	public FirearmBuild copy() {
		FirearmBuild result = new FirearmBuild(firearmUid);
		result.attachments.putAll(attachments);
		return result;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(FIREARM_UID, firearmUid);
		String[] stored = new String[attachments.size()];
		int index = 0;
		for (Map.Entry<FirearmAttachmentSlot, String> entry
				: attachments.entrySet()) {
			stored[index++] = entry.getKey().name() + "=" + entry.getValue();
		}
		bundle.put(ATTACHMENTS, stored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		String restoredUid = requireId(
				bundle.getString(FIREARM_UID), "firearmUid");
		EnumMap<FirearmAttachmentSlot, String> restored =
				new EnumMap<>(FirearmAttachmentSlot.class);
		for (String stored : bundle.getStringArray(ATTACHMENTS)) {
			int separator = stored == null ? -1 : stored.indexOf('=');
			if (separator <= 0 || separator == stored.length() - 1) {
				throw new IllegalStateException("Invalid stored firearm attachment");
			}
			FirearmAttachmentSlot slot;
			try {
				slot = FirearmAttachmentSlot.valueOf(
						stored.substring(0, separator));
			} catch (IllegalArgumentException invalid) {
				throw new IllegalStateException(
						"Unknown stored firearm attachment slot", invalid);
			}
			String attachmentId = stored.substring(separator + 1);
			FirearmAttachmentDefinition definition =
					FirearmAttachmentCatalog.require(attachmentId);
			if (definition.slot != slot || restored.put(slot, definition.id) != null) {
				throw new IllegalStateException(
						"Invalid stored firearm attachment slot binding");
			}
		}
		firearmUid = restoredUid;
		attachments.clear();
		attachments.putAll(restored);
	}

	private static String requireId(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}
}
