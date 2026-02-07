package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.model.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Recipe> findByIdAndUserId(Long id, Long userId);
    Optional<Recipe> findByIdAndIsPublicTrue(Long id);

    List<Recipe> findByIsPublicTrueOrderByLikesDescViewsDescCreatedAtDesc(Pageable pageable);

    long countByIsPublicTrue();

    // ✅ for imported recipes (avoid duplicates)
    Optional<Recipe> findBySourceAndExternalId(String source, String externalId);

    // ✅ local search (public recipes only, ordered like feed)
    List<Recipe> findByIsPublicTrueAndTitleContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
            String q, Pageable pageable
    );

    // ✅ OPTIONAL: search by tags too (ONLY if tags is a String column)
    List<Recipe> findByIsPublicTrueAndTagsContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
            String q, Pageable pageable
    );
}
