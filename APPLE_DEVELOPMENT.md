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

## Tests and distribution gates

```sh
scripts/apple-gradle :core:test :desktop:test :ios:test
python3 scripts/validate_release.py
scripts/apple-distribution-audit
```

An IPA produced with an Apple Development certificate is limited to registered devices; it is not a TestFlight or App Store package. Public distribution must not proceed until `scripts/apple-distribution-audit` passes.

The distribution gate requires:

- A `Developer ID Application` certificate for macOS signing.
- An `Apple Distribution` certificate and explicit `leogameone` provisioning profile.
- Notarization credentials saved with `xcrun notarytool store-credentials`; pass the profile name in `NOTARY_KEYCHAIN_PROFILE`.

Development builds remain available for Leo's own devices, but the workflow no longer reports a development-signed build as publicly distributable.
