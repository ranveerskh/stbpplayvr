# Android test 2.0.20.10 — user-reported fixes

Foundation: user-tested 2.0.20.6 f90249b466cace478e5a038c1cd96ca0e4fc7aba;
previous signed test 2.0.20.9 fe016c026d10cf9baa7b4989c35e8e64fb18ace3.
All work remains on feature/android-glass-player-2.0.20.6. No stable merge/release.

1. After protected live category loading, first-channel focus waits for actual rows.
2. Leaving a protected live category in normal mode resets selection, stored channel
   focus, scroll and category authorization. Adult-only mode keeps its existing intent.
3. Provider channel-row projection runs on Dispatchers.Default, with an explicit
   loading state and category identity check to avoid showing stale category rows.
4. Owner and personal PIN prompts auto-verify correct input. Unlock is the primary
   action, Cancel compact. Existing hash verification, kids and restrictions retained.
5. Home hero shows the provider poster at its original aspect ratio, without a
   giant cropped portrait backdrop. No replacement posters or invented metadata.
6. Real Media3 Cast picker has one shared matching surrounding surface.
7. Home title uses the active viewer name; Live list heading uses category name;
   duplicate Favourites heading removed.
8. Standard Glass gains Planet, Midnight Lake, Graphite Silk and local image import.
9. Adult-only settings show After Dark / Adult Glass; normal preferences are kept.
   Adult Glass selects the approved landscape artwork or portrait gown artwork by
   current orientation, and supports its own separate local custom background.
10. Native progress stays inside the inset Glass panel. Original layout/backgrounds
    are restored when leaving Glass. Native settings/timeline/seek behavior retained.
11. Bottom player actions group Back / previous channel / next channel / fullscreen
    in equal controls where applicable. Cast stays top-right. Phones have the
    fullscreen toggle; standalone TV/Quest playback is already fullscreen. Duplicate
    native playlist previous/next buttons are hidden, keeping channel input callbacks.

No new dependencies, blur, animations, catalogue or search caps. Existing VOD +10,
backward and Live seeking policy retained. Existing theme preferences are not reset.

Static approved assets, generated using built-in ImageGen and encoded to WebP:
- drawable-nodpi/glass_midnight_lake.webp: midnight alpine lake, subdued teal aurora,
  navy mountains, soft silver-blue reflection, dark text-safe center, no UI/text.
- drawable-nodpi/glass_graphite_silk.webp: flowing charcoal/navy satin and smoky
  glass arcs, bronze rim light, calm dark middle, no UI/text.
- drawable-nodpi/glass_adult_landscape.webp: burgundy velvet, bronze glass arcs,
  floral lace motifs, tasteful opaque-bikini adult fashion figures at edges,
  dark central negative space; no nudity, sexual objects, UI or text.
- drawable-nodpi/glass_adult_portrait.webp: matching portrait burgundy velvet,
  floral lace, smoky arcs, adult women in opaque long-sleeved evening gowns at
  upper-left/lower-right edges, dark center; no UI/text.

Workflow gates signed APK upload on unit tests, baseline lint regression comparison,
phone/TV instrumentation, APK identity and original signing certificate. Physical
Android boxes and Meta Quest performance and casting receivers require user testing.
