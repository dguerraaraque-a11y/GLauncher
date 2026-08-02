const BANNED_WORDS = [
    // --- CATEGORÍA: INSULTOS Y TOXICIDAD GLOBAL ---
    'mierda', 'puto', 'puta', 'pendejo', 'estupido', 'idiota', 'imbecil', 'zorra', 'cabron', 'malparido', 'culiao',
    'maricon', 'hdp', 'lpm', 'tmr', 'ctm', 'vptlv', 'alv', 'fck', 'perra', 'bastardo', 'basura', 'escoria',
    'careverga', 'maldito', 'gonorrea', 'pirobo', 'chingada', 'boludo', 'pelotudo', 'forro', 'concha',
    'gilipollas', 'pajero', 'pajera', 'pajerin', 'orto', 'ojete', 'pichula', 'pito', 'verga', 'pene', 'vagina',
    'clitoris', 'escroto', 'testiculo', 'bolas', 'huevos', 'tetas', 'chichis', 'nalgas', 'culo', 'asqueroso',
    'perro', 'perraza', 'prostituta', 'ramera', 'lacra', 'esperpento', 'malnacido', 'mamahuevo', 'singao',
    'marico', 'marica', 'mka', 'mk', 'guevon', 'huevon', 'wbon', 'gabon', 'vayon', 'vlc', 'cmr', 'cdtm', 'lrpm', 'lrpqlp', 'chpt', 'ptm', 'aspt', 'mrd', 'pndj', 'vlv', 'qlo', 'wn', 'vdg', 'pqc', 'mamaguevo', 'triplehijoeputa', 'malditasea', 'coño', 'coñazo', 'coñisimo', 'gueveta', 'mamon', 'mamona', 'weona', 'weoncito', 'maraco', 'maraca', 'conchesumadre', 'ctmre', 'chuchatumadre', 'carepicha', 'playo', 'carepinga', 'caremonda', 'monda', 'malditazo', 'puton', 'putanga', 'putiferio', 'puticlub', 'putarraco', 'meretriz', 'scort', 'mugre', 'piltrafa', 'pendejito', 'pendejita', 'estupidazo', 'estupidaza', 'tarado', 'tarada', 'taradito', 'taradita', 'mongol', 'mongolico', 'mongolica', 'subnormal', 'enfermo', 'enferma', 'virgo', 'virgucho', 'pajolin', 'pajuela', 'manuela', 'baboso', 'babosa', 'papanatas', 'pazguato', 'sorete', 'cacatua', 'mierdecilla', 'cornudo', 'cornuda', 'venado', 'cabro', 'cabra', 'rosquete', 'chivo', 'chiva', 'cacanero', 'culopollo', 'careculo', 'carepan', 'caralavada', 'carenalga', 'huevada', 'huevadas', 'chingaquedito', 'desmadre', 'pinche', 'pinches', 'chingadazo', 'chingadera', 'chingon', 'mamilas', 'mamarracho', 'mamarracha', 'adfeacio', 'engendro', 'aborto', 'hijodeperra', 'hijadeputa', 'hijodeputa', 'hijueputa', 'juemadre', 'malditos', 'malditas', 'malparidos', 'malparidas', 'caraepicha', 'mamapicha', 'singada', 'mamaverga', 'mamala', 'mamalo', 'tragatodo', 'tragapijas', 'soplanucas', 'mordealmohadas', 'mordedor', 'gorrion', 'arrastrado', 'arrastrada', 'muertodehambre', 'muertadehambre', 'limosnero', 'limosnera', 'basurero', 'rata', 'raton', 'pestilencia', 'mierdero', 'puterio', 'caca', 'pipi', 'popo', 'tetazas', 'culazo', 'conchudo', 'conchuda', 'choto', 'chota', 'poronga', 'pifia', 'pifias', 'cagon', 'cagona', 'pendejada', 'pendejadas', 'mierdotas', 'mierdazas', 'medioculos', 'mediopolvos', 'chingaderas', 'chingoneria', 'lambebotas', 'arribismo', 'marranos', 'marranas', 'cerdos', 'cerdas', 'asnos', 'animales', 'bestias', 'cacorros', 'pichurrias', 'bobolongos', 'mamertos', 'desgraciados', 'desgraciadas', 'cabrones', 'cabronas', 'culiaos', 'culiadas', 'culiada', 'maricones', 'mariconas', 'mariconerias', 'mariconeria', 'perras', 'perrazas', 'prostitutas', 'rameras', 'meretrices', 'scorts', 'esperpentos', 'mamahuevos', 'singaesposas', 'singamadres', 'coñazos', 'coñisimos', 'guevetas', 'mamones', 'mamonas', 'weonas', 'weoncitos', 'maracos', 'maracas', 'conchesumadres', 'ctmres', 'chuchatumadres', 'carepichas', 'playos', 'carepingas', 'caremondas', 'mondas', 'malditazos', 'putones', 'putangas', 'putiferios', 'puticlubs', 'putarracos',

    // --- CATEGORÍA: ENGLISH SLURS & TOXICITY ---
    'motherfucker', 'mf', 'motherfucking', 'motherfuckerz', 'asshole', 'assholes', 'bitch', 'bitches', 'bitchy', 'bastard', 'bastards', 'cunt', 'cunts', 'dickhead', 'dickheads', 'wanker', 'wankers', 'twat', 'twats', 'faggot', 'faggots', 'fag', 'fags', 'retard', 'retards', 'tard', 'retarded', 'idiot', 'idiots', 'dumbass', 'dumbasses', 'moron', 'morons', 'loser', 'losers', 'scum', 'scumbag', 'scumbags', 'trash', 'garbage', 'pieceofshit', 'pos', 'bullshit', 'bs', 'dipshit', 'dipshits', 'jackass', 'jackasses', 'prick', 'pricks', 'nigger', 'nigga', 'niggaz', 'slut', 'sluts', 'whore', 'whores', 'skank', 'skanks', 'hoe', 'hoes', 'sucker', 'suckers', 'freak', 'freaks', 'nerd', 'nerds', 'noob', 'noobs', 'n00b', 'n00bs', 'scrub', 'scrubs', 'clown', 'clowns', 'suckmydick', 'suckmydck', 'suckit', 'eatshit', 'killYourself', 'kys', 'gofuckyourself', 'gfy', 'fuckyou', 'fuyu', 'fcku', 'fckyou', 'fuckoff', 'fkoff', 'stfu', 'gtfo', 'stfubitch', 'biatch', 'sonofabitch', 'soab',

    // --- CATEGORÍA: NSFW / EMOJIS / SEX-CHAT ---
    '🖕', '💩', '🍆', '🍑', '🍌', '🤡', '👉👌', '🍆💦', '🍑💦', '🍌💦', '🍼💦', '👅💦', '👅🍑', '🖕🏻', '🖕🏼', '🖕🏽', '🖕🏾', '🖕🏿', '🐖', '🐷', '🐕', '🐩', '🐀', '🐭', '🖕💩', '🖕🤡', '🖕🤬', '👉🏻👌🏻', 'hotchat', 'pack', 'cybersex', 'coger', 'garchar', 'culiar', 'pichar', 'singar', 'cum', 'gemir', 'erp', 'sexting', 'hentai', 'yaoi', 'yuri', 'packs', 'pax', 'pasapack', 'pasarpack', 'rolplaysex', 'roles', 'rolex', 'rolero', 'rolera', 'cybersexo', 'cibersexo', 'tirar', 'chichar', 'repasar', 'enterrar', 'coito', 'chupada', 'gargantaprofunda', 'cumshot', 'squirt', 'orgasmo', 'leche', 'pajearme', 'pajearte', 'dedazo', 'dedito', 'masturbarme', 'masturbacion', 'ganas', 'gemidos', 'gemi', 'hacerlo', 'hacerelamor', 'hacerlohot', 'tocame', 'tocate', 'manosear', 'chuparmela', 'chupartela', 'lamer', 'lameme', 'mamamela', 'metemela', 'metertela', 'ponmela', 'ponertela', 'sacatela', 'abrirse', 'abrete', 'encuerar', 'desnudate', 'desnudarse', 'pontehot', 'pontearrecho', 'pontearrecha', 'pontebella', 'pontebellaco', 'quieroverte', 'enseñame', 'muestrame', 'fotosintimas', 'fotoshot', 'videoshot', 'vendede', 'vendopack', 'compropack', 'totona', 'chocha', 'bicho', 'manguaco', 'riata', 'reata', 'corneta', 'macana', 'papaya', 'almeja', 'pepa', 'papo', 'ñoqui', 'guebo', 'webo', 'wevito', 'paloma', 'manubrio', 'mazorca', 'plano', 'peras', 'melones', 'limones', 'pezones', 'tetitas', 'chichotas', 'nalgon', 'nalgona', 'daddy', 'mommy', 'sugar', 'bitch', 'slut', 'whore', 'ass', 'dick', 'cock', 'fap', 'fapear', 'fapeame', 'gimiendo', 'gemiditos', 'pasame pack', 'pasa pack', 'busca novio', 'busca novia',

    // --- CATEGORÍA: ACCIONES DE ROL SIMULADAS ---
    '*gime*', '*ah*', '*oh*', '*desviste*', '*toca*', '*besa*', '*mete*', '*saca*', '*gimiendo*', '*ahhh*', '*ohhh*', '*loesconde*', '*lointroduce*', '*desnuda*', '*empuja*', '*azota*', '*azotando*', '*azote*', '*nalguea*', '*yamete*', '*kudasai*', '*duro*', '*rapido*', '*suave*', '*muerde*', '*araña*', '*loagarra*', '*loaprieta*', '*lo muerde*', '*lo besa*', '*lo lame*', '*lo toca*', '*se la mete*', '*se lo mete*', '*lo saca*', '*se encuera*', '*se desnuda*', '*lo introduce*', '*empuja duro*', '*lo muerde suave*', '*lo azota duro*',
];

const LEET_MAP = {
    'a': '[aA4@ÀÁÂÃÄÅàáâãäåæÆ^ªαΔΛ]',
    'b': '[bB8ß8ßвЪ♭]',
    'c': '[cCç(<{\\[©¢kKκ]',
    'd': '(?:[dDÐð]|cl|ԁ)',
    'e': '[eE3ÈÉÊËèéêë€£Σεё]',
    'f': '[fFƒ]',
    'g': '[gG96ɢ]',
    'h': '(?:[hH#]|\\|-\\||н)',
    'i': '[iI1!|ÌÍÎÏìíîï¡ı]',
    'j': '[jJ]',
    'k': '[kK|к]',
    'l': '[lL1!|iIℓr]',
    'm': '(?:[mM]|nn|rn|м)',
    'n': '[nNñÑńŃии]',
    'o': '[oO0ÒÓÔÕÖøòóôõöØœŒ*°өΩ]',
    'p': '[pP¶рр]',
    'q': '[qQ9]',
    'r': '[rR®яlL]',
    's': '[sS5$§zZ2]',
    't': '[tT7+†т]',
    'u': '[uUvVÙÚÛÜùúûüµ]',
    'v': '(?:[vVuU]|\\\\/)',
    'w': '(?:[wW]|vv|vvv|\\\\/\\\\/)',
    'x': '(?:[xX×]|><|х]',
    'y': '[yY¥λɥ]',
    'z': '[zZ2sS5$]'
};

// 🔥 BÓVEDA DE ALTA VELOCIDAD (Cache global para no compilar RegEx en cada mensaje)
const REGEX_CACHE = new Map();

/**
 * Detecta si el mensaje contiene links maliciosos o intentos de descarga de malware.
 * @param {string} text 
 * @returns {boolean} True si se detecta una amenaza que amerite ban.
 */
function isCriticalThreat(text) {
    if (!text) return false;
    // Dominios conocidos de malware, screamers y keywords de estafas
    const blacklistedDomains = /youareanidiot\.(org|cc|com|net|ru|zip)|yaai\.org|free-minecoins|minecraft-hacks/i;
    
    if (/free no virus|sin virus gratis|download cheat/i.test(text)) return true;

    // Extensiones de archivos ejecutables altamente peligrosos
    const dangerousExtensions = /\.(exe|scr|bat|cmd|vbs|msi|ps1|com|pif)$/i;
    
    // Regex para encontrar URLs o posibles dominios
    const urlPattern = /(?:https?:\/\/|www\.)?([a-z0-9-]+\.[a-z]{2,}(?:\/[^\s]*)?)/gi;

    let match;
    while ((match = urlPattern.exec(text)) !== null) {
        const fullUrl = match[0];
        const pathOrUrl = match[1];
        
        if (blacklistedDomains.test(fullUrl) || dangerousExtensions.test(pathOrUrl)) {
            return true;
        }
    }
    return false;
}

/**
 * Filtra palabras ofensivas usando el motor Hardcore V2 (4K FULL HD 1080p 60 FPS Edition)
 * Optimizado para rendimiento extremo en chats globales en tiempo real.
 */
function filterProfanity(text) {
    if (!text) return "";

    // 🛠 NORMALIZACIÓN: Quita acentos y espacios invisibles
    let processed = text.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
    processed = processed.replace(/[\u200B-\u200D\uFEFF]/g, "");

    // 🛡 PROTECCIÓN DE IPs
    const ipRegex = /\b(?:\d{1,3}\.){3}\d{1,3}(?::\d+)?\b/g;
    const ipMap = [];
    processed = processed.replace(ipRegex, (match) => {
        ipMap.push(match);
        return `##SAFE_IP_${ipMap.length - 1}##`;
    });

    let filtered = processed;
    
    for (const word of BANNED_WORDS) {
        try {
            // 🚀 INTENTAR OBTENER LA REGEX DE LA NUBE DE MEMORIA
            let regex = REGEX_CACHE.get(word);

            // Si no existe, la fabricamos UNA SOLA VEZ y la guardamos
            if (!regex) {
                const isEmoji = /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}]/u.test(word);
                const isAction = word.startsWith('*') && word.endsWith('*');

                if (isEmoji || word.length < 3 || isAction) {
                    const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                    regex = new RegExp(escaped, 'giu');
                } else {
                    // Mapeo ultra veloz usando métodos nativos encadenados
                    const pattern = word.split('').map(char => {
                        const c = char.toLowerCase();
                        const escapedChar = char.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                        return `(?:${LEET_MAP[c] || escapedChar})+`;
                    }).join('[\\s\\W_]*');

                    // Lookarounds quirúrgicos para evitar falsos positivos destructivos
                    const regexStr = `(?<=(?:^|[\\s\\W_]))${pattern}(?=(?:[\\s\\W_]|$))`;
                    regex = new RegExp(regexStr, 'giu');
                }

                // Guardar en el cache para el próximo milisegundo
                REGEX_CACHE.set(word, regex);
            }

            // ⚡ REEMPLAZO NATIVO A NIVEL DE BITS
            filtered = filtered.replace(regex, (match) => '*'.repeat(match.length));

        } catch (e) {
            // Failsafe por si una palabra rompe el compilador, el chat sigue fluyendo
            continue;
        }
    }
    

    // 🔓 RESTAURACIÓN: Devolver las IPs originales al texto filtrado
    ipMap.forEach((ip, index) => {
        filtered = filtered.replace(`##SAFE_IP_${index}##`, ip);
    });

    // Si el texto original contenía emojis que no se filtraron, mantenemos el original
    // pero aplicamos el filtrado donde hubo coincidencias.
    return filtered;
}

// Exportar funciones al ámbito global para asegurar que renderer.js pueda acceder a ellas
window.isCriticalThreat = isCriticalThreat;
window.filterProfanity = filterProfanity;