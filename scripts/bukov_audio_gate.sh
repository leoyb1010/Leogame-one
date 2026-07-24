#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
world="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/runtime/BukovRealtimeWorld.java"
fire_control="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov/combat/FireControl.java"
assets="$repo_root/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java"
sound_dir="$repo_root/core/src/main/assets/sounds/bukov"
firearms="$repo_root/core/src/main/assets/bukov/content/firearms.json"
provenance="$repo_root/artwork/licenses/ASSET_PROVENANCE.csv"

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

if rg -F --quiet 'Assets.Sounds.Bukov.GUNSHOT_PLAYER' "$world"; then
  echo "Bukov audio gate: player still uses the shared gunshot asset" >&2
  exit 1
fi

if rg -F --quiet 'Assets.Sounds.Bukov.RELOAD_FINISH' "$world" \
  || [[ "$(rg -F -c 'Assets.Sounds.Bukov.RELOAD_START' "$world")" != "1" ]]; then
  echo "Bukov audio gate: legacy reload cues must remain enemy fallback only" >&2
  exit 1
fi

for required in \
  'SpatialAudioModel.resolve(' \
  'aiSoundSpatial.perceivable()' \
  'GunshotAudioResolver.resolve(' \
  'definition.audioProfile.gunshotFamily.mechanicalAsset(sequence)' \
  'definition.audioProfile.gunshotFamily.bodyAsset(sequence)' \
  'GunshotAcousticSpaceResolver.resolve(' \
  'acousticSpace.tailAsset(sequence)' \
  'playPlayerGunshotLayers(' \
  'void reloadAudioCues(' \
  'ReloadAudioCue.values()' \
  'cue.asset()' \
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

for required in \
  'ReloadAudioCueResolver.crossed(' \
  'sink.reloadAudioCues(definition, cueMask);' \
  'public void cancelReload()' \
  'cancelReload();'
do
  if ! rg -F --quiet "$required" "$fire_control"; then
    echo "Bukov audio gate: fixed-step reload wiring missing: $required" >&2
    exit 1
  fi
done

random_float_count="$(rg -F -c 'Random.Float(' "$world")"
if [[ "$random_float_count" != "1" ]] \
  || ! rg -F --quiet 'Random.Float(-spread, spread)' "$world"; then
  echo "Bukov audio gate: audio must not consume gameplay RNG" >&2
  exit 1
fi

python3 - "$firearms" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as source:
    firearms = json.load(source)["firearms"]

if len(firearms) != 18:
    raise SystemExit(
        f"Bukov audio gate: expected 18 firearm profiles, got {len(firearms)}")

expected = {"PISTOL", "SMG", "CARBINE", "RIFLE", "SHOTGUN", "HEAVY"}
actual = set()
for firearm in firearms:
    audio = firearm.get("audio")
    if not isinstance(audio, dict):
        raise SystemExit(
            f"Bukov audio gate: missing firearm audio: {firearm['id']}")
    actual.add(audio.get("gunshotFamily"))
    cues = audio.get("reloadCueFractions", {})
    values = [cues.get("magOut"), cues.get("magIn"), cues.get("charge")]
    if not all(isinstance(value, (int, float)) for value in values):
        raise SystemExit(
            f"Bukov audio gate: incomplete reload cues: {firearm['id']}")
    if not (0 < values[0] < values[1] < values[2] < 1):
        raise SystemExit(
            f"Bukov audio gate: unordered reload cues: {firearm['id']}")
if actual != expected:
    raise SystemExit(
        f"Bukov audio gate: firearm family coverage {actual} != {expected}")
PY

expected=(
  "gunshot_player.wav:8939860ecd0ac24507fd320289eebaa57ec1c3c0a7975e6fbfd10efa3127cf83"
  "gunshot_enemy.wav:0364d57714ed8c690f8321b236facadca6d7525a041e2de2d3fc130e3636dfd9"
  "gunshot_pistol.wav:1289f58ab56f28b4d430d2a5ac9633d93acad948eca1da87493a4b556a12781d"
  "gunshot_smg.wav:84d29254d42511c370b24255db083797e613c4d6aced18dbd33f3007e8fab20d"
  "gunshot_carbine.wav:801c79050fee1fe890ee8a8bd9863da44f1d09abbf695bcce89474909059730b"
  "gunshot_rifle.wav:d0800cb6e86912751cf68b44d04bfd5c1e9d5fa067a3119ce7c368ddd0374b88"
  "gunshot_shotgun.wav:d46ca3991fe642a4ade34770ff19701ccaf2f4770949482f06d8fe4ab7100ecf"
  "gunshot_heavy.wav:f1002ebdf2275d975638814810a49d7b9eea1d407cb67e6a049f33e4fae99dc4"
  "bullet_hit.wav:4d938255e0d1030e880953885b4b51d56135d4392cc539a8dd26512a4e53e600"
  "contact_hit.wav:9207d4a2a5114c71c53f6160fd5f25f57b4d68d39b6f27fdfca17da202221a43"
  "dry_fire.wav:4286d982c4eaa3f6455a02958c5b17de24b08734287fc7a7a71bdd4f1151a1aa"
  "reload_start.wav:ab6d41ec0fc70786b4d7675b0b01da111e17379a5442eacaf161581666e2468d"
  "reload_finish.wav:9f10892b735ed68d058f48307d51ccd0c26289e73ddce81850b4597747a917eb"
  "reload_mag_out.wav:d8b6bbeba084577ff80436248a9f357fa72cfade0bd26cc0fdbb09a5f02decef"
  "reload_mag_in.wav:0a39a4555d0e791a2fc2cdc5cd2f991f430c648533a07cd092e08a92c2100699"
  "reload_charge.wav:21c5fc326c6b8881ad7c0533b274b3a78c5b1143ed020642352534ecdf9d337e"
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
  "gunshot_pistol_mechanical_1.wav:0ca692eac88e7190b55c0046719451caaeb908387e15f1be6ffd15665b5a8dd2"
  "gunshot_pistol_mechanical_2.wav:2c4cbc678fe8bcf5f33bf723bca319f709503f6a74265f6f07a7517523738136"
  "gunshot_pistol_mechanical_3.wav:5bee4b0c101c7db7a57def4a9aea806c29af30531f905560669dbf42f960e2c2"
  "gunshot_pistol_body_1.wav:ce2be2d42eb041fca2bd9e7b2cb9107a2d65d85b07da39e8d26ea7e6944323ec"
  "gunshot_pistol_body_2.wav:bc10701a643f26eb718749454e6ec950320668af0910b76e1005f8e7ebacfcfc"
  "gunshot_pistol_body_3.wav:1d9ae475b539219671d59db5f0fab994e81e8855fca59b101ef18e5bcea02a5f"
  "gunshot_smg_mechanical_1.wav:a1535ac8e26f63adf652885e4deb751fe1e2bbd7670f0f99e304fb67afd7e8cb"
  "gunshot_smg_mechanical_2.wav:e82f7277b3dc1497a6b90748fce2729153802b6014e614c0b527d34a0d070f86"
  "gunshot_smg_mechanical_3.wav:0f5023f8a873776aeac70817e7c3649b1f245bfa58a32e9b082cc019cc0a98bc"
  "gunshot_smg_body_1.wav:b6fccc005335cb7c6ea92112612b66d35ad009590497dfe40eab4e8e2a60e942"
  "gunshot_smg_body_2.wav:bb2102c0c2ad05d52ceb19daa9a1c16642b20164d8a8cfbf4f0be833d85b5f08"
  "gunshot_smg_body_3.wav:c03022513b4be120e8ec9260a312a2c7483310378a17bf3ceb575507ea3d7826"
  "gunshot_carbine_mechanical_1.wav:fad8b7381275391623d181dcc69092002e52fbd0e383dfbe763cfce56f757dc1"
  "gunshot_carbine_mechanical_2.wav:9ab95e5590c05a67a28168c606765941f93a86067978ed4659907e728a8cfff6"
  "gunshot_carbine_mechanical_3.wav:05357cf48396dfa56cb8bf42a7218ffc0205ab6693e4af6a92c04c8083bd8e5c"
  "gunshot_carbine_body_1.wav:d5842c82b75b876945525cec490a6b0c57743d79ebe8c799b54a614361a23ab4"
  "gunshot_carbine_body_2.wav:17087ef4b0e08676f9c102e61c02afd576d0e2c0011bd05d3803fe052a4bd77f"
  "gunshot_carbine_body_3.wav:e93615bb247ae38b05e888a2d3533e66f5328fe2245765c1239ec7570fefb368"
  "gunshot_rifle_mechanical_1.wav:7a9ea5371b6ae985e43e4656ce7c0f18613e768674e33afda0816a664fd3061b"
  "gunshot_rifle_mechanical_2.wav:34d2ecbe380cd6b7670a62a82b66f1747f400f134ee9e11760f17d021a3da596"
  "gunshot_rifle_mechanical_3.wav:71ec15113c500a20bb99dd3355cbc69cfa37d0c76db791c1d073e87be1a8eb5b"
  "gunshot_rifle_body_1.wav:38b3c94dd964425f603ebd850d1d5a9c895eefdbf612fe897c202588b863bf97"
  "gunshot_rifle_body_2.wav:e50e4671e3a9d966d4d7c0840716a36521b51e5efb58b6a0e5741dc6c1dd874e"
  "gunshot_rifle_body_3.wav:1ea1ee0c5b33c2ec09ade40dfc41ea888631202274c89289080e882f8030f663"
  "gunshot_shotgun_mechanical_1.wav:a727782d028a8bb20bed85d0bb0afc1856a2e2a59c0b832b322178021746167d"
  "gunshot_shotgun_mechanical_2.wav:b18d770a73b6f87200ce35f23d2796278f9fc1bac987f9a0de47802016ef256a"
  "gunshot_shotgun_mechanical_3.wav:fca99a60a19e05ce6c78ca1165c35d05864be948714af5f6d308e0b826668a22"
  "gunshot_shotgun_body_1.wav:c775550db2d37dbdf1d47917b8fb365e8580a5347dd34d5fb133dcabcf4f4fb0"
  "gunshot_shotgun_body_2.wav:2c8acce5dd9747269e1ee439a5897330f75586f0453b3718e15de5f96e6373f2"
  "gunshot_shotgun_body_3.wav:1ea621530b05108e591758228d96ec932be814dca6462e145c3c5fd80b054210"
  "gunshot_heavy_mechanical_1.wav:4097cc2298c8d9c20783da0184455a1480a4350671dbfb0789cdabbe4c61f796"
  "gunshot_heavy_mechanical_2.wav:06c0a531c36f7f741e7695bb4d1e2021afdd5803892032434d3f11942b9d3db4"
  "gunshot_heavy_mechanical_3.wav:d64ab18ad67beb9d7a72b1d85fe86bcc2d762e841167f778d9945d801bbe5e36"
  "gunshot_heavy_body_1.wav:53993456c63854e9e76c7c4c8fcd4911b02c14fc95bce67929be6743a7c07442"
  "gunshot_heavy_body_2.wav:0d1f4dbab1a4db0af76e1efd46947a3272c65b50242192d9510cc2232589fb33"
  "gunshot_heavy_body_3.wav:7310d047c8302cb2534c0f9c858909dd5aec746193a01b7d7fcfcce250cbfef7"
  "gunshot_tail_indoor_1.wav:76fc29d671b429104d165e377ce2fc996352fcb01db9e638af559d6c14bbcf6f"
  "gunshot_tail_indoor_2.wav:b2718310c40055d410437523922fd10fbefb9e58b6e1357aa0fb64733d40536a"
  "gunshot_tail_indoor_3.wav:8e221e92126e1a9b225fee9de0224926e6b4675030e5e6116c5825c016dae0d2"
  "gunshot_tail_corridor_1.wav:36e51a744e48564fdc4077b452564838b31eac4afb737924f599a4f6b15c6e47"
  "gunshot_tail_corridor_2.wav:1eb31024b924b7bd448855dbbdf8fd6eafc473540853059da86575909a6fc520"
  "gunshot_tail_corridor_3.wav:cb535861c6f10254bf1452004df295765a2bbf15c2ab6f2164431b57a948e411"
  "gunshot_tail_open_1.wav:4d1e0302e9b23a91cbc8bf56e3f2f0b842bdbc2bdb14a0b38627b9381d5c248e"
  "gunshot_tail_open_2.wav:67ba37b204ac37acd5074c225303c8c5252c41dfd150860737f5a9a4988d4be7"
  "gunshot_tail_open_3.wav:7279203676453cf1d44d7fa4477a140e17aa32d389cf7023a963698a42f929e4"
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
  if ! rg -F --quiet "$asset_path" "$provenance"; then
    echo "Bukov audio gate: provenance missing for $filename" >&2
    exit 1
  fi
done

echo "Bukov audio gate: PASS (${#expected[@]} original PCM SFX; no legacy combat audio)"
