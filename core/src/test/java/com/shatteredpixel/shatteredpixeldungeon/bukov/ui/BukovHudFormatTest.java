package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BukovHudFormatTest {

	@Test
	public void formatsRaidClockWithoutLocaleDependencies() {
		assertEquals("00:00", BukovHudFormat.clock(-1f));
		assertEquals("09:05", BukovHudFormat.clock(545.9f));
		assertEquals("01:02:03", BukovHudFormat.clock(3723f));
	}

	@Test
	public void formatsLiveVitalsAndAmmo() {
		assertEquals("HP 72/100 +12", BukovHudFormat.health(72, 100, 12));
		assertEquals("Armor 2-8", BukovHudFormat.armor(2, 8));
		assertEquals("Armor --", BukovHudFormat.armor(null, null));
		assertEquals("Ammo 17 / 90", BukovHudFormat.ammo(17, 90));
		assertEquals("Ammo -- / --", BukovHudFormat.ammo(null, null));
		assertEquals("17 | 90", BukovHudFormat.tacticalAmmo(
				"Needlebee-9", 17, 24, 90));
		assertEquals("-- | --", BukovHudFormat.tacticalAmmo(
				null, 17, 24, 90));
		assertEquals("Needlebee-9 · Single",
				BukovHudFormat.weapon("Needlebee-9", false));
		assertEquals("Ward-556 · Auto",
				BukovHudFormat.weapon("Ward-556", true));
	}

	@Test
	public void fallsBackToRequiredFirstRaidObjective() {
		assertEquals(BukovHudFormat.DEFAULT_OBJECTIVE, BukovHudFormat.objective("  "));
		assertEquals("Start pump", BukovHudFormat.objective("  Start pump  "));
	}

	@Test
	public void formatsInjuriesReloadAndInteractionWithoutFrameUnits() {
		assertEquals("Stable",
				BukovHudFormat.status(0f, false, 0f, 0f, 0f));
		assertEquals(
				"Bleeding 0.4/s · Fracture · Concussion 2.5s · Pain",
				BukovHudFormat.status(0.4f, true, 0.2f, 2.5f, 0f));
		assertEquals("∞",
				BukovHudFormat.injuryRemaining(true, 0f));
		assertEquals("3s",
				BukovHudFormat.injuryRemaining(true, 2.5f));
		assertEquals("",
				BukovHudFormat.injuryRemaining(false, 9f));
		assertEquals("Reload 35%", BukovHudFormat.reload(true, 0.35f));
		assertEquals(
				"Hold interact · Search container · 1.2s",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"Search container",
						0f,
						1.2f));
		assertEquals(
				"Hold E · Search container · 1.2s",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"Search container",
						0f,
						1.2f,
						true));
		assertEquals(
				"Press E · Pick up supplies · View weight/value in backpack",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.PICKUP,
						"Pick up supplies",
						0f,
						0f,
						true));
		assertEquals(
				"Press E · Unlock with maintenance key",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.UNLOCK,
						"Unlock with maintenance key",
						0f,
						0f,
						true));
		assertEquals(
				"Unavailable · Maintenance key required",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.LOCKED,
						"Maintenance key required",
						0f,
						0f,
						true));
		assertEquals(
				"Search container 50%",
				BukovHudFormat.interaction(
						BukovRaidHudState.Interaction.SEARCH,
						"Search container",
						0.5f,
						1.2f));
	}

	@Test
	public void formatsExtractionAvailabilityAndActiveCountdown() {
		assertEquals(
				"Extraction points available: 2",
				BukovHudFormat.extraction(2, null, false, false, 0f, 0f));
		assertEquals(
				"Extract E02 · Locked",
				BukovHudFormat.extraction(1, "E02", false, false, 0f, 8f));
		assertEquals(
				"Extract E01 · 3.0s",
				BukovHudFormat.extraction(1, "E01", true, true, 0.4f, 5f));
	}
}
