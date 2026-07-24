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

Keeps exactly the named Escape from Bukov version directory and finds older
direct children of DIR matching:
  逃离布科夫-alpha*
  逃离布科夫-v*
  逃离布科夫-[numeric version]*

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

keep_name="${keep_dir:t}"
case "$keep_name" in
  逃离布科夫-alpha*|逃离布科夫-v*|逃离布科夫-[0-9]*)
    ;;
  *)
    fail "--keep does not match an Escape from Bukov version directory: $keep_name"
    ;;
esac

typeset -a candidates
candidates=()
while IFS= read -r -d '' candidate; do
  candidate="${candidate:A}"
  [[ "$candidate" == "$keep_dir" ]] && continue
  [[ "${candidate:h}" == "$output_dir" ]] \
    || fail "refusing candidate outside output directory: $candidate"
  candidates+=("$candidate")
done < <(
  find "$output_dir" -mindepth 1 -maxdepth 1 -type d \
    \( -name '逃离布科夫-alpha*' -o -name '逃离布科夫-v*' \
       -o -name '逃离布科夫-[0-9]*' \) \
    -print0
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
