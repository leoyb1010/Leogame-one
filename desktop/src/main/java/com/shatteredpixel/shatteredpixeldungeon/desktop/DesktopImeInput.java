/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWPreeditCallback;

/**
 * Keeps desktop IME composition scoped to a real TextInput.
 *
 * GLFW's Cocoa view is always an NSTextInputClient. A Chinese input source can
 * therefore open its candidate window for ordinary WASD/R/E/Tab gameplay even
 * when no libGDX text widget exists. GLFW may re-enable IME when composition
 * starts, so the preedit callback closes it again unless TextInput opted in.
 */
final class DesktopImeInput extends DefaultLwjgl3Input {

	private static DesktopImeInput active;
	private static boolean requestedTextInputEnabled;

	private long windowHandle;
	private boolean textInputEnabled;
	private GLFWPreeditCallback preeditCallback;

	DesktopImeInput(Lwjgl3Window window) {
		super(window);
		active = this;
		applyTextInputEnabled(requestedTextInputEnabled);
	}

	static void setTextInputEnabled(boolean enabled) {
		requestedTextInputEnabled = enabled;
		if (active != null) {
			active.applyTextInputEnabled(enabled);
		}
	}

	@Override
	public void windowHandleChanged(long windowHandle) {
		super.windowHandleChanged(windowHandle);
		long previousHandle = this.windowHandle;
		this.windowHandle = windowHandle;
		GLFWPreeditCallback previousCallback = preeditCallback;
		preeditCallback = GLFWPreeditCallback.create(
				(window, preeditCount, preeditString, blockCount,
						blockSizes, focusedBlock, caret) -> {
					if (!textInputEnabled) {
						GLFW.glfwSetInputMode(
								window,
								GLFW.GLFW_IME,
								GLFW.GLFW_FALSE);
					}
				});
		GLFW.glfwSetPreeditCallback(windowHandle, preeditCallback);
		if (previousCallback != null && previousHandle == windowHandle) {
			previousCallback.free();
		}
		applyTextInputEnabled(textInputEnabled);
	}

	private void applyTextInputEnabled(boolean enabled) {
		textInputEnabled = enabled;
		if (windowHandle == 0L) {
			return;
		}
		if (!enabled) {
			GLFW.glfwResetPreeditText(windowHandle);
		}
		GLFW.glfwSetInputMode(
				windowHandle,
				GLFW.GLFW_IME,
				enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
	}
}
