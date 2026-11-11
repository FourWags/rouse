# Rouse 0.1.43 — alarm controls Preview

This update combines three changes:
- While an alarm is snoozed, a silent private notification shows when the next snooze is due. Its “I’m up” action opens an End snooze confirmation; opening the notification never cancels an alarm. Ending one snooze leaves other snoozes and personal alarms scheduled.
- The home screen now shows a next-alarm check with the Android scheduled time, exact-alarm and notification access, full-screen display access, current phone alarm volume, selected sound or fallback, and work-calendar freshness when the next alarm comes from a shift. “Ready to test” describes reported settings; it does not guarantee delivery.
- The ringing screen explains the hold-to-dismiss control and gives the short-tap guidance an OK action. It remains on screen after Back, swipe or screen-off attempts. Linked-audio preparation now releases its worker resources after use.

Validation: the signed, non-debuggable APK upgraded v0.1.42 in place on the Pixel 11 Pro XL; installed version is 0.1.43/code 44 and installed APK hash matches the verified build. The app launched and remained running. Emulator checks covered notification privacy and confirmation, cancel and End snooze, remaining alarms, ringing-screen behavior, and lifecycle cleanup. 44 host tests passed; release lint found no errors and 70 warnings. A clean Android 15 emulator with alarm permissions also passed 30 native smoke checks, including alarm delivery, volume rise and snooze replay.

Personal Preview only. No audible alarm was rung on the Pixel during this update. Real-calendar changes, long-duration battery behavior, live TalkBack/Switch Access qualification, and production signing remain outside this release check. The existing preview signing certificate is retained for in-place Obtainium updates. The offline USB copy of that signing key remains pending. Existing audio and component notices remain; this release does not establish worldwide recording-rights clearance.

Stable APK asset: `petal-wake.apk`. The corresponding source and notices are in `petal-wake-source.zip`. Verify both against `SHA256SUMS`.
