package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.model.RecipeHistory;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeHistoryRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final RecipeHistoryRepository historyRepo;
    private final RecipeRepository recipeRepo;
    private final UserRepository userRepo;

    public HistoryController(RecipeHistoryRepository historyRepo,
                             RecipeRepository recipeRepo,
                             UserRepository userRepo) {
        this.historyRepo = historyRepo;
        this.recipeRepo = recipeRepo;
        this.userRepo = userRepo;
    }

    private User me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // ✅ Track history when recipe detail opened
    // POST /api/history/{recipeId}
    @PostMapping("/{recipeId}")
    public ResponseEntity<?> track(@PathVariable Long recipeId, Authentication auth) {
        User user = me(auth);

        Recipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        var existing = historyRepo.findByUserIdAndRecipeId(user.getId(), recipeId);

        RecipeHistory h = existing.orElseGet(RecipeHistory::new);
        h.setUser(user);
        h.setRecipe(recipe);
        h.setLastViewedAt(LocalDateTime.now());
        historyRepo.save(h);

        return ResponseEntity.ok(Map.of("recipeId", recipeId, "tracked", true));
    }

    // ✅ Get my history
    // GET /api/history
    @GetMapping
    public ResponseEntity<?> myHistory(Authentication auth) {
        User user = me(auth);

        var list = historyRepo.findByUserIdOrderByLastViewedAtDesc(user.getId());

        var out = list.stream().map(h -> {
            Recipe r = h.getRecipe();
            return Map.of(
                    "id", r.getId(),
                    "title", r.getTitle(),
                    "imageUrl", r.getImageUrl(),
                    "likes", r.getLikes(),
                    "views", r.getViews(),
                    "lastViewedAt", h.getLastViewedAt() == null ? null : h.getLastViewedAt().toString()
            );
        }).toList();

        return ResponseEntity.ok(out);
    }

    // ✅ Delete ONE history item
    // DELETE /api/history/{recipeId}
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<?> remove(@PathVariable Long recipeId, Authentication auth) {
        User user = me(auth);

        var existing = historyRepo.findByUserIdAndRecipeId(user.getId(), recipeId);

        existing.ifPresent(historyRepo::delete);

        return ResponseEntity.ok(Map.of(
                "recipeId", recipeId,
                "deleted", true
        ));
    }
}
