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
import javafx.scene.layout.*;
import glauncher.MainView;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Optional;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.function.Consumer;
import java.util.concurrent.Future;

public class DownloadsView {

    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int offset = 0;
    private final int limit = 20;
    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    
    private TextField searchField;
    private ComboBox<String> typeFilter;
    private ComboBox<String> sortFilter;
    private FlowPane resultsContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;

    private Future<?> currentDownloadTask; // Referencia para cancelar la descarga
    // Componentes para la barra de progreso visual
    private VBox progressOverlay;
    private ProgressBar progressBar;
    private Label progressTitle;
    private Label progressStatus;

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
            System.setProperty("https.protocols", "TLSv1.2"); // [FIX] Forzar TLS 1.2 para evitar handshake_failure
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) { }
    }

    public Parent getView() {
        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 15;");

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Mods y Contenido");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 0, 0);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Buscar mods, packs...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #555; -fx-border-radius: 5;");
        searchField.setOnAction(e -> { offset = 0; search(); });

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("Mods", "Resource Packs", "Shaders", "Modpacks / Mapas");
        typeFilter.setValue("Mods");
        typeFilter.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-base: #333;");
        typeFilter.setOnAction(e -> { offset = 0; search(); });

        sortFilter = new ComboBox<>();
        sortFilter.getItems().addAll("Relevancia", "Más Populares", "Recientes", "Actualizados");
        sortFilter.setValue("Relevancia");
        sortFilter.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-base: #333;");
        sortFilter.setOnAction(e -> { offset = 0; search(); });

        Button btnSearch = new Button("Buscar");
        btnSearch.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnSearch.setOnAction(e -> { offset = 0; search(); });

        Button btnPrev = new Button("<");
        btnPrev.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btnPrev.setOnAction(e -> {
            if (offset >= limit) {
                offset -= limit;
                search();
            }
        });

        Button btnNext = new Button(">");
        btnNext.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btnNext.setOnAction(e -> {
            offset += limit;
            search();
        });

        header.getChildren().addAll(title, spacer, btnPrev, btnNext, searchField, typeFilter, sortFilter, btnSearch);

        resultsContainer = new FlowPane();
        resultsContainer.setHgap(15);
        resultsContainer.setVgap(15);
        resultsContainer.setAlignment(Pos.TOP_LEFT);
        resultsContainer.setPadding(new Insets(10));
        resultsContainer.setPrefWrapLength(800); 
        
        scrollPane = new ScrollPane(resultsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        statusLabel = new Label("Listo para buscar. Usa el buscador para encontrar contenido.");
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");

        content.getChildren().addAll(header, statusLabel, scrollPane);

        // --- Overlay de Progreso (Panel que aparece al descargar) ---
        progressOverlay = new VBox(15);
        progressOverlay.setAlignment(Pos.CENTER);
        progressOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 15;");
        progressOverlay.setVisible(false); // Oculto por defecto

        progressTitle = new Label("Instalando Contenido");
        progressTitle.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);
        // [MEJORA] Barra de descarga personalizada con gradiente y bordes redondeados
        progressBar.setStyle("-fx-accent: #00b4db; -fx-control-inner-background: #222; -fx-background-radius: 15; -fx-padding: 3; -fx-background-insets: 0;");

        progressStatus = new Label("Preparando descarga...");
        progressStatus.setStyle("-fx-text-fill: #ccc; -fx-font-size: 14px;");

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 20;");
        btnCancel.setOnAction(e -> cancelDownload());

        progressOverlay.getChildren().addAll(progressTitle, progressBar, progressStatus, btnCancel);

        rootStack.getChildren().addAll(content, progressOverlay);

        search();

        return rootStack;
    }

    private void cancelDownload() {
        if (currentDownloadTask != null && !currentDownloadTask.isDone()) {
            currentDownloadTask.cancel(true); // Interrumpir el hilo
        }
        progressOverlay.setVisible(false);
        MainView.showNotification("Cancelado", "Descarga detenida por el usuario.", "info");
    }

    private void search() {
        String query = searchField.getText().trim();
        String type = typeFilter.getValue();
        String sort = sortFilter.getValue();
        
        statusLabel.setText("Buscando en Modrinth...");
        resultsContainer.getChildren().clear();

        executor.submit(() -> {
            try {
                String facetType = "mod";
                if (type.equals("Resource Packs")) facetType = "resourcepack";
                else if (type.equals("Shaders")) facetType = "shader";
                else if (type.equals("Modpacks / Mapas")) facetType = "modpack";

                String index = "relevance";
                if ("Más Populares".equals(sort)) index = "downloads";
                else if ("Recientes".equals(sort)) index = "newest";
                else if ("Actualizados".equals(sort)) index = "updated";

                String facets = String.format("[[\"project_type:%s\"]]", facetType);
                
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String encodedFacets = URLEncoder.encode(facets, StandardCharsets.UTF_8.toString());
                
                String urlStr = "https://api.modrinth.com/v2/search?query=" + encodedQuery + "&facets=" + encodedFacets + "&index=" + index + "&limit=" + limit + "&offset=" + offset;
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // [FIX] Asegurar uso de SSL parcheado
                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
                }
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // [FIX] Check content type to prevent parsing errors if the API returns something other than JSON.
                    String contentType = conn.getContentType();
                    if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                        JsonObject response = gson.fromJson(reader, JsonObject.class);
                        JsonArray hits = response.getAsJsonArray("hits");

                        Platform.runLater(() -> {
                            if (hits.size() == 0) {
                                statusLabel.setText("No se encontraron resultados para '" + query + "' en " + type + ".");
                            } else {
                                statusLabel.setText("Mostrando " + hits.size() + " resultados.");
                                for (JsonElement hit : hits) {
                                    resultsContainer.getChildren().add(createCard(hit.getAsJsonObject()));
                                }
                            }
                        });
                    } else {
                         Platform.runLater(() -> statusLabel.setText("Error: La API de Modrinth devolvió un formato inesperado."));
                    }
                } else {
                    Platform.runLater(() -> statusLabel.setText("Error en la API de Modrinth: " + responseCode));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Error de conexión: " + e.getMessage()));
            }
        });
    }

    private VBox createCard(JsonObject item) {
        VBox card = new VBox(10);
        card.setPrefSize(180, 260);
        card.setMaxSize(180, 260);
        card.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_CENTER);

        String iconUrl = item.has("icon_url") && !item.get("icon_url").isJsonNull() ? item.get("icon_url").getAsString() : null;
        ImageView icon = new ImageView();
        icon.setFitWidth(80);
        icon.setFitHeight(80);
        icon.setPreserveRatio(true);
        
        // [FIX] Carga robusta con User-Agent para evitar bloqueo de Modrinth (Error 403/429)
        Image placeholder = new Image("https://cdn.modrinth.com/assets/logo.png", 80, 80, true, true);
        icon.setImage(placeholder);

        if (iconUrl != null) {
            executor.submit(() -> {
                try {
                    URL url = new URL(iconUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    if (conn.getResponseCode() == 200) {
                        try (java.io.InputStream in = new java.io.BufferedInputStream(conn.getInputStream())) {
                            Image img = new Image(in, 128, 128, true, true);
                            Platform.runLater(() -> icon.setImage(img));
                        }
                    }
                } catch (Exception e) {}
            });
        }

        Label lblTitle = new Label(item.get("title").getAsString());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        lblTitle.setWrapText(true);
        lblTitle.setAlignment(Pos.CENTER);
        lblTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lblTitle.setMaxHeight(40);

        Label lblAuthor = new Label("Por: " + item.get("author").getAsString());
        lblAuthor.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnInstall = new Button("Instalar");
        btnInstall.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnInstall.setMaxWidth(Double.MAX_VALUE);
        btnInstall.setOnAction(e -> installProject(item));

        card.getChildren().addAll(icon, lblTitle, lblAuthor, spacer, btnInstall);
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,120,215,0.4), 10, 0, 0, 0); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #222; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);"));

        return card;
    }

    private void installProject(JsonObject item) {
        String slug = item.get("slug").getAsString();
        String title = item.get("title").getAsString();

        MainView.showNotification("Cargando", "Obteniendo versiones de " + title + "...", "info");

        executor.submit(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + slug + "/version");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
                }
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    JsonArray versions = gson.fromJson(reader, JsonArray.class);
                    Platform.runLater(() -> showVersionSelector(item, versions));
                } else {
                     Platform.runLater(() -> MainView.showNotification("Error", "Error al obtener versiones.", "error"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo de conexión: " + e.getMessage(), "error"));
            }
        });
    }

    private void showVersionSelector(JsonObject project, JsonArray versions) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Instalar " + project.get("title").getAsString());
        dialog.setHeaderText("Selecciona Versión y Cargador");

        ButtonType installBtnType = new ButtonType("Instalar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(installBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        ComboBox<String> cmbVersion = new ComboBox<>();
        ComboBox<String> cmbLoader = new ComboBox<>();
        ComboBox<String> cmbInstance = new ComboBox<>(); // [NUEVO] Selector de Instancia
        
        Set<String> gameVersions = new TreeSet<>(Comparator.reverseOrder());
        Set<String> loaders = new TreeSet<>();

        for (JsonElement e : versions) {
            JsonObject v = e.getAsJsonObject();
            JsonArray gvs = v.getAsJsonArray("game_versions");
            JsonArray lds = v.getAsJsonArray("loaders");
            
            for (JsonElement gv : gvs) gameVersions.add(gv.getAsString());
            for (JsonElement ld : lds) loaders.add(ld.getAsString());
        }

        cmbVersion.getItems().addAll(gameVersions);
        cmbLoader.getItems().addAll(loaders);

        if (!cmbVersion.getItems().isEmpty()) cmbVersion.getSelectionModel().selectFirst();
        if (!cmbLoader.getItems().isEmpty()) cmbLoader.getSelectionModel().selectFirst();

        // Cargar Instancias
        cmbInstance.getItems().add("(Ninguno por defecto)");
        File instancesDir = new File(DATA_DIR, "instances");
        if (instancesDir.exists() && instancesDir.isDirectory()) {
            File[] insts = instancesDir.listFiles(File::isDirectory);
            if (insts != null) {
                for (File f : insts) cmbInstance.getItems().add(f.getName());
            }
        }
        cmbInstance.getSelectionModel().selectFirst();

        grid.add(new Label("Versión de Minecraft:"), 0, 0);
        grid.add(cmbVersion, 1, 0);
        grid.add(new Label("Modloader:"), 0, 1);
        grid.add(cmbLoader, 1, 1);
        grid.add(new Label("Instalar en:"), 0, 2); // [NUEVO] Etiqueta más clara
        grid.add(cmbInstance, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == installBtnType) {
                String selectedVer = cmbVersion.getValue();
                String selectedLoader = cmbLoader.getValue();
                String selectedInstance = cmbInstance.getValue();
                
                for (JsonElement e : versions) {
                    JsonObject v = e.getAsJsonObject();
                    boolean verMatch = false;
                    boolean loaderMatch = false;
                    
                    for (JsonElement gv : v.getAsJsonArray("game_versions")) {
                        if (gv.getAsString().equals(selectedVer)) verMatch = true;
                    }
                    for (JsonElement ld : v.getAsJsonArray("loaders")) {
                        if (ld.getAsString().equals(selectedLoader)) loaderMatch = true;
                    }
                    
                    if (verMatch && loaderMatch) {
                        // Inyectar el nombre de la instancia en el objeto JSON para usarlo después
                        v.addProperty("_targetInstance", selectedInstance);
                        return v;
                    }
                }
            }
            return null;
        });

        Optional<JsonObject> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            processDownload(project, result.get());
        } else {
            // Si el usuario cancela o no hay coincidencia
            if (dialog.getResult() == null && dialog.getDialogPane().getScene().getWindow().isShowing() == false) {
                 // Cancelado
            } else if (dialog.getResult() == null) {
                 MainView.showNotification("Aviso", "No existe una versión con esa combinación.", "warning");
            }
        }
    }

    private void processDownload(JsonObject project, JsonObject versionObj) {
        String title = project.get("title").getAsString();
        String type = project.get("project_type").getAsString();
        
        JsonArray files = versionObj.getAsJsonArray("files");
        if (files.size() > 0) {
            JsonObject fileObj = files.get(0).getAsJsonObject();
            // Intentar buscar el archivo primario
            for (JsonElement f : files) {
                if (f.getAsJsonObject().has("primary") && f.getAsJsonObject().get("primary").getAsBoolean()) {
                    fileObj = f.getAsJsonObject();
                    break;
                }
            }

            String downloadUrl = fileObj.get("url").getAsString();
            String filename = fileObj.get("filename").getAsString();

            String folderName = "mods";
            if (type.equals("resourcepack")) folderName = "resourcepacks";
            else if (type.equals("shader")) folderName = "shaderpacks";
            else if (type.equals("modpack")) folderName = "modpacks";

            final String finalTargetFolder = folderName;
            
            // [NUEVO] Lógica de ruta de instancia
            File baseDir = new File(DATA_DIR);
            if (versionObj.has("_targetInstance")) {
                String instName = versionObj.get("_targetInstance").getAsString();
                if (!instName.equals("(Ninguno por defecto)")) {
                    baseDir = new File(DATA_DIR, "instances/" + instName);
                }
            }

            File destDir = new File(baseDir, finalTargetFolder);
            if (!destDir.exists()) destDir.mkdirs();

            File destFile = new File(destDir, filename);

            Platform.runLater(() -> {
                progressOverlay.setVisible(true);
                progressTitle.setText("Instalando " + title);
                progressStatus.setText("Descargando archivo: " + filename);
                progressBar.setProgress(0);
            });

            currentDownloadTask = executor.submit(() -> {
                try {
                    downloadFile(downloadUrl, destFile, (progress) -> Platform.runLater(() -> progressBar.setProgress(progress)));
                    Platform.runLater(() -> progressOverlay.setVisible(false));
                    Platform.runLater(() -> MainView.showNotification("Éxito", title + " instalado en " + finalTargetFolder, "success"));
                } catch (InterruptedException | java.io.InterruptedIOException e) {
                    // Limpiar archivo parcial si se cancela
                    if (destFile.exists()) destFile.delete();
                    Platform.runLater(() -> progressOverlay.setVisible(false));
                    // La notificación ya se muestra en cancelDownload, o aquí si fue error de red
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> progressOverlay.setVisible(false));
                    Platform.runLater(() -> MainView.showNotification("Error", "Fallo en la descarga: " + e.getMessage(), "error"));
                }
            });
        } else {
            Platform.runLater(() -> MainView.showNotification("Error", "La versión seleccionada no tiene archivos.", "error"));
        }
    }

    private void downloadFile(String urlStr, File dest, Consumer<Double> onProgress) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection) conn).setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
        }
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        long totalSize = conn.getContentLengthLong();
        long downloaded = 0;

        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] data = new byte[4096];
            int count;
            while ((count = in.read(data, 0, 4096)) != -1) {
                // [FIX] Verificar si se canceló la descarga
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Descarga cancelada");
                }
                out.write(data, 0, count);
                downloaded += count;
                if (totalSize > 0 && onProgress != null) {
                    double progress = (double) downloaded / totalSize;
                    onProgress.accept(progress);
                }
            }
        }
    }
}
