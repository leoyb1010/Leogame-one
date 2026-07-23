#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
count=${1:-500}

if ! [[ "$count" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "seed count must be a positive integer"
  exit 2
fi

exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSeedSweepTest' \
  -Dbukov.seed.count="$count" \
  --rerun-tasks \
  --no-daemon
