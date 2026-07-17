#!/usr/bin/env python3
"""Deterministic internal release checks for Leo's Dungeon Siege."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_BUNDLE_ID = "leogameone"


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def check_bundle_id() -> None:
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")
    require(f"appBundleId = '{EXPECTED_BUNDLE_ID}'" in build, "unexpected appBundleId")
    info = (ROOT / "ios/Info.plist").read_text(encoding="utf-8")
    require("${appApplePackageName}" in info, "Info.plist must use the generated bundle identifier")
    launch_screen = (ROOT / "ios/assets/LaunchScreen.storyboard").read_text(encoding="utf-8")
    require('text="LEO"' in launch_screen, "Leo launch mark is missing")
    require('image="Banner"' not in launch_screen and 'image="BannerWide"' not in launch_screen,
            "upstream launch banner is still referenced")


def property_keys(path: Path, prefix: str) -> set[str]:
    keys = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith(prefix) and "=" in line:
            keys.add(line.split("=", 1)[0])
    return keys


def check_localization() -> None:
    for group in ("actors", "items", "journal", "levels", "misc", "plants", "scenes", "ui", "windows"):
        english = ROOT / f"core/src/main/assets/messages/{group}/{group}.properties"
        chinese = ROOT / f"core/src/main/assets/messages/{group}/{group}_zh.properties"
        require(property_keys(english, "") == property_keys(chinese, ""),
                f"global English/Chinese key mismatch in {group}")

    pairs = (
        ("misc/misc.properties", "misc/misc_zh.properties", "leoidentityconfig."),
        ("scenes/scenes.properties", "scenes/scenes_zh.properties", "scenes.aboutscene."),
        ("ui/ui.properties", "ui/ui_zh.properties", "ui.changelist.leochanges."),
        ("windows/windows.properties", "windows/windows_zh.properties", "windows.wndleowelcome."),
    )
    base = ROOT / "core/src/main/assets/messages"
    for english, chinese, prefix in pairs:
        english_keys = property_keys(base / english, prefix)
        chinese_keys = property_keys(base / chinese, prefix)
        require(english_keys, f"no English keys for {prefix}")
        require(english_keys == chinese_keys, f"English/Chinese key mismatch for {prefix}")

    cjk_literal = re.compile(r'"(?:[^"\\]|\\.)*[\u3400-\u9fff](?:[^"\\]|\\.)*"')
    for relative in (
        "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/AboutScene.java",
        "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/WelcomeScene.java",
        "desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLauncher.java",
        "desktop/src/main/java/com/shatteredpixel/shatteredpixeldungeon/desktop/DesktopLaunchValidator.java",
    ):
        source = (ROOT / relative).read_text(encoding="utf-8")
        require(not cjk_literal.search(source), f"hard-coded Chinese UI string in {relative}")


def check_offline_adapters() -> None:
    for relative in ("desktop/build.gradle", "ios/build.gradle"):
        text = (ROOT / relative).read_text(encoding="utf-8")
        require(":services:news:offlineNews" in text, f"offline news adapter missing in {relative}")
        require(":services:updates:offlineUpdates" in text, f"offline update adapter missing in {relative}")
        require(":services:news:debugNews" not in text, f"debug news adapter shipped by {relative}")
        require(":services:updates:debugUpdates" not in text, f"debug update adapter shipped by {relative}")


def check_artwork_ledger() -> None:
    ledger_path = ROOT / "artwork/licenses/ASSET_PROVENANCE.csv"
    with ledger_path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    recorded = {row["path"] for row in rows}
    actual = {
        str(path.relative_to(ROOT))
        for path in (ROOT / "artwork/inbox").rglob("*")
        if path.is_file() and path.name not in {".gitkeep", "README.md"}
    }
    require(recorded == actual, "artwork provenance ledger does not match artwork/inbox")
    require(all(row["rights_status"] for row in rows), "artwork rights status must not be blank")


def check_ci_gates() -> None:
    workflow = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    for gate in (":core:test", ":desktop:test", ":ios:test", ":desktop:jpackageImage", "validate_release.py"):
        require(gate in workflow, f"CI gate missing: {gate}")


def main() -> None:
    check_bundle_id()
    check_localization()
    check_offline_adapters()
    check_artwork_ledger()
    check_ci_gates()
    print("Leo internal release validation passed.")


if __name__ == "__main__":
    main()
