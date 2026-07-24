package com.shatteredpixel.shatteredpixeldungeon.bukov.audio;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the production path from firearm data through fixed-step timing to
 * playback. Unit tests cover the crossing math; this test prevents a future
 * refactor from leaving that tested contract disconnected from the live world.
 */
public class BukovFirearmAudioProductionWiringTest {

	@Test
	public void fixedStepOwnsReloadCueCrossingsAndClearsInterruptedState()
			throws Exception {
		String fireControl = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/combat/FireControl.java");

		assertTrue(fireControl.contains("ReloadAudioCueResolver.crossed("));
		assertTrue(fireControl.contains(
				"float previousElapsed = reloadDuration - reloadRemaining;"));
		assertTrue(fireControl.contains(
				"reloadRemaining = Math.max(0f, reloadRemaining - dt);"));
		assertTrue(fireControl.contains(
				"sink.reloadAudioCues(definition, cueMask);"));
		assertTrue(fireControl.indexOf(
				"sink.reloadAudioCues(definition, cueMask);")
				< fireControl.indexOf("sink.reloadFinished();"));
		assertTrue(fireControl.contains("public void cancelReload()"));
		assertTrue(fireControl.contains("reloadDuration = 0f;"));
		assertTrue(fireControl.contains("cancelReload();"));
	}

	@Test
	public void liveWorldPlaysAuthoredGunshotFamilyAndMechanicalReloadAssets()
			throws Exception {
		String world = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java");
		String firearm = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/combat/firearms/Firearm.java");
		String assets = source(
				"src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java");

		assertTrue(world.contains(
				"definition.audioProfile.gunshotFamily.mechanicalAsset(sequence)"));
		assertTrue(world.contains(
				"definition.audioProfile.gunshotFamily.bodyAsset(sequence)"));
		assertTrue(world.contains("acousticSpace.tailAsset(sequence)"));
		assertTrue(world.contains("playPlayerGunshotLayers("));
		assertTrue(world.contains("GunshotAcousticSpaceResolver.resolve("));
		assertFalse(world.contains("Assets.Sounds.Bukov.GUNSHOT_PLAYER"));
		assertTrue(world.contains("public void reloadAudioCues("));
		assertTrue(world.contains("ReloadAudioCue.values()"));
		assertTrue(world.contains("ReloadAudioCueResolver.contains("));
		assertTrue(world.contains("cue.asset()"));
		assertTrue(world.contains("fireControl.resetForWeaponSwap();"));

		assertTrue(firearm.contains(
				"result.audioProfile = source.audioProfile;"));

		assertTrue(assets.contains("GUNSHOT_PISTOL"));
		assertTrue(assets.contains("GUNSHOT_SMG"));
		assertTrue(assets.contains("GUNSHOT_CARBINE"));
		assertTrue(assets.contains("GUNSHOT_RIFLE"));
		assertTrue(assets.contains("GUNSHOT_SHOTGUN"));
		assertTrue(assets.contains("GUNSHOT_HEAVY"));
		assertTrue(assets.contains("GUNSHOT_PISTOL_MECHANICAL"));
		assertTrue(assets.contains("GUNSHOT_PISTOL_BODY"));
		assertTrue(assets.contains("GUNSHOT_TAIL_INDOOR"));
		assertTrue(assets.contains("GUNSHOT_TAIL_CORRIDOR"));
		assertTrue(assets.contains("GUNSHOT_TAIL_OPEN"));
		assertTrue(assets.contains("RELOAD_MAG_OUT"));
		assertTrue(assets.contains("RELOAD_MAG_IN"));
		assertTrue(assets.contains("RELOAD_CHARGE"));
	}

	private static String source(String path) throws Exception {
		return new String(
				Files.readAllBytes(Paths.get(path)),
				StandardCharsets.UTF_8);
	}
}
