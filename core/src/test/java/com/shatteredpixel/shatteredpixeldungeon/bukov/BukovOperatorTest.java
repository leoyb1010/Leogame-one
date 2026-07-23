package com.shatteredpixel.shatteredpixeldungeon.bukov;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BukovOperatorTest {

	@After
	public void resetHostState() {
		Dungeon.hero = null;
		Dungeon.quickslot.reset();
	}

	@Test
	public void sanitizationRemovesClassKitWithoutChangingHeroVitals() {
		Hero hero = legacyRogue();
		Dungeon.hero = hero;
		int hp = hero.HP;
		int ht = hero.HT;
		int strength = hero.STR;

		BukovOperator.sanitizeHostHero(hero);

		assertEquals(BukovOperator.HOST_CLASS, hero.heroClass);
		assertEquals(HeroSubClass.NONE, hero.subClass);
		assertNull(hero.armorAbility);
		assertEquals(hp, hero.HP);
		assertEquals(ht, hero.HT);
		assertEquals(strength, hero.STR);
		assertNull(hero.belongings.weapon);
		assertNull(hero.belongings.armor);
		assertNull(hero.belongings.artifact);
		assertTrue(hero.belongings.backpack.items.isEmpty());
		assertNull(hero.buff(Hunger.class));
		assertNull(hero.buff(Regeneration.class));
		assertEquals(Talent.MAX_TALENT_TIERS, hero.talents.size());
		for (LinkedHashMap<Talent, Integer> tier : hero.talents) {
			assertTrue(tier.isEmpty());
		}
		for (int i = 0; i < Dungeon.quickslot.SIZE; i++) {
			assertNull(Dungeon.quickslot.getItem(i));
		}
	}

	@Test
	public void sanitizedOperatorRemainsSerializable() {
		Hero hero = legacyRogue();
		Dungeon.hero = hero;
		BukovOperator.sanitizeHostHero(hero);

		Bundle stored = new Bundle();
		hero.storeInBundle(stored);
		Hero restored = new Hero();
		restored.restoreFromBundle(stored);
		BukovOperator.normalize(restored);

		assertEquals(HeroClass.ROGUE, restored.heroClass);
		assertEquals(HeroSubClass.NONE, restored.subClass);
		assertEquals(Talent.MAX_TALENT_TIERS, restored.talents.size());
		assertTrue(restored.belongings.backpack.items.isEmpty());
	}

	private static Hero legacyRogue() {
		Hero hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		hero.subClass = HeroSubClass.FREERUNNER;
		Talent.initClassTalents(hero);
		Buff.affect(hero, Hunger.class);
		Buff.affect(hero, Regeneration.class);
		hero.belongings.weapon = new Dagger();
		hero.belongings.artifact = new CloakOfShadows();
		hero.belongings.backpack.items.add(new ThrowingKnife());
		hero.belongings.backpack.items.add(new Food());
		hero.belongings.backpack.items.add(new Waterskin());
		Dungeon.quickslot.setSlot(0, hero.belongings.artifact);
		Dungeon.quickslot.setSlot(1, hero.belongings.backpack.items.get(0));
		return hero;
	}
}
