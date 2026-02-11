package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.model.RecipeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeHistoryRepository extends JpaRepository<RecipeHistory, Long> {

    Optional<RecipeHistory> findByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    List<RecipeHistory> findByUser_IdOrderByLastViewedAtDesc(Long userId);
}
