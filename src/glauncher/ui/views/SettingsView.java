package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import glauncher.MainView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.awt.Desktop;

public class SettingsView {

    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    private final Gson gson = new Gson();

    // --- TEXTURAS / SPRITE SHEET ---
    private final String TEXTURE_PATH = "assets/texture/ui_sprites.png"; // Renombrado para claridad
    
    // [IMPORTANTE] Ajusta estas coordenadas (X, Y, Ancho, Alto) según tu imagen 'Sin Titulo.png'

    private final Rectangle2D SPRITE_SWITCH_OFF = new Rectangle2D(33, 191, 46, 20);
    private final Rectangle2D SPRITE_SWITCH_ON  = new Rectangle2D(33, 214, 46, 20);
    private final Rectangle2D SPRITE_SLIDER_TRACK = new Rectangle2D(48, 134, 224, 8);
    private final Rectangle2D SPRITE_SLIDER_THUMB = new Rectangle2D(21, 128, 20, 20);
    private final Rectangle2D SPRITE_SCROLL_THUMB = new Rectangle2D(226, 4, 207, 39); // Usar el mismo que combobox list para scrollbar thumb
    private final Rectangle2D SPRITE_CHECKBOX_OFF = new Rectangle2D(33, 160, 25, 25); // <--- Coordenadas Checkbox OFF
    private final Rectangle2D SPRITE_CHECKBOX_ON  = new Rectangle2D(60, 160, 25, 25); // <--- Coordenadas Checkbox ON
    private final Rectangle2D SPRITE_COMBOBOX_OFF = new Rectangle2D(7, 53, 209, 41); // <--- [NUEVO] ComboBox Cerrado
    private final Rectangle2D SPRITE_COMBOBOX_ON  = new Rectangle2D(7, 3, 209, 41); // <--- [NUEVO] ComboBox Abierto
    private final Rectangle2D SPRITE_COMBOBOX_LIST = new Rectangle2D(226, 4, 207, 39); // <--- [NUEVO] Fondo de la Lista

    // Performance
    private Slider ramSlider;
    private Label ramLabel;
    private TextField txtJavaPath;
    private TextField txtJvmArgs;
    private TextField txtWidth;
    private TextField txtHeight;
    private CheckBox chkAutoUpdates;
    private ComboBox<String> cmbPlaybackMode;
    private TextField txtYoutubeApiKey;

    // Personalization
    private CheckBox chkCustomCursor; // [NUEVO]
    private CheckBox chkAnimations;
    private TextField txtBackgroundPath;
    private ColorPicker cpAccentColor;
    private ColorPicker cpOverlayColor;
    private Slider slBlurRadius;
    private Slider slOverlayOpacity;
    private Slider slCornerRadius;
    private TextField txtCustomCss;
    private CheckBox chkFullscreen; // Mover aquí para que no se elimine
    private CheckBox chkShowConsole; // Mover aquí para que no se elimine
    private ComboBox<String> cmbOnLaunch;
    private ComboBox<String> cmbLanguage;
    
    private StackPane contentArea;
    
    // Labels para traducción dinámica
    private Label lblTitle;
    private ToggleButton btnPerformance;
    private ToggleButton btnPersonalization;
    private ToggleButton btnLauncher;
    private Button btnSave;
    private Button btnReset;

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 20;");
        root.setPadding(new Insets(20));

        // --- Sidebar (Izquierda) ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(0, 20, 0, 0));
        sidebar.setStyle("-fx-background-color: transparent;");

        lblTitle = new Label("Ajustes");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 0 0 20 0;");

        btnPerformance = createCategoryButton("🎮 Rendimiento / Juego");
        btnPersonalization = createCategoryButton("🎨 Personalización");
        btnLauncher = createCategoryButton("⚙️ Launcher / Sistema");

        
        ToggleGroup group = new ToggleGroup();
        btnPerformance.setToggleGroup(group);
        btnPersonalization.setToggleGroup(group);
        btnLauncher.setToggleGroup(group);
        btnPerformance.setSelected(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        btnSave = new Button("Guardar");
        btnSave.setMaxWidth(Double.MAX_VALUE); // [FIX]
        btnSave.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btnSave.setOnAction(e -> saveSettings());

        btnReset = new Button("Restablecer");
        btnReset.setMaxWidth(Double.MAX_VALUE); // [FIX]
        btnReset.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;");
        btnReset.setOnAction(e -> resetToDefaults());

        sidebar.getChildren().addAll(lblTitle, btnPerformance, btnPersonalization, btnLauncher, spacer, btnSave, btnReset);

        // [FIX] ScrollPane para la sidebar para evitar que se corten los botones en pantallas pequeñas
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: #444; -fx-border-width: 0 1 0 0;");
        sidebarScroll.setPrefWidth(250); // [FIX] Más ancho para textos largos

        // Aplicar textura a la barra de desplazamiento del menú lateral
        skinScrollPane(sidebarScroll);

        // --- Content Area (Centro) ---
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(0, 0, 0, 20));

        // Vistas de Categorías
        VBox performanceView = createPerformanceView();
        VBox personalizationView = createPersonalizationView();
        VBox launcherView = createLauncherView();

        // Lógica de cambio de vista
        contentArea.getChildren().add(performanceView); // Default
        
        btnPerformance.setOnAction(e -> contentArea.getChildren().setAll(performanceView));
        btnPersonalization.setOnAction(e -> contentArea.getChildren().setAll(personalizationView));
        btnLauncher.setOnAction(e -> contentArea.getChildren().setAll(launcherView));

        // Cargar configuración guardada
        loadSettings();

        root.setLeft(sidebarScroll);
        root.setCenter(contentArea);

        return root;
    }

    private VBox createPerformanceView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        
        // --- SECCIÓN RAM (Modernizada) ---
        VBox ramCard = createCard("Memoria RAM");
        Label lblRamInfo = new Label("Asigna la cantidad de memoria para el juego. Se recomienda 4GB para modpacks.");
        lblRamInfo.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");

        // Detectar RAM del sistema (Intento seguro)
        long totalRamMB = 16384; // Default 16GB
        try {
            com.sun.management.OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean(); // [FIX] Cast explícit            totalRamMB = osBean.getTotalPhysicalMemorySize() / (1024 * 1024); // Convertir a MB
        } catch (Exception | Error e) {
            // Fallback si no se puede acceder a la clase interna
        }

        // Limitar la RAM máxima para dejar espacio al sistema (80% de la RAM total)
        long maxSafeRam = (long) (totalRamMB * 0.8);
        ramSlider = new Slider(512, Math.max(1024, maxSafeRam), 2048);
        ramSlider.setMajorTickUnit(512); // Ahora permite saltos de 512MB (ej. 1.5GB, 2.5GB)
        ramSlider.setMinorTickCount(0);
        ramSlider.setSnapToTicks(true); // [FIX]
        ramSlider.setMaxWidth(Double.MAX_VALUE);
        skinSlider(ramSlider); // Aplicar textura recortada

        ramLabel = new Label("2048 MB");
        ramLabel.setStyle("-fx-text-fill: #00b4db; -fx-font-size: 14px; -fx-font-weight: bold;");
        ramSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            ramLabel.setText(String.format("%.0f MB", newVal));
        });
        
        HBox ramHeader = new HBox(10, new Label("Asignado:"), ramLabel);
        ramHeader.getChildren().get(0).setStyle("-fx-text-fill: white;");
        
        ramCard.getChildren().addAll(lblRamInfo, ramHeader, ramSlider); // [FIX] Añadir ramHeader

        // --- SECCIÓN JAVA ---
        VBox javaCard = createCard("Configuración de Java");
        
        HBox javaBox = new HBox(10);
        txtJavaPath = new TextField(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        txtJavaPath.setPromptText("Ruta al ejecutable javaw.exe");
        txtJavaPath.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        HBox.setHgrow(txtJavaPath, Priority.ALWAYS);
        
        Button btnBrowseJava = new Button("Seleccionar");
        btnBrowseJava.setGraphic(getFontIcon(FontAwesomeSolid.FOLDER_OPEN, 16, Color.WHITE)); // [FIX] Usar FontIcon
        btnBrowseJava.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnBrowseJava.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar ejecutable de Java (java.exe / javaw.exe)");
            File f = fc.showOpenDialog(btnBrowseJava.getScene().getWindow());
            if (f != null) txtJavaPath.setText(f.getAbsolutePath());
        });
        
        Button btnDetect = new Button("Auto-Detectar");
        btnDetect.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;"); // [FIX]
        btnDetect.setOnAction(e -> detectJava8());
        
        javaBox.getChildren().addAll(txtJavaPath, btnBrowseJava, btnDetect);

        Label lblJvm = new Label("Argumentos JVM:");
        lblJvm.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        txtJvmArgs = new TextField("-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
        txtJvmArgs.setStyle("-fx-background-color: #333; -fx-text-fill: #aaa; -fx-background-radius: 5;");

        javaCard.getChildren().addAll(new Label("Ruta del Ejecutable:"), javaBox, lblJvm, txtJvmArgs);

        // --- SECCIÓN PANTALLA ---
        VBox screenCard = createCard("Pantalla y Resolución");
        
        HBox resBox = new HBox(10);
        resBox.setAlignment(Pos.CENTER_LEFT);
        
        txtWidth = new TextField("854"); txtWidth.setPromptText("Ancho"); 
        txtWidth.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        txtWidth.setPrefWidth(80);
        
        txtHeight = new TextField("480"); txtHeight.setPromptText("Alto"); 
        txtHeight.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        txtHeight.setPrefWidth(80);
        
        chkFullscreen = new CheckBox("Pantalla Completa");
        chkFullscreen.setStyle("-fx-text-fill: white;");
        
        resBox.getChildren().addAll(txtWidth, new Label("x"), txtHeight, chkFullscreen);
        screenCard.getChildren().addAll(resBox);
        skinCheckBox(chkFullscreen); // Aplicar textura de switch

        ScrollPane scroll = new ScrollPane(new VBox(20, ramCard, javaCard, screenCard)); // [FIX] Más espacio
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        layout.getChildren().addAll(createHeader("Rendimiento y Juego"), scroll);
        return layout;
    }

    private VBox createPersonalizationView() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_LEFT);

        // --- SECCIÓN APARIENCIA ---
        VBox uiCard = createCard("Interfaz de Usuario");
        
        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);
        Label lblColor = new Label("Color de Acento:");
        lblColor.setStyle("-fx-text-fill: white;");
        cpAccentColor = new ColorPicker(Color.web("#0078d7"));
        cpAccentColor.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        
        Label lblOverlay = new Label("Color Overlay:");
        lblOverlay.setStyle("-fx-text-fill: white;");
        cpOverlayColor = new ColorPicker(Color.BLACK);
        cpOverlayColor.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        
        colorBox.getChildren().addAll(lblColor, cpAccentColor, lblOverlay, cpOverlayColor);

        Label lblBlur = new Label("Intensidad de Desenfoque (Blur):");
        lblBlur.setStyle("-fx-text-fill: white;");
        slBlurRadius = new Slider(0, 50, 20);
        slBlurRadius.setStyle("-fx-control-inner-background: #444;");

        Label lblOpacity = new Label("Opacidad de Paneles:");
        lblOpacity.setStyle("-fx-text-fill: white;");
        slOverlayOpacity = new Slider(0.1, 1.0, 0.8);
        slOverlayOpacity.setStyle("-fx-control-inner-background: #444;");
        
        chkAnimations = new CheckBox("Animaciones Suaves");
        chkAnimations.setStyle("-fx-text-fill: white;");
        chkAnimations.setSelected(true);
        skinCheckBox(chkAnimations);

        // [NUEVO] Checkbox para cursor personalizado
        chkCustomCursor = new CheckBox("Cursor Personalizado (Gamer)");
        chkCustomCursor.setStyle("-fx-text-fill: white;");
        skinCheckBox(chkCustomCursor);

        Label lblRadius = new Label("Redondeo de Bordes:");
        lblRadius.setStyle("-fx-text-fill: white;");
        slCornerRadius = new Slider(0, 50, 15);
        slCornerRadius.setStyle("-fx-control-inner-background: #444;");

        uiCard.getChildren().addAll(colorBox, lblBlur, slBlurRadius, lblOpacity, slOverlayOpacity, lblRadius, slCornerRadius, chkAnimations);
        uiCard.getChildren().add(chkCustomCursor); // [NUEVO]
        // --- SECCIÓN FONDO ---
        VBox bgCard = createCard("Fondo del Launcher");
        Label lblBgInfo = new Label("Soporta Imagenes (PNG, JPG), GIFs y Video (MP4).");
        lblBgInfo.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");

        HBox bgBox = new HBox(10);
        txtBackgroundPath = new TextField();
        txtBackgroundPath.setPromptText("Ruta del archivo...");
        txtBackgroundPath.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        HBox.setHgrow(txtBackgroundPath, Priority.ALWAYS);

        Button btnBrowseBg = new Button("Examinar");
        btnBrowseBg.setGraphic(getFontIcon(FontAwesomeSolid.FOLDER_OPEN, 16, Color.WHITE)); // [FIX] Usar FontIcon
        btnBrowseBg.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnBrowseBg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Media Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.mp4"));
            File f = fc.showOpenDialog(null);
            if (f != null) txtBackgroundPath.setText(f.getAbsolutePath());
        });
        
        Button btnRemoveBg = new Button("Quitar");
        btnRemoveBg.setGraphic(getFontIcon(FontAwesomeSolid.TIMES, 16, Color.WHITE)); // [FIX] Usar FontIcon
        btnRemoveBg.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnRemoveBg.setOnAction(e -> txtBackgroundPath.setText(""));
        
        bgBox.getChildren().addAll(txtBackgroundPath, btnBrowseBg, btnRemoveBg);
        bgCard.getChildren().addAll(lblBgInfo, bgBox);

        // --- SECCIÓN TEMAS (NUEVO) ---
        VBox themeCard = createCard("Temas y Estilos Avanzados");
        Label lblPresets = new Label("Temas Predefinidos:");
        lblPresets.setStyle("-fx-text-fill: white;");
        HBox presetBox = new HBox(10, new Button("Oscuro (Default)"), new Button("Claro"), new Button("Glow"));
        // [NUEVO] Más presets
        presetBox.getChildren().addAll(new Button("Neon"), new Button("Retro"));
        ((Button) presetBox.getChildren().get(0)).setOnAction(e -> applyPreset("dark"));
        ((Button) presetBox.getChildren().get(1)).setOnAction(e -> applyPreset("light"));
        ((Button) presetBox.getChildren().get(2)).setOnAction(e -> applyPreset("glow"));
        ((Button) presetBox.getChildren().get(3)).setOnAction(e -> applyPreset("neon")); // [NUEVO]
        ((Button) presetBox.getChildren().get(4)).setOnAction(e -> applyPreset("retro")); // [NUEVO]
        presetBox.getChildren().forEach(node -> ((Button)node).setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;"));

        Label lblCss = new Label("CSS Personalizado (Avanzado):");
        lblCss.setStyle("-fx-text-fill: white;");
        HBox cssBox = new HBox(10);
        txtCustomCss = new TextField();
        txtCustomCss.setTooltip(new Tooltip("Ruta a un archivo .css para aplicar estilos personalizados."));
        txtCustomCss.setPromptText("Ruta al archivo .css...");
        txtCustomCss.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        HBox.setHgrow(txtCustomCss, Priority.ALWAYS);
        Button btnBrowseCss = new Button("..."); // [FIX] Texto más claro
        btnBrowseCss.setTooltip(new Tooltip("Seleccionar archivo CSS"));
        // btnBrowseCss.setGraphic(getFontIcon(FontAwesomeSolid.FOLDER_OPEN, 16, Color.WHITE)); // [FIX] No es necesario si tiene texto
        btnBrowseCss.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnBrowseCss.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Hojas de Estilo CSS", "*.css"));
            File f = fc.showOpenDialog(null);
            if (f != null) txtCustomCss.setText(f.getAbsolutePath());
        });

        // [NUEVO] Botón para abrir el archivo CSS en el editor por defecto
        Button btnOpenCss = new Button("Abrir");
        btnOpenCss.setTooltip(new Tooltip("Abrir el archivo CSS en el editor de texto por defecto."));
        btnOpenCss.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnOpenCss.setOnAction(e -> {
            try {
                File cssFile = new File(txtCustomCss.getText());
                if (cssFile.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(cssFile);
            } catch (IOException ex) { MainView.showNotification("Error", "No se pudo abrir el archivo CSS.", "error"); }
        });

        cssBox.getChildren().addAll(txtCustomCss, btnBrowseCss, btnOpenCss); // [FIX] Añadir btnOpenCss
        themeCard.getChildren().addAll(lblPresets, presetBox, new Separator(javafx.geometry.Orientation.HORIZONTAL), lblCss, cssBox);

        content.getChildren().addAll(createHeader("Personalización"), uiCard, bgCard, themeCard);
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        skinScrollPane(scroll); // Aplicar textura al scroll
        
        VBox layout = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return layout;
    }

    private VBox createLauncherView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_LEFT);

        VBox generalCard = createCard("Comportamiento");
        
        Label lblLang = new Label("Idioma:");
        lblLang.setStyle("-fx-text-fill: white;");
        cmbLanguage = new ComboBox<>();
        cmbLanguage.getItems().addAll("Español", "English", "Português");
        cmbLanguage.setValue("Español");
        cmbLanguage.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        skinComboBox(cmbLanguage); // Aplicar textura
        cmbLanguage.setOnAction(e -> updateLanguageTexts(cmbLanguage.getValue()));

        chkAutoUpdates = new CheckBox("Buscar actualizaciones automáticamente");
        chkAutoUpdates.setStyle("-fx-text-fill: white;");
        chkAutoUpdates.setSelected(true);
        skinCheckBox(chkAutoUpdates);

        Label lblOnLaunch = new Label("Al iniciar el juego:");
        lblOnLaunch.setStyle("-fx-text-fill: white;");
        cmbOnLaunch = new ComboBox<>();
        cmbOnLaunch.getItems().addAll("No hacer nada", "Ocultar el launcher", "Cerrar el launcher");
        cmbOnLaunch.setValue("No hacer nada");
        cmbOnLaunch.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        skinComboBox(cmbOnLaunch); // Aplicar textura
        
        chkShowConsole = new CheckBox("Mostrar Consola de Desarrollo (Logs)");
        chkShowConsole.setStyle("-fx-text-fill: white;");
        skinCheckBox(chkShowConsole);
        
        // [NUEVO] Botón para verificar actualizaciones manualmente
        Button btnCheckUpdates = new Button("Verificar Actualizaciones Ahora");
        btnCheckUpdates.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnCheckUpdates.setOnAction(e -> glauncher.UpdateChecker.checkForUpdates());

        Label lblPlayback = new Label("Modo de Reproducción (Música/Video):");
        lblPlayback.setStyle("-fx-text-fill: white;");
        cmbPlaybackMode = new ComboBox<>();
        cmbPlaybackMode.getItems().addAll("Solo Audio", "Video");
        cmbPlaybackMode.setValue("Solo Audio");
        cmbPlaybackMode.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        skinComboBox(cmbPlaybackMode); // Aplicar textura

        Label lblApiKey = new Label("YouTube Data API Key (Requerido para búsqueda):");
        lblApiKey.setStyle("-fx-text-fill: white;");
        txtYoutubeApiKey = new TextField();
        txtYoutubeApiKey.setPromptText("Pega tu API Key aquí...");
        txtYoutubeApiKey.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");

        Button btnOpenFolder = new Button("📂 Abrir Carpeta de Datos (.glauncher)");
        btnOpenFolder.setGraphic(getFontIcon(FontAwesomeSolid.FOLDER_OPEN, 16, Color.WHITE)); // [FIX] Usar FontIcon
        btnOpenFolder.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnOpenFolder.setOnAction(e -> {
            try { Desktop.getDesktop().open(new File(DATA_DIR)); } catch (Exception ex) { ex.printStackTrace(); }
        });
        btnOpenFolder.setTooltip(new Tooltip("Abre la carpeta donde GLauncher guarda todos sus datos (versiones, mods, configuraciones)."));

        Button btnClearCache = new Button("Borrar Caché y Temporales");
        btnClearCache.setGraphic(loadIcon("assets/icons/icons-gui/papelera.png", 16));
        btnClearCache.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        btnClearCache.setOnAction(e -> {
            File cacheDir = new File(DATA_DIR, "cache");
            File assetsTemp = new File(DATA_DIR, "assets/temp");
            deleteDirectory(cacheDir);
            deleteDirectory(assetsTemp);
            MainView.showNotification("Limpieza", "Caché eliminada correctamente.", "success");
        });

        generalCard.getChildren().addAll(
            lblLang, cmbLanguage, chkAutoUpdates,
            lblOnLaunch, cmbOnLaunch,
            chkShowConsole, btnCheckUpdates, // [FIX] Añadir btnCheckUpdates
            new Separator(), lblPlayback, cmbPlaybackMode, lblApiKey, txtYoutubeApiKey,
            new Separator(), btnOpenFolder, btnClearCache);

        layout.getChildren().addAll(createHeader("Launcher / Sistema"), generalCard);
        return layout;
    }

    private void loadSettings() {
        if (SETTINGS_FILE.exists()) {
            try (FileReader reader = new FileReader(SETTINGS_FILE)) {
                JsonObject settings = gson.fromJson(reader, JsonObject.class);
                if (settings != null) {
                    if (settings.has("ram")) {
                        double ram = settings.get("ram").getAsDouble();
                        ramSlider.setValue(ram);
                        ramLabel.setText(String.format("%.0f MB", ram));
                    }
                    if (settings.has("javaPath")) txtJavaPath.setText(settings.get("javaPath").getAsString());
                    if (settings.has("jvmArgs")) txtJvmArgs.setText(settings.get("jvmArgs").getAsString());
                    if (settings.has("width")) txtWidth.setText(settings.get("width").getAsString());
                    if (settings.has("height")) txtHeight.setText(settings.get("height").getAsString());
                    if (settings.has("fullscreen")) chkFullscreen.setSelected(settings.get("fullscreen").getAsBoolean());
                    if (settings.has("enableAnimations")) chkAnimations.setSelected(settings.get("enableAnimations").getAsBoolean());
                    if (settings.has("backgroundPath")) txtBackgroundPath.setText(settings.get("backgroundPath").getAsString());
                    if (settings.has("accentColor")) cpAccentColor.setValue(Color.web(settings.get("accentColor").getAsString()));
                    if (settings.has("overlayColor")) cpOverlayColor.setValue(Color.web(settings.get("overlayColor").getAsString()));
                    if (settings.has("blurRadius")) slBlurRadius.setValue(settings.get("blurRadius").getAsDouble());
                    if (settings.has("overlayOpacity")) slOverlayOpacity.setValue(settings.get("overlayOpacity").getAsDouble());
                    if (settings.has("cornerRadius")) slCornerRadius.setValue(settings.get("cornerRadius").getAsDouble());
                    if (settings.has("customCursor")) chkCustomCursor.setSelected(settings.get("customCursor").getAsBoolean()); // [NUEVO]
                    if (settings.has("showConsole")) chkShowConsole.setSelected(settings.get("showConsole").getAsBoolean());
                    if (settings.has("customCssPath")) txtCustomCss.setText(settings.get("customCssPath").getAsString());
                    if (settings.has("onLaunch")) cmbOnLaunch.setValue(settings.get("onLaunch").getAsString());
                    if (settings.has("language")) cmbLanguage.setValue(settings.get("language").getAsString());
                    if (settings.has("playbackMode")) cmbPlaybackMode.setValue(settings.get("playbackMode").getAsString());
                    if (settings.has("youtubeApiKey")) txtYoutubeApiKey.setText(settings.get("youtubeApiKey").getAsString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveSettings() {
        try {
            JsonObject settings = new JsonObject();
            settings.addProperty("ram", ramSlider.getValue());
            settings.addProperty("javaPath", txtJavaPath.getText());
            settings.addProperty("jvmArgs", txtJvmArgs.getText());
            settings.addProperty("width", txtWidth.getText());
            settings.addProperty("height", txtHeight.getText());
            settings.addProperty("fullscreen", chkFullscreen.isSelected());
            settings.addProperty("enableAnimations", chkAnimations.isSelected());
            settings.addProperty("backgroundPath", txtBackgroundPath.getText());
            settings.addProperty("accentColor", toHexString(cpAccentColor.getValue()));
            settings.addProperty("overlayColor", toHexString(cpOverlayColor.getValue()));
            settings.addProperty("blurRadius", slBlurRadius.getValue());
            settings.addProperty("overlayOpacity", slOverlayOpacity.getValue());
            settings.addProperty("cornerRadius", slCornerRadius.getValue());
            settings.addProperty("customCursor", chkCustomCursor.isSelected()); // [NUEVO]
            settings.addProperty("showConsole", chkShowConsole.isSelected());
            settings.addProperty("customCssPath", txtCustomCss.getText());
            settings.addProperty("autoUpdates", chkAutoUpdates.isSelected());
            settings.addProperty("onLaunch", cmbOnLaunch.getValue());
            settings.addProperty("language", cmbLanguage.getValue());
            settings.addProperty("playbackMode", cmbPlaybackMode.getValue());
            settings.addProperty("youtubeApiKey", txtYoutubeApiKey.getText());

            // Ensure directory exists
            new File(DATA_DIR).mkdirs();

            try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
                gson.toJson(settings, writer);
            }
            MainView.showNotification("Ajustes Guardados", "La configuración se ha actualizado correctamente.", "success");
            
            // [FIX] Aplicar cambios inmediatamente
            if (MainView.getInstance() != null) MainView.getInstance().applyThemeSettings();
        } catch (IOException e) {
            e.printStackTrace();
            MainView.showNotification("Error", "No se pudo guardar el archivo de configuración.", "error");
        }
    }

    private void resetToDefaults() {
        ramSlider.setValue(2048);
        txtJavaPath.setText(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        txtJvmArgs.setText("-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M");
        txtJvmArgs.setText("");
        txtWidth.setText("854");
        txtHeight.setText("480");
        chkFullscreen.setSelected(false);
        chkAnimations.setSelected(true);
        txtBackgroundPath.setText("");
        cpAccentColor.setValue(Color.web("#0078d7"));
        cpOverlayColor.setValue(Color.BLACK);
        slBlurRadius.setValue(20);
        slOverlayOpacity.setValue(0.8);
        slCornerRadius.setValue(15);
        chkCustomCursor.setSelected(false); // [NUEVO]
        txtCustomCss.setText("");
        chkShowConsole.setSelected(false);
        chkAutoUpdates.setSelected(true);
        cmbOnLaunch.setValue("No hacer nada");
        cmbLanguage.setValue("Español");
        cmbPlaybackMode.setValue("Solo Audio");
        txtYoutubeApiKey.setText("");
        
        saveSettings();
        MainView.showNotification("Restablecido", "La configuración ha vuelto a los valores por defecto.", "info");
    }

    private void detectJava8() {
        String[] commonPaths = {
            "C:\\Program Files\\Java",
            "C:\\Program Files (x86)\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\Amazon Corretto",
            "C:\\Program Files\\Azul\\zulu"
        };
        
        for (String path : commonPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        // Buscar carpetas que indiquen versión 8 (1.8)
                        if (f.getName().contains("jre1.8") || f.getName().contains("jdk1.8") || f.getName().contains("jdk-8") || f.getName().contains("zulu8")) {
                            File bin = new File(f, "bin/javaw.exe");
                            if (!bin.exists()) bin = new File(f, "bin/java.exe");
                            
                            if (bin.exists()) {
                                txtJavaPath.setText(bin.getAbsolutePath());
                                MainView.showNotification("Java 8 Detectado", "Se ha seleccionado: " + f.getName(), "success");
                                return;
                            }
                        }
                    }
                }
            }
        }
        MainView.showNotification("No encontrado", "No se detectó una instalación estándar de Java 8.", "warning");
    }

    private void updateLanguageTexts(String lang) {
        if ("English".equals(lang)) {
            lblTitle.setText("Settings");
            btnPerformance.setText("🎮 Performance / Game");
            btnPersonalization.setText("Personalization");
            btnLauncher.setText("Launcher / System");
            btnSave.setText("Save");
            btnReset.setText("Reset");
            chkAutoUpdates.setText("Check for updates automatically");
            chkShowConsole.setText("Show Developer Console (Logs)");
            chkCustomCursor.setText("Custom Cursor (Gamer)"); // [NUEVO]
        } else if ("Português".equals(lang)) {
            lblTitle.setText("Configurações");
            btnPerformance.setText("🎮 Desempenho / Jogo");
            btnPersonalization.setText("Personalização");
            btnPersonalization.setGraphic(getFontIcon(FontAwesomeSolid.PAINT_BRUSH, 24, Color.WHITE)); // [FIX]
            btnLauncher.setText("Launcher / Sistema");
            btnSave.setText("Salvar");
            btnReset.setText("Redefinir");
            chkAutoUpdates.setText("Verificar atualizações automaticamente");
            chkShowConsole.setText("Mostrar Console do Desenvolvedor");
        } else {
            lblTitle.setText("Ajustes");
            btnPerformance.setText("🎮 Rendimiento / Juego");
            btnPersonalization.setText("Personalización");
            btnPersonalization.setGraphic(getFontIcon(FontAwesomeSolid.PAINT_BRUSH, 24, Color.WHITE)); // [FIX]
            btnLauncher.setText("Launcher / Sistema");
            btnSave.setText("Guardar");
            btnReset.setText("Restablecer");
            chkAutoUpdates.setText("Buscar actualizaciones automáticamente");
            chkShowConsole.setText("Mostrar Consola de Desarrollo (Logs)");
        }
        
        // Actualizar también la vista principal
        if (MainView.getInstance() != null) MainView.getInstance().updateLanguage(lang);
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) return;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) deleteDirectory(file);
                else file.delete();
            }
        }
        directory.delete();
    }

    // --- UI Helpers ---

    // [NUEVO] Helper para FontIcon
    private FontIcon getFontIcon(FontAwesomeSolid iconCode, double size, Color color) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize((int) size);
        icon.setIconColor(color);
        return icon;
    }

    // [FIX] loadIcon para imágenes PNG/JPG (usado en botones como papelera)
    private ImageView loadIcon(String path, double size) { // [FIX] Ahora usa resolveAssetPath
        try {
            // Usar MainView.resolveAssetPath para cargar recursos correctamente
            String resolvedPath = glauncher.MainView.getInstance().resolveAssetPath(path);
            Image image = new Image(resolvedPath);
            ImageView iv = new ImageView(image);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            javafx.scene.effect.ColorAdjust whiteEffect = new javafx.scene.effect.ColorAdjust(); // [FIX] Mantener efecto blanco
            whiteEffect.setBrightness(1.0); iv.setEffect(whiteEffect);
            return iv;
        } catch (Exception e) { }
        return null;
    }

    // --- MÉTODOS DE TEXTURIZADO (SPRITE SHEET) ---

    private void skinCheckBox(CheckBox chk) {
        // Añadir cursor de mano si no está, respetando estilos previos
        String currentStyle = chk.getStyle();
        if (currentStyle == null) currentStyle = "";
        if (!currentStyle.contains("-fx-cursor")) {
            chk.setStyle(currentStyle + " -fx-cursor: hand;"); // [FIX] Espacio
        }
        
        // Crear ImageViews para los estados
        ImageView imgOff = getSprite(SPRITE_CHECKBOX_OFF);
        ImageView imgOn = getSprite(SPRITE_CHECKBOX_ON);

        // Listener para cambiar la textura
        chk.selectedProperty().addListener((obs, oldVal, newVal) -> {
            chk.setGraphic(newVal ? imgOn : imgOff);
        });

        // Estado inicial
        chk.setGraphic(chk.isSelected() ? imgOn : imgOff);
        
        // Ocultar la caja original (.box) buscando el nodo interno una vez renderizado
        javafx.application.Platform.runLater(() -> {
            Node box = chk.lookup(".box");
            if (box != null) {
                box.setVisible(false);
                box.setManaged(false); // Para que no ocupe espacio y solo se vea la textura
            }
        });
    }

    private void skinComboBox(ComboBox<?> cmb) {
        try {
            File f = new File(TEXTURE_PATH);
            String url = f.toURI().toString();

            // Estilos base usando background-position para recortar la textura
            String styleOff = String.format(
                "-fx-background-image: url('%s'); -fx-background-position: -%spx -%spx; -fx-background-size: auto; -fx-background-repeat: no-repeat; -fx-background-color: transparent; -fx-border-color: transparent;",
                url, SPRITE_COMBOBOX_OFF.getMinX(), SPRITE_COMBOBOX_OFF.getMinY()
            );
            
            String styleOn = String.format(
                "-fx-background-image: url('%s'); -fx-background-position: -%spx -%spx; -fx-background-size: auto; -fx-background-repeat: no-repeat; -fx-background-color: transparent; -fx-border-color: transparent;",
                url, SPRITE_COMBOBOX_ON.getMinX(), SPRITE_COMBOBOX_ON.getMinY()
            );

            // Aplicar estilo inicial
            cmb.setStyle(styleOff);

            // Listener para cambiar la textura al abrir/cerrar
            cmb.showingProperty().addListener((obs, oldVal, newVal) -> {
                cmb.setStyle(newVal ? styleOn : styleOff);
                
                // Intentar estilizar la lista desplegable (Popup) cuando se abre
                if (newVal) {
                    javafx.application.Platform.runLater(() -> {
                        Node listView = cmb.lookup(".list-view");
                        if (listView != null) {
                            listView.setStyle(String.format(
                                "-fx-background-image: url('%s'); -fx-background-position: -%spx -%spx; -fx-background-size: auto; -fx-background-repeat: no-repeat; -fx-background-color: #222; -fx-border-color: #444;",
                                url, SPRITE_COMBOBOX_LIST.getMinX(), SPRITE_COMBOBOX_LIST.getMinY()
                            ));
                        }
                    });
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void skinSlider(Slider slider) {
        // Usamos CSS dinámico con la URL de la textura para recortar usando background-position
        try {
            File f = new File(TEXTURE_PATH);
            String url = f.toURI().toString();
            
            // Esperar a que el Slider se renderice para buscar sus partes (.track y .thumb)
            javafx.application.Platform.runLater(() -> {
                Node track = slider.lookup(".track");
                Node thumb = slider.lookup(".thumb");

                if (track != null) {
                    track.setStyle(String.format(
                        "-fx-background-image: url('%s'); -fx-background-position: -%spx -%spx; -fx-background-size: auto; -fx-background-repeat: no-repeat; -fx-pref-height: %spx;",
                        url, SPRITE_SLIDER_TRACK.getMinX(), SPRITE_SLIDER_TRACK.getMinY(), SPRITE_SLIDER_TRACK.getHeight()
                    ));
                }
                if (thumb != null) {
                    thumb.setStyle(String.format(
                        "-fx-background-image: url('%s'); -fx-background-position: -%spx -%spx; -fx-background-size: auto; -fx-background-color: transparent; -fx-pref-width: %spx; -fx-pref-height: %spx;",
                        url, SPRITE_SLIDER_THUMB.getMinX(), SPRITE_SLIDER_THUMB.getMinY(), SPRITE_SLIDER_THUMB.getWidth(), SPRITE_SLIDER_THUMB.getHeight()
                    ));
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void skinScrollPane(ScrollPane scrollPane) {
        // Aplicar estilo a las barras de desplazamiento
        try {
            File f = new File(TEXTURE_PATH);
            String url = f.toURI().toString();
            
            // Inyectamos un estilo en línea que afecta a los hijos .thumb del scrollbar
            // Nota: Esto es un poco experimental sin una hoja de estilo externa, pero intenta forzar el background.
            scrollPane.setStyle(scrollPane.getStyle() + String.format(
                "-fx-scrollbar-thumb: url('%s');", url
            ));
        } catch (Exception e) { }
    }

    private ImageView getSprite(Rectangle2D viewport) {
        try {
            File f = new File(TEXTURE_PATH);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.setViewport(viewport);
                return iv;
            }
        } catch (Exception e) { }
        return new ImageView(); // Retornar vacío si falla
    }

    private VBox createCard(String title) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(lbl);
        return card;
    }

    private Label createHeader(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 0);");
        return title;
    }

    private void applyPreset(String theme) {
        if ("dark".equals(theme)) {
            cpAccentColor.setValue(Color.web("#0078d7"));
            cpOverlayColor.setValue(Color.BLACK);
            slOverlayOpacity.setValue(0.8);
            slBlurRadius.setValue(20);
            slCornerRadius.setValue(15);
        } else if ("light".equals(theme)) {
            cpAccentColor.setValue(Color.web("#005a9e"));
            cpOverlayColor.setValue(Color.WHITE);
            slOverlayOpacity.setValue(0.6);
            slBlurRadius.setValue(40);
            slCornerRadius.setValue(10);
        } else if ("glow".equals(theme)) {
            cpAccentColor.setValue(Color.CYAN);
            cpOverlayColor.setValue(Color.web("#1a001a"));
            slOverlayOpacity.setValue(0.7);
            slBlurRadius.setValue(30);
            slCornerRadius.setValue(25);
        }
        MainView.showNotification("Tema Aplicado", "Se ha aplicado el tema '" + theme + "'. Guarda para conservar.", "info");
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    private ToggleButton createCategoryButton(String text) {
        ToggleButton btn = new ToggleButton(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 8;";
        String hoverStyle = "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 8;";
        String selectedStyle = "-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,120,215,0.4), 10, 0, 0, 0);";

        btn.setStyle(baseStyle);

        btn.selectedProperty().addListener((obs, old, isSelected) -> 
            btn.setStyle(isSelected ? selectedStyle : baseStyle));

        btn.setOnMouseEntered(e -> { if (!btn.isSelected()) btn.setStyle(hoverStyle); });
        btn.setOnMouseExited(e -> { if (!btn.isSelected()) btn.setStyle(baseStyle); });

        return btn;
    }
}