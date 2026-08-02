const { exec } = require('child_process');

/**
 * Optimizator.js - Engine de alto rendimiento para GLauncher
 * Diseñado para reducir la latencia de red y liberar recursos del sistema.
 */

function optimizeSystem() {
    return new Promise((resolve, reject) => {
        // Verificación de plataforma (Minecraft corre mejor optimizado en Windows)
        if (process.platform !== 'win32') {
            return reject(new Error("La optimización avanzada de registro solo está disponible en Windows."));
        }

        // Script de PowerShell consolidado:
        const tweaks = [
            "Write-Host '--- INICIANDO GLAUNCHER ULTRA OPTIMIZER ---' -ForegroundColor Yellow",
            "Write-Host '[1/5] Optimizando Registro y Latencia...' -ForegroundColor Cyan",
            "$p = 'HKLM:\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Multimedia\\SystemProfile'",
            "Set-ItemProperty -Path $p -Name 'NetworkThrottlingIndex' -Value 0xffffffff -Force",
            "Set-ItemProperty -Path $p -Name 'SystemResponsiveness' -Value 0 -Force",
            "$g = \"$p\\Tasks\\Games\"",
            "Set-ItemProperty -Path $g -Name 'GPU Priority' -Value 8 -Force",
            "Set-ItemProperty -Path $g -Name 'Priority' -Value 6 -Force",
            "Set-ItemProperty -Path $g -Name 'Scheduling Category' -Value 'High' -Force",
            
            "Write-Host '[2/5] Deshabilitando Game DVR y Game Bar...' -ForegroundColor Cyan",
            "reg add 'HKCU\\System\\GameConfigStore' /v 'GameDVR_Enabled' /t REG_DWORD /d 0 /f",
            "reg add 'HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\GameDVR' /v 'AppCaptureEnabled' /t REG_DWORD /d 0 /f",

            "Write-Host '[3/5] Ajustes de Red Avanzados (Netsh)...' -ForegroundColor Cyan",
            "netsh int tcp set global rss=enabled",
            "netsh int tcp set global autotuninglevel=normal",
            "netsh int tcp set global ecncapability=disabled",
            "netsh int tcp set global timestamps=disabled",
            "netsh int tcp set global initialrto=2000",
            "$interfaces = Get-ChildItem 'HKLM:\\SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters\\Interfaces'",
            "foreach($i in $interfaces){ Set-ItemProperty -Path $i.PSPath -Name 'TcpAckFrequency' -Value 1 -ErrorAction SilentlyContinue; Set-ItemProperty -Path $i.PSPath -Name 'TCPNoDelay' -Value 1 -ErrorAction SilentlyContinue }",

            "Write-Host '[4/5] Eliminando Servicios de Telemetría y Bloatware...' -ForegroundColor Cyan",
            "$svcs = @('DiagTrack', 'SysMain', 'MapsBroker', 'XblAuthManager', 'XblGameSave', 'PrintNotify', 'RemoteRegistry', 'TermService')",
            "foreach($s in $svcs){ if(Get-Service $s -ErrorAction SilentlyContinue){ Stop-Service $s -Force -ErrorAction SilentlyContinue; Set-Service $s -StartupType Disabled } }",

            "Write-Host '[5/5] Mantenimiento de Sistema y Energía...' -ForegroundColor Cyan",
            "powercfg /duplicatescheme e9a42b02-d5df-448d-aa00-03f14749eb61 | Out-Null", // Intenta crear Ultimate Performance
            "powercfg /setactive 8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c", // High Performance
            "Write-Host 'Limpiando caché de temporales...' -ForegroundColor Cyan",
            "Remove-Item -Path $env:TEMP\\* -Recurse -Force -ErrorAction SilentlyContinue",
            "ipconfig /flushdns",
            "Write-Host '-------------------------------------------' -ForegroundColor Yellow",
            "Write-Host '¡OPTIMIZACIÓN COMPLETADA CON ÉXITO!' -ForegroundColor Green",
            "Write-Host 'Reinicia tu PC para aplicar todos los cambios.' -ForegroundColor White",
            "Read-Host 'Presiona Enter para cerrar'"
        ].join('; ');

        // Ejecutar solicitando privilegios de administrador (UAC)
        // Añadimos -Wait para que el launcher sepa cuando termina
        const command = `powershell -Command "Start-Process powershell -ArgumentList '-NoProfile -Command ${tweaks}' -Verb RunAs -Wait"`;

        exec(command, (error) => {
            if (error) return reject(error);
            resolve(true);
        });
    });
}

module.exports = { optimizeSystem };