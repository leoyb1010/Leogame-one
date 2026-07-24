package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExperienceContractRegistryTest {

	@Test
	public void loadsFourChannelDefaultsAndGateFiveCeilings() {
		ExperienceContract contract = ExperienceContractTestFixture.load();
		BukovExperienceSettings settings =
				BukovExperienceSettings.defaults(contract);

		assertEquals(0.8f, settings.channelGain(AudioChannel.MASTER), 0f);
		assertEquals(0.56f, settings.channelGain(AudioChannel.MUSIC), 0.0001f);
		assertEquals(0.72f, settings.channelGain(AudioChannel.SFX), 0.0001f);
		assertEquals(0.56f, settings.channelGain(AudioChannel.AMBIENCE), 0.0001f);
		assertEquals(8f, contract.maximumShakePx, 0f);
		assertEquals(
				120,
				contract.profile(
						CombatFeedbackType.BOSS_PHASE_BREAK
				).hitstopMs
		);
	}

	@Test
	public void authoredFeedbackTakesHapticsFromUiTokens()
			throws Exception {
		String experience = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/"
								+ "experience_contract.json")),
				StandardCharsets.UTF_8);
		String ui = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/ui_tokens.json")),
				StandardCharsets.UTF_8);
		BukovUiTokens tokens = BukovUiTokens.parse(ui);
		ExperienceContract contract =
				new ExperienceContractRegistry().loadJson(
						experience, tokens);

		assertFalse(experience.contains("\"shakeAmplitudePx\""));
		assertFalse(experience.contains("\"vibrationAmplitude\""));
		assertFalse(experience.contains("\"maximumShakePx\""));
		assertEquals(tokens.maximumShakePx(), contract.maximumShakePx, 0f);
		assertEquals(
				tokens.haptic("PLAYER_HIT").shakeAmplitudePx(),
				contract.profile(
						CombatFeedbackType.PLAYER_HIT)
						.shakeAmplitudePx,
				0f);
		assertEquals(
				tokens.haptic("PLAYER_HIT").vibrationDurationMs(),
				contract.profile(
						CombatFeedbackType.PLAYER_HIT)
						.vibrationDurationMs);
		assertEquals(
				tokens.haptic("PLAYER_HIT").frequency(),
				contract.profile(
						CombatFeedbackType.PLAYER_HIT).frequency);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingFeedbackProfiles() {
		new ExperienceContractRegistry().loadJson(
				"{\"schemaVersion\":1,"
						+ "\"mixDefaults\":{\"master\":80,\"music\":70,"
						+ "\"sfx\":90,\"ambience\":70},"
						+ "\"spatialAudio\":{\"fullVolumeDistance\":1,"
						+ "\"referenceDistance\":8,\"referenceDecibels\":-12,"
						+ "\"wallDecibels\":-6,\"lowPassDistance\":15,"
						+ "\"lowPassHz\":1000,\"minimumAudibleDecibels\":-48},"
						+ "\"keySoundVisualization\":{\"nearDistance\":4,"
						+ "\"midDistance\":10},"
						+ "\"feedback\":{\"maximumShakePx\":8,\"profiles\":[]}}"
		);
	}
}
