#!/bin/bash

BUILD_DIR="GLauncher_v1_Dist_Linux"
TEMP_INPUT="temp_input"
FX_LIB="lib/javafx-sdk-17.0.13/lib"
GSON="lib/gson-2.10.1.jar"

echo "[1/5] Limpiando..."
rm -rf $BUILD_DIR $TEMP_INPUT out GLauncher_Linux.zip
mkdir -p out $BUILD_DIR $TEMP_INPUT

echo "[2/5] Compilando..."
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "$GSON" --module-path "$FX_LIB" --add-modules javafx.controls,javafx.media -d out @sources.txt
rm sources.txt

echo "[3/5] Creando JAR..."
echo "Main-Class: glauncher.GLauncher" > manifest.txt
echo "Class-Path: gson-2.10.1.jar" >> manifest.txt
jar cmf manifest.txt $TEMP_INPUT/GLauncher.jar -C out .
rm manifest.txt
cp $TEMP_INPUT/GLauncher.jar $BUILD_DIR/

echo "[4/5] Copiando recursos..."
cp -r lib $BUILD_DIR/
cp -r assets $BUILD_DIR/

# Crear script de lanzamiento universal
cat <<EOT > $BUILD_DIR/JUGAR.sh
#!/bin/bash
DIR="\$( cd "\$( dirname "\${BASH_SOURCE[0]}" )" && pwd )"
java --module-path "\$DIR/lib/javafx-sdk-17.0.13/lib" --add-modules javafx.controls,javafx.media -cp "\$DIR/GLauncher.jar:\$DIR/lib/gson-2.10.1.jar" glauncher.GLauncher
EOT
chmod +x $BUILD_DIR/JUGAR.sh

echo "[5/5] Empaquetando ZIP..."
cd $BUILD_DIR
zip -r ../GLauncher_Linux_Mac.zip *
cd ..

echo "=========================================="
echo " ¡Listo!"
echo " ZIP generado: GLauncher_Linux_Mac.zip"
echo " Para ejecutar en Linux/Mac:"
echo " 1. Descomprimir"
echo " 2. Dar permisos: chmod +x JUGAR.sh"
echo " 3. Ejecutar: ./JUGAR.sh"
echo "=========================================="