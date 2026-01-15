package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import glauncher.MainView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewsView {

    private static final String API_BASE_URL = "https://glauncher-api.onrender.com";
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private VBox newsContainer;

    public Parent getView() {
        // Contenedor principal para toda la vista de noticias
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: transparent;"); // El fondo lo da el MainView

        Label title = new Label("Noticias y Novedades");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        title.setAlignment(Pos.CENTER_LEFT);

        // Contenedor para las tarjetas de noticias con scroll
        newsContainer = new VBox(25);
        ScrollPane scrollPane = new ScrollPane(newsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(title, scrollPane);

        fetchNewsFromApi();

        return root;
    }

    private void fetchNewsFromApi() {
        // 1. Mostrar un indicador de carga mientras se obtienen los datos
        Platform.runLater(() -> {
            ProgressIndicator loadingIndicator = new ProgressIndicator();
            loadingIndicator.setStyle("-fx-progress-color: #00aaff;");
            newsContainer.getChildren().add(loadingIndicator);
            newsContainer.setAlignment(Pos.CENTER);
        });

        executor.submit(() -> {
            try {
                URL url = new URL(API_BASE_URL + "/api/news");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "GLauncher/1.0");
                conn.setConnectTimeout(5000); // 5 segundos timeout
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        JsonArray newsArray = gson.fromJson(reader, JsonArray.class);
                        Platform.runLater(() -> displayNews(newsArray));
                    }
                } else {
                    throw new Exception("No se pudo obtener noticias. Código: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                // 2. Mostrar un mensaje de error en la UI si algo falla
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    private void displayNews(JsonArray newsArray) {
        newsContainer.getChildren().clear();
        newsContainer.setAlignment(Pos.TOP_LEFT);
        for (JsonElement newsElement : newsArray) {
            newsContainer.getChildren().add(createEnhancedNewsCard(newsElement.getAsJsonObject()));
        }
    }

    private void showError(String errorMessage) {
        newsContainer.getChildren().clear();
        newsContainer.setAlignment(Pos.CENTER);
        Label errorLabel = new Label("Error al cargar noticias: " + errorMessage);
        errorLabel.setStyle("-fx-text-fill: #ff8a80; -fx-font-size: 16px;");
        newsContainer.getChildren().add(errorLabel);
    }

    private StackPane createEnhancedNewsCard(JsonObject news) {
        // --- Contenedor Principal (StackPane para superponer elementos) ---
        StackPane card = new StackPane();
        card.setPrefHeight(250);
        card.setStyle("-fx-background-radius: 15; -fx-background-color: #1a1a1a;");
        card.setEffect(new DropShadow(20, Color.BLACK));

        // --- 1. Capa de Fondo (Imagen) ---
        ImageView imageView = new ImageView();
        String imageUrl = API_BASE_URL + news.get("image").getAsString();
        Image image = new Image(imageUrl, true); // Carga asíncrona
        imageView.setImage(image);
        imageView.setFitWidth(850); // Ancho aproximado, se ajustará
        imageView.setPreserveRatio(false);
        imageView.setOpacity(0.7);

        // --- 2. Capa de Degradado (Para legibilidad del texto) ---
        Pane gradient = new Pane();
        gradient.setStyle("-fx-background-color: linear-gradient(from 10% 25% to 100% 100%, rgba(0,0,0,0.2), rgba(0,0,0,0.9)); -fx-background-radius: 15;");

        // --- 3. Capa de Contenido (Texto y Botón) ---
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(25));
        contentBox.setAlignment(Pos.BOTTOM_LEFT);

        // Categoría y Fecha
        Label categoryLabel = new Label(news.get("category").getAsString().toUpperCase());
        categoryLabel.setStyle("-fx-background-color: #00aaff; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label dateLabel = new Label(news.get("date").getAsString());
        dateLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
        HBox categoryDate = new HBox(10, categoryLabel, dateLabel);
        categoryDate.setAlignment(Pos.CENTER_LEFT);

        // Título
        Label titleLabel = new Label(news.get("title").getAsString());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // Resumen
        Label summaryLabel = new Label(news.get("summary").getAsString());
        summaryLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px;");
        summaryLabel.setWrapText(true);
        summaryLabel.setMaxWidth(500);

        // Botón
        Button actionButton = new Button(news.get("buttonText").getAsString().toUpperCase());
        actionButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
        actionButton.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(news.get("link").getAsString()));
            } catch (Exception ex) {
                MainView.showNotification("Error", "No se pudo abrir el enlace.", "error");
            }
        });

        contentBox.getChildren().addAll(categoryDate, titleLabel, summaryLabel, new Separator(javafx.geometry.Orientation.HORIZONTAL), actionButton);
        
        // Unir las capas
        card.getChildren().addAll(imageView, gradient, contentBox);
        StackPane.setAlignment(contentBox, Pos.BOTTOM_LEFT);

        // --- 4. Animación de Hover ---
        Scale scale = new Scale(1, 1);
        card.getTransforms().add(scale);
        card.setOnMouseEntered(e -> {
            scale.setX(1.02); scale.setY(1.02);
            imageView.setOpacity(1.0);
        });
        card.setOnMouseExited(e -> {
            scale.setX(1); scale.setY(1);
            imageView.setOpacity(0.7);
        });

        return card;
    }
}
