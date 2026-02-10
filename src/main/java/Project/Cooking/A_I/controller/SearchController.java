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
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 50) size = 50;

        String query = (q == null) ? "" : q.trim();
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

        // ✅ 1) LOCAL search (DTO ONLY -> avoids LOB crash)
        List<FeedRecipeDto> local = recipeRepository.searchTitle(query, pageable);

        // Optional tag search top-up (also DTO)
        if (local.size() < size) {
            List<FeedRecipeDto> tagMatches = recipeRepository.searchTags(query, pageable);

            Set<Long> seen = new HashSet<>();
            for (FeedRecipeDto d : local) seen.add(d.getId());

            for (FeedRecipeDto d : tagMatches) {
                if (d.getId() != null && seen.add(d.getId())) {
                    local.add(d);
                }
                if (local.size() >= size) break;
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();

        for (FeedRecipeDto r : local) {
            // keep your likes logic same (count from likes table)
            long likesCount = likeRepository.countByRecipeId(r.getId());
            boolean likedByMe = false;

            if (myUserId != null) {
                likedByMe = likeRepository.findByUserIdAndRecipeId(myUserId, r.getId()).isPresent();
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("imageUrl", r.getImageUrl());
            m.put("tags", r.getTags());
            m.put("likes", likesCount);
            m.put("likedByMe", likedByMe);
            m.put("views", r.getViews());
            m.put("source", r.getSource());
            m.put("createdAt", r.getCreatedAt() == null ? null : r.getCreatedAt().toString());

            m.put("isExternal", false);
            m.put("externalId", null);

            out.add(m);
        }

        // ✅ 2) TOP UP WITH SPOONACULAR (never crash)
        int remaining = size - out.size();
        String externalError = null;

        if (remaining > 0) {
            int offset = page * size;

            try {
                var external = spoonacularClient.search(query, remaining, offset);

                if (external != null) {
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
                }
            } catch (Exception ex) {
                externalError = ex.getMessage();
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("items", out);
        resp.put("localCount", local.size());
        resp.put("externalCount", Math.max(0, out.size() - local.size()));
        if (externalError != null) resp.put("externalError", externalError);

        return ResponseEntity.ok(resp);
    }
}
