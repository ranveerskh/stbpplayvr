# Modern themes batch 1 (test only)

Based on the verified v2.0.20 source plus the isolated performance batch at 2634c9d. Modern-theme work stays on optimize/android-modern-theme-batch1; no merge or stable release.

Refresh the existing Blue, Light, Black and After Dark palettes with cool flat surfaces, readable secondary text, warm yellow TV focus in the normal themes, and the existing pink accent in adult-only mode. Keep existing theme preference IDs and launcher icons. Shared static TV surfaces use 10dp corners and a subtle unfocused outline; the focused outline remains 2dp. System-font body text keeps its size/line height and uses slightly tighter tracking.

No new effects, fonts, dependencies or animation. Layout geometry, focus routing, item keys, player/decoder code, parental behaviour, provider/subscription logic, saved settings and catalogue sizes are unchanged. Free/donation/open-source policy changes are outside this visual batch.

VersionName 2.0.20.2, same versionCode 72 and release signing key, permitting in-place update and rollback. The branch-only CI reruns unit tests, TV focus tests, phone touch/search tests and fixture playback checks. It captures real Compose Home/Live/Movies screens on the TV emulator and Home/Movies on the phone emulator in all three normal themes for visual review. Physical TV/phone/Quest checks remain required; screenshots do not establish a device-performance improvement.
