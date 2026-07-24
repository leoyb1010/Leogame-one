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
)

# UI colors belong in ui_tokens.json. The single allowed hexadecimal value is
# the RGB bit mask used by BukovUiTokens.colorWithAlpha(), not a rendered color.
hardcoded_colors="$(
  rg -n --glob '*.java' \
    '0x[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?\b' \
    "${scan_targets[@]}" \
    | grep -v 'BukovUiTokens.java:.*color(token) & 0xFFFFFF' \
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
