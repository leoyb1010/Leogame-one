#!/usr/bin/env python3
"""Machine-check the Bukov release manifest against repository facts."""

from __future__ import annotations

import csv
import hashlib
import json
import re
import subprocess
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

    release_commit = manifest["product"]["releaseSourceCommit"]
    require(
        re.fullmatch(r"[0-9a-f]{40}", release_commit) is not None,
        "release source commit must be a full SHA-1",
    )
    commit_probe = subprocess.run(
        ["git", "cat-file", "-e", f"{release_commit}^{{commit}}"],
        cwd=ROOT,
        check=False,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    require(commit_probe.returncode == 0, "release source commit is not present")

    mac = manifest["platforms"]["macOS"]
    ios = manifest["platforms"]["iOS"]
    require(mac["packageEvidence"]["status"] == "passed", "macOS package not passed")
    require(mac["interactiveQa"]["status"] == "passed", "macOS QA not passed")
    require(ios["aotEvidence"]["status"] == "passed", "iOS AOT not passed")
    require(ios["simulatorQa"]["status"] == "passed", "iOS simulator QA not passed")
    require(
        ios["deviceQa"]["status"] == "not_run",
        "physical iOS evidence must be explicit when not run",
    )
    require(
        manifest["validation"]["gradleSuite"]["status"] == "passed",
        "Gradle suite not passed",
    )
    require(
        manifest["validation"]["gradleSuite"]["sourceCommit"] == release_commit,
        "Gradle suite is not bound to the release source commit",
    )
    require(
        manifest["evidencePolicy"]["currentPlatformClaim"]
        == "passed_mac_and_ios_simulator",
        "platform claim must match the verified personal-build scope",
    )

    for label, evidence in (
        ("macOS", mac["packageEvidence"]),
        ("iOS", ios["aotEvidence"]),
    ):
        binary = Path(evidence["binary"])
        require(binary.is_file(), f"{label} evidence binary is missing")
        digest = hashlib.sha256(binary.read_bytes()).hexdigest()
        require(
            digest == evidence["binarySha256"],
            f"{label} evidence binary SHA-256 mismatch",
        )

    qa_template = ROOT / manifest["evidencePolicy"]["qaTemplate"]
    require(qa_template.is_file(), "final QA template is missing")
    qa_text = qa_template.read_text(encoding="utf-8")
    for token in ("Release commit SHA", "macOS", "iOS", "PENDING EVIDENCE"):
        require(token in qa_text, f"QA template missing field: {token}")

    qa_report = ROOT / manifest["evidencePolicy"]["qaReport"]
    require(qa_report.is_file(), "final QA report is missing")
    report_text = qa_report.read_text(encoding="utf-8")
    for token in (release_commit, "PASS", "macOS", "iOS", "NOT RUN"):
        require(token in report_text, f"final QA report missing field: {token}")


def main() -> None:
    manifest = load_json("docs/bukov/RELEASE_MANIFEST.json")
    require(manifest.get("schemaVersion") == 2, "unsupported release manifest schema")
    require(
        manifest["product"]["releaseState"]
        == "personal_build_validated_mac_and_ios_simulator",
        "release state must match the validated personal-build scope",
    )
    check_content(manifest)
    check_assets(manifest)
    check_license_and_evidence(manifest)
    print(
        "Bukov release manifest: PASS "
        "(6 themes, 18 firearms, 13 enemies, 4 modes, "
        "72 icon frames, 19 SFX; macOS + iOS simulator evidence verified)"
    )


if __name__ == "__main__":
    main()
