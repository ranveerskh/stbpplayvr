The video.mp4 fixture is synthetic, generated with FFmpeg. It contains a test pattern and a quiet sine wave. It is used only by instrumentation tests and is not packaged into the release APK.

Regenerate it from this directory:

```sh
ffmpeg -hide_banner -loglevel error -f lavfi -i testsrc2=size=160x90:rate=15 -f lavfi -i sine=frequency=440:sample_rate=48000 -t 20 -c:v libx264 -profile:v baseline -level 3.0 -pix_fmt yuv420p -crf 35 -c:a aac -b:a 24k -af volume=0.03 -movflags +faststart video.mp4
```

For local emulator playback checks, serve this directory on the host:

```sh
python3 -m http.server 8765 --bind 127.0.0.1
```
