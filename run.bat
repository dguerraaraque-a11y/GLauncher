@echo off
setlocal EnableDelayedExpansion

REM Configurar rutas
set "BASE_DIR=%~dp0"
set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"

REM --- VERIFICACION DE JAVA ---
REM 1. Intentar detectar JAVA_HOME del sistema
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
        echo [INFO] Usando JAVA_HOME: %JAVA_HOME%
        goto check_java
    )
)

REM 2. Intentar usar Java del PATH
where javac >nul 2>nul
if %errorlevel% equ 0 (
    set "JAVAC_CMD=javac"
    set "JAVA_CMD=java"
    echo [INFO] Usando Java del PATH.
    goto check_java
)

REM 3. Fallback a ruta especifica (si las anteriores fallan)
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    echo [INFO] Usando ruta fallback: %JAVA_HOME%
    goto check_java
)

echo [ERROR] No se encontro el JDK (javac). Asegurate de tener Java 17+ instalado.
pause
exit /b

:check_java
"%JAVAC_CMD%" -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] El comando javac no responde correctamente.
    pause
    exit /b
)
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
    if exist "%BASE_DIR%Ver.bat" (
        pushd "%BASE_DIR%"
        call Ver.bat
        popd
    )
)

REM Construir Classpath incluyendo todos los JARs en 'lib' y sus subcarpetas
set "LIBS_CP=out"
for /r "%BASE_DIR%lib" %%f in (*.jar) do (
    set "LIBS_CP=!LIBS_CP!;%%f"
)

REM Crear lista de fuentes Java
if exist "%BASE_DIR%sources.txt" del "%BASE_DIR%sources.txt"
dir /s /b "%BASE_DIR%src\*.java" > "%BASE_DIR%sources.txt"

REM Compilar
if not exist "out" mkdir "out"
echo [INFO] Compilando proyecto...
"%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -d out @"%BASE_DIR%sources.txt"
if %errorlevel% neq 0 (
    echo [ERROR] Error de compilacion detectado.
    del "%BASE_DIR%sources.txt"
    pause
    exit /b
)
del "%BASE_DIR%sources.txt"

REM Ejecutar
echo [INFO] Ejecutando GLauncher...
"%JAVA_CMD%" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web,javafx.swing -cp "!LIBS_CP!" glauncher.GLauncher

pause