package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;
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

	private static final Map<String, Spec> SPECS =
			new LinkedHashMap<>();

	static {
		register(new Spec(
				SURVIVOR,
				"bukov.economy.services.contract_survivor_title",
				"bukov.economy.services.contract_survivor_objective",
				BukovLongTermContractDefinition.Metric.SUCCESSFUL_EXTRACTIONS,
				3L,
				600L));
		register(new Spec(
				SUPPLIER,
				"bukov.economy.services.contract_supplier_title",
				"bukov.economy.services.contract_supplier_objective",
				BukovLongTermContractDefinition.Metric.EXTRACTED_VALUE,
				15000L,
				1200L));
		register(new Spec(
				HUNTER,
				"bukov.economy.services.contract_hunter_title",
				"bukov.economy.services.contract_hunter_objective",
				BukovLongTermContractDefinition.Metric.KILLS,
				25L,
				1000L));
		register(new Spec(
				VETERAN,
				"bukov.economy.services.contract_veteran_title",
				"bukov.economy.services.contract_veteran_objective",
				BukovLongTermContractDefinition.Metric.RAIDS_COMPLETED,
				10L,
				1800L));
	}

	private BukovLongTermContractCatalog() {
	}

	public static BukovLongTermContractDefinition require(String id) {
		Spec spec = SPECS.get(id);
		if (spec == null) {
			throw new IllegalArgumentException("Unknown long-term contract: " + id);
		}
		return spec.definition();
	}

	public static List<BukovLongTermContractDefinition> all() {
		List<BukovLongTermContractDefinition> result = new ArrayList<>();
		for (Spec spec : SPECS.values()) {
			result.add(spec.definition());
		}
		return Collections.unmodifiableList(result);
	}

	private static void register(Spec spec) {
		if (SPECS.put(spec.id, spec) != null) {
			throw new IllegalStateException(
					"Duplicate long-term contract: " + spec.id);
		}
	}

	/**
	 * Stable gameplay data is cached, localized presentation is resolved when
	 * a screen asks for it. This keeps an in-session language change from
	 * leaving contract cards in the previous language.
	 */
	private static final class Spec {
		final String id;
		final String titleKey;
		final String objectiveKey;
		final BukovLongTermContractDefinition.Metric metric;
		final long target;
		final long rewardCurrency;

		Spec(
				String id,
				String titleKey,
				String objectiveKey,
				BukovLongTermContractDefinition.Metric metric,
				long target,
				long rewardCurrency) {
			this.id = id;
			this.titleKey = titleKey;
			this.objectiveKey = objectiveKey;
			this.metric = metric;
			this.target = target;
			this.rewardCurrency = rewardCurrency;
		}

		BukovLongTermContractDefinition definition() {
			return new BukovLongTermContractDefinition(
					id,
					BukovMessages.get(titleKey),
					BukovMessages.get(objectiveKey),
					metric,
					target,
					rewardCurrency);
		}
	}
}
