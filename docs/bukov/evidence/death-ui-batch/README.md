# iOS hideout button verification

- Device: Codex Test iPhone 17 Pro simulator
- Logical viewport: 874 × 402
- Back buffer: 2622 × 1206 (3×)
- UI scale: 150%
- Locale: Simplified Chinese
- Build path: forced RoboVM AOT with `ios:launchIPhoneSimulator --rerun-tasks`
- Screenshot: `ios-hub-icon-buttons.png`

Verified in the captured frame:

- Hideout primary, destructive, service, settings, and exit actions use
  project-owned Bukov atlas icons with short labels.
- The long active-raid copy no longer overlaps the action buttons.
- Active-raid stash details remain inside their panel.
- Touch targets retain the scaled iOS control height while the label uses the
  smaller caption typography role.
