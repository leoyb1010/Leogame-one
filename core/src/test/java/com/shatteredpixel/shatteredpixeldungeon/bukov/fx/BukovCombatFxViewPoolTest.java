package com.shatteredpixel.shatteredpixeldungeon.bukov.fx;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ui.BukovUiTokens;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BukovCombatFxViewPoolTest {

	@Test
	public void productionCapacitiesComeFromAllSevenUiTokens()
			throws Exception {
		String json = new String(
				Files.readAllBytes(Paths.get(
						"src/main/assets/bukov/content/ui_tokens.json")),
				StandardCharsets.UTF_8);
		BukovCombatFxViewPool.Capacities capacities =
				BukovCombatFxViewPool.Capacities.from(
						BukovUiTokens.parse(json));

		assertEquals(16, capacities.forType(
				CombatFxEvent.Type.MUZZLE_FLASH));
		assertEquals(64, capacities.forType(CombatFxEvent.Type.TRACER));
		assertEquals(32, capacities.forType(CombatFxEvent.Type.SHELL));
		assertEquals(48, capacities.forType(CombatFxEvent.Type.IMPACT));
		assertEquals(32, capacities.forType(
				CombatFxEvent.Type.BLOOD_MIST));
		assertEquals(96, capacities.forType(
				CombatFxEvent.Type.BULLET_MARK));
		assertEquals(8, capacities.forType(
				CombatFxEvent.Type.EXPLOSION));
		for (CombatFxEvent.Type type : CombatFxEvent.Type.values()) {
			assertTrue(capacities.forType(type) > 0);
			assertTrue(capacities.forType(type) <= 256);
		}
	}

	@Test
	public void freeSlotWinsBeforeAnyLiveViewIsReused() {
		assertEquals(
				1,
				BukovCombatFxViewPool.oldestOrFree(
						new boolean[]{true, false, true},
						new long[]{1L, 0L, 2L}));
	}

	@Test
	public void saturationReusesTheOldestPresentationOnlySlot() {
		assertEquals(
				1,
				BukovCombatFxViewPool.oldestOrFree(
						new boolean[]{true, true, true},
						new long[]{8L, 3L, 12L}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void slotPolicyRejectsMismatchedState() {
		BukovCombatFxViewPool.oldestOrFree(
				new boolean[]{true},
				new long[]{1L, 2L});
	}
}
