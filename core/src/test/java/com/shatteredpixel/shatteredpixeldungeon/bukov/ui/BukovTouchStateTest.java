package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovTouchStateTest {

	@Test
	public void stickDeadZonePreventsAccidentalMovementAndFiring() {
		BukovTouchState state = new BukovTouchState(0.20f);

		assertTrue(state.beginStick(
				BukovTouchState.Stick.MOVEMENT,
				1,
				100f,
				100f,
				50f,
				105f,
				100f
		));
		assertTrue(state.beginStick(
				BukovTouchState.Stick.AIM_FIRE,
				2,
				300f,
				100f,
				50f,
				300f,
				105f
		));

		assertEquals(0f, state.movementX(), 0f);
		assertEquals(0f, state.aimY(), 0f);
		assertFalse(state.fireHeld());
	}

	@Test
	public void simultaneousSticksProduceClampedAnalogAxes() {
		BukovTouchState state = new BukovTouchState();
		state.beginStick(
				BukovTouchState.Stick.MOVEMENT,
				4,
				50f,
				50f,
				40f,
				90f,
				50f
		);
		state.beginStick(
				BukovTouchState.Stick.AIM_FIRE,
				5,
				200f,
				50f,
				40f,
				200f,
				-30f
		);

		assertEquals(1f, state.movementX(), 0.0001f);
		assertEquals(0f, state.movementY(), 0.0001f);
		assertEquals(0f, state.aimX(), 0.0001f);
		assertEquals(-1f, state.aimY(), 0.0001f);
		assertTrue(state.fireHeld());
	}

	@Test
	public void actionsExposeHeldStateAndSingleConsumableEdge() {
		BukovTouchState state = new BukovTouchState();

		assertTrue(state.beginAction(BukovTouchState.Action.RELOAD, 8));
		assertTrue(state.actionHeld(BukovTouchState.Action.RELOAD));
		assertTrue(state.consumePressed(BukovTouchState.Action.RELOAD));
		assertFalse(state.consumePressed(BukovTouchState.Action.RELOAD));

		state.endPointer(8);
		assertFalse(state.actionHeld(BukovTouchState.Action.RELOAD));
	}

	@Test
	public void onePointerCannotOwnTwoControls() {
		BukovTouchState state = new BukovTouchState();

		assertTrue(state.beginAction(BukovTouchState.Action.INTERACT, 10));
		assertFalse(state.beginAction(BukovTouchState.Action.MEDICAL, 10));
		assertFalse(state.beginStick(
				BukovTouchState.Stick.MOVEMENT,
				10,
				0f,
				0f,
				20f,
				20f,
				0f
		));
	}

	@Test
	public void cancelResetClearsEveryHeldAndQueuedInput() {
		BukovTouchState state = new BukovTouchState();
		state.beginStick(
				BukovTouchState.Stick.MOVEMENT,
				1,
				0f,
				0f,
				20f,
				20f,
				0f
		);
		state.beginStick(
				BukovTouchState.Stick.AIM_FIRE,
				2,
				100f,
				0f,
				20f,
				120f,
				0f
		);
		state.beginAction(BukovTouchState.Action.MEDICAL, 3);

		state.reset();

		assertFalse(state.movementHeld());
		assertFalse(state.aimHeld());
		assertFalse(state.fireHeld());
		assertFalse(state.actionHeld(BukovTouchState.Action.MEDICAL));
		assertFalse(state.consumePressed(BukovTouchState.Action.MEDICAL));
	}
}
