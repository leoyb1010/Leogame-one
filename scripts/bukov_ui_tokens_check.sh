#!/bin/zsh
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovUiTokensTest' \
  --tests '*BukovTypographyHapticTokenGuardTest' \
  --tests '*ExperienceContractRegistryTest' \
  --rerun-tasks \
  --no-daemon
