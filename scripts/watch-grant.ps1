# One-time grants the app needs. Run after the first install (and after every reinstall
# that changes the signing key). Requires an adb connection (scripts/watch-connect.ps1).
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$pkg = "il.hiya.shabbatwatch"

Write-Host "== WRITE_SECURE_SETTINGS (wake gestures / always-on display) =="
& $adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS

Write-Host "== Notification policy access (do not disturb) =="
& $adb shell cmd notification allow_dnd $pkg

Write-Host "== SYSTEM_ALERT_WINDOW app-op (lets the guard service bring the clock back) =="
& $adb shell appops set $pkg SYSTEM_ALERT_WINDOW allow

Write-Host "== WRITE_SETTINGS app-op (system namespace, e.g. screen timeout) =="
& $adb shell appops set $pkg WRITE_SETTINGS allow

Write-Host "== POST_NOTIFICATIONS (guard service notification) =="
& $adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS

Write-Host "== Battery optimisation exemption (keeps the guard service alive all Shabbat) =="
& $adb shell dumpsys deviceidle whitelist +$pkg

Write-Host "== Verify =="
& $adb shell dumpsys package $pkg | Select-String "WRITE_SECURE_SETTINGS|POST_NOTIFICATIONS"
& $adb shell appops get $pkg SYSTEM_ALERT_WINDOW
& $adb shell appops get $pkg WRITE_SETTINGS
& $adb shell cmd notification list_dnd 2>$null
