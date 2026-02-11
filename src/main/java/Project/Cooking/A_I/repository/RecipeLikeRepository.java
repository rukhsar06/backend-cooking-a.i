package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.model.RecipeLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeLikeRepository extends JpaRepository<RecipeLike, Long> {

    long countByRecipe_Id(Long recipeId);

    Optional<RecipeLike> findByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    List<RecipeLike> findByUser_Id(Long userId);

    void deleteByUser_IdAndRecipe_Id(Long userId, Long recipeId);
}
