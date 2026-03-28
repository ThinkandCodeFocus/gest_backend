@echo off
where mvn >nul 2>nul
if %errorlevel%==0 (
  mvn %*
  exit /b %errorlevel%
)

set "LOCAL_MVN=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%LOCAL_MVN%" (
  call "%LOCAL_MVN%" %*
  exit /b %errorlevel%
)

echo Maven is not installed on this machine and no local Maven was found at .tools\apache-maven-3.9.9.
echo To fix: install Maven globally OR run once the setup command to download local Maven.
exit /b 1
