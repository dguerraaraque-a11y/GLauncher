@echo off
setlocal EnableDelayedExpansion
title Generador de Carpeta de Instalacion GLauncher
color 0a
cls

echo ==================================================
echo      PREPARANDO CARPETA DE INSTALACION (.EXE)
echo ==================================================
echo.

set "BASE_DIR=%~dp0"
if "%BASE_DIR:~-1%"=="\" set "BASE_DIR=%BASE_DIR:~0,-1%"

set "OUT_DIR=%BASE_DIR%\out_build"
set "DIST_DIR=%BASE_DIR%\dist_folder"
set "INPUT_DIR=%BASE_DIR%\jpackage_input"

set "FX_LIB=C:\Program Files\Java\javafx-sdk-17.0.13\lib"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
set "JPACKAGE_CMD=%JAVA_HOME%\bin\jpackage.exe"

echo [1/5] Limpiando directorios...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"

mkdir "%OUT_DIR%"
mkdir "%DIST_DIR%"
mkdir "%INPUT_DIR%"

echo [2/5] Escaneando librerias...
set "LIBS_CP=%OUT_DIR%"
for /r "%BASE_DIR%\lib" %%f in (*.jar) do (
    echo %%~nxf | findstr /R /I "^javafx-" >nul
    if errorlevel 1 (
        set "LIBS_CP=!LIBS_CP!;%%f"
    )
)

echo [3/5] Compilando Proyecto...
dir /s /b "%BASE_DIR%\src\glauncher\*.java" | findstr /v "agent" | findstr /v "installer" > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.media,javafx.web,javafx.swing,javafx.base,java.desktop,jdk.management,jdk.crypto.ec -d "%OUT_DIR%" @sources.txt
if !errorlevel! neq 0 ( echo [ERROR] Fallo compilacion. & pause & exit /b )
del sources.txt

echo [4/5] Compilando e integrando Skin Agent...
set "AGENT_OUT=%OUT_DIR%\agent_build"
mkdir "%AGENT_OUT%"
"%JAVAC_CMD%" -encoding UTF-8 -source 8 -target 8 -cp "!LIBS_CP!" -d "%AGENT_OUT%" "%BASE_DIR%\src\glauncher\agent\SkinAgent.java"
pushd "%AGENT_OUT%"
for %%j in ("%BASE_DIR%\lib\*.jar") do (
    echo %%~nxj | findstr /I "javassist" >nul
    if !errorlevel! == 0 ( "%JAR_CMD%" xf "%%j" )
)
if exist "META-INF" rmdir /s /q "META-INF"
popd
(echo Premain-Class: glauncher.agent.SkinAgent) > "%AGENT_OUT%\MANIFEST.MF"
"%JAR_CMD%" cfm "%INPUT_DIR%\GLauncherSkinAgent.jar" "%AGENT_OUT%\MANIFEST.MF" -C "%AGENT_OUT%" .

REM Crear el JAR principal
"%JAR_CMD%" cfe "%INPUT_DIR%\GLauncher.jar" glauncher.GLauncher -C "%OUT_DIR%" .

echo [5/5] Generando Carpeta de Instalacion con jpackage...
"%JPACKAGE_CMD%" ^
  --type app-image ^
  --dest "%DIST_DIR%" ^
  --name "GLauncher" ^
  --input "%INPUT_DIR%" ^
  --main-jar "GLauncher.jar" ^
  --main-class glauncher.GLauncher ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.fxml,javafx.media,javafx.web,javafx.swing,javafx.base,java.desktop,jdk.management,jdk.crypto.ec ^
  --icon "%BASE_DIR%\assets\icons\favicon.ico"

echo [INFO] Copiando assets y librerias a la carpeta de instalacion...
set "FINAL_APP_DIR=%DIST_DIR%\GLauncher"
if exist "%BASE_DIR%\assets" xcopy /S /E /Y /I "%BASE_DIR%\assets" "%FINAL_APP_DIR%\app\assets" > nul
if exist "%BASE_DIR%\lib" xcopy /S /E /Y /I "%BASE_DIR%\lib" "%FINAL_APP_DIR%\app\lib" > nul

REM El agente debe ir en una carpeta accesible para el launcher instalado
mkdir "%FINAL_APP_DIR%\app\agents"
copy /Y "%INPUT_DIR%\GLauncherSkinAgent.jar" "%FINAL_APP_DIR%\app\agents\" > nul

echo.
echo ==================================================
echo   CARPETA DE INSTALACION CREADA CON EXITO
echo ==================================================
echo Ubicacion: %FINAL_APP_DIR%
echo.
echo Dentro de 'app' encontraras las carpetas:
echo   - assets/
echo   - lib/
echo   - agents/
echo.
pause