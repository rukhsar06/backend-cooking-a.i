package Project.Cooking.A_I.controller;

import Project.Cooking.A_I.model.Recipe;
import Project.Cooking.A_I.model.RecipeLike;
import Project.Cooking.A_I.model.User;
import Project.Cooking.A_I.repository.RecipeLikeRepository;
import Project.Cooking.A_I.repository.RecipeRepository;
import Project.Cooking.A_I.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private final RecipeLikeRepository likeRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public LikeController(RecipeLikeRepository likeRepository,
                          RecipeRepository recipeRepository,
                          UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    private User me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @PostMapping("/{recipeId}")
    public ResponseEntity<?> toggle(@PathVariable Long recipeId, Authentication auth) {

        User user = me(auth);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));

        var existing = likeRepository.findByUserIdAndRecipeId(user.getId(), recipeId);

        boolean likedNow;

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            likedNow = false;
        } else {
            RecipeLike rl = new RecipeLike();
            rl.setUser(user);
            rl.setRecipe(recipe);
            likeRepository.save(rl);
            likedNow = true;
        }

        long likesCount = likeRepository.countByRecipeId(recipeId);

        // ✅ FIX: don't cast to int
        recipe.setLikes(likesCount);
        recipeRepository.save(recipe);

        return ResponseEntity.ok(Map.of(
                "recipeId", recipeId,
                "liked", likedNow,
                "likes", likesCount
        ));
    }

    @GetMapping
    public ResponseEntity<?> myLikes(Authentication auth) {
        User user = me(auth);

        var likedRecipes = likeRepository.findByUserId(user.getId());

        var out = likedRecipes.stream().map(rl -> {
            Recipe r = rl.getRecipe();
            return Map.of(
                    "id", r.getId(),
                    "title", r.getTitle(),
                    "imageUrl", r.getImageUrl(),
                    "likes", r.getLikes()
            );
        }).toList();

        return ResponseEntity.ok(out);
    }
}
