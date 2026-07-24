package com.shatteredpixel.shatteredpixeldungeon.bukov.content;

import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoStack;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.AmmoVariant;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLootTable;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class BukovFirstRaidLootTablesTest {

	@Test
	public void registryHasFiveEconomyLayersPlusGuaranteedMissionArchive() {
		Map<String, BukovLootTable> tables = BukovFirstRaidLootTables.all();
		assertEquals(
				new HashSet<>(Arrays.asList(
						BukovFirstRaidLootTables.LOW,
						BukovFirstRaidLootTables.MEDICAL,
						BukovFirstRaidLootTables.INDUSTRIAL,
						BukovFirstRaidLootTables.HIGH_VALUE,
						BukovFirstRaidLootTables.MAINTENANCE_CACHE,
						BukovFirstRaidLootTables.BOSS,
						BukovFirstRaidLootTables.MISSION_ARCHIVE)),
				tables.keySet());

		Set<String> entryIds = new HashSet<>();
		for (Map.Entry<String, BukovLootTable> registered : tables.entrySet()) {
			BukovLootTable table = registered.getValue();
			assertEquals(registered.getKey(), table.tableId());
			assertTrue(table.totalWeight() > 0);
			for (BukovLootTable.Entry entry : table.entries()) {
				assertTrue(entryIds.add(entry.entryId()));
				assertTrue(entry.weight() > 0);
				assertTrue(entry.minimumQuantity() > 0);
				assertTrue(entry.maximumQuantity() >= entry.minimumQuantity());
			}
		}
		assertTrue(entryIds.size() >= 30);
	}

	@Test
	public void everyFactoryReturnsFreshItemWithValidEconomy() {
		Set<String> definitionIds = new HashSet<>();
		for (BukovLootTable table : BukovFirstRaidLootTables.all().values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item first = entry.createForValidation();
				Item second = entry.createForValidation();

				assertNotNull(first);
				assertNotNull(second);
				assertNotSame(first, second);
				assertTrue(first instanceof BukovEconomicItem);

				BukovEconomicItem economic = (BukovEconomicItem) first;
				assertNotNull(economic.bukovDefinitionId());
				assertFalse(economic.bukovDefinitionId().trim().isEmpty());
				assertTrue(definitionIds.add(economic.bukovDefinitionId()));
				assertTrue(Float.isFinite(economic.bukovUnitWeight()));
				assertTrue(economic.bukovUnitWeight() >= 0f);
				assertTrue(economic.bukovUnitValue() >= 0);
			}
		}
		assertTrue(definitionIds.size() >= 30);
	}

	@Test
	public void ammoEntriesReuseAmmoStackAndCoverFiveBallisticVariants()
			throws Exception {
		String json = new String(
				java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
						"src/main/assets/bukov/content/ammunition.json"
				)),
				java.nio.charset.StandardCharsets.UTF_8
		);
		AmmoRegistry registry = new AmmoRegistry();
		registry.loadJson(json);
		Set<String> ammoIds = new HashSet<>();
		EnumSet<AmmoVariant> variants = EnumSet.noneOf(AmmoVariant.class);
		int ammoEntryCount = 0;
		for (BukovLootTable table : BukovFirstRaidLootTables.all().values()) {
			for (BukovLootTable.Entry entry : table.entries()) {
				Item item = entry.createForValidation();
				if (item instanceof AmmoStack) {
					ammoEntryCount++;
					String definitionId = ((AmmoStack) item).definitionId();
					ammoIds.add(definitionId);
					variants.add(registry.require(definitionId).variant);
				}
			}
		}

		assertTrue(ammoEntryCount >= 5);
		assertEquals(ammoEntryCount, ammoIds.size());
		assertEquals(EnumSet.allOf(AmmoVariant.class), variants);
	}

	@Test
	public void sameSeedAndContainerProduceSameContentsButFreshInstances() {
		BukovLootTable table =
				BukovFirstRaidLootTables.require(BukovFirstRaidLootTables.LOW);
		List<Item> first = table.roll(884422L, "warehouse-crate-03", 32);
		List<Item> second = table.roll(884422L, "warehouse-crate-03", 32);

		assertEquals(first.size(), second.size());
		for (int index = 0; index < first.size(); index++) {
			Item firstItem = first.get(index);
			Item secondItem = second.get(index);
			assertNotSame(firstItem, secondItem);
			assertEquals(
					((BukovEconomicItem) firstItem).bukovDefinitionId(),
					((BukovEconomicItem) secondItem).bukovDefinitionId());
			assertEquals(firstItem.quantity(), secondItem.quantity());
		}
	}

	@Test
	public void dataDrivenLootSurvivesBundleRoundTrip() {
		BukovLootItem original = new BukovLootItem().configure(
				"test_medical",
				"测试医疗包",
				BukovLootItem.Category.MEDICAL,
				0.45f,
				640);
		original.quantity(3);
		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);

		BukovLootItem restored = new BukovLootItem();
		restored.restoreFromBundle(bundle);

		assertEquals("test_medical", restored.bukovDefinitionId());
		assertEquals("测试医疗包", restored.name());
		assertEquals(BukovLootItem.Category.MEDICAL, restored.category());
		assertEquals(0.45f, restored.bukovUnitWeight(), 0.0001f);
		assertEquals(640, restored.bukovUnitValue());
		assertEquals(3, restored.quantity());
	}

	@Test(expected = IllegalArgumentException.class)
	public void unknownLayerIsRejected() {
		BukovFirstRaidLootTables.require("not-a-real-layer");
	}
}
