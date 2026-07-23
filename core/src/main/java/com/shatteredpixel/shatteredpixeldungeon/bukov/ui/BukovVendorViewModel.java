package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovEconomyService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovVendorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable renderer-independent stock and warehouse rows for the vendor. */
public final class BukovVendorViewModel {

	public static final class BuyRow {
		public final String offerId;
		public final String label;
		public final int quantity;
		public final float weight;
		public final long itemValue;
		public final long price;
		public final boolean affordable;

		private BuyRow(BukovVendorCatalog.Offer offer, long currency) {
			offerId = offer.offerId;
			label = BukovHubViewModel.displayName(offer.definitionId);
			quantity = offer.quantity;
			weight = offer.quantity * offer.unitWeight;
			itemValue = (long) offer.quantity * offer.unitValue;
			price = offer.purchasePrice;
			affordable = currency >= price;
		}
	}

	public static final class SellRow {
		public final String itemUid;
		public final String label;
		public final int quantity;
		public final float weight;
		public final long itemValue;
		public final long price;
		public final boolean sellable;
		public final String blockReason;

		private SellRow(RaidItem item) {
			itemUid = item.itemUid();
			label = BukovHubViewModel.displayName(item.definitionId());
			quantity = item.quantity();
			weight = item.totalWeight();
			itemValue = item.totalValue();
			sellable = BukovEconomyService.sellable(item);
			price = sellable ? BukovEconomyService.appraisal(item) : 0L;
			blockReason = sellable ? null : "任务或补给物资不可出售";
		}
	}

	public final long currency;
	public final List<BuyRow> offers;
	public final List<SellRow> stash;
	public final boolean tradingLocked;

	private BukovVendorViewModel(
			long currency,
			List<BuyRow> offers,
			List<SellRow> stash,
			boolean tradingLocked) {
		this.currency = currency;
		this.offers = Collections.unmodifiableList(offers);
		this.stash = Collections.unmodifiableList(stash);
		this.tradingLocked = tradingLocked;
	}

	static BukovVendorViewModel from(
			BukovProfile profile,
			List<BukovVendorCatalog.Offer> offers,
			boolean tradingLocked) {
		List<BuyRow> buyRows = new ArrayList<>();
		for (BukovVendorCatalog.Offer offer : offers) {
			buyRows.add(new BuyRow(offer, profile.currency()));
		}
		List<SellRow> sellRows = new ArrayList<>();
		for (RaidItem item : profile.stash().items()) {
			if (!profile.loadout().contains(item.itemUid())) {
				sellRows.add(new SellRow(item));
			}
		}
		return new BukovVendorViewModel(
				profile.currency(),
				buyRows,
				sellRows,
				tradingLocked);
	}
}
