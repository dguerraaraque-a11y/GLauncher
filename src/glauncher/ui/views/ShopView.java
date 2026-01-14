package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShopView {

    private final String API_BASE_URL = "https://glauncher-api.onrender.com";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private FlowPane itemsGrid;

    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 15;");

        Label title = new Label("Tienda de Cosméticos");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        VBox.setMargin(title, new Insets(0, 0, 10, 0));
        root.setAlignment(Pos.TOP_CENTER);
        
        itemsGrid = new FlowPane(20, 20); // Hgap, Vgap
        itemsGrid.setAlignment(Pos.CENTER);
        itemsGrid.setPadding(new Insets(10));

        ScrollPane scroll = new ScrollPane(itemsGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(title, scroll);
        
        fetchShopItems();
        
        return root;
    }

    private void fetchShopItems() {
        executor.submit(() -> {
            try {
                URL url = new URL(API_BASE_URL + "/api/shop"); // Endpoint corregido
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "GLauncher/1.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JsonArray items = gson.fromJson(reader, JsonArray.class);
                    
                    Platform.runLater(() -> {
                        itemsGrid.getChildren().clear();
                        for (JsonElement itemElement : items) {
                            itemsGrid.getChildren().add(createItemCard(itemElement.getAsJsonObject()));
                        }
                    });
                } else {
                     Platform.runLater(() -> {
                        Label errorLabel = new Label("No se pudieron cargar los artículos. Código: " + conn.getResponseCode());
                        errorLabel.setStyle("-fx-text-fill: #ff8080;");
                        itemsGrid.getChildren().add(errorLabel);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Error de conexión al cargar la tienda.");
                    errorLabel.setStyle("-fx-text-fill: #ff8080;");
                    itemsGrid.getChildren().add(errorLabel);
                });
                e.printStackTrace();
            }
        });
    }

    private VBox createItemCard(JsonObject item) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 15; -fx-border-radius: 15;");
        card.setPrefSize(180, 250);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(15));

        // [CORREGIDO] Construir la URL de la imagen de forma segura
        String imageUrl = API_BASE_URL + item.get("image_url").getAsString();
        ImageView imageView = new ImageView(new Image(imageUrl, 120, 120, true, true, true));
        imageView.setPreserveRatio(true);
        
        Label name = new Label(item.get("name").getAsString());
        name.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        name.setAlignment(Pos.CENTER);
        name.setWrapText(true);
        
        Label price = new Label(item.get("price").getAsInt() + " G-Coins");
        price.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button buyButton = new Button("Comprar");
        buyButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        buyButton.setOnAction(e -> {
            // TODO: Implementar lógica de compra
            System.out.println("Intentando comprar: " + item.get("name").getAsString());
        });

        // Espaciador para empujar el botón hacia abajo
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageView, name, price, spacer, buyButton);
        return card;
    }
}
