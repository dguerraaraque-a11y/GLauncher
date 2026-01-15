@echo off
title Publicador GLauncher - GitHub
color 0b

echo ======================================================
echo    PUBLICANDO CAMBIOS EN GITHUB
echo ======================================================

:: 1. Inicializar si no existe la carpeta .git
if not exist ".git" (
    echo [1/5] Inicializando repositorio Git...
    git init
)

:: 2. Configurar el servidor remoto (Aseguramos que se llame 'origin')
echo [2/5] Configurando servidor remoto...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/dguerraaraque-a11y/GLauncher.git

:: 3. Preparar los archivos (Añadir todo lo nuevo)
echo [3/5] Añadiendo archivos al area de preparacion...
git add .

:: 4. Crear el Commit con fecha y hora
echo [4/5] Creando el punto de guardado (Commit)...
set fecha=%date% %time%
git commit -m "Actualizacion automatica: %fecha%"

:: 5. Subir a GitHub
echo [5/5] Subiendo a la rama main...
echo.
git push -u origin main -f

echo ======================================================
echo    ¡LISTO! Revisa tu link: 
echo    https://github.com/dguerraaraque-a11y/GLauncher
echo ======================================================
pause
exit