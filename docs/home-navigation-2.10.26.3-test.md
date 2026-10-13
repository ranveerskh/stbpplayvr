# Home history/navigation experiment — test only

Rollback foundation: user-tested 2.10.26.2, exact remote commit
`0f898f89d43adbce02e8246ca96706e97e82e11f`, tree
`8d38866bd9f05c40b2e7fa3e85cc9433e11c38b8`.
Stable main/release remains 2.10.26.1 (`165cf88a8fa6a853d5a487ffb2cb004d872a24be`).

User explicitly wants this design reversible. Keep this work on
`test/home-navigation-2.10.26.3`; do not merge or publish a release.
If rejected, revert the Home/history/navigation experiment onto that exact .2
foundation with a *higher* numeric Android versionCode for an in-place rollback
APK. Do not tell users to uninstall: that would erase their personal data.
The existing progress/favourites encodings remain compatible; extra episode
identity data can be ignored by old source. The .2 VOD progress isolation remains.

## Scope

- Home removes general Live TV and Favourite Channels rows; favourites screen/data remain.
- Separate nonempty watched VOD and recent Live rows, newest timestamp first.
- Home previews ten items per row. View All opens complete saved history or the
  existing full content browser. No new catalogue/search/storage caps.
- Live Home cards fit provider logos in a 48dp image slot with initials fallback.
  The existing parser supports logo/logo_url/icon/icon_url and authenticated artwork.
- Home Live selection opens Play/Favourite/Remove History/Back actions on phones too.
- Save series-parent/episode-name/season identity atomically alongside progress,
  per portal/viewer. One latest unfinished episode per series in Continue Watching.
  Re-fetch current provider episodes before resuming; do not persist stream URLs,
  or silently substitute a different episode if the old one is unavailable.
- Series Resume/Start Over/Favourite/Remove History/Episodes/Back actions.
- Existing completion cutoff remains 95%. Historic episode records without parent
  metadata cannot be safely inferred; association becomes available when watched
  with this build. Existing movie/channel history is retained immediately.
- Timestamp/episode projections freeze with hidden VOD projections while playing;
  real progress continues saving. Refresh when returning from playback.
- TV rail expands statically on focus, with full icon/label rows. Overlay has a fixed
  content footprint: no content movement, tooltip clipping, blur or sliding animation.
  Yellow remote focus and page-entry targets retained. Other rail layouts retain
  their manual toggle and show full labels when expanded.

## Validation

Run unit tests, lint regression comparison, TV and phone emulator suites, real
fixture playback/persistence with 100k titles, series storage isolation/actions,
recency/deduplication and rail geometry/focus checks using the signed test workflow.
Report actual results after completion. Real provider imagery/series availability,
TV/box navigation speed and Meta Quest require user/physical-device testing.

### Control-idle test clock diagnosis

The unchanged phone movie smoke check timed out twice at its seven-second
wall-clock wait. Isolated diagnostics (run 38404972277, commit
16c00f3405be104f4aced3ca61210ace72fa8677) measured **2304ms virtual time versus
7066ms real time**, while decoded video was still playing and the native
controller visible. The production Compose idle delay is 3000ms: wall-clock
polling had not advanced the virtual timer far enough. Player production source
matches the working .2 foundation byte for byte.

The test now explicitly drives virtual time, asserts both Compose and native
controls remain visible at 2500ms, and asserts both hide after 3100ms. During
native drawing synchronization the virtual clock stays fixed, so extra polling
cannot accidentally grant a longer application timeout. Seek/back/playback
assertions remain. No production player timeout was changed.
