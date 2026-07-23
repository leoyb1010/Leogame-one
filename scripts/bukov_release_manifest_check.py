#!/usr/bin/env python3
"""Machine-check the Bukov release manifest against repository facts."""

from __future__ import annotations

import csv
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST_PATH = ROOT / "docs/bukov/RELEASE_MANIFEST.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"ERROR: {message}")


def load_json(relative: str) -> dict:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def check_content(manifest: dict) -> None:
    content = manifest["content"]
    for key, payload_key in (
        ("themes", "themes"),
        ("firearms", "firearms"),
        ("enemies", "enemies"),
    ):
        entry = content[key]
        actual = load_json(entry["source"])[payload_key]
        require(
            len(actual) == entry["expected"],
            f"{key} count mismatch: {len(actual)} != {entry['expected']}",
        )

    enemies = load_json(content["enemies"]["source"])["enemies"]
    tiers = {
        tier: sum(enemy["tier"] == tier for enemy in enemies)
        for tier in ("COMMON", "ELITE", "BOSS")
    }
    for tier, field in (("COMMON", "common"), ("ELITE", "elite"), ("BOSS", "boss")):
        require(
            tiers[tier] == content["enemies"][field],
            f"enemy tier mismatch: {tier}",
        )

    mode_source = (ROOT / content["raidModes"]["source"]).read_text(
        encoding="utf-8"
    )
    mode_ids = content["raidModes"]["ids"]
    require(len(mode_ids) == content["raidModes"]["expected"], "raid mode count mismatch")
    for mode_id in mode_ids:
        require(
            re.search(rf"\b{re.escape(mode_id)}\s*\(", mode_source) is not None,
            f"raid mode missing: {mode_id}",
        )
    for source in content["vendor"]["sources"]:
        require((ROOT / source).is_file(), f"vendor source missing: {source}")


def check_assets(manifest: dict) -> None:
    assets = manifest["originalAssets"]
    atlas_manifest = load_json(assets["itemInteractionFrames"]["manifest"])
    require(
        atlas_manifest["frameCount"] == assets["itemInteractionFrames"]["expected"],
        "item/interaction frame count mismatch",
    )
    atlas_path = (
        ROOT
        / "core/src/main/assets"
        / atlas_manifest["atlas"]
    )
    require(atlas_path.is_file(), "item/interaction atlas is missing")
    digest = hashlib.sha256(atlas_path.read_bytes()).hexdigest()
    require(digest == atlas_manifest["sha256"], "item atlas SHA-256 mismatch")

    sound_root = ROOT / assets["soundEffects"]["directory"]
    sounds = sorted(sound_root.glob("*.wav"))
    require(
        len(sounds) == assets["soundEffects"]["expected"],
        f"sound count mismatch: {len(sounds)}",
    )

    ledger_path = ROOT / assets["provenanceLedger"]
    with ledger_path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    by_path = {row["path"]: row for row in rows}
    for sound in sounds:
        relative = str(sound.relative_to(ROOT))
        require(relative in by_path, f"sound missing from provenance: {relative}")
    for generator in assets["generatorScripts"]:
        require((ROOT / generator).is_file(), f"generator missing: {generator}")
        require(
            any(row["source_or_prompt_reference"] == generator for row in rows),
            f"generator has no recorded output in provenance: {generator}",
        )


def check_license_and_evidence(manifest: dict) -> None:
    source_rows = list(
        csv.DictReader(
            (ROOT / "docs/SOURCE_PROVENANCE.csv").open(
                encoding="utf-8", newline=""
            )
        )
    )
    licenses = {row["source"]: row["license"] for row in source_rows}
    require(licenses.get("Leogame-one") == "GPL-3.0", "host GPL record missing")
    require(
        licenses.get("Shattered Pixel Dungeon") == "GPL-3.0",
        "upstream GPL record missing",
    )
    license_text = (ROOT / "LICENSE.txt").read_text(
        encoding="utf-8", errors="replace"
    )
    require("GNU GENERAL PUBLIC LICENSE" in license_text, "GPL license text missing")

    required_pending = (
        manifest["platforms"]["macOS"]["packageEvidence"],
        manifest["platforms"]["macOS"]["interactiveQa"],
        manifest["platforms"]["iOS"]["aotEvidence"],
        manifest["platforms"]["iOS"]["simulatorQa"],
        manifest["platforms"]["iOS"]["deviceQa"],
        manifest["performance"]["packagedGpuFramePacing"],
        manifest["performance"]["thirtyMinuteSoak"],
        manifest["persistence"]["freshInstallRecoveryQa"],
        manifest["validation"]["gradleSuite"]["status"],
        manifest["evidencePolicy"]["currentPlatformClaim"],
    )
    require(
        all(value == "pending_evidence" for value in required_pending),
        "unverified platform evidence must remain pending_evidence",
    )
    qa_template = ROOT / manifest["evidencePolicy"]["qaTemplate"]
    require(qa_template.is_file(), "final QA template is missing")
    qa_text = qa_template.read_text(encoding="utf-8")
    for token in ("Release commit SHA", "macOS", "iOS", "PENDING EVIDENCE"):
        require(token in qa_text, f"QA template missing field: {token}")


def main() -> None:
    manifest = load_json("docs/bukov/RELEASE_MANIFEST.json")
    require(manifest.get("schemaVersion") == 1, "unsupported release manifest schema")
    require(
        manifest["product"]["releaseState"]
        == "candidate_pending_platform_evidence",
        "release state must not overclaim completion",
    )
    check_content(manifest)
    check_assets(manifest)
    check_license_and_evidence(manifest)
    print(
        "Bukov release manifest: PASS "
        "(6 themes, 18 firearms, 13 enemies, 4 modes, "
        "72 icon frames, 19 SFX; platform evidence pending)"
    )


if __name__ == "__main__":
    main()
