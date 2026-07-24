package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmClass;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FirearmAudioProfileTest {

	@Test
	public void sixGunshotFamiliesExposeThreeSeparateLayerVariants() {
		Set<String> mechanicalAssets = new HashSet<>();
		Set<String> bodyAssets = new HashSet<>();
		for (GunshotSoundFamily family : GunshotSoundFamily.values()) {
			Set<String> familyMechanical = new HashSet<>();
			Set<String> familyBody = new HashSet<>();
			for (int sequence = 0; sequence < 3; sequence++) {
				String mechanical = family.mechanicalAsset(sequence);
				String body = family.bodyAsset(sequence);
				assertTrue(mechanical.endsWith(".wav"));
				assertTrue(body.endsWith(".wav"));
				familyMechanical.add(mechanical);
				familyBody.add(body);
				mechanicalAssets.add(mechanical);
				bodyAssets.add(body);
			}
			assertEquals(3, familyMechanical.size());
			assertEquals(3, familyBody.size());
		}
		assertEquals(18, mechanicalAssets.size());
		assertEquals(18, bodyAssets.size());
	}

	@Test
	public void weaponClassesChooseExpectedFamilies() {
		assertEquals(GunshotSoundFamily.PISTOL,
				FirearmAudioProfile.defaultFor(
						FirearmClass.PISTOL).gunshotFamily);
		assertEquals(GunshotSoundFamily.SMG,
				FirearmAudioProfile.defaultFor(
						FirearmClass.SUBMACHINE_GUN).gunshotFamily);
		assertEquals(GunshotSoundFamily.CARBINE,
				FirearmAudioProfile.defaultFor(
						FirearmClass.CARBINE).gunshotFamily);
		assertEquals(GunshotSoundFamily.RIFLE,
				FirearmAudioProfile.defaultFor(
						FirearmClass.ASSAULT_RIFLE).gunshotFamily);
		assertEquals(GunshotSoundFamily.SHOTGUN,
				FirearmAudioProfile.defaultFor(
						FirearmClass.SHOTGUN).gunshotFamily);
		assertEquals(GunshotSoundFamily.HEAVY,
				FirearmAudioProfile.defaultFor(
						FirearmClass.HEAVY_WEAPON).gunshotFamily);
	}

	@Test
	public void longFrameEmitsEveryCrossedReloadPhaseOnce() {
		FirearmAudioProfile profile = new FirearmAudioProfile(
				GunshotSoundFamily.RIFLE,
				0.10f,
				0.60f,
				0.90f);
		int first = ReloadAudioCueResolver.crossed(
				profile, 0f, 1.3f, 2f);
		assertTrue(ReloadAudioCueResolver.contains(
				first, ReloadAudioCue.MAG_OUT));
		assertTrue(ReloadAudioCueResolver.contains(
				first, ReloadAudioCue.MAG_IN));
		assertFalse(ReloadAudioCueResolver.contains(
				first, ReloadAudioCue.CHARGE));

		int second = ReloadAudioCueResolver.crossed(
				profile, 1.3f, 2f, 2f);
		assertTrue(ReloadAudioCueResolver.contains(
				second, ReloadAudioCue.CHARGE));
		assertFalse(ReloadAudioCueResolver.contains(
				second, ReloadAudioCue.MAG_IN));
	}

	@Test(expected = IllegalArgumentException.class)
	public void reloadFractionsMustBeStrictlyOrdered() {
		new FirearmAudioProfile(
				GunshotSoundFamily.RIFLE,
				0.4f,
				0.3f,
				0.9f);
	}
}
