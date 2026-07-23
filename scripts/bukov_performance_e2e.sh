#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
seconds=${1:-60}

if ! [[ "$seconds" =~ '^[1-9][0-9]*$' ]]; then
  print -u2 "end-to-end performance duration must be a positive integer"
  exit 2
fi

print "Bukov end-to-end CPU gate: 30 enemies, 200 projectiles."
print "This test does not render through the GPU; capture a real build separately."
mkdir -p "$project_root/build/reports"
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovEndToEndPerformanceSmoke' \
  -Dbukov.performance.e2e.seconds="$seconds" \
  --rerun-tasks \
  --no-daemon \
  | tee "$project_root/build/reports/bukov-performance-e2e.log"
