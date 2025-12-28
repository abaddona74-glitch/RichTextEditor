@echo off
setlocal
REM Helper to install system image and create Pixel_9_Pro AVD using SDK from local.properties or E:\Sdk
set "DEFAULT_SDK=E:\Sdk"
set "SDKDIR=%DEFAULT_SDK%"
if exist local.properties (
  for /f "usebackq tokens=1* delims==" %%A in ("local.properties") do (
    if /i "%%A"=="sdk.dir" set "SDKDIR=%%B"
  )
)
echo Using SDK at: %SDKDIR%

set "SDKMAN=%SDKDIR%\cmdline-tools\latest\bin\sdkmanager.bat"
set "AVDMAN=%SDKDIR%\cmdline-tools\latest\bin\avdmanager.bat"
if not exist "%SDKMAN%" set "SDKMAN=%SDKDIR%\tools\bin\sdkmanager.bat"
if not exist "%AVDMAN%" set "AVDMAN=%SDKDIR%\tools\bin\avdmanager.bat"

if not exist "%SDKMAN%" (
  echo sdkmanager not found. Please install Android SDK command-line tools or use Android Studio SDK Manager.
  exit /b 1
)
if not exist "%AVDMAN%" (
  echo avdmanager not found. Please install Android SDK command-line tools or use Android Studio SDK Manager.
  exit /b 1
)

echo Installing required SDK components (may ask to accept licenses)...
"%SDKMAN%" "platform-tools" "emulator" "platforms;android-36" "system-images;android-36;google_apis;x86_64"

echo Creating AVD Pixel_9_Pro (force if exists)...
"%AVDMAN%" create avd -n Pixel_9_Pro -k "system-images;android-36;google_apis;x86_64" --device "pixel" --force

if %errorlevel% neq 0 (
  echo Failed to create AVD. Check output above.
  exit /b %errorlevel%
)

echo AVD Pixel_9_Pro created.
exit /b 0
