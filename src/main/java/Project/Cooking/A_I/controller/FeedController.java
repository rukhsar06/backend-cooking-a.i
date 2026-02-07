package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.model.RecipeView;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeLikeRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.RecipeViewRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final RecipeRepository recipeRepository;
    private final RecipeLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final RecipeViewRepository viewRepository;

    public FeedController(
            RecipeRepository recipeRepository,
            RecipeLikeRepository likeRepository,
            UserRepository userRepository,
            RecipeViewRepository viewRepository
    ) {
        this.recipeRepository = recipeRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.viewRepository = viewRepository;
    }

    // ✅ TOTAL COUNT (public)
    // GET /api/feed/count
    @GetMapping("/count")
    public ResponseEntity<?> count() {
        long totalPublic = recipeRepository.countByIsPublicTrue();
        return ResponseEntity.ok(Map.of("publicCount", totalPublic));
    }

    // ✅ Home feed (public)
    // GET /api/feed?page=0&size=20
    // If token present -> also returns likedByMe
    @GetMapping
    public ResponseEntity<?> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = sanitizePage(page);
        size = sanitizeSize(size);

        Long myUserId = getMyUserId(auth);

        List<Recipe> items = recipeRepository
                .findByIsPublicTrueOrderByLikesDescViewsDescCreatedAtDesc(PageRequest.of(page, size));

        return ResponseEntity.ok(toFeedDto(items, myUserId));
    }

    // ✅ LIMITLESS SEARCH (public)
    // GET /api/feed/search?q=maggie&page=0&size=20
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = sanitizePage(page);
        size = sanitizeSize(size);

        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) {
            // if empty search, behave like normal feed
            return feed(page, size, auth);
        }

        Long myUserId = getMyUserId(auth);

        List<Recipe> items = recipeRepository
                .findByIsPublicTrueAndTitleContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
                        query, PageRequest.of(page, size)
                );

        return ResponseEntity.ok(toFeedDto(items, myUserId));
    }

    // ✅ Track view (ONLY ONCE per logged-in user per recipe)
    // POST /api/feed/{id}/view
    @PostMapping("/{id}/view")
    public ResponseEntity<?> view(@PathVariable Long id, Authentication auth) {

        // no login? no view count
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Login required to count a view"));
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        boolean alreadyViewed = viewRepository.findByUserIdAndRecipeId(user.getId(), id).isPresent();

        if (!alreadyViewed) {
            RecipeView rv = new RecipeView();
            rv.setUser(user);
            rv.setRecipe(recipe);
            viewRepository.save(rv);

            recipe.setViews(recipe.getViews() + 1);
            recipeRepository.save(recipe);
        }

        return ResponseEntity.ok(Map.of(
                "id", id,
                "views", recipe.getViews(),
                "counted", !alreadyViewed
        ));
    }

    // ----------------- helpers -----------------

    private int sanitizePage(int page) {
        return Math.max(page, 0);
    }

    private int sanitizeSize(int size) {
        if (size < 1) size = 1;
        if (size > 200) size = 200;
        return size;
    }

    private Long getMyUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return userRepository.findByEmail(auth.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    private List<Map<String, Object>> toFeedDto(List<Recipe> items, Long myUserId) {
        return items.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();

            long likesCount = likeRepository.countByRecipeId(r.getId());

            boolean likedByMe = false;
            if (myUserId != null) {
                likedByMe = likeRepository
                        .findByUserIdAndRecipeId(myUserId, r.getId())
                        .isPresent();
            }

            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("imageUrl", r.getImageUrl());
            m.put("tags", r.getTags());
            m.put("likes", likesCount);
            m.put("likedByMe", likedByMe);
            m.put("views", r.getViews());
            m.put("source", r.getSource());
            m.put("createdAt", r.getCreatedAt() == null ? null : r.getCreatedAt().toString());

            return m;
        }).toList();
    }
}
