Set-Location $PSScriptRoot

if (!(Test-Path "target/classes")) {
    New-Item -ItemType Directory -Path "target/classes" | Out-Null
}

javac -encoding UTF-8 -d target/classes src/main/java/com/eclinic/*.java
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java -cp target/classes com.eclinic.App