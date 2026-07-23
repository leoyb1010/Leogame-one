package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CombatPresentationEventPoolTest {

	@Test
	public void overflowDropsOldestCosmeticPulseOnly() {
		CombatPresentationEventPool pool =
				new CombatPresentationEventPool(2);
		pool.emit(
				CombatPresentationEvent.Type.PLAYER_FIRE,
				1, -1, 10, 11,
				CombatFeedbackType.RIFLE_SHOT, 1f);
		pool.emit(
				CombatPresentationEvent.Type.ENEMY_FIRE,
				2, 1, 20, 10,
				null, 0.8f);
		pool.emit(
				CombatPresentationEvent.Type.ENEMY_HIT,
				1, 2, 10, 20,
				null, 0.5f);

		List<CombatPresentationEvent.Type> types = new ArrayList<>();
		assertEquals(2, pool.drain(event -> types.add(event.type())));
		assertEquals(1, pool.dropped());
		assertEquals(CombatPresentationEvent.Type.ENEMY_FIRE, types.get(0));
		assertEquals(CombatPresentationEvent.Type.ENEMY_HIT, types.get(1));
		assertEquals(0, pool.size());
	}

	@Test
	public void carriesAnimationTargetAndOptionalFeedback() {
		CombatPresentationEventPool pool =
				new CombatPresentationEventPool(1);
		final int[] values = new int[4];
		final CombatFeedbackType[] feedback = new CombatFeedbackType[1];
		pool.emit(
				CombatPresentationEvent.Type.PLAYER_RELOAD,
				7, 7, 41, 42,
				null, 1f);

		pool.drain(event -> {
			values[0] = event.sourceId();
			values[1] = event.targetId();
			values[2] = event.sourceCell();
			values[3] = event.targetCell();
			feedback[0] = event.feedbackType();
		});

		assertEquals(7, values[0]);
		assertEquals(7, values[1]);
		assertEquals(41, values[2]);
		assertEquals(42, values[3]);
		assertNull(feedback[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidIntensity() {
		new CombatPresentationEventPool(1).emit(
				CombatPresentationEvent.Type.PLAYER_HIT,
				1, 1, 0, 0,
				CombatFeedbackType.PLAYER_HIT,
				Float.NaN);
	}
}
