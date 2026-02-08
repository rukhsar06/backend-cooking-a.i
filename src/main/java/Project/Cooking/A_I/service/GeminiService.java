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
public class GeminiService {

    // ✅ MUST match application.properties key: gemini.apiKey=...
    @Value("${gemini.apiKey:}")
    private String apiKey;

    // ✅ Use a model that actually exists for REST in docs example
    // You can override in properties: gemini.model=...
    @Value("${gemini.model:gemini-3-flash-preview}")
    private String model;

    @Value("${gemini.enabled:true}")
    private boolean enabled;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String getCookingGuideReply(String userText, String recipeTitle, String contextText) {

        if (!enabled) {
            return "AI is disabled right now.";
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "AI is disabled right now (missing Gemini API key).";
        }

        if (userText == null || userText.trim().isEmpty()) {
            return "Say that again? I didn’t catch anything.";
        }

        // ✅ Put “system prompt” inside the user prompt (REST-safe)
        String instructions =
                "You are a calm cooking assistant.\n" +
                        "Guide the user step-by-step.\n" +
                        "Be concise, practical, and safety-aware.\n" +
                        "If the user asks what to do next, give the next 1-2 steps only.\n" +
                        "If recipe context is present, use it.\n";

        String title = (recipeTitle == null || recipeTitle.isBlank()) ? "Unknown" : recipeTitle.trim();
        String ctx = (contextText == null) ? "" : contextText.trim();

        String prompt =
                instructions + "\n" +
                        "Recipe Title: " + title + "\n\n" +
                        (ctx.isBlank() ? "" : ("Recipe Context:\n" + ctx + "\n\n")) +
                        "User: " + userText.trim() + "\n" +
                        "Assistant:";

        try {
            // ✅ REST endpoint (docs show v1beta)
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + escapePath(model)
                    + ":generateContent";

            // ✅ NO systemInstruction field
            String jsonBody = """
            {
              "contents": [{
                "parts": [{"text": %s}]
              }]
            }
            """.formatted(toJsonString(prompt));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() >= 400) {
                // ✅ return real error so you can debug fast
                return "Gemini error (" + res.statusCode() + "): " + res.body();
            }

            JsonNode root = mapper.readTree(res.body());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String text = textNode.asText("");

            if (text == null || text.isBlank()) {
                return "Okay. Tell me what step you’re on and what you see.";
            }

            return text.trim();

        } catch (Exception e) {
            return "Gemini error (exception): " + e.getMessage();
        }
    }

    private static String toJsonString(String s) {
        return "\"" + escapeJson(s == null ? "" : s) + "\"";
    }

    private static String escapeJson(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapePath(String s) {
        return (s == null ? "" : s).replaceAll("[^a-zA-Z0-9._\\-]", "");
    }
}
