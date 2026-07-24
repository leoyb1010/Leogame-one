package com.shatteredpixel.shatteredpixeldungeon.bukov.ui;

import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovProfile;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCheckpoint;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidCoordinator;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovRaidMode;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovEconomyService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovCareerProgression;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovGearRules;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovStarterProvisioning;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovVendorCatalog;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovInsuranceService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.BukovLongTermContractService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmAttachmentSlot;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.firearms.FirearmBuild;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidItem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidOutcome;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.RaidResult;
import com.shatteredpixel.shatteredpixeldungeon.bukov.raid.SettlementReceipt;
import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.messages.BukovMessages;

import java.io.IOException;
import java.util.List;

/** Small application controller shared by the first playable hub window. */
public final class BukovHubController {

	public static final float FIRST_RAID_WEIGHT_LIMIT = 40f;

	private final BukovSaveService saves;
	private final BukovEconomyService economy;
	private BukovProfile profile;
	private final BukovRaidCheckpoint activeCheckpoint;

	public BukovHubController(BukovSaveService saves) throws IOException {
		if (saves == null) {
			throw new IllegalArgumentException("saves are required");
		}
		this.saves = saves;
		economy = new BukovEconomyService(saves);
		activeCheckpoint = saves.loadRaidCheckpoint();
		profile = saves.loadProfile();
		if (activeCheckpoint != null) {
			// During a raid the checkpoint, not the stash/loadout editor, owns
			// the deployed UIDs. Do not prune, provision or persist anything.
			return;
		}
		int selectedBefore = profile.loadout().distinctItemCount();
		profile.loadout().pruneMissing(profile.stash());
		boolean changed = selectedBefore != profile.loadout().distinctItemCount();
		changed |= BukovCareerProgression.reconcile(profile);
		if (profile.selectedRaidMode().usesPlayerLoadout()) {
			changed |= BukovStarterProvisioning.ensure(profile, false);
		}
		if (changed) {
			saves.saveProfile(profile);
		}
	}

	public void recommendLoadout() throws IOException {
		requireEditableLoadout();
		BukovProfile working = profile.copy();
		applyRecommendedLoadout(working);
		commitProfile(working);
	}

	/**
	 * Repairs a formal loadout and validates it in the same player action.
	 *
	 * The caller may transition directly to the deployment scene after this
	 * returns. Persisting only the validated copy prevents a partially repaired
	 * loadout from becoming visible when provisioning or validation fails.
	 */
	public void prepareAndConfirmDeployment() throws IOException {
		if (activeCheckpoint != null) {
			return;
		}
		BukovProfile working = profile.copy();
		if (!working.selectedRaidMode().usesPlayerLoadout()) {
			working.loadout().clear();
		} else if (!viewModel(working).canDeploy) {
			applyRecommendedLoadout(working);
		}
		BukovHubViewModel state = viewModel(working);
		if (!state.canDeploy) {
			throw new IllegalStateException(state.deploymentBlockReason);
		}
		commitProfile(working);
	}

	/** Atomic vendor entry point for a future hideout shop panel. */
	public BukovEconomyService.Receipt buy(
			String transactionId,
			String offerId) throws IOException {
		requireEditableLoadout();
		BukovEconomyService.Receipt receipt =
				economy.buy(transactionId, offerId);
		profile = saves.loadProfile();
		return receipt;
	}

	/** Atomic stash-sale entry point; selected deployment items are rejected. */
	public BukovEconomyService.Receipt sell(
			String transactionId,
			String itemUid) throws IOException {
		requireEditableLoadout();
		BukovEconomyService.Receipt receipt =
				economy.sell(transactionId, itemUid);
		profile = saves.loadProfile();
		return receipt;
	}

	public List<BukovVendorCatalog.Offer> vendorOffers() {
		return BukovVendorCatalog.availableFor(profile);
	}

	public BukovVendorViewModel vendorViewModel() {
		return BukovVendorViewModel.from(
				profile,
				vendorOffers(),
				activeCheckpoint != null);
	}

	public BukovServicesViewModel servicesViewModel() {
		return BukovServicesViewModel.from(
				profile,
				activeCheckpoint != null);
	}

	public BukovLongTermContractService.ClaimResult claimContract(
			String contractId) throws IOException {
		requireEditableLoadout();
		BukovProfile working = profile.copy();
		BukovLongTermContractService.ClaimResult result =
				new BukovLongTermContractService().claim(
						working, contractId);
		if (result.status
				== BukovLongTermContractService.ClaimStatus.CLAIMED) {
			commitProfile(working);
		}
		return result;
	}

	public BukovInsuranceService.ClaimResult claimInsuranceReturns()
			throws IOException {
		requireEditableLoadout();
		BukovProfile working = profile.copy();
		BukovInsuranceService.ClaimResult result =
				new BukovInsuranceService().claimAvailable(working);
		if (!result.returnedItemUids.isEmpty()) {
			commitProfile(working);
		}
		return result;
	}

	public boolean toggleInsurance(String itemUid) throws IOException {
		requireEditableLoadout();
		BukovProfile working = profile.copy();
		if (!working.loadout().contains(itemUid)) {
			throw new IllegalArgumentException(
					BukovMessages.get(
							"bukov.economy.feedback.insurance_selected_only"));
		}
		RaidItem item = working.stash().item(itemUid);
		if (item == null) {
			throw new IllegalArgumentException(BukovMessages.get(
					"bukov.economy.feedback.stash_item_missing"));
		}
		RaidItem updated = item.withInsured(!item.insured());
		working.stash().replace(updated);
		commitProfile(working);
		return updated.insured();
	}

	public boolean toggleAttachment(
			String firearmUid,
			FirearmAttachmentSlot slot) throws IOException {
		requireEditableLoadout();
		if (slot == null) throw new IllegalArgumentException("slot is required");
		BukovProfile working = profile.copy();
		RaidItem item = working.stash().item(firearmUid);
		if (item == null || !item.definitionId().startsWith("firearm:")) {
			throw new IllegalArgumentException(BukovMessages.get(
					"bukov.economy.feedback.stash_firearm_missing"));
		}
		if (!BukovServicesViewModel.supportsFirearm(
				item.definitionId())) {
			throw new IllegalArgumentException(
					BukovMessages.get(
							"bukov.economy.feedback.firearm_unsupported"));
		}
		FirearmBuild build = working.firearmBuilds().build(firearmUid);
		if (build == null) build = new FirearmBuild(firearmUid);
		boolean installed;
		if (build.attachment(slot) == null) {
			build.install(BukovServicesViewModel.attachmentFor(slot));
			installed = true;
		} else {
			build.remove(slot);
			installed = false;
		}
		if (build.attachments().isEmpty()) {
			working.firearmBuilds().remove(firearmUid);
		} else {
			working.firearmBuilds().save(build);
		}
		commitProfile(working);
		return installed;
	}

	public void clearLoadout() throws IOException {
		requireEditableLoadout();
		profile.loadout().clear();
		saves.saveProfile(profile);
	}

	public void cycleRaidMode() throws IOException {
		selectRaidMode(profile.selectedRaidMode().next());
	}

	/**
	 * Cycles only the four economic actions. Training is a permanent, explicit
	 * destination in the hideout and must never appear to be a random contract.
	 */
	public void cycleFormalRaidMode() throws IOException {
		requireEditableLoadout();
		BukovRaidMode current = profile.selectedRaidMode();
		BukovRaidMode next;
		switch (current) {
			case EXPEDITION:
				next = BukovRaidMode.QUICK_SWEEP;
				break;
			case QUICK_SWEEP:
				next = BukovRaidMode.SCAVENGER;
				break;
			case SCAVENGER:
				next = BukovRaidMode.BOSS_CONTRACT;
				break;
			default:
				next = BukovRaidMode.EXPEDITION;
				break;
		}
		selectRaidMode(next);
	}

	public void selectTrainingGround() throws IOException {
		selectRaidMode(BukovRaidMode.TRAINING_GROUND);
	}

	public void cycleSelectedMap() throws IOException {
		requireEditableLoadout();
		List<String> available =
				BukovCareerProgression.availableMapIds(profile);
		int current = available.indexOf(profile.selectedMap());
		profile.selectMap(available.get((current + 1) % available.size()));
		saves.saveProfile(profile);
	}

	public BukovRaidMode selectedRaidMode() {
		return activeCheckpoint == null
				? profile.selectedRaidMode()
				: activeCheckpoint.session().raidMode();
	}

	public boolean hasActiveRaid() {
		return activeCheckpoint != null;
	}

	public void selectRaidMode(BukovRaidMode mode) throws IOException {
		requireEditableLoadout();
		profile.selectRaidMode(mode);
		if (!mode.usesPlayerLoadout()) {
			profile.loadout().clear();
		} else {
			// The mode picker reuses this controller when it returns to the
			// hideout. Risk-free modes intentionally clear the loadout, so a
			// direct switch back to a formal raid must restore a usable firearm
			// and matching ammunition without requiring an app restart.
			BukovStarterProvisioning.ensure(profile, false);
		}
		saves.saveProfile(profile);
	}

	public void toggleItem(String itemUid) throws IOException {
		requireEditableLoadout();
		if (profile.loadout().contains(itemUid)) {
			profile.loadout().remove(itemUid);
		} else {
			profile.loadout().select(itemUid, profile.stash());
		}
		saves.saveProfile(profile);
	}

	public int repeatLastLoadout() throws IOException {
		requireEditableLoadout();
		BukovProfile working = profile.copy();
		working.loadout().clear();
		int selected = 0;
		for (String definitionId : working.lastLoadoutDefinitions()) {
			for (RaidItem item : working.stash().items()) {
				if (definitionId.equals(item.definitionId())
						&& !working.loadout().contains(item.itemUid())) {
					working.loadout().select(
							item.itemUid(), working.stash());
					selected++;
					break;
				}
			}
		}
		if (working.selectedRaidMode().usesPlayerLoadout()
				&& !viewModel(working).canDeploy) {
			applyRecommendedLoadout(working);
		}
		commitProfile(working);
		return selected;
	}

	public BukovHubViewModel viewModel() {
		return BukovHubViewModel.from(
				profile,
				activeCheckpoint,
				FIRST_RAID_WEIGHT_LIMIT);
	}

	public void confirmDeployment() throws IOException {
		if (activeCheckpoint != null) {
			// The deployment callback resumes the existing host save. A hub
			// round-trip must never rewrite the profile or checkpoint first.
			return;
		}
		if (!profile.selectedRaidMode().usesPlayerLoadout()
				&& profile.loadout().distinctItemCount() > 0) {
			profile.loadout().clear();
		}
		BukovHubViewModel state = viewModel();
		if (!state.canDeploy) {
			// Every production entry point shares the same recovery behavior.
			// This also protects callers whose visible hub state became stale
			// while the loadout editor was open.
			prepareAndConfirmDeployment();
			return;
		}
		saves.saveProfile(profile);
	}

	/**
	 * Settles the active checkpoint as a failed action. Host save deletion is a
	 * scene-level concern and happens only after this durable settlement
	 * succeeds.
	 */
	public RaidResult abandonActiveRaid() throws IOException {
		if (activeCheckpoint == null) {
			throw new IllegalStateException(BukovMessages.get(
					"bukov.economy.feedback.no_active_raid"));
		}
		BukovRaidCoordinator raid = BukovRaidCoordinator.resume(saves);
		if (raid == null) {
			throw new IllegalStateException(BukovMessages.get(
					"bukov.economy.feedback.checkpoint_finished"));
		}
		RaidResult result = raid.settleDeath();
		profile = saves.loadProfile();
		return result;
	}

	public String summary() {
		if (activeCheckpoint != null) {
			return BukovMessages.get(
					"bukov.economy.hub.controller_active_summary",
					activeCheckpoint.session().raidId,
					activeCheckpoint.loot().distinctItemCount(),
					formatWeight(activeCheckpoint.loot().totalWeight()),
					activeCheckpoint.loot().totalValue());
		}
		StringBuilder out = new StringBuilder(BukovMessages.get(
				"bukov.economy.hub.controller_summary",
				profile.stash().distinctItemCount(),
				profile.stash().totalValue(),
				profile.loadout().distinctItemCount(),
				formatWeight(profile.loadout().totalWeight(profile.stash())),
				profile.loadout().totalValue(profile.stash())));

		for (RaidItem item : profile.loadout().items(profile.stash())) {
			out.append(BukovMessages.get(
					"bukov.economy.hub.controller_item",
					BukovHubViewModel.displayName(item.definitionId()),
					item.quantity()));
		}

		List<SettlementReceipt> receipts = profile.settlements();
		if (!receipts.isEmpty()) {
			SettlementReceipt latest = receipts.get(receipts.size() - 1);
			out.append(BukovMessages.get(
					latest.outcome() == RaidOutcome.SUCCESS
							? "bukov.economy.hub.controller_last_success"
							: "bukov.economy.hub.controller_last_failed",
					latest.outcome() == RaidOutcome.SUCCESS
							? latest.transferredValue()
							: latest.lostValue()));
		}
		return out.toString();
	}

	private void applyRecommendedLoadout(BukovProfile working) {
		// Free all slots before provisioning; a corrupt or player-filled
		// twelve-stack loadout must not prevent the recovery pair being added.
		working.loadout().clear();
		BukovStarterProvisioning.ensure(working, false);
		working.loadout().clear();
		RaidItem firearm = null;
		RaidItem compatibleAmmo = null;
		float lightestPairWeight = Float.POSITIVE_INFINITY;
		for (RaidItem candidate : working.stash().items()) {
			if (!candidate.definitionId().startsWith("firearm:")) {
				continue;
			}
			for (RaidItem ammunition : working.stash().items()) {
				if (BukovHubViewModel.compatible(
						candidate.definitionId(),
						ammunition.definitionId())) {
					float pairWeight = candidate.totalWeight()
							+ ammunition.totalWeight();
					if (com.shatteredpixel.shatteredpixeldungeon.bukov
							.BukovNumbers.isFinite(pairWeight)
							&& pairWeight <= FIRST_RAID_WEIGHT_LIMIT
							&& (pairWeight < lightestPairWeight
									|| pairWeight == lightestPairWeight
									&& pairKey(candidate, ammunition)
											.compareTo(pairKey(
													firearm,
													compatibleAmmo)) < 0)) {
						firearm = candidate;
						compatibleAmmo = ammunition;
						lightestPairWeight = pairWeight;
					}
				}
			}
		}
		if (firearm == null || compatibleAmmo == null) {
			throw new IllegalStateException(
					BukovMessages.get(
							"bukov.economy.feedback.loadout_pair_failed"));
		}
		working.loadout().select(firearm.itemUid(), working.stash());
		working.loadout().select(
				compatibleAmmo.itemUid(),
				working.stash());
		selectFirstWithDefinition(working, "bandage");
		selectFirstWithPrefix(working, "backpack:");
		selectFirstWithPrefix(working, "armor:");

		if (viewModel(working).overweight) {
			// Optional protection must never turn one-click repair into another
			// deployment blocker. The firearm and ammunition are authoritative.
			working.loadout().clear();
			working.loadout().select(firearm.itemUid(), working.stash());
			working.loadout().select(
					compatibleAmmo.itemUid(),
					working.stash());
		}
		BukovHubViewModel state = viewModel(working);
		if (!state.canDeploy) {
			throw new IllegalStateException(state.deploymentBlockReason);
		}
	}

	private static String selectFirstWithPrefix(
			BukovProfile working,
			String prefix) {
		for (RaidItem item : working.stash().items()) {
			if (item.definitionId().startsWith(prefix)
					&& (BukovGearRules.slotFor(item.definitionId()) == null
							|| item.quantity() == 1)) {
				working.loadout().select(
						item.itemUid(),
						working.stash());
				return item.definitionId();
			}
		}
		return null;
	}

	private static void selectFirstWithDefinition(
			BukovProfile working,
			String definitionId) {
		for (RaidItem item : working.stash().items()) {
			if (definitionId.equals(item.definitionId())) {
				working.loadout().select(
						item.itemUid(),
						working.stash());
				return;
			}
		}
	}

	private static String pairKey(
			RaidItem firearm,
			RaidItem ammunition) {
		if (firearm == null || ammunition == null) {
			return "\uFFFF";
		}
		return firearm.itemUid() + "\u0000" + ammunition.itemUid();
	}

	private static BukovHubViewModel viewModel(BukovProfile working) {
		return BukovHubViewModel.from(
				working,
				null,
				FIRST_RAID_WEIGHT_LIMIT);
	}

	private void requireEditableLoadout() {
		if (activeCheckpoint != null) {
			throw new IllegalStateException(
					BukovMessages.get(
							"bukov.economy.feedback.loadout_raid_locked"));
		}
	}

	private void commitProfile(BukovProfile working) throws IOException {
		// The controller keeps the old detached snapshot until durable storage
		// accepts the new document. A failed write therefore changes neither
		// the visible UI model nor the authoritative save.
		saves.saveProfile(working);
		profile = working;
	}

	private static String formatWeight(float value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
