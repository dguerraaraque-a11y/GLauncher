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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
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

        btnGoogle.setOnAction(e -> openLoginUrl("google"));
        btnMicrosoft.setOnAction(e -> startMicrosoftLogin());

        // Botón temporal para simular login exitoso y ver el dashboard
        Button btnSimulate = new Button("Simular Login (Dev)");
        btnSimulate.setStyle("-fx-background-color: transparent; -fx-text-fill: #555;");
        btnSimulate.setOnAction(e -> {
            isLoggedIn = true;
            saveSession("DevUser", "dummy_token_123", "00000000-0000-0000-0000-000000000000"); // Guardar sesión simulada
            showDashboard();
        });

        root.getChildren().addAll(title, subtitle, btnGoogle, btnMicrosoft, btnSimulate);

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

    private VBox createSkinsView() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Gestor de Skins");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

        // Visor 3D Simulado (Cuerpo Completo)
        StackPane skinPreview = new StackPane();
        skinPreview.setPrefSize(300, 400);
        
        Group character = new Group();
        
        double s = 5.0; // Escala del modelo
        
        // Cabeza (8x8x8)
        Box head = new Box(8*s, 8*s, 8*s);
        head.setTranslateY(-10*s);
        
        // Cuerpo (8x12x4)
        Box body = new Box(8*s, 12*s, 4*s);
        body.setTranslateY(0);
        
        // Brazos (4x12x4)
        Box leftArm = new Box(4*s, 12*s, 4*s);
        leftArm.setTranslateX(-6*s);
        leftArm.setTranslateY(0);
        
        Box rightArm = new Box(4*s, 12*s, 4*s);
        rightArm.setTranslateX(6*s);
        rightArm.setTranslateY(0);
        
        // Piernas (4x12x4)
        Box leftLeg = new Box(4*s, 12*s, 4*s);
        leftLeg.setTranslateX(-2*s);
        leftLeg.setTranslateY(12*s);
        
        Box rightLeg = new Box(4*s, 12*s, 4*s);
        rightLeg.setTranslateX(2*s);
        rightLeg.setTranslateY(12*s);

        // [NUEVO] Capas externas (Overlays/Ropa) para efecto 3D completo
        double os = s * 1.12; // Escala ligeramente mayor para la ropa
        Box headOver = new Box(8*os, 8*os, 8*os); headOver.setTranslateY(-10*s);
        Box bodyOver = new Box(8*os, 12*os, 4*os); bodyOver.setTranslateY(0);
        Box lArmOver = new Box(4*os, 12*os, 4*os); lArmOver.setTranslateX(-6*s);
        Box rArmOver = new Box(4*os, 12*os, 4*os); rArmOver.setTranslateX(6*s);
        Box lLegOver = new Box(4*os, 12*os, 4*os); lLegOver.setTranslateX(-2*s); lLegOver.setTranslateY(12*s);
        Box rLegOver = new Box(4*os, 12*os, 4*os); rLegOver.setTranslateX(2*s); rLegOver.setTranslateY(12*s);
        
        character.getChildren().addAll(head, body, leftArm, rightArm, leftLeg, rightLeg, 
                                       headOver, bodyOver, lArmOver, rArmOver, lLegOver, rLegOver);
        
        // [NUEVO] Animación de Caminata (Walking Animation)
        Rotate rLeftArm = new Rotate(0, Rotate.X_AXIS); rLeftArm.setPivotY(-6*s);
        Rotate rRightArm = new Rotate(0, Rotate.X_AXIS); rRightArm.setPivotY(-6*s);
        Rotate rLeftLeg = new Rotate(0, Rotate.X_AXIS); rLeftLeg.setPivotY(-6*s);
        Rotate rRightLeg = new Rotate(0, Rotate.X_AXIS); rRightLeg.setPivotY(-6*s);

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
        updateModelTexture(character, null);

        // [NUEVO] Cargar Skin Real desde Mojang usando UUID
        String uuid = getCurrentUuid();
        if (uuid != null) {
            new Thread(() -> {
                String skinUrl = getSkinUrlFromMojang(uuid);
                if (skinUrl != null) {
                    Image realSkin = new Image(skinUrl);
                    Platform.runLater(() -> updateModelTexture(character, realSkin));
                }
            }).start();
        }
        
        // Rotación animada
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        Rotate rotateX = new Rotate(10, Rotate.X_AXIS);
        character.getTransforms().addAll(rotateY, rotateX);
        character.setTranslateY(20); // Centrar verticalmente
        
        // [MEJORA] Control con Mouse para rotar
        DoubleProperty angleX = new SimpleDoubleProperty(0);
        DoubleProperty angleY = new SimpleDoubleProperty(0);
        DoubleProperty anchorX = new SimpleDoubleProperty(0);
        DoubleProperty anchorY = new SimpleDoubleProperty(0);
        DoubleProperty anchorAngleX = new SimpleDoubleProperty(0);
        DoubleProperty anchorAngleY = new SimpleDoubleProperty(0);

        skinPreview.setOnMousePressed(event -> {
            anchorX.set(event.getSceneX());
            anchorY.set(event.getSceneY());
            anchorAngleX.set(rotateY.getAngle());
            anchorAngleY.set(rotateX.getAngle());
        });

        skinPreview.setOnMouseDragged(event -> {
            rotateY.setAngle(anchorAngleX.get() - (anchorX.get() - event.getSceneX()));
            rotateX.setAngle(anchorAngleY.get() + (anchorY.get() - event.getSceneY()));
        });

        skinPreview.getChildren().add(character);

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        Button btnUpload = new Button("Subir Skin");
        btnUpload.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        
        Button btnSave = new Button("Guardar en Lista");
        btnSave.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        
        controls.getChildren().addAll(btnUpload, btnSave);

        ListView<String> savedSkins = new ListView<>();
        // Cargar skins guardadas desde JSON
        savedSkins.getItems().addAll(loadSavedSkins());
        savedSkins.setMaxHeight(150);
        
        // [MEJORA] Estilo moderno para la lista (Adiós Windows XP)
        savedSkins.setStyle("-fx-background-color: transparent; -fx-control-inner-background: rgba(0,0,0,0.3); -fx-background-radius: 10;");
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
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label lblType = new Label("Skin");
                    lblType.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px; -fx-border-color: #aaa; -fx-border-radius: 3; -fx-padding: 2;");
                    cell.getChildren().addAll(lbl, lblType);
                    setGraphic(cell);
                    setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5; -fx-padding: 5; -fx-border-width: 0 0 1 0; -fx-border-color: #333;");
                }
            }
        });

        // Funcionalidad de los botones
        btnUpload.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Skin (PNG)");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes PNG", "*.png"));
            File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
            if (file != null) {
                // Cargar imagen síncronamente (false) para poder leer sus píxeles inmediatamente
                Image skinImg = new Image(file.toURI().toString(), false);
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

        layout.getChildren().addAll(title, skinPreview, controls, new Label("Mis Skins Guardadas:"), savedSkins);
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
        PhongMaterial mHead = new PhongMaterial();
        PhongMaterial mBody = new PhongMaterial();
        PhongMaterial mArm = new PhongMaterial();
        PhongMaterial mLeg = new PhongMaterial();
        PhongMaterial mArmL = new PhongMaterial();
        PhongMaterial mLegL = new PhongMaterial();
        
        // Materiales para Overlays
        PhongMaterial oHead = new PhongMaterial(); oHead.setDiffuseColor(Color.TRANSPARENT);
        PhongMaterial oBody = new PhongMaterial(); oBody.setDiffuseColor(Color.TRANSPARENT);
        PhongMaterial oArm = new PhongMaterial(); oArm.setDiffuseColor(Color.TRANSPARENT);
        PhongMaterial oLeg = new PhongMaterial(); oLeg.setDiffuseColor(Color.TRANSPARENT);
        PhongMaterial oArmL = new PhongMaterial(); oArmL.setDiffuseColor(Color.TRANSPARENT);
        PhongMaterial oLegL = new PhongMaterial(); oLegL.setDiffuseColor(Color.TRANSPARENT);

        // Verificar si la imagen es válida y tiene dimensiones de skin (min 64px ancho)
        if (skin != null && !skin.isError() && skin.getWidth() >= 64) {
            boolean is64x64 = skin.getHeight() == 64;

            // --- CAPA BASE ---
            mHead.setDiffuseMap(cropImage(skin, 8, 8, 8, 8));       // Cara
            mBody.setDiffuseMap(cropImage(skin, 20, 20, 8, 12));    // Pecho
            mArm.setDiffuseMap(cropImage(skin, 44, 20, 4, 12));     // Brazo Der
            mLeg.setDiffuseMap(cropImage(skin, 4, 20, 4, 12));      // Pierna Der
            
            // Brazo/Pierna Izq (Si es 64x64 usa sus propias coords, si no, espejo del derecho)
            mArmL.setDiffuseMap(is64x64 ? cropImage(skin, 36, 52, 4, 12) : cropImage(skin, 44, 20, 4, 12));
            mLegL.setDiffuseMap(is64x64 ? cropImage(skin, 20, 52, 4, 12) : cropImage(skin, 4, 20, 4, 12));

            // --- CAPA OVERLAY (Ropa/Accesorios) ---
            oHead.setDiffuseMap(cropImage(skin, 40, 8, 8, 8));      // Sombrero
            
            if (is64x64) {
                oBody.setDiffuseMap(cropImage(skin, 20, 36, 8, 12)); // Chaqueta
                oArm.setDiffuseMap(cropImage(skin, 44, 36, 4, 12));  // Manga Der
                oLeg.setDiffuseMap(cropImage(skin, 4, 36, 4, 12));   // Pantalón Der
                oArmL.setDiffuseMap(cropImage(skin, 52, 52, 4, 12)); // Manga Izq
                oLegL.setDiffuseMap(cropImage(skin, 4, 52, 4, 12));  // Pantalón Izq
            }
        } else {
            // Color de respaldo si no hay skin o es inválida
            Color base = Color.web("#0078d7");
            mHead.setDiffuseColor(base); mBody.setDiffuseColor(base);
            mArm.setDiffuseColor(base); mLeg.setDiffuseColor(base);
            mArmL.setDiffuseColor(base); mLegL.setDiffuseColor(base);
        }

        // Aplicar materiales a las partes del cuerpo
        ((Box)character.getChildren().get(0)).setMaterial(mHead); // Cabeza
        ((Box)character.getChildren().get(1)).setMaterial(mBody); // Cuerpo
        ((Box)character.getChildren().get(2)).setMaterial(mArmL); // Brazo Izq
        ((Box)character.getChildren().get(3)).setMaterial(mArm);  // Brazo Der
        ((Box)character.getChildren().get(4)).setMaterial(mLegL); // Pierna Izq
        ((Box)character.getChildren().get(5)).setMaterial(mLeg);  // Pierna Der
        
        // Aplicar materiales a los overlays
        ((Box)character.getChildren().get(6)).setMaterial(oHead);
        ((Box)character.getChildren().get(7)).setMaterial(oBody);
        ((Box)character.getChildren().get(8)).setMaterial(oArmL);
        ((Box)character.getChildren().get(9)).setMaterial(oArm);
        ((Box)character.getChildren().get(10)).setMaterial(oLegL);
        ((Box)character.getChildren().get(11)).setMaterial(oLeg);
    }

    private WritableImage cropImage(Image src, int x, int y, int w, int h) {
        PixelReader reader = src.getPixelReader();
        if (reader != null) {
            return new WritableImage(reader, x, y, w, h);
        }
        return null;
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

    private void saveSession(String username, String token, String uuid) {
        try (FileWriter writer = new FileWriter(SESSION_FILE)) {
            JsonObject session = new JsonObject();
            session.addProperty("username", username);
            session.addProperty("token", token);
            session.addProperty("uuid", uuid);
            session.addProperty("type", "microsoft");
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
                    saveSession(username, mcAccessToken, uuid);
                    showDashboard();
                    MainView.showNotification("Éxito", "Bienvenido " + username, "success");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error Login", e.getMessage(), "error"));
            }
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