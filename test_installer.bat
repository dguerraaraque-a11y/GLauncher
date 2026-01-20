@echo off
setlocal EnableDelayedExpansion

REM --- SCRIPT DE PRUEBA RAPIDA PARA LA GUI DEL INSTALADOR ---
set "BASE_DIR=%~dp0"
set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"

REM Configuracion de Java (Misma que en tu proyecto)
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERROR] No se encontro el JDK en la ruta esperada.
    pause
    exit /b
)
set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

REM 1. Compilar solo el Installer.java (Muy rapido)
if not exist "out_test" mkdir "out_test"
echo [TEST] Compilando GUI...
"%JAVAC_CMD%" -encoding UTF-8 --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics -d "out_test" "%BASE_DIR%src\glauncher\installer\Installer.java"

REM 2. Ejecutar
echo [TEST] Abriendo ventana del instalador...
echo NOTA: Al darle a "Instalar" fallara porque no hemos empaquetado el juego (app.zip), pero sirve para ver el diseño.
"%JAVA_CMD%" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics -cp "out_test" glauncher.installer.Installer

pause