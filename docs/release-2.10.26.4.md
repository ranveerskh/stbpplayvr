# STB PLAY Android 2.10.26.4

Delayed Live TV category loading now retains its focus handover. Settings subpages transfer focus directly without briefly expanding the navigation panel. Replacing a category/settings target tracks the actual focused requester, preserving the existing yellow highlight and focus restoration.

Home shows separate latest-ten rows for unfinished Movies/Series and recently watched Live TV. Empty history rows remain hidden. Series retain their exact episode/resume actions; watched channels retain Play, Favourite, Remove History and Back. Full provider catalogues, favourites and stored resume history remain available without catalogue caps.

TV heroes rotate every 12 seconds while unfocused, pause on focus, and retain the displayed title when the pool is reordered. Heroes and up to ten recommendations favour genuinely watched categories, available genres/languages, and the latest two watched titles. A small per-portal/profile viewing summary survives completion and resume-row removal; seeking and paused progress ticks do not count as watching. Earlier unfinished history provides a cold-start signal until qualified viewing sessions accumulate. Suggestions include some category variety and exclude known completed movies.

Ranking scans the available catalogue off the UI thread, keeps the existing safe pool during refresh, and does not launch new rankings during standalone VOD playback. The previously tested VOD progress isolation fix remains included. No player dependencies, codec/buffer settings, PIN rules or saved theme preferences were changed.

## Verification

- VersionCode 83; original release signing certificate verified.
- Exact 2.0.20.6 provenance baseline: `f90249b466cace478e5a038c1cd96ca0e4fc7aba`.
- Latest tested Home foundation: `8742a838bbaa5098046de83033823030dcd51425` (2.10.26.3).
- Verified candidate source: `41edfc0dc15691057855e6c29ac6ed6fe9668f8a`; tree `9896fd1d603a8baec1819804f75ec3e56cdcb7cf`.
- [Verification run](https://github.com/ranveerskh/stbpplayvr/actions/runs/38428887165): successful signed build, 68 unit tests passed, no new lint errors against the exact baseline (14 existing errors).
- Android TV emulator: 51 passed, one phone-only test skipped.
- Android phone emulator: 42 passed, one TV-only test skipped.
- Suites cover late-category focus, settings transition frames, idle/focused hero rotation, catalogue reuse, real fixture playback, gestures, search, history/resume, profiles/PIN and theme retention.
- Physical Android TV/box and Meta Quest validation was not performed. Emulator results do not establish real-device performance.
- Genre/language matching depends on available provider metadata; historical watch duration cannot be reconstructed retroactively.

Only release documentation and a signed APK artifact upload step were added after the successful candidate verification; application and test source trees are unchanged. Original stable/test tags and the tested 2.10.26.2/2.10.26.3 branches remain available as rollback foundations. Windows and platform-panel repositories were not changed.
