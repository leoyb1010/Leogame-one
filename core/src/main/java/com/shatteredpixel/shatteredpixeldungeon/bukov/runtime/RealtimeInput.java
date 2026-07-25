package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
	private final InputFrame fixedFrame = new InputFrame();
	private final InputEdgeLatch edgeLatch = new InputEdgeLatch();
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
	private boolean backpackHeld;
	private boolean suppressBackpackUntilRelease;
	private boolean suppressFireUntilRelease;
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
	private int bindingRevision = -1;

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
		boolean controller =
				ControllerHandler.icControllerKey(event.code);
		if (event.pressed
				&& BukovInputBindings.isBackpack(
						action, controller)
				&& !backpackHeld
				&& !suppressBackpackUntilRelease) {
			backpackPressed = true;
		}
		if (BukovInputBindings.isBackpack(action, controller)) {
			backpackHeld = event.pressed;
			if (!event.pressed) {
				suppressBackpackUntilRelease = false;
			}
		}
		if (BukovInputBindings.isFire(action)) {
			// Bukov owns FIRE as realtime state. Without consuming the raw
			// action KeyEvent would also emulate a pointer click after this
			// listener returns, causing one physical press to shoot and click.
			event.consumeRaw();
			if (event.pressed
					&& !controllerFireHeld
					&& !suppressFireUntilRelease) {
				controllerFirePressed = true;
			}
			controllerFireHeld = event.pressed;
		} else if (BukovInputBindings.isReload(
				action, controller) && event.pressed) {
			controllerReloadPressed = true;
		} else if (BukovInputBindings.isInteract(
				action, controller)) {
			if (event.pressed && !controllerInteractHeld) {
				controllerInteractPressed = true;
			}
			controllerInteractHeld = event.pressed;
		} else if (BukovInputBindings.isSprint(
				action, controller)) {
			controllerSprintHeld = event.pressed;
		} else if (event.pressed) {
			int medicalSlot =
					BukovInputBindings.medicalSlot(
							action, controller);
			if (medicalSlot > 0) {
				controllerMedicalSlot = medicalSlot;
			}
		}
		return false;
	};

	public void start() {
		if (!listening) {
			refreshBindings();
			KeyEvent.addRawKeyListener(keyListener);
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
			KeyEvent.removeRawKeyListener(keyListener);
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
		clearTransientState();
		suppressBackpackUntilRelease = false;
		suppressFireUntilRelease = false;
	}

	/**
	 * Drops the input which opened or dismissed a blocking interface and keeps
	 * held fire suppressed until the physical button is released. This must be
	 * called by the interface transition itself: the window can be destroyed
	 * before the next world {@code paused()} check.
	 */
	public void suppressInterfaceInputUntilRelease() {
		boolean keepFireSuppressed = suppressFireUntilRelease
				|| currentFireHeld();
		boolean keepBackpackSuppressed = suppressBackpackUntilRelease
				|| backpackHeld;
		clearTransientState();
		suppressFireUntilRelease = keepFireSuppressed;
		suppressBackpackUntilRelease = keepBackpackSuppressed;
	}

	private void clearTransientState() {
		controllerFireHeld = false;
		controllerFirePressed = false;
		controllerReloadPressed = false;
		controllerInteractHeld = false;
		controllerInteractPressed = false;
		controllerSprintHeld = false;
		controllerMedicalSlot = 0;
		backpackPressed = false;
		backpackHeld = false;
		previousFireHeld = false;
		frame.movement.set(0f, 0f);
		frame.fireHeld = false;
		frame.interactHeld = false;
		frame.sprintHeld = false;
		frame.clearEdges();
		fixedFrame.movement.set(0f, 0f);
		fixedFrame.aim.set(1f, 0f);
		fixedFrame.fireHeld = false;
		fixedFrame.interactHeld = false;
		fixedFrame.sprintHeld = false;
		fixedFrame.clearEdges();
		edgeLatch.reset();
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

	/**
	 * Samples devices exactly once per rendered frame. Edge values are latched
	 * until a fixed step consumes them; continuous axes and held states simply
	 * replace the previous render sample.
	 */
	public void sample(RealtimeBody heroBody) {
		if (heroBody == null) {
			throw new IllegalArgumentException("heroBody is required");
		}
		frame.clearEdges();
		if (bindingRevision != BukovInputBindings.revision()) {
			refreshBindings();
		}
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
		if (suppressFireUntilRelease) {
			if (!fireHeld) {
				suppressFireUntilRelease = false;
			}
			fireHeld = false;
			mouseFirePressed = false;
			controllerFirePressed = false;
		}
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
		edgeLatch.capture(frame);
	}

	private boolean currentFireHeld() {
		BukovTouchState mobile =
				touchControls == null ? null : touchControls.state();
		return resolveFireHeld(
				!touchEnabled
						&& Gdx.input != null
						&& Gdx.input.isButtonPressed(Input.Buttons.LEFT),
				controllerFireHeld,
				mobile != null && mobile.fireHeld(),
				touchEnabled && touch.fireHeld());
	}

	/**
	 * Returns the latest continuous sample plus edge values for only the first
	 * fixed step after capture.
	 */
	public InputFrame consumeFixedStep() {
		fixedFrame.movement.set(frame.movement);
		fixedFrame.aim.set(frame.aim);
		fixedFrame.fireHeld = frame.fireHeld;
		fixedFrame.interactHeld = frame.interactHeld;
		fixedFrame.sprintHeld = frame.sprintHeld;
		fixedFrame.aimAssistScale = frame.aimAssistScale;
		edgeLatch.drainTo(fixedFrame);
		return fixedFrame;
	}

	/**
	 * Compatibility seam for renderer-free callers which intentionally use one
	 * device sample per fixed step.
	 */
	public InputFrame poll(RealtimeBody heroBody) {
		sample(heroBody);
		return consumeFixedStep();
	}

	private int firstMedicalSlotPressed() {
		if (anyKeyJustPressed(medicalOneKeys)) return 1;
		if (anyKeyJustPressed(medicalTwoKeys)) return 2;
		if (anyKeyJustPressed(medicalThreeKeys)) return 3;
		if (anyKeyJustPressed(medicalFourKeys)) return 4;
		return 0;
	}

	private void refreshBindings() {
		northKeys = boundKeys(
				BukovInputBindings.MOVE_UP, Input.Keys.W);
		southKeys = boundKeys(
				BukovInputBindings.MOVE_DOWN, Input.Keys.S);
		westKeys = boundKeys(
				BukovInputBindings.MOVE_LEFT, Input.Keys.A);
		eastKeys = boundKeys(
				BukovInputBindings.MOVE_RIGHT, Input.Keys.D);
		reloadKeys = boundKeys(
				BukovInputBindings.RELOAD, Input.Keys.R);
		interactKeys = boundKeys(
				BukovInputBindings.INTERACT, Input.Keys.E);
		medicalOneKeys =
				boundKeys(
						BukovInputBindings.MEDICAL_1,
						Input.Keys.NUM_1);
		medicalTwoKeys =
				boundKeys(
						BukovInputBindings.MEDICAL_2,
						Input.Keys.NUM_2);
		medicalThreeKeys =
				boundKeys(
						BukovInputBindings.MEDICAL_3,
						Input.Keys.NUM_3);
		medicalFourKeys =
				boundKeys(
						BukovInputBindings.MEDICAL_4,
						Input.Keys.NUM_4);
		bindingRevision = BukovInputBindings.revision();
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
		return BukovInputBindings.medicalSlot(action, keyCode);
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
