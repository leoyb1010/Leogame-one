package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.EnemyRangedCombatController;
import com.shatteredpixel.shatteredpixeldungeon.bukov.ai.RealtimeEnemyBrain;
import com.shatteredpixel.shatteredpixeldungeon.bukov.audio.PlayerSoundEventBuffer;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeMedicalSystem;
import com.shatteredpixel.shatteredpixeldungeon.bukov.combat.medical.RealtimeStatusState;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The complete resumable raid payload. Keeping session, carried loot and
 * extraction progress in one document prevents partially restored raids.
 */
public final class BukovRaidCheckpoint implements Bundlable {

	public static final int CURRENT_VERSION = 7;

	private static final String VERSION = "checkpoint_version";
	private static final String SESSION = "session";
	private static final String LOOT = "loot";
	private static final String EXTRACTIONS = "extractions";
	private static final String ACTIVE_EXTRACTION = "active_extraction";
	private static final String HOST_ITEMS = "host_items";
	private static final String NEXT_ITEM_SEQUENCE = "next_item_sequence";
	private static final String CONTAINERS = "containers";
	private static final String COMPLETED_EVENTS = "completed_events";
	private static final String DEPLOYMENT_DEFINITIONS =
			"deployment_definitions";
	private static final String PLAYER_STATUS = "player_realtime_status";
	private static final String MEDICAL_RUNTIME = "medical_runtime";
	private static final String ENEMY_RUNTIME = "enemy_runtime";
	private static final String PLAYER_SOUNDS = "player_sound_events";
	private static final String LEGACY_SOUND_SEQUENCE =
			"player_sound_sequence";
	private static final String LEGACY_SOUND_X = "player_sound_x";
	private static final String LEGACY_SOUND_Y = "player_sound_y";
	private static final String LEGACY_SOUND_RADIUS = "player_sound_radius";
	private static final String LEGACY_SOUND_REMAINING =
			"player_sound_remaining";

	public static final class EnemyRuntimeState implements Bundlable {

		private int stableId;
		private String definitionId = "";
		private RealtimeEnemyBrain.Snapshot brain;
		private EnemyRangedCombatController.Snapshot rangedCombat;
		private int heardSoundSequence = Integer.MIN_VALUE;

		public EnemyRuntimeState() {
			// Required by Bundle reflection.
		}

		public EnemyRuntimeState(
				int stableId,
				String definitionId,
				RealtimeEnemyBrain.Snapshot brain,
				EnemyRangedCombatController.Snapshot rangedCombat) {
			this(
					stableId,
					definitionId,
					brain,
					rangedCombat,
					Integer.MIN_VALUE);
		}

		public EnemyRuntimeState(
				int stableId,
				String definitionId,
				RealtimeEnemyBrain.Snapshot brain,
				EnemyRangedCombatController.Snapshot rangedCombat,
				int heardSoundSequence) {
			if (stableId < 0 || brain == null) {
				throw new IllegalArgumentException(
						"stableId and brain snapshot are required");
			}
			this.stableId = stableId;
			this.definitionId = definitionId == null ? "" : definitionId;
			this.brain = brain;
			this.rangedCombat = rangedCombat;
			this.heardSoundSequence = heardSoundSequence;
		}

		public int stableId() {
			return stableId;
		}

		public String definitionId() {
			return definitionId;
		}

		public RealtimeEnemyBrain.Snapshot brain() {
			return brain;
		}

		public EnemyRangedCombatController.Snapshot rangedCombat() {
			return rangedCombat;
		}

		public int heardSoundSequence() {
			return heardSoundSequence;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			bundle.put("stable_id", stableId);
			bundle.put("definition_id", definitionId);
			bundle.put("brain", brain);
			if (rangedCombat != null) {
				bundle.put("ranged_combat", rangedCombat);
			}
			bundle.put("heard_sound_sequence", heardSoundSequence);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			stableId = bundle.getInt("stable_id");
			if (stableId < 0) {
				throw new IllegalStateException(
						"Invalid enemy runtime stable ID");
			}
			definitionId = bundle.getString("definition_id");
			Bundlable restoredBrain = bundle.get("brain");
			if (!(restoredBrain instanceof RealtimeEnemyBrain.Snapshot)) {
				throw new IllegalStateException(
						"Enemy runtime snapshot is missing brain state");
			}
			brain = (RealtimeEnemyBrain.Snapshot)restoredBrain;
			Bundlable restoredRanged = bundle.get("ranged_combat");
			rangedCombat = restoredRanged
					instanceof EnemyRangedCombatController.Snapshot
					? (EnemyRangedCombatController.Snapshot)restoredRanged
					: null;
			heardSoundSequence = bundle.contains("heard_sound_sequence")
					? bundle.getInt("heard_sound_sequence")
					: Integer.MIN_VALUE;
		}
	}

	private int version = CURRENT_VERSION;
	private RaidSession session;
	private LootTransaction loot;
	private final Map<String, ExtractionState> extractions = new LinkedHashMap<>();
	private final Map<String, Item> hostItemsByUid = new LinkedHashMap<>();
	private final Map<String, BukovSearchableContainer> containersById =
			new LinkedHashMap<>();
	private final Set<String> completedEvents = new LinkedHashSet<>();
	private final List<String> deploymentDefinitions = new ArrayList<>();
	private final Map<Integer, EnemyRuntimeState> enemyRuntimeByStableId =
			new LinkedHashMap<>();
	private String activeExtractionId;
	private long nextItemSequence;
	private RealtimeStatusState playerStatus;
	private RealtimeMedicalSystem.Snapshot medicalRuntime;
	private PlayerSoundEventBuffer.Snapshot playerSoundEvents;

	public BukovRaidCheckpoint() {
		// Required by Bundle reflection.
	}

	public BukovRaidCheckpoint(
			RaidSession session,
			LootTransaction loot,
			Collection<ExtractionState> extractionStates) {
		if (session == null) {
			throw new IllegalArgumentException("session is required");
		}
		if (loot == null) {
			throw new IllegalArgumentException("loot is required");
		}
		if (!session.raidId.equals(loot.raidId())) {
			throw new IllegalArgumentException("session and loot raid IDs must match");
		}
		if (extractionStates == null) {
			throw new IllegalArgumentException("extractionStates are required");
		}
		this.session = session;
		this.loot = loot;
		for (ExtractionState extraction : extractionStates) {
			addExtraction(extraction);
		}
	}

	public static BukovRaidCheckpoint sessionOnly(RaidSession session) {
		return new BukovRaidCheckpoint(
				session,
				new LootTransaction(session.raidId, Float.MAX_VALUE),
				Collections.emptyList());
	}

	public int version() {
		return version;
	}

	public RaidSession session() {
		return session;
	}

	public LootTransaction loot() {
		return loot;
	}

	public List<ExtractionState> extractions() {
		return Collections.unmodifiableList(new ArrayList<>(extractions.values()));
	}

	public ExtractionState extraction(String extractionId) {
		return extractions.get(extractionId);
	}

	public String activeExtractionId() {
		return activeExtractionId;
	}

	void setActiveExtractionId(String extractionId) {
		if (extractionId != null && !extractions.containsKey(extractionId)) {
			throw new IllegalArgumentException("Unknown extraction: " + extractionId);
		}
		activeExtractionId = extractionId;
	}

	String itemUid(Item item, String sourceKey) {
		if (item == null) {
			throw new IllegalArgumentException("item is required");
		}
		String existing = item.bukovItemUid();
		if (existing != null) {
			return existing;
		}
		if (sourceKey == null || sourceKey.trim().isEmpty()) {
			throw new IllegalArgumentException("sourceKey is required");
		}
		String uid = session.raidId
				+ ":"
				+ sourceKey
				+ ":"
				+ com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
						.toUnsignedString(nextItemSequence++);
		item.assignBukovItemUid(uid);
		return uid;
	}

	void carryHostItem(String itemUid, Item item) {
		if (item == null || !itemUid.equals(item.bukovItemUid())) {
			throw new IllegalArgumentException("host item UID mismatch");
		}
		Item existing = hostItemsByUid.get(itemUid);
		if (existing != null && existing != item) {
			throw new IllegalStateException("Duplicate carried host item UID: " + itemUid);
		}
		hostItemsByUid.put(itemUid, item);
	}

	Item hostItem(String itemUid) {
		return hostItemsByUid.get(itemUid);
	}

	Item releaseHostItem(String itemUid) {
		return hostItemsByUid.remove(itemUid);
	}

	void addContainer(BukovSearchableContainer container) {
		if (container == null) {
			throw new IllegalArgumentException("container is required");
		}
		if (containersById.containsKey(container.containerId())) {
			throw new IllegalArgumentException(
					"Duplicate container: " + container.containerId());
		}
		if (container.cell() >= 0) {
			for (BukovSearchableContainer existing : containersById.values()) {
				if (existing.cell() == container.cell()) {
					throw new IllegalArgumentException(
							"Duplicate container cell: " + container.cell());
				}
			}
		}
		containersById.put(container.containerId(), container);
	}

	BukovSearchableContainer container(String containerId) {
		return containersById.get(containerId);
	}

	Collection<BukovSearchableContainer> containers() {
		return Collections.unmodifiableCollection(containersById.values());
	}

	boolean eventCompleted(String eventId) {
		return completedEvents.contains(requireEventId(eventId));
	}

	boolean completeEvent(String eventId) {
		return completedEvents.add(requireEventId(eventId));
	}

	void removeCompletedEvent(String eventId) {
		completedEvents.remove(requireEventId(eventId));
	}

	Set<String> completedEvents() {
		return Collections.unmodifiableSet(
				new LinkedHashSet<>(completedEvents));
	}

	void rememberDeploymentDefinitions(Collection<String> definitions) {
		if (definitions == null) {
			throw new IllegalArgumentException(
					"deployment definitions are required");
		}
		List<String> validated = new ArrayList<>();
		for (String definition : definitions) {
			if (definition == null || definition.trim().isEmpty()) {
				throw new IllegalArgumentException(
						"deployment definition is required");
			}
			validated.add(definition);
		}
		deploymentDefinitions.clear();
		deploymentDefinitions.addAll(validated);
	}

	List<String> deploymentDefinitions() {
		return Collections.unmodifiableList(
				new ArrayList<>(deploymentDefinitions));
	}

	RealtimeStatusState playerStatus() {
		return playerStatus;
	}

	RealtimeMedicalSystem.Snapshot medicalRuntime() {
		return medicalRuntime;
	}

	PlayerSoundEventBuffer.Snapshot playerSoundEvents() {
		return playerSoundEvents;
	}

	EnemyRuntimeState enemyRuntime(int stableId) {
		return enemyRuntimeByStableId.get(stableId);
	}

	Collection<EnemyRuntimeState> enemyRuntimeStates() {
		return Collections.unmodifiableCollection(
				enemyRuntimeByStableId.values());
	}

	void replaceRuntimeState(
			RealtimeStatusState playerStatus,
			RealtimeMedicalSystem.Snapshot medicalRuntime,
			Collection<EnemyRuntimeState> enemyRuntimeStates) {
		replaceRuntimeState(
				playerStatus,
				medicalRuntime,
				enemyRuntimeStates,
				playerSoundEvents);
	}

	void replaceRuntimeState(
			RealtimeStatusState playerStatus,
			RealtimeMedicalSystem.Snapshot medicalRuntime,
			Collection<EnemyRuntimeState> enemyRuntimeStates,
			PlayerSoundEventBuffer.Snapshot playerSoundEvents) {
		if (playerStatus == null
				|| medicalRuntime == null
				|| enemyRuntimeStates == null) {
			throw new IllegalArgumentException(
					"Complete realtime state is required");
		}
		Map<Integer, EnemyRuntimeState> validated = new LinkedHashMap<>();
		for (EnemyRuntimeState state : enemyRuntimeStates) {
			if (state == null
					|| validated.put(state.stableId(), state) != null) {
				throw new IllegalArgumentException(
						"Duplicate or missing enemy runtime state");
			}
		}
		this.playerStatus = playerStatus;
		this.medicalRuntime = medicalRuntime;
		this.playerSoundEvents = playerSoundEvents;
		enemyRuntimeByStableId.clear();
		enemyRuntimeByStableId.putAll(validated);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(VERSION, version);
		bundle.put(SESSION, session);
		bundle.put(LOOT, loot);
		bundle.put(EXTRACTIONS, extractions.values());
		bundle.put(ACTIVE_EXTRACTION, activeExtractionId);
		bundle.put(HOST_ITEMS, hostItemsByUid.values());
		bundle.put(NEXT_ITEM_SEQUENCE, nextItemSequence);
		bundle.put(CONTAINERS, containersById.values());
		bundle.put(COMPLETED_EVENTS,
				completedEvents.toArray(new String[0]));
		bundle.put(
				DEPLOYMENT_DEFINITIONS,
				deploymentDefinitions.toArray(new String[0]));
		if (playerStatus != null) bundle.put(PLAYER_STATUS, playerStatus);
		if (medicalRuntime != null) {
			bundle.put(MEDICAL_RUNTIME, medicalRuntime);
		}
		bundle.put(ENEMY_RUNTIME, enemyRuntimeByStableId.values());
		if (playerSoundEvents != null) {
			bundle.put(PLAYER_SOUNDS, playerSoundEvents);
		}
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		int restoredVersion = bundle.getInt(VERSION);
		if (restoredVersion <= 0 || restoredVersion > CURRENT_VERSION) {
			throw new IllegalStateException(
					"Unsupported Bukov raid checkpoint version: " + restoredVersion);
		}
		Bundlable restoredSession = bundle.get(SESSION);
		Bundlable restoredLoot = bundle.get(LOOT);
		if (!(restoredSession instanceof RaidSession)
				|| !(restoredLoot instanceof LootTransaction)) {
			throw new IllegalStateException("Incomplete Bukov raid checkpoint");
		}

		BukovRaidCheckpoint restored = new BukovRaidCheckpoint(
				(RaidSession) restoredSession,
				(LootTransaction) restoredLoot,
				restoreExtractions(bundle.getCollection(EXTRACTIONS)));
		restored.version = restoredVersion;
		String restoredActive = bundle.getString(ACTIVE_EXTRACTION);
		if (!restoredActive.isEmpty()) {
			restored.setActiveExtractionId(restoredActive);
		}
		for (Bundlable stored : bundle.getCollection(HOST_ITEMS)) {
			if (!(stored instanceof Item)) {
				throw new IllegalStateException("Unexpected carried host item");
			}
			Item item = (Item) stored;
			if (item.bukovItemUid() == null) {
				throw new IllegalStateException("Carried host item lacks Bukov UID");
			}
			restored.carryHostItem(item.bukovItemUid(), item);
		}
		restored.nextItemSequence = bundle.getLong(NEXT_ITEM_SEQUENCE);
		if (restored.nextItemSequence < 0L) {
			throw new IllegalStateException("Invalid Bukov item sequence");
		}
		if (restoredVersion >= 3) {
			for (Bundlable stored : bundle.getCollection(CONTAINERS)) {
				if (!(stored instanceof BukovSearchableContainer)) {
					throw new IllegalStateException("Unexpected Bukov container");
				}
				restored.addContainer((BukovSearchableContainer) stored);
			}
		}
		if (restoredVersion >= 4) {
			String[] events = bundle.getStringArray(COMPLETED_EVENTS);
			if (events != null) {
				for (String event : events) {
					restored.completeEvent(event);
				}
			}
		}
		if (restoredVersion >= 5) {
			restored.rememberDeploymentDefinitions(
					java.util.Arrays.asList(
							bundle.getStringArray(DEPLOYMENT_DEFINITIONS)));
		}
		if (restoredVersion >= 6) {
			Bundlable status = bundle.get(PLAYER_STATUS);
			Bundlable medical = bundle.get(MEDICAL_RUNTIME);
			if (status != null || medical != null) {
				if (!(status instanceof RealtimeStatusState)
						|| !(medical
								instanceof RealtimeMedicalSystem.Snapshot)) {
					throw new IllegalStateException(
							"Incomplete realtime player checkpoint");
				}
				restored.playerStatus = (RealtimeStatusState)status;
				restored.medicalRuntime =
						(RealtimeMedicalSystem.Snapshot)medical;
			}
			for (Bundlable stored :
					bundle.getCollection(ENEMY_RUNTIME)) {
				if (!(stored instanceof EnemyRuntimeState)) {
					throw new IllegalStateException(
							"Unexpected enemy runtime checkpoint");
				}
				EnemyRuntimeState state = (EnemyRuntimeState)stored;
				if (restored.enemyRuntimeByStableId.put(
						state.stableId(), state) != null) {
					throw new IllegalStateException(
							"Duplicate enemy runtime stable ID");
				}
			}
		}
		if (restoredVersion >= 7) {
			Bundlable sounds = bundle.get(PLAYER_SOUNDS);
			if (sounds != null
					&& !(sounds
							instanceof PlayerSoundEventBuffer.Snapshot)) {
				throw new IllegalStateException(
						"Unexpected player sound event checkpoint");
			}
			restored.playerSoundEvents =
					(PlayerSoundEventBuffer.Snapshot)sounds;
		} else if (bundle.contains(LEGACY_SOUND_SEQUENCE)) {
			restored.playerSoundEvents =
					PlayerSoundEventBuffer.Snapshot.legacySingleSlot(
							bundle.getInt(LEGACY_SOUND_SEQUENCE),
							bundle.getFloat(LEGACY_SOUND_X),
							bundle.getFloat(LEGACY_SOUND_Y),
							bundle.getFloat(LEGACY_SOUND_RADIUS),
							bundle.getFloat(LEGACY_SOUND_REMAINING));
		}
		// v2 had no container collection; v3 had no mission events; v4 had
		// no durable deployment template; v5 had no fixed-step player or
		// ordinary-enemy runtime snapshot; v6 had no sound-event buffer.
		restored.version = CURRENT_VERSION;

		version = restored.version;
		session = restored.session;
		loot = restored.loot;
		extractions.clear();
		extractions.putAll(restored.extractions);
		hostItemsByUid.clear();
		hostItemsByUid.putAll(restored.hostItemsByUid);
		activeExtractionId = restored.activeExtractionId;
		nextItemSequence = restored.nextItemSequence;
		containersById.clear();
		containersById.putAll(restored.containersById);
		completedEvents.clear();
		completedEvents.addAll(restored.completedEvents);
		deploymentDefinitions.clear();
		deploymentDefinitions.addAll(restored.deploymentDefinitions);
		playerStatus = restored.playerStatus;
		medicalRuntime = restored.medicalRuntime;
		playerSoundEvents = restored.playerSoundEvents;
		enemyRuntimeByStableId.clear();
		enemyRuntimeByStableId.putAll(
				restored.enemyRuntimeByStableId);
	}

	void addExtraction(ExtractionState extraction) {
		if (extraction == null) {
			throw new IllegalArgumentException("extraction is required");
		}
		if (extractions.put(extraction.extractionId(), extraction) != null) {
			throw new IllegalArgumentException(
					"Duplicate extraction: " + extraction.extractionId());
		}
	}

	private static List<ExtractionState> restoreExtractions(
			Collection<Bundlable> storedExtractions) {
		List<ExtractionState> result = new ArrayList<>();
		for (Bundlable stored : storedExtractions) {
			if (!(stored instanceof ExtractionState)) {
				throw new IllegalStateException("Unexpected extraction entry");
			}
			result.add((ExtractionState) stored);
		}
		return result;
	}

	private static String requireEventId(String eventId) {
		if (eventId == null || eventId.trim().isEmpty()) {
			throw new IllegalArgumentException("eventId is required");
		}
		return eventId;
	}
}
