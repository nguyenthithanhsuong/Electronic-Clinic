[CmdletBinding()]
param(
    [switch]$ForceRestart
)

$ErrorActionPreference = 'Stop'

$workspaceRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $workspaceRoot

$jdkRoot = Join-Path $env:LOCALAPPDATA 'Programs\Java\temurin17'
if (-not (Test-Path $jdkRoot)) {
    Write-Error "JDK 17 not found at $jdkRoot. Install Temurin 17 first."
    exit 1
}

$jdkHome = Get-ChildItem $jdkRoot -Directory | Select-Object -First 1 -ExpandProperty FullName
$javac = Join-Path $jdkHome 'bin\javac.exe'
$java = Join-Path $jdkHome 'bin\java.exe'

if (-not (Test-Path $javac)) {
    Write-Error "javac not found: $javac"
    exit 1
}
if (-not (Test-Path $java)) {
    Write-Error "java not found: $java"
    exit 1
}

$sources = Get-ChildItem -Path 'src/main/java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) {
    Write-Error 'No Java source files found under src/main/java'
    exit 1
}

$listenLine = netstat -ano | Select-String -Pattern ':3001\s+.*LISTENING\s+\d+$' | Select-Object -First 1
if ($listenLine) {
    $existingPid = ($listenLine.Line -split '\s+')[-1]
    if ($ForceRestart) {
        Write-Host "Port 3001 is in use by PID $existingPid. Stopping it because -ForceRestart was provided..."
        Stop-Process -Id ([int]$existingPid) -Force
    } else {
        Write-Error "Port 3001 is already in use (PID $existingPid). Close that process or rerun with: .\\run-clinic.ps1 -ForceRestart"
        exit 1
    }
}

Write-Host 'Compiling Clinic sources...'
& $javac -encoding UTF-8 -cp 'lib/postgresql-42.7.5.jar;lib/jbcrypt-0.4.jar;lib/jjwt-0.9.1.jar;lib/jackson-databind-2.12.7.1.jar;lib/jackson-core-2.12.7.jar;lib/jackson-annotations-2.12.7.jar;lib/jaxb-api-2.3.1.jar;lib/jaxb-runtime-2.3.3.jar' -d 'target/classes' $sources
if ($LASTEXITCODE -ne 0) {
    Write-Error 'Compilation failed.'
    exit $LASTEXITCODE
}

Write-Host 'Starting Clinic REST server at http://localhost:3001'
& $java -cp 'target/classes;lib/postgresql-42.7.5.jar;lib/jbcrypt-0.4.jar;lib/jjwt-0.9.1.jar;lib/jackson-databind-2.12.7.1.jar;lib/jackson-core-2.12.7.jar;lib/jackson-annotations-2.12.7.jar;lib/jaxb-api-2.3.1.jar;lib/jaxb-runtime-2.3.3.jar' com.eclinic.api.RestServer
