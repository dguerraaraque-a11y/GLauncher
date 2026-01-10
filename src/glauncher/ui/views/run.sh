#!/bin/bash

# Rutas relativas (ajusta si tu JavaFX está en otro lado en Linux)
FX="lib/javafx-sdk-17.0.13/lib"
GSON="lib/gson-2.10.1.jar"

# Compilar
echo "Compilando..."
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "$GSON" --module-path "$FX" --add-modules javafx.controls,javafx.media -d out @sources.txt
rm sources.txt

# Ejecutar (Nota el separador ':' en lugar de ';')
echo "Iniciando GLauncher..."
java --module-path "$FX" --add-modules javafx.controls,javafx.media -cp "out:$GSON" glauncher.GLauncher