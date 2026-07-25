package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.GameAction;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WndKeyBindingsCatalogTest {

	@Test
	public void catalogResetPreservesKeysOwnedByOutsideActions() {
		List<GameAction> catalog = Arrays.asList(
				SPDAction.TAG_RESUME,
				SPDAction.EXAMINE);
		LinkedHashMap<Integer, GameAction> current =
				new LinkedHashMap<>();
		current.put(Input.Keys.R, SPDAction.JOURNAL);
		current.put(Input.Keys.F, SPDAction.TAG_RESUME);
		current.put(Input.Keys.J, SPDAction.HERO_INFO);

		LinkedHashMap<Integer, GameAction> defaults =
				new LinkedHashMap<>();
		defaults.put(Input.Keys.R, SPDAction.TAG_RESUME);
		defaults.put(Input.Keys.E, SPDAction.EXAMINE);

		LinkedHashMap<Integer, GameAction> reset =
				WndKeyBindings.resetCatalogBindings(
						current, defaults, catalog);

		assertEquals(SPDAction.JOURNAL, reset.get(Input.Keys.R));
		assertEquals(SPDAction.HERO_INFO, reset.get(Input.Keys.J));
		assertFalse(reset.containsKey(Input.Keys.F));
		assertEquals(SPDAction.EXAMINE, reset.get(Input.Keys.E));
	}

	@Test
	public void catalogEditorCannotReplaceOutsideAction() {
		List<GameAction> catalog = Arrays.asList(
				SPDAction.TAG_RESUME,
				SPDAction.EXAMINE);

		assertFalse(WndKeyBindings.canReplaceBinding(
				catalog, SPDAction.JOURNAL));
		assertTrue(WndKeyBindings.canReplaceBinding(
				catalog, SPDAction.TAG_RESUME));
		assertTrue(WndKeyBindings.canReplaceBinding(
				catalog, null));
		assertTrue(WndKeyBindings.canReplaceBinding(
				null, SPDAction.JOURNAL));
	}
}
