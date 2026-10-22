# STB PLAY Android 2.10.26.7

System Home now pauses local movie/series and Live TV playback when the host activity stops. Returning resumes only a player that was playing before Home; a manually paused player stays paused. The lifecycle observer targets the local player, not the remote Cast controls. Current VOD progress is saved on stop.

Phone Back/Fullscreen actions use a separate bottom footer, with matching reserved space in the native controller. This replaces the fixed 120dp overlay offset and compact-landscape translation. Video bounds and the shared controls timeout remain unchanged. Fullscreen gestures evaluate video touch before revealing controls and ignore native control hit boxes while those controls are hiding.

Liquid Glass adds saved background blur choices: 0%, 50% and 100%. Blur is computed on a bounded software bitmap by Coil and cached, rather than applied to the live UI/video. The default is 0% to preserve existing appearance. Other themes and fixed profile-selection artwork remain independent.

Owner PIN setup, Settings PIN change and authenticated profile editing now offer profile-entry or restricted-content-only modes. Existing owner credentials stay in their original store. Restricted-only Owner can enter without a PIN and is verified for restricted playback; profile-entry Owner authenticates on entry like other adult profiles. Profile management remains protected, and Kids entry behavior is unchanged.

## Verification

- VersionName 2.10.26.7; versionCode 86.
- Stable base: `4766488b522a85378bceabd98734caff887a1412` (`v2.10.26.6`).
- Development branch: `fix/playback-blur-owner-2.10.26.7`.
- Candidate source: `dd8f1b082819bcacf966e5844036f55e613fc5e0`.
- [Successful verification](https://github.com/ranveerskh/stbpplayvr/actions/runs/39776065474): signed build passed; 76 unit tests passed; Android TV 65 passed / two phone-only tests skipped; Android phone 53 passed / two TV-only tests skipped. The separate focused phone job passed all four checks.
- Zero new lint errors against exact stable 2.10.26.6 source; pre-existing lint errors remain.
- Tests cover real Home presses and foreground return for Live TV and movies, retention of manual pause, compact native/Compose control bounds, fullscreen brightness/volume gestures, blur persistence and bitmap transformation, Owner entry/restricted-only behavior, Owner mode storage and TV PIN-option focus. Existing Search/category focus, episodes, history/resume, themes, profiles and playback suites passed.
- Candidate APK package `com.example.stbplay`, versionCode 86, versionName 2.10.26.7 and original release signing certificate verified.
- Signing certificate SHA-256: `91943d0efc4125eb6aeda132ba6b8d79855496e030477c2d5cd00e9bedd4fdb0`.
- Candidate APK SHA-256: `947d5be6f76cd25374226e9f8e85226d6bdbab1cb88fb72a851fe8d7cfbf8454`.
- Earlier diagnostic runs found a fullscreen gesture regression and test-return/focus issues. These were corrected before the successful full run. Only this release document follows final candidate verification; application and test source remain identical.
- Physical phone, Android TV/box and Meta Quest checks remain pending; emulator checks are not physical-device performance proof.

The separate YouTube portal experiment remains unmerged. Existing stable tags remain available for rollback. Only the Android repository is changed.
