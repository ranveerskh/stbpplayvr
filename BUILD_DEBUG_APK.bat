@echo off
call gradlew.bat assembleDebug
if errorlevel 1 (
  echo.
  echo Build failed. Open the project in Android Studio, complete Gradle sync, and run again.
  pause
  exit /b 1
)
echo.
echo APK created at app\build\outputs\apk\debug\app-debug.apk
pause
