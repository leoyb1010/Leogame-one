package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class BukovUiTokensTest {

	@Test
	public void loadsAuthoredPresentationContract() throws IOException {
		BukovUiTokens tokens = BukovUiTokens.parse(authoredJson());

		assertEquals(0x10242D, tokens.color("ink.background"));
		assertEquals(0x02090C, tokens.color("ink.shadow"));
		assertEquals(0x101C20, tokens.color("panel.deep"));
		assertEquals(0xE05A3A, tokens.color("accent.danger"));
		assertEquals(120, tokens.motionMs("fast"));
		assertEquals(64, tokens.vfxPoolCapacity("tracer"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingRequiredColor() {
		BukovUiTokens.parse(minimalJson("\"ink.background\":\"#10242D\""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidHexColor() throws IOException {
		BukovUiTokens.parse(
				authoredJson().replace("#10242D", "#12GG2D"));
	}

	private static String minimalJson(String colors) {
		return "{"
				+ "\"uiTokensVersion\":1,"
				+ "\"colors\":{" + colors + "},"
				+ "\"typographyPx\":[9,11,14,18,24],"
				+ "\"motionMs\":{\"instant\":70,\"fast\":120,\"base\":180,"
				+ "\"slow\":320,\"ritual\":900},"
				+ "\"haptics\":{"
				+ haptic("a") + "," + haptic("b") + "," + haptic("c") + ","
				+ haptic("d") + "," + haptic("e") + "," + haptic("f")
				+ "},"
				+ "\"vfxPoolCapacity\":{\"muzzleFlash\":16,\"tracer\":64,"
				+ "\"shell\":32,\"impactSpark\":48,\"bloodMist\":32,"
				+ "\"bulletMark\":96,\"explosion\":8}"
				+ "}";
	}

	private static String authoredJson() throws IOException {
		return new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/ui_tokens.json")),
				StandardCharsets.UTF_8);
	}

	private static String haptic(String name) {
		return "\"" + name + "\":{"
				+ "\"amplitudePx\":1,\"durationMs\":80,\"frequency\":\"high\"}";
	}
}
