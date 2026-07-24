package com.shatteredpixel.shatteredpixeldungeon.actors;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CharDamagePresentationTest {

	@Test
	public void callerOwnedFloatingTextIsScopedToOneDamageCall() {
		final boolean[] floatingTextEnabledDuringDamage = {true};
		Char target = new Char() {
			@Override
			public void damage(int damage, Object source) {
				floatingTextEnabledDuringDamage[0] =
						damageFloatingTextEnabled();
			}
		};

		target.damageWithoutFloatingText(15, target);

		assertFalse(floatingTextEnabledDuringDamage[0]);
		assertTrue(target.damageFloatingTextEnabled());

		target.damage(15, target);
		assertTrue(floatingTextEnabledDuringDamage[0]);
	}

	@Test
	public void suppressionIsRestoredWhenDamageThrows() {
		Char target = new Char() {
			@Override
			public void damage(int damage, Object source) {
				throw new IllegalStateException("expected");
			}
		};

		try {
			target.damageWithoutFloatingText(15, target);
			fail("damage exception should escape");
		} catch (IllegalStateException expected) {
			assertTrue(target.damageFloatingTextEnabled());
		}
	}
}
