package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.EffectiveFirearmStats;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentSlot;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmBuild;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmRegistry;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovContractProgress;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovInsuranceReturn;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractDefinition;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Renderer-independent rows for contracts, insurance and firearm builds. */
public final class BukovServicesViewModel {

	public static final class ContractRow {
		public final String contractId;
		public final String title;
		public final String objective;
		public final long progress;
		public final long target;
		public final long reward;
		public final boolean ready;
		public final boolean claimed;

		private ContractRow(
				BukovLongTermContractDefinition definition,
				BukovContractProgress state) {
			contractId = definition.id;
			title = definition.title;
			objective = definition.objective;
			progress = state.progress();
			target = state.target();
			reward = definition.rewardCurrency;
			ready = state.ready();
			claimed = state.claimed();
		}

		public String progressLabel() {
			return progress + "/" + target
					+ (claimed ? " · 已领取" : ready ? " · 可领奖" : "");
		}
	}

	public static final class InsuranceRow {
		public final String itemUid;
		public final String label;
		public final long value;
		public final boolean insured;

		private InsuranceRow(RaidItem item) {
			itemUid = item.itemUid();
			label = BukovHubViewModel.displayName(item.definitionId());
			value = item.totalValue();
			insured = item.insured();
		}
	}

	public static final class FirearmRow {
		public final String itemUid;
		public final String label;
		public final String optic;
		public final String magazine;
		public final String muzzle;
		public final int baseMagazine;
		public final int effectiveMagazine;
		public final float baseSpread;
		public final float effectiveSpread;
		public final float baseRecoil;
		public final float effectiveRecoil;
		public final float baseNoise;
		public final float effectiveNoise;

		private FirearmRow(
				RaidItem item,
				FirearmDefinition base,
				FirearmBuild build,
				EffectiveFirearmStats effective) {
			itemUid = item.itemUid();
			label = BukovHubViewModel.displayName(item.definitionId());
			optic = attachmentName(build, FirearmAttachmentSlot.OPTIC);
			magazine = attachmentName(build, FirearmAttachmentSlot.MAGAZINE);
			muzzle = attachmentName(build, FirearmAttachmentSlot.MUZZLE);
			baseMagazine = base.magazineSize;
			effectiveMagazine = effective.magazineSize;
			baseSpread = base.baseSpreadDeg;
			effectiveSpread = effective.baseSpreadDeg;
			baseRecoil = base.recoilPerShot;
			effectiveRecoil = effective.recoilPerShot;
			baseNoise = base.noiseRadiusTiles;
			effectiveNoise = effective.noiseRadiusTiles;
		}

		public String slotsLabel() {
			return "镜 " + optic + " · 匣 " + magazine + " · 口 " + muzzle;
		}

		public String deltaLabel() {
			return "弹 " + baseMagazine + "→" + effectiveMagazine
					+ " · 散 " + decimal(baseSpread)
					+ "→" + decimal(effectiveSpread)
					+ " · 后 " + decimal(baseRecoil)
					+ "→" + decimal(effectiveRecoil)
					+ " · 声 " + decimal(baseNoise)
					+ "→" + decimal(effectiveNoise);
		}
	}

	public final long currency;
	public final boolean locked;
	public final List<ContractRow> contracts;
	public final List<InsuranceRow> insuranceItems;
	public final List<FirearmRow> firearms;
	public final int pendingInsuranceReturns;
	public final int dueInsuranceReturns;

	private BukovServicesViewModel(
			long currency,
			boolean locked,
			List<ContractRow> contracts,
			List<InsuranceRow> insuranceItems,
			List<FirearmRow> firearms,
			int pendingInsuranceReturns,
			int dueInsuranceReturns) {
		this.currency = currency;
		this.locked = locked;
		this.contracts = Collections.unmodifiableList(contracts);
		this.insuranceItems = Collections.unmodifiableList(insuranceItems);
		this.firearms = Collections.unmodifiableList(firearms);
		this.pendingInsuranceReturns = pendingInsuranceReturns;
		this.dueInsuranceReturns = dueInsuranceReturns;
	}

	static BukovServicesViewModel from(
			BukovProfile profile, boolean locked) {
		List<ContractRow> contracts = new ArrayList<>();
		for (BukovLongTermContractDefinition definition
				: BukovLongTermContractCatalog.all()) {
			contracts.add(new ContractRow(
					definition,
					profile.longTermContracts().progress(definition.id)));
		}

		List<InsuranceRow> insurance = new ArrayList<>();
		for (RaidItem item : profile.loadout().items(profile.stash())) {
			insurance.add(new InsuranceRow(item));
		}
		int formalSettlements =
				profile.statistics().successfulRaids()
						+ profile.statistics().deaths();
		int pending = 0;
		int due = 0;
		for (BukovInsuranceReturn value : profile.insurance().returns()) {
			if (value.claimed()) continue;
			pending++;
			if (value.availableAt(formalSettlements)) due++;
		}

		FirearmRegistry registry = loadFirearms();
		List<FirearmRow> firearms = new ArrayList<>();
		for (RaidItem item : profile.stash().items()) {
			if (!item.definitionId().startsWith("firearm:")) continue;
			String definitionId = item.definitionId().substring(
					"firearm:".length());
			FirearmDefinition base = registry.find(definitionId);
			// Old profiles may legitimately outlive a firearm definition that
			// was renamed or removed. Keep the item safe in the stash and omit
			// only its unsupported workshop row instead of crashing the entire
			// contracts/insurance/services window.
			if (base == null) continue;
			FirearmBuild build = profile.firearmBuilds()
					.build(item.itemUid());
			EffectiveFirearmStats effective = profile.firearmBuilds()
					.effectiveStats(item.itemUid(), base);
			firearms.add(new FirearmRow(item, base, build, effective));
		}
		return new BukovServicesViewModel(
				profile.currency(),
				locked,
				contracts,
				insurance,
				firearms,
				pending,
				due);
	}

	public static String attachmentFor(FirearmAttachmentSlot slot) {
		if (slot == FirearmAttachmentSlot.OPTIC) {
			return FirearmAttachmentCatalog.RED_DOT;
		}
		if (slot == FirearmAttachmentSlot.MAGAZINE) {
			return FirearmAttachmentCatalog.EXTENDED_MAG;
		}
		if (slot == FirearmAttachmentSlot.MUZZLE) {
			return FirearmAttachmentCatalog.SUPPRESSOR;
		}
		throw new IllegalArgumentException("slot is required");
	}

	static boolean supportsFirearm(String storedDefinitionId) {
		if (storedDefinitionId == null
				|| !storedDefinitionId.startsWith("firearm:")) {
			return false;
		}
		String definitionId = storedDefinitionId.substring(
				"firearm:".length());
		return !definitionId.isEmpty()
				&& loadFirearms().find(definitionId) != null;
	}

	private static String attachmentName(
			FirearmBuild build, FirearmAttachmentSlot slot) {
		if (build == null || build.attachment(slot) == null) return "无";
		return FirearmAttachmentCatalog.require(
				build.attachment(slot)).name;
	}

	private static FirearmRegistry loadFirearms() {
		FirearmRegistry registry = new FirearmRegistry();
		if (Gdx.files == null) {
			registry.load(new FileHandle(
					"src/main/assets/" + FirearmRegistry.DEFAULT_PATH));
		} else {
			registry.loadDefault();
		}
		return registry;
	}

	private static String decimal(float value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
