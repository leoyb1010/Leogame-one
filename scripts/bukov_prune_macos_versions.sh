#!/bin/zsh
set -euo pipefail

default_output_dir="$HOME/Documents/日常/output"
output_dir="$default_output_dir"
keep_arg=""
apply=false

usage() {
  cat <<'USAGE'
Usage:
  scripts/bukov_prune_macos_versions.sh --keep VERSION_DIR [--output DIR] [--apply]

Keeps exactly the named Escape from Bukov package directory and finds older
direct children of DIR named 逃离布科夫-* that contain a valid archive-only
PACKAGE_INFO.txt and SHA256SUMS.txt. Evidence/report directories are ignored.

The default is a dry run. With --apply, old version directories are moved to a
new timestamped folder under ~/.Trash after their .app bundles are unregistered
from LaunchServices when possible. Nothing is permanently deleted.

Options:
  --keep VERSION_DIR  Required. Directory name or absolute path to keep.
  --output DIR        Version output directory (default: ~/Documents/日常/output).
  --apply             Perform the recoverable move to Trash.
  -h, --help          Show this help.
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

is_personal_package_dir() {
  local candidate="$1"
  local package_info="${candidate}/PACKAGE_INFO.txt"
  [[ -d "$candidate" && ! -L "$candidate" ]] || return 1
  [[ "${candidate:t}" == 逃离布科夫-* ]] || return 1
  [[ -f "$package_info" && -f "${candidate}/SHA256SUMS.txt" ]] || return 1
  [[ "$(info_value product "$package_info")" == \
      "逃离布科夫 / Escape from Bukov" ]] || return 1
  [[ "$(info_value distribution "$package_info")" == \
      verified-archives-only ]] || return 1
  [[ "$(info_value source_commit "$package_info")" =~ \
      '^[0-9a-f]{40}$' ]]
}

while (( $# > 0 )); do
  case "$1" in
    --keep)
      (( $# >= 2 )) || fail "--keep requires a value"
      keep_arg="$2"
      shift 2
      ;;
    --output)
      (( $# >= 2 )) || fail "--output requires a value"
      output_dir="$2"
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

[[ -n "$keep_arg" ]] || fail "--keep is required"
[[ -d "$output_dir" ]] || fail "output directory does not exist: $output_dir"

output_dir="${output_dir:A}"
if [[ "$keep_arg" = /* ]]; then
  keep_dir="${keep_arg:A}"
else
  keep_dir="${output_dir}/${keep_arg}"
  keep_dir="${keep_dir:A}"
fi

[[ "${keep_dir:h}" == "$output_dir" ]] \
  || fail "--keep must be a direct child of $output_dir"
[[ -d "$keep_dir" ]] || fail "version to keep does not exist: $keep_dir"
is_personal_package_dir "$keep_dir" \
  || fail "--keep is not a valid archive-only Escape from Bukov package: ${keep_dir:t}"

typeset -a candidates
candidates=()
while IFS= read -r -d '' candidate; do
  candidate="${candidate:A}"
  [[ "$candidate" == "$keep_dir" ]] && continue
  is_personal_package_dir "$candidate" || continue
  [[ "${candidate:h}" == "$output_dir" ]] \
    || fail "refusing candidate outside output directory: $candidate"
  candidates+=("$candidate")
done < <(
  find "$output_dir" -mindepth 1 -maxdepth 1 -type d \
    -name '逃离布科夫-*' -print0
)

print "Output: $output_dir"
print "Keep:   $keep_dir"
if (( ${#candidates} == 0 )); then
  print "No older matching version directories found."
  exit 0
fi

print "Older versions (${#candidates}):"
for candidate in "${candidates[@]}"; do
  print "  $candidate"
done

if [[ "$apply" != true ]]; then
  print "Dry run only. Re-run with --apply to move these directories to Trash."
  exit 0
fi

trash_batch="$HOME/.Trash/逃离布科夫-旧版本-$(date +%Y%m%d-%H%M%S)-$$"
[[ ! -e "$trash_batch" ]] || fail "Trash batch already exists: $trash_batch"
mkdir -p "$trash_batch"

lsregister="/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister"
moved=0
for candidate in "${candidates[@]}"; do
  if [[ -x "$lsregister" ]]; then
    while IFS= read -r -d '' app_bundle; do
      "$lsregister" -u "$app_bundle" >/dev/null 2>&1 \
        || print -u2 "WARNING: could not unregister: $app_bundle"
    done < <(find "$candidate" -type d -name '*.app' -prune -print0)
  fi

  destination="$trash_batch/${candidate:t}"
  [[ ! -e "$destination" ]] \
    || fail "refusing to overwrite Trash destination: $destination"
  mv "$candidate" "$destination"
  (( moved += 1 ))
done

print "Moved $moved older version directories to:"
print "  $trash_batch"
print "Kept latest version:"
print "  $keep_dir"
