package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCombatPresentationWiringTest {

	@Test
	public void realtimeActionsCannotReenterTurnCallbacks() throws Exception {
		String sprites = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/CharSprite.java");
		String hero = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroSprite.java");

		assertTrue(sprites.contains("realtimeActionPlaying"));
		assertTrue(sprites.contains("priority <= realtimeActionPriority"));
		assertTrue(sprites.contains(
				"if (realtimeActionPlaying) {\n\t\t\treturn;"));
		assertTrue(hero.contains(
				"playBukovAction(fire, targetCell, 1, callback)"));
		assertTrue(hero.contains(
				"playBukovAction(reload, targetCell, 2, callback)"));
		assertTrue(hero.contains(
				"playBukovAction(hit, ch.pos, 3, callback)"));
		assertTrue(hero.contains(
				"playBukovAction(medical, ch.pos, 2, null)"));
		assertTrue(hero.contains(
				"playBukovAction(extract, ch.pos, 2, callback)"));
		assertTrue(hero.contains(
				"playRealtimeAction(animation, targetCell, priority, completion)"));
		assertFalse(hero.contains(
				"firearmFire(int targetCell, final Callback callback) {\n"
						+ "\t\tanimCallback"));
	}

	@Test
	public void sceneDrainsCosmeticEventsAfterSimulation() throws Exception {
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String presentation = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/BukovCombatPresentation.java");

		assertTrue(scene.contains("bukovRealtime.update(Game.elapsed);"));
		assertTrue(scene.contains("drainCombatPresentation"));
		assertTrue(world.contains(
				"CombatPresentationEvent.Type.PLAYER_FIRE"));
		assertTrue(world.contains(
				"CombatPresentationEvent.Type.ENEMY_MELEE"));
		assertTrue(world.contains(
				"CombatPresentationEvent.Type.PLAYER_MEDICAL_START"));
		assertTrue(world.contains(
				"CombatPresentationEvent.Type.PLAYER_EXTRACTION"));
		assertTrue(world.contains(
				"CombatPresentationEvent.Type.EXTRACTION_COMPLETE"));
		assertTrue(presentation.contains("CombatFeedbackResolver.add("));
		assertTrue(presentation.contains("combatFeedback <= 0"));
		assertTrue(presentation.contains(
				"hitstopEnabled(combatFeedback, reduceMotion)"));
		assertTrue(presentation.contains("isHitOutcome(event.type())"));
		assertTrue(presentation.contains("applySpriteHitstop("));
		assertFalse(presentation.contains("damage("));
		assertFalse(presentation.contains("Random."));
		assertFalse(presentation.contains("Actor.process"));
	}

	@Test
	public void playerHitscanAlwaysEmitsCompleteGunshotFxPacket() throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		int fireStart = world.indexOf(
				"public void fire(Firearm firearm, FirearmDefinition definition)");
		int fireEnd = world.indexOf(
				"public FireControl.AmmoSelection requestAmmo(", fireStart);
		String playerFire = world.substring(fireStart, fireEnd);

		int muzzle = playerFire.indexOf("combatFx.muzzle(");
		int shell = playerFire.indexOf("combatFx.shell(", muzzle);
		int resolver = playerFire.indexOf("resolvePlayerShot(", shell);
		int impact = playerFire.indexOf("combatFx.impact(", resolver);
		int targetFilter = playerFire.indexOf(
				"Char target = charsByBody.get(shotHit.body);", impact);
		assertTrue(muzzle >= 0);
		assertTrue(shell > muzzle);
		assertTrue(resolver > shell);
		assertTrue(impact > resolver);
		// The impact must be emitted before target filtering so a wall stop is
		// just as readable as an enemy hit.
		assertTrue(targetFilter > impact);

		int resolverStart = world.indexOf(
				"static void resolvePlayerShot(");
		int resolverEnd = world.indexOf(
				"public FireControl.AmmoSelection requestAmmo(", resolverStart);
		String resolverBody = world.substring(resolverStart, resolverEnd);
		int cast = resolverBody.indexOf("HitscanResolver.cast(");
		int tracer = resolverBody.indexOf("combatFx.tracer(", cast);
		assertTrue(cast >= 0);
		assertTrue(tracer > cast);
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
