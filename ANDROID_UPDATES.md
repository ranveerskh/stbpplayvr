# Android update channel

The app checks the latest **published GitHub Release** from this repository when it opens and once daily in the background. A release must contain a signed `.apk` asset. The 14-day deadline starts at the GitHub release's `published_at` time. On Android 13+, the system asks permission before the app can display update notifications. Periodic background work can be delayed by Android battery management; the in-app check also runs on every launch.

## One-time signing setup

Create and keep a private Android signing keystore outside this public repository. Back it up securely: every later APK for the same package must use the same key. Add these repository Actions secrets:

- `STB_SIGNING_KEYSTORE_B64`: base64 of the complete keystore file, without line breaks.
- `STB_RELEASE_STORE_PASSWORD`: keystore password.
- `STB_RELEASE_KEY_ALIAS`: alias of the signing key.
- `STB_RELEASE_KEY_PASSWORD`: key password.

On Windows PowerShell, encode an existing keystore with `[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\path\to\stb-release.jks'))` and paste that value into the GitHub secret. Do not commit the keystore or passwords.

After testing a signed APK on phone, TV, and Quest, push a version tag matching `app/build.gradle.kts`, for example `v1.8.40`. The release workflow builds and publishes the signed APK. The app will offer only a later release that contains an APK, and Android will ask the user to approve installation.

**Existing debug builds use a different signing key.** They cannot be updated in place by the first signed release; users must make a one-time clean install and re-enter their portal details. After that transition, updates from the same signing key preserve app data. Debug builds display update prompts but do not enforce the 14-day lock. The lock is active only for signed release builds.
