package glauncher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    // URL de tu API (Asegúrate de que coincida con tu dominio en producción)
    private static final String UPDATE_API = "https://glauncher-api.onrender.com/api/updates/latest?platform=windows";
    private static final String CURRENT_VERSION = "1.1"; // Debe coincidir con la versión de tu instalador

    public static void checkForUpdates() {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(UPDATE_API);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                    
                    String latestVersion = response.get("version").getAsString();
                    String downloadUrl = response.get("url").getAsString();

                    // Si la versión de la nube es mayor que la local
                    if (latestVersion.compareTo(CURRENT_VERSION) > 0) {
                        Platform.runLater(() -> showUpdateDialog(latestVersion, downloadUrl));
                    }
                }
            } catch (Exception e) {
                System.out.println("[UpdateChecker] No se pudo verificar actualizaciones: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static void showUpdateDialog(String version, String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Actualización Disponible");
        alert.setHeaderText("¡Nueva versión " + version + " disponible!");
        alert.setContentText("Se ha detectado una nueva versión de GLauncher.\n¿Quieres ir a descargarla ahora?");
        
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
}