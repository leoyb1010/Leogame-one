package com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FirearmBuildTest {

	@Test
	public void opticMagazineAndMuzzleModifyLiveBallisticInputs()
			throws IOException {
		FirearmDefinition base = firearm();
		FirearmBuild build = new FirearmBuild("weapon-uid");
		build.install(FirearmAttachmentCatalog.RED_DOT);
		build.install(FirearmAttachmentCatalog.EXTENDED_MAG);
		build.install(FirearmAttachmentCatalog.SUPPRESSOR);

		EffectiveFirearmStats effective = build.effectiveStats(base);

		assertTrue(effective.effectiveRangeTiles > base.effectiveRangeTiles);
		assertTrue(effective.baseSpreadDeg < base.baseSpreadDeg);
		assertTrue(effective.movingSpreadDeg < base.movingSpreadDeg);
		assertTrue(effective.magazineSize > base.magazineSize);
		assertTrue(effective.reloadSeconds > base.reloadSeconds);
		assertTrue(effective.recoilPerShot < base.recoilPerShot);
		assertTrue(effective.noiseRadiusTiles < base.noiseRadiusTiles);
		assertTrue(effective.damage < base.damage);
		assertTrue(effective.weightKg > base.weightKg);
	}

	@Test
	public void profileRoundTripPreservesOnePartPerSlot() throws IOException {
		BukovProfile profile = new BukovProfile();
		FirearmBuild build = new FirearmBuild("persistent-weapon");
		build.install(FirearmAttachmentCatalog.RED_DOT);
		build.install(FirearmAttachmentCatalog.EXTENDED_MAG);
		build.install(FirearmAttachmentCatalog.SUPPRESSOR);
		profile.firearmBuilds().save(build);

		Bundle bundle = new Bundle();
		bundle.put("profile", profile);
		BukovProfile restored = (BukovProfile) bundle.get("profile");
		FirearmBuild restoredBuild =
				restored.firearmBuilds().build("persistent-weapon");

		assertNotNull(restoredBuild);
		assertEquals(3, restoredBuild.attachments().size());
		assertEquals(
				FirearmAttachmentCatalog.RED_DOT,
				restoredBuild.attachment(FirearmAttachmentSlot.OPTIC));
		assertEquals(
				FirearmAttachmentCatalog.EXTENDED_MAG,
				restoredBuild.attachment(FirearmAttachmentSlot.MAGAZINE));
		assertEquals(
				FirearmAttachmentCatalog.SUPPRESSOR,
				restoredBuild.attachment(FirearmAttachmentSlot.MUZZLE));
		assertTrue(restored.firearmBuilds()
				.effectiveStats("persistent-weapon", firearm())
				.magazineSize > firearm().magazineSize);
	}

	@Test
	public void runtimeFirearmConsumesAndPersistsTheEffectiveBuild()
			throws IOException {
		FirearmRegistry registry = registry();
		FirearmDefinition base = registry.all().iterator().next();
		FirearmBuild build = new FirearmBuild("runtime-weapon");
		build.install(FirearmAttachmentCatalog.RED_DOT);
		build.install(FirearmAttachmentCatalog.EXTENDED_MAG);
		build.install(FirearmAttachmentCatalog.SUPPRESSOR);
		Firearm runtime = new Firearm().configure(
				base.id, "runtime-weapon", 0, base.defaultAmmo);
		runtime.applyBuild(build);

		FirearmDefinition effective = runtime.definition(registry);
		assertTrue(effective.magazineSize > base.magazineSize);
		assertTrue(effective.recoilPerShot < base.recoilPerShot);
		assertTrue(effective.noiseRadiusTiles < base.noiseRadiusTiles);
		assertEquals(
				base.audioProfile.gunshotFamily,
				effective.audioProfile.gunshotFamily);

		Bundle bundle = new Bundle();
		bundle.put("firearm", runtime);
		Firearm restored = (Firearm) bundle.get("firearm");
		assertNotNull(restored.attachmentBuild());
		assertEquals(
				effective.magazineSize,
				restored.definition(registry).magazineSize);
		assertEquals(
				effective.noiseRadiusTiles,
				restored.definition(registry).noiseRadiusTiles,
				0.0001f);
	}

	private static FirearmDefinition firearm() throws IOException {
		return registry().all().iterator().next();
	}

	private static FirearmRegistry registry() throws IOException {
		FirearmRegistry registry = new FirearmRegistry();
		registry.loadJson(new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/firearms.json")),
				StandardCharsets.UTF_8));
		return registry;
	}
}
