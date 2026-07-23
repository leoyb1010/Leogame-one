#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
test_dir="$(mktemp -d "${TMPDIR:-/tmp}/bukov-camera-test.XXXXXX")"
trap 'rm -rf "$test_dir"' EXIT

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  java_home="$JAVA_HOME"
elif [[ -x /opt/homebrew/bin/brew ]]; then
  java_home="$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
else
  java_home=""
fi
javac_cmd="${java_home:+$java_home/bin/}javac"
java_cmd="${java_home:+$java_home/bin/}java"

"$javac_cmd" -d "$test_dir" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeCameraFollow.java" \
  "$repo_dir/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/RealtimeCameraFollowStandaloneTest.java"

"$java_cmd" -cp "$test_dir" \
  com.shatteredpixel.shatteredpixeldungeon.bukov.runtime.RealtimeCameraFollowStandaloneTest

python3 - "$repo_dir" <<'PY'
from pathlib import Path
import sys

root = Path(sys.argv[1])
scene = (root / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java").read_text()
world = (root / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java").read_text()
collision = (root / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/LevelCollisionMap.java").read_text()

checks = {
    "Bukov disables legacy edge scrolling": "edgeScroll.set(BukovMode.active() ? 0 : 1)" in scene,
    "camera target uses interpolated hero sprite": "updateRealtimeCamera(Game.elapsed)" in world,
    "legacy scene pan is cancelled": "camera.shift(ZERO_CAMERA_SHIFT)" in world,
    "camera preserves UI center offset": "+ camera.centerOffset.x" in world and "+ camera.centerOffset.y" in world,
    "movement collision uses level dimensions": "return level.width()" in collision and "return level.height()" in collision,
    "movement collision does not depend on viewport": "Camera" not in collision and "viewport" not in collision,
}
failed = [name for name, passed in checks.items() if not passed]
if failed:
    raise SystemExit("FAIL: " + "; ".join(failed))
print("PASS: camera lifecycle and level-bound movement static gate")
PY
