#!/usr/bin/env python3
"""Deterministic release checks for Escape from Bukov."""

from __future__ import annotations

import csv
import json
import re
import struct
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_APP_NAME = "逃离布科夫"
EXPECTED_ENGLISH_NAME = "Escape from Bukov"
EXPECTED_BUNDLE_ID = "com.leoyuan.escapefrombukov"

IOS_ICON_SIZES = {
    "Icon-20.png": 20,
    "Icon-20@2x.png": 40,
    "Icon-20@3x.png": 60,
    "Icon-29.png": 29,
    "Icon-29@2x.png": 58,
    "Icon-29@3x.png": 87,
    "Icon-40.png": 40,
    "Icon-40@2x.png": 80,
    "Icon-40@3x.png": 120,
    "Icon-60@2x.png": 120,
    "Icon-60@3x.png": 180,
    "Icon-76.png": 76,
    "Icon-76@2x.png": 152,
    "Icon-83.5@2x.png": 167,
    "Icon-1024.png": 1024,
}

DESKTOP_ICON_SIZES = {
    "icon_16.png": 16,
    "icon_32.png": 32,
    "icon_48.png": 48,
    "icon_64.png": 64,
    "icon_128.png": 128,
    "icon_256.png": 256,
}


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def check_build_metadata() -> None:
    build = (ROOT / "build.gradle").read_text(encoding="utf-8")
    require(f"appName = '{EXPECTED_APP_NAME}'" in build, "unexpected appName")
    require(f"appBundleId = '{EXPECTED_BUNDLE_ID}'" in build, "unexpected appBundleId")
    ios_info = (ROOT / "ios/Info.plist").read_text(encoding="utf-8")
    require("${appApplePackageName}" in ios_info,
            "iOS Info.plist must use the generated bundle identifier")
    require("<string>${appName}</string>" in ios_info,
            "iOS Info.plist must use the generated app name")

    desktop_build = (ROOT / "desktop/build.gradle").read_text(encoding="utf-8")
    require('"--mac-app-category", "games"' in desktop_build,
            "macOS package must use the Games application category")
    require('"--resource-dir", file("./src/main/jpackage")' in desktop_build,
            "macOS package must use the project-owned Info.plist template")
    require('"--mac-package-identifier", appBundleId' in desktop_build,
            "macOS package identifier must come from the compatible bundle ID")

    ios_build = (ROOT / "ios/build.gradle").read_text(encoding="utf-8")
    require("robovmProps.setProperty('appApplePackageName', appBundleId)" in ios_build,
            "iOS package identifier must come from the compatible bundle ID")
    require("robovmProps.setProperty('appName', appName)" in ios_build,
            "iOS product name must come from the Bukov app name")

    mac_info = (ROOT / "desktop/src/main/jpackage/Info.plist").read_text(encoding="utf-8")
    require("<string>DEPLOY_BUNDLE_IDENTIFIER</string>" in mac_info,
            "macOS Info.plist must use the generated bundle identifier")
    require("<string>DEPLOY_BUNDLE_NAME</string>" in mac_info,
            "macOS Info.plist must use the generated product name")
    require("<string>DEPLOY_APP_CATEGORY</string>" in mac_info,
            "macOS Info.plist must use the generated application category")

    apple_metadata = "\n".join((
        ios_info,
        mac_info,
        (ROOT / "desktop/macos-entitlements.plist").read_text(encoding="utf-8"),
    ))
    for microphone_key in (
        "NSMicrophoneUsageDescription",
        "com.apple.security.device.audio-input",
    ):
        require(microphone_key not in apple_metadata,
                f"unused microphone metadata remains: {microphone_key}")

    player_facing_metadata = "\n".join((
        ios_info,
        mac_info,
        desktop_build,
        ios_build,
    )).lower()
    for former_name in (
        "shattered pixel dungeon",
        "leogame-one",
        "leogameone",
        "leo's dungeon",
        "leo dungeon",
    ):
        require(former_name not in player_facing_metadata,
                f"former product identity remains in Apple package metadata: {former_name}")

    launch_screen = (ROOT / "ios/assets/LaunchScreen.storyboard").read_text(encoding="utf-8")
    require(f'text="{EXPECTED_APP_NAME}"' in launch_screen, "Chinese Bukov launch title is missing")
    require(f'text="{EXPECTED_ENGLISH_NAME.upper()}"' in launch_screen,
            "English Bukov launch title is missing")
    require('text="LEO"' not in launch_screen, "former Leo launch mark is still referenced")
    require('image="Banner"' not in launch_screen and 'image="BannerWide"' not in launch_screen,
            "upstream launch banner is still referenced")

    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    require(readme.startswith(f"# {EXPECTED_APP_NAME} · {EXPECTED_ENGLISH_NAME.upper()}"),
            "README product heading is inconsistent")
    require("Shattered Pixel Dungeon" in readme and "Pixel Dungeon" in readme and "GPLv3" in readme,
            "README must preserve upstream attribution and GPLv3 notice")


def png_info(path: Path) -> tuple[int, int, int, int]:
    data = path.read_bytes()[:26]
    require(len(data) == 26 and data[:8] == b"\x89PNG\r\n\x1a\n", f"invalid PNG: {path}")
    require(data[12:16] == b"IHDR", f"PNG has no leading IHDR: {path}")
    width, height = struct.unpack(">II", data[16:24])
    return width, height, data[24], data[25]


def check_icon_matrix() -> None:
    ios_root = ROOT / "ios/assets/Assets.xcassets/AppIcon.appiconset"
    contents = json.loads((ios_root / "Contents.json").read_text(encoding="utf-8"))
    declared = {entry["filename"] for entry in contents["images"] if "filename" in entry}
    require(set(IOS_ICON_SIZES) == declared, "iOS AppIcon Contents.json does not match the icon matrix")

    for name, size in IOS_ICON_SIZES.items():
        width, height, bit_depth, color_type = png_info(ios_root / name)
        require((width, height) == (size, size), f"wrong iOS icon dimensions: {name}")
        require(bit_depth == 8 and color_type == 2, f"iOS icon must be opaque 8-bit RGB: {name}")

    desktop_root = ROOT / "desktop/src/main/assets/icons"
    for name, size in DESKTOP_ICON_SIZES.items():
        width, height, bit_depth, color_type = png_info(desktop_root / name)
        require((width, height) == (size, size), f"wrong desktop icon dimensions: {name}")
        require(bit_depth == 8 and color_type == 2, f"desktop icon must be opaque 8-bit RGB: {name}")
    for name in ("mac.icns", "windows.ico"):
        require((desktop_root / name).stat().st_size > 1024, f"desktop icon container is missing: {name}")


def property_keys(path: Path, prefix: str) -> set[str]:
    keys = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith(prefix) and "=" in line:
            keys.add(line.split("=", 1)[0])
    return keys


def property_values(path: Path) -> str:
    values = []
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped and not stripped.startswith(("#", "!")) and "=" in line:
            values.append(line.split("=", 1)[1])
    return "\n".join(values)


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


def check_player_visible_branding() -> None:
    banned = (
        "leo's dungeon",
        "leo’s dungeon",
        "leo的地牢",
        "leo dungeon",
        "dungeon siege",
        "dungeon assault",
        "leogame-one",
        "leogameone",
    )
    property_roots = (
        ROOT / "core/src/main/assets/messages",
        ROOT / "desktop/src/main/resources/com/shatteredpixel/shatteredpixeldungeon/desktop",
    )
    for root in property_roots:
        for path in root.rglob("*.properties"):
            visible_text = property_values(path).lower()
            for former_name in banned:
                require(former_name not in visible_text,
                        f"former product identity remains in player-visible copy: {path}: {former_name}")

    about_en = property_values(ROOT / "core/src/main/assets/messages/scenes/scenes.properties")
    about_zh = property_values(ROOT / "core/src/main/assets/messages/scenes/scenes_zh.properties")
    require(EXPECTED_ENGLISH_NAME in about_en, "English player copy is missing the Bukov product name")
    require(EXPECTED_APP_NAME in about_zh, "Chinese player copy is missing the Bukov product name")

    about_source = (
        ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/AboutScene.java"
    ).read_text(encoding="utf-8").lower()
    for required in ("Shattered Pixel Dungeon", "Pixel Dungeon", "Evan Debenham", "Watabou"):
        require(required.lower() in about_source, f"About screen lost required attribution: {required}")


def check_offline_adapters() -> None:
    for relative in ("desktop/build.gradle", "ios/build.gradle"):
        text = (ROOT / relative).read_text(encoding="utf-8")
        require(":services:news:offlineNews" in text, f"offline news adapter missing in {relative}")
        require(":services:updates:offlineUpdates" in text, f"offline update adapter missing in {relative}")
        require(":services:news:debugNews" not in text, f"debug news adapter shipped by {relative}")
        require(":services:updates:debugUpdates" not in text, f"debug update adapter shipped by {relative}")


def check_packaged_legal_notices() -> None:
    repository_license = ROOT / "LICENSE.txt"
    packaged_license = ROOT / "core/src/main/assets/legal/LICENSE.txt"
    notices = ROOT / "core/src/main/assets/legal/THIRD_PARTY_NOTICES.txt"
    root_notices = ROOT / "THIRD_PARTY_NOTICES.md"
    require(repository_license.read_bytes() == packaged_license.read_bytes(),
            "packaged GPL license is missing or differs from LICENSE.txt")
    notice_text = notices.read_text(encoding="utf-8")
    require("GNU General Public License" in notice_text,
            "packaged notices must name the project license")
    require("https://github.com/leoyb1010/Leogame-one" in notice_text,
            "packaged notices must include corresponding source")
    require("Shattered Pixel Dungeon" in root_notices.read_text(encoding="utf-8"),
            "root third-party notices lost upstream attribution")


def check_artwork_ledger() -> None:
    ledger_path = ROOT / "artwork/licenses/ASSET_PROVENANCE.csv"
    with ledger_path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    paths = [row["path"] for row in rows]
    require(len(paths) == len(set(paths)), "artwork provenance ledger contains duplicate paths")
    recorded = set(paths)
    artwork_inputs = {
        str(path.relative_to(ROOT))
        for path in (ROOT / "artwork/inbox").rglob("*")
        if path.is_file() and path.name not in {".gitkeep", "README.md"}
    }
    runtime_assets = set()
    for relative_root, suffixes in (
        ("core/src/main/assets/splashes/bukov", {".png", ".jpg", ".jpeg"}),
        ("core/src/main/assets/sprites/bukov", {".png"}),
        ("core/src/main/assets/sounds/bukov", {".wav", ".ogg", ".mp3"}),
        ("core/src/main/assets/environment/bukov", {".png"}),
    ):
        for path in (ROOT / relative_root).rglob("*"):
            if path.is_file() and path.suffix.lower() in suffixes:
                runtime_assets.add(str(path.relative_to(ROOT)))
    operator = ROOT / "core/src/main/assets/sprites/bukov_operator.png"
    if operator.is_file():
        runtime_assets.add(str(operator.relative_to(ROOT)))

    missing = sorted((artwork_inputs | runtime_assets) - recorded)
    require(not missing, "assets missing from provenance ledger: " + ", ".join(missing))
    for row in rows:
        require(row["provider"], f"artwork provider must not be blank: {row['path']}")
        require(row["source_or_prompt_reference"],
                f"artwork source or prompt reference must not be blank: {row['path']}")
        require(row["rights_status"], f"artwork rights status must not be blank: {row['path']}")


def check_ci_gates() -> None:
    workflow = (ROOT / ".github/workflows/ci.yml").read_text(encoding="utf-8")
    for gate in (":core:test", ":desktop:test", ":ios:test", ":desktop:jpackageImage", "validate_release.py"):
        require(gate in workflow, f"CI gate missing: {gate}")


def check_bukov_release_manifest() -> None:
    subprocess.run(
        [sys.executable, str(ROOT / "scripts/bukov_release_manifest_check.py")],
        cwd=ROOT,
        check=True,
    )


def main() -> None:
    check_build_metadata()
    check_icon_matrix()
    check_localization()
    check_player_visible_branding()
    check_offline_adapters()
    check_packaged_legal_notices()
    check_artwork_ledger()
    check_ci_gates()
    check_bukov_release_manifest()
    print("Escape from Bukov release validation passed.")


if __name__ == "__main__":
    main()
