@echo off
setlocal
cd /d "%~dp0\.."

if not exist "target\auto-testing-imd-0.1.0.jar" (
    call mvn package
    if errorlevel 1 exit /b %errorlevel%
)

java -cp "target\auto-testing-imd-0.1.0.jar;target\dependency\*" com.testpilot.Launcher
