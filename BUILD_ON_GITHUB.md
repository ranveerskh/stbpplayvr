# Build STB PLAY v1.8.16 APK with GitHub Actions

1. Put this project in a GitHub repository. Use a private repository if you want to keep the source private.
2. Push the project to the `main` branch, or open the repository's **Actions** tab and run **Build STB PLAY Android TV APK** with **Run workflow**.
3. When the workflow succeeds, open that run and download the artifact named `STB-PLAY-Android-TV-v1.8.16-debug-APK`.
4. Extract the artifact ZIP. The `app-debug.apk` inside is the debug APK for sideload testing.

The workflow builds the project as supplied. A successful Android build confirms APK packaging; the app still needs to be tested on the Meta Quest headset for input and playback compatibility.
