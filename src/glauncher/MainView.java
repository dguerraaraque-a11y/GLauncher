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
        HBox navBar = new HBox(15);
        navBar.setAlignment(Pos.CENTER); // [FIX] Centrar botones verticalmente en la textura
        
        // Estilo "Pachonchito" (Redondeado y con cuerpo)
        navBar.setMaxWidth(Region.USE_PREF_SIZE);
        navBar.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-background-radius: 40; -fx-padding: 15 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 40; -fx-border-width: 1;");

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
        StackPane.setMargin(navBar, new Insets(0, 0, 30, 0)); // Margen inferior de 30px

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
