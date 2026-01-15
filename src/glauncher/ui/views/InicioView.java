package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import glauncher.MainView;

public class InicioView {

    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final File VERSIONS_DIR = new File(DATA_DIR, "versions");
    private final File SESSION_FILE = new File(DATA_DIR, "session.json");
    private final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    private final Gson gson = new Gson();
    
    private VBox devConsole;
    private TextArea devConsoleOutput;
    private Label lblUser;
    private Circle avatar;
    private long lastSessionModified = 0;

    // [FIX] SSL Handshake fix for old Java 8 / Forge Maven
    // This globally disables SSL certificate validation.
    // It's a security risk but necessary for old Java versions to connect to modern servers.
    static {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        try {
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3"); // [FIX] Forzar protocolos seguros para evitar handshake_failure
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) { }
    }

    public Parent getView() {
        StackPane root = new StackPane();
        // [FIX] Fondo transparente para que se vea el fondo global
        root.setStyle("-fx-background-color: transparent;");
        
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(0)); // Sin padding externo para que la barra lateral ocupe todo el alto

        // --- BARRA LATERAL (Izquierda) ---
        VBox sidebar = new VBox(20);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPrefWidth(280);
        sidebar.setPadding(new Insets(30));
        // [MODIFICADO] Barra lateral transparente con widgets flotantes (Estilo Moderno)
        sidebar.setStyle("-fx-background-color: transparent;"); 

        // Declaraciones faltantes
        VBox userWidget = new VBox(10);
        HBox userHeader = new HBox(10);

        // Widget Usuario
        // 1. Widget Usuario
        userWidget.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        userWidget.setAlignment(Pos.CENTER);
        
        userHeader.setAlignment(Pos.CENTER_LEFT);
        avatar = new Circle(25, Color.web("#0078d7"));
        avatar.setStroke(Color.WHITE);
        avatar.setStrokeWidth(1.5);
        // Intentar cargar avatar
        
        lblUser = new Label("Cargando...");
        lblUser.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");
        userHeader.getChildren().addAll(avatar, lblUser);
        
        Label lblRank = new Label("Rango: Usuario");
        lblRank.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");

        // Widget Noticias (Movido a la izquierda)
        VBox newsWidget = new VBox(10);
        newsWidget.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        Label newsTitle = new Label("Últimas Novedades");
        newsTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label newsContent = new Label("• Nuevo sistema de Skins\n• Chat Global activado\n• Tienda de cosméticos\n• Optimización de FPS");
        newsContent.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        newsContent.setWrapText(true);
        newsWidget.getChildren().addAll(newsTitle, newsContent);

        // Carga inicial del usuario
        userWidget.getChildren().addAll(userHeader, lblRank);
        updateUserInfo();

        // --- CENTRO (Logo y Play) ---
        VBox centerArea = new VBox(30);
        centerArea.setAlignment(Pos.CENTER);
        
        Region spacerTop = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);

        Label title = new Label("GLAUNCHER");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 70px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, cyan, 25, 0.5, 0, 0); -fx-font-family: 'Segoe UI Black', Impact, sans-serif;");

        // Área de Juego (Barra inferior)
        HBox playArea = new HBox(20); // [MEJORA] Más espacio entre elementos
        playArea.setAlignment(Pos.CENTER);
        playArea.setMaxWidth(700);
        playArea.setPadding(new Insets(20));
        playArea.setStyle("-fx-background-color: rgba(20, 20, 20, 0.8); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");

        // Selector de Versiones
        // 2. Selector de Versiones
        ComboBox<String> versionSelector = new ComboBox<>();
        versionSelector.setPromptText("Seleccionar Versión");
        versionSelector.setPrefWidth(250);
        versionSelector.setPrefHeight(45);
        versionSelector.setStyle("-fx-font-size: 14px; -fx-base: #222; -fx-text-fill: white; -fx-background-radius: 3;");
        
        // Cargar versiones descargadas
        List<String> versions = getDownloadedVersions();
        if (versions.isEmpty()) {
            versionSelector.getItems().add("Sin versiones instaladas");
            versionSelector.getSelectionModel().selectFirst();
        } else {
            versionSelector.getItems().addAll(versions);
            versionSelector.getSelectionModel().selectFirst();
        }

        // Botón PLAY
        // 3. Botón JUGAR
        Button btnPlay = new Button("JUGAR");
        btnPlay.setPrefWidth(200);
        btnPlay.setMaxWidth(Double.MAX_VALUE);
        btnPlay.setPrefHeight(60);
        // Estilo estilo Minecraft (Verde, bloque)
        String defaultStyle = "-fx-background-color: #3c8527; " + // Verde base
                              "-fx-text-fill: white; " +
                              "-fx-font-size: 24px; " +
                              "-fx-font-family: 'Segoe UI Black', 'Impact', sans-serif; " + // Fuente gruesa
                              "-fx-cursor: hand; " +
                              "-fx-background-radius: 2; " + // Bordes casi cuadrados
                              "-fx-border-color: #1e4d13; " + // Borde oscuro
                              "-fx-border-width: 2; " +
                              "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 2);";
        
        btnPlay.setStyle(defaultStyle);
        
        // Animación RGB en el borde del botón Play
        Timeline rgbPlay = new Timeline(
            new KeyFrame(Duration.seconds(0), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: red;")),
            new KeyFrame(Duration.seconds(0.5), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: yellow;")),
            new KeyFrame(Duration.seconds(1.0), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: green;")),
            new KeyFrame(Duration.seconds(1.5), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: cyan;")),
            new KeyFrame(Duration.seconds(2.0), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: blue;")),
            new KeyFrame(Duration.seconds(2.5), e -> btnPlay.setStyle(defaultStyle + "-fx-border-color: magenta;"))
        );
        rgbPlay.setCycleCount(Timeline.INDEFINITE);
        rgbPlay.play();

        // 4. Botón Reparar
        Button btnRepair = new Button();
        // [NUEVO] Usar el icono de tuerca en lugar de texto
        btnRepair.setGraphic(loadIcon("assets/icons/icons-gui/tuerca.png", 20));
        btnRepair.setTooltip(new Tooltip("Reparar Instalación (Verificar archivos)"));
        // [MEJORA] Estilo más moderno y limpio para el botón de icono
        btnRepair.setPrefHeight(50);
        btnRepair.setPrefWidth(60);
        btnRepair.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;");
        
        // Efecto Hover para el botón de reparar
        btnRepair.setOnMouseEntered(e -> btnRepair.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;"));
        btnRepair.setOnMouseExited(e -> btnRepair.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 3;"));
        
        btnRepair.setOnAction(e -> {
            String selected = versionSelector.getValue();
            if (selected != null && !selected.equals("Sin versiones instaladas")) {
                repairGame(selected);
            }
        });

        btnPlay.setOnAction(e -> {
            String selected = versionSelector.getValue();
            if (selected != null && !selected.equals("Sin versiones instaladas")) {
                launchGame(selected, root);
            } else {
                MainView.showNotification("Error", "Debes descargar una versión primero.", "error");
            }
        });
        // 5. Hora (Solo la hora)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        sidebar.getChildren().addAll(userWidget, newsWidget, spacer);

        // --- CENTRO (Título y Redes) ---

        // Barra de Redes Sociales
        HBox socialBox = new HBox(15);
        socialBox.setAlignment(Pos.CENTER);
        
        Button btnDiscord = createSocialButton("Discord", "#7289da", "https://discord.gg/tu-invitacion");
        Button btnYoutube = createSocialButton("YouTube", "#ff0000", "https://youtube.com/@DaniCraftYT25");
        Button btnWeb = createSocialButton("Sitio Web", "#0078d7", "https://glauncher.vercel.app");
        
        socialBox.getChildren().addAll(btnDiscord, btnYoutube, btnWeb);
        
        // --- Consola de Desarrollador (Panel Admin / Lanzamiento) ---
        devConsole = new VBox(15);
        devConsole.setAlignment(Pos.CENTER);
        // Estilo moderno, oscuro y translúcido
        devConsole.setStyle("-fx-background-color: rgba(10, 10, 10, 0.95); -fx-background-radius: 20; -fx-padding: 40; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 20, 0, 0, 0);");
        devConsole.setVisible(false);
        devConsole.setOpacity(0); // Para animación de entrada
        devConsole.setMaxSize(900, 600);

        // Reloj Grande (Solo deja el reloj)
        Label consoleClock = new Label();
        consoleClock.setStyle("-fx-text-fill: #00b4db; -fx-font-size: 64px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,180,219,0.4), 15, 0, 0, 0);");
        Timeline clockUpdater = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            consoleClock.setText(java.time.LocalTime.now().toString().substring(0, 5));
        }));
        clockUpdater.setCycleCount(Timeline.INDEFINITE);
        clockUpdater.play();

        Label consoleTitle = new Label("Iniciando Minecraft...");
        consoleTitle.setStyle("-fx-text-fill: #aaa; -fx-font-size: 18px;");

        devConsoleOutput = new TextArea();
        devConsoleOutput.setEditable(false);
        devConsoleOutput.setStyle("-fx-control-inner-background: #000; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced';");
        devConsoleOutput.setWrapText(true);
        // Estilo terminal hacker bonito
        devConsoleOutput.setStyle("-fx-control-inner-background: #151515; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced'; -fx-highlight-fill: #00ff00; -fx-highlight-text-fill: #000; -fx-background-radius: 10; -fx-border-color: #333; -fx-border-radius: 10;");
        VBox.setVgrow(devConsoleOutput, Priority.ALWAYS);
        devConsole.getChildren().addAll(consoleTitle, devConsoleOutput);
        
        Button btnHideConsole = new Button("Ocultar Consola");
        btnHideConsole.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20; -fx-padding: 10 20;");
        btnHideConsole.setOnAction(e -> {
            // Restaurar vista normal
            FadeTransition ft = new FadeTransition(Duration.millis(300), devConsole);
            ft.setToValue(0);
            ft.setOnFinished(ev -> { devConsole.setVisible(false); layout.setVisible(true); layout.setOpacity(1); });
            ft.play();
        });

        devConsole.getChildren().addAll(consoleClock, consoleTitle, devConsoleOutput, btnHideConsole);
        
        playArea.getChildren().addAll(versionSelector, btnPlay, btnRepair);
        
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

        centerArea.getChildren().addAll(spacerTop, title, playArea, socialBox, spacerBottom);

        // --- DERECHA (Noticias Mini) ---
        VBox rightWidgets = new VBox(20);
        rightWidgets.setAlignment(Pos.TOP_RIGHT);
        rightWidgets.setPrefWidth(280);
        
        // Widget Estado del Servidor
        VBox serverWidget = new VBox(10);
        serverWidget.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        Label serverTitle = new Label("Estado del Servidor");
        serverTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(5, Color.LIMEGREEN);
        Label statusText = new Label("Online - 34/100 Jugadores");
        statusText.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        statusBox.getChildren().addAll(statusDot, statusText);
        
        serverWidget.getChildren().addAll(serverTitle, statusBox);

        // Widget Sistema (RAM + Hora) - Movido a la derecha
        VBox sysWidget = new VBox(10);
        sysWidget.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        
        HBox sysHeader = new HBox(10);
        sysHeader.setAlignment(Pos.CENTER_LEFT);
        Label sysTitle = new Label("Sistema");
        sysTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Region sysSpacer = new Region();
        HBox.setHgrow(sysSpacer, Priority.ALWAYS);
        Label lblTime = new Label(java.time.LocalTime.now().toString().substring(0, 5));
        lblTime.setStyle("-fx-text-fill: cyan; -fx-font-weight: bold;");
        sysHeader.getChildren().addAll(sysTitle, sysSpacer, lblTime);

        ProgressBar ramBar = new ProgressBar(0.5);
        ramBar.setStyle("-fx-accent: #00ff00;");
        ramBar.setMaxWidth(Double.MAX_VALUE);
        Label ramLabel = new Label("RAM: Calculando...");
        ramLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 10px;");
        
        Timeline ramUpdate = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            double used = (double)(total - free) / total;
            ramBar.setProgress(used);
            ramLabel.setText(String.format("RAM: %.0f%% Usada", used * 100));
            lblTime.setText(java.time.LocalTime.now().toString().substring(0, 5));
            updateUserInfo(); 
        }));
        ramUpdate.setCycleCount(Timeline.INDEFINITE);
        ramUpdate.play();

        sysWidget.getChildren().addAll(sysHeader, ramLabel, ramBar);

        // Widget de Música
        VBox musicWidget = createMusicWidget();
        
        rightWidgets.getChildren().addAll(serverWidget, sysWidget, musicWidget);

        layout.setLeft(sidebar);
        layout.setCenter(centerArea);
        layout.setRight(rightWidgets);

        // Añadir layout y consola al root (StackPane permite superposición)
        root.getChildren().addAll(layout, devConsole);

        return root;
    }

    private VBox createMusicWidget() {
        VBox widget = new VBox(10);
        widget.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 20;");
        
        Label header = new Label("GMusic");
        header.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        Label songTitle = new Label("Sin reproducción");
        songTitle.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        songTitle.setWrapText(true);
        
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        
        Button btnPrev = new Button("⏮");
        btnPrev.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        
        Button btnPlay = new Button("▶");
        btnPlay.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-background-radius: 20; -fx-min-width: 30px; -fx-cursor: hand;");
        
        Button btnNext = new Button("⏭");
        btnNext.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        
        controls.getChildren().addAll(btnPrev, btnPlay, btnNext);
        widget.getChildren().addAll(header, songTitle, controls);
        
        // [GEMINI CODE ASSIST] La lógica anterior fue eliminada porque la clase MusicView
        // ha sido rediseñada. Ya no es un controlador de música de fondo (Singleton),
        // sino una vista completa con su propia interfaz.
        // Este widget ya no puede controlar la música de esa manera.
        songTitle.setText("Abre la pestaña de Música");
        btnPrev.setDisable(true);
        btnPlay.setDisable(true);
        btnNext.setDisable(true);
        
        return widget;
    }

    // [NUEVO] Método auxiliar para cargar iconos fácilmente
    private ImageView loadIcon(String path, double size) {
        try {
            String resolved = resolveAssetPath(path);
            ImageView iv = new ImageView(new Image(resolved));
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) { }
        return null;
    }

    // [FIX] Método ROBUSTO para encontrar assets en cualquier entorno (IDE, Portable, EXE)
    private String resolveAssetPath(String path) {
        // 1. Intentar ruta directa (Entorno desarrollo / Portable)
        File f = new File(path);
        if (f.exists()) return "file:" + f.getAbsolutePath();
        
        // 2. Intentar ruta 'app' (Instalador EXE - Working Dir = Install Dir)
        File appAssets = new File("app" + File.separator + path);
        if (appAssets.exists()) return "file:" + appAssets.getAbsolutePath();
        
        // 3. Intentar ruta relativa al JAR (Lo más seguro para jpackage)
        try {
            String jarPath = InicioView.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            File jarDir = jarFile.getParentFile(); // carpeta 'app'
            
            if (jarDir != null) {
                File siblingAssets = new File(jarDir, path);
                if (siblingAssets.exists()) return "file:" + siblingAssets.getAbsolutePath();
            }
        } catch (Exception e) { }
        
        return "file:" + path; // Fallback
    }

    private List<String> getDownloadedVersions() {
        List<String> list = new ArrayList<>();
        // Crear directorio si no existe para evitar errores
        if (!VERSIONS_DIR.exists()) VERSIONS_DIR.mkdirs();
        
        if (VERSIONS_DIR.exists() && VERSIONS_DIR.isDirectory()) {
            File[] files = VERSIONS_DIR.listFiles(File::isDirectory);
            if (files != null) {
                for (File f : files) list.add(f.getName());
            }
        }
        return list;
    }

    private void launchGame(String version, StackPane root) {
        // [FIX] Detectar entorno móvil (Android/iOS) para evitar crashes
        if ("android".equals(System.getProperty("javafx.platform")) || System.getProperty("java.vendor").toLowerCase().contains("android")) {
            Platform.runLater(() -> MainView.showNotification("Móvil", "El lanzamiento de Minecraft Java no está soportado en móviles aún.", "warning"));
            return;
        }

        // Mostrar consola
        // Animación: Ocultar interfaz principal (Widget)
        if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof BorderPane) {
            Node mainLayout = root.getChildren().get(0);
            FadeTransition ftOut = new FadeTransition(Duration.millis(500), mainLayout);
            ftOut.setFromValue(1); ftOut.setToValue(0);
            ftOut.setOnFinished(e -> mainLayout.setVisible(false));
            ftOut.play();
        }

        // Animación: Mostrar Consola (Panel Admin) con Reloj
        devConsole.setVisible(true);
        devConsole.setManaged(true);
        devConsole.toFront();
        FadeTransition ftIn = new FadeTransition(Duration.millis(500), devConsole);
        ftIn.setFromValue(0); ftIn.setToValue(1);
        ftIn.play();

        devConsoleOutput.clear();
        devConsoleOutput.appendText("Iniciando Minecraft " + version + "...\n");
        
        new Thread(() -> {
            try {
                final JsonObject settings = loadSettings();
                
                // Leer usuario actualizado para el juego
                JsonObject session = loadSession();
                String username = session.has("username") ? session.get("username").getAsString() : "Invitado";
                String uuid = session.has("uuid") ? session.get("uuid").getAsString() : "0";
                String token = session.has("token") ? session.get("token").getAsString() : "0";
                String userType = session.has("type") && session.get("type").getAsString().equals("microsoft") ? "msa" : "mojang";

                File versionFolder = new File(VERSIONS_DIR, version);
                File jsonFile = new File(versionFolder, version + ".json");

                // [FIX] 1. Cargar JSON primero para detectar herencia ("jar": "1.8.9")
                JsonObject versionJson = null;
                if (jsonFile.exists()) {
                    try {
                        versionJson = gson.fromJson(new FileReader(jsonFile), JsonObject.class);
                    } catch (Exception ex) { /* Ignorar error de parseo inicial */ }
                }

                if (versionJson == null) {
                    Platform.runLater(() -> MainView.showNotification("Error Fatal", "El archivo JSON de la versión no existe o está dañado.", "error"));
                    return;
                }

                // [FIX] 2. Determinar el JAR correcto (Forge usa 1.8.9.jar, no el suyo propio)
                String jarId = version;
                if (versionJson.has("jar")) {
                    jarId = versionJson.get("jar").getAsString();
                } else if (versionJson.has("inheritsFrom")) {
                    jarId = versionJson.get("inheritsFrom").getAsString();
                }
                
                File targetJarFile = new File(VERSIONS_DIR, jarId + File.separator + jarId + ".jar");
                File currentVersionJar = new File(versionFolder, version + ".jar");

                if (!targetJarFile.exists()) {
                    // Fallback: Si no existe el heredado, buscar el local
                    if (currentVersionJar.exists()) {
                        targetJarFile = currentVersionJar;
                    } else {
                        final String missingJar = jarId + ".jar";
                        Platform.runLater(() -> MainView.showNotification("Error", "Falta el archivo JAR del juego: " + missingJar, "error"));
                        return;
                    }
                }

                // [FIX] 3. Fusionar librerías del padre si existe herencia (Fix Missing Libraries)
                // Esto asegura que se descarguen TODAS las librerías (Vanilla + Forge/Optifine)
                if (versionJson.has("inheritsFrom")) {
                    String parentId = versionJson.get("inheritsFrom").getAsString();
                    File parentJsonFile = new File(VERSIONS_DIR, parentId + File.separator + parentId + ".json");
                    if (parentJsonFile.exists()) {
                        try {
                            JsonObject parentJson = gson.fromJson(new FileReader(parentJsonFile), JsonObject.class);
                            if (parentJson.has("libraries")) {
                                JsonArray parentLibs = parentJson.getAsJsonArray("libraries");
                                JsonArray currentLibs = versionJson.has("libraries") ? versionJson.getAsJsonArray("libraries") : new JsonArray();
                                // Añadir las del hijo al final para que tengan preferencia o se sumen
                                parentLibs.addAll(currentLibs); 
                                versionJson.add("libraries", parentLibs);
                            }
                            
                            // [FIX] Heredar mainClass y argumentos si faltan (Forge antiguo 1.1 - 1.7.10)
                            if (!versionJson.has("mainClass") && parentJson.has("mainClass")) {
                                versionJson.add("mainClass", parentJson.get("mainClass"));
                            }
                            if (!versionJson.has("minecraftArguments") && parentJson.has("minecraftArguments")) {
                                versionJson.add("minecraftArguments", parentJson.get("minecraftArguments"));
                            }
                        } catch (Exception e) {
                            devConsoleOutput.appendText("Advertencia: No se pudo leer la versión padre " + parentId + "\n");
                        }
                    } else {
                        devConsoleOutput.appendText("Advertencia: No se encontró el archivo JSON de la versión padre (" + parentId + "). Faltarán librerías y configuración.\n");
                    }
                }

                if (!versionJson.has("mainClass")) {
                    Platform.runLater(() -> MainView.showNotification("Error Fatal", "No se encontró 'mainClass'. Falta la versión base (Vanilla) o el JSON está corrupto.", "error"));
                    return;
                }

                String mainClass = versionJson.get("mainClass").getAsString();

                // [FIX] Advertir si se usa Java moderno con versiones antiguas (LaunchWrapper)
                if (mainClass.contains("launchwrapper") && !settings.has("javaPath")) {
                    String javaVersion = System.getProperty("java.version");
                    if (!javaVersion.startsWith("1.8")) {
                        Platform.runLater(() -> MainView.showNotification("Advertencia", "Esta versión podría no ser compatible con Java " + javaVersion + ". Se recomienda usar Java 8.", "warning"));
                    }
                }

                // 1.5 Descargar Asset Index (Crucial para que el juego arranque)
                if (versionJson.has("assetIndex")) {
                    JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
                    String id = assetIndex.get("id").getAsString();
                    String url = assetIndex.get("url").getAsString();
                    File assetsIndexDir = new File(DATA_DIR, "assets/indexes");
                    File indexFile = new File(assetsIndexDir, id + ".json");
                    if (!indexFile.exists()) downloadFile(url, indexFile);
                    
                    // [FIX] Descargar los objetos de sonido/recursos (Assets)
                    downloadAssets(indexFile);
                }
                
                // 2. Construir Classpath
                StringBuilder cp = new StringBuilder();
                
                // Descargar y añadir librerías
                final File libsDir = new File(DATA_DIR, "libraries");
                Platform.runLater(() -> MainView.showNotification("Lanzamiento", "Verificando librerías...", "info"));
                
                List<File> libraries = downloadLibraries(versionJson, libsDir);
                
                // [FIX] Inyectar dependencias faltantes para Forge/Legacy (jopt-simple y Log4j)
                if (mainClass.contains("launchwrapper")) {
                    boolean hasJopt = false;
                    boolean hasLog4jApi = false;
                    boolean hasLog4jCore = false;
                    boolean hasLzma = false;
                    boolean hasAsm = false;
                    boolean hasGuava = false;

                    boolean hasScalaLib = false;
                    boolean hasScalaCompiler = false;

                    for (File f : libraries) {
                        String name = f.getName();
                        if (name.contains("jopt-simple")) hasJopt = true;
                        if (name.contains("log4j-api")) hasLog4jApi = true;
                        if (name.contains("log4j-core")) hasLog4jCore = true;
                        if (name.contains("guava")) hasGuava = true;
                        if (name.contains("lzma")) hasLzma = true;
                        if (name.contains("asm-all")) hasAsm = true;
                        if (name.contains("scala-library")) hasScalaLib = true;
                        if (name.contains("scala-compiler")) hasScalaCompiler = true;
                    }
                    
                    if (!hasJopt) {
                        devConsoleOutput.appendText("Detectado entorno Legacy sin jopt-simple. Inyectando dependencia...\n");
                        File joptFile = new File(libsDir, "net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar");
                        if (!joptFile.exists()) downloadFile("https://libraries.minecraft.net/net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar", joptFile);
                        libraries.add(joptFile);
                    }
                    
                    if (!hasLog4jApi) {
                        devConsoleOutput.appendText("Inyectando log4j-api (Fix Crash)...\n");
                        File lib = new File(libsDir, "org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar", lib);
                        libraries.add(lib);
                    }

                    if (!hasLog4jCore) {
                        devConsoleOutput.appendText("Inyectando log4j-core (Fix Crash)...\n");
                        File lib = new File(libsDir, "org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar", lib);
                        libraries.add(lib);
                    }
                    
                    if (!hasGuava) {
                        devConsoleOutput.appendText("Inyectando Guava (Fix Forge Crash)...\n");
                        File lib = new File(libsDir, "com/google/guava/guava/17.0/guava-17.0.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/com/google/guava/guava/17.0/guava-17.0.jar", lib);
                        libraries.add(lib);
                    }

                    if (!hasLzma) {
                        devConsoleOutput.appendText("Inyectando lzma (Fix LaunchWrapper)...\n");
                        File lib = new File(libsDir, "lzma/lzma/0.0.1/lzma-0.0.1.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/lzma/lzma/0.0.1/lzma-0.0.1.jar", lib);
                        libraries.add(lib);
                    }

                    if (!hasAsm) {
                        devConsoleOutput.appendText("Inyectando asm-all (Fix LaunchWrapper)...\n");
                        File lib = new File(libsDir, "org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar", lib);
                        libraries.add(lib);
                    }

                    if (!hasScalaLib) {
                        devConsoleOutput.appendText("Inyectando scala-library (Fix Forge antiguo)...\n");
                        File lib = new File(libsDir, "org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar", lib);
                        libraries.add(lib);
                    }

                    if (!hasScalaCompiler) {
                        devConsoleOutput.appendText("Inyectando scala-compiler (Fix Forge antiguo)...\n");
                        File lib = new File(libsDir, "org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar");
                        if (!lib.exists()) downloadFile("https://libraries.minecraft.net/org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar", lib);
                        libraries.add(lib);
                    }
                }

                for (File lib : libraries) {
                    cp.append(lib.getAbsolutePath()).append(File.pathSeparator);
                }
                
                // [FIX] Añadir JAR de Forge (versión actual) si es distinto al Vanilla
                if (!currentVersionJar.getAbsolutePath().equals(targetJarFile.getAbsolutePath()) && currentVersionJar.exists()) {
                    cp.append(currentVersionJar.getAbsolutePath()).append(File.pathSeparator);
                }
                
                cp.append(targetJarFile.getAbsolutePath());

                // Extraer Natives (DLLs)
                // [FIX] Natives por versión (Estilo TLauncher/Prism) para evitar conflictos entre versiones
                File nativesDir = new File(versionFolder, "natives");
                extractNatives(versionJson, libsDir, nativesDir);

                // 3. Construir Comando
                devConsoleOutput.appendText("Construyendo comando de lanzamiento...\n");
                String javaPath = settings.has("javaPath") ? settings.get("javaPath").getAsString() : System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                
                List<String> command = new ArrayList<>();
                command.add(javaPath);
                
                // RAM
                double ram = settings.has("ram") ? settings.get("ram").getAsDouble() : 2048;
                command.add("-Xmx" + (int)ram + "M");
                
                // Argumentos JVM Personalizados
                if (settings.has("jvmArgs")) {
                    String argsStr = settings.get("jvmArgs").getAsString();
                    if (!argsStr.isEmpty()) {
                        for (String arg : argsStr.split(" ")) command.add(arg);
                    }
                }

                // [NUEVO] Soporte para "Mods Universales" (Java Agents)
                // Permite cargar archivos .jar que modifican el juego en cualquier versión (ej. Skins, DiscordRPC)
                File agentsDir = new File(DATA_DIR, "agents");
                if (agentsDir.exists() && agentsDir.isDirectory()) {
                    File[] agents = agentsDir.listFiles((dir, name) -> name.endsWith(".jar"));
                    if (agents != null) {
                        for (File agent : agents) {
                            command.add("-javaagent:" + agent.getAbsolutePath());
                            Platform.runLater(() -> devConsoleOutput.appendText(">> [SISTEMA] Inyectando agente global: " + agent.getName() + "\n"));
                        }
                    }
                }

                command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
                command.add("-cp");
                command.add(cp.toString());
                command.add(mainClass);

                // [FIX] Añadir TweakClass para LaunchWrapper (ej. Vanilla 1.7.10, Forge)
                if (versionJson.has("tweakClass")) {
                    command.add("--tweakClass");
                    command.add(versionJson.get("tweakClass").getAsString());
                }

                // --- Argumentos del Juego (Lógica mejorada para versiones antiguas y modernas) ---
                if (versionJson.has("minecraftArguments")) {
                    // Versión Antigua (pre 1.13) que usa placeholders
                    String mcArgs = versionJson.get("minecraftArguments").getAsString();
                    mcArgs = mcArgs.replace("${auth_player_name}", username);
                    mcArgs = mcArgs.replace("${version_name}", version);
                    mcArgs = mcArgs.replace("${game_directory}", "\"" + DATA_DIR + "\"");
                    mcArgs = mcArgs.replace("${assets_root}", "\"" + new File(DATA_DIR, "assets").getAbsolutePath() + "\"");
                    String indexId = versionJson.has("assetIndex") ? versionJson.getAsJsonObject("assetIndex").get("id").getAsString() : version;
                    mcArgs = mcArgs.replace("${assets_index_name}", indexId); // [FIX] Placeholder correcto para 1.8.9
                    mcArgs = mcArgs.replace("${asset_index}", indexId);
                    mcArgs = mcArgs.replace("${auth_uuid}", uuid);
                    mcArgs = mcArgs.replace("${auth_access_token}", token);
                    mcArgs = mcArgs.replace("${user_properties}", "{}");
                    mcArgs = mcArgs.replace("${user_type}", userType);
                    
                    // [FIX] Usar parser inteligente para argumentos con espacios (Rutas con espacios)
                    for (String arg : splitArgs(mcArgs)) {
                        command.add(arg);
                    }
                } else {
                    // Versión Moderna (1.13+) que usa una lista de argumentos
                    command.add("--username"); command.add(username);
                    command.add("--version"); command.add(version);
                    command.add("--gameDir"); command.add(DATA_DIR);
                    command.add("--assetsDir"); command.add(new File(DATA_DIR, "assets").getAbsolutePath());
                    command.add("--assetIndex"); command.add(versionJson.has("assetIndex") ? versionJson.getAsJsonObject("assetIndex").get("id").getAsString() : version);
                    command.add("--uuid"); command.add(uuid);
                    command.add("--accessToken"); command.add(token);
                    command.add("--userType"); command.add(userType);
                    command.add("--versionType"); command.add(versionJson.has("type") ? versionJson.get("type").getAsString() : "release");
                }

                // Resolución
                if (settings.has("width")) { command.add("--width"); command.add(settings.get("width").getAsString()); }
                if (settings.has("height")) { command.add("--height"); command.add(settings.get("height").getAsString()); }
                if (settings.has("fullscreen") && settings.get("fullscreen").getAsBoolean()) command.add("--fullscreen");

                devConsoleOutput.appendText("Comando: " + String.join(" ", command) + "\n\n");

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(DATA_DIR));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // --- Acción al lanzar ---
                String onLaunch = settings.has("onLaunch") ? settings.get("onLaunch").getAsString() : "No hacer nada";
                Platform.runLater(() -> {
                    if (onLaunch.equals("Ocultar el launcher")) {
                        ((Stage) root.getScene().getWindow()).setIconified(true);
                    } else if (onLaunch.equals("Cerrar el launcher")) {
                        System.exit(0);
                    }
                });

                // Leer salida del proceso (logs)
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String logLine = line;
                    Platform.runLater(() -> devConsoleOutput.appendText(logLine + "\n"));
                }

                int exitCode = process.waitFor();
                Platform.runLater(() -> {
                    devConsoleOutput.appendText("\nProceso finalizado con código de salida: " + exitCode + "\n");
                    if (onLaunch.equals("Ocultar el launcher")) {
                        Stage stage = ((Stage) root.getScene().getWindow());
                        if (stage.isIconified()) {
                            stage.setIconified(false);
                        }
                        stage.toFront();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    devConsoleOutput.appendText("\n--- ERROR DE LANZAMIENTO ---\n");
                    devConsoleOutput.appendText(e.toString() + "\n");
                    for (StackTraceElement ste : e.getStackTrace()) {
                        devConsoleOutput.appendText("    at " + ste.toString() + "\n");
                    }
                });
            }
        }).start();
    }

    private void repairGame(String version) {
        MainView.showNotification("Reparación", "Iniciando verificación de archivos...", "info");
        devConsole.setVisible(true);
        devConsole.setManaged(true);
        devConsoleOutput.clear();
        devConsoleOutput.appendText("Iniciando reparación de " + version + "...\n");

        new Thread(() -> {
            try {
                File versionFolder = new File(VERSIONS_DIR, version);
                File jsonFile = new File(versionFolder, version + ".json");

                // 1. Cargar JSON
                JsonObject versionJson = null;
                if (jsonFile.exists()) {
                    try {
                        versionJson = gson.fromJson(new FileReader(jsonFile), JsonObject.class);
                    } catch (Exception ex) { }
                }

                if (versionJson == null) {
                    Platform.runLater(() -> MainView.showNotification("Error Fatal", "El JSON de la versión no existe.", "error"));
                    return;
                }

                // 2. Lógica de Herencia (Igual que en launchGame)
                if (versionJson.has("inheritsFrom")) {
                    String parentId = versionJson.get("inheritsFrom").getAsString();
                    File parentJsonFile = new File(VERSIONS_DIR, parentId + File.separator + parentId + ".json");
                    if (parentJsonFile.exists()) {
                        try {
                            JsonObject parentJson = gson.fromJson(new FileReader(parentJsonFile), JsonObject.class);
                            if (parentJson.has("libraries")) {
                                JsonArray parentLibs = parentJson.getAsJsonArray("libraries");
                                JsonArray currentLibs = versionJson.has("libraries") ? versionJson.getAsJsonArray("libraries") : new JsonArray();
                                parentLibs.addAll(currentLibs); 
                                versionJson.add("libraries", parentLibs);
                            }
                        } catch (Exception e) {
                            devConsoleOutput.appendText("Advertencia: No se pudo leer la versión padre " + parentId + "\n");
                        }
                    }
                }

                // 3. Descargar Assets
                if (versionJson.has("assetIndex")) {
                    JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
                    String id = assetIndex.get("id").getAsString();
                    String url = assetIndex.get("url").getAsString();
                    File assetsIndexDir = new File(DATA_DIR, "assets/indexes");
                    File indexFile = new File(assetsIndexDir, id + ".json");
                    if (!indexFile.exists()) downloadFile(url, indexFile);
                    
                    downloadAssets(indexFile);
                }

                // 4. Descargar Librerías
                File libsDir = new File(DATA_DIR, "libraries");
                Platform.runLater(() -> devConsoleOutput.appendText("Verificando librerías...\n"));
                downloadLibraries(versionJson, libsDir);

                // 5. Extraer Natives (Reparar DLLs corruptas)
                File nativesDir = new File(versionFolder, "natives");
                extractNatives(versionJson, libsDir, nativesDir);

                Platform.runLater(() -> {
                    devConsoleOutput.appendText("\n--- REPARACIÓN COMPLETADA ---\n");
                    MainView.showNotification("Éxito", "Archivos verificados y reparados.", "success");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> devConsoleOutput.appendText("Error en reparación: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    // [FIX] Método para dividir argumentos respetando comillas (Estilo TLauncher)
    // Esto evita errores si tu carpeta de usuario tiene espacios (ej. "Juan Perez")
    private List<String> splitArgs(String args) {
        List<String> result = new ArrayList<>();
        // Regex: Captura texto sin espacios O texto entre comillas
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(args);
        while (m.find()) {
            String match = m.group(1);
            // Eliminar comillas si las tiene
            if (match.startsWith("\"") && match.endsWith("\"")) {
                match = match.substring(1, match.length() - 1);
            }
            result.add(match);
        }
        return result;
    }

    private void downloadAssets(File indexFile) {
        try {
            JsonObject json = gson.fromJson(new FileReader(indexFile), JsonObject.class);
            
            // [FIX] Detectar modo "virtual" (Legacy 1.6.4 - 1.7.2) para arreglar sonido
            boolean isVirtual = false;
            if (json.has("virtual") && json.get("virtual").getAsBoolean()) {
                isVirtual = true;
            }

            if (json.has("objects")) {
                JsonObject objects = json.getAsJsonObject("objects");
                File objectsDir = new File(DATA_DIR, "assets/objects");
                File virtualDir = new File(DATA_DIR, "assets/virtual/legacy"); // Ruta para legacy
                
                int total = objects.entrySet().size();
                Platform.runLater(() -> devConsoleOutput.appendText("Verificando " + total + " assets (sonidos/texturas)...\n"));

                for (java.util.Map.Entry<String, JsonElement> entry : objects.entrySet()) {
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    String hash = obj.get("hash").getAsString();
                    String prefix = hash.substring(0, 2);
                    File assetFile = new File(objectsDir, prefix + File.separator + hash);
                    
                    if (!assetFile.exists()) {
                        String url = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
                        downloadFile(url, assetFile);
                    }
                    
                    // [FIX] Copiar a virtual/legacy si es necesario (Fix Sonido en versiones antiguas)
                    if (isVirtual) {
                        String assetPath = entry.getKey();
                        File virtualFile = new File(virtualDir, assetPath);
                        if (!virtualFile.exists()) {
                            virtualFile.getParentFile().mkdirs();
                            copyFile(assetFile, virtualFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> devConsoleOutput.appendText("Error verificando assets: " + e.getMessage() + "\n"));
        }
    }

    private void copyFile(File source, File dest) {
        try (FileInputStream is = new FileInputStream(source);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (Exception e) { }
    }

    private List<File> downloadLibraries(JsonObject versionJson, File libsDir) {
        List<File> files = new ArrayList<>();
        if (!versionJson.has("libraries")) return files;

        JsonArray libs = versionJson.getAsJsonArray("libraries");
        for (JsonElement el : libs) {
            JsonObject lib = el.getAsJsonObject();
            
            // Verificar reglas (OS) para no descargar basura de Mac/Linux
            if (!isLibraryAllowed(lib)) continue;

            String path = null;
            String url = null;

            if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("artifact")) {
                // Formato Mojang
                JsonObject artifact = lib.getAsJsonObject("downloads").getAsJsonObject("artifact");
                path = artifact.get("path").getAsString();
                url = artifact.get("url").getAsString();
            } else if (lib.has("name") && !lib.has("natives")) {
                // Formato Legacy/Prism (para Forge antiguo), ignorando natives
                String name = lib.get("name").getAsString();
                String baseUrl = lib.has("url") ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
                if (!baseUrl.endsWith("/")) baseUrl += "/"; // [FIX] Asegurar URL válida
                String[] parts = name.split(":");
                String group = parts[0].replace('.', '/');
                String artifactId = parts[1];
                String version = parts[2];
                path = String.format("%s/%s/%s/%s-%s.jar", group, artifactId, version, artifactId, version);
                url = baseUrl + path;
            }

            if (path != null && url != null) {
                File libFile = new File(libsDir, path);
                if (libFile.exists() && libFile.length() == 0) libFile.delete(); // Borrar si está corrupto (0 bytes)
                if (!libFile.exists()) {
                    // [FIX] Intentar descarga con fallback para Forge (Universal)
                    boolean success = downloadFile(url, libFile);
                    if (!success && url.contains("minecraftforge") && !url.contains("universal")) {
                        // Intentar con clasificador universal si falla el normal
                        String universalUrl = url.replace(".jar", "-universal.jar");
                        Platform.runLater(() -> devConsoleOutput.appendText("Reintentando Forge con classifier universal...\n"));
                        downloadFile(universalUrl, libFile);
                    }
                }
                files.add(libFile);
            }
        }
        return files;
    }

    private boolean isLibraryAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        JsonArray rules = lib.getAsJsonArray("rules");
        boolean allowed = false; // Si hay reglas, por defecto es denegar a menos que se permita
        String currentOS = getOS();
        
        for (JsonElement e : rules) {
            JsonObject rule = e.getAsJsonObject();
            String action = rule.get("action").getAsString();
            
            if (rule.has("os")) {
                String osName = rule.getAsJsonObject("os").get("name").getAsString();
                if (currentOS.equals(osName)) allowed = "allow".equals(action);
            } else {
                allowed = "allow".equals(action);
            }
        }
        return allowed;
    }

    private void extractNatives(JsonObject versionJson, File libsDir, File nativesDir) {
        // [FIX] Limpiar natives anteriores para asegurar que son los de esta versión
        if (nativesDir.exists()) {
            File[] files = nativesDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
        if (!nativesDir.exists()) nativesDir.mkdirs();
        if (!versionJson.has("libraries")) return;

        JsonArray libs = versionJson.getAsJsonArray("libraries");
        for (JsonElement el : libs) {
            JsonObject lib = el.getAsJsonObject();
            
            if (!isLibraryAllowed(lib)) continue;

            String path = null;
            String url = null;
            String os = getOS();
            String nativeClassifier = "natives-" + os;

            if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("classifiers")) {
                // Formato Mojang
                JsonObject classifiers = lib.getAsJsonObject("downloads").getAsJsonObject("classifiers");
                if (classifiers.has(nativeClassifier)) {
                    JsonObject artifact = classifiers.getAsJsonObject(nativeClassifier);
                    path = artifact.get("path").getAsString();
                    url = artifact.get("url").getAsString();
                }
            } else if (lib.has("natives") && lib.getAsJsonObject("natives").has(os)) {
                // Formato Legacy/Prism
                String name = lib.get("name").getAsString();
                String classifier = lib.getAsJsonObject("natives").get(os).getAsString();
                String baseUrl = lib.has("url") ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
                if (!baseUrl.endsWith("/")) baseUrl += "/"; // [FIX] Asegurar URL válida

                String[] parts = name.split(":");
                String group = parts[0].replace('.', '/');
                String artifactId = parts[1];
                String version = parts[2];
                path = String.format("%s/%s/%s/%s-%s-%s.jar", group, artifactId, version, artifactId, version, classifier);
                url = baseUrl + path;
            }

            if (path != null && url != null) {
                File nativeJar = new File(libsDir, path);
                if (nativeJar.exists() && nativeJar.length() == 0) nativeJar.delete(); // Borrar si está corrupto
                if (!nativeJar.exists()) downloadFile(url, nativeJar);
                // Descomprimir DLLs
                unzipNatives(nativeJar, nativesDir);
            }
        }
    }

    private void unzipNatives(File jarFile, File destDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // [FIX] Soporte robusto para Windows (.dll), Linux (.so) y macOS (.dylib / .jnilib)
                if ((name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib") || name.endsWith(".jnilib")) && !name.contains("/")) {
                    File outFile = new File(destDir, entry.getName());
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            System.out.println("Error extrayendo natives de " + jarFile.getName());
        }
    }

    // [FIX] Detector de Sistema Operativo para Minecraft
    private String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        if (os.contains("nux") || os.contains("nix") || os.contains("aix")) return "linux";
        return "unknown";
    }

    private boolean downloadFile(String urlStr, File dest) {
        try {
            dest.getParentFile().mkdirs();
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // [FIX] User-Agent real para evitar bloqueo de Mojang (Sonidos/Librerias)
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] data = new byte[1024];
                int count;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, count);
                }
            }
            return true;
        } catch (Exception e) {
            final String errorMsg = "Error descargando: " + urlStr + " -> " + e.getMessage();
            System.out.println(errorMsg);
            Platform.runLater(() -> {
                devConsoleOutput.appendText("\n[ERROR] Fallo la descarga de: " + urlStr + "\n");
                devConsoleOutput.appendText("Causa: " + e.getClass().getSimpleName() + " - " + e.getMessage() + "\n");
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                devConsoleOutput.appendText(sw.toString() + "\n");
            });
            return false;
        }
    }

    private JsonObject loadSettings() {
        if (SETTINGS_FILE.exists()) {
            try (FileReader reader = new FileReader(SETTINGS_FILE)) {
                return gson.fromJson(reader, JsonObject.class);
            } catch (Exception e) {}
        }
        return new JsonObject();
    }
    
    private JsonObject loadSession() {
        if (SESSION_FILE.exists()) {
            try (FileReader reader = new FileReader(SESSION_FILE)) {
                return gson.fromJson(reader, JsonObject.class);
            } catch (Exception e) {}
        }
        return new JsonObject();
    }

    private void updateUserInfo() {
        if (SESSION_FILE.exists()) {
            long currentModified = SESSION_FILE.lastModified();
            if (currentModified > lastSessionModified) {
                lastSessionModified = currentModified;
                JsonObject session = loadSession();
                String name = session.has("username") ? session.get("username").getAsString() : "Invitado";
                String avatarPath = session.has("avatar_path") && !session.get("avatar_path").isJsonNull() ? session.get("avatar_path").getAsString() : null;
                
                Platform.runLater(() -> {
                    lblUser.setText(name);
                    if (avatarPath != null && new File(avatarPath).exists()) {
                        // Cargar avatar personalizado local
                        Image customAvatar = new Image(new File(avatarPath).toURI().toString(), false);
                        avatar.setFill(new ImagePattern(customAvatar));
                    } else if (!"Invitado".equals(name)) {
                        // Cargar cabeza 3D del skin usando el nombre de usuario
                        Image skinHead = new Image("https://minotar.net/cube/" + name + "/64.png", true);
                        avatar.setFill(new ImagePattern(skinHead));
                    } else {
                        avatar.setFill(Color.web("#0078d7"));
                    }
                });
            }
        } else {
            Platform.runLater(() -> { lblUser.setText("Invitado"); avatar.setFill(Color.web("#0078d7")); });
        }
    }

    private Button createSocialButton(String text, String color, String url) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 20; -fx-padding: 5 15;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        btn.setOnAction(e -> {
            try {
                // [FIX] Uso de reflexión para evitar dependencia directa de AWT (incompatible con Android)
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
                desktopClass.getMethod("browse", java.net.URI.class).invoke(desktop, new java.net.URI(url));
            } catch (Exception ex) { 
                System.out.println("No se pudo abrir URL: " + url);
            }
        });
        return btn;
    }
}