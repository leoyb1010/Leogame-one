#!/bin/zsh
set -euo pipefail

script_path="${0:A}"
project_root="${script_path:h:h}"
default_cache_root="$(getconf DARWIN_USER_CACHE_DIR)/escape-from-bukov-gradle"

output_arg=""
version=""
cache_arg="$default_cache_root"
apply=false
self_test=false
mode_seen=""
staging_dir=""
staging_parent=""
lock_dir=""
output_dir=""

usage() {
  cat <<'USAGE'
Usage:
  scripts/bukov_package_personal_build.sh \
    --output /absolute/output/directory \
    --version alpha16-description \
    [--cache /absolute/apple-gradle/cache] \
    [--dry-run | --apply]

Seals the current macOS and iOS Simulator .app bundles from apple-gradle's
cache into one immutable, archive-only direct child of --output:

  OUTPUT/逃离布科夫-VERSION/

The default is --dry-run. --apply performs these steps without a developer
certificate:

  1. Requires a clean Git worktree and proves both cached bundles embed the
     exact current source commit and clean build state.
  2. Strictly verifies both cached source bundles.
  3. Copies both bundles into a private, non-FileProvider staging directory.
  4. Recursively removes extended attributes, including FinderInfo and
     resource forks.
  5. Ad-hoc signs and strictly verifies both copied bundles.
  6. Normalizes bundle timestamps, creates deterministic ZIP archives,
     extracts them, and strictly verifies the extracted signed bundles.
  7. Writes executable/archive SHA-256 values and atomically publishes only
     the ZIP archives and text manifests. Loose .app bundles are deliberately
     omitted because Finder/FileProvider can reattach metadata that invalidates
     an otherwise correct signature.

An existing version directory is never overwritten. VERSION is a safe slug,
not a path; use letters, digits, dot, underscore, and hyphen only.

Options:
  --output DIR     Existing output root. Required.
  --version VALUE  Version slug, for example alpha16-loadout-fix. Required.
  --cache DIR      apple-gradle cache root (default: Darwin user cache).
  --dry-run        Validate and print the plan without writing anything.
  --apply          Package and publish the version.
  --self-test      Run shell syntax and path-safety checks only.
  -h, --help       Show this help.

SOURCE_DATE_EPOCH may be set to an explicit positive Unix timestamp. Otherwise
the newer cached bundle timestamp is used for reproducible archive metadata.
USAGE
}

fail() {
  print -u2 "ERROR: $*"
  exit 1
}

valid_version() {
  local candidate="$1"
  [[ "$candidate" =~ '^[A-Za-z0-9][A-Za-z0-9._-]*$' ]] \
    && [[ "$candidate" != *..* ]]
}

valid_build_identity() {
  local current_commit="$1"
  local current_state="$2"
  local mac_commit="$3"
  local mac_state="$4"
  local ios_commit="$5"
  local ios_state="$6"
  [[ "$current_commit" =~ '^[0-9a-f]{40}$' ]] \
    && [[ "$current_state" == clean ]] \
    && [[ "$mac_commit" == "$current_commit" ]] \
    && [[ "$mac_state" == clean ]] \
    && [[ "$ios_commit" == "$current_commit" ]] \
    && [[ "$ios_state" == clean ]]
}

safe_output_root() {
  local candidate="$1"
  [[ -d "$candidate" ]] || return 1
  candidate="${candidate:A}"
  [[ "$candidate" != "/" ]] || return 1
  [[ "$candidate" != "${HOME:A}" ]] || return 1
}

direct_child() {
  local parent="${1:A}"
  local candidate="${2:A}"
  [[ "${candidate:h}" == "$parent" ]]
}

target_available() {
  local candidate="$1"
  [[ ! -e "$candidate" && ! -L "$candidate" ]]
}

safe_remove_staging() {
  local candidate="$1"
  [[ -n "$candidate" ]] || return 0
  [[ -n "$staging_parent" ]] || return 1
  candidate="${candidate:A}"
  direct_child "$staging_parent" "$candidate" || return 1
  [[ "${candidate:t}" == "bukov-package-${version}.staging."* ]] || return 1
  [[ -d "$candidate" ]] || return 0
  rm -rf -- "$candidate"
}

cleanup() {
  local exit_code=$?
  if [[ -n "$staging_dir" && -e "$staging_dir" ]]; then
    safe_remove_staging "$staging_dir" \
      || print -u2 "WARNING: refusing unsafe staging cleanup: $staging_dir"
  fi
  if [[ -n "$lock_dir" && -d "$lock_dir" ]]; then
    if [[ -n "$output_dir" ]] \
        && direct_child "$output_dir" "$lock_dir" \
        && [[ "${lock_dir:t}" == ".bukov-package-${version}.lock" ]]; then
      rmdir "$lock_dir" 2>/dev/null \
        || print -u2 "WARNING: could not remove packaging lock: $lock_dir"
    else
      print -u2 "WARNING: refusing unsafe lock cleanup: $lock_dir"
    fi
  fi
  return $exit_code
}

run_self_test() {
  /bin/zsh -n "$script_path" \
    || fail "zsh syntax check failed"

  valid_version "alpha16-loadout-fix" \
    || fail "safe version was rejected"
  for unsafe in "../alpha15" "alpha/15" "/tmp/alpha15" "." ".." \
      "alpha 15" "逃离布科夫-alpha15"; do
    if valid_version "$unsafe"; then
      fail "unsafe version was accepted: $unsafe"
    fi
  done

  local self_root
  self_root="$(mktemp -d "${TMPDIR:-/tmp}/bukov-package-selftest.XXXXXX")"
  local self_output="$self_root/output"
  mkdir "$self_output"
  self_output="${self_output:A}"
  safe_output_root "$self_output" \
    || fail "safe output root was rejected"
  ! safe_output_root "/" \
    || fail "filesystem root was accepted as an output root"
  ! safe_output_root "$HOME" \
    || fail "home directory was accepted as an output root"

  local safe_target="${self_output}/逃离布科夫-alpha16-test"
  direct_child "$self_output" "$safe_target" \
    || fail "direct target child was rejected"
  ! direct_child "$self_output" "${self_output}/nested/version" \
    || fail "nested target was accepted"
  target_available "$safe_target" \
    || fail "unused target was reported as occupied"
  mkdir "$safe_target"
  ! target_available "$safe_target" \
    || fail "existing target was reported as available"

  local fake_commit="0123456789abcdef0123456789abcdef01234567"
  valid_build_identity \
      "$fake_commit" clean "$fake_commit" clean "$fake_commit" clean \
    || fail "matching clean build identities were rejected"
  ! valid_build_identity \
      "$fake_commit" dirty "$fake_commit" clean "$fake_commit" clean \
    || fail "dirty source worktree identity was accepted"
  ! valid_build_identity \
      "$fake_commit" clean "$fake_commit" clean \
      "1123456789abcdef0123456789abcdef01234567" clean \
    || fail "mismatched iOS source commit was accepted"

  [[ "${self_root:A:h}" == "${${TMPDIR:-/tmp}:A}" ]] \
    || fail "self-test temporary directory escaped TMPDIR"
  rm -rf -- "$self_root"
  print "PASS: zsh syntax and package path-safety checks"
}

while (( $# > 0 )); do
  case "$1" in
    --output)
      (( $# >= 2 )) || fail "--output requires a value"
      output_arg="$2"
      shift 2
      ;;
    --version)
      (( $# >= 2 )) || fail "--version requires a value"
      version="$2"
      shift 2
      ;;
    --cache)
      (( $# >= 2 )) || fail "--cache requires a value"
      cache_arg="$2"
      shift 2
      ;;
    --dry-run)
      [[ -z "$mode_seen" ]] || fail "--dry-run and --apply are mutually exclusive"
      mode_seen="dry-run"
      apply=false
      shift
      ;;
    --apply)
      [[ -z "$mode_seen" ]] || fail "--dry-run and --apply are mutually exclusive"
      mode_seen="apply"
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
  [[ -z "$output_arg" && -z "$version" && "$cache_arg" == "$default_cache_root" ]] \
    || fail "--self-test cannot be combined with packaging options"
  run_self_test
  exit 0
fi

[[ -n "$output_arg" ]] || fail "--output is required"
[[ -n "$version" ]] || fail "--version is required"
valid_version "$version" \
  || fail "--version must be a path-free ASCII slug and cannot contain '..'"
safe_output_root "$output_arg" \
  || fail "--output must be an existing directory other than / or HOME"
output_dir="${output_arg:A}"

[[ -d "$cache_arg" ]] || fail "apple-gradle cache does not exist: $cache_arg"
cache_root="${cache_arg:A}"
mac_source="${cache_root}/desktop/jpackage/逃离布科夫.app"
ios_source="${cache_root}/ios/robovm.tmp/IOSLauncher.app"
[[ -d "$mac_source" ]] || fail "cached macOS app is missing: $mac_source"
[[ -d "$ios_source" ]] || fail "cached iOS Simulator app is missing: $ios_source"
mac_source="${mac_source:A}"
ios_source="${ios_source:A}"
[[ "$mac_source" == "$cache_root/"* ]] \
  || fail "macOS source escaped the apple-gradle cache"
[[ "$ios_source" == "$cache_root/"* ]] \
  || fail "iOS source escaped the apple-gradle cache"

target_dir="${output_dir}/逃离布科夫-${version}"
target_dir="${target_dir:A}"
direct_child "$output_dir" "$target_dir" \
  || fail "version target must be a direct child of --output"
target_available "$target_dir" \
  || fail "version already exists; refusing to overwrite: $target_dir"

for command_path in \
    /usr/bin/awk \
    /bin/date \
    /usr/bin/codesign \
    /usr/bin/ditto \
    /usr/bin/file \
    /usr/bin/find \
    /usr/bin/grep \
    /usr/bin/shasum \
    /usr/bin/sort \
    /usr/bin/stat \
    /usr/bin/touch \
    /usr/bin/unzip \
    /usr/bin/xattr \
    /usr/bin/zip \
    /usr/libexec/PlistBuddy; do
  [[ -x "$command_path" ]] || fail "required tool is unavailable: $command_path"
done

mac_plist="${mac_source}/Contents/Info.plist"
ios_plist="${ios_source}/Info.plist"
[[ -f "$mac_plist" ]] || fail "macOS Info.plist is missing"
[[ -f "$ios_plist" ]] || fail "iOS Info.plist is missing"
mac_executable="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$mac_plist"
)" || fail "macOS CFBundleExecutable is missing"
ios_executable="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$ios_plist"
)" || fail "iOS CFBundleExecutable is missing"
ios_platform="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleSupportedPlatforms:0' "$ios_plist"
)" || fail "iOS CFBundleSupportedPlatforms is missing"
[[ "$ios_platform" == "iPhoneSimulator" ]] \
  || fail "cached iOS app is not a Simulator build: $ios_platform"
[[ -f "${mac_source}/Contents/MacOS/${mac_executable}" ]] \
  || fail "macOS executable is missing from cached app"
[[ -f "${ios_source}/${ios_executable}" ]] \
  || fail "iOS executable is missing from cached app"

git_commit="$(
  git -C "$project_root" rev-parse HEAD 2>/dev/null
)" || fail "could not resolve the source Git commit"
git_state=clean
if [[ -n "$(git -C "$project_root" status --porcelain --untracked-files=all)" ]]; then
  git_state=dirty
fi

typeset -a mac_identity_jars
mac_identity_jars=()
while IFS= read -r identity_jar; do
  mac_identity_jars+=("$identity_jar")
done < <(
  /usr/bin/find "${mac_source}/Contents/app" -maxdepth 1 -type f \
    -name 'desktop-*.jar' -print
)
(( ${#mac_identity_jars} == 1 )) \
  || fail "expected exactly one macOS desktop jar, found ${#mac_identity_jars}"
mac_identity="$(
  /usr/bin/unzip -p "${mac_identity_jars[1]}" bukov-build-identity.properties
)" || fail "macOS bundle has no embedded build identity"
mac_build_commit="$(
  print -r -- "$mac_identity" \
    | /usr/bin/awk -F= '$1 == "source_commit" { print substr($0, index($0, "=") + 1); exit }'
)"
mac_build_state="$(
  print -r -- "$mac_identity" \
    | /usr/bin/awk -F= '$1 == "source_worktree" { print substr($0, index($0, "=") + 1); exit }'
)"
ios_build_commit="$(
  /usr/libexec/PlistBuddy -c 'Print :BukovSourceCommit' "$ios_plist"
)" || fail "iOS bundle has no embedded source commit"
ios_build_state="$(
  /usr/libexec/PlistBuddy -c 'Print :BukovSourceWorktree' "$ios_plist"
)" || fail "iOS bundle has no embedded source worktree state"

valid_build_identity \
    "$git_commit" "$git_state" \
    "$mac_build_commit" "$mac_build_state" \
    "$ios_build_commit" "$ios_build_state" \
  || fail "cached bundles are not clean builds of current HEAD: current=${git_commit}/${git_state}, macOS=${mac_build_commit}/${mac_build_state}, iOS=${ios_build_commit}/${ios_build_state}"

print "Mode:       $([[ "$apply" == true ]] && print apply || print dry-run)"
print "Cache:      $cache_root"
print "macOS:      $mac_source"
print "iOS Sim:    $ios_source"
print "Output:     $output_dir"
print "Version:    $version"
print "Destination:$target_dir"
print "Source:     $git_commit ($git_state, embedded in both bundles)"

print "Checking cached source signatures..."
/usr/bin/codesign --verify --deep --strict --verbose=2 "$mac_source"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$ios_source"

if [[ "$apply" != true ]]; then
  print "Dry run complete. No files were written."
  print "Re-run with --apply to create this immutable version directory."
  exit 0
fi

lock_dir="${output_dir}/.bukov-package-${version}.lock"
direct_child "$output_dir" "$lock_dir" \
  || fail "packaging lock escaped --output"
mkdir "$lock_dir" 2>/dev/null \
  || fail "another package operation is active for version: $version"
trap cleanup EXIT
trap 'exit 130' HUP INT TERM

target_available "$target_dir" \
  || fail "version appeared during validation; refusing to overwrite: $target_dir"
staging_parent="${TMPDIR:-/tmp}"
[[ -d "$staging_parent" ]] \
  || fail "temporary directory is unavailable: $staging_parent"
staging_parent="${staging_parent:A}"
staging_dir="$(
  mktemp -d "${staging_parent}/bukov-package-${version}.staging.XXXXXX"
)"
staging_dir="${staging_dir:A}"
direct_child "$staging_parent" "$staging_dir" \
  || fail "staging directory escaped the temporary root"
[[ "${staging_dir:t}" == "bukov-package-${version}.staging."* ]] \
  || fail "unexpected staging directory name"

mac_copy="${staging_dir}/逃离布科夫.app"
ios_copy="${staging_dir}/逃离布科夫-iOS-Simulator.app"
/usr/bin/ditto --noqtn "$mac_source" "$mac_copy"
/usr/bin/ditto --noqtn "$ios_source" "$ios_copy"

/usr/bin/xattr -cr "$mac_copy"
/usr/bin/xattr -cr "$ios_copy"
for copied_app in "$mac_copy" "$ios_copy"; do
  # Finder may immediately recreate an empty FinderInfo attribute on the
  # bundle root after a recursive clear. Delete both code-signing-forbidden
  # attributes explicitly once more before signing.
  /usr/bin/xattr -dr com.apple.FinderInfo "$copied_app" 2>/dev/null || true
  /usr/bin/xattr -dr com.apple.ResourceFork "$copied_app" 2>/dev/null || true
  if /usr/bin/xattr -lr "$copied_app" 2>/dev/null \
      | /usr/bin/grep -Eq 'com\.apple\.(FinderInfo|ResourceFork)'; then
    fail "resource fork or FinderInfo remains after cleanup: $copied_app"
  fi
done

sign_args=(
  --force
  --deep
  --sign -
  --timestamp=none
  --preserve-metadata=identifier,entitlements,requirements,flags,runtime
)
/usr/bin/codesign "${sign_args[@]}" "$mac_copy"
/usr/bin/codesign "${sign_args[@]}" "$ios_copy"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$mac_copy"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$ios_copy"

mac_source_epoch="$(/usr/bin/stat -f '%m' "$mac_source")"
ios_source_epoch="$(/usr/bin/stat -f '%m' "$ios_source")"
archive_epoch="${SOURCE_DATE_EPOCH:-$mac_source_epoch}"
if [[ -z "${SOURCE_DATE_EPOCH:-}" ]] \
    && (( ios_source_epoch > mac_source_epoch )); then
  archive_epoch="$ios_source_epoch"
fi
[[ "$archive_epoch" =~ '^[1-9][0-9]*$' ]] \
  || fail "SOURCE_DATE_EPOCH must be a positive Unix timestamp"
archive_timestamp="$(/bin/date -r "$archive_epoch" '+%Y%m%d%H%M.%S')"
for copied_app in "$mac_copy" "$ios_copy"; do
  /usr/bin/find "$copied_app" -exec \
    /usr/bin/touch -h -t "$archive_timestamp" {} +
done
/usr/bin/codesign --verify --deep --strict --verbose=2 "$mac_copy"
/usr/bin/codesign --verify --deep --strict --verbose=2 "$ios_copy"

mac_archive="${staging_dir}/逃离布科夫-macOS-${version}.zip"
ios_archive="${staging_dir}/逃离布科夫-iOS-Simulator-${version}.zip"
(
  cd "$staging_dir"
  /usr/bin/find "${mac_copy:t}" -print \
    | LC_ALL=C /usr/bin/sort \
    | COPYFILE_DISABLE=1 /usr/bin/zip -q -X -y "${mac_archive:t}" -@
  /usr/bin/find "${ios_copy:t}" -print \
    | LC_ALL=C /usr/bin/sort \
    | COPYFILE_DISABLE=1 /usr/bin/zip -q -X -y "${ios_archive:t}" -@
)

archive_verify_dir="$(
  mktemp -d "${staging_dir}/.archive-verify.XXXXXX"
)"
/usr/bin/unzip -q "$mac_archive" -d "${archive_verify_dir}/macos"
/usr/bin/unzip -q "$ios_archive" -d "${archive_verify_dir}/ios"
/usr/bin/codesign --verify --deep --strict --verbose=2 \
  "${archive_verify_dir}/macos/${mac_copy:t}"
/usr/bin/codesign --verify --deep --strict --verbose=2 \
  "${archive_verify_dir}/ios/${ios_copy:t}"
[[ "${archive_verify_dir:A:h}" == "$staging_dir" ]] \
  || fail "archive verification directory escaped staging"
[[ "${archive_verify_dir:t}" == ".archive-verify."* ]] \
  || fail "unexpected archive verification directory"
rm -rf -- "$archive_verify_dir"

mac_copy_executable="${mac_copy}/Contents/MacOS/${mac_executable}"
ios_copy_executable="${ios_copy}/${ios_executable}"
mac_executable_hash="$(
  /usr/bin/shasum -a 256 "$mac_copy_executable"
)"
mac_executable_hash="${mac_executable_hash%% *}"
ios_executable_hash="$(
  /usr/bin/shasum -a 256 "$ios_copy_executable"
)"
ios_executable_hash="${ios_executable_hash%% *}"
hashes_file="${staging_dir}/SHA256SUMS.txt"
(
  cd "$staging_dir"
  /usr/bin/shasum -a 256 \
    "${mac_archive:t}" \
    "${ios_archive:t}"
) > "$hashes_file"

package_info="${staging_dir}/PACKAGE_INFO.txt"
{
  print "product=逃离布科夫 / Escape from Bukov"
  print "version=$version"
  print "source_commit=$git_commit"
  print "source_worktree=$git_state"
  print "apple_gradle_cache=$cache_root"
  print "macos_source=$mac_source"
  print "ios_simulator_source=$ios_source"
  print "signing=ad-hoc"
  print "source_date_epoch=$archive_epoch"
  print "distribution=verified-archives-only"
  print "macos_archive=${mac_archive:t}"
  print "ios_simulator_archive=${ios_archive:t}"
  print "macos_executable_sha256=$mac_executable_hash"
  print "ios_simulator_executable_sha256=$ios_executable_hash"
  print "hashes=SHA256SUMS.txt"
} > "$package_info"

# Do not publish loose bundles into a FileProvider-backed output directory.
# Their verified ZIPs are the immutable distribution artifacts.
for copied_app in "$mac_copy" "$ios_copy"; do
  [[ "${copied_app:A:h}" == "$staging_dir" ]] \
    || fail "refusing to remove staged bundle outside staging: $copied_app"
  rm -rf -- "$copied_app"
done

target_available "$target_dir" \
  || fail "version appeared while packaging; refusing to overwrite: $target_dir"
mv "$staging_dir" "$target_dir"
staging_dir=""
rmdir "$lock_dir"
lock_dir=""
trap - EXIT HUP INT TERM

print "Published immutable personal build:"
print "  $target_dir"
print "SHA-256:"
while IFS= read -r hash_line; do
  print "  $hash_line"
done < "${target_dir}/SHA256SUMS.txt"
