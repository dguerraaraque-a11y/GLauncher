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
import java.net.URL;
import java.io.File;
import java.io.FileReader;
import glauncher.ui.views.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import javafx.scene.transform.Scale;

import javafx.animation.ScaleTransition;

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
    private boolean isLowRes; // Campo para detectar baja resolución
    
    // Botones de navegación (Promovidos a campos para traducción)
    private Button btnInicio, btnNews, btnShop, btnVersions, btnCuenta, btnChat, btnMusic, btnDownloads, btnSettings, btnServers;
    private Button selectedButton; // Para rastrear el botón actualmente seleccionado

    // Instancias de las vistas para navegación global
    private InicioView inicio;
    private MiCuentaView miCuenta;
    private GChatView gchat;
    private MusicView music;
    private DownloadsView downloads;
    private NewsView news;
    private ShopView shop;
    private SettingsView settings;
    private VersionesView versiones;
    private ServersView servers;

    public MainView() {
        instance = this;
        root = new StackPane();

        // [NUEVO] Detectar resolución para ajustes de interfaz (Canaima/Laptop)
        Rectangle2D screen = Screen.getPrimary().getBounds(); // Inicializar el campo
        isLowRes = screen.getWidth() <= 1024 || screen.getHeight() <= 600;

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
        inicio = new InicioView();
        miCuenta = new MiCuentaView();
        gchat = new GChatView();
        music = new MusicView();
        downloads = new DownloadsView();
        news = new NewsView();
        shop = new ShopView();
        settings = new SettingsView();
        versiones = new VersionesView();
        servers = new ServersView();

        contentPane.setCenter(inicio.getView());

        // Barra inferior flotante
        HBox navBar = new HBox(5); // [FIX] Reducir espaciado para evitar que ocupe todo el ancho
        navBar.setAlignment(Pos.CENTER); // [FIX] Centrar botones verticalmente en la textura
        
        // Estilo "Pachonchito" (Redondeado y con cuerpo)
        navBar.setMaxWidth(Region.USE_PREF_SIZE);
        navBar.setMaxHeight(Region.USE_PREF_SIZE); // [FIX] Evitar expansión vertical
        navBar.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(25, 25, 25, 0.95), rgba(15, 15, 15, 0.95)); -fx-background-radius: 30; -fx-padding: " + (isLowRes ? "8 15" : "12 25") + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5); -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 30; -fx-border-width: 1;");

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
        btnInicio.setGraphic(new FontIcon(FontAwesomeSolid.HOME));
        btnNews.setGraphic(new FontIcon(FontAwesomeSolid.NEWSPAPER));
        btnShop.setGraphic(new FontIcon(FontAwesomeSolid.SHOPPING_BAG));
        btnVersions.setGraphic(new FontIcon(FontAwesomeSolid.CUBES));
        btnCuenta.setGraphic(new FontIcon(FontAwesomeSolid.USER));
        btnChat.setGraphic(new FontIcon(FontAwesomeSolid.COMMENT));
        btnServers.setGraphic(new FontIcon(FontAwesomeSolid.SERVER));
        btnMusic.setGraphic(new FontIcon(FontAwesomeSolid.MUSIC));
        btnDownloads.setGraphic(new FontIcon(FontAwesomeSolid.DOWNLOAD));
        btnSettings.setGraphic(new FontIcon(FontAwesomeSolid.COGS));
        btnInicio.setOnAction(e -> switchView(inicio.getView(), btnInicio));
        btnNews.setOnAction(e -> switchView(news.getView(), btnNews));
        btnShop.setOnAction(e -> switchView(shop.getView(), btnShop));
        btnVersions.setOnAction(e -> switchView(versiones.getView(), btnVersions));
        btnCuenta.setOnAction(e -> switchView(miCuenta.getView(), btnCuenta));
        btnChat.setOnAction(e -> switchView(gchat.getView(), btnChat));
        btnServers.setOnAction(e -> switchView(servers.getView(), btnServers));
        btnMusic.setOnAction(e -> switchView(music.getView(), btnMusic));
        btnDownloads.setOnAction(e -> switchView(downloads.getView(), btnDownloads));
        btnSettings.setOnAction(e -> switchView(settings.getView(), btnSettings));

        navBar.getChildren().addAll(btnInicio, btnNews, btnShop, btnVersions, btnServers, btnCuenta, btnChat, btnMusic, btnDownloads, btnSettings);
        
        // Aplicar estilos iniciales a todos los botones y seleccionar el primero
        navBar.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button btn = (Button) node;
                applyNavButtonStyle(btn, btn == btnInicio); // btnInicio es el seleccionado por defecto
            }
        });
        selectedButton = btnInicio; // Establecer el botón inicial como seleccionado

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
        
        // Añadir capas: fondo, overlay, contenido, barra nav, notificaciones
        root.getChildren().addAll(backgroundContainer, overlay, contentPane, navBar, notificationContainer);
        
        // [NUEVO] Verificar si es la primera vez (Asistente de Configuración)
        checkForFirstRun();
        
        // Posicionar la barra flotante
        StackPane.setAlignment(navBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(navBar, new Insets(0, 0, 15, 0)); // Mover la barra hacia arriba para que "flote" más

        // Aplicar configuración inicial
        applyThemeSettings();

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

    private void switchView(Parent view, Button clickedButton) {
        view.setOpacity(0);
        view.setTranslateY(10);
        contentPane.setCenter(view);

        FadeTransition ft = new FadeTransition(Duration.millis(300), view);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), view);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();

        // [NUEVO] Actualizar estilo del botón seleccionado
        if (selectedButton != null) {
            applyNavButtonStyle(selectedButton, false);
        }
        selectedButton = clickedButton;
        applyNavButtonStyle(selectedButton, true);
    }

    // [NUEVO] Método público para cambiar de vista desde cualquier parte del código
    public void showView(String viewName) {
        Platform.runLater(() -> {
            switch (viewName) {
                case "Inicio": switchView(inicio.getView(), btnInicio); break;
                case "Noticias": switchView(news.getView(), btnNews); break;
                case "Tienda": switchView(shop.getView(), btnShop); break;
                case "Versiones": switchView(versiones.getView(), btnVersions); break;
                case "Mi Cuenta": switchView(miCuenta.getView(), btnCuenta); break;
                case "GChat": switchView(gchat.getView(), btnChat); break;
                case "Servidores": switchView(servers.getView(), btnServers); break;
                case "GMusic": switchView(music.getView(), btnMusic); break;
                case "Mods": switchView(downloads.getView(), btnDownloads); break;
                case "Ajustes": switchView(settings.getView(), btnSettings); break;
            }
        });
    }

    // [NUEVO] Método auxiliar para aplicar estilos a los botones de navegación
    private void applyNavButtonStyle(Button btn, boolean isSelected) {
        String fontSize = isLowRes ? "11px" : "12px";
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: " + fontSize + "; -fx-padding: 6 12; -fx-background-radius: 20;";
        String hoverStyle = "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: " + fontSize + "; -fx-background-radius: 20;";
        String selectedStyle = "-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: " + fontSize + "; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,120,215,0.4), 8, 0, 0, 0);";

        if (isSelected) {
            btn.setStyle(selectedStyle);
            btn.setOnMouseEntered(null); // Desactivar hover para el seleccionado
            btn.setOnMouseExited(null);
        } else {
            btn.setStyle(baseStyle);
            btn.setOnMouseEntered(e -> {
                btn.setStyle(hoverStyle);
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn); st.setToX(1.05); st.setToY(1.05); st.play();
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle(baseStyle);
                ScaleTransition st = new ScaleTransition(Duration.millis(100), btn); st.setToX(1.0); st.setToY(1.0); st.play();
            });
        }
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
            
            // Animación de entrada (Slide from Right)
            card.setTranslateX(400); // Empezar fuera de la pantalla a la derecha (ajustado para seguridad)
            card.setOpacity(0);
            notificationContainer.getChildren().add(card);
            
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), card);
            slideIn.setToX(0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
            fadeIn.setToValue(1.0);
            
            ParallelTransition entryAnim = new ParallelTransition(slideIn, fadeIn);
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
                slideOut.setToX(400); // Deslizar hacia afuera (derecha)
                
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
        // ... (Tu lógica existente para actualizar el texto de los botones) ...
        // Después de actualizar el texto, reaplicar los estilos para asegurar el estado correcto
        applyNavButtonStyle(btnInicio, btnInicio == selectedButton);
        applyNavButtonStyle(btnNews, btnNews == selectedButton);
        applyNavButtonStyle(btnShop, btnShop == selectedButton);
        applyNavButtonStyle(btnVersions, btnVersions == selectedButton);
        applyNavButtonStyle(btnServers, btnServers == selectedButton);
        applyNavButtonStyle(btnCuenta, btnCuenta == selectedButton);
        applyNavButtonStyle(btnChat, btnChat == selectedButton);
        applyNavButtonStyle(btnMusic, btnMusic == selectedButton);
        applyNavButtonStyle(btnDownloads, btnDownloads == selectedButton);
        applyNavButtonStyle(btnSettings, btnSettings == selectedButton);
    }

    // [NUEVO] Método para resolver rutas de assets (útil para otras vistas)
    public String resolveAssetPath(String path) {
        // 1. Prioridad: Ruta de instalacion EXE (app\assets)
        File exePath = new File("app" + File.separator + path);
        if (exePath.exists()) return exePath.toURI().toString();

        // 2. Secundaria: Ruta de desarrollo o portable (assets\)
        File devPath = new File(path);
        if (devPath.exists()) return devPath.toURI().toString();
        
        // 3. Fallback: Dentro del JAR
        try {
            URL resource = getClass().getResource("/" + path);
            if (resource != null) return resource.toExternalForm();
        } catch (Exception ignored) {}
        
        return new File(path).toURI().toString(); // Fallback
    }
}
