@echo off
setlocal
cd /d "%~dp0\.."

if not exist "target\testpilot-studio-0.1.0.jar" (
    call mvn package
    if errorlevel 1 exit /b %errorlevel%
)

java -cp "target\testpilot-studio-0.1.0.jar;target\dependency\*" com.testpilot.Launcher
