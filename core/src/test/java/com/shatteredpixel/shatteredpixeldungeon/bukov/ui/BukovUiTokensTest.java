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
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/ui_tokens.json"
				)),
				StandardCharsets.UTF_8
		);
		BukovUiTokens tokens = BukovUiTokens.parse(json);

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
	public void rejectsInvalidHexColor() {
		BukovUiTokens.parse(minimalJson(
				requiredColors().replace("#10242D", "#12GG2D")
		));
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

	private static String requiredColors() {
		return "\"ink.background\":\"#10242D\","
				+ "\"ink.shadow\":\"#02090C\","
				+ "\"ink.loading\":\"#07100E\","
				+ "\"ink.failure\":\"#050B0D\","
				+ "\"panel.surface\":\"#1A3644\","
				+ "\"panel.deep\":\"#101C20\","
				+ "\"panel.result\":\"#101514\","
				+ "\"panel.border\":\"#3A5A66\","
				+ "\"accent.interact\":\"#4FA7A0\","
				+ "\"accent.valuable\":\"#E3B94E\","
				+ "\"accent.danger\":\"#E05A3A\","
				+ "\"accent.extract\":\"#6FCF97\","
				+ "\"text.primary\":\"#E8F1F0\","
				+ "\"text.secondary\":\"#9FB8B4\","
				+ "\"text.disabled\":\"#5A7076\"";
	}

	private static String haptic(String name) {
		return "\"" + name + "\":{"
				+ "\"amplitudePx\":1,\"durationMs\":80,\"frequency\":\"high\"}";
	}
}
