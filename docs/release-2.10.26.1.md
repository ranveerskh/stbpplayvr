# STB PLAY Android 2.10.26.1

Foundation: tested 2.0.20.6 `f90249b466cace478e5a038c1cd96ca0e4fc7aba`, then signed 2.0.20.10 `44e5de716883a7c2bf0f6fa86b1247cde319020e`.

- Phone fullscreen brightness/volume gestures; VOD +10 seconds, existing backward/Live input retained.
- Lightweight Glass appearance, retained normal themes and separate Adult Glass preferences.
- Protected Live first-channel focus, adult-category exit reset and immediate loading screen.
- Valid PIN auto-unlock, prominent Unlock and compact Cancel.
- Compact original-aspect hero poster, matching Cast surface and profile-name header.
- Live player timeline removed on phones as on TV; existing channel and playback controls retained.
- Compact equal-size player actions, explicit white icons, darker surfaces and consistent Episodes/Cast styling.
- One shared idle timeout hides native and surrounding controls together; landscape transport spacing improved.
- Fixed peaceful profile-selection artwork and theme, independent of all profile/adult/custom appearance settings.
- Dedicated portrait Planet, Midnight Lake and Graphite Silk backgrounds; TV/landscape artwork retained.
- Free app trial is 365 days from the original install date; updates do not restart it. Assigned provider license/trial expiry still takes precedence.

Version name 2.10.26.1, versionCode 80, original signing identity.
No catalogue/search limits, new dependencies, real-time blur or navigation animations.

Validation uses unit tests, lint against exact tested baseline, phone and Android TV emulators with generated video playback, and signing checks. Physical Android boxes/TVs, Meta Quest performance and casting receivers still require real-device testing.
