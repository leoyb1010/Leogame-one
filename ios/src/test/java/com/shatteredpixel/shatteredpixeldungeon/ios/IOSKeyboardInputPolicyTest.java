package com.shatteredpixel.shatteredpixeldungeon.ios;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IOSKeyboardInputPolicyTest {

	@Test
	public void nativeKeyboardDoesNotInjectSentinelText() throws Exception {
		String launcher = new String(
				Files.readAllBytes(Paths.get(
						"src/main/java/com/shatteredpixel/"
								+ "shatteredpixeldungeon/ios/IOSLauncher.java")),
				StandardCharsets.UTF_8);

		assertTrue(launcher.contains("return new DefaultIOSInput(this);"));
		assertFalse(launcher.contains(
				"getActiveKeyboardTextField()).setText("));
	}
}
