# VOD progress isolation test (not a stable release)

Foundation: stable 2.10.26.1, commit 165cf88a8fa6a853d5a487ffb2cb004d872a24be.

Reported symptom: Android TV VOD freezes briefly every 4–5 seconds; Live is smooth.
Source finding: NativePlayerScreen calls progress persistence every five seconds for VOD. Progress changes invalidate catalogue/Continue Watching projections on the main thread; the new Live projection also maps hidden Live rows. This is a plausible cause, not a confirmed trace from the user's TV.

Scope: Keep catalogue progress inputs stable during standalone VOD playback while retaining actual five-second resume persistence. Apply current progress immediately on leaving playback. Live row projection runs only for the Live screen and retains loading/focus handling when returning. No player, decoder, buffering, catalogue limits or saved-history policy changes.

Regression checks: 100,000-row projection reuse over history changes and immediate return refresh; real fixture playback with four actual five-second SettingsManager saves and 100,000 catalogue rows, retained player identity and resumed UI state on phone/TV emulators. Reports include save durations, not a claim of real-device performance or a fully reproduced physical-TV hitch.

Signed test APK only. Do not merge main, create release/* branches or publish a GitHub release.
