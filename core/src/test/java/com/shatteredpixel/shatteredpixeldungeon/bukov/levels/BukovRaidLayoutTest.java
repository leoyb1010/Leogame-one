package com.shatteredpixel.shatteredpixeldungeon.bukov.levels;

import com.watabou.utils.Bundle;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovRaidLayoutTest {

	@Test
	public void layoutRoundTripsThroughProjectBundleFormat() {
		BukovRaidLayout original = BukovZonePlanner.generateFirstRaid(42L);
		Bundle container = new Bundle();
		container.put("layout", original);

		BukovRaidLayout restored = (BukovRaidLayout) container.get("layout");

		assertNotNull(restored);
		assertEquals(original.seed, restored.seed);
		assertEquals(original.themeId, restored.themeId);
		assertEquals(original.marks.size(), restored.marks.size());
		assertEquals(original.links.size(), restored.links.size());
		assertEquals(original.extractions.size(), restored.extractions.size());
		assertEquals(original.lootAnchors.size(), restored.lootAnchors.size());
		assertEquals(original.routes.size(), restored.routes.size());
		assertEquals(original.marks.get(0).roomId(), restored.marks.get(0).roomId());
		for (int i = 0; i < original.extractions.size(); i++) {
			assertEquals(original.extractions.get(i).interactionCell,
					restored.extractions.get(i).interactionCell);
			assertEquals(original.extractions.get(i).interactionX,
					restored.extractions.get(i).interactionX);
			assertEquals(original.extractions.get(i).interactionY,
					restored.extractions.get(i).interactionY);
		}
		for (int i = 0; i < original.lootAnchors.size(); i++) {
			assertEquals(original.lootAnchors.get(i).id, restored.lootAnchors.get(i).id);
			assertEquals(original.lootAnchors.get(i).roomId,
					restored.lootAnchors.get(i).roomId);
			assertEquals(original.lootAnchors.get(i).cell,
					restored.lootAnchors.get(i).cell);
			assertEquals(original.lootAnchors.get(i).lootTableId,
					restored.lootAnchors.get(i).lootTableId);
			assertEquals(original.lootAnchors.get(i).searchSeconds,
					restored.lootAnchors.get(i).searchSeconds, 0f);
		}
		assertTrue(RaidMapValidator.validate(restored).valid);
	}

	@Test
	public void extractionRulesMatchFirstRaidContract() {
		BukovRaidLayout layout = BukovZonePlanner.generateFirstRaid(7L);
		ExtractionDefinition baseline = layout.extractions.get(0);
		ExtractionDefinition conditional = layout.extractions.get(1);
		ExtractionDefinition temporary = layout.extractions.get(2);

		assertTrue(baseline.isAvailable(0f, Collections.<String>emptySet()));
		assertFalse(conditional.isAvailable(600f, Collections.<String>emptySet()));
		assertTrue(conditional.isAvailable(600f,
				new HashSet<>(Collections.singletonList("pump_power"))));
		assertFalse(temporary.isAvailable(temporary.availableFromSeconds - 1f,
				Collections.<String>emptySet()));
		assertTrue(temporary.isAvailable(temporary.availableFromSeconds + 60f,
				Collections.<String>emptySet()));
		assertFalse(temporary.isAvailable(temporary.availableUntilSeconds + 1f,
				Collections.<String>emptySet()));
		assertEquals("E01", baseline.id);
		assertEquals("pump_power", conditional.requiredEvent);
		assertTrue(temporary.availableFromSeconds >= 480f);
		assertTrue(temporary.availableFromSeconds <= 840f);
		assertEquals(120f,
				temporary.availableUntilSeconds - temporary.availableFromSeconds, 0f);
	}
}
