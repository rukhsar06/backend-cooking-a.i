package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.service.SpoonacularClient;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final RecipeRepository recipeRepository;
    private final SpoonacularClient spoonacularClient;

    public ImportController(RecipeRepository recipeRepository, SpoonacularClient spoonacularClient) {
        this.recipeRepository = recipeRepository;
        this.spoonacularClient = spoonacularClient;
    }

    // ✅ Import Spoonacular recipe into your DB (public)
    // POST /api/import/spoonacular/{externalId}
    @PostMapping("/spoonacular/{externalId}")
    public ResponseEntity<?> importSpoonacular(@PathVariable String externalId) {

        if (!spoonacularClient.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Spoonacular disabled");
        }

        // already imported?
        return recipeRepository.findBySourceAndExternalId("SPOONACULAR", externalId)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(Map.of("id", r.getId(), "imported", false)))
                .orElseGet(() -> {
                    var info = spoonacularClient.getRecipeInformation(externalId);

                    if (info == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch external recipe");
                    }

                    Recipe r = new Recipe();
                    BeanWrapper bw = new BeanWrapperImpl(r);

                    // Always set what almost certainly exists
                    setIfPresent(bw, "title", info.getTitle());
                    setIfPresent(bw, "imageUrl", info.getImageUrl());

                    // source + external id (your model might use externalId or apiId etc)
                    setIfPresent(bw, "source", "SPOONACULAR");
                    setIfPresent(bw, "externalId", info.getExternalId());
                    setIfPresent(bw, "externalID", info.getExternalId());
                    setIfPresent(bw, "spoonacularId", info.getExternalId());

                    // public flag (property names vary)
                    setIfPresent(bw, "isPublic", true);
                    setIfPresent(bw, "public", true);
                    setIfPresent(bw, "publicRecipe", true);

                    // tags: could be String or List
                    if (info.getTags() != null && !info.getTags().isEmpty()) {
                        Object tagsValue = info.getTags();
                        if (hasProperty(bw, "tags")) {
                            Class<?> t = bw.getPropertyType("tags");
                            if (t != null && String.class.isAssignableFrom(t)) {
                                tagsValue = String.join(", ", info.getTags());
                            }
                            setIfPresent(bw, "tags", tagsValue);
                        }
                    }

                    // ingredients: could be String or List
                    if (info.getIngredients() != null && !info.getIngredients().isEmpty()) {
                        Object ingredientsValue = info.getIngredients();
                        if (hasProperty(bw, "ingredients")) {
                            Class<?> t = bw.getPropertyType("ingredients");
                            if (t != null && String.class.isAssignableFrom(t)) {
                                ingredientsValue = String.join("\n", info.getIngredients());
                            }
                            setIfPresent(bw, "ingredients", ingredientsValue);
                        }
                        // some projects name it differently
                        if (hasProperty(bw, "ingredientList")) setIfPresent(bw, "ingredientList", info.getIngredients());
                        if (hasProperty(bw, "ingredientText")) setIfPresent(bw, "ingredientText", String.join("\n", info.getIngredients()));
                    }

                    // summary/description
                    String summary = info.getSummary();
                    if (summary != null && !summary.isBlank()) {
                        setIfPresent(bw, "description", summary);
                        setIfPresent(bw, "summary", summary);
                    }

                    // instructions
                    String instructions = info.getInstructions();
                    if (instructions != null && !instructions.isBlank()) {
                        setIfPresent(bw, "instructions", instructions);
                        setIfPresent(bw, "instruction", instructions);
                        setIfPresent(bw, "steps", instructions);
                        setIfPresent(bw, "method", instructions);
                    }

                    Recipe saved = recipeRepository.save(r);

                    return ResponseEntity.ok(Map.of("id", saved.getId(), "imported", true));
                });
    }

    private boolean hasProperty(BeanWrapper bw, String prop) {
        try {
            bw.getPropertyType(prop);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setIfPresent(BeanWrapper bw, String prop, Object value) {
        try {
            if (value == null) return;
            bw.setPropertyValue(prop, value);
        } catch (Exception ignored) {
            // ignore if property doesn't exist or cannot be set
        }
    }
}
