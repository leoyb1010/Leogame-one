package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RaidSession implements Bundlable {

	private static final String SEED = "seed";
	private static final String RAID_ID = "raid_id";
	private static final String ELAPSED = "elapsed";
	private static final String SETTLED = "settled";
	private static final String MAP_HASH = "map_hash";
	private static final String INITIAL_ENEMY_SPAWN_COMPLETED =
			"initial_enemy_spawn_completed";
	private static final String ENEMY_SPAWN_EPOCH = "enemy_spawn_epoch";
	private static final String KILL_COUNT = "kill_count";
	private static final String RAID_MODE = "raid_mode";
	private static final String RAID_ORDINAL = "raid_ordinal";
	private static final String KEY_DOORS = "key_doors";
	private static final String BALANCE_TELEMETRY = "balance_telemetry";
	private static final String BALANCE_VISITED_ROOMS =
			"balance_visited_rooms";
	private static final int MAX_BALANCE_ROOMS = 128;

	public long seed;
	public String raidId;
	public float elapsedSeconds;
	public boolean settled;
	public String mapHash;
	private boolean initialEnemySpawnCompleted;
	private long enemySpawnEpoch;
	private int killCount;
	private BukovRaidMode raidMode = BukovRaidMode.EXPEDITION;
	private int raidOrdinal = 1;
	private BukovKeyDoorState keyDoors = new BukovKeyDoorState();
	private RaidBalanceTelemetry balanceTelemetry =
			RaidBalanceTelemetry.unavailable();
	private final ArrayList<String> balanceVisitedRooms =
			new ArrayList<>();

	public static RaidSession create(long seed, String raidId) {
		return create(seed, raidId, BukovRaidMode.EXPEDITION, 1);
	}

	public static RaidSession create(
			long seed,
			String raidId,
			BukovRaidMode raidMode,
			int raidOrdinal) {
		if (raidId == null || raidId.trim().isEmpty()) {
			throw new IllegalArgumentException("raidId is required");
		}
		if (raidMode == null) {
			throw new IllegalArgumentException("raidMode is required");
		}
		if (raidOrdinal <= 0) {
			throw new IllegalArgumentException("raidOrdinal must be positive");
		}
		RaidSession result = new RaidSession();
		result.seed = seed;
		result.raidId = raidId;
		result.raidMode = raidMode;
		result.raidOrdinal = raidOrdinal;
		result.balanceTelemetry =
				RaidBalanceTelemetry.begin(seed, raidMode);
		return result;
	}

	public BukovRaidMode raidMode() {
		return raidMode;
	}

	public int raidOrdinal() {
		return raidOrdinal;
	}

	public BukovKeyDoorState keyDoors() {
		return keyDoors;
	}

	public boolean firstRaidProtectionActive() {
		return raidOrdinal == 1 && elapsedSeconds < 90f;
	}

	public void advance(float deltaSeconds) {
		if (!settled) {
			elapsedSeconds += Math.max(0f, deltaSeconds);
		}
	}

	public void markSettled() {
		if (settled) {
			throw new IllegalStateException("Raid already settled: " + raidId);
		}
		settled = true;
	}

	public boolean initialEnemySpawnCompleted() {
		return initialEnemySpawnCompleted;
	}

	public void markInitialEnemySpawnCompleted() {
		initialEnemySpawnCompleted = true;
	}

	/**
	 * Claims the next deterministic enemy-spawn sequence before evaluating a
	 * spawn. Persisting attempts, including rejected ones, prevents a resumed
	 * raid from rerolling a different enemy at the same elapsed time.
	 */
	public long claimEnemySpawnEpoch() {
		if (enemySpawnEpoch == Long.MAX_VALUE) {
			throw new IllegalStateException("Enemy spawn epoch exhausted");
		}
		return enemySpawnEpoch++;
	}

	public long enemySpawnEpoch() {
		return enemySpawnEpoch;
	}

	public void recordKill() {
		if (killCount == Integer.MAX_VALUE) {
			throw new IllegalStateException("Kill count exhausted");
		}
		killCount++;
	}

	public int killCount() {
		return killCount;
	}

	public RaidBalanceTelemetry balanceTelemetry() {
		return balanceTelemetry.copy();
	}

	/**
	 * Records authoritative map context once the host can identify the route
	 * actually traversed. Unknown context remains empty rather than inferred.
	 */
	public void identifyBalanceContext(String themeId, String routeId) {
		balanceTelemetry.identifyContext(themeId, routeId);
	}

	public void identifyBalanceTheme(String themeId) {
		balanceTelemetry.identifyTheme(themeId);
	}

	public void identifyBalanceRoute(String routeId) {
		balanceTelemetry.identifyRoute(routeId);
	}

	public void recordBalanceRoom(String roomId) {
		if (!balanceTelemetry.available()) return;
		if (roomId == null || roomId.isEmpty()) {
			throw new IllegalArgumentException("roomId is required");
		}
		if (balanceVisitedRooms.contains(roomId)) return;
		if (balanceVisitedRooms.size() >= MAX_BALANCE_ROOMS) {
			throw new IllegalStateException(
					"Balance route evidence exhausted");
		}
		balanceVisitedRooms.add(roomId);
	}

	public List<String> balanceVisitedRooms() {
		return Collections.unmodifiableList(
				new ArrayList<>(balanceVisitedRooms));
	}

	public void recordContainerSearch() {
		balanceTelemetry.recordContainerSearch();
	}

	public void recordFirefight() {
		balanceTelemetry.recordFirefight();
	}

	public void updateBalanceFirefightState(
			boolean combatActive,
			boolean searchingAfterContact) {
		balanceTelemetry.updateFirefightState(
				combatActive,
				searchingAfterContact);
	}

	public void recordDamageTaken(int amount) {
		balanceTelemetry.recordDamageTaken(amount);
	}

	RaidBalanceTelemetry settledBalanceTelemetry(
			RaidOutcome outcome,
			ExtractionState.Type extractionType) {
		RaidBalanceTelemetry.End end;
		if (outcome == RaidOutcome.DEATH) {
			end = RaidBalanceTelemetry.End.DEATH;
		} else if (extractionType == ExtractionState.Type.BASIC) {
			end = RaidBalanceTelemetry.End.BASIC_EXTRACTION;
		} else if (extractionType == ExtractionState.Type.CONDITIONAL) {
			end = RaidBalanceTelemetry.End.CONDITIONAL_EXTRACTION;
		} else if (extractionType == ExtractionState.Type.TEMPORARY) {
			end = RaidBalanceTelemetry.End.TEMPORARY_EXTRACTION;
		} else {
			throw new IllegalArgumentException(
					"successful raid requires extraction type");
		}
		return balanceTelemetry.settle(
				elapsedSeconds,
				killCount,
				0L,
				end);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(SEED, seed);
		bundle.put(RAID_ID, raidId);
		bundle.put(ELAPSED, elapsedSeconds);
		bundle.put(SETTLED, settled);
		bundle.put(MAP_HASH, mapHash);
		bundle.put(
				INITIAL_ENEMY_SPAWN_COMPLETED,
				initialEnemySpawnCompleted);
		bundle.put(ENEMY_SPAWN_EPOCH, enemySpawnEpoch);
		bundle.put(KILL_COUNT, killCount);
		bundle.put(RAID_MODE, raidMode);
		bundle.put(RAID_ORDINAL, raidOrdinal);
		bundle.put(KEY_DOORS, keyDoors);
		bundle.put(BALANCE_TELEMETRY, balanceTelemetry);
		bundle.put(
				BALANCE_VISITED_ROOMS,
				balanceVisitedRooms.toArray(new String[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		seed = bundle.getLong(SEED);
		raidId = bundle.getString(RAID_ID);
		elapsedSeconds = bundle.getFloat(ELAPSED);
		settled = bundle.getBoolean(SETTLED);
		mapHash = bundle.getString(MAP_HASH);
		initialEnemySpawnCompleted =
				bundle.getBoolean(INITIAL_ENEMY_SPAWN_COMPLETED);
		enemySpawnEpoch = bundle.getLong(ENEMY_SPAWN_EPOCH);
		killCount = bundle.getInt(KILL_COUNT);
		BukovRaidMode restoredMode = bundle.contains(RAID_MODE)
				? bundle.getEnum(RAID_MODE, BukovRaidMode.class)
				: null;
		raidMode = restoredMode == null
				? BukovRaidMode.EXPEDITION : restoredMode;
		int restoredOrdinal = bundle.contains(RAID_ORDINAL)
				? bundle.getInt(RAID_ORDINAL) : 1;
		raidOrdinal = restoredOrdinal <= 0 ? 1 : restoredOrdinal;
		if (bundle.contains(KEY_DOORS)) {
			Bundlable restoredKeyDoors = bundle.get(KEY_DOORS);
			if (!(restoredKeyDoors instanceof BukovKeyDoorState)) {
				throw new IllegalStateException(
						"Invalid persisted key door state");
			}
			keyDoors = (BukovKeyDoorState) restoredKeyDoors;
		} else {
			// Safe migration for checkpoints created before key doors existed.
			keyDoors = new BukovKeyDoorState();
		}
		if (bundle.contains(BALANCE_TELEMETRY)) {
			Bundlable restoredTelemetry = bundle.get(BALANCE_TELEMETRY);
			if (!(restoredTelemetry instanceof RaidBalanceTelemetry)) {
				throw new IllegalStateException(
						"Invalid persisted balance telemetry");
			}
			balanceTelemetry =
					(RaidBalanceTelemetry) restoredTelemetry;
		} else {
			// Partial facts cannot be reconstructed from a legacy checkpoint.
			balanceTelemetry = RaidBalanceTelemetry.unavailable();
		}
		balanceVisitedRooms.clear();
		if (bundle.contains(BALANCE_VISITED_ROOMS)) {
			String[] restoredRooms =
					bundle.getStringArray(BALANCE_VISITED_ROOMS);
			if (restoredRooms != null) {
				for (String roomId : restoredRooms) {
					if (roomId == null
							|| roomId.isEmpty()
							|| balanceVisitedRooms.contains(roomId)
							|| balanceVisitedRooms.size()
									>= MAX_BALANCE_ROOMS) {
						throw new IllegalStateException(
								"Invalid persisted balance route evidence");
					}
					balanceVisitedRooms.add(roomId);
				}
			}
		}
		if (enemySpawnEpoch < 0L || killCount < 0) {
			throw new IllegalStateException("Invalid realtime raid progress");
		}
	}
}
