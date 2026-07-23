package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable offline vendor stock for the first complete economy loop. */
public final class BukovVendorCatalog {

	public static final class Offer {
		public final String offerId;
		public final String definitionId;
		public final int quantity;
		public final float unitWeight;
		public final int unitValue;
		public final long purchasePrice;
		public final float durability;

		private Offer(
				String offerId,
				String definitionId,
				int quantity,
				float unitWeight,
				int unitValue,
				long purchasePrice,
				float durability) {
			this.offerId = offerId;
			this.definitionId = definitionId;
			this.quantity = quantity;
			this.unitWeight = unitWeight;
			this.unitValue = unitValue;
			this.purchasePrice = purchasePrice;
			this.durability = durability;
		}

		RaidItem createItem(String itemUid) {
			return new RaidItem(
					itemUid,
					definitionId,
					quantity,
					unitWeight,
					unitValue,
					false,
					false,
					durability);
		}
	}

	private static final Map<String, Offer> OFFERS = createOffers();

	private BukovVendorCatalog() {
	}

	public static Offer require(String offerId) {
		Offer offer = OFFERS.get(offerId);
		if (offer == null) {
			throw new IllegalArgumentException(
					"Unknown Bukov vendor offer: " + offerId);
		}
		return offer;
	}

	public static List<Offer> all() {
		return Collections.unmodifiableList(
				new ArrayList<>(OFFERS.values()));
	}

	private static Map<String, Offer> createOffers() {
		Map<String, Offer> offers = new LinkedHashMap<>();
		add(offers, new Offer(
				"ammo_9_standard_24",
				"ammo:ammo_9_standard",
				24, 0.012f, 12, 360L, 1f));
		add(offers, ammunition(
				"ammo_9_training", 30, 0.012f, 5, 190L));
		add(offers, ammunition(
				"ammo_9_subsonic", 24, 0.013f, 22, 660L));
		add(offers, ammunition(
				"ammo_556_standard", 24, 0.013f, 18, 540L));
		add(offers, ammunition(
				"ammo_556_armor_piercing", 18, 0.013f, 42, 950L));
		add(offers, ammunition(
				"ammo_762_standard", 20, 0.024f, 24, 600L));
		add(offers, ammunition(
				"ammo_762_expanding", 16, 0.025f, 48, 960L));
		add(offers, ammunition(
				"ammo_12g_buckshot", 12, 0.045f, 28, 420L));
		add(offers, new Offer(
				"bandage_1",
				"bandage",
				1, 0.12f, 180, 220L, 1f));
		add(offers, new Offer(
				"soft_vest_1",
				"armor:soft_vest",
				1, 2.4f, 1_400, 1_800L, 1f));
		add(offers, new Offer(
				"patrol_vest_1",
				"armor:patrol_vest",
				1, 3.6f, 3_100, 4_100L, 1f));
		add(offers, new Offer(
				"ceramic_rig_1",
				"armor:ceramic_rig",
				1, 5.2f, 7_200, 9_600L, 1f));
		add(offers, new Offer(
				"scout_pack_1",
				"backpack:scout_pack",
				1, 1.1f, 1_200, 1_600L, 1f));
		add(offers, new Offer(
				"field_pack_1",
				"backpack:field_pack",
				1, 2.0f, 3_100, 4_200L, 1f));
		add(offers, firearm("needle_9", 0.90f, 850));
		add(offers, firearm("sentinel_9", 1.05f, 1_450));
		add(offers, firearm("sparrow_9", 0.78f, 1_750));
		add(offers, firearm("shuttle_9", 2.20f, 2_100));
		add(offers, firearm("hive_9", 2.55f, 3_200));
		add(offers, firearm("whisper_9", 2.35f, 3_900));
		add(offers, firearm("jackal_9", 2.70f, 4_400));
		add(offers, firearm("ward_556", 3.00f, 4_200));
		add(offers, firearm("river_556", 2.85f, 4_700));
		add(offers, firearm("carbine_556", 2.65f, 5_600));
		add(offers, firearm("mountain_762", 3.60f, 6_100));
		add(offers, firearm("foundry_762", 4.15f, 7_200));
		add(offers, firearm("bolt_12", 3.10f, 4_800));
		add(offers, firearm("breaker_12", 3.55f, 5_900));
		add(offers, firearm("rainstorm_12", 4.80f, 9_800));
		add(offers, firearm("longstreet_762", 4.00f, 7_600));
		add(offers, firearm("watchtower_556", 3.70f, 8_900));
		add(offers, firearm("frontier_762", 4.60f, 11_200));
		return Collections.unmodifiableMap(offers);
	}

	private static Offer ammunition(
			String definitionId,
			int quantity,
			float unitWeight,
			int unitValue,
			long purchasePrice) {
		return new Offer(
				definitionId + "_" + quantity,
				"ammo:" + definitionId,
				quantity,
				unitWeight,
				unitValue,
				purchasePrice,
				1f);
	}

	private static Offer firearm(
			String definitionId,
			float unitWeight,
			int unitValue) {
		return new Offer(
				"firearm_" + definitionId,
				"firearm:" + definitionId,
				1,
				unitWeight,
				unitValue,
				Math.round(unitValue * 1.3f),
				1f);
	}

	private static void add(Map<String, Offer> offers, Offer offer) {
		if (offers.put(offer.offerId, offer) != null) {
			throw new IllegalStateException(
					"Duplicate Bukov vendor offer: " + offer.offerId);
		}
	}
}
