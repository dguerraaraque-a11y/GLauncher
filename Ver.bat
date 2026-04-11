@echo off
echo [INFO] Ver.bat: Descargando dependencias faltantes...

REM --- URLs de las librerias ---
set "GSON_URL=https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
set "JAVASSIST_URL=https://repo1.maven.org/maven2/org/javassist/javassist/3.29.2-GA/javassist-3.29.2-GA.jar"
set "JSOUP_URL=https://repo1.maven.org/maven2/org/jsoup/jsoup/1.16.1/jsoup-1.16.1.jar"
set "IKONLI_JAFX_URL=https://repo1.maven.org/maven2/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar"
set "IKONLI_FA_URL=https://repo1.maven.org/maven2/org/kordamp/ikonli/ikonli-fontawesome-pack/12.3.1/ikonli-fontawesome-pack-12.3.1.jar"
set "NEWPIPE_EXTRACTOR_URL=https://github.com/TeamNewPipe/NewPipeExtractor/releases/download/v0.24.8/NewPipeExtractor-0.24.8.jar"
set "DISCORD_RPC_URL=https://github.com/discord-rpc/discord-rpc/releases/download/v2.0.1/java-discord-rpc-2.0.1.jar"


REM --- Directorio de destino ---
set "TARGET_DIR=%~dp0lib"

REM --- Descarga de archivos ---
echo [DOWN] Descargando Gson...
curl -L -o "%TARGET_DIR%\gson-2.10.1.jar" "%GSON_URL%"

echo [DOWN] Descargando Javassist...
curl -L -o "%TARGET_DIR%\javassist-3.29.2-GA.jar" "%JAVASSIST_URL%"

echo [DOWN] Descargando Jsoup...
curl -L -o "%TARGET_DIR%\jsoup-1.16.1.jar" "%JSOUP_URL%"

echo [DOWN] Descargando Ikonli JavaFX...
curl -L -o "%TARGET_DIR%\ikonli-javafx-12.3.1.jar" "%IKONLI_JAFX_URL%"

echo [DOWN] Descargando Ikonli FontAwesome Pack...
curl -L -o "%TARGET_DIR%\ikonli-fontawesome-pack-12.3.1.jar" "%IKONLI_FA_URL%"

echo [DOWN] Descargando NewPipeExtractor...
curl -L -o "%TARGET_DIR%\NewPipeExtractor-0.24.8.jar" "%NEWPIPE_EXTRACTOR_URL%"

echo [DOWN] Descargando Discord RPC...
curl -L -o "%TARGET_DIR%\java-discord-rpc-2.0.1.jar" "%DISCORD_RPC_URL%"


echo [INFO] Ver.bat: Descarga de dependencias completada.
