#!/bin/zsh
set -euo pipefail

script_path="${0:A}"
package_arg=""
expected_source_commit=""
device_udid_arg=""
apply=false
self_test=false
install_root="$HOME/Applications"
mac_target=""
verify_root=""
old_mac_backup=""
mac_install_started=false
mac_launched_pid=""
resolved_device_udid=""
resolved_device_name=""
resolved_device_runtime=""
receipt_file=""

usage() {
  cat <<'USAGE'
Usage:
  scripts/bukov_install_personal_build.sh \
    --package /absolute/逃离布科夫-VERSION \
    --expected-source-commit 40_HEX_GIT_SHA \
    [--device-udid BOOTED_SIMULATOR_UDID] \
    [--apply]

Verifies an archive-only personal build, then installs exactly that build to
~/Applications and the currently booted iOS Simulator. The default is a dry
run that performs the same archive, signature, source-identity, and executable
hash checks without changing either installation.

With --apply, an existing ~/Applications/逃离布科夫.app is moved to a unique
folder in ~/.Trash before the verified replacement is installed. It is never
permanently deleted.

The expected source commit is mandatory. Omitting it or selecting a valid older
package is rejected. If --device-udid is omitted, exactly one iOS Simulator must
be booted. A successful install launches both exact bundles and writes a receipt
under ~/Library/Logs/EscapeFromBukov/install-receipts/.

Options:
  --package DIR                  Verified personal-build directory.
  --expected-source-commit SHA   Required exact 40-character lowercase SHA.
  --device-udid UDID             Exact booted Simulator. Required when more
                                 than one Simulator is booted.
  --apply                        Install and launch after verification.
  --self-test                    Run non-Gradle syntax and policy self-tests.
  -h, --help                     Show this help.
USAGE
}

fail() {
  print -u2 "ERROR: $*"
  exit 1
}

info_value() {
  local key="$1"
  local file="$2"
  /usr/bin/awk -F= -v wanted="$key" \
    '$1 == wanted { print substr($0, index($0, "=") + 1); exit }' "$file"
}

valid_source_commit() {
  [[ "$1" =~ '^[0-9a-f]{40}$' ]]
}

valid_simulator_udid() {
  [[ "$1" =~ '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$' ]]
}

source_identity_matches() {
  valid_source_commit "$1" \
    && valid_source_commit "$2" \
    && [[ "$1" == "$2" ]]
}

process_pids_for_executable() {
  local executable_path="$1"
  /usr/bin/python3 - "$executable_path" <<'PY'
import subprocess
import sys

expected = sys.argv[1]
output = subprocess.check_output(
    ["/bin/ps", "-axo", "pid=,command="],
    text=True,
    errors="replace",
)
for raw_line in output.splitlines():
    line = raw_line.strip()
    if not line:
        continue
    fields = line.split(None, 1)
    if len(fields) != 2 or not fields[0].isdigit():
        continue
    command = fields[1]
    if command == expected or command.startswith(expected + " "):
        print(fields[0])
PY
}

process_command_for_pid() {
  local pid="$1"
  /bin/ps -ww -p "$pid" -o command= 2>/dev/null \
    | /usr/bin/awk '{$1=$1; print; exit}'
}

terminate_executable_processes() {
  local executable_path="$1"
  typeset -a pids
  pids=()
  while IFS= read -r pid; do
    [[ -n "$pid" ]] && pids+=("$pid")
  done < <(process_pids_for_executable "$executable_path")
  (( ${#pids} > 0 )) || return 0

  local pid
  for pid in "${pids[@]}"; do
    if [[ ! "$pid" =~ '^[1-9][0-9]*$' ]]; then
      print -u2 "ERROR: unsafe process id while closing old macOS app: $pid"
      return 1
    fi
    /bin/kill -TERM "$pid" || return 1
  done

  local attempt
  for attempt in {1..50}; do
    pids=()
    while IFS= read -r pid; do
      [[ -n "$pid" ]] && pids+=("$pid")
    done < <(process_pids_for_executable "$executable_path")
    (( ${#pids} == 0 )) && return 0
    /bin/sleep 0.1
  done
  print -u2 "ERROR: old macOS app did not exit cleanly: $executable_path"
  return 1
}

wait_for_single_executable_pid() {
  local executable_path="$1"
  local attempt
  typeset -a pids
  for attempt in {1..100}; do
    pids=()
    while IFS= read -r pid; do
      [[ -n "$pid" ]] && pids+=("$pid")
    done < <(process_pids_for_executable "$executable_path")
    if (( ${#pids} == 1 )); then
      print -r -- "${pids[1]}"
      return 0
    fi
    (( ${#pids} <= 1 )) \
      || fail "multiple macOS processes launched from exact install: $executable_path"
    /bin/sleep 0.1
  done
  fail "macOS app did not launch from exact install: $executable_path"
}

select_booted_simulator() {
  local requested_udid="$1"
  /usr/bin/python3 -c '
import json
import sys

requested = sys.argv[1]
payload = json.load(sys.stdin)
booted = []
for runtime, devices in payload.get("devices", {}).items():
    for device in devices:
        if device.get("state") != "Booted":
            continue
        if device.get("isAvailable") is False:
            continue
        booted.append((
            str(device.get("udid", "")),
            str(device.get("name", "")),
            str(runtime),
        ))

if requested:
    matches = [device for device in booted if device[0].lower() == requested.lower()]
    if len(matches) != 1:
        raise SystemExit(
            f"requested Simulator is not the unique booted match: {requested}")
    selected = matches[0]
else:
    if len(booted) != 1:
        names = ", ".join(f"{name} ({udid})" for udid, name, _ in booted)
        raise SystemExit(
            f"expected exactly one booted Simulator, found {len(booted)}: {names}")
    selected = booted[0]

if any("\t" in value or "\n" in value for value in selected):
    raise SystemExit("Simulator metadata contains control characters")
print("\t".join(selected))
' "$requested_udid"
}

resolve_booted_simulator() {
  local requested_udid="$1"
  local simulator_json
  simulator_json="$(/usr/bin/xcrun simctl list devices booted -j)" \
    || fail "could not list booted iOS Simulators"
  print -r -- "$simulator_json" \
    | select_booted_simulator "$requested_udid"
}

run_self_test() {
  /bin/zsh -n "$script_path" || fail "zsh syntax check failed"
  local sample_commit="0123456789abcdef0123456789abcdef01234567"
  valid_source_commit "$sample_commit" \
    || fail "valid source commit was rejected"
  ! valid_source_commit "0123456789abcdef" \
    || fail "short source commit was accepted"
  ! valid_source_commit "0123456789ABCDEF0123456789ABCDEF01234567" \
    || fail "uppercase source commit was accepted"
  source_identity_matches "$sample_commit" "$sample_commit" \
    || fail "matching source identities were rejected"
  ! source_identity_matches \
      "$sample_commit" "1123456789abcdef0123456789abcdef01234567" \
    || fail "mismatched source identities were accepted"
  valid_simulator_udid "50D8337F-7AFD-4AAE-AB44-318BCDC02AF6" \
    || fail "valid Simulator UDID was rejected"
  ! valid_simulator_udid "booted" \
    || fail "ambiguous Simulator alias was accepted"
  local sample_udid="50D8337F-7AFD-4AAE-AB44-318BCDC02AF6"
  local sample_simulator_json='{"devices":{"com.apple.CoreSimulator.SimRuntime.iOS-18-5":[{"state":"Booted","isAvailable":true,"name":"iPhone 16 Pro","udid":"50D8337F-7AFD-4AAE-AB44-318BCDC02AF6"}]}}'
  local selected_simulator
  selected_simulator="$(
    print -r -- "$sample_simulator_json" \
      | select_booted_simulator "$sample_udid"
  )" || fail "exact Simulator selector rejected one valid device"
  [[ "$selected_simulator" == \
      $'50D8337F-7AFD-4AAE-AB44-318BCDC02AF6\tiPhone 16 Pro\tcom.apple.CoreSimulator.SimRuntime.iOS-18-5' ]] \
    || fail "exact Simulator selector returned unexpected metadata"
  local ambiguous_simulator_json='{"devices":{"runtime":[{"state":"Booted","name":"A","udid":"50D8337F-7AFD-4AAE-AB44-318BCDC02AF6"},{"state":"Booted","name":"B","udid":"60D8337F-7AFD-4AAE-AB44-318BCDC02AF6"}]}}'
  ! print -r -- "$ambiguous_simulator_json" \
      | select_booted_simulator "" >/dev/null 2>&1 \
    || fail "ambiguous booted Simulator set was accepted"
  [[ -z "$(process_pids_for_executable \
      "/definitely/not/a/real/bukov/executable")" ]] \
    || fail "nonexistent executable unexpectedly matched a process"
  print "PASS: install identity, device-target, process, and syntax self-tests"
}

cleanup() {
  local exit_code=$?
  if [[ -n "$verify_root" && -d "$verify_root" ]]; then
    [[ "${verify_root:A:h}" == "${${TMPDIR:-/tmp}:A}" ]] \
      && [[ "${verify_root:t}" == bukov-install-verify.* ]] \
      && rm -rf -- "$verify_root"
  fi
  if (( exit_code != 0 )) && [[ "$mac_install_started" == true ]]; then
    if [[ -n "$mac_target" && -d "$mac_target" ]]; then
      local failed_executable=""
      if [[ -n "${mac_executable:-}" ]]; then
        failed_executable="${mac_target}/Contents/MacOS/${mac_executable}"
      fi
      if [[ -n "$failed_executable" ]]; then
        terminate_executable_processes "$failed_executable" 2>/dev/null \
          || print -u2 "WARNING: could not stop failed macOS install"
      fi
      local failed_install_dir="$HOME/.Trash/逃离布科夫-安装失败-$(date +%Y%m%d-%H%M%S)-$$"
      mkdir -p "$failed_install_dir"
      mv "$mac_target" "${failed_install_dir}/逃离布科夫.app" \
        || print -u2 "WARNING: could not move failed macOS install to Trash"
    fi
    if [[ -n "$old_mac_backup" && -d "$old_mac_backup" ]] \
        && [[ -n "$mac_target" && ! -e "$mac_target" ]]; then
      mv "$old_mac_backup" "$mac_target" \
        || print -u2 "WARNING: could not restore previous macOS app from $old_mac_backup"
    fi
  fi
  return $exit_code
}

while (( $# > 0 )); do
  case "$1" in
    --package)
      (( $# >= 2 )) || fail "--package requires a value"
      package_arg="$2"
      shift 2
      ;;
    --expected-source-commit)
      (( $# >= 2 )) || fail "--expected-source-commit requires a value"
      expected_source_commit="$2"
      shift 2
      ;;
    --device-udid)
      (( $# >= 2 )) || fail "--device-udid requires a value"
      device_udid_arg="$2"
      shift 2
      ;;
    --apply)
      apply=true
      shift
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
  [[ -z "$package_arg" && -z "$expected_source_commit"
      && -z "$device_udid_arg" && "$apply" != true ]] \
    || fail "--self-test cannot be combined with install options"
  run_self_test
  exit 0
fi

[[ -n "$package_arg" ]] || fail "--package is required"
[[ -n "$expected_source_commit" ]] \
  || fail "--expected-source-commit is required"
valid_source_commit "$expected_source_commit" \
  || fail "--expected-source-commit must be a lowercase 40-character Git SHA"
if [[ -n "$device_udid_arg" ]]; then
  valid_simulator_udid "$device_udid_arg" \
    || fail "--device-udid must be a full Simulator UDID"
fi
[[ -d "$package_arg" ]] || fail "package directory does not exist: $package_arg"
package_dir="${package_arg:A}"
[[ "${package_dir:t}" == 逃离布科夫-* ]] \
  || fail "unexpected package directory name: ${package_dir:t}"

for command_path in \
    /usr/bin/awk \
    /bin/kill \
    /usr/bin/open \
    /bin/ps \
    /usr/bin/python3 \
    /usr/bin/codesign \
    /usr/bin/find \
    /usr/bin/shasum \
    /usr/bin/unzip \
    /usr/bin/xattr \
    /usr/bin/xcrun \
    /usr/libexec/PlistBuddy; do
  [[ -x "$command_path" ]] || fail "required tool is unavailable: $command_path"
done

package_info="${package_dir}/PACKAGE_INFO.txt"
hashes_file="${package_dir}/SHA256SUMS.txt"
[[ -f "$package_info" ]] || fail "PACKAGE_INFO.txt is missing"
[[ -f "$hashes_file" ]] || fail "SHA256SUMS.txt is missing"
[[ "$(info_value distribution "$package_info")" == verified-archives-only ]] \
  || fail "package is not an archive-only verified distribution"
[[ "$(info_value source_worktree "$package_info")" == clean ]] \
  || fail "package was not built from a clean worktree"

mac_archive_name="$(info_value macos_archive "$package_info")"
ios_archive_name="$(info_value ios_simulator_archive "$package_info")"
mac_expected_hash="$(info_value macos_executable_sha256 "$package_info")"
ios_expected_hash="$(info_value ios_simulator_executable_sha256 "$package_info")"
source_commit="$(info_value source_commit "$package_info")"
[[ "$mac_archive_name" == *.zip && "$mac_archive_name" != */* ]] \
  || fail "unsafe macOS archive name in PACKAGE_INFO.txt"
[[ "$ios_archive_name" == *.zip && "$ios_archive_name" != */* ]] \
  || fail "unsafe iOS archive name in PACKAGE_INFO.txt"
[[ "$source_commit" =~ '^[0-9a-f]{40}$' ]] \
  || fail "invalid source commit in PACKAGE_INFO.txt"
source_identity_matches "$expected_source_commit" "$source_commit" \
  || fail "refusing package from wrong source commit: expected=${expected_source_commit}, package=${source_commit}"
[[ "$mac_expected_hash" =~ '^[0-9a-f]{64}$' ]] \
  || fail "invalid macOS executable hash in PACKAGE_INFO.txt"
[[ "$ios_expected_hash" =~ '^[0-9a-f]{64}$' ]] \
  || fail "invalid iOS executable hash in PACKAGE_INFO.txt"

(
  cd "$package_dir"
  /usr/bin/shasum -a 256 -c "${hashes_file:t}"
)

verify_root="$(mktemp -d "${TMPDIR:-/tmp}/bukov-install-verify.XXXXXX")"
verify_root="${verify_root:A}"
trap cleanup EXIT
trap 'exit 130' HUP INT TERM
/usr/bin/unzip -q "${package_dir}/${mac_archive_name}" -d "${verify_root}/macos"
/usr/bin/unzip -q "${package_dir}/${ios_archive_name}" -d "${verify_root}/ios"

typeset -a mac_apps ios_apps
mac_apps=("${verify_root}/macos"/*.app(N))
ios_apps=("${verify_root}/ios"/*.app(N))
(( ${#mac_apps} == 1 )) || fail "macOS archive must contain exactly one .app"
(( ${#ios_apps} == 1 )) || fail "iOS archive must contain exactly one .app"
mac_app="${mac_apps[1]:A}"
ios_app="${ios_apps[1]:A}"

/usr/bin/codesign --verify --deep --strict --verbose=2 "$mac_app"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$ios_app"

mac_executable="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' \
    "${mac_app}/Contents/Info.plist"
)" || fail "macOS bundle executable is missing"
ios_executable="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' \
    "${ios_app}/Info.plist"
)" || fail "iOS bundle executable is missing"
[[ -n "$mac_executable" && "$mac_executable" != */*
    && "$mac_executable" != "." && "$mac_executable" != ".." ]] \
  || fail "unsafe macOS bundle executable name"
[[ -n "$ios_executable" && "$ios_executable" != */*
    && "$ios_executable" != "." && "$ios_executable" != ".." ]] \
  || fail "unsafe iOS bundle executable name"
mac_bundle_id="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' \
    "${mac_app}/Contents/Info.plist"
)" || fail "macOS bundle identifier is missing"
ios_bundle_id="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' \
    "${ios_app}/Info.plist"
)" || fail "iOS bundle identifier is missing"
[[ -n "$mac_bundle_id" && "$mac_bundle_id" == "$ios_bundle_id" ]] \
  || fail "macOS and iOS bundle identifiers differ"
mac_actual_hash="$(
  /usr/bin/shasum -a 256 "${mac_app}/Contents/MacOS/${mac_executable}"
)"
mac_actual_hash="${mac_actual_hash%% *}"
ios_actual_hash="$(
  /usr/bin/shasum -a 256 "${ios_app}/${ios_executable}"
)"
ios_actual_hash="${ios_actual_hash%% *}"
[[ "$mac_actual_hash" == "$mac_expected_hash" ]] \
  || fail "macOS executable hash does not match PACKAGE_INFO.txt"
[[ "$ios_actual_hash" == "$ios_expected_hash" ]] \
  || fail "iOS executable hash does not match PACKAGE_INFO.txt"

typeset -a mac_identity_jars
mac_identity_jars=()
while IFS= read -r identity_jar; do
  mac_identity_jars+=("$identity_jar")
done < <(
  /usr/bin/find "${mac_app}/Contents/app" -maxdepth 1 -type f \
    -name 'desktop-*.jar' -print
)
(( ${#mac_identity_jars} == 1 )) \
  || fail "expected exactly one macOS identity jar, found ${#mac_identity_jars}"
mac_jar="${mac_identity_jars[1]}"
mac_embedded_commit="$(
  /usr/bin/unzip -p "$mac_jar" bukov-build-identity.properties \
    | /usr/bin/awk -F= '$1 == "source_commit" { print substr($0, index($0, "=") + 1); exit }'
)"
ios_embedded_commit="$(
  /usr/libexec/PlistBuddy -c 'Print :BukovSourceCommit' "${ios_app}/Info.plist"
)" || fail "iOS embedded build identity is missing"
[[ "$mac_embedded_commit" == "$source_commit" ]] \
  || fail "macOS embedded source commit differs from PACKAGE_INFO.txt"
[[ "$ios_embedded_commit" == "$source_commit" ]] \
  || fail "iOS embedded source commit differs from PACKAGE_INFO.txt"

print "Verified personal build:"
print "  package=$package_dir"
print "  source_commit=$source_commit"
print "  expected_source_commit=$expected_source_commit"
print "  macos_executable_sha256=$mac_actual_hash"
print "  ios_simulator_executable_sha256=$ios_actual_hash"

if [[ "$apply" != true ]]; then
  print "Dry run complete. No installed app was changed."
  print "Re-run with --apply to install this exact build."
  exit 0
fi

[[ -d "$install_root" ]] || mkdir -p "$install_root"
install_root="${install_root:A}"
mac_target="${install_root}/逃离布科夫.app"
[[ "${mac_target:A:h}" == "$install_root" ]] \
  || fail "macOS install target escaped ~/Applications"

simulator_record="$(resolve_booted_simulator "$device_udid_arg")" \
  || fail "could not resolve one exact booted iOS Simulator"
IFS=$'\t' read -r \
  resolved_device_udid resolved_device_name resolved_device_runtime \
  <<< "$simulator_record"
valid_simulator_udid "$resolved_device_udid" \
  || fail "resolved Simulator returned an invalid UDID"
print "  simulator=${resolved_device_name} (${resolved_device_udid})"
print "  runtime=${resolved_device_runtime}"

old_mac_executable="${mac_target}/Contents/MacOS/${mac_executable}"
if [[ -f "${mac_target}/Contents/Info.plist" ]]; then
  old_mac_executable_name="$(
    /usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' \
      "${mac_target}/Contents/Info.plist" 2>/dev/null
  )" || fail "existing macOS bundle executable is unreadable"
  [[ -n "$old_mac_executable_name" && "$old_mac_executable_name" != */*
      && "$old_mac_executable_name" != "."
      && "$old_mac_executable_name" != ".." ]] \
    || fail "existing macOS bundle has an unsafe executable name"
  old_mac_executable="${mac_target}/Contents/MacOS/${old_mac_executable_name}"
fi
terminate_executable_processes "$old_mac_executable" \
  || fail "could not close the old macOS app before replacement"
mac_install_started=true

if [[ -e "$mac_target" || -L "$mac_target" ]]; then
  [[ -d "$mac_target" && ! -L "$mac_target" ]] \
    || fail "existing macOS target is not a regular app directory"
  trash_batch="$HOME/.Trash/逃离布科夫-旧安装-$(date +%Y%m%d-%H%M%S)-$$"
  [[ ! -e "$trash_batch" ]] || fail "Trash destination already exists"
  mkdir -p "$trash_batch"
  old_mac_backup="${trash_batch}/逃离布科夫.app"
  mv "$mac_target" "$old_mac_backup"
fi

mv "$mac_app" "$mac_target"
/usr/bin/xattr -cr "$mac_target"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$mac_target"
installed_mac_hash="$(
  /usr/bin/shasum -a 256 "${mac_target}/Contents/MacOS/${mac_executable}"
)"
installed_mac_hash="${installed_mac_hash%% *}"
[[ "$installed_mac_hash" == "$mac_expected_hash" ]] \
  || fail "installed macOS executable differs from verified package"
installed_mac_executable="${mac_target}/Contents/MacOS/${mac_executable}"
/usr/bin/open -n "$mac_target"
mac_launched_pid="$(wait_for_single_executable_pid "$installed_mac_executable")"
[[ "$mac_launched_pid" =~ '^[1-9][0-9]*$' ]] \
  || fail "macOS launch did not return a valid process id"
mac_process_command="$(process_command_for_pid "$mac_launched_pid")"
[[ "$mac_process_command" == "$installed_mac_executable"
    || "${mac_process_command#"$installed_mac_executable "}" != "$mac_process_command" ]] \
  || fail "macOS process is not running from exact installed executable: $mac_process_command"

/usr/bin/xcrun simctl terminate \
  "$resolved_device_udid" "$ios_bundle_id" >/dev/null 2>&1 || true
/usr/bin/xcrun simctl install "$resolved_device_udid" "$ios_app"
installed_ios_app="$(
  /usr/bin/xcrun simctl get_app_container \
    "$resolved_device_udid" "$ios_bundle_id" app
)"
[[ -d "$installed_ios_app" ]] || fail "installed iOS app container is missing"
installed_ios_app="${installed_ios_app:A}"
installed_ios_hash="$(
  /usr/bin/shasum -a 256 "${installed_ios_app}/${ios_executable}"
)"
installed_ios_hash="${installed_ios_hash%% *}"
[[ "$installed_ios_hash" == "$ios_expected_hash" ]] \
  || fail "installed iOS executable differs from verified package"

ios_launch_output="$(
  /usr/bin/xcrun simctl launch --terminate-running-process \
    "$resolved_device_udid" "$ios_bundle_id"
)" || fail "iOS Simulator app failed to launch"
ios_launched_pid="$(
  print -r -- "$ios_launch_output" \
    | /usr/bin/awk '{candidate=$NF} END {print candidate}'
)"
[[ "$ios_launched_pid" =~ '^[1-9][0-9]*$' ]] \
  || fail "iOS launch did not return a valid process id: $ios_launch_output"
ios_process_command="$(process_command_for_pid "$ios_launched_pid")"
[[ -n "$ios_process_command" ]] \
  || fail "iOS launched process is no longer running"

receipt_root="$HOME/Library/Logs/EscapeFromBukov/install-receipts"
mkdir -p "$receipt_root"
receipt_root="${receipt_root:A}"
receipt_stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
receipt_file="${receipt_root}/${receipt_stamp}-${source_commit[1,12]}-$$.txt"
[[ ! -e "$receipt_file" && ! -L "$receipt_file" ]] \
  || fail "install receipt already exists: $receipt_file"
umask 077
{
  print "status=passed"
  print "installed_utc=$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  print "package=$package_dir"
  print "source_commit=$source_commit"
  print "expected_source_commit=$expected_source_commit"
  print "macos_bundle_id=$mac_bundle_id"
  print "macos_app=$mac_target"
  print "macos_executable_sha256=$installed_mac_hash"
  print "macos_pid=$mac_launched_pid"
  print "macos_process=$mac_process_command"
  print "ios_bundle_id=$ios_bundle_id"
  print "ios_device_udid=$resolved_device_udid"
  print "ios_device_name=$resolved_device_name"
  print "ios_runtime=$resolved_device_runtime"
  print "ios_app=$installed_ios_app"
  print "ios_executable_sha256=$installed_ios_hash"
  print "ios_pid=$ios_launched_pid"
  print "ios_process=$ios_process_command"
} > "$receipt_file"

old_mac_backup=""
print "Installed verified latest build:"
print "  macOS=$mac_target"
print "  iOS Simulator=$installed_ios_app"
print "  source_commit=$source_commit"
print "  macOS PID=$mac_launched_pid"
print "  iOS PID=$ios_launched_pid"
print "  receipt=$receipt_file"
