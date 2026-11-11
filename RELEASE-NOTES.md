# Rouse 0.1.46 — accessible setup Preview

Settings now has a dedicated **Accessibility** section. It gives step-by-step instructions for Android TalkBack and Switch Access, explains the Next and Select switches, and describes how to reach the two large alarm buttons when Android's own menu or notification is scanned first. A button opens Android accessibility settings. The guide distinguishes a live alarm's two-selection I’m up confirmation from the one-selection Stop test alarm control.

The alarm controls and calendar scheduling are unchanged. The user reports using Proton calendar work alarms daily. This release's new guide was tested on the exact signed APK in an Android 17 emulator: navigation, guide content, button size, and the Android accessibility settings action passed. All 44 host tests passed; release lint found zero errors and 70 inherited warnings. The APK has the same package, signer, and permissions as 0.1.45, with a higher version code.

Personal Preview. This release did not repeat a listened-to physical TalkBack or Switch Access alarm session; those passed on the prior 0.1.45 Pixel build. Other devices, long-term battery behavior, production signing, and worldwide recording rights remain outside this qualification. A byte-for-byte verified signing-key copy was saved to an encrypted removable USB on 2026-11-11; keep its passphrase separately and unplug the powered-off drive. No personal calendar content or credentials were included in the release.

Stable APK asset: `petal-wake.apk`. Source and notices: `petal-wake-source.zip`. Verify both against `SHA256SUMS`.
