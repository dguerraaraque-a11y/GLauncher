package glauncher.installer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.scene.image.Image;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Installer extends Application {

    private double xOffset = 0;
    private double yOffset = 0;

    private Stage stage;
    private BorderPane rootLayout;
    private VBox sidebar;
    private StackPane contentArea;
    private HBox navBar;
    private Button btnNext;
    private Button btnBack;

    private int currentStep = 1;
    private final int TOTAL_STEPS = 4;

    // Datos de instalación
    private String installPath;
    private boolean createDesktopShortcut = true;
    private boolean createStartMenuShortcut = true;
    private boolean keepData = true;

    // Componentes UI para mantener estado
    private CheckBox chkEula;
    private TextField txtPath;
    private CheckBox chkDesktop;
    private CheckBox chkStartMenu;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextArea logArea;
    private Button btnFinish;
    private Button btnRun;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        
        // [NUEVO] Cargar icono de la ventana
        try {
            primaryStage.getIcons().add(new Image("file:assets/icons/favicon.png"));
        } catch (Exception e) { /* Ignorar si no existe */ }

        // Ruta por defecto solicitada
        // [FIX] Usar APPDATA para evitar problemas de permisos de Administrador en Windows
        installPath = System.getenv("APPDATA") + File.separator + "GLauncher";

        // --- DISEÑO MINIMALISTA GRIS ---
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #444; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 0);");
        
        // [NUEVO] CSS Global para eliminar estilos por defecto de JavaFX (CheckBox, ScrollBar, TextField)
        rootLayout.getStylesheets().add("data:text/css," + 
            ".root { -fx-font-family: 'Segoe UI', sans-serif; }" +
            ".check-box .box { -fx-background-color: #333; -fx-border-color: #555; -fx-border-radius: 3; }" +
            ".check-box:selected .mark { -fx-background-color: white; -fx-shape: 'M0,0 L2,0 L2,2 L0,2 Z'; }" +
            ".check-box:selected .box { -fx-background-color: #0078d7; -fx-border-color: #0078d7; }" +
            ".check-box:hover .box { -fx-border-color: #00aaff; -fx-background-color: #444; }" + // Efecto hover en la caja
            ".text-field { -fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: #777; -fx-border-color: #555; -fx-border-radius: 3; }" +
            ".text-field:focused { -fx-border-color: #0078d7; -fx-background-color: #383838; }" +
            ".button { -fx-background-radius: 5; -fx-cursor: hand; }" +
            ".progress-bar .track { -fx-background-color: #333; -fx-background-radius: 5; }" +
            ".progress-bar .bar { -fx-background-color: #0078d7; -fx-background-radius: 5; }" +
            // [NUEVO] Estilo ScrollBar personalizado
            ".scroll-bar { -fx-background-color: transparent; }" +
            ".scroll-bar .track { -fx-background-color: transparent; }" +
            ".scroll-bar .thumb { -fx-background-color: #555; -fx-background-radius: 5; }" +
            ".scroll-bar .thumb:hover { -fx-background-color: #0078d7; }"
        );
        rootLayout.setPrefSize(750, 500); // Ventana más grande

        // Barra de título
        HBox titleBar = createTitleBar();
        rootLayout.setTop(titleBar);

        // Sidebar (Pasos)
        sidebar = createSidebar();
        rootLayout.setLeft(sidebar);

        // Área de Contenido
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(30));
        rootLayout.setCenter(contentArea);

        // Barra de Navegación
        navBar = createNavBar();
        rootLayout.setBottom(navBar);

        // Mostrar primer paso
        showStep(1);

        // Arrastrar ventana
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(rootLayout);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox(10);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(10, 15, 10, 15));
        titleBar.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Instalador GLauncher");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 14px; -fx-cursor: hand;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #aaa; -fx-font-size: 14px; -fx-cursor: hand;"));
        btnClose.setOnAction(e -> System.exit(0));

        titleBar.getChildren().addAll(title, spacer, btnClose);
        return titleBar;
    }

    private VBox createSidebar() {
        VBox box = new VBox(5);
        box.setPrefWidth(200);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #252526; -fx-border-color: #333; -fx-border-width: 0 1 0 0;");
        return box;
    }

    private void updateSidebar() {
        sidebar.getChildren().clear();
        String[] steps = {"1. EULA y Condiciones", "2. Ruta de Instalación", "3. Opciones", "4. Instalar"};
        
        for (int i = 0; i < steps.length; i++) {
            Label lbl = new Label(steps[i]);
            lbl.setPadding(new Insets(8));
            lbl.setMaxWidth(Double.MAX_VALUE);
            
            if (i + 1 == currentStep) {
                // Paso actual
                // [MODIFICADO] Fondo azul para resaltar el paso actual
                lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-background-color: #0078d7; -fx-background-radius: 5;");
            } else if (i + 1 < currentStep) {
                // Paso completado
                lbl.setStyle("-fx-text-fill: #888;");
            } else {
                // Paso futuro
                lbl.setStyle("-fx-text-fill: #555;");
            }
            sidebar.getChildren().add(lbl);
        }
    }

    private HBox createNavBar() {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(15, 30, 15, 30));
        box.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 1 0 0 0;");

        btnBack = new Button("Atrás");
        btnBack.setStyle("-fx-background-color: #3e3e42; -fx-text-fill: white; -fx-cursor: hand; -fx-min-width: 80px;");
        btnBack.setOnAction(e -> navigate(-1));

        btnNext = new Button("Siguiente");
        btnNext.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand; -fx-min-width: 80px; -fx-font-weight: bold;");
        btnNext.setOnAction(e -> navigate(1));

        box.getChildren().addAll(btnBack, btnNext);
        return box;
    }

    private void navigate(int direction) {
        if (direction == 1) {
            if (!validateStep()) return;
            if (currentStep < TOTAL_STEPS) {
                showStep(currentStep + 1);
            }
        } else {
            if (currentStep > 1) {
                showStep(currentStep - 1);
            }
        }
    }

    private boolean validateStep() {
        if (currentStep == 1) {
            return chkEula.isSelected();
        }
        if (currentStep == 2) {
            String path = txtPath.getText().trim();
            if (path.isEmpty()) return false;
            
            File dir = new File(path);
            if (dir.exists() && dir.list() != null && dir.list().length > 0) {
                return showOverwriteDialog(dir);
            }
            installPath = path;
            return true;
        }
        return true;
    }

    private boolean showOverwriteDialog(File dir) {
        // Ventana de advertencia personalizada
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #ffcc00; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        root.setPrefWidth(400);

        Label title = new Label("⚠ Carpeta ya existente");
        title.setStyle("-fx-text-fill: #ffcc00; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label msg = new Label("La carpeta de instalación ya existe y contiene archivos.\n¿Qué deseas hacer?");
        msg.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");

        CheckBox chkKeep = new CheckBox("Mantener versiones y mods (Actualizar)");
        chkKeep.setSelected(true);
        styleCheckBox(chkKeep); // Aplicar estilo y animación

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-background-color: #3e3e42; -fx-text-fill: white;");
        btnCancel.setOnAction(e -> { dialog.close(); });

        Button btnContinue = new Button("Continuar");
        btnContinue.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white;");
        
        final boolean[] result = {false};

        btnContinue.setOnAction(e -> {
            keepData = chkKeep.isSelected();
            installPath = dir.getAbsolutePath();
            result[0] = true;
            dialog.close();
        });

        buttons.getChildren().addAll(btnCancel, btnContinue);
        root.getChildren().addAll(title, msg, chkKeep, buttons);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }

    private void showStep(int step) {
        currentStep = step;
        contentArea.getChildren().clear();
        updateSidebar();
        
        btnBack.setDisable(step == 1 || step == 4);
        btnNext.setDisable(step == 4);
        btnNext.setText(step == 3 ? "Instalar" : "Siguiente");

        Node view = null;
        switch (step) {
            case 1: view = createStep1(); break;
            case 2: view = createStep2(); break;
            case 3: view = createStep3(); break;
            case 4: 
                view = createStep4(); 
                navBar.setVisible(false); // Ocultar navegación durante instalación
                startInstallation(); 
                break;
        }
        
        // [NUEVO] Animación de entrada (Fade + Slide Up)
        if (view != null) {
            view.setOpacity(0);
            view.setTranslateY(10);
            contentArea.getChildren().add(view);
            
            // [FIX] Corregido error de compilación: Instanciar transiciones sin doble llave
            FadeTransition ft = new FadeTransition(Duration.millis(400), view);
            ft.setToValue(1);
            
            TranslateTransition tt = new TranslateTransition(Duration.millis(400), view);
            tt.setToY(0);
            
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.play();
        }
    }

    // --- PASOS ---

    private VBox createStep1() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.TOP_LEFT);
        
        Label title = new Label("Acuerdo de Licencia");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        TextArea eulaText = new TextArea(
            "ACUERDO DE LICENCIA DE USUARIO FINAL (EULA) - GLAUNCHER\n\n" +
            "1. ACEPTACION\n" +
            "Al instalar o utilizar este software, usted acepta los terminos de este acuerdo.\n\n" +
            "2. USO\n" +
            "GLauncher es un software gratuito desarrollado por la comunidad. \n" +
            "No esta afiliado, asociado ni respaldado por Mojang Studios o Microsoft.\n" +
            "No se permite su venta ni instalacion pagada.\n\n" +
            "3. RESPONSABILIDAD\n" +
            "El software se proporciona \"tal cual\", sin garantia de ningun tipo. \n" +
            "Los autores no se hacen responsables de danos en su equipo o perdida de datos\n" +
            "aun que el sotware no esta afiliado y puede dar error de desconocido.\n\n" +
            "4. DISTRIBUCION\n" +
            "Usted puede distribuir este software libremente."
        );
        eulaText.setEditable(false);
        eulaText.setWrapText(true);
        eulaText.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #ccc; -fx-font-family: 'Consolas';");
        VBox.setVgrow(eulaText, Priority.ALWAYS);

        chkEula = new CheckBox("He leído y acepto los términos y condiciones");
        styleCheckBox(chkEula); // Aplicar estilo y animación
        chkEula.selectedProperty().addListener((obs, old, val) -> btnNext.setDisable(!val));
        
        btnNext.setDisable(!chkEula.isSelected()); // Estado inicial

        box.getChildren().addAll(title, eulaText, chkEula);
        return box;
    }

    private VBox createStep2() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Ruta de Instalación");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label desc = new Label("Selecciona dónde quieres instalar GLauncher.");
        desc.setStyle("-fx-text-fill: #aaa;");

        HBox pathBox = new HBox(10);
        txtPath = new TextField(installPath);
        txtPath.setPromptText("Ruta...");
        txtPath.setPrefWidth(300);
        txtPath.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white; -fx-border-color: #444; -fx-border-radius: 3;");
        HBox.setHgrow(txtPath, Priority.ALWAYS);

        Button btnBrowse = new Button("Examinar...");
        btnBrowse.setStyle("-fx-background-color: #3e3e42; -fx-text-fill: white; -fx-cursor: hand;");
        btnBrowse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Seleccionar carpeta de instalación");
            File f = dc.showDialog(stage);
            if (f != null) txtPath.setText(f.getAbsolutePath());
        });

        pathBox.getChildren().addAll(txtPath, btnBrowse);
        
        Label info = new Label("Espacio requerido: ~150 MB");
        info.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        box.getChildren().addAll(title, desc, pathBox, info);
        return box;
    }

    private VBox createStep3() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Opciones de Instalación");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        chkDesktop = new CheckBox("Crear acceso directo en el Escritorio");
        chkDesktop.setSelected(createDesktopShortcut);
        styleCheckBox(chkDesktop); // Aplicar estilo y animación
        chkDesktop.selectedProperty().addListener((obs, old, val) -> createDesktopShortcut = val);

        chkStartMenu = new CheckBox("Crear acceso directo en el Menú de Inicio");
        chkStartMenu.setSelected(createStartMenuShortcut);
        styleCheckBox(chkStartMenu); // Aplicar estilo y animación
        chkStartMenu.selectedProperty().addListener((obs, old, val) -> createStartMenuShortcut = val);

        box.getChildren().addAll(title, chkDesktop, chkStartMenu);
        return box;
    }

    private VBox createStep4() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);

        Label title = new Label("Instalando GLauncher...");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(20);
        progressBar.setStyle("-fx-accent: #0078d7;");

        statusLabel = new Label("Iniciando...");
        statusLabel.setStyle("-fx-text-fill: #aaa;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        logArea.setMaxWidth(500);
        logArea.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas';");

        btnFinish = new Button("Finalizar");
        btnFinish.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-cursor: hand;");
        btnFinish.setVisible(false);
        btnFinish.setOnAction(e -> System.exit(0));

        btnRun = new Button("Ejecutar GLauncher");
        btnRun.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-cursor: hand;");
        btnRun.setVisible(false);
        btnRun.setOnAction(e -> {
            launchApp();
            System.exit(0);
        });

        HBox buttons = new HBox(15, btnRun, btnFinish);
        buttons.setAlignment(Pos.CENTER);

        box.getChildren().addAll(title, progressBar, statusLabel, logArea, buttons);
        return box;
    }

    private void startInstallation() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Preparando instalación...");
                updateProgress(0, 100);

                File installDir = new File(installPath);
                
                // Limpieza si no se mantiene data
                if (!keepData && installDir.exists()) {
                    updateMessage("Limpiando instalación anterior...");
                    deleteDirectory(installDir);
                }
                
                if (!installDir.exists()) installDir.mkdirs();

                // Extraer app.zip
                try (InputStream is = getClass().getResourceAsStream("/app.zip")) {
                    if (is == null) {
                        updateMessage("Error: No se encontró el paquete de instalación (app.zip).");
                        return null;
                    }
                    
                    ZipInputStream zis = new ZipInputStream(is);
                    ZipEntry entry;
                    
                    while ((entry = zis.getNextEntry()) != null) {
                        File newFile = new File(installDir, entry.getName());
                        
                        // Si keepData es true, no sobrescribir carpetas de datos importantes
                        if (keepData && (entry.getName().startsWith("data/") || entry.getName().startsWith("versions/"))) {
                            if (newFile.exists()) {
                                zis.closeEntry();
                                continue;
                            }
                        }

                        updateMessage("Extrayendo: " + entry.getName());
                        
                        if (entry.isDirectory()) {
                            newFile.mkdirs();
                        } else {
                            newFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                        zis.closeEntry();
                        updateProgress(-1, 100); 
                    }
                    zis.close();
                } catch (Exception e) {
                    updateMessage("Error crítico: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }

                updateProgress(90, 100);
                updateMessage("Creando accesos directos...");

                // Crear Accesos Directos
                String exePath = installPath + File.separator + "GLauncher.bat"; // O .exe
                String iconPath = installPath + File.separator + "assets" + File.separator + "icons" + File.separator + "icons-gui" + File.separator + "tuerca.png";
                
                if (createDesktopShortcut) {
                    String desktopPath = System.getProperty("user.home") + "/Desktop/GLauncher.lnk";
                    createShortcut(desktopPath, exePath, iconPath);
                }
                
                if (createStartMenuShortcut) {
                    String startMenuPath = System.getenv("APPDATA") + "/Microsoft/Windows/Start Menu/Programs/GLauncher.lnk";
                    createShortcut(startMenuPath, exePath, iconPath);
                }

                updateProgress(100, 100);
                updateMessage("¡Instalación Completada!");
                
                Platform.runLater(() -> {
                    btnFinish.setVisible(true);
                    btnRun.setVisible(true);
                    logArea.appendText("\n--- INSTALACIÓN EXITOSA ---\n");
                    logArea.appendText("Instalado en: " + installPath + "\n");
                });

                return null;
            }
        };

        statusLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());
        task.messageProperty().addListener((obs, old, newVal) -> Platform.runLater(() -> logArea.appendText(newVal + "\n")));

        new Thread(task).start();
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) deleteDirectory(entry);
            }
        }
        file.delete();
    }

    private void createShortcut(String lnkPath, String targetPath, String iconPath) {
        try {
            String vbsScript = "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
                    "sLinkFile = \"" + lnkPath + "\"\n" +
                    "Set oLink = oWS.CreateShortcut(sLinkFile)\n" +
                    "oLink.TargetPath = \"" + targetPath + "\"\n" +
                    "oLink.IconLocation = \"" + iconPath + "\"\n" +
                    "oLink.WindowStyle = 1\n" + 
                    "oLink.Save";
            
            File script = File.createTempFile("shortcut_gen", ".vbs");
            Files.writeString(script.toPath(), vbsScript);
            
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + script.getAbsolutePath());
            p.waitFor();
            script.delete();
        } catch (Exception e) {
            Platform.runLater(() -> logArea.appendText("Error creando acceso directo: " + e.getMessage() + "\n"));
        }
    }

    private void launchApp() {
        try {
            String exe = installPath + File.separator + "GLauncher.bat";
            if (!new File(exe).exists()) exe = installPath + File.separator + "GLauncher.exe";
            
            if (new File(exe).exists()) {
                if (exe.endsWith(".bat")) {
                    new ProcessBuilder("cmd", "/c", "start", "\"\"", exe).directory(new File(installPath)).start();
                } else {
                    new ProcessBuilder(exe).directory(new File(installPath)).start();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // [NUEVO] Método auxiliar para estilizar y animar CheckBoxes
    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 14px;");
        
        // Animación de escala al pasar el mouse
        ScaleTransition st = new ScaleTransition(Duration.millis(200), cb);
        cb.setOnMouseEntered(e -> {
            st.setToX(1.05);
            st.setToY(1.05);
            st.playFromStart();
        });
        cb.setOnMouseExited(e -> {
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
