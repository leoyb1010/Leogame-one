package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.BukovInputBindings;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.watabou.input.GameAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bukov-only binding editor backed by the realtime input action catalog.
 */
public final class WndBukovKeyBindings extends WndKeyBindings {

	public WndBukovKeyBindings(boolean controller) {
		super(
				controller,
				BukovInputBindings.actions(controller),
				labels(controller),
				entryMessage(controller
						? "bindings.controller_info"
						: "bindings.keyboard_info"),
				entryMessage("bindings.action.other"),
				entryMessage("bindings.conflict_protected"));
	}

	@Override
	public void hide() {
		BukovInputBindings.bindingsChanged();
		super.hide();
	}

	private static Map<GameAction, String> labels(boolean controller) {
		List<GameAction> actions = BukovInputBindings.actions(controller);
		Map<GameAction, String> result = new LinkedHashMap<>();
		for (GameAction action : actions) {
			result.put(
					action,
					entryMessage(
							"bindings.action."
									+ BukovInputBindings.labelKey(
											action, controller)));
		}
		return result;
	}

	private static String entryMessage(String key, Object... args) {
		return BukovMessages.get("bukov.entry." + key, args);
	}
}
