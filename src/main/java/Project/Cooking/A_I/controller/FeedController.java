package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.repository.RecipeLikeRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final RecipeRepository recipeRepository;
    private final RecipeLikeRepository likeRepository;
    private final UserRepository userRepository;

    public FeedController(
            RecipeRepository recipeRepository,
            RecipeLikeRepository likeRepository,
            UserRepository userRepository
    ) {
        this.recipeRepository = recipeRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/count")
    public ResponseEntity<?> count() {
        long totalPublic = recipeRepository.countByIsPublicTrue();
        return ResponseEntity.ok(Map.of("publicCount", totalPublic));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> view(@PathVariable Long id) {
        Recipe r = recipeRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public recipe not found"));

        r.setViews(r.getViews() + 1);
        recipeRepository.save(r);

        return ResponseEntity.ok(Map.of(
                "id", r.getId(),
                "views", r.getViews()
        ));
    }

    @GetMapping
    public ResponseEntity<?> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 200);

        Long myUserId = null;
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            myUserId = userRepository.findByEmail(auth.getName())
                    .map(u -> u.getId())
                    .orElse(null);
        }

        List<FeedRecipeDto> items = recipeRepository.feed(PageRequest.of(page, size));

        if (myUserId != null) {
            Long uid = myUserId;
            items = items.stream().map(i ->
                    new FeedRecipeDto(
                            i.id(),
                            i.title(),
                            i.imageUrl(),
                            i.tags(),
                            i.likes(),
                            likeRepository.findByUser_IdAndRecipe_Id(uid, i.id()).isPresent(),
                            i.views(),
                            i.source(),
                            i.createdAt()
                    )
            ).toList();
        }

        return ResponseEntity.ok(items);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "title") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 200);

        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) return ResponseEntity.ok(List.of());

        Long myUserId = null;
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            myUserId = userRepository.findByEmail(auth.getName())
                    .map(u -> u.getId())
                    .orElse(null);
        }

        List<FeedRecipeDto> items;
        if ("tags".equalsIgnoreCase(type)) {
            items = recipeRepository.searchTags(query, PageRequest.of(page, size));
        } else {
            items = recipeRepository.searchTitle(query, PageRequest.of(page, size));
        }

        if (myUserId != null) {
            Long uid = myUserId;
            items = items.stream().map(i ->
                    new FeedRecipeDto(
                            i.id(),
                            i.title(),
                            i.imageUrl(),
                            i.tags(),
                            i.likes(),
                            likeRepository.findByUser_IdAndRecipe_Id(uid, i.id()).isPresent(),
                            i.views(),
                            i.source(),
                            i.createdAt()
                    )
            ).toList();
        }

        return ResponseEntity.ok(items);
    }
}
