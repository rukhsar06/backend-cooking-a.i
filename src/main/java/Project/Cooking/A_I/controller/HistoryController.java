package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.model.RecipeHistory;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeHistoryRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        String email = auth.getName().trim().toLowerCase();

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // POST /api/history/{recipeId}
    @PostMapping("/{recipeId}")
    @Transactional
    public ResponseEntity<?> track(@PathVariable Long recipeId, Authentication auth) {
        User user = me(auth);

        // validate recipe exists (404 instead of 500)
        recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        var existing = historyRepo.findByUser_IdAndRecipe_Id(user.getId(), recipeId);

        RecipeHistory h = existing.orElseGet(RecipeHistory::new);
        h.setUser(user);

        // ✅ do NOT load full recipe (LOB safety)
        h.setRecipe(recipeRepo.getReferenceById(recipeId));

        h.setLastViewedAt(LocalDateTime.now());
        historyRepo.save(h);

        return ResponseEntity.ok(Map.of("recipeId", recipeId, "tracked", true));
    }

    // GET /api/history
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> myHistory(Authentication auth) {
        User user = me(auth);

        var list = historyRepo.findByUser_IdOrderByLastViewedAtDesc(user.getId());

        // Extract IDs in order
        List<Long> ids = list.stream()
                .map(h -> h.getRecipe().getId())
                .filter(Objects::nonNull)
                .toList();

        if (ids.isEmpty()) return ResponseEntity.ok(List.of());

        // Fetch lightweight recipe rows (DTO) -> no LOB touched
        List<FeedRecipeDto> light = recipeRepo.findLightByIds(ids);

        Map<Long, FeedRecipeDto> byId = light.stream()
                .collect(Collectors.toMap(FeedRecipeDto::id, x -> x));

        // Return in history order
        List<Map<String, Object>> out = new ArrayList<>();

        for (RecipeHistory h : list) {
            Long rid = h.getRecipe().getId();
            FeedRecipeDto r = byId.get(rid);
            if (r == null) continue;

            out.add(Map.of(
                    "id", r.id(),
                    "title", r.title(),
                    "imageUrl", r.imageUrl(),
                    "tags", r.tags(),
                    "likes", r.likes(),
                    "views", r.views(),
                    "source", r.source(),
                    "createdAt", r.createdAt() == null ? null : r.createdAt().toString(),
                    "lastViewedAt", h.getLastViewedAt() == null ? null : h.getLastViewedAt().toString()
            ));
        }

        return ResponseEntity.ok(out);
    }

    // DELETE /api/history/{recipeId}
    @DeleteMapping("/{recipeId}")
    @Transactional
    public ResponseEntity<?> remove(@PathVariable Long recipeId, Authentication auth) {
        User user = me(auth);

        var existing = historyRepo.findByUser_IdAndRecipe_Id(user.getId(), recipeId);
        existing.ifPresent(historyRepo::delete);

        return ResponseEntity.ok(Map.of(
                "recipeId", recipeId,
                "deleted", true
        ));
    }
}
