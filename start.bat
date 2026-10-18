@echo off
REM Start Cinema Management System on port 8090 with native auth DLL
setlocal
cd /d "%~dp0"

REM Set java.library.path to current dir so mssql-jdbc_auth-*.dll is found
set "DEST=%~dp0target\dll"
if not exist "%DEST%" mkdir "%DEST%"
if not exist "%DEST%\mssql-jdbc_auth-12.6.4.x64.dll" (
  copy /Y "%~dp0src\main\resources\mssql-jdbc_auth-12.6.4.x64.dll" "%DEST%\" >nul
)

REM Use -Djava.library.path so Windows Integrated Auth works
java -Djava.library.path="%DEST%" -Dserver.port=8090 -jar "%~dp0target\cinema-management-system-0.0.1-SNAPSHOT.jar"
endlocal
