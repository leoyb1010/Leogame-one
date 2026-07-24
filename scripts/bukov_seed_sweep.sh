#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
count=${1:-500}

if ! [[ "$count" =~ '^[1-9][0-9]*$' ]] || (( count > 10000 )); then
  print -u2 "seed count must be an integer from 1 to 10000"
  exit 2
fi

exec "$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSeedSweepTest' \
  --tests '*BukovFirstRaidCriticalPathSeedGateTest' \
  -Dbukov.seed.count="$count" \
  -Dbukov.firstRaidSeedCount="$count" \
  --rerun-tasks \
  --no-daemon
