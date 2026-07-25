package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.GameSettings;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The realtime listener sits on the raw key signal, which an open window does
 * not consume. A paused frame never calls sample(), so the world must drop the
 * edges a window keystroke latched; otherwise closing the backpack immediately
 * reopens it and a keypress made inside a window fires on resume.
 */
public class RealtimeInputPauseLatchTest {

	@Test
	public void keystrokesDuringAPausedFrameDoNotSurviveTheResume() {
		withHeadlessInput(() -> {
			RealtimeInput input = new RealtimeInput();
			RealtimeBody hero = new RealtimeBody();
			input.start();
			try {
				pressInventoryKey();
				assertTrue(
						"the raw listener must still observe the keystroke",
						input.poll(hero).backpackPressed);

				pressInventoryKey();
				// What the interface transition and paused world both do.
				input.suppressInterfaceInputUntilRelease();

				assertFalse(
						"a window keystroke must not reopen the backpack",
						input.poll(hero).backpackPressed);
			} finally {
				input.stop();
			}
		});
	}

	/**
	 * A headless world cannot open a real window, so this supplements the
	 * behavior tests with a narrow production-wiring assertion.
	 */
	@Test
	public void pausedWorldResetsEveryInputSurface() throws Exception {
		String world = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/bukov/runtime/"
								+ "BukovRealtimeWorld.java")),
				StandardCharsets.UTF_8);
		int start = world.indexOf("public boolean paused() {");
		assertTrue(start >= 0);
		String body = world.substring(start, world.indexOf("\n\t}", start));
		assertTrue(body.contains(
				"input.suppressInterfaceInputUntilRelease();"));
		assertFalse(body.contains("input.cancelTouches();"));
	}

	private static void pressInventoryKey() {
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.I, true));
		KeyEvent.processKeyEvents();
	}

	private static void withHeadlessInput(Runnable body) {
		Input previousInput = Gdx.input;
		Camera previousCamera = Camera.main;
		int previousWidth = Game.width;
		int previousHeight = Game.height;
		LinkedHashMap<Integer, GameAction> previousBindings =
				KeyBindings.getAllBindings();
		try {
			GameSettings.set(
					new BukovPlayerJourneyAcceptanceTest
							.MemoryPreferences());
			Gdx.input = idleInput();
			Game.width = 1280;
			Game.height = 960;
			Camera.main = new Camera(0, 0, Game.width, Game.height, 1f);
			LinkedHashMap<Integer, GameAction> bindings =
					new LinkedHashMap<>();
			bindings.put(Input.Keys.I, SPDAction.INVENTORY);
			KeyBindings.setAllBindings(bindings);
			body.run();
		} finally {
			KeyBindings.setAllBindings(previousBindings);
			Camera.main = previousCamera;
			Game.width = previousWidth;
			Game.height = previousHeight;
			Gdx.input = previousInput;
			GameSettings.set(null);
		}
	}

	/** No key or button is ever held, so only listener edges can set a frame. */
	private static Input idleInput() {
		InvocationHandler handler = (proxy, method, arguments) -> {
			Class<?> type = method.getReturnType();
			if (type == boolean.class) return false;
			if (type == int.class) return 0;
			if (type == long.class) return 0L;
			if (type == float.class) return 0f;
			if (type == double.class) return 0d;
			return null;
		};
		return (Input)Proxy.newProxyInstance(
				Input.class.getClassLoader(),
				new Class<?>[] {Input.class},
				handler);
	}
}
