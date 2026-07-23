package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchControls;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovTouchState;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.input.ControllerHandler;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PointF;
import com.watabou.utils.Signal;

/**
 * Polls realtime controls without consuming the host UI event stream.
 */
public final class RealtimeInput {

	private final InputFrame frame = new InputFrame();
	private final RealtimeTouchState touch = new RealtimeTouchState();
	private final PointF tunedLeftStick = new PointF();
	private final PointF tunedRightStick = new PointF();
	private boolean previousFireHeld;
	private boolean controllerFireHeld;
	private boolean controllerFirePressed;
	private boolean controllerReloadPressed;
	private boolean controllerInteractHeld;
	private boolean controllerInteractPressed;
	private boolean controllerMedicalPressed;
	private boolean backpackPressed;
	private boolean listening;
	private boolean touchEnabled;
	private boolean legacyTouchListening;
	private BukovTouchControls touchControls;
	private PointerEvent movementTouch;
	private PointerEvent fireTouch;
	private PointerEvent interactTouch;

	private final Signal.Listener<PointerEvent> pointerListener = event -> {
		if (!touchEnabled) {
			return false;
		}
		if (GameScene.interfaceBlockingHero()) {
			cancelTouches();
			return false;
		}
		if (event == null) {
			updateTouch(movementTouch);
			updateTouch(fireTouch);
			updateTouch(interactTouch);
			return false;
		}
		if (event.type == PointerEvent.Type.DOWN) {
			touch.pointerDown(
					event.id,
					event.current.x,
					event.current.y,
					Game.width,
					Game.height);
			if (event.current.y <= Game.height
					* RealtimeTouchState.ACTION_STRIP_FRACTION) {
				if (event.current.x < Game.width * 0.5f) {
					if (interactTouch == null) interactTouch = event;
				}
			} else if (event.current.x < Game.width * 0.5f) {
				if (movementTouch == null) movementTouch = event;
			} else if (fireTouch == null) {
				fireTouch = event;
			}
		} else if (event.type == PointerEvent.Type.UP
				|| event.type == PointerEvent.Type.CANCEL) {
			updateTouch(event);
			touch.pointerUp(event.id);
			if (movementTouch != null && movementTouch.id == event.id) {
				movementTouch = null;
			}
			if (fireTouch != null && fireTouch.id == event.id) {
				fireTouch = null;
			}
			if (interactTouch != null && interactTouch.id == event.id) {
				interactTouch = null;
			}
		}
		// Touch controls observe playfield input but never consume UI events.
		return false;
	};

	private final Signal.Listener<KeyEvent> keyListener = event -> {
		if (event.pressed
				&& (KeyBindings.getActionForKey(event) == SPDAction.INVENTORY
						|| event.code == Input.Keys.TAB)) {
			backpackPressed = true;
		}
		if (event.code == Input.Keys.BUTTON_R2) {
			if (event.pressed && !controllerFireHeld) {
				controllerFirePressed = true;
			}
			controllerFireHeld = event.pressed;
		} else if (event.code == Input.Keys.BUTTON_X && event.pressed) {
			controllerReloadPressed = true;
		} else if (event.code == Input.Keys.BUTTON_A) {
			if (event.pressed && !controllerInteractHeld) {
				controllerInteractPressed = true;
			}
			controllerInteractHeld = event.pressed;
		} else if (event.code == Input.Keys.BUTTON_Y && event.pressed) {
			backpackPressed = true;
		} else if (event.code >= Input.Keys.DPAD_UP
						+ ControllerHandler.DPAD_KEY_OFFSET
				&& event.code <= Input.Keys.DPAD_RIGHT
						+ ControllerHandler.DPAD_KEY_OFFSET
				&& event.pressed) {
			controllerMedicalPressed = true;
		}
		return false;
	};

	public void start() {
		if (!listening) {
			KeyEvent.addKeyListener(keyListener);
			touchEnabled = !DeviceCompat.isDesktop();
			if (touchEnabled && touchControls == null) {
				PointerEvent.addPointerListener(pointerListener);
				legacyTouchListening = true;
			}
			listening = true;
		}
	}

	public void stop() {
		if (listening) {
			KeyEvent.removeKeyListener(keyListener);
			if (legacyTouchListening) {
				PointerEvent.removePointerListener(pointerListener);
				legacyTouchListening = false;
			}
			listening = false;
		}
		resetTransientState();
		touchEnabled = false;
	}

	/**
	 * Clears every held/edge input which must not survive an app pause or scene
	 * transition. Controller release events are not guaranteed while the app is
	 * backgrounded, so clearing only touch pointers can leave fire held.
	 */
	public void resetTransientState() {
		controllerFireHeld = false;
		controllerFirePressed = false;
		controllerReloadPressed = false;
		controllerInteractHeld = false;
		controllerInteractPressed = false;
		controllerMedicalPressed = false;
		backpackPressed = false;
		previousFireHeld = false;
		frame.movement.set(0f, 0f);
		frame.fireHeld = false;
		frame.interactHeld = false;
		frame.clearEdges();
		cancelTouches();
		if (touchControls != null) {
			touchControls.resetInput();
		}
	}

	public void touchControls(BukovTouchControls controls) {
		touchControls = controls;
		if (legacyTouchListening) {
			PointerEvent.removePointerListener(pointerListener);
			legacyTouchListening = false;
		}
		cancelTouches();
	}

	public void cancelTouches() {
		touch.reset();
		movementTouch = null;
		fireTouch = null;
		interactTouch = null;
	}

	public InputFrame poll(RealtimeBody heroBody) {
		if (heroBody == null) {
			throw new IllegalArgumentException("heroBody is required");
		}
		frame.clearEdges();
		ControllerHandler.configureTriggerThresholds(
				SPDSettings.bukovTriggerPress() / 100f,
				SPDSettings.bukovTriggerRelease() / 100f);
		frame.aimAssistScale = BukovInputTuning.aimAssistScale(
				SPDSettings.bukovAimAssist());
		float moveX = 0f;
		float moveY = 0f;
		if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1f;
		if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1f;
		if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY -= 1f;
		if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY += 1f;

		normalizeInto(moveX, moveY, frame.movement);
		PointF left = ControllerHandler.leftStickPosition;
		BukovInputTuning.sampleStick(
				left.x,
				left.y,
				SPDSettings.bukovLeftInnerDeadZone() / 100f,
				SPDSettings.bukovLeftOuterDeadZone() / 100f,
				false,
				tunedLeftStick);
		if (lengthSquared(tunedLeftStick.x, tunedLeftStick.y) > 0f) {
			// Preserve stick magnitude. The old normalization made walking at
			// 20% stick travel indistinguishable from full-speed movement.
			frame.movement.set(tunedLeftStick);
		}
		BukovTouchState mobile =
				touchControls == null ? null : touchControls.state();
		if (mobile != null && mobile.movementHeld()) {
			frame.movement.set(
					mobile.movementX(),
					mobile.movementY());
		} else if (touchEnabled && touch.movementActive()) {
			touch.sample(touchRadius(), frame.movement, frame.aim);
		}

		PointF right = ControllerHandler.rightStickPosition;
		BukovInputTuning.sampleStick(
				right.x,
				right.y,
				SPDSettings.bukovRightInnerDeadZone() / 100f,
				SPDSettings.bukovRightOuterDeadZone() / 100f,
				SPDSettings.bukovAimCurve() == 1,
				tunedRightStick);
		if (lengthSquared(tunedRightStick.x, tunedRightStick.y) > 0f) {
			normalizeInto(tunedRightStick.x, tunedRightStick.y, frame.aim);
		} else if (mobile != null && mobile.aimHeld()) {
			frame.aim.set(mobile.aimX(), mobile.aimY());
		} else if (touchEnabled && touch.fireHeld()) {
			touch.sample(touchRadius(), frame.movement, frame.aim);
		} else {
			PointF pointer = PointerEvent.currentHoverPos();
			PointF world = Camera.main.screenToCamera((int)pointer.x, (int)pointer.y);
			normalizeInto(
					world.x / DungeonTilemap.SIZE - heroBody.x,
					world.y / DungeonTilemap.SIZE - heroBody.y,
					frame.aim
			);
		}

		boolean mouseFirePressed = !touchEnabled
				&& Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
		boolean fireHeld = (!touchEnabled
				&& Gdx.input.isButtonPressed(Input.Buttons.LEFT))
				|| controllerFireHeld
				|| mobile != null && mobile.fireHeld()
				|| touchEnabled && touch.fireHeld();
		frame.fireHeld = fireHeld;
		frame.firePressed = mouseFirePressed || controllerFirePressed || (fireHeld && !previousFireHeld);
		previousFireHeld = fireHeld;
		frame.reloadPressed = Gdx.input.isKeyJustPressed(Input.Keys.R)
				|| controllerReloadPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.RELOAD)
				|| touchEnabled && touch.consumeReloadPressed();
		frame.interactHeld = Gdx.input.isKeyPressed(Input.Keys.E)
				|| controllerInteractHeld
				|| mobile != null
						&& mobile.actionHeld(
								BukovTouchState.Action.INTERACT)
				|| touchEnabled && touch.interactHeld();
		frame.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E)
				|| controllerInteractPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.INTERACT)
				|| touchEnabled && touch.consumeInteractPressed();
		frame.medicalPressed = Gdx.input.isKeyJustPressed(Input.Keys.H)
				|| Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)
				|| Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)
				|| Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)
				|| Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)
				|| controllerMedicalPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.MEDICAL);
		frame.dropPressed = Gdx.input.isKeyJustPressed(Input.Keys.G)
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.DROP);
		frame.backpackPressed = backpackPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.BACKPACK);
		controllerFirePressed = false;
		controllerReloadPressed = false;
		controllerInteractPressed = false;
		controllerMedicalPressed = false;
		backpackPressed = false;
		return frame;
	}

	static void normalizeInto(float x, float y, PointF output) {
		float lengthSquared = lengthSquared(x, y);
		if (lengthSquared <= 0.000001f) {
			output.set(0f, 0f);
			return;
		}
		float inverseLength = 1f / (float)Math.sqrt(lengthSquared);
		output.set(x * inverseLength, y * inverseLength);
	}

	private static float lengthSquared(float x, float y) {
		return x * x + y * y;
	}

	private static float touchRadius() {
		return Math.max(36f, Math.min(Game.width, Game.height) * 0.10f);
	}

	private void updateTouch(PointerEvent event) {
		if (event != null) {
			touch.pointerMoved(event.id, event.current.x, event.current.y);
		}
	}
}
