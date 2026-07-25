#!/bin/zsh
set -euo pipefail

# Captures one uninterrupted packaged-app render-callback frame log for the
# final gate's step 17, which requires macOS and iOS records of >=1800 seconds
# each.
#
# BukovFrameTelemetry only emits while a raid is running and the hero is alive
# and unpaused, so the operator must enter a raid once; this script then runs
# unattended and does every mechanical part: launching the packaged build,
# teeing stdout, enforcing the duration, and validating the result through the
# real gate. It deliberately does not automate entering the raid - a hook that
# auto-plays would change what this evidence means.
#
# Usage:
#   scripts/bukov_capture_render_log.sh macOS /path/to/逃离布科夫.app [seconds]
#   scripts/bukov_capture_render_log.sh iOS   <simulator-udid> <bundle-id> [seconds]

script_dir=${0:A:h}
project_root=${script_dir:h}
self=${0:A}
platform=${1:-}
seconds_default=1830

usage() {
  print -u2 "usage:"
  print -u2 "  $self macOS <app-bundle-path> [seconds]"
  print -u2 "  $self iOS <simulator-udid> <bundle-id> [seconds]"
  exit 2
}

[[ -n "$platform" ]] || usage

source_commit="$(git -C "$project_root" rev-parse HEAD)"
if [[ -n "$(git -C "$project_root" status --porcelain --untracked-files=all)" ]]; then
  print -u2 "worktree is dirty; render evidence must come from a clean commit"
  exit 1
fi

output_dir="$project_root/build/reports/bukov-render-capture"
mkdir -p "$output_dir"
log="$output_dir/${platform}-render-${source_commit:0:12}.log"

print "platform=$platform"
print "source_commit=$source_commit"
print "output=$log"

capture_macos() {
  local app=${1:-}
  local seconds=${2:-$seconds_default}
  [[ -d "$app" ]] || { print -u2 "app bundle not found: $app"; exit 2; }
  local binary
  binary="$app/Contents/MacOS/$(ls "$app/Contents/MacOS" | head -1)"
  [[ -x "$binary" ]] || { print -u2 "no executable in $app"; exit 2; }

  {
    print "source_commit=$source_commit"
    print "platform=macOS"
  } > "$log"

  print ""
  print "Launching the packaged app. Enter a raid within the first minute and"
  print "then leave it running - do not pause, background or resize the window."
  print "Capturing ${seconds}s..."
  print ""

  "$binary" >> "$log" 2>&1 &
  local pid=$!
  local deadline=$((SECONDS + seconds))
  while (( SECONDS < deadline )); do
    if ! kill -0 "$pid" 2>/dev/null; then
      print -u2 "app exited after $SECONDS s, before the capture window closed"
      exit 1
    fi
    sleep 5
  done
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

capture_ios() {
  local udid=${1:-}
  local bundle=${2:-}
  local seconds=${3:-$seconds_default}
  [[ -n "$udid" && -n "$bundle" ]] || usage

  {
    print "source_commit=$source_commit"
    print "platform=iOS"
  } > "$log"

  xcrun simctl bootstatus "$udid" -b >/dev/null 2>&1 || true
  xcrun simctl launch "$udid" "$bundle" >/dev/null

  print ""
  print "Launched on $udid. Enter a raid within the first minute and leave it"
  print "running - do not pause or background the app. Capturing ${seconds}s..."
  print ""

  xcrun simctl spawn "$udid" log stream \
    --predicate 'eventMessage CONTAINS "BUKOV_FRAME_TELEMETRY"' \
    --style compact >> "$log" 2>&1 &
  local pid=$!
  sleep "$seconds"
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

case "$platform" in
  macOS) shift; capture_macos "$@" ;;
  iOS)   shift; capture_ios "$@" ;;
  *)     usage ;;
esac

records=$(grep -c 'bukov-render-frame-v4' "$log" || true)
print ""
print "telemetry_records=$records"
if (( records == 0 )); then
  print -u2 ""
  print -u2 "No telemetry was recorded. BukovFrameTelemetry only emits inside a"
  print -u2 "live raid with the hero alive and the game unpaused - the capture"
  print -u2 "most likely sat on the title or hub screen."
  exit 1
fi

print ""
print "Validating through the real gate:"
python3 "$script_dir/bukov_render_frame_gate.py" \
  --input "$log" \
  --expected-source-commit "$source_commit" \
  --require-platform "$platform" \
  --minimum-duration-seconds 1800 \
  --output "$output_dir/${platform}-render-summary.json"
