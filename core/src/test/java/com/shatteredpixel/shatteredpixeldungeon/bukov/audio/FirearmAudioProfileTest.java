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
	public void sixGunshotFamiliesResolveToSixDifferentAssets() {
		Set<String> assets = new HashSet<>();
		for (GunshotSoundFamily family : GunshotSoundFamily.values()) {
			assertTrue(family.asset().endsWith(".wav"));
			assets.add(family.asset());
		}
		assertEquals(6, assets.size());
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
