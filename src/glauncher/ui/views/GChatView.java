package glauncher.ui.views;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import glauncher.MainView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.Files;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GChatView {

    private final String DATA_DIR = (System.getenv("APPDATA") != null ? 
        System.getenv("APPDATA") : System.getProperty("user.home")) + File.separator + ".glauncher";
    private final File SESSION_FILE = new File(DATA_DIR, "session.json");
    private final String API_BASE = "https://glauncher-api.onrender.com/api"; 

    private final Gson gson = new Gson();
    private String authToken = null;
    private int currentUserId = -1;
    private String currentUsername = "Yo"; // Valor por defecto

    private JsonArray fullFriendList = new JsonArray();
    private TextField searchFriendsField;
    private Label typingIndicator;
    private long lastTypingEventSent = 0;

    private VBox friendListContainer;
    private VBox requestsListContainer;
    private VBox chatArea;
    private ScrollPane chatScroll;
    private VBox messagesContainer;
    private TextField messageInput;
    private TextField searchChatField; // Nuevo campo de búsqueda
    private Button saveChatButton;     // Nuevo botón de guardar
    private Label chatHeaderLabel;
    private Label statusLabel;
    
    private int activeFriendId = -1;
    private ScheduledExecutorService poller;

    public Parent getView() {
        loadSession();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 15;");

        if (authToken == null) {
            Label lblError = new Label("Debes iniciar sesión para usar GChat.");
            lblError.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
            root.setCenter(lblError);
            return root;
        }

        // --- BARRA LATERAL (Amigos y Solicitudes) ---
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 10; -fx-padding: 10;");

        Label lblFriends = new Label("GChat");
        lblFriends.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        
        // Añadir Amigo
        HBox addFriendBox = new HBox(5);
        TextField txtAddFriend = new TextField();
        txtAddFriend.setPromptText("Añadir amigo (Usuario)");
        txtAddFriend.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        HBox.setHgrow(txtAddFriend, Priority.ALWAYS);
        
        Button btnAddFriend = new Button("+");
        btnAddFriend.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAddFriend.setOnAction(e -> {
            sendFriendRequest(txtAddFriend.getText());
            txtAddFriend.clear();
        });
        addFriendBox.getChildren().addAll(txtAddFriend, btnAddFriend);

        // Campo de búsqueda de amigos
        searchFriendsField = new TextField();
        searchFriendsField.setPromptText("Buscar amigos...");
        searchFriendsField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: #888; -fx-background-radius: 5;");
        searchFriendsField.textProperty().addListener((obs, old, val) -> updateFriendList(fullFriendList));

        // Contenedores de listas
        friendListContainer = new VBox(5);
        ScrollPane scrollFriends = new ScrollPane(friendListContainer);
        scrollFriends.setFitToWidth(true);
        scrollFriends.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        requestsListContainer = new VBox(5);
        ScrollPane scrollRequests = new ScrollPane(requestsListContainer);
        scrollRequests.setFitToWidth(true);
        scrollRequests.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Botones de filtro en lugar de TabPane
        ToggleButton btnFriends = new ToggleButton("Amigos");
        ToggleButton btnRequests = new ToggleButton("Solicitudes");
        ToggleGroup filterGroup = new ToggleGroup();
        btnFriends.setToggleGroup(filterGroup);
        btnRequests.setToggleGroup(filterGroup);
        btnFriends.setSelected(true);

        String toggleStyle = "-fx-background-color: #444; -fx-text-fill: white; -fx-border-color: transparent; -fx-cursor: hand; -fx-background-radius: 5 0 0 5;";
        String selectedToggleStyle = "-fx-background-color: #0078d7; -fx-text-fill: white; -fx-border-color: transparent; -fx-cursor: hand; -fx-background-radius: 5 0 0 5;";
        String toggleStyleRight = toggleStyle.replace("5 0 0 5", "0 5 5 0");
        String selectedToggleStyleRight = selectedToggleStyle.replace("5 0 0 5", "0 5 5 0");

        btnFriends.setStyle(selectedToggleStyle);
        btnRequests.setStyle(toggleStyleRight);

        filterGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                if (oldToggle != null) oldToggle.setSelected(true);
            } else {
                btnFriends.setStyle(newToggle == btnFriends ? selectedToggleStyle : toggleStyle);
                btnRequests.setStyle(newToggle == btnRequests ? selectedToggleStyleRight : toggleStyleRight);
            }
        });

        HBox filterBox = new HBox(0, btnFriends, btnRequests);
        filterBox.setAlignment(Pos.CENTER);
        btnFriends.setMaxWidth(Double.MAX_VALUE);
        btnRequests.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnFriends, Priority.ALWAYS);
        HBox.setHgrow(btnRequests, Priority.ALWAYS);

        StackPane listStack = new StackPane(scrollRequests, scrollFriends);
        VBox.setVgrow(listStack, Priority.ALWAYS);

        scrollFriends.visibleProperty().bind(btnFriends.selectedProperty());
        scrollRequests.visibleProperty().bind(btnRequests.selectedProperty());

        sidebar.getChildren().addAll(lblFriends, addFriendBox, searchFriendsField, filterBox, listStack);

        // --- ÁREA DE CHAT ---
        chatArea = new VBox(10);
        chatArea.setPadding(new Insets(0, 0, 0, 20));
        chatArea.setVisible(false);

        // Cabecera del Chat
        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15; -fx-background-radius: 10;");
        chatHeaderLabel = new Label("Chat");
        chatHeaderLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        typingIndicator = new Label("Escribiendo...");
        typingIndicator.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");
        typingIndicator.setVisible(false);
        chatHeader.getChildren().addAll(chatHeaderLabel, spacer, typingIndicator);

        // --- BARRA DE HERRAMIENTAS DEL CHAT (Búsqueda y Guardado) ---
        HBox chatTools = new HBox(10);
        chatTools.setAlignment(Pos.CENTER_LEFT);
        chatTools.setPadding(new Insets(5, 10, 5, 10));
        chatTools.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 5;");

        searchChatField = new TextField();
        searchChatField.setPromptText("Buscar en el chat...");
        searchChatField.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 15;");
        searchChatField.setPrefWidth(200);
        searchChatField.textProperty().addListener((obs, oldVal, newVal) -> filterMessages(newVal));

        saveChatButton = new Button("Guardar Chat");
        saveChatButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 5;");
        saveChatButton.setOnAction(e -> saveChat());

        Region toolSpacer = new Region();
        HBox.setHgrow(toolSpacer, Priority.ALWAYS);

        chatTools.getChildren().addAll(searchChatField, toolSpacer, saveChatButton);

        // Mensajes
        messagesContainer = new VBox(10);
        messagesContainer.setPadding(new Insets(10));
        chatScroll = new ScrollPane(messagesContainer);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        chatScroll.vvalueProperty().bind(messagesContainer.heightProperty()); // Auto-scroll al fondo
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Input de Mensaje
        HBox inputBox = new HBox(10);
        messageInput = new TextField();
        messageInput.setPromptText("Escribe un mensaje...");
        messageInput.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 20;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        Button btnAttach = new Button("📎");
        btnAttach.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        btnAttach.setOnAction(e -> showAttachmentMenu(btnAttach));

        Button btnEmoji = new Button("😀");
        btnEmoji.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-cursor: hand;");
        btnEmoji.setOnAction(e -> showEmojiPicker());
        
        Button btnSend = new Button("➤");
        btnSend.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 50; -fx-min-width: 40px; -fx-min-height: 40px; -fx-cursor: hand;");
        btnSend.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
        
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            long now = System.currentTimeMillis();
            if (now - lastTypingEventSent > 2000) { // Enviar evento cada 2 segundos
                sendTypingEvent();
                lastTypingEventSent = now;
            }
        });
        
        inputBox.getChildren().addAll(btnAttach, messageInput, btnEmoji, btnSend);

        chatArea.getChildren().addAll(chatHeader, chatTools, chatScroll, inputBox);

        // Estado Inicial (Sin chat seleccionado)
        StackPane centerStack = new StackPane();
        statusLabel = new Label("Selecciona un amigo para chatear");
        statusLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 16px;");
        centerStack.getChildren().addAll(statusLabel, chatArea);

        root.setLeft(sidebar);
        root.setCenter(centerStack);

        // Iniciar actualización automática (Polling)
        startPolling();

        return root;
    }

    private void loadSession() {
        if (SESSION_FILE.exists()) {
            try (FileReader reader = new FileReader(SESSION_FILE)) {
                JsonObject session = gson.fromJson(reader, JsonObject.class);
                if (session.has("token")) {
                    authToken = session.get("token").getAsString();
                    // Intentar obtener ID del usuario desde el token JWT
                    if (authToken != null) {
                        String[] parts = authToken.split("\\.");
                        if (parts.length > 1) {
                            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                            JsonObject json = gson.fromJson(payload, JsonObject.class);
                            if (json.has("user_id")) currentUserId = json.get("user_id").getAsInt();
                            if (json.has("username")) currentUsername = json.get("username").getAsString();
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void startPolling() {
        poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> {
            if (authToken == null) return;
            refreshFriends();
            if (activeFriendId != -1) {
                refreshChat(activeFriendId);
            }
        }, 0, 3, TimeUnit.SECONDS); // Actualizar cada 3 segundos
    }

    private void refreshFriends() {
        try {
            String response = sendRequest("GET", "/friends", null);
            if (response == null || response.isEmpty()) return;

            JsonObject json;
            try {
                json = gson.fromJson(response, JsonObject.class);
            } catch (com.google.gson.JsonSyntaxException ex) {
                System.err.println("GChat: Received invalid JSON from /friends endpoint: " + response);
                return;
            }

            if (json == null || !json.has("friends") || !json.has("pending")) return;

            JsonArray friends = json.getAsJsonArray("friends");
            JsonArray pending = json.getAsJsonArray("pending");

            Platform.runLater(() -> {
                this.fullFriendList = friends;
                updateFriendList(friends);
                updateRequestsList(pending);
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateFriendList(JsonArray friends) {
        friendListContainer.getChildren().clear();
        String filter = searchFriendsField.getText().toLowerCase().trim();
        boolean friendIsTyping = false; // Variable para rastrear el estado de escritura

        for (JsonElement e : friends) {
            JsonObject f = e.getAsJsonObject();
            int id = f.get("id").getAsInt();
            String username = f.get("username").getAsString();
            String status = f.has("status") ? f.get("status").getAsString() : "Offline";

            if (!filter.isEmpty() && !username.toLowerCase().contains(filter)) {
                continue;
            }

            // Comprobar si el amigo activo está escribiendo
            if (id == activeFriendId) {
                if (f.has("last_typing_time") && !f.get("last_typing_time").isJsonNull()) {
                    String timeStr = f.get("last_typing_time").getAsString();
                    try {
                        Instant typingTime = Instant.parse(timeStr);
                        long secondsSinceTyping = Duration.between(typingTime, Instant.now()).getSeconds();
                        if (secondsSinceTyping < 5) friendIsTyping = true;
                    } catch (DateTimeParseException ex) {}
                }
            }

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8));
            row.setStyle("-fx-background-color: " + (id == activeFriendId ? "rgba(0,120,215,0.4)" : "transparent") + "; -fx-background-radius: 5; -fx-cursor: hand;");
            
            Circle avatar = new Circle(15, Color.GRAY);

            VBox info = new VBox(2);
            Label lblName = new Label(username);
            lblName.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            Label lblStatus = new Label(status);
            lblStatus.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");
            info.getChildren().addAll(lblName, lblStatus);

            row.getChildren().addAll(avatar, info);
            row.setOnMouseClicked(ev -> selectFriend(id, username));
            
            // Efecto Hover
            row.setOnMouseEntered(ev -> { if(id != activeFriendId) row.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5; -fx-cursor: hand;"); });
            row.setOnMouseExited(ev -> { if(id != activeFriendId) row.setStyle("-fx-background-color: transparent; -fx-background-radius: 5; -fx-cursor: hand;"); });

            friendListContainer.getChildren().add(row);
        }
        
        // Actualizar el indicador de escritura fuera del bucle
        final boolean finalIsTyping = friendIsTyping;
        Platform.runLater(() -> {
            typingIndicator.setVisible(finalIsTyping);
        });
    }

    private void updateRequestsList(JsonArray requests) {
        requestsListContainer.getChildren().clear();
        if (requests.size() == 0) {
            Label lbl = new Label("No hay solicitudes");
            lbl.setStyle("-fx-text-fill: #777; -fx-padding: 10;");
            requestsListContainer.getChildren().add(lbl);
            return;
        }

        for (JsonElement e : requests) {
            JsonObject r = e.getAsJsonObject();
            int id = r.get("id").getAsInt();
            String username = r.get("username").getAsString();

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5));
            row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");

            Label lblName = new Label(username);
            lblName.setStyle("-fx-text-fill: white;");
            HBox.setHgrow(lblName, Priority.ALWAYS);

            Button btnAccept = new Button("✔");
            btnAccept.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
            btnAccept.setOnAction(ev -> acceptRequest(id));

            Button btnReject = new Button("✖");
            btnReject.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
            btnReject.setOnAction(ev -> removeFriend(id));

            row.getChildren().addAll(lblName, btnAccept, btnReject);
            requestsListContainer.getChildren().add(row);
        }
    }

    private void selectFriend(int id, String username) {
        activeFriendId = id;
        chatHeaderLabel.setText(username);
        statusLabel.setVisible(false);
        chatArea.setVisible(true);
        messagesContainer.getChildren().clear();
        refreshFriends();
        new Thread(() -> refreshChat(id)).start();
    }

    private void refreshChat(int friendId) {
        try {
            String response = sendRequest("GET", "/gchat/history/" + friendId, null);
            if (response == null) {
                // Si falla la conexión, intentar cargar localmente
                loadMessagesFromFile();
                return;
            }
            
            JsonArray messages = gson.fromJson(response, JsonArray.class);
            Platform.runLater(() -> updateChatMessages(messages));
        } catch (Exception e) { 
            e.printStackTrace();
            loadMessagesFromFile(); // Cargar caché en caso de error
        }
    }

    private void updateChatMessages(JsonArray messages) {
        if (messagesContainer.getChildren().size() == messages.size()) return;

        messagesContainer.getChildren().clear();
        for (JsonElement e : messages) {
            JsonObject m = e.getAsJsonObject();
            int senderId = m.get("sender_id").getAsInt();
            final String content = m.get("content").getAsString();
            String type = m.has("type") ? m.get("type").getAsString() : "text";
            String timestamp = m.has("timestamp") ? m.get("timestamp").getAsString() : "";
            
            boolean isMe = (senderId == currentUserId);
            
            // Determinar nombre para guardado/búsqueda
            String senderName = isMe ? currentUsername : chatHeaderLabel.getText();
            String searchContent = senderName + ": " + content;
            
            // Contenedor para el mensaje completo (burbuja + hora)
            VBox messageUnit = new VBox(3);
            messageUnit.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            // Nodo que contiene el contenido principal del mensaje
            Node contentNode;
            
            if ("image".equals(type)) {
                ImageView imageView = new ImageView();
                imageView.setFitWidth(250);
                imageView.setPreserveRatio(true);
                Image img = new Image(API_BASE.replace("/api", "") + content, true); // Carga en segundo plano
                imageView.setImage(img);
                imageView.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
                imageView.setOnMouseClicked(ev -> {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(API_BASE.replace("/api", "") + content)); } catch (Exception ex) { ex.printStackTrace(); }
                });
                contentNode = imageView;
            } else if ("video".equals(type)) {
                Button btnVideo = new Button("🎥 Ver Video");
                btnVideo.setStyle("-fx-background-color: #e91e63; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;");
                btnVideo.setOnAction(ev -> {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(API_BASE.replace("/api", "") + content)); } catch (Exception ex) { ex.printStackTrace(); }
                });
                contentNode = btnVideo;
            } else if ("audio".equals(type)) {
                Button btnAudio = new Button("🎵 Reproducir Audio");
                btnAudio.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;");
                btnAudio.setOnAction(ev -> {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(API_BASE.replace("/api", "") + content)); } catch (Exception ex) { ex.printStackTrace(); }
                });
                contentNode = btnAudio;
            } else if ("file".equals(type)) {
                Button btnFile = new Button("📄 Descargar Archivo");
                btnFile.setStyle("-fx-background-color: #607d8b; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 10;");
                btnFile.setOnAction(ev -> {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(API_BASE.replace("/api", "") + content)); } catch (Exception ex) { ex.printStackTrace(); }
                });
                contentNode = btnFile;
            } else {
                Label lblMsg = new Label(content);
                lblMsg.setWrapText(true);
                lblMsg.setMaxWidth(300);
                lblMsg.setStyle("-fx-text-fill: white; -fx-background-color: " + (isMe ? "#0078d7" : "#444") + "; -fx-padding: 8 12; -fx-background-radius: 15;");
                contentNode = lblMsg;
            }

            // Etiqueta para la hora
            Label lblTimestamp = new Label(formatTimestamp(timestamp));
            lblTimestamp.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");
            lblTimestamp.setPadding(new Insets(0, 8, 0, 8)); // Pequeño padding horizontal

            messageUnit.getChildren().addAll(contentNode, lblTimestamp);
            
            // Guardar datos para búsqueda y exportación
            messageUnit.setUserData(searchContent);
            
            messagesContainer.getChildren().add(messageUnit);
        }
        chatScroll.setVvalue(1.0);
    }

    // --- NUEVAS FUNCIONALIDADES ---

    private void filterMessages(String query) {
        String lowerQuery = query.toLowerCase();
        for (Node node : messagesContainer.getChildren()) {
            if (node instanceof VBox) {
                String content = (String) node.getUserData();
                if (content != null) {
                    boolean match = content.toLowerCase().contains(lowerQuery);
                    node.setVisible(match);
                    node.setManaged(match);
                }
            }
        }
    }

    private void saveChat() {
        try (FileWriter writer = new FileWriter(new File(DATA_DIR, "gchat_log.txt"))) {
            for (Node node : messagesContainer.getChildren()) {
                if (node.getUserData() != null) {
                    writer.write(node.getUserData().toString() + "\n");
                }
            }
            Platform.runLater(() -> MainView.showNotification("Guardado", "Chat guardado localmente.", "success"));
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> MainView.showNotification("Error", "No se pudo guardar el chat.", "error"));
        }
    }

    private void loadMessagesFromFile() {
        File file = new File(DATA_DIR, "gchat_log.txt");
        if (!file.exists()) return;

        Platform.runLater(() -> {
            // Evitar recargar si ya hay mensajes (simple debounce visual)
            if (!messagesContainer.getChildren().isEmpty()) return;

            MainView.showNotification("Offline", "Error de conexión. Mostrando historial local.", "warning");
            messagesContainer.getChildren().clear();
            
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    int splitIndex = line.indexOf(": ");
                    if (splitIndex != -1) {
                        String user = line.substring(0, splitIndex);
                        String content = line.substring(splitIndex + 2);
                        boolean isMe = user.equals(currentUsername) || user.equals("Yo");

                        VBox messageUnit = new VBox(3);
                        messageUnit.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        
                        Label lblMsg = new Label(content);
                        lblMsg.setWrapText(true);
                        lblMsg.setMaxWidth(300);
                        lblMsg.setStyle("-fx-text-fill: white; -fx-background-color: " + (isMe ? "#0078d7" : "#444") + "; -fx-padding: 8 12; -fx-background-radius: 15;");
                        
                        messageUnit.getChildren().add(lblMsg);
                        messageUnit.setUserData(line);
                        messagesContainer.getChildren().add(messageUnit);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || activeFriendId == -1) return;
        messageInput.clear();
        sendMessage(text, "text");
    }

    private void sendMessage(String content, String type) {
        if (content.isEmpty() || activeFriendId == -1) return;

        new Thread(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("content", content);
            payload.addProperty("type", type);
            String response = sendRequest("POST", "/gchat/send/" + activeFriendId, payload.toString());
            // Solo refrescar si el envío fue exitoso para evitar duplicados visuales
            if (response != null)
            refreshChat(activeFriendId);
        }).start();
    }

    private void sendFriendRequest(String username) {
        if (username.isEmpty()) return;
        new Thread(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("username", username);
            String resp = sendRequest("POST", "/friends/add", payload.toString());
            if (resp != null) {
                Platform.runLater(() -> MainView.showNotification("Solicitud", "Solicitud enviada", "info"));
                refreshFriends();
            }
        }).start();
    }

    private void acceptRequest(int friendId) {
        new Thread(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("friend_id", friendId);
            sendRequest("POST", "/friends/accept", payload.toString());
            refreshFriends();
        }).start();
    }

    private void removeFriend(int friendId) {
        new Thread(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("friend_id", friendId);
            sendRequest("POST", "/friends/remove", payload.toString());
            refreshFriends();
        }).start();
    }

    private void sendTypingEvent() {
        new Thread(() -> {
            sendRequest("POST", "/gchat/typing", "{}");
        }).start();
    }

    private void showAttachmentMenu(Button owner) {
        ContextMenu menu = new ContextMenu();
        MenuItem imgItem = new MenuItem("Imagen"); imgItem.setOnAction(e -> selectFile("image"));
        MenuItem vidItem = new MenuItem("Video"); vidItem.setOnAction(e -> selectFile("video"));
        MenuItem audItem = new MenuItem("Audio / Nota de Voz"); audItem.setOnAction(e -> selectFile("audio"));
        MenuItem fileItem = new MenuItem("Archivo"); fileItem.setOnAction(e -> selectFile("all"));
        
        menu.getItems().addAll(imgItem, vidItem, audItem, fileItem);
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    private void selectFile(String type) {
        FileChooser fc = new FileChooser();
        if (type.equals("image")) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        else if (type.equals("video")) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.webm", "*.mkv", "*.avi"));
        else if (type.equals("audio")) fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg", "*.m4a"));
        
        File file = fc.showOpenDialog(null);
        if (file != null) {
            uploadAndSendFile(file);
        }
    }

    private void uploadAndSendFile(File file) {
        MainView.showNotification("Subiendo...", "Enviando archivo, por favor espera.", "info");
        new Thread(() -> {
            try {
                String response = sendMultipartRequest("/gchat/upload_attachment", file);
                if (response != null) {
                    JsonObject json = gson.fromJson(response, JsonObject.class);
                    if (json.has("url")) {
                        String url = json.get("url").getAsString();
                        String type = json.get("type").getAsString();
                        sendMessage(url, type);
                    } else {
                        Platform.runLater(() -> MainView.showNotification("Error", "Respuesta del servidor inválida.", "error"));
                    }
                } else {
                    Platform.runLater(() -> MainView.showNotification("Error", "No se pudo subir la imagen.", "error"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> MainView.showNotification("Error", "Fallo al subir imagen: " + e.getMessage(), "error"));
            }
        }).start();
    }

    private void showEmojiPicker() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Emojis");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        FlowPane flow = new FlowPane();
        flow.setPadding(new Insets(10));
        flow.setHgap(5);
        flow.setVgap(5);

        String[] emojis = {"😀", "😂", "😍", "🤔", "👍", "👎", "❤️", "🔥", "🚀", "🎉", "💀", "😭", "💯", "🙏", "👀", "👋"};
        for (String emoji : emojis) {
            Button btn = new Button(emoji);
            btn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; -fx-cursor: hand;");
            btn.setOnAction(e -> messageInput.appendText(emoji));
            flow.getChildren().add(btn);
        }
        dialog.getDialogPane().setContent(flow);
        dialog.showAndWait();
    }

    private String sendMultipartRequest(String endpoint, File file) throws IOException {
        String boundary = "===" + System.currentTimeMillis() + "===";
        URL url = new URL(API_BASE + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append("\r\n");
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append("\r\n");
            writer.append("\r\n").flush();
            
            Files.copy(file.toPath(), os);
            os.flush();
            
            writer.append("\r\n").flush();
            writer.append("--" + boundary + "--").append("\r\n").flush();
        }
        return getResponse(conn);
    }

    private String sendRequest(String method, String endpoint, String jsonBody) {
        try {
            URL url = new URL(API_BASE + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "GLauncher/1.0");
            
            if (jsonBody != null) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            return getResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();

        // [FIX] Add Content-Type check to avoid parsing HTML as JSON when the API service is waking up.
        String contentType = conn.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            System.err.println("GChat: Received non-JSON response (Content-Type: " + contentType + "). The API service might be waking up or returning an error page.");
            // Consume and close the stream to allow connection reuse.
            try (InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                if (stream != null) {
                    while (stream.read() != -1) {}
                }
            } catch (IOException e) {
                // Ignore exceptions while trying to clear the stream.
            }
            return null; // Return null to indicate a non-JSON response, preventing parse errors.
        }

        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return null;
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            return response.toString();
        }
    }

    private String formatTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return "";
        }
        try {
            Instant instant = Instant.parse(isoTimestamp);
            ZonedDateTime localDateTime = instant.atZone(ZoneId.systemDefault());
            return DateTimeFormatter.ofPattern("HH:mm").format(localDateTime);
        } catch (DateTimeParseException e) {
            System.err.println("Could not parse timestamp: " + isoTimestamp);
            return "";
        }
    }
}