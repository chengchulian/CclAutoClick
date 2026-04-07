# CclAutoClick EXE Build Script
# Using launch4j to create Windows EXE

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CclAutoClick EXE Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Maven build with launch4j
Write-Host "[1/2] Running Maven build with launch4j..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Maven build failed" -ForegroundColor Red
    exit 1
}
Write-Host "Maven build successful" -ForegroundColor Green
Write-Host ""

# Find generated EXE
Write-Host "[2/2] Finding generated EXE file..." -ForegroundColor Yellow
$exeFile = Get-ChildItem -Path "target" -Filter "*.exe" | Select-Object -First 1

if (-not $exeFile) {
    Write-Host "Error: No EXE file found" -ForegroundColor Red
    exit 1
}

Write-Host "Found EXE: $($exeFile.Name)" -ForegroundColor Green
Write-Host ""

# Create output directory
$outputDir = "dist"
if (Test-Path $outputDir) {
    Remove-Item -Recurse -Force $outputDir
}
New-Item -ItemType Directory -Path $outputDir | Out-Null

# Copy EXE and JAR to dist
Copy-Item $exeFile.FullName -Destination $outputDir
$jarFile = Get-ChildItem -Path "target" -Filter "*.jar" | Where-Object { $_.Name -notmatch "-original" } | Select-Object -First 1
if ($jarFile) {
    Copy-Item $jarFile.FullName -Destination $outputDir
}

Write-Host "========================================" -ForegroundColor Green
Write-Host "  Build Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Output directory: $outputDir" -ForegroundColor Cyan
Write-Host ""

# List generated files
Write-Host "Generated files:" -ForegroundColor Cyan
Get-ChildItem -Path $outputDir | ForEach-Object {
    $sizeMB = [math]::Round($_.Length / 1MB, 2)
    Write-Host "  - $($_.Name) ($sizeMB MB)" -ForegroundColor White
}

Write-Host ""
Write-Host "Tips:" -ForegroundColor Yellow
Write-Host "  1. Run the .exe file directly (requires JRE 21+)" -ForegroundColor White
Write-Host "  2. The EXE is a launcher that wraps the JAR file" -ForegroundColor White
Write-Host "  3. Distribute both EXE and JAR files together" -ForegroundColor White
Write-Host ""
