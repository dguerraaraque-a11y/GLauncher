package glauncher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;
import javafx.stage.Stage;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.AWTException;
import java.io.File;
// [FIX] Importación corregida: UpdateChecker está en el mismo paquete
import glauncher.UpdateChecker;

public class GLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Capturar errores en hilos secundarios que no llegan al try-catch principal
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("!!! ERROR NO CAPTURADO EN HILO: " + t.getName() + " !!!");
            e.printStackTrace();
        });

        try {
        MainView mainView = new MainView();
        Parent root = mainView.getRoot();
        if (root == null) throw new RuntimeException("El root de MainView es nulo. Revisa el constructor de MainView.");

        Scene scene = new Scene(root, 1000, 700);
        
        primaryStage.setMaximized(true); // Puedes descomentar esto si quieres que inicie maximizado
        
        // Cargar CSS
        try {
            File css = new File("assets/css/theme.css");
            if (css.exists()) scene.getStylesheets().add(css.toURI().toString());
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar el tema CSS.");
        }

        // Cargar Icono de la aplicación
        try {
            File icon = new File("assets/icons/favicon.png");
            if (icon.exists()) primaryStage.getIcons().add(new Image(icon.toURI().toString()));
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar el icono (favicon.png).");
        }

        primaryStage.setTitle("GLauncher");
        primaryStage.setScene(scene);
        primaryStage.show();

        // [NUEVO] Configurar icono de bandeja del sistema (System Tray)
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.Image trayImage = null;
            try {
                // Cargar imagen desde recursos (funciona tanto en desarrollo como en app empaquetada)
                trayImage = java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/icons/favicon.png"));
                if (trayImage == null) { // Fallback si el recurso no se encuentra
                    System.err.println("Favicon.png no encontrado en recursos. Usando icono por defecto de AWT.");
                    // Asegúrate de tener un default_tray_icon.png en assets/icons
                    trayImage = java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/icons/default_tray_icon.png")); 
                }
            } catch (Exception e) {
                System.err.println("Error cargando imagen para TrayIcon: " + e.getMessage());
                // Fallback a un icono por defecto de AWT si la carga falla
                trayImage = java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/icons/default_tray_icon.png")); // Placeholder
            }

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Abrir GLauncher");
            MenuItem exitItem = new MenuItem("Salir");
            popup.add(openItem);
            popup.add(exitItem);

            final TrayIcon trayIcon = new TrayIcon(trayImage, "GLauncher", popup);
            trayIcon.setImageAutoSize(true);

            openItem.addActionListener(event -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
                tray.remove(trayIcon); // Quitar de la bandeja cuando la ventana se muestra
            }));
            exitItem.addActionListener(event -> {
                tray.remove(trayIcon);
                Platform.exit();
                System.exit(0);
            });

            primaryStage.setOnCloseRequest(event -> {
                event.consume(); // Consumir el evento para evitar que la app se cierre
                primaryStage.hide();
                try { tray.add(trayIcon); } catch (AWTException e) { System.err.println("Error añadiendo TrayIcon: " + e.getMessage()); }
            });
        } else {
            System.out.println("System Tray no soportado en este sistema.");
        }

        // [FIX] Ejecutar actualizaciones al final, cuando la ventana ya es visible
        // Esto evita que si falta la librería GSON, la app crashee antes de abrirse.
        UpdateChecker.checkForUpdates();
        
        } catch (Throwable t) {
            System.err.println("!!! ERROR FATAL DURANTE EL INICIO !!!");
            t.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}