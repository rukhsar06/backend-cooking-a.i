package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.model.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // ----------------- profile/user -----------------
    List<Recipe> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Recipe> findByIdAndUserId(Long id, Long userId);
    Optional<Recipe> findByIdAndIsPublicTrue(Long id);

    long countByIsPublicTrue();

    Optional<Recipe> findBySourceAndExternalId(String source, String externalId);

    // ----------------- ✅ REQUIRED BY SearchController -----------------
    // Local search by TITLE (public only) + ordered like feed
    List<Recipe> findByIsPublicTrueAndTitleContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
            String q,
            Pageable pageable
    );

    // Local search by TAGS (public only) + ordered like feed
    List<Recipe> findByIsPublicTrueAndTagsContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
            String q,
            Pageable pageable
    );

    // ----------------- ✅ FeedController DTO queries (avoid LOB fetch) -----------------
    @Query("""
        SELECT new Project.Cooking.A_I.dto.FeedRecipeDto(
            r.id,
            r.title,
            r.imageUrl,
            r.tags,
            r.likes,
            false,
            r.views,
            r.source,
            r.createdAt
        )
        FROM Recipe r
        WHERE r.isPublic = true
        ORDER BY r.likes DESC, r.views DESC, r.createdAt DESC
    """)
    List<FeedRecipeDto> feed(Pageable pageable);

    @Query("""
        SELECT new Project.Cooking.A_I.dto.FeedRecipeDto(
            r.id,
            r.title,
            r.imageUrl,
            r.tags,
            r.likes,
            false,
            r.views,
            r.source,
            r.createdAt
        )
        FROM Recipe r
        WHERE r.isPublic = true
          AND LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY r.likes DESC, r.views DESC, r.createdAt DESC
    """)
    List<FeedRecipeDto> searchTitle(String q, Pageable pageable);

    @Query("""
        SELECT new Project.Cooking.A_I.dto.FeedRecipeDto(
            r.id,
            r.title,
            r.imageUrl,
            r.tags,
            r.likes,
            false,
            r.views,
            r.source,
            r.createdAt
        )
        FROM Recipe r
        WHERE r.isPublic = true
          AND r.tags IS NOT NULL
          AND LOWER(r.tags) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY r.likes DESC, r.views DESC, r.createdAt DESC
    """)
    List<FeedRecipeDto> searchTags(String q, Pageable pageable);
}
