@echo off
rem Builds the mod jar. Double-click it or run "build.bat" from any folder.
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem Gradle 7.6 needs Java 17: take JAVA_HOME if it is 17, otherwise look in the usual install folders
set "JDK="
if defined JAVA_HOME call :check "!JAVA_HOME!"
if not defined JDK for %%R in ("%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Java" "%ProgramFiles%\Microsoft" "%ProgramFiles%\Zulu" "%ProgramFiles%\BellSoft" "%USERPROFILE%\.jdks") do (
    for /d %%D in ("%%~R\*17*") do if exist "%%~D\bin\java.exe" set "JDK=%%~D"
)
if not defined JDK goto nojdk

set "JAVA_HOME=!JDK!"
echo Using JDK: !JAVA_HOME!
echo.

rem Clean build? Throws away everything Gradle built before (old jars, old resources): slower but always fresh.
choice /c YN /t 15 /d N /m "Clean build (Y) or quick build (N, in 15 s)"
if errorlevel 2 (
    set "TASKS=build"
) else (
    set "TASKS=clean build"
    if exist "build\libs" rmdir /s /q "build\libs"
)
echo.
echo Running: gradlew !TASKS!
call gradlew.bat !TASKS! --no-daemon
if errorlevel 1 goto failed

set "JAR="
for %%F in ("build\libs\militarytech-*.jar") do (
    echo %%~nF | findstr /i /c:"sources" /c:"dev" >nul || set "JAR=%%~fF"
)
echo.
echo Done. The mod is:
echo     !JAR!

rem Put it in the game's mods folder, replacing the old Military Tech jar (two jars = the old one may win)
set "MODS=%APPDATA%\.minecraft\mods"
if not exist "!MODS!" goto nomods
choice /c YN /t 15 /d Y /m "Copy it to !MODS! and remove the old militarytech jar (Y in 15 s)"
if errorlevel 2 goto nomods
del /q "!MODS!\militarytech-*.jar" 2>nul
copy /y "!JAR!" "!MODS!\" >nul && echo Copied to !MODS!
:nomods
start "" "build\libs"
pause
exit /b 0

:check
"%~1\bin\java.exe" -version 2>&1 | findstr /r /c:"version .17\." >nul && set "JDK=%~1"
exit /b 0

:nojdk
echo.
echo [!] JDK 17 not found.
echo     Install it:  winget install EclipseAdoptium.Temurin.17.JDK
echo     or set JAVA_HOME to a JDK 17 folder and run build.bat again.
echo.
pause
exit /b 1

:failed
echo.
echo [!] Build failed, see the log above.
pause
exit /b 1
