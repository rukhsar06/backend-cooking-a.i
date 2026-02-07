package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeController(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    // DTO for create request
    public static class CreateRecipeRequest {
        public String title;
        public String ingredients;
        public String steps;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing/invalid token");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found for token email: " + email
                ));
    }

    // ✅ Create recipe (PRIVATE, owned by user)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateRecipeRequest req, Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (req == null || req.title == null || req.title.isBlank()
                || req.ingredients == null || req.ingredients.isBlank()
                || req.steps == null || req.steps.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title, ingredients, and steps are required");
        }

        Recipe recipe = new Recipe();
        recipe.setTitle(req.title.trim());
        recipe.setIngredients(req.ingredients.trim());
        recipe.setSteps(req.steps.trim());
        recipe.setUser(user);

        // default private unless you later add a toggle endpoint
        recipe.setPublic(false);

        Recipe saved = recipeRepository.save(recipe);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "title", saved.getTitle(),
                "ingredients", saved.getIngredients(),
                "steps", saved.getSteps(),
                "createdAt", saved.getCreatedAt() == null ? null : saved.getCreatedAt().toString()
        ));
    }

    // ✅ List my recipes (PRIVATE)
    @GetMapping
    public ResponseEntity<?> list(Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<Recipe> recipes = recipeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<Map<String, Object>> out = recipes.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "title", r.getTitle(),
                        "createdAt", r.getCreatedAt() == null ? null : r.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(out);
    }

    // ✅ Get one of MY recipes (PRIVATE, owned by user)
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Recipe recipe = recipeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        return ResponseEntity.ok(Map.of(
                "id", recipe.getId(),
                "title", recipe.getTitle(),
                "ingredients", recipe.getIngredients(),
                "steps", recipe.getSteps(),
                "createdAt", recipe.getCreatedAt() == null ? null : recipe.getCreatedAt().toString()
        ));
    }

    // ✅ Delete one of MY recipes (PRIVATE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Recipe recipe = recipeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        recipeRepository.delete(recipe);
        return ResponseEntity.ok(Map.of("message", "Deleted", "id", id));
    }

    // ✅ PUBLIC: details for FEED recipes (MEALDB/CURATED/public ones)
    // GET /api/recipes/public/{id}
    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublic(@PathVariable Long id) {

        Recipe r = recipeRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public recipe not found"));

        return ResponseEntity.ok(Map.of(
                "id", r.getId(),
                "title", r.getTitle(),
                "imageUrl", r.getImageUrl(),
                "tags", r.getTags(),
                "source", r.getSource(),
                "views", r.getViews(),
                "createdAt", r.getCreatedAt() == null ? null : r.getCreatedAt().toString(),
                "ingredients", r.getIngredients(),
                "steps", r.getSteps()
        ));
    }

    // ✅ PUBLIC: count (optional but useful)
    @GetMapping("/count")
    public ResponseEntity<?> countPublic() {
        return ResponseEntity.ok(Map.of("publicCount", recipeRepository.countByIsPublicTrue()));
    }
}
