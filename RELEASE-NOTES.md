# Rouse 0.1.45 — two-button alarm Preview

The ringing screen now has only the two large **Snooze** and **I’m up** app controls. A short touch on I’m up shows hold instructions inside that button; a two-second hold dismisses. With TalkBack or Switch Access, the first selection changes the same large button to **Select again to stop** and the second selection dismisses. Switch Access guidance on the button explains the blue outline, Next and Select switches, and that no screen tap is needed.

The Android ringing notification still opens the alarm screen but no longer adds small Snooze or I’m up actions. Android can still show its own persistent alarm banner and Switch Access Menu as extra scan targets; those belong to the system and remain to preserve reliable full-screen alarm access.

Validation: the signed, non-debuggable APK installed over 0.1.44 on the Pixel 11 Pro XL without clearing data, and the installed APK hash matched the candidate. A physical synthetic alarm was dismissed using the volume buttons as Switch Access Next and Select switches; the user confirmed the wording was clear and no screen tap was needed. On the exact release APK, Android 17 emulator checks passed TalkBack and Switch Access two-selection dismissal, normal hold guidance, swipe and Back resistance, and notification behavior. A 200% font visual check showed the instructions fully visible. All 44 host tests passed; release lint had zero errors and 70 inherited warnings.

Personal Preview. The phone session used a synthetic alarm, so real future shift delivery, long-term battery behavior, and other Android devices remain outside this check. The offline USB signing-key copy remains pending, and the existing preview certificate is retained for in-place updates. Existing audio rights and production-signing limits remain. This release has not been uploaded or verified through Obtainium.

Stable APK asset: `petal-wake.apk`. Source and notices: `petal-wake-source.zip`. Verify both against `SHA256SUMS`.
