package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovSaveService;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeMedicalSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeStatusState;
import com.shatteredpixel.shatteredpixeldungeon.bukov.mission.FirstRaidMission;
import com.shatteredpixel.shatteredpixeldungeon.bukov.tutorial.BukovTutorialEvent;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;

import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateful bridge between the realtime world and Bukov raid persistence.
 *
 * The world sends ticks and player intents here. The coordinator owns the
 * resumable raid document and enforces settlement ordering.
 */
public final class BukovRaidCoordinator {

	public static final class ContainerSnapshot {
		public final String containerId;
		public final int cell;
		public final String lootTableId;
		public final BukovSearchableContainer.State state;
		public final float searchSeconds;
		public final float progressSeconds;
		public final float progressFraction;
		public final int contentCount;
		public final boolean contentsReleased;

		private ContainerSnapshot(BukovSearchableContainer container) {
			containerId = container.containerId();
			cell = container.cell();
			lootTableId = container.lootTableId();
			state = container.state();
			searchSeconds = container.searchSeconds();
			progressSeconds = container.progressSeconds();
			progressFraction = container.progressFraction();
			contentCount = container.contents().size();
			contentsReleased = container.contentsReleased();
		}
	}

	private final BukovSaveService saves;
	private final RaidSettlement settlement;
	private final BukovProfile profile;
	private final BukovRaidCheckpoint checkpoint;
	private boolean finished;

	private BukovRaidCoordinator(
			BukovSaveService saves,
			RaidSettlement settlement,
			BukovProfile profile,
			BukovRaidCheckpoint checkpoint) {
		this.saves = saves;
		this.settlement = settlement;
		this.profile = profile;
		this.checkpoint = checkpoint;
	}

	public static BukovRaidCoordinator start(
			BukovSaveService saves,
			long seed,
			String raidId,
			float maxWeight,
			Collection<ExtractionState> extractions) throws IOException {
		return start(
				saves,
				new RaidSettlement(),
				seed,
				raidId,
				maxWeight,
				extractions,
				Collections.emptyList());
	}

	public static BukovRaidCoordinator start(
			BukovSaveService saves,
			long seed,
			String raidId,
			float maxWeight,
			Collection<ExtractionState> extractions,
			Collection<BukovContainerDefinition> containerDefinitions)
			throws IOException {
		return start(
				saves,
				new RaidSettlement(),
				seed,
				raidId,
				maxWeight,
				extractions,
				containerDefinitions);
	}

	static BukovRaidCoordinator start(
			BukovSaveService saves,
			RaidSettlement settlement,
			long seed,
			String raidId,
			float maxWeight,
			Collection<ExtractionState> extractions) throws IOException {
		return start(
				saves,
				settlement,
				seed,
				raidId,
				maxWeight,
				extractions,
				Collections.emptyList());
	}

	static BukovRaidCoordinator start(
			BukovSaveService saves,
			RaidSettlement settlement,
			long seed,
			String raidId,
			float maxWeight,
			Collection<ExtractionState> extractions,
			Collection<BukovContainerDefinition> containerDefinitions)
			throws IOException {
		requireDependencies(saves, settlement);
		if (extractions == null || extractions.isEmpty()) {
			throw new IllegalArgumentException("at least one extraction is required");
		}
		if (containerDefinitions == null) {
			throw new IllegalArgumentException("containerDefinitions are required");
		}
		if (saves.loadRaidCheckpoint() != null) {
			throw new IllegalStateException("An active Bukov raid already exists");
		}

		BukovProfile storedProfile = saves.loadProfile();
		if (storedProfile.isSettled(raidId)) {
			throw new IllegalArgumentException("raidId was already settled: " + raidId);
		}
		BukovProfile profile = storedProfile.copy();
		BukovRaidMode raidMode = profile.selectedRaidMode();
		// Practice attempts must not advance the four-contract raid counter.
		// Keep a positive session ordinal for the save schema without mutating
		// durable economic progress.
		int raidOrdinal = raidMode.countsTowardEconomyStatistics()
				? profile.beginRaid()
				: Math.max(1, profile.raidsStarted());
		RaidSession session = RaidSession.create(
				seed,
				raidId,
				raidMode,
				raidOrdinal);
		LootTransaction carried = new LootTransaction(raidId, maxWeight);
		profile.rememberCurrentLoadout();
		if (raidMode.usesPlayerLoadout()) {
			for (String itemUid : profile.loadout().selectedUids()) {
				RaidItem item = profile.stash().withdraw(itemUid);
				if (item == null) {
					throw new IllegalStateException(
							"Loadout item disappeared: " + itemUid);
				}
				LootTransaction.PickupResult pickup = carried.pickup(item);
				if (pickup != LootTransaction.PickupResult.ADDED) {
					throw new IllegalStateException(
							"Loadout exceeds raid capacity: " + itemUid);
				}
			}
		}
		profile.loadout().clear();
		BukovRaidCheckpoint checkpoint = new BukovRaidCheckpoint(
				session,
				carried,
				extractions);
		checkpoint.rememberDeploymentDefinitions(
				profile.lastLoadoutDefinitions());
		for (BukovContainerDefinition definition :
				raidMode.configureContainers(containerDefinitions, seed)) {
			if (definition == null) {
				throw new IllegalArgumentException("container definition is required");
			}
			checkpoint.addContainer(definition.create(seed));
		}

		// Persist the raid first. If the following profile write is interrupted,
		// resume() removes the same deployed UIDs from the stash before play.
		saves.saveRaidCheckpoint(checkpoint);
		saves.saveProfile(profile);
		return new BukovRaidCoordinator(saves, settlement, profile, checkpoint);
	}

	/** Returns null when no resumable raid exists. */
	public static BukovRaidCoordinator resume(
			BukovSaveService saves) throws IOException {
		return resume(saves, new RaidSettlement());
	}

	static BukovRaidCoordinator resume(
			BukovSaveService saves,
			RaidSettlement settlement) throws IOException {
		requireDependencies(saves, settlement);
		BukovProfile profile = saves.loadProfile();
		BukovRaidCheckpoint checkpoint = saves.loadRaidCheckpoint();
		if (checkpoint == null) {
			return null;
		}

		if (profile.isSettled(checkpoint.session().raidId)) {
			// Recovery after "profile committed, raid delete interrupted".
			saves.deleteRaid();
			return null;
		}
		if (checkpoint.session().settled) {
			throw new IOException("Settled raid checkpoint has no profile receipt");
		}
		reconcileDeployment(saves, profile, checkpoint);
		return new BukovRaidCoordinator(saves, settlement, profile, checkpoint);
	}

	public BukovProfile profile() {
		return profile;
	}

	public RaidSession session() {
		return checkpoint.session();
	}

	public LootTransaction loot() {
		return checkpoint.loot();
	}

	/**
	 * Atomically claims a profile-wide tutorial flag. False means it had
	 * already been shown; a failed save rolls the in-memory claim back by
	 * reloading the last durable profile.
	 */
	public boolean claimTutorial(BukovTutorialEvent event) throws IOException {
		ensureOpen();
		if (profile.tutorialSeen(event)) {
			return false;
		}
		if (!profile.markTutorialSeen(event)) {
			return false;
		}
		try {
			saves.saveProfile(profile);
			return true;
		} catch (IOException failure) {
			BukovProfile durable = saves.loadProfile();
			profile.replaceWith(durable);
			throw failure;
		}
	}

	public List<ExtractionState> extractions() {
		return checkpoint.extractions();
	}

	public ExtractionState extraction(String extractionId) {
		return checkpoint.extraction(extractionId);
	}

	public String activeExtractionId() {
		return checkpoint.activeExtractionId();
	}

	public boolean finished() {
		return finished;
	}

	public boolean eventCompleted(String eventId) {
		return checkpoint.eventCompleted(eventId);
	}

	public RealtimeStatusState realtimeStatus() {
		return checkpoint.playerStatus();
	}

	public RealtimeMedicalSystem.Snapshot medicalRuntime() {
		return checkpoint.medicalRuntime();
	}

	public BukovRaidCheckpoint.EnemyRuntimeState enemyRuntime(
			int stableId) {
		return checkpoint.enemyRuntime(stableId);
	}

	public Collection<BukovRaidCheckpoint.EnemyRuntimeState>
			enemyRuntimeStates() {
		return checkpoint.enemyRuntimeStates();
	}

	/**
	 * Replaces one coherent fixed-step snapshot before the checkpoint write.
	 * Stable enemy IDs are the join key to host Mob actors after resume.
	 */
	public void updateRealtimeState(
			RealtimeStatusState playerStatus,
			RealtimeMedicalSystem.Snapshot medicalRuntime,
			Collection<BukovRaidCheckpoint.EnemyRuntimeState> enemies) {
		ensureOpen();
		checkpoint.replaceRuntimeState(
				playerStatus, medicalRuntime, enemies);
	}

	/**
	 * Commits a route-changing mission event immediately. If persistence
	 * fails, the in-memory gate remains locked so the visible world never gets
	 * ahead of the resumable checkpoint.
	 */
	public boolean completeEvent(String eventId) throws IOException {
		ensureOpen();
		if (!checkpoint.completeEvent(eventId)) {
			return false;
		}
		try {
			saves.saveRaidCheckpoint(checkpoint);
		} catch (IOException failure) {
			checkpoint.removeCompletedEvent(eventId);
			throw failure;
		}
		return true;
	}

	public List<ContainerSnapshot> containers() {
		List<ContainerSnapshot> result = new ArrayList<>();
		for (BukovSearchableContainer container : checkpoint.containers()) {
			result.add(new ContainerSnapshot(container));
		}
		return Collections.unmodifiableList(result);
	}

	public ContainerSnapshot container(String containerId) {
		BukovSearchableContainer container = checkpoint.container(containerId);
		return container == null ? null : new ContainerSnapshot(container);
	}

	/**
	 * Adds definitions introduced after an older checkpoint was created.
	 * Existing progress and generated contents remain authoritative.
	 */
	public boolean ensureWorldDefinitions(
			Collection<ExtractionState> extractionStates,
			Collection<BukovContainerDefinition> containerDefinitions)
			throws IOException {
		ensureOpen();
		if (extractionStates == null || containerDefinitions == null) {
			throw new IllegalArgumentException("world definitions are required");
		}
		boolean changed = false;
		if (session().raidMode().countsTowardEconomyStatistics()) {
			changed |= profile.ensureRaidStarted(
					checkpoint.session().raidOrdinal());
		}
		for (ExtractionState definition : extractionStates) {
			if (definition == null) {
				throw new IllegalArgumentException("extraction definition is required");
			}
			ExtractionState existing = checkpoint.extraction(definition.extractionId());
			if (existing == null) {
				checkpoint.addExtraction(definition);
				changed = true;
			} else if (existing.type() != definition.type()
					|| existing.interactionSeconds() != definition.interactionSeconds()
					|| existing.opensAtSeconds() != definition.opensAtSeconds()
					|| existing.closesAtSeconds() != definition.closesAtSeconds()) {
				throw new IllegalStateException(
						"Checkpoint extraction definition changed: "
								+ definition.extractionId());
			}
		}
		for (BukovContainerDefinition definition :
				session().raidMode().configureContainers(
						containerDefinitions, session().seed)) {
			if (definition == null) {
				throw new IllegalArgumentException("container definition is required");
			}
			BukovSearchableContainer existing =
					checkpoint.container(definition.containerId);
			if (existing == null) {
				checkpoint.addContainer(definition.create(session().seed));
				changed = true;
			} else if (existing.cell() != definition.cell
					|| !existing.lootTableId().equals(definition.lootTableId)
					|| existing.rolls() != definition.rolls
					|| existing.searchSeconds() != definition.searchSeconds) {
				throw new IllegalStateException(
						"Checkpoint container definition changed: "
								+ definition.containerId);
			}
		}
		if (changed) saves.saveRaidCheckpoint(checkpoint);
		return changed;
	}

	public boolean unlockContainer(String containerId) {
		ensureOpen();
		return requireContainer(containerId).unlock();
	}

	public boolean beginContainerSearch(String containerId) {
		ensureOpen();
		return requireContainer(containerId).begin();
	}

	public BukovSearchableContainer.UpdateResult updateContainerSearch(
			String containerId,
			float deltaSeconds,
			boolean insideRange,
			boolean moving,
			boolean damaged,
			BukovLootTable lootTable) {
		return updateContainerSearch(
				containerId,
				deltaSeconds,
				insideRange,
				moving,
				damaged,
				false,
				lootTable);
	}

	public BukovSearchableContainer.UpdateResult updateContainerSearch(
			String containerId,
			float deltaSeconds,
			boolean insideRange,
			boolean moving,
			boolean damaged,
			boolean reloading,
			BukovLootTable lootTable) {
		ensureOpen();
		return requireContainer(containerId).update(
				deltaSeconds,
				insideRange,
				moving,
				damaged,
				reloading,
				lootTable);
	}

	public int releaseContainerContents(String containerId, Heap heap) {
		ensureOpen();
		return requireContainer(containerId).releaseTo(heap);
	}

	BukovRaidCheckpoint checkpoint() {
		return checkpoint;
	}

	public void tick(
			float deltaSeconds,
			ExtractionState.Interaction extractionInteraction) {
		ensureOpen();
		if (!com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(
				deltaSeconds) || deltaSeconds < 0f) {
			throw new IllegalArgumentException("deltaSeconds must be finite and non-negative");
		}
		if (extractionInteraction == null) {
			throw new IllegalArgumentException("extractionInteraction is required");
		}

		session().advance(deltaSeconds);
		String activeId = checkpoint.activeExtractionId();
		for (ExtractionState extraction : checkpoint.extractions()) {
			ExtractionState.Interaction interaction =
					extraction.extractionId().equals(activeId)
							? extractionInteraction
							: ExtractionState.Interaction.NONE;
			extraction.update(session().elapsedSeconds, deltaSeconds, interaction);
		}
		if (activeId != null && interruptsExtraction(extractionInteraction)) {
			checkpoint.setActiveExtractionId(null);
		}
	}

	public LootTransaction.PickupResult pickup(RaidItem item) {
		ensureOpen();
		return loot().pickup(item);
	}

	public RaidItem drop(String itemUid) {
		ensureOpen();
		return loot().drop(itemUid);
	}

	public boolean beginExtraction(String extractionId) {
		ensureOpen();
		if (checkpoint.activeExtractionId() != null) {
			return checkpoint.activeExtractionId().equals(extractionId);
		}
		ExtractionState extraction = checkpoint.extraction(extractionId);
		if (extraction == null
				|| extraction.completed()
				|| !extraction.availableAt(session().elapsedSeconds)) {
			return false;
		}
		checkpoint.setActiveExtractionId(extractionId);
		return true;
	}

	public void cancelExtraction() {
		ensureOpen();
		ExtractionState active = activeExtraction();
		if (active != null) {
			active.update(
					session().elapsedSeconds,
					0f,
					ExtractionState.Interaction.MOVED);
			checkpoint.setActiveExtractionId(null);
		}
	}

	public void setExtractionCondition(String extractionId, boolean conditionMet) {
		ensureOpen();
		ExtractionState extraction = checkpoint.extraction(extractionId);
		if (extraction == null) {
			throw new IllegalArgumentException("Unknown extraction: " + extractionId);
		}
		extraction.setConditionMet(conditionMet);
		if (!extraction.availableAt(session().elapsedSeconds)
				&& extractionId.equals(checkpoint.activeExtractionId())) {
			checkpoint.setActiveExtractionId(null);
		}
	}

	public void saveCheckpoint() throws IOException {
		ensureOpen();
		saves.saveRaidCheckpoint(checkpoint);
	}

	public RaidResult settleSuccess() throws IOException {
		ensureOpen();
		boolean extracted = false;
		for (ExtractionState extraction : checkpoint.extractions()) {
			if (extraction.completed()) {
				extracted = true;
				break;
			}
		}
		if (!extracted) {
			throw new IllegalStateException("No extraction has completed");
		}
		return settle(RaidOutcome.SUCCESS);
	}

	public RaidResult settleDeath() throws IOException {
		ensureOpen();
		return settle(RaidOutcome.DEATH);
	}

	private RaidResult settle(RaidOutcome outcome) throws IOException {
		// Work on a copy: a failed disk write must not mutate the live profile.
		BukovProfile committedProfile = profile.copy();
		RaidResult result = settlement.settle(
				committedProfile,
				loot(),
				outcome,
				session().elapsedSeconds,
				session().killCount(),
				checkpoint.eventCompleted(FirstRaidMission.EVENT_ID),
				session().raidMode());

		// The receipt and transferred/lost inventory become durable together.
		saves.saveProfile(committedProfile);
		profile.replaceWith(committedProfile);

		// If this fails, resume() sees the durable receipt and retries deletion.
		saves.deleteRaid();
		if (!session().settled) {
			session().markSettled();
		}
		finished = true;
		return result;
	}

	private ExtractionState activeExtraction() {
		String activeId = checkpoint.activeExtractionId();
		return activeId == null ? null : checkpoint.extraction(activeId);
	}

	private BukovSearchableContainer requireContainer(String containerId) {
		BukovSearchableContainer container = checkpoint.container(containerId);
		if (container == null) {
			throw new IllegalArgumentException("Unknown container: " + containerId);
		}
		return container;
	}

	private void ensureOpen() {
		if (finished || session().settled) {
			throw new IllegalStateException("Bukov raid is already finished");
		}
	}

	private static boolean interruptsExtraction(
			ExtractionState.Interaction interaction) {
		return interaction == ExtractionState.Interaction.MOVED
				|| interaction == ExtractionState.Interaction.RELOADED
				|| interaction == ExtractionState.Interaction.HEAVY_HIT;
	}

	private static void requireDependencies(
			BukovSaveService saves,
			RaidSettlement settlement) {
		if (saves == null) {
			throw new IllegalArgumentException("saves are required");
		}
		if (settlement == null) {
			throw new IllegalArgumentException("settlement is required");
		}
	}

	private static void reconcileDeployment(
			BukovSaveService saves,
			BukovProfile profile,
			BukovRaidCheckpoint checkpoint) throws IOException {
		boolean changed = false;
		if (!checkpoint.deploymentDefinitions().isEmpty()
				&& !checkpoint.deploymentDefinitions().equals(
						profile.lastLoadoutDefinitions())) {
			profile.rememberLoadoutDefinitions(
					checkpoint.deploymentDefinitions());
			changed = true;
		}
		for (RaidItem carried : checkpoint.loot().items()) {
			if (carried.foundInRaid() || !profile.stash().contains(carried.itemUid())) {
				continue;
			}
			RaidItem stored = profile.stash().item(carried.itemUid());
			if (!carried.equals(stored)) {
				throw new IOException(
						"Deployment UID payload mismatch: " + carried.itemUid());
			}
			profile.stash().withdraw(carried.itemUid());
			changed = true;
		}
		int selectedBefore = profile.loadout().distinctItemCount();
		profile.loadout().pruneMissing(profile.stash());
		changed |= selectedBefore != profile.loadout().distinctItemCount();
		if (changed) {
			saves.saveProfile(profile);
		}
	}
}
