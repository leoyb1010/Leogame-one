# Bukov tactical SFX

The Bukov realtime path uses a dedicated set of short tactical sound effects.
These sounds are original procedural synthesis: seeded noise, oscillators,
envelopes, filtering, a soft limiter, and PCM encoding. No third-party
recording or legacy Leogame-one sound is sampled.

## Reproduction

```bash
./scripts/generate_bukov_sfx.mjs
./scripts/bukov_audio_gate.sh
```

The generator is deterministic under Node.js and writes mono 48 kHz, 16-bit
PCM WAV files. `bukov_audio_gate.sh` verifies format, exact SHA-256, Assets
registration, and rejects the old bow/crossbow/click/hit audio references from
`BukovRealtimeWorld`.

## Runtime mapping

| Action | Asset | SHA-256 |
|---|---|---|
| Player gunshot | `sounds/bukov/gunshot_player.wav` | `8939860ecd0ac24507fd320289eebaa57ec1c3c0a7975e6fbfd10efa3127cf83` |
| Enemy gunshot | `sounds/bukov/gunshot_enemy.wav` | `0364d57714ed8c690f8321b236facadca6d7525a041e2de2d3fc130e3636dfd9` |
| Pistol family | `sounds/bukov/gunshot_pistol.wav` | `1289f58ab56f28b4d430d2a5ac9633d93acad948eca1da87493a4b556a12781d` |
| SMG family | `sounds/bukov/gunshot_smg.wav` | `84d29254d42511c370b24255db083797e613c4d6aced18dbd33f3007e8fab20d` |
| Carbine family | `sounds/bukov/gunshot_carbine.wav` | `801c79050fee1fe890ee8a8bd9863da44f1d09abbf695bcce89474909059730b` |
| Rifle family | `sounds/bukov/gunshot_rifle.wav` | `d0800cb6e86912751cf68b44d04bfd5c1e9d5fa067a3119ce7c368ddd0374b88` |
| Shotgun family | `sounds/bukov/gunshot_shotgun.wav` | `d46ca3991fe642a4ade34770ff19701ccaf2f4770949482f06d8fe4ab7100ecf` |
| Heavy family | `sounds/bukov/gunshot_heavy.wav` | `f1002ebdf2275d975638814810a49d7b9eea1d407cb67e6a049f33e4fae99dc4` |
| Bullet impact | `sounds/bukov/bullet_hit.wav` | `4d938255e0d1030e880953885b4b51d56135d4392cc539a8dd26512a4e53e600` |
| Contact impact | `sounds/bukov/contact_hit.wav` | `9207d4a2a5114c71c53f6160fd5f25f57b4d68d39b6f27fdfca17da202221a43` |
| Empty chamber | `sounds/bukov/dry_fire.wav` | `4286d982c4eaa3f6455a02958c5b17de24b08734287fc7a7a71bdd4f1151a1aa` |
| Reload start | `sounds/bukov/reload_start.wav` | `ab6d41ec0fc70786b4d7675b0b01da111e17379a5442eacaf161581666e2468d` |
| Reload finish | `sounds/bukov/reload_finish.wav` | `9f10892b735ed68d058f48307d51ccd0c26289e73ddce81850b4597747a917eb` |
| Magazine out | `sounds/bukov/reload_mag_out.wav` | `d8b6bbeba084577ff80436248a9f357fa72cfade0bd26cc0fdbb09a5f02decef` |
| Magazine in | `sounds/bukov/reload_mag_in.wav` | `0a39a4555d0e791a2fc2cdc5cd2f991f430c648533a07cd092e08a92c2100699` |
| Charge / chamber | `sounds/bukov/reload_charge.wav` | `21c5fc326c6b8881ad7c0533b274b3a78c5b1143ed020642352534ecdf9d337e` |
| Loot pickup | `sounds/bukov/loot_pickup.wav` | `c65c545a8cf8012af4e39067168e328bcd3fc89adcd08c3b34fcf67a6eb54081` |
| Container searched | `sounds/bukov/search_complete.wav` | `e5d4a2de429741685fa21965797a1ec0dab40a3d9d994a790486ef3e4d759705` |
| Mission gate unlocked | `sounds/bukov/gate_unlock.wav` | `3618d9de7dd2b8c405c266fb36f417f788f443366003c60782883a6337c07746` |
| Extraction transponder starts | `sounds/bukov/extraction_start.wav` | `4b1478710831348c7086f4b13bcc5c5c5111b4678828453ad79c9d594a189712` |
| Extraction confirmation | `sounds/bukov/extraction_complete.wav` | `b62c7d0ffbe9a367bae62d79a2ec61936a4af440e89183f3b838215e32effd27` |
| Kill confirmation | `sounds/bukov/kill_confirm.wav` | `20897e3b5421e3815f8268295efafa96f3b98c8c3ccbbc4947005730a9450be4` |
| Boss phase break | `sounds/bukov/boss_phase_break.wav` | `301eec7ef93f19b691e10ec414705518770131c559a56ad3051bf4992b2c6547` |
| Boss slam | `sounds/bukov/boss_slam.wav` | `55cf5d2514865e614bfd3cbcd19c11e381bfe1dd17a6d695a22ee930e3d5049d` |
| Boss overload | `sounds/bukov/boss_overload.wav` | `fc3beb9ae86de0f6178e3983bebf1d8b2a78ada84d77c910df985dc223886ba5` |
| Hard footstep 1 | `sounds/bukov/footstep_hard_1.wav` | `2b9c0d8a85914502bedc62a05168c2c32f439787806e815cef55df1a4014794d` |
| Hard footstep 2 | `sounds/bukov/footstep_hard_2.wav` | `71b90b18a6ceb6fe3cbac900d6b69179e137bd6335dc60ae5616040ff01a2deb` |
| Water footstep 1 | `sounds/bukov/footstep_water_1.wav` | `6e4f45708cb00f384229ebdd0ccae67f94fedb8bb069f78d49c88dffc68bb71a` |
| Water footstep 2 | `sounds/bukov/footstep_water_2.wav` | `15a9b7c0ab808b6cd4a4e2ea5d3c9d5779d5f4db2d572b5588965708c9746c23` |
| Metal footstep 1 | `sounds/bukov/footstep_metal_1.wav` | `b4c6effdfde2306c15adfc78fe9ef01d931bf7e7b6bbf0b4de8d4d7173085d9e` |
| Metal footstep 2 | `sounds/bukov/footstep_metal_2.wav` | `5a78c3ac9880bc9cfb593ebdf94a8375087510d24a22d2b52d4f8ec9cba05e45` |

Footsteps are paced by accepted movement distance, not input or render frames.
Water terrain routes to the water pair; industrial `EMBERS`, `EMPTY_SP` and
open-door threshold terrain route to the metal pair; other walkable terrain
uses the hard pair. All three surfaces use the same master/SFX mix as gun and
interaction cues.

The footstep gate also measures duration, zero-crossing rate and
first-difference energy for each family. This keeps future remasters flexible
while rejecting six valid-looking WAV files that no longer sound materially
different from one another.

## Concurrent voice budget

`SoundConcurrencyBudget` caps each of the four audio buses at six logical
voices across the whole Bukov runtime, not six per producer. UI and World
players share one production coordinator, so their combined SFX count cannot
exceed six. When a bus is full it deterministically replaces the oldest voice
in the lowest eligible priority, including synchronously stopping a victim
owned by another producer. Protected player gunfire, extraction cues and key
UI cues cannot be evicted by lower-priority ambience or footsteps. Explicit
release supports backends with a playback-complete callback; bounded timeouts
release and stop voices on the host backend otherwise. Each owner advances
only its own timeouts and `stopAll()` only clears that owner, so a paused or
disposed World cannot cut off an unrelated UI cue.

The existing `BukovUiSoundRouter` production path now enters the budget through
`BukovUiSoundPlayer`: focus ticks are low priority, while confirm, cancel and
error cues are protected. The router's master/SFX gain and `soundFx` mute check
remain upstream and unchanged.

The realtime world's private playback seam uses the same budget. One gunshot
is one logical source even though its mechanical, body and environment-tail
layers remain three independently mixed PCM instances. Player gunfire and
extraction cues are critical/protected, enemy gunfire is normal/replaceable,
and footsteps are low priority. The fixed-step sound update owns timeout
release for World voices, while world disposal stops every remaining
World-owned backend instance without cutting off UI playback.

`extraction_complete.wav` is registered for the settlement transition seam;
the current realtime world plays the transponder-start cue while the player is
still in the raid.

Gate 5 combat outcomes use a dedicated feedback resolver. Normal and weak-point
kills share the short 200-300 Hz `kill_confirm.wav`; Boss phase breaks, slam
pulses and fog-lamp overload explosions each use a distinct authored cue. All
four assets play at their recorded pitch (`1.0`) through the Bukov SFX bus and
the shared six-voice concurrency budget rather than repitching a UI sound.

The original `gunshot_player.wav` and `gunshot_enemy.wav` remain registered
only as compatibility fallbacks while production call sites migrate. New
firearm content declares an explicit `audio.gunshotFamily` and ordered
`reloadCueFractions` in `firearms.json`. The fractions are relative to total
reload time, so attachment-based duration changes keep magazine-out,
magazine-in and charge cues synchronized.
