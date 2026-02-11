package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeLikeRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import Project.Cooking.A_I.service.SpoonacularClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final RecipeRepository recipeRepository;
    private final RecipeLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final SpoonacularClient spoonacularClient;

    public SearchController(
            RecipeRepository recipeRepository,
            RecipeLikeRepository likeRepository,
            UserRepository userRepository,
            SpoonacularClient spoonacularClient
    ) {
        this.recipeRepository = recipeRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.spoonacularClient = spoonacularClient;
    }

    // GET /api/search?q=chicken&page=0&size=20
    @GetMapping("/search")
    public ResponseEntity<?> hybridSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 50);

        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "items", List.of(),
                    "localCount", 0,
                    "externalCount", 0
            ));
        }

        Long myUserId = null;
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            myUserId = userRepository.findByEmail(auth.getName())
                    .map(User::getId)
                    .orElse(null);
        }

        var pageable = PageRequest.of(page, size);

        // ✅ LOCAL SEARCH (DTO ONLY – no LOB crash)
        List<FeedRecipeDto> local = recipeRepository.searchTitle(query, pageable);

        if (local.size() < size) {
            List<FeedRecipeDto> tagMatches = recipeRepository.searchTags(query, pageable);
            Set<Long> seen = new HashSet<>();

            for (FeedRecipeDto d : local) seen.add(d.id());

            for (FeedRecipeDto d : tagMatches) {
                if (d.id() != null && seen.add(d.id())) {
                    local.add(d);
                }
                if (local.size() >= size) break;
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();

        for (FeedRecipeDto r : local) {
            long likesCount = likeRepository.countByRecipeId(r.id());
            boolean likedByMe = false;

            if (myUserId != null) {
                likedByMe = likeRepository
                        .findByUserIdAndRecipeId(myUserId, r.id())
                        .isPresent();
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", r.id());
            m.put("title", r.title());
            m.put("imageUrl", r.imageUrl());
            m.put("tags", r.tags());
            m.put("likes", likesCount);
            m.put("likedByMe", likedByMe);
            m.put("views", r.views());
            m.put("source", r.source());
            m.put("createdAt", r.createdAt() == null ? null : r.createdAt().toString());
            m.put("isExternal", false);
            m.put("externalId", null);

            out.add(m);
        }

        // ✅ Spoonacular top-up (never crash)
        int remaining = size - out.size();

        if (remaining > 0) {
            try {
                int offset = page * size;
                var external = spoonacularClient.search(query, remaining, offset);

                for (var e : external) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", null);
                    m.put("title", e.getTitle());
                    m.put("imageUrl", e.getImageUrl());
                    m.put("tags", null);
                    m.put("likes", 0);
                    m.put("likedByMe", false);
                    m.put("views", 0);
                    m.put("source", "SPOONACULAR");
                    m.put("createdAt", null);
                    m.put("isExternal", true);
                    m.put("externalId", e.getExternalId());
                    out.add(m);
                }
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(Map.of(
                "items", out,
                "localCount", local.size(),
                "externalCount", Math.max(0, out.size() - local.size())
        ));
    }
}
