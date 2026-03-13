package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.ScaleTransition;

public class SetupAssistantView {

    private StackPane root;
    private VBox contentBox;
    private Runnable onFinish;
    private int currentStep = 0;
    private final int TOTAL_STEPS = 7; // Aumentado a 7 pasos
    
    // Datos temporales para guardar
    private String language = "Español";
    private double ram = 2048;
    private String theme = "dark";
    private String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    private String accountType = "Offline";
    private String username = "Player";
    private String selectedVersion = "1.20.4";

    // UI Components (para mantener estado)
    private TextField txtJavaPath;
    private Slider slRam;
    private Label lblRamValue;

    public SetupAssistantView(Runnable onFinish) {
        this.onFinish = onFinish;
        root = new StackPane();
        root.setStyle("-fx-background-color: rgba(10, 10, 10, 0.9);"); // Fondo oscuro moderno

        contentBox = new VBox(20);
        contentBox.setMaxWidth(600);
        contentBox.setMaxHeight(450);
        contentBox.setPadding(new Insets(40));
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 30, 0, 0, 10); -fx-border-color: #333; -fx-border-radius: 15; -fx-border-width: 1;");

        showStep(0);
        root.getChildren().add(contentBox);
    }

    public Parent getView() {
        return root;
    }

    private void showStep(int step) {
        currentStep = step;
        contentBox.getChildren().clear();

        // Textos localizados
        String txtNext = "Siguiente ➜";
        String txtBack = "Atrás";
        String txtFinish = "Finalizar y Jugar";
        
        if ("English".equals(language)) {
            txtNext = "Next ➜"; txtBack = "Back"; txtFinish = "Finish & Play";
        } else if ("Português".equals(language)) {
            txtNext = "Próximo ➜"; txtBack = "Voltar"; txtFinish = "Concluir e Jogar";
        }

        Label title = new Label();
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: white;");

        Label desc = new Label();
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #bbb; -fx-font-size: 15px; -fx-text-alignment: center; -fx-line-spacing: 5;");
        desc.setMaxWidth(500);

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.CENTER);
        VBox.setVgrow(centerContent, Priority.ALWAYS);

        HBox navButtons = new HBox(15);
        navButtons.setAlignment(Pos.CENTER_RIGHT);
        navButtons.setPadding(new Insets(20, 0, 0, 0));
        
        Button btnNext = createStyledButton(step == TOTAL_STEPS - 1 ? txtFinish : txtNext, true);
        btnNext.setOnAction(e -> {
            if (step == TOTAL_STEPS - 1) finish();
            else showStep(step + 1);
        });

        if (step > 0) {
            Button btnBack = createStyledButton(txtBack, false);
            btnBack.setOnAction(e -> showStep(step - 1));
            navButtons.getChildren().add(0, btnBack);
        }
        navButtons.getChildren().add(btnNext);

        switch (step) {
            case 0: // Bienvenida
                if ("English".equals(language)) {
                    title.setText("Welcome to GLauncher");
                    desc.setText("Your new Minecraft experience starts here.\nLet's configure the essentials to optimize performance and customize your style.");
                } else if ("Português".equals(language)) {
                    title.setText("Bem-vindo ao GLauncher");
                    desc.setText("Sua nova experiência no Minecraft começa aqui.\nVamos configurar o essencial para otimizar o desempenho e personalizar seu estilo.");
                } else {
                    title.setText("Bienvenido a GLauncher");
                    desc.setText("Tu nueva experiencia en Minecraft comienza aquí.\nVamos a configurar lo esencial para optimizar el rendimiento y personalizar tu estilo.");
                }
                Label icon = new Label("🚀");
                icon.setStyle("-fx-font-size: 72px; -fx-text-fill: white;");
                centerContent.getChildren().add(icon);
                break;

            case 1: // Idioma
                title.setText("Idioma / Language");
                if ("English".equals(language)) desc.setText("Select the interface language.");
                else if ("Português".equals(language)) desc.setText("Selecione o idioma da interface.");
                else desc.setText("Selecciona el idioma de la interfaz.");
                
                ComboBox<String> cmbLang = new ComboBox<>();
                cmbLang.getItems().addAll("Español", "English", "Português");
                cmbLang.setValue(language);
                cmbLang.setStyle("-fx-font-size: 14px; -fx-pref-width: 250; -fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #555; -fx-border-radius: 5;");
                cmbLang.setOnAction(e -> {
                    language = cmbLang.getValue();
                    showStep(currentStep); // Recargar paso para aplicar idioma
                });
                
                centerContent.getChildren().add(cmbLang);
                break;

            case 2: // Cuenta (NUEVO)
                if ("English".equals(language)) {
                    title.setText("Account");
                    desc.setText("Log in to access online servers or play in offline mode.");
                } else if ("Português".equals(language)) {
                    title.setText("Conta");
                    desc.setText("Faça login para acessar servidores online ou jogue no modo offline.");
                } else {
                    title.setText("Cuenta");
                    desc.setText("Inicia sesión para acceder a servidores online o juega en modo offline.");
                }

                VBox accountBox = new VBox(15);
                accountBox.setAlignment(Pos.CENTER);

                ToggleGroup accGroup = new ToggleGroup();
                ToggleButton tbMs = createThemeButton("Microsoft", "#2d2d2d", Color.WHITE);
                ToggleButton tbOffline = createThemeButton("Offline", "#2d2d2d", Color.WHITE);
                tbMs.setToggleGroup(accGroup);
                tbOffline.setToggleGroup(accGroup);

                HBox accButtons = new HBox(20, tbMs, tbOffline);
                accButtons.setAlignment(Pos.CENTER);

                TextField txtUser = new TextField(username);
                txtUser.setPromptText("Username");
                txtUser.setMaxWidth(250);
                txtUser.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5;");
                txtUser.setVisible(false);
                txtUser.textProperty().addListener((obs, old, val) -> username = val);

                if ("Microsoft".equals(accountType)) {
                    tbMs.setSelected(true);
                } else {
                    tbOffline.setSelected(true);
                    txtUser.setVisible(true);
                }

                tbMs.setOnAction(e -> {
                    accountType = "Microsoft";
                    txtUser.setVisible(false);
                });
                tbOffline.setOnAction(e -> {
                    accountType = "Offline";
                    txtUser.setVisible(true);
                });

                centerContent.getChildren().addAll(accButtons, txtUser);
                break;

            case 3: // Java
                if ("English".equals(language)) {
                    title.setText("Java Configuration");
                    desc.setText("Minecraft needs Java to run. We detected this path, but you can change it if you prefer another version.");
                } else if ("Português".equals(language)) {
                    title.setText("Configuração Java");
                    desc.setText("Minecraft precisa de Java para rodar. Detectamos este caminho, mas você pode alterá-lo se preferir outra versão.");
                } else {
                    title.setText("Configuración de Java");
                    desc.setText("Minecraft necesita Java para funcionar. Hemos detectado esta ruta, pero puedes cambiarla si prefieres otra versión.");
                }

                txtJavaPath = new TextField(javaPath);
                txtJavaPath.setPromptText("Ruta de java.exe");
                txtJavaPath.setEditable(false);
                txtJavaPath.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #555; -fx-border-radius: 5;");
                txtJavaPath.setTooltip(new Tooltip("Ruta actual del ejecutable de Java"));
                
                Button btnBrowse = createStyledButton("📂", false);
                btnBrowse.setOnAction(e -> {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Seleccionar Java (java.exe / javaw.exe)");
                    File f = fc.showOpenDialog(root.getScene().getWindow());
                    if (f != null) {
                        javaPath = f.getAbsolutePath();
                        txtJavaPath.setText(javaPath);
                    }
                });
                btnBrowse.setTooltip(new Tooltip("Buscar manualmente el archivo java.exe en tu PC"));

                HBox javaBox = new HBox(10, txtJavaPath, btnBrowse);
                javaBox.setAlignment(Pos.CENTER);
                HBox.setHgrow(txtJavaPath, Priority.ALWAYS);

                Button btnAuto = createStyledButton("✨ Auto-Detect", false);
                btnAuto.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 15; -fx-background-radius: 5; -fx-font-weight: bold;");
                btnAuto.setTooltip(new Tooltip("Intentar encontrar Java automáticamente en las carpetas por defecto"));
                btnAuto.setOnAction(e -> detectJava());

                // Sección de Descarga de Java
                Label lblDownload = new Label("¿No tienes Java? Descarga el instalador:");
                lblDownload.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px; -fx-padding: 10 0 5 0;");

                HBox downloadButtons = new HBox(15);
                downloadButtons.setAlignment(Pos.CENTER);

                Button btnJava8 = createStyledButton("⬇ Java 8 (Legacy)", false);
                btnJava8.setTooltip(new Tooltip("Necesario para versiones antiguas (1.8 - 1.16.5) y Forge antiguo.\nHaz clic para descargar el instalador."));
                btnJava8.setOnAction(e -> openUrl("https://api.adoptium.net/v3/installer/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"));

                Button btnJava17 = createStyledButton("⬇ Java 17 (Modern)", false);
                btnJava17.setTooltip(new Tooltip("Necesario para versiones modernas (1.17+).\nHaz clic para descargar el instalador."));
                btnJava17.setOnAction(e -> openUrl("https://api.adoptium.net/v3/installer/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"));

                Button btnJava21 = createStyledButton("⬇ Java 21 (Latest)", false);
                btnJava21.setTooltip(new Tooltip("Necesario para versiones 1.20.5+.\nHaz clic para descargar el instalador."));
                btnJava21.setOnAction(e -> openUrl("https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"));

                downloadButtons.getChildren().addAll(btnJava8, btnJava17, btnJava21);

                centerContent.getChildren().addAll(javaBox, btnAuto, new Separator(), lblDownload, downloadButtons);
                break;

            case 4: // RAM
                if ("English".equals(language)) {
                    title.setText("RAM Memory");
                    desc.setText("Allocate memory for the game. At least 2GB (2048MB) is recommended. Do not allocate more than half of your total RAM.");
                } else if ("Português".equals(language)) {
                    title.setText("Memória RAM");
                    desc.setText("Aloque memória para o jogo. Recomenda-se pelo menos 2GB (2048MB). Não aloque mais da metade da sua RAM total.");
                } else {
                    title.setText("Memoria RAM");
                    desc.setText("Asigna memoria al juego. Se recomienda al menos 2GB (2048MB). No asignes más de la mitad de tu RAM total.");
                }
                
                lblRamValue = new Label((int)ram + " MB");
                lblRamValue.setStyle("-fx-text-fill: #00b4db; -fx-font-weight: bold; -fx-font-size: 24px;");
                
                long maxRam = 4096;
                try {
                    maxRam = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / (1024 * 1024);
                } catch (Exception | Error ignored) {}

                slRam = new Slider(1024, Math.min(maxRam, 16384), ram);
                slRam.setMajorTickUnit(1024);
                slRam.setShowTickMarks(true);
                slRam.setPrefWidth(400);
                slRam.setStyle("-fx-control-inner-background: #444; -fx-accent: #00b4db;");
                
                slRam.valueProperty().addListener((obs, old, val) -> {
                    ram = val.doubleValue();
                    lblRamValue.setText(String.format("%.0f MB", ram));
                });

                centerContent.getChildren().addAll(lblRamValue, slRam);
                break;

            case 5: // Descargar Versión (NUEVO)
                if ("English".equals(language)) {
                    title.setText("Initial Installation");
                    desc.setText("Select a version to download now and have everything ready to play.");
                } else if ("Português".equals(language)) {
                    title.setText("Instalação Inicial");
                    desc.setText("Selecione uma versão para baixar agora e ter tudo pronto para jogar.");
                } else {
                    title.setText("Instalación Inicial");
                    desc.setText("Selecciona una versión para descargarla ahora y tener todo listo para jugar.");
                }

                ComboBox<String> cmbVer = new ComboBox<>();
                cmbVer.getItems().addAll("1.20.4 (Latest)", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9");
                cmbVer.setValue(selectedVersion);
                cmbVer.setStyle("-fx-font-size: 14px; -fx-pref-width: 250; -fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #555; -fx-border-radius: 5;");
                cmbVer.setOnAction(e -> selectedVersion = cmbVer.getValue());

                ProgressBar pb = new ProgressBar(0);
                pb.setPrefWidth(300);
                pb.setVisible(false);

                Button btnDownload = createStyledButton("⬇ Download", true);
                btnDownload.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20; -fx-background-radius: 5;");
                
                Label lblStatus = new Label("");
                lblStatus.setStyle("-fx-text-fill: #aaa;");

                btnDownload.setOnAction(e -> {
                    btnDownload.setDisable(true);
                    pb.setVisible(true);
                    lblStatus.setText("Descargando " + selectedVersion + "...");
                    
                    // Simulación de descarga
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO, kv -> pb.setProgress(0)),
                        new KeyFrame(Duration.seconds(2), kv -> { pb.setProgress(1); lblStatus.setText("¡Descarga Completada!"); btnDownload.setText("✔ Listo"); })
                    );
                    timeline.play();
                });

                centerContent.getChildren().addAll(cmbVer, btnDownload, pb, lblStatus);
                break;

            case 6: // Tema
                if ("English".equals(language)) {
                    title.setText("Personalization");
                    desc.setText("Choose the visual look of the launcher. You can change it later in Settings.");
                } else if ("Português".equals(language)) {
                    title.setText("Personalização");
                    desc.setText("Escolha o visual do launcher. Você pode alterá-lo depois nas Configurações.");
                } else {
                    title.setText("Personalización");
                    desc.setText("Elige el aspecto visual del launcher. Podrás cambiarlo después en Ajustes.");
                }
                
                HBox themes = new HBox(20);
                themes.setAlignment(Pos.CENTER);
                
                ToggleGroup tg = new ToggleGroup();
                ToggleButton tbDark = createThemeButton("Dark", "#222", Color.WHITE);
                ToggleButton tbLight = createThemeButton("Light", "#eee", Color.BLACK);
                
                tbDark.setToggleGroup(tg);
                tbLight.setToggleGroup(tg);
                
                if ("dark".equals(theme)) tbDark.setSelected(true);
                else tbLight.setSelected(true);
                
                tbDark.setOnAction(e -> theme = "dark");
                tbLight.setOnAction(e -> theme = "light");
                
                themes.getChildren().addAll(tbDark, tbLight);
                centerContent.getChildren().add(themes);
                break;
        }

        // Animación de entrada suave
        FadeTransition ft = new FadeTransition(Duration.millis(300), centerContent);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        contentBox.getChildren().addAll(title, desc, centerContent, navButtons);
    }

    // --- Helpers de Estilo ---

    private Button createStyledButton(String text, boolean primary) {
        Button btn = new Button(text);
        String baseStyle = "-fx-cursor: hand; -fx-padding: 10 25; -fx-background-radius: 5; -fx-font-size: 14px; -fx-font-weight: bold;";
        
        // Animación de escala
        ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
        
        if (primary) {
            btn.setStyle(baseStyle + " -fx-background-color: #0078d7; -fx-text-fill: white;");
            btn.setOnMouseEntered(e -> {
                btn.setStyle(baseStyle + " -fx-background-color: #005a9e; -fx-text-fill: white;");
                st.setToX(1.05); st.setToY(1.05); st.playFromStart();
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle(baseStyle + " -fx-background-color: #0078d7; -fx-text-fill: white;");
                st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            });
        } else {
            btn.setStyle(baseStyle + " -fx-background-color: #444; -fx-text-fill: white;");
            btn.setOnMouseEntered(e -> {
                btn.setStyle(baseStyle + " -fx-background-color: #555; -fx-text-fill: white;");
                st.setToX(1.05); st.setToY(1.05); st.playFromStart();
            });
            btn.setOnMouseExited(e -> {
                btn.setStyle(baseStyle + " -fx-background-color: #444; -fx-text-fill: white;");
                st.setToX(1.0); st.setToY(1.0); st.playFromStart();
            });
        }
        return btn;
    }

    private ToggleButton createThemeButton(String text, String colorHex, Color textColor) {
        ToggleButton btn = new ToggleButton(text);
        btn.setPrefSize(120, 80);
        String hexText = String.format("#%02X%02X%02X", (int)(textColor.getRed()*255), (int)(textColor.getGreen()*255), (int)(textColor.getBlue()*255));
        
        String style = "-fx-background-color: " + colorHex + "; -fx-text-fill: " + hexText + "; -fx-background-radius: 10; -fx-border-color: #555; -fx-border-radius: 10; -fx-cursor: hand; -fx-font-size: 16px;";
        String selectedStyle = style + " -fx-border-color: #0078d7; -fx-border-width: 3; -fx-effect: dropshadow(three-pass-box, rgba(0,120,215,0.5), 10, 0, 0, 0);";
        
        btn.setStyle(style);
        btn.selectedProperty().addListener((obs, old, val) -> btn.setStyle(val ? selectedStyle : style));
        return btn;
    }

    private void detectJava() {
        String[] commonPaths = {
            "C:\\Program Files\\Java", "C:\\Program Files (x86)\\Java",
            "C:\\Program Files\\Eclipse Adoptium", "C:\\Program Files\\Amazon Corretto", "C:\\Program Files\\Azul\\zulu"
        };
        for (String path : commonPaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().contains("jre1.8") || f.getName().contains("jdk1.8") || f.getName().contains("jdk-8") || f.getName().contains("zulu8")) {
                            File bin = new File(f, "bin/javaw.exe");
                            if (!bin.exists()) bin = new File(f, "bin/java.exe");
                            if (bin.exists()) {
                                javaPath = bin.getAbsolutePath();
                                txtJavaPath.setText(javaPath);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finish() {
        try {
            File dataDir = new File((System.getenv("APPDATA") != null ? System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher");
            if (!dataDir.exists()) dataDir.mkdirs();
            
            JsonObject settings = new JsonObject();
            settings.addProperty("language", language);
            settings.addProperty("ram", ram);
            settings.addProperty("javaPath", javaPath);
            settings.addProperty("accountType", accountType);
            settings.addProperty("username", username);
            settings.addProperty("lastVersion", selectedVersion);
            
            // Configurar tema
            if ("light".equals(theme)) {
                settings.addProperty("accentColor", "#005a9e");
                settings.addProperty("overlayColor", "#FFFFFF");
                settings.addProperty("overlayOpacity", 0.6);
                settings.addProperty("blurRadius", 40);
            } else {
                settings.addProperty("accentColor", "#0078d7");
                settings.addProperty("overlayColor", "#000000");
                settings.addProperty("overlayOpacity", 0.8);
                settings.addProperty("blurRadius", 20);
            }
            
            // Valores por defecto
            settings.addProperty("width", "854");
            settings.addProperty("height", "480");
            settings.addProperty("fullscreen", false);
            settings.addProperty("enableAnimations", true);
            settings.addProperty("discordRpc", true);
            settings.addProperty("discordShowTime", true);
            settings.addProperty("autoUpdates", true);
            settings.addProperty("onLaunch", "No hacer nada");

            try (FileWriter writer = new FileWriter(new File(dataDir, "settings.json"))) {
                new Gson().toJson(settings, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
        
        if (onFinish != null) onFinish.run();
    }
}