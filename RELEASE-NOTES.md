# Rouse 0.1.44 — readable alarm dismissal Preview

The alarm dismissal confirmation now has larger text, high-contrast buttons, and a clear choice between **Dismiss alarm** and **Keep ringing**. The same readable styling applies to the short-tap hold guidance. At 200% Android font size the actions remain visible in a stacked layout.

TalkBack and Switch Access can activate the two-step dismissal without a timed touch hold. An unrelated accessibility service, such as a launcher helper, keeps the normal hold-to-dismiss behavior.

Validation: 44 host tests passed. The signed, non-debuggable APK was installed in place on the Pixel 11 Pro XL; the installed file matched the tested APK hash. Physical TalkBack and Switch Access sessions on the preceding 0.1.43 build reached alarm dismissal using spoken controls and physical volume-switch selection. The 0.1.44 fix passed emulator confirmation, contrast/size assertions, normal hold guidance, swipe/Back resistance, and a 200% font visual check. On the Pixel, the 0.1.44 selection check confirmed its normal hold path with only the user's launcher accessibility service enabled. No personal calendar content was inspected.

Personal Preview. A physical, listened-to repeat of the *new 0.1.44 dialog* and longer-term delivery/battery qualification remain open. The earlier physical TalkBack check required help finishing the ringing session; the physical Switch Access retry stopped the alarm. The user's preferred Google speech Voice VIII is a Pixel setting, not bundled in Rouse. The existing preview signing certificate remains in use for in-place updates; its offline USB recovery copy is pending. Existing audio-rights and production-signing limits remain.

Stable APK asset: `petal-wake.apk`. Source and notices: `petal-wake-source.zip`. Verify both against `SHA256SUMS`.
