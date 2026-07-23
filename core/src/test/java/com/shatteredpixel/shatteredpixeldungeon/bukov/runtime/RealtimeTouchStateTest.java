package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.watabou.utils.PointF;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RealtimeTouchStateTest {

	@Test
	public void leftDragProducesClampedMovementAndClearsOnUp() {
		RealtimeTouchState touch = new RealtimeTouchState();
		PointF movement = new PointF();
		PointF aim = new PointF(1f, 0f);

		touch.pointerDown(1, 100f, 500f, 1000f, 800f);
		touch.pointerMoved(1, 200f, 500f);
		touch.sample(50f, movement, aim);

		assertEquals(1f, movement.x, 0.0001f);
		assertEquals(0f, movement.y, 0.0001f);
		touch.pointerUp(1);
		movement.set(0f, 0f);
		touch.sample(50f, movement, aim);
		assertEquals(0f, movement.x, 0f);
		assertEquals(0f, movement.y, 0f);
	}

	@Test
	public void rightHoldFiresAndDragUpdatesAim() {
		RealtimeTouchState touch = new RealtimeTouchState();
		PointF movement = new PointF();
		PointF aim = new PointF(1f, 0f);

		touch.pointerDown(2, 700f, 500f, 1000f, 800f);
		assertTrue(touch.fireHeld());
		touch.pointerMoved(2, 700f, 400f);
		touch.sample(50f, movement, aim);
		assertEquals(0f, aim.x, 0.0001f);
		assertEquals(-1f, aim.y, 0.0001f);
		touch.pointerUp(2);
		assertFalse(touch.fireHeld());
	}

	@Test
	public void topCornersExposeInteractAndReloadWithoutStartingSticks() {
		RealtimeTouchState touch = new RealtimeTouchState();

		touch.pointerDown(3, 100f, 50f, 1000f, 800f);
		touch.pointerDown(4, 900f, 50f, 1000f, 800f);

		assertTrue(touch.consumeInteractPressed());
		assertTrue(touch.interactHeld());
		assertTrue(touch.consumeReloadPressed());
		assertFalse(touch.consumeInteractPressed());
		assertFalse(touch.fireHeld());
		assertFalse(touch.movementActive());
		touch.pointerUp(3);
		assertFalse(touch.interactHeld());
	}

	@Test
	public void resetClearsHeldPointersAndQueuedActions() {
		RealtimeTouchState touch = new RealtimeTouchState();
		touch.pointerDown(1, 100f, 500f, 1000f, 800f);
		touch.pointerDown(2, 700f, 500f, 1000f, 800f);
		touch.pointerDown(3, 100f, 10f, 1000f, 800f);

		touch.reset();

		assertFalse(touch.fireHeld());
		assertFalse(touch.movementActive());
		assertFalse(touch.interactHeld());
		assertFalse(touch.consumeInteractPressed());
	}
}
