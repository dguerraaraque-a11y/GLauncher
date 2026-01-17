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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicView {
    
    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private BorderPane root;
    private StackPane contentArea;
    
    // Vistas
    private VBox internalView;
    private VBox youtubeView;
    private VBox settingsView;
    
    private VBox playerWrapper;

    // Componentes Interna
    private ListView<File> localFilesList;
    private MediaPlayer localPlayer;
    private Label lblLocalStatus;

    // Componentes YouTube
    private TextField searchField;
    private VBox resultsContainer;
    private Label lblQueueStatus;
    private ListView<String> queueListView;
    private Label lblOverlayTitle; // [NUEVO] Título en overlay
    private WebView webPlayer;
    private final String YT_API_KEY = "AIzaSyAlGmxmZbvkEKjVGLD487giPvl10wO-C9k";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<String> playlist = new ArrayList<>();
    private Timeline monitorTimer; // [FIX] Reemplaza al timer fijo por uno de monitoreo
    private TranslateTransition marqueeTransition; // [NUEVO] Animación de marquesina
    private List<JsonObject> currentSearchItems = new ArrayList<>(); // [NUEVO] Cache de resultados
    private Stage playlistWindow; // [NUEVO] Ventana de lista de reproducción

    // Componentes Ajustes (Hotkeys)
    private KeyCode keyPlayPause = KeyCode.P;
    private KeyCode keyStop = KeyCode.S;
    private KeyCode keyMiniPlayer = KeyCode.M; // [NUEVO] Tecla para Mini Player
    private Button btnKeyPlayPause;
    private Button btnKeyStop;
    private Button btnKeyMiniPlayer;
    private CheckBox chkLowSpecMode; // [NUEVO] Checkbox optimización
    private boolean waitingForKey = false;
    private Button activeKeyButton = null;
    
    // Mini Player
    private Stage miniPlayerStage;
    private double xOffset = 0;
    private double yOffset = 0;

    // [OPTIMIZACION] Intentar forzar aceleración por hardware para mejorar rendimiento del WebView
    static {
        // [FIX] Optimización para PCs de bajos recursos (Canaima/Gobierno)
        // Priorizar GPU (d3d/es2) sobre Software (sw) para decodificar video sin lag
        System.setProperty("prism.order", "d3d,es2,sw");
        System.setProperty("prism.forceGPU", "true"); // Intentar usar GPU aunque sea antigua
        System.setProperty("prism.vsync", "false");   // Desactivar VSync para mejorar respuesta
    }

    // [FIX] Instancia estática para mantener el estado (Singleton)
    // Esto evita que se reinicie la vista o se duplique el audio al cambiar de pestaña
    private static MusicView instance;

    public Parent getView() {
        if (instance != null) {
            return instance.root;
        }
        instance = this;

        root = new BorderPane();
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 15;");
        root.setPadding(new Insets(10));

        // --- Sidebar ---
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10;");

        Label title = new Label("GMusic");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        ToggleGroup group = new ToggleGroup();
        ToggleButton btnInternal = createNavButton("Interna", "assets/icons/icons-gui/nota-m.png", group);
        ToggleButton btnYoutube = createNavButton("Buscador YT", "assets/icons/icons-gui/buscador-y.png", group);
        ToggleButton btnSettings = createNavButton("Ajustes", "assets/icons/icons-gui/tuerca.png", group);

        sidebar.getChildren().addAll(title, new Separator(), btnInternal, btnYoutube, btnSettings);

        // --- Content Area ---
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(0, 0, 0, 15));

        // Inicializar Vistas
        initInternalView();
        initYoutubeView();
        initSettingsView();

        // Eventos de Navegación
        btnInternal.setOnAction(e -> showView(internalView));
        btnYoutube.setOnAction(e -> showView(youtubeView));
        btnSettings.setOnAction(e -> showView(settingsView));

        // Vista por defecto
        btnInternal.setSelected(true);
        // [FIX] Inicializar visibilidad y añadir todas las vistas para persistencia (Background Playback)
        if (internalView != null) internalView.setVisible(true);
        if (youtubeView != null) youtubeView.setVisible(false);
        if (settingsView != null) settingsView.setVisible(false);
        
        contentArea.getChildren().addAll(internalView, youtubeView, settingsView);

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Global Key Handler for Hotkeys
        root.setOnKeyPressed(this::handleGlobalKeyPress);

        return root;
    }

    private void showView(Node view) {
        // [OPTIMIZACION] Usar setVisible en lugar de clear/add para mantener el WebView activo en segundo plano
        if (internalView != null) internalView.setVisible(view == internalView);
        if (youtubeView != null) youtubeView.setVisible(view == youtubeView);
        if (settingsView != null) settingsView.setVisible(view == settingsView);
    }

    // --- 1. VISTA INTERNA (Local) ---
    private void initInternalView() {
        internalView = new VBox(15);
        internalView.setAlignment(Pos.TOP_LEFT);

        Label lblHeader = new Label("Música Local");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox controls = new HBox(10);
        Button btnAdd = new Button("📂 Añadir Archivos");
        btnAdd.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> addLocalFiles());

        Button btnStop = new Button("⏹ Detener");
        btnStop.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand;");
        btnStop.setOnAction(e -> stopLocalMusic());

        controls.getChildren().addAll(btnAdd, btnStop);

        localFilesList = new ListView<>();
        localFilesList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: rgba(0,0,0,0.3);");
        localFilesList.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                } else {
                    setText(item.getName());
                    setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                }
            }
        });
        
        localFilesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                File selected = localFilesList.getSelectionModel().getSelectedItem();
                if (selected != null) playLocalFile(selected);
            }
        });

        lblLocalStatus = new Label("Listo para reproducir.");
        lblLocalStatus.setStyle("-fx-text-fill: #aaa;");

        internalView.getChildren().addAll(lblHeader, controls, localFilesList, lblLocalStatus);
    }

    private void addLocalFiles() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a"));
        List<File> files = fc.showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            localFilesList.getItems().addAll(files);
        }
    }

    private void playLocalFile(File file) {
        stopLocalMusic();
        // Detener YouTube si suena
        if (webPlayer != null) webPlayer.getEngine().load(null);

        try {
            Media media = new Media(file.toURI().toString());
            localPlayer = new MediaPlayer(media);
            localPlayer.setOnEndOfMedia(this::playNextLocal);
            localPlayer.play();
            lblLocalStatus.setText("Reproduciendo: " + file.getName());
        } catch (Exception e) {
            lblLocalStatus.setText("Error al reproducir: " + e.getMessage());
        }
    }

    private void playNextLocal() {
        int index = localFilesList.getSelectionModel().getSelectedIndex();
        if (index < localFilesList.getItems().size() - 1) {
            localFilesList.getSelectionModel().select(index + 1);
            playLocalFile(localFilesList.getItems().get(index + 1));
        }
    }

    private void stopLocalMusic() {
        if (localPlayer != null) {
            localPlayer.stop();
            localPlayer.dispose();
            localPlayer = null;
            lblLocalStatus.setText("Detenido.");
        }
    }

    // --- 2. VISTA YOUTUBE ---
    private void initYoutubeView() {
        youtubeView = new VBox(20);
        youtubeView.setAlignment(Pos.TOP_CENTER);
        youtubeView.setPadding(new Insets(20));

        // --- Header & Búsqueda ---
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label lblHeader = new Label("YouTube");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_RIGHT);
        
        searchField = new TextField();
        searchField.setPromptText("Buscar video o pegar enlace...");
        searchField.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 15; -fx-font-size: 14px;");
        searchField.setPrefWidth(350);
        searchField.setOnAction(e -> searchYoutube());

        Button btnSearch = new Button("🔍");
        btnSearch.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20; -fx-padding: 8 15; -fx-font-size: 14px;");
        btnSearch.setOnAction(e -> searchYoutube());
        
        lblQueueStatus = new Label("Cola: 0");
        lblQueueStatus.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px; -fx-padding: 0 10 0 0;");

        searchBox.getChildren().add(0, lblQueueStatus);
        searchBox.getChildren().addAll(searchField, btnSearch);
        headerBox.getChildren().addAll(lblHeader, spacer, searchBox);

        // --- Contenido Principal (Split: Resultados | Reproductor) ---
        HBox mainContent = new HBox(20);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        mainContent.setAlignment(Pos.TOP_LEFT);

        // Columna Izquierda: Resultados
        resultsContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(resultsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox resultsWrapper = new VBox(10, new Label("Resultados") {{ setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;"); }}, scroll);
        HBox.setHgrow(resultsWrapper, Priority.ALWAYS);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Columna Derecha: Reproductor Fijo
        playerWrapper = new VBox(15);
        playerWrapper.setPrefWidth(500);
        playerWrapper.setMinWidth(500);
        playerWrapper.setAlignment(Pos.TOP_CENTER);
        playerWrapper.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 15; -fx-padding: 15;"); // [FIX] Fondo más oscuro para resaltar

        webPlayer = new WebView();
        // [FIX] Tamaño dinámico/grande para el panel "mini web" (Panel completo)
        webPlayer.setPrefSize(480, 360); 
        webPlayer.setMinSize(300, 250);
        // [FIX] User-Agent actualizado a Chrome 133 para mejor compatibilidad y calidad
        webPlayer.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
        webPlayer.setContextMenuEnabled(false); // Desactivar menú contextual para ahorrar recursos
        
        Label lblNowPlaying = new Label("Reproductor");
        lblNowPlaying.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // [FIX] Eliminadas pestañas de categoría (Colas/Detalles) a petición del usuario
        playerWrapper.getChildren().addAll(lblNowPlaying, webPlayer);

        mainContent.getChildren().addAll(resultsWrapper, playerWrapper);
        
        // [NUEVO] Mini Overlay (Arriba Lateral) - Controles flotantes
        HBox overlayBox = new HBox(10);
        overlayBox.setAlignment(Pos.CENTER_RIGHT);
        overlayBox.setPadding(new Insets(5, 15, 5, 15));
        overlayBox.setStyle("-fx-background-color: rgba(0, 120, 215, 0.2); -fx-background-radius: 20; -fx-border-color: rgba(0, 120, 215, 0.5); -fx-border-radius: 20;");
        
        // [NUEVO] Contenedor con recorte para efecto marquesina
        Pane titleContainer = new Pane();
        titleContainer.setPrefSize(200, 20);
        titleContainer.setMaxSize(200, 20);
        Rectangle clip = new Rectangle(200, 20);
        titleContainer.setClip(clip);

        lblOverlayTitle = new Label("Sin reproducción");
        lblOverlayTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        // lblOverlayTitle.setMaxWidth(200); // Eliminado para permitir que el texto crezca
        
        titleContainer.getChildren().add(lblOverlayTitle);

        // Lógica de marquesina automática
        lblOverlayTitle.textProperty().addListener((obs, oldVal, newVal) -> {
            if (marqueeTransition != null) {
                marqueeTransition.stop();
                lblOverlayTitle.setTranslateX(0);
            }
            Platform.runLater(() -> {
                double textWidth = lblOverlayTitle.getLayoutBounds().getWidth();
                if (textWidth > 200) {
                    marqueeTransition = new TranslateTransition(Duration.seconds((textWidth + 200) / 50), lblOverlayTitle);
                    marqueeTransition.setFromX(200);
                    marqueeTransition.setToX(-textWidth);
                    marqueeTransition.setCycleCount(Timeline.INDEFINITE);
                    marqueeTransition.setInterpolator(Interpolator.LINEAR);
                    marqueeTransition.play();
                }
            });
        });
        
        // [NUEVO] Botón Atrasar 10s (Solución al problema de "se pega")
        Button btnOverlayRewind = new Button("⏪");
        btnOverlayRewind.setTooltip(new Tooltip("Atrasar 10 segundos"));
        btnOverlayRewind.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        btnOverlayRewind.setOnAction(e -> {
            webPlayer.getEngine().executeScript("var v = document.querySelector('video'); if(v){ v.currentTime -= 10; }");
        });

        Button btnOverlayPlay = new Button("⏯");
        btnOverlayPlay.setTooltip(new Tooltip("Pausar/Reanudar (Espacio)"));
        btnOverlayPlay.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        btnOverlayPlay.setOnAction(e -> {
            // Inyectar JS para pausar/reproducir el video HTML5
            webPlayer.getEngine().executeScript("var vid = document.querySelector('video'); if(vid){ if(vid.paused){vid.play();}else{vid.pause();} }");
        });
        
        Button btnOverlayNext = new Button("⏭");
        btnOverlayNext.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        btnOverlayNext.setOnAction(e -> playNextInPlaylist());

        overlayBox.getChildren().addAll(titleContainer, btnOverlayRewind, btnOverlayPlay, btnOverlayNext);
        
        // [NUEVO] Botón de Modo Audio (Nota Musical)
        Button btnPlaylist = new Button("🎵");
        btnPlaylist.setTooltip(new Tooltip("Modo Audio (Mini Player)"));
        // Estilo Gamer: Neon, oscuro, sin bordes default
        btnPlaylist.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-text-fill: #00ff00; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-size: 14px; -fx-border-color: #00ff00; -fx-border-radius: 50; -fx-border-width: 1; -fx-min-width: 30px; -fx-min-height: 30px;");
        // [FIX] Botón siempre visible
        btnPlaylist.setVisible(true);
        btnPlaylist.setManaged(true);
        
        btnPlaylist.setOnAction(e -> toggleMiniPlayer());

        // Contenedor para el overlay y el botón
        HBox overlayContainer = new HBox(5);
        overlayContainer.setAlignment(Pos.CENTER_RIGHT);
        overlayContainer.getChildren().addAll(btnPlaylist, overlayBox);

        // Integrar overlay en la barra superior
        HBox topBar = new HBox(10, headerBox, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, overlayContainer);
        HBox.setHgrow(headerBox, Priority.ALWAYS);
        
        youtubeView.getChildren().addAll(topBar, mainContent);
    }

    private void showPlaylistPopup(Node anchor) {
        if (playlistWindow != null && playlistWindow.isShowing()) {
            playlistWindow.close();
            playlistWindow = null;
            return;
        }

        playlistWindow = new Stage();
        playlistWindow.initStyle(StageStyle.TRANSPARENT);
        playlistWindow.setAlwaysOnTop(true);
        
        VBox listContent = new VBox(5);
        listContent.setPadding(new Insets(10));
        listContent.setStyle("-fx-background-color: rgba(10, 10, 10, 0.95); -fx-border-color: #00ff00; -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,255,0,0.3), 10, 0, 0, 0);");
        listContent.setPrefWidth(300);
        listContent.setMaxHeight(400);
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTitle = new Label("Resultados de Búsqueda");
        lblTitle.setStyle("-fx-text-fill: #00ff00; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ff00; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnClose.setOnAction(e -> playlistWindow.close());
        
        header.getChildren().addAll(lblTitle, spacer, btnClose);

        VBox itemsBox = new VBox(5);
        ScrollPane scroll = new ScrollPane(itemsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.getStylesheets().add("data:text/css,.scroll-pane .viewport { -fx-background-color: transparent; } .scroll-bar:vertical .track { -fx-background-color: #222; } .scroll-bar:vertical .thumb { -fx-background-color: #00ff00; -fx-background-radius: 5; }");
        
        if (currentSearchItems.isEmpty()) {
            Label empty = new Label("Sin resultados recientes.");
            empty.setStyle("-fx-text-fill: #aaa; -fx-padding: 10;");
            itemsBox.getChildren().add(empty);
        } else {
            for (JsonObject item : currentSearchItems) {
                JsonObject snippet = item.getAsJsonObject("snippet");
                String title = Jsoup.parse(snippet.get("title").getAsString()).text();
                String videoId = item.getAsJsonObject("id").get("videoId").getAsString();
                
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5));
                row.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-border-radius: 5;");
                
                Label lblSong = new Label(title);
                lblSong.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                lblSong.setWrapText(false);
                lblSong.setTextOverrun(OverrunStyle.ELLIPSIS);
                
                row.getChildren().add(lblSong);
                
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(0, 255, 0, 0.2); -fx-background-radius: 5;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));
                row.setOnMouseClicked(e -> {
                    playYoutubeVideo(videoId, title);
                    // No cerrar la ventana al seleccionar, para permitir añadir varios a la cola si se desea
                });
                
                itemsBox.getChildren().add(row);
            }
        }
        
        listContent.getChildren().addAll(header, scroll);
        
        // Hacer la ventana arrastrable
        final double[] xOffset = {0};
        final double[] yOffset = {0};
        listContent.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        listContent.setOnMouseDragged(event -> {
            playlistWindow.setX(event.getScreenX() - xOffset[0]);
            playlistWindow.setY(event.getScreenY() - yOffset[0]);
        });

        Scene scene = new Scene(listContent);
        scene.setFill(null);
        playlistWindow.setScene(scene);
        
        javafx.geometry.Point2D point = anchor.localToScreen(0, 0);
        if (point != null) {
            playlistWindow.setX(point.getX() - 310);
            playlistWindow.setY(point.getY());
        }
        
        playlistWindow.show();
    }

    private void searchYoutube() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        // Soporte para URLs directas (Reproducir sin buscar)
        String directId = extractVideoId(query);
        if (directId != null) {
            playYoutubeVideo(directId);
            searchField.clear();
            return;
        }

        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(new Label("Buscando..."));
        currentSearchItems.clear(); // [FIX] Limpiar cache anterior

        executor.submit(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String apiKey = getApiKey();
                String urlStr = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=10&q=" + encodedQuery + "&type=video&key=" + apiKey;
                
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                
                final int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    JsonArray items = json.getAsJsonArray("items");

                    Platform.runLater(() -> {
                        resultsContainer.getChildren().clear();
                        for (JsonElement el : items) {
                            currentSearchItems.add(el.getAsJsonObject()); // [FIX] Guardar en cache
                            // Usamos Jsoup para limpiar el titulo de caracteres HTML (como &quot;)
                            resultsContainer.getChildren().add(createYoutubeResult(el.getAsJsonObject()));
                        }
                    });
                } else if (responseCode == 403) {
                    Platform.runLater(() -> resultsContainer.getChildren().setAll(
                        new Label("Error 403: Clave API inválida o cuota excedida."),
                        new Label("Por favor configura tu API Key en Ajustes.")
                    ));
                } else {
                    Platform.runLater(() -> resultsContainer.getChildren().setAll(new Label("Error API: " + responseCode)));
                }
            } catch (Exception e) {
                Platform.runLater(() -> resultsContainer.getChildren().setAll(new Label("Error: " + e.getMessage())));
            }
        });
    }

    private String getApiKey() {
        File settingsFile = new File(DATA_DIR, "settings.json");
        if (settingsFile.exists()) {
            try (FileReader reader = new FileReader(settingsFile)) {
                JsonObject settings = gson.fromJson(reader, JsonObject.class);
                if (settings.has("youtubeApiKey")) {
                    String key = settings.get("youtubeApiKey").getAsString();
                    if (key != null && !key.trim().isEmpty()) return key;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return YT_API_KEY; // Fallback a la clave por defecto si no hay configuración
    }

    private String extractVideoId(String url) {
        if (url.contains("youtube.com/watch?v=")) {
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                String id = parts[1];
                if (id.contains("&")) id = id.split("&")[0];
                return id;
            }
        } else if (url.contains("youtu.be/")) {
            String[] parts = url.split("youtu.be/");
            if (parts.length > 1) {
                String id = parts[1];
                if (id.contains("?")) id = id.split("\\?")[0];
                return id;
            }
        }
        return null;
    }

    private HBox createYoutubeResult(JsonObject item) {
        JsonObject snippet = item.getAsJsonObject("snippet");
        
        // Usamos Jsoup para decodificar entidades HTML en el titulo
        String title = Jsoup.parse(snippet.get("title").getAsString()).text();
        String channel = snippet.get("channelTitle").getAsString();
        String thumbUrl = snippet.getAsJsonObject("thumbnails").getAsJsonObject("default").get("url").getAsString();
        String videoId = item.getAsJsonObject("id").get("videoId").getAsString();

        HBox row = new HBox(15);
        row.setPadding(new Insets(10));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;");
        
        // Efecto Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 10;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;"));

        ImageView thumb = new ImageView(new Image(thumbUrl, 80, 60, true, true));
        
        VBox info = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label lblChannel = new Label(channel);
        lblChannel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        info.getChildren().addAll(lblTitle, lblChannel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // [FIX] Botón de reproducción corregido para usar el mini web (embed) y actualizar título
        Button btnPlay = new Button("▶");
        btnPlay.setTooltip(new Tooltip("Reproducir"));
        btnPlay.setStyle("-fx-background-color: transparent; -fx-text-fill: #00ff00; -fx-cursor: hand; -fx-font-size: 18px; -fx-font-weight: bold;");
        btnPlay.setOnAction(e -> {
            e.consume();
            playYoutubeVideo(videoId, title);
        });

        Button btnQueue = new Button("+");
        btnQueue.setTooltip(new Tooltip("Añadir a la cola"));
        btnQueue.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-cursor: hand; -fx-font-size: 18px; -fx-font-weight: bold;");
        btnQueue.setOnAction(e -> {
            e.consume();
            addToPlaylist(videoId);
        });

        row.getChildren().addAll(thumb, info, btnQueue, btnPlay);
        row.setOnMouseClicked(e -> playYoutubeVideo(videoId, title));
        
        return row;
    }
    
    // [NUEVO] Sobrecarga para actualizar el título en el overlay
    private void playYoutubeVideo(String videoId, String title) {
        if (lblOverlayTitle != null) Platform.runLater(() -> lblOverlayTitle.setText(title));
        playYoutubeVideo(videoId);
    }

    private void addToPlaylist(String videoId) {
        playlist.add(videoId);
        lblQueueStatus.setText("Cola: " + playlist.size());
        glauncher.MainView.showNotification("Cola", "Video añadido a la lista de reproducción.", "success");
    }

    private void playYoutubeVideo(String videoId) {
        stopLocalMusic(); // Detener música local
        
        // [FIX] Reiniciar monitor de estado
        startMonitoring();

        // [FIX] Cargar la página completa de YouTube (Mini Web) en lugar del embed
        String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
        webPlayer.getEngine().load(watchUrl);
    }

    // [FIX] Nuevo sistema de monitoreo: Pregunta al video si terminó en lugar de adivinar el tiempo
    private void startMonitoring() {
        if (monitorTimer != null) monitorTimer.stop();
        
        monitorTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            try {
                // Inyectar JS para verificar si el video terminó (ended === true)
                Object result = webPlayer.getEngine().executeScript("var v = document.querySelector('video'); v ? v.ended : false;");
                if (result instanceof Boolean && (Boolean) result) {
                    playNextInPlaylist();
                }

                // [NUEVO] Script para leer el título de la canción sonando y actualizar el overlay
                Object titleObj = webPlayer.getEngine().executeScript("var t = document.querySelector('.ytp-title-link'); t ? t.innerText : ''");
                if (titleObj instanceof String) {
                    String currentTitle = (String) titleObj;
                    if (!currentTitle.isEmpty() && lblOverlayTitle != null && !lblOverlayTitle.getText().equals(currentTitle)) {
                        lblOverlayTitle.setText(currentTitle);
                    }
                }
            } catch (Exception ex) { 
                // Ignorar errores mientras carga la página
            }
        }));
        monitorTimer.setCycleCount(Timeline.INDEFINITE);
        monitorTimer.play();
    }

    private void playNextInPlaylist() {
        if (!playlist.isEmpty()) {
            String nextId = playlist.remove(0);
            lblQueueStatus.setText("Cola: " + playlist.size());
            playYoutubeVideo(nextId);
            glauncher.MainView.showNotification("GMusic", "Reproduciendo siguiente video...", "info");
        }
    }

    // --- 3. VISTA AJUSTES (Hotkeys) ---
    private void initSettingsView() {
        settingsView = new VBox(20);
        settingsView.setAlignment(Pos.TOP_LEFT);

        Label lblHeader = new Label("Configuración de Teclas (Hotkeys)");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);

        grid.add(createLabel("Reproducir / Pausar:"), 0, 0);
        btnKeyPlayPause = createKeyButton(keyPlayPause);
        btnKeyPlayPause.setOnAction(e -> startKeyBinding(btnKeyPlayPause, code -> keyPlayPause = code));
        grid.add(btnKeyPlayPause, 1, 0);

        grid.add(createLabel("Detener:"), 0, 1);
        btnKeyStop = createKeyButton(keyStop);
        btnKeyStop.setOnAction(e -> startKeyBinding(btnKeyStop, code -> keyStop = code));
        grid.add(btnKeyStop, 1, 1);

        grid.add(createLabel("Mini Player:"), 0, 2);
        btnKeyMiniPlayer = createKeyButton(keyMiniPlayer);
        btnKeyMiniPlayer.setOnAction(e -> startKeyBinding(btnKeyMiniPlayer, code -> keyMiniPlayer = code));
        grid.add(btnKeyMiniPlayer, 1, 2);

        Label lblInfo = new Label("Haz clic en el botón y presiona una tecla para asignar.");
        lblInfo.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");

        // [NUEVO] Sección de Optimización
        Separator sep = new Separator();
        Label lblOpt = new Label("Optimización");
        lblOpt.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        chkLowSpecMode = new CheckBox("Modo Bajo Rendimiento (Dispositivos Bajos)");
        chkLowSpecMode.setStyle("-fx-text-fill: white;");
        chkLowSpecMode.setTooltip(new Tooltip("Desactiva animaciones y efectos visuales pesados."));
        chkLowSpecMode.selectedProperty().addListener((obs, old, val) -> applyLowSpecMode(val));

        settingsView.getChildren().addAll(lblHeader, grid, lblInfo, sep, lblOpt, chkLowSpecMode);
    }

    private void applyLowSpecMode(boolean enabled) {
        if (enabled) {
            // Reducir calidad visual o desactivar efectos para ganar FPS
            if (webPlayer != null) webPlayer.setEffect(null);
            root.setStyle("-fx-background-color: #111;"); // Fondo sólido simple (menos carga GPU)
        } else {
            root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 15;");
        }
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white;");
        return l;
    }

    private Button createKeyButton(KeyCode current) {
        Button btn = new Button(current.getName());
        btn.setPrefWidth(150);
        btn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-color: #666; -fx-border-radius: 5; -fx-background-radius: 5;");
        return btn;
    }

    private void startKeyBinding(Button btn, java.util.function.Consumer<KeyCode> setter) {
        if (waitingForKey) return;
        waitingForKey = true;
        activeKeyButton = btn;
        btn.setText("Presiona tecla...");
        btn.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        
        // El manejador global capturará la tecla
        btn.getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, new javafx.event.EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                setter.accept(event.getCode());
                btn.setText(event.getCode().getName());
                btn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-color: #666; -fx-border-radius: 5; -fx-background-radius: 5;");
                waitingForKey = false;
                activeKeyButton = null;
                btn.getScene().getRoot().removeEventHandler(KeyEvent.KEY_PRESSED, this);
            }
        });
    }

    private void handleGlobalKeyPress(KeyEvent event) {
        if (waitingForKey) return; // No ejecutar acciones si estamos configurando

        if (event.getCode() == keyPlayPause) {
            if (localPlayer != null) {
                if (localPlayer.getStatus() == MediaPlayer.Status.PLAYING) localPlayer.pause();
                else localPlayer.play();
            }
        } else if (event.getCode() == keyStop) {
            stopLocalMusic();
            if (webPlayer != null) webPlayer.getEngine().load(null);
            if (monitorTimer != null) monitorTimer.stop();
            playlist.clear();
            lblQueueStatus.setText("Cola: 0");
        } else if (event.getCode() == keyMiniPlayer) {
            toggleMiniPlayer();
        }
    }

    private void toggleMiniPlayer() {
        if (miniPlayerStage != null && miniPlayerStage.isShowing()) {
            // Restaurar a la ventana principal
            miniPlayerStage.close();
            miniPlayerStage = null;
            
            // Volver a añadir el webPlayer al contenedor original
            if (playerWrapper != null && !playerWrapper.getChildren().contains(webPlayer)) {
                // Insertar en la posición correcta (índice 1, después del label)
                try {
                    playerWrapper.getChildren().add(1, webPlayer);
                } catch (Exception e) {
                    playerWrapper.getChildren().add(webPlayer);
                }
                webPlayer.setPrefSize(480, 360); // Restaurar tamaño original
                webPlayer.setVisible(true); // [FIX] Asegurar visibilidad al volver
                webPlayer.setOpacity(1.0);  // [FIX] Restaurar opacidad
            }
        } else {
            // Activar Mini Player
            if (webPlayer.getParent() instanceof Pane) {
                ((Pane) webPlayer.getParent()).getChildren().remove(webPlayer);
            }

            miniPlayerStage = new Stage();
            miniPlayerStage.initStyle(StageStyle.TRANSPARENT);
            miniPlayerStage.setAlwaysOnTop(true);

            // Configurar contenedor del Mini Player (Estilo Spotify/Black Box)
            StackPane miniRoot = new StackPane();
            miniRoot.setStyle("-fx-background-color: #111; -fx-border-color: #333; -fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");
            miniRoot.setPadding(new Insets(10));
            
            // [FIX] Modo Audio: Ocultar video (1x1 px, invisible) pero mantenerlo en escena para el audio
            webPlayer.setPrefSize(1, 1);
            webPlayer.setOpacity(0);
            
            // UI de Audio (Título y Estado)
            VBox audioInfo = new VBox(2);
            audioInfo.setAlignment(Pos.CENTER_LEFT);
            
            Label lblTitle = new Label();
            lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            if (lblOverlayTitle != null) lblTitle.textProperty().bind(lblOverlayTitle.textProperty());
            else lblTitle.setText("GMusic Audio");
            
            Label lblSub = new Label("Audio Mode");
            lblSub.setStyle("-fx-text-fill: #00b4db; -fx-font-size: 10px;");
            
            audioInfo.getChildren().addAll(lblTitle, lblSub);
            
            // Añadir webPlayer (oculto) y la info
            miniRoot.getChildren().addAll(webPlayer, audioInfo);

            // Permitir arrastrar la ventana
            miniRoot.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            miniRoot.setOnMouseDragged(event -> {
                miniPlayerStage.setX(event.getScreenX() - xOffset);
                miniPlayerStage.setY(event.getScreenY() - yOffset);
            });

            // Tamaño compacto tipo notificación/barra
            Scene scene = new Scene(miniRoot, 250, 50);
            scene.setFill(null);
            miniPlayerStage.setScene(scene);
            
            // [FIX] Posicionar ARRIBA a la izquierda (Top-Left)
            miniPlayerStage.setX(20);
            miniPlayerStage.setY(50); // Margen superior
            
            miniPlayerStage.show();
        }
    }

    // --- Helpers ---
    private ToggleButton createNavButton(String text, String iconPath, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10;");
        
        try {
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                ImageView icon = new ImageView(new Image(iconFile.toURI().toString()));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
            }
        } catch (Exception e) {
            // Ignorar si no se encuentra el icono
        }

        btn.selectedProperty().addListener((obs, old, val) -> {
            if (val) btn.setStyle("-fx-background-color: rgba(0, 120, 215, 0.3); -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 5;");
            else btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10;");
        });
        
        return btn;
    }
}
