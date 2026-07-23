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
| Bullet impact | `sounds/bukov/bullet_hit.wav` | `4d938255e0d1030e880953885b4b51d56135d4392cc539a8dd26512a4e53e600` |
| Contact impact | `sounds/bukov/contact_hit.wav` | `9207d4a2a5114c71c53f6160fd5f25f57b4d68d39b6f27fdfca17da202221a43` |
| Empty chamber | `sounds/bukov/dry_fire.wav` | `4286d982c4eaa3f6455a02958c5b17de24b08734287fc7a7a71bdd4f1151a1aa` |
| Reload start | `sounds/bukov/reload_start.wav` | `ab6d41ec0fc70786b4d7675b0b01da111e17379a5442eacaf161581666e2468d` |
| Reload finish | `sounds/bukov/reload_finish.wav` | `9f10892b735ed68d058f48307d51ccd0c26289e73ddce81850b4597747a917eb` |
| Loot pickup | `sounds/bukov/loot_pickup.wav` | `c65c545a8cf8012af4e39067168e328bcd3fc89adcd08c3b34fcf67a6eb54081` |
| Container searched | `sounds/bukov/search_complete.wav` | `e5d4a2de429741685fa21965797a1ec0dab40a3d9d994a790486ef3e4d759705` |
| Mission gate unlocked | `sounds/bukov/gate_unlock.wav` | `3618d9de7dd2b8c405c266fb36f417f788f443366003c60782883a6337c07746` |
| Extraction transponder starts | `sounds/bukov/extraction_start.wav` | `4b1478710831348c7086f4b13bcc5c5c5111b4678828453ad79c9d594a189712` |
| Extraction confirmation | `sounds/bukov/extraction_complete.wav` | `b62c7d0ffbe9a367bae62d79a2ec61936a4af440e89183f3b838215e32effd27` |

`extraction_complete.wav` is registered for the settlement transition seam;
the current realtime world plays the transponder-start cue while the player is
still in the raid.
