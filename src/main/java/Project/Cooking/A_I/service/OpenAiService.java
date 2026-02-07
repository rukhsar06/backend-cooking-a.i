package Project.Cooking.A_I.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class OpenAiService {

    @Value("${openai.apiKey:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.enabled:true}")
    private boolean enabled;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String getCookingGuideReply(String userText, String recipeTitle, String contextText) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return "AI is disabled right now.";
        }

        // Keep the prompt tight so it responds fast and doesn't yap during viva
        String instructions =
                "You are a calm cooking assistant. " +
                        "Guide the user step-by-step. " +
                        "Be concise, practical, and safety-aware. " +
                        "If the user asks what to do next, give the next 1-2 steps only. " +
                        "If the recipe context is present, use it.";

        String input =
                "Recipe Title: " + (recipeTitle.isBlank() ? "Unknown" : recipeTitle) + "\n\n" +
                        (contextText.isBlank() ? "" : ("Recipe Context:\n" + contextText + "\n\n")) +
                        "User: " + userText;

        try {
            // Responses API request body
            String jsonBody = """
            {
              "model": "%s",
              "instructions": %s,
              "input": %s
            }
            """.formatted(
                    escapeJson(model),
                    toJsonString(instructions),
                    toJsonString(input)
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)  // Bearer auth per docs :contentReference[oaicite:6]{index=6}
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() >= 400) {
                // Don’t crash the app during demo
                return "I couldn’t reach the AI service. Try again.";
            }

            JsonNode root = mapper.readTree(res.body());

            // output_text is the simplest way to read text from Responses
            JsonNode outputText = root.get("output_text");
            if (outputText != null && !outputText.asText("").isBlank()) {
                return outputText.asText();
            }

            // fallback: try reading from output array
            return "Okay. Tell me what step you’re on and what you see.";

        } catch (Exception e) {
            return "AI is having a moment. Try again.";
        }
    }

    private static String toJsonString(String s) {
        // minimal safe JSON string quoting
        return "\"" + escapeJson(s) + "\"";
    }

    private static String escapeJson(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
