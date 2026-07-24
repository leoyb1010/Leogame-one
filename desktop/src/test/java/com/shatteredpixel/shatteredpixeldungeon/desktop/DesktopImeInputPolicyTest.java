package com.shatteredpixel.shatteredpixeldungeon.desktop;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class DesktopImeInputPolicyTest {

	@Test
	public void gameplayDisablesImeAndRealTextInputOwnsTheOptIn()
			throws Exception {
		String input = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/desktop/DesktopImeInput.java");
		String launcher = source(
				"src/main/java/com/shatteredpixel/"
						+ "shatteredpixeldungeon/desktop/DesktopLauncher.java");
		String scene = source(
				"../SPD-classes/src/main/java/com/watabou/noosa/Scene.java");
		String textInput = source(
				"../SPD-classes/src/main/java/com/watabou/noosa/TextInput.java");

		assertTrue(launcher.contains("return new DesktopImeInput(window);"));
		assertTrue(input.contains("GLFW.glfwSetPreeditCallback"));
		assertTrue(input.contains("if (!textInputEnabled)"));
		assertTrue(input.contains("GLFW.GLFW_IME"));
		assertTrue(input.contains("GLFW.GLFW_FALSE"));
		assertTrue(scene.contains(
				"Game.platform.setTextInputEnabled(false);"));
		assertTrue(textInput.contains(
				"Game.platform.setTextInputEnabled(true);"));
		assertTrue(textInput.contains(
				"Game.platform.setTextInputEnabled(false);"));
	}

	@Test
	public void desktopUsesOneImeCapableLwjglAbi() throws Exception {
		String rootBuild = source("../build.gradle");
		String desktopBuild = source("build.gradle");

		assertTrue(rootBuild.contains("lwjglVersion = '3.4.1'"));
		assertTrue(desktopBuild.contains(
				"implementation \"org.lwjgl:lwjgl-glfw:$lwjglVersion\""));
		assertTrue(desktopBuild.contains(
				"implementation \"org.lwjgl:lwjgl-tinyfd:$lwjglVersion\""));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
