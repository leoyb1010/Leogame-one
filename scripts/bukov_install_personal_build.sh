#!/bin/zsh
set -euo pipefail

package_arg=""
apply=false
install_root="$HOME/Applications"
mac_target=""
verify_root=""
old_mac_backup=""
new_mac_installed=false

usage() {
  cat <<'USAGE'
Usage:
  scripts/bukov_install_personal_build.sh \
    --package /absolute/逃离布科夫-VERSION \
    [--apply]

Verifies an archive-only personal build, then installs exactly that build to
~/Applications and the currently booted iOS Simulator. The default is a dry
run that performs the same archive, signature, source-identity, and executable
hash checks without changing either installation.

With --apply, an existing ~/Applications/逃离布科夫.app is moved to a unique
folder in ~/.Trash before the verified replacement is installed. It is never
permanently deleted.
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

cleanup() {
  local exit_code=$?
  if [[ -n "$verify_root" && -d "$verify_root" ]]; then
    [[ "${verify_root:A:h}" == "${${TMPDIR:-/tmp}:A}" ]] \
      && [[ "${verify_root:t}" == bukov-install-verify.* ]] \
      && rm -rf -- "$verify_root"
  fi
  if (( exit_code != 0 )) && [[ "$new_mac_installed" != true ]]; then
    if [[ -n "$mac_target" && -d "$mac_target" ]]; then
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
    --apply)
      apply=true
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

[[ -n "$package_arg" ]] || fail "--package is required"
[[ -d "$package_arg" ]] || fail "package directory does not exist: $package_arg"
package_dir="${package_arg:A}"
[[ "${package_dir:t}" == 逃离布科夫-* ]] \
  || fail "unexpected package directory name: ${package_dir:t}"

for command_path in \
    /usr/bin/awk \
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

simulator_state="$(
  /usr/bin/xcrun simctl list devices booted
)"
[[ "$simulator_state" == *"(Booted)"* ]] \
  || fail "no booted iOS Simulator is available"

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
new_mac_installed=true

/usr/bin/xcrun simctl install booted "$ios_app"
bundle_id="$(
  /usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "${ios_app}/Info.plist"
)"
installed_ios_app="$(
  /usr/bin/xcrun simctl get_app_container booted "$bundle_id" app
)"
[[ -d "$installed_ios_app" ]] || fail "installed iOS app container is missing"
installed_ios_hash="$(
  /usr/bin/shasum -a 256 "${installed_ios_app}/${ios_executable}"
)"
installed_ios_hash="${installed_ios_hash%% *}"
[[ "$installed_ios_hash" == "$ios_expected_hash" ]] \
  || fail "installed iOS executable differs from verified package"

old_mac_backup=""
print "Installed verified latest build:"
print "  macOS=$mac_target"
print "  iOS Simulator=$installed_ios_app"
print "  source_commit=$source_commit"
