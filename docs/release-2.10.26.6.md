# STB PLAY Android 2.10.26.6

Search on Android TV now keeps its yellow focus highlight and opens with the remote. The TV header is part of the page focus group, so page recovery no longer mistakes intentional focus on Search for lost focus.

Returning to Live TV or Movies & Series from another section starts at the first category. Saved category lists scroll to the entry category before the attached-target focus handover. Previously the list could restore a deep scroll position while the requested category was offscreen, leaving focus pending indefinitely.

Within-section rail navigation, accidental focus recovery, preview controls and Settings subpage/back behavior retain their existing paths. Player/details returns keep their remembered item and position. Phone layout, playback, themes, history, profiles and PIN behavior are preserved. The YouTube experiment remains separate and unmerged.

## Verification

- VersionName 2.10.26.6; versionCode 85; original release signing certificate verified.
- Stable base: `92771b7053997a2712ab1660d3b7cca457cc2abc` (`v2.10.26.5`).
- Development branch: `fix/search-category-focus-2.10.26.6`.
- Candidate: `2cb5a85883af45479ff40fb0d220143a36e12e58`.
- [Baseline reproduction run](https://github.com/ranveerskh/stbpplayvr/actions/runs/38800443342): both Search-focus and offscreen-category regressions failed against the unmodified 2.10.26.5 UI and passed against the fixed UI. The initial full TV run identified an incorrect new-test expectation that Settings should forget its last menu; this expectation was corrected to preserve existing Settings behavior.
- [Corrected candidate verification](https://github.com/ranveerskh/stbpplayvr/actions/runs/38803465697): 75 unit tests passed; Android TV 58 passed / one phone-only skipped; Android phone 46 passed / one TV-only skipped. Signed candidate build passed. Zero new lint errors against the exact stable 2.10.26.5 source (14 pre-existing errors remain).
- The corrected TV suite checks D-pad Up to Search, retained Search focus, remote OK opening, repeated section round trips, and re-entry after scrolling 28 categories down. Existing playback, search return, profile/PIN, themes, history/resume and episode-pagination suites passed.
- Candidate APK package `com.example.stbplay`, versionCode 85 and versionName 2.10.26.6 verified; original signing certificate SHA-256 `91943d0efc4125eb6aeda132ba6b8d79855496e030477c2d5cd00e9bedd4fdb0`.
- Candidate APK SHA-256: `60fd2e76a8089fbd1ad8db853ef89ac34500927bd0d1731dd54d6a551602bd11`.
- Only release documentation was added after candidate verification; application and test source remain identical to the verified candidate.
- Physical Android TV/box and Meta Quest checks remain pending. Emulator results do not establish physical-device performance.

Existing stable tags remain available for rollback. Only the Android repository is changed.
