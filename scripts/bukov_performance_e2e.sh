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
report="$project_root/build/reports/bukov-performance-e2e.log"
{
  print "gate=bukov_performance_e2e"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "duration_seconds=$seconds"
  print "host=$(uname -srm)"
  print "rendering=false"
} | tee "$report"

set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovEndToEndPerformanceSmoke' \
  -Dbukov.performance.e2e.seconds="$seconds" \
  --rerun-tasks \
  --no-daemon \
  2>&1 | tee -a "$report"
gate_exit=${pipestatus[1]}
set -e

result_xml="$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle/core/test-results/test/TEST-com.shatteredpixel.shatteredpixeldungeon.bukov.performance.BukovEndToEndPerformanceSmoke.xml"
if (( gate_exit == 0 )); then
  if [[ ! -f "$result_xml" ]]; then
    print -u2 "performance result XML is missing: $result_xml"
    gate_exit=1
  else
    performance_json=$(python3 -c \
      'import sys, xml.etree.ElementTree as ET; print((ET.parse(sys.argv[1]).getroot().findtext("system-out") or "").strip())' \
      "$result_xml")
    if [[ "$performance_json" != \{*\} ]]; then
      print -u2 "performance JSON is missing from test output"
      gate_exit=1
    else
      print "performance_json=$performance_json" | tee -a "$report"
    fi
  fi
fi
{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$report"
exit "$gate_exit"
