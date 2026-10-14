param([string]$JavaHome)

$ErrorActionPreference = 'Stop'
$savedJavaHome = $env:JAVA_HOME
$projectDirectory = $PSScriptRoot
try {
    if (-not $JavaHome) {
        $candidates = @(
            $env:JAVA_HOME,
            'D:\Android studio\jbr',
            "$env:ProgramFiles\Android\Android Studio\jbr"
        )
        $JavaHome = $candidates | Where-Object {
            $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe'))
        } | Select-Object -First 1
    }
    if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
        throw 'Please provide the Android Studio JDK folder using -JavaHome.'
    }
    $env:JAVA_HOME = $JavaHome
    Push-Location $projectDirectory
    try {
        & '.\gradlew.bat' --no-daemon --console=plain assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "Android build failed (exit $LASTEXITCODE)." }
        $apk = Join-Path $projectDirectory 'app\build\outputs\apk\debug\app-debug.apk'
        if (-not (Test-Path -LiteralPath $apk)) { throw 'The build produced no APK.' }
        Write-Output "APK: $apk"
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $savedJavaHome
}
