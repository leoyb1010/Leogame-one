#!/bin/zsh
set -euo pipefail

script_path="${0:A}"
script_dir="${script_path:h}"
project_root="${script_dir:h}"
report_root="${project_root}/build/reports/bukov-mode-theme-boss-matrix"
apple_build_root="${APPLE_BUILD_ROOT:-$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle}"
source_commit="$(git -C "$project_root" rev-parse HEAD)"
started_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
started_epoch="$(date '+%s')"
run_stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
allow_dirty=false
output_arg=""

usage() {
  cat <<'USAGE'
Usage: scripts/bukov_mode_theme_boss_matrix_gate.sh [options]

Runs the production host matrix: 4 economic modes x 6 themes plus the fixed
cold-storage training ground (25 effective host combinations).

Options:
  --output DIR    New absolute evidence directory to create.
  --allow-dirty   Development-only run; evidence is marked unsealed.
  -h, --help      Show this help.
USAGE
}

while (( $# > 0 )); do
  case "$1" in
    --output)
      (( $# >= 2 )) || {
        print -u2 -- "FAIL: --output requires a directory"
        exit 2
      }
      output_arg="$2"
      shift 2
      ;;
    --allow-dirty)
      allow_dirty=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      print -u2 -- "FAIL: unknown option: $1"
      usage >&2
      exit 2
      ;;
  esac
done

worktree_state="clean"
if [[ -n "$(git -C "$project_root" status --porcelain --untracked-files=all)" ]]; then
  worktree_state="dirty"
fi
if [[ "$worktree_state" == dirty && "$allow_dirty" != true ]]; then
  print -u2 -- "FAIL: matrix release evidence requires a clean worktree"
  print -u2 -- "Use --allow-dirty only for an unsealed development run."
  exit 2
fi

mkdir -p "$report_root"
if [[ -n "$output_arg" ]]; then
  [[ "$output_arg" == /* ]] || {
    print -u2 -- "FAIL: --output must be absolute"
    exit 2
  }
  evidence_dir="${output_arg:A}"
  [[ ! -e "$evidence_dir" && -d "${evidence_dir:h}" ]] || {
    print -u2 -- "FAIL: output must not exist and its parent must exist"
    exit 2
  }
  mkdir "$evidence_dir"
else
  evidence_dir="$(mktemp -d \
    "${report_root}/${run_stamp}-${source_commit[1,12]}.XXXXXX")"
fi
gradle_log="${evidence_dir}/gradle.log"

print -r -- "Running 25-combination production host acceptance matrix"
set +e
"$script_dir/apple-gradle" \
  core:test \
  --tests '*BukovModeThemeBossAcceptanceMatrixTest' \
  --rerun-tasks \
  --no-daemon \
  2>&1 | tee "$gradle_log"
pipeline_status=("${pipestatus[@]}")
set -e
gradle_status=${pipeline_status[1]}
tee_status=${pipeline_status[2]}

if (( gradle_status != 0 )); then
  print -u2 -r -- "FAIL: matrix test failed; evidence: $evidence_dir"
  exit "$gradle_status"
fi
if (( tee_status != 0 )); then
  print -u2 -r -- "FAIL: matrix log capture failed; evidence: $evidence_dir"
  exit "$tee_status"
fi

xml_file="${apple_build_root:A}/core/test-results/test/TEST-com.shatteredpixel.shatteredpixeldungeon.bukov.acceptance.BukovModeThemeBossAcceptanceMatrixTest.xml"
[[ -f "$xml_file" ]] || {
  print -u2 -r -- "FAIL: JUnit XML was not produced: $xml_file"
  exit 1
}

cp "$xml_file" "$evidence_dir/junit.xml"
finished_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
duration_seconds="$(( $(date '+%s') - started_epoch ))"

python3 - \
  "$evidence_dir/junit.xml" \
  "$evidence_dir" \
  "$source_commit" \
  "$started_utc" \
  "$finished_utc" \
  "$duration_seconds" \
  "$worktree_state" <<'PY'
import json
import hashlib
import pathlib
import sys
import xml.etree.ElementTree as ET

xml_path = pathlib.Path(sys.argv[1])
evidence_dir = pathlib.Path(sys.argv[2])
source_commit = sys.argv[3]
started_utc = sys.argv[4]
finished_utc = sys.argv[5]
duration_seconds = int(sys.argv[6])
worktree_state = sys.argv[7]

root = ET.parse(xml_path).getroot()
failures = int(root.attrib.get("failures", "0"))
errors = int(root.attrib.get("errors", "0"))
tests = int(root.attrib.get("tests", "0"))
if failures or errors:
    raise SystemExit(
        f"JUnit reported failures={failures} errors={errors}")
if tests != 25:
    raise SystemExit(f"expected 25 matrix tests, found {tests}")

rows = []
for system_out in root.findall(".//system-out"):
    for line in (system_out.text or "").splitlines():
        if not line.startswith("BUKOV_MATRIX\t"):
            continue
        fields = {}
        for field in line.split("\t")[1:]:
            key, separator, value = field.partition("=")
            if not separator or not key or key in fields:
                raise SystemExit(f"malformed matrix field: {field!r}")
            fields[key] = value
        rows.append(fields)

required = {
    "mode", "theme", "map_theme", "seed", "map", "rooms", "fingerprint",
    "extractions", "boss_policy", "boss_model", "win", "win_qty",
    "loss", "loss_qty", "receipts",
}
modes = {
    "EXPEDITION": (True, 26, 34),
    "QUICK_SWEEP": (False, 18, 24),
    "SCAVENGER": (False, 22, 29),
    "BOSS_CONTRACT": (True, 28, 37),
    "TRAINING_GROUND": (False, 16, 20),
}
themes = {
    "fog_depot",
    "rust_workshop",
    "flooded_passage",
    "overgrown_yard",
    "cold_storage",
    "sealed_lab",
}

if len(rows) != 25:
    raise SystemExit(f"expected 25 evidence rows, found {len(rows)}")

seen = set()
for row in rows:
    if set(row) != required:
        missing = sorted(required - set(row))
        extra = sorted(set(row) - required)
        raise SystemExit(
            f"invalid evidence schema missing={missing} extra={extra}")
    key = (row["mode"], row["theme"])
    if key in seen:
        raise SystemExit(f"duplicate evidence row: {key}")
    seen.add(key)
    if row["mode"] not in modes or row["theme"] not in themes:
        raise SystemExit(f"unknown mode/theme: {key}")
    boss_enabled, minimum_rooms, maximum_rooms = modes[row["mode"]]
    if (row["map"] != "VALID"
            or not minimum_rooms <= int(row["rooms"]) <= maximum_rooms):
        raise SystemExit(f"invalid map evidence: {key}")
    if row["map_theme"] != row["theme"]:
        raise SystemExit(f"wrong effective map theme: {key}")
    if row["extractions"] != "3":
        raise SystemExit(f"production extraction wiring mismatch: {key}")
    if len(row["fingerprint"]) != 24:
        raise SystemExit(f"invalid map fingerprint: {key}")
    if row["win"] != "SUCCESS" or row["loss"] != "DEATH":
        raise SystemExit(f"incomplete outcome loop: {key}")
    if row["receipts"] != "2":
        raise SystemExit(f"incomplete settlement receipts: {key}")
    if row["mode"] == "TRAINING_GROUND":
        if row["win_qty"] != "0" or row["loss_qty"] != "0":
            raise SystemExit(f"training leaked economy state: {key}")
    elif row["win_qty"] != "1" or row["loss_qty"] != "1":
        raise SystemExit(f"economic settlement mismatch: {key}")

    if boss_enabled:
        if (row["boss_policy"], row["boss_model"]) != (
                "ELIGIBLE", "DEFEATED"):
            raise SystemExit(f"boss-enabled mode failed: {key}")
    elif (row["boss_policy"], row["boss_model"]) != (
            "SUPPRESSED", "NOT_APPLICABLE"):
        raise SystemExit(f"boss-disabled mode leaked boss: {key}")

economic_modes = {
    mode for mode in modes if mode != "TRAINING_GROUND"
}
expected = {
    (mode, theme)
    for mode in economic_modes
    for theme in themes
}
expected.add(("TRAINING_GROUND", "cold_storage"))
if seen != expected:
    raise SystemExit(
        f"matrix coverage mismatch missing={sorted(expected-seen)}")

columns = [
    "mode", "theme", "map_theme", "seed", "map", "rooms", "fingerprint",
    "extractions", "boss_policy", "boss_model", "win", "win_qty",
    "loss", "loss_qty", "receipts",
]
with (evidence_dir / "matrix.tsv").open("w", encoding="utf-8") as handle:
    handle.write("\t".join(columns) + "\n")
    for row in sorted(rows, key=lambda item: (
            list(modes).index(item["mode"]), item["theme"])):
        handle.write("\t".join(row[column] for column in columns) + "\n")

def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()

summary = {
    "gate": "bukov_mode_theme_boss_matrix",
    "status": (
        "passed" if worktree_state == "clean"
        else "passed_unsealed_dirty_worktree"
    ),
    "source_commit": source_commit,
    "source_worktree": worktree_state,
    "started_utc": started_utc,
    "finished_utc": finished_utc,
    "duration_seconds": duration_seconds,
    "junit_tests": tests,
    "effective_host_combinations": len(rows),
    "economic_mode_theme_rows": sum(
        row["mode"] != "TRAINING_GROUND" for row in rows),
    "training_rows": sum(
        row["mode"] == "TRAINING_GROUND" for row in rows),
    "mode_count": len(modes),
    "theme_count": len(themes),
    "training_theme_policy": "fixed_cold_storage",
    "boss_enabled_rows": sum(
        row["boss_policy"] == "ELIGIBLE" for row in rows),
    "success_settlements": sum(
        row["win"] == "SUCCESS" for row in rows),
    "death_settlements": sum(
        row["loss"] == "DEATH" for row in rows),
    "artifacts": {
        "junit.xml": sha256(evidence_dir / "junit.xml"),
        "gradle.log": sha256(evidence_dir / "gradle.log"),
        "matrix.tsv": sha256(evidence_dir / "matrix.tsv"),
    },
}
temporary_summary = evidence_dir / ".summary.json.tmp"
temporary_summary.write_text(
    json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
temporary_summary.replace(evidence_dir / "summary.json")
PY

print -r -- "PASS: 25/25 effective host combinations (24 economic mode-theme + fixed training)"
print -r -- "Evidence: $evidence_dir"
