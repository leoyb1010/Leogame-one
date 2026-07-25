#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "$0")/.." && pwd)"
test_dir="$(mktemp -d "${TMPDIR:-/tmp}/bukov-audio-test.XXXXXX")"
trap 'rm -rf "$test_dir"' EXIT

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
  java_home="$JAVA_HOME"
elif [[ -x /opt/homebrew/bin/brew ]]; then
  java_home="$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home"
else
  java_home=""
fi
javac_cmd="${java_home:+$java_home/bin/}javac"
java_cmd="${java_home:+$java_home/bin/}java"

"$javac_cmd" -d "$test_dir" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/BukovNumbers.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/Terrain.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/levels/ThemeEnvironmentRules.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/AudioChannel.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovAudioBusMix.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovAtmosphereSignal.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovAtmosphereController.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/SoundConcurrencyBudget.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovSoundPlaybackSink.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovSoundConcurrencyRuntime.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovConcurrentSoundPlayer.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/FootstepCadence.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/FootstepSurface.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/SoundCategory.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/SpatialAudioModel.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotAudioPlan.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotVariantResolver.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotAudioResolver.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotAcousticSpace.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotAcousticSpaceResolver.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/GunshotSoundFamily.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/KeySoundVisualEvent.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/KeySoundVisualizationResolver.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/CombatFeedbackType.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/CombatFeedbackPlan.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/CombatFeedbackRequest.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/fx/CombatFeedbackResolver.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/settings/BukovExperienceSettings.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/settings/ExperienceContract.java" \
  "$repo_dir/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/CollisionMap.java" \
  "$repo_dir/core/src/test/java/com/shatteredpixel/shatteredpixeldungeon/bukov/audio/BukovAudioModelStandaloneTest.java"

"$java_cmd" -cp "$test_dir" \
  com.shatteredpixel.shatteredpixeldungeon.bukov.audio.BukovAudioModelStandaloneTest
