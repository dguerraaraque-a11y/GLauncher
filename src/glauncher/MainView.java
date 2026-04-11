package glauncher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import java.io.File;
import java.io.FileReader;
import glauncher.ui.views.*;
import glauncher.utils.DiscordIntegration;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

public class MainView {

    private static MainView instance;
    private StackPane root;
    private BorderPane contentPane;
    private StackPane backgroundContainer;
    private Region overlay;
    private MediaPlayer videoPlayer;
    private String currentCustomCss = null;
    private static VBox notificationContainer; // Contenedor estático para acceso global
    
    // Botones de navegación (Promovidos a campos para traducción)
    private Button btnInicio, btnNews, btnShop, btnVersions, btnCuenta, btnChat, btnMusic, btnDownloads, btnSettings, btnServers;

    public MainView() {
        instance = this;
        root = new StackPane();
        // [FIX] Asegurar que la ventana tenga un fondo base transparente para soportar bordes redondeados
        root.setStyle("-fx-background-color: transparent;");

        // [NUEVO] Inyectar CSS global moderno para reemplazar estilo por defecto de JavaFX
        // Esto rediseña TODOS los componentes para que no parezcan de Java estándar
        root.getStylesheets().add("data:text/css," + 
            ".root { -fx-base: #1e1e1e; -fx-accent: #0078d7; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-font-family: 'Segoe UI', sans-serif; }" +
            ".button { -fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #3d3d3d; -fx-border-radius: 8; -fx-cursor: hand; -fx-padding: 8 15; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 3, 0, 0, 1); transition: all 0.2s; }" +
            ".button:hover { -fx-background-color: #3a3a3a; -fx-border-color: #555; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2); -fx-translate-y: -1; }" +
            ".button:pressed { -fx-background-color: #222; -fx-translate-y: 1; }" +
            ".text-field, .text-area { -fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #333; -fx-border-radius: 8; -fx-prompt-text-fill: #666; -fx-padding: 10; }" +
            ".text-field:focused, .text-area:focused { -fx-border-color: #0078d7; -fx-background-color: #202020; -fx-effect: dropshadow(three-pass-box, rgba(0,120,215,0.3), 5, 0, 0, 0); }" +
            ".scroll-bar { -fx-background-color: transparent; -fx-pref-width: 10; }" +
            ".scroll-bar .track { -fx-background-color: transparent; }" +
            ".scroll-bar .thumb { -fx-background-color: #444; -fx-background-radius: 10; -fx-background-insets: 2; }" +
            ".scroll-bar .thumb:hover { -fx-background-color: #666; }" +
            ".scroll-pane { -fx-background-color: transparent; -fx-background-insets: 0; }" +
            ".scroll-pane .viewport { -fx-background-color: transparent; }" +
            ".list-view { -fx-background-color: transparent; -fx-background-insets: 0; }" +
            ".list-cell { -fx-text-fill: white; -fx-background-color: transparent; -fx-padding: 8 12; }" +
            ".list-cell:filled:selected { -fx-background-color: rgba(0,120,215,0.2); -fx-background-radius: 5; -fx-border-color: rgba(0,120,215,0.5); -fx-border-radius: 5; }" +
            ".list-cell:filled:hover { -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5; }" +
            ".combo-box { -fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #3d3d3d; -fx-border-radius: 8; }" +
            ".combo-box .cell { -fx-text-fill: white; }" +
            ".combo-box-popup .list-view { -fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5); }" +
            ".combo-box-popup .list-cell { -fx-text-fill: white; }" +
            ".combo-box-popup .list-cell:hover { -fx-background-color: #333; }" +
            ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: transparent; }" +
            ".tab { -fx-background-color: #2d2d2d; -fx-text-fill: #aaa; -fx-background-radius: 8 8 0 0; -fx-padding: 8 15; }" +
            ".tab:selected { -fx-background-color: #0078d7; -fx-text-fill: white; }" +
            ".tab-label { -fx-text-fill: inherit; -fx-font-weight: bold; }" +
            ".check-box { -fx-text-fill: white; -fx-cursor: hand; }" +
            ".check-box .box { -fx-background-color: #1a1a1a; -fx-border-color: #444; -fx-border-radius: 4; }" +
            ".check-box:selected .mark { -fx-background-color: white; -fx-shape: 'M 0 0 L 2 2 L 6 -4'; }" +
            ".check-box:selected .box { -fx-background-color: #0078d7; -fx-border-color: #0078d7; }" +
            ".slider .track { -fx-background-color: #333; -fx-background-radius: 5; }" +
            ".slider .thumb { -fx-background-color: #0078d7; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 3, 0, 0, 1); }" +
            ".progress-bar .track { -fx-background-color: #1a1a1a; -fx-background-radius: 10; }" +
            ".progress-bar .bar { -fx-background-color: linear-gradient(to right, #0078d7, #00b4db); -fx-background-radius: 10; }" +
            ".separator .line { -fx-border-color: #333; -fx-border-width: 1 0 0 0; }" +
            ".tooltip { -fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-border-color: #444; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 2); }" +
            ".dialog-pane { -fx-background-color: #1e1e1e; }" +
            ".dialog-pane .header-panel { -fx-background-color: #252525; }" +
            ".dialog-pane .label { -fx-text-fill: white; }"
        );

        contentPane = new BorderPane();

        // Contenedor de Fondo (Imagen o Video)
        backgroundContainer = new StackPane();
        backgroundContainer.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15;");

        // Overlay degradado para mejorar la estética y legibilidad
        overlay = new Region();
        overlay.setMouseTransparent(true);

        // Vistas
        InicioView inicio = new InicioView();
        MiCuentaView miCuenta = new MiCuentaView();
        GChatView gchat = new GChatView();
        MusicView music = new MusicView();
        DownloadsView downloads = new DownloadsView();
        NewsView news = new NewsView();
        ShopView shop = new ShopView();
        SettingsView settings = new SettingsView();
        VersionesView versiones = new VersionesView();
        ServersView servers = new ServersView();

        contentPane.setCenter(inicio.getView());

        // Barra inferior flotante
        HBox navBar = new HBox(5); // [FIX] Reducir espaciado para evitar que ocupe todo el ancho
        navBar.setAlignment(Pos.CENTER); // [FIX] Centrar botones verticalmente en la textura
        
        // Estilo "Pachonchito" (Redondeado y con cuerpo)
        navBar.setMaxWidth(Region.USE_PREF_SIZE);
        navBar.setMaxHeight(Region.USE_PREF_SIZE); // [FIX] Evitar expansión vertical
        navBar.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-background-radius: 30; -fx-padding: 12 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 30; -fx-border-width: 1;");

        btnInicio = new Button("Inicio");
        btnNews = new Button("Noticias");
        btnShop = new Button("Tienda");
        btnVersions = new Button("Versiones");
        btnCuenta = new Button("Mi Cuenta");
        btnChat = new Button("GChat");
        btnServers = new Button("Servidores");
        btnMusic = new Button("GMusic");
        btnDownloads = new Button("Mods");
        btnSettings = new Button("Ajustes");

        // [NUEVO] Asignar iconos a los botones de navegación
        btnInicio.setGraphic(new FontIcon(FontAwesome.HOME));
        btnNews.setGraphic(new FontIcon(FontAwesome.NEWSPAPER_O));
        btnShop.setGraphic(new FontIcon(FontAwesome.SHOPPING_BAG));
        btnVersions.setGraphic(new FontIcon(FontAwesome.CUBES));
        btnCuenta.setGraphic(new FontIcon(FontAwesome.USER));
        btnChat.setGraphic(new FontIcon(FontAwesome.COMMENT));
        btnServers.setGraphic(new FontIcon(FontAwesome.SERVER));
        btnMusic.setGraphic(new FontIcon(FontAwesome.MUSIC));
        btnDownloads.setGraphic(new FontIcon(FontAwesome.DOWNLOAD));
        btnSettings.setGraphic(new FontIcon(FontAwesome.COGS));
        btnInicio.setOnAction(e -> { 
            switchView(inicio.getView()); 
            DiscordIntegration.update("En el Inicio", "Menú Principal");
        });
        btnNews.setOnAction(e -> { 
            switchView(news.getView()); 
            DiscordIntegration.update("Leyendo Noticias", "Manteniéndose al día");
        });
        btnShop.setOnAction(e -> { 
            switchView(shop.getView()); 
            DiscordIntegration.update("En la Tienda", "Buscando cosméticos");
        });
        btnVersions.setOnAction(e -> { 
            switchView(versiones.getView()); 
            DiscordIntegration.update("Gestionando Versiones", "Configurando Minecraft");
        });
        btnCuenta.setOnAction(e -> { 
            switchView(miCuenta.getView()); 
            DiscordIntegration.update("Mi Cuenta", "Gestionando perfil");
        });
        btnChat.setOnAction(e -> { 
            switchView(gchat.getView()); 
            DiscordIntegration.update("En GChat", "Chateando con la comunidad");
        });
        btnServers.setOnAction(e -> { 
            switchView(servers.getView()); 
            DiscordIntegration.update("En Servidores", "Gestionando Servidores");
        });
        btnMusic.setOnAction(e -> { 
            switchView(music.getView()); 
            DiscordIntegration.update("Escuchando Música", "GMusic Player");
        });
        btnDownloads.setOnAction(e -> { 
            switchView(downloads.getView()); 
            DiscordIntegration.update("Descargando Mods", "Explorando Modrinth");
        });
        btnSettings.setOnAction(e -> { 
            switchView(settings.getView()); 
            DiscordIntegration.update("Configurando", "Ajustes del Launcher");
        });

        navBar.getChildren().addAll(btnInicio, btnNews, btnShop, btnVersions, btnServers, btnCuenta, btnChat, btnMusic, btnDownloads, btnSettings);
        
        // [FIX] Estilizar botones para que sean más compactos (Estilo transparente)
        navBar.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button btn = (Button) node;
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 12;");
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-background-radius: 20; -fx-padding: 6 12;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 12;"));
            }
        });

        // Animación de elevación al pasar el mouse (Hover)
        TranslateTransition hoverAnim = new TranslateTransition(Duration.millis(200), navBar);
        navBar.setOnMouseEntered(e -> {
            hoverAnim.stop();
            hoverAnim.setToY(-10); // Subir 10px suavemente
            hoverAnim.play();
        });
        navBar.setOnMouseExited(e -> {
            hoverAnim.stop();
            hoverAnim.setToY(0); // Bajar a posición original
            hoverAnim.play();
        });
        
        // --- Contenedor de Notificaciones ---
        notificationContainer = new VBox(10);
        notificationContainer.setAlignment(Pos.TOP_RIGHT);
        notificationContainer.setPickOnBounds(false); // Permitir clicks a través de áreas vacías
        notificationContainer.setMaxHeight(0); // No ocupar espacio en el layout
        StackPane.setAlignment(notificationContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(notificationContainer, new Insets(20, 20, 0, 0));

        // --- Pantalla de Bienvenida (Splash Screen) ---
        VBox splashScreen = new VBox(20);
        splashScreen.setAlignment(Pos.CENTER);
        splashScreen.setStyle("-fx-background-color: #111; -fx-background-radius: 15;");
        
        Label splashTitle = new Label("GLAUNCHER");
        splashTitle.setStyle("-fx-text-fill: white; -fx-font-size: 64px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, cyan, 20, 0.5, 0, 0);");
        Label splashSub = new Label("Cargando recursos...");
        splashSub.setStyle("-fx-text-fill: #888; -fx-font-size: 16px;");
        
        splashScreen.getChildren().addAll(splashTitle, splashSub);

        // Añadir capas: fondo, overlay, contenido, barra nav, notificaciones, splash
        root.getChildren().addAll(backgroundContainer, overlay, contentPane, navBar, notificationContainer, splashScreen);
        
        // [NUEVO] Verificar si es la primera vez (Asistente de Configuración)
        checkForFirstRun();

        // Animación de salida del Splash Screen
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            FadeTransition ft = new FadeTransition(Duration.seconds(1), splashScreen);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(ev -> root.getChildren().remove(splashScreen));
            ft.play();
        });
        delay.play();
        
        // Posicionar la barra flotante
        StackPane.setAlignment(navBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(navBar, new Insets(0, 0, 15, 0)); // Mover la barra hacia arriba para que "flote" más

        // Aplicar configuración inicial
        applyThemeSettings();
        
        // Iniciar Discord RPC
        Platform.runLater(() -> {
            try {
                File settingsFile = new File((System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher" + File.separator + "settings.json");
                boolean rpcEnabled = true;
                boolean showTime = true;
                if (settingsFile.exists()) {
                     JsonObject s = new Gson().fromJson(new FileReader(settingsFile), JsonObject.class);
                     if (s.has("discordRpc")) rpcEnabled = s.get("discordRpc").getAsBoolean();
                     if (s.has("discordShowTime")) showTime = s.get("discordShowTime").getAsBoolean();
                }
                if (rpcEnabled) {
                    DiscordIntegration.start();
                    DiscordIntegration.setShowTime(showTime);
                }
            } catch(Exception e) {}
        });
    }

    private void checkForFirstRun() {
        File settingsFile = new File((System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher" + File.separator + "settings.json");
        
        if (!settingsFile.exists()) {
            SetupAssistantView assistant = new SetupAssistantView(() -> {
                // Al finalizar el asistente
                root.getChildren().removeIf(node -> node.getId() != null && node.getId().equals("setupAssistant"));
                applyThemeSettings(); // Aplicar la configuración recién creada
                showNotification("¡Todo listo!", "GLauncher se ha configurado correctamente.", "success");
            });
            Parent view = assistant.getView();
            view.setId("setupAssistant");
            root.getChildren().add(view); // Añadir al final para que quede encima de todo
        }
    }

    private void switchView(Parent view) {
        view.setOpacity(0);
        view.setTranslateY(10);
        contentPane.setCenter(view);

        FadeTransition ft = new FadeTransition(Duration.millis(300), view);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), view);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    public static void showNotification(String title, String message, String type) {
        Platform.runLater(() -> {
            VBox card = new VBox(5);
            
            // Colores según el tipo
            String bgColor = "#333"; // Default
            if (type.equalsIgnoreCase("success") || type.contains("bien")) bgColor = "#28a745"; // Verde
            else if (type.equalsIgnoreCase("warning") || type.contains("falta")) bgColor = "#ffc107"; // Amarillo
            else if (type.equalsIgnoreCase("error") || type.contains("error")) bgColor = "#dc3545"; // Rojo

            card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");
            card.setMaxWidth(350);
            card.setAlignment(Pos.CENTER_LEFT);

            Label lblTitle = new Label(title);
            lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            Label lblMsg = new Label(message);
            lblMsg.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            lblMsg.setWrapText(true);

            card.getChildren().addAll(lblTitle, lblMsg);
            
            // Animación de entrada (Fade In)
            card.setTranslateY(-20); // Empezar un poco arriba
            card.setOpacity(0);
            notificationContainer.getChildren().add(card);
            
            TranslateTransition slideDown = new TranslateTransition(Duration.millis(300), card);
            slideDown.setToY(0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
            fadeIn.setToValue(1.0);
            
            ParallelTransition entryAnim = new ParallelTransition(slideDown, fadeIn);
            entryAnim.play();

            // Reproducir sonido
            try {
                File soundFile = new File("assets/sounds/notifications/sound-1.mp3");
                if (soundFile.exists()) {
                    Media sound = new Media(soundFile.toURI().toString());
                    MediaPlayer mediaPlayer = new MediaPlayer(sound);
                    mediaPlayer.play();
                }
            } catch (Exception e) {
                System.out.println("Error de sonido: " + e.getMessage());
            }

            // Auto-eliminar después de 4 segundos
            PauseTransition delay = new PauseTransition(Duration.seconds(4));
            delay.setOnFinished(e -> {
                TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), card);
                slideOut.setByX(50); // Deslizar a la derecha
                
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), card);
                fadeOut.setToValue(0.0);
                
                ParallelTransition exitAnim = new ParallelTransition(slideOut, fadeOut);
                exitAnim.setOnFinished(ev -> notificationContainer.getChildren().remove(card));
                exitAnim.play();
            });
            delay.play();
        });
    }

    public static MainView getInstance() {
        return instance;
    }

    public void applyThemeSettings() {
        Platform.runLater(() -> {
            try {
                File settingsFile = new File((System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher" + File.separator + "settings.json");
                JsonObject settings = new JsonObject();
                if (settingsFile.exists()) {
                    settings = new Gson().fromJson(new FileReader(settingsFile), JsonObject.class);
                }

                // 1. Fondo (Imagen o Video)
                String bgPath = settings.has("backgroundPath") ? settings.get("backgroundPath").getAsString() : "";
                backgroundContainer.getChildren().clear();
                if (videoPlayer != null) { videoPlayer.dispose(); videoPlayer = null; }

                if (!bgPath.isEmpty() && new File(bgPath).exists()) {
                    if (bgPath.endsWith(".mp4")) {
                        // Video
                        Media media = new Media(new File(bgPath).toURI().toString());
                        videoPlayer = new MediaPlayer(media);
                        videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        videoPlayer.setVolume(0);
                        videoPlayer.play();
                        MediaView mediaView = new MediaView(videoPlayer);
                        mediaView.setPreserveRatio(false);
                        mediaView.fitWidthProperty().bind(root.widthProperty());
                        mediaView.fitHeightProperty().bind(root.heightProperty());
                        backgroundContainer.getChildren().add(mediaView);
                    } else {
                        // Imagen Personalizada
                        Region bg = new Region();
                        bg.setStyle("-fx-background-image: url('" + new File(bgPath).toURI().toString().replace("\\", "/") + "'); -fx-background-size: cover; -fx-background-position: center;");
                        backgroundContainer.getChildren().add(bg);
                    }
                } else {
                    // Fondo por defecto
                    int randomBg = (int) (Math.random() * 4) + 1;
                    Region bg = new Region();
                    bg.setStyle("-fx-background-image: url('file:assets/images/fondo-" + randomBg + ".jfif'); -fx-background-size: cover; -fx-background-position: center;");
                    backgroundContainer.getChildren().add(bg);
                }

                // 2. Efectos y Estilos
                double blur = settings.has("blurRadius") ? settings.get("blurRadius").getAsDouble() : 20;
                backgroundContainer.setEffect(new GaussianBlur(blur));

                double opacity = settings.has("overlayOpacity") ? settings.get("overlayOpacity").getAsDouble() : 0.8;
                String overlayColorHex = settings.has("overlayColor") ? settings.get("overlayColor").getAsString() : "#000000";
                Color c = Color.web(overlayColorHex);
                String rgba = String.format("rgba(%d,%d,%d,%.2f)", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), opacity);
                
                double radius = settings.has("cornerRadius") ? settings.get("cornerRadius").getAsDouble() : 15;
                
                overlay.setStyle("-fx-background-color: " + rgba + "; -fx-background-radius: " + radius + ";");
                backgroundContainer.setStyle("-fx-background-radius: " + radius + "; -fx-background-color: #1a1a1a;");

                // 3. Acento Global
                if (settings.has("accentColor")) {
                    String accent = settings.get("accentColor").getAsString();
                    root.setStyle(root.getStyle() + "-fx-accent: " + accent + "; -fx-focus-color: " + accent + "; -fx-faint-focus-color: transparent;");
                }

                // 4. Custom CSS
                String cssPath = settings.has("customCssPath") ? settings.get("customCssPath").getAsString() : "";
                // Remove old custom CSS if it exists and is different
                if (currentCustomCss != null && !currentCustomCss.equals(cssPath)) {
                    root.getStylesheets().remove(currentCustomCss);
                    currentCustomCss = null;
                }
                // Add new custom CSS if path is valid and not already added
                if (!cssPath.isEmpty() && new File(cssPath).exists() && !cssPath.equals(currentCustomCss)) {
                    try {
                        String cssUri = new File(cssPath).toURI().toString();
                        root.getStylesheets().add(cssUri);
                        currentCustomCss = cssUri;
                    } catch (Exception e) { System.err.println("Error al cargar CSS personalizado: " + e.getMessage()); }
                }

                // 5. Idioma y Usuario
                if (settings.has("language")) updateLanguage(settings.get("language").getAsString());
                if (settings.has("username")) {
                    // Opcional: Podríamos actualizar el texto de la cuenta aquí si fuera necesario
                    // String user = settings.get("username").getAsString();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public Parent getRoot() {
        return root;
    }

    public void updateLanguage(String lang) {
        if ("English".equals(lang)) {
            btnInicio.setText("Home");
            btnNews.setText("News");
            btnShop.setText("Shop");
            btnVersions.setText("Versions");
            btnServers.setText("Servers");
            btnCuenta.setText("My Account");
            btnChat.setText("GChat");
            btnMusic.setText("GMusic");
            btnDownloads.setText("Mods");
            btnSettings.setText("Settings");
        } else if ("Português".equals(lang)) {
            btnInicio.setText("Início");
            btnNews.setText("Notícias");
            btnShop.setText("Loja");
            btnVersions.setText("Versões");
            btnServers.setText("Servidores");
            btnCuenta.setText("Minha Conta");
            btnChat.setText("GChat");
            btnMusic.setText("GMusic");
            btnDownloads.setText("Mods");
            btnSettings.setText("Configurações");
        } else {
            btnInicio.setText("Inicio");
            btnNews.setText("Noticias");
            btnShop.setText("Tienda");
            btnVersions.setText("Versiones");
            btnServers.setText("Servidores");
            btnCuenta.setText("Mi Cuenta");
            btnChat.setText("GChat");
            btnMusic.setText("GMusic");
            btnDownloads.setText("Mods");
            btnSettings.setText("Ajustes");
        }
    }
}
