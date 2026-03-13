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

    // URL DEL ARCHIVO DE VERSIÓN EN TU SERVIDOR
    private static final String UPDATE_URL = "https://glauncher-api.onrender.com/version.json";
    
    // VERSIÓN ACTUAL DE TU APLICACIÓN JAVA (¡IMPORTANTE!)
    // Debes cambiar este valor cada vez que compiles una nueva versión del launcher.
    private static final String CURRENT_VERSION = "1.0.0"; 

    public static void checkForUpdates() {
        // Se ejecuta en un hilo separado para no congelar la interfaz de usuario
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // Tiempos de espera para evitar que la app se quede colgada si no hay conexión
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) { // Código 200
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                    
                    // Leemos los campos del version.json que creamos
                    String latestVersion = response.get("latestVersion").getAsString();
                    String downloadUrl = response.get("downloadUrl").getAsString();
                    String releaseNotes = response.get("releaseNotes").getAsString();

                    // Comparamos la versión del servidor con la versión actual de la app
                    if (isNewer(latestVersion, CURRENT_VERSION)) {
                        // Si hay una actualización, mostramos el diálogo en el hilo de la UI de JavaFX
                        Platform.runLater(() -> showUpdateDialog(latestVersion, downloadUrl, releaseNotes));
                    } else {
                        System.out.println("[UpdateChecker] La aplicación ya está actualizada.");
                    }
                } else {
                    System.out.println("[UpdateChecker] Error al contactar el servidor: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                System.out.println("[UpdateChecker] No se pudo verificar actualizaciones: " + e.getMessage());
                // Opcional: podrías mostrar un pequeño aviso al usuario de que no se pudo comprobar.
            }
        });
        thread.setDaemon(true); // El hilo no impedirá que la aplicación se cierre
        thread.start();
    }

    private static void showUpdateDialog(String version, String url, String notes) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("¡Actualización Disponible!");
        alert.setHeaderText("¡Hay una nueva versión de GLauncher disponible (" + version + ")!");
        alert.setContentText("Notas de la versión:\n" + notes + "\n\n¿Quieres ir a la página de descargas ahora?");
        
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    // Abre el navegador por defecto del usuario
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(url));
                    }
                } catch (Exception e) {
                    System.err.println("Error al abrir el navegador: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Compara dos números de versión semántica (ej. "1.0.1", "1.1").
     * @return true si la nueva versión (v1) es mayor que la versión actual (v2).
     */
    private static boolean isNewer(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int p2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;
            if (p1 > p2) return true;
            if (p1 < p2) return false;
        }
        return false; // Las versiones son iguales
    }
}
