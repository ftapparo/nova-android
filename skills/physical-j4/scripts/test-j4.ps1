[CmdletBinding()]
param(
    [ValidateSet("Status", "Install", "Validate", "Instrumented", "Launch", "Logs", "Screenshot")]
    [string]$Mode = "Status"
)

$ErrorActionPreference = "Stop"
$utf8 = [System.Text.UTF8Encoding]::new($false)
[Console]::InputEncoding = $utf8
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$gradle = Join-Path $projectRoot "gradlew.bat"
$packageName = "br.com.novaresidence"

function Find-Adb {
    $candidates = @()
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
    }
    if ($env:ANDROID_HOME) {
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    }
    $candidates += "D:\Android\SDK\platform-tools\adb.exe"

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw "ADB não foi encontrado. Configure ANDROID_SDK_ROOT ou instale Android SDK Platform-Tools."
}

function Get-PhysicalJ4Serial([string]$adb) {
    $lines = & $adb devices -l | Select-Object -Skip 1 | Where-Object { $_.Trim() }
    $devices = foreach ($line in $lines) {
        $parts = $line -split "\s+"
        [PSCustomObject]@{
            Serial = $parts[0]
            State = $parts[1]
            Details = $line
        }
    }

    $j4Devices = $devices | Where-Object {
        $_.Serial -notlike "emulator-*" -and
        $_.Details -match "(?i)(model:SM[_-]?J410|device:j4corelte|product:j4corelte)"
    }

    if ($env:NOVA_DEVICE_SERIAL) {
        $selected = $j4Devices | Where-Object Serial -eq $env:NOVA_DEVICE_SERIAL
        if (-not $selected) {
            throw "NOVA_DEVICE_SERIAL não identifica um J4 físico conectado: $env:NOVA_DEVICE_SERIAL"
        }
        if ($selected.State -ne "device") {
            throw "O J4 selecionado está em estado '$($selected.State)'. Desbloqueie-o e autorize a depuração USB."
        }
        return $selected.Serial
    }

    $ready = @($j4Devices | Where-Object State -eq "device")
    if ($ready.Count -eq 0) {
        $found = if ($devices) { ($devices | ForEach-Object Details) -join [Environment]::NewLine } else { "nenhum" }
        throw "Nenhum J4 físico autorizado foi encontrado. Dispositivos ADB:`n$found"
    }
    if ($ready.Count -gt 1) {
        throw "Há mais de um J4 físico conectado. Defina NOVA_DEVICE_SERIAL para escolher um deles."
    }
    return $ready[0].Serial
}

function Find-JavaHome {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    $candidates += "D:\Android\Android Studio\jbr"
    $candidates += "C:\Program Files\Android\Android Studio\jbr"

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe")) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw "Java não foi encontrado. Instale o JDK do Android Studio ou defina JAVA_HOME para um JDK que contenha bin\\java.exe."
}

function Invoke-Gradle([string]$serial, [string[]]$arguments) {
    if (-not (Test-Path -LiteralPath $gradle)) {
        throw "gradlew.bat não foi encontrado em $projectRoot"
    }
    $previousSerial = $env:ANDROID_SERIAL
    $previousJavaHome = $env:JAVA_HOME
    try {
        $env:ANDROID_SERIAL = $serial
        $env:JAVA_HOME = Find-JavaHome
        Push-Location $projectRoot
        & $gradle @arguments "--console=plain"
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle terminou com código $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
        $env:ANDROID_SERIAL = $previousSerial
        $env:JAVA_HOME = $previousJavaHome
    }
}

$adb = Find-Adb
$serial = Get-PhysicalJ4Serial $adb
$model = (& $adb -s $serial shell getprop ro.product.model).Trim()
$androidVersion = (& $adb -s $serial shell getprop ro.build.version.release).Trim()
Write-Host "J4 físico conectado: $serial ($model, Android $androidVersion)"

switch ($Mode) {
    "Status" {
        & $adb -s $serial get-state
    }
    "Install" {
        Invoke-Gradle $serial @("installDebug")
        Write-Host "APK instalado no J4. Abra Nova Residence manualmente para testar."
    }
    "Validate" {
        Invoke-Gradle $serial @("lint", "testDebugUnitTest", "assembleDebug", "installDebug")
        Write-Host "Validação concluída e APK instalado no J4."
    }
    "Instrumented" {
        Write-Warning "Testes instrumentados podem substituir a instalação de depuração e encerrar a sessão aberta no aparelho."
        # O runner do Gradle pode encerrar com sucesso sem executar casos se o APK já estiver instalado.
        Invoke-Gradle $serial @("assembleDebug", "assembleDebugAndroidTest")
        $mainApk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
        $testApk = Join-Path $projectRoot "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
        foreach ($apk in @($mainApk, $testApk)) {
            if (-not (Test-Path -LiteralPath $apk)) { throw "APK de teste não encontrado: $apk" }
            & $adb -s $serial install -r $apk | Out-Host
            if ($LASTEXITCODE -ne 0) { throw "Não foi possível instalar $apk no J4." }
        }
        $result = (& $adb -s $serial shell am instrument -w "$packageName.test/androidx.test.runner.AndroidJUnitRunner" | Out-String)
        Write-Host $result
        if ($LASTEXITCODE -ne 0 -or $result -notmatch 'OK \([1-9]\d* tests?\)') {
            throw "Os testes instrumentados não terminaram com casos aprovados no J4."
        }
    }
    "Launch" {
        & $adb -s $serial shell monkey -p $packageName 1 | Out-Null
        Write-Host "Nova Residence aberto no J4."
    }
    "Logs" {
        $pid = (& $adb -s $serial shell pidof $packageName).Trim()
        if ($pid) {
            & $adb -s $serial logcat --pid=$pid -v threadtime
        }
        else {
            Write-Warning "O aplicativo não está em execução; mostrando somente linhas relacionadas ao pacote."
            & $adb -s $serial logcat -v threadtime | Select-String -Pattern "br.com.novaresidence|AndroidRuntime"
        }
    }
    "Screenshot" {
        $evidenceDir = Join-Path $projectRoot ("artifacts\physical-j4\" + (Get-Date -Format "yyyyMMdd-HHmmss"))
        New-Item -ItemType Directory -Path $evidenceDir -Force | Out-Null
        $destination = Join-Path $evidenceDir "tela.png"
        $remoteCapture = "/sdcard/nova-residence-test-capture.png"
        try {
            & $adb -s $serial shell screencap -p $remoteCapture
            if ($LASTEXITCODE -ne 0) { throw "Não foi possível capturar a tela do J4." }
            & $adb -s $serial pull $remoteCapture $destination | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "Não foi possível copiar a captura do J4." }
        }
        finally {
            & $adb -s $serial shell rm -f $remoteCapture | Out-Null
        }
        Write-Host "Captura salva em $destination"
    }
}
