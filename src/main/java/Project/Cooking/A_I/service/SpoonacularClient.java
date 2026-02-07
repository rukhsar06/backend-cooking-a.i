package Project.Cooking.A_I.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpoonacularClient {

    @Value("${spoonacular.apiKey:}")
    private String apiKey;

    @Value("${spoonacular.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    // Search results (cards)
    public List<ExternalRecipeCard> search(String q, int number, int offset) {
        if (!isEnabled()) return List.of();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.spoonacular.com/recipes/complexSearch")
                    .queryParam("apiKey", apiKey)
                    .queryParam("query", q)
                    .queryParam("number", number)
                    .queryParam("offset", offset)
                    .queryParam("addRecipeInformation", false)
                    .toUriString();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);

            List<ExternalRecipeCard> out = new ArrayList<>();
            for (JsonNode r : root.path("results")) {
                ExternalRecipeCard card = new ExternalRecipeCard();
                card.setExternalId(r.path("id").asText());
                card.setTitle(r.path("title").asText(""));
                card.setImageUrl(r.path("image").asText(""));
                out.add(card);
            }
            return out;

        } catch (Exception e) {
            return List.of();
        }
    }

    // Full recipe info (for importing)
    public ExternalRecipeInfo getRecipeInformation(String externalId) {
        if (!isEnabled()) return null;

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.spoonacular.com/recipes/" + externalId + "/information")
                    .queryParam("apiKey", apiKey)
                    .queryParam("includeNutrition", false)
                    .toUriString();

            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);

            ExternalRecipeInfo info = new ExternalRecipeInfo();
            info.setExternalId(root.path("id").asText());
            info.setTitle(root.path("title").asText(""));
            info.setImageUrl(root.path("image").asText(""));
            info.setSummary(stripHtml(root.path("summary").asText("")));
            info.setInstructions(stripHtml(root.path("instructions").asText("")));

            // tags/dishTypes
            List<String> tags = new ArrayList<>();
            for (JsonNode t : root.path("dishTypes")) tags.add(t.asText());
            for (JsonNode t : root.path("cuisines")) tags.add(t.asText());
            info.setTags(tags);

            // ingredients (original strings)
            List<String> ingredients = new ArrayList<>();
            for (JsonNode ing : root.path("extendedIngredients")) {
                String original = ing.path("original").asText("");
                if (!original.isBlank()) ingredients.add(original);
            }
            info.setIngredients(ingredients);

            return info;

        } catch (Exception e) {
            return null;
        }
    }

    private String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]*>", "").trim();
    }

    // DTOs
    public static class ExternalRecipeCard {
        private String externalId;
        private String title;
        private String imageUrl;

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class ExternalRecipeInfo {
        private String externalId;
        private String title;
        private String imageUrl;
        private String summary;
        private String instructions;
        private List<String> tags;
        private List<String> ingredients;

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public List<String> getIngredients() { return ingredients; }
        public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    }
}
