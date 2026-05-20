# copies all built files to output folder
$outputDir = "output"
# Create the output directory if it doesn't exist
if (-not (Test-Path -Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

function log($level, $message) {
    switch ($level) {
        "INFO" { 
            Write-Host "[INFO] " -ForegroundColor Blue -NoNewline
            Write-Host "$message"
            }
        "DEBUG" { 
            if ($env:NODEBUG -eq "true") { 
                return 
            } else {
                Write-Host "[DEBUG] " -ForegroundColor Cyan -NoNewline
                Write-Host "$message" 
            }
            }
        "ERROR" { 
            Write-Host "[ERROR] " -ForegroundColor Red -NoNewline
            Write-Host "$message" 
            Write-Host "Press any key to continue..." -ForegroundColor Yellow
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
            exit 1 
            }
        "SUCCESS" { 
            Write-Host "[SUCCESS] " -ForegroundColor Green -NoNewline
            Write-Host "$message" 
            }
        default { Write-Host "$message" }
    }
}

Remove-Item -Path "$outputDir\*" -Recurse -Force -ErrorAction SilentlyContinue
$destinationPath = $outputDir
$versionsDir = "versions"
$versionFolders = Get-ChildItem -Path $versionsDir -Directory
$versionCount = $versionFolders.Count
log "INFO" " Starting file copy process..."
log "INFO" " Found $versionCount version(s) in the versions folder."

foreach ($versionFolder in $versionFolders) {
    $version = $versionFolder.Name
    log "INFO" " Found version $version"
    $sourceDir = "versions/$version/build/libs"
    $sourcePath = Join-Path -Path "$sourceDir" -ChildPath "qolmod-$version.jar"
    log "DEBUG" " Detected source path: $sourcePath"
    log "DEBUG" " Destination path: $destinationPath"
    log "INFO" " Attempting to copy file for version $version to output folder..."
    try {
        Copy-Item -Path $sourcePath -Destination $destinationPath -Force -ErrorAction Stop
    }
    catch {
        log "ERROR" " Failed to copy files for version $version. Exception: $($_.Exception.Message)"
        exit 1
    }
    Write-Host "[INFO]" -ForegroundColor Blue -NoNewline 
    Write-Host " COPY SUCCESS" -ForegroundColor Green -NoNewline
    Write-Host " Successfully copied files for version $version."
}