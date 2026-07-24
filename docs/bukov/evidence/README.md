# Current player-path visual evidence

These images are scoped visual checks, not final-release proof.

## iOS touch controls

- `ios-touch-icons/ios-landscape-touch-icons-before-opacity-fix.png`
  records the failed first inspection: icon geometry existed but token RGB was
  passed to a texture without an alpha byte, so the icons were transparent.
- `ios-touch-icons/ios-landscape-touch-icons-final.jpeg` is the Simulator
  window after the opacity fix. It shows the eight semantic touch glyphs in
  the actual raid UI.
- `ios-touch-icons/ios-landscape-touch-icons-final.png` is the matching raw
  Simulator framebuffer capture.

The installed Simulator application was verified against the RoboVM build
cache and passed strict code-signature validation. These images cover visual
presence and layout only; they do not replace the final physical-device,
pressed-state, accessibility-scale, or long-duration acceptance runs.

## macOS current batch

- `macos-current-batch/raid-window.jpeg` shows the rebuilt windowed raid.
- `macos-current-batch/backpack-window.jpeg` shows the same installed
  application with the action backpack open.

The installed application matched the current jpackage cache and passed
strict code-signature validation. These screenshots cover the responsive HUD
and backpack batch only; the final complete-player-path recording must be
captured again from the sealed release commit.
