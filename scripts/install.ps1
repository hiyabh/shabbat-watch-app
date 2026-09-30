# Builds the debug APK and installs it on the connected watch.
# Build outputs live under %LOCALAPPDATA%\shabbat-watch-build (see build.gradle.kts).
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $env:LOCALAPPDATA "shabbat-watch-build\app\outputs\apk\debug\app-debug.apk"

if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdk" -Directory -Filter "jdk-17*" | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}

Push-Location $root
try {
    & .\gradlew.bat assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed - is the watch connected? (scripts/watch-connect.ps1)" }
    & $adb shell am start -n il.hiya.shabbatwatch/.MainActivity
} finally {
    Pop-Location
}
