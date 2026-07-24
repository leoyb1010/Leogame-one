#!/bin/zsh
set -euo pipefail

script_path="${0:A}"
script_dir="${script_path:h}"
project_root="${script_dir:h}"
default_reports_root="${project_root}/build/reports/bukov-final-gate"
repository_lock="${project_root}/build/reports/.bukov-final-gate.lock"
apple_cache_root="$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle"

apply=false
mode_seen=""
self_test=false
output_arg=""
ios_device="Codex Test iPhone 17 Pro"
render_frame_logs=()

evidence_dir=""
steps_file=""
environment_file=""
source_commit=""
source_branch=""
started_utc=""
lock_held=false
evidence_created=false
planned_step_count=33

usage() {
  cat <<'USAGE'
Usage:
  scripts/bukov_final_gate.sh [options]

Runs the serial, fail-fast final evidence gate for Escape from Bukov.
The default is a non-mutating dry run. Nothing is executed until --apply is
given.

On --apply the script requires a clean Git worktree, acquires one repository-
wide lock, records the exact commit and host environment, and runs:

  static source/asset/legal gates
  clean Core, Desktop, and iOS test suites
  RoboVM API compatibility gate
  10,000-seed first-raid sweep
  100-iteration real disk save stress
  1,800-second performance smoke and E2E gates
  captured macOS and iOS render-callback frame-pacing evidence
  macOS jpackage image and iOS Simulator app builds
  packaged legal check and final source-integrity check

Every step has an independent immutable log. A machine-readable summary.json
is written on both success and gate failure. Commands run strictly in the
listed order; the first failure stops the run.

Options:
  --dry-run          Print the exact plan without executing it (default).
  --apply            Execute the final gate.
  --output DIR       New absolute evidence directory to create. It must not
                     exist. By default a SHA/time directory is created below
                     build/reports/bukov-final-gate/.
  --ios-device NAME  iOS Simulator device used by the build step
                     (default: Codex Test iPhone 17 Pro).
  --render-frame-log FILE
                     Existing absolute packaged-app log containing one
                     uninterrupted bukov-render-frame-v3 live gameplay scene.
                     Repeat for macOS and iOS. Required by --apply. Include
                     source_commit=<full SHA> and platform=macOS or platform=iOS.
  --self-test        Run syntax, argument, lock, and path-safety checks only.
  -h, --help         Show this help.

Render telemetry is CPU render-callback frame pacing, not a hardware GPU
counter or a replacement for Instruments/Metal evidence. The script never
deletes or replaces an evidence directory. Concurrent runs are rejected.
USAGE
}

fail() {
  print -u2 -r -- "ERROR: $*"
  exit 1
}

contains_control_character() {
  local value="$1"
  [[ "$value" == *$'\n'* || "$value" == *$'\r'* || "$value" == *$'\t'* ]]
}

safe_explicit_target() {
  local candidate="$1"
  [[ -n "$candidate" && "$candidate" == /* ]] || return 1
  contains_control_character "$candidate" && return 1

  candidate="${candidate:A}"
  local parent="${candidate:h}"
  [[ -d "$parent" && ! -L "$parent" ]] || return 1
  [[ ! -e "$candidate" && ! -L "$candidate" ]] || return 1
  [[ "$candidate" != "/" ]] || return 1
  [[ "$candidate" != "${HOME:A}" ]] || return 1
  [[ "$candidate" != "${project_root:A}" ]] || return 1
  [[ "$candidate" != "${project_root:A}/.git" ]] || return 1
  [[ "$candidate" != "${project_root:A}/.git/"* ]] || return 1

  # Inside the repository, final-gate evidence belongs only under the ignored
  # build/reports tree. An explicit target elsewhere must be outside the repo.
  if [[ "$candidate" == "${project_root:A}/"* ]]; then
    [[ "$candidate" == "${default_reports_root:A}/"* ]] || return 1
  fi
}

target_is_available() {
  local candidate="$1"
  [[ ! -e "$candidate" && ! -L "$candidate" ]]
}

quote_command() {
  local rendered=""
  local argument
  for argument in "$@"; do
    [[ -z "$rendered" ]] || rendered+=" "
    rendered+="${(q)argument}"
  done
  print -r -- "$rendered"
}

run_self_test() {
  /bin/zsh -n "$script_path" || fail "zsh syntax check failed"

  if "$script_path" --dry-run --apply >/dev/null 2>&1; then
    fail "mutually exclusive execution modes were accepted"
  fi
  if "$script_path" --self-test --apply >/dev/null 2>&1; then
    fail "--self-test accepted execution options"
  fi
  if "$script_path" --output >/dev/null 2>&1; then
    fail "missing --output value was accepted"
  fi
  if "$script_path" --render-frame-log >/dev/null 2>&1; then
    fail "missing --render-frame-log value was accepted"
  fi
  if "$script_path" --not-a-real-option >/dev/null 2>&1; then
    fail "unknown option was accepted"
  fi

  local self_root
  self_root="$(mktemp -d "${TMPDIR:-/tmp}/bukov-final-gate-selftest.XXXXXX")"
  local safe_target="${self_root}/evidence"

  safe_explicit_target "$safe_target" \
    || fail "safe absolute evidence target was rejected"
  ! safe_explicit_target "relative/evidence" \
    || fail "relative evidence target was accepted"
  ! safe_explicit_target "/" \
    || fail "filesystem root was accepted"
  ! safe_explicit_target "$HOME" \
    || fail "home directory was accepted"
  ! safe_explicit_target "$project_root" \
    || fail "repository root was accepted"
  ! safe_explicit_target "$project_root/.git/evidence" \
    || fail ".git evidence target was accepted"
  ! safe_explicit_target "$project_root/evidence" \
    || fail "non-report repository target was accepted"
  ! safe_explicit_target $'/tmp/evidence\nescape' \
    || fail "control characters were accepted in a path"

  target_is_available "$safe_target" \
    || fail "unused target was reported as occupied"
  mkdir "$safe_target"
  ! target_is_available "$safe_target" \
    || fail "existing evidence directory was reported as available"
  ! safe_explicit_target "$safe_target" \
    || fail "existing evidence directory was accepted"

  local self_lock="${self_root}/lock"
  mkdir "$self_lock"
  if mkdir "$self_lock" 2>/dev/null; then
    fail "a concurrent lock acquisition unexpectedly succeeded"
  fi

  [[ "$(quote_command /bin/echo "two words")" == \
      "/bin/echo two\\ words" ]] \
    || fail "command rendering lost an argument boundary"
  ! contains_control_character "ordinary text" \
    || fail "ordinary text was reported as containing controls"
  contains_control_character $'bad\ttext' \
    || fail "tab control character was not detected"

  rmdir "$self_lock"
  rmdir "$safe_target"
  rmdir "$self_root"
  print "PASS: final-gate syntax, argument, lock, and path-safety self-tests"
}

write_environment() {
  {
    print -r -- "gate=bukov_final_gate"
    print -r -- "source_commit=$source_commit"
    print -r -- "source_branch=$source_branch"
    print -r -- "started_utc=$started_utc"
    print -r -- "timezone=$(date '+%Z %z')"
    print -r -- "host=$(hostname)"
    print -r -- "kernel=$(uname -srm)"
    print -r -- "architecture=$(uname -m)"
    print -r -- "shell=zsh $ZSH_VERSION"
    print -r -- "ios_simulator_device=$ios_device"
    print -r -- "apple_build_root=${apple_cache_root:A}"
    print -r -- "gradle_wrapper_sha256=$(shasum -a 256 "$project_root/gradle/wrapper/gradle-wrapper.jar" | awk '{print $1}')"
    if (( $+commands[sw_vers] )); then
      print -r -- "macos_product=$(sw_vers -productName) $(sw_vers -productVersion) ($(sw_vers -buildVersion))"
    else
      print -r -- "macos_product=unavailable"
    fi
    if (( $+commands[xcodebuild] )); then
      print -r -- "xcode=$(xcodebuild -version 2>&1 | tr '\n' ';' | sed 's/;$//')"
    else
      print -r -- "xcode=unavailable"
    fi
    if [[ -x /opt/homebrew/bin/brew ]]; then
      local jdk_prefix
      jdk_prefix="$(/opt/homebrew/bin/brew --prefix openjdk@17 2>/dev/null || true)"
      if [[ -n "$jdk_prefix" && -x "$jdk_prefix/bin/java" ]]; then
        print -r -- "java=$("$jdk_prefix/bin/java" -version 2>&1 | head -1)"
      else
        print -r -- "java=unavailable"
      fi
    else
      print -r -- "java=unavailable"
    fi
    print -r -- "python=$(python3 --version 2>&1)"
    if (( $+commands[node] )); then
      print -r -- "node=$(node --version 2>&1)"
    else
      print -r -- "node=unavailable"
    fi
  } > "$environment_file"
}

run_step() {
  local step_id="$1"
  local label="$2"
  shift 2
  local command_text
  command_text="$(quote_command "$@")"

  if [[ "$apply" != true ]]; then
    print -r -- "[DRY-RUN] ${step_id}: ${label}"
    print -r -- "          ${command_text}"
    return 0
  fi

  local log_file="${evidence_dir}/${step_id}.log"
  target_is_available "$log_file" \
    || fail "step log already exists; refusing overwrite: $log_file"

  local step_started
  step_started="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  {
    print -r -- "gate=bukov_final_gate"
    print -r -- "step_id=$step_id"
    print -r -- "label=$label"
    print -r -- "source_commit=$source_commit"
    print -r -- "started_utc=$step_started"
    print -r -- "command=$command_text"
    print -r -- "--- output ---"
  } > "$log_file"

  print -r -- "==> ${step_id}: ${label}"
  set +e
  "$@" 2>&1 | tee -a "$log_file"
  local step_exit=${pipestatus[1]}
  set -e

  local step_finished
  step_finished="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  {
    print -r -- "--- result ---"
    print -r -- "finished_utc=$step_finished"
    print -r -- "exit_code=$step_exit"
  } >> "$log_file"

  local step_status="passed"
  if (( step_exit != 0 )); then
    step_status="failed"
  fi
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$step_id" \
    "$label" \
    "$step_status" \
    "$step_exit" \
    "$step_started" \
    "$step_finished" \
    "${log_file:t}" \
    "$command_text" \
    >> "$steps_file"

  if (( step_exit != 0 )); then
    print -u2 -r -- "FAILED: ${step_id}; see $log_file"
    return "$step_exit"
  fi
}

execute_sequence() {
  local gradle="$script_dir/apple-gradle"
  local mac_app="${apple_cache_root:A}/desktop/jpackage/逃离布科夫.app"
  local ios_app="${apple_cache_root:A}/ios/robovm.tmp/IOSLauncher.app"
  local -a render_frame_gate_command=(
    python3 "$script_dir/bukov_render_frame_gate.py"
    --output "$evidence_dir/render-frame-summary.json"
    --expected-source-commit "$source_commit"
    --require-platform macOS
    --require-platform iOS
    --minimum-duration-seconds 1800
    --max-p95-ms 18.4
    --max-p99-ms 33.3
    --max-over-budget-ratio 0.05
    --max-over-33-3-ratio 0.01
    --high-refresh-min-hz 120
    --high-refresh-max-p95-ms 10
  )
  if (( ${#render_frame_logs[@]} == 0 )); then
    render_frame_gate_command+=(
      --input "<required-macOS-render-log>"
      --input "<required-iOS-render-log>"
    )
  else
    local render_log
    for render_log in "${render_frame_logs[@]}"; do
      render_frame_gate_command+=(--input "$render_log")
    done
  fi

  run_step "01-static-diff-check" \
    "Git whitespace/error diff check" \
    git -C "$project_root" diff --check
  run_step "02-static-release" \
    "Release metadata, branding, provenance, and manifest validation" \
    python3 "$script_dir/validate_release.py"
  run_step "03-static-localization" \
    "English and Simplified Chinese resource and player-path gate" \
    /bin/bash "$script_dir/bukov_localization_gate.sh"
  run_step "04-static-content-scale" \
    "Authored content scale and obtainability gate" \
    python3 "$script_dir/bukov_content_scale_gate.py"
  run_step "05-static-audio" \
    "Bukov audio assets and runtime wiring gate" \
    /bin/bash "$script_dir/bukov_audio_gate.sh"
  run_step "06-static-audio-model" \
    "Standalone audio model gate" \
    /bin/bash "$script_dir/bukov_audio_model_test.sh"
  run_step "07-static-enemy-sprites" \
    "Enemy sprite generation and wiring gate" \
    /bin/bash "$script_dir/bukov_enemy_sprite_gate.sh"
  run_step "08-static-item-atlas" \
    "Item and interaction atlas gate" \
    /bin/bash "$script_dir/bukov_item_atlas_gate.sh"
  run_step "09-static-ui-tokens" \
    "UI token boundary and authored interaction contract gate" \
    /bin/zsh "$script_dir/bukov_ui_tokens_check.sh"
  run_step "10-static-ui-assets" \
    "Project-owned UI atlas generation and provenance gate" \
    /bin/bash "$script_dir/bukov_ui_asset_gate.sh"
  run_step "11-static-legal-bundle" \
    "Repository legal bundle gate" \
    /bin/sh "$script_dir/bukov_legal_bundle_gate.sh"
  run_step "12-static-loot" \
    "Loot discoverability gate" \
    /bin/bash "$script_dir/bukov_loot_discoverability_gate.sh"
  run_step "13-static-map" \
    "Map scale, traversal, fog, and camera source gate" \
    /bin/bash "$script_dir/bukov_map_visibility_gate.sh"
  run_step "14-static-original-visuals" \
    "Original operator and landmark visual gate" \
    /bin/bash "$script_dir/bukov_original_visual_gate.sh"
  run_step "15-static-theme-visuals" \
    "Six-theme visual gate" \
    /bin/bash "$script_dir/bukov_theme_visual_gate.sh"
  run_step "16-static-camera" \
    "Standalone realtime camera and world-bounds gate" \
    /bin/bash "$script_dir/bukov_realtime_camera_test.sh"
  run_step "17-render-frame-pacing" \
    "Real packaged-app CPU render-callback frame-pacing gate" \
    "${render_frame_gate_command[@]}"

  run_step "18-gradle-clean" \
    "Clean Core, Desktop, and iOS build outputs" \
    "$gradle" :core:clean :desktop:clean :ios:clean --no-daemon
  run_step "19-test-core" \
    "Complete Core test suite" \
    "$gradle" :core:test --rerun-tasks --no-daemon
  run_step "20-five-mode-lifecycle" \
    "All five selectable modes share the complete raid lifecycle" \
    /bin/zsh "$script_dir/bukov_five_mode_lifecycle_gate.sh"
  run_step "21-mode-theme-boss-matrix" \
    "24 economic mode-theme hosts plus fixed training close map, Boss, extraction, and settlement contracts" \
    /bin/zsh "$script_dir/bukov_mode_theme_boss_matrix_gate.sh" \
      --output "$evidence_dir/mode-theme-boss-matrix"
  run_step "22-test-desktop" \
    "Complete Desktop test suite" \
    "$gradle" :desktop:test --rerun-tasks --no-daemon
  run_step "23-test-ios" \
    "Complete iOS test suite" \
    "$gradle" :ios:test --rerun-tasks --no-daemon
  run_step "24-robovm-api" \
    "RoboVM AOT API compatibility gate" \
    python3 "$script_dir/bukov_robovm_api_gate.py"

  run_step "25-seed-10000" \
    "10,000-seed synthetic and real first-raid critical-path sweep" \
    "$script_dir/bukov_seed_sweep.sh" 10000
  run_step "26-save-100" \
    "100-iteration in-memory and real-disk save stress gate" \
    "$script_dir/bukov_save_stress.sh" 100
  run_step "27-performance-smoke-1800" \
    "1,800-second fixed-step performance smoke gate" \
    "$script_dir/bukov_performance_smoke.sh" 1800
  run_step "28-performance-e2e-1800" \
    "1,800-second 30-enemy/200-projectile E2E CPU gate" \
    "$script_dir/bukov_performance_e2e.sh" 1800

  run_step "29-build-macos" \
    "Build the macOS jpackage application image" \
    "$gradle" :desktop:jpackageImage --rerun-tasks --no-daemon
  run_step "30-build-ios-simulator" \
    "Build and launch the iOS Simulator application" \
    "$gradle" :ios:launchIPhoneSimulator \
      "-Probovm.device.name=$ios_device" --rerun-tasks --no-daemon
  run_step "31-packaged-legal" \
    "Verify legal payloads in both built application bundles" \
    /bin/sh "$script_dir/bukov_packaged_legal_gate.sh" "$mac_app" "$ios_app"
  run_step "32-packaged-provenance" \
    "Prove both Apple bundles are clean builds of the sealed source commit" \
    "$script_dir/bukov_package_personal_build.sh" \
      --output "$evidence_dir" \
      --version "gate-${source_commit[1,12]}" \
      --dry-run
  run_step "33-source-integrity" \
    "Verify final HEAD and clean worktree still match the sealed source" \
    /bin/zsh -c \
      '[[ "$(git -C "$1" rev-parse HEAD)" == "$2" ]] && [[ -z "$(git -C "$1" status --porcelain --untracked-files=all)" ]]' \
      bukov-source-integrity "$project_root" "$source_commit"
}

generate_summary() {
  local run_exit="$1"
  local finished_utc
  finished_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  local summary_file="${evidence_dir}/summary.json"
  local temporary_summary="${evidence_dir}/.summary.json.tmp"
  target_is_available "$summary_file" \
    || return 1
  target_is_available "$temporary_summary" \
    || return 1

  python3 - \
    "$steps_file" \
    "$temporary_summary" \
    "$run_exit" \
    "$source_commit" \
    "$source_branch" \
    "$started_utc" \
    "$finished_utc" \
    "$planned_step_count" \
    "$environment_file" \
    "$evidence_dir" <<'PY'
import csv
import hashlib
import json
import sys
from datetime import datetime
from pathlib import Path

(
    steps_path,
    output_path,
    exit_code,
    source_commit,
    source_branch,
    started_utc,
    finished_utc,
    planned_step_count,
    environment_path,
    evidence_dir,
) = sys.argv[1:]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


steps = []
with Path(steps_path).open(encoding="utf-8", newline="") as handle:
    for row in csv.DictReader(handle, delimiter="\t"):
        log_path = Path(evidence_dir) / row["log"]
        steps.append(
            {
                "id": row["id"],
                "label": row["label"],
                "status": row["status"],
                "exitCode": int(row["exit_code"]),
                "startedUtc": row["started_utc"],
                "finishedUtc": row["finished_utc"],
                "log": row["log"],
                "logSha256": sha256(log_path),
                "command": row["command"],
            }
        )

exit_code = int(exit_code)
planned_step_count = int(planned_step_count)
all_passed = (
    exit_code == 0
    and len(steps) == planned_step_count
    and all(step["status"] == "passed" for step in steps)
)
start = datetime.fromisoformat(started_utc.replace("Z", "+00:00"))
finish = datetime.fromisoformat(finished_utc.replace("Z", "+00:00"))
environment = Path(environment_path)

payload = {
    "schemaVersion": 1,
    "gate": "bukov_final_gate",
    "status": "passed" if all_passed else "failed",
    "exitCode": exit_code if not (exit_code == 0 and not all_passed) else 1,
    "sourceCommit": source_commit,
    "sourceBranch": source_branch,
    "startedUtc": started_utc,
    "finishedUtc": finished_utc,
    "durationSeconds": int((finish - start).total_seconds()),
    "serialExecution": True,
    "parameters": {
        "seedCount": 10000,
        "saveIterations": 100,
        "performanceSmokeSeconds": 1800,
        "performanceE2eSeconds": 1800,
    },
    "plannedStepCount": planned_step_count,
    "completedStepCount": len(steps),
    "environment": environment.name,
    "environmentSha256": sha256(environment),
    "steps": steps,
}
Path(output_path).write_text(
    json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
PY

  /bin/mv "$temporary_summary" "$summary_file"
}

release_lock() {
  if [[ "$lock_held" == true ]]; then
    if [[ -d "$repository_lock" && ! -L "$repository_lock" ]]; then
      rmdir "$repository_lock" \
        || print -u2 -r -- "WARNING: could not release final-gate lock: $repository_lock"
    fi
    lock_held=false
  fi
}

on_exit() {
  local original_exit=$?
  trap - EXIT
  local final_exit=$original_exit

  if [[ "$evidence_created" == true ]]; then
    if ! generate_summary "$original_exit"; then
      print -u2 -r -- "ERROR: could not write machine-readable final-gate summary"
      final_exit=1
    elif (( original_exit == 0 )); then
      print -r -- "PASS: final gate evidence: $evidence_dir"
    else
      print -u2 -r -- "Final gate stopped at first failure. Evidence: $evidence_dir"
    fi
  fi

  release_lock
  exit "$final_exit"
}

while (( $# > 0 )); do
  case "$1" in
    --dry-run)
      [[ -z "$mode_seen" ]] \
        || fail "--dry-run and --apply are mutually exclusive"
      mode_seen="dry-run"
      apply=false
      shift
      ;;
    --apply)
      [[ -z "$mode_seen" ]] \
        || fail "--dry-run and --apply are mutually exclusive"
      mode_seen="apply"
      apply=true
      shift
      ;;
    --output)
      (( $# >= 2 )) || fail "--output requires a value"
      output_arg="$2"
      shift 2
      ;;
    --ios-device)
      (( $# >= 2 )) || fail "--ios-device requires a value"
      ios_device="$2"
      shift 2
      ;;
    --render-frame-log)
      (( $# >= 2 )) || fail "--render-frame-log requires a value"
      render_frame_logs+=("$2")
      shift 2
      ;;
    --self-test)
      self_test=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

if [[ "$self_test" == true ]]; then
  [[ -z "$mode_seen" && -z "$output_arg" \
      && ${#render_frame_logs[@]} -eq 0 \
      && "$ios_device" == "Codex Test iPhone 17 Pro" ]] \
    || fail "--self-test cannot be combined with execution options"
  run_self_test
  exit 0
fi

[[ -n "$ios_device" ]] || fail "--ios-device cannot be empty"
contains_control_character "$ios_device" \
  && fail "--ios-device cannot contain tabs or newlines"
for render_log in "${render_frame_logs[@]}"; do
  [[ -n "$render_log" && "$render_log" == /* ]] \
    || fail "--render-frame-log must be an absolute file"
  contains_control_character "$render_log" \
    && fail "--render-frame-log cannot contain tabs or newlines"
  [[ -f "$render_log" ]] \
    || fail "--render-frame-log is not a file: $render_log"
done

source_commit="$(git -C "$project_root" rev-parse HEAD)"
source_branch="$(git -C "$project_root" symbolic-ref --quiet --short HEAD \
  || print "DETACHED")"
started_utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

if [[ -n "$output_arg" ]]; then
  safe_explicit_target "$output_arg" \
    || fail "--output must be a new safe absolute directory"
  evidence_dir="${output_arg:A}"
else
  run_stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
  evidence_dir="${default_reports_root}/${source_commit[1,12]}-${run_stamp}"
fi

target_is_available "$evidence_dir" \
  || fail "evidence directory already exists; refusing overwrite: $evidence_dir"

if [[ "$apply" != true ]]; then
  worktree_state="clean"
  [[ -z "$(git -C "$project_root" status --porcelain --untracked-files=all)" ]] \
    || worktree_state="dirty (an --apply run would be rejected)"
  print -r -- "Bukov final gate dry run"
  print -r -- "source_commit=$source_commit"
  print -r -- "source_branch=$source_branch"
  print -r -- "worktree_state=$worktree_state"
  print -r -- "evidence_dir=$evidence_dir"
  print -r -- "ios_simulator_device=$ios_device"
  print -r -- "render_frame_log_count=${#render_frame_logs[@]}"
  execute_sequence
  print -r -- "Dry run only: no command was executed and no evidence was written."
  exit 0
fi

(( ${#render_frame_logs[@]} >= 2 )) \
  || fail "--apply requires separate macOS and iOS --render-frame-log inputs"

mkdir -p "${repository_lock:h}"
if ! mkdir "$repository_lock" 2>/dev/null; then
  fail "another final-gate run is active or left a lock: $repository_lock"
fi
lock_held=true
trap on_exit EXIT

[[ -z "$(git -C "$project_root" status --porcelain --untracked-files=all)" ]] \
  || fail "--apply requires a completely clean Git worktree"
[[ "$(git -C "$project_root" rev-parse HEAD)" == "$source_commit" ]] \
  || fail "HEAD changed while acquiring the final-gate lock"
target_is_available "$evidence_dir" \
  || fail "evidence directory appeared concurrently; refusing overwrite"

if [[ -z "$output_arg" ]]; then
  mkdir -p "$default_reports_root"
fi
mkdir "$evidence_dir"
evidence_created=true
steps_file="${evidence_dir}/steps.tsv"
environment_file="${evidence_dir}/environment.txt"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "id" \
  "label" \
  "status" \
  "exit_code" \
  "started_utc" \
  "finished_utc" \
  "log" \
  "command" \
  > "$steps_file"
write_environment

execute_sequence
