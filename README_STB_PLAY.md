# STB PLAY Android TV — v1.8.19

This is the **native Android TV** project for STB PLAY. It is not a WebView or a
copy of the Windows HTML UI. The screen flow is built with Kotlin, Jetpack
Compose for TV, Media3 and Android DataStore.

## Import and run

1. Extract `STB-Play-Android-TV-v1.8.15.zip`.
2. In Android Studio choose **File → Open**.
3. Select the extracted `STB-Play-Android-TV-v1.8.15` folder — it must be the
   folder that contains `settings.gradle.kts`.
4. Wait for the Gradle sync to finish. Use JDK 17 and install Android SDK 35 if
   Android Studio asks.
5. Select an Android TV device/emulator and press the green **Run** button.

The first run shows the STB PLAY usage notice and then portal setup. Enter only
a portal, MAC address and subscription that you are authorized to use. Both
`http://` and `https://` portals are supported because some authorized Stalker
portals still use HTTP.

## Native Android TV feature set

| Windows STB PLAY feature | Android TV implementation |
| --- | --- |
| Portal profiles, editable MAC and parental PIN | Device-local per-profile storage, add/edit/use/delete portal screens |
| Premium navy/gold home | D-pad-first collapsible rail, rotating hero, Continue Watching, favourites, latest and recommendations |
| Live TV | Provider categories, protected category PIN gate, channel rows, favourites and direct Media3 playback |
| Movies and Series | Shared catalogue, category browser, movie details, series → season → episode → quality path |
| Search | Local strict 3-character minimum search over title, alternative metadata, genre, language, cast and year |
| Favourites and history | Persistent per-profile favourites, resume point, remove one item or clear all history |
| Player preferences | Internal Media3, explicit VLC, or Auto fallback to VLC if installed |
| Subtitles | Provider subtitle tracks with Auto, Off, English, Hindi and Punjabi preferences |
| Refresh and cache | Manual refresh, clear local catalogue state, last-refresh display and portal loading progress |
| Parental controls | Locked adult/A-rated categories and titles, PIN prompt and re-lock when leaving the area |
| Updates | Android APK update check/download/install flow; the update manifest must provide `androidApkUrl` or `apkUrl` |
| Privacy/diagnostics | Local anonymous-diagnostics preference; portal URL, MAC, PIN, stream URLs and titles are never sent by this app |

## Build from a terminal

On Windows:

```bat
gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Playback behaviour

The app asks the authorized Stalker portal for a `create_link` stream command
and plays that response. Live streams open directly. Movies and episode streams
open through the provider quality selector when quality variants are reported.
If a provider does not return a stream command, the app deliberately reports it
as unavailable instead of inventing a URL.
