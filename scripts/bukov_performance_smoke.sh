#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
seconds=${1:-60}

if ! [[ "$seconds" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "performance duration must be a positive integer"
  exit 2
fi

mkdir -p "$project_root/build/reports"
report="$project_root/build/reports/bukov-performance-smoke.log"
{
  print "gate=bukov_performance_smoke"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "duration_seconds=$seconds"
  print "host=$(uname -srm)"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovPerformanceSmoke' \
  -Dbukov.performance.seconds="$seconds" \
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
