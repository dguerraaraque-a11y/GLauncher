const { app, BrowserWindow, ipcMain, dialog, Tray, Menu } = require('electron');
const path = require('path');
const fs = require('fs');
const axios = require('axios');
const { shell } = require('electron'); // Add shell for opening paths
const { MinecraftFolder, launch } = require('@xmcl/core');
const { install, installFabric, installForge, installNeoForge } = require('@xmcl/installer');
const { offline } = require('@xmcl/user');
const yts = require('yt-search'); //
const util = require('minecraft-server-util');
const ytdl = require('@distube/ytdl-core');
const fsExtra = require('fs-extra');
const ws = require('windows-shortcuts');
const findJavaHome = require('find-java-home'); //
const Jimp = require('jimp'); // Importamos Jimp para procesamiento de imágenes
const { optimizeSystem } = require('./optimizator.js');

// Evitar el warning de MaxListeners (útil para descargas masivas de assets)
require('events').EventEmitter.defaultMaxListeners = 100;

// Global variables
let mainWindow;
let splash;
let tray;
let externalMiniPlayer = null; //

// Credenciales de Supabase
const SUPABASE_PROJECT_URL = "https://ouqpeojilykkrmatijxp.supabase.co";
const SUPABASE_PUB_KEY = "sb_publishable_Xq5tXNmuTSSGlry49JEDIQ_pgBxBJoc";

// Definimos la ruta raíz según tu petición
const rootPath = path.join(app.getPath('appData'), '.glauncher');
const configPath = path.join(rootPath, 'launcher.json');
const sessionPath = path.join(rootPath, 'sesion.json');

// Asegurar que la carpeta raíz exista
if (!fs.existsSync(rootPath)) fs.mkdirSync(rootPath, { recursive: true });

// Configuración por defecto
const defaultSettings = {
    game: { resolutionWidth: 1280, resolutionHeight: 720, fullscreen: false, closeOnLaunch: true, autoJoin: "", openLogs: false },
    general: { language: "Español (Latinoamérica)", root: rootPath, telemetry: true },
    java: { ram: 4, ramMin: 1, jvmArgs: "-XX:+UseG1GC", javaPath: "default" },
    music: { playlists: [] },
    achievements: {
        first_download: false,
        first_launch: false,
        melomano: false,
        socializer: false,
        stylist: false,
        configurator: false,
        mod_hunter: false,
        veteran: false,
        explorer: false,
        cleaner: false,
        rey_del_pop: false,
        server_adder: false,
        ram_master: false,
        old_school: false,
        bg_collector: false,
        fullscreen_king: false,
        modloader_expert: false,
        safety_first: false
    },
    appearance: { 
        theme: "Oscuro Moderno (Default)", 
        dynamicBg: true, 
        accentColor: "#0078d7",
        animations: true,
        skinPath: null,
        yggdrasilServer: "", // URL de la API del servidor de skins
        customBackgrounds: [], // Lista de rutas de imágenes o videos
        bgBlur: 0,
        bgVideoVolume: 0.5,
        minimizeToTray: false, // New setting for minimizing to tray
        zoomFactor: 1.0,
        avatarHistory: [
            { id: 1, name: "Invitado", url: "assets/images/default-avatar.png" },
            { id: 2, name: "Steve", url: "https://mc-heads.net/avatar/Steve" }
        ]
    },
    servers: [],
    stats: {
        launch_count: 0,
        views_visited: []
    }
};

function deepMerge(target, source) {
    for (const key in source) {
        if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
            if (!target[key]) target[key] = {};
            deepMerge(target[key], source[key]);
        } else {
            target[key] = source[key];
        }
    }
    return target;
}

function readSettings() {
    if (!fs.existsSync(configPath)) {
        fs.writeFileSync(configPath, JSON.stringify(defaultSettings, null, 4));
        return defaultSettings;
    }
    try {
        const content = fs.readFileSync(configPath, 'utf-8').trim();
        if (!content) throw new Error("Archivo de configuración vacío");
        const data = JSON.parse(content);
        return deepMerge({ ...defaultSettings }, data);
    } catch (e) {
        console.error("[main.js] Error al leer launcher.json, restaurando valores por defecto:", e.message);
        // Si el JSON es inválido, sobreescribimos con el default para evitar crashes futuros
        fs.writeFileSync(configPath, JSON.stringify(defaultSettings, null, 4));
        return defaultSettings;
    }
}

/**
 * Lee el archivo de sesión para obtener las cuentas y la cuenta activa.
 */
function readSession() {
    if (!fs.existsSync(sessionPath)) return null;
    try {
        const content = fs.readFileSync(sessionPath, 'utf-8').trim();
        if (!content) return null;
        return JSON.parse(content);
    } catch (e) {
        console.error("[main.js] Error al leer sesion.json:", e.message);
        return null;
    }
}

function createWindows() {
    const startTime = Date.now();
    // Creamos la ventana de Splash
    splash = new BrowserWindow({
        width: 600,
        height: 400,
        transparent: true,
        frame: false,
        alwaysOnTop: true,
        resizable: false,
        webPreferences: { 
            nodeIntegration: true,
            autoplayPolicy: 'no-user-gesture-required'
        }
    });

    splash.loadFile('splash.html');

    mainWindow = new BrowserWindow({
        width: 1100,
        height: 700,
        minWidth: 900,
        minHeight: 600,
        frame: false, // Quitamos los bordes nativos de Windows
        transparent: true, // Permitimos transparencia para los bordes redondeados
        show: false, // No la mostramos hasta que termine el splash
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js'),
            webviewTag: true
        }
    });

    // Detectar si es el primer inicio (si no existe la configuración)
    if (!fs.existsSync(configPath)) {
        mainWindow.loadFile('src/installer/installer.html');
    } else {
        mainWindow.loadFile('index.html');
    }

    mainWindow.webContents.on('did-finish-load', () => {
        const settings = readSettings();
        mainWindow.webContents.setZoomFactor(settings.appearance.zoomFactor || 1.0);
    });

    // Mostrar la ventana principal después de 11 segundos exactos
    mainWindow.once('ready-to-show', () => {
        const elapsed = Date.now() - startTime;
        const delay = Math.max(0, 11000 - elapsed);
        setTimeout(() => {
            if (splash && !splash.isDestroyed()) splash.destroy();
            mainWindow.show();
        }, delay);
    });

    // Handle minimize to tray
    mainWindow.on('close', (event) => {
        const settings = readSettings();
        if (settings.appearance.minimizeToTray && !app.isQuitting) {
            event.preventDefault();
            mainWindow.hide();
            if (!tray) {
                tray = new Tray(path.join(__dirname, 'assets/icons/favicon.png'));
                const contextMenu = Menu.buildFromTemplate([
                    { label: 'Abrir GLauncher', click: () => mainWindow.show() },
                    { label: 'Cerrar GLauncher', click: () => {
                        app.isQuitting = true;
                        app.quit();
                    }}
                ]);
                tray.setToolTip('GLauncher');
                tray.setContextMenu(contextMenu);
                tray.on('double-click', () => mainWindow.show());
            }
        } else {
            app.quit();
        }
    });

    // Set a flag to prevent the 'close' event from minimizing to tray when the app is actually quitting
    app.isQuitting = false;

    mainWindow.on('unmaximize', () => {
        mainWindow.webContents.send('window-maximized', false);
    });
    mainWindow.on('maximize', () => {
        mainWindow.webContents.send('window-maximized', true);
    });
}

// =====================================================================
// DISABLE CSP: Elimina Content-Security-Policy de todas las respuestas
// para que ninguna URL o API sea bloqueada por el navegador interno.
// =====================================================================
app.whenReady().then(() => {
    const { session } = require('electron');

    session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
        const headers = { ...details.responseHeaders };

        // Eliminar cualquier variante de la cabecera CSP
        Object.keys(headers).forEach(key => {
            if (key.toLowerCase() === 'content-security-policy' ||
                key.toLowerCase() === 'content-security-policy-report-only' ||
                key.toLowerCase() === 'x-content-security-policy' ||
                key.toLowerCase() === 'x-webkit-csp') {
                delete headers[key];
            }
        });

        callback({ responseHeaders: headers });
    });

    createWindows();
});


// Manejador para búsqueda de YouTube
ipcMain.handle('search-youtube', async (event, query) => {
    try {
        const r = await yts(query);
        // Retornamos solo datos serializables para evitar el error de clonación
        return r.videos.slice(0, 10).map(v => ({
            videoId: v.videoId,
            title: v.title,
            thumbnail: v.thumbnail,
            timestamp: v.timestamp,
            author: {
                name: v.author.name
            }
        }));
    } catch (e) {
        console.error("Error buscando en YT:", e);
        return [];
    }
});

// Manejador para obtener información detallada del video vía yt-dlp
ipcMain.handle('get-video-info', async (event, url) => {
    try {
        const info = await ytdl.getBasicInfo(url);
        return info.videoDetails; // Retornamos los detalles (título, autor, thumbnails, etc.)
    } catch (e) {
        console.error("Error al obtener info de YouTube:", e);
        return null;
    }
});

// Lista de dominios seguros que no necesitan ser escaneados por el webview
// Esto evita falsos positivos y mejora el rendimiento al no cargar sitios confiables en el escáner.
const WHITELISTED_DOMAINS = ['google.com', 'youtube.com', 'github.com', 'modrinth.com', 'fabricmc.net', 'mojang.com', 'minecraft.net', 'tenor.com', 'discord.com', 'microsoft.com'];

// --- Análisis de Seguridad de URLs (Anti-Malware/Screamer) ---
ipcMain.handle('check-url-safety', async (event, url) => {
    return new Promise((resolve) => {
        try {
            if (!url.startsWith('http')) return resolve({ safe: true });

            const urlObj = new URL(url);
            const hostname = urlObj.hostname.toLowerCase().replace('www.', '');

            // 1. Omitir dominios de confianza (Whitelist)
            if (WHITELISTED_DOMAINS.some(d => hostname === d || hostname.endsWith('.' + d))) {
                return resolve({ safe: true });
            }

            // 2. Crear ventana oculta para análisis dinámico
            const scanner = new BrowserWindow({
                show: false,
                webPreferences: { offscreen: true, partition: 'persist:scanner', images: false }
            });

            let popupCount = 0;
            scanner.webContents.setWindowOpenHandler(() => {
                popupCount++;
                return { action: 'deny' };
            });

            const safetyTimeout = setTimeout(() => {
                if (!scanner.isDestroyed()) { scanner.destroy(); resolve({ safe: true }); }
            }, 8000);

            scanner.loadURL(url).catch(() => {
                clearTimeout(safetyTimeout);
                if (!scanner.isDestroyed()) scanner.destroy();
                resolve({ safe: true });
            });

            scanner.webContents.on('did-finish-load', async () => {
                // Pequeña espera para que carguen scripts de publicidad
                setTimeout(async () => {
                    try {
                        const pageData = await scanner.webContents.executeJavaScript(`
                            (function() {
                                const text = document.body.innerText.toLowerCase();
                                const deceptive = ['free no virus', 'no virus gratis', 'gift card generator', 'win free minecoins', 'tu pc tiene virus'];
                                return {
                                    found: deceptive.find(phrase => text.includes(phrase)),
                                    downloads: Array.from(document.querySelectorAll('a[href*=".exe"], a[href*=".zip"], a[href*=".msi"]')).length
                                };
                            })()
                        `);
                        clearTimeout(safetyTimeout);
                        if (scanner.isDestroyed()) return;
                        scanner.destroy();
                        if (popupCount > 2) resolve({ safe: false, reason: 'Publicidad invasiva (Pop-up Ads)' });
                        else if (pageData.found) resolve({ safe: false, reason: 'Contenido engañoso: ' + pageData.found });
                        else if (pageData.downloads > 5) resolve({ safe: false, reason: 'Múltiples archivos sospechosos' });
                        else resolve({ safe: true });
                    } catch (e) {
                        clearTimeout(safetyTimeout);
                        if (!scanner.isDestroyed()) scanner.destroy();
                        resolve({ safe: true });
                    }
                }, 1500);
            });
        } catch (err) { resolve({ safe: true }); }
    });
});

// Manejador para estado de servidores Minecraft
ipcMain.handle('get-server-status', async (event, host) => {
    try {
        const options = { timeout: 1000 * 5, enableSRV: true };
        const result = await util.status(host, 25565, options);
        return result;
    } catch (e) {
        console.error("Error al pinguear server:", e);
        return null;
    }
});

// Helper function to upload a buffer to Supabase Storage
async function uploadFileToSupabase(bucket, fileName, buffer) {
    try {
        const url = `${SUPABASE_PROJECT_URL}/storage/v1/object/${bucket}/${fileName}`;
        const response = await axios.put(url, buffer, {
            headers: {
                'apikey': SUPABASE_PUB_KEY,
                'Content-Type': 'image/png', // Asumimos PNG para skins
                'x-upsert': 'true' // Sobrescribir si el archivo ya existe
            },
            maxContentLength: Infinity,
            maxBodyLength: Infinity
        });

        if (response.status === 200) {
            console.log(`[Supabase] Archivo ${fileName} subido a ${bucket} con éxito.`);
            return `${SUPABASE_PROJECT_URL}/storage/v1/object/public/${bucket}/${fileName}`;
        } else {
            throw new Error(`Error al subir a Supabase: ${response.status} - ${response.statusText}`);
        }
    } catch (error) {
        console.error(`[Supabase] Error al subir ${fileName} a ${bucket}:`, error.message);
        if (error.response) {
            console.error('Supabase Response Data:', error.response.data);
        }
        throw error;
    }
}

// Function to process skin and upload to Supabase
async function processAndUploadSkin(filePath, nickname) {
    try {
        if (!fs.existsSync(filePath)) {
            throw new Error('El archivo de skin no existe en la ruta especificada.');
        }

        const skinBuffer = fs.readFileSync(filePath);
        const image = await Jimp.read(skinBuffer);

        // 1. Subir la skin completa (Sobrescribe si existe por el header x-upsert)
        const fullSkinFileName = `${nickname}.png`;
        const fullSkinUrl = await uploadFileToSupabase('skins/skins', fullSkinFileName, skinBuffer);

        // 2. Generar cabeza combinando Capa 1 (Base) y Capa 2 (Hat/Accesorios)
        // Capa 1: (8, 8, 8, 8) | Capa 2: (40, 8, 8, 8)
        const headBase = image.clone().crop(8, 8, 8, 8);
        const headOverlay = image.clone().crop(40, 8, 8, 8);
        
        // Superponer la segunda capa sobre la primera
        headBase.composite(headOverlay, 0, 0);
        
        // Redimensionar para que se vea nítida en la UI (Pixel Art scaling)
        const headImage = await headBase.resize(64, 64, Jimp.RESIZE_NEAREST_NEIGHBOR);
        const headBuffer = await headImage.getBufferAsync(Jimp.MIME_PNG);
        
        const headFileName = `${nickname}_head.png`;
        const headUrl = await uploadFileToSupabase('skins/heads', headFileName, headBuffer);

        // 3. Sincronizar con la tabla de la base de datos (Upsert por player_name)
        // Se asume que la tabla se llama 'player_skins'
        await axios.post(dbUrl, {
            player_name: nickname,
            skin_url: fullSkinUrl,
            head_url: headUrl
        }, {
            headers: {
                'apikey': SUPABASE_PUB_KEY,
                'Content-Type': 'application/json',
                'Prefer': 'resolution=merge-duplicates' // Esto maneja el Upsert basado en la constraint del player_name
            }
        });

        return { 
            fullSkinUrl, 
            headUrl 
        };
    } catch (error) {
        console.error('[Skin Upload] Error procesando y subiendo skin:', error);
        throw error;
    }
}

// IPC handler for uploading skin to Supabase
ipcMain.handle('upload-skin-to-supabase', async (event, { filePath, nickname }) => {
    try {
        const urls = await processAndUploadSkin(filePath, nickname);
        return { success: true, fullSkinUrl: urls.fullSkinUrl, headUrl: urls.headUrl };
    } catch (error) {
        return { success: false, error: error.message };
    }
});

// Manejador para descargar y subir skin desde una URL de biblioteca
ipcMain.handle('upload-skin-from-url', async (event, { url, nickname }) => {
    try {
        const response = await axios.get(url, { responseType: 'arraybuffer' });
        const tempPath = path.join(rootPath, `temp_skin_${nickname}.png`);
        fs.writeFileSync(tempPath, Buffer.from(response.data));
        
        // Reutilizamos tu función existente que ya hace el recorte de cabeza y el upsert en la DB
        const urls = await processAndUploadSkin(tempPath, nickname);
        
        // Limpiamos el temporal
        fs.unlinkSync(tempPath);
        
        return { success: true, ...urls };
    } catch (error) {
        console.error("Error en upload-skin-from-url:", error);
        return { success: false, error: error.message };
    }
});

// Canal de Optimización de Sistema
ipcMain.handle('run-system-optimization', async () => {
    try {
        return await optimizeSystem();
    } catch (e) {
        return { success: false, error: e.message };
    }
});

// Manejadores de Configuración
ipcMain.handle('get-settings', () => {
    return readSettings();
});

ipcMain.handle('reset-launcher', async () => {
    try {
        if (fs.existsSync(configPath)) fs.unlinkSync(configPath);
        if (fs.existsSync(sessionPath)) fs.unlinkSync(sessionPath);
        app.relaunch();
        app.exit(0);
        return true;
    } catch (e) {
        console.error("Error al resetear el launcher:", e);
        return false;
    }
});

ipcMain.handle('save-settings', (event, newSettings) => {
    try {
        fs.writeFileSync(configPath, JSON.stringify(newSettings, null, 4));
        return true;
    } catch (e) {
        console.error('[main.js] Error saving settings:', e);
        return false;
    }
});

ipcMain.handle('get-accounts', () => {
    return readSession();
});

ipcMain.handle('save-accounts', (event, accounts) => {
    try {
        fs.writeFileSync(sessionPath, JSON.stringify(accounts, null, 4));
        return true;
    } catch (e) {
        console.error("Error saving sesion.json:", e);
        return false;
    }
});

// Obtener lista de carpetas en .glauncher/versions para el selector
ipcMain.handle('get-installed-versions', async () => {
    const vPath = path.join(rootPath, 'versions');
    if (!fs.existsSync(vPath)) return [];
    try {
        return fs.readdirSync(vPath).filter(f => fs.statSync(path.join(vPath, f)).isDirectory());
    } catch (e) {
        return [];
    }
});

// Evento para descargar versiones sin iniciar el juego
ipcMain.on('download-version', async (event, args) => {
    const { version, modloaderType, modloaderVersion } = args;
    const versionId = version.id;
    const mcFolder = new MinecraftFolder(rootPath);

    // Ensure the versions directory exists
    const versionsPath = path.join(rootPath, 'versions');
    if (!fs.existsSync(versionsPath)) {
        console.log('[GLauncher] Creating versions directory at', versionsPath);
        fs.mkdirSync(versionsPath, { recursive: true });
    }

    try {
        mainWindow.webContents.send('download-status', { status: 'started', version: versionId });

        console.log(`[GLauncher] Descargando archivos para la versión: ${versionId} (${modloaderType || 'Vanilla'})`);

        // Configuración de instalación más robusta para evitar Timeouts y archivos corruptos
        const installOptions = {
            resourceTimeout: 30000, // 30 segundos de espera por archivo
            maxConcurrency: 5,      // Bajamos de 10 a 5 para no saturar conexiones débiles
            side: 'client'
        };

        // If a modloader is requested, first ensure the vanilla version is installed
        if (modloaderType && modloaderType !== 'vanilla') {
            // Check if vanilla version folder already exists
            const vanillaPath = path.join(versionsPath, versionId);
            const vanillaExists = fs.existsSync(vanillaPath);
            if (!vanillaExists) {
                console.log('[GLauncher] Vanilla not present, installing base version first...');
                await install(version, mcFolder, installOptions);
            } else {
                console.log('[GLauncher] Vanilla already present, skipping base install.');
            }

            // Now install the requested modloader
            if (modloaderType === 'fabric' && modloaderVersion) {
                await installFabric({ minecraft: versionId, loader: modloaderVersion }, mcFolder);
            } else if (modloaderType === 'forge' && modloaderVersion) {
                await installForge({ mcversion: versionId, version: modloaderVersion }, mcFolder);
            } else if (modloaderType === 'neoforge' && modloaderVersion) {
                await installNeoForge({ minecraft: versionId, version: modloaderVersion }, mcFolder);
            }
        } else {
            // No modloader requested, just install vanilla
            await install(version, mcFolder, installOptions);
        }

        // Log installed files for debugging
        try {
            const installed = fs.readdirSync(versionsPath);
            console.log('[GLauncher] Installed versions after install:', installed);
        } catch (e) {
            console.warn('[GLauncher] Could not list versions folder:', e);
        }



        
        console.log(`[GLauncher] Descarga e instalación completada con éxito.`);
        mainWindow.webContents.send('download-status', { status: 'success', version: versionId });
        
    } catch (err) {
        console.error("Error en proceso de descarga:", err);
        if (mainWindow && !mainWindow.webContents.isDestroyed()) {
            mainWindow.webContents.send('download-status', { status: 'error', message: err.message });
        }
    }
});

/**
 * Descarga el authlib-injector.jar si no existe
 */
async function ensureAuthlibInjector() {
    const injectorPath = path.join(rootPath, 'authlib-injector.jar');
    if (fs.existsSync(injectorPath)) return injectorPath;

    console.log("[GLauncher] Descargando Authlib-Injector...");
    try {
        const response = await axios({
            method: 'get',
            url: 'https://authlib-injector.yangeon.org/authlib-injector.jar',
            responseType: 'stream'
        });
        if (response.status !== 200) throw new Error("Servidor de injector respondió con error");
        const writer = fs.createWriteStream(injectorPath);
        response.data.pipe(writer);
        return new Promise((resolve, reject) => {
            writer.on('finish', () => resolve(injectorPath));
            writer.on('error', (err) => { fs.unlink(injectorPath, () => {}); reject(err); });
        });
    } catch (e) {
        console.error("Error descargando injector:", e);
        return null;
    }
}

ipcMain.on('launch-game', async (event, args) => {
    const { version, username, uuid, accountType } = args;
    const settings = readSettings();
    const mcFolder = new MinecraftFolder(rootPath);

    try {
        console.log(`[Launch] Preparando sesión para: ${username} [${accountType}]`);

        // Si es cuenta de Microsoft, idealmente usarías el token real, 
        // para offline usamos el método 'offline' que genera la estructura necesaria.
        const validUuid = (uuid && uuid.includes('-')) ? uuid : undefined;
        const authInfo = offline(username, validUuid);

        // Sanitizamos los argumentos eliminando entradas vacías o indefinidas que rompen @xmcl/core
        const rawJvmArgs = settings.java.jvmArgs || "";
        let jvmArgs = rawJvmArgs.split(/\s+/).filter(arg => arg.length > 0);
        
        // Lógica de Agent local (-Dskin.path)
        if (settings.appearance.skinPath) {
            jvmArgs.push(`-Dskin.path=${settings.appearance.skinPath}`);
        }

        // Lógica de Authlib-Injector para Skins Globales (Multiplayer)
        if (settings.appearance.yggdrasilServer) {
            const injector = await ensureAuthlibInjector();
            if (injector) {
                jvmArgs.push(`-javaagent:${injector}=${settings.appearance.yggdrasilServer}`);
            }
        }

        // Validación robusta de la ruta de Java
        let javaPath = settings.java.javaPath;
        if (javaPath && javaPath !== 'default' && javaPath.trim() !== "") {
            if (!fs.existsSync(javaPath)) {
                console.warn(`[GLauncher] Ruta de Java no encontrada: ${javaPath}. Usando defecto.`);
                javaPath = undefined;
            }
        } else {
            javaPath = undefined;
        }

        launch({
            gamePath: mcFolder.root,
            version: version,
            javaPath: javaPath,
            minMemory: settings.java.ramMin * 1024,
            maxMemory: settings.java.ram * 1024,
            extraJVMArgs: jvmArgs,
            gameProfile: {
                name: username,
                id: authInfo.selectedProfile.id.replace(/-/g, "")
            },
            accessToken: authInfo.accessToken,
            userType: accountType === 'Microsoft' ? 'mojang' : 'legacy',
            properties: authInfo.selectedProfile.properties || {},
            resolution: {
                width: settings.game.resolutionWidth || 1280,
                height: settings.game.resolutionHeight || 720,
                fullscreen: settings.game.fullscreen || false
            },
            launcherName: "GLauncher",
            launcherBrand: "1.0.0"
        }).then((process) => {
            console.log("Juego lanzado con éxito");
            // Avisamos al renderer que el juego inició para el logro
            mainWindow.webContents.send('game-launched');
            process.on('close', () => {
                if (mainWindow && !mainWindow.webContents.isDestroyed()) {
                    mainWindow.webContents.send('download-status', { status: 'closed' });
                }
            });
        }).catch(err => {
            console.error("Error en la generación de argumentos:", err);
            mainWindow.webContents.send('download-status', { 
                status: 'error', 
                message: "Error en los argumentos de Java: " + err.message 
            });
        });
    } catch (err) {
        console.error("Error crítico al lanzar:", err);
        mainWindow.webContents.send('download-status', { 
            status: 'error', 
            message: "No se pudo iniciar el juego. Verifica tu configuración." 
        });
    }
});

// Manejo de botones de la ventana personalizada
ipcMain.on('window-minimize', () => {
    mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
    if (mainWindow.isMaximized()) {
        mainWindow.unmaximize();
    } else {
        mainWindow.maximize();
    }
});

ipcMain.on('window-close', () => {
    mainWindow.close();
});

// Manejador para abrir el selector de imágenes de perfil
ipcMain.handle('select-profile-picture', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile'],
        filters: [{ name: 'Imágenes', extensions: ['png', 'jpg', 'jpeg', 'gif'] }]
    });
    return canceled ? null : filePaths[0];
});

// Manejador para seleccionar múltiples fondos (imágenes o videos)
ipcMain.handle('select-background-files', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile', 'multiSelections'],
        filters: [{ name: 'Fondos (Imagen/Video)', extensions: ['png', 'jpg', 'jpeg', 'jfif', 'mp4', 'webm'] }]
    });
    return canceled ? null : filePaths;
});

// Manejador para seleccionar el archivo de Skin (.png)
ipcMain.handle('select-skin-file', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile'],
        filters: [{ name: 'Skins de Minecraft', extensions: ['png'] }]
    });
    return canceled ? null : filePaths[0];
});

// Manejador para seleccionar el ejecutable de Java
ipcMain.handle('select-java-executable', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        properties: ['openFile'],
        filters: [{ name: 'Java Executable', extensions: ['exe', 'bin', '*'] }]
    });
    return canceled ? null : filePaths[0];
});

// Handler for opening a path in the system's default file explorer
ipcMain.handle('open-path', async (event, path) => {
    shell.openPath(path);
});

// --- Lógica de Ventana Externa para Mini Player ---
ipcMain.on('open-external-mini-player', (event, title) => {
    if (externalMiniPlayer) {
        externalMiniPlayer.close();
        externalMiniPlayer = null;
        return;
    }

    externalMiniPlayer = new BrowserWindow({
        width: 320,
        height: 100,
        frame: false,
        transparent: true,
        alwaysOnTop: true,
        resizable: false,
        skipTaskbar: true, // No ensucia la barra de tareas
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        }
    });

    externalMiniPlayer.loadFile('src/ui/mini-player.html');
    
    externalMiniPlayer.webContents.on('did-finish-load', () => {
        externalMiniPlayer.webContents.send('update-title', title);
    });

    externalMiniPlayer.on('closed', () => externalMiniPlayer = null);
});

ipcMain.on('sync-mini-player-title', (event, title) => {
    if (externalMiniPlayer) externalMiniPlayer.webContents.send('update-title', title);
});

ipcMain.on('update-zoom', (event, factor) => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.setZoomFactor(factor);
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});

// --- Lógica del Instalador Funcional ---

ipcMain.handle('installer-select-path', async () => {
    const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
        properties: ['openDirectory'],
        title: 'Seleccionar carpeta de instalación'
    });
    return canceled ? null : filePaths[0];
});

ipcMain.handle('installer-run', async (event, config) => {
    try {
        // 1. Asegurar que la ruta exista
        await fsExtra.ensureDir(config.path);
        
        // 2. Crear estructura de carpetas de Minecraft
        await fsExtra.ensureDir(path.join(config.path, 'versions'));
        await fsExtra.ensureDir(path.join(config.path, 'assets'));
        await fsExtra.ensureDir(path.join(config.path, 'libraries'));
        
        // 3. Generar el archivo de configuración inicial del Launcher
        const initialSettings = JSON.parse(JSON.stringify(defaultSettings));
        initialSettings.general.root = config.path;
        initialSettings.java.javaPath = config.java;
        
        await fsExtra.writeJson(path.join(config.path, 'launcher.json'), initialSettings, { spaces: 4 });
        
        // 4. Crear accesos directos reales en Windows
        if (process.platform === 'win32') {
            if (config.shortcuts.desktop) {
                ws.create({
                    src: process.execPath,
                    dest: path.join(app.getPath('desktop'), 'GLauncher.lnk'),
                    icon: path.join(__dirname, 'assets/icons/favicon.ico'),
                    desc: 'Lanzador moderno de Minecraft'
                });
            }
            
            if (config.shortcuts.startMenu) {
                const startMenuPath = path.join(app.getPath('appData'), 'Microsoft', 'Windows', 'Start Menu', 'Programs', 'GLauncher');
                await fsExtra.ensureDir(startMenuPath);
                ws.create({
                    src: process.execPath,
                    dest: path.join(startMenuPath, 'GLauncher.lnk'),
                    icon: path.join(__dirname, 'assets/icons/favicon.ico')
                });
            }
        }

        console.log(`[Instalador] GLauncher configurado en: ${config.path}`);
        return { success: true };
    } catch (error) {
        console.error("Error durante la instalación:", error);
        return { success: false, error: error.message };
    }
});

ipcMain.handle('installer-detect-java', async () => {
    return new Promise((resolve) => {
        findJavaHome({ allowHomeWithoutJava: true }, (err, home) => {
            if (err || !home) return resolve(null);
            const javaExe = path.join(home, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
            resolve(javaExe);
        });
    });
});

ipcMain.handle('get-default-install-path', () => {
    return path.join(app.getPath('appData'), '.glauncher');
});

ipcMain.on('installer-finished', () => {
    // Reiniciar la aplicación para cargar la nueva configuración
    app.relaunch();
    app.exit();
});