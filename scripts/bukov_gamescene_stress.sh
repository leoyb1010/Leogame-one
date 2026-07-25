#!/bin/zsh
set -euo pipefail

script_dir=${0:A:h}
project_root=${script_dir:h}
frames=${1:-600}
report="$project_root/build/reports/bukov-gamescene-stress.log"

[[ "$frames" == <-> && "$frames" -gt 0 ]] || {
  print -u2 "usage: $0 [positive-render-frame-count]"
  exit 2
}

mkdir -p "${report:h}"
{
  print "gate=bukov_gamescene_production_stress"
  print "headless=true"
  print "gpu_rendered=false"
  print "rendered_fps_claim=false"
  print "frames=$frames"
} | tee "$report"

"$script_dir/apple-gradle" \
  core:test \
  --tests \
  'com.shatteredpixel.shatteredpixeldungeon.bukov.performance.BukovGameSceneProductionStressTest' \
  -Dbukov.gamescene.stress.frames="$frames" \
  --rerun-tasks \
  --no-daemon 2>&1 | tee -a "$report"

print "report=$report"
