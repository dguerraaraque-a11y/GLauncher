package glauncher.ui;

import javafx.scene.layout.BorderPane;
import glauncher.ui.views.HomeView;

public class MainLayout extends BorderPane {

    public MainLayout() {
        setCenter(new HomeView());
        setBottom(new Navbar(this));
    }

    public void setView(javafx.scene.Node view) {
        setCenter(view);
    }
}
