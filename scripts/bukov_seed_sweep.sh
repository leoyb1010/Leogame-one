#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
count=${1:-500}
# Structural seeds are cheap; a full production-World route is not, so the route
# sample is scaled separately.
#
# Ten routes were too few to be meaningful. Raising the sample to 200 exposed
# interaction-priority and onboarding-combat-lane defects that the smaller
# sample had simply missed. Keep the production route matrix at 200 by default
# so regressions cannot hide behind another lucky ten-seed run.
route_count=${2:-${BUKOV_WORLD_ROUTE_SEEDS:-200}}

if ! [[ "$count" =~ '^[1-9][0-9]*$' ]] || (( count > 10000 )); then
  print -u2 "seed count must be an integer from 1 to 10000"
  exit 2
fi

if ! [[ "$route_count" =~ '^[1-9][0-9]*$' ]] || (( route_count > 10000 )); then
  print -u2 "world route seed count must be an integer from 1 to 10000"
  exit 2
fi

if (( route_count > count )); then
  route_count=$count
fi

mkdir -p "$project_root/build/reports"
report="$project_root/build/reports/bukov-seed-sweep.log"
{
  print "gate=bukov_seed_sweep"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "seed_count=$count"
  print "world_route_seed_count=$route_count"
  print "host=$(uname -srm)"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSeedSweepTest' \
  --tests '*BukovFirstRaidCriticalPathSeedGateTest' \
  --tests '*BukovFirstRaidWorldSeedMatrixTest' \
  -Dbukov.seed.count="$count" \
  -Dbukov.firstRaidSeedCount="$count" \
  -Dbukov.firstRaidWorldSeedCount="$route_count" \
  -Dbukov.firstRaidWorldEvidenceDir="$project_root/build/reports/bukov-first-raid-world-seed-matrix" \
  --rerun-tasks \
  --no-daemon 2>&1 | tee -a "$report"
gate_exit=${pipestatus[1]}
set -e
{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$report"
exit "$gate_exit"
