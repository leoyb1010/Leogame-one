package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

/**
 * Deterministic weighted selection and first-raid protection policy.
 *
 * Map code supplies only local placement facts; this class does not inspect a
 * level, pathfind, or allocate collections.
 */
public final class FirstRaidEnemySpawnDirector {

	public static final String FIRST_GUNNER = "scavenger_gunner";
	public static final String FIRST_RUSHER = "melee_rusher";
	public static final String FIRST_GUARD = "iron_clasp_guard";
	public static final String FIRST_ALARM = "sensor_doll";
	public static final String FIRST_ELITE = "iron_clasp_captain";
	public static final String FIRST_BOSS = "boss_white_line";

	private static final String[] FIRST_RAID_MILESTONES = {
			FIRST_ELITE,
			FIRST_ALARM,
			FIRST_GUARD,
			FIRST_RUSHER,
			FIRST_GUNNER
	};

	public interface ActiveCounts {
		int active(String definitionId);
	}

	public interface SpawnWeights {
		int weight(EnemyArchetypeDefinition definition);
	}

	public static final class Context {
		public final float elapsedSeconds;
		public final boolean firstRaid;
		public final int distanceFromSpawnRooms;
		public final boolean insidePlayerFieldOfView;
		public final boolean allowVisibleInitialContact;
		public final boolean mandatorySingleRoute;
		public final boolean bossArena;

		public Context(float elapsedSeconds,
					   boolean firstRaid,
					   int distanceFromSpawnRooms,
					   boolean insidePlayerFieldOfView,
					   boolean mandatorySingleRoute,
					   boolean bossArena) {
			this(
					elapsedSeconds,
					firstRaid,
					distanceFromSpawnRooms,
					insidePlayerFieldOfView,
					false,
					mandatorySingleRoute,
					bossArena);
		}

		public Context(float elapsedSeconds,
					   boolean firstRaid,
					   int distanceFromSpawnRooms,
					   boolean insidePlayerFieldOfView,
					   boolean allowVisibleInitialContact,
					   boolean mandatorySingleRoute,
					   boolean bossArena) {
			if (elapsedSeconds < 0f || !com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(elapsedSeconds)) {
				throw new IllegalArgumentException(
						"elapsedSeconds must be finite and non-negative"
				);
			}
			if (distanceFromSpawnRooms < 0) {
				throw new IllegalArgumentException(
						"distanceFromSpawnRooms must not be negative"
				);
			}
			this.elapsedSeconds = elapsedSeconds;
			this.firstRaid = firstRaid;
			this.distanceFromSpawnRooms = distanceFromSpawnRooms;
			this.insidePlayerFieldOfView = insidePlayerFieldOfView;
			this.allowVisibleInitialContact = allowVisibleInitialContact;
			this.mandatorySingleRoute = mandatorySingleRoute;
			this.bossArena = bossArena;
		}
	}

	public static boolean eligible(EnemyArchetypeDefinition definition,
								   Context context,
								   ActiveCounts counts) {
		if (definition == null || context == null || counts == null) {
			throw new IllegalArgumentException(
					"definition, context, and counts are required"
			);
		}
		if ((context.insidePlayerFieldOfView
						&& !context.allowVisibleInitialContact)
				|| context.elapsedSeconds < definition.minimumSpawnSeconds
				|| context.distanceFromSpawnRooms
				< definition.minimumDistanceFromSpawnRooms) {
			return false;
		}
		if (context.firstRaid && !firstRaidRoster(definition.id)) {
			return false;
		}
		if (definition.optionalRouteOnly && context.mandatorySingleRoute) {
			return false;
		}
		if (definition.bossArenaOnly != context.bossArena) {
			return false;
		}
		int limit = context.firstRaid
				? definition.firstRaidMaximumActive
				: definition.maximumActive;
		float opensAt = context.firstRaid
				? Math.max(
						definition.minimumSpawnSeconds,
						definition.firstRaidMinimumSeconds
				)
				: definition.minimumSpawnSeconds;
		return context.elapsedSeconds >= opensAt
				&& counts.active(definition.id) < limit;
	}

	/**
	 * Makes the authored first-level cast actually appear, instead of relying
	 * on a low-probability weighted roll. The newest unlocked missing role is
	 * introduced first; normal weighted selection resumes once it is present.
	 */
	public static EnemyArchetypeDefinition selectFirstRaidMilestone(
			Iterable<EnemyArchetypeDefinition> definitions,
			Context context,
			ActiveCounts counts) {
		if (definitions == null || context == null || counts == null) {
			throw new IllegalArgumentException(
					"definitions, context, and counts are required");
		}
		if (!context.firstRaid) return null;
		for (String requiredId : FIRST_RAID_MILESTONES) {
			if (counts.active(requiredId) > 0) continue;
			for (EnemyArchetypeDefinition definition : definitions) {
				if (requiredId.equals(definition.id)
						&& eligible(definition, context, counts)) {
					return definition;
				}
			}
		}
		return null;
	}

	public static boolean firstRaidRoster(String definitionId) {
		return FIRST_GUNNER.equals(definitionId)
				|| FIRST_RUSHER.equals(definitionId)
				|| FIRST_GUARD.equals(definitionId)
				|| FIRST_ALARM.equals(definitionId)
				|| FIRST_ELITE.equals(definitionId)
				|| FIRST_BOSS.equals(definitionId);
	}

	/**
	 * Returns a weighted deterministic spawn, or null when no definition is
	 * currently eligible. Bosses use explicit arena placement and weight zero.
	 */
	public static EnemyArchetypeDefinition select(
			Iterable<EnemyArchetypeDefinition> definitions,
			Context context,
			ActiveCounts counts,
			long rollKey) {
		return select(
				definitions,
				context,
				counts,
				rollKey,
				new SpawnWeights() {
					@Override
					public int weight(EnemyArchetypeDefinition definition) {
						return definition.spawnWeight;
					}
				});
	}

	public static EnemyArchetypeDefinition select(
			Iterable<EnemyArchetypeDefinition> definitions,
			Context context,
			ActiveCounts counts,
			long rollKey,
			SpawnWeights weights) {
		if (definitions == null || context == null || counts == null
				|| weights == null) {
			throw new IllegalArgumentException(
					"definitions, context, counts, and weights are required"
			);
		}
		int totalWeight = 0;
		for (EnemyArchetypeDefinition definition : definitions) {
			int liveWeight = weights.weight(definition);
			if (liveWeight > 0 && eligible(definition, context, counts)) {
				totalWeight += liveWeight;
			}
		}
		if (totalWeight == 0) {
			return null;
		}
		int roll = (int)com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers
				.remainderUnsigned(mix(rollKey), totalWeight);
		for (EnemyArchetypeDefinition definition : definitions) {
			int liveWeight = weights.weight(definition);
			if (liveWeight <= 0 || !eligible(definition, context, counts)) {
				continue;
			}
			if (roll < liveWeight) {
				return definition;
			}
			roll -= liveWeight;
		}
		throw new IllegalStateException("weighted spawn selection drift");
	}

	private static long mix(long value) {
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private FirstRaidEnemySpawnDirector() {
	}
}
