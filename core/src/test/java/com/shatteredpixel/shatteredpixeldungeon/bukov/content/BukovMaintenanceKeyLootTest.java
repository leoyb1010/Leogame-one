package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BukovMaintenanceKeyLootTest {

	@Test
	public void maintenanceKeyIsAnAuthoredObtainableTool() {
		Item item = BukovFirstRaidLootTables
				.createByEconomicDefinitionId(
						BukovFirstRaidLootTables
								.MAINTENANCE_KEY_DEFINITION_ID);

		assertNotNull(item);
		assertTrue(item instanceof BukovLootItem);
		assertEquals(
				BukovMessages.get(
						"bukov.economy.content.item_key_maintenance"),
				item.name());
		assertEquals(
				BukovLootItem.Category.TOOL,
				((BukovLootItem)item).category());
	}

	@Test
	public void rareDropPolicyIsStableAndActuallyRare() {
		int drops = 0;
		for (int stableId = 0; stableId < 10_000; stableId++) {
			boolean first = BukovFirstRaidLootTables.maintenanceKeyDrops(
					991177L,
					stableId);
			boolean second = BukovFirstRaidLootTables.maintenanceKeyDrops(
					991177L,
					stableId);
			assertEquals(first, second);
			if (first) drops++;
		}
		assertTrue("key must remain rare", drops >= 650);
		assertTrue("key must remain rare", drops <= 950);
	}

	@Test
	public void lockedCacheRollsDeterministicHighValueContents() {
		BukovLootTable table = BukovFirstRaidLootTables.require(
				BukovFirstRaidLootTables.MAINTENANCE_CACHE);
		List<Item> first = table.roll(
				778899L,
				BukovFirstRaidLootTables
						.MAINTENANCE_CACHE_CONTAINER_ID,
				3);
		List<Item> second = table.roll(
				778899L,
				BukovFirstRaidLootTables
						.MAINTENANCE_CACHE_CONTAINER_ID,
				3);

		assertEquals(3, first.size());
		for (int index = 0; index < first.size(); index++) {
			BukovLootItem firstItem = (BukovLootItem)first.get(index);
			BukovLootItem secondItem = (BukovLootItem)second.get(index);
			assertEquals(
					BukovLootItem.Category.HIGH_VALUE,
					firstItem.category());
			assertEquals(
					firstItem.bukovDefinitionId(),
					secondItem.bukovDefinitionId());
			assertEquals(
					firstItem.quantity(),
					secondItem.quantity());
		}
	}
}
