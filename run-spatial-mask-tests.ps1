# Run SpatialTokenMask unit tests (OsmAnd PR #25535)
# Requires: Eclipse Temurin JDK 17 (winget install EclipseAdoptium.Temurin.17.JDK)

$jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
if (-not (Test-Path "$jdk\bin\java.exe")) {
    Write-Error "JDK 17 not found at $jdk. Install: winget install EclipseAdoptium.Temurin.17.JDK"
    exit 1
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + $env:Path
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
$env:GRADLE_OPTS = "-Xmx4g"

Set-Location $PSScriptRoot

Write-Host "=== JUnit: SpatialTokenMaskTest ===" -ForegroundColor Cyan
.\gradlew.bat ":OsmAnd-java:test" "--tests" "net.osmand.search.core.spatial.SpatialTokenMaskTest" --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n=== Python: mask_verify.py ===" -ForegroundColor Cyan
python OsmAnd-java\src\test\resources\spatial\mask_verify.py
exit $LASTEXITCODE
