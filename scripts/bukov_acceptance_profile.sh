#!/bin/zsh
set -euo pipefail

script_path="${0:A}"

usage() {
  cat <<'EOF'
Usage:
  scripts/bukov_acceptance_profile.sh reset \
    --platform mac|ios|all --evidence-root <directory> [--udid <simulator-udid>]

  scripts/bukov_acceptance_profile.sh restore \
    --backup <reset-backup-directory> [--udid <simulator-udid>]

  scripts/bukov_acceptance_profile.sh self-test

The reset operation never deletes a profile. It moves the Bukov profile and
reserved host slot into a timestamped evidence backup. Restore first moves any
post-run profile into the same backup before returning the original files.
EOF
}

fail() {
  print -u2 -- "ERROR: $*"
  exit 1
}

command_name="${1:-}"
[[ -n "$command_name" ]] || {
  usage
  exit 2
}
if [[ "$command_name" == "-h" || "$command_name" == "--help" ]]; then
  usage
  exit 0
fi
shift

platform=""
evidence_root=""
backup_root=""
simulator_udid=""
while (( $# > 0 )); do
  case "$1" in
    --platform)
      (( $# >= 2 )) || fail "--platform requires a value"
      platform="$2"
      shift 2
      ;;
    --evidence-root)
      (( $# >= 2 )) || fail "--evidence-root requires a value"
      evidence_root="$2"
      shift 2
      ;;
    --backup)
      (( $# >= 2 )) || fail "--backup requires a value"
      backup_root="$2"
      shift 2
      ;;
    --udid)
      (( $# >= 2 )) || fail "--udid requires a value"
      simulator_udid="$2"
      shift 2
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

resolve_udid() {
  if [[ -n "$simulator_udid" ]]; then
    print -r -- "$simulator_udid"
    return
  fi
  local json selected
  json="$(xcrun simctl list devices booted --json)"
  selected="$(
    print -r -- "$json" | python3 -c '
import json, sys
data = json.load(sys.stdin)
devices = [
    device
    for values in data.get("devices", {}).values()
    for device in values
    if device.get("state") == "Booted" and device.get("isAvailable", True)
]
if len(devices) != 1:
    raise SystemExit(
        "expected exactly one booted available simulator, found "
        + str(len(devices))
    )
print(devices[0]["udid"])
'
  )" || fail "unable to resolve one booted simulator; pass --udid"
  print -r -- "$selected"
}

mac_data_root() {
  print -r -- "$HOME/Library/Application Support/逃离布科夫"
}

ios_data_root() {
  local udid="$1"
  local container
  container="$(
    xcrun simctl get_app_container \
      "$udid" com.leoyuan.escapefrombukov data
  )" || fail "unable to locate the iOS Bukov app container"
  print -r -- "$container/Library/local"
}

stop_platform() {
  local target="$1"
  if [[ "$target" == "mac" ]]; then
    pkill -x "逃离布科夫" 2>/dev/null || true
  else
    local udid="$2"
    xcrun simctl terminate \
      "$udid" com.leoyuan.escapefrombukov >/dev/null 2>&1 || true
  fi
}

move_family() {
  local source_root="$1"
  local destination_root="$2"
  local label="$3"
  mkdir -p "$destination_root"
  local moved=0
  local family
  for family in bukov game100; do
    local source="$source_root/$family"
    local destination="$destination_root/$family"
    if [[ -e "$source" ]]; then
      [[ ! -e "$destination" ]] \
        || fail "backup target already exists: $destination"
      mv "$source" "$destination"
      moved=1
    fi
  done
  print -r -- "$label moved=$moved source=$source_root destination=$destination_root"
}

reset_platform() {
  local target="$1"
  local destination="$2"
  local udid="$3"
  stop_platform "$target" "$udid"
  local source_root
  if [[ "$target" == "mac" ]]; then
    source_root="$(mac_data_root)"
  else
    source_root="$(ios_data_root "$udid")"
  fi
  mkdir -p "$source_root"
  move_family "$source_root" "$destination/$target" "$target"
}

restore_platform() {
  local target="$1"
  local source="$2"
  local udid="$3"
  [[ -d "$source" ]] || return 0
  stop_platform "$target" "$udid"
  local destination_root
  if [[ "$target" == "mac" ]]; then
    destination_root="$(mac_data_root)"
  else
    destination_root="$(ios_data_root "$udid")"
  fi
  mkdir -p "$destination_root"
  local post_run="$source/post-run-current"
  move_family "$destination_root" "$post_run" "$target post-run"
  local family
  for family in bukov game100; do
    if [[ -e "$source/$family" ]]; then
      [[ ! -e "$destination_root/$family" ]] \
        || fail "restore destination still exists: $destination_root/$family"
      mv "$source/$family" "$destination_root/$family"
    fi
  done
	print -r -- "$target restored_from=$source"
}

run_self_test() {
  /bin/zsh -n "$script_path" || fail "zsh syntax check failed"
  local self_root
  self_root="$(mktemp -d "${TMPDIR:-/tmp}/bukov-profile-selftest.XXXXXX")"
  [[ -d "$self_root" ]] || fail "unable to create self-test directory"
  local source_root="$self_root/source"
  local backup_root="$self_root/backup"
  mkdir -p "$source_root/bukov" "$source_root/game100"
  print -r -- profile > "$source_root/bukov/profile.txt"
  print -r -- host > "$source_root/game100/host.txt"

  move_family "$source_root" "$backup_root" "self-test" >/dev/null
  [[ ! -e "$source_root/bukov" && ! -e "$source_root/game100" ]] \
    || fail "self-test reset left live profile data behind"
  [[ -f "$backup_root/bukov/profile.txt" ]] \
    || fail "self-test did not preserve Bukov profile"
  [[ -f "$backup_root/game100/host.txt" ]] \
    || fail "self-test did not preserve reserved host slot"

  mkdir -p "$source_root"
  mv "$backup_root/bukov" "$source_root/bukov"
  mv "$backup_root/game100" "$source_root/game100"
  [[ -f "$source_root/bukov/profile.txt" ]] \
    || fail "self-test restore lost Bukov profile"
  [[ -f "$source_root/game100/host.txt" ]] \
    || fail "self-test restore lost reserved host slot"

  [[ "${self_root:A:h}" == "${${TMPDIR:-/tmp}:A}" ]] \
    || fail "self-test directory escaped TMPDIR"
  rm -rf -- "$self_root"
  print "PASS: recoverable profile move and restore self-test"
}

case "$command_name" in
  self-test)
    [[ -z "$platform" && -z "$evidence_root" && -z "$backup_root" \
      && -z "$simulator_udid" ]] \
      || fail "self-test does not accept platform arguments"
    run_self_test
    ;;
  reset)
    [[ "$platform" == "mac" || "$platform" == "ios" || "$platform" == "all" ]] \
      || fail "--platform must be mac, ios, or all"
    [[ -n "$evidence_root" ]] || fail "--evidence-root is required"
    mkdir -p "$evidence_root"
    timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
    destination="$evidence_root/$timestamp"
    [[ ! -e "$destination" ]] || fail "backup already exists: $destination"
    mkdir -p "$destination"
    udid=""
    if [[ "$platform" == "ios" || "$platform" == "all" ]]; then
      udid="$(resolve_udid)"
    fi
    if [[ "$platform" == "mac" || "$platform" == "all" ]]; then
      reset_platform mac "$destination" "$udid"
    fi
    if [[ "$platform" == "ios" || "$platform" == "all" ]]; then
      reset_platform ios "$destination" "$udid"
      print -r -- "$udid" > "$destination/ios-udid.txt"
    fi
    {
      print -r -- "status=reset-ready"
      print -r -- "created_utc=$timestamp"
      print -r -- "platform=$platform"
      print -r -- "backup=$destination"
    } > "$destination/RECEIPT.txt"
    print -r -- "$destination"
    ;;
  restore)
    [[ -n "$backup_root" ]] || fail "--backup is required"
    [[ -d "$backup_root" ]] || fail "backup directory does not exist"
    udid=""
    if [[ -d "$backup_root/ios" ]]; then
      if [[ -f "$backup_root/ios-udid.txt" ]]; then
        udid="$(<"$backup_root/ios-udid.txt")"
      else
        udid="$(resolve_udid)"
      fi
    fi
    restore_platform mac "$backup_root/mac" "$udid"
    restore_platform ios "$backup_root/ios" "$udid"
    print -r -- "status=restored backup=$backup_root"
    ;;
  *)
    usage
    exit 2
    ;;
esac
