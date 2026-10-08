# Android optimization batch 1 (test only)

Baseline: released v2.0.20, commit `47216fdf98fc7d013c4d436c97a5c13d284fb3c4`. At implementation start, main has the same tree. Work stays on `optimize/android-2.0.20-batch1`; no release tag, stable publication or merge is part of this batch.

Test APK versionName is `2.0.20.1`, versionCode is still `72`, and the existing release signing key is used. This permits an in-place install over 2.0.20 and an in-place return to stable without a version-code downgrade. No app-data migration is introduced.

## Changes

- Reuse the already remembered parental-filtered Live/VOD inputs for search. Remember the language-filtered Content projection and combined Home/Settings projection. No additional search or catalogue limit is introduced.
- Remember favourites' UI mapping, including progress, favourite flags, portal settings, catalogue generation, artwork token and cookie as invalidation inputs.
- Look up a selected typed ID by scanning sources in the released order, returning early without concatenating entire catalogues. Category-only, remote-search and locally saved favourite fallbacks remain supported.

The focus coordinator, lazy list keys, UI geometry, keyboard implementation, player lifecycle, portal requests, cache format, settings persistence, Device ID/MAC and subscription/provider code are unchanged.

## Validation

The test-only CI workflow runs unit tests, the existing seven Android TV focus/highlight checks, projection invalidation/reuse tests, phone touch/search checks, and real Media3 playback with a generated H.264/AAC fixture. The TV preview/fullscreen test checks player identity and playback position. The phone fullscreen test checks player identity across rotation and Back. Quest layout inflation and directional controller callbacks are checked on the emulator, together with the existing surface-policy unit tests; this is not physical Quest verification.

The projection reuse test uses 20,000 VOD items and 80 favourites across 20 focus-only updates. It asserts that the search/favourite list instances stay the same and that favourites are mapped only once. These are work-reuse checks, not an emulator frame-time benchmark or evidence of a specific physical-device speedup.

The API 30 phone emulator bundles an old Google Cast Dynamite module. Tests initialize that module synchronously on the main thread before Media3's asynchronous initialization; production Cast code and dependencies remain unchanged. These checks cover phone UI and local playback, not Cast receiver transfers or physical phone startup timing.

Physical TV/box, phone and Quest testing is still required. Install over the current app without uninstalling or clearing data. Repeat the same provider, parental mode, language and cache conditions before and after: Home shelf moves, Live scrolling without OK, channel preview/fullscreen/Back, Movies grid movement and return focus, favourites/history, search keyboard, portal switching and authenticated artwork. Use the read-only audit's ADB startup, gfxinfo, meminfo and Perfetto protocol for performance comparisons.
