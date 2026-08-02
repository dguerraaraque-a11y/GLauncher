const { spawn } = require('child_process');
const readline = require('readline');

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

function ask(question) {
    return new Promise(resolve => rl.question(question, resolve));
}

async function main() {
    console.log("\x1b[36m%s\x1b[0m", "========================================");
    console.log("\x1b[36m%s\x1b[0m", "   GLAUNCHER BUILD SYSTEM - BY DANICRAFTYT25");
    console.log("\x1b[36m%s\x1b[0m", "========================================\n");

    console.log("\x1b[33m%s\x1b[0m", "1. SELECCIONA ARQUITECTURA (Windows):");
    console.log("  [1] Solo x64 (64 bits)");
    console.log("  [2] Solo x86 (32 bits)");
    console.log("  [3] Ambas (x64 + x86)");
    const archChoice = await ask("\x1b[32mOpción: \x1b[0m");

    console.log("\n\x1b[33m%s\x1b[0m", "2. SELECCIONA FORMATO PARA WINDOWS:");
    console.log("  [1] NSIS (.exe - Instalador Clásico)");
    console.log("  [2] Appx/MSIX (.msix - Microsoft Store)");
    console.log("  [3] Ambos");
    const winTargetChoice = await ask("\x1b[32mOpción: \x1b[0m");

    console.log("\n\x1b[33m%s\x1b[0m", "3. SELECCIONA PLATAFORMAS DE DESTINO:");
    console.log("  [1] Solo Windows (.exe)");
    console.log("  [2] Windows + Linux (.AppImage/.deb)");
    console.log("  [3] Windows + Linux + macOS (Nota: Mac desde Windows genera .zip/.app)");
    const platformChoice = await ask("\x1b[32mOpción: \x1b[0m");

    let args = ["build"];

    // Configurar Arquitectura para Windows
    if (archChoice === "1") {
        args.push("--x64");
    } else if (archChoice === "2") {
        args.push("--ia32");
    } else if (archChoice === "3") {
        args.push("--x64");
        args.push("--ia32");
    } else {
        console.log("Opción inválida, usando x64 por defecto.");
        args.push("--x64");
    }

    // Configurar Target de Windows
    let winTargets = [];
    if (winTargetChoice === "1") winTargets = ["nsis"];
    else if (winTargetChoice === "2") winTargets = ["appx"];
    else winTargets = ["nsis", "appx"];

    // Configurar Plataformas
    if (platformChoice === "1") {
        args.push("--win");
        winTargets.forEach(t => args.push(t));
    } else if (platformChoice === "2") {
        args.push("--win");
        winTargets.forEach(t => args.push(t));
        args.push("--linux");
    } else if (platformChoice === "3") {
        args.push("--win");
        winTargets.forEach(t => args.push(t));
        args.push("--linux");
        args.push("--mac");
    } else {
        args.push("--win");
    }

    console.log(`\n\x1b[35mIniciando construcción: electron-builder ${args.join(' ')}...\x1b[0m\n`);
    
    // En Windows, ejecutar archivos .cmd (como npx) requiere obligatoriamente shell: true
    // en versiones recientes de Node.js para evitar el error de crash EINVAL.
    const isWin = process.platform === 'win32';
    const cmd = isWin ? 'npx.cmd' : 'npx';
    const buildProcess = spawn(cmd, ["electron-builder", ...args], { 
        stdio: 'inherit',
        shell: isWin
    });

    buildProcess.on('close', (code) => {
        console.log(`\n\x1b[36mProceso de DaniCraftYT25 finalizado con código: ${code}\x1b[0m`);
        rl.close();
        process.exit(code);
    });
}

main();