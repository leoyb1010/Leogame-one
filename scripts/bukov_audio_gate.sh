#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
world="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
assets="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java"
sound_dir="$repo_root/core/src/main/assets/sounds/bukov"

for forbidden in \
  'Assets.Sounds.ATK_CROSSBOW' \
  'Assets.Sounds.ATK_SPIRITBOW' \
  'Assets.Sounds.HIT_ARROW' \
  'Assets.Sounds.CLICK' \
  'equippedFirearm.hitSound' \
  'attacker.hitSound'
do
  if rg -F --quiet "$forbidden" "$world"; then
    echo "Bukov audio gate: legacy sound reference remains: $forbidden" >&2
    exit 1
  fi
done

if rg --pcre2 --quiet 'Assets\\.Sounds\\.(?!Bukov\\.)' "$world"; then
  echo "Bukov audio gate: realtime world uses a non-Bukov sound constant" >&2
  rg --pcre2 -n 'Assets\\.Sounds\\.(?!Bukov\\.)' "$world" >&2
  exit 1
fi

for required in \
  'SpatialAudioModel.resolve(' \
  'aiSoundSpatial.perceivable()' \
  'GunshotAudioResolver.resolve(' \
  'implements RealtimeRaidSystem.World' \
  'KeySoundVisualizationSource' \
  'readKeySoundVisualEvent(' \
  'Assets.Sounds.Bukov.SEARCH_COMPLETE' \
  'Assets.Sounds.Bukov.GATE_UNLOCK' \
  'Assets.Sounds.Bukov.EXTRACTION_START' \
  'Assets.Sounds.Bukov.EXTRACTION_COMPLETE'
do
  if ! rg -F --quiet "$required" "$world"; then
    echo "Bukov audio gate: runtime contract missing: $required" >&2
    exit 1
  fi
done

random_float_count="$(rg -F -c 'Random.Float(' "$world")"
if [[ "$random_float_count" != "1" ]] \
  || ! rg -F --quiet 'Random.Float(-spread, spread)' "$world"; then
  echo "Bukov audio gate: audio must not consume gameplay RNG" >&2
  exit 1
fi

expected=(
  "gunshot_player.wav:8939860ecd0ac24507fd320289eebaa57ec1c3c0a7975e6fbfd10efa3127cf83"
  "gunshot_enemy.wav:0364d57714ed8c690f8321b236facadca6d7525a041e2de2d3fc130e3636dfd9"
  "bullet_hit.wav:4d938255e0d1030e880953885b4b51d56135d4392cc539a8dd26512a4e53e600"
  "contact_hit.wav:9207d4a2a5114c71c53f6160fd5f25f57b4d68d39b6f27fdfca17da202221a43"
  "dry_fire.wav:4286d982c4eaa3f6455a02958c5b17de24b08734287fc7a7a71bdd4f1151a1aa"
  "reload_start.wav:ab6d41ec0fc70786b4d7675b0b01da111e17379a5442eacaf161581666e2468d"
  "reload_finish.wav:9f10892b735ed68d058f48307d51ccd0c26289e73ddce81850b4597747a917eb"
  "loot_pickup.wav:c65c545a8cf8012af4e39067168e328bcd3fc89adcd08c3b34fcf67a6eb54081"
  "search_complete.wav:e5d4a2de429741685fa21965797a1ec0dab40a3d9d994a790486ef3e4d759705"
  "gate_unlock.wav:3618d9de7dd2b8c405c266fb36f417f788f443366003c60782883a6337c07746"
  "extraction_start.wav:4b1478710831348c7086f4b13bcc5c5c5111b4678828453ad79c9d594a189712"
  "extraction_complete.wav:b62c7d0ffbe9a367bae62d79a2ec61936a4af440e89183f3b838215e32effd27"
  "ui_focus.wav:82db81c0a81cbf07443b3ae0bbf76c3b3f9df1633f9e1a0a92216f3c4fe423f6"
  "ui_confirm.wav:a732006019dd556eef4807ce13c32b5dff456349ebb5b0ea13abb29194c81836"
  "ui_cancel.wav:2a68b61aeab0d45c4d54fb658f5130a06e2b5df309a5b3cf32113208974cf6c4"
  "ui_error.wav:220d0e0155bd397836dc1a44d30de5ae4fda7ad63f2a4dc6c57e6b5de2283586"
  "ambience_calm.wav:3d3172edfed54f7d8c2c1b45ca925e4f0eb934c335454e2a984466e2ad3fd9a7"
  "ambience_tense.wav:36650e44c2ad61df80c5fd28cfbd7c6b3562c672aebe813fbdde2c57f324256c"
  "ambience_combat.wav:f4d8e02c916e869b52cb518ab800d435c1a208989253169e29ea2f5ad190724d"
)

for record in "${expected[@]}"; do
  filename="${record%%:*}"
  expected_hash="${record#*:}"
  sound="$sound_dir/$filename"
  if [[ ! -f "$sound" ]]; then
    echo "Bukov audio gate: missing $sound" >&2
    exit 1
  fi
  actual_hash="$(shasum -a 256 "$sound" | awk '{print $1}')"
  if [[ "$actual_hash" != "$expected_hash" ]]; then
    echo "Bukov audio gate: hash mismatch for $filename" >&2
    exit 1
  fi
  if ! file "$sound" | rg --quiet 'WAVE audio, Microsoft PCM, 16 bit, mono 48000 Hz'; then
    echo "Bukov audio gate: invalid format for $filename" >&2
    exit 1
  fi
  asset_path="sounds/bukov/$filename"
  if ! rg -F --quiet "$asset_path" "$assets"; then
    echo "Bukov audio gate: Assets constant missing for $filename" >&2
    exit 1
  fi
done

echo "Bukov audio gate: PASS (${#expected[@]} original PCM SFX; no legacy combat audio)"
