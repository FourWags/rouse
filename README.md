# WagRise

**Wake up with a dog by your side.** WagRise is a private Android alarm for workdays and personal plans. Connect a shared iCalendar feed to schedule work alarms, or set an alarm yourself. Choose from 28 illustrated dogs and mixes, then pick a coat: each of the 84 coats shapes the app's colors and appears as a resting or stretching companion on the two large alarm buttons. Your chosen breed also becomes the actual home-screen icon as a side-profile portrait with gentle sound marks. The portrait now has more space around the entire head. The original icons and color themes have separate expandable menus. A clear action returns to the basic WagRise look without deleting saved personal themes.

WagRise includes gentle recorded orchestral music, standard tones, and nature sounds. You can import your own audio, add the optional orchestra pack, set the Android alarm volume and gradual rise, and test the actual alarm path. Calendar links and alarm data stay on your phone; there is no WagRise account, analytics, or advertising. See [README.txt](README.txt) for setup and limits.

This source release is **0.1.52 Preview** (Android version code 53). Download the signed APK and see its actual test results on [GitHub releases](https://github.com/FourWags/wagrise/releases). The compatible [optional orchestra pack](https://github.com/FourWags/wagrise/releases/tag/v0.1.23) is separate.

## Obtainium

Source: `https://github.com/FourWags/wagrise`

APK filename filter: `^petal-wake\.apk$` (kept for existing Obtainium subscriptions)

Android package: `app.daybreak` (kept so existing installs and data update in place)

Signing certificate SHA-256: `386f097ea7de3749f0b0cc87e433d0f8a8a70cebb1848d7e7cb4bd5f60cff2b4`

Obtainium detects published releases. An update needs a higher version code and the same signing key. The key, private calendar links, and personal app data are never published.

## Build and rights

Build with JDK 17 and Android SDK API 35 using `./gradlew assembleRelease`. The release variant is non-debuggable and uses the existing update certificate kept outside the repository. See [RELEASE_SIGNING.md](RELEASE_SIGNING.md) and [VALIDATION.txt](VALIDATION.txt).

Four Wags' original materials are proprietary; see [LICENSE](LICENSE). Third-party terms and audio provenance are in [THIRD_PARTY_COMPONENTS.md](THIRD_PARTY_COMPONENTS.md), [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt), and [CREDITS.txt](CREDITS.txt). No upstream endorsement is implied.
