#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
legal_root="$repo_root/core/src/main/assets/legal"

test -s "$repo_root/LICENSE.txt"
test -s "$repo_root/THIRD_PARTY_NOTICES.md"
test -s "$legal_root/LICENSE.txt"
test -s "$legal_root/THIRD_PARTY_NOTICES.txt"

cmp "$repo_root/LICENSE.txt" "$legal_root/LICENSE.txt"
grep -Fq 'https://github.com/leoyb1010/Leogame-one' \
	"$legal_root/THIRD_PARTY_NOTICES.txt"
grep -Fq 'GNU General Public License' \
	"$legal_root/THIRD_PARTY_NOTICES.txt"

printf 'Bukov legal bundle gate passed: GPL text and notices are packaged.\n'
