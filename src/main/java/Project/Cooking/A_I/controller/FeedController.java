package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.repository.RecipeLikeRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<?> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 200);

        Long myUserId = null;
        if (auth != null && auth.isAuthenticated()) {
            myUserId = userRepository.findByEmail(auth.getName())
                    .map(u -> u.getId())
                    .orElse(null);
        }

        List<FeedRecipeDto> items =
                recipeRepository.feed(PageRequest.of(page, size));

        if (myUserId != null) {
            Long uid = myUserId;
            items = items.stream().map(i ->
                    new FeedRecipeDto(
                            i.id(),
                            i.title(),
                            i.imageUrl(),
                            i.tags(),
                            i.likes(),
                            likeRepository.findByUserIdAndRecipeId(uid, i.id()).isPresent(),
                            i.views(),
                            i.source(),
                            i.createdAt()
                    )
            ).toList();
        }

        return ResponseEntity.ok(items);
    }
}
