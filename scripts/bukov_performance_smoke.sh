#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
simulated_seconds_at_60hz=${1:-60}

if ! [[ "$simulated_seconds_at_60hz" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "simulated frame duration must be a positive integer"
  exit 2
fi

simulated_frames=$((simulated_seconds_at_60hz * 60))
print "Bukov simulated-frame CPU benchmark; this is not a wall-clock soak."
mkdir -p "$project_root/build/reports"
report="$project_root/build/reports/bukov-performance-smoke.log"
{
  print "gate=bukov_performance_smoke"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "benchmark_kind=simulated_frame_cpu"
  print "wall_clock_soak=false"
  print "simulated_seconds_at_60hz=$simulated_seconds_at_60hz"
  print "simulated_frames=$simulated_frames"
  print "host=$(uname -srm)"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovPerformanceSmoke' \
  -Dbukov.performance.seconds="$simulated_seconds_at_60hz" \
  --rerun-tasks \
  --no-daemon \
  2>&1 | tee -a "$report"
gate_exit=${pipestatus[1]}
set -e
{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$report"
exit "$gate_exit"
