<div align="center">

<img src="assets/logo.png" alt="GLauncher Logo" width="120" height="120"/>

# ⛏️ GLauncher

### Lanzador moderno de Minecraft para Windows · Modern Minecraft Launcher for Windows

[![Platform](https://img.shields.io/badge/platform-Windows-0078d7?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/dguerraaraque-a11y/GLauncher/releases)
[![Built with Electron](https://img.shields.io/badge/built%20with-Electron-47848f?style=for-the-badge&logo=electron&logoColor=white)](https://www.electronjs.org/)
[![Languages](https://img.shields.io/badge/languages-ES%20%7C%20EN%20%7C%20PT-blueviolet?style=for-the-badge&logo=googletranslate&logoColor=white)](#)
[![Version](https://img.shields.io/badge/version-1.1.0-0078d7?style=for-the-badge)](https://github.com/dguerraaraque-a11y/GLauncher/releases/tag/v1.1.0)
[![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)](LICENSE)

</div>

---

> 🇪🇸 **Español** | [🇬🇧 English](#-english-documentation)

---

## 🇪🇸 Documentación en Español

GLauncher es un lanzador de Minecraft moderno, elegante y completo, construido con **Electron** para Windows. Diseñado pensando en la comunidad hispanohablante, ofrece soporte multilenguaje, integración con Modrinth, reproductor de música integrado y mucho más.

---

### 📸 Capturas de Pantalla

> *Las capturas de pantalla se agregarán próximamente. Consulta la sección de [Releases](https://github.com/dguerraaraque-a11y/GLauncher/releases) para ver videos de demostración.*

| Pantalla principal | Gestor de mods | GMusic Player |
|:------------------:|:--------------:|:-------------:|
| *(próximamente)* | *(próximamente)* | *(próximamente)* |

---

### ✨ Características

#### 🎮 Lanzamiento de Minecraft

| Característica | Descripción |
|---|---|
| 🟢 **Vanilla** | Lanza Minecraft sin modificaciones |
| 🟡 **Fabric** | Soporte completo para el mod loader Fabric |
| 🔴 **Forge** | Compatible con Minecraft Forge |
| 🟠 **NeoForge** | Soporte para NeoForge |

#### 👤 Sistema de Perfiles

- 🎭 Perfiles personalizables con **skins propias**
- 🗂️ **Gestor de instancias y versiones** — crea y administra múltiples instalaciones
- ⚙️ Configuración individual por perfil

#### 🧩 Gestor de Mods

- 🔗 Integración con la **API de Modrinth** — descarga mods, paquetes de recursos y shaders directamente desde el launcher
- ✅ **Activar/desactivar** mods instalados sin borrarlos
- ✏️ Renombrar y eliminar mods fácilmente
- 📦 Gestor de mods instalados con vista clara y ordenada

#### 🖥️ Servidores

- 📡 **Lista de servidores** con ping en tiempo real
- 🟢 Estado de conexión visual para cada servidor

#### ⚙️ Configuración Java & JVM

- ☕ Configuración de **ruta de Java** personalizada
- 🧠 **Asignación de RAM** flexible
- 🛠️ **Argumentos JVM** avanzados

---

### 🆕 Novedades en v1.1.0

#### 🌐 Soporte Multilenguaje
- Idiomas disponibles: **Español 🇪🇸**, **Inglés 🇬🇧**, **Portugués 🇧🇷**
- Detección automática del idioma del sistema operativo
- Traducciones descargadas desde GitHub al iniciar el launcher (siempre actualizadas)

#### 🪟 Compatibilidad con Windows Legacy
- Soporte nativo para **Windows 7, 8 y 8.1**
- Sin dependencias adicionales en sistemas más antiguos

#### 🎵 GMusic — Reproductor Integrado
- Reproduce música de **YouTube** desde dentro del launcher
- **Mini-player flotante** siempre visible (always on top)
- Visualizador de barras animadas 🎶
- Muestra el título de la canción en reproducción
- Experiencia musical sin salir del launcher

#### 🔄 Auto-actualizador
- Verifica nuevas versiones en los **releases de GitHub** bajo demanda
- Descarga e instala actualizaciones con **barra de progreso**
- Actualización silenciosa sin interrumpir el flujo de trabajo

#### 🎨 Rediseño UI — Glassmorphism
- Diseño moderno con **efectos de blur y gradientes**
- **Tema oscuro** con color de acento azul (`#0078d7`)
- **Scrollbars personalizados** en toda la interfaz

#### 🏆 Sistema de Logros
- Desbloquea **logros** mientras usas el launcher
- Sistema de recompensas visuales integrado

#### 🧹 Limpieza Automática de Caché
- Limpieza automática de la caché de Electron al iniciar
- Ahorra aproximadamente **~500 MB** de espacio en disco

#### 🔒 Seguridad en Producción
- DevTools y atajos de recarga **desactivados** en builds de producción
- Protege la integridad del launcher ante el usuario final

#### 📦 Instalador Personalizado (NSIS)
- Instalador nativo de Windows
- **Limpieza completa** de datos de la app al desinstalar

---

### 🛠️ Stack Tecnológico

| Tecnología | Uso |
|---|---|
| ⚡ **Electron** | Framework de escritorio multiplataforma |
| 🟢 **Node.js** | Runtime del proceso principal |
| 🌐 **HTML / CSS / JS** | Interfaz de usuario (renderer) |
| 🔐 **Supabase** | Autenticación de usuarios |
| 📦 **Modrinth API** | Descarga de mods, shaders y resource packs |
| 🎵 **YouTube API** | Reproducción de música (GMusic) |
| 🚀 **GitHub Releases API** | Auto-actualizador |

---

### 📋 Requisitos

- 🪟 **Windows 7 / 8 / 8.1 / 10 / 11** (64-bit recomendado)
- ☕ **Java 8 o superior** (Java 17 recomendado para versiones modernas de Minecraft)
- 🌐 Conexión a internet (para auth, Modrinth, GMusic y actualizaciones)
- 🖥️ Mínimo 4 GB de RAM (8 GB recomendado)

---

### 🚀 Instalación & Desarrollo

#### Instalación del ejecutable (usuarios finales)

1. Ve a la sección [**Releases**](https://github.com/dguerraaraque-a11y/GLauncher/releases)
2. Descarga el instalador `.exe` más reciente
3. Ejecuta el instalador y sigue los pasos
4. ¡Listo! GLauncher aparecerá en tu escritorio

#### Instalación para desarrollo

```bash
# 1. Clona el repositorio
git clone https://github.com/dguerraaraque-a11y/GLauncher.git
cd GLauncher

# 2. Instala las dependencias
npm install

# 3. Inicia en modo desarrollo
npm start

# 4. Compila el instalador para producción
npm run build
```

> **Nota:** Para compilar el instalador de producción necesitas **NSIS** instalado en tu sistema y configurado en el PATH.

---

### 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor sigue estos pasos:

1. **Fork** el repositorio
2. Crea una rama para tu feature: `git checkout -b feature/mi-nueva-feature`
3. Realiza tus cambios y haz commit: `git commit -m "feat: agrega mi nueva feature"`
4. Haz push a tu rama: `git push origin feature/mi-nueva-feature`
5. Abre un **Pull Request** describiendo los cambios

> 💡 Sigue el estilo de código existente y añade comentarios en las partes más complejas. Para cambios mayores, abre primero un **Issue** para discutir la propuesta.

---

### 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<br/>

> 🇬🇧 **English** | [🇪🇸 Español](#-documentación-en-español)

---

## 🇬🇧 English Documentation

GLauncher is a modern, sleek, and feature-rich Minecraft launcher built with **Electron** for Windows. Designed with the Spanish-speaking community in mind, it offers multi-language support, Modrinth integration, a built-in music player, and much more.

---

### 📸 Screenshots

> *Screenshots coming soon. Check the [Releases](https://github.com/dguerraaraque-a11y/GLauncher/releases) page for demo videos.*

| Main Screen | Mod Manager | GMusic Player |
|:-----------:|:-----------:|:-------------:|
| *(coming soon)* | *(coming soon)* | *(coming soon)* |

---

### ✨ Features

#### 🎮 Minecraft Launching

| Feature | Description |
|---|---|
| 🟢 **Vanilla** | Launch unmodified Minecraft |
| 🟡 **Fabric** | Full support for the Fabric mod loader |
| 🔴 **Forge** | Compatible with Minecraft Forge |
| 🟠 **NeoForge** | Support for NeoForge |

#### 👤 Profile System

- 🎭 Customizable profiles with **custom skins**
- 🗂️ **Instance & version manager** — create and manage multiple installations
- ⚙️ Per-profile individual settings

#### 🧩 Mod Manager

- 🔗 **Modrinth API** integration — download mods, resource packs, and shaders directly from the launcher
- ✅ **Enable/disable** installed mods without deleting them
- ✏️ Rename and delete mods easily
- 📦 Installed mods manager with a clean, organized view

#### 🖥️ Server List

- 📡 **Server list** with real-time ping
- 🟢 Visual connection status for each server

#### ⚙️ Java & JVM Configuration

- ☕ **Custom Java path** configuration
- 🧠 Flexible **RAM allocation**
- 🛠️ Advanced **JVM arguments**

---

### 🆕 What''s New in v1.1.0

#### 🌐 Multi-Language Support
- Available languages: **Spanish 🇪🇸**, **English 🇬🇧**, **Portuguese 🇧🇷**
- Automatic detection of the operating system language
- Translations downloaded from GitHub on launch (always up to date)

#### 🪟 Legacy Windows Compatibility
- Native support for **Windows 7, 8, and 8.1**
- No additional dependencies on older systems

#### 🎵 GMusic — Integrated Player
- Play **YouTube music** from inside the launcher
- **Floating mini-player** always visible (always on top)
- Animated bar visualizer 🎶
- Displays the current song title
- Full music experience without leaving the launcher

#### 🔄 Auto-Updater
- Checks for new versions on **GitHub Releases** on demand
- Downloads and installs updates with a **progress bar**
- Silent update without interrupting your workflow

#### 🎨 UI Redesign — Glassmorphism
- Modern design with **blur effects and gradients**
- **Dark theme** with blue accent color (`#0078d7`)
- **Custom scrollbars** throughout the UI

#### 🏆 Achievements System
- Unlock **achievements** while using the launcher
- Integrated visual rewards system

#### 🧹 Automatic Cache Cleanup
- Automatically cleans Electron cache on startup
- Saves approximately **~500 MB** of disk space

#### 🔒 Production Security
- DevTools and reload shortcuts **disabled** in production builds
- Protects launcher integrity for end users

#### 📦 Custom Installer (NSIS)
- Native Windows installer
- **Complete cleanup** of app data on uninstall

---

### 🛠️ Tech Stack

| Technology | Usage |
|---|---|
| ⚡ **Electron** | Cross-platform desktop framework |
| 🟢 **Node.js** | Main process runtime |
| 🌐 **HTML / CSS / JS** | User interface (renderer) |
| 🔐 **Supabase** | User authentication |
| 📦 **Modrinth API** | Mods, shaders & resource packs download |
| 🎵 **YouTube API** | Music playback (GMusic) |
| 🚀 **GitHub Releases API** | Auto-updater |

---

### 📋 Requirements

- 🪟 **Windows 7 / 8 / 8.1 / 10 / 11** (64-bit recommended)
- ☕ **Java 8 or higher** (Java 17 recommended for modern Minecraft versions)
- 🌐 Internet connection (for auth, Modrinth, GMusic, and updates)
- 🖥️ Minimum 4 GB RAM (8 GB recommended)

---

### 🚀 Installation & Development

#### Executable installation (end users)

1. Go to the [**Releases**](https://github.com/dguerraaraque-a11y/GLauncher/releases) section
2. Download the latest `.exe` installer
3. Run the installer and follow the steps
4. Done! GLauncher will appear on your desktop

#### Development installation

```bash
# 1. Clone the repository
git clone https://github.com/dguerraaraque-a11y/GLauncher.git
cd GLauncher

# 2. Install dependencies
npm install

# 3. Start in development mode
npm start

# 4. Build the production installer
npm run build
```

> **Note:** To build the production installer, you need **NSIS** installed on your system and available in your PATH.

---

### 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. Create a branch for your feature: `git checkout -b feature/my-new-feature`
3. Make your changes and commit: `git commit -m "feat: add my new feature"`
4. Push to your branch: `git push origin feature/my-new-feature`
5. Open a **Pull Request** describing your changes

> 💡 Follow the existing code style and add comments in complex sections. For major changes, please open an **Issue** first to discuss the proposal.

---

### 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by the GLauncher team

⭐ **Si te gusta el proyecto, dale una estrella!** · **If you like the project, give it a star!** ⭐

[![GitHub Stars](https://img.shields.io/github/stars/dguerraaraque-a11y/GLauncher?style=social)](https://github.com/dguerraaraque-a11y/GLauncher/stargazers)

</div>
