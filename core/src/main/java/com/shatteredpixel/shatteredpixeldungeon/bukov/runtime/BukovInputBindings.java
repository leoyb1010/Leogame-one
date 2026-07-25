package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.ControllerHandler;
import com.watabou.input.GameAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The explicit input surface supported by the realtime Bukov runtime.
 *
 * <p>Several actions intentionally reuse host action identifiers so existing
 * saved bindings remain compatible. Player-facing Bukov code must use this
 * catalog instead of enumerating every host {@link GameAction}.</p>
 */
public final class BukovInputBindings {

	public static final GameAction MOVE_UP = SPDAction.N;
	public static final GameAction MOVE_LEFT = SPDAction.W;
	public static final GameAction MOVE_DOWN = SPDAction.S;
	public static final GameAction MOVE_RIGHT = SPDAction.E;
	public static final GameAction FIRE = SPDAction.LEFT_CLICK;
	public static final GameAction INTERACT = SPDAction.EXAMINE;
	public static final GameAction RELOAD = SPDAction.TAG_RESUME;
	public static final GameAction BACKPACK = SPDAction.INVENTORY;
	public static final GameAction MEDICAL_1 = SPDAction.QUICKSLOT_1;
	public static final GameAction MEDICAL_2 = SPDAction.QUICKSLOT_2;
	public static final GameAction MEDICAL_3 = SPDAction.QUICKSLOT_3;
	public static final GameAction MEDICAL_4 = SPDAction.QUICKSLOT_4;
	public static final GameAction PAUSE = SPDAction.BACK;

	public static final GameAction CONTROLLER_INTERACT = SPDAction.TAG_ATTACK;
	public static final GameAction CONTROLLER_RELOAD =
			SPDAction.QUICKSLOT_SELECTOR;
	public static final GameAction CONTROLLER_BACKPACK =
			SPDAction.INVENTORY_SELECTOR;
	public static final GameAction CONTROLLER_SPRINT =
			SPDAction.WAIT_OR_PICKUP;
	public static final GameAction CONTROLLER_MEDICAL_1 =
			SPDAction.TAG_ACTION;
	public static final GameAction CONTROLLER_MEDICAL_2 =
			SPDAction.TAG_LOOT;
	public static final GameAction CONTROLLER_MEDICAL_3 =
			SPDAction.TAG_RESUME;
	public static final GameAction CONTROLLER_MEDICAL_4 =
			SPDAction.CYCLE;

	private static final List<GameAction> KEYBOARD_ACTIONS = Arrays.asList(
			MOVE_UP,
			MOVE_LEFT,
			MOVE_DOWN,
			MOVE_RIGHT,
			FIRE,
			INTERACT,
			RELOAD,
			BACKPACK,
			MEDICAL_1,
			MEDICAL_2,
			MEDICAL_3,
			MEDICAL_4,
			PAUSE);

	private static final List<GameAction> CONTROLLER_ACTIONS = Arrays.asList(
			FIRE,
			CONTROLLER_INTERACT,
			CONTROLLER_RELOAD,
			CONTROLLER_BACKPACK,
			CONTROLLER_SPRINT,
			CONTROLLER_MEDICAL_1,
			CONTROLLER_MEDICAL_2,
			CONTROLLER_MEDICAL_3,
			CONTROLLER_MEDICAL_4,
			PAUSE);

	private static int revision;

	private BukovInputBindings() {
	}

	public static ArrayList<GameAction> actions(boolean controller) {
		return new ArrayList<>(
				controller ? CONTROLLER_ACTIONS : KEYBOARD_ACTIONS);
	}

	public static String labelKey(GameAction action, boolean controller) {
		if (action == MOVE_UP) return "move_up";
		if (action == MOVE_LEFT) return "move_left";
		if (action == MOVE_DOWN) return "move_down";
		if (action == MOVE_RIGHT) return "move_right";
		if (action == FIRE) return controller ? "fire" : "fire_keyboard";
		if (action == PAUSE) return "pause";
		if (controller) {
			if (action == CONTROLLER_INTERACT) return "interact";
			if (action == CONTROLLER_RELOAD) return "reload";
			if (action == CONTROLLER_BACKPACK) return "backpack";
			if (action == CONTROLLER_SPRINT) return "sprint";
			if (action == CONTROLLER_MEDICAL_1) return "medical_1";
			if (action == CONTROLLER_MEDICAL_2) return "medical_2";
			if (action == CONTROLLER_MEDICAL_3) return "medical_3";
			if (action == CONTROLLER_MEDICAL_4) return "medical_4";
		} else {
			if (action == INTERACT) return "interact";
			if (action == RELOAD) return "reload";
			if (action == BACKPACK) return "backpack";
			if (action == MEDICAL_1) return "medical_1";
			if (action == MEDICAL_2) return "medical_2";
			if (action == MEDICAL_3) return "medical_3";
			if (action == MEDICAL_4) return "medical_4";
		}
		return "other";
	}

	public static boolean isFire(GameAction action) {
		return action == FIRE;
	}

	public static boolean isReload(GameAction action, boolean controller) {
		return action == (controller ? CONTROLLER_RELOAD : RELOAD);
	}

	public static boolean isInteract(GameAction action, boolean controller) {
		return action == (controller ? CONTROLLER_INTERACT : INTERACT);
	}

	public static boolean isBackpack(GameAction action, boolean controller) {
		return action == (controller ? CONTROLLER_BACKPACK : BACKPACK)
				|| !controller
				&& (action == SPDAction.INVENTORY_SELECTOR
						|| action == SPDAction.CYCLE);
	}

	public static boolean isSprint(GameAction action, boolean controller) {
		return controller && action == CONTROLLER_SPRINT;
	}

	public static int medicalSlot(GameAction action, boolean controller) {
		if (controller) {
			if (action == CONTROLLER_MEDICAL_1) return 1;
			if (action == CONTROLLER_MEDICAL_2) return 2;
			if (action == CONTROLLER_MEDICAL_3) return 3;
			if (action == CONTROLLER_MEDICAL_4) return 4;
		} else {
			if (action == MEDICAL_1) return 1;
			if (action == MEDICAL_2) return 2;
			if (action == MEDICAL_3) return 3;
			if (action == MEDICAL_4) return 4;
		}
		return 0;
	}

	/**
	 * Compatibility seam for renderer-free tests and older controller events.
	 */
	public static int medicalSlot(GameAction action, int keyCode) {
		boolean controller = ControllerHandler.icControllerKey(keyCode);
		int slot = medicalSlot(action, controller);
		if (slot > 0) return slot;
		int offset = ControllerHandler.DPAD_KEY_OFFSET;
		if (keyCode == Input.Keys.DPAD_UP + offset) return 1;
		if (keyCode == Input.Keys.DPAD_RIGHT + offset) return 2;
		if (keyCode == Input.Keys.DPAD_DOWN + offset) return 3;
		if (keyCode == Input.Keys.DPAD_LEFT + offset) return 4;
		return 0;
	}

	public static int revision() {
		return revision;
	}

	public static void bindingsChanged() {
		revision++;
	}
}
