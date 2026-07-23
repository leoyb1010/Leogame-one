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
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovPerformanceSmoke' \
  -Dbukov.performance.seconds="$seconds" \
  --rerun-tasks \
  --no-daemon \
  | tee "$project_root/build/reports/bukov-performance-smoke.log"
