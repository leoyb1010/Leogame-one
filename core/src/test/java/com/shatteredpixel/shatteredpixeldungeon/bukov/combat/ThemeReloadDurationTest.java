package com.shatteredpixel.shatteredpixeldungeon.bukov.combat;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.Firearm;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinitionTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThemeReloadDurationTest {

	@Test
	public void environmentMultiplierOnlyExtendsReloadClock() {
		FirearmDefinition definition = FirearmDefinitionTest.validDefinition();
		definition.magazineSize = 5;
		definition.reloadSeconds = 1f;
		Firearm firearm = new Firearm().configure("test", "uid", 2);
		RecordingSink sink = new RecordingSink();
		FireControl control = new FireControl();

		control.update(
				0f,
				false,
				false,
				true,
				firearm,
				definition,
				1.2f,
				sink);
		assertEquals(1.2f, control.reloadRemaining(), 0.0001f);
		control.update(
				1f,
				false,
				false,
				false,
				firearm,
				definition,
				1f,
				sink);
		assertTrue(control.isReloading());
		control.update(
				0.21f,
				false,
				false,
				false,
				firearm,
				definition,
				1f,
				sink);
		assertFalse(control.isReloading());
		assertEquals(1, sink.finished);
	}

	private static final class RecordingSink implements FireControl.Sink {
		private int finished;

		@Override
		public void fire(Firearm firearm, FirearmDefinition definition) {
		}

		@Override
		public FireControl.AmmoSelection requestAmmo(
				String caliber,
				String preferredDefinitionId,
				int maximum,
				boolean allowAlternative) {
			return FireControl.AmmoSelection.none();
		}

		@Override
		public void dryFire() {
		}

		@Override
		public void reloadStarted(float seconds) {
		}

		@Override
		public void reloadAudioCues(
				FirearmDefinition definition,
				int cueMask) {
		}

		@Override
		public void reloadFinished() {
			finished++;
		}
	}
}
