package Project.Cooking.A_I.service;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MealDbImportService {

    private final RestTemplate restTemplate;
    private final RecipeRepository recipeRepo;

    public MealDbImportService(RestTemplate restTemplate, RecipeRepository recipeRepo) {
        this.restTemplate = restTemplate;
        this.recipeRepo = recipeRepo;
    }

    // ✅ MAIN ENTRY: import "count" more recipes
    public int importMoreMeals(int count) {
        return importMeals(count);
    }

    // Imports meals and UPSERTS into DB (prevents duplicates)
    public int importMeals(int limit) {
        if (limit < 1) limit = 1;
        if (limit > 500) limit = 500;

        int saved = 0;

        // ✅ 1) First: pull from A-Z (bigger pool than a-f)
        List<String> letters = new ArrayList<>();
        for (char c = 'a'; c <= 'z'; c++) letters.add(String.valueOf(c));
        Collections.shuffle(letters);

        for (String letter : letters) {
            if (saved >= limit) break;

            String url = "https://www.themealdb.com/api/json/v1/1/search.php?f=" + letter;
            MealDbResponse resp = safeGet(url);

            if (resp == null || resp.meals == null) continue;

            for (MealDbMeal m : resp.meals) {
                if (saved >= limit) break;

                boolean inserted = upsertMeal(m);
                if (inserted) saved++;
            }
        }

        // ✅ 2) If still not enough, spam random endpoint until limit reached
        // (and stop after some attempts so it doesn’t loop forever)
        int attempts = 0;
        int maxAttempts = limit * 6;

        while (saved < limit && attempts < maxAttempts) {
            attempts++;

            String url = "https://www.themealdb.com/api/json/v1/1/random.php";
            MealDbResponse resp = safeGet(url);
            if (resp == null || resp.meals == null || resp.meals.isEmpty()) continue;

            boolean inserted = upsertMeal(resp.meals.get(0));
            if (inserted) saved++;
        }

        return saved;
    }

    private MealDbResponse safeGet(String url) {
        try {
            return restTemplate.getForObject(url, MealDbResponse.class);
        } catch (Exception e) {
            System.out.println("MealDB fetch failed: " + url + " -> " + e.getMessage());
            return null;
        }
    }

    // returns true only when a NEW row was inserted (not updated)
    private boolean upsertMeal(MealDbMeal m) {
        if (m == null) return false;
        if (m.idMeal == null || m.strMeal == null || m.strInstructions == null) return false;

        Optional<Recipe> existing = recipeRepo.findBySourceAndExternalId("MEALDB", m.idMeal);
        boolean isNew = existing.isEmpty();

        Recipe r = existing.orElseGet(Recipe::new);

        r.setSource("MEALDB");
        r.setExternalId(m.idMeal);

        r.setTitle(m.strMeal.trim());
        r.setImageUrl(m.strMealThumb);

        // steps = instructions
        r.setSteps(m.strInstructions.trim());

        // ingredients must be non-null
        String ing = buildIngredients(m);
        if (ing.isBlank()) ing = "Ingredients not available";
        r.setIngredients(ing);

        // tags
        String tags = joinNonBlank(m.strCategory, m.strArea, m.strTags);
        r.setTags(tags.isBlank() ? null : tags);

        // show in feed
        r.setPublic(true);

        recipeRepo.save(r);
        return isNew;
    }

    private String joinNonBlank(String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.trim().isBlank()) out.add(p.trim());
        }
        return String.join(", ", out);
    }

    private String buildIngredients(MealDbMeal m) {
        List<String> lines = new ArrayList<>();

        addIng(lines, m.strMeasure1, m.strIngredient1);
        addIng(lines, m.strMeasure2, m.strIngredient2);
        addIng(lines, m.strMeasure3, m.strIngredient3);
        addIng(lines, m.strMeasure4, m.strIngredient4);
        addIng(lines, m.strMeasure5, m.strIngredient5);
        addIng(lines, m.strMeasure6, m.strIngredient6);
        addIng(lines, m.strMeasure7, m.strIngredient7);
        addIng(lines, m.strMeasure8, m.strIngredient8);
        addIng(lines, m.strMeasure9, m.strIngredient9);
        addIng(lines, m.strMeasure10, m.strIngredient10);
        addIng(lines, m.strMeasure11, m.strIngredient11);
        addIng(lines, m.strMeasure12, m.strIngredient12);
        addIng(lines, m.strMeasure13, m.strIngredient13);
        addIng(lines, m.strMeasure14, m.strIngredient14);
        addIng(lines, m.strMeasure15, m.strIngredient15);
        addIng(lines, m.strMeasure16, m.strIngredient16);
        addIng(lines, m.strMeasure17, m.strIngredient17);
        addIng(lines, m.strMeasure18, m.strIngredient18);
        addIng(lines, m.strMeasure19, m.strIngredient19);
        addIng(lines, m.strMeasure20, m.strIngredient20);

        return String.join("\n", lines);
    }

    private void addIng(List<String> lines, String measure, String ingredient) {
        if (ingredient == null) return;
        String ing = ingredient.trim();
        if (ing.isBlank()) return;

        String meas = (measure == null) ? "" : measure.trim();
        String line = (meas.isBlank() ? ing : (meas + " " + ing)).trim();
        lines.add("• " + line);
    }

    // ---- DTOs ----
    public static class MealDbResponse {
        public List<MealDbMeal> meals;
    }

    public static class MealDbMeal {
        public String idMeal;
        public String strMeal;
        public String strMealThumb;
        public String strInstructions;
        public String strCategory;
        public String strArea;
        public String strTags;

        public String strIngredient1; public String strIngredient2; public String strIngredient3; public String strIngredient4; public String strIngredient5;
        public String strIngredient6; public String strIngredient7; public String strIngredient8; public String strIngredient9; public String strIngredient10;
        public String strIngredient11; public String strIngredient12; public String strIngredient13; public String strIngredient14; public String strIngredient15;
        public String strIngredient16; public String strIngredient17; public String strIngredient18; public String strIngredient19; public String strIngredient20;

        public String strMeasure1; public String strMeasure2; public String strMeasure3; public String strMeasure4; public String strMeasure5;
        public String strMeasure6; public String strMeasure7; public String strMeasure8; public String strMeasure9; public String strMeasure10;
        public String strMeasure11; public String strMeasure12; public String strMeasure13; public String strMeasure14; public String strMeasure15;
        public String strMeasure16; public String strMeasure17; public String strMeasure18; public String strMeasure19; public String strMeasure20;
    }
}
