@echo off
setlocal EnableDelayedExpansion

REM Configurar rutas
set "BASE_DIR=%~dp0"
set "FX_LIB=%BASE_DIR%lib\javafx-sdk-17.0.13\lib"

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
javac -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web -d out @sources.txt
if %errorlevel% neq 0 (
    echo [ERROR] Error de compilacion detectado.
    pause
    exit /b
)

REM Ejecutar
echo Ejecutando GLauncher...
java --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.media,javafx.web -cp "!LIBS_CP!" glauncher.GLauncher

pause