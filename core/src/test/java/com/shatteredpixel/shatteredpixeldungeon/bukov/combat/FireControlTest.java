package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FireMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinitionTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FireControlTest {

	@Test
	public void semiAutomaticFiresOncePerPress() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		definition.fireMode = FireMode.SEMI;
		Firearm firearm = new Firearm().configure("test", "uid", 3);
		RecordingSink sink = new RecordingSink();
		FireControl control = new FireControl();

		control.update(0f, true, true, false, firearm, definition, sink);
		control.update(1f, true, false, false, firearm, definition, sink);

		assertEquals(1, sink.shots);
		assertEquals(2, firearm.magazineAmmo());

		control.update(0f, false, false, false, firearm, definition, sink);
		control.update(0f, true, true, false, firearm, definition, sink);

		assertEquals(2, sink.shots);
		assertEquals(1, firearm.magazineAmmo());
	}

	@Test
	public void automaticFireRespectsRpmCooldown() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		definition.fireMode = FireMode.AUTO;
		definition.rpm = 600f;
		Firearm firearm = new Firearm().configure("test", "uid", 3);
		RecordingSink sink = new RecordingSink();
		FireControl control = new FireControl();

		control.update(0f, true, true, false, firearm, definition, sink);
		control.update(0.05f, true, false, false, firearm, definition, sink);
		control.update(0.05f, true, false, false, firearm, definition, sink);

		assertEquals(2, sink.shots);
		assertEquals(1, firearm.magazineAmmo());
	}

	@Test
	public void reloadRequestsOnlyMissingRounds() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		definition.magazineSize = 5;
		definition.reloadSeconds = 0.5f;
		Firearm firearm = new Firearm().configure("test", "uid", 2);
		RecordingSink sink = new RecordingSink();
		sink.availableAmmo = 2;
		FireControl control = new FireControl();

		control.update(0f, false, false, true, firearm, definition, sink);
		assertTrue(control.isReloading());
		assertEquals(1, sink.reloadStarts);

		control.update(0.5f, false, false, false, firearm, definition, sink);

		assertFalse(control.isReloading());
		assertEquals(3, sink.lastRequestedMaximum);
		assertEquals(4, firearm.magazineAmmo());
		assertEquals(1, sink.reloadFinishes);
	}

	@Test
	public void emptyMagazineEmitsDryFire() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		Firearm firearm = new Firearm().configure("test", "uid", 0);
		RecordingSink sink = new RecordingSink();
		FireControl control = new FireControl();

		control.update(0f, true, true, false, firearm, definition, sink);

		assertEquals(0, sink.shots);
		assertEquals(1, sink.dryFires);
		assertEquals(0.15f, control.shotCooldown(), 0.0001f);
	}

	@Test
	public void emptyMagazineCanSelectAnotherCompatibleVariant() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		definition.reloadSeconds = 0.25f;
		Firearm firearm = new Firearm().configure(
				"test",
				"uid",
				0,
				"test_standard");
		RecordingSink sink = new RecordingSink();
		sink.availableAmmo = 3;
		sink.suppliedDefinitionId = "test_expanding";
		FireControl control = new FireControl();

		control.update(0f, false, false, true, firearm, definition, sink);
		control.update(0.25f, false, false, false, firearm, definition, sink);

		assertTrue(sink.lastAllowedAlternative);
		assertEquals("test_caliber", sink.lastCaliber);
		assertEquals("test_standard", sink.lastPreferredDefinitionId);
		assertEquals(3, firearm.magazineAmmo());
		assertEquals(
				"test_expanding",
				firearm.loadedAmmoDefinitionId(definition));
	}

	private static final class RecordingSink implements FireControl.Sink {
		int shots;
		int dryFires;
		int reloadStarts;
		int reloadFinishes;
		int availableAmmo;
		int lastRequestedMaximum;
		String suppliedDefinitionId;
		String lastCaliber;
		String lastPreferredDefinitionId;
		boolean lastAllowedAlternative;

		@Override
		public void fire(Firearm firearm, FirearmDefinition definition) {
			shots++;
		}

		@Override
		public FireControl.AmmoSelection requestAmmo(
				String caliber,
				String preferredDefinitionId,
				int maximum,
				boolean allowAlternative) {
			lastRequestedMaximum = maximum;
			lastCaliber = caliber;
			lastPreferredDefinitionId = preferredDefinitionId;
			lastAllowedAlternative = allowAlternative;
			int supplied = Math.min(availableAmmo, maximum);
			availableAmmo -= supplied;
			return supplied == 0
					? FireControl.AmmoSelection.none()
					: new FireControl.AmmoSelection(
							suppliedDefinitionId == null
									? preferredDefinitionId
									: suppliedDefinitionId,
							supplied
					);
		}

		@Override
		public void dryFire() {
			dryFires++;
		}

		@Override
		public void reloadStarted(float seconds) {
			reloadStarts++;
		}

		@Override
		public void reloadFinished() {
			reloadFinishes++;
		}
	}
}
