# Apple development

This fork currently targets macOS, iPhone, and iPad. The Android module remains unchanged so upstream core-game updates can still be merged cleanly.

## Requirements

- Apple Silicon Mac
- Xcode with an iOS Simulator runtime
- Homebrew OpenJDK 17

## Commands

Use the Apple wrapper so generated applications and frameworks are built outside FileProvider-managed source folders:

```sh
scripts/apple-gradle :desktop:release
scripts/apple-gradle :desktop:debug
scripts/apple-gradle :desktop:jpackageImage
scripts/apple-gradle :ios:launchIPhoneSimulator
scripts/apple-gradle :ios:launchIPadSimulator
```

Build products are stored under the macOS per-user cache directory in `our-game-gradle/`.
The macOS application packager automatically selects an arm64 JDK on Apple Silicon and an x64 JDK on Intel Macs.

The application name, package identifier, icons, title artwork, credits, news feed, and update feed still use upstream values. Change them together when the new product identity is selected.
