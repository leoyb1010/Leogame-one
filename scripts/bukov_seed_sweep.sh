#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
count=${1:-500}

if ! [[ "$count" =~ '^[1-9][0-9]*$' ]] || (( count > 10000 )); then
  print -u2 "seed count must be an integer from 1 to 10000"
  exit 2
fi

mkdir -p "$project_root/build/reports"
report="$project_root/build/reports/bukov-seed-sweep.log"
{
  print "gate=bukov_seed_sweep"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "seed_count=$count"
  print "host=$(uname -srm)"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSeedSweepTest' \
  --tests '*BukovFirstRaidCriticalPathSeedGateTest' \
  -Dbukov.seed.count="$count" \
  -Dbukov.firstRaidSeedCount="$count" \
  --rerun-tasks \
  --no-daemon 2>&1 | tee -a "$report"
gate_exit=${pipestatus[1]}
set -e
{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$report"
exit "$gate_exit"
