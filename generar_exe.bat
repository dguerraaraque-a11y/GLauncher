@echo off
setlocal EnableDelayedExpansion
title Creando Instalador GLauncher v1.0
color 0a
cls

echo ==========================================
echo       CONSTRUCTOR DE GLAUNCHER v1.0
echo ==========================================
echo.

set "BUILD_DIR=%~dp0GLauncher_v1_Dist"
set "SETUP_DIR=%~dp0GLauncher_Setup"
set "TEMP_INPUT=%~dp0temp_input"
set "FX_LIB=%~dp0lib\javafx-sdk-17.0.13\lib"
set "FX_BIN=%~dp0lib\javafx-sdk-17.0.13\bin"
set "LIBS_CP="
for /r "%~dp0lib" %%f in (*.jar) do (
    set "LIBS_CP=!LIBS_CP!;%%f"
)
set "LIBS_CP=%LIBS_CP:~1%"

echo [1/7] Limpiando archivos anteriores...
if exist %BUILD_DIR% rmdir /s /q %BUILD_DIR%
if exist %SETUP_DIR% rmdir /s /q %SETUP_DIR%
if exist %TEMP_INPUT% rmdir /s /q %TEMP_INPUT%
if exist out rmdir /s /q out
mkdir out
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%TEMP_INPUT%" mkdir "%TEMP_INPUT%"

echo [2/7] Compilando codigo fuente...
if exist sources.txt del /f /q sources.txt
powershell -Command "Get-ChildItem -Path '%~dp0src' -Recurse -Filter *.java | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ASCII sources.txt"

javac -encoding UTF-8 -cp "%LIBS_CP%" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web -d out @sources.txt

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Fallo la compilacion. 
    echo Posibles causas: 
    echo 1. Hay un error de sintaxis en el nuevo MusicView.java
    echo 2. Las librerias de JavaFX no estan en %FX_LIB%
    pause
    exit /b
)
del sources.txt

echo [3/7] Creando JAR...
echo Main-Class: glauncher.GLauncher > manifest.txt
set "CP_LIST="
set "LIB_DIR=%~dp0lib"
pushd "%LIB_DIR%"
for /r %%f in (*.jar) do (
    set "jar_path=%%f"
    set "relative_path=!jar_path:%LIB_DIR%\=!"
    set "relative_path=!relative_path:\=/!"
    set "CP_LIST=!CP_LIST! lib/!relative_path!"
)
popd
echo Class-Path: !CP_LIST! >> manifest.txt
jar cmf manifest.txt %TEMP_INPUT%\GLauncher.jar -C out .
del manifest.txt

echo [4/7] Copiando recursos y DLLs...
xcopy "%~dp0lib" "%BUILD_DIR%\lib\" /E /I /Y >nul
xcopy "%~dp0assets" "%BUILD_DIR%\assets\" /E /I /Y >nul
if exist "%~dp0assets" xcopy "%~dp0assets" "%TEMP_INPUT%\assets\" /E /I /Y >nul
if exist "%~dp0lib" xcopy "%~dp0lib" "%TEMP_INPUT%\lib\" /E /I /Y >nul
if exist "%FX_BIN%" xcopy "%FX_BIN%\*.dll" "%TEMP_INPUT%\" /Y >nul

echo [5/7] Creando Portable...
set "PORTABLE_CP=GLauncher.jar"
pushd "%BUILD_DIR%"
for /r "lib" %%f in (*.jar) do (
    set "jar_path=%%f"
    set "relative_path=!jar_path:%BUILD_DIR%\=!"
    set "PORTABLE_CP=!PORTABLE_CP!;!relative_path!"
)
popd
echo @echo off > %BUILD_DIR%\JUGAR.bat
echo start javaw -Xmx1G -Dsun.net.http.allowRestrictedHeaders=true --module-path "lib\javafx-sdk-17.0.13\lib" --add-modules javafx.controls,javafx.media,javafx.web -cp "!PORTABLE_CP!" glauncher.GLauncher >> %BUILD_DIR%\JUGAR.bat
powershell Compress-Archive -Path %BUILD_DIR%\* -DestinationPath GLauncher_v1.zip -Force

echo [6/7] Creando Instalador EXE...
jpackage ^
  --type exe ^
  --dest %SETUP_DIR% ^
  --name "GLauncher" ^
  --input %TEMP_INPUT% ^
  --main-jar GLauncher.jar ^
  --main-class glauncher.GLauncher ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.media,javafx.web ^
  --win-dir-chooser --win-shortcut --win-menu ^
  --java-options "-Xmx1G -Dsun.net.http.allowRestrictedHeaders=true" ^
  --vendor "DaniCraftYT25" ^
  --icon assets/icons/favicon.ico

echo [7/7] Finalizado.
pause