package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ShopView {

    private FlowPane itemsGrid;

    public Parent getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Tienda de Cosméticos");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        itemsGrid = new FlowPane();
        itemsGrid.setHgap(20);
        itemsGrid.setVgap(20);
        itemsGrid.setAlignment(Pos.CENTER);

        ScrollPane scroll = new ScrollPane(itemsGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(title, scroll);
        fetchShopItems();
        return root;
    }

    private void fetchShopItems() {
        Thread t = new Thread(() -> {
            try {
                String json = fetchUrl("https://glauncher-api.onrender.com/api/shop/items");
                JsonArray items = new Gson().fromJson(json, JsonArray.class);
                
                Platform.runLater(() -> {
                    for (JsonElement el : items) {
                        itemsGrid.getChildren().add(createItemCard(el.getAsJsonObject()));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private VBox createItemCard(JsonObject item) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: rgba(50,50,50,0.8); -fx-background-radius: 10; -fx-padding: 10;");
        card.setPrefSize(150, 200);
        card.setAlignment(Pos.CENTER);

        String imgUrl = item.get("image_url").getAsString();
        if (imgUrl.startsWith("/")) imgUrl = "https://glauncher.vercel.app" + imgUrl;
        if (imgUrl.startsWith("/")) imgUrl = "http://localhost:5000" + imgUrl;
        
        ImageView img = new ImageView(new Image(imgUrl, 100, 100, true, true, true));
        
        Label name = new Label(item.get("name").getAsString());
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        
        Label price = new Label(item.get("price").getAsInt() + " GCoins");
        price.setStyle("-fx-text-fill: gold;");

        card.getChildren().addAll(img, name, price);
        return card;
    }

    private String fetchUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line; while ((line = in.readLine()) != null) content.append(line);
            return content.toString();
        }
    }
}
