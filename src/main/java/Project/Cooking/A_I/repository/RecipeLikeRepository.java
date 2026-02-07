package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.model.RecipeLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeLikeRepository extends JpaRepository<RecipeLike, Long> {

    long countByRecipeId(Long recipeId);

    Optional<RecipeLike> findByUserIdAndRecipeId(Long userId, Long recipeId);

    List<RecipeLike> findByUserId(Long userId);

    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
}
