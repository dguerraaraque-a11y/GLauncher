@echo off
setlocal EnableDelayedExpansion

REM --- CONFIGURACION ---
set "BASE_DIR=%~dp0"
set "DIST_DIR=%BASE_DIR%dist"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"

echo [1/5] Limpiando directorio de distribucion...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"
mkdir "%DIST_DIR%\bin"

echo [2/5] Compilando codigo fuente...
REM Crear lista de fuentes
dir /s /b "%BASE_DIR%src\*.java" > "%BASE_DIR%sources.txt"

REM Construir Classpath
set "LIBS_CP="
for /r "%BASE_DIR%lib" %%f in (*.jar) do (
    set "LIBS_CP=!LIBS_CP!;%%f"
)
set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"

REM Compilar a la carpeta dist/bin
"%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -d "%DIST_DIR%\bin" @"%BASE_DIR%sources.txt"
del "%BASE_DIR%sources.txt"

if %errorlevel% neq 0 (
    echo [ERROR] La compilacion fallo. Revisa los errores arriba.
    pause
    exit /b
)

echo [3/5] Copiando librerias y recursos...
REM Copiar librerias (JARs y JavaFX)
xcopy /S /E /Y /I "%BASE_DIR%lib" "%DIST_DIR%\lib" >nul

REM Copiar Assets (Imagenes, Fuentes, Iconos)
xcopy /S /E /Y /I "%BASE_DIR%assets" "%DIST_DIR%\assets" >nul

echo [4/5] Creando lanzador portatil (GLauncher.bat)...
REM Crear un .bat que funcione en cualquier PC (usando rutas relativas)
(
echo @echo off
echo set "BASE_DIR=%%~dp0"
echo set "FX_LIB=%%BASE_DIR%%lib\javafx-sdk-17.0.13\lib"
echo set "FX_BIN=%%BASE_DIR%%lib\javafx-sdk-17.0.13\bin"
echo set "PATH=%%FX_BIN%%;%%PATH%%"
echo.
echo REM Construir Classpath dinamicamente
echo set "LIBS_CP=%%BASE_DIR%%bin"
echo setlocal EnableDelayedExpansion
echo for /r "%%BASE_DIR%%lib" %%%%f in (*.jar^) do ^(
echo     set "LIBS_CP=!LIBS_CP!;%%%%f"
echo ^)
echo.
echo REM Detectar Java en el sistema
echo set "JAVA_CMD=java"
echo if defined JAVA_HOME set "JAVA_CMD=%%JAVA_HOME%%\bin\java"
echo.
echo echo Iniciando GLauncher...
echo "%%JAVA_CMD%%" --module-path "%%FX_LIB%%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -cp "!LIBS_CP!" glauncher.GLauncher
) > "%DIST_DIR%\GLauncher.bat"

echo [5/6] 
REM 1. Crear carpeta temporal para el instalador
set "INSTALLER_BUILD=%BASE_DIR%installer_build"
if exist "%INSTALLER_BUILD%" rmdir /s /q "%INSTALLER_BUILD%"
mkdir "%INSTALLER_BUILD%"
mkdir "%INSTALLER_BUILD%\classes"
mkdir "%INSTALLER_BUILD%\jar"

REM 2. Comprimir la carpeta 'dist' (el juego) en 'app.zip'
echo   - Comprimiendo archivos del juego...
powershell -Command "Compress-Archive -Path '%DIST_DIR%\*' -DestinationPath '%INSTALLER_BUILD%\classes\app.zip' -Force"

REM 3. Compilar el codigo del Instalador
echo   - Compilando Installer.java...
"%JAVAC_CMD%" -encoding UTF-8 --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics -d "%INSTALLER_BUILD%\classes" "%BASE_DIR%src\glauncher\installer\Installer.java"

REM 4. Crear el JAR del Instalador (Incluyendo app.zip dentro)
echo   - Creando GLauncher_Installer.jar...
jar cfe "%INSTALLER_BUILD%\jar\GLauncher_Installer.jar" glauncher.installer.Installer -C "%INSTALLER_BUILD%\classes" .

REM [FIX] Copiar assets a la carpeta del instalador para que el EXE tenga iconos
mkdir "%INSTALLER_BUILD%\jar\assets"
xcopy /S /E /Y /I "%BASE_DIR%assets" "%INSTALLER_BUILD%\jar\assets" >nul

REM 5. Convertir a EXE usando jpackage (Requiere JDK 14+)
echo   - Generando ejecutable nativo (GLauncher_Setup.exe)...
if exist "%BASE_DIR%out_setup" rmdir /s /q "%BASE_DIR%out_setup"

"%JAVA_HOME%\bin\jpackage" ^
  --type app-image ^
  --dest "%BASE_DIR%out_setup" ^
  --name "GLauncher_Setup" ^
  --input "%INSTALLER_BUILD%\jar" ^
  --main-jar "GLauncher_Installer.jar" ^
  --main-class glauncher.installer.Installer ^
  --module-path "%FX_LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.web,javafx.media,jdk.crypto.ec ^
  --win-console

if %errorlevel% neq 0 (
    echo [ADVERTENCIA] jpackage fallo o no esta disponible. Se usara el JAR como fallback.
    copy "%INSTALLER_BUILD%\jar\GLauncher_Installer.jar" "%BASE_DIR%GLauncher_Installer.jar"
)

REM Limpieza
rmdir /s /q "%INSTALLER_BUILD%"

echo [6/6] Construccion completada con exito!
echo -------------------------------------------------------
echo 1. Juego portable: %DIST_DIR%
echo 2. INSTALADOR EXE: %BASE_DIR%out_setup\GLauncher_Setup\GLauncher_Setup.exe
echo.
echo NOTA: La carptur esa carpeta y compartirla, o crear un acceso directo al 