@echo off
setlocal enableextensions enabledelayedexpansion

REM Interactive build helper for QoLMod
REM - Prompts for a Minecraft version (e.g. 1.19.4). Leave blank to build all versions.
REM - Runs Gradle build for the selected version(s) and reports locations of produced jars.

echo QoLMod build helper
echo --------------------
set /p VERSION="Enter version to build (leave blank for all): "

if "%VERSION%"=="" (
	echo Building all versions...
	set GRADLE_TASK=build
) else (
	echo Building version %VERSION% ...
	set GRADLE_TASK=:versions:%VERSION%:build
)

echo Running Gradle. This may take a while...
call gradlew %GRADLE_TASK% -x test

if ERRORLEVEL 1 (
	echo.
	echo Build failed. See Gradle output above for details.
	exit /b 1
)

echo.
echo Build succeeded. Locating generated jars...

REM If specific version was built, list jars for that version; otherwise list jars for every version subproject
if "%VERSION%"=="" (
	for /f "delims=" %%D in ('dir /b /ad versions') do (
		call :ListJarsForVersion "%%D"
	)
) else (
	call :ListJarsForVersion "%VERSION%"
)

echo.
echo Also check top-level build/libs/ for any combined artifacts.
if exist build\libs\ (
	for %%F in (build\libs\*.jar) do echo %%~fF
) else (
	echo (no top-level jars found)
)

echo.
echo Done.
exit /b 0

:ListJarsForVersion
setlocal
set VER=%~1
if not exist versions\%VER%\build\libs\ (
	echo [%VER%] No build/libs/ found (project may not have been built)
	endlocal & goto :eof
)
echo [%VER%] JARs:
for %%J in (versions\%VER%\build\libs\*.jar) do (
	echo  %%~fJ
)
endlocal
goto :eof