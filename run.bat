@echo off
setlocal

rem ============================================================
rem SVN Incremental Packager - one-click launcher
rem Finds an existing Java (8 / 11 / 17 / 21 ...) and runs the
rem shaded JAR. You do NOT need to install "Java 8 Update 491".
rem ============================================================

set "APP_HOME=%~dp0"
set "JAR=%APP_HOME%target\svn-packager-1.0.0.jar"

rem 1) Optional: point to a specific JRE/JDK you already have
rem    (e.g. Java 11 / 17 / 21). Uncomment and edit the line below.
rem set "JAVA_BIN=C:\Program Files\Java\jdk-21\bin\java.exe"

rem 2) Otherwise, use "java" from the system PATH
if not defined JAVA_BIN (
    for /f "delims=" %%i in ('where java 2^>nul') do (
        set "JAVA_BIN=%%i"
        goto :havejava
    )
)

:havejava
if not defined JAVA_BIN (
    echo [ERROR] Java runtime not found.
    echo Please install Java 8 or newer, or set JAVA_BIN at the top of this script.
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo [ERROR] Cannot find %JAR%
    echo Please build first with: mvn clean package
    pause
    exit /b 1
)

echo Using Java: %JAVA_BIN%
echo Starting SVN Incremental Packager...
"%JAVA_BIN%" -jar "%JAR%"

if errorlevel 1 (
    echo.
    echo [ERROR] The application exited with an error. See output above.
    pause
)
endlocal
