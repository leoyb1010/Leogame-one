package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.settings.ExperienceContractTestFixture;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovCombatPresentationHitstopTest {

	@Test
	public void freezesOnlySpritesAndReentryUsesLongestRemainingWindow() {
		BukovCombatPresentation presentation = presentation();
		CharSprite source = new CharSprite();
		CharSprite target = new CharSprite();
		target.paused = true;

		presentation.applySpriteHitstop(source, target, 50);
		assertTrue(source.paused);
		assertTrue(target.paused);
		assertEquals(2, presentation.activeHitstopCount());

		presentation.applySpriteHitstop(source, source, 120);
		assertEquals("same source and target must not duplicate state",
				2, presentation.activeHitstopCount());

		presentation.update(0.06f);
		assertTrue("longer reentrant window must remain active", source.paused);
		assertTrue("originally paused target stays paused", target.paused);
		assertEquals(1, presentation.activeHitstopCount());

		presentation.update(0.07f);
		assertFalse("original unpaused state must be restored", source.paused);
		assertEquals(0, presentation.activeHitstopCount());
	}

	@Test
	public void disposeRestoresEveryOriginalPausedState() {
		BukovCombatPresentation presentation = presentation();
		CharSprite source = new CharSprite();
		CharSprite target = new CharSprite();
		target.paused = true;

		presentation.applySpriteHitstop(source, target, 120);
		presentation.dispose();

		assertFalse(source.paused);
		assertTrue(target.paused);
		assertEquals(0, presentation.activeHitstopCount());
	}

	@Test
	public void settingsDisableOrReduceHitstopWithoutAffectingOtherFeedback() {
		assertFalse(BukovCombatPresentation.hitstopEnabled(0, false));
		assertFalse(BukovCombatPresentation.hitstopEnabled(2, true));
		assertTrue(BukovCombatPresentation.hitstopEnabled(2, false));
		assertEquals(0, BukovCombatPresentation.scaledHitstopMs(120, 0));
		assertEquals(60, BukovCombatPresentation.scaledHitstopMs(120, 1));
		assertEquals(120, BukovCombatPresentation.scaledHitstopMs(120, 2));
	}

	@Test
	public void onlyCommittedHitOutcomesMayFreezeSprites() {
		assertTrue(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.PLAYER_HIT));
		assertTrue(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.ENEMY_HIT));
		assertTrue(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.PLAYER_DEATH));
		assertTrue(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.ENEMY_DEATH));
		assertFalse(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.PLAYER_FIRE));
		assertFalse(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.ENEMY_FIRE));
		assertFalse(BukovCombatPresentation.isHitOutcome(
				CombatPresentationEvent.Type.PLAYER_RELOAD));
	}

	@Test
	public void rollingBudgetCapsHitstopAtSixHundredMillisecondsPerMinute() {
		HitstopBudget budget = new HitstopBudget();

		assertEquals(120, budget.request(120));
		assertEquals(
				"same-frame higher tier only pays its extension",
				160,
				budget.request(160));
		budget.advance(0.2f);
		for (int index = 0; index < 4; index++) {
			assertEquals(
					index < 3 ? 120 : 80,
					budget.request(120));
			budget.advance(0.2f);
		}
		assertEquals(
				HitstopBudget.MAXIMUM_PER_MINUTE_MS,
				budget.rollingTotalMs());
		assertEquals(0, budget.request(120));

		budget.advance(60f);
		assertEquals(0, budget.rollingTotalMs());
		assertEquals(120, budget.request(120));
	}

	@Test
	public void presentationCannotPauseOrMutateRealtimeSimulation()
			throws Exception {
		String presentation = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/BukovCombatPresentation.java");
		String scene = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java");

		assertFalse(presentation.contains("BukovRealtimeWorld"));
		assertFalse(presentation.contains("RealtimeRaidSystem"));
		assertFalse(presentation.contains("fixedStep"));
		assertFalse(presentation.contains("inputFrame"));
		assertFalse(presentation.contains(".damage("));
		assertTrue(presentation.contains("sprite.paused = true"));
		assertTrue(scene.contains("bukovRealtime.update(Game.elapsed);"));
		assertTrue(scene.contains(
				"bukovCombatPresentation.update(Game.elapsed);"));
		assertTrue(scene.contains("bukovCombatPresentation.dispose();"));
	}

	private static BukovCombatPresentation presentation() {
		return new BukovCombatPresentation(
				ExperienceContractTestFixture.load());
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
