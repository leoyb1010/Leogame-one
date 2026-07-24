package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable contract IDs and targets shared by persistence, settlement and UI. */
public final class BukovLongTermContractCatalog {

	public static final String SURVIVOR = "longterm:survivor";
	public static final String SUPPLIER = "longterm:supplier";
	public static final String HUNTER = "longterm:hunter";
	public static final String VETERAN = "longterm:veteran";

	private static final Map<String, BukovLongTermContractDefinition> DEFINITIONS =
			new LinkedHashMap<>();

	static {
		register(new BukovLongTermContractDefinition(
				SURVIVOR,
				"活着回来",
				"累计成功撤离 3 次",
				BukovLongTermContractDefinition.Metric.SUCCESSFUL_EXTRACTIONS,
				3L,
				600L));
		register(new BukovLongTermContractDefinition(
				SUPPLIER,
				"回收专家",
				"累计带回价值 15000 的物资",
				BukovLongTermContractDefinition.Metric.EXTRACTED_VALUE,
				15000L,
				1200L));
		register(new BukovLongTermContractDefinition(
				HUNTER,
				"清场行动",
				"在正式突袭中累计击杀 25 名敌人",
				BukovLongTermContractDefinition.Metric.KILLS,
				25L,
				1000L));
		register(new BukovLongTermContractDefinition(
				VETERAN,
				"老兵记录",
				"完成 10 次正式突袭",
				BukovLongTermContractDefinition.Metric.RAIDS_COMPLETED,
				10L,
				1800L));
	}

	private BukovLongTermContractCatalog() {
	}

	public static BukovLongTermContractDefinition require(String id) {
		BukovLongTermContractDefinition definition = DEFINITIONS.get(id);
		if (definition == null) {
			throw new IllegalArgumentException("Unknown long-term contract: " + id);
		}
		return definition;
	}

	public static List<BukovLongTermContractDefinition> all() {
		return Collections.unmodifiableList(
				new ArrayList<>(DEFINITIONS.values()));
	}

	private static void register(BukovLongTermContractDefinition definition) {
		if (DEFINITIONS.put(definition.id, definition) != null) {
			throw new IllegalStateException(
					"Duplicate long-term contract: " + definition.id);
		}
	}
}
