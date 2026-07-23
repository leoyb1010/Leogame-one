package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;

/** Shared keyboard/controller navigation vocabulary for every Bukov window. */
public final class BukovNavigation {

	public static boolean previous(KeyEvent event) {
		GameAction action = action(event);
		return action == SPDAction.N
				|| action == SPDAction.W
				|| action == SPDAction.TAG_ACTION
				|| action == SPDAction.TAG_LOOT;
	}

	public static boolean next(KeyEvent event) {
		GameAction action = action(event);
		return action == SPDAction.S
				|| action == SPDAction.E
				|| action == SPDAction.TAG_RESUME
				|| action == SPDAction.CYCLE;
	}

	public static boolean confirm(KeyEvent event) {
		GameAction action = action(event);
		return action == SPDAction.WAIT_OR_PICKUP
				|| action == SPDAction.TAG_ATTACK
				|| event != null && event.code == Input.Keys.BUTTON_A;
	}

	public static boolean back(KeyEvent event) {
		GameAction action = action(event);
		return action == SPDAction.BACK
				|| action == SPDAction.WAIT
				|| action == SPDAction.EXAMINE
				|| event != null && (event.code == Input.Keys.BUTTON_B
						|| event.code == Input.Keys.BUTTON_START);
	}

	public static boolean inventory(KeyEvent event) {
		GameAction action = action(event);
		return action == SPDAction.INVENTORY
				|| action == SPDAction.INVENTORY_SELECTOR
				|| event != null && (event.code == Input.Keys.BUTTON_Y
						|| event.code == Input.Keys.TAB);
	}

	private static GameAction action(KeyEvent event) {
		return event == null ? SPDAction.NONE
				: KeyBindings.getActionForKey(event);
	}

	private BukovNavigation() {
	}
}
