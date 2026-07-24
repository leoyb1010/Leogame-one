#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
iterations=${1:-100}

if ! [[ "$iterations" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "save iterations must be a positive integer"
  exit 2
fi

mkdir -p "$project_root/build/reports"
report="$project_root/build/reports/bukov-save-stress.log"
{
  print "gate=bukov_save_stress"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "save_iterations=$iterations"
  print "host=$(uname -srm)"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovSaveStressTest' \
  --tests '*BukovDiskSaveStressTest' \
  -Dbukov.save.iterations="$iterations" \
  --rerun-tasks \
  --no-daemon 2>&1 | tee -a "$report"
gate_exit=${pipestatus[1]}
set -e
{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$report"
exit "$gate_exit"
