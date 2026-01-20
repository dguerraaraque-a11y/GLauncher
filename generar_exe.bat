@echo off
setlocal EnableDelayedExpansion
title Generador de Setup GLauncher (Master Script)
color 0a
cls

echo ==================================================
echo      GENERADOR DE SETUP GLAUNCHER (.EXE)
echo ==================================================
echo.

set "BASE_DIR=%~dp0"
set "BUILD_DIR=%BASE_DIR%build_temp"
set "DIST_DIR=%BASE_DIR%dist_game"
set "INSTALLER_DIR=%BASE_DIR%installer_build"
set "OUTPUT_DIR=%BASE_DIR%out_setup"

set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
set "JPACKAGE_CMD=%JAVA_HOME%\bin\jpackage.exe"

echo [1/6] Limpiando directorios...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
if exist "%INSTALLER_DIR%" rmdir /s /q "%INSTALLER_DIR%"
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"

mkdir "%BUILD_DIR%"
mkdir "%DIST_DIR%"
mkdir "%INSTALLER_DIR%"
mkdir "%INSTALLER_DIR%\classes"
mkdir "%INSTALLER_DIR%\jar"

echo [2/6] Compilando GLauncher (Juego)...
dir /s /b "%BASE_DIR%src\*.java" > sources.txt
REM Excluir el instalador de la compilacion del juego
findstr /v "Installer.java" sources.txt > sources_game.txt

set "LIBS_CP="
for /r "%BASE_DIR%lib" %%f in (*.jar) do (
    set "LIBS_CP=!LIBS_CP!;%%f"
)

"%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -d "%DIST_DIR%" @sources_game.txt
if %errorlevel% neq 0 (
    echo [ERROR] Fallo al compilar el juego.
    pause
    exit /b
)
del sources.txt
del sources_game.txt

echo [3/6] Preparando recursos del juego...
xcopy /S /E /Y /I "%BASE_DIR%lib" "%DIST_DIR%\lib" >nul
xcopy /S /E /Y /I "%BASE_DIR%assets" "%DIST_DIR%\assets" >nul

REM Crear script de arranque para el juego instalado
(
echo @echo off
echo set "BASE_DIR=%%~dp0"
echo set "FX_LIB=%%BASE_DIR%%lib\javafx-sdk-17.0.13\lib"
echo set "FX_BIN=%%BASE_DIR%%lib\javafx-sdk-17.0.13\bin"
echo set "PATH=%%FX_BIN%%;%%PATH%%"
echo start "" "javaw" --module-path "%%FX_LIB%%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -cp "%%BASE_DIR%%;%%BASE_DIR%%lib\*" glauncher.GLauncher
) > "%DIST_DIR%\GLauncher.bat"

echo [4/6] Empaquetando juego en app.zip...
powershell -Command "Compress-Archive -Path '%DIST_DIR%\*' -DestinationPath '%INSTALLER_DIR%\classes\app.zip' -Force"

echo [5/6] Compilando Instalador (Installer.java)...
"%JAVAC_CMD%" -encoding UTF-8 --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics -d "%INSTALLER_DIR%\classes" "%BASE_DIR%src\glauncher\installer\Installer.java"

REM Copiar assets al instalador para que tenga iconos
mkdir "%INSTALLER_DIR%\jar\assets"
xcopy /S /E /Y /I "%BASE_DIR%assets" "%INSTALLER_DIR%\jar\assets" >nul

REM Crear JAR del instalador
"%JAR_CMD%" cfe "%INSTALLER_DIR%\jar\GLauncher_Installer.jar" glauncher.installer.Installer -C "%INSTALLER_DIR%\classes" .

echo [6/6] Generando EXE del Instalador con jpackage...
"%JPACKAGE_CMD%" ^
  --type app-image ^
  --dest "%OUTPUT_DIR%" ^
  --name "GLauncher_Setup" ^
  --input "%INSTALLER_DIR%\jar" ^
  --main-jar "GLauncher_Installer.jar" ^
  --main-class glauncher.installer.Installer ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.web,javafx.media,jdk.crypto.ec ^
  --icon "%BASE_DIR%assets\icons\favicon.ico" ^
  --win-console

if %errorlevel% neq 0 (
    echo [ERROR] jpackage fallo.
    pause
    exit /b
)

echo.
echo ==================================================
echo   INSTALADOR CREADO CON EXITO
echo ==================================================
echo Ubicacion: %OUTPUT_DIR%\GLauncher_Setup\GLauncher_Setup.exe
echo.
echo Este ejecutable contiene el juego y lo instalara en %%APPDATA%%\GLauncher
echo.
pause