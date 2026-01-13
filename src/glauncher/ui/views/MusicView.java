package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
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
    
    private BorderPane root;
    private StackPane contentArea;
    
    // Vistas
    private VBox internalView;
    private VBox youtubeView;
    private VBox settingsView;

    // Componentes Interna
    private ListView<File> localFilesList;
    private MediaPlayer localPlayer;
    private Label lblLocalStatus;

    // Componentes YouTube
    private TextField searchField;
    private VBox resultsContainer;
    private WebView webPlayer;
    private final String YT_API_KEY = "AIzaSyAlGmxmZbvkEKjVGLD487giPvl10wO-C9k";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Componentes Ajustes (Hotkeys)
    private KeyCode keyPlayPause = KeyCode.P;
    private KeyCode keyStop = KeyCode.S;
    private Button btnKeyPlayPause;
    private Button btnKeyStop;
    private boolean waitingForKey = false;
    private Button activeKeyButton = null;

    public Parent getView() {
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
        ToggleButton btnInternal = createNavButton("🎵 Interna", group);
        ToggleButton btnYoutube = createNavButton("📺 Buscador YT", group);
        ToggleButton btnSettings = createNavButton("⚙ Ajustes", group);

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
        showView(internalView);

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Global Key Handler for Hotkeys
        root.setOnKeyPressed(this::handleGlobalKeyPress);

        return root;
    }

    private void showView(VBox view) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
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
        youtubeView = new VBox(15);
        youtubeView.setAlignment(Pos.TOP_CENTER);

        Label lblHeader = new Label("Buscador de YouTube");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox searchBox = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Buscar video...");
        searchField.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        searchField.setPrefWidth(300);
        searchField.setOnAction(e -> searchYoutube());

        Button btnSearch = new Button("🔍");
        btnSearch.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        btnSearch.setOnAction(e -> searchYoutube());

        searchBox.getChildren().addAll(searchField, btnSearch);
        searchBox.setAlignment(Pos.CENTER);

        // WebView para reproducir
        webPlayer = new WebView();
        webPlayer.setPrefSize(480, 270);
        webPlayer.setMaxSize(480, 270);
        webPlayer.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        resultsContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(resultsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        youtubeView.getChildren().addAll(lblHeader, searchBox, webPlayer, scroll);
    }

    private void searchYoutube() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        resultsContainer.getChildren().clear();
        resultsContainer.getChildren().add(new Label("Buscando..."));

        executor.submit(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String urlStr = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=10&q=" + encodedQuery + "&type=video&key=" + YT_API_KEY;
                
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
                            // Usamos Jsoup para limpiar el titulo de caracteres HTML (como &quot;)
                            resultsContainer.getChildren().add(createYoutubeResult(el.getAsJsonObject()));
                        }
                    });
                } else {
                    Platform.runLater(() -> resultsContainer.getChildren().setAll(new Label("Error API: " + responseCode)));
                }
            } catch (Exception e) {
                Platform.runLater(() -> resultsContainer.getChildren().setAll(new Label("Error: " + e.getMessage())));
            }
        });
    }

    private HBox createYoutubeResult(JsonObject item) {
        JsonObject snippet = item.getAsJsonObject("snippet");
        
        // Usamos Jsoup para decodificar entidades HTML en el titulo
        String title = Jsoup.parse(snippet.get("title").getAsString()).text();
        String channel = snippet.get("channelTitle").getAsString();
        String thumbUrl = snippet.getAsJsonObject("thumbnails").getAsJsonObject("default").get("url").getAsString();
        String videoId = item.getAsJsonObject("id").get("videoId").getAsString();

        HBox row = new HBox(10);
        row.setPadding(new Insets(5));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5; -fx-cursor: hand;");

        ImageView thumb = new ImageView(new Image(thumbUrl, 80, 60, true, true));
        
        VBox info = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        Label lblChannel = new Label(channel);
        lblChannel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
        info.getChildren().addAll(lblTitle, lblChannel);

        row.getChildren().addAll(thumb, info);
        row.setOnMouseClicked(e -> playYoutubeVideo(videoId));
        
        return row;
    }

    private void playYoutubeVideo(String videoId) {
        stopLocalMusic(); // Detener música local
        // Usamos la version embed con autoplay y sin controles excesivos
        String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=1&rel=0&modestbranding=1";
        webPlayer.getEngine().load(embedUrl);
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

        Label lblInfo = new Label("Haz clic en el botón y presiona una tecla para asignar.");
        lblInfo.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");

        settingsView.getChildren().addAll(lblHeader, grid, lblInfo);
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
        }
    }

    // --- Helpers ---
    private ToggleButton createNavButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10;");
        
        btn.selectedProperty().addListener((obs, old, val) -> {
            if (val) btn.setStyle("-fx-background-color: rgba(0, 120, 215, 0.3); -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10; -fx-background-radius: 5;");
            else btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 10;");
        });
        
        return btn;
    }
}
