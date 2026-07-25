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
		assertEquals(7, tokens.typographyPx("hud"));
		assertEquals(8, tokens.typographyPx("body"));
		assertEquals(9, tokens.typographyPx("section"));
		assertEquals(12, tokens.typographyPx("title"));
		assertEquals(16, tokens.typographyPx("display"));
		assertEquals(120, tokens.motionMs("fast"));
		assertEquals(8f, tokens.maximumShakePx(), 0f);
		assertEquals(
				2.5f,
				tokens.haptic("PLAYER_HIT").shakeAmplitudePx(),
				0f);
		assertEquals(
				120,
				tokens.haptic("PLAYER_HIT").shakeDurationMs());
		assertEquals(
				0.7f,
				tokens.haptic("PLAYER_HIT").vibrationAmplitude(),
				0f);
		assertEquals(
				120,
				tokens.haptic("PLAYER_HIT").vibrationDurationMs());
		assertEquals(
				"medium",
				tokens.haptic("PLAYER_HIT").frequency());
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

	@Test(expected = IllegalArgumentException.class)
	public void rejectsTypographyOutsideAuthoredScale()
			throws IOException {
		BukovUiTokens.parse(
				authoredJson().replace("\"hud\": 7", "\"hud\": 6"));
	}

	private static String minimalJson(String colors) {
		return "{"
				+ "\"uiTokensVersion\":1,"
				+ "\"colors\":{" + colors + "},"
				+ "\"typographyPx\":{\"hud\":7,\"body\":8,"
				+ "\"section\":9,\"title\":12,\"display\":16},"
				+ "\"motionMs\":{\"instant\":70,\"fast\":120,\"base\":180,"
				+ "\"slow\":320,\"ritual\":900},"
				+ "\"hapticMaximumShakePx\":8,"
				+ "\"haptics\":{"
				+ haptic("RIFLE_SHOT") + ","
				+ haptic("PLAYER_HIT") + ","
				+ haptic("SHOTGUN_NEAR") + ","
				+ haptic("EXPLOSION") + ","
				+ haptic("BOSS_SLAM") + ","
				+ haptic("BOSS_OVERLOAD") + ","
				+ haptic("EXTRACT_STAMP") + ","
				+ haptic("KILL") + ","
				+ haptic("WEAKPOINT_KILL") + ","
				+ haptic("BOSS_PHASE_BREAK")
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
				+ "\"shakeAmplitudePx\":1,\"shakeDurationMs\":80,"
				+ "\"vibrationAmplitude\":1,"
				+ "\"vibrationDurationMs\":80,\"frequency\":\"high\"}";
	}
}
