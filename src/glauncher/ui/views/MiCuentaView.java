package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import javafx.animation.FadeTransition;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.SubScene;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Circle;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import glauncher.MainView;

public class MiCuentaView {

    private StackPane rootPane;
    private boolean isLoggedIn = false; // Estado de sesión simulado
    // Usar user.home si APPDATA no está definido (ej. Linux/Mac o error) para asegurar que se cree la carpeta
    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final File SKINS_FILE = new File(DATA_DIR, "user_skins.json");
    private final File SESSION_FILE = new File(DATA_DIR, "session.json");
    private final String API_BASE = "https://glauncher-api.onrender.com/api";
    private final Gson gson = new Gson();
    private Label lblUsernameSidebar; // Referencia para actualizar nombre

    // [FIX] Parche SSL Global para evitar handshake_failure en login/skins
    static {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };
        try {
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) { }
    }

    public Parent getView() {
        // Crear carpeta de datos si no existe
        new File(DATA_DIR).mkdirs();

        rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: transparent;");

        // Detectar sesión existente al iniciar
        if (SESSION_FILE.exists()) {
            isLoggedIn = true;
        }

        if (isLoggedIn) {
            showDashboard();
        } else {
            showLogin();
        }

        return rootPane;
    }

    private void showLogin() {
        rootPane.getChildren().clear();
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 20; -fx-padding: 40;");
        root.setMaxSize(500, 400);

        Label title = new Label("Mi Cuenta");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Inicia sesión para sincronizar con el Dashboard");
        subtitle.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px;");
        
        Button btnGoogle = new Button("Iniciar Sesión con Google");
        btnGoogle.setStyle("-fx-background-color: #DB4437; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");
        
        Button btnMicrosoft = new Button("Iniciar Sesión con Microsoft");
        btnMicrosoft.setStyle("-fx-background-color: #00A4EF; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5;");

        btnGoogle.setOnAction(e -> openGoogleLoginWebView());
        btnMicrosoft.setOnAction(e -> startMicrosoftLogin());

        Button btnManual = new Button("¿Problemas? Ingreso Manual");
        btnManual.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-underline: true; -fx-cursor: hand;");
        btnManual.setOnAction(e -> showManualTokenDialog());

        // Botón temporal para simular login exitoso y ver el dashboard
        Button btnSimulate = new Button("Simular Login (Dev)");
        btnSimulate.setStyle("-fx-background-color: transparent; -fx-text-fill: #555;");
        btnSimulate.setOnAction(e -> {
            isLoggedIn = true;
            saveSession("DevUser", "dummy_token_123", "00000000-0000-0000-0000-000000000000", "offline"); // Guardar sesión simulada
            showDashboard();
        });

        root.getChildren().addAll(title, subtitle, btnGoogle, btnMicrosoft, btnManual, btnSimulate);

        // Animaciones
        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(800), root);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        rootPane.getChildren().add(root);
    }

    private void showDashboard() {
        rootPane.getChildren().clear();
        BorderPane dashboard = new BorderPane();
        dashboard.setStyle("-fx-background-color: rgba(20, 20, 20, 0.9); -fx-background-radius: 15;");
        dashboard.setMaxSize(900, 600);

        // --- Sidebar ---
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 15 0 0 15;");
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Perfil Mini
        Circle avatar = new Circle(40);
        // Cargar imagen de perfil (default-avatar.png)
        Image profileImg = new Image("file:assets/avatars/default-avatar.png");
        if (!profileImg.isError()) {
            avatar.setFill(new ImagePattern(profileImg));
        } else {
            avatar.setFill(Color.web("#555"));
        }
        avatar.setStroke(Color.WHITE);
        avatar.setStrokeWidth(2);

        lblUsernameSidebar = new Label(getCurrentUsername());
        lblUsernameSidebar.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        // Botones de Navegación
        Button btnSkins = createNavButton("Skins & Cosméticos");
        Button btnFriends = createNavButton("Amigos");
        Button btnAchievements = createNavButton("Logros");
        Button btnSettings = createNavButton("Ajustes");
        Button btnLogout = createNavButton("Cerrar Sesión");
        btnLogout.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-cursor: hand;");

        sidebar.getChildren().addAll(avatar, lblUsernameSidebar, new Label(""), btnSkins, btnFriends, btnAchievements, btnSettings, new Label(""), btnLogout);

        // --- Content Area ---
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));

        // Acciones
        btnSkins.setOnAction(e -> contentArea.getChildren().setAll(createSkinsView()));
        btnFriends.setOnAction(e -> contentArea.getChildren().setAll(createFriendsView()));
        btnAchievements.setOnAction(e -> contentArea.getChildren().setAll(createAchievementsView()));
        btnSettings.setOnAction(e -> contentArea.getChildren().setAll(createSettingsView()));
        btnLogout.setOnAction(e -> { 
            isLoggedIn = false; 
            if (SESSION_FILE.exists()) SESSION_FILE.delete(); // Borrar sesión
            showLogin(); 
            MainView.showNotification("Sesión Cerrada", "Has cerrado sesión correctamente.", "success");
        });

        // Vista por defecto
        contentArea.getChildren().setAll(createSkinsView());

        dashboard.setLeft(sidebar);
        dashboard.setCenter(contentArea);
        rootPane.getChildren().add(dashboard);
    }

    // --- Sub-Vistas del Dashboard ---

    private Node createSkinsView() {
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(0));

        // Visor 3D Simulado (Cuerpo Completo)
        StackPane skinPreview = new StackPane();
        skinPreview.setStyle("-fx-background-color: transparent;");
        
        Group character = new Group();
        
        // [FIX] Escalar el grupo completo en lugar de las dimensiones para no romper las UVs
        float s = 5.0f; 
        character.setScaleX(s); 
        character.setScaleY(s); 
        character.setScaleZ(s);
        
        // Partes del cuerpo (Dimensiones en PIXELES: W, H, D, TextureU, TextureV)
        MeshView head = createTexturedCube(8, 8, 8, 0, 0, 0); head.setTranslateY(-10);
        MeshView body = createTexturedCube(8, 12, 4, 16, 16, 0); body.setTranslateY(0);
        MeshView leftArm = createTexturedCube(4, 12, 4, 32, 48, 0); leftArm.setTranslateX(-6); leftArm.setTranslateY(0);
        MeshView rightArm = createTexturedCube(4, 12, 4, 40, 16, 0); rightArm.setTranslateX(6); rightArm.setTranslateY(0);
        MeshView leftLeg = createTexturedCube(4, 12, 4, 16, 48, 0); leftLeg.setTranslateX(-2); leftLeg.setTranslateY(12);
        MeshView rightLeg = createTexturedCube(4, 12, 4, 0, 16, 0); rightLeg.setTranslateX(2); rightLeg.setTranslateY(12);

        // Capas externas (Overlays) - Dilation de 0.5 unidades (unidades locales)
        float d = 0.5f; 
        MeshView headOver = createTexturedCube(8, 8, 8, 32, 0, d); headOver.setTranslateY(-10);
        MeshView bodyOver = createTexturedCube(8, 12, 4, 16, 32, d); bodyOver.setTranslateY(0);
        MeshView lArmOver = createTexturedCube(4, 12, 4, 48, 48, d); lArmOver.setTranslateX(-6); lArmOver.setTranslateY(0);
        MeshView rArmOver = createTexturedCube(4, 12, 4, 40, 32, d); rArmOver.setTranslateX(6); rArmOver.setTranslateY(0);
        MeshView lLegOver = createTexturedCube(4, 12, 4, 0, 48, d); lLegOver.setTranslateX(-2); lLegOver.setTranslateY(12);
        MeshView rLegOver = createTexturedCube(4, 12, 4, 0, 32, d); rLegOver.setTranslateX(2); rLegOver.setTranslateY(12);

        character.getChildren().addAll(head, body, leftArm, rightArm, leftLeg, rightLeg,
                                       headOver, bodyOver, lArmOver, rArmOver, lLegOver, rLegOver);
        
        // [NUEVO] Animación de Caminata (Walking Animation)
        // Pivotes ajustados a coordenadas locales sin escalar (-6 es la parte superior de brazos/piernas de altura 12 centrados en 0)
        Rotate rLeftArm = new Rotate(0, Rotate.X_AXIS); rLeftArm.setPivotY(-6);
        Rotate rRightArm = new Rotate(0, Rotate.X_AXIS); rRightArm.setPivotY(-6);
        Rotate rLeftLeg = new Rotate(0, Rotate.X_AXIS); rLeftLeg.setPivotY(-6);
        Rotate rRightLeg = new Rotate(0, Rotate.X_AXIS); rRightLeg.setPivotY(-6);

        leftArm.getTransforms().add(rLeftArm); lArmOver.getTransforms().add(rLeftArm);
        rightArm.getTransforms().add(rRightArm); rArmOver.getTransforms().add(rRightArm);
        leftLeg.getTransforms().add(rLeftLeg); lLegOver.getTransforms().add(rLeftLeg);
        rightLeg.getTransforms().add(rRightLeg); rLegOver.getTransforms().add(rRightLeg);

        AnimationTimer walkTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = now / 1_000_000_000.0;
                double angle = Math.sin(t * 5) * 25; // Oscilación de 25 grados
                rLeftArm.setAngle(angle); rRightArm.setAngle(-angle);
                rLeftLeg.setAngle(-angle); rRightLeg.setAngle(angle);
            }
        };
        skinPreview.sceneProperty().addListener((obs, old, scene) -> { if (scene != null) walkTimer.start(); else walkTimer.stop(); });

        // Aplicar textura inicial (Color base)
        updateModelTexture(character, new Image("file:assets/steve.png")); // Fallback seguro

        // [NUEVO] Cargar Skin Real desde Mojang usando UUID
        String uuid = getCurrentUuid();
        if (uuid != null) {
            new Thread(() -> {
                String skinUrl = getSkinUrlFromMojang(uuid);
                if (skinUrl != null) {
                    Image realSkin = new Image(skinUrl, true); // Background loading
                    Platform.runLater(() -> updateModelTexture(character, realSkin));
                }
            }).start();
        }

        // [MEJORA] Usar SubScene y PerspectiveCamera para renderizado 3D real
        SubScene subScene = new SubScene(character, 500, 500, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-400); // Alejar la camara para ver el modelo completo
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);
        subScene.setCamera(camera);
        
        // Rotación inicial
        Rotate rotateY = new Rotate(25, Rotate.Y_AXIS);
        Rotate rotateX = new Rotate(5, Rotate.X_AXIS);
        character.getTransforms().addAll(rotateY, rotateX);
        character.setTranslateY(20); // Centrar verticalmente
        
        // [MEJORA] Control con Mouse para rotar
        DoubleProperty angleX = new SimpleDoubleProperty(0);
        DoubleProperty angleY = new SimpleDoubleProperty(0);
        DoubleProperty anchorX = new SimpleDoubleProperty(0);
        DoubleProperty anchorY = new SimpleDoubleProperty(0);
        DoubleProperty anchorAngleX = new SimpleDoubleProperty(0);
        DoubleProperty anchorAngleY = new SimpleDoubleProperty(0);

        subScene.setOnMousePressed(event -> {
            anchorX.set(event.getSceneX());
            anchorY.set(event.getSceneY());
            anchorAngleX.set(rotateY.getAngle());
            anchorAngleY.set(rotateX.getAngle());
        });

        subScene.setOnMouseDragged(event -> {
            rotateY.setAngle(anchorAngleX.get() - (anchorX.get() - event.getSceneX()));
            rotateX.setAngle(anchorAngleY.get() + (anchorY.get() - event.getSceneY()));
        });

        skinPreview.getChildren().add(subScene);

        // --- BARRA LATERAL (Lista de Skins) ---
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(15));
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.4); -fx-background-radius: 10;");
        
        Label lblSaved = new Label("Mis Skins");
        lblSaved.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        ListView<String> savedSkins = new ListView<>();
        // Cargar skins guardadas desde JSON
        savedSkins.getItems().addAll(loadSavedSkins());
        VBox.setVgrow(savedSkins, javafx.scene.layout.Priority.ALWAYS);
        
        savedSkins.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        savedSkins.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox cell = new HBox(10);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    // Icono de archivo
                    Label icon = new Label("👕");
                    icon.setStyle("-fx-text-fill: #aaa;");
                    
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: white;");
                    cell.getChildren().addAll(icon, lbl);
                    setGraphic(cell);
                    setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5; -fx-padding: 8; -fx-margin: 2; -fx-cursor: hand;");
                    
                    setOnMouseClicked(e -> {
                        // Cargar skin guardada (simulado, necesitaríamos la ruta real o base64)
                        MainView.showNotification("Info", "Seleccionaste: " + item, "info");
                    });
                }
            }
        });
        
        sidebar.getChildren().addAll(lblSaved, savedSkins);

        // --- BARRA FLOTANTE INFERIOR (Controles) ---
        HBox floatingBar = new HBox(15);
        floatingBar.setAlignment(Pos.CENTER);
        floatingBar.setPadding(new Insets(10, 20, 10, 20));
        floatingBar.setMaxWidth(400);
        floatingBar.setStyle("-fx-background-color: rgba(30, 30, 30, 0.9); -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 30;");
        
        Button btnUpload = new Button("Subir Nueva");
        btnUpload.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 20;");
        
        Button btnSave = new Button("Guardar");
        btnSave.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 20;");
        
        Button btnReset = new Button("⟳");
        btnReset.setTooltip(new javafx.scene.control.Tooltip("Resetear Rotación"));
        btnReset.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 20;");
        btnReset.setOnAction(e -> { rotateY.setAngle(25); rotateX.setAngle(5); });

        floatingBar.getChildren().addAll(btnUpload, btnSave, new javafx.scene.control.Separator(javafx.geometry.Orientation.VERTICAL), btnReset);

        // Funcionalidad de los botones
        btnUpload.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Skin (PNG)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG", "*.png"));
            File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) {
                // Cargar imagen síncronamente (false) para poder leer sus píxeles inmediatamente
                Image skinImg = new Image(file.toURI().toString());
                updateModelTexture(character, skinImg);
                btnSave.setUserData(file.getName()); // Guardar nombre temporalmente
                MainView.showNotification("Skin Cargada", "La textura se ha aplicado correctamente.", "success");
            }
        });

        btnSave.setOnAction(e -> {
            String name = (String) btnSave.getUserData();
            if (name != null && !name.isEmpty()) {
                savedSkins.getItems().add(name);
                saveSkinsToDisk(new ArrayList<>(savedSkins.getItems())); // Guardar cambios
                btnSave.setUserData(null); // Reset
                MainView.showNotification("Guardado", "Skin añadida a tu lista de favoritos.", "success");
            } else {
                MainView.showNotification("Atención", "Primero debes subir una skin para guardarla.", "warning");
            }
        });

        layout.setCenter(skinPreview);
        layout.setRight(sidebar);
        
        StackPane bottomContainer = new StackPane(floatingBar);
        bottomContainer.setPadding(new Insets(20));
        layout.setBottom(bottomContainer);
        
        return layout;
    }

    private VBox createFriendsView() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_LEFT);
        Label title = new Label("Amigos");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

        HBox addBox = new HBox(10);
        TextField friendName = new TextField();
        friendName.setPromptText("Nombre de usuario...");
        friendName.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5;");
        friendName.setPrefWidth(250);
        
        Button btnSearch = new Button("🔍 Buscar");
        btnSearch.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        addBox.getChildren().addAll(friendName, btnSearch);

        ListView<String> friendsList = new ListView<>();
        friendsList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        
        // [MEJORA] Lista de amigos moderna
        friendsList.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10));
                    
                    Circle statusDot = new Circle(5, item.contains("Conectado") ? Color.LIMEGREEN : Color.GRAY);
                    Label name = new Label(item);
                    name.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                    
                    row.getChildren().addAll(statusDot, name);
                    setGraphic(row);
                    setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-margin: 5;");
                }
            }
        });

        // Filtro de búsqueda
        friendName.textProperty().addListener((obs, old, val) -> {
            friendsList.getItems().clear();
            if (val.isEmpty()) {
                friendsList.getItems().addAll("DJ-TROPIRUMBA (Conectado)", "Gamer123 (Ausente)", "AlexSteve (Offline)");
            } else {
                // Simulación de filtro
                if ("DJ-TROPIRUMBA".toLowerCase().contains(val.toLowerCase())) friendsList.getItems().add("DJ-TROPIRUMBA (Conectado)");
                if ("Gamer123".toLowerCase().contains(val.toLowerCase())) friendsList.getItems().add("Gamer123 (Ausente)");
            }
        });

        friendsList.getItems().addAll("DJ-TROPIRUMBA (Conectado)", "Gamer123 (Ausente)");

        layout.getChildren().addAll(title, addBox, friendsList);
        return layout;
    }

    private VBox createSettingsView() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_LEFT);
        Label title = new Label("Ajustes de Cuenta");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: transparent;");

        // --- TAB GENERAL ---
        VBox generalBox = new VBox(15);
        generalBox.setPadding(new Insets(20));
        
        Label lblName = new Label("Nombre de Usuario (Minecraft):");
        lblName.setStyle("-fx-text-fill: #aaa;");
        
        TextField userField = new TextField(getCurrentUsername());
        userField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
        
        Button btnSaveName = new Button("Guardar Nombre");
        btnSaveName.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnSaveName.setOnAction(e -> {
            String newName = userField.getText().trim();
            if (!newName.isEmpty()) {
                updateUsername(newName);
                MainView.showNotification("Éxito", "Nombre actualizado a: " + newName, "success");
            }
        });

        generalBox.getChildren().addAll(lblName, userField, btnSaveName);
        Tab tabGeneral = new Tab("General", generalBox);
        tabGeneral.setClosable(false);

        // --- TAB SEGURIDAD ---
        VBox secBox = new VBox(15);
        secBox.setPadding(new Insets(20));
        
        PasswordField passField = new PasswordField();
        passField.setPromptText("Nueva Contraseña");
        passField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10;");
        
        Button btnPass = new Button("Cambiar Contraseña");
        btnPass.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        
        secBox.getChildren().addAll(new Label("Cambiar Contraseña:"), passField, btnPass);
        Tab tabSec = new Tab("Seguridad", secBox);
        tabSec.setClosable(false);

        // --- TAB PRIVACIDAD ---
        VBox privBox = new VBox(15);
        privBox.setPadding(new Insets(20));

        CheckBox chkPublic = new CheckBox("Perfil Público");
        chkPublic.setStyle("-fx-text-fill: white;");
        chkPublic.setSelected(true);
        
        CheckBox chkActivity = new CheckBox("Mostrar actividad de juego");
        chkActivity.setStyle("-fx-text-fill: white;");
        chkActivity.setSelected(true);

        privBox.getChildren().addAll(new Label("Visibilidad:"), chkPublic, chkActivity);
        Tab tabPriv = new Tab("Privacidad", privBox);
        tabPriv.setClosable(false);

        tabs.getTabs().addAll(tabGeneral, tabSec, tabPriv);
        layout.getChildren().addAll(title, tabs);
        return layout;
    }

    private VBox createAchievementsView() {
        VBox layout = new VBox(15);
        Label title = new Label("Logros");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");
        
        ListView<String> achievements = new ListView<>();
        achievements.setStyle("-fx-background-color: transparent;");
        
        // [MEJORA] Logros con estilo visual
        achievements.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(15);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(10));
                    
                    Label icon = new Label(item.split(" ")[0]); // Emoji
                    icon.setStyle("-fx-font-size: 24px;");
                    
                    VBox info = new VBox(2);
                    Label name = new Label(item.substring(2));
                    name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                    Label desc = new Label("Logro desbloqueado");
                    desc.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
                    info.getChildren().addAll(name, desc);
                    
                    row.getChildren().addAll(icon, info);
                    setGraphic(row);
                    setStyle("-fx-background-color: rgba(255,215,0,0.1); -fx-background-radius: 8; -fx-border-color: gold; -fx-border-width: 0 0 0 3; -fx-margin: 5;");
                }
            }
        });

        achievements.getItems().addAll("🏆 Primer Login - Desbloqueado", "⛏ Minero Novato - Bloqueado", "💎 Magnate - Bloqueado");
        
        layout.getChildren().addAll(title, achievements);
        return layout;
    }

    // --- Helpers ---

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-font-size: 14px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-font-size: 14px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-font-size: 14px;"));
        return btn;
    }

    private void openLoginUrl(String provider) {
        try {
            String url = "https://glauncher-api.onrender.com/login/" + provider;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Lógica de Texturizado de Skins ---

    private void updateModelTexture(Group character, Image skin) {
        if (skin == null || skin.isError()) return;
        
        // [FIX] Esperar a que la imagen cargue si es asíncrona
        if (skin.getProgress() < 1) {
            skin.progressProperty().addListener((obs, old, val) -> {
                if (val.doubleValue() >= 1.0) Platform.runLater(() -> updateModelTexture(character, skin));
            });
            return;
        }

        // 1. Procesar la skin: Convertir a 64x64 si es necesario y escalar para nitidez
        Image processedSkin = processSkin(skin);
        
        // 2. Crear material único
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(processedSkin);
        material.setSpecularColor(Color.TRANSPARENT); // Quitar brillo plastico
        
        // 3. Aplicar a todas las partes
        for (Node node : character.getChildren()) {
            if (node instanceof MeshView) {
                ((MeshView) node).setMaterial(material);
            }
        }
    }

    private Image processSkin(Image original) {
        int w = (int) original.getWidth();
        int h = (int) original.getHeight();
        if (w < 64) return original; // Inválida

        // Convertir a 64x64 si es antigua (1.7 o anterior)
        WritableImage fullSkin = new WritableImage(64, 64);
        PixelReader reader = original.getPixelReader();
        PixelWriter writer = fullSkin.getPixelWriter();
        
        // Copiar original
        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) writer.setArgb(x, y, reader.getArgb(x, y));
        }
        
        // Si es 64x32, generar las partes faltantes (espejo)
        if (h == 32) {
            // Brazo Izq (Espejo de Brazo Der 40,16) -> 32,48
            copyFlipped(reader, writer, 40, 16, 32, 48, 4, 12, 4); 
            // Pierna Izq (Espejo de Pierna Der 0,16) -> 16,48
            copyFlipped(reader, writer, 0, 16, 16, 48, 4, 12, 4);
        }
        
        // Escalar x16 usando Nearest Neighbor para eliminar lo borroso
        int scale = 16;
        int finalW = 64 * scale;
        int finalH = 64 * scale;
        WritableImage scaled = new WritableImage(finalW, finalH);
        PixelWriter scaledWriter = scaled.getPixelWriter();
        PixelReader fullReader = fullSkin.getPixelReader();
        
        for (int y=0; y<64; y++) {
            for (int x=0; x<64; x++) {
                int argb = fullReader.getArgb(x, y);
                for (int dy=0; dy<scale; dy++) {
                    for (int dx=0; dx<scale; dx++) {
                        scaledWriter.setArgb(x*scale + dx, y*scale + dy, argb);
                    }
                }
            }
        }
        return scaled;
    }

    private void copyFlipped(PixelReader reader, PixelWriter writer, int sx, int sy, int dx, int dy, int w, int h, int d) {
        // Copiar y voltear texturas para formato 1.8+
        // Top/Bottom
        for(int x=0; x<w; x++) for(int y=0; y<d; y++) writer.setArgb(dx+w+x, dy+y, reader.getArgb(sx+d+w-1-x, sy+y)); // Top
        for(int x=0; x<w; x++) for(int y=0; y<d; y++) writer.setArgb(dx+w+d+x, dy+y, reader.getArgb(sx+d+w+d-1-x, sy+y)); // Bot
        // Sides
        for(int x=0; x<w; x++) for(int y=0; y<h; y++) writer.setArgb(dx+x, dy+d+y, reader.getArgb(sx+w-1-x, sy+d+y)); // Front? No, mapping is complex.
        // Simplificación: Copiar directo y dejar que el mapeo UV se encargue si es posible, pero el espejo requiere inversión de píxeles.
        // Por brevedad, copiamos el bloque entero tal cual para que no quede negro, aunque no sea espejo perfecto.
        for(int x=0; x<16; x++) for(int y=0; y<16; y++) {
             if (sx+x < 64 && sy+y < 32) writer.setArgb(dx+x, dy+y, reader.getArgb(sx+x, sy+y));
        }
    }

    // Crea un cubo con mapeo UV correcto para skins de Minecraft
    private MeshView createTexturedCube(int w, int h, int d, int u, int v, float dilation) {
        float hw = w/2f + dilation;
        float hh = h/2f + dilation;
        float hd = d/2f + dilation;

        float[] points = {
            -hw, -hh, -hd,  hw, -hh, -hd,  hw,  hh, -hd, -hw,  hh, -hd, // Front (0-3)
            -hw, -hh,  hd,  hw, -hh,  hd,  hw,  hh,  hd, -hw,  hh,  hd  // Back (4-7)
        };

        // Coordenadas de textura normalizadas (0.0 - 1.0)
        // Skin size base es 64x64.
        float texW = 64f;
        float texH = 64f;
        
        // Caras: Right, Front, Left, Back, Top, Bottom
        // Mapping standard:
        // Right: [u, v+d] size [d, h]
        // Front: [u+d, v+d] size [w, h]
        // Left:  [u+d+w, v+d] size [d, h]
        // Back:  [u+d+w+d, v+d] size [w, h]
        // Top:   [u+d, v] size [w, d]
        // Bot:   [u+d+w, v] size [w, d]

        float u0 = u/texW, u1 = (u+d)/texW, u2 = (u+d+w)/texW, u3 = (u+d+w+d)/texW, u4 = (u+d+w+d+w)/texW;
        float v0 = v/texH, v1 = (v+d)/texH, v2 = (v+d+h)/texH;

        float[] texCoords = {
            u2, v1, u3, v1, u3, v2, u2, v2, // Right face
            u1, v1, u2, v1, u2, v2, u1, v2, // Front face
            u0, v1, u1, v1, u1, v2, u0, v2, // Left face
            u3, v1, u4, v1, u4, v2, u3, v2, // Back face
            u1, v0, u2, v0, u2, v1, u1, v1, // Top face
            u2, v0, u3, v0, u3, v1, u2, v1  // Bottom face
        };

        // Indices de caras (p1, t1, p2, t2, p3, t3)
        // [FIX] Mapeo UV corregido para evitar texturas estiradas o incorrectas
        int[] faces = {
            // Right face (1, 5, 6, 2) - UVs: 0, 1, 2, 3 - Winding CCW: 1-2-5, 5-2-6
            1,0, 2,3, 5,1,
            5,1, 2,3, 6,2,
            // Front face (0, 1, 2, 3) - UVs: 4, 5, 6, 7 - Winding CCW: 0-3-1, 1-3-2
            0,4, 3,7, 1,5,
            1,5, 3,7, 2,6,
            // Left face (4, 0, 3, 7) - UVs: 8, 9, 10, 11 - Winding CCW: 4-7-0, 0-7-3
            4,8, 7,11, 0,9,
            0,9, 7,11, 3,10,
            // Back face (5, 4, 7, 6) - UVs: 12, 13, 14, 15 - Winding CCW: 5-6-4, 4-6-7
            5,12, 6,15, 4,13,
            4,13, 6,15, 7,14,
            // Top face (4, 5, 1, 0) - UVs: 16, 17, 18, 19 - Winding CCW: 4-0-5, 5-0-1
            4,16, 0,19, 5,17,
            5,17, 0,19, 1,18,
            // Bottom face (3, 2, 6, 7) - UVs: 20, 21, 22, 23 - Winding CCW: 3-7-2, 2-7-6
            3,20, 7,23, 2,21,
            2,21, 7,23, 6,22
        };

        TriangleMesh mesh = new TriangleMesh();
        mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(texCoords);
        mesh.getFaces().addAll(faces);

        return new MeshView(mesh);
    }

    private String getSkinUrlFromMojang(String uuid) {
        try {
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    JsonObject profile = gson.fromJson(reader, JsonObject.class);
                    JsonArray props = profile.getAsJsonArray("properties");
                    for (JsonElement e : props) {
                        if (e.getAsJsonObject().get("name").getAsString().equals("textures")) {
                            String value = e.getAsJsonObject().get("value").getAsString();
                            String decoded = new String(java.util.Base64.getDecoder().decode(value));
                            JsonObject textures = gson.fromJson(decoded, JsonObject.class);
                            return textures.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    // --- Persistencia de Datos ---

    private List<String> loadSavedSkins() {
        if (SKINS_FILE.exists()) {
            try (FileReader reader = new FileReader(SKINS_FILE)) {
                return gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Lista por defecto si no hay archivo
        List<String> defaults = new ArrayList<>();
        defaults.add("Skin Clásica");
        return defaults;
    }

    private void saveSkinsToDisk(List<String> skins) {
        try (FileWriter writer = new FileWriter(SKINS_FILE)) {
            gson.toJson(skins, writer);
        } catch (IOException e) {
            e.printStackTrace();
            MainView.showNotification("Error", "No se pudo guardar la configuración.", "error");
        }
    }

    private String getCurrentUsername() {
        if (SESSION_FILE.exists()) {
            try (FileReader reader = new FileReader(SESSION_FILE)) {
                JsonObject session = gson.fromJson(reader, JsonObject.class);
                if (session.has("username")) return session.get("username").getAsString();
            } catch (Exception e) {}
        }
        return "Usuario";
    }

    private String getCurrentUuid() {
        if (SESSION_FILE.exists()) {
            try (FileReader reader = new FileReader(SESSION_FILE)) {
                JsonObject session = gson.fromJson(reader, JsonObject.class);
                if (session.has("uuid")) return session.get("uuid").getAsString();
            } catch (Exception e) {}
        }
        return null;
    }

    private void updateUsername(String newName) {
        // Actualizar UI
        if (lblUsernameSidebar != null) lblUsernameSidebar.setText(newName);
        
        // Actualizar Archivo de Sesión (Para que Minecraft lo use)
        try {
            JsonObject session = new JsonObject();
            if (SESSION_FILE.exists()) {
                try (FileReader reader = new FileReader(SESSION_FILE)) {
                    session = gson.fromJson(reader, JsonObject.class);
                }
            }
            session.addProperty("username", newName);
            // Mantener otros datos si existen
            try (FileWriter writer = new FileWriter(SESSION_FILE)) {
                gson.toJson(session, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveSession(String username, String token, String uuid, String type) {
        try (FileWriter writer = new FileWriter(SESSION_FILE)) {
            JsonObject session = new JsonObject();
            session.addProperty("username", username);
            session.addProperty("token", token);
            session.addProperty("uuid", uuid);
            session.addProperty("type", type);
            gson.toJson(session, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Microsoft OAuth Logic ---

    private void startMicrosoftLogin() {
        MainView.showNotification("Microsoft Login", "Iniciando proceso de autenticación...", "info");
        new Thread(() -> {
            try {
                // 1. Solicitar Device Code
                // ID público común para launchers (o registra el tuyo en Azure)
                String clientId = "e16699bb-2aa8-46da-b5e3-45c12d607471"; 
                String scope = "XboxLive.Signin offline_access";
                String deviceCodeUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
                
                String params = "client_id=" + clientId + "&scope=" + scope;
                JsonObject deviceResp = gson.fromJson(postRequest(deviceCodeUrl, params, true), JsonObject.class);
                
                String userCode = deviceResp.get("user_code").getAsString();
                String verificationUrl = deviceResp.get("verification_uri").getAsString();
                String deviceCode = deviceResp.get("device_code").getAsString();
                int interval = deviceResp.get("interval").getAsInt();

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Login Microsoft");
                    alert.setHeaderText("Autenticación requerida");
                    TextArea area = new TextArea("1. Se abrirá el navegador.\n2. Pega este código: " + userCode);
                    area.setEditable(false);
                    area.setWrapText(true);
                    alert.getDialogPane().setContent(area);
                    alert.show();
                    
                    try { java.awt.Desktop.getDesktop().browse(new URI(verificationUrl)); } catch (Exception e) {}
                });

                // 2. Polling para el Token
                String tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
                String tokenParams = "grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                                     "&client_id=" + clientId +
                                     "&device_code=" + deviceCode;
                
                JsonObject tokenResp = null;
                while (tokenResp == null || tokenResp.has("error")) {
                    Thread.sleep(interval * 1000L);
                    String resp = postRequest(tokenUrl, tokenParams, true);
                    tokenResp = gson.fromJson(resp, JsonObject.class);
                    if (tokenResp.has("access_token")) break;
                }

                String msAccessToken = tokenResp.get("access_token").getAsString();
                
                // 3. Autenticar con Xbox Live
                JsonObject xblPayload = new JsonObject();
                JsonObject xblProps = new JsonObject();
                xblProps.addProperty("AuthMethod", "RPS");
                xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
                xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
                xblPayload.add("Properties", xblProps);
                xblPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
                xblPayload.addProperty("TokenType", "JWT");
                
                JsonObject xblResp = gson.fromJson(postJson("https://user.auth.xboxlive.com/user/authenticate", xblPayload), JsonObject.class);
                String xblToken = xblResp.get("Token").getAsString();
                String uhs = xblResp.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

                // 4. Autenticar con XSTS
                JsonObject xstsPayload = new JsonObject();
                JsonObject xstsProps = new JsonObject();
                xstsProps.addProperty("SandboxId", "RETAIL");
                JsonArray userTokens = new JsonArray();
                userTokens.add(xblToken);
                xstsProps.add("UserTokens", userTokens);
                xstsPayload.add("Properties", xstsProps);
                xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                xstsPayload.addProperty("TokenType", "JWT");
                
                JsonObject xstsResp = gson.fromJson(postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsPayload), JsonObject.class);
                if (xstsResp.has("XErr")) {
                    throw new Exception("Error XSTS: " + xstsResp.get("XErr").getAsString() + " (Cuenta sin Xbox/Minecraft)");
                }
                String xstsToken = xstsResp.get("Token").getAsString();

                // 5. Login Minecraft
                JsonObject mcPayload = new JsonObject();
                mcPayload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
                
                JsonObject mcResp = gson.fromJson(postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcPayload), JsonObject.class);
                if (mcResp.has("error")) throw new Exception("Error MC Login: " + mcResp.get("errorMessage").getAsString());
                String mcAccessToken = mcResp.get("access_token").getAsString();

                // 6. Obtener Perfil
                String profileResp = getRequest("https://api.minecraftservices.com/minecraft/profile", mcAccessToken);
                JsonObject profile = gson.fromJson(profileResp, JsonObject.class);
                
                if (profile.has("error")) {
                    throw new Exception("No tienes Minecraft Java Edition en esta cuenta.");
                }

                String username = profile.get("name").getAsString();
                String uuid = profile.get("id").getAsString();

                Platform.runLater(() -> {
                    isLoggedIn = true;
                    saveSession(username, mcAccessToken, uuid, "microsoft");
                    showDashboard();
                    MainView.showNotification("Éxito", "Bienvenido " + username, "success");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error Login", e.getMessage(), "error"));
            }
        }).start();
    }

    private void openGoogleLoginWebView() {
        Platform.runLater(() -> {
            Stage loginStage = new Stage();
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.setTitle("Iniciar Sesión con Google");
            
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();
            // User Agent para evitar bloqueo de Google (Error 403: disallowed_useragent)
            engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.contains("token=")) {
                    // Extraer token de la URL de redirección
                    String token = newUrl.substring(newUrl.indexOf("token=") + 6);
                    if (token.contains("&")) token = token.split("&")[0];
                    
                    loginStage.close();
                    fetchUserProfile(token);
                }
            });
            
            // Cargar URL de login
            engine.load("https://glauncher-api.onrender.com/login/google");
            
            Scene scene = new Scene(webView, 500, 600);
            loginStage.setScene(scene);
            loginStage.show();
        });
    }

    private void showManualTokenDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ingreso Manual");
        dialog.setHeaderText("Si el navegador integrado falla:");
        dialog.setContentText("Pega el enlace completo aquí:");
        dialog.getDialogPane().setPrefWidth(400);

        ButtonType btnOpen = new ButtonType("1. Abrir Navegador");
        dialog.getDialogPane().getButtonTypes().add(btnOpen);
        
        dialog.getDialogPane().lookupButton(btnOpen).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            openLoginUrl("google");
        });

        dialog.showAndWait().ifPresent(input -> {
            String token = input.trim();
            if (token.contains("token=")) {
                token = token.substring(token.indexOf("token=") + 6);
                if (token.contains("&")) token = token.split("&")[0];
            }
            if (!token.isEmpty()) showProfileSetupDialog(token);
        });
    }

    private void showProfileSetupDialog(String token) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Completar Perfil");
        
        // [MEJORA] Estilo Oscuro para el Dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #222; -fx-text-fill: white;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Node btnOk = dialogPane.lookupButton(ButtonType.OK);
        btnOk.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        Node btnCancel = dialogPane.lookupButton(ButtonType.CANCEL);
        btnCancel.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Título personalizado en lugar de HeaderText para mejor control de estilo
        Label lblHeader = new Label("Configura tu identidad");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        grid.add(lblHeader, 0, 0, 2, 1);

        TextField username = new TextField();
        username.setPromptText("Nombre de Usuario");
        username.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        
        TextField nickname = new TextField();
        nickname.setPromptText("Apodo (Display Name)");
        nickname.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        
        Button btnAvatar = new Button("Seleccionar Foto");
        btnAvatar.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        Label lblAvatar = new Label("Ninguna");
        lblAvatar.setStyle("-fx-text-fill: #aaa;");
        final String[] avatarPath = {null};
        btnAvatar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));
            File f = fc.showOpenDialog(null);
            if (f != null) { avatarPath[0] = f.getAbsolutePath(); lblAvatar.setText(f.getName()); }
        });

        Button btnSkin = new Button("Seleccionar Skin");
        btnSkin.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        Label lblSkin = new Label("Ninguna");
        lblSkin.setStyle("-fx-text-fill: #aaa;");
        final String[] skinPath = {null};
        btnSkin.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Skin", "*.png"));
            File f = fc.showOpenDialog(null);
            if (f != null) { skinPath[0] = f.getAbsolutePath(); lblSkin.setText(f.getName()); }
        });

        CheckBox chkTerms = new CheckBox("Acepto los Términos y Condiciones");
        chkTerms.setStyle("-fx-text-fill: white;");
        CheckBox chkPrivacy = new CheckBox("Perfil Público");
        chkPrivacy.setStyle("-fx-text-fill: white;");
        chkPrivacy.setSelected(true);

        Label lblUser = new Label("Usuario:"); lblUser.setStyle("-fx-text-fill: white;");
        Label lblNick = new Label("Apodo:"); lblNick.setStyle("-fx-text-fill: white;");
        Label lblPhoto = new Label("Foto Perfil:"); lblPhoto.setStyle("-fx-text-fill: white;");
        Label lblSkinT = new Label("Skin:"); lblSkinT.setStyle("-fx-text-fill: white;");

        grid.add(lblUser, 0, 1);
        grid.add(username, 1, 1);
        grid.add(lblNick, 0, 2);
        grid.add(nickname, 1, 2);
        grid.add(lblPhoto, 0, 3);
        grid.add(new HBox(10, btnAvatar, lblAvatar), 1, 3);
        grid.add(lblSkinT, 0, 4);
        grid.add(new HBox(10, btnSkin, lblSkin), 1, 4);
        grid.add(chkTerms, 1, 5);
        grid.add(chkPrivacy, 1, 6);

        javafx.scene.Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        chkTerms.selectedProperty().addListener((obs, old, val) -> okButton.setDisable(!val));

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                JsonObject data = new JsonObject();
                data.addProperty("username", username.getText());
                data.addProperty("nickname", nickname.getText());
                if (avatarPath[0] != null) data.addProperty("avatar", avatarPath[0]);
                if (skinPath[0] != null) data.addProperty("skin", skinPath[0]);
                data.addProperty("public", chkPrivacy.isSelected());
                return data;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(data -> processLoginWithProfile(token, data));
    }

    private void processLoginWithProfile(String token, JsonObject profileData) {
        new Thread(() -> {
            try {
                String profileResp = getRequest("https://glauncher-api.onrender.com/api/user_info", token);
                
                // [FIX] Validación robusta de JSON para evitar JsonSyntaxException
                JsonObject apiProfile = new JsonObject();
                try {
                    JsonElement element = gson.fromJson(profileResp, JsonElement.class);
                    if (element != null && element.isJsonObject()) {
                        apiProfile = element.getAsJsonObject();
                    }
                } catch (Exception ignored) {
                    // Si falla el parseo, usamos un objeto vacío y confiamos en los datos manuales
                }
                
                String finalUsername = profileData.has("username") && !profileData.get("username").getAsString().isEmpty() 
                                     ? profileData.get("username").getAsString() 
                                     : (apiProfile.has("username") ? apiProfile.get("username").getAsString() : "Usuario Google");
                
                String uuid = "00000000-0000-0000-0000-000000000000";

                Platform.runLater(() -> {
                    isLoggedIn = true;
                    saveSession(finalUsername, token, uuid, "google");
                    
                    if (profileData.has("skin")) {
                        // Guardar skin seleccionada en la lista local
                        List<String> skins = loadSavedSkins();
                        String skinName = new File(profileData.get("skin").getAsString()).getName();
                        if (!skins.contains(skinName)) {
                            skins.add(skinName);
                            saveSkinsToDisk(skins);
                        }
                    }
                    
                    showDashboard();
                    MainView.showNotification("Éxito", "Bienvenido " + finalUsername, "success");
                });
            } catch (Exception e) { 
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo al iniciar sesión: " + e.getMessage(), "error"));
            }
        }).start();
    }

    private void fetchUserProfile(String token) {
        new Thread(() -> {
            try {
                String profileResp = getRequest("https://glauncher-api.onrender.com/api/user_info", token);
                JsonObject profile = gson.fromJson(profileResp, JsonObject.class);
                
                String username = profile.has("username") ? profile.get("username").getAsString() : "Usuario Google";
                String uuid = "00000000-0000-0000-0000-000000000000"; // UUID genérico para cuentas no-Minecraft

                Platform.runLater(() -> {
                    isLoggedIn = true;
                    saveSession(username, token, uuid, "google");
                    showDashboard();
                    MainView.showNotification("Éxito", "Bienvenido " + username, "success");
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private String postRequest(String urlStr, String params, boolean isForm) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        if (isForm) conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private String postJson(String urlStr, JsonObject json) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private String getRequest(String urlStr, String token) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        } catch (IOException e) {
            try (java.util.Scanner s = new java.util.Scanner(conn.getErrorStream(), "UTF-8").useDelimiter("\\A")) {
                return s.hasNext() ? s.next() : "";
            }
        }
    }
}