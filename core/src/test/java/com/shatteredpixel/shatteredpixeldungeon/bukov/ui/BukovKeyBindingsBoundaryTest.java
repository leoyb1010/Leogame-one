package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.BukovInputBindings;
import com.watabou.input.GameAction;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BukovKeyBindingsBoundaryTest {

	@Test
	public void settingsOnlyReachBukovBindingEditorAndKeepLegal()
			throws Exception {
		String settings = source(
				"bukov/ui/WndBukovSettings.java");
		assertTrue(settings.contains(
				"new WndBukovKeyBindings(controller)"));
		assertFalse(settings.contains("new WndKeyBindings("));
		assertFalse(settings.contains(
				"windows.WndKeyBindings"));
		assertTrue(settings.contains("case LEGAL:"));
		assertTrue(settings.contains("showLegalNotice();"));
	}

	@Test
	public void editorUsesExplicitRealtimeCatalog() throws Exception {
		String editor = source(
				"bukov/ui/WndBukovKeyBindings.java");
		String inheritedEditor = source(
				"windows/WndKeyBindings.java");
		assertTrue(editor.contains(
				"BukovInputBindings.actions(controller)"));
		assertFalse(editor.contains("GameAction.allActions()"));
		assertTrue(inheritedEditor.contains(
				"if (actionCatalog == null)"));
		assertTrue(inheritedEditor.contains(
				"actionList = new ArrayList<>(actionCatalog)"));
		assertTrue(inheritedEditor.contains(
				"resetCatalogBindings("));
		assertTrue(inheritedEditor.contains(
				"!canReplaceBinding("));
		assertTrue(editor.contains(
				"bindings.conflict_protected"));
	}

	@Test
	public void catalogsExcludeInheritedTurnBasedActions() {
		assertRealtimeOnly(BukovInputBindings.actions(false));
		assertRealtimeOnly(BukovInputBindings.actions(true));
	}

	@Test
	public void fireBindingsUseRawEventsBeforeClickEmulation()
			throws Exception {
		String realtime = source(
				"bukov/runtime/RealtimeInput.java");
		String keyEvents = new String(
				Files.readAllBytes(Paths.get(
						"../SPD-classes/src/main/java/com/watabou/"
								+ "input/KeyEvent.java")),
				StandardCharsets.UTF_8);
		assertTrue(realtime.contains(
				"KeyEvent.addRawKeyListener(keyListener)"));
		assertTrue(realtime.contains(
				"KeyEvent.removeRawKeyListener(keyListener)"));
		int rawDispatch = keyEvents.indexOf(
				"rawKeySignal.dispatch(k);");
		int clickEmulation = keyEvents.indexOf(
				"GameAction.LEFT_CLICK");
		assertTrue(rawDispatch >= 0);
		assertTrue(clickEmulation > rawDispatch);
		assertTrue(keyEvents.contains(
				"rawKeySignal.removeAll();"));
	}

	private static void assertRealtimeOnly(List<GameAction> actions) {
		for (GameAction legacy : new GameAction[] {
				SPDAction.HERO_INFO,
				SPDAction.JOURNAL,
				SPDAction.WAIT,
				SPDAction.REST,
				SPDAction.BAG_1,
				SPDAction.BAG_2,
				SPDAction.BAG_3,
				SPDAction.BAG_4,
				SPDAction.BAG_5,
				SPDAction.ZOOM_IN,
				SPDAction.ZOOM_OUT
		}) {
			assertFalse(legacy.name(), actions.contains(legacy));
		}
		assertTrue(actions.contains(BukovInputBindings.FIRE));
		assertTrue(actions.contains(BukovInputBindings.PAUSE));
	}

	private static String source(String relative) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/" + relative)),
				StandardCharsets.UTF_8);
	}
}
