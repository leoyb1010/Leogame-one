# Apple development

Leo的地牢围攻 currently targets macOS, iPhone, and iPad. The Android module remains outside the current delivery scope.

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

Build products are stored under the macOS per-user cache directory in `leogameone-gradle/`.
The macOS application packager automatically selects an arm64 JDK on Apple Silicon and an x64 JDK on Intel Macs.

The Apple application name is `Leo的地牢围攻`, the Bundle ID is `leogameone`, and first launch defaults to Simplified Chinese. Upstream news and release-update feeds are disabled until Leo-owned endpoints exist.

Artwork intake requirements are documented in `docs/ARTWORK_GENERATION_BRIEF_ZH.md`.
