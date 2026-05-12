package glauncher.utils;

import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class EmojiHandler {

    // CDN de LobeHub para Fluent Emojis 3D (vía JSDelivr)
    private static final String FLUENT_CDN = "https://cdn.jsdelivr.net/npm/@lobehub/fluent-emoji-3d@1.1.0/assets/";

    /**
     * Convierte un texto que contiene emojis (ej: "Hola! 😄") en un TextFlow
     * que renderiza los emojis como imágenes del CDN de Fluent Emojis.
     * 
     * @param input El texto a procesar
     * @param fontSize Tamaño de la fuente y de los emojis
     * @param textColor Color del texto
     * @return Un TextFlow listo para añadir a la GUI
     */
    public static TextFlow render(String input, double fontSize, Color textColor) {
        TextFlow flow = new TextFlow();
        
        // Convertir shortcodes como :smile: a unicode real
        String unicodeInput = EmojiParser.parseToUnicode(input);
        
        int lastIndex = 0;
        List<Node> nodes = new ArrayList<>();

        // Obtener la lista de emojis encontrados en el texto
        List<EmojiParser.UnicodeCandidate> candidates = EmojiParser.getUnicodeCandidates(unicodeInput);

        for (EmojiParser.UnicodeCandidate candidate : candidates) {
            // 1. Añadir el texto que está ANTES del emoji
            if (candidate.getEmojiIndex() > lastIndex) {
                Text textNode = new Text(unicodeInput.substring(lastIndex, candidate.getEmojiIndex()));
                textNode.setFill(textColor);
                textNode.setStyle("-fx-font-size: " + fontSize + "px;");
                nodes.add(textNode);
            }

            // 2. Convertir el emoji en una imagen del CDN
            // Obtenemos el hex del primer codepoint (suficiente para la mayoría de emojis de Fluent)
            String hex = Integer.toHexString(candidate.getEmoji().getUnicode().codePointAt(0));
            
            ImageView emojiView = new ImageView();
            try {
                // Cargamos la imagen (.webp) desde el nuevo CDN de LobeHub
                // Nota: Usamos .webp ya que es el formato de este paquete 3D de alta calidad
                Image img = new Image(FLUENT_CDN + hex + ".webp", fontSize * 1.5, fontSize * 1.5, true, true);
                emojiView.setImage(img);
                emojiView.setFitWidth(fontSize * 1.3); // Un poco más grande que el texto para que destaque
                emojiView.setFitHeight(fontSize * 1.3);
                emojiView.setTranslateY(fontSize * 0.2); // Ajuste vertical para que no flote
            } catch (Exception e) {
                // Si falla la red, ponemos el unicode como texto plano (fallback)
                Text fallback = new Text(candidate.getEmoji().getUnicode());
                fallback.setFill(textColor);
                nodes.add(fallback);
            }
            nodes.add(emojiView);

            lastIndex = candidate.getEmojiIndex() + candidate.getEmoji().getUnicode().length();
        }

        // 3. Añadir el resto del texto después del último emoji
        if (lastIndex < unicodeInput.length()) {
            Text lastText = new Text(unicodeInput.substring(lastIndex));
            lastText.setFill(textColor);
            lastText.setStyle("-fx-font-size: " + fontSize + "px;");
            nodes.add(lastText);
        }

        flow.getChildren().addAll(nodes);
        return flow;
    }
}