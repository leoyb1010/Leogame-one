package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;

public class HeroSaveRegressionTest {

	@Test
	public void defaultAccuracySurvivesSaveRoundTrip() {
		Hero hero = new Hero();
		for (int i = 0; i < Talent.MAX_TALENT_TIERS; i++) {
			hero.talents.add(new LinkedHashMap<>());
		}
		Bundle stored = new Bundle();
		hero.storeInBundle(stored);
		assertEquals(10, stored.getInt("attackSkill"));

		Hero restored = new Hero();
		restored.restoreFromBundle(stored);
		Bundle roundTripped = new Bundle();
		restored.storeInBundle(roundTripped);
		assertEquals(10, roundTripped.getInt("attackSkill"));
	}
}
