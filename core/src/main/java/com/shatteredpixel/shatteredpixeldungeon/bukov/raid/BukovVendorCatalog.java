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
		public final String requiredMapId;

		private Offer(
				String offerId,
				String definitionId,
				int quantity,
				float unitWeight,
				int unitValue,
				long purchasePrice,
				float durability,
				String requiredMapId) {
			this.offerId = offerId;
			this.definitionId = definitionId;
			this.quantity = quantity;
			this.unitWeight = unitWeight;
			this.unitValue = unitValue;
			this.purchasePrice = purchasePrice;
			this.durability = durability;
			this.requiredMapId = requiredMapId;
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

	/**
	 * Returns the stock earned by the profile's highest unlocked campaign map.
	 *
	 * A raw new or legacy profile can have no map IDs until the hideout
	 * reconciliation is persisted. It still receives the starting stock, while
	 * a later map retained by an old save also implies every earlier tier.
	 */
	public static List<Offer> availableFor(BukovProfile profile) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		int unlockedTier = unlockedTier(profile);
		List<Offer> available = new ArrayList<>();
		for (Offer offer : OFFERS.values()) {
			if (requiredTier(offer) <= unlockedTier) {
				available.add(offer);
			}
		}
		return Collections.unmodifiableList(available);
	}

	public static boolean isAvailable(BukovProfile profile, Offer offer) {
		if (profile == null) {
			throw new IllegalArgumentException("profile is required");
		}
		if (offer == null) {
			throw new IllegalArgumentException("offer is required");
		}
		return requiredTier(offer) <= unlockedTier(profile);
	}

	private static Map<String, Offer> createOffers() {
		Map<String, Offer> offers = new LinkedHashMap<>();
		add(offers, new Offer(
				"ammo_9_standard_24",
				"ammo:ammo_9_standard",
				24, 0.012f, 12, 360L, 1f,
				"fog_depot"));
		add(offers, ammunition(
				"fog_depot",
				"ammo_9_training", 30, 0.012f, 5, 190L));
		add(offers, ammunition(
				"rust_workshop",
				"ammo_9_subsonic", 24, 0.013f, 22, 660L));
		add(offers, ammunition(
				"rust_workshop",
				"ammo_556_standard", 24, 0.013f, 18, 540L));
		add(offers, ammunition(
				"flooded_passage",
				"ammo_556_armor_piercing", 18, 0.013f, 42, 950L));
		add(offers, ammunition(
				"overgrown_yard",
				"ammo_762_standard", 20, 0.024f, 24, 600L));
		add(offers, ammunition(
				"cold_storage",
				"ammo_762_expanding", 16, 0.025f, 48, 960L));
		add(offers, ammunition(
				"flooded_passage",
				"ammo_12g_buckshot", 12, 0.045f, 28, 420L));
		add(offers, new Offer(
				"bandage_1",
				"bandage",
				1, 0.12f, 180, 220L, 1f,
				"fog_depot"));
		add(offers, new Offer(
				"soft_vest_1",
				"armor:soft_vest",
				1, 2.4f, 1_400, 1_800L, 1f,
				"fog_depot"));
		add(offers, new Offer(
				"patrol_vest_1",
				"armor:patrol_vest",
				1, 3.6f, 3_100, 4_100L, 1f,
				"rust_workshop"));
		add(offers, new Offer(
				"ceramic_rig_1",
				"armor:ceramic_rig",
				1, 5.2f, 7_200, 9_600L, 1f,
				"cold_storage"));
		add(offers, new Offer(
				"scout_pack_1",
				"backpack:scout_pack",
				1, 1.1f, 1_200, 1_600L, 1f,
				"fog_depot"));
		add(offers, new Offer(
				"field_pack_1",
				"backpack:field_pack",
				1, 2.0f, 3_100, 4_200L, 1f,
				"rust_workshop"));
		add(offers, firearm("fog_depot", "needle_9", 0.90f, 850));
		add(offers, firearm("fog_depot", "sentinel_9", 1.05f, 1_450));
		add(offers, firearm("rust_workshop", "sparrow_9", 0.78f, 1_750));
		add(offers, firearm("fog_depot", "shuttle_9", 2.20f, 2_100));
		add(offers, firearm("rust_workshop", "hive_9", 2.55f, 3_200));
		add(offers, firearm("flooded_passage", "whisper_9", 2.35f, 3_900));
		add(offers, firearm("flooded_passage", "jackal_9", 2.70f, 4_400));
		add(offers, firearm("rust_workshop", "ward_556", 3.00f, 4_200));
		add(offers, firearm("flooded_passage", "river_556", 2.85f, 4_700));
		add(offers, firearm("overgrown_yard", "carbine_556", 2.65f, 5_600));
		add(offers, firearm("overgrown_yard", "mountain_762", 3.60f, 6_100));
		add(offers, firearm("overgrown_yard", "foundry_762", 4.15f, 7_200));
		add(offers, firearm("flooded_passage", "bolt_12", 3.10f, 4_800));
		add(offers, firearm("overgrown_yard", "breaker_12", 3.55f, 5_900));
		add(offers, firearm("sealed_lab", "rainstorm_12", 4.80f, 9_800));
		add(offers, firearm("cold_storage", "longstreet_762", 4.00f, 7_600));
		add(offers, firearm("cold_storage", "watchtower_556", 3.70f, 8_900));
		add(offers, firearm("sealed_lab", "frontier_762", 4.60f, 11_200));
		return Collections.unmodifiableMap(offers);
	}

	private static Offer ammunition(
			String requiredMapId,
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
				1f,
				requiredMapId);
	}

	private static Offer firearm(
			String requiredMapId,
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
				1f,
				requiredMapId);
	}

	private static int unlockedTier(BukovProfile profile) {
		List<String> maps = BukovCareerProgression.allMapIds();
		int unlockedTier = 0;
		for (String unlocked : profile.unlockedMaps()) {
			int tier = maps.indexOf(unlocked);
			if (tier > unlockedTier) {
				unlockedTier = tier;
			}
		}
		return unlockedTier;
	}

	private static int requiredTier(Offer offer) {
		int tier = BukovCareerProgression.allMapIds().indexOf(
				offer.requiredMapId);
		if (tier < 0) {
			throw new IllegalStateException(
					"Vendor offer requires an unknown map: "
							+ offer.offerId + " -> " + offer.requiredMapId);
		}
		return tier;
	}

	private static void add(Map<String, Offer> offers, Offer offer) {
		if (offers.put(offer.offerId, offer) != null) {
			throw new IllegalStateException(
					"Duplicate Bukov vendor offer: " + offer.offerId);
		}
	}
}
