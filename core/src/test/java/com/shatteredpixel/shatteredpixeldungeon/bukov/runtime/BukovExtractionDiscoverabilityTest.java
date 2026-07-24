package com.shatteredpixel.shatteredpixeldungeon.bukov.runtime;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.ExtractionState;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BukovExtractionDiscoverabilityTest {

	private static final int WIDTH = 12;
	private static final int LENGTH = 144;
	private static final int HERO = 65;
	private static final int E01_CELL = HERO + 1;
	private static final int E02_CELL = HERO;
	private static final int E03_CELL = HERO + WIDTH * 3;

	@Test
	public void exactAndAllEightAdjacentCellsShareThePromptRange() {
		assertEquals(
				0,
				BukovRealtimeWorld.extractionCellDistance(
						HERO, HERO, WIDTH, LENGTH));
		int[] adjacentOffsets = {
				-WIDTH - 1, -WIDTH, -WIDTH + 1,
				-1, 1,
				WIDTH - 1, WIDTH, WIDTH + 1
		};
		for (int offset : adjacentOffsets) {
			assertEquals(
					"offset=" + offset,
					1,
					BukovRealtimeWorld.extractionCellDistance(
							HERO,
							HERO + offset,
							WIDTH,
							LENGTH));
		}
		assertEquals(
				2,
				BukovRealtimeWorld.extractionCellDistance(
						HERO, HERO + 2, WIDTH, LENGTH));
		assertEquals(
				Integer.MAX_VALUE,
				BukovRealtimeWorld.extractionCellDistance(
						-1, HERO, WIDTH, LENGTH));
	}

	@Test
	public void nearestQueryCoversAllExtractionTypesAndExactCellWins() {
		ExtractionState e01 = ExtractionState.basic();
		ExtractionState e02 = ExtractionState.conditional();
		ExtractionState e03 = ExtractionState.temporary(
				ExtractionState.TEMPORARY_EARLIEST_SECONDS);
		List<ExtractionState> extractions = Arrays.asList(e01, e02, e03);
		BukovRealtimeWorld.ExtractionLookup lookup =
				lookup(true, true, true);

		assertSame(
				"E02 exact cell must beat adjacent E01",
				e02,
				BukovRealtimeWorld.nearestExtraction(
						HERO,
						WIDTH,
						LENGTH,
						0f,
						1,
						false,
						extractions,
						lookup));
		assertSame(
				e01,
				BukovRealtimeWorld.nearestExtraction(
						E01_CELL,
						WIDTH,
						LENGTH,
						0f,
						1,
						false,
						extractions,
						lookup));
		assertSame(
				e03,
				BukovRealtimeWorld.nearestExtraction(
						E03_CELL,
						WIDTH,
						LENGTH,
						ExtractionState.TEMPORARY_EARLIEST_SECONDS,
						1,
						false,
						extractions,
						lookup));
	}

	@Test
	public void availabilityFilterReturnsNullWhenNoExtractionIsUsable() {
		ExtractionState e01 = ExtractionState.basic();
		ExtractionState e02 = ExtractionState.conditional();
		ExtractionState e03 = ExtractionState.temporary(
				ExtractionState.TEMPORARY_EARLIEST_SECONDS);
		List<ExtractionState> extractions = Arrays.asList(e01, e02, e03);

		assertSame(
				"unavailable exact E02 must yield to available adjacent E01",
				e01,
				BukovRealtimeWorld.nearestExtraction(
						HERO,
						WIDTH,
						LENGTH,
						0f,
						1,
						true,
						extractions,
						lookup(true, false, false)));

		assertNull(BukovRealtimeWorld.nearestExtraction(
				HERO,
				WIDTH,
				LENGTH,
				0f,
				-1,
				true,
				extractions,
				lookup(false, false, false)));
	}

	@Test
	public void adjacentPromptNamesEveryTargetAndRequestsMarkerEntry() {
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.extraction_approach_available_format",
						"E01"),
				BukovRealtimeWorld.extractionApproachLabel(
						"E01", true));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.extraction_approach_locked_format",
						"E02"),
				BukovRealtimeWorld.extractionApproachLabel(
						"E02", false));
		assertEquals(
				BukovMessages.get(
						"bukov.raid.runtime.extraction_approach_available_format",
						"E03"),
				BukovRealtimeWorld.extractionApproachLabel(
						"E03", true));
	}

	@Test
	public void onlyExactMarkerCellQualifiesToStartCountdown() {
		boolean exact = BukovRealtimeWorld.extractionCellDistance(
				HERO, HERO, WIDTH, LENGTH) == 0;
		boolean adjacent = BukovRealtimeWorld.extractionCellDistance(
				HERO, E01_CELL, WIDTH, LENGTH) == 0;

		assertTrue(ExtractionIntentResolver.wantsToStart(
				exact, true, true, false, 0));
		assertFalse(ExtractionIntentResolver.wantsToStart(
				adjacent, true, true, false, 0));
	}

	private static BukovRealtimeWorld.ExtractionLookup lookup(
			boolean e01Available,
			boolean e02Available,
			boolean e03Available) {
		return new BukovRealtimeWorld.ExtractionLookup() {
			@Override
			public int cell(String extractionId) {
				if ("E01".equals(extractionId)) return E01_CELL;
				if ("E02".equals(extractionId)) return E02_CELL;
				if ("E03".equals(extractionId)) return E03_CELL;
				return -1;
			}

			@Override
			public boolean available(
					ExtractionState extraction, float elapsed) {
				if ("E01".equals(extraction.extractionId())) {
					return e01Available;
				}
				if ("E02".equals(extraction.extractionId())) {
					return e02Available;
				}
				return "E03".equals(extraction.extractionId())
						&& e03Available;
			}
		};
	}
}
