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
import com.watabou.input.GameAction;
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
	private boolean controllerSprintHeld;
	private int controllerMedicalSlot;
	private boolean backpackPressed;
	private boolean listening;
	private boolean touchEnabled;
	private boolean legacyTouchListening;
	private BukovTouchControls touchControls;
	private PointerEvent movementTouch;
	private PointerEvent fireTouch;
	private PointerEvent interactTouch;
	private int[] northKeys = {Input.Keys.W};
	private int[] southKeys = {Input.Keys.S};
	private int[] westKeys = {Input.Keys.A};
	private int[] eastKeys = {Input.Keys.D};
	private int[] reloadKeys = {Input.Keys.R};
	private int[] interactKeys = {Input.Keys.E};
	private int[] medicalOneKeys = {Input.Keys.NUM_1};
	private int[] medicalTwoKeys = {Input.Keys.NUM_2};
	private int[] medicalThreeKeys = {Input.Keys.NUM_3};
	private int[] medicalFourKeys = {Input.Keys.NUM_4};

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
		GameAction action = KeyBindings.getActionForKey(event);
		if (event.pressed
				&& (action == SPDAction.INVENTORY
						|| action == SPDAction.INVENTORY_SELECTOR
						|| action == SPDAction.CYCLE
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
		} else if (event.code == Input.Keys.BUTTON_THUMBL) {
			controllerSprintHeld = event.pressed;
		} else if (event.code == Input.Keys.BUTTON_Y && event.pressed) {
			backpackPressed = true;
		} else if (event.pressed) {
			int medicalSlot = medicalSlotFor(action, event.code);
			if (medicalSlot > 0) {
				controllerMedicalSlot = medicalSlot;
			}
		}
		return false;
	};

	public void start() {
		if (!listening) {
			refreshBindings();
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
		controllerSprintHeld = false;
		controllerMedicalSlot = 0;
		backpackPressed = false;
		previousFireHeld = false;
		frame.movement.set(0f, 0f);
		frame.fireHeld = false;
		frame.interactHeld = false;
		frame.sprintHeld = false;
		frame.clearEdges();
		cancelTouches();
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
		// Visible mobile controls own their own pointer capture. Clearing only
		// the legacy playfield state can leave interact/fire held across a
		// backpack, modal window or gameplay pause.
		if (touchControls != null) {
			touchControls.resetInput();
		}
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
		if (anyKeyPressed(westKeys)) moveX -= 1f;
		if (anyKeyPressed(eastKeys)) moveX += 1f;
		if (anyKeyPressed(northKeys)) moveY -= 1f;
		if (anyKeyPressed(southKeys)) moveY += 1f;

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
		} else if (!touchEnabled && !ControllerHandler.controllerActive) {
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
		boolean fireHeld = resolveFireHeld(
				!touchEnabled
						&& Gdx.input.isButtonPressed(Input.Buttons.LEFT),
				controllerFireHeld,
				mobile != null && mobile.fireHeld(),
				touchEnabled && touch.fireHeld());
		frame.fireHeld = fireHeld;
		frame.firePressed = resolveFirePressed(
				mouseFirePressed,
				controllerFirePressed,
				fireHeld,
				previousFireHeld);
		previousFireHeld = fireHeld;
		frame.reloadPressed = anyKeyJustPressed(reloadKeys)
				|| controllerReloadPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.RELOAD)
				|| touchEnabled && touch.consumeReloadPressed();
		frame.interactHeld = anyKeyPressed(interactKeys)
				|| controllerInteractHeld
				|| mobile != null
						&& mobile.actionHeld(
								BukovTouchState.Action.INTERACT)
				|| touchEnabled && touch.interactHeld();
		frame.interactPressed = anyKeyJustPressed(interactKeys)
				|| controllerInteractPressed
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.INTERACT)
				|| touchEnabled && touch.consumeInteractPressed();
		frame.medicalSlot = firstMedicalSlotPressed();
		if (frame.medicalSlot == 0) {
			frame.medicalSlot = controllerMedicalSlot;
		}
		boolean automaticMedical =
				Gdx.input.isKeyJustPressed(Input.Keys.H)
				|| touchControls != null
						&& touchControls.consumePressed(
								BukovTouchState.Action.MEDICAL);
		frame.medicalPressed =
				automaticMedical || frame.medicalSlot > 0;
		frame.sprintHeld =
				Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
				|| Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
				|| controllerSprintHeld;
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
		controllerMedicalSlot = 0;
		backpackPressed = false;
		return frame;
	}

	private int firstMedicalSlotPressed() {
		if (anyKeyJustPressed(medicalOneKeys)) return 1;
		if (anyKeyJustPressed(medicalTwoKeys)) return 2;
		if (anyKeyJustPressed(medicalThreeKeys)) return 3;
		if (anyKeyJustPressed(medicalFourKeys)) return 4;
		return 0;
	}

	private void refreshBindings() {
		northKeys = boundKeys(SPDAction.N, Input.Keys.W);
		southKeys = boundKeys(SPDAction.S, Input.Keys.S);
		westKeys = boundKeys(SPDAction.W, Input.Keys.A);
		eastKeys = boundKeys(SPDAction.E, Input.Keys.D);
		reloadKeys = boundKeys(SPDAction.TAG_RESUME, Input.Keys.R);
		interactKeys = boundKeys(SPDAction.EXAMINE, Input.Keys.E);
		medicalOneKeys =
				boundKeys(SPDAction.QUICKSLOT_1, Input.Keys.NUM_1);
		medicalTwoKeys =
				boundKeys(SPDAction.QUICKSLOT_2, Input.Keys.NUM_2);
		medicalThreeKeys =
				boundKeys(SPDAction.QUICKSLOT_3, Input.Keys.NUM_3);
		medicalFourKeys =
				boundKeys(SPDAction.QUICKSLOT_4, Input.Keys.NUM_4);
	}

	private static int[] boundKeys(GameAction action, int fallback) {
		java.util.ArrayList<Integer> keys =
				KeyBindings.getKeyboardKeysForAction(action);
		if (keys.isEmpty()) {
			return new int[] {fallback};
		}
		int[] result = new int[keys.size()];
		for (int index = 0; index < keys.size(); index++) {
			result[index] = keys.get(index);
		}
		return result;
	}

	private static boolean anyKeyPressed(int[] keys) {
		for (int key : keys) {
			if (Gdx.input.isKeyPressed(key)) return true;
		}
		return false;
	}

	private static boolean anyKeyJustPressed(int[] keys) {
		for (int key : keys) {
			if (Gdx.input.isKeyJustPressed(key)) return true;
		}
		return false;
	}

	static int medicalSlotFor(GameAction action, int keyCode) {
		if (action == SPDAction.QUICKSLOT_1) return 1;
		if (action == SPDAction.QUICKSLOT_2) return 2;
		if (action == SPDAction.QUICKSLOT_3) return 3;
		if (action == SPDAction.QUICKSLOT_4) return 4;
		int offset = ControllerHandler.DPAD_KEY_OFFSET;
		if (keyCode == Input.Keys.DPAD_UP + offset) return 1;
		if (keyCode == Input.Keys.DPAD_RIGHT + offset) return 2;
		if (keyCode == Input.Keys.DPAD_DOWN + offset) return 3;
		if (keyCode == Input.Keys.DPAD_LEFT + offset) return 4;
		return 0;
	}

	/**
	 * Single production merge point for mouse, controller and both touch
	 * surfaces. Keeping the edge rule here lets integration tests prove that
	 * every supported input reaches the exact FireControl booleans used live.
	 */
	static boolean resolveFireHeld(
			boolean mouseHeld,
			boolean controllerHeld,
			boolean touchControlsHeld,
			boolean legacyTouchHeld) {
		return mouseHeld
				|| controllerHeld
				|| touchControlsHeld
				|| legacyTouchHeld;
	}

	static boolean resolveFirePressed(
			boolean mousePressed,
			boolean controllerPressed,
			boolean fireHeld,
			boolean previousFireHeld) {
		return mousePressed
				|| controllerPressed
				|| fireHeld && !previousFireHeld;
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
