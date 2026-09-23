@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information.
@echo off
setlocal
for %%i in ("%~dp0.") do set MAVEN_PROJECTBASEDIR=%%~fi
if exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" goto runwrapper
echo Maven wrapper JAR is missing. Run "mvn wrapper:wrapper" once or use Docker Compose.
exit /b 1
:runwrapper
"%JAVA_HOME%\bin\java.exe" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
