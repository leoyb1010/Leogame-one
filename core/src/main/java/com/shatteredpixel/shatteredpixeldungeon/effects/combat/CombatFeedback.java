/*
 * Leo's Dungeon Siege combat feedback layer.
 * Copyright (C) 2026 Leo Yuan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.combat;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

/** Presentation-only hit orchestration. Never spends turns, changes HP, or rolls combat RNG. */
public final class CombatFeedback {

	private CombatFeedback() {
	}

	public static void play(Char attacker, Char defender, int damage) {
		int intensity = SPDSettings.combatFeedback();
		if (intensity == 0 || damage <= 0 || attacker.sprite == null || defender.sprite == null) return;
		if (!attacker.sprite.visible && !defender.sprite.visible) return;

		KindOfWeapon.ImpactFamily family = familyFor(attacker);
		boolean killed = !defender.isAlive();
		float damageRatio = Math.min(1f, damage / (float) Math.max(1, defender.HT));
		boolean heavy = killed || damageRatio >= 0.30f || family == KindOfWeapon.ImpactFamily.CRUSH;

		float magnitude = baseMagnitude(family) + damageRatio * 0.35f + (killed ? 0.25f : 0f);
		float duration = heavy ? 0.13f : 0.08f;
		if (intensity == 1) {
			magnitude *= 0.55f;
			duration *= 0.7f;
		}
		PixelScene.shake(magnitude, duration);

		int particles = intensity == 1 ? 2 : (heavy ? 6 : 4);
		defender.sprite.burst(materialColor(defender), particles);

		PointF from = attacker.sprite.center();
		PointF to = defender.sprite.center();
		Wound.hit(defender, PointF.angle(from, to));

		playMaterialLayer(defender, heavy);

		if (SPDSettings.vibration() && (attacker == Dungeon.hero || defender == Dungeon.hero)) {
			Game.vibrate(heavy ? 45 : 18);
		}
	}

	private static KindOfWeapon.ImpactFamily familyFor(Char attacker) {
		if (attacker instanceof Hero) {
			KindOfWeapon weapon = ((Hero) attacker).belongings.attackingWeapon();
			if (weapon != null) return weapon.impactFamily();
		}
		return KindOfWeapon.ImpactFamily.GENERIC;
	}

	private static float baseMagnitude(KindOfWeapon.ImpactFamily family) {
		switch (family) {
			case STAB: return 0.16f;
			case SLASH: return 0.24f;
			case CRUSH: return 0.42f;
			default: return 0.20f;
		}
	}

	private static int materialColor(Char defender) {
		String name = defender.getClass().getSimpleName().toLowerCase();
		if (name.contains("slime") || name.contains("goo")) return 0xFF45C66B;
		if (Char.hasProp(defender, Char.Property.INORGANIC)) return 0xFFD0C7A5;
		if (Char.hasProp(defender, Char.Property.UNDEAD)) return 0xFFAAA7B2;
		return defender.sprite.blood();
	}

	private static void playMaterialLayer(Char defender, boolean heavy) {
		if (Char.hasProp(defender, Char.Property.INORGANIC)) {
			Sample.INSTANCE.play(Assets.Sounds.STURDY, heavy ? 0.32f : 0.20f, 1f);
		} else if (Char.hasProp(defender, Char.Property.UNDEAD)) {
			Sample.INSTANCE.play(Assets.Sounds.BONES, heavy ? 0.26f : 0.16f, 1f);
		}
	}
}
