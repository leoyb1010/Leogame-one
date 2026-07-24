#!/usr/bin/env python3
"""Dependency-free scale and obtainability gate for authored Bukov content."""

import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "core/src/main/assets/bukov/content"
LOOT_SOURCE = (
    ROOT
    / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
    "bukov/content/BukovFirstRaidLootTables.java"
).read_text(encoding="utf-8")
VENDOR_SOURCE = (
    ROOT
    / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
    "bukov/raid/BukovVendorCatalog.java"
).read_text(encoding="utf-8")
MESSAGE_ROOT = ROOT / "core/src/main/assets/messages"
ECONOMY_MESSAGES = MESSAGE_ROOT / "bukov_economy"
RAID_MESSAGES = MESSAGE_ROOT / "bukov_raid"


def load_properties(directory, name):
    """Load the repository's deliberately strict key=value message format."""
    values = {}
    path = directory / name
    for line_number, raw_line in enumerate(
        path.read_text(encoding="utf-8").splitlines(),
        start=1,
    ):
        line = raw_line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        assert separator, (
            f"invalid strict key=value line in {path}:{line_number}: "
            f"{raw_line}"
        )
        key = key.strip()
        assert key, f"empty properties key in {path}:{line_number}"
        assert key not in values, (
            f"duplicate properties key in {path}:{line_number}: {key}"
        )
        values[key] = value.strip()
    return values


def load(name, key):
    payload = json.loads((CONTENT / name).read_text(encoding="utf-8"))
    assert payload["schemaVersion"] == 1
    return payload[key]


firearms = load("firearms.json", "firearms")
ammunition = load("ammunition.json", "ammunition")
enemies = load("enemies.json", "enemies")
economy_en = load_properties(
    ECONOMY_MESSAGES, "bukov_economy.properties"
)
economy_zh = load_properties(
    ECONOMY_MESSAGES, "bukov_economy_zh.properties"
)
raid_en = load_properties(RAID_MESSAGES, "bukov_raid.properties")
raid_zh = load_properties(RAID_MESSAGES, "bukov_raid_zh.properties")

assert len(firearms) == 18, f"expected 18 firearms, got {len(firearms)}"
firearm_ids = {item["id"] for item in firearms}
assert len(firearm_ids) == len(firearms), "duplicate firearm id"
assert {item["weaponClass"] for item in firearms} == {
    "PISTOL",
    "SUBMACHINE_GUN",
    "CARBINE",
    "ASSAULT_RIFLE",
    "SHOTGUN",
    "MARKSMAN_RIFLE",
    "HEAVY_WEAPON",
}
representative_six = {
    "needle_9": "PISTOL",
    "shuttle_9": "SUBMACHINE_GUN",
    "carbine_556": "CARBINE",
    "bolt_12": "SHOTGUN",
    "longstreet_762": "MARKSMAN_RIFLE",
    "rainstorm_12": "HEAVY_WEAPON",
}
firearm_by_id = {item["id"]: item for item in firearms}
assert {
    firearm_id: firearm_by_id[firearm_id]["weaponClass"]
    for firearm_id in representative_six
} == representative_six

ammo_by_id = {item["id"]: item for item in ammunition}
for ammo_id in ammo_by_id:
    assert ammo_id in VENDOR_SOURCE, f"not obtainable from vendor: {ammo_id}"
for firearm in firearms:
    assert math.isfinite(firearm["damage"]) and firearm["damage"] > 0
    assert math.isfinite(firearm["penetration"]) and firearm["penetration"] >= 0
    assert 30 <= firearm["rpm"] <= 1500
    assert 0 < firearm["magazineSize"] <= 200
    assert 0 < firearm["reloadSeconds"] <= 15
    assert firearm["effectiveRangeTiles"] > 0
    assert firearm["baseSpreadDeg"] >= 0
    assert firearm["movingSpreadDeg"] >= 0
    assert firearm["recoilPerShot"] >= 0
    assert firearm["recoilRecovery"] >= 0
    assert 1 <= firearm["pellets"] <= 20
    assert firearm["noiseRadiusTiles"] >= 0
    assert firearm["weightKg"] > 0 and firearm["value"] > 0
    assert firearm["feedbackProfile"]
    assert 0.72 <= firearm["soundPitch"] <= 1.28
    assert 0.35 <= firearm["soundGain"] <= 1.5
    for field in (
        "muzzleIntensity",
        "tracerIntensity",
        "impactIntensity",
        "feedbackIntensity",
    ):
        assert 0.35 <= firearm[field] <= 1.5, (
            f"invalid {field}: {firearm['id']}"
        )
    default_ammo = ammo_by_id.get(firearm["defaultAmmo"])
    assert default_ammo is not None, f"missing ammo: {firearm['id']}"
    assert default_ammo["caliber"] == firearm["caliber"], (
        f"incompatible ammo: {firearm['id']}"
    )
    localized_key = f"bukov.economy.item.firearm_{firearm['id']}"
    raid_key = f"bukov.raid.item.firearm_{firearm['id']}"
    assert economy_en.get(localized_key), (
        f"missing English firearm localization: {firearm['id']}"
    )
    assert economy_zh.get(localized_key) == firearm["name"], (
        f"Chinese firearm localization drift: {firearm['id']}"
    )
    assert raid_en.get(raid_key) == economy_en[localized_key], (
        f"raid/economy English firearm name drift: {firearm['id']}"
    )
    assert raid_zh.get(raid_key) == firearm["name"], (
        f"raid Chinese firearm localization drift: {firearm['id']}"
    )
    assert raid_zh[raid_key] == economy_zh[localized_key], (
        f"raid/economy Chinese firearm name drift: {firearm['id']}"
    )
    assert f'firearm("{firearm["id"]}"' in LOOT_SOURCE, (
        f"not obtainable as raid loot: {firearm['id']}"
    )
    assert f'firearm("{firearm["id"]}"' in VENDOR_SOURCE, (
        f"not obtainable from vendor: {firearm['id']}"
    )

feel_signatures = {
    (
        firearm["feedbackProfile"],
        firearm["soundPitch"],
        firearm["soundGain"],
        firearm["muzzleIntensity"],
        firearm["tracerIntensity"],
        firearm["impactIntensity"],
        firearm["feedbackIntensity"],
    )
    for firearm in firearms
}
assert len(feel_signatures) == len(firearms), (
    "every firearm requires an independent sound/FX signature"
)

tiers = {
    tier: sum(enemy["tier"] == tier for enemy in enemies)
    for tier in ("COMMON", "ELITE", "BOSS")
}
assert tiers["COMMON"] + tiers["ELITE"] >= 12, tiers
assert tiers["COMMON"] > 0 and tiers["ELITE"] > 0, tiers
assert tiers["BOSS"] == 1, tiers

enemy_ids = {enemy["id"] for enemy in enemies}
assert len(enemy_ids) == len(enemies), "duplicate enemy id"
behavior_profiles = set()
for enemy in enemies:
    assert enemy["health"] > 0
    assert math.isfinite(enemy["movementSpeed"]) and enemy["movementSpeed"] > 0
    assert math.isfinite(enemy["perceptionRange"]) and enemy["perceptionRange"] > 0
    assert math.isfinite(enemy["engagementRange"]) and enemy["engagementRange"] > 0
    assert 0 <= enemy["minimumDamage"] <= enemy["maximumDamage"]
    assert enemy["maximumActive"] > 0
    assert math.isfinite(enemy["minimumSpawnSeconds"])
    assert enemy["minimumSpawnSeconds"] >= 0
    assert math.isfinite(enemy["firstRaidMinimumSeconds"])
    assert enemy["firstRaidMinimumSeconds"] >= 0
    assert 0 < enemy["firstRaidMaximumActive"] <= enemy["maximumActive"]
    assert enemy["abilities"], f"missing abilities: {enemy['id']}"
    weapon_id = enemy.get("weaponDefinitionId")
    assert weapon_id is None or weapon_id in firearm_ids, (
        f"unknown enemy firearm: {enemy['id']} -> {weapon_id}"
    )
    if enemy["tier"] == "BOSS":
        assert enemy["spawnWeight"] == 0
        assert enemy["optionalRouteOnly"] and enemy["bossArenaOnly"]
    else:
        assert enemy["spawnWeight"] > 0
        assert not enemy["bossArenaOnly"]
    profile = (
        enemy["role"],
        tuple(enemy["abilities"]),
        enemy["movementSpeed"],
        enemy["engagementRange"],
    )
    assert profile not in behavior_profiles, (
        f"duplicate enemy behavior profile: {enemy['id']}"
    )
    behavior_profiles.add(profile)

print(
    "Bukov content scale gate: PASS "
    f"({len(firearms)} firearms, "
    f"{tiers['COMMON']} common, {tiers['ELITE']} elite, "
    f"{tiers['BOSS']} boss)"
)
