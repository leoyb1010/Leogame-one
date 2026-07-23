package com.shatteredpixel.shatteredpixeldungeon.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;

import java.util.LinkedHashMap;

/**
 * Fixed host representation for the Bukov action operator.
 *
 * The classic hero classes remain available only through classic mode. Bukov
 * uses one internal host class so new raids and saves created by the earlier
 * class-selection flow present the same operator.
 */
public final class BukovOperator {

	public static final HeroClass HOST_CLASS = HeroClass.ROGUE;

	private BukovOperator() {
	}

	public static void prepareNewRaid() {
		GamesInProgress.selectedClass = HOST_CLASS;
		GamesInProgress.randomizedClass = false;
	}

	/**
	 * Converts the legacy dungeon Hero into an inventory-neutral Bukov host.
	 *
	 * Hero is retained because it is the engine's stable actor/save contract,
	 * but class starter equipment, quickslots and talent trees are not part of
	 * Bukov. Runtime raid equipment must be installed afterwards from the
	 * durable LootTransaction.
	 */
	public static void sanitizeHostHero(Hero hero) {
		if (hero == null) {
			throw new IllegalArgumentException("hero is required");
		}

		hero.heroClass = HOST_CLASS;
		hero.subClass = HeroSubClass.NONE;
		hero.armorAbility = null;

		hero.talents.clear();
		for (int i = 0; i < Talent.MAX_TALENT_TIERS; i++) {
			hero.talents.add(new LinkedHashMap<Talent, Integer>());
		}
		hero.metamorphedTalents.clear();

		// Older Rogue-host saves may have a live cloak buff even after its item
		// reference is removed. Hunger and turn-based regeneration are dungeon
		// survival mechanics, not part of the realtime extraction rules.
		// Ordinary combat damage/status state remains intact across resume.
		for (Buff buff : hero.buffs()) {
			if (buff instanceof Artifact.ArtifactBuff
					|| buff instanceof Preparation
					|| buff instanceof Hunger
					|| buff instanceof Regeneration) {
				buff.detach();
			}
		}

		hero.belongings.clear();
		hero.belongings.thrownWeapon = null;
		hero.belongings.abilityWeapon = null;
		hero.belongings.lostInventory(false);
		Dungeon.quickslot.reset();
	}

	/**
	 * Backward-compatible migration entrypoint for saves created before the
	 * dedicated Bukov deployment flow.
	 */
	public static void normalize(Hero hero) {
		if (hero == null) {
			return;
		}
		sanitizeHostHero(hero);
	}
}
