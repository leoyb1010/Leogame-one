package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.HitscanResolver;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovShotOwnershipTest {

	@Test
	public void playerRoundDamagesOnlyTheResolvedLivingEnemy() {
		Hero player = hero();
		RealtimeBody playerBody = player.realtimeBody;
		TestMob enemy = new TestMob();
		RealtimeBody enemyBody = body();
		enemy.realtimeBody = enemyBody;
		HitscanResolver.Hit hit = hit(enemyBody);

		assertTrue(BukovRealtimeWorld.playerShotCanDamage(
				hit, playerBody, player, enemy));

		hit.body = playerBody;
		assertFalse(BukovRealtimeWorld.playerShotCanDamage(
				hit, playerBody, player, player));

		hit.body = null;
		assertFalse(BukovRealtimeWorld.playerShotCanDamage(
				hit, playerBody, player, null));
	}

	@Test
	public void hostileRoundCannotDamageItsShooterOrAnotherHostile() {
		Hero player = hero();
		RealtimeBody playerBody = player.realtimeBody;
		TestMob shooter = new TestMob();
		RealtimeBody shooterBody = body();
		shooter.realtimeBody = shooterBody;
		TestMob ally = new TestMob();
		RealtimeBody allyBody = body();
		ally.realtimeBody = allyBody;

		assertTrue(BukovRealtimeWorld.enemyShotCanDamage(
				hit(playerBody),
				shooterBody,
				playerBody,
				shooter,
				player));
		assertFalse(BukovRealtimeWorld.enemyShotCanDamage(
				hit(shooterBody),
				shooterBody,
				playerBody,
				shooter,
				player));
		assertFalse(BukovRealtimeWorld.enemyShotCanDamage(
				hit(allyBody),
				shooterBody,
				playerBody,
				shooter,
				player));
	}

	@Test
	public void terrainImpactHasNoDamageOwnerOrTarget() {
		Hero player = hero();
		RealtimeBody playerBody = player.realtimeBody;
		TestMob shooter = new TestMob();
		RealtimeBody shooterBody = body();
		shooter.realtimeBody = shooterBody;
		HitscanResolver.Hit terrain = hit(null);

		assertFalse(BukovRealtimeWorld.playerShotCanDamage(
				terrain, playerBody, player, null));
		assertFalse(BukovRealtimeWorld.enemyShotCanDamage(
				terrain,
				shooterBody,
				playerBody,
				shooter,
				player));
	}

	@Test
	public void queuedHostileRoundIsDiscardedWhenShooterDiesFirst() {
		Hero player = hero();
		TestMob shooter = new TestMob();

		assertTrue(BukovRealtimeWorld.queuedEnemyShotCanDamage(
				shooter, player));
		shooter.HP = 0;
		assertFalse(BukovRealtimeWorld.queuedEnemyShotCanDamage(
				shooter, player));
	}

	private static HitscanResolver.Hit hit(RealtimeBody body) {
		HitscanResolver.Hit hit = new HitscanResolver.Hit();
		hit.body = body;
		hit.distance = 2f;
		return hit;
	}

	private static RealtimeBody body() {
		RealtimeBody body = new RealtimeBody();
		body.active = true;
		body.radius = 0.28f;
		return body;
	}

	private static Hero hero() {
		Hero hero = new Hero();
		hero.HP = hero.HT = 20;
		hero.realtimeBody = body();
		return hero;
	}

	private static final class TestMob extends Mob {
		private TestMob() {
			alignment = Char.Alignment.ENEMY;
			HP = HT = 10;
		}
	}
}
