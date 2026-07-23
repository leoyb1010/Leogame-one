package com.shatteredpixel.shatteredpixeldungeon.bukov.settings;

import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.AudioChannel;
import com.shatteredpixel.shatteredpixeldungeon.bukov.fx.CombatFeedbackType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
