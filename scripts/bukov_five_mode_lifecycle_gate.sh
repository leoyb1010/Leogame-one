#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}

# One behavioral gate: each selectable mode must survive the same public
# selection -> deployment -> objective -> extraction -> settlement lifecycle.
exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovFiveModeLifecycleTest' \
  --rerun-tasks \
  --no-daemon
