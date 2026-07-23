#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
iterations=${1:-100}

if ! [[ "$iterations" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "save iterations must be a positive integer"
  exit 2
fi

exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSaveStressTest' \
  -Dbukov.save.iterations="$iterations" \
  --rerun-tasks \
  --no-daemon
