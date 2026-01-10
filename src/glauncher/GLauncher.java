package glauncher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import glauncher.api.UpdateChecker;

public class GLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        // [FIX] Iniciar comprobación de actualizaciones en segundo plano
        UpdateChecker.checkForUpdates();

        MainView mainView = new MainView();
        Scene scene = new Scene(mainView.getRoot(), 1000, 700);
        
        primaryStage.setMaximized(true); // Puedes descomentar esto si quieres que inicie maximizado
        
        // Cargar CSS
        try {
            scene.getStylesheets().add("file:assets/css/theme.css");
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar el tema CSS.");
        }

        // Cargar Icono de la aplicación
        try {
            primaryStage.getIcons().add(new Image("file:assets/icons/favicon.png"));
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar el icono (favicon.png).");
        }

        primaryStage.setTitle("GLauncher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}