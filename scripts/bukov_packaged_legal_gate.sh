#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
	echo "usage: $0 /path/to/逃离布科夫.app /path/to/IOSLauncher.app" >&2
	exit 2
fi

mac_app=$1
ios_app=$2
mac_jar="$mac_app/Contents/app/desktop-2.0.0.jar"

test -f "$mac_jar"
test -s "$ios_app/legal/LICENSE.txt"
test -s "$ios_app/legal/THIRD_PARTY_NOTICES.txt"

jar tf "$mac_jar" | grep -Fxq 'legal/LICENSE.txt'
jar tf "$mac_jar" | grep -Fxq 'legal/THIRD_PARTY_NOTICES.txt'

printf 'Bukov packaged legal gate passed for macOS and iOS.\n'
