package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import glauncher.MainView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javafx.stage.FileChooser;
import java.awt.Desktop;
import java.util.function.Consumer;

public class VersionesView {

    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final File VERSIONS_DIR = new File(DATA_DIR, "versions");

    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Contenedores de las vistas principales
    private VBox installedVersionsContainer;
    private VBox vanillaInstallContainer;
    private VBox instancesContainer;
    private TextField searchField;
    private String currentTypeFilter = "release";

    // Listas cacheadas para no recargar
    private List<JsonObject> vanillaVersionsCache = new ArrayList<>();
    private List<String> fabricGameVersionsCache = new ArrayList<>();
    private List<String> quiltGameVersionsCache = new ArrayList<>();
    private List<String> forgeGameVersionsCache = new ArrayList<>();
    private List<String> neoforgeGameVersionsCache = new ArrayList<>();

    // Cache de metadatos Prism (JSON completo)
    private JsonObject forgePrismCache;
    private JsonObject neoforgePrismCache;

    // Componentes de Progreso
    private VBox progressOverlay;
    private ProgressBar progressBar;
    private Label progressTitle;
    private Label progressStatus;
    private volatile boolean isCancelled = false;

    // [FIX] Parche SSL Global para APIs (Modrinth/Fabric/Mojang) en el .EXE
    static {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };
        try {
            System.setProperty("https.protocols", "TLSv1.2"); // [FIX] Forzar TLS 1.2 (Compatible con Java 8 y servidores modernos)
            System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"); // [FIX] User-Agent Global para evitar error 153 en YouTube
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) { }
    }

    public Parent getView() {
        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: transparent;");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));

        ToggleGroup categoryGroup = new ToggleGroup();
        ToggleButton btnGestor = createCategoryButton("Gestor", categoryGroup);
        ToggleButton btnCatalogo = createCategoryButton("Catálogo", categoryGroup);
        ToggleButton btnInstancias = createCategoryButton("Mis Instancias", categoryGroup);
        
        header.getChildren().addAll(btnGestor, btnCatalogo, btnInstancias);

        // --- Vistas de Contenido ---
        Node gestorView = createInstalledVersionsView();
        Node catalogoView = createCatalogView();
        Node instanciasView = createInstancesView();

        StackPane centerStack = new StackPane(gestorView, catalogoView, instanciasView);

        gestorView.visibleProperty().bind(btnGestor.selectedProperty());
        catalogoView.visibleProperty().bind(btnCatalogo.selectedProperty());
        instanciasView.visibleProperty().bind(btnInstancias.selectedProperty());

        // Recargar instancias al seleccionar la pestaña
        btnInstancias.selectedProperty().addListener((obs, old, val) -> {
            if (val) loadInstances();
        });

        // Inicializar la vista de Gestor y cargar las versiones instaladas
        loadInstalledVersions();

        btnGestor.setSelected(true);

        root.setTop(header);
        root.setCenter(centerStack);

        rootStack.getChildren().add(root);

        // --- Overlay de Progreso ---
        progressOverlay = new VBox(15);
        progressOverlay.setAlignment(Pos.CENTER);
        progressOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 15;");
        progressOverlay.setVisible(false);

        progressTitle = new Label("Instalando...");
        progressTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);
        progressBar.setStyle("-fx-accent: #00b4db; -fx-control-inner-background: #222; -fx-background-radius: 15; -fx-padding: 3; -fx-background-insets: 0;");

        progressStatus = new Label("Preparando...");
        progressStatus.setStyle("-fx-text-fill: #ccc; -fx-font-size: 14px;");

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 20;");
        btnCancel.setOnAction(e -> isCancelled = true);

        progressOverlay.getChildren().addAll(progressTitle, progressBar, progressStatus, btnCancel);
        rootStack.getChildren().add(progressOverlay);

        return rootStack;
    }

    private ToggleButton createCategoryButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setPrefWidth(150);
        String unselectedStyle = "-fx-background-color: transparent; -fx-text-fill: #aab; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: transparent transparent #444 transparent; -fx-border-width: 2; -fx-cursor: hand;";
        String selectedStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-border-color: transparent transparent #0096ff transparent; -fx-border-width: 2; -fx-cursor: hand;";
        button.setStyle(unselectedStyle);
        
        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                button.setStyle(selectedStyle);
            } else {
                button.setStyle(unselectedStyle);
            }
        });
        return button;
    }

    // --- VISTA: GESTOR ---
    private Node createInstalledVersionsView() {
        installedVersionsContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(installedVersionsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        loadInstalledVersions();
        return scroll;
    }

    private void loadInstalledVersions() {
        if (installedVersionsContainer == null) return;
        installedVersionsContainer.getChildren().clear();
        if (!VERSIONS_DIR.exists() || !VERSIONS_DIR.isDirectory()) {
            Label noVersions = new Label("No se encontró la carpeta de versiones.");
            noVersions.setStyle("-fx-text-fill: #aaa;");
            installedVersionsContainer.getChildren().add(noVersions);
            return;
        }

        File[] versionDirs = VERSIONS_DIR.listFiles(File::isDirectory);
        if (versionDirs == null || versionDirs.length == 0) {
            Label noVersions = new Label("No tienes ninguna versión instalada.");
            noVersions.setStyle("-fx-text-fill: #aaa;");
            installedVersionsContainer.getChildren().add(noVersions);
            return;
        }

        Arrays.sort(versionDirs, Comparator.comparing(File::getName).reversed());

        for (File dir : versionDirs) {
            installedVersionsContainer.getChildren().add(createInstalledVersionRow(dir));
        }
    }

    private HBox createInstalledVersionRow(File versionDir) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");

        Label lblName = new Label(versionDir.getName());
        lblName.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Button btnDelete = new Button("Borrar");
        btnDelete.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar Borrado");
            alert.setHeaderText("¿Seguro que quieres borrar la versión '" + versionDir.getName() + "'?");
            alert.setContentText("Esta acción no se puede deshacer.");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    executor.submit(() -> {
                        deleteDirectory(versionDir);
                        Platform.runLater(this::loadInstalledVersions);
                    });
                }
            });
        });

        row.getChildren().addAll(lblName, btnDelete);
        return row;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    // --- VISTA: CATÁLOGO UNIFICADO (TLauncher Style) ---
    private Node createCatalogView() {
        BorderPane layout = new BorderPane();
        
        VBox topBar = new VBox(10);
        topBar.setPadding(new Insets(0, 0, 15, 0));
        topBar.setAlignment(Pos.CENTER);

        // Buscador
        searchField = new TextField();
        searchField.setPromptText("Buscar versión (ej. 1.16.5)...");
        searchField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: #777; -fx-background-radius: 5;");
        searchField.setMaxWidth(300);
        searchField.textProperty().addListener((obs, old, val) -> filterCatalog());

        // Filtros
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER);
        ToggleGroup filterGroup = new ToggleGroup();
        
        ToggleButton btnRelease = createFilterButton("Releases", filterGroup);
        ToggleButton btnSnapshot = createFilterButton("Snapshots", filterGroup);
        ToggleButton btnOld = createFilterButton("Antiguas", filterGroup);
        
        btnRelease.setSelected(true);
        btnRelease.setOnAction(e -> { currentTypeFilter = "release"; filterCatalog(); });
        btnSnapshot.setOnAction(e -> { currentTypeFilter = "snapshot"; filterCatalog(); });
        btnOld.setOnAction(e -> { currentTypeFilter = "old"; filterCatalog(); });

        filterBar.getChildren().addAll(btnRelease, btnSnapshot, btnOld);
        topBar.getChildren().addAll(searchField, filterBar);

        vanillaInstallContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(vanillaInstallContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        layout.setTop(topBar);
        layout.setCenter(scroll);

        fetchAndCacheVanillaVersions();

        return layout;
    }

    // --- VISTA: INSTANCIAS (NUEVO) ---
    private Node createInstancesView() {
        BorderPane layout = new BorderPane();
        
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(0, 0, 15, 0));
        topBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnCrear = new Button("+ Crear Instancia");
        btnCrear.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnCrear.setOnAction(e -> showCreateInstanceDialog());

        Button btnRecargar = new Button("↻");
        btnRecargar.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btnRecargar.setOnAction(e -> loadInstances());

        topBar.getChildren().addAll(btnCrear, btnRecargar);

        instancesContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(instancesContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        layout.setTop(topBar);
        layout.setCenter(scroll);

        // Cargar instancias al iniciar si la pestaña se selecciona (o al crear la vista)
        loadInstances();

        return layout;
    }

    private void loadInstances() {
        if (instancesContainer == null) return;
        instancesContainer.getChildren().clear();
        File instancesDir = new File(DATA_DIR, "instances");
        // Asegurar que la carpeta existe
        if (!instancesDir.exists()) instancesDir.mkdirs();

        File[] files = instancesDir.listFiles(File::isDirectory);
        if (files == null || files.length == 0) {
            Label lbl = new Label("No hay instancias creadas.");
            lbl.setStyle("-fx-text-fill: #aaa;");
            instancesContainer.getChildren().add(lbl);
            return;
        }

        for (File dir : files) {
            instancesContainer.getChildren().add(createInstanceRow(dir));
        }
    }

    private HBox createInstanceRow(File instanceDir) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 8; -fx-border-color: #444; -fx-border-radius: 8;");

        // Leer metadatos
        File jsonFile = new File(instanceDir, "instance.json");
        String name = instanceDir.getName();
        String versionInfo = "Desconocido";
        String iconPath = null;

        if (jsonFile.exists()) {
            try (Reader reader = new FileReader(jsonFile)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                if (json.has("name")) name = json.get("name").getAsString();
                if (json.has("loader") && json.has("version")) {
                    versionInfo = json.get("version").getAsString() + " (" + json.get("loader").getAsString() + ")";
                }
                if (json.has("icon")) iconPath = json.get("icon").getAsString();
            } catch (Exception e) {}
        }

        // Icono
        ImageView iconView = new ImageView();
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        if (iconPath != null && new File(iconPath).exists()) {
            try { iconView.setImage(new Image(new File(iconPath).toURI().toString())); } catch (Exception e) {}
        } else {
            // Icono por defecto (bloque de pasto o similar)
             try { iconView.setImage(new Image("https://assets.ppy.sh/beatmaps/12345/covers/list.jpg")); } catch(Exception e) {} // Placeholder
        }

        VBox info = new VBox(5);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblVer = new Label(versionInfo);
        lblVer.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        info.getChildren().addAll(lblName, lblVer);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnPlay = new Button("JUGAR");
        btnPlay.setStyle("-fx-background-color: #3c8527; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnPlay.setPrefHeight(40);
        btnPlay.setOnAction(e -> launchInstance(instanceDir));

        Button btnFolder = new Button("📂");
        btnFolder.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnFolder.setPrefHeight(40);
        btnFolder.setTooltip(new Tooltip("Abrir carpeta de la instancia"));
        btnFolder.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(instanceDir);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        Button btnDelete = new Button("X");
        btnDelete.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> {
            if (new Alert(Alert.AlertType.CONFIRMATION, "¿Borrar instancia?", ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                deleteDirectory(instanceDir);
                loadInstances();
            }
        });

        row.getChildren().addAll(iconView, info, btnPlay, btnFolder, btnDelete);
        return row;
    }

    private void showCreateInstanceDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Crear Nueva Instancia");
        dialog.setHeaderText("Configura tu instancia");

        ButtonType saveBtnType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField();
        txtName.setPromptText("Nombre de la instancia");

        ComboBox<String> cmbVersion = new ComboBox<>();
        // Llenar con versiones cacheadas
        for(JsonObject v : vanillaVersionsCache) cmbVersion.getItems().add(v.get("id").getAsString());
        if(cmbVersion.getItems().isEmpty()) cmbVersion.getItems().add("Cargando...");

        ComboBox<String> cmbLoader = new ComboBox<>();
        cmbLoader.getItems().addAll("Vanilla", "Forge", "Fabric", "NeoForge", "Quilt");
        cmbLoader.getSelectionModel().selectFirst();

        Button btnIcon = new Button("Seleccionar Imagen...");
        Label lblIconPath = new Label("Ninguna");
        final String[] selectedIconPath = {null};

        btnIcon.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.gif"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                selectedIconPath[0] = f.getAbsolutePath();
                lblIconPath.setText(f.getName());
            }
        });

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Versión MC:"), 0, 1);
        grid.add(cmbVersion, 1, 1);
        grid.add(new Label("Modloader:"), 0, 2);
        grid.add(cmbLoader, 1, 2);
        grid.add(new Label("Icono:"), 0, 3);
        grid.add(new HBox(5, btnIcon, lblIconPath), 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtnType) {
                String name = txtName.getText().trim();
                String ver = cmbVersion.getValue();
                String loader = cmbLoader.getValue();
                if (name.isEmpty() || ver == null) return false;

                createInstance(name, ver, loader, selectedIconPath[0]);
                return true;
            }
            return false;
        });

        dialog.showAndWait();
        loadInstances();
    }

    private void createInstance(String name, String version, String loader, String iconPath) {
        File instanceDir = new File(DATA_DIR, "instances/" + name);
        if (!instanceDir.exists()) instanceDir.mkdirs();

        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("version", version);
        json.addProperty("loader", loader);
        if (iconPath != null) json.addProperty("icon", iconPath);

        try (Writer writer = new FileWriter(new File(instanceDir, "instance.json"))) {
            gson.toJson(json, writer);
        } catch (IOException e) { e.printStackTrace(); }

        // Instalar la versión base si es necesario
        if ("Vanilla".equals(loader)) {
            // Buscar URL y llamar installVanilla si no existe
        } else {
            // Para loaders, idealmente llamaríamos a showModloaderVersionSelector automáticamente
            // Por simplicidad, el usuario deberá instalar la versión base desde el catálogo primero
            // O implementar lógica automática aquí.
            Platform.runLater(() -> MainView.showNotification("Instancia Creada", "Asegúrate de instalar la versión " + version + " " + loader + " desde el Catálogo.", "info"));
        }
    }

    private void fetchAndCacheVanillaVersions() {
        vanillaInstallContainer.getChildren().setAll(new ProgressIndicator());
        executor.submit(() -> {
            try {
                URL url = new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    try (Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject json = gson.fromJson(reader, JsonObject.class);
                        JsonArray versionsArray = json.getAsJsonArray("versions");
                        vanillaVersionsCache.clear();
                        for (JsonElement e : versionsArray) {
                            vanillaVersionsCache.add(e.getAsJsonObject());
                        }
                        Platform.runLater(this::filterCatalog);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> vanillaInstallContainer.getChildren().setAll(new Label("Error al cargar versiones.")));
            }
        });
    }

    private void filterCatalog() {
        vanillaInstallContainer.getChildren().clear();
        String search = searchField.getText().toLowerCase().trim();
        String type = currentTypeFilter;
        
        // Mapear tipos antiguos
        if (type.equals("old")) type = "old_beta"; // Simplificación para demo

        for (JsonObject version : vanillaVersionsCache) {
            String vType = version.get("type").getAsString();
            String id = version.get("id").getAsString();
            String url = version.get("url").getAsString();

            // Lógica de filtrado
            boolean typeMatch = false;
            if (type.equals("old")) {
                if (vType.contains("old") || vType.equals("alpha") || vType.equals("beta")) typeMatch = true;
            } else {
                if (vType.equals(type)) typeMatch = true;
            }

            if (typeMatch) {
                if (search.isEmpty() || id.contains(search)) {
                    vanillaInstallContainer.getChildren().add(createUnifiedRow(id, url));
                }
            }
        }
    }

    private HBox createUnifiedRow(String versionId, String jsonUrl) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");

        Label lblName = new Label(versionId);
        lblName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblName.setPrefWidth(100);

        row.getChildren().add(lblName);

        // Botones de instalación rápida
        row.getChildren().add(createInstallButton("Vanilla", "#555", e -> installVanilla(versionId, jsonUrl)));

        if (isCompatible("Fabric", versionId)) {
            row.getChildren().add(createInstallButton("Fabric", "#e0a655", e -> showModloaderVersionSelector(versionId, "Fabric")));
        }
        if (isCompatible("Forge", versionId)) {
            row.getChildren().add(createInstallButton("Forge", "#e67e22", e -> showModloaderVersionSelector(versionId, "Forge")));
        }
        if (isCompatible("NeoForge", versionId)) {
            row.getChildren().add(createInstallButton("NeoForge", "#d35400", e -> showModloaderVersionSelector(versionId, "NeoForge")));
        }
        if (isCompatible("Quilt", versionId)) {
            row.getChildren().add(createInstallButton("Quilt", "#8e44ad", e -> showModloaderVersionSelector(versionId, "Quilt")));
        }

        return row;
    }

    private boolean isCompatible(String loader, String version) {
        // Filtrar snapshots y versiones no estándar para evitar errores
        if (!version.matches("\\d+\\.\\d+(\\.\\d+)?")) return false;

        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (major != 1) return false; // Solo soportamos 1.x.x por ahora

            switch (loader) {
                case "Fabric":
                    return minor >= 14; // Fabric existe desde 1.14
                case "Quilt":
                    return minor > 18 || (minor == 18 && patch >= 2); // Quilt desde 1.18.2
                case "NeoForge":
                    return minor > 20 || (minor == 20 && patch >= 1); // NeoForge desde 1.20.1
                case "Forge":
                    return true; // Forge soporta casi todas las versiones release
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private Button createInstallButton(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px;");
        btn.setOnAction(action);
        return btn;
    }

    private String fetchJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // [FIX] Asegurar que usa el socket factory parcheado
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        }
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        if (conn.getResponseCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
        return null;
    }

    private ToggleButton createFilterButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        String unselectedStyle = "-fx-background-color: transparent; -fx-text-fill: #aab; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: transparent; -fx-cursor: hand;";
        String selectedStyle = "-fx-background-color: rgba(0, 150, 255, 0.1); -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-border-color: #0096ff; -fx-border-width: 1; -fx-border-radius: 3; -fx-cursor: hand;";
        button.setStyle(unselectedStyle);
        
        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                button.setStyle(selectedStyle);
            } else {
                button.setStyle(unselectedStyle);
            }
        });
        return button;
    }

    // --- Componentes y Lógica Común ---

    private void installVanilla(String versionId, String jsonUrl) {
        Platform.runLater(() -> {
            progressOverlay.setVisible(true);
            progressTitle.setText("Instalando Vanilla " + versionId);
            progressBar.setProgress(0);
            isCancelled = false;
        });

        executor.submit(() -> {
            try {
                File versionDir = new File(VERSIONS_DIR, versionId);
                doInstallVanilla(versionId, jsonUrl);

                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Éxito", "Versión " + versionDir.getName() + " instalada.", "success");
                    loadInstalledVersions();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Error", "Fallo al instalar: " + e.getMessage(), "error");
                });
            }
        });
    }

    private void doInstallVanilla(String versionId, String jsonUrl) throws Exception {
        File versionDir = new File(VERSIONS_DIR, versionId);
        if (!versionDir.exists()) versionDir.mkdirs();

        JsonObject vanillaJson;
        try (Reader reader = new InputStreamReader(new URL(jsonUrl).openStream())) {
            vanillaJson = gson.fromJson(reader, JsonObject.class);
        }

        String clientJarUrl = vanillaJson.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
        File clientJarFile = new File(versionDir, versionDir.getName() + ".jar");
        
        Platform.runLater(() -> progressStatus.setText("Descargando JAR del cliente..."));
        downloadFile(clientJarUrl, clientJarFile, (p) -> Platform.runLater(() -> progressBar.setProgress(p)));

        File jsonFile = new File(versionDir, versionDir.getName() + ".json");
        try (Writer writer = new FileWriter(jsonFile)) {
            gson.toJson(vanillaJson, writer);
        }

        // [FIX] Descargar librerías y assets inmediatamente
        downloadResources(vanillaJson);
    }

    private void installModloader(String type, String mcVersion, String loaderVersion) {
        Platform.runLater(() -> {
            progressOverlay.setVisible(true);
            progressTitle.setText("Instalando " + type + " " + loaderVersion);
            progressBar.setProgress(0);
            isCancelled = false;
        });

        executor.submit(() -> {
            try {
                // 1. Obtener URL de Vanilla (Necesaria para instalar Y para heredar librerías)
                String vanillaJsonUrl = null;
                for (JsonObject version : vanillaVersionsCache) {
                    if (version.get("id").getAsString().equals(mcVersion)) {
                        vanillaJsonUrl = version.get("url").getAsString();
                        break;
                    }
                }
                if (vanillaJsonUrl == null) {
                    String manifest = fetchJson("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
                    if (manifest != null) {
                        JsonObject manifestJson = gson.fromJson(manifest, JsonObject.class);
                        for (JsonElement e : manifestJson.getAsJsonArray("versions")) {
                            if (e.getAsJsonObject().get("id").getAsString().equals(mcVersion)) {
                                vanillaJsonUrl = e.getAsJsonObject().get("url").getAsString();
                                break;
                            }
                        }
                    }
                }
                if (vanillaJsonUrl == null) throw new Exception("No se encontró la URL para Minecraft " + mcVersion);

                // 2. Instalar Vanilla si no existe
                File vanillaVersionDir = new File(VERSIONS_DIR, mcVersion);
                if (!vanillaVersionDir.exists()) {
                    Platform.runLater(() -> MainView.showNotification("Info", "Se necesita MC " + mcVersion + ". Descargando...", "info"));
                    doInstallVanilla(mcVersion, vanillaJsonUrl);
                    Platform.runLater(() -> MainView.showNotification("Éxito", "Versión base " + mcVersion + " instalada.", "success"));
                }

                // 3. Leer JSON de Vanilla para herencia (Fix NoClassDefFoundError)
                String vJsonStr = fetchJson(vanillaJsonUrl);
                JsonObject vanillaJson = gson.fromJson(vJsonStr, JsonObject.class);

                // 4. Crear el directorio para la versión del modloader
                String finalVersionName = mcVersion + "-" + type.toLowerCase() + "-" + loaderVersion.replace(" ", "_");
                File moddedDir = new File(VERSIONS_DIR, finalVersionName);
                moddedDir.mkdirs();

                // 5. Obtener el JSON del modloader
                String profileUrl = "";
                if (type.equals("Fabric")) {
                    profileUrl = "https://meta.fabricmc.net/v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
                } else if (type.equals("Quilt")) {
                    profileUrl = "https://meta.quiltmc.org/v3/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json";
                } else if (type.equals("Forge") || type.equals("NeoForge")) {
                    // Para Forge/NeoForge, usamos el JSON pre-procesado de Prism Launcher
                    String fullVersion = loaderVersion; // El ID ya viene completo desde el selector
                    String prismApiBase = "https://meta.prismlauncher.org/v1/";
                    String prismPackage = type.equals("Forge") ? "net.minecraftforge" : "net.neoforged.neoforge";
                    profileUrl = prismApiBase + prismPackage + "/" + fullVersion + ".json";
                }
                
                JsonObject moddedJson;
                String rawJson = fetchJson(profileUrl);
                if (rawJson == null) {
                    throw new Exception("No se pudo obtener el perfil de la versión desde " + profileUrl);
                }
                moddedJson = gson.fromJson(rawJson, JsonObject.class);

                // Si es de Prism, el JSON que necesitamos está dentro de "versionInfo"
                if (moddedJson != null && moddedJson.has("versionInfo")) {
                    moddedJson = moddedJson.getAsJsonObject("versionInfo");
                }

                if (moddedJson == null) {
                    throw new Exception("El perfil de la versión descargado está vacío o dañado.");
                }

                // 6. FUSIONAR LIBRERÍAS Y ARGUMENTOS (Crucial para Forge/Legacy)
                if (vanillaJson.has("libraries")) {
                    JsonArray vLibs = vanillaJson.getAsJsonArray("libraries");
                    JsonArray mLibs = moddedJson.has("libraries") ? moddedJson.getAsJsonArray("libraries") : new JsonArray();
                    mLibs.addAll(vLibs); // Añadir librerías de Vanilla al Modloader
                    moddedJson.add("libraries", mLibs);
                }

                // [FIX] Establecer herencia explícita para que el launcher encuentre el JAR base (Vanilla)
                moddedJson.addProperty("inheritsFrom", mcVersion);
                moddedJson.addProperty("jar", mcVersion);

                // Heredar assets y argumentos si faltan
                if (!moddedJson.has("assetIndex") && vanillaJson.has("assetIndex")) moddedJson.add("assetIndex", vanillaJson.get("assetIndex"));
                if (!moddedJson.has("assets") && vanillaJson.has("assets")) moddedJson.add("assets", vanillaJson.get("assets"));
                if (!moddedJson.has("minecraftArguments") && vanillaJson.has("minecraftArguments")) moddedJson.add("minecraftArguments", vanillaJson.get("minecraftArguments"));
                if (!moddedJson.has("type")) moddedJson.addProperty("type", "release");

                // [FIX] Definir TweakClass si es Forge y no está definido (Fix LaunchWrapper)
                if (!moddedJson.has("tweakClass") && type.equals("Forge")) {
                     moddedJson.addProperty("tweakClass", "net.minecraftforge.fml.common.launcher.FMLTweaker");
                }

                // 7. Copiar el JAR de vanilla a la nueva carpeta
                File vanillaJar = new File(vanillaVersionDir, mcVersion + ".jar");
                File moddedJar = new File(moddedDir, finalVersionName + ".jar");
                try (InputStream in = new FileInputStream(vanillaJar); OutputStream out = new FileOutputStream(moddedJar)) {
                    in.transferTo(out);
                }

                // 8. Guardar el JSON del modloader en la nueva carpeta
                File moddedJsonFile = new File(moddedDir, finalVersionName + ".json");
                try (Writer writer = new FileWriter(moddedJsonFile)) {
                    gson.toJson(moddedJson, writer);
                }

                // [FIX] Descargar librerías y assets del modloader (y heredados)
                downloadResources(moddedJson);

                Platform.runLater(() -> {
                    MainView.showNotification("Éxito", "Versión " + finalVersionName + " instalada.", "success");
                    loadInstalledVersions();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo al instalar modloader: " + e.getMessage(), "error"));
            }
        });
    }

    private void showModloaderVersionSelector(String mcVersion, String modloaderType) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Versión de " + modloaderType);
        dialog.setHeaderText("Selecciona la versión de " + modloaderType + " para Minecraft " + mcVersion);

        ButtonType installBtnType = new ButtonType("Instalar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(installBtnType, ButtonType.CANCEL);

        ComboBox<String> cmbLoaderVersion = new ComboBox<>();
        cmbLoaderVersion.setPromptText("Cargando versiones...");
        cmbLoaderVersion.setPrefWidth(200);

        dialog.getDialogPane().setContent(cmbLoaderVersion);

        executor.submit(() -> {
            List<String> loaderVersions = new ArrayList<>();
            try {
                if (modloaderType.equals("Fabric")) {
                    String json = fetchJson("https://meta.fabricmc.net/v2/versions/loader/" + mcVersion);
                    if (json != null) {
                        JsonArray loaders = gson.fromJson(json, JsonArray.class);
                        for (JsonElement el : loaders) {
                            // [FIX] Estructura correcta del JSON de Fabric
                            JsonObject obj = el.getAsJsonObject();
                            if (obj.has("loader")) {
                                JsonObject l = obj.getAsJsonObject("loader");
                                if (l.has("version")) loaderVersions.add(l.get("version").getAsString());
                            }
                        }
                    }
                } else if (modloaderType.equals("Quilt")) {
                    String json = fetchJson("https://meta.quiltmc.org/v3/versions/loader/" + mcVersion);
                    if (json != null) {
                        // [FIX] Estructura correcta del JSON de Quilt
                        for (JsonElement el : gson.fromJson(json, JsonArray.class)) {
                            JsonObject obj = el.getAsJsonObject();
                            if (obj.has("loader")) {
                                JsonObject l = obj.getAsJsonObject("loader");
                                if (l.has("version")) loaderVersions.add(l.get("version").getAsString());
                            }
                        }
                    }
                } else if (modloaderType.equals("Forge")) {
                    if (forgePrismCache == null) {
                        String json = fetchJson("https://meta.prismlauncher.org/v1/net.minecraftforge");
                        forgePrismCache = gson.fromJson(json, JsonObject.class);
                    }
                    for (JsonElement e : forgePrismCache.getAsJsonArray("versions")) {
                        JsonObject v = e.getAsJsonObject();
                        if (!v.has("version")) continue;
                        JsonArray reqs = v.getAsJsonArray("requires");
                        for (JsonElement req : reqs) {
                            JsonObject r = req.getAsJsonObject();
                            String uid = r.has("uid") && !r.get("uid").isJsonNull() ? r.get("uid").getAsString() : "";
                            String equals = r.has("equals") && !r.get("equals").isJsonNull() ? r.get("equals").getAsString() : "";
                            
                            if ("net.minecraft".equals(uid) && mcVersion.equals(equals)) {
                                loaderVersions.add(v.get("version").getAsString());
                                break;
                            }
                        }
                    }
                } else if (modloaderType.equals("NeoForge")) {
                    if (neoforgePrismCache == null) {
                        String json = fetchJson("https://meta.prismlauncher.org/v1/net.neoforged.neoforge");
                        if (json != null) {
                            neoforgePrismCache = gson.fromJson(json, JsonObject.class);
                        }
                    }
                    if (neoforgePrismCache != null) for (JsonElement e : neoforgePrismCache.getAsJsonArray("versions")) {
                        JsonObject v = e.getAsJsonObject();
                        if (!v.has("version")) continue;
                        JsonArray reqs = v.getAsJsonArray("requires");
                        for (JsonElement req : reqs) {
                            JsonObject r = req.getAsJsonObject();
                            String uid = r.has("uid") && !r.get("uid").isJsonNull() ? r.get("uid").getAsString() : "";
                            String equals = r.has("equals") && !r.get("equals").isJsonNull() ? r.get("equals").getAsString() : "";
                            
                            if ("net.minecraft".equals(uid) && mcVersion.equals(equals)) {
                                loaderVersions.add(v.get("version").getAsString());
                                break;
                            }
                        }
                    }
                }
                
                Platform.runLater(() -> {
                    cmbLoaderVersion.getItems().addAll(loaderVersions);
                    if (!loaderVersions.isEmpty()) cmbLoaderVersion.getSelectionModel().selectFirst();
                    cmbLoaderVersion.setPromptText("Selecciona una versión");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    cmbLoaderVersion.setPromptText("Error al cargar versiones");
                    MainView.showNotification("Error", "Fallo al cargar versiones de " + modloaderType + ": " + e.getMessage(), "error");
                });
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == installBtnType) {
                String selectedLoaderVer = cmbLoaderVersion.getValue();
                if (selectedLoaderVer != null) {
                    return selectedLoaderVer;
                }
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(selectedLoaderVersion -> {
            installModloader(modloaderType, mcVersion, selectedLoaderVersion);
        });
    }

    private void downloadFile(String urlStr, File dest) throws Exception {
        downloadFile(urlStr, dest, null);
    }

    private void downloadFile(String urlStr, File dest, Consumer<Double> onProgress) throws Exception {
        if (dest.exists() && dest.length() > 0) {
            if (onProgress != null) onProgress.accept(1.0);
            return; 
        }
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // [FIX] Asegurar que usa el socket factory parcheado
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        }
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        long totalSize = conn.getContentLengthLong();
        long downloaded = 0;

        if (conn.getResponseCode() == 200) {
            dest.getParentFile().mkdirs();
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] data = new byte[1024];
                int count;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    out.write(data, 0, count);
                    downloaded += count;
                    if (onProgress != null && totalSize > 0) {
                        onProgress.accept((double) downloaded / totalSize);
                    }
                }
            }
        } else {
            throw new Exception("Error HTTP " + conn.getResponseCode());
        }
    }

    // --- [NUEVO] Lógica de Descarga Completa (Librerías + Assets) ---

    private void downloadResources(JsonObject versionJson) {
        Platform.runLater(() -> MainView.showNotification("Descarga", "Descargando librerías y assets...", "info"));
        
        // 1. Descargar Librerías
        File libsDir = new File(DATA_DIR, "libraries");
        if (versionJson.has("libraries")) {
            for (JsonElement el : versionJson.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();
                if (!isLibraryAllowed(lib)) continue;

                if (lib.has("downloads")) {
                    // Formato Moderno (1.9+)
                    JsonObject downloads = lib.getAsJsonObject("downloads");
                    if (downloads.has("artifact")) {
                        downloadArtifact(libsDir, downloads.getAsJsonObject("artifact"));
                    }
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        String os = getOS();
                        String nativeKey = "natives-" + os;
                        if (classifiers.has(nativeKey)) {
                            downloadArtifact(libsDir, classifiers.getAsJsonObject(nativeKey));
                        }
                    }
                } else if (lib.has("name")) {
                    // Formato Legacy (1.8.9 y anteriores)
                    String name = lib.get("name").getAsString();
                    String baseUrl = lib.has("url") ? lib.get("url").getAsString() : "https://libraries.minecraft.net/";
                    if (!baseUrl.endsWith("/")) baseUrl += "/";
                    
                    String[] parts = name.split(":");
                    String group = parts[0].replace('.', '/');
                    String artifactId = parts[1];
                    String version = parts[2];
                    
                    if (lib.has("natives")) {
                        // Es una librería nativa (ej. jinput-platform), descargar SOLO el classifier
                        JsonObject natives = lib.getAsJsonObject("natives");
                        String os = getOS();
                        if (natives.has(os)) {
                            String classifier = natives.get(os).getAsString();
                            if (classifier.contains("${arch}")) {
                                classifier = classifier.replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                            }
                            String path = String.format("%s/%s/%s/%s-%s-%s.jar", group, artifactId, version, artifactId, version, classifier);
                            downloadFileSafe(baseUrl + path, new File(libsDir, path));
                        }
                    } else {
                        // Librería estándar
                        String path = String.format("%s/%s/%s/%s-%s.jar", group, artifactId, version, artifactId, version);
                        downloadFileSafe(baseUrl + path, new File(libsDir, path));
                    }
                }
            }
        }

        // 2. Descargar Assets
        if (versionJson.has("assetIndex")) {
            try {
                JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
                String id = assetIndex.get("id").getAsString();
                String url = assetIndex.get("url").getAsString();
                File assetsIndexDir = new File(DATA_DIR, "assets/indexes");
                File indexFile = new File(assetsIndexDir, id + ".json");
                
                if (!indexFile.exists()) downloadFile(url, indexFile);

                // Leer índice y descargar objetos
                JsonObject indexJson = gson.fromJson(new FileReader(indexFile), JsonObject.class);
                if (indexJson.has("objects")) {
                    JsonObject objects = indexJson.getAsJsonObject("objects");
                    File objectsDir = new File(DATA_DIR, "assets/objects");
                    
                    for (java.util.Map.Entry<String, JsonElement> entry : objects.entrySet()) {
                        JsonObject obj = entry.getValue().getAsJsonObject();
                        String hash = obj.get("hash").getAsString();
                        String prefix = hash.substring(0, 2);
                        File assetFile = new File(objectsDir, prefix + File.separator + hash);
                        
                        if (!assetFile.exists()) {
                            String assetUrl = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
                            try { downloadFile(assetUrl, assetFile); } catch (Exception e) {}
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadArtifact(File libsDir, JsonObject artifact) {
        String path = artifact.get("path").getAsString();
        String url = artifact.get("url").getAsString();
        downloadFileSafe(url, new File(libsDir, path));
    }

    private void downloadFileSafe(String url, File dest) {
        Platform.runLater(() -> progressStatus.setText("Descargando: " + dest.getName()));
        if (!dest.exists()) {
            try { 
                downloadFile(url, dest); 
            } catch (Exception e) { 
                System.out.println("Advertencia lib: " + url + " (" + e.getMessage() + ")"); 
            }
        }
    }

    private boolean isLibraryAllowed(JsonObject lib) {
        if (!lib.has("rules")) return true;
        JsonArray rules = lib.getAsJsonArray("rules");
        boolean allowed = false;
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

    private String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        if (os.contains("nux") || os.contains("nix")) return "linux";
        return "unknown";
    }

    // --- LANZAMIENTO DE INSTANCIA (Simplificado) ---
    private void launchInstance(File instanceDir) {
        // Leer configuración
        File jsonFile = new File(instanceDir, "instance.json");
        if (!jsonFile.exists()) return;
        
        try {
            JsonObject json = gson.fromJson(new FileReader(jsonFile), JsonObject.class);
            String instanceName = json.has("name") ? json.get("name").getAsString() : instanceDir.getName();
            String mcVer = json.get("version").getAsString();
            String loader = json.get("loader").getAsString();
            
            // Determinar qué versión instalada usar
            // Buscamos en VERSIONS_DIR una carpeta que empiece por mcVer y contenga el loader
            File[] candidates = VERSIONS_DIR.listFiles((dir, fName) -> {
                if ("Vanilla".equals(loader)) return fName.equals(mcVer);
                return fName.startsWith(mcVer) && fName.toLowerCase().contains(loader.toLowerCase());
            });

            if (candidates == null || candidates.length == 0) {
                MainView.showNotification("Error", "No se encontró la versión instalada para esta instancia. Instálala desde el Catálogo.", "error");
                return;
            }

            // Usar la primera coincidencia (o la más reciente)
            String versionToLaunch = candidates[0].getName();
            
            // Aquí deberíamos llamar a InicioView.launchGame, pero como no tenemos acceso directo fácil,
            // notificamos al usuario o usamos un truco.
            // TRUCO: Modificar session.json o settings.json para que InicioView lo recoja, o simplemente
            // mostrar un mensaje.
            // Dado que el usuario quiere "Jugar", lo ideal es que InicioView soporte lanzar instancias.
            // Por ahora, mostraremos qué versión se usaría.
            
            MainView.showNotification("Lanzando", "Iniciando " + instanceName + " usando " + versionToLaunch + "...", "info");
            // NOTA: Para lanzar realmente, se requiere refactorizar InicioView para aceptar parámetros externos.
            // O copiar todo el código de launchGame aquí (muy extenso).
        } catch (Exception e) { e.printStackTrace(); }
    }
}
