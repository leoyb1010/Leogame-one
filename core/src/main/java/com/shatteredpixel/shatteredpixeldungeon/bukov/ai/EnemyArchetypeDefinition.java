package com.shatteredpixel.shatteredpixeldungeon.bukov.ai;

public final class EnemyArchetypeDefinition {

	public String id;
	public String name;
	public EnemyTier tier;
	public EnemyRole role;
	public String hostClassHint;
	public String weaponDefinitionId;
	public String[] abilities = new String[0];
	public int health;
	public float movementSpeed;
	public float perceptionRange;
	public float engagementRange;
	public int minimumDamage;
	public int maximumDamage;
	public int spawnWeight;
	public float minimumSpawnSeconds;
	public int minimumDistanceFromSpawnRooms;
	public int maximumActive;
	public float firstRaidMinimumSeconds;
	public int firstRaidMaximumActive;
	public boolean optionalRouteOnly;
	public boolean bossArenaOnly;

	public void validate() {
		require(text(id), "missing id");
		require(text(name), "missing name: " + id);
		require(tier != null, "missing tier: " + id);
		require(role != null, "missing role: " + id);
		require(text(hostClassHint), "missing hostClassHint: " + id);
		require(health > 0, "health must be positive: " + id);
		require(finitePositive(movementSpeed), "invalid movementSpeed: " + id);
		require(finitePositive(perceptionRange), "invalid perceptionRange: " + id);
		require(finitePositive(engagementRange), "invalid engagementRange: " + id);
		require(minimumDamage >= 0 && maximumDamage >= minimumDamage,
				"invalid damage range: " + id);
		require(spawnWeight >= 0 && spawnWeight <= 10_000,
				"invalid spawnWeight: " + id);
		require(finiteNonNegative(minimumSpawnSeconds),
				"invalid minimumSpawnSeconds: " + id);
		require(minimumDistanceFromSpawnRooms >= 0,
				"invalid minimumDistanceFromSpawnRooms: " + id);
		require(maximumActive > 0, "maximumActive must be positive: " + id);
		require(finiteNonNegative(firstRaidMinimumSeconds),
				"invalid firstRaidMinimumSeconds: " + id);
		require(firstRaidMaximumActive > 0
						&& firstRaidMaximumActive <= maximumActive,
				"invalid firstRaidMaximumActive: " + id);
		require(abilities != null, "abilities are required: " + id);
		java.util.HashSet<String> uniqueAbilities = new java.util.HashSet<>();
		for (String ability : abilities) {
			require(text(ability), "blank ability: " + id);
			require(uniqueAbilities.add(ability),
					"duplicate ability " + ability + ": " + id);
		}
		boolean firearmRole = role == EnemyRole.RANGED_SKIRMISHER
				|| role == EnemyRole.ARMORED_SUPPRESSOR
				|| role == EnemyRole.ELITE_COMMANDER;
		require(!firearmRole || text(weaponDefinitionId),
				"firearm role requires weaponDefinitionId: " + id);
		if (tier == EnemyTier.BOSS) {
			require(bossArenaOnly, "boss must be arena-only: " + id);
			require(optionalRouteOnly, "boss must remain optional: " + id);
			require(spawnWeight == 0, "boss cannot enter random spawn roll: " + id);
		} else {
			require(!bossArenaOnly, "non-boss cannot require boss arena: " + id);
			require(spawnWeight > 0, "non-boss needs spawnWeight: " + id);
		}
	}

	private static boolean text(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static boolean finitePositive(float value) {
		return value > 0f
				&& com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value);
	}

	private static boolean finiteNonNegative(float value) {
		return value >= 0f
				&& com.shatteredpixel.shatteredpixeldungeon.bukov.BukovNumbers.isFinite(value);
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
