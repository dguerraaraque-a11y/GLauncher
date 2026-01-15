package glauncher.ui.views;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class AccountView {

    public Parent getView() {
        StackPane root = new StackPane();
        root.getChildren().add(new Label("Account View"));
        return root;
    }
}