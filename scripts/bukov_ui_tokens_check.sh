#!/bin/zsh
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
repo_root="$(CDPATH= cd -- "$script_dir/.." && pwd)"

if ! command -v rg >/dev/null 2>&1; then
  print -u2 "Bukov UI token gate requires ripgrep (rg)"
  exit 1
fi

scan_targets=(
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/ui"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/bukov"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/BukovLevel.java"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/WelcomeScene.java"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovHubScene.java"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/BukovDeploymentScene.java"
  "$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/GameScene.java"
)

# UI colors belong in ui_tokens.json. The single allowed hexadecimal value is
# the RGB bit mask used by BukovUiTokens.withAlpha(), not a rendered color.
# GameScene is shared with the classic host game, so its known legacy-only
# visuals are explicitly exempted while every new literal remains gate-visible.
hardcoded_colors="$(
  rg -n --glob '*.java' \
    '0x[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?\b' \
    "${scan_targets[@]}" \
    | grep -v 'BukovUiTokens.java:.*color & 0xFFFFFF' \
    | grep -v 'GameScene.java:.*TextureCache.createSolid(0x88000000)' \
    | grep -v 'GameScene.java:.*new Flare.*0xFFFF00' \
    | grep -v 'GameScene.java:.*color < 0x01000000' \
    | grep -v 'GameScene.java:.*0xFF000000 | color' \
    | grep -v 'GameScene.java:.*gameOver.show( 0x000000' \
    | grep -v 'GameScene.java:.*bossSlain.show( 0xFFFFFF' \
    || true
)"

if [[ -n "$hardcoded_colors" ]]; then
  print -u2 "Bukov production Java contains hardcoded UI colors:"
  print -u2 -- "$hardcoded_colors"
  print -u2 "Move rendered colors to bukov/content/ui_tokens.json."
  exit 1
fi

print "Bukov production Java color-token scan passed."

exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovUiTokensTest' \
  --tests '*BukovUiTokenBoundaryGuardTest' \
  --tests '*BukovTypographyHapticTokenGuardTest' \
  --tests '*ExperienceContractRegistryTest' \
  --rerun-tasks \
  --no-daemon
