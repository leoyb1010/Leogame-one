#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
	echo "usage: $0 /path/to/逃离布科夫.app /path/to/IOSLauncher.app" >&2
	exit 2
fi

mac_app=$1
ios_app=$2

mac_jar_count=$(find "$mac_app/Contents/app" -maxdepth 1 -type f \
	-name 'desktop-*.jar' | wc -l | tr -d ' ')
test "$mac_jar_count" -eq 1
mac_jar=$(find "$mac_app/Contents/app" -maxdepth 1 -type f \
	-name 'desktop-*.jar')
test -s "$ios_app/legal/LICENSE.txt"
test -s "$ios_app/legal/THIRD_PARTY_NOTICES.txt"

/usr/bin/unzip -Z1 "$mac_jar" | grep -Fxq 'legal/LICENSE.txt'
/usr/bin/unzip -Z1 "$mac_jar" | grep -Fxq 'legal/THIRD_PARTY_NOTICES.txt'
if /usr/bin/unzip -Z1 "$mac_jar" | grep -Eq \
	'(^|/)(leo_[^/]*\.(png|jpg)|(LeoIdentityConfig|LeoStyledButton|LeoChanges|WndLeoWelcome)\.class)$'; then
	echo "retired Leo player-facing resource found in macOS package" >&2
	exit 1
fi

if find "$ios_app" -type f | grep -Eq \
	'/(leo_[^/]*\.(png|jpg)|(LeoIdentityConfig|LeoStyledButton|LeoChanges|WndLeoWelcome)\.class)$'; then
	echo "retired Leo player-facing resource found in iOS package" >&2
	exit 1
fi

printf 'Bukov packaged legal and retired-brand gate passed for macOS and iOS.\n'
