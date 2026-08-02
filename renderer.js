const ipcRenderer = window.electronAPI || {
    send: (c) => console.warn(`IPC Send ignorado: ${c}. ¿Estás en un navegador?`),
    invoke: async (c, ...args) => { 
        console.warn(`IPC Invoke ignorado: ${c}`); //
        if (c === 'get-installed-versions') return ['1.21 (Mock)', '1.20.1 (Mock)']; // Mock for versions
        if (c === 'search-youtube') return [ // Mock for YouTube search
            { videoId: 'dQw4w9WgXcQ', title: 'Rick Astley - Never Gonna Give You Up (Mock)', thumbnail: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg', author: { name: 'Rick Astley' }, timestamp: '3:33' },
            { videoId: '9bZkp7q19f0', title: 'PSY - GANGNAM STYLE (Mock)', thumbnail: 'https://i.ytimg.com/vi/9bZkp7q19f0/hqdefault.jpg', author: { name: 'officialpsy' }, timestamp: '4:12' }
        ];
        if (c === 'get-settings') { // Mock for settings
            console.warn('Returning mock settings for get-settings');
            // Provide a mock settings object that includes the achievements structure
            return { 
                achievements: { melomano: false, first_download: false, first_launch: false, socializer: false, stylist: false, configurator: false, mod_hunter: false, veteran: false, explorer: false, cleaner: false, rey_del_pop: false, server_adder: false, ram_master: false, old_school: false, bg_collector: false, fullscreen_king: false, modloader_expert: false, safety_first: false },
                stats: { launch_count: 0, views_visited: [] },
                appearance: { theme: "Dark", skinPath: null, yggdrasilServer: "", minimizeToTray: false, customBackgrounds: [] },
                game: { resolutionWidth: 1280, resolutionHeight: 720, closeOnLaunch: true, fullscreen: false, autoJoin: "", openLogs: false },
                java: { ram: 4, ramMin: 1, jvmArgs: "-XX:+UseG1GC", javaPath: "default", priority: "normal" },
                general: { language: "Español", root: "" }
            };
        }
        if (c === 'save-settings') { // Mock for save-settings
            console.warn('Mocking save-settings, returning true');
            return true;
        }
        if (c === 'check-url-safety') {
            const url = args[0] || "";
            const urlLower = url.toLowerCase();
            // Whitelist simulada para evitar bloqueos en sitios comunes durante la prueba
            const whitelist = ['google.com', 'youtube.com', 'github.com', 'modrinth.com', 'minecraft.net', 'tenor.com'];
            if (whitelist.some(d => urlLower.includes(d))) return { safe: true };

            // Heurística de prueba para el simulador web
            if (urlLower.includes('youareanidiot') || urlLower.includes('virus') || urlLower.includes('free-ram') || urlLower.includes('scam')) {
                return { safe: false, reason: 'Web maliciosa simulada (Prueba Web)' };
            }
            if (urlLower.includes('.exe') || urlLower.includes('.msi') || urlLower.includes('.zip')) {
                return { safe: false, reason: 'Descarga de archivo sospechosa (Prueba Web)' };
            }
            return { safe: true };
        }
        return null; // Default for unhandled invokes
    },
    on: (c) => console.warn(`IPC Listener ignorado: ${c}`)
};

// --- Estado Global (Declarados al inicio para evitar ReferenceError) ---
let allModResults = [];
let allVersionsCache = [];
let currentModloaderData = { fabric: [], forge: [], neoforge: [] };
let skinViewer = null;
let profileSkinViewer = null;
let cachedSettings = null;
let selectedModloader = { value: 'vanilla', text: 'Vanilla (Original)' };

// Configuración de Supabase para Texturas
const SUPABASE_STORAGE_URL = "https://ouqpeojilykkrmatijxp.supabase.co/storage/v1/object/public/skins";

// Mock de historial de avatares para previsualización
let userPfpHistory = [];
// Mock de cuentas múltiples (YouTube for TV Style)
let userAccounts = [];
const modsPerPage = 25;
// Cache local para evitar spam de notificaciones y peticiones IPC innecesarias
const unlockedCache = new Set();
const pendingUnlocks = new Set();
// Estado del Chat
let lastMessageTime = 0;
let lastMessageContent = "";
let chatMutedUntil = 0;
let profanityStrikes = 0;
const BAN_DURATION_5_DAYS = 5 * 24 * 60 * 60 * 1000;
const BAN_DURATION_1_DAY = 24 * 60 * 60 * 1000;
const BAN_DURATION_10_MINS = 10 * 60 * 1000;
const CHAT_COOLDOWN = 2000; // 2 segundos entre mensajes
let currentTypeFilter = 'release';
let selectedPlaylistId = null;
/**
 * Mapeo de inicializadores de vista para evitar el crecimiento del switch/if
 */
const viewInitializers = {
    'inicio': setupVersionSelector,
    'versions': loadMinecraftVersions,
    'settings': initSettingsLogic,
    'server': loadServers,
    'micuenta': initAccountView,
    'gmusic': initGMusic
};

/**
 * Carga dinámicamente una vista desde src/ui/
 * @param {string} viewName - Nombre del archivo html sin extensión
 */
async function loadView(viewName) {
    const content = document.querySelector('.content');
    
    // Usar caché para evitar llamadas IPC redundantes
    // Forzamos actualización si entramos a ajustes o perfil para asegurar consistencia
    if (!cachedSettings || viewName === 'settings' || viewName === 'micuenta') {
        cachedSettings = await ipcRenderer.invoke('get-settings').catch(() => null);
    }
    const settings = cachedSettings;
    
    const useAnimations = settings?.appearance?.animations !== false;

    if (useAnimations) content.classList.add('view-changing');

    if (useAnimations) await new Promise(resolve => setTimeout(resolve, 150));
    
    // Mostrar un spinner mientras se carga la vista
    content.innerHTML = '<div class="view-loader" style="display:flex; justify-content:center; align-items:center; height:100%; opacity:0.5;"><i class="fas fa-circle-notch fa-spin fa-2x" style="color:var(--primary);"></i></div>';

    try {
        const response = await fetch(`src/ui/${viewName}.html`);
        if (!response.ok) throw new Error(`Vista ${viewName} no encontrada`);
        
        const html = await response.text();
        content.innerHTML = html;
        content.classList.remove('view-changing');

        // --- MEJORA: Ejecutar scripts incrustados en la vista ---
        const scripts = content.querySelectorAll('script');
        scripts.forEach(oldScript => {
            const newScript = document.createElement('script');
            // Copiamos el contenido del script
            newScript.textContent = oldScript.textContent;
            // Copiamos atributos si los hay (src, etc)
            Array.from(oldScript.attributes).forEach(attr => newScript.setAttribute(attr.name, attr.value));
            // Lo añadimos al documento para que se ejecute y luego lo limpiamos
            document.body.appendChild(newScript).parentNode.removeChild(newScript);
        });

        // Lógica de Logro: Explorador
        if (settings && settings.stats) {
            if (!settings.stats.views_visited.includes(viewName)) {
                settings.stats.views_visited.push(viewName);
                await ipcRenderer.invoke('save-settings', settings);
                // Si visitó las 5 principales (inicio, versions, mods, gmusic, micuenta)
                if (settings.stats.views_visited.length >= 5) {
                    unlockAchievement('explorer', "Explorador");
                }
            }
        }

        // Actualizar estado visual de los botones en la navbar
        const buttons = document.querySelectorAll('.nav-btn');
        buttons.forEach(btn => {
            btn.classList.remove('active');
            // Si el texto del botón coincide o el onclick lo referencia, lo activamos.
            // Se añade una verificación para asegurar que getAttribute no retorne null.
            const onClickAttr = btn.getAttribute('onclick') || "";
            if (onClickAttr && onClickAttr.includes(`'${viewName}'`)) {
                btn.classList.add('active');
            }
        });

        // Ejecutar inicializador si existe para la vista
        if (viewInitializers[viewName]) viewInitializers[viewName]();

        // Sincronizar datos de la cuenta activa en la nueva vista cargada (Inicio, Mi Cuenta, etc.)
        updateUIFromAccount();

        // Manejo del Reproductor Global
        const player = document.getElementById('global-player-container');
        if (player) {
            if (viewName === 'gmusic') {
                player.classList.remove('hidden-player');
            } else {
                player.classList.add('hidden-player');
            }
        }

        console.log(`Vista [${viewName}] cargada correctamente.`);
    } catch (error) {
        showNotification(`Error al cargar la sección ${viewName}`, "error");
        console.error(error);
    }
}

/**
 * Actualiza globalmente todos los elementos de la interfaz que muestran información del usuario
 */
function updateUIFromAccount() {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    if (!activeAcc) return;

    let avatarUrl;
    
    if (activeAcc.type === 'Microsoft') {
        // Si es Premium, usamos el servicio de Mojang
        avatarUrl = `https://mc-heads.net/avatar/${activeAcc.nickname || activeAcc.id}`;
    } else {
        // Si es Offline, usamos tu base de datos de Supabase
        avatarUrl = `${SUPABASE_STORAGE_URL}/heads/${activeAcc.nickname}_head.png`;
    }

    // Fallback de seguridad por si no hay imagen en la DB aún
    const finalAvatar = avatarUrl || activeAcc.avatar || "assets/images/default-avatar.png";

    // name es el apodo (Hola), nickname es el técnico (DaniCraftYT25)
    document.querySelectorAll('.display-name').forEach(el => el.innerText = activeAcc.name || activeAcc.nickname || "Jugador");
    document.querySelectorAll('.mc-handle').forEach(el => el.innerText = '@' + (activeAcc.nickname || "offline_user"));
    document.querySelectorAll('.profile-avatar-large, .user-avatar').forEach(img => {
        img.src = finalAvatar;
        img.onerror = () => {
            img.src = "assets/images/default-avatar.png";
            img.onerror = null;
        };
    });
}

/**
 * Inicializa la lógica de carga y guardado de ajustes
 */
async function initSettingsLogic() {
    const settings = await ipcRenderer.invoke('get-settings');
    cachedSettings = settings;
    if (!settings) return;

    const inputMap = {
        'game-res-w': settings.game.resolutionWidth,
        'game-res-h': settings.game.resolutionHeight,
        'game-auto-join': settings.game.autoJoin,
        'yggdrasil-url': settings.appearance.yggdrasilServer,
        'appearance-theme': settings.appearance.theme,
        'appearance-accent': settings.appearance.accentColor,
        'appearance-zoom': settings.appearance.zoomFactor,
        'appearance-blur-range': settings.appearance.bgBlur || 0,
        'appearance-volume-range': settings.appearance.bgVideoVolume ?? 0.5,
        'general-lang': settings.general.language,
        'general-root': settings.general.root,
        'ram-range': settings.java.ram,
        'ram-min-range': settings.java.ramMin || 1,
        'java-args': settings.java.jvmArgs,
        'java-path': settings.java.javaPath,
        'java-priority': settings.java.priority || "normal"
    };

    // Poblar valores
    for (const [id, value] of Object.entries(inputMap)) {
        const el = document.getElementById(id);
        if (el) {
            el.value = value ?? "";
            if (el.type === 'range') {
                // Detección de etiquetas: ram-val, ram-min-val, zoom-val, blur-val, volume-val
                let labelId = el.id.includes('-range') ? el.id.replace('-range', '-val') : el.id + '-val';
                labelId = labelId.replace('appearance-', '');
                const label = document.getElementById(labelId);
                
                const updateLabel = (val) => {
                    if (!label) return;
                    if (el.id.includes('zoom')) {
                        label.innerText = Math.round(val * 100) + "%";
                    } else if (el.id.includes('blur')) {
                        label.innerText = val + "px";
                    } else if (el.id.includes('volume')) {
                        label.innerText = Math.round(val * 100) + "%";
                    } else {
                        label.innerText = val + "GB";
                    }
                };

                updateLabel(value);
                el.oninput = () => updateLabel(el.value);
            }
            // Añadir evento de guardado automático
            el.onchange = () => saveLauncherConfig();
        }
    }

    // Lógica de Logro: Maestro del Java
    if (settings.java.ram > 8) {
        unlockAchievement('ram_master', "Maestro del Java");
    }

    // Manejo del switch de animaciones
    const animToggle = document.getElementById('appearance-animations');
    if (animToggle) {
        animToggle.checked = settings.appearance.animations !== false;
        animToggle.onchange = () => saveLauncherConfig();
    }

    // Poblar Switches / Checkboxes
    const checkboxMap = {
        'appearance-animations': settings.appearance.animations,
        'appearance-dynamic-bg': settings.appearance.dynamicBg,
        'general-telemetry': settings.general.telemetry,
        'general-tray': settings.appearance.minimizeToTray, // New tray setting
        'game-fullscreen': settings.game.fullscreen,
        'game-open-logs': settings.game.openLogs, // New openLogs setting
        'game-close-on-launch': settings.game.closeOnLaunch
    };

    for (const [id, value] of Object.entries(checkboxMap)) {
        const el = document.getElementById(id);
        if (el) {
            el.checked = value !== false;
            el.onchange = () => saveLauncherConfig();
        }
    }

    // Botón de Selección de Fondos
    const bgBtn = document.getElementById('btn-select-backgrounds');
    if (bgBtn) {
        bgBtn.onclick = async () => {
            const paths = await ipcRenderer.invoke('select-background-files');
            if (paths && paths.length > 0) await updateBackgrounds(paths);
        };
    }

    // Asegurar que los motores del modal de descarga funcionen aunque no entres a ajustes
    if (!window.modalDropdownsInitialized) {
        initModalDropdowns();
        window.modalDropdownsInitialized = true;
    }
}

async function updateBackgrounds(paths) {
    const settings = await ipcRenderer.invoke('get-settings');
    settings.appearance.customBackgrounds = paths;

    if (paths.length >= 3) {
        unlockAchievement('bg_collector', "Arquitecto de Paisajes");
    }

    await ipcRenderer.invoke('save-settings', settings);
    cachedSettings = settings;
    
    loadRandomBackground(); // Aplicar inmediatamente
    showNotification(`${paths.length} fondo(s) configurado(s)`, "success");
}

async function saveLauncherConfig() {
    // Recuperamos los ajustes actuales PRIMERO para no perder datos como skinPath
    const currentSettings = await ipcRenderer.invoke('get-settings').catch(() => ({}));
    
    // Helper para obtener valores de inputs solo si existen en el DOM actual,
    // de lo contrario mantener el valor actual del archivo de configuración.
    const getV = (id, currentVal) => document.getElementById(id) ? document.getElementById(id).value : currentVal;
    const getC = (id, currentVal) => document.getElementById(id) ? document.getElementById(id).checked : currentVal;
    
    const newSettings = {
        game: {
            resolutionWidth: parseInt(getV('game-res-w', currentSettings.game.resolutionWidth)),
            resolutionHeight: parseInt(getV('game-res-h', currentSettings.game.resolutionHeight)),
            closeOnLaunch: getC('game-close-on-launch', currentSettings.game.closeOnLaunch),
            fullscreen: getC('game-fullscreen', currentSettings.game.fullscreen),
            autoJoin: getV('game-auto-join', currentSettings.game.autoJoin),
            openLogs: getC('game-open-logs', currentSettings.game.openLogs)
        },
        appearance: {
            theme: getV('appearance-theme', currentSettings.appearance.theme),
            accentColor: getV('appearance-accent', currentSettings.appearance.accentColor),
            animations: getC('appearance-animations', currentSettings.appearance.animations),
            dynamicBg: getC('appearance-dynamic-bg', currentSettings.appearance.dynamicBg),
            skinPath: currentSettings.appearance?.skinPath || null,
            yggdrasilServer: getV('yggdrasil-url', currentSettings.appearance.yggdrasilServer),
            customBackgrounds: currentSettings.appearance?.customBackgrounds || [],
            minimizeToTray: getC('general-tray', currentSettings.appearance.minimizeToTray),
            zoomFactor: parseFloat(getV('appearance-zoom', currentSettings.appearance.zoomFactor)),
            bgBlur: parseInt(getV('appearance-blur-range', currentSettings.appearance.bgBlur || 0)),
            bgVideoVolume: parseFloat(getV('appearance-volume-range', currentSettings.appearance.bgVideoVolume ?? 0.5))
        },
        general: {
            language: getV('general-lang', currentSettings.general.language),
            telemetry: getC('general-telemetry', currentSettings.general.telemetry)
        },
        java: {
            ram: parseInt(getV('ram-range', currentSettings.java.ram)),
            ramMin: parseInt(getV('ram-min-range', currentSettings.java.ramMin)),
            jvmArgs: getV('java-args', currentSettings.java.jvmArgs),
            javaPath: getV('java-path', currentSettings.java.javaPath),
            priority: getV('java-priority', currentSettings.java.priority)
        }
    };

    applyPerformanceSettings(newSettings.appearance.animations);
    newSettings.achievements = currentSettings.achievements;
    newSettings.stats = currentSettings.stats;
    newSettings.servers = currentSettings.servers;

    if (!newSettings.achievements.configurator) unlockAchievement('configurator', "¡Configurador!");
    cachedSettings = newSettings;

    await ipcRenderer.invoke('save-settings', newSettings);
    ipcRenderer.send('update-zoom', newSettings.appearance.zoomFactor);
    showNotification("Configuración guardada", "success");
}

/**
 * Aplica o remueve la clase de optimización para quitar animaciones
 */
function applyPerformanceSettings(enabled) {
    if (enabled) document.body.classList.remove('no-animations');
    else document.body.classList.add('no-animations');
}

/**
 * Cambia entre pestañas dentro de la vista de Mi Cuenta
 */
function switchAccountTab(tabId, btn) {
    // Ocultar todos los contenidos
    const tabs = document.querySelectorAll('.account-tab-content');
    tabs.forEach(t => t.classList.remove('active'));
    
    // Quitar clase activa de los botones
    const buttons = document.querySelectorAll('.side-tab-btn');
    buttons.forEach(b => b.classList.remove('active'));
    
    // Activar el seleccionado
    document.getElementById(tabId).classList.add('active');
    btn.classList.add('active');
    
    // Inicializar historial si entramos a personalización
    if (tabId === 'tab-personalizacion') renderPfpHistory();
    if (tabId === 'tab-logros') loadAchievements(); // Cargar lógica específica para logros
    if (tabId === 'tab-skins') updateSkinUI(); // Actualizar previa de skin

    showNotification(`Sección: ${btn.getAttribute('data-label')}`, "success");
}

/**
 * Lista de skins populares predeterminadas
 */
const presetSkins = [
    { name: "Steve", username: "Steve" },
    { name: "Alex", username: "Alex" },
    { name: "Herobrine", username: "Herobrine" },
    { name: "Notch", username: "Notch" },
    { name: "Dream", username: "Dream" },
    { name: "Technoblade", username: "Technoblade" },
    { name: "ElRichMC", username: "ElRichMC" },
    { name: "Geni", username: "Geni" }
];

/**
 * Renderiza la biblioteca de skins populares preestablecidas
 */
function renderPresetSkins() {
    const resultsContainer = document.getElementById('skin-library-results');
    if (!resultsContainer) return;
    
    resultsContainer.innerHTML = '';
    presetSkins.forEach(skin => {
        const card = document.createElement('div');
        card.className = 'mod-card card';
        card.style.minHeight = 'auto';
        card.style.display = 'flex';
        card.style.flexDirection = 'column';
        card.style.alignItems = 'center';
        card.style.padding = '15px';
        card.style.gap = '10px';
        
        const skinUrl = `https://mc-heads.net/skin/${skin.username}`;
        const previewUrl = `https://mc-heads.net/body/${skin.username}/left`;
        
        card.innerHTML = `
            <img src="${previewUrl}" style="height: 100px; object-fit: contain; margin-bottom: 5px;" onerror="this.src='https://mc-heads.net/body/Steve/left'">
            <div class="mod-title" style="font-size: 0.75rem; font-family: 'Montserrat-Bold'; text-align: center; color: #fff; margin: 0;">${skin.name}</div>
            <button class="btn btn-primary" style="width: 100%; font-size: 0.65rem; padding: 6px;" onclick="applyLibrarySkin('${skinUrl}')">
                <i class="fa-solid fa-shirt"></i> APLICAR
            </button>
        `;
        resultsContainer.appendChild(card);
    });
}

/**
 * Lógica de Skins (El "Agent")
 */
async function changeSkin() {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    if (!activeAcc) return showNotification("Debes tener una cuenta activa", "error");

    const filePath = await ipcRenderer.invoke('select-skin-file');
    if (!filePath) return;

    const loading = document.getElementById('skin-loading-overlay');
    if (loading) loading.style.display = 'flex';

    try {
        showNotification("Subiendo skin a la nube...", "warning");
        
        // Llamamos al proceso principal para que procese la imagen y la suba a Supabase
        const result = await ipcRenderer.invoke('upload-skin-to-supabase', { 
            filePath, 
            nickname: activeAcc.nickname 
        });

        if (result.success) {
            const settings = await ipcRenderer.invoke('get-settings');
            settings.appearance.skinPath = filePath; // Guardamos la ruta local por si acaso
            await ipcRenderer.invoke('save-settings', settings);
            
            updateSkinUI();
            unlockAchievement('stylist', "Estilista");
            showNotification("¡Skin subida y aplicada con éxito!", "success");
        } else {
            throw new Error(result.error);
        }
    } catch (error) {
        console.error(error);
        showNotification("Error al subir skin: " + error.message, "error");
    } finally {
        if (loading) loading.style.display = 'none';
    }
}

async function resetSkin() {
    const settings = await ipcRenderer.invoke('get-settings');
    settings.appearance.skinPath = null;
    await ipcRenderer.invoke('save-settings', settings);
    updateSkinUI();
    showNotification("Skin restablecida a Steve", "success");
}

/**
 * Inicializa el visor 3D de Minecraft con soporte para recargas seguras
 */
async function initSkinViewer(skinUrl) {
    const container = document.getElementById('skin-viewer-3d');
    if (!container) return;
    
    if (!skinViewer) {
        skinViewer = new skinview3d.SkinViewer({
            canvas: document.createElement("canvas"),
            width: container.offsetWidth,
            height: container.offsetHeight,
            skin: 'https://mc-heads.net/skin/Steve' // Iniciar con Steve por defecto
        });
        
        container.innerHTML = "";
        container.appendChild(skinViewer.canvas);

        // Animaciones y controles
        skinViewer.animations.add(skinview3d.WalkingAnimation);
        skinViewer.controls.enableZoom = false;
    }

    if (skinUrl) {
        const loadingOverlay = document.getElementById('skin-loading-overlay');
        if (loadingOverlay) loadingOverlay.style.display = 'flex';
        try {
            await skinViewer.loadSkin(skinUrl);
        } catch (e) {
            console.warn("No se pudo cargar la skin especificada, usando Steve:", e);
            await skinViewer.loadSkin('https://mc-heads.net/skin/Steve').catch(() => {});
        } finally {
            if (loadingOverlay) loadingOverlay.style.display = 'none';
        }
    }
}

/**
 * Inicializa el visor 3D en la pestaña de Perfil (Dashboard)
 */
async function initProfileSkinViewer(skinUrl) {
    const container = document.getElementById('profile-skin-viewer-3d');
    if (!container) return;
    
    if (!profileSkinViewer) {
        profileSkinViewer = new skinview3d.SkinViewer({
            canvas: document.createElement("canvas"),
            width: container.offsetWidth,
            height: container.offsetHeight,
            skin: 'https://mc-heads.net/skin/Steve' // Iniciar con Steve por defecto
        });
        
        container.innerHTML = "";
        container.appendChild(profileSkinViewer.canvas);

        // Animaciones y controles
        profileSkinViewer.animations.add(skinview3d.WalkingAnimation);
        profileSkinViewer.controls.enableZoom = false;
    }

    if (skinUrl) {
        try {
            await profileSkinViewer.loadSkin(skinUrl);
        } catch (e) {
            console.warn("No se pudo cargar la skin en el perfil, usando Steve:", e);
            await profileSkinViewer.loadSkin('https://mc-heads.net/skin/Steve').catch(() => {});
        }
    }
}

/**
 * Busca skins ingresando el nombre de usuario de cualquier jugador Premium de Minecraft
 */
async function searchSkins() {
    const query = document.getElementById('skin-search-input').value.trim();
    const resultsContainer = document.getElementById('skin-library-results');
    if (!query) {
        renderPresetSkins();
        return;
    }

    resultsContainer.innerHTML = '<p style="text-align: center; color: #888; padding: 40px;"><i class="fas fa-circle-notch fa-spin"></i> Buscando skin del jugador...</p>';

    try {
        const skinUrl = `https://mc-heads.net/skin/${query}`;
        const previewUrl = `https://mc-heads.net/body/${query}/left`;
        
        resultsContainer.innerHTML = `
            <div class="mod-card card" style="grid-column: 1 / -1; max-width: 300px; margin: 0 auto; display: flex; flex-direction: column; align-items: center; padding: 20px; gap: 15px;">
                <img src="${previewUrl}" style="height: 150px; object-fit: contain;" onerror="this.src='https://mc-heads.net/body/Steve/left'">
                <div class="mod-title" style="font-size: 0.85rem; font-family: 'Montserrat-Bold'; text-align: center; color: #fff; margin: 0;">Jugador: ${query}</div>
                <button class="btn btn-primary" style="width: 100%; font-size: 0.7rem; padding: 8px;" onclick="applyLibrarySkin('${skinUrl}')">
                    <i class="fa-solid fa-shirt"></i> APLICAR SKIN
                </button>
            </div>
        `;
    } catch (e) {
        resultsContainer.innerHTML = '<p style="text-align: center; color: #ff4757; padding: 40px;">No se pudo encontrar la skin del jugador.</p>';
    }
}

/**
 * Aplica una skin de la biblioteca, la sube a Supabase y limpia la skin local de las configuraciones
 */
async function applyLibrarySkin(url) {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    if (!activeAcc) return showNotification("Debes tener una cuenta activa", "error");
    
    const loading = document.getElementById('skin-loading-overlay');
    if (loading) loading.style.display = 'flex';

    try {
        showNotification("Sincronizando skin con tu cuenta...", "warning");
        
        const result = await ipcRenderer.invoke('upload-skin-from-url', { 
            url: url, 
            nickname: activeAcc.nickname 
        });

        if (result.success) {
            const settings = await ipcRenderer.invoke('get-settings');
            settings.appearance.skinPath = null; // Limpiar skin local para usar la de Supabase/Mojang
            await ipcRenderer.invoke('save-settings', settings);
            
            showNotification("¡Skin aplicada y guardada con éxito!", "success");
            updateUIFromAccount();
            await updateSkinUI();
        } else {
            throw new Error(result.error);
        }
    } catch (e) {
        console.error(e);
        showNotification("Error al procesar skin de biblioteca", "error");
    } finally {
        if (loading) loading.style.display = 'none';
    }
}

async function updateSkinUI() {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    const username = activeAcc ? activeAcc.nickname : "Steve";
    
    let skinUrl;
    if (activeAcc && activeAcc.type === 'Microsoft') {
        skinUrl = `https://mc-heads.net/skin/${activeAcc.nickname || 'Steve'}`;
    } else {
        skinUrl = `${SUPABASE_STORAGE_URL}/skins/${username}.png`;
    }
    
    await initSkinViewer(skinUrl);
    await initProfileSkinViewer(skinUrl);
    renderPresetSkins();
}

/**
 * Cambia entre pestañas dentro de la vista de Ajustes
 */
function switchSettingsTab(tabId, btn) {
    const tabs = document.querySelectorAll('.settings-tab-content');
    tabs.forEach(t => t.classList.remove('active'));
    
    const buttons = document.querySelectorAll('.settings-tab-btn');
    buttons.forEach(b => b.classList.remove('active'));
    
    document.getElementById(tabId).classList.add('active');
    btn.classList.add('active');
}

/**
 * Muestra el panel de detalles de un logro con animación
 */
async function showAchievementDetail(index, card) {
    const mapping = ['first_download', 'first_launch', 'melomano', 'rey_del_pop', 'socializer', 'stylist', 'configurator', 'mod_hunter', 'veteran', 'explorer', 'cleaner', 'server_adder', 'ram_master', 'old_school', 'bg_collector', 'fullscreen_king', 'modloader_expert', 'safety_first'];
    const id = mapping[index];
    
    const settings = await ipcRenderer.invoke('get-settings');
    const isUnlocked = settings.achievements && settings.achievements[id];
    
    const modal = document.getElementById('achievement-modal');
    const title = document.getElementById('detail-title');
    const desc = document.getElementById('detail-desc');
    const tag = document.getElementById('detail-tag');
    const iconBox = document.getElementById('detail-icon');
    const progressFill = document.getElementById('detail-progress-fill');

    // Extraer datos del card original
    const cardTitle = card.querySelector('h3').innerText;
    const cardDesc = card.querySelector('p').innerText;
    const cardIcon = card.querySelector('i').cloneNode(true);
    cardIcon.className = cardIcon.className.replace('fa-3x', 'fa-4x'); 

    title.innerText = cardTitle;
    desc.innerText = cardDesc;
    iconBox.innerHTML = '';
    iconBox.appendChild(cardIcon);
    
    tag.innerText = isUnlocked ? 'Completado' : 'Bloqueado';
    tag.className = `achievement-status ${isUnlocked ? 'completed' : 'locked'}`;
    progressFill.style.width = isUnlocked ? '100%' : '0%';

    modal.style.display = 'flex';
}

function closeAchievementDetail() {
    const modal = document.getElementById('achievement-modal');
    modal.classList.add('notification-fade-out');
    setTimeout(() => {
        modal.style.display = 'none';
        modal.classList.remove('notification-fade-out');
    }, 500);
}

/**
 * Función placeholder para cargar y mostrar los logros.
 */
async function loadAchievements() {
    const settings = await ipcRenderer.invoke('get-settings');
    const ach = settings.achievements || {};
    
    // Mapeo de IDs según el orden en micuenta.html
    const mapping = ['first_download', 'first_launch', 'melomano', 'rey_del_pop', 'socializer', 'stylist', 'configurator', 'mod_hunter', 'veteran', 'explorer', 'cleaner', 'server_adder', 'ram_master', 'old_school', 'bg_collector', 'fullscreen_king', 'modloader_expert', 'safety_first'];
    const cards = document.querySelectorAll('.launcher-feat');

    // Limpiamos y repoblamos el cache local de logros desbloqueados
    unlockedCache.clear();

    cards.forEach((card, index) => {
        const key = mapping[index];
        if (ach[key]) {
            unlockedCache.add(key);
            card.querySelector('.progress-bar-fill').style.width = '100%';
            
            // Actualizamos el color del icono si estaba en gris
            const icon = card.querySelector('i');
            if (icon && icon.style.color === 'rgb(85, 85, 85)') icon.style.color = ''; 

            const status = card.querySelector('.achievement-status');
            status.className = 'achievement-status completed';
            status.innerText = 'Completado';
        }
    });
}

/**
 * Desbloquea un logro y guarda el estado
 */
async function unlockAchievement(id, title) {
    // Si ya está desbloqueado en esta sesión o hay un proceso de desbloqueo en curso, ignoramos
    if (unlockedCache.has(id) || pendingUnlocks.has(id)) return;
    
    pendingUnlocks.add(id);
    console.log(`[Achievements] Intentando desbloquear: ${title}`);

    const settings = await ipcRenderer.invoke('get-settings');
    if (!settings || !settings.achievements) {
        console.error('[Achievements] Failed to retrieve settings or achievements object is missing.');
        pendingUnlocks.delete(id);
        return;
    }

    // Doble verificación: si el archivo de settings ya lo tiene, actualizamos cache y salimos
    if (settings.achievements[id]) {
        unlockedCache.add(id);
        pendingUnlocks.delete(id);
        return;
    }

    settings.achievements[id] = true;
    const saveSuccess = await ipcRenderer.invoke('save-settings', settings);
    
    if (saveSuccess) {
        unlockedCache.add(id);
        showNotification(`🏆 Logro Desbloqueado: ${title}`, "success");
        // Si la vista de logros está abierta, la refrescamos
        const logrosTab = document.getElementById('tab-logros');
        if (logrosTab && logrosTab.classList.contains('active')) {
            loadAchievements();
        }
    } else {
        showNotification(`Error al guardar el logro: ${title}`, "error");
    }

    pendingUnlocks.delete(id);
}

/**
 * Guarda el historial de avatares en la configuración local
 */
async function saveAvatarHistory() {
    const settings = await ipcRenderer.invoke('get-settings');
    settings.appearance.avatarHistory = userPfpHistory;
    await ipcRenderer.invoke('save-settings', settings);
}

/**
 * Permite cambiar la foto de perfil para cualquier tipo de cuenta (Microsoft u Offline).
 */
async function changeProfilePicture() {
    try {
        const filePath = await ipcRenderer.invoke('select-profile-picture');
        if (filePath) {
            // Actualizamos todas las instancias del avatar en la UI
            document.querySelectorAll('.profile-avatar-large, .user-avatar').forEach(img => img.src = filePath);
            
            // Persistir en el objeto de cuenta activa
            const activeAcc = userAccounts.find(acc => acc.active);
            if (activeAcc) activeAcc.avatar = filePath;
            saveSession(); // Guardar cambios usando la estructura consolidada en sesion.json

            // Añadir al historial si no existe
            if (!userPfpHistory.some(p => p.url === filePath)) {
                userPfpHistory.push({
                    id: Date.now(),
                    name: `Avatar ${userPfpHistory.length + 1}`,
                    url: filePath
                });
                await saveAvatarHistory();
                // Si estamos en la pestaña de personalización, refrescamos el grid
                if (document.getElementById('pfp-history-grid')) renderPfpHistory();
            }

            unlockAchievement('stylist', "¡Estilista!");
            showNotification("¡Foto de perfil actualizada!", "success");
        }
    } catch (error) {
        showNotification("No se pudo cargar la imagen", "error");
    }
}

/**
 * Renderiza el historial de fotos de perfil en la pestaña de Personalización
 */
function renderPfpHistory() {
    const grid = document.getElementById('pfp-history-grid');
    if (!grid) return;
    
    grid.innerHTML = '';
    userPfpHistory.forEach((pfp, index) => {
        const card = document.createElement('div');
        card.className = 'pfp-card';
        card.style.animationDelay = `${index * 0.1}s`;
        
        card.innerHTML = `
            <button class="pfp-action-btn edit" onclick="event.stopPropagation(); renamePfp(${pfp.id})" title="Editar nombre">
                <i class="fa-solid fa-pen"></i>
            </button>
            <button class="pfp-action-btn delete" onclick="event.stopPropagation(); deletePfp(${pfp.id})" title="Eliminar">
                <i class="fa-solid fa-trash"></i>
            </button>
            <img src="${pfp.url}" alt="${pfp.name}" onclick="applyHistoryPfp(${pfp.id})">
            <div class="pfp-name-tag">${pfp.name}</div>
        `;
        grid.appendChild(card);
    });
}

/**
 * Aplica un avatar del historial a la cuenta activa
 */
async function applyHistoryPfp(id) {
    const pfp = userPfpHistory.find(p => p.id === id);
    if (!pfp) return;

    const activeAcc = userAccounts.find(acc => acc.active);
    if (activeAcc) {
        activeAcc.avatar = pfp.url;
        updateUIFromAccount();
        await saveSession();
        showNotification(`Avatar "${pfp.name}" aplicado`, "success");
    }
}
window.applyHistoryPfp = applyHistoryPfp;

async function deletePfp(id) {
    userPfpHistory = userPfpHistory.filter(p => p.id !== id);
    await saveAvatarHistory();
    renderPfpHistory();
    unlockAchievement('cleaner', "Limpieza Profunda");
    showNotification("Avatar eliminado del historial", "success");
}

async function renamePfp(id) {
    const pfp = userPfpHistory.find(p => p.id === id);
    const newName = await showInputPrompt("Nuevo nombre para este avatar:", pfp.name);
    if (newName) {
        pfp.name = newName;
        await saveAvatarHistory();
        renderPfpHistory();
        showNotification("Nombre actualizado", "success");
    }
}

/**
 * Inicializa la vista de cuenta con el perfil activo
 */
function initAccountView() {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    if (!activeAcc) return;

    updateUIFromAccount();
    
    // Cargar detalles en la tabla de perfil (Nombre técnico vs UUID)
    const userEl = document.getElementById('detail-username');
    if (userEl) userEl.innerText = activeAcc.nickname || "Steve";

    const uuidEl = document.getElementById('detail-uuid');
    if (uuidEl) uuidEl.innerText = activeAcc.id || "0000-0000-0000";

    const joinedEl = document.getElementById('detail-joined');
    if (joinedEl) joinedEl.innerText = activeAcc.memberSince || "Desconocido";

    // Poblar inputs de edición (Nickname técnico vs Apodo visual)
    const userInp = document.getElementById('edit-username');
    const nickInp = document.getElementById('edit-nickname');
    if (userInp) userInp.value = activeAcc.nickname || "Steve";
    if (nickInp) nickInp.value = activeAcc.name || "Steve";

    updateSkinUI();

    // Soporte navegación por teclado (Tabs / Flechas) en menú lateral
    const tabBtns = Array.from(document.querySelectorAll('.sidebar-floating .side-tab-btn'));
    tabBtns.forEach((btn, idx) => {
        btn.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                const nextBtn = tabBtns[(idx + 1) % tabBtns.length];
                nextBtn.focus();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                const prevBtn = tabBtns[(idx - 1 + tabBtns.length) % tabBtns.length];
                prevBtn.focus();
            }
        });
    });
}

/**
 * Lógica de Cambio de Cuentas (YouTube TV Style)
 */
function openAccountSwitcher() {
    const modal = document.getElementById('account-switcher-modal');
    if (modal) {
        modal.style.display = 'flex';
        renderAccountSwitcherList();
    }
}

function closeAccountSwitcher() {
    const modal = document.getElementById('account-switcher-modal');
    if (modal) {
        modal.classList.add('notification-fade-out');
        setTimeout(() => {
            modal.style.display = 'none';
            modal.classList.remove('notification-fade-out');
        }, 500);
    }
}

function renderAccountSwitcherList() {
    const grid = document.getElementById('accounts-grid');
    if (!grid) return;
    
    grid.innerHTML = '';
    userAccounts.forEach(acc => {
        const card = document.createElement('div');
        card.className = `account-selector-card ${acc.active ? 'active' : ''}`;
        card.onclick = () => switchActiveAccount(acc.id);
        
        card.innerHTML = `
            <div class="avatar-wrapper">
                <img src="${acc.avatar}" alt="${acc.name}">
            </div>
            <span class="account-name">${acc.name}</span>
            <span style="font-size: 0.65rem; color: #666; font-family: 'Montserrat-Bold';">${acc.type.toUpperCase()}</span>
        `;
        grid.appendChild(card);
    });
}

function switchActiveAccount(id) {
    userAccounts.forEach(acc => acc.active = (acc.id === id));
    const activeAcc = userAccounts.find(acc => acc.active);
    saveSession(); // Guardar estructura con la cuenta seleccionada

    // Actualizar toda la interfaz
    updateUIFromAccount();
    
    // Actualizar detalles en la tabla de perfil si la vista está cargada
    const infoBoxes = document.querySelectorAll('.profile-info-grid .info-box .value');
    if (infoBoxes.length >= 3) {
        infoBoxes[0].innerText = activeAcc.nickname; // Índice 0: Nombre técnico
        infoBoxes[1].innerText = activeAcc.id;       // Índice 1: UUID
        infoBoxes[2].innerHTML = activeAcc.type === 'Microsoft' 
            ? '<i class="fa-brands fa-microsoft"></i> Microsoft (Premium)' 
            : '<i class="fa-solid fa-user-slash"></i> Modo Offline';
    }

    showNotification(`Sesión cambiada a: ${activeAcc.name}`, "success");
    closeAccountSwitcher();
}

/**
 * Guarda la sesión completa en el archivo sesion.json con la nueva estructura
 */
async function saveSession() {
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    const sessionData = {
        accounts: userAccounts,
        "select-account": activeAcc ? activeAcc.id : null
    };
    await ipcRenderer.invoke('save-accounts', sessionData);
}

/**
 * Genera un UUID determinístico basado en el nombre para modo offline
 */
function generateOfflineUUID(username) {
    let hash = 0;
    for (let i = 0; i < username.length; i++) {
        hash = ((hash << 5) - hash) + username.charCodeAt(i);
        hash |= 0;
    }
    const hex = Math.abs(hash).toString(16).padStart(8, '0');
    return `${hex}-0000-3000-a000-${hex.repeat(3).slice(0, 12)}`;
}

async function addNewAccount() {
    const name = await showInputPrompt("Nombre de usuario (Offline):");
    if (name && name.trim() !== "") {
        const technicalName = name.trim();
        const id = generateOfflineUUID(technicalName);
        const newAcc = { 
            id: id, 
            name: technicalName, 
            nickname: technicalName, 
            avatar: 'assets/images/default-avatar.png', 
            type: 'Offline', 
            active: false,
            memberSince: new Date().toLocaleDateString()
        };
        userAccounts.push(newAcc);
        await saveSession();
        switchActiveAccount(id);
    }
    closeAccountSwitcher();
}

async function setupVersionSelector() {
    const selector = document.getElementById('version-selector');
    if (!selector) return;

    const runBtn = document.querySelector('.btn-ejecutar');
    const list = selector.querySelector('.options-list');
    const text = document.getElementById('current-version');

    const settings = await ipcRenderer.invoke('get-settings');
    
    // Cargar estadísticas en el sidebar
    const stats = settings.stats || { launch_count: 0 };
    const achievements = settings.achievements || {};
    const unlockedCount = Object.values(achievements).filter(v => v === true).length;
    const totalAchievements = Object.keys(achievements).length || 18;

    const launchesEl = document.getElementById('sidebar-stat-launches');
    if (launchesEl) launchesEl.innerText = stats.launch_count;

    const achievementsEl = document.getElementById('sidebar-stat-achievements');
    if (achievementsEl) achievementsEl.innerText = `${unlockedCount}/${totalAchievements}`;

    // Cargar versiones reales desde el sistema (.glauncher/versions)
    const installed = await ipcRenderer.invoke('get-installed-versions');
    list.innerHTML = '';

    if (installed && installed.length > 0) {
        installed.forEach(v => {
            const opt = document.createElement('div');
            opt.className = 'option';
            opt.innerText = v;
            opt.onclick = () => {
                text.innerText = v;
                list.classList.remove('active');
                showNotification(`Versión seleccionada: ${v}`, "success");
            };
            list.appendChild(opt);
        });
        text.innerText = installed[0]; // Seleccionamos la primera por defecto
    } else {
        text.innerText = "No hay versiones";
    }

    if (runBtn) {
        runBtn.onclick = () => handleGameLaunch();
    }

    const selected = selector.querySelector('.selected-option');
    selected.onclick = (e) => {
        e.stopPropagation();
        list.classList.toggle('active');
    };

    window.onclick = () => list.classList.remove('active');

    // Navegación accesible del selector de versiones por teclado (Tab + Flechas)
    let focusedOptionIndex = -1;
    
    selector.addEventListener('keydown', (e) => {
        const options = Array.from(list.querySelectorAll('.option'));
        if (options.length === 0) return;

        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (list.classList.contains('active')) {
                if (focusedOptionIndex >= 0 && focusedOptionIndex < options.length) {
                    options[focusedOptionIndex].click();
                } else {
                    list.classList.remove('active');
                }
            } else {
                list.classList.add('active');
                focusedOptionIndex = 0;
                highlightOption(options, focusedOptionIndex);
            }
        } else if (e.key === 'ArrowDown') {
            if (list.classList.contains('active')) {
                e.preventDefault();
                focusedOptionIndex = (focusedOptionIndex + 1) % options.length;
                highlightOption(options, focusedOptionIndex);
            }
        } else if (e.key === 'ArrowUp') {
            if (list.classList.contains('active')) {
                e.preventDefault();
                focusedOptionIndex = (focusedOptionIndex - 1 + options.length) % options.length;
                highlightOption(options, focusedOptionIndex);
            }
        } else if (e.key === 'Escape') {
            list.classList.remove('active');
            selector.focus();
        }
    });

    function highlightOption(options, index) {
        options.forEach((opt, idx) => {
            if (idx === index) {
                opt.classList.add('focused');
                opt.scrollIntoView({ block: 'nearest' });
            } else {
                opt.classList.remove('focused');
            }
        });
    }
}

/**
 * Abre el explorador para buscar el ejecutable de Java
 */
async function browseJavaPath() {
    const path = await ipcRenderer.invoke('select-java-executable');
    if (path) {
        const input = document.getElementById('java-path');
        if (input) {
            input.value = path;
            saveLauncherConfig();
        }
    }
}

async function loadRandomBackground() {
    const settings = await ipcRenderer.invoke('get-settings');
    const container = document.getElementById('bg-container');
    if (!container) return;
    
    const blur = settings?.appearance?.bgBlur || 0;
    container.style.filter = `blur(${blur}px)`;

    const custom = settings?.appearance?.customBackgrounds || [];
    let source = "";

    // Si hay fondos personalizados, elegimos uno al azar, si no, usamos los de fábrica
    if (custom.length > 0) {
        source = custom[Math.floor(Math.random() * custom.length)];
        // Limpiar caracteres de control corruptos y normalizar barras de Windows para URLs
        source = source.replace(/[\x00-\x1F\x7F-\x9F]/g, "").replace(/\\/g, "/");
    } else {
        source = `assets/images/fondo-${Math.floor(Math.random() * 4) + 1}.jfif`;
    }

    const isVideo = /\.(mp4|webm)$/i.test(source);
    container.innerHTML = '';

    if (isVideo) {
        const video = document.createElement('video');
        // Asegurar que la ruta local sea un protocolo válido si no es URL remota
        video.src = source.startsWith('http') ? source : `file:///${source}`;
        video.autoplay = true;
        video.volume = settings?.appearance?.bgVideoVolume ?? 0.5;
        video.muted = video.volume === 0;
        video.loop = true;
        video.className = 'bg-media';
        container.appendChild(video);
    } else {
        const img = document.createElement('div');
        img.className = 'bg-media';
        const safeUrl = source.startsWith('http') ? source : `file:///${source}`;
        img.style.backgroundImage = `linear-gradient(rgba(0,0,0,0.6), rgba(0,0,0,0.6)), url('${safeUrl}')`;
        container.appendChild(img);
    }
}

/**
 * Guarda los cambios realizados en la pestaña de Personalización
 */
async function saveProfileChanges() {
    const newTechnical = document.getElementById('edit-username').value.trim();
    const newDisplayName = document.getElementById('edit-nickname').value.trim();
    
    if (!newTechnical) return showNotification("El nombre de usuario no puede estar vacío", "error");

    const activeAcc = userAccounts.find(acc => acc.active);
    if (activeAcc) {
        activeAcc.nickname = newTechnical;
        activeAcc.name = newDisplayName || newTechnical;
        activeAcc.id = generateOfflineUUID(newTechnical);
        
        updateUIFromAccount();

        const detailUser = document.getElementById('detail-username');
        if (detailUser) detailUser.innerText = activeAcc.nickname;
        const detailUuid = document.getElementById('detail-uuid');
        if (detailUuid) detailUuid.innerText = activeAcc.id;
        
        await saveSession();
        updateSkinUI(); 
        showNotification("¡Perfil actualizado con éxito!", "success");
    }
}

/**
 * Elimina la configuración y reinicia el launcher para volver al instalador
 */
async function uninstallLauncher() {
    const confirm = await showInputPrompt("¿Deseas desinstalar la configuración? Escribe 'BORRAR' para confirmar.");
    if (confirm === "BORRAR") {
        showNotification("Reseteando launcher...", "warning");
        await ipcRenderer.invoke('reset-launcher');
    } else if (confirm !== null) {
        showNotification("Acción cancelada", "error");
    }
}
window.uninstallLauncher = uninstallLauncher;

/**
 * Lanza una notificación visual y sonora
 * @param {string} message - Texto a mostrar
 * @param {string} type - 'success' o 'error'
 */
function showNotification(message, type = 'success') {
    const container = document.getElementById('notification-container');
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `<span>${message}</span>`;

    container.appendChild(notification);

    // Reproducir el sonido correspondiente
    const audioId = type === 'error' ? 'audio-error' : 'audio-success';
    const audio = document.getElementById(audioId);
    if (audio) { audio.currentTime = 0; audio.play(); }

    // Desvanecer y remover
    setTimeout(() => {
        notification.classList.add('notification-fade-out');
        setTimeout(() => notification.remove(), 500);
    }, 4500);
}

/**
 * Muestra un modal para solicitar una entrada de texto al usuario (Reemplazo de prompt)
 */
function showInputPrompt(title, defaultValue = "") {
    return new Promise((resolve) => {
        const modal = document.getElementById('generic-prompt-modal');
        const input = document.getElementById('prompt-input');
        const titleEl = document.getElementById('prompt-title');
        const okBtn = document.getElementById('prompt-ok');
        const cancelBtn = document.getElementById('prompt-cancel');
        
        titleEl.innerText = title;
        input.value = defaultValue;
        modal.style.display = 'flex';
        input.focus();

        const close = (val) => {
            modal.style.display = 'none';
            resolve(val);
        };

        okBtn.onclick = () => close(input.value.trim());
        cancelBtn.onclick = () => close(null);
        input.onkeypress = (e) => { if (e.key === 'Enter') close(input.value.trim()); };
    });
}

// Inicialización del Launcher
document.addEventListener('DOMContentLoaded', async () => {
    loadRandomBackground();

    // Aplicar ajustes de rendimiento al iniciar
    const settings = await ipcRenderer.invoke('get-settings');
    if (settings) applyPerformanceSettings(settings.appearance.animations);

    // Cargar cuentas guardadas desde sesion.json para persistencia real
    const accountData = await ipcRenderer.invoke('get-accounts');
    if (accountData && accountData.accounts) {
        userAccounts = accountData.accounts;
        const selectedId = accountData["select-account"];
        
        // Restaurar cuál era la cuenta activa
        userAccounts.forEach(acc => acc.active = (acc.id === selectedId));
        
        // Si hay cuentas pero ninguna marcada como activa, activamos la primera por defecto
        if (userAccounts.length > 0 && !userAccounts.some(acc => acc.active)) {
            userAccounts[0].active = true;
        }

        // Actualizar la interfaz global con los datos cargados
        const activeAcc = userAccounts.find(acc => acc.active);
        if (activeAcc) updateUIFromAccount();
    }

    // Cargar historial de avatares desde settings
    if (settings && settings.appearance && settings.appearance.avatarHistory) {
        userPfpHistory = settings.appearance.avatarHistory;
    }

    // --- Fix para pruebas en Navegador ---
    if (!window.electronAPI) {
        const oldWebview = document.getElementById('yt-webview');
        if (oldWebview) {
            const iframe = document.createElement('iframe');
            iframe.id = 'yt-webview';
            iframe.style.width = '100%';
            iframe.style.height = '100%';
            iframe.style.border = 'none';
            oldWebview.parentNode.replaceChild(iframe, oldWebview);
        }
    }
    
    // Cargar la navbar primero
    try {
        const navRes = await fetch('src/ui/navbar.html');
        const navHtml = await navRes.text();
        document.getElementById('navbar-container').innerHTML = navHtml;
    } catch (e) {
        console.error("No se pudo cargar la navbar", e);
    }

    // Configurar botones de control de ventana
    const minBtn = document.getElementById('win-minimize');
    const maxBtn = document.getElementById('win-maximize');
    const closeBtn = document.getElementById('win-close');
    
    if (minBtn) minBtn.onclick = async () => {
        const settings = await ipcRenderer.invoke('get-settings');
        if (settings.appearance.minimizeToTray) ipcRenderer.send('window-close'); else ipcRenderer.send('window-minimize');
    };
    if (maxBtn) maxBtn.onclick = () => ipcRenderer.send('window-maximize');
    if (closeBtn) closeBtn.onclick = () => ipcRenderer.send('window-close');

    // Cargar la vista de inicio por defecto
    loadView('inicio');
});

// Manejo de estado maximizado para la GUI
ipcRenderer.on('window-maximized', (isMaximized) => {
    const body = document.body;
    const maxIcon = document.getElementById('max-icon');
    
    if (isMaximized) {
        body.classList.add('maximized');
        if (maxIcon) maxIcon.className = 'fa-regular fa-clone'; // Icono de restaurar
    } else {
        body.classList.remove('maximized');
        if (maxIcon) maxIcon.className = 'fa-regular fa-square'; // Icono de maximizar
    }
});

// --- Lógica de Lanzamiento ---
function handleGameLaunch() {
    const text = document.getElementById('current-version').innerText;
    const activeAcc = userAccounts.find(acc => acc.active) || userAccounts[0];
    
    if (text === "No hay versiones") {
        return showNotification("Primero debes instalar una versión", "error");
    }
    if (!activeAcc) {
        return showNotification("Debes añadir una cuenta en 'Mi Cuenta' primero", "error");
    }

    const version = text.startsWith('Minecraft ') ? text.replace('Minecraft ', '') : text;
    
    // El "nickname" es el nombre técnico que Minecraft usa como --username
    const username = (activeAcc.nickname || activeAcc.name || "Steve").trim();
    const uuid = activeAcc.id; // UUID generado o real de Microsoft

    ipcRenderer.invoke('get-settings').then(settings => {
        if (settings.game.fullscreen) {
            unlockAchievement('fullscreen_king', "Inmersión Total");
        }
    });

    showNotification(`Iniciando Minecraft ${version}...`, "success");

    // Enviamos el objeto de cuenta activo completo para que main.js no tenga que adivinar
    ipcRenderer.send('launch-game', { version, username, uuid, accountType: activeAcc.type });
}

// Escuchar cuando el juego inicia con éxito
ipcRenderer.on('game-launched', () => {
    unlockAchievement('first_launch', "Primer Despegue");
});

ipcRenderer.on('download-status', (data) => {
    if (data.status === 'started') {
        showNotification(`Descargando archivos de Minecraft ${data.version}...`, "success");
    } else if (data.status === 'success') {
        setupVersionSelector(); // Refresh the version selector in the 'inicio' view
        unlockAchievement('first_download', "Iniciando el Viaje");
        showNotification(`¡Minecraft ${data.version} instalado correctamente!`, "success");
    } else if (data.status === 'error') {
        showNotification(`Error en descarga: ${data.message}`, "error");
    }

    // Si la descarga termina o falla, cerramos el panel después de un breve delay
    if (data.status === 'success' || data.status === 'error') {
        setTimeout(() => { 
            // Solo intentamos cerrar si el elemento existe en la vista actual
            if (document.getElementById('download-panel')) {
                closeDownloadPanel();
            }
        }, 1500);
    }
});

ipcRenderer.on('launch-progress', (progress) => {
    const progressPerc = Math.round((progress.task / progress.total) * 100);

    // Actualizar barra de progreso en el modal de instalación
    const modalBar = document.getElementById('download-progress-bar');
    const statusText = document.getElementById('download-status-text');
    if (modalBar) {
        modalBar.style.width = progressPerc + "%";
        if (statusText) statusText.innerText = `Descargando ${progress.type}: ${progressPerc}%`;
    }

    // La barra de progreso de YT ya no existe, se elimina esta referencia
    // const progressBar = document.getElementById('yt-progress'); 
    // if (progressBar && progress.type === 'download') {
    //     progressBar.style.width = progressPerc + "%";
    // }
});

// --- Lógica de Inicio de Sesión ---
function handleLogin(method) {
    showNotification(`Sesión iniciada como ${method}`, "success");
    // Al estar en 'Mi Cuenta', simplemente refrescamos o redirigimos
    loadView('inicio');
}

// --- Integración de GMusic (YouTube Webview) ---

/**
 * Inicializa la vista de GMusic cargando las playlists del usuario
 */
async function initGMusic() {
    const settings = await ipcRenderer.invoke('get-settings');
    renderPlaylists(settings.music?.playlists || []);
}

/**
 * Crea una nueva playlist preguntando el nombre al usuario
 */
async function createNewPlaylist() {
    const name = await showInputPrompt("Nombre de la Playlist:");
    if (!name) return;

    const settings = await ipcRenderer.invoke('get-settings');
    if (!settings.music) settings.music = { playlists: [] };
    
    const newPlaylist = {
        id: Date.now(),
        name: name,
        tracks: []
    };

    settings.music.playlists.push(newPlaylist);
    await ipcRenderer.invoke('save-settings', settings);
    renderPlaylists(settings.music.playlists);
    showNotification(`Playlist "${name}" creada`, "success");
}

function renderPlaylists(playlists) {
    const container = document.getElementById('playlists-container');
    if (!container) return;

    container.innerHTML = `
        <div class="playlist-item" onclick="createNewPlaylist()" tabindex="0" id="btn-new-playlist" style="position: relative;">
            <i class="fa-solid fa-plus fa-2x"></i>
            <div style="font-size: 0.8rem;">Nueva Lista</div>
        </div>
    `;

    const newPlayBtn = container.querySelector('#btn-new-playlist');
    if (newPlayBtn) {
        newPlayBtn.onkeydown = (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                createNewPlaylist();
            } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
                e.preventDefault();
                const next = newPlayBtn.nextElementSibling;
                if (next && next.classList.contains('playlist-item')) {
                    next.focus();
                }
            }
        };
    }

    playlists.forEach(pl => {
        const isSelected = pl.id === selectedPlaylistId;
        const el = document.createElement('div');
        el.className = `playlist-item ${isSelected ? 'active' : ''}`;
        el.setAttribute('tabindex', '0');
        el.style.position = 'relative';
        
        // Un solo click: Seleccionar para añadir y mostrar contenido
        el.onclick = () => {
            selectedPlaylistId = pl.id;
            document.body.classList.add('playlist-selected');
            renderPlaylists(playlists);
            showPlaylistTracks(pl.id); // Función para mostrar las canciones
            showNotification(`Playlist "${pl.name}" seleccionada`, "success");
        };

        // Doble click: Reproducir la playlist
        el.ondblclick = (e) => {
            e.stopPropagation();
            playPlaylist(pl.id);
        };

        // Soporte navegación por teclado (Enter, Space y Flechas)
        el.onkeydown = (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                el.click();
            } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
                e.preventDefault();
                const next = el.nextElementSibling;
                if (next && next.classList.contains('playlist-item')) {
                    next.focus();
                }
            } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
                e.preventDefault();
                const prev = el.previousElementSibling;
                if (prev && prev.classList.contains('playlist-item')) {
                    prev.focus();
                }
            } else if (e.key === 'Delete') {
                e.preventDefault();
                deletePlaylist(pl.id);
            }
        };

        el.innerHTML = `
            <button class="delete-playlist-btn" onclick="event.stopPropagation(); deletePlaylist(${pl.id})" style="position: absolute; top: 8px; right: 8px; background: transparent; border: none; color: #ff4757; cursor: pointer; display: flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; background-color: rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.05); font-size: 0.7rem;" title="Eliminar Playlist">
                <i class="fa-solid fa-xmark"></i>
            </button>
            <i class="fa-solid ${isSelected ? 'fa-circle-check' : 'fa-music'} fa-2x"></i>
            <div style="font-size: 0.8rem; font-family: 'Montserrat-Bold'; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; width: 90%; margin-top: 5px;">${pl.name}</div>
            <div style="font-size: 0.6rem; color: #888;">${pl.tracks.length} canciones</div>
            ${isSelected ? '<span style="font-size: 0.5rem; color: var(--primary); text-transform: uppercase; margin-top: 5px; display: block;">Seleccionada</span>' : ''}
        `;
        container.appendChild(el);
    });
}

/**
 * Elimina una playlist guardada
 */
async function deletePlaylist(playlistId) {
    const confirmDelete = confirm("¿Estás seguro de que deseas eliminar esta playlist?");
    if (!confirmDelete) return;

    const settings = await ipcRenderer.invoke('get-settings');
    if (settings.music && settings.music.playlists) {
        settings.music.playlists = settings.music.playlists.filter(pl => pl.id !== playlistId);
        await ipcRenderer.invoke('save-settings', settings);
        
        if (selectedPlaylistId === playlistId) {
            selectedPlaylistId = null;
            document.body.classList.remove('playlist-selected');
            const container = document.getElementById('yt-search-results');
            if (container) {
                container.innerHTML = '<p style="text-align: center; color: #555; margin-top: 20px; font-size: 0.85rem;">Los resultados de búsqueda aparecerán aquí.</p>';
            }
        }
        renderPlaylists(settings.music.playlists);
        showNotification("Playlist eliminada correctamente", "success");
    }
}
window.deletePlaylist = deletePlaylist;

/**
 * Añade una canción a la playlist seleccionada
 */
async function addTrackToPlaylist(videoId, title, thumbnail, author, timestamp) {
    if (!selectedPlaylistId) {
        return showNotification("Primero selecciona una playlist haciendo clic en ella", "warning");
    }

    const settings = await ipcRenderer.invoke('get-settings');
    const playlists = settings.music?.playlists || [];
    const playlist = playlists.find(pl => pl.id === selectedPlaylistId);

    if (playlist) {
        if (playlist.tracks.some(t => t.videoId === videoId)) {
            return showNotification("Esta canción ya está en la lista", "warning");
        }

        playlist.tracks.push({ videoId, title, thumbnail, author, timestamp });
        await ipcRenderer.invoke('save-settings', settings);
        renderPlaylists(playlists);
        showPlaylistTracks(selectedPlaylistId); // Refrescar inmediatamente el contenido de la playlist en pantalla
        showNotification(`Añadida a "${playlist.name}"`, "success");
    }
}
window.addTrackToPlaylist = addTrackToPlaylist;

/**
 * Muestra las canciones de una playlist en el contenedor de resultados de búsqueda
 */
async function showPlaylistTracks(playlistId) {
    const settings = await ipcRenderer.invoke('get-settings');
    const playlist = settings.music?.playlists?.find(pl => pl.id === playlistId);
    if (!playlist) return;

    const container = document.getElementById('yt-search-results');
    if (!container) return;

    container.innerHTML = `<h4 class="mc-font" style="margin-top: 20px; font-size: 0.8rem; color: var(--primary); text-transform: uppercase;">Contenido: ${playlist.name}</h4>`;

    if (playlist.tracks.length === 0) {
        container.innerHTML += '<p style="text-align: center; color: #555; margin-top: 20px; font-size: 0.85rem;">Esta playlist no tiene canciones aún.</p>';
        return;
    }

    playlist.tracks.forEach((track, index) => {
        const item = document.createElement('div');
        item.className = 'version-item card';
        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 15px; flex: 1; cursor: pointer;" onclick="playVideo('${track.videoId}')">
                <img src="${track.thumbnail}" style="width: 80px; border-radius: 5px;">
                <div>
                    <strong style="font-size: 0.9rem;">${track.title}</strong>
                    <div style="font-size: 0.7rem; color: #888;">${track.author} • ${track.timestamp}</div>
                </div>
            </div>
            <div style="display: flex; gap: 5px;">
                <button class="btn" style="padding: 5px 12px; font-size: 0.8rem;" onclick="moveTrackInPlaylist(${playlist.id}, '${track.videoId}', -1)" ${index === 0 ? 'disabled style="opacity:0.3"' : ''} title="Subir">
                    <i class="fa-solid fa-chevron-up"></i>
                </button>
                <button class="btn" style="padding: 5px 12px; font-size: 0.8rem;" onclick="moveTrackInPlaylist(${playlist.id}, '${track.videoId}', 1)" ${index === playlist.tracks.length - 1 ? 'disabled style="opacity:0.3"' : ''} title="Bajar">
                    <i class="fa-solid fa-chevron-down"></i>
                </button>
                <button class="btn" style="padding: 5px 12px; font-size: 0.8rem; color: #ff4757; border-color: rgba(255,71,87,0.2);" onclick="removeTrackFromPlaylist(${playlist.id}, '${track.videoId}')" title="Quitar de la lista">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `;
        container.appendChild(item);
    });
}

/**
 * Reproduce la primera canción de una playlist (Lógica de inicio de reproducción)
 */
async function playPlaylist(playlistId) {
    const settings = await ipcRenderer.invoke('get-settings');
    const playlist = settings.music?.playlists?.find(pl => pl.id === playlistId);
    
    if (!playlist || playlist.tracks.length === 0) {
        return showNotification("La playlist está vacía", "error");
    }

    showNotification(`Reproduciendo playlist: ${playlist.name}`, "success");
    playVideo(playlist.tracks[0].videoId);
}

/**
 * Cambia el orden de una canción en la playlist
 */
async function moveTrackInPlaylist(playlistId, videoId, direction) {
    const settings = await ipcRenderer.invoke('get-settings');
    const playlists = settings.music?.playlists || [];
    const playlist = playlists.find(pl => pl.id === playlistId);

    if (playlist) {
        const index = playlist.tracks.findIndex(t => t.videoId === videoId);
        if (index === -1) return;

        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= playlist.tracks.length) return;

        // Intercambiar posiciones en el array
        [playlist.tracks[index], playlist.tracks[newIndex]] = [playlist.tracks[newIndex], playlist.tracks[index]];

        await ipcRenderer.invoke('save-settings', settings);
        showPlaylistTracks(playlistId);
        showNotification("Orden de canciones actualizado", "success");
    }
}
window.moveTrackInPlaylist = moveTrackInPlaylist;

/**
 * Elimina una canción de una playlist guardada
 */
async function removeTrackFromPlaylist(playlistId, videoId) {
    const settings = await ipcRenderer.invoke('get-settings');
    const playlists = settings.music?.playlists || [];
    const playlist = playlists.find(pl => pl.id === playlistId);

    if (playlist) {
        playlist.tracks = playlist.tracks.filter(t => t.videoId !== videoId);
        await ipcRenderer.invoke('save-settings', settings);
        showPlaylistTracks(playlistId);
        renderPlaylists(playlists);
        showNotification("Canción eliminada de la playlist", "success");
    }
}
window.removeTrackFromPlaylist = removeTrackFromPlaylist;

let isMusicPlaying = false;

// Función para inyectar CSS y JS en el webview para limpiar la interfaz de YouTube y bloquear anuncios
function handleWebviewDomReady() {
    // Si no estamos en Electron, esta función no hace nada
    if (!window.electronAPI) return;

    const webview = document.getElementById('yt-webview');
    if (!webview) return;

    // Reducimos el zoom para que quepan mejor los controles en el espacio pequeño
    webview.setZoomFactor(0.5);

    const cssInject = `
        /* Ocultar TODO excepto el reproductor puro */
        #masthead-container, #secondary, #comments, #footer,
        ytd-video-primary-info-renderer, ytd-video-secondary-info-renderer,
        #merch-shelf, #ticket-shelf, #chat-container, .ytp-chrome-top,
        .ytp-pause-overlay, .ytp-ce-element, .ytp-ad-overlay-container,
        #columns > #secondary, #panels { display: none !important; }
        
        ytd-page-manager { margin-top: 0 !important; }
        #primary { padding: 0 !important; margin: 0 !important; width: 100% !important; }
        ytd-watch-flexy { padding: 0 !important; margin: 0 !important; }
        
        /* Asegurar que los controles de abajo no se oculten */
        .ytp-chrome-bottom { 
            opacity: 1 !important; 
            visibility: visible !important;
        }

        #player-container-outer, #player-container-inner { 
            max-width: 100% !important; 
            min-width: 100% !important; 
            margin: 0 !important; 
        }
        
        /* Forzar el video a ocupar el 100% del webview */
        #ytd-player { background: black !important; }
        .html5-video-container, video { 
            width: 100% !important; 
            height: 100% !important; 
            left: 0 !important; 
            top: 0 !important;
            object-fit: contain !important; 
        }

        .ytp-ad-player-overlay, .ytp-ad-text, .ytp-ad-image-overlay,
        ytd-promoted-sparkles-text-renderer, ytd-promoted-sparkles-web-renderer,
        ytd-display-ad-renderer, ytd-ad-slot-renderer,
        .ytp-ad-module, .ytp-ad-progress-list, .ytp-ad-overlay-slot,
        /* Ocultar elementos de la página de inicio/recomendaciones si se carga una URL de video directamente */
        ytd-browse, ytd-two-column-browse-results-renderer, ytd-rich-grid-renderer,
        ytd-feed-filter-chip-bar-renderer, #contents.ytd-rich-grid-renderer {
            display: none !important;
        }
        body { overflow: hidden !important; background: black !important; }
    `;
    webview.insertCSS(cssInject);
    
    // Inyectar JavaScript para intentar bloquear/saltar anuncios
    const jsInject = `
        function blockYouTubeAds() {
            // 1. Intentar hacer clic en el botón "Omitir anuncio"
            const skipButton = document.querySelector('.ytp-ad-skip-button');
            if (skipButton) {
                skipButton.click();
                console.log('YouTube Ad: Skipped ad via button click.');
            }

            // 2. Intentar adelantar los anuncios si es posible (no funciona para todos los tipos de anuncios)
            const videoPlayer = document.querySelector('video');
            if (videoPlayer && document.querySelector('.ytp-ad-player-overlay')) {
                // Verificar si el anuncio se está reproduciendo y tiene una duración
                if (!videoPlayer.paused && videoPlayer.duration && videoPlayer.currentTime < videoPlayer.duration) {
                    videoPlayer.currentTime = videoPlayer.duration; // Saltar al final del anuncio
                    console.log('YouTube Ad: Fast-forwarded through ad.');
                }
            }

            // 3. Ocultar elementos de anuncios comunes si aparecen
            const adElementsToHide = [
                '.ytp-ad-player-overlay', '.ytp-ad-text', '.ytp-ad-image-overlay',
                '.ytp-ad-module', '.ytp-ad-progress-list', '.ytp-ad-overlay-slot',
                'ytd-promoted-sparkles-text-renderer', 'ytd-promoted-sparkles-web-renderer',
                'ytd-display-ad-renderer', 'ytd-ad-slot-renderer',
                'ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"]'
            ];
            adElementsToHide.forEach(selector => {
                document.querySelectorAll(selector).forEach(ad => {
                    if (ad.style.display !== 'none') {
                        ad.style.display = 'none';
                        console.log('YouTube Ad: Hidden ad element via CSS.');
                    }
                });
            });

            // 4. Eliminar módulos de anuncios del DOM (más agresivo)
            const adModulesToRemove = [
                'ytd-promoted-sparkles-text-renderer', 'ytd-promoted-sparkles-web-renderer',
                'ytd-display-ad-renderer', 'ytd-ad-slot-renderer',
                'ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-ads"]'
            ];
            adModulesToRemove.forEach(selector => {
                document.querySelectorAll(selector).forEach(module => {
                    if (module.parentNode) {
                        module.parentNode.removeChild(module);
                        console.log('YouTube Ad: Removed ad module from DOM.');
                    }
                });
            });
        }

        // Ejecutar el bloqueo de anuncios periódicamente
        setInterval(blockYouTubeAds, 200); // Verificar con más frecuencia
        blockYouTubeAds(); // Ejecutar una vez inmediatamente
    `;
    webview.executeJavaScript(jsInject);

    // Intentar darle play automático si es posible
    webview.executeJavaScript(`document.querySelector('video').play();`);
}

let webviewDomReadyListenerAdded = false; // Flag para asegurar que el listener se añade una sola vez

function playVideo(videoId) {
    const webview = document.getElementById('yt-webview');
    if (!webview) return;
    
    // En navegador usamos la URL de embed, en Electron la de watch normal
    const isBrowser = !window.electronAPI;
    const videoUrl = isBrowser 
        ? `https://www.youtube.com/embed/${videoId}?autoplay=1`
        : `https://www.youtube.com/watch?v=${videoId}`;

    // Asegurarse de que el listener se añada solo una vez
    if (!isBrowser && !webviewDomReadyListenerAdded) {
        webview.addEventListener('dom-ready', handleWebviewDomReady);
        webviewDomReadyListenerAdded = true;
    }

    webview.src = videoUrl;

    isMusicPlaying = true;
    document.getElementById('global-player-container').style.display = 'block';

    // Aquí activamos el logro de música
    unlockAchievement('melomano', "¡Melómano!");

    ipcRenderer.invoke('get-video-info', videoUrl).then(info => {
        if (info) {
            document.getElementById('track-name').innerText = info.title;
            showNotification(`Reproduciendo: ${info.title}`, "success");
            // Sincronizar con la ventana externa si existe
            ipcRenderer.send('sync-mini-player-title', info.title);

            // Lógica de Logro: Rey del PoP
            if (info.title.toLowerCase().includes('michael jackson') || info.title.toLowerCase().includes('mj')) {
                unlockAchievement('rey_del_pop', "Rey del PoP");
            }
        }
    });
}

async function handleYTPlayer() {
    const input = document.getElementById('yt-query');
    const val = input.value.trim();
    if (!val) return showNotification("Ingresa un ID o Link de YouTube", "warning");

    // Detectar si es una URL o una búsqueda
    const isUrl = val.includes('youtube.com') || val.includes('youtu.be');
    
    if (isUrl) {
        let videoId = val;
        if (val.includes('v=')) videoId = val.split('v=')[1].split('&')[0];
        else if (val.includes('be/')) videoId = val.split('be/')[1].split('?')[0];
        playVideo(videoId);
    } else {
        // Es una búsqueda de texto
        const resultsContainer = document.getElementById('yt-search-results');
        resultsContainer.innerHTML = '<p style="text-align:center; color: #888;">Buscando en YouTube...</p>';
        resultsContainer.classList.add('active'); // Opcional si usas clases

        const videos = await ipcRenderer.invoke('search-youtube', val);
        renderSearchResults(videos);
    }
}

function renderSearchResults(videos) {
    const container = document.getElementById('yt-search-results');
    container.innerHTML = '';

    if (videos.length === 0) {
        container.innerHTML = '<p style="text-align:center; color: #ff4757;">No se encontraron videos.</p>';
        return;
    }

    videos.forEach(video => {
        const item = document.createElement('div');
        item.className = 'version-item card';
        item.innerHTML = `
            <div style="display: flex; align-items: center; gap: 15px; flex: 1; cursor: pointer;" onclick="playVideo('${video.videoId}')">
                <img src="${video.thumbnail}" style="width: 80px; border-radius: 5px;">
                <div>
                    <strong style="font-size: 0.9rem;">${video.title}</strong>
                    <div style="font-size: 0.7rem; color: #888;">${video.author.name} • ${video.timestamp}</div>
                </div>
            </div>
            <button class="btn btn-primary add-to-playlist-btn" style="padding: 5px 12px; font-size: 0.8rem;" onclick="addTrackToPlaylist('${video.videoId}', '${video.title.replace(/'/g, "\\'")}', '${video.thumbnail}', '${video.author.name.replace(/'/g, "\\'")}', '${video.timestamp}')" title="Añadir a playlist">
                <i class="fa-solid fa-plus"></i>
            </button>
        `;
        container.appendChild(item);
    });
}

/**
 * Abre o cierra la ventana flotante externa
 */
function toggleExternalMiniPlayer() {
    const title = document.getElementById('track-name').innerText;
    ipcRenderer.send('open-external-mini-player', title);
}

/**
 * Simulación de envío de mensaje para el logro Socializador
 */
async function sendMockChatMessage() {
    const now = Date.now();
    const input = document.getElementById('chat-input');
    const container = document.getElementById('chat-messages');
    if (!input || !input.value.trim() || now < chatMutedUntil) {
        if (now < chatMutedUntil) {
            showNotification(`Estás silenciado. Espera ${Math.ceil((chatMutedUntil - now) / 1000)}s`, "error");
        }
        return;
    }

    // Verificar Cooldown (Anti-Spam rápido)
    if (now - lastMessageTime < CHAT_COOLDOWN) {
        chatMutedUntil = now + 5000; // Silencio de 5 segundos
        return showNotification("¡No spamees! Silenciado por 5s.", "error");
    }

    // Verificar Duplicados
    if (input.value === lastMessageContent) {
        return showNotification("No puedes enviar el mismo mensaje dos veces.", "warning");
    }

    // 🔍 ANALISIS DE LINKS PROFUNDO
    const urlMatch = input.value.match(/(https?:\/\/[^\s]+)/gi);
    if (urlMatch) {
        for (const url of urlMatch) {
            const safety = await ipcRenderer.invoke('check-url-safety', url);
            unlockAchievement('safety_first', "Escudo de Datos");
            if (!safety.safe) {
                chatMutedUntil = now + BAN_DURATION_1_DAY;
                input.value = '';
                return showNotification(`¡SUSPENSIÓN! Link malicioso detectado: ${safety.reason}`, "error");
            }
        }
    }

    // 🛑 SEGURIDAD: Protección contra links de descarga maliciosos y malware (YAAI)
    if (isCriticalThreat(input.value)) {
        chatMutedUntil = now + BAN_DURATION_5_DAYS;
        input.value = '';
        return showNotification("¡AMENAZA DETECTADA! Has sido baneado del chat por 5 días por intentar enviar malware o links prohibidos.", "error");
    }

    // Aplicar Filtro Hardcore
    const filteredText = filterProfanity(input.value);

    // Sistema de Reincidencia (3 Strikes = 10 Minutos)
    if (filteredText !== input.value) {
        profanityStrikes++;
        if (profanityStrikes >= 3) {
            chatMutedUntil = now + BAN_DURATION_10_MINS;
            profanityStrikes = 0;
            input.value = '';
            return showNotification("Has sido silenciado por 10 minutos por insultos repetidos.", "error");
        }
        showNotification(`¡Advertencia ${profanityStrikes}/3! Cuida tu lenguaje.`, "warning");
    }

    // Bloqueo completo: si el mensaje filtrado es solo asteriscos y espacios, no se envía
    if (filteredText.replace(/[* ]/g, '') === '' && input.value.trim() !== '') {
        input.value = '';
        return showNotification("Mensaje bloqueado por toxicidad extrema.", "error");
    }

    // Crear elemento de mensaje de forma SEGURA (evita XSS)
    const msg = document.createElement('div');
    msg.style.marginBottom = "5px";
    const prefix = document.createElement('span');
    prefix.style.color = "#2ecc71";
    prefix.style.fontSize = "0.7rem";
    prefix.textContent = "[Tú] ";
    msg.appendChild(prefix);
    msg.appendChild(document.createTextNode(filteredText));
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
    
    lastMessageTime = now;
    lastMessageContent = input.value;
    input.value = '';
    unlockAchievement('socializer', "Socializador");
}

/**
 * Lógica de Tenor API para Emojis/GIFs
 */
function toggleTenorPicker() {
    const picker = document.getElementById('tenor-picker');
    picker.style.display = picker.style.display === 'none' ? 'block' : 'none';
}

async function searchTenor() {
    const query = document.getElementById('tenor-search-input').value || 'minecraft emoji';
    const resultsDiv = document.getElementById('tenor-results');
    resultsDiv.innerHTML = '<p style="grid-column: 1/-1; text-align:center; font-size:0.6rem;">Buscando...</p>';

    try {
        // Usamos la API key pública de Tenor para el ejemplo
        const apiKey = "LIVDSRZULELA"; 
        const response = await fetch(`https://api.tenor.com/v1/search?q=${encodeURIComponent(query)}&key=${apiKey}&limit=12`);
        const data = await response.json();
        
        resultsDiv.innerHTML = '';
        data.results.forEach(gif => {
            const gifUrl = gif.media[0].tinygif.url;
            const img = document.createElement('img');
            img.src = gifUrl;
            img.style.width = "100%";
            img.style.cursor = "pointer";
            img.onclick = () => sendGif(gifUrl);
            resultsDiv.appendChild(img);
        });
    } catch (e) {
        resultsDiv.innerHTML = '<p style="color:red; font-size:0.6rem;">Error cargando GIFs</p>';
    }
}

function sendGif(url) {
    const container = document.getElementById('chat-messages');
    const msg = document.createElement('div');
    msg.innerHTML = `<span style="color: #2ecc71; font-size: 0.7rem;">[Tú]</span> <img src="${url}" class="chat-msg-gif">`;
    container.appendChild(msg);
    container.scrollTop = container.scrollHeight;
    toggleTenorPicker();
    unlockAchievement('socializer', "Socializador");
}

// --- Gestión de Mods (Modrinth) ---

async function performModSearch() {
    const query = document.getElementById('search-input').value.trim();
    const projectType = document.getElementById('filter-type').value;
    const resultsContainer = document.getElementById('api-results');
    const resultsList = document.getElementById('results-list');

    if (!query) {
        showNotification('Por favor, escribe algo para buscar.', 'warning');
        return;
    }

    resultsList.innerHTML = '<p style="text-align: center; color: #888; padding: 20px;">🔍 Buscando en Modrinth...</p>';
    resultsContainer.style.display = 'block';

    try {
        // Estructuración de los facets requeridos por Modrinth v2 para filtrar por tipo de proyecto
        const facets = JSON.stringify([[`project_type:${projectType}`]]);
        const url = `https://api.modrinth.com/v2/search?query=${encodeURIComponent(query)}&facets=${encodeURIComponent(facets)}&limit=50`;

        // Modrinth exige un User-Agent único por buenas prácticas para evitar bloqueos
        const response = await fetch(url, {
            headers: {
                'User-Agent': 'GLauncher/1.0.0 (https://github.com/user/glauncher)'
            }
        });

        if (!response.ok) throw new Error('Error de conexión con Modrinth');

        const data = await response.json();
        allModResults = data.hits;

        unlockAchievement('mod_hunter', "Cazador de Mods");

        if (allModResults.length === 0) {
            resultsList.innerHTML = '<p style="text-align: center; color: #ff4757; padding: 20px;">No se encontraron resultados.</p>';
            const pag = document.getElementById('mod-pagination');
            if (pag) pag.innerHTML = '';
            return;
        }

        renderModPage(1);

    } catch (error) {
        console.error(error);
        showNotification('Error al conectar con la API de Modrinth', 'error');
        resultsList.innerHTML = '<p style="text-align: center; color: #ff4757; padding: 20px;">Ocurrió un error al realizar la búsqueda.</p>';
    }
}

function renderModPage(page) {
    const resultsList = document.getElementById('results-list');
    resultsList.innerHTML = '';
    
    const start = (page - 1) * modsPerPage;
    const end = start + modsPerPage;
    const pageItems = allModResults.slice(start, end);

    pageItems.forEach((project, index) => {
        const item = document.createElement('div');
        item.className = 'mod-card card';
        item.style.animationDelay = `${index * 0.05}s`;

        item.innerHTML = `
            <img src="${project.icon_url || 'assets/icons/favicon.png'}" alt="icon" onerror="this.src='assets/icons/favicon.png'">
            <strong class="mod-title" title="${project.title}">${project.title}</strong>
            <div class="mod-author">Por <span>${project.author}</span></div>
            <button onclick="window.open('https://modrinth.com/${project.project_type}/${project.slug}', '_blank')" 
                    class="btn btn-primary" style="width: 100%; padding: 8px 0; font-size: 0.7rem; margin-top: auto;">
                DETALLES
            </button>
        `;
        resultsList.appendChild(item);
    });

    renderModPagination(page);
}

function renderModPagination(currentPage) {
    const resultsContainer = document.getElementById('api-results');
    let paginationDiv = document.getElementById('mod-pagination');
    
    if (!paginationDiv) {
        paginationDiv = document.createElement('div');
        paginationDiv.id = 'mod-pagination';
        paginationDiv.className = 'pagination-container';
        resultsContainer.appendChild(paginationDiv);
    }
    
    paginationDiv.innerHTML = '';
    const totalPages = Math.ceil(allModResults.length / modsPerPage);

    if (totalPages <= 1) return;

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement('button');
        btn.innerText = i;
        btn.className = `pagination-btn ${i === currentPage ? 'active' : ''}`;
        btn.onclick = () => {
            renderModPage(i);
            document.querySelector('.content').scrollTo({ top: 0, behavior: 'smooth' });
        };
        paginationDiv.appendChild(btn);
    }
}
/**
 * Lógica para hacer funcionales los desplegables personalizados del modal de descarga
 */
function initModalDropdowns() {
    const setup = (containerId, onSelect) => {
        const container = document.getElementById(containerId);
        if (!container) return;
        const selected = container.querySelector('.selected-option');
        const list = container.querySelector('.options-list');

        selected.onclick = (e) => {
            e.stopPropagation();
            document.querySelectorAll('.options-list').forEach(l => l !== list && l.classList.remove('active'));
            list.classList.toggle('active');
        };

        list.onclick = (e) => {
            const opt = e.target.closest('.option');
            if (opt) {
                const val = opt.dataset.value;
                selected.querySelector('span').innerText = opt.innerText;
                selected.querySelector('span').dataset.value = val;
                list.classList.remove('active');
                if (onSelect) onSelect(val);
            }
        };
    };

    setup('modloader-select-custom', (val) => {
        const verSection = document.getElementById('modloader-version-section');
        const verList = document.getElementById('modloader-version-options-list');
        const verText = document.getElementById('modloader-version-selected-text').querySelector('span');

        verText.innerText = 'Selecciona una versión';
        verText.dataset.value = '';
        verList.innerHTML = '';
        verSection.style.display = (val === 'vanilla') ? 'none' : 'block';

        if (val !== 'vanilla') {
            (currentModloaderData[val] || []).forEach(v => {
                const li = document.createElement('li');
                li.className = 'option';
                li.dataset.value = v;
                // Aplicar nombre amigable según el tipo de modloader
                if (val === 'forge') {
                    const fmt = window._formatForgeVersion || ((x) => x);
                    li.innerText = fmt(v);
                } else if (val === 'fabric') {
                    li.innerText = `${v} (Fabric)`;
                } else if (val === 'neoforge') {
                    li.innerText = `${v} (NeoForge)`;
                } else {
                    li.innerText = v;
                }
                verList.appendChild(li);
            });
        }
    });

    setup('modloader-version-select-custom');
    window.addEventListener('click', () => document.querySelectorAll('.options-list').forEach(l => l.classList.remove('active')));
}



// --- Gestión de Versiones de Minecraft ---

// Simulación de una API de versiones de Minecraft
async function getMinecraftVersionsAPI() {
    try {
        const response = await fetch('https://launchermeta.mojang.com/mc/game/version_manifest.json');
        if (!response.ok) throw new Error("Servidor de Mojang no responde");
        return await response.json();
    } catch (e) {
        throw e;
    }
};


async function loadMinecraftVersions() {
    try {
        const versionsData = await getMinecraftVersionsAPI();
        allVersionsCache = versionsData.versions;
        
        filterAndRenderVersions();
        
        showNotification("Catálogo de Mojang sincronizado.", "success");
    } catch (error) {
        showNotification("Error de conexión con Mojang.", "error");
        console.error(error);
    }
}

function setVersionFilter(type, btn) {
    currentTypeFilter = type;
    
    // Actualizar estado visual de los botones de filtro
    const filterButtons = document.querySelectorAll('.filter-buttons .nav-btn');
    filterButtons.forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    filterAndRenderVersions();
}

function filterAndRenderVersions() {
    const container = document.getElementById('versions-list-dynamic');
    if (!container) return;

    const searchQuery = document.getElementById('version-search').value.toLowerCase();
    
    // Filtrar la lista
    const filtered = allVersionsCache.filter(version => {
        const matchesSearch = version.id.toLowerCase().includes(searchQuery);
        
        let matchesType = false;
        if (currentTypeFilter === 'all') matchesType = true;
        else if (currentTypeFilter === 'release') matchesType = version.type === 'release';
        else if (currentTypeFilter === 'snapshot') matchesType = version.type === 'snapshot';
        else if (currentTypeFilter === 'old') matchesType = version.type === 'old_beta' || version.type === 'old_alpha';
        
        return matchesSearch && matchesType;
    });

    // Renderizar
    container.innerHTML = '';
    if (filtered.length === 0) {
        container.innerHTML = '<p style="text-align: center; color: #888; padding: 20px;">No se encontraron versiones.</p>';
        return;
    }

    filtered.forEach(version => {
        const card = document.createElement('div');
        card.className = 'version-item card';
        // Eliminamos el style inline pesado para usar la clase del CSS
        card.innerHTML = `
            <div>
                <strong style="font-size: 1.1rem; color: #fff;">${version.id}</strong>
                <div style="color: #888; margin-top: 4px; font-size: 0.75rem; font-family: 'Montserrat-Bold';">${version.type.toUpperCase()} • ${version.releaseTime.split('T')[0]}</div>
            </div>
            <button class="btn btn-primary" onclick="openDownloadPanel('${version.id}', '${version.type}')" style="padding: 5px 15px; font-size: 0.8rem;">
                INSTALAR
            </button>
        `;
        container.appendChild(card);
    });
}

// --- Lógica del Panel de Descarga ---
let selectedVersionForDownload = null;

async function openDownloadPanel(versionId, type) {
    // Buscamos el objeto completo de metadatos en el cache
    selectedVersionForDownload = allVersionsCache.find(v => v.id === versionId);

    const modal = document.getElementById('download-panel');
    const modloaderSelectedText = document.getElementById('modloader-selected-text').querySelector('span');
    const modloaderOptionsList = document.getElementById('modloader-options-list');
    const modloaderVersionSelectedText = document.getElementById('modloader-version-selected-text').querySelector('span');
    const modloaderVersionOptionsList = document.getElementById('modloader-version-options-list');

    const modSection = document.getElementById('modloader-section');
    const versionSection = document.getElementById('modloader-version-section');
    const noModMsg = document.getElementById('no-modloader-msg');

    document.getElementById('modal-version-title').innerText = `Minecraft ${versionId}`;
    

    // Resetear UI completamente
    document.getElementById('download-progress-container').style.display = 'none';
    document.getElementById('modal-actions').style.display = 'flex';
    document.getElementById('download-progress-bar').style.width = '0%';

    // Resetear selección a Vanilla
    modloaderSelectedText.innerText = 'Vanilla (Original)';
    modloaderSelectedText.dataset.value = 'vanilla';
    // SIEMPRE limpiar la lista para evitar duplicados al reabrir el panel
    modloaderOptionsList.innerHTML = '<li class="option" data-value="vanilla">Vanilla (Original)</li>';
    modloaderVersionSelectedText.innerText = 'Selecciona una versión';
    modloaderVersionSelectedText.dataset.value = '';
    modloaderVersionOptionsList.innerHTML = '';
    modSection.style.opacity = '1';
    modSection.style.pointerEvents = 'auto';
    versionSection.style.display = 'none';
    versionSection.style.opacity = '1';
    versionSection.style.pointerEvents = 'auto';
    noModMsg.style.display = 'none';

    currentModloaderData = { fabric: [], forge: [], neoforge: [] };

    modal.style.display = 'flex';

    if (type !== 'release') {
        noModMsg.style.display = 'block';
        return;
    }

    // Carga asíncrona de modloaders
    const headers = { 'User-Agent': 'GLauncher/1.0.0' };

        // Evitar error 400: Fabric solo disponible para >= 1.14
        const versionParts = versionId.split('.');
        const canFabric = versionParts.length >= 2 && parseInt(versionParts[1]) >= 14;
    
    // Usamos Promise.allSettled para que si un servicio falla, los demás sigan cargando
    Promise.allSettled([
        canFabric ? fetch(`https://meta.fabricmc.net/v2/versions/loader/${versionId}`, { headers }).then(r => r.ok ? r.json() : []) : Promise.resolve([]),
        fetch(`https://meta.fabricmc.net/v2/versions/loader`, { headers }).then(r => r.ok ? r.json() : []),
        fetch(`https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json`, { headers }).then(r => r.ok ? r.json() : {}),
        fetch(`https://bmclapi2.bangbang93.com/neoforge/list/${versionId}`, { headers }).then(r => r.ok ? r.json() : [])
    ]).then(results => {
        let found = false;

        // Helper: convierte "forge-1.8.9-11.15.1.2318-1.8.9" → "11.15.1.2318 (Forge)"
        // o "1.8.9-11.15.1.2318" → "11.15.1.2318 (Forge)"
        function formatForgeVersion(rawVersion) {
            if (!rawVersion) return rawVersion;
            // Formato: forge-MC-BUILD o forge-MC-BUILD-MC o MC-BUILD
            const parts = rawVersion.replace(/^forge-/, '').split('-');
            // Intentar encontrar la parte del build (normalmente X.Y.Z.W)
            const buildPart = parts.find(p => /^\d+\.\d+\.\d+(\.\d+)?$/.test(p) && p.split('.').length >= 3);
            if (buildPart) return `${buildPart} (Forge)`;
            // Fallback: quitar prefijo forge- y mc version
            return `${parts[parts.length - 1]} (Forge)`;
        }

        // Procesar Fabric — usar la API de versiones de loader globales y filtrar estables
        const fabricLoaders = results[1].status === 'fulfilled' ? results[1].value : [];
        const fabricForVersion = results[0].status === 'fulfilled' ? results[0].value : [];
        const fabricVersions = fabricForVersion.length > 0
            ? fabricForVersion.map(v => v.loader.version)
            : fabricLoaders.filter(l => l.stable).map(l => l.version);

        if (fabricVersions.length > 0) {
            currentModloaderData.fabric = fabricVersions;
            const opt = document.createElement('li');
            opt.className = 'option';
            opt.dataset.value = 'fabric';
            opt.innerText = 'Fabric (Optimizado)';
            modloaderOptionsList.appendChild(opt);
            found = true;
        }

        // Procesar Forge — usar maven-metadata.json de Forge oficial
        let forgeMeta = results[2].status === 'fulfilled' ? results[2].value : {};
        let forgeVersionsRaw = [];
        if (forgeMeta && typeof forgeMeta === 'object' && forgeMeta[versionId]) {
            forgeVersionsRaw = forgeMeta[versionId];
        }

        // Fallback: intentar BMCLAPI si maven-metadata falló o no tiene la versión
        const bmclForgePromise = (forgeVersionsRaw.length === 0)
            ? fetch(`https://bmclapi2.bangbang93.com/forge/minecraft/${versionId}`, { headers }).then(r => r.ok ? r.json() : []).catch(() => [])
            : Promise.resolve(null);

        bmclForgePromise.then(bmclData => {
            if (bmclData !== null && bmclData.length > 0) {
                forgeVersionsRaw = bmclData.sort((a,b) => b.build - a.build).map(f => f.version);
            }

            if (forgeVersionsRaw.length > 0) {
                currentModloaderData.forge = forgeVersionsRaw;
                const opt = document.createElement('li');
                opt.className = 'option';
                opt.dataset.value = 'forge';
                opt.innerText = 'Forge (Clásico)';
                modloaderOptionsList.appendChild(opt);
                found = true;
            }

            // Procesar NeoForge
            if (results[3].status === 'fulfilled' && results[3].value.length > 0) {
                currentModloaderData.neoforge = results[3].value;
                const opt = document.createElement('li');
                opt.className = 'option';
                opt.dataset.value = 'neoforge';
                opt.innerText = 'NeoForge (Moderno)';
                modloaderOptionsList.appendChild(opt);
                found = true;
            }

            if (!found) {
                noModMsg.style.display = 'block';
                noModMsg.innerText = "No se encontraron motores compatibles. Solo Vanilla disponible.";
            }

            // Guardar helper para uso en el dropdown de versiones
            window._formatForgeVersion = formatForgeVersion;
        });
    });
}

/**
 * Lógica de Gestión de Servidores
 */
async function loadServers() {
    const container = document.getElementById('servers-display-list');
    if (!container) return;

    const settings = await ipcRenderer.invoke('get-settings');
    const servers = settings.servers || [];

    if (servers.length === 0) {
        container.innerHTML = `
            <div class="card" style="grid-column: 1/-1; text-align: center; padding: 40px; color: #555;">
                <i class="fa-solid fa-ghost fa-3x" style="margin-bottom: 10px; opacity: 0.3;"></i>
                <p>Aún no has agregado ningún servidor.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = '';
    servers.forEach((srv, index) => {
        const card = document.createElement('div');
        card.className = 'mod-card card server-card';
        card.innerHTML = `
            <div class="server-status-dot offline" id="status-dot-${index}"></div>
            <strong class="mod-title">${srv.name}</strong>
            <div class="mod-author">${srv.ip}</div>
            <div id="server-players-${index}" style="font-size: 0.7rem; color: #888; margin-bottom: 15px;">Pingueando...</div>
            <div style="display: flex; gap: 8px; width: 100%;">
                <button class="btn btn-primary" style="flex: 1; padding: 8px; font-size: 0.7rem;" onclick="joinServer('${srv.ip}')">ENTRAR</button>
                <button class="btn" style="padding: 8px; font-size: 0.7rem; color: #ff4757; border-color: rgba(255,71,87,0.3);" onclick="deleteServer(${index})">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `;
        container.appendChild(card);
        updateSingleServerStatus(srv.ip, index);
    });
}

async function addCustomServer() {
    const nameInput = document.getElementById('server-name-input');
    const ipInput = document.getElementById('server-ip-input');
    
    if (!nameInput.value || !ipInput.value) {
        return showNotification("Rellena ambos campos", "error");
    }

    const settings = await ipcRenderer.invoke('get-settings');
    if (!settings.servers) settings.servers = [];
    
    settings.servers.push({ name: nameInput.value, ip: ipInput.value });
    await ipcRenderer.invoke('save-settings', settings);
    
    nameInput.value = '';
    ipInput.value = '';
    loadServers();
    
    // Lógica de Logro: Arquitecto de Redes
    unlockAchievement('server_adder', "Arquitecto de Redes");
    showNotification("Servidor guardado en la lista", "success");
}

/**
 * Inicializa la creación de un servidor local
 */
async function initLocalServerCreation() {
    const ram = document.getElementById('local-server-ram').value;
    const port = document.getElementById('local-server-port').value;
    const rconPass = document.getElementById('local-server-rcon').value;

    if(!rconPass) return showNotification("Debes definir una contraseña RCON", "warning");

    showNotification("Iniciando creación de servidor local...", "success");
    
    // Aquí usarías minecraft-server-util en el MAIN para conectar por RCON una vez el proceso inicie
    console.log(`Configurando servidor: RAM ${ram}GB, Port ${port}, RCON habilitado.`);
    
    // Simulación de éxito
    setTimeout(() => showNotification("Servidor local listo y ejecutándose", "success"), 2000);
}

async function deleteServer(index) {
    const settings = await ipcRenderer.invoke('get-settings');
    settings.servers.splice(index, 1);
    await ipcRenderer.invoke('save-settings', settings);
    loadServers();
    showNotification("Servidor eliminado", "success");
}

async function updateSingleServerStatus(ip, index) {
    const dot = document.getElementById(`status-dot-${index}`);
    const text = document.getElementById(`server-players-${index}`);
    
    if (!dot || !text) return;

    const status = await ipcRenderer.invoke('get-server-status', ip);
    if (status && status.players) {
        dot.className = 'server-status-dot online';
        text.innerText = `Online: ${status.players.online}/${status.players.max}`;
        text.style.color = "var(--minecraft-green)";
    } else {
        dot.className = 'server-status-dot offline';
        text.innerText = "Offline / Error";
        text.style.color = "#ff4757";
    }
}

async function joinServer(ip) {
    const settings = await ipcRenderer.invoke('get-settings');
    settings.game.autoJoin = ip;
    await ipcRenderer.invoke('save-settings', settings);
    handleGameLaunch();
}

function switchServerTab(tabId, btn) {
    const tabs = document.querySelectorAll('#tab-search-servers, #tab-create-server, #tab-my-servers, #tab-server-settings');
    tabs.forEach(t => t.classList.remove('active'));
    
    const buttons = btn.parentElement.querySelectorAll('.side-tab-btn');
    buttons.forEach(b => b.classList.remove('active'));
    
    document.getElementById(tabId).classList.add('active');
    btn.classList.add('active');
    
    if (tabId === 'tab-my-servers') loadServers();
    showNotification(`Sección: ${btn.getAttribute('data-label')}`, "success");
}
function closeDownloadPanel() {
    const panel = document.getElementById('download-panel');
    if (panel) panel.style.display = 'none';
}

function confirmDownload() {
    const type = document.getElementById('modloader-selected-text').querySelector('span').dataset.value;
    const ver = document.getElementById('modloader-version-selected-text').querySelector('span').dataset.value;

    const progressContainer = document.getElementById('download-progress-container');
    const modalActions = document.getElementById('modal-actions');
    const modSection = document.getElementById('modloader-section');
    const versionSection = document.getElementById('modloader-version-section');

    // Cambiar UI a modo descarga: mostrar barra y ocultar botones
    progressContainer.style.display = 'block';
    modalActions.style.display = 'none';
    modSection.style.opacity = '0.5';
    modSection.style.pointerEvents = 'none';
    versionSection.style.opacity = '0.5';
    versionSection.style.pointerEvents = 'none';

    if (type !== 'vanilla') {
        unlockAchievement('modloader_expert', "Ingeniero de Software");
    }
    
    ipcRenderer.send('download-version', {
        version: selectedVersionForDownload,
        modloaderType: type,
        modloaderVersion: ver || null
    });

    // Lógica de Logro: Viajero del Tiempo
    if (selectedVersionForDownload.type.includes('old')) {
        unlockAchievement('old_school', "Viajero del Tiempo");
    }
}

/**
 * Ejecuta el Optimizador Global (Booster de Red y Sistema)
 * Requiere permisos de administrador en Windows.
 */
async function runGlobalOptimization() {
    showNotification("Ejecutando Game Booster. Por favor, acepta los permisos de administrador.", "warning");
    
    try {
        const result = await ipcRenderer.invoke('run-system-optimization');
        if (result === true) {
            showNotification("¡Sistema Optimizado! Reinicia tu PC para maximizar los FPS.", "success");
        }
    } catch (error) {
        showNotification(`Error: ${error.message || 'El usuario rechazó los permisos'}`, "error");
    }
}

// Escuchar cuando el juego inicia con éxito para desbloquear logros
ipcRenderer.on('game-launched', () => {
    unlockAchievement('first_launch', "Primer Despegue");
});
