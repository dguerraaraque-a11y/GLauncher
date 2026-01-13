@echo off
setlocal EnableDelayedExpansion

REM Configurar rutas
set "BASE_DIR=%~dp0"
set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"

REM --- VERIFICACION DE JAVA ---
REM Usar ruta especifica de Java 17 Temurin
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

REM Comprobar si javac soporta modulos (Java 9+)
"%JAVAC_CMD%" --help > "%BASE_DIR%java_check.tmp" 2>&1
findstr /C:"--module-path" "%BASE_DIR%java_check.tmp" >nul
if %errorlevel% neq 0 (
    echo [ERROR] Tu version de Java es antigua o no se encuentra el JDK.
    echo Se requiere JDK 17 o superior para compilar este proyecto.
    echo Por favor instala JDK 17 y asegurate de que JAVA_HOME apunte a el.
    echo Ruta intentada: "%JAVAC_CMD%"
    del "%BASE_DIR%java_check.tmp"
    pause
    exit /b
)
del "%BASE_DIR%java_check.tmp"
REM -----------------------------

REM Verificar integridad de librerias criticas (si pesan menos de 50KB es probable que esten corruptas)
if exist "%BASE_DIR%lib\NewPipeExtractor-0.24.8.jar" (
    for %%F in ("%BASE_DIR%lib\NewPipeExtractor-0.24.8.jar") do (
        if %%~zF LSS 50000 del "%%F"
    )
)

REM Verificar y descargar librerias faltantes si es necesario
if not exist "%BASE_DIR%lib\NewPipeExtractor-0.24.8.jar" (
    echo [INFO] Librerias no encontradas. Ejecutando Ver.bat para descargarlas...
    call "%BASE_DIR%Ver.bat"
)

REM Construir Classpath incluyendo todos los JARs en 'lib' y sus subcarpetas
set "LIBS_CP=out"
for /r "%BASE_DIR%lib" %%f in (*.jar) do (
    set "LIBS_CP=!LIBS_CP!;%%f"
    echo [LIB] Detectada: %%~nxf
)

REM Crear lista de fuentes Java
if exist "%BASE_DIR%sources.txt" del "%BASE_DIR%sources.txt"
powershell -Command "Get-ChildItem -Path '%BASE_DIR%src' -Recurse -Filter *.java | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ASCII -FilePath '%BASE_DIR%sources.txt'"

REM Compilar
if not exist "out" mkdir "out"
echo Compilando proyecto...
"%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web -d out @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Error de compilacion detectado.
    pause
    exit /b
)

REM Ejecutar
echo Ejecutando GLauncher...
"%JAVA_CMD%" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web -cp "!LIBS_CP!" glauncher.GLauncher

pause