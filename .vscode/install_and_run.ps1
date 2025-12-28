param(
  [string]$Workspace = (Get-Location).Path
)

# Resolve adb path
$adb = $null
if ($env:ANDROID_SDK_ROOT) {
  $candidate = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
  if (Test-Path $candidate) { $adb = $candidate }
}
if (-not $adb) { $candidate = 'E:\Sdk\platform-tools\adb.exe'; if (Test-Path $candidate) { $adb = $candidate } }
if (-not $adb) { Write-Error "adb not found. Set ANDROID_SDK_ROOT or install platform-tools."; exit 2 }

Write-Output "Using adb: $adb"

& $adb devices

Write-Output "Waiting for emulator device..."
while ((& $adb devices) -split "`n" | Where-Object { $_ -match '\S' -and $_ -notmatch 'List of devices attached' } | Where-Object { $_ -match '\s+device(\s|$)' } | Measure-Object | Select-Object -ExpandProperty Count -First 1) {
  break
}
# Above loop uses a break immediate; implement robust wait instead
while ( -not ((& $adb devices) -split "`n" | Where-Object { $_ -match '\S' -and $_ -notmatch 'List of devices attached' } | Where-Object { $_ -match '\s+device(\s|$)' } ) ) {
  Start-Sleep -Seconds 1
}

Write-Output "Device reported by adb. Waiting for full boot..."
while ( (& $adb shell getprop sys.boot_completed 2>$null).Trim() -ne '1' ) {
  Start-Sleep -Seconds 2
}

Write-Output "Boot complete. Installing APK..."
$apk = Join-Path $Workspace "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { Write-Error "APK not found: $apk"; exit 3 }

& $adb install -r $apk

Write-Output "Starting app activity..."
& $adb shell am start -n com.example.richtexteditor/.RichTextEditorActivity

Write-Output "Install & run completed."
exit 0
