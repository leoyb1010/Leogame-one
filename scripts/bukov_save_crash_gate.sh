#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
requested_evidence=${1:-}

if [[ -n "$requested_evidence" ]]; then
  evidence_dir=${requested_evidence:A}
  [[ ! -e "$evidence_dir" ]] || {
    print -u2 "evidence target already exists: $evidence_dir"
    exit 2
  }
  mkdir -p "$evidence_dir"
else
  mkdir -p "$project_root/build/reports"
  evidence_dir=$(mktemp -d \
    "$project_root/build/reports/bukov-save-crash.XXXXXX")
fi

metadata="$evidence_dir/gate.txt"
started_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
{
  print "gate=bukov_process_crash_recovery"
  print "source_commit=$(git -C "$project_root" rev-parse HEAD)"
  print "worktree_state=$([[ -n "$(git -C "$project_root" status --porcelain)" ]] && print dirty || print clean)"
  print "started_utc=$started_utc"
  print "evidence_dir=$evidence_dir"
  print "storage_scope=isolated_test_directories_only"
  print "real_user_saves_touched=false"
} | tee "$metadata"

set +e
BUKOV_CRASH_EVIDENCE_DIR="$evidence_dir" \
  "$script_dir/apple-gradle" \
  core:test \
  --tests \
  'com.shatteredpixel.shatteredpixeldungeon.bukov.save.BukovProcessCrashRecoveryTest' \
  --rerun-tasks \
  --no-daemon 2>&1 | tee "$evidence_dir/test.log"
gate_exit=${pipestatus[1]}
set -e

{
  print "finished_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "exit_code=$gate_exit"
} | tee -a "$metadata"

if (( gate_exit == 0 )); then
  [[ -f "$evidence_dir/summary.json" ]] || {
    print -u2 "missing process-crash JSON evidence"
    exit 1
  }
  [[ -f "$evidence_dir/summary.txt" ]] || {
    print -u2 "missing process-crash text evidence"
    exit 1
  }
  python3 - "$evidence_dir/summary.json" <<'PY'
import json
import pathlib
import sys

summary = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
assert summary["status"] == "passed"
assert summary["processesHalted"] == 4
assert summary["validRecoveryStates"] is True
assert summary["duplicateSettlement"] is False
assert summary["duplicateUid"] is False
assert len(summary["cases"]) == 4
PY
fi

print "evidence=$evidence_dir"
exit "$gate_exit"
