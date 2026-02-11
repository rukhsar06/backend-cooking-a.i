package Project.Cooking.A_I.repository;

import Project.Cooking.A_I.dto.FeedRecipeDto;
import Project.Cooking.A_I.model.Recipe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

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
    List<Recipe> findByIsPublicTrueAndTitleContainingIgnoreCaseOrderByLikesDescViewsDescCreatedAtDesc(
            String q,
            Pageable pageable
    );

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
    List<FeedRecipeDto> searchTitle(@Param("q") String q, Pageable pageable);

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
    List<FeedRecipeDto> searchTags(@Param("q") String q, Pageable pageable);

    // ✅ likes update (keep if you still use it somewhere)
    @Modifying
    @Transactional
    @Query("UPDATE Recipe r SET r.likes = :likes WHERE r.id = :id")
    int updateLikes(@Param("id") Long id, @Param("likes") long likes);

    // ✅ FIX: increment views without loading LOB entity
    @Modifying
    @Transactional
    @Query("UPDATE Recipe r SET r.views = r.views + 1 WHERE r.id = :id AND r.isPublic = true")
    int incrementViewsPublic(@Param("id") Long id);

    // ✅ read views only
    @Query("SELECT r.views FROM Recipe r WHERE r.id = :id")
    long getViews(@Param("id") Long id);

    // ✅ history-safe lightweight read (NO LOB touched)
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
        WHERE r.id IN :ids
    """)
    List<FeedRecipeDto> findLightByIds(@Param("ids") List<Long> ids);
}
