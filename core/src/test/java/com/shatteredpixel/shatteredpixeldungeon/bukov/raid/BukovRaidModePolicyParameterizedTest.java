package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BukovRaidModePolicyParameterizedTest {

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> parameters() {
		return Arrays.asList(new Object[][] {
				{BukovRaidMode.EXPEDITION, 100, 4},
				{BukovRaidMode.QUICK_SWEEP, 72, 3},
				{BukovRaidMode.SCAVENGER, 58, 4},
				{BukovRaidMode.BOSS_CONTRACT, 125, 4}
		});
	}

	private final BukovRaidMode mode;
	private final int adjustedUnitValue;
	private final int configuredContainerCount;

	public BukovRaidModePolicyParameterizedTest(
			BukovRaidMode mode,
			int adjustedUnitValue,
			int configuredContainerCount) {
		this.mode = mode;
		this.adjustedUnitValue = adjustedUnitValue;
		this.configuredContainerCount = configuredContainerCount;
	}

	@Test
	public void targetWindowDrivesPressureAndActiveCap() {
		float before = mode.targetMinimumSeconds() - 1f;
		float middle = (mode.targetMinimumSeconds()
				+ mode.targetMaximumSeconds()) * 0.5f;
		float overtime = mode.targetMaximumSeconds();
		assertEquals(1f, mode.pressureMultiplier(before), 0f);
		assertEquals(1.25f, mode.pressureMultiplier(middle), 0.0001f);
		assertEquals(1.75f, mode.pressureMultiplier(overtime), 0f);
		assertTrue(mode.spawnIntervalAt(middle)
				< mode.spawnIntervalAt(before));
		assertEquals(mode.maximumActiveEnemies,
				mode.maximumActiveEnemiesAt(before));
		assertEquals(mode.maximumActiveEnemies + 2,
				mode.maximumActiveEnemiesAt(overtime));
	}

	@Test
	public void containerProjectionIsCompactDeterministicAndMissionSafe() {
		List<BukovContainerDefinition> first =
				mode.configureContainers(containers(), 881177L);
		List<BukovContainerDefinition> second =
				mode.configureContainers(containers(), 881177L);
		assertEquals(configuredContainerCount, first.size());
		assertEquals(fingerprint(first), fingerprint(second));
		assertTrue(fingerprint(first).contains("Q01:mission_archive:1"));
		for (BukovContainerDefinition definition : first) {
			if ("mission_archive".equals(definition.lootTableId)) continue;
			if (mode == BukovRaidMode.SCAVENGER) {
				assertEquals(1, definition.rolls);
			}
			if (mode == BukovRaidMode.BOSS_CONTRACT) {
				assertTrue(definition.rolls >= 3);
			}
		}
	}

	@Test
	public void successfulSettlementConservesAdjustedPhysicalValue() {
		BukovProfile profile = new BukovProfile();
		LootTransaction loot = new LootTransaction(
				"mode-value-" + mode.name(), 40f);
		assertEquals(LootTransaction.PickupResult.ADDED,
				loot.pickup(item("found", 2, 100, true)));
		assertEquals(LootTransaction.PickupResult.ADDED,
				loot.pickup(item("owned", 1, 50, false)));

		RaidResult result = new RaidSettlement().settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				120f,
				2,
				true,
				mode);
		long expected = adjustedUnitValue * 2L + 50L;
		assertEquals(3L, result.transferredQuantity());
		assertEquals(expected, result.transferredValue());
		assertEquals(expected, profile.stash().totalValue());
		assertEquals(expected, profile.statistics().extractedValue());
		assertEquals(adjustedUnitValue,
				profile.stash().item("found").unitValue());

		RaidResult replay = new RaidSettlement().settle(
				profile,
				loot,
				RaidOutcome.SUCCESS,
				120f,
				2,
				true,
				mode);
		assertTrue(replay.replayed());
		assertEquals(expected, profile.stash().totalValue());
	}

	@Test
	public void sessionRestoreKeepsModePolicyDeterministic() {
		RaidSession session = RaidSession.create(
				991L, "mode-save-" + mode.name(), mode, 4);
		session.advance(mode.targetMinimumSeconds() + 3f);
		Bundle bundle = new Bundle();
		bundle.put("session", session);
		RaidSession restored = (RaidSession)bundle.get("session");
		assertEquals(mode, restored.raidMode());
		assertEquals(
				mode.pressureMultiplier(session.elapsedSeconds),
				restored.raidMode().pressureMultiplier(
						restored.elapsedSeconds),
				0f);
		assertEquals(
				fingerprint(mode.configureContainers(containers(), 991L)),
				fingerprint(restored.raidMode()
						.configureContainers(containers(), 991L)));
	}

	private static List<BukovContainerDefinition> containers() {
		return Arrays.asList(
				container("L01", "low", 2, 2f),
				container("L02", "medical", 2, 2.4f),
				container("L03", "high_value", 3, 3f),
				container("Q01", "mission_archive", 1, 1.4f));
	}

	private static BukovContainerDefinition container(
			String id, String table, int rolls, float seconds) {
		return new BukovContainerDefinition(
				id, id.hashCode() & 0x7FFF, table, rolls, seconds, false);
	}

	private static String fingerprint(
			List<BukovContainerDefinition> definitions) {
		StringBuilder result = new StringBuilder();
		for (BukovContainerDefinition definition : definitions) {
			result.append(definition.containerId).append(':')
					.append(definition.lootTableId).append(':')
					.append(definition.rolls).append(':')
					.append(Float.floatToIntBits(definition.searchSeconds))
					.append('|');
		}
		return result.toString();
	}

	private static RaidItem item(
			String uid, int quantity, int value, boolean found) {
		return new RaidItem(
				uid,
				"def:" + uid,
				quantity,
				0.5f,
				value,
				found,
				false,
				1f);
	}
}
