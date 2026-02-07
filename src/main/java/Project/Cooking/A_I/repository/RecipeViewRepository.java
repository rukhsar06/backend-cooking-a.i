package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.model.RecipeView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeViewRepository extends JpaRepository<RecipeView, Long> {
    Optional<RecipeView> findByUserIdAndRecipeId(Long userId, Long recipeId);
    long countByRecipeId(Long recipeId);
}
