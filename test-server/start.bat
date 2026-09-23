@echo off
rem Local Hounded test server. Deploy the plugin first with: gradlew deployToTestServer
setlocal
if defined JAVA25_HOME (set "JAVA=%JAVA25_HOME%\bin\java.exe") else (set "JAVA=C:\Program Files\Amazon Corretto\jdk25.0.2_10\bin\java.exe")
cd /d "%~dp0"
"%JAVA%" -Xms1G -Xmx2G -jar paper.jar --nogui
endlocal
