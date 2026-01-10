package glauncher.ui.views;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomeView extends VBox {

    public HomeView() {
        setStyle("-fx-padding: 20;");
        getChildren().add(new Label("🏠 Inicio"));
    }
}
