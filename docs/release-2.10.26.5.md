# STB PLAY Android 2.10.26.5

Series details and the player episode picker now load complete paginated seasons instead of stopping at the first repeated 14-item page. Series paging handles one-based and zero-based portals, deduplicates stable episode IDs, and reports incomplete loads rather than silently returning a truncated list. There is no arbitrary episode or season cap.

Episode labels use provider episode/series numbers rather than the visible row index. Latest-to-oldest ordering and the episode selected for playback remain unchanged. Labels are cached during playback so long seasons do not add work to every progress update.

TV focus recovery repairs actual page focus loss, respects deliberate navigation to the rail, and waits for attached targets. Explicit channel entry still uses the remembered channel or the first channel after adult-category loading; accidental recovery uses the last focused control. Preview actions, settings subpages and modal dialogs retain their focus.

Phone Home history Remove and Favourite actions now have separate bounds. TV heroes rotate every 12 seconds whether focused or unfocused; the existing phone pause-on-focus behavior is preserved.

## Verification

- VersionCode 84; versionName 2.10.26.5; original Android release signing certificate verified.
- Working stable foundation: `1a51f365a9c2300353f601ad392987295243c1e4` (2.10.26.4).
- Original tested 2.0.20.6 source provenance: `f90249b466cace478e5a038c1cd96ca0e4fc7aba`.
- Verified candidate: `70a56bc719327a0fcb9b1f61552f1b90e063451c`; tree `2fa57b3cafecbd2f05701e2d31f75716acd69b98`.
- Development branch: `update/episodes-focus-2.10.26.5`.
- [Verification run](https://github.com/ranveerskh/stbpplayvr/actions/runs/38444512586): signed Gradle build passed; 75 unit tests passed; no new lint errors against the exact tested baseline.
- Android TV emulator: 55 passed, one phone-only test skipped.
- Android phone emulator: 46 passed, one TV-only test skipped.
- Regressions cover 330 episodes in 14-item pages, zero-based paging, incomplete page failures, real descending episode numbers, the 50-episode player picker, delayed category focus, remembered channel restoration, repeated focus loss, settings transitions, focused hero rotation and non-overlapping touch actions.
- Existing suites also cover catalogue reuse, real fixture playback, gestures, search restoration/history, watch history/resume, profiles/PIN and saved theme preferences.
- Physical Android TV/box and Meta Quest tests were not performed. Emulator results do not establish physical-device performance.
- Provider data and paging must expose the missing episodes; an incomplete server response now surfaces a load failure instead of appearing to be a complete season.

Only this release document was added after successful candidate verification; application and test source trees remain identical to the verified candidate. Existing stable tags and development branches remain available for rollback. The prior VOD progress isolation fix, saved data and themes are preserved. Windows and platform-panel repositories were not changed.

