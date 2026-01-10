package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsView {

    private final String API_BASE = "https://glauncher-api.onrender.com/api";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private VBox newsContainer;

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 15;");

        Label title = new Label("Noticias y Novedades");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        BorderPane.setMargin(title, new Insets(0, 0, 20, 0));

        newsContainer = new VBox(20);
        
        ScrollPane scrollPane = new ScrollPane(newsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.setTop(title);
        root.setCenter(scrollPane);

        fetchNews();

        return root;
    }

    private void fetchNews() {
        executor.submit(() -> {
            try {
                URL url = new URL(API_BASE + "/news");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "GLauncher/1.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JsonArray newsArray = gson.fromJson(reader, JsonArray.class);
                    Platform.runLater(() -> {
                        newsContainer.getChildren().clear();
                        for (JsonElement newsEl : newsArray) {
                            newsContainer.getChildren().add(createNewsCard(newsEl.getAsJsonObject()));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private BorderPane createNewsCard(JsonObject news) {
        BorderPane card = new BorderPane();
        card.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10; -fx-padding: 20;");

        // --- Contenido Izquierdo (Texto) ---
        VBox content = new VBox(10);
        
        Label category = new Label(news.get("category").getAsString().toUpperCase() + " - " + news.get("date").getAsString());
        category.setStyle("-fx-text-fill: #00aaff; -fx-font-weight: bold;");

        Label title = new Label(news.get("title").getAsString());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        title.setWrapText(true);

        Label summary = new Label(news.get("summary").getAsString());
        summary.setStyle("-fx-text-fill: #ccc;");
        summary.setWrapText(true);

        Button readMore = new Button(news.get("buttonText").getAsString());
        readMore.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-cursor: hand;");
        readMore.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(news.get("link").getAsString()));
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        content.getChildren().addAll(category, title, summary, readMore);

        // --- Contenido Derecho (Imagen) ---
        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        String imageUrl = API_BASE.replace("/api", "") + news.get("image").getAsString();
        Image image = new Image(imageUrl, true); // Carga en segundo plano
        imageView.setImage(image);

        card.setLeft(content);
        card.setRight(imageView);
        BorderPane.setAlignment(imageView, Pos.CENTER);
        BorderPane.setMargin(content, new Insets(0, 20, 0, 0));

        return card;
    }
}
