package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovAlleyScoutSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovArmoredSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovBreachVeteranSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovCaptainSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovDepotShotgunnerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovDroneSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovFogStalkerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovGunnerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovIronClaspMarksmanSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovLineRiflemanSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovScavengerSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovSignalOperatorSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.bukov.BukovWhiteLineSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Serializable host shell for data-driven Bukov enemies.
 *
 * Combat and movement remain authored by EnemyArchetypeDefinition, while the
 * visual mapping is fully Bukov-owned and survives save/restore.
 */
public final class BukovHostMob extends Mob {

	private static final String DEFINITION_ID = "bukov_definition_id";
	private static final String DISPLAY_NAME = "bukov_display_name";
	private static final String HOST_CLASS_HINT = "bukov_host_class_hint";
	private static final String MINIMUM_DAMAGE = "bukov_minimum_damage";
	private static final String MAXIMUM_DAMAGE = "bukov_maximum_damage";
	private static final String ONBOARDING_CONTACT =
			"bukov_onboarding_contact";

	private String definitionId = "";
	private String displayName = "布科夫敌人";
	private String hostClassHint = "Rat";
	private int minimumDamage = 1;
	private int maximumDamage = 1;
	private boolean onboardingContact;

	{
		spriteClass = BukovScavengerSprite.class;
		EXP = 0;
		maxLvl = -1;
		loot = null;
		lootChance = 0f;
	}

	public BukovHostMob configure(EnemyArchetypeDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition is required");
		}
		definition.validate();
		definitionId = definition.id;
		displayName = definition.name;
		hostClassHint = definition.hostClassHint;
		minimumDamage = definition.minimumDamage;
		maximumDamage = definition.maximumDamage;
		HP = HT = definition.health;
		defenseSkill = definition.tier == EnemyTier.BOSS
				? 16 : definition.tier == EnemyTier.ELITE ? 12 : 7;
		applyBukovSprite();
		return this;
	}

	public String definitionId() {
		return definitionId;
	}

	public BukovHostMob markOnboardingContact() {
		onboardingContact = true;
		return this;
	}

	public boolean onboardingContact() {
		return onboardingContact;
	}

	@Override
	public String name() {
		return displayName;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(minimumDamage, maximumDamage);
	}

	@Override
	public int attackSkill(Char target) {
		return defenseSkill + 3;
	}

	@Override
	public void rollToDropLoot() {
		// Bukov loot is released through its deterministic table seam.
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEFINITION_ID, definitionId);
		bundle.put(DISPLAY_NAME, displayName);
		bundle.put(HOST_CLASS_HINT, hostClassHint);
		bundle.put(MINIMUM_DAMAGE, minimumDamage);
		bundle.put(MAXIMUM_DAMAGE, maximumDamage);
		bundle.put(ONBOARDING_CONTACT, onboardingContact);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		definitionId = bundle.getString(DEFINITION_ID);
		displayName = bundle.getString(DISPLAY_NAME);
		hostClassHint = bundle.getString(HOST_CLASS_HINT);
		minimumDamage = bundle.getInt(MINIMUM_DAMAGE);
		maximumDamage = bundle.getInt(MAXIMUM_DAMAGE);
		onboardingContact = bundle.getBoolean(ONBOARDING_CONTACT);
		applyBukovSprite();
	}

	private void applyBukovSprite() {
		// Definition IDs are the stable content contract. Host hints remain only
		// as a backwards-compatible fallback for old checkpoints and modded data.
		switch (definitionId) {
			case "scavenger_gunner":
				spriteClass = BukovGunnerSprite.class;
				return;
			case "melee_rusher":
				spriteClass = BukovScavengerSprite.class;
				return;
			case "iron_clasp_guard":
				spriteClass = BukovArmoredSprite.class;
				return;
			case "sensor_doll":
				spriteClass = BukovDroneSprite.class;
				return;
			case "iron_clasp_captain":
				spriteClass = BukovCaptainSprite.class;
				return;
			case "boss_white_line":
				spriteClass = BukovWhiteLineSprite.class;
				return;
			case "alley_scout":
				spriteClass = BukovAlleyScoutSprite.class;
				return;
			case "depot_shotgunner":
				spriteClass = BukovDepotShotgunnerSprite.class;
				return;
			case "line_rifleman":
				spriteClass = BukovLineRiflemanSprite.class;
				return;
			case "fog_stalker":
				spriteClass = BukovFogStalkerSprite.class;
				return;
			case "signal_operator":
				spriteClass = BukovSignalOperatorSprite.class;
				return;
			case "iron_clasp_marksman":
				spriteClass = BukovIronClaspMarksmanSprite.class;
				return;
			case "breach_veteran":
				spriteClass = BukovBreachVeteranSprite.class;
				return;
			default:
				break;
		}

		switch (hostClassHint) {
			case "GnollTrickster":
				spriteClass = BukovGunnerSprite.class;
				break;
			case "Guard":
				spriteClass = BukovArmoredSprite.class;
				break;
			case "DM100":
				spriteClass = BukovDroneSprite.class;
				break;
			case "Brute":
				spriteClass = BukovCaptainSprite.class;
				break;
			case "Goo":
				spriteClass = BukovWhiteLineSprite.class;
				break;
			case "Rat":
			default:
				spriteClass = BukovScavengerSprite.class;
				break;
		}
	}
}
