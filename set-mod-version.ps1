# set-mod-version.ps1
# Updates mod_version in gradle.properties so all version subprojects pick it up at build time.
# Usage:
#   .\set-mod-version.ps1 1.2.0        # set to 1.2.0
#   .\set-mod-version.ps1              # print current version and exit

param (
    [string]$NewVersion = ""
)

$propsFile = Join-Path $PSScriptRoot "gradle.properties"

if (-not (Test-Path $propsFile)) {
    Write-Host "[ERROR] " -ForegroundColor Red -NoNewline
    Write-Host "Could not find gradle.properties at: $propsFile"
    exit 1
}

$content = Get-Content $propsFile -Raw

# Extract current version
if ($content -match '(?m)^mod_version\s*=\s*(.+)$') {
    $currentVersion = $Matches[1].Trim()
} else {
    Write-Host "[ERROR] " -ForegroundColor Red -NoNewline
    Write-Host "mod_version key not found in gradle.properties."
    exit 1
}

if ($NewVersion -eq "") {
    Write-Host "[INFO] " -ForegroundColor Blue -NoNewline
    Write-Host "Current mod_version: " -NoNewline
    Write-Host $currentVersion -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Usage: .\set-mod-version.ps1 <new-version>"
    exit 0
}

# Validate semver-ish format (digits and dots, optional pre-release suffix)
if ($NewVersion -notmatch '^[0-9]+\.[0-9]+(\.[0-9]+)?([.\-][a-zA-Z0-9.\-]+)?$') {
    Write-Host "[WARN] " -ForegroundColor Yellow -NoNewline
    Write-Host "Version '$NewVersion' does not look like a standard version (e.g. 1.2.0). Continuing anyway."
}

$newContent = $content -replace '(?m)^(mod_version\s*=\s*)(.+)$', "`${1}$NewVersion"

Set-Content -Path $propsFile -Value $newContent -NoNewline

Write-Host "[SUCCESS] " -ForegroundColor Green -NoNewline
Write-Host "mod_version updated: " -NoNewline
Write-Host $currentVersion -ForegroundColor DarkGray -NoNewline
Write-Host " -> " -NoNewline
Write-Host $NewVersion -ForegroundColor Cyan
Write-Host ""
Write-Host "[INFO] " -ForegroundColor Blue -NoNewline
Write-Host "All version subprojects will use '$NewVersion' on next build (fabric.mod.json is populated at build time via `"`${version}`")."
