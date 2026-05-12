echo off
setlocal EnableDelayedExpansion

REM --- CONFIGURACION DE RUTAS ---
set "BASE_DIR=%~dp0"
:: Limpiar la ruta para evitar problemas de doble slash
if "%BASE_DIR:~-1%"=="\" set "BASE_DIR=%BASE_DIR:~0,-1%"

REM --- BUSQUEDA DINAMICA DE JAVAFX ---
if exist "%BASE_DIR%\javafx-sdk\lib" (
    set "FX_LIB=%BASE_DIR%\javafx-sdk\lib"
) else (
    set "FX_LIB=C:\Program Files\Java\javafx-sdk-17.0.13\lib"
)
set "OUT_DIR=%BASE_DIR%\out"
set "SOURCES_FILE=%BASE_DIR%\sources.txt"
set "LOG_DIR=%BASE_DIR%\logs"
set "LOG_FILE=%LOG_DIR%\latest.log"

REM --- VERIFICACION DE JAVA (PORTABLE) ---
echo [INFO] Buscando una instalacion de JDK 17+...

REM Opcion 1: Usar la variable de entorno JAVA_HOME (preferido)
if defined JAVA_HOME (
    echo [INFO] Variable JAVA_HOME encontrada en: "%JAVA_HOME%"
    set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
)

REM Opcion 2: Si no hay JAVA_HOME, buscar javac y java en el PATH del sistema
if not defined JAVAC_CMD (
    echo [INFO] JAVA_HOME no definida. Buscando en el PATH del sistema...
    for %%i in (javac.exe) do set "JAVAC_CMD=%%~$PATH:i"
    for %%i in (java.exe) do set "JAVA_CMD=%%~$PATH:i"
    for %%i in (jar.exe) do set "JAR_CMD=%%~$PATH:i"
)

REM Comprobar si se encontro un JDK valido
if not defined JAVAC_CMD (
    echo [ERROR] No se pudo encontrar el JDK.
    echo Por favor, instala JDK 17 o superior y asegurate de que:
    echo   1. La variable de entorno JAVA_HOME apunte a la carpeta del JDK.
    echo   2. O que 'javac.exe' este en el PATH del sistema.
    pause
    exit /b
)
echo [INFO] Usando JDK encontrado en: "%JAVAC_CMD%"

REM Comprobar si la version de Java es compatible (Java 9+ para modulos)
"%JAVAC_CMD%" --help > "%BASE_DIR%java_check.tmp" 2>&1
findstr /C:"--module-path" "%BASE_DIR%java_check.tmp" >nul
if %errorlevel% neq 0 (
    echo [ERROR] La version de Java encontrada no es compatible.
    echo Se requiere JDK 17 o superior para compilar este proyecto.
    echo Por favor, asegurate de que tu JAVA_HOME o PATH apunten a un JDK 17+.
    echo Ruta utilizada: "%JAVAC_CMD%"
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

REM Verificar si la libreria de Discord RPC esta corrupta (tamaño menor a 10KB)
if exist "%BASE_DIR%lib\java-discord-rpc-2.0.1.jar" (
    for %%F in ("%BASE_DIR%lib\java-discord-rpc-2.0.1.jar") do (
        if %%~zF LSS 10000 (
            echo [WARN] La libreria java-discord-rpc-2.0.1.jar parece corrupta. Eliminando para forzar descarga...
            del "%%F"
        )
    )
)

REM Verificar y descargar librerias faltantes si es necesario
REM if not exist "%BASE_DIR%lib\NewPipeExtractor-0.24.8.jar" (
REM     echo [INFO] Librerias no encontradas. Ejecutando Ver.bat para descargarlas...
REM     if exist "%BASE_DIR%Ver.bat" (
REM         pushd "%BASE_DIR%"
REM         call Ver.bat
REM         popd
REM     )
REM )
REM if not exist "%BASE_DIR%lib\java-discord-rpc-2.0.1.jar" (
REM     echo [INFO] Libreria Discord RPC no encontrada. Ejecutando Ver.bat para descargarla...
REM     if exist "%BASE_DIR%Ver.bat" (
REM         pushd "%BASE_DIR%"
REM         call Ver.bat
REM         popd
REM     )
REM )

REM Construir Classpath incluyendo todos los JARs en 'lib' y sus subcarpetas
set "LIBS_CP=%OUT_DIR%"
echo [INFO] Escaneando librerias en %BASE_DIR%\lib...
set "JAR_COUNT=0"
for /r "%BASE_DIR%\lib" %%f in (*.jar) do (
    REM Solo agregar al classpath lo que no sea una libreria base de JavaFX (que van al module-path)
    echo %%~nxf | findstr /R /I "^javafx-" >nul
    if errorlevel 1 (
        set "LIBS_CP=!LIBS_CP!;%%f"
        set /a "JAR_COUNT+=1"
    )
)
echo [INFO] Se han detectado !JAR_COUNT! librerias externas.

set "MARKER_FILE=%OUT_DIR%\glauncher\GLauncher.class"

REM --- MODO LIVE-RELOAD ---
echo.
echo ==================================================
echo      MODO DE DESARROLLO EN VIVO ACTIVADO
echo ==================================================
echo.
echo [WATCHER] El script se ejecutara en un bucle.
echo [WATCHER] Al guardar un archivo .java, la app se reiniciara.
echo [WATCHER] Cierra esta ventana para detener el proceso.
echo.

REM Compilacion y ejecucion inicial
call :compile_and_run

:main_loop
    REM Esperar 2 segundos antes de la siguiente comprobacion
    timeout /t 2 /nobreak >nul

    REM Comprobar si algun archivo .java es mas nuevo que la ultima compilacion.
    REM El comando de PowerShell sale con codigo 0 si hay cambios, 1 si no los hay.
    powershell -Command "$source = (Get-ChildItem -Path '%BASE_DIR%\src' -Recurse -Filter *.java | Sort-Object LastWriteTime -Descending | Select-Object -First 1); if ($null -eq $source) { exit 1 }; $target = (Get-Item '%MARKER_FILE%' -ErrorAction SilentlyContinue); if ($null -eq $target) { exit 0 }; exit ($source.LastWriteTime -le $target.LastWriteTime)"
    
    if %errorlevel% == 0 (
        echo [WATCHER] Cambios detectados. Recompilando y reiniciando...
        call :compile_and_run
    )
goto main_loop


:compile_and_run
    REM Matar la instancia anterior de la aplicacion si se esta ejecutando
    taskkill /F /FI "WINDOWTITLE eq GLauncherDev" >nul 2>&1

    echo [INFO] Limpiando compilacion anterior...
    if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
    if exist "%SOURCES_FILE%" del /F /Q "%SOURCES_FILE%"
    
    REM [FIX] Excluir carpetas de agentes, instalador y archivos obsoletos como Splash
    REM Esto evita que javac colapse por intentar compilar archivos duplicados o con errores.
    dir /s /b "%BASE_DIR%\src\glauncher\*.java" | findstr /v /i "agent" | findstr /v /i "installer" | findstr /v /i "Splash" > "%SOURCES_FILE%" 2>nul
    
    if not exist "%SOURCES_FILE%" (
        echo [ERROR] No se encontraron archivos fuente en %BASE_DIR%\src
        pause & exit /b
    )
    
    echo [INFO] Compilando proyecto...
    if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
    "%JAVAC_CMD%" -encoding UTF-8 -cp "!LIBS_CP!" --module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics,javafx.media,javafx.web,javafx.swing,javafx.fxml,javafx.base,jdk.management,jdk.crypto.ec -d "%OUT_DIR%" @"%SOURCES_FILE%" || (
        echo [ERROR] Error de compilacion detectado. 
        echo [ERROR] Error de compilacion detectado. Revisa los errores arriba.
        pause & goto :eof 
    )

    REM --- Compilar y empaquetar el Agente de Skins ---
    echo [AGENT] Compilando Skin Agent...
    set "AGENT_SRC=%BASE_DIR%\src\glauncher\agent\SkinAgent.java"
    set "AGENT_OUT=%OUT_DIR%\agent_build"
    set "AGENT_JAR_NAME=GLauncherSkinAgent.jar"
    set "AGENT_TARGET_DIR=%APPDATA%\.glauncher\agents"

    if exist "%AGENT_SRC%" (
        if not exist "%AGENT_OUT%" mkdir "%AGENT_OUT%"
        "%JAVAC_CMD%" -encoding UTF-8 -source 8 -target 8 -cp "!LIBS_CP!" -d "%AGENT_OUT%" "%AGENT_SRC%"
        if !errorlevel! neq 0 ( echo [ERROR] Error de compilacion del Agente. & goto :eof )
        
        echo [AGENT] Integrando dependencias (Javassist)...
        pushd "%AGENT_OUT%"
        for %%j in ("%BASE_DIR%\lib\*.jar") do (
            echo %%~nxj | findstr /I "javassist" >nul
            if !errorlevel! == 0 ( "%JAR_CMD%" xf "%%j" )
        )
        if exist "META-INF" rmdir /s /q "META-INF"
        popd

        echo [AGENT] Creando manifest...
        (echo Premain-Class: glauncher.agent.SkinAgent) > "%AGENT_OUT%\MANIFEST.MF"
        
        echo [AGENT] Empaquetando %AGENT_JAR_NAME%...
        "%JAR_CMD%" cfm "%BASE_DIR%\%AGENT_JAR_NAME%" "%AGENT_OUT%\MANIFEST.MF" -C "%AGENT_OUT%" .
        if not exist "%AGENT_TARGET_DIR%" mkdir "%AGENT_TARGET_DIR%"
        copy /Y "%BASE_DIR%\%AGENT_JAR_NAME%" "%AGENT_TARGET_DIR%\" > nul
    )

    echo [INFO] Sincronizando recursos (assets)...
    if exist "%BASE_DIR%\assets" ( xcopy /D /S /E /Y /I "%BASE_DIR%\assets" "%OUT_DIR%\assets" >nul )

    echo [WATCHER] Compilacion exitosa. Iniciando aplicacion...
    if not exist "%OUT_DIR%\assets" echo [WARN] La carpeta de assets no se encontro en el output.

    if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
    echo [INFO] ---------------------------------------------------
    echo [INFO] Iniciando GLauncher...
    echo [INFO] Si la app se cierra, el error aparecera aqui abajo.
    
    REM Añadimos java.desktop explícitamente por el uso de Desktop en UpdateChecker
    set "LAUNCH_OPTS=--module-path "%FX_LIB%" --add-modules javafx.controls,javafx.graphics,javafx.media,javafx.web,javafx.swing,javafx.fxml,javafx.base,jdk.management,jdk.crypto.ec,java.desktop -cp "!LIBS_CP!""
    
    REM Redirigir errores a la consola y a un archivo para que no se pierdan
    "%JAVA_CMD%" !LAUNCH_OPTS! glauncher.GLauncher > "%LOG_FILE%" 2>&1
    
    if !errorlevel! neq 0 (
        echo.
        echo ==================================================
        echo           DETALLES DEL CRASH DETECTADO
        echo ==================================================
        if exist "%LOG_FILE%" type "%LOG_FILE%"
        echo.
        echo [FATAL] La aplicacion se cerro con codigo !errorlevel!
        pause 
    )
goto :eof

pause