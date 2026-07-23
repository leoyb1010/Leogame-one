package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RaidMapValidatorTest {

	@Test
	public void rejectsDisconnectedSpawnAfterDynamicDoorsAreApplied() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(1L);
		String spawn = layout.marks.get(0).roomId();
		for (BukovRaidLayout.Link link : layout.links) {
			if (link.joins(spawn)) link.requiredEvent = "sealed";
		}

		RaidMapValidator.Result result = RaidMapValidator.validate(layout);

		assertFalse(result.valid);
		assertEquals(RaidMapValidator.Failure.SPAWN_HAS_FEWER_THAN_TWO_RESOURCE_ROOMS,
				result.failure);
	}

	@Test
	public void rejectsHighValueRoomPlacedNextToSpawn() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(2L);
		BukovRaidLayout.Mark nearSpawn = layout.marks.get(1);
		BukovRaidLayout.Mark originalHigh = layout.marks.get(27);
		nearSpawn.zone = BukovRaidLayout.Zone.HIGH_VALUE;
		originalHigh.zone = BukovRaidLayout.Zone.LOW_LOOT;

		RaidMapValidator.Result result = RaidMapValidator.validate(layout);

		assertFalse(result.valid);
		assertEquals(RaidMapValidator.Failure.HIGH_VALUE_TOO_CLOSE_TO_SPAWN, result.failure);
	}

	@Test
	public void rejectsSingleTileEliteChokepoint() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(3L);
		BukovRaidLayout.Mark boss = layout.marks.get(28);
		boss.minimumPassageWidthTiles = 1;

		RaidMapValidator.Result result = RaidMapValidator.validate(layout);

		assertFalse(result.valid);
		assertEquals(RaidMapValidator.Failure.ELITE_ON_SINGLE_TILE_CHOKEPOINT, result.failure);
	}

	@Test
	public void rejectsConditionalExtractionWithoutPumpPower() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(4L);
		layout.extraction("E02").requiredEvent = "";

		RaidMapValidator.Result result = RaidMapValidator.validate(layout);

		assertFalse(result.valid);
		assertEquals(RaidMapValidator.Failure.INVALID_EXTRACTION, result.failure);
	}

	@Test
	public void rejectsOverlappingExtractionAndLootAnchors() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(5L);
		layout.lootAnchor("L01").cell = layout.extraction("E01").interactionCell;

		RaidMapValidator.Result result = RaidMapValidator.validate(layout);

		assertFalse(result.valid);
		assertEquals(RaidMapValidator.Failure.INVALID_ANCHOR, result.failure);
	}
}
