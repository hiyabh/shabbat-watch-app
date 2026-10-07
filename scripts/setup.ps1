# End-user setup: connects to the watch, installs a downloaded APK and grants the one-time
# permissions. No build tools needed - only adb (Android platform-tools).
#
# First time (from the watch's "Pair new device" screen):
#   .\setup.ps1 -Apk .\shabbat-watch-0.3.0.apk -Ip 192.168.1.50 -Port 40001 -PairPort 40002 -Code 123456
# Later updates (already paired):
#   .\setup.ps1 -Apk .\shabbat-watch-0.4.0.apk -Ip 192.168.1.50 -Port 40001
param(
    [Parameter(Mandatory = $true)][string]$Apk,
    [Parameter(Mandatory = $true)][string]$Ip,
    [Parameter(Mandatory = $true)][int]$Port,
    [int]$PairPort = 0,
    [string]$Code = ""
)

$PlatformToolsUrl = "https://developer.android.com/tools/releases/platform-tools"
$Package = "il.hiya.shabbatwatch"

function Find-Adb {
    $onPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    $candidates = @(
        (Join-Path $PSScriptRoot "adb.exe"),
        (Join-Path $PSScriptRoot "platform-tools\adb.exe"),
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) { return $candidate }
    }
    throw "adb was not found. Download 'SDK Platform-Tools' from $PlatformToolsUrl, unzip it next to this script and run again."
}

if (-not (Test-Path $Apk)) { throw "APK not found at '$Apk'. Download it from the project's Releases page." }
$adb = Find-Adb
$device = "${Ip}:${Port}"

if ($PairPort -gt 0 -and $Code -ne "") {
    & $adb pair "${Ip}:${PairPort}" $Code
    if ($LASTEXITCODE -ne 0) { throw "Pairing failed. Check the pairing port and code on the watch (they change every time the screen is opened)." }
}

& $adb connect $device | Out-Host
$state = & $adb -s $device get-state 2>$null
if ($state -ne "device") {
    throw "Could not connect to $device. Make sure Wireless debugging is on, the watch and the PC are on the same Wi-Fi, and the IP and port match the watch screen."
}

Write-Host "== Installing =="
& $adb -s $device install -r $Apk
if ($LASTEXITCODE -ne 0) {
    throw "Install failed. If an older build signed with another key is installed, turn Shabbat mode off, uninstall it ($adb -s $device uninstall $Package) and run again."
}

Write-Host "== Granting one-time permissions =="
& $adb -s $device shell pm grant $Package android.permission.WRITE_SECURE_SETTINGS
& $adb -s $device shell cmd notification allow_dnd $Package
& $adb -s $device shell appops set $Package SYSTEM_ALERT_WINDOW allow
& $adb -s $device shell appops set $Package WRITE_SETTINGS allow
& $adb -s $device shell pm grant $Package android.permission.POST_NOTIFICATIONS
& $adb -s $device shell dumpsys deviceidle whitelist +$Package

& $adb -s $device shell am start -n "$Package/.MainActivity" | Out-Null
Write-Host "Done. The app is open on the watch; no red warning on its home screen means all permissions were granted."
