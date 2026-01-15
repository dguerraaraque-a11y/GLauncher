package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import glauncher.MainView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.util.function.Consumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServersView {

    private final String DATA_DIR = (System.getenv("APPDATA") != null ?
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    private ComboBox<String> typeBox;
    private ComboBox<String> verBox;
    
    private StackPane contentPanel;
    private VBox myServersList;
    private ToggleGroup sidebarGroup;

    private VBox progressOverlay;
    private ProgressBar progressBar;
    private Label progressTitle;
    private Label progressStatus;
    private Button btnCancel;
    
    // Componentes Modrinth
    private StackPane modrinthOverlay;
    private StackPane modDetailsOverlay; // Nuevo panel de detalles
    private VBox modrinthResults;
    private TextField modrinthSearch;
    private ComboBox<String> modrinthVersionBox;
    private ComboBox<String> modrinthLoaderBox;
    private ComboBox<String> modrinthTypeBox; // Nuevo filtro de tipo
    private File currentServerForMods;

    public Parent getView() {
        StackPane rootStack = new StackPane();
        BorderPane rootPane = new BorderPane();
        rootPane.setPadding(new Insets(20));

        // --- BARRA LATERAL FLOTANTE (Izquierda) ---
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: rgba(30, 30, 30, 0.95); -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        sidebar.setAlignment(Pos.TOP_CENTER);

        Label lblTitle = new Label("Servidores");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        sidebarGroup = new ToggleGroup();
        ToggleButton btnMyServers = createSidebarButton("Mis Servidores", sidebarGroup, "assets/icons/icons-gui/server.png");
        ToggleButton btnCreate = createSidebarButton("Crear Server", sidebarGroup, "assets/icons/icons-gui/plus.png");
        ToggleButton btnSearch = createSidebarButton("Buscar Servers", sidebarGroup, "assets/icons/icons-gui/search.png");
        
        btnMyServers.setSelected(true);

        sidebar.getChildren().addAll(lblTitle, new Separator(), btnMyServers, btnCreate, btnSearch);

        // --- PANEL GRANDE (Derecha) ---
        contentPanel = new StackPane();
        contentPanel.setStyle("-fx-background-color: rgba(20, 20, 20, 0.85); -fx-background-radius: 30; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 30; -fx-border-width: 1;");
        contentPanel.setPadding(new Insets(30));

        // Vistas internas
        Node myServersView = createMyServersView();
        Node createView = createCreateServerView();
        Node searchView = createSearchServerView();

        // Lógica de navegación
        btnMyServers.setOnAction(e -> { if(btnMyServers.isSelected()) { refreshMyServersList(); contentPanel.getChildren().setAll(myServersView); } });
        btnCreate.setOnAction(e -> { if(btnCreate.isSelected()) contentPanel.getChildren().setAll(createView); });
        btnSearch.setOnAction(e -> { if(btnSearch.isSelected()) contentPanel.getChildren().setAll(searchView); });

        // Vista inicial
        contentPanel.getChildren().setAll(myServersView);
        refreshMyServersList();

        // Inicializar Overlay de Modrinth
        modrinthOverlay = createModrinthOverlay();
        rootStack.getChildren().add(modrinthOverlay); // Añadir antes del progressOverlay si se quiere debajo, o después
        modDetailsOverlay = createModDetailsOverlay();
        rootStack.getChildren().add(modDetailsOverlay);

        rootPane.setLeft(sidebar);
        rootPane.setCenter(contentPanel);
        BorderPane.setMargin(contentPanel, new Insets(0, 0, 0, 20)); // Espacio entre sidebar y panel

        // --- Overlay de Progreso ---
        progressOverlay = new VBox(15);
        progressOverlay.setAlignment(Pos.CENTER);
        progressOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 15;");
        progressOverlay.setVisible(false);

        progressTitle = new Label("Creando Servidor...");
        progressTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #28a745;");

        progressStatus = new Label("Preparando...");
        progressStatus.setStyle("-fx-text-fill: #ccc; -fx-font-size: 14px;");

        btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> {
            // Lógica de cancelación (a implementar)
            progressOverlay.setVisible(false);
        });

        progressOverlay.getChildren().addAll(progressTitle, progressBar, progressStatus, btnCancel);
        rootStack.getChildren().addAll(rootPane, modrinthOverlay, modDetailsOverlay, progressOverlay); // Asegurar orden

        return rootStack;
    }

    private ToggleButton createSidebarButton(String text, ToggleGroup group, String iconPath) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(loadIcon(iconPath, 18));
        btn.setGraphicTextGap(10);

        String baseStyle = "-fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 15;";
        
        btn.styleProperty().bind(javafx.beans.binding.Bindings.when(btn.selectedProperty())
            .then(baseStyle + "-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold;")
            .otherwise(baseStyle + "-fx-background-color: transparent; -fx-text-fill: #aaa;"));

        return btn;
    }

    private Node createCreateServerView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("Crear Servidor Avanzado");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        // Inputs simulados
        TextField nameField = new TextField(); nameField.setPromptText("Nombre del Servidor");
        nameField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
        
        typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Paper (Optimizado)", "Vanilla", "Fabric", "Forge");
        typeBox.setValue("Paper (Optimizado)");
        typeBox.setStyle("-fx-base: #333; -fx-text-fill: white;");
        typeBox.valueProperty().addListener((obs, old, val) -> updateAvailableVersions(val));

        verBox = new ComboBox<>();
        verBox.setPromptText("Cargando...");
        verBox.setStyle("-fx-base: #333; -fx-text-fill: white;");
        updateAvailableVersions("Paper (Optimizado)"); // Carga inicial

        Slider ramSlider = new Slider(1, 16, 4);
        ramSlider.setBlockIncrement(1);
        ramSlider.setMajorTickUnit(1);
        ramSlider.setMinorTickCount(0);
        ramSlider.setShowTickLabels(true);
        ramSlider.setShowTickMarks(true);
        
        grid.add(new Label("Nombre:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Tipo:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Versión:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 2);
        grid.add(verBox, 1, 2);
        grid.add(new Label("RAM (GB):") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 3);
        grid.add(ramSlider, 1, 3);

        Button btnStart = new Button("🚀 Iniciar Servidor");
        btnStart.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
        btnStart.setOnAction(e -> initiateServerCreation(nameField.getText(), (int) ramSlider.getValue()));

        layout.getChildren().addAll(header, grid, new Separator(), btnStart);
        return layout;
    }

    private Node createMyServersView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_LEFT);
        
        Label header = new Label("Mis Servidores");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        Button btnRefresh = new Button("↻ Actualizar");
        btnRefresh.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshMyServersList());

        myServersList = new VBox(10);
        ScrollPane scroll = new ScrollPane(myServersList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        layout.getChildren().addAll(new HBox(15, header, btnRefresh), scroll);
        return layout;
    }

    private Node createSearchServerView() {
        VBox layout = new VBox(20);
        Label header = new Label("Explorar Servidores");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        Label placeholder = new Label("Próximamente: Lista de servidores públicos.");
        placeholder.setStyle("-fx-text-fill: #aaa;");
        
        layout.getChildren().addAll(header, placeholder);
        return layout;
    }

    // --- GESTIÓN DE SERVIDORES ---

    private void refreshMyServersList() {
        myServersList.getChildren().clear();
        File serversDir = new File(DATA_DIR, "servers");
        if (!serversDir.exists()) serversDir.mkdirs();

        File[] files = serversDir.listFiles(File::isDirectory);
        if (files == null || files.length == 0) {
            Label empty = new Label("No tienes servidores creados.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px;");
            myServersList.getChildren().add(empty);
            return;
        }

        for (File serverDir : files) {
            myServersList.getChildren().add(createServerCard(serverDir));
        }
    }

    private HBox createServerCard(File serverDir) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-cursor: hand;");
        
        // Cargar metadatos
        JsonObject meta = loadServerMetadata(serverDir);
        String name = meta.has("name") ? meta.get("name").getAsString() : serverDir.getName();
        String iconPath = meta.has("icon") ? meta.get("icon").getAsString() : null;
        String type = meta.has("type") ? meta.get("type").getAsString() : "Desconocido";

        ImageView icon = new ImageView();
        icon.setFitWidth(50); icon.setFitHeight(50);
        if (iconPath != null && new File(iconPath).exists()) {
            icon.setImage(new Image(new File(iconPath).toURI().toString()));
        } else {
            // Icono default
            icon.setImage(new Image("https://assets.ppy.sh/beatmaps/12345/covers/list.jpg")); // Placeholder
        }

        VBox info = new VBox(5);
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        Label lblType = new Label(type);
        lblType.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        info.getChildren().addAll(lblName, lblType);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnManage = new Button("Gestionar");
        btnManage.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnManage.setOnAction(e -> openServerDashboard(serverDir));

        card.getChildren().addAll(icon, info, btnManage);
        return card;
    }

    private void openServerDashboard(File serverDir) {
        JsonObject meta = loadServerMetadata(serverDir);
        String serverName = meta.has("name") ? meta.get("name").getAsString() : serverDir.getName();
        String serverType = meta.has("type") ? meta.get("type").getAsString() : "Vanilla";

        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(10));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        Button btnBack = new Button("⬅ Volver");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand;");
        btnBack.setOnAction(e -> { refreshMyServersList(); contentPanel.getChildren().setAll(createMyServersView()); });
        
        Label title = new Label("Gestionando: " + serverName);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnStartServer = new Button("▶ Iniciar Servidor");
        btnStartServer.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnStartServer.setOnAction(e -> startServer(serverDir));

        header.getChildren().addAll(btnBack, title, spacer, btnStartServer);

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: transparent;");

        // --- TAB 1: GENERAL (Editar Info) ---
        VBox generalTab = new VBox(15);
        generalTab.setPadding(new Insets(20));

        TextField txtName = new TextField(serverName);
        txtName.setPromptText("Nombre del Servidor");
        
        // Editor de MOTD con colores
        Label lblMotd = new Label("Descripción (MOTD) - Usa los botones para colores:");
        lblMotd.setStyle("-fx-text-fill: #ccc;");
        TextArea txtMotd = new TextArea();
        Properties props = loadServerProperties(serverDir);
        txtMotd.setText(props.getProperty("motd", "A Minecraft Server"));
        
        FlowPane colorPalette = createColorPalette(txtMotd);

        // --- AJUSTES DE JUEGO (Dificultad, Gamemode, Max Players) ---
        Label lblGameSettings = new Label("Ajustes del Juego");
        lblGameSettings.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        GridPane gameGrid = new GridPane();
        gameGrid.setHgap(15); gameGrid.setVgap(10);

        ComboBox<String> cmbDifficulty = new ComboBox<>();
        cmbDifficulty.getItems().addAll("peaceful", "easy", "normal", "hard");
        cmbDifficulty.setValue(props.getProperty("difficulty", "normal"));
        cmbDifficulty.setStyle("-fx-base: #333; -fx-text-fill: white;");

        ComboBox<String> cmbGamemode = new ComboBox<>();
        cmbGamemode.getItems().addAll("survival", "creative", "adventure", "spectator");
        cmbGamemode.setValue(props.getProperty("gamemode", "survival"));
        cmbGamemode.setStyle("-fx-base: #333; -fx-text-fill: white;");

        TextField txtMaxPlayers = new TextField(props.getProperty("max-players", "20"));
        txtMaxPlayers.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        txtMaxPlayers.setPrefWidth(80);

        CheckBox chkPvp = new CheckBox("PvP");
        chkPvp.setSelected(Boolean.parseBoolean(props.getProperty("pvp", "true")));
        chkPvp.setStyle("-fx-text-fill: white;");

        CheckBox chkCooldown = new CheckBox("Cooldown de Ataque (1.9+)");
        chkCooldown.setSelected(meta.has("attack_cooldown") ? meta.get("attack_cooldown").getAsBoolean() : true);
        chkCooldown.setStyle("-fx-text-fill: white;");

        gameGrid.add(new Label("Dificultad:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 0);
        gameGrid.add(cmbDifficulty, 1, 0);
        gameGrid.add(new Label("Modo de Juego:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 0, 1);
        gameGrid.add(cmbGamemode, 1, 1);
        gameGrid.add(new Label("Máx. Jugadores:") {{ setStyle("-fx-text-fill: #ccc;"); }}, 2, 0);
        gameGrid.add(txtMaxPlayers, 3, 0);
        gameGrid.add(chkPvp, 2, 1);
        gameGrid.add(chkCooldown, 3, 1);

        Button btnSaveGeneral = new Button("Guardar Cambios");
        btnSaveGeneral.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnSaveGeneral.setOnAction(e -> {
            meta.addProperty("name", txtName.getText());
            saveServerMetadata(serverDir, meta);
            
            meta.addProperty("attack_cooldown", chkCooldown.isSelected());
            props.setProperty("motd", txtMotd.getText());
            props.setProperty("difficulty", cmbDifficulty.getValue());
            props.setProperty("gamemode", cmbGamemode.getValue());
            props.setProperty("max-players", txtMaxPlayers.getText());
            props.setProperty("pvp", String.valueOf(chkPvp.isSelected()));
            saveServerProperties(serverDir, props);
            
            MainView.showNotification("Guardado", "Configuración actualizada.", "success");
        });

        generalTab.getChildren().addAll(new Label("Nombre:"), txtName, lblMotd, colorPalette, txtMotd, lblGameSettings, gameGrid, new Separator(), btnSaveGeneral);
        Tab t1 = new Tab("General", generalTab); t1.setClosable(false);

        // --- TAB 2: MODS Y RECURSOS (Unificado) ---
        Tab tModsResources = new Tab("Mods y Recursos", createModsResourcesView(serverDir, serverType));
        tModsResources.setClosable(false);

        // --- TAB 4: JUGADORES (Admins & Blacklist) ---
        VBox playersTab = new VBox(15);
        playersTab.setPadding(new Insets(20));
        
        HBox playerListsBox = new HBox(20);
        
        // Admins (ops.json)
        VBox opsBox = createPlayerListBox(serverDir, "Administradores (OPs)", "ops.json");
        HBox.setHgrow(opsBox, Priority.ALWAYS);
        
        // Blacklist (banned-players.json)
        VBox banBox = createPlayerListBox(serverDir, "Lista Negra (Baneados)", "banned-players.json");
        HBox.setHgrow(banBox, Priority.ALWAYS);
        
        playerListsBox.getChildren().addAll(opsBox, banBox);
        playersTab.getChildren().addAll(playerListsBox);
        Tab t4 = new Tab("Jugadores", playersTab); t4.setClosable(false);
        
        // --- TAB 5: BACKUPS ---
        VBox backupTab = new VBox(15);
        backupTab.setPadding(new Insets(20));
        
        Button btnCreateBackup = new Button("Crear Backup del Servidor");
        btnCreateBackup.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCreateBackup.setOnAction(e -> createBackup(serverDir));
        
        ListView<String> backupList = new ListView<>();
        backupList.setStyle("-fx-control-inner-background: #333;");
        refreshBackups(backupList, serverDir);
        
        backupTab.getChildren().addAll(new Label("Copias de Seguridad") {{ setStyle("-fx-text-fill: white; -fx-font-size: 18px;"); }}, btnCreateBackup, backupList);
        Tab t5 = new Tab("Backups", backupTab); t5.setClosable(false);

        tabs.getTabs().addAll(t1, tModsResources, t4, t5);
        dashboard.getChildren().addAll(header, tabs);
        
        contentPanel.getChildren().setAll(dashboard);
    }

    private Node createModsResourcesView(File serverDir, String serverType) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Contenido Instalado");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnUpdateMods = new Button("Actualizar Mods");
        btnUpdateMods.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnUpdateMods.setOnAction(e -> updateInstalledMods(serverDir, serverType));

        Button btnCompat = new Button("Instalar Compatibilidad");
        btnCompat.setTooltip(new Tooltip("Instala ViaVersion para permitir entrar con otras versiones"));
        btnCompat.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnCompat.setOnAction(e -> installViaVersion(serverDir, serverType));

        Button btnExplore = new Button("Explorar Modrinth");
        btnExplore.setGraphic(loadIcon("assets/icons/icons-gui/search.png", 16));
        btnExplore.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5;");
        btnExplore.setOnAction(e -> openModrinthBrowser(serverDir, serverType));
        
        header.getChildren().addAll(title, spacer, btnUpdateMods, btnCompat, btnExplore);

        TextField searchField = new TextField();
        searchField.setPromptText("Filtrar archivos...");
        searchField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 5;");

        TabPane subTabs = new TabPane();
        subTabs.setStyle("-fx-background-color: transparent;");

        // Mods/Plugins Tab
        boolean isPlugin = serverType.contains("Paper") || serverType.contains("Spigot");
        File modsDir = new File(serverDir, isPlugin ? "plugins" : "mods");
        if (!modsDir.exists()) modsDir.mkdirs();
        ListView<File> modsList = createFileList(modsDir, searchField);
        Tab tabMods = new Tab(isPlugin ? "Plugins" : "Mods", modsList);
        tabMods.setClosable(false);

        // Resource Packs Tab
        File rpDir = new File(serverDir, "resourcepacks");
        if (!rpDir.exists()) rpDir.mkdirs();
        ListView<File> rpList = createFileList(rpDir, searchField);
        Tab tabRP = new Tab("Resource Packs", rpList);
        tabRP.setClosable(false);

        // Shaders Tab
        File shaderDir = new File(serverDir, "shaderpacks");
        if (!shaderDir.exists()) shaderDir.mkdirs();
        ListView<File> shaderList = createFileList(shaderDir, searchField);
        Tab tabShaders = new Tab("Shaders", shaderList);
        tabShaders.setClosable(false);

        subTabs.getTabs().addAll(tabMods, tabRP, tabShaders);
        
        layout.getChildren().addAll(header, searchField, subTabs);
        return layout;
    }

    private ListView<File> createFileList(File dir, TextField searchField) {
        ListView<File> list = new ListView<>();
        list.setStyle("-fx-control-inner-background: #333;");
        list.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    String size = formatSize(item.length());
                    setText(item.getName() + " (" + size + ")");
                    setStyle("-fx-text-fill: white; -fx-background-color: transparent;");
                    
                    ContextMenu cm = new ContextMenu();
                    MenuItem delete = new MenuItem("Eliminar");
                    delete.setOnAction(e -> {
                        item.delete();
                        refreshFileList(list, dir);
                    });
                    cm.getItems().add(delete);
                    setContextMenu(cm);
                }
            }
        });
        
        Runnable refresh = () -> refreshFileList(list, dir, searchField.getText());
        searchField.textProperty().addListener((obs, old, val) -> refresh.run());
        refresh.run();
        
        return list;
    }

    // --- SISTEMA DE BACKUPS ---

    private void createBackup(File serverDir) {
        progressOverlay.setVisible(true);
        progressTitle.setText("Creando Backup...");
        progressBar.setProgress(-1); // Indeterminado
        progressStatus.setText("Comprimiendo archivos...");

        executor.submit(() -> {
            try {
                File backupsDir = new File(serverDir, "backups");
                if (!backupsDir.exists()) backupsDir.mkdirs();

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                File zipFile = new File(backupsDir, "backup_" + timestamp + ".zip");

                Path sourcePath = serverDir.toPath();
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                    Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                        @Override
                        public java.nio.file.FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            // Excluir la carpeta de backups para no hacer backup del backup
                            if (file.toString().contains("backups")) return FileVisitResult.CONTINUE;
                            
                            Path targetFile = sourcePath.relativize(file);
                            zos.putNextEntry(new ZipEntry(targetFile.toString()));
                            Files.copy(file, zos);
                            zos.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Backup", "Copia de seguridad creada exitosamente.", "success");
                    // Refrescar lista si estamos en el dashboard (esto es un poco hacky, idealmente usaríamos un observable)
                    openServerDashboard(serverDir); 
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Error", "Fallo al crear backup: " + e.getMessage(), "error");
                });
            }
        });
    }

    private void refreshBackups(ListView<String> list, File serverDir) {
        list.getItems().clear();
        File backupsDir = new File(serverDir, "backups");
        if (backupsDir.exists()) {
            File[] files = backupsDir.listFiles((d, name) -> name.endsWith(".zip"));
            if (files != null) {
                for (File f : files) {
                    list.getItems().add(f.getName() + " (" + formatSize(f.length()) + ")");
                }
            }
        }
    }

    // --- MODRINTH BROWSER ---

    private StackPane createModrinthOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);");
        overlay.setVisible(false);
        
        VBox container = new VBox(15);
        container.setMaxSize(1000, 750); // Más grande
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #222; -fx-background-radius: 15; -fx-border-color: #444; -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 0);");
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Explorador de Mods (Modrinth)");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("Cerrar");
        closeBtn.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> overlay.setVisible(false));
        
        header.getChildren().addAll(title, spacer, closeBtn);
        
        // Filters
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        
        modrinthSearch = new TextField();
        modrinthSearch.setPromptText("Buscar mods...");
        modrinthSearch.setPrefWidth(300);
        modrinthSearch.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 8;");
        modrinthSearch.setOnAction(e -> searchModrinth());
        
        modrinthVersionBox = new ComboBox<>();
        modrinthVersionBox.setPromptText("Versión");
        modrinthVersionBox.setStyle("-fx-base: #333; -fx-text-fill: white;");
        
        modrinthLoaderBox = new ComboBox<>();
        modrinthLoaderBox.getItems().addAll("fabric", "forge", "neoforge", "quilt", "paper", "spigot", "bukkit");
        modrinthLoaderBox.setPromptText("Loader");
        modrinthLoaderBox.setStyle("-fx-base: #333; -fx-text-fill: white;");
        
        modrinthTypeBox = new ComboBox<>();
        modrinthTypeBox.getItems().addAll("Mods", "Resource Packs", "Shaders");
        modrinthTypeBox.setValue("Mods");
        modrinthTypeBox.setStyle("-fx-base: #333; -fx-text-fill: white;");
        
        Button searchBtn = new Button("🔍 Buscar");
        searchBtn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        searchBtn.setOnAction(e -> searchModrinth());
        
        filters.getChildren().addAll(modrinthSearch, modrinthTypeBox, modrinthVersionBox, modrinthLoaderBox, searchBtn);
        
        // Results
        modrinthResults = new VBox(10);
        ScrollPane scroll = new ScrollPane(modrinthResults);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        container.getChildren().addAll(header, filters, scroll);
        overlay.getChildren().add(container);
        
        return overlay;
    }

    private void openModrinthBrowser(File serverDir, String serverType) {
        this.currentServerForMods = serverDir;
        modrinthOverlay.setVisible(true);
        modrinthOverlay.toFront();
        
        // Pre-fill filters based on server info
        Properties props = loadServerProperties(serverDir); // Reusing existing method logic if possible or just rely on passed type
        // Try to guess version from serverType string or metadata
        // For simplicity, we clear and let user select or use defaults
        
        // Map server type to loader
        String loader = "fabric"; // Default
        if (serverType.toLowerCase().contains("forge")) loader = "forge";
        else if (serverType.toLowerCase().contains("paper") || serverType.toLowerCase().contains("spigot")) loader = "paper";
        else if (serverType.toLowerCase().contains("fabric")) loader = "fabric";
        
        modrinthLoaderBox.setValue(loader);
        
        // Populate versions (simple list for now)
        modrinthVersionBox.getItems().clear();
        modrinthVersionBox.getItems().addAll("1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2");
        modrinthVersionBox.getSelectionModel().selectFirst();
        
        searchModrinth();
    }

    private void searchModrinth() {
        modrinthResults.getChildren().clear();
        modrinthResults.getChildren().add(new Label("Buscando...") {{ setStyle("-fx-text-fill: white;"); }});
        
        String query = modrinthSearch.getText();
        String loader = modrinthLoaderBox.getValue();
        String version = modrinthVersionBox.getValue();
        String type = modrinthTypeBox.getValue();
        
        executor.submit(() -> {
            try {
                String projectType = "mod";
                if (type.equals("Resource Packs")) projectType = "resourcepack";
                else if (type.equals("Shaders")) projectType = "shader";

                // Construir facets para filtrado
                String facets = String.format("[[\"categories:%s\"],[\"versions:%s\"],[\"project_type:%s\"]]", loader, version, projectType);
                
                if (loader.equals("paper") || loader.equals("spigot")) {
                     facets = String.format("[[\"categories:bukkit\"],[\"versions:%s\"]]", version); // Modrinth usa 'bukkit' para plugins
                }
                
                String url = "https://api.modrinth.com/v2/search?query=" + java.net.URLEncoder.encode(query, "UTF-8") + "&facets=" + java.net.URLEncoder.encode(facets, "UTF-8");
                String json = fetchJson(url);
                
                JsonObject response = gson.fromJson(json, JsonObject.class);
                JsonArray hits = response.getAsJsonArray("hits");
                
                Platform.runLater(() -> {
                    modrinthResults.getChildren().clear();
                    if (hits.size() == 0) {
                        modrinthResults.getChildren().add(new Label("No se encontraron resultados.") {{ setStyle("-fx-text-fill: #aaa;"); }});
                        return;
                    }
                    
                    for (JsonElement hit : hits) {
                        modrinthResults.getChildren().add(createModCard(hit.getAsJsonObject()));
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> modrinthResults.getChildren().setAll(new Label("Error: " + e.getMessage()) {{ setStyle("-fx-text-fill: red;"); }}));
            }
        });
    }

    private HBox createModCard(JsonObject mod) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5; -fx-border-color: #444; -fx-border-radius: 5;");
        card.setAlignment(Pos.CENTER_LEFT);
        
        String iconUrl = mod.has("icon_url") && !mod.get("icon_url").isJsonNull() ? mod.get("icon_url").getAsString() : null;
        ImageView icon = new ImageView();
        icon.setFitWidth(50); icon.setFitHeight(50);
        if (iconUrl != null) {
            new Thread(() -> {
                try {
                    Image img = new Image(iconUrl);
                    Platform.runLater(() -> icon.setImage(img));
                } catch (Exception e) {}
            }).start();
        }
        
        VBox info = new VBox(5);
        Label name = new Label(mod.get("title").getAsString());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label desc = new Label(mod.get("description").getAsString());
        desc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        desc.setWrapText(true);
        desc.setMaxWidth(500);
        
        String author = mod.get("author").getAsString();
        int downloads = mod.get("downloads").getAsInt();
        String formattedDownloads = NumberFormat.getNumberInstance(Locale.US).format(downloads);
        
        Label lblMeta = new Label("👤 " + author + "  ⬇ " + formattedDownloads);
        lblMeta.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        
        info.getChildren().addAll(name, lblMeta, desc);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Button btnView = new Button("Ver Detalles");
        btnView.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        btnView.setOnAction(e -> openModDetails(mod));
        
        card.getChildren().addAll(icon, info, btnView);
        return card;
    }

    // --- DETALLES DEL MOD (WIDGET) ---

    private StackPane createModDetailsOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.95);");
        overlay.setVisible(false);
        return overlay;
    }

    private void openModDetails(JsonObject mod) {
        modDetailsOverlay.getChildren().clear();
        modDetailsOverlay.setVisible(true);
        modDetailsOverlay.toFront();

        VBox container = new VBox(20);
        container.setMaxSize(600, 500);
        container.setPadding(new Insets(30));
        container.setStyle("-fx-background-color: #222; -fx-background-radius: 15; -fx-border-color: #555; -fx-border-radius: 15;");
        container.setAlignment(Pos.TOP_CENTER);

        Label title = new Label(mod.get("title").getAsString());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label desc = new Label(mod.get("description").getAsString());
        desc.setStyle("-fx-text-fill: #ccc; -fx-font-size: 14px;");
        desc.setWrapText(true);

        Button btnInstall = new Button("Instalar Versión Compatible");
        btnInstall.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20;");
        btnInstall.setOnAction(e -> {
            installModVersion(mod.get("slug").getAsString(), mod.get("project_type").getAsString());
            modDetailsOverlay.setVisible(false);
        });

        Button btnClose = new Button("Cerrar");
        btnClose.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modDetailsOverlay.setVisible(false));

        container.getChildren().addAll(title, desc, new Separator(), btnInstall, new Region() {{ VBox.setVgrow(this, Priority.ALWAYS); }}, btnClose);
        modDetailsOverlay.getChildren().add(container);
    }

    private void installModVersion(String slug, String projectType) {
        String version = modrinthVersionBox.getValue();
        String loader = modrinthLoaderBox.getValue();
        
        MainView.showNotification("Instalando", "Descargando mod...", "info");
        executor.submit(() -> {
            try {
                // Obtener versiones del proyecto
                String loadersParam = "";
                if (projectType.equals("mod")) {
                    loadersParam = "&loaders=[\"" + (loader.equals("paper") ? "bukkit" : loader) + "\"]";
                }
                
                String verUrl = "https://api.modrinth.com/v2/project/" + slug + "/version?game_versions=[\"" + version + "\"]" + loadersParam;
                String json = fetchJson(verUrl);
                JsonArray versions = gson.fromJson(json, JsonArray.class);
                
                if (versions.size() == 0) {
                    Platform.runLater(() -> MainView.showNotification("Error", "No hay versión compatible para " + version, "error"));
                    return;
                }
                
                // Tomar la primera (más reciente)
                JsonObject targetVer = versions.get(0).getAsJsonObject();
                JsonObject fileObj = targetVer.getAsJsonArray("files").get(0).getAsJsonObject();
                String downloadUrl = fileObj.get("url").getAsString();
                String filename = fileObj.get("filename").getAsString();
                
                // Determinar carpeta destino (mods o plugins)
                String targetFolder = "mods";
                if (projectType.equals("resourcepack")) targetFolder = "resourcepacks";
                else if (projectType.equals("shader")) targetFolder = "shaderpacks";
                else if (loader.equals("paper") || loader.equals("spigot") || loader.equals("bukkit")) targetFolder = "plugins";

                File destDir = new File(currentServerForMods, targetFolder);
                if (!destDir.exists()) destDir.mkdirs();
                
                downloadFile(downloadUrl, new File(destDir, filename), null);
                
                Platform.runLater(() -> MainView.showNotification("Éxito", "Mod instalado: " + filename, "success"));
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo al instalar mod: " + e.getMessage(), "error"));
            }
        });
    }

    private VBox createPlayerListBox(File serverDir, String title, String jsonFileName) {
        VBox box = new VBox(10);
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        ListView<String> list = new ListView<>();
        list.setStyle("-fx-control-inner-background: #333;");
        list.setPrefHeight(200);
        
        File jsonFile = new File(serverDir, jsonFileName);
        Runnable loadList = () -> {
            list.getItems().clear();
            if (jsonFile.exists()) {
                try (FileReader reader = new FileReader(jsonFile)) {
                    JsonArray arr = gson.fromJson(reader, JsonArray.class);
                    if (arr != null) {
                        for (JsonElement e : arr) {
                            if (e.isJsonObject() && e.getAsJsonObject().has("name")) {
                                list.getItems().add(e.getAsJsonObject().get("name").getAsString());
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        };
        loadList.run();
        
        HBox inputRow = new HBox(5);
        TextField txtUser = new TextField();
        txtUser.setPromptText("Usuario");
        txtUser.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        HBox.setHgrow(txtUser, Priority.ALWAYS);
        
        Button btnAdd = new Button("+");
        btnAdd.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnAdd.setOnAction(e -> {
            String user = txtUser.getText().trim();
            if (!user.isEmpty() && !list.getItems().contains(user)) {
                list.getItems().add(user);
                savePlayerList(jsonFile, list.getItems());
                txtUser.clear();
            }
        });
        
        Button btnRemove = new Button("-");
        btnRemove.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
        btnRemove.setOnAction(e -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                list.getItems().remove(selected);
                savePlayerList(jsonFile, list.getItems());
            }
        });
        
        inputRow.getChildren().addAll(txtUser, btnAdd, btnRemove);
        box.getChildren().addAll(lbl, list, inputRow);
        return box;
    }

    private void savePlayerList(File jsonFile, List<String> names) {
        JsonArray arr = new JsonArray();
        for (String name : names) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString());
            obj.addProperty("name", name);
            obj.addProperty("level", 4); // Default OP level
            obj.addProperty("banned", true); // For banned-players context (ignored by ops)
            arr.add(obj);
        }
        try (FileWriter writer = new FileWriter(jsonFile)) { gson.toJson(arr, writer); } catch (Exception e) {}
    }

    private void refreshFileList(ListView<File> list, File dir, String filter) {
        list.getItems().clear();
        if (dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (files != null) {
                for (File f : files) {
                    if (filter == null || filter.isEmpty() || f.getName().toLowerCase().contains(filter.toLowerCase())) {
                        list.getItems().add(f);
                    }
                }
            }
        }
    }

    private FlowPane createColorPalette(TextInputControl target) {
        FlowPane pane = new FlowPane(5, 5);
        pane.setPadding(new Insets(5));
        pane.setStyle("-fx-background-color: #333; -fx-background-radius: 5;");

        String[] colors = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
        };
        String[] formats = {"k", "l", "m", "n", "o", "r"};
        String[] hexColors = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA", "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF", "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
        };

        for (int i = 0; i < colors.length; i++) {
            String code = colors[i];
            Button btn = new Button("§" + code);
            btn.setStyle("-fx-background-color: " + hexColors[i] + "; -fx-text-fill: " + (i == 0 || i == 1 ? "white" : "black") + "; -fx-font-weight: bold; -fx-min-width: 30px;");
            btn.setOnAction(e -> target.appendText("\\u00A7" + code)); // Insertar código unicode escapado para properties
            pane.getChildren().add(btn);
        }
        
        for (String fmt : formats) {
            Button btn = new Button("§" + fmt);
            btn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-min-width: 30px;");
            btn.setOnAction(e -> target.appendText("\\u00A7" + fmt));
            pane.getChildren().add(btn);
        }
        
        return pane;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String getFileHash(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int n = 0;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return String.format("%040x", new BigInteger(1, digest.digest()));
        } catch (Exception e) { return null; }
    }

    private void startServer(File serverDir) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                File bat = new File(serverDir, "start.bat");
                if (bat.exists()) {
                    Runtime.getRuntime().exec("cmd /c start start.bat", null, serverDir);
                } else {
                    MainView.showNotification("Error", "No se encontró start.bat", "error");
                }
            } else {
                File sh = new File(serverDir, "start.sh");
                if (sh.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(sh);
            }
        } catch (Exception e) {
            MainView.showNotification("Error", "No se pudo iniciar el servidor: " + e.getMessage(), "error");
        }
    }

    private void updateInstalledMods(File serverDir, String serverType) {
        boolean isPlugin = serverType.contains("Paper") || serverType.contains("Spigot");
        File modsDir = new File(serverDir, isPlugin ? "plugins" : "mods");
        if (!modsDir.exists()) return;

        MainView.showNotification("Actualizando", "Buscando actualizaciones...", "info");
        executor.submit(() -> {
            try {
                File[] files = modsDir.listFiles((d, name) -> name.endsWith(".jar"));
                if (files == null) return;

                JsonObject hashes = new JsonObject();
                JsonArray hashList = new JsonArray();
                for (File f : files) {
                    String h = getFileHash(f);
                    if (h != null) hashList.add(h);
                }
                hashes.add("hashes", hashList);
                hashes.addProperty("algorithm", "sha1");

                URL url = new URL("https://api.modrinth.com/v2/version_files/update");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(gson.toJson(hashes).getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == 200) {
                    JsonObject response = gson.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class);
                    List<JsonObject> updates = new ArrayList<>();
                    
                    for (File f : files) {
                        String h = getFileHash(f);
                        if (response.has(h)) {
                            JsonObject update = response.getAsJsonObject(h);
                            update.addProperty("_localFile", f.getAbsolutePath());
                            updates.add(update);
                        }
                    }
                    
                    if (updates.isEmpty()) {
                        Platform.runLater(() -> MainView.showNotification("Info", "Todos los mods están actualizados.", "info"));
                    } else {
                        Platform.runLater(() -> showUpdateConfirmation(updates, modsDir));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo al actualizar mods.", "error"));
            }
        });
    }

    private void showUpdateConfirmation(List<JsonObject> updates, File modsDir) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Actualizaciones Disponibles");
        dialog.setHeaderText("Se encontraron " + updates.size() + " actualizaciones.");
        
        ListView<String> list = new ListView<>();
        for (JsonObject u : updates) {
            String newName = u.getAsJsonObject("files").get("filename").getAsString();
            File oldFile = new File(u.get("_localFile").getAsString());
            list.getItems().add(oldFile.getName() + "  ➜  " + newName);
        }
        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executor.submit(() -> {
                    int count = 0;
                    for (JsonObject u : updates) {
                        try {
                            new File(u.get("_localFile").getAsString()).delete();
                            String url = u.getAsJsonObject("files").get("url").getAsString();
                            downloadFile(url, new File(modsDir, u.getAsJsonObject("files").get("filename").getAsString()), null);
                            count++;
                        } catch (Exception e) {}
                    }
                    int finalCount = count;
                    Platform.runLater(() -> MainView.showNotification("Éxito", "Se actualizaron " + finalCount + " mods.", "success"));
                });
            }
        });
    }

    private void installViaVersion(File serverDir, String serverType) {
        if (!serverType.contains("Paper") && !serverType.contains("Spigot")) {
            MainView.showNotification("Error", "ViaVersion requiere un servidor Paper o Spigot.", "error");
            return;
        }

        MainView.showNotification("Instalando", "Descargando ViaVersion...", "info");
        executor.submit(() -> {
            try {
                // Buscar ViaVersion en Modrinth
                String searchUrl = "https://api.modrinth.com/v2/project/viaversion/version";
                String json = fetchJson(searchUrl);
                JsonArray versions = gson.fromJson(json, JsonArray.class);
                if (versions.size() > 0) {
                    JsonObject ver = versions.get(0).getAsJsonObject();
                    String dlUrl = ver.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();
                    File dest = new File(serverDir, "plugins/ViaVersion.jar");
                    downloadFile(dlUrl, dest, null);
                    Platform.runLater(() -> MainView.showNotification("Éxito", "ViaVersion instalado. Reinicia el servidor.", "success"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> MainView.showNotification("Error", "No se pudo instalar ViaVersion.", "error"));
            }
        });
    }

    // --- PERSISTENCIA ---

    private JsonObject loadServerMetadata(File serverDir) {
        File jsonFile = new File(serverDir, "glauncher_server.json");
        if (jsonFile.exists()) {
            try (FileReader reader = new FileReader(jsonFile)) {
                return gson.fromJson(reader, JsonObject.class);
            } catch (Exception e) {}
        }
        return new JsonObject();
    }

    private void saveServerMetadata(File serverDir, JsonObject meta) {
        try (FileWriter writer = new FileWriter(new File(serverDir, "glauncher_server.json"))) {
            gson.toJson(meta, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Properties loadServerProperties(File serverDir) {
        Properties props = new Properties();
        File f = new File(serverDir, "server.properties");
        if(f.exists()) {
            try(FileInputStream in = new FileInputStream(f)) { props.load(in); } catch(Exception e){}
        }
        return props;
    }

    private void saveServerProperties(File serverDir, Properties props) {
        File f = new File(serverDir, "server.properties");
        try(FileOutputStream out = new FileOutputStream(f)) { 
            props.store(out, "Minecraft server properties"); 
        } catch(Exception e){}
    }

    private void updateAvailableVersions(String serverType) {
        verBox.getItems().clear();
        verBox.setPromptText("Cargando...");
        executor.submit(() -> {
            List<String> versions = new ArrayList<>();
            try {
                String url = "";
                if (serverType.startsWith("Paper")) {
                    url = "https://api.papermc.io/v2/projects/paper";
                    String json = fetchJson(url);
                    versions.addAll(gson.fromJson(json, JsonObject.class).getAsJsonArray("versions").asList().stream()
                        .map(JsonElement::getAsString).toList());
                    Collections.reverse(versions);
                } else if (serverType.equals("Fabric")) {
                    url = "https://meta.fabricmc.net/v2/versions/game";
                    String json = fetchJson(url);
                    versions.addAll(gson.fromJson(json, JsonArray.class).asList().stream()
                        .map(e -> e.getAsJsonObject().get("version").getAsString()).toList());
                } else { // Vanilla y Forge (usamos manifest de Mojang como base)
                    url = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
                    String json = fetchJson(url);
                    versions.addAll(gson.fromJson(json, JsonObject.class).getAsJsonArray("versions").asList().stream()
                        .filter(e -> e.getAsJsonObject().get("type").getAsString().equals("release"))
                        .map(e -> e.getAsJsonObject().get("id").getAsString()).toList());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                verBox.getItems().addAll(versions);
                if (!versions.isEmpty()) verBox.getSelectionModel().selectFirst();
                verBox.setPromptText("Seleccionar Versión");
            });
        });
    }

    private void initiateServerCreation(String serverName, int ram) {
        String serverType = typeBox.getValue();
        String mcVersion = verBox.getValue();

        if (serverName == null || serverName.trim().isEmpty()) {
            MainView.showNotification("Error", "El nombre del servidor no puede estar vacío.", "error");
            return;
        }
        if (mcVersion == null) {
            MainView.showNotification("Error", "Debes seleccionar una versión de Minecraft.", "error");
            return;
        }

        progressOverlay.setVisible(true);
        progressTitle.setText("Creando " + serverName);
        progressBar.setProgress(0);

        executor.submit(() -> {
            try {
                File serverDir = new File(DATA_DIR, "servers/" + serverName.trim().replaceAll("[^a-zA-Z0-9.-]", "_"));
                if (serverDir.exists()) {
                    Platform.runLater(() -> {
                        MainView.showNotification("Error", "Ya existe una carpeta con ese nombre.", "error");
                        progressOverlay.setVisible(false);
                    });
                    return;
                }
                serverDir.mkdirs();

                Platform.runLater(() -> progressStatus.setText("Obteniendo URL de descarga..."));
                String downloadUrl = getServerDownloadUrl(serverType, mcVersion);
                if (downloadUrl == null) throw new IOException("No se pudo obtener la URL de descarga.");

                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                File serverJar = new File(serverDir, fileName);

                Platform.runLater(() -> progressStatus.setText("Descargando " + fileName + "..."));
                downloadFile(downloadUrl, serverJar, p -> Platform.runLater(() -> progressBar.setProgress(p)));

                Platform.runLater(() -> progressStatus.setText("Creando archivos de inicio..."));
                createStartScript(serverDir, ram, fileName);
                createEula(serverDir);

                // Guardar metadatos iniciales
                JsonObject meta = new JsonObject();
                meta.addProperty("name", serverName);
                meta.addProperty("type", serverType);
                saveServerMetadata(serverDir, meta);

                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Éxito", "Servidor creado en: " + serverDir.getName(), "success");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    progressOverlay.setVisible(false);
                    MainView.showNotification("Error", "Fallo en la creación: " + e.getMessage(), "error");
                });
            }
        });
    }

    private String getServerDownloadUrl(String type, String version) throws IOException {
        if (type.startsWith("Paper")) {
            String buildsJson = fetchJson("https://api.papermc.io/v2/projects/paper/versions/" + version);
            JsonArray builds = gson.fromJson(buildsJson, JsonObject.class).getAsJsonArray("builds");
            String latestBuild = builds.get(builds.size() - 1).getAsString();
            String buildJson = fetchJson("https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds/" + latestBuild);
            String jarName = gson.fromJson(buildJson, JsonObject.class).getAsJsonObject("downloads").getAsJsonObject("application").get("name").getAsString();
            return "https://api.papermc.io/v2/projects/paper/versions/" + version + "/builds/" + latestBuild + "/downloads/" + jarName;
        }
        if (type.equals("Vanilla")) {
            String manifestJson = fetchJson("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");
            for (JsonElement e : gson.fromJson(manifestJson, JsonObject.class).getAsJsonArray("versions")) {
                JsonObject v = e.getAsJsonObject();
                if (v.get("id").getAsString().equals(version)) {
                    String versionJsonUrl = v.get("url").getAsString();
                    String versionJson = fetchJson(versionJsonUrl);
                    return gson.fromJson(versionJson, JsonObject.class).getAsJsonObject("downloads").getAsJsonObject("server").get("url").getAsString();
                }
            }
        }
        if (type.equals("Fabric")) {
            String json = fetchJson("https://meta.fabricmc.net/v2/versions/installer");
            return gson.fromJson(json, JsonArray.class).get(0).getAsJsonObject().get("url").getAsString();
        }
        if (type.equals("Forge")) {
            // La descarga de Forge es compleja. Por ahora, enlazamos a la página de archivos.
            // Una implementación real requeriría parsear su HTML.
            Platform.runLater(() -> MainView.showNotification("Info", "La descarga automática de Forge no está implementada. Descarga el instalador manualmente.", "warning"));
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://files.minecraftforge.net/net/minecraftforge/forge/index_" + version + ".html"));
            } catch (Exception e) {}
            throw new IOException("Descarga manual requerida para Forge.");
        }
        return null;
    }

    private void createStartScript(File dir, int ram, String jarName) throws IOException {
        String batContent = String.format("@echo off\njava -Xms1024M -Xmx%dM -jar %s nogui\npause", ram * 1024, jarName);
        try (FileWriter writer = new FileWriter(new File(dir, "start.bat"))) {
            writer.write(batContent);
        }

        String shContent = String.format("#!/bin/sh\njava -Xms1024M -Xmx%dM -jar %s nogui", ram * 1024, jarName);
        File shFile = new File(dir, "start.sh");
        try (FileWriter writer = new FileWriter(shFile)) {
            writer.write(shContent);
        }
        shFile.setExecutable(true);
    }

    private void createEula(File dir) throws IOException {
        String eulaContent = "eula=true\n";
        try (FileWriter writer = new FileWriter(new File(dir, "eula.txt"))) {
            writer.write(eulaContent);
        }
    }

    private String fetchJson(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "GLauncher/1.0");
        if (conn.getResponseCode() == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        }
        throw new IOException("HTTP Error " + conn.getResponseCode());
    }

    private void downloadFile(String urlStr, File dest, Consumer<Double> onProgress) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "GLauncher/1.0");
        long totalSize = conn.getContentLengthLong();
        long downloaded = 0;

        if (conn.getResponseCode() == 200) {
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] data = new byte[4096];
                int count;
                while ((count = in.read(data, 0, 4096)) != -1) {
                    out.write(data, 0, count);
                    downloaded += count;
                    if (onProgress != null && totalSize > 0) {
                        onProgress.accept((double) downloaded / totalSize);
                    }
                }
            }
        } else {
            throw new IOException("Error HTTP " + conn.getResponseCode());
        }
    }

    private ImageView loadIcon(String path, double size) {
        try {
            File f = new File(path);
            if (f.exists()) {
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                return iv;
            }
        } catch (Exception e) { }
        return new ImageView();
    }
}