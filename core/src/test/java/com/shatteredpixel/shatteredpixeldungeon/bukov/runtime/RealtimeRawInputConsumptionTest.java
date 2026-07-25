package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.ControllerHandler;
import com.watabou.input.GameAction;
import com.watabou.input.InputHandler;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.GameSettings;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Behavior tests for the raw FIRE path and interface-dismissal latch. */
public class RealtimeRawInputConsumptionTest {

	@Test
	public void bukovFireReceivesRawClickWithoutEmulatingUiClick() {
		withInput(new InputState(), state -> {
			CountingInputHandler handler =
					(CountingInputHandler)Game.inputHandler;
			RealtimeInput input = new RealtimeInput();
			input.start();
			try {
				fireKey(true);

				InputFrame frame = input.poll(new RealtimeBody());
				assertTrue(frame.firePressed);
				assertTrue(frame.fireHeld);
				assertEquals(
						"one raw FIRE event must not also click the UI",
						0,
						handler.emulatedTouches);
			} finally {
				input.stop();
			}

			// With the Bukov raw listener gone, the host keeps its original
			// LEFT_CLICK conversion behavior.
			fireKey(true);
			assertEquals(1, handler.emulatedTouches);
		});
	}

	@Test
	public void closingBackpackDropsSameFrameInventoryAndFireEdges() {
		withInput(new InputState(), state -> {
			RealtimeInput input = new RealtimeInput();
			RealtimeBody hero = new RealtimeBody();
			input.start();
			try {
				inventoryKey(true);
				// Actual order: raw listener -> window hide/destroy -> world
				// update/sample. No paused() call is required in between.
				input.suppressInterfaceInputUntilRelease();
				assertFalse(input.poll(hero).backpackPressed);

				inventoryKey(false);
				inventoryKey(true);
				assertTrue(
						"a fresh press after release must still work",
						input.poll(hero).backpackPressed);
				inventoryKey(false);

				fireKey(true);
				input.suppressInterfaceInputUntilRelease();
				InputFrame dismissed = input.poll(hero);
				assertFalse(dismissed.firePressed);
				assertFalse(dismissed.fireHeld);

				fireKey(false);
				assertFalse(input.poll(hero).firePressed);
				fireKey(true);
				assertTrue(
						"a fresh FIRE press after release must shoot",
						input.poll(hero).firePressed);
			} finally {
				input.stop();
			}
		});
	}

	@Test
	public void mouseHeldWhileWindowClosesCannotBecomeResumeShot() {
		InputState device = new InputState();
		withInput(device, state -> {
			RealtimeInput input = new RealtimeInput();
			RealtimeBody hero = new RealtimeBody();
			input.start();
			try {
				state.leftHeld = true;
				state.leftJustPressed = true;
				input.suppressInterfaceInputUntilRelease();
				InputFrame closeFrame = input.poll(hero);
				assertFalse(closeFrame.firePressed);
				assertFalse(closeFrame.fireHeld);

				state.leftJustPressed = false;
				InputFrame heldFrame = input.poll(hero);
				assertFalse(heldFrame.firePressed);
				assertFalse(heldFrame.fireHeld);

				state.leftHeld = false;
				assertFalse(input.poll(hero).firePressed);

				state.leftHeld = true;
				state.leftJustPressed = true;
				InputFrame freshPress = input.poll(hero);
				assertTrue(freshPress.firePressed);
				assertTrue(freshPress.fireHeld);
			} finally {
				input.stop();
			}
		});
	}

	private static void fireKey(boolean pressed) {
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.F, pressed));
		KeyEvent.processKeyEvents();
	}

	private static void inventoryKey(boolean pressed) {
		KeyEvent.addKeyEvent(new KeyEvent(Input.Keys.I, pressed));
		KeyEvent.processKeyEvents();
	}

	private static void withInput(InputState state, Scenario scenario) {
		Input previousInput = Gdx.input;
		InputHandler previousHandler = Game.inputHandler;
		Camera previousCamera = Camera.main;
		int previousWidth = Game.width;
		int previousHeight = Game.height;
		boolean previousControllerActive = ControllerHandler.controllerActive;
		LinkedHashMap<Integer, GameAction> previousBindings =
				KeyBindings.getAllBindings();
		try {
			KeyEvent.clearListeners();
			GameSettings.set(new MemoryPreferences());
			Gdx.input = state.proxy();
			Game.inputHandler = new CountingInputHandler(Gdx.input);
			Game.width = 1280;
			Game.height = 960;
			Camera.main = new Camera(0, 0, Game.width, Game.height, 1f);
			ControllerHandler.controllerActive = false;
			LinkedHashMap<Integer, GameAction> bindings =
					new LinkedHashMap<>();
			bindings.put(Input.Keys.F, SPDAction.LEFT_CLICK);
			bindings.put(Input.Keys.I, SPDAction.INVENTORY);
			KeyBindings.setAllBindings(bindings);
			scenario.run(state);
		} finally {
			KeyEvent.clearListeners();
			KeyBindings.setAllBindings(previousBindings);
			ControllerHandler.controllerActive = previousControllerActive;
			Camera.main = previousCamera;
			Game.width = previousWidth;
			Game.height = previousHeight;
			Game.inputHandler = previousHandler;
			Gdx.input = previousInput;
			GameSettings.set(null);
		}
	}

	private interface Scenario {
		void run(InputState state);
	}

	private static final class CountingInputHandler extends InputHandler {
		int emulatedTouches;

		CountingInputHandler(Input input) {
			super(input);
		}

		@Override
		public void emulateTouch(int id, int button, boolean down) {
			emulatedTouches++;
		}
	}

	private static final class InputState implements InvocationHandler {
		boolean leftHeld;
		boolean leftJustPressed;

		Input proxy() {
			return (Input)Proxy.newProxyInstance(
					Input.class.getClassLoader(),
					new Class<?>[] {Input.class},
					this);
		}

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method method,
							 Object[] arguments) {
			if ("isButtonPressed".equals(method.getName())
					&& arguments != null
					&& (Integer)arguments[0] == Input.Buttons.LEFT) {
				return leftHeld;
			}
			if ("isButtonJustPressed".equals(method.getName())
					&& arguments != null
					&& (Integer)arguments[0] == Input.Buttons.LEFT) {
				return leftJustPressed;
			}
			Class<?> type = method.getReturnType();
			if (type == boolean.class) return false;
			if (type == int.class) return 0;
			if (type == long.class) return 0L;
			if (type == float.class) return 0f;
			if (type == double.class) return 0d;
			return null;
		}
	}

	private static final class MemoryPreferences implements Preferences {
		private final Map<String, Object> values = new HashMap<>();

		@Override
		public Preferences putBoolean(String key, boolean value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putInteger(String key, int value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putLong(String key, long value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putFloat(String key, float value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences putString(String key, String value) {
			values.put(key, value);
			return this;
		}

		@Override
		public Preferences put(Map<String, ?> additions) {
			values.putAll(additions);
			return this;
		}

		@Override
		public boolean getBoolean(String key) {
			return getBoolean(key, false);
		}

		@Override
		public int getInteger(String key) {
			return getInteger(key, 0);
		}

		@Override
		public long getLong(String key) {
			return getLong(key, 0L);
		}

		@Override
		public float getFloat(String key) {
			return getFloat(key, 0f);
		}

		@Override
		public String getString(String key) {
			return getString(key, "");
		}

		@Override
		public boolean getBoolean(String key, boolean fallback) {
			Object value = values.get(key);
			return value instanceof Boolean ? (Boolean)value : fallback;
		}

		@Override
		public int getInteger(String key, int fallback) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number)value).intValue() : fallback;
		}

		@Override
		public long getLong(String key, long fallback) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number)value).longValue() : fallback;
		}

		@Override
		public float getFloat(String key, float fallback) {
			Object value = values.get(key);
			return value instanceof Number
					? ((Number)value).floatValue() : fallback;
		}

		@Override
		public String getString(String key, String fallback) {
			Object value = values.get(key);
			return value instanceof String ? (String)value : fallback;
		}

		@Override
		public Map<String, ?> get() {
			return Collections.unmodifiableMap(new HashMap<>(values));
		}

		@Override
		public boolean contains(String key) {
			return values.containsKey(key);
		}

		@Override
		public void clear() {
			values.clear();
		}

		@Override
		public void remove(String key) {
			values.remove(key);
		}

		@Override
		public void flush() {
		}
	}
}
